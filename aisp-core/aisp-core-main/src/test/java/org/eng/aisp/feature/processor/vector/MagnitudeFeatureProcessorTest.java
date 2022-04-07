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
import org.eng.aisp.util.L1DistanceFunction;
import org.junit.Test;

public class MagnitudeFeatureProcessorTest extends AbstractVectorFeatureProcessorTest {


	@Override
	protected IFeatureProcessor<?> newFeatureProcessor() {
		return new MagnitudeFeatureProcessor(new L1DistanceFunction(), false, false);
	}

	@Test
	public void testSingleMagnitude() {
		MagnitudeFeatureProcessor mfp = new MagnitudeFeatureProcessor(new L1DistanceFunction(), false, true);
		
		IFeatureGram<double[]> featureGram = createFeatures(
				new double[] { 1, 1, 1, 1 },	// normalized l1 mag = 1
				new double[] { 2, 2, 2, 2 },	// normalized l1 mag = 2
				new double[] { 3, 3, 3, 3 } 	// normalized l1 mag = 3 
				);

		featureGram = mfp.apply(featureGram);
		
		verifyFeatureData(featureGram.getFeatures() , 
				new double[][] {
						{ 2 } 	// average of 1 2 3 
				}
		);
		
	}
	
	@Test
	public void testFeatureMagnitude() {
		MagnitudeFeatureProcessor mfp = new MagnitudeFeatureProcessor(new L1DistanceFunction(), false, false);
		
		IFeatureGram<double[]> featureGram = createFeatures(
				new double[] { 1,   4,  7, 12 },	// normalized l1 mag = 24/4 = 6
				new double[] { 20, 20, 20, 20 },	// normalized l1 mag = 20 
				new double[] { 30, 30, 30, 30 } 	// normalized l1 mag = 30 
				);
		// normalized l1 mag   17, 18, 19, 20  across time 

		featureGram = mfp.apply(featureGram);
		
		verifyFeatureData(featureGram.getFeatures() , 
				new double[][] {
						{ 6 },
						{ 20 }, 	
						{ 30 } 	
				}
		);
		
	}
	
	@Test
	public void testAcrossTimeMagnitude() {
		MagnitudeFeatureProcessor mfp = new MagnitudeFeatureProcessor(new L1DistanceFunction(), true, false);
		
		IFeatureGram<double[]> featureGram = createFeatures(
				new double[] { 1,   4,  7, 12 },	
				new double[] { 20, 20, 20, 20 },	
				new double[] { 30, 30, 30, 28 } 	
				);
		// normalized l1 mag   17, 18, 19, 20  across time

		featureGram = mfp.apply(featureGram);
		
		verifyFeatureData(featureGram.getFeatures() , 
				new double[][] {
						{ 17, 18, 19,20 }
				}
		);
	}
}
