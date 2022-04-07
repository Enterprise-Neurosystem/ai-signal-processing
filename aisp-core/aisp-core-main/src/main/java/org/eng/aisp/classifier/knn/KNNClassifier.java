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
package org.eng.aisp.classifier.knn;

import java.util.List;
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPProperties;
import org.eng.aisp.classifier.AbstractFixableFeatureExtractingUpdatableClassifier;
import org.eng.aisp.classifier.IFixableUpdatableClassifier;
import org.eng.aisp.classifier.knn.merge.LpDistanceMergeKNNFunc;
import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.FeatureGramNormalizer;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.MFCCFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.transform.ITrainingWindowTransform;
import org.eng.util.MutatingIterable;

/**
 * Extends the super class to implement a nearest neighbor classifier using various distance metrics.
 * Neighborhoods are identified by clusters of features.
 * @author wangshiq
 * @author dawood
 *
 */
public class KNNClassifier extends AbstractFixableFeatureExtractingUpdatableClassifier<double[], double[]> implements IFixableUpdatableClassifier<double[]> {
	private static final long serialVersionUID = 1064082090375318093L;
	
	/** Name of caa.properties property (or system property) that defines the maximum number of lists of labeled features */
	public final static String DEFAULT_MAX_LIST_SIZE_PROPERTY_NAME = "classifiers.nn.max_list_size";
	/** Name of caa.properties property (or system property) that defines the boundary outside of which something will be considered unlabeled */ 
	public final static String DEFAULT_STDDEV_FACTOR_PROPERTY_NAME = "classifiers.nn.stddev_factor";
	/** Name of caa.properties property (or system property) that defines the reduction ratio of feature vectors when exceeding the maximum size of list of labeled features */ 
	public final static String DEFAULT_REDUCTION_RATIO_PROPERTY_NAME = "classifiers.nn.reduction_ratio";

	public final static int DEFAULT_MAX_LIST_SIZE =    AISPProperties.instance().getProperty(DEFAULT_MAX_LIST_SIZE_PROPERTY_NAME,1000);
	public final static double DEFAULT_STDDEV_FACTOR = AISPProperties.instance().getProperty(DEFAULT_STDDEV_FACTOR_PROPERTY_NAME, 3.0);
	public final static double DEFAULT_REDUCTION_RATIO = AISPProperties.instance().getProperty(DEFAULT_REDUCTION_RATIO_PROPERTY_NAME, 0.5);
	public final static IFeatureExtractor<double[], double[]> DEFAULT_FEATURE_EXTRACTOR = new MFCCFeatureExtractor(40); 
	public final static ITrainingWindowTransform<double[]> DEFAULT_TRANSFORMS = null; 
	public final static IFeatureProcessor<double[]> DEFAULT_FEATURE_PROCESSOR = null; 

	public final static boolean DEFAULT_NORMALIZE_FEATURES = false;

	public final static IFeatureGramDescriptor<double[], double[]> DEFAULT_FEATUREGRAM_EXTRACTOR = new FeatureGramDescriptor<double[],double[]>(
						DEFAULT_WINDOW_SIZE_MSEC, DEFAULT_WINDOW_SHIFT_MSEC, DEFAULT_FEATURE_EXTRACTOR, DEFAULT_FEATURE_PROCESSOR);


	public static final boolean DEFAULT_NORMALIZE_WHEN_MERGING = false;
	public static final boolean DEFAULT_ENABLE_OUTLIER_DETECTION = true;
	
	public static final INearestNeighborFunction<double[]> DEFAULT_KNN_FUNCTION = new LpDistanceMergeKNNFunc(DEFAULT_NORMALIZE_FEATURES) ;
	
	protected final double stdDevFactor;
	private double maxDistAmplifyFactor; 
	protected double lowerBoundDelta;
	private final KNNVectorSummarizer dataSummary;
	
	/** If true, the normalize the feature prior to processing */
	protected final boolean normalizeFeatures; 

	private final INearestNeighborFunction<double[]> nnFunc;
	
	/**
	 * 
	 * @param transforms
	 * @param extractor
	 * @param windowSizeMsec
	 * @param windowShiftMsec
	 * @param featureProcessor
	 * @param nnFunc
	 * @param stddevFactor
	 * @param enableOutlierDetection
	 * @param maxListSize
	 * @param normalizeFeatures 
	 * @deprecated in favor of {@link #KNNClassifier(ITrainingWindowTransform, IFeatureGramDescriptor, LpDistanceMergeKNNFunc, double, boolean, int)}.
	 */
	protected KNNClassifier(ITrainingWindowTransform<double[]> transforms, 
			IFeatureExtractor<double[], double[]>  extractor,
			int windowSizeMsec, 
			int windowShiftMsec, IFeatureProcessor<double[]> featureProcessor, INearestNeighborFunction<double[]> nnFunc,
			double stddevFactor, boolean enableOutlierDetection, int maxListSize, boolean normalizeFeatures) {
		this(transforms, new FeatureGramDescriptor(windowSizeMsec, windowShiftMsec, extractor, featureProcessor), 
				nnFunc, stddevFactor, enableOutlierDetection, maxListSize, normalizeFeatures);
	}

	/**
	 * 
	 * @param transforms
	 * @param fge if null, then use the {@link #DEFAULT_FEATUREGRAM_EXTRACTOR} defined in this class.
	 * @param nnFunc
	 * @param stddevFactor
	 * @param enableOutlierDetection
	 * @param maxListSize
	 * @param normalizeFeatures 
	 */
	public KNNClassifier(ITrainingWindowTransform<double[]> transforms,
			IFeatureGramDescriptor<double[], double[]> fge, INearestNeighborFunction<double[]> nnFunc,
			double stddevFactor, boolean enableOutlierDetection, int maxListSize, boolean normalizeFeatures) {
//		// preshuffle training data so that merging is not biased by order of data.
		super(true, transforms, fge == null ? DEFAULT_FEATUREGRAM_EXTRACTOR : fge, false,false);  	
		this.nnFunc = nnFunc;
		this.stdDevFactor = stddevFactor;
		this.maxDistAmplifyFactor=1.0; //fixed for now
//		this.maxListSize = maxListSize;
		dataSummary = new KNNVectorSummarizer(maxListSize, nnFunc, stdDevFactor, 
				enableOutlierDetection, DEFAULT_REDUCTION_RATIO);
		this.normalizeFeatures = normalizeFeatures;
	}

	@Override
	protected FixedKNNClassifier updateFixedClassifierOnFeatures(
			Iterable<? extends ILabeledFeatureGram<double[]>[]> incrementalFeatures) throws AISPException {
		return addFeatures(incrementalFeatures);
	}

	@Override
	protected FixedKNNClassifier trainFixedClassifierOnFeatures(Iterable<? extends ILabeledFeatureGram<double[]>[]> featureWithLabels) throws AISPException {
		dataSummary.reset();
		return addFeatures(featureWithLabels);
	}
	
	private FixedKNNClassifier addFeatures(Iterable<? extends ILabeledFeatureGram<double[]>[]> featureWithLabels) throws AISPException {
//		return addFeaturesStreaming(featureWithLabels);
		return addFeaturesNonStreaming(featureWithLabels);	// This is faster by about 5-10% (looking at 8,16 and 32 minutes of DCASE  2016 data with ModelProfiler)
	}



	private FixedKNNClassifier addFeaturesNonStreaming(Iterable<? extends ILabeledFeatureGram<double[]>[]> featureWithLabels) throws AISPException {
		boolean foundData = false;
		boolean foundLabels = false;
		FeatureGramNormalizer normalizer = null;
		if (normalizeFeatures)  {
			normalizer = new FeatureGramNormalizer(featureWithLabels);
			featureWithLabels = new MutatingIterable<ILabeledFeatureGram<double[]>[], ILabeledFeatureGram<double[]>[]>(
					(Iterable<ILabeledFeatureGram<double[]>[]>) featureWithLabels, normalizer);
		}
		
	
		for(ILabeledFeatureGram<double[]>[] lfga : featureWithLabels) {
			if (lfga.length == 0)
				continue;
			if (lfga.length > 1) 
				throw new IllegalArgumentException("More than one feature gram is not supported");
			//TODO Labels are only read from the first sub window. While this should not cause any problem, there might be a better way to handle this
			Properties labels = lfga[0].getLabels();
			String labelValue = labels.getProperty(this.primaryTrainingLabel);
			if (labelValue == null) 
				continue;
			foundLabels = true;
			IFeature<double[]>[] f = lfga[0].getFeatureGram().getFeatures();
			if (f.length == 0)	// can happen when sound is shorter than subwindow size.
				continue;		
			List<double[]> referenceList = nnFunc.featurePreProcessing(f);

//			StringBuilder sb = new StringBuilder("Feature: " );
// 			for (int i=0 ; i<f[0].getData().length ; i++) {
// 				if (i != 0)
// 					sb.append(',');
// 				sb.append(f[0].getData()[i]);
//			}
//			AISPLogger.logger.info(sb.toString());

			if (referenceList != null) {
				foundData = foundData || referenceList.size() > 0;
				for (double[] reference : referenceList) 
					dataSummary.mergeData(labelValue, reference);
			}
		}
		
		if (!foundLabels) {
//			int count=0;
//			for(ILabeledFeatureGram<double[]>[] lfga : featureWithLabels)  {
//				AISPLogger.logger.info("Labels not found: " + lfga[0].getLabels());
//				count++;
//			}
//			if (count == 0)
//				AISPLogger.logger.info("Zero features!");
			throw new AISPException("Label '" + primaryTrainingLabel + "' was not found in training data");
		}
		if (!foundData)
			throw new AISPException("No feature subwindows found (is subwindow too small?)");
		
		
		KNNDataSummaryClassifier<double[]> newDS = new KNNDataSummaryClassifier<double[]>(this.nnFunc, this.dataSummary.listOfLabeledData,
				dataSummary.getLowerBoundDelta(), dataSummary.getMaxDistBetweenSameLabel(), dataSummary.isEnableOutlierDetection());

		return new FixedKNNClassifier(this.primaryTrainingLabel, featureGramDescriptors,
				this.maxDistAmplifyFactor, this.nnFunc, newDS, normalizer);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return this.getClass().getSimpleName() + "[primaryTrainingLabel=" + primaryTrainingLabel
				+ ", trainingWindowTransform=" + this.trainingWindowTransform 
				+ ", featureGramDescriptors="
				+ (featureGramDescriptors != null
						? featureGramDescriptors.subList(0, Math.min(featureGramDescriptors.size(), maxLen)) : null)
				+ ", classifier=" + classifier + ", nnFunc=" + nnFunc + ", stdDevFactor=" + stdDevFactor
				+ ", maxDistAmplifyFactor=" + maxDistAmplifyFactor + ", lowerBoundDelta=" + lowerBoundDelta
				+ ", dataSummary=" + dataSummary + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((dataSummary == null) ? 0 : dataSummary.hashCode());
		long temp;
		temp = Double.doubleToLongBits(lowerBoundDelta);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(maxDistAmplifyFactor);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((nnFunc == null) ? 0 : nnFunc.hashCode());
		result = prime * result + (normalizeFeatures ? 1231 : 1237);
		temp = Double.doubleToLongBits(stdDevFactor);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof KNNClassifier))
			return false;
		KNNClassifier other = (KNNClassifier) obj;
		if (dataSummary == null) {
			if (other.dataSummary != null)
				return false;
		} else if (!dataSummary.equals(other.dataSummary))
			return false;
		if (Double.doubleToLongBits(lowerBoundDelta) != Double.doubleToLongBits(other.lowerBoundDelta))
			return false;
		if (Double.doubleToLongBits(maxDistAmplifyFactor) != Double.doubleToLongBits(other.maxDistAmplifyFactor))
			return false;
		if (nnFunc == null) {
			if (other.nnFunc != null)
				return false;
		} else if (!nnFunc.equals(other.nnFunc))
			return false;
		if (normalizeFeatures != other.normalizeFeatures)
			return false;
		if (Double.doubleToLongBits(stdDevFactor) != Double.doubleToLongBits(other.stdDevFactor))
			return false;
		return true;
	}




}
