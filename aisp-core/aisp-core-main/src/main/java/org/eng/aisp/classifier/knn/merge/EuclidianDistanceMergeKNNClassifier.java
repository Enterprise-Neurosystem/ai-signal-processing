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

public class EuclidianDistanceMergeKNNClassifier extends KNNClassifier {

	private static final long serialVersionUID = -7205252258539964233L;

	
	/**
	 * Uses an FFTFeatureExtractor.
	 * @param windowSizeMsec
	 * @param windowShiftMsec
	 * @param stddevFactor
	 * @param enableOutlierDetection
	 * @param maxListSize
	 * @param normalizeWhenMerging
	 */
	public EuclidianDistanceMergeKNNClassifier(ITrainingWindowTransform<double[]> transforms,
			IFeatureExtractor<double[], double[]> extractor,
			int windowSizeMsec, int windowShiftMsec, IFeatureProcessor<double[]> featureProcessor,
			double stddevFactor, boolean enableOutlierDetection, int maxListSize, boolean normalizeFeatures, boolean normalizeWhenMerging) {
		super(transforms, extractor, windowSizeMsec, windowShiftMsec, featureProcessor, new EuclidianDistanceMergeKNNFunc(normalizeWhenMerging), stddevFactor, 
				enableOutlierDetection, maxListSize, normalizeFeatures);
	}

	public EuclidianDistanceMergeKNNClassifier(ITrainingWindowTransform<double[]> transforms,
			IFeatureGramDescriptor<double[], double[]> extractor,
			double stddevFactor, boolean enableOutlierDetection, int maxListSize, boolean normalizeFeatures, boolean normalizeWhenMerging) {
		super(transforms, extractor, new EuclidianDistanceMergeKNNFunc(normalizeWhenMerging), stddevFactor, 
				enableOutlierDetection, maxListSize, normalizeFeatures);
	}	
	
	/**
	 * Uses an MFCCFeatureExtractor with rolling windows of 500 msec.
	 * @param stddevFactor
	 * @param enableOutlierDetection
	 * @param maxListSize
	 * @param normalizeWhenMerging
	 */
	public EuclidianDistanceMergeKNNClassifier(double stddevFactor, boolean enableOutlierDetection, int maxListSize, boolean normalizeWhenMerging) {
		this(DEFAULT_WINDOW_SIZE_MSEC,DEFAULT_WINDOW_SHIFT_MSEC, stddevFactor, enableOutlierDetection, maxListSize, normalizeWhenMerging);
	}

	public EuclidianDistanceMergeKNNClassifier(IFeatureExtractor<double[], double[]> extractor) { 
		this(DEFAULT_TRANSFORMS, extractor, DEFAULT_WINDOW_SIZE_MSEC, DEFAULT_WINDOW_SHIFT_MSEC, null, DEFAULT_STDDEV_FACTOR, DEFAULT_ENABLE_OUTLIER_DETECTION, 
				DEFAULT_MAX_LIST_SIZE, DEFAULT_NORMALIZE_FEATURES, DEFAULT_NORMALIZE_WHEN_MERGING);
	}
	
	/**
	 * Use all defaults for window size, processor, etc and an MFCCFeatureExtractor().
	 */
	public EuclidianDistanceMergeKNNClassifier() {
		this(DEFAULT_ENABLE_OUTLIER_DETECTION);
	}
	
	/**
	 * Use all defaults for window size, processor, etc and an MFCCFeatureExtractor().
	 * @param enableOutlierDetection
	 */
	public EuclidianDistanceMergeKNNClassifier(boolean enableOutlierDetection) {
		this(DEFAULT_TRANSFORMS, DEFAULT_FEATURE_EXTRACTOR,DEFAULT_WINDOW_SIZE_MSEC, DEFAULT_WINDOW_SHIFT_MSEC, DEFAULT_FEATURE_PROCESSOR, DEFAULT_STDDEV_FACTOR, 
				enableOutlierDetection, DEFAULT_MAX_LIST_SIZE,  DEFAULT_NORMALIZE_FEATURES, DEFAULT_NORMALIZE_WHEN_MERGING);
	}
	
	/**
	 * Uses an MFCCFeatureExtractor.
	 * @param windowSizeMsec
	 * @param windowShiftMsec
	 * @param stddevFactor
	 * @param enableOutlierDetection
	 * @param maxListSize
	 * @param normalizeWhenMerging
	 */
	public EuclidianDistanceMergeKNNClassifier(int windowSizeMsec, int windowShiftMsec, double stddevFactor, boolean enableOutlierDetection, int maxListSize, boolean normalizeWhenMerging) {
		this(DEFAULT_TRANSFORMS,DEFAULT_FEATURE_EXTRACTOR,windowSizeMsec, windowShiftMsec, DEFAULT_FEATURE_PROCESSOR, stddevFactor, enableOutlierDetection, maxListSize, 
				DEFAULT_NORMALIZE_FEATURES, normalizeWhenMerging);
	}

	public EuclidianDistanceMergeKNNClassifier(IFeatureExtractor<double[], double[]> extractor, double stddevFactor, 
			boolean enableOutlierDetection, int maxListSize, boolean normalizeWhenMerging) {
		super(DEFAULT_TRANSFORMS, extractor,DEFAULT_WINDOW_SIZE_MSEC, DEFAULT_WINDOW_SHIFT_MSEC, null, new EuclidianDistanceMergeKNNFunc(normalizeWhenMerging), 
				stddevFactor, enableOutlierDetection, maxListSize,  DEFAULT_NORMALIZE_FEATURES);
	}



}
