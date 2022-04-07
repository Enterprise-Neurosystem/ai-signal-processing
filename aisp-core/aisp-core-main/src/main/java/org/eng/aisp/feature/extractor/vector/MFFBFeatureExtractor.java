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
package org.eng.aisp.feature.extractor.vector;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.eng.aisp.IDataWindow;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.feature.DoubleFeature;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.extractor.IDoubleFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.util.ExtendedFFT;
import org.eng.aisp.feature.extractor.vector.util.TriangleFilter;
import org.eng.aisp.processor.AbstractCachingWindowProcessor;
import org.eng.aisp.util.Signal2D;
import org.eng.aisp.util.VectorUtils;
import org.eng.util.Vector;

/**
 * Mel-frequency filter bank (MFFB) feature extractor.
 * Beyond the number of bands in the resulting MFFB, configuration is available to allow restricting to a frequency range, 
 * normalization (scaling), taking the log of the MFFB coefficients and sizing the output feature. 
 * 
 * @author wangshiq
 *
 */

public class MFFBFeatureExtractor extends AbstractCachingWindowProcessor<IDataWindow<double[]>, IFeature<double[]>>  implements IDoubleFeatureExtractor {

	
	
	private static final long serialVersionUID = 4048791630967304489L;
	protected final int numBands;
	protected final int minFreq;  //in Hz
	protected final int maxFreq;
	protected final boolean normalize;
	protected final boolean useLog;
	protected final double targetSamplingRate;
	protected final int maxFinalCoefficients;
	


	public final static int DEFAULT_NUM_BANDS = 40;
	public final static int DEFAULT_MIN_FREQ = 5;
	public final static int DEFAULT_MAX_FREQ = 20000;
	public final static boolean DEFAULT_NORMALIZE = false;
	public final static boolean DEFAULT_USE_LOG = false;
	
	public final static int DEFAULT_RESAMPLING_RATE = ExtendedFFT.DEFAULT_FFT_MAX_SAMPLING_RATE;
	
	/**
	 * A convenience on {@link #MFFBFeatureExtractor(double, int, int, int, boolean, boolean)} that uses the default
	 * target sampling rate as defined in ExtendedFFT.
	 */
	public MFFBFeatureExtractor(int numBands, int minFreq, int maxFreq, boolean normalize, boolean useLog) {
		this(MFFBFeatureExtractor.DEFAULT_RESAMPLING_RATE,numBands,minFreq,maxFreq,normalize,useLog);
	}

	/**
	 * A convenience on {@link #MFFBFeatureExtractor(double, int, int, int, boolean, boolean, int)} that does not alter the feature length (i.e. sets maxFinalCoefs=0).
	 */
	public MFFBFeatureExtractor(double targetSamplingRate, int numBands, int minFreq, int maxFreq, boolean normalize, boolean useLog) {
		this(targetSamplingRate, numBands, minFreq, maxFreq, normalize, useLog, 0);
	}

	/**
	 * Constructor for Mel-frequency filter bank feature extractor.
	 * The feature can be restricted to a number of features within a given frequency range.
	 * @param numBands defines the number of bands used in the Triangle filter.  
	 * THe base feature produced covers the frequency range with this number of values.  
	 * The feature length ultimately produces can be further controlled by minFinalCoefs parameter.
	 * @param minFreq the minimum frequency present in the output feature. 
	 * @param maxFreq the maximum frequency present in the output feature. 
	 * @param normalize Specifies whether normalization is applied to the output of triangle filter.  Normalization involves scaling each feature value such that the
	 * sum of all features equals 1.
	 * @param useLog Specifies whether log is applied to the output of triangle filter and normalization (if enabled)
	 * @param maxFinalCoefs may be use to truncate the base feature vector computed using numBands.  
	 * if 0, then the feature length is the number of bands.  
	 * If larger than 0, then the feature vector will be padded as necessary to the given length.
	 */
	public MFFBFeatureExtractor(double targetSamplingRate, int numBands, int minFreq, int maxFreq, boolean normalize, boolean useLog, int maxFinalCoefs) {
		super();
		if (numBands <= 0)
			throw new IllegalArgumentException("Number of bands must be greater than 0");
		if (minFreq < 0)
			throw new IllegalArgumentException("Min frequency must be non-negative");
		if (maxFreq < 0)
			throw new IllegalArgumentException("Max frequency must be non-negative");
		if (minFreq >= maxFreq)
			throw new IllegalArgumentException("Minimum frequency must be smaller than the maximum frequency");
			
		this.numBands = numBands;
		this.minFreq = minFreq;
		this.maxFreq = maxFreq;
		this.normalize = normalize;
		this.useLog = useLog;
		this.targetSamplingRate = targetSamplingRate;
		this.maxFinalCoefficients = maxFinalCoefs;
	}
	
	/**
	 * Constructor for Mel-frequency filter bank feature extractor using {@link #DEFAULT_NORMALIZE} and {@link #DEFAULT_USE_LOG}. 
	 * @param numBands
	 * @param minFreq
	 * @param maxFreq
	 * @see {@link #DEFAULT_NORMALIZE}, {@link #DEFAULT_USE_LOG}.
	 */
	public MFFBFeatureExtractor(int numBands, int minFreq, int maxFreq) {
		this(numBands, minFreq, maxFreq, DEFAULT_NORMALIZE, DEFAULT_USE_LOG);
	}
	
	/**
	 * Constructor for Mel-frequency filter bank feature extractor using all the defaults.
	 * @see {@link #DEFAULT_NUM_BANDS}, {@link #DEFAULT_MIN_FREQ}, {@link #DEFAULT_MAX_FREQ}, {@link #DEFAULT_NORMALIZE}, {@link #DEFAULT_USE_LOG}.
	 */
	public MFFBFeatureExtractor() {
		this(DEFAULT_NUM_BANDS, DEFAULT_MIN_FREQ, DEFAULT_MAX_FREQ, DEFAULT_NORMALIZE, DEFAULT_USE_LOG);
	}
	
	/**
	 * Constructor for Mel-frequency filter bank feature extractor using {@link #DEFAULT_NUM_BANDS}, {@link #DEFAULT_MIN_FREQ} and {@link #DEFAULT_MAX_FREQ}.
	 * @param normalize Specifies whether normalization is applied to the output of triangle filter 
	 * @param useLog Specifies whether log is applied to the output of triangle filter and normalization (if enabled)
	 * @see {@link #DEFAULT_NUM_BANDS}, {@link #DEFAULT_MIN_FREQ}, {@link #DEFAULT_MAX_FREQ}
	 */
	public MFFBFeatureExtractor(boolean normalize, boolean useLog) {
		this(DEFAULT_NUM_BANDS, DEFAULT_MIN_FREQ, DEFAULT_MAX_FREQ, normalize, useLog);
	}

	/**
	 * Constructor for Mel-frequency filter bank feature extractor using {@link #DEFAULT_MIN_FREQ}, {@link #DEFAULT_MAX_FREQ} .
	 * @param numBands
	 * @param normalize Specifies whether normalization is applied to the output of triangle filter 
	 * @param useLog Specifies whether log is applied to the output of triangle filter and normalization (if enabled)
	 * @see {@link #DEFAULT_MIN_FREQ}, {@link #DEFAULT_MAX_FREQ} 
	 */
	public MFFBFeatureExtractor(int numBands, boolean normalize, boolean useLog) {
		this(numBands, DEFAULT_MIN_FREQ, DEFAULT_MAX_FREQ, normalize, useLog);
	}
	
	@Override
	protected IFeature<double[]> applyImpl(IDataWindow<double[]> window) {
		if (!(window instanceof IDataWindow))
			throw new IllegalArgumentException("window must be an instance of " + IDataWindow.class.getName());

		Signal2D featureData = computeMFFB((IDataWindow)window, targetSamplingRate, numBands, minFreq, maxFreq, normalize,  useLog, ExtendedFFT.MAX_PEAK_TO_NOISE_FLOOR_RATIO);
		
		IFeature<double[]> feature = buildFeature(window, featureData, this.maxFinalCoefficients);
		return feature;

//		IDataWindow<double[]> recording = (IDataWindow<double[]>) window;
//        Signal2D triangle = TriangleFilter.filter(recording, targetSamplingRate, numBands, minFreq, maxFreq);
//        Vector trigFreq = triangle.getXValues(); 
//        double[] trigPower = triangle.getYValues(); 
//
//        if(this.normalize) {
//        	VectorUtils.normalize(trigPower);
//        } 
//
//        if (this.useLog) {
//			for(int i=0; i<numBands; i++) {
//				trigPower[i] = Math.log10(Math.max(1e-50, trigPower[i]));  //Do not allow zero values for log operation
//			}
//        }
//        
//		DoubleFeature feature = new DoubleFeature(recording.getStartTimeMsec(), recording.getEndTimeMsec(), trigFreq, trigPower);
//		return feature;
	}
	
	/**
	 * Compute the LogMel.  Broken out so we can share code with MFCCFeatureExtractor. 
	 * @param recording
	 * @param targetSamplingRate
	 * @param numBands
	 * @param minFreq
	 * @param maxFreq
	 * @param normalize by setting the sum of all coefficients to 1.
	 * @param useLog take the log10 of all coeffecients.
	 * @return nver null
	 * @throws IllegalArgumentException
	 */
	protected static Signal2D computeMFFB(IDataWindow<double[]> recording, double targetSamplingRate, int numBands, int minFreq, int maxFreq, 
			boolean normalize, boolean useLog, double maxPeakToNoiseRation) throws IllegalArgumentException {
		if (recording.getData().length == 0)
			throw new IllegalArgumentException("data array can not be zero length");
	    Signal2D triangle = TriangleFilter.filter(recording, targetSamplingRate, numBands, minFreq, maxFreq, maxPeakToNoiseRation);
	    
	    double[] trigPower = triangle.getYValues(); 
		
        if(normalize) {
        	VectorUtils.normalize(trigPower);
        } 

        if (useLog) {
			for(int i=0; i<trigPower.length; i++) {
				trigPower[i] = Math.log10(Math.max(1e-50, trigPower[i]));  //Do not allow zero values for log operation
			}
        }
        return triangle; 
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + maxFinalCoefficients;
		result = prime * result + maxFreq;
		result = prime * result + minFreq;
		result = prime * result + (normalize ? 1231 : 1237);
		result = prime * result + numBands;
		long temp;
		temp = Double.doubleToLongBits(targetSamplingRate);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (useLog ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof MFFBFeatureExtractor))
			return false;
		MFFBFeatureExtractor other = (MFFBFeatureExtractor) obj;
		if (maxFinalCoefficients != other.maxFinalCoefficients)
			return false;
		if (maxFreq != other.maxFreq)
			return false;
		if (minFreq != other.minFreq)
			return false;
		if (normalize != other.normalize)
			return false;
		if (numBands != other.numBands)
			return false;
		if (Double.doubleToLongBits(targetSamplingRate) != Double.doubleToLongBits(other.targetSamplingRate))
			return false;
		if (useLog != other.useLog)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MFFBFeatureExtractor [numBands=" + numBands + ", minFreq=" + minFreq + ", maxFreq=" + maxFreq
				+ ", normalize=" + normalize + ", useLog=" + useLog + ", targetSamplingRate=" + targetSamplingRate
				+ ", maxFinalCoefficients=" + maxFinalCoefficients + "]";
	}

	/**
	 * Create a feature of from the given vector of doubles.
	 * @param recording
	 * @param featureData
	 * @param maxLength if larger than 0, then specifies the length of the data array in the returned feature.
	 * If less than the given feature vector length, then pad with 0s.
	 * @return a feature with the start/stop times from the givne window and data set from the given feature vector.
	 */
	public static IFeature<double[]> buildFeature(IDataWindow<double[]> recording, Signal2D feature, int maxLength) {
		//Truncate if resulting array is longer than maxFinalCoeffs
		int len = feature.size();
		if (maxLength > 0 && len != maxLength ) {
			if (len > maxLength)
				feature = feature.trim(0, maxLength);
			else
				feature = feature.pad(maxLength,0);
//			AISPLogger.logger.info("1) featureLen= " + dctPower.length);
		}
//		Vector rv = new Vector(0,1,dctPower.length);
		double[] featureData = feature.getYValues();
//		double deltaMsec = recording.getEndTimeMsec() - recording.getStartTimeMsec();
//		Vector rv = new Vector(recording.getStartTimeMsec(),deltaMsec/(featureData.length-1),featureData.length);
		Vector rv = feature.getXValues();
		DoubleFeature df = new DoubleFeature(recording.getStartTimeMsec(), recording.getEndTimeMsec(), rv, featureData);

//		double[] dctFreq = new double[dctPower.length];
//		for(int i=0; i<dctPower.length; i++) {
//			dctFreq[i] = i;
//		}
//		DoubleFeature feature = new DoubleFeature(recording.getStartTimeMsec(), recording.getEndTimeMsec(), dctFreq, dctPower);
		
//		DoubleFeature feature = new DoubleFeature(dctFreq[0], dctFreq[dctFreq.length-1], dctFreq, dctPower);	
		
		return df;
	}


}