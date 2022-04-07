/*******************************************************************************
 * Copyright [2022] [IBM]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.eng.aisp.feature.processor.vector;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.eng.aisp.AISPException;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.util.MatrixUtil;
import org.eng.aisp.util.MatrixUtil.NormalizationMode;
import org.eng.cache.IMultiKeyCache;
import org.eng.aisp.util.VectorUtils;

/**
 * Computes the delta (defined as the standard way in speech processing) of features with options to normalize and scale features.
 * <p>
 * See MatrixUtil#columnDelta(double[][], int) and 
 * <a href="http://practicalcryptography.com/miscellaneous/machine-learning/guide-mel-iifrequency-cepstral-coefficients-mfccs/#deltas-and-delta-deltas">
 * http://practicalcryptography.com/miscellaneous/machine-learning/guide-mel-iifrequency-cepstral-coefficients-mfccs/#deltas-and-delta-deltas</a>
 * @author wangshiq
 */
public class DeltaFeatureProcessor extends AbstractVectorMappingFeatureProcessor implements IFeatureProcessor<double[]> {

	public enum ScaleMethod implements Serializable {
		Unscaled,
		ScaleByStddev,
		ScaleByRange
	}
	
	private static class NormalizationMethod implements Serializable {
		
		private static final long serialVersionUID = -8891477852310337300L;
		private ScaleMethod scaling;
		private boolean zeroMean;

		NormalizationMethod(ScaleMethod scaling, boolean zeroMean) {
			this.scaling = scaling;
			this.zeroMean = zeroMean;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			// WARNING: we change to take the hashcode of the string value of 'scaling' because the 
			// IBM JVM (v8 and 9) is returning different hash codes for the same value across different runs
			// of the JVM - the Java spec does say this is ok, but the Oracle JVM does not do this.  
			// The hash code of the string does appear to be repeatable across JVM invocations.
			// The ModelUtil.rank...() function currently relies on repeatable hash codes
			// across JVM invocations.  This is an easy fix. dawood - 9/20/2020. 
//			result = prime * result + ((scaling == null) ? 0 : scaling.hashCode());
			result = prime * result + ((scaling == null) ? 0 : scaling.toString().hashCode());
			result = prime * result + (zeroMean ? 1231 : 1237);
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof NormalizationMethod))
				return false;
			NormalizationMethod other = (NormalizationMethod) obj;
			if (scaling != other.scaling)
				return false;
			if (zeroMean != other.zeroMean)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "NormalizationMethod [scaling=" + scaling + ", zeroMean=" + zeroMean + "]";
		}

	}

	private static final long serialVersionUID = 7051550414628966476L;
	protected final int halfWindowSize;
	protected final double[] differenceWeights;
	
	/** For each difference, identifies whether or not the delta is normalized before applying the weight */
	protected final NormalizationMethod[] normalizationMethod;
	
	/** The number of differences including the order 0 difference (i.e. the original data) */
	private transient int nKeptDifferences;
	/** 0-based index of the maximum difference */
	private transient int maxDifferenceIndex;
	/** Is there a non-unit weight or are any of the features normalized some how */
	private transient boolean scaledOrNormalized;
	/** Flags indicating whether a given difference is calculated, including the 0th difference */
	private transient boolean[] isDifferenceComputed;
	
	/** Caches unscaled, unnormalized differences */
	private static IMultiKeyCache cache = null; 
	// Looking at the Ensemble with new GMMs on 1,2,4, and 8 minutes of training data, turning on the cache reduces performance by about 3%! dawood-10/15/2017
	// So given that and the extra memory load the cache adds, let's not use it.
//	private static IMultiKeyCache cache = Cache.newGlobalMultiKeyCache(); 

	
	/**
	 * Define the features produced including the number of differences and whether and how normalization is done on a per delta basis. 
	 * Any number of differences may be computed, including 0, with all feature deltas for a given feature concatenated into a single vector.
	 * The resulting feature for an input of N x M will be N x k*M, where k is the number of deltas (including 0 if requested) requested.
	 * The number of differences and which ones to output is controlled by the {@link #differenceWeights} parameter and normalization
	 * by {@link #scaleMethod} and {@link #zeroMean}. 
	 * Normalization is done across the whole matrix of features provided to {@link #map(IFeature[], IFeature[])} or deltas of those features, separately.
	 * The values are normalized by the following formula:
	 * <ul>
	 * <li> x[i] = (x[i] - offsetAdjust) / scaleFactor;
	 * </ul>
	 * Where offsetAdjust is the mean of the x values across the whole matrix if {@link #zeroMean} is true, otherwise 0, and scaleFactor is 
	 * set according to {@link #scaleMethod}. 
	 * Deltas are computed on the original unnormalized values.
	 * @param halfWindowWidth The defines the window size as 2*halfWidth+1 for delta computation, must be larger than zero. 
	 * @param differenceWeights defines a) the number of differences to take and b) the weights applied to each difference.
	 * The zero'th element applies to the original feature (i.e. zero order difference).  
	 * Weights are applied after normalization, if any.
	 * If the weight for the Nth difference is 0, then the Nth difference is not included in the output. 
	 * So for example, to compute the order 1 difference use { 0, 1 }, for the order 2 difference only,
	 * use { 0,0,1 } for both the order 1 and 2 differences using { 0, 1, 1 }.  
	 * To include the original feature data set the first weight to a non-zero value.  
	 * For example, to get 3 features concatenated into a single feature, set the weights to {1, 1, 1}.  And of course you may use different weights other than 1.
	 * <b>NOTE:</b>Because of the way deltas are computed, the 1st difference is scaled down by 2 and 2nd difference is scaled down by 8, so that you may want to
	 * consider weights of {1, 2, 8}.
	 * @param scaleMethod controls whether or not scaling is done and if so, what scale factor is used.   
	 * <ul>
	 * <li> {@link ScaleMethod#Unscaled} no scaling is done and scaleFactor is set to 1.
	 * <li> {@link ScaleMethod#ScaleByStddev} then scaling is done by the standard deviation and scaleFactor is set to the standard deviation of the whole matrix. 
	 * <li> {@link ScaleMethod#ScaleByRange} then scaling is done by range of values and scaleFactor is set to the (max - min) of the whole matrix. 
	 * </ul>
	 * @param zeroMean if true then the  offsetAdjustment value in the above formula is set to the mean of the x values across the matrix, otherwise it is
	 * set to 0. This setting is independent of the scaling method (so you can zero the mean even if you don't want to scale the data). 
	 * <br>
	 */
	public DeltaFeatureProcessor(int halfWindowSize, double[] differenceWeights, ScaleMethod scaleMethod, boolean zeroMean) {
		if (halfWindowSize < 1)
			throw new IllegalArgumentException("Window size must be 1 or larger");
		this.halfWindowSize = halfWindowSize;
		this.normalizationMethod = new NormalizationMethod[differenceWeights.length];
		this.differenceWeights = new double[differenceWeights.length];
		for (int i=0 ; i<differenceWeights.length ;i++) {
			this.differenceWeights[i] = differenceWeights[i];
			normalizationMethod[i] = new NormalizationMethod(scaleMethod, zeroMean);
		}
	}


	private void initializeTransients() {
		if (isDifferenceComputed != null)
			return;	// Already done by another thread.
		synchronized(this) {
			if (isDifferenceComputed != null)
				return;
			boolean localIsDifferenceComputed[] = new boolean[differenceWeights.length];
			int count = 0, max = 0;
			for (int i=0 ; i<differenceWeights.length ;i++) {
				double weight = differenceWeights[i];
				if (weight != 0) {
					count++;
					max = i;
					localIsDifferenceComputed[i] = true;
					scaledOrNormalized = scaledOrNormalized 
						|| weight != 1 
						|| normalizationMethod[i].zeroMean 	
						|| normalizationMethod[i].scaling != ScaleMethod.Unscaled;
				} else {
					localIsDifferenceComputed[i] = false;
				}
			}
			this.maxDifferenceIndex = max; 
			nKeptDifferences = count;	
			isDifferenceComputed = localIsDifferenceComputed; // needs to be last because of the test above.
		}
		
	}

	/**
	 * Create the instance to compute the given deltas, w/o any scaling or offset adjustment.
	 * @see {@link #DeltaFeatureProcessor(int, double[], ScaleMethod, boolean)}
	 */
	public DeltaFeatureProcessor(int halfWindowSize, double[] differenceWeights) {
		this(halfWindowSize, differenceWeights, ScaleMethod.Unscaled, false);
	}

	/**
	 * Computes and returns the delta features
	 * @param features array of IFeature<double[]>
	 * @return new matrix delta features computed on the input features, possibly including the original features.  
	 * If there are not enough input features, then we return a 0x0 matrix.
	 * @throws AISPException 
	 */
	@Override
	protected double[][] map(IFeature<double[]>[] features, final double[][] inputFeatureData) {

//		if (inputFeatureData.length < this.windSize)
		if (inputFeatureData.length < (2*this.halfWindowSize + 1))	// halfWindowSize is actually the half width.
			throw new IllegalArgumentException("There are too few input feature vectors to compute the requested deltas using a window size of " 
						+ this.halfWindowSize);

		// Make sure our transients are initialized.
		initializeTransients();
		
		// First get the unscaled, unnormalized features, 
		double[][][] differences = computeDifferences(features, inputFeatureData);
		
		// Next modify them according to weights and normalization.
		if (scaledOrNormalized) 
			differences = normalizeAndWeightDifferences(differences);

		double[][] newFeatures = this.vectorizeDifferences(inputFeatureData[0].length, differences);
//		double[][] newFeatures = inputFeatureData;
		
		return newFeatures;
	}

	private static AtomicInteger CacheHits = new AtomicInteger();
//	private static Map<Object, AtomicInteger> FeatureKeyCount = new Hashtable<Object,AtomicInteger>();
	
	/**
	 * Compute each of the requested differences.
	 * @param inputFeatureData
	 * @return an array of length equal to the maximum difference to compute with uncomputed differences having the corresponding element set to null.
	 * All returned data is newly allocated and does not reference data in the input.
	 */
	private double[][][] computeDifferences(final IFeature<double[]>[] features, final double[][] inputFeatureData) {
		// First look in our cache .
		double[][][] featureDataDifferences = null; 
		Object featureKey = null;
		int isComputedHashCode = 0;
		
		if (cache != null && maxDifferenceIndex > 1) {	// Don't cache unless we're computing differences.
//			featureKey = Arrays.hashCode((double[])getKey(inputFeatureData));
			featureKey = features[0].getInstanceID();
			isComputedHashCode = Arrays.hashCode(isDifferenceComputed);
			featureDataDifferences = (double[][][])cache.get(halfWindowSize, maxDifferenceIndex, isComputedHashCode, featureKey);
		}

//		AtomicInteger count; 
//		synchronized (FeatureKeyCount) {
//			count = FeatureKeyCount.get(featureKey);
//			if (count == null) {
//				count = new AtomicInteger();
//				FeatureKeyCount.put(featureKey, count);
//			}
//		}
//		int cnt = count.addAndGet(1);
//		AISPLogger.logger.info("featureKey=" + featureKey + ", count=" + cnt);

		if (featureDataDifferences != null) {

//			AISPLogger.logger.info("halfWindowSize=" + halfWindowSize + ", maxDiff=" + maxDifferenceIndex + ", isComputed= " 
//					+ Arrays.toString(isDifferenceComputed) + " and its hashcode=" + isComputedHashCode 
//					+ ", featureKey=" + featureKey);
			
//			int v = CacheHits.incrementAndGet();
//			if (v % 100 == 0) 
//				AISPLogger.logger.info("Cache hit #" + v);

//			AISPLogger.logger.info("Cache hit resulted in...");
//			for (int i=0 ; i<featureDataDifferences.length; i++) {
//				MatrixUtil.showMatrix(featureDataDifferences[i]);
//			}
			return featureDataDifferences;
		}

		double[][] featureData = inputFeatureData;
		featureDataDifferences = new double[maxDifferenceIndex+1][][];
		// compute the differences on the features.  with no scaling. 
		for (int i=0 ; i<=maxDifferenceIndex; i++)  {
			if (i != 0)
				featureData = MatrixUtil.columnDelta(featureData, halfWindowSize);	// Produces same dimensions as input. halfWindowSize is actually the half width.
			if (isDifferenceComputed == null)
				this.initializeTransients();
			if (!isDifferenceComputed[i]) {
				featureDataDifferences[i] = null; 
			} else if (i == 0) { 	// original feature, don't reuse, make a copy. 
				featureDataDifferences[i] = MatrixUtil.clone(featureData);			// make a copy. scaling is done later. 
			} else {
				featureDataDifferences[i] = featureData; 
			}
		}
		// Store the result in the cache.
//		AISPLogger.logger.info("Caching ...");
//		for (int i=0 ; i<featureDataDifferences.length; i++) {
//			MatrixUtil.showMatrix(featureDataDifferences[i]);
//		}
		if (cache != null && featureKey != null)
			cache.put(featureDataDifferences, halfWindowSize, maxDifferenceIndex, isComputedHashCode, featureKey); 
		return featureDataDifferences;
	}
	
	/**
	 * Takes the multiple matrices and effectively appends them vertically.
	 * @param featureLength the length of an individual feature (i.e. the number of rows/values for given subwindow).
	 * @param featureDifferences an array of all the requested differences (include 0 if requested).
	 * @return
	 */
	private double[][] vectorizeDifferences(int featureLength, double[][][] featureDifferences) {
		assert featureDifferences.length > 0;
		// Now concatenate the differences into a single double[] for each feature.
		int outputFeatureLen = nKeptDifferences * featureLength; 
		int numFeaturesInTime = featureDifferences[maxDifferenceIndex].length;
		double[][] newFeatures = new double[numFeaturesInTime][];	// 1 output feature for each input feature.
		for (int i=0 ; i<numFeaturesInTime; i++)  {	// Over each column
			double[] outputFeatureData; 
			if (nKeptDifferences == 1) {
				// Don't need to do any actual copying.
				outputFeatureData = featureDifferences[maxDifferenceIndex][i];
//				outputFeatureData = Arrays.copyOf(outputFeatureData, outputFeatureData.length);
			} else {
				// Create a new array and do the concatenation.
				outputFeatureData = new double[outputFeatureLen];
				int destPos = 0;
				for (int k=0 ; k<=maxDifferenceIndex ; k++) {	// Over each matrix of differences
					// Append the column from the kth matrix/difference.
					if (featureDifferences[k] != null) {
						int length = featureDifferences[k][i].length;
						System.arraycopy(featureDifferences[k][i], 0, outputFeatureData, destPos, length);
						destPos += length;
					}
				}
			} 
			newFeatures[i] = outputFeatureData; 
		}

		return newFeatures;
	}

	/**
	 * @param newFeatures
	 * @return
	 */
	private double[][][] normalizeAndWeightDifferences(double[][][] differences) {
		double[][][] newDiffs = new double[differences.length][][];
		for (int i=0 ; i<differences.length ;i++)  {	// All differences
			if (!isDifferenceComputed[i]) 
				newDiffs[i] = null; // difference was not computed and is not present in featureData
			else
				newDiffs[i] = normalizeAndWeightDifference(i, differences[i]); 
		}
		return newDiffs;
	}

	private static Object getKey(double[][] features) {
//		int count = 1; 
		int count =  features.length;
		int featureLen = features[0].length;
		double[] key =  null;

		int captured = 0;
		for (int i=0 ; captured < count && i<features.length ; i++) {
			if (!VectorUtils.isConstant(features[i])) {
				if (count == 1)  {
//					AISPLogger.logger.info("feature len=" + features[i].length);
					return features[i];
				} else {
					if (key == null)
						key = new double[count * featureLen]; 
					System.arraycopy(features[i], 0, key, captured * featureLen, featureLen);
				}
				captured++;
			}
		}
		return key;
		
	}

	/**
	 * Apply the normalization and scaling/weighting of the given i'th difference.
	 * @param differenceIndex the index of this difference feature.  0..maxDifferenceIndex
	 * @param differenceFeature the feature to normalized and scale.  This feature is  unmodified upon return.
	 * @return  a new new feature matrix that has been normalized and/or scaled according to this instance's configuration.
	 */
	private double[][] normalizeAndWeightDifference(int differenceIndex, final double[][] differenceFeature) {
		assert differenceIndex >= 0 && differenceIndex <= maxDifferenceIndex;
		assert isDifferenceComputed[differenceIndex] || differenceFeature == null;
		assert differenceWeights[differenceIndex] != 0;
		
		double[][] r;
		final boolean zeroMean =     normalizationMethod[differenceIndex].zeroMean; 
		if (normalizationMethod[differenceIndex].scaling != ScaleMethod.Unscaled) {
			final boolean normByStddev = normalizationMethod[differenceIndex].scaling == ScaleMethod.ScaleByStddev;
			r = MatrixUtil.normalize(differenceFeature, normByStddev, zeroMean, NormalizationMode.Matrix, false); 	// new matrix
			r = MatrixUtil.scalarMultiply(r, differenceWeights[differenceIndex], true);
		} else if (zeroMean) {	// no scaling, but zero the mean. 
			r = MatrixUtil.zeroMean(differenceFeature, false);														// new matrix
			r = MatrixUtil.scalarMultiply(r, differenceWeights[differenceIndex], true);		
		} else {
			r = MatrixUtil.scalarMultiply(differenceFeature, differenceWeights[differenceIndex], false);			// new matrix
		}
		return r;
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return "DeltaFeatureProcessor [halfWindowSize=" + halfWindowSize + ", differenceWeights="
				+ (differenceWeights != null
						? Arrays.toString(Arrays.copyOf(differenceWeights, Math.min(differenceWeights.length, maxLen)))
						: null)
				+ ", normalizationMethod="
				+ (normalizationMethod != null
						? Arrays.asList(normalizationMethod).subList(0, Math.min(normalizationMethod.length, maxLen))
						: null)
				+ "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(differenceWeights);
		result = prime * result + Arrays.hashCode(normalizationMethod);
		result = prime * result + halfWindowSize;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof DeltaFeatureProcessor))
			return false;
		DeltaFeatureProcessor other = (DeltaFeatureProcessor) obj;
		if (!Arrays.equals(differenceWeights, other.differenceWeights))
			return false;
		if (!Arrays.equals(normalizationMethod, other.normalizationMethod))
			return false;
		if (halfWindowSize != other.halfWindowSize)
			return false;
		return true;
	}
	
	public static void main(String[] args) {
		ScaleMethod sm1 = ScaleMethod.ScaleByRange;
		ScaleMethod sm2 = ScaleMethod.ScaleByStddev;
		ScaleMethod sm3 = ScaleMethod.Unscaled;
		System.out.println("sm1 " + sm1.hashCode());
		System.out.println("sm2 " + sm2.hashCode());
		System.out.println("sm3 " + sm3.hashCode());
		System.out.println(sm1.equals(sm1) ? "Equal" : "Not equal");
		System.out.println(sm2.equals(sm2) ? "Equal" : "Not equal");
		System.out.println(sm3.equals(sm3) ? "Equal" : "Not equal");
		System.out.println(sm1.equals(sm2) ? "Equal" : "Not equal");
		System.out.println(sm1.equals(sm3) ? "Equal" : "Not equal");
		System.out.println(sm2.equals(sm3) ? "Equal" : "Not equal");
		System.out.println("sm1 " + sm1.hashCode());
		System.out.println("sm2 " + sm2.hashCode());
		System.out.println("sm3 " + sm3.hashCode());
	}

}
