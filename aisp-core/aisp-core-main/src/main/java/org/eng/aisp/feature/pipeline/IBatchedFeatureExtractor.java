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
package org.eng.aisp.feature.pipeline;

import java.util.List;

import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.pipeline.FeatureExtractionPipeline;

/**
 * Defines a method to retrieve a batch of labeled features from batch of labeled data windows.
 * @author dawood
 *
 * @param <WINDATA>
 * @param <FDATA>
 */
public interface IBatchedFeatureExtractor<WINDATA, FDATA> {

	
	/**
	 * Get the next batch of extracted features from the given batch of data windows.
	 * @param data the labeled data from which labeled features are extracted.
	 * @param fep the feature extraction function applied to the IDataWindow in a ILabeledDataWindow obtained from the data iterator.
	 * @return null if no more features could be extracted, otherwise a
	 *         non-empty iterable over features extracted from the data windows with one feature for each data window.  
	 *         The returned iterable should iterate over the features in the same order as the given data windows if you want repeatable (accuracy) results.
	 */
	public Iterable<? extends ILabeledFeatureGram<FDATA>[]> nextBatch(List<? extends ILabeledDataWindow<WINDATA>> data, 
			FeatureExtractionPipeline<WINDATA,FDATA> fep); 

}
