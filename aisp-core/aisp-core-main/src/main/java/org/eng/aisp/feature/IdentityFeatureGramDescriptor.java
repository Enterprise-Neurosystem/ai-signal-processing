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
package org.eng.aisp.feature;

import org.eng.aisp.feature.extractor.vector.IdentityFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;

/**
 * A feature gram extractor that passes the contents of IDataWindow.getData() through as the feature gram on which models are trained 
 * and classifications are performed.  This means that the feature gram has a single feature vector at a single point in time, unlike
 * a feature gram computed on subwindows in which there is a feature vector computed on the subwindows and therefore a feature at successive
 * points in time.
 * <p>
 * This is useful when the data in the data window already represents the features on which the model should be trained.  For example a 
 * csv file of metrics representing a process..
 * @author DavidWood
 *
 */
public class IdentityFeatureGramDescriptor extends FeatureGramDescriptor<double[], double[]> {

	private static final long serialVersionUID = -5426904077990500880L;

	/**
	 * Convenience on {@link #IdentityFeatureGramDescriptor(int)} with no resampling.
	 */
	public IdentityFeatureGramDescriptor() {
		this(0);
	}

	/**
	 * Resample the original data so that all data is at the given sampling rate.
	 * @param targetSamplingRate if 0 or less, then don't resample.
	 */
	public IdentityFeatureGramDescriptor(int targetSamplingRate) {
		this(targetSamplingRate, null);
	}

	public IdentityFeatureGramDescriptor(IFeatureProcessor<double[]> processor) {
		super(0,0, new IdentityFeatureExtractor(),processor);
	}

	public IdentityFeatureGramDescriptor(int targetSamplingRate, IFeatureProcessor<double[]> processor) {
		super(0,0, new IdentityFeatureExtractor(targetSamplingRate),processor);
	}

}
