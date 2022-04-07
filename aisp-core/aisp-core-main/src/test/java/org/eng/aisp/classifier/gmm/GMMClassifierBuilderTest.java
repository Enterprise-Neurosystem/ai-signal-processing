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
package org.eng.aisp.classifier.gmm;

import org.eng.aisp.classifier.AbstractClassifierBuilderTest;
import org.eng.aisp.classifier.IClassifierBuilder;
import org.eng.aisp.classifier.gmm.GMMClassifierBuilder;

public class GMMClassifierBuilderTest extends AbstractClassifierBuilderTest {

	@Override
	protected IClassifierBuilder getBuilder() {
		return new GMMClassifierBuilder();
	}

}
