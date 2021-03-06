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
package org.eng.aisp.classifier.mvi;

import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.AbstractMulticlassClassifierTest;
import org.eng.aisp.classifier.IFixableClassifier;
import org.eng.aisp.classifier.mvi.MVIClassifierBuilder;
import org.junit.Assume;

public class MVIClassifierTest extends AbstractMulticlassClassifierTest {

	@Override
	protected IFixableClassifier<double[]> getClassifier() throws AISPException {
		// Expects VAPI_HOST, VAPI_TOKEN to be defined in the environment.
		MVIClassifierBuilder builder = new MVIClassifierBuilder();
		builder
			.setModelName("junit-" + this.getClass().getSimpleName())
			.setPreserveModel(false)
//			.setFeatureExtractor(new LogMelFeatureExtractor(512))
			;

		return builder.build(); 
	}

	@Override
	protected IFixableClassifier<double[]> getUnknownClassifier() throws AISPException {
		return null; 
	}

	@Override
	protected int getMinimumTrainingClasses() {
		return 2; 
	}

	@Override
	protected int getMinimumTrainingDataPerClass() {
		return 10; 
	}
	
	/** Just to reduce the length of the test */
	protected int getParallelClassificationCountPerThread() {
		return 25;
	}
	
	@Override
	public void testLabelValueCaseSensitivity() {
		Assume.assumeTrue("Skipping this test since MVI model training fails on this data set", false);
	}
	
//	@Test
//	public void testMixedDurations() throws AISPException, IOException  {
//		Assume.assumeTrue("Skipping this test since MVI model training fails on this data set", false);
//	}

}
