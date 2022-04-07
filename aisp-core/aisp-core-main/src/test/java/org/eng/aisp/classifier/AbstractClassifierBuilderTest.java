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
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.IClassifierBuilder;
import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.FFTFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.feature.processor.vector.DeltaFeatureProcessor;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractClassifierBuilderTest {
	
	protected abstract IClassifierBuilder getBuilder();

	@Test
	public void testReturnValues() {
		IClassifierBuilder builder = getBuilder();
		
		Assert.assertTrue(builder == builder.setWindowSizeMsec(51));
		Assert.assertTrue(builder.getWindowSizeMsec() == 51);
		Assert.assertTrue(builder == builder.setWindowShiftMsec(53));
		Assert.assertTrue(builder.getWindowShiftMsec() == 53);
		
		IFeatureExtractor extractor = new FFTFeatureExtractor();
		Assert.assertTrue(builder == builder.setFeatureExtractor(extractor));
		Assert.assertTrue(builder.getFeatureExtractor() == extractor);

		IFeatureProcessor processor = new DeltaFeatureProcessor(2,new double[] { 1, 1, 1});
		Assert.assertTrue(builder == builder.setFeatureProcessor(processor));
		Assert.assertTrue(builder.getFeatureProcessor() == processor);

		FeatureGramDescriptor fge = new FeatureGramDescriptor(50,53, extractor, processor);
		Assert.assertTrue(builder == builder.setFeatureGramDescriptor(fge));
//		Assert.assertTrue(builder.getFeatureGramExtractor() == fge);
		
	}
	
	@Test
	public void testBuildDefault() throws AISPException {
		IClassifierBuilder builder = getBuilder();
		IClassifier c = builder.build();
		Assert.assertTrue(c != null);
	}
}
