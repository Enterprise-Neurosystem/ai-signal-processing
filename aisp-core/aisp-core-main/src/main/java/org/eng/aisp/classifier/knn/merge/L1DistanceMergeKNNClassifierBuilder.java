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

import java.util.List;

import org.eng.aisp.classifier.knn.INearestNeighborFunction;
import org.eng.aisp.classifier.knn.KNNClassifier;
import org.eng.aisp.classifier.knn.KNNClassifierBuilder;
import org.eng.aisp.feature.IFeatureGramDescriptor;

public class L1DistanceMergeKNNClassifierBuilder extends KNNClassifierBuilder {
	private static final long serialVersionUID = 7523900272923731079L;
	private boolean normalizeWhenMerging = KNNClassifier.DEFAULT_NORMALIZE_WHEN_MERGING;


	public L1DistanceMergeKNNClassifierBuilder() {
		super();
	}


	/**
	 * Override to create an instance of L1DistanceMergeKNNClassifier.
	 */
	@Override
	public L1DistanceMergeKNNClassifier build() {
		List<IFeatureGramDescriptor<double[],double[]>> fgeList = this.getFeatureGramExtractors();
		if (fgeList.size() > 1)
			throw new IllegalArgumentException("Only one feature gram extractor is supported");
		return new L1DistanceMergeKNNClassifier(transform,fgeList.get(0),stdDevFactor, enableOutlierDetection, maxListSize, normalizeFeatures, normalizeWhenMerging);
	}

	public L1DistanceMergeKNNClassifierBuilder setKnnFunc(INearestNeighborFunction<double[]> knnFunc) {
		throw new IllegalArgumentException("Call not allowed.  Set p and normalizeWhenMerging instead.");
	}

	public L1DistanceMergeKNNClassifierBuilder setNormalizeWhenMerging(boolean normalizeWhenMerging) {
		this.normalizeWhenMerging = normalizeWhenMerging;
		return this;
	}

}
