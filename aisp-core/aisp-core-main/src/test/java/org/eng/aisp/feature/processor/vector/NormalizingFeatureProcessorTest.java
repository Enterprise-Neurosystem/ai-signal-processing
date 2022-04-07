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

import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.util.MatrixUtil;
import org.junit.Assert;
import org.junit.Test;


public class NormalizingFeatureProcessorTest extends AbstractVectorFeatureProcessorTest {


	@Override
	protected IFeatureProcessor<?> newFeatureProcessor() {
		return new NormalizingFeatureProcessor();
	}

	@Test
	public void testMatrixRangeNormalize() {
		double[][] featureData = new double[][] { {0,0,0}, {5,5,10}, { 10,10, 5} };
		IFeatureGram<double[]> featureGram = createFeatures(featureData);

		// Apply the feature processors 
		IFeatureProcessor<double[]> fp = new NormalizingFeatureProcessor(false, false, true, true);	// normalize range to 1, but mean unchanged.
		IFeatureGram<double[]> newFeatureGram = fp.apply(featureGram);

		// Make sure original feature is not modified
		verifyFeatureData(featureGram.getFeatures(),featureData);	

		// Make sure the feature correctly processed the data by scaling the range from 0..1
		double[][] expectedData = new double[][] { {0,0,0}, {.5,.5,1}, { 1,1, .5} };
		verifyFeatureData(newFeatureGram.getFeatures(),expectedData);
	}

	@Test
	public void testColumnRangeNormalize() {
		double[][] featureData = new double[][] { {0,0,0}, {1,2,3}, { 10,20,30} };
		IFeatureGram<double[]> featureGram = createFeatures(featureData);

		// Apply the feature processors 
		IFeatureProcessor<double[]> fp = new NormalizingFeatureProcessor(false, true, true, false);	// column values in the range -0.5 to 0.5
		IFeatureGram<double[]> newFeatureGram = fp.apply(featureGram);

		// Make sure original feature is not modified
		verifyFeatureData(featureGram.getFeatures(),featureData);	

		// Make sure the feature correctly processed the data by scaling the range from -.5,.5 
		double[][] expectedData = new double[][] { {0,0,0}, {-.5,0,.5}, { -.5, 0, .5 }}; 
		verifyFeatureData(newFeatureGram.getFeatures(),expectedData);
	}
	
	
	@Test
	public void testRowRangeNormalize() {
		double[][] featureData = new double[][] { {0,0,0}, {1,2,3}, { 10,20,30} };	// Same data as column test, but then we transpose it.
		featureData = MatrixUtil.transpose(featureData);
		IFeatureGram<double[]> featureGram = createFeatures(featureData);

		// Apply the feature processors 
		IFeatureProcessor<double[]> fp = new NormalizingFeatureProcessor(false, true, false, true);	// true values in the range -0.5 to 0.5
		IFeatureGram<double[]> newFeatureGram = fp.apply(featureGram);

		// Make sure original feature is not modified
		verifyFeatureData(featureGram.getFeatures(),featureData);	

		// Make sure the feature correctly processed the data by scaling the range from -.5,.5 
		double[][] expectedData = new double[][] { {0,0,0}, {-.5,0,.5}, { -.5, 0, .5 }}; 	// Same  data as for column test, but transpose it.
		expectedData = MatrixUtil.transpose(expectedData);
		verifyFeatureData(newFeatureGram.getFeatures(),expectedData);
	}
	

	protected void verifyFeatureData(IFeature<double[]>[] computedFeatures, double[][] expectedData) {
		Assert.assertTrue(computedFeatures.length == expectedData.length);
		double[][] computedData = new double[computedFeatures.length][];
		for (int i=0 ; i<computedData.length ; i++)
			computedData[i] = computedFeatures[i].getData();

		assertArrayEquality(computedData, expectedData);
	}

	protected void assertArrayEquality(double[][] computedData, double[][] expectedData) {
		Assert.assertTrue(computedData.length == expectedData.length);

		for (int i=0 ; i<computedData.length ; i++) {
			double[] cd = computedData[i];
			double[] ed = expectedData[i];
			Assert.assertArrayEquals(cd,ed,0);
		}

		
	}

}
