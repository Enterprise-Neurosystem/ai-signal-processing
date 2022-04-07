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

public class CosineDistanceMergeKNNClassifier extends KNNClassifier {
	
	private static final long serialVersionUID = 52996854319273447L;

	public CosineDistanceMergeKNNClassifier(double stddevFactor, boolean enableOutlierDetection, int maxListSize, boolean normalizeWhenMerging) {
		this(DEFAULT_WINDOW_SIZE_MSEC,DEFAULT_WINDOW_SHIFT_MSEC, stddevFactor, enableOutlierDetection, maxListSize, normalizeWhenMerging);
	}

	public CosineDistanceMergeKNNClassifier(ITrainingWindowTransform<double[]> transforms, 
			IFeatureExtractor<double[], double[]> extractor,
			int windowSizeMsec,
			int windowShiftMsec, IFeatureProcessor<double[]> featureProcessor, double stddevFactor, boolean enableOutlierDetection, int maxListSize,
			boolean normalizeFeatures,
			boolean normalizeWhenMerging) {
		super(transforms, extractor, windowSizeMsec,  windowShiftMsec, featureProcessor,new CosineDistanceMergeKNNFunc(normalizeWhenMerging), stddevFactor, 
				enableOutlierDetection, maxListSize, normalizeFeatures);
	}

	public CosineDistanceMergeKNNClassifier(ITrainingWindowTransform<double[]> transforms, 
			IFeatureGramDescriptor<double[], double[]> extractor,
			double stddevFactor, boolean enableOutlierDetection, int maxListSize,
			boolean normalizeFeatures,
			boolean normalizeWhenMerging) {
		super(transforms, extractor, new CosineDistanceMergeKNNFunc(normalizeWhenMerging), stddevFactor, 
				enableOutlierDetection, maxListSize, normalizeFeatures);
	}

	public CosineDistanceMergeKNNClassifier(int windowSizeMsec, int windowShiftMsec, double stddevFactor, boolean enableOutlierDetection, int maxListSize, boolean normalizeWhenMerging) {
		this(DEFAULT_TRANSFORMS, DEFAULT_FEATURE_EXTRACTOR, windowSizeMsec, windowShiftMsec, null, stddevFactor, enableOutlierDetection, maxListSize, 
				DEFAULT_NORMALIZE_FEATURES, normalizeWhenMerging);
	}

}
