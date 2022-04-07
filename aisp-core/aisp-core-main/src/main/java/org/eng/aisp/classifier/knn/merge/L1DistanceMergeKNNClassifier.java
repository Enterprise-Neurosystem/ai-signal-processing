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

public class L1DistanceMergeKNNClassifier extends KNNClassifier  {

	private static final long serialVersionUID = 932242502217072522L;
	
	public L1DistanceMergeKNNClassifier() {
		this(DEFAULT_ENABLE_OUTLIER_DETECTION);
	}
	
	public L1DistanceMergeKNNClassifier(boolean enableOutlierDetection) {
		this(DEFAULT_WINDOW_SIZE_MSEC,DEFAULT_WINDOW_SHIFT_MSEC, DEFAULT_STDDEV_FACTOR, enableOutlierDetection, DEFAULT_MAX_LIST_SIZE, DEFAULT_NORMALIZE_WHEN_MERGING);
	}

	public L1DistanceMergeKNNClassifier(IFeatureExtractor<double[],double[]>  extractor, int windowSizeMsec, int windowShiftMsec, IFeatureProcessor<double[]> processor) {
		this(DEFAULT_TRANSFORMS,extractor, windowSizeMsec, windowShiftMsec, processor, DEFAULT_STDDEV_FACTOR, DEFAULT_ENABLE_OUTLIER_DETECTION, DEFAULT_MAX_LIST_SIZE, 
				DEFAULT_NORMALIZE_FEATURES, DEFAULT_NORMALIZE_WHEN_MERGING);
	}
	

	public L1DistanceMergeKNNClassifier(ITrainingWindowTransform<double[]> transform, IFeatureGramDescriptor<double[],double[]> featureGramExtractor , double stddevFactor, 
			boolean enableOutlierDetection, int maxListSize, boolean normalizeFeatures, boolean normalizeWhenMerging) {
		super(transform, featureGramExtractor, new L1DistanceMergeKNNFunc(normalizeWhenMerging), stddevFactor, enableOutlierDetection, 
				maxListSize, normalizeFeatures);
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
	public L1DistanceMergeKNNClassifier(int windowSizeMsec, int windowShiftMsec, double stddevFactor,
			 boolean enableOutlierDetection, int maxListSize, boolean normalizeWhenMerging) {
		this(DEFAULT_TRANSFORMS,DEFAULT_FEATURE_EXTRACTOR, windowSizeMsec, windowShiftMsec, DEFAULT_FEATURE_PROCESSOR, stddevFactor, enableOutlierDetection, 
				maxListSize, DEFAULT_NORMALIZE_FEATURES, normalizeWhenMerging);
	}


	/**
	 * 
	 * @param extractor
	 * @param windowSizeMsec size of subwindows on which to extract features.  If 0, then subwindows will not be used and a single feature is extracted on the whole window.
	 * @param windowShiftMsec only used if windowSizeMsec is positive.  Is the amount of time to shift the starting point of subwindows.
	 * @param stddevFactor
	 * @param enableOutlierDetection
	 * @param maxListSize
	 * @param normalizeWhenMerging
	 */
	public L1DistanceMergeKNNClassifier(ITrainingWindowTransform<double[]> transforms, 
			IFeatureExtractor<double[], double[]> extractor,
			int windowSizeMsec,
			int windowShiftMsec, IFeatureProcessor<double[]> featureProcessor, double stddevFactor, boolean enableOutlierDetection, int maxListSize,
			boolean normalizeFeatures, 
			boolean normalizeWhenMerging) {
		super(transforms, extractor, windowSizeMsec, windowShiftMsec, featureProcessor, new L1DistanceMergeKNNFunc(normalizeWhenMerging), stddevFactor, enableOutlierDetection, 
				maxListSize, normalizeFeatures);
	}



}
