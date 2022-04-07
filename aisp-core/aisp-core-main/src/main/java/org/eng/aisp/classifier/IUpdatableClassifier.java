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
import org.eng.aisp.ILabeledDataWindow;

/**
 * Defines a trainable classifier from which a IFixedClassifier can be generated.
 * 
 * @author dawood
 *
 * @param <DATA>
 */
public interface IUpdatableClassifier<DATA> extends IClassifier<DATA> { 

;

	/**
	 * Method for incremental training.
	 * Iterable of all data can be passed to incrementalTrain(), which is useful for ensemble model which supports models that may not support incremental training.
	 * Pamameter allData is not required for all classifiers and a null value is allowed by certain classifiers.
	 * @param incrementalData
	 * @throws AISPException
	 */
	public void update(Iterable<? extends ILabeledDataWindow<DATA>> incrementalData) throws AISPException;

}
