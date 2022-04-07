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
package org.eng.aisp.classifier.cnn;

import org.eng.aisp.classifier.IFixableClassifier;
import org.eng.aisp.classifier.cnn.CNNClassifierBuilder;

public class ExtCNNClassifierTest extends CNNModelerTest {


	/**
	 * Override to turn on disk caching.
	 */
	@Override
	protected IFixableClassifier<double[]> getClassifier() {
		CNNClassifierBuilder builder = new CNNClassifierBuilder();
		builder.setTrainingFolds(2);
		builder.setUseDiskCache(true);
		return builder.build();
	}

}
