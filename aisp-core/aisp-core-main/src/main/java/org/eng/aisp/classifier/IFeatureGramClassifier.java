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

import org.eng.aisp.AISPException;
import org.eng.aisp.feature.ILabeledFeatureGram;

/**
 * Extends the super class to add training on ILabeledFeatureGram instances.
 * Abstracts out the training of models on ILabeledFeatureGrams instead of ILabeledDataWindows.
 * Implementations of this are used by AbstractFixableFeatureGramClassifier to do the training after it has extracted the features.
 * 
 * @param <FDATA> data type of a feature.  For example, double[].
 */
public interface IFeatureGramClassifier<FDATA> extends IFixedFeatureGramClassifier<FDATA> {

	/**
	 * Train the classifier on the the labeled features and enable future calls to {@link #classify(org.eng.aisp.feature.IFeatureGram[])}.
	 * @param trainingLabel the name of the label found in the labeled feature to train on. 
	 * @param features the features to use to build the model.
	 * @throws AISPException 
	 */
	public void train(String trainingLabel, Iterable<? extends ILabeledFeatureGram<FDATA>[]> features) throws AISPException;

}