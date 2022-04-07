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

import org.eng.aisp.classifier.AbstractClassifierBuilder;
import org.eng.aisp.classifier.IFixableClassifierBuilder;
import org.eng.aisp.feature.IFeatureGramDescriptor;

public class KNNClassifierBuilder extends AbstractClassifierBuilder<double[], double[]>  implements IFixableClassifierBuilder<double[], double[]> { 

	private static final long serialVersionUID = -2671464831380630698L;
	protected double stdDevFactor = KNNClassifier.DEFAULT_STDDEV_FACTOR;
	protected int maxListSize = KNNClassifier.DEFAULT_MAX_LIST_SIZE;
	protected boolean enableOutlierDetection = KNNClassifier.DEFAULT_ENABLE_OUTLIER_DETECTION;
	protected boolean normalizeFeatures = KNNClassifier.DEFAULT_NORMALIZE_FEATURES;
	protected INearestNeighborFunction<double[]> knnFunc = KNNClassifier.DEFAULT_KNN_FUNCTION;

	protected KNNClassifierBuilder() { 
		super(KNNClassifier.DEFAULT_FEATURE_EXTRACTOR, KNNClassifier.DEFAULT_FEATURE_PROCESSOR);
		this.setWindowSizeMsec(KNNClassifier.DEFAULT_WINDOW_SIZE_MSEC);
		this.setWindowShiftMsec(KNNClassifier.DEFAULT_WINDOW_SHIFT_MSEC);
		this.setTransform(KNNClassifier.DEFAULT_TRANSFORMS);
	}
	
	@Override
	public KNNClassifier build() {
		List<IFeatureGramDescriptor<double[],double[]>> fgeList = this.getFeatureGramExtractors();
		if (fgeList.size() > 1)
			throw new IllegalArgumentException("Only one feature gram extractor is supported");
		return new KNNClassifier(this.getTransform(),
				this.getFeatureGramExtractors().get(0),
				this.knnFunc, this.stdDevFactor, this.enableOutlierDetection, this.maxListSize, this.normalizeFeatures) ;
	}
	
	public KNNClassifierBuilder setStdDevFactor(double stdDevFactor) {
		//We do not allow one to set stdDevFactor when enableOutlierDetection is false. This avoids errors during classifier configuration.
		if (!enableOutlierDetection) throw new IllegalArgumentException("Attempting to set stdDevFactor with enableOutlierDetection==false.");
		this.stdDevFactor = stdDevFactor;
		return this; 
	}
	
	public KNNClassifierBuilder setMaxListSize(int maxListSize) {
		this.maxListSize = maxListSize;
		return this; 
	}

	/**
	 * Turn on/off feature normalization.  This is generally only used when the elements of the feature
	 * vector use different units. This is often the case when importing features directly and using the IdentityFeatureGramDescriptor.
	 * @param normalizeFeatures the normalizeFeatures to set
	 */
	public KNNClassifierBuilder setNormalizeFeatures(boolean normalizeFeatures) {
		this.normalizeFeatures = normalizeFeatures;
		return this;
	}
	
	public KNNClassifierBuilder setEnableOutlierDetection(boolean enableOutlierDetection) {
		this.enableOutlierDetection = enableOutlierDetection;
		return this;
	}

	public KNNClassifierBuilder setKnnFunc(INearestNeighborFunction<double[]> knnFunc) {
		this.knnFunc = knnFunc;
		return this;
	}
	
}
