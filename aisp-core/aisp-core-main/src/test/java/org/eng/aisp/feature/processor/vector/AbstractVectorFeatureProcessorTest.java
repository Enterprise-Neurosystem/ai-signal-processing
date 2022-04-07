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

import org.eng.aisp.feature.DoubleFeature;
import org.eng.aisp.feature.FeatureGram;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.processor.AbstractFeatureProcessorTest;
import org.junit.Assert;

public abstract class AbstractVectorFeatureProcessorTest extends AbstractFeatureProcessorTest {

	public AbstractVectorFeatureProcessorTest() {
		super();
	}

	protected void assertArrayEquality(double[][] computedData, double[][] expectedData) {
		Assert.assertTrue(computedData.length == expectedData.length);
	
		for (int i=0 ; i<computedData.length ; i++) {
			double[] cd = computedData[i];
			double[] ed = expectedData[i];
			Assert.assertArrayEquals(ed, cd,0);
		}
	
		
	}
	
	protected void verifyFeatureData(IFeature<double[]>[] computedFeatures, double[][] expectedData) {
		Assert.assertTrue(computedFeatures.length == expectedData.length);
		double[][] computedData = new double[computedFeatures.length][];
		for (int i=0 ; i<computedData.length ; i++)
			computedData[i] = computedFeatures[i].getData();
	
		assertArrayEquality(computedData, expectedData);
	}

	protected static IFeatureGram<double[]> createFeatures(double[]...featureData) {
		IFeature<double[]> features[] = new IFeature[featureData.length]; 
	
		int startMsec = 0, deltaMsec = 100;
		for (int i=0 ; i<featureData.length ; i++) {
			IFeature<double[]> f = new DoubleFeature(startMsec, startMsec + deltaMsec, featureData[i]);
			features[i] = f;
			startMsec += deltaMsec;
		}
		return new FeatureGram(features);
		
	}

}
