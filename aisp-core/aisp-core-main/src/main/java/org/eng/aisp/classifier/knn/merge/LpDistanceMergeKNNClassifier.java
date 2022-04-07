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
package org.eng.aisp.classifier.knn.merge;

import org.eng.aisp.classifier.knn.KNNClassifier;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.transform.ITrainingWindowTransform;

public class LpDistanceMergeKNNClassifier extends KNNClassifier {
	
	private static final long serialVersionUID = 52996854319273447L;
	public static final double DEFAULT_P = LpDistanceMergeKNNFunc.DEFAULT_P;

	public LpDistanceMergeKNNClassifier() {
		this(DEFAULT_ENABLE_OUTLIER_DETECTION);
	}

	public LpDistanceMergeKNNClassifier(boolean enableOutlierDetection) {
		this(DEFAULT_STDDEV_FACTOR, enableOutlierDetection, DEFAULT_P, DEFAULT_MAX_LIST_SIZE, DEFAULT_NORMALIZE_WHEN_MERGING);
	}

	public LpDistanceMergeKNNClassifier(double p) {
		this(DEFAULT_STDDEV_FACTOR, DEFAULT_ENABLE_OUTLIER_DETECTION, p, DEFAULT_MAX_LIST_SIZE, DEFAULT_NORMALIZE_WHEN_MERGING);
	}
	
	public LpDistanceMergeKNNClassifier(IFeatureExtractor<double[], double[]> extractor, IFeatureProcessor<double[] > processor, double p) {
		this(DEFAULT_TRANSFORMS,extractor, DEFAULT_WINDOW_SIZE_MSEC, DEFAULT_WINDOW_SHIFT_MSEC,  processor, DEFAULT_STDDEV_FACTOR, 
				DEFAULT_ENABLE_OUTLIER_DETECTION, p, DEFAULT_MAX_LIST_SIZE, DEFAULT_NORMALIZE_FEATURES, DEFAULT_NORMALIZE_WHEN_MERGING);
	}
	
	public LpDistanceMergeKNNClassifier(IFeatureExtractor<double[], double[]> extractor, int windowSizeMsec,
			int windowShiftMsec, IFeatureProcessor<double[]> processor, double p) {
		this(DEFAULT_TRANSFORMS,extractor, windowSizeMsec, windowShiftMsec,  processor, DEFAULT_STDDEV_FACTOR, 
				DEFAULT_ENABLE_OUTLIER_DETECTION, p, DEFAULT_MAX_LIST_SIZE, DEFAULT_NORMALIZE_FEATURES,DEFAULT_NORMALIZE_WHEN_MERGING);
	}

	public LpDistanceMergeKNNClassifier(double stddevFactor, boolean enableOutlierDetection, double p, int maxListSize, boolean normalizeWhenMerging) {
		this(DEFAULT_WINDOW_SIZE_MSEC,DEFAULT_WINDOW_SHIFT_MSEC, stddevFactor, enableOutlierDetection, p, maxListSize, normalizeWhenMerging);
	}

	/**
	 * @param transforms
	 * @param extractor
	 * @param windowSizeMsec
	 * @param windowShiftMsec
	 * @param featureProcessor
	 * @param stddevFactor
	 * @param enableOutlierDetection
	 * @param p
	 * @param maxListSize
	 * @param normalizeWhenMerging
	 * @deprecated in favor of {@link #LpDistanceMergeKNNClassifier(ITrainingWindowTransform, IFeatureGramDescriptor, double, boolean, double, int, boolean)}.
	 */
	private LpDistanceMergeKNNClassifier(ITrainingWindowTransform<double[]> transforms, 
			IFeatureExtractor<double[], double[]> extractor, 
			int windowSizeMsec,
			int windowShiftMsec, IFeatureProcessor<double[]> featureProcessor, double stddevFactor, boolean enableOutlierDetection,
			double p, int maxListSize, boolean normalizeFeatures, boolean normalizeWhenMerging) {
		super(transforms, extractor, windowSizeMsec,  windowShiftMsec, featureProcessor,new LpDistanceMergeKNNFunc(p, normalizeWhenMerging), stddevFactor, 
				enableOutlierDetection, maxListSize, normalizeFeatures);
	}

	/**
	 * 
	 * @param transform
	 * @param fge if null, then use the default as defined by the super class.
	 * @param stddevFactor
	 * @param enableOutlierDetection
	 * @param p
	 * @param maxListSize
	 * @param normalizeWhenMerging
	 */
	public LpDistanceMergeKNNClassifier(ITrainingWindowTransform<double[]> transform, 
			IFeatureGramDescriptor<double[], double[]> fge, double stddevFactor, boolean enableOutlierDetection,
			int maxListSize, boolean normalizeFeatures, double p, boolean normalizeWhenMerging) {
		super(transform, fge, new LpDistanceMergeKNNFunc(p, normalizeWhenMerging), stddevFactor, enableOutlierDetection, maxListSize, normalizeFeatures);
	}

	public LpDistanceMergeKNNClassifier(IFeatureGramDescriptor<double[], double[]> fge, boolean enableOutlierDetection) {
		super(null, fge, new LpDistanceMergeKNNFunc(DEFAULT_P, DEFAULT_NORMALIZE_WHEN_MERGING), DEFAULT_STDDEV_FACTOR, enableOutlierDetection, DEFAULT_MAX_LIST_SIZE, DEFAULT_NORMALIZE_FEATURES);
	}
	

	/**
	 * 
	 * @param windowSizeMsec
	 * @param windowShiftMsec
	 * @param stddevFactor
	 * @param enableOutlierDetection
	 * @param p parameter in Minkowski distance,  p=1 gives Manahattan distanc, p=2 givens Euclidian.  Use p &ge; 1 to maintain the triangle inequality.
	 * @param maxListSize
	 * @param normalizeWhenMerging
	 */
	public LpDistanceMergeKNNClassifier(int windowSizeMsec, int windowShiftMsec, double stddevFactor, boolean enableOutlierDetection, 
			double p, int maxListSize, boolean normalizeWhenMerging) {
		this(DEFAULT_TRANSFORMS,DEFAULT_FEATURE_EXTRACTOR, windowSizeMsec, windowShiftMsec, DEFAULT_FEATURE_PROCESSOR, stddevFactor, enableOutlierDetection, p, maxListSize, 
				DEFAULT_NORMALIZE_FEATURES, normalizeWhenMerging);
	}

}
