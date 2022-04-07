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

public class LpDistanceMergeKNNClassifierBuilder extends KNNClassifierBuilder {
	private static final long serialVersionUID = 3123184157092842521L;
	private double p = LpDistanceMergeKNNClassifier.DEFAULT_P;
	private boolean normalizeWhenMerging = KNNClassifier.DEFAULT_NORMALIZE_WHEN_MERGING; // false;

	public LpDistanceMergeKNNClassifierBuilder() {
		super();
	}

	/**
	 * Override to create an instance of LpDistanceMegeKNNClassifier.
	 */
	@Override
	public LpDistanceMergeKNNClassifier build() {
		List<IFeatureGramDescriptor<double[],double[]>> fgeList = this.getFeatureGramExtractors();
		if (fgeList.size() > 1)
			throw new IllegalArgumentException("Only one feature gram extractor is supported");
		return new LpDistanceMergeKNNClassifier(transform,fgeList.get(0),stdDevFactor, enableOutlierDetection, maxListSize, normalizeFeatures, p, normalizeWhenMerging);
	}

	public LpDistanceMergeKNNClassifierBuilder setKnnFunc(INearestNeighborFunction<double[]> knnFunc) {
		throw new IllegalArgumentException("Call not allowed.  Set p and normalizeWhenMerging instead.");
	}

	public LpDistanceMergeKNNClassifierBuilder setParamP(double p) {
		this.p = p;
		return this;
	}
	
	public LpDistanceMergeKNNClassifierBuilder setNormalizeWhenMerging(boolean normalizeWhenMerging) {
		this.normalizeWhenMerging = normalizeWhenMerging;
		return this;
	}


}
