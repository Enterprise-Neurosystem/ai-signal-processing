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
package org.eng.aisp.feature.processor.vector;

import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.junit.Test;


public class PipelinedFeatureProcessorTest extends AbstractVectorFeatureProcessorTest {


	@Override
	protected IFeatureProcessor<?> newFeatureProcessor() {
		return new PipelinedFeatureProcessor(new IdentityFeatureProcessor(), new IdentityFeatureProcessor());
	}

	@Test
	public void testPipeline() {
		double[][] featureData = new double[][] { {0,0,0}, {5,5,10}, { 10,10, 5} };
		IFeatureGram<double[]> featureGram = createFeatures(featureData);

		// Apply the feature processors 
		IFeatureProcessor<double[]> fp = new PipelinedFeatureProcessor(new NormalizingFeatureProcessor(false, false, true,true), new LinearFeatureProcessor(10, 100));
		IFeatureGram<double[]> newFeatureGram = fp.apply(featureGram);

		// Make sure original feature is not modified
		verifyFeatureData(featureGram.getFeatures(),featureData);	

		// Make sure the feature correctly processed the data by scaling the range from 0..1
		double[][] expectedData = new double[][] { {100,100,100}, {105,105,110}, { 110,110, 105} };
		verifyFeatureData(newFeatureGram.getFeatures(),expectedData);
	}

}
