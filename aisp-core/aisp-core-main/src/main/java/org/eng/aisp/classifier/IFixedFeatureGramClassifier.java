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
package org.eng.aisp.classifier;

import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.feature.IFeatureGram;


/**
 * 
 * Abstracts out the classification of IFeatureGrams. 
 * Implementations of this are created by AbstractFixableFeatureGramClassifier 
 * and used by FixedFeatureGramClassifier to do the classification after it has extracted the features.
 * 
 * @author dawood 
 * 
 * @param <FDATA> data type of a feature.  For example, double[].
 * @see {@link FixedFeatureGramClassifier},  {@link AbstractFixableFeatureGramClassifier}.
 */
public interface IFixedFeatureGramClassifier<FDATA> {

	/** 
	 * Get the classification(s) of the feature.
	 * This method must be thread-safe and if need-be the implementation should add synchronization if required.
	 * @param features 1 or more features extracted from a single data window.
	 * @return a list of 1 or more classifications each derived from the 1 or more features. 
	 * @throws AISPException if not trained.
	 */
	List<Classification> classify(IFeatureGram<FDATA>[] features) throws AISPException;
	
	/**
	 * Get the label(s) which this classifier will generate.  
	 * @return null if not currently available to generate classification results.
	 */
	String getTrainedLabel();

}