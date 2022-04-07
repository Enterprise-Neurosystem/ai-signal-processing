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

public interface IClassifierFactory<WINDATA> {

	/**
	 * Create an new instance of a classifier according to the builder's state.
	 * @return never null
	 * @throws AISPException if there was a problem creating the instance.
	 */
	public IClassifier<WINDATA> build() throws AISPException;

	
}
