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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.feature.processor.vector.DeltaFeatureProcessor.ScaleMethod;
import org.junit.Assert;
import org.junit.Test;


public class DeltaFeatureProcessorTest extends AbstractVectorFeatureProcessorTest {
	
	
	@Override
	protected IFeatureProcessor<?> newFeatureProcessor() {
		return new DeltaFeatureProcessor(1, new double[] { 1, 1, 1} );
	}
	
	/**
	 * Test no differences.
	 */
	@Test
	public void testDelta0() {
		DeltaFeatureProcessor dfp = new DeltaFeatureProcessor(1, new double[] { 1, 0, 0 });	
		IFeatureGram<double[]> featureGram =  createFeatures(new double[] {0,0,0}, new double[] {1,1,1}, new double[] { 2,2,2});
		double[][] expected =  new double[][] { new double[] {0,0,0}, new double[] {1,1,1}, new double[] { 2,2,2}};
		featureGram = dfp.apply(featureGram);
		IFeature<double[]>[] features = featureGram.getFeatures();
		verifyFeatureData(features, expected);
//		featureGram = dfp.apply(featureGram);
//		IFeature<double[]>[] features = featureGram.getFeatures();;
//		Assert.assertTrue(features.length == 3);
//		for (int i=0 ; i<features.length ; i++) {
//			double[] fdata= features[i].getData();
//			Assert.assertTrue(fdata.length == 3);
//			for (int j=0 ; j<fdata.length ; j++)
//				Assert.assertTrue(fdata[j] == i);	
//		}
	}

	/**
	 * Test generation of the 1st order difference only.
	 */
	@Test
	public void testDelta1() {
		DeltaFeatureProcessor dfp = new DeltaFeatureProcessor(1, new double[] { 0, 1, 0 });	
		IFeatureGram<double[]> featureGram =  createFeatures(new double[] {0,0}, new double[] {1,1}, new double[] { 2,2}, new double[] {3,3} );
		double[][] expected =  new double[][] { {0,0}, {1,1}, { 1,1}, {0,0}};
		featureGram = dfp.apply(featureGram);
		IFeature<double[]>[] features = featureGram.getFeatures();
		verifyFeatureData(features, expected);

	}
	
	/**
	 * Test generation of the 2nd order difference only.
	 */
	@Test
	public void testDelta2() {
		DeltaFeatureProcessor dfp = new DeltaFeatureProcessor(1, new double[] { 0, 0, 1} ); 
		IFeatureGram<double[]> featureGram =  createFeatures(new double[] {0,0}, new double[] {1,1}, new double[] { 2,2}, new double[] { 3, 3});
		// first derivative -> new double[][] {{ 0,0}, {1,1}, { 1,1}, {0,0}};
		double[][] expected =  new double[][] { {0,0}, {.5,.5}, { -.5,-.5}, {0,0}};
		featureGram = dfp.apply(featureGram);
		IFeature<double[]>[] features = featureGram.getFeatures();
		verifyFeatureData(features, expected);
	}

	/**
	 * Test generation and concatenation of the 0th, 1st and 2nd order differences.
	 */
	@Test
	public void testDelta012() {
		DeltaFeatureProcessor dfp = new DeltaFeatureProcessor(1, new double[] { 1, 1,1} ); 
		IFeatureGram<double[]> featureGram =  createFeatures(new double[] {0,0}, new double[] {1,1}, new double[] { 2,2}, new double[] { 3, 3});
		// 1st derivative -> new double[][] {{ 0,0}, {1,1}, { 1,1}, {0,0}};
		// 2nd derivative -> new double[][] { {0,0}, {.5,.5}, { -.5,-.5}, {0,0}};
		double[][] expected =  new double[][] { {0,0, 0,0, 0,0}, {1,1, 1,1, .5,.5 }, {2,2, 1,1, -.5,-.5}, {3,3, 0,0, 0,0}};
		featureGram = dfp.apply(featureGram);
		IFeature<double[]>[] features = featureGram.getFeatures();
		verifyFeatureData(features, expected);
	}
	
	@Test
	public void testWeightedDelta() {
		DeltaFeatureProcessor dfp = new DeltaFeatureProcessor(1, new double[] { 1, 2,4} ); 
		IFeatureGram<double[]> featureGram =  createFeatures(new double[] {0,0}, new double[] {1,1}, new double[] { 2,2}, new double[] { 3, 3});
		// 1st derivative -> new double[][] {{ 0,0}, {1,1}, { 1,1}, {0,0}};
		// 2nd derivative -> new double[][] { {0,0}, {.5,.5}, { -.5,-.5}, {0,0}};
		// w/o weights    -> new double[][] { {0,0, 0,0, 0,0}, {1,1, 1,1, .5,.5 }, {2,2, 1,1, -.5,-.5}, {3,3, 0,0, 0,0}};;
		double[][] expected =  new double[][] { {0,0, 0,0, 0,0}, {1,1, 2,2, 2,2 }, {2,2, 2,2, -2,-2}, {3,3, 0,0, 0,0}};
		featureGram = dfp.apply(featureGram);
		IFeature<double[]>[] features = featureGram.getFeatures();
		verifyFeatureData(features, expected);
	}
	
	@Test
	public void testWideWindowVelocity() {
		DeltaFeatureProcessor dfp = new DeltaFeatureProcessor(2, new double[] { 1, 1,0} ); 
		IFeatureGram<double[]> featureGram = createFeatures(new double[] {0,0}, new double[] {1,1}, new double[] { 2,2 }, new double[] { 3,3 }, new double[] { 4,4 });
		// 1st derivative -> double[][] { new double[] {.5}, new double[] {1,1}, new double[] { .5,.5}};
		// 2nd derivative -> double[][] { new double[] {.25,.25}, new double[] {0,0}, new double[] { -.25,-.25}};
		// w/o weights    -> double[][] { new double[] {0, 0, .5, .5, .25,.25}, new double[] {1,1, 1,1, 0,0 }, new double[] {2,2, .5,.5, -.25,-.25}};
//		double[][] expected =  new double[][] { new double[] {0, 0, 1, 1, 1,1}, new double[] {1,1, 2,2, 0,0 }, new double[] {2,2, 1,1, -1,-1}};
		// see http://practicalcryptography.com/miscellaneous/machine-learning/guide-mel-frequency-cepstral-coefficients-mfccs/#deltas-and-delta-deltas
		double midVelocity = (1*2.0 + 2*4) / (1+4) / 2;
		double[][] expected =  new double[][] { {0,0, 0,0}, 
												{1,1, 1,1}, 
												{2,2, midVelocity, midVelocity}, 
												{3,3, 1,1}, 
												{4,4, 0,0}, 
												};
		featureGram = dfp.apply(featureGram);
		IFeature<double[]>[] features = featureGram.getFeatures();
		verifyFeatureData(features, expected);
	}
	
	@Test
	public void testNormalization() {
		DeltaFeatureProcessor dfp = new DeltaFeatureProcessor(1, new double[] { 1,1,1}, ScaleMethod.ScaleByRange, false); 
		IFeatureGram<double[]> featureGram =  createFeatures(new double[] {0,0,0}, new double[] {1,1,1}, new double[] { 2,2,2});
		featureGram = dfp.apply(featureGram);
		IFeature<double[]>[] features = featureGram.getFeatures();
		Assert.assertTrue(features.length == 3);
		for (int i=0 ; i<features.length ; i++) {
			double[] fdata= features[i].getData();
			Assert.assertTrue(fdata.length == 9);
			for (int j=0 ; j<fdata.length ; j++)
				Assert.assertTrue(fdata[j] != 10);	
		}
	}

	protected double[] newData(double start, int count) {
		double[] data = new double[count];
		for (int i=0 ; i<data.length; i++)
			data[i] = start+i;
		return data;
	}

	protected double[] newDataShuffle(double start, int count) {
		double[] data = newData(start,count); 
		List<Double> dlist = new ArrayList<Double>();
		for (int i=0 ; i<data.length; i++)
			dlist.add(data[i]);
		Collections.shuffle(dlist);
		for (int i=0 ; i<data.length; i++)
			data[i] = dlist.get(i);
		return data;
	}
	
	protected double[][] newMatrix(double start, int dim, int power) {
		double[][] matrix = new double[dim][];
		for (int i=0 ; i<dim ; i++) {
			double offset = Math.pow(i, power);
			matrix[i] = newData(start+offset, dim);
		}
		return matrix;
	}

//	@Test
//	public void testCaching() {
//		int matrixSize=20;
//		int itemCount = 2;
//		DeltaFeatureProcessor processors[] = {
//				new DeltaFeatureProcessor(2, new double[] {1.0,1.0,1.0}, ScaleMethod.Unscaled, true),
//				new DeltaFeatureProcessor(2, new double[] {1.0,1.0,1.0}, ScaleMethod.Unscaled, false),
//				new DeltaFeatureProcessor(2, new double[] {1.0,1.0,1.0}, ScaleMethod.ScaleByStddev, true),
//				new DeltaFeatureProcessor(2, new double[] {1.0,1.0,1.0}, ScaleMethod.ScaleByStddev, false),
//				new DeltaFeatureProcessor(2, new double[] {1.0,1.0,1.0}, ScaleMethod.ScaleByRange , true),
//				new DeltaFeatureProcessor(2, new double[] {1.0,1.0,1.0}, ScaleMethod.ScaleByRange , false),
//				
//				new DeltaFeatureProcessor(2, new double[] {1.0,2.0,8.0}, ScaleMethod.Unscaled, true),
//				new DeltaFeatureProcessor(2, new double[] {1.0,2.0,8.0}, ScaleMethod.Unscaled, false),
//				new DeltaFeatureProcessor(2, new double[] {1.0,2.0,8.0}, ScaleMethod.ScaleByStddev, true),
//				new DeltaFeatureProcessor(2, new double[] {1.0,2.0,8.0}, ScaleMethod.ScaleByStddev, false),
//				new DeltaFeatureProcessor(2, new double[] {1.0,2.0,8.0}, ScaleMethod.ScaleByRange , true),
//				new DeltaFeatureProcessor(2, new double[] {1.0,2.0,8.0}, ScaleMethod.ScaleByRange , false),
//		};
//	
//		List<double[][]> results = new ArrayList<double[][]>();
//		
//		for (int i=0 ; i<itemCount ; i++) {
//			double[][] feature = newMatrix(i, matrixSize,i+1);
////			System.out.println("\nBase feature");
////			MatrixUtil.showMatrix(feature);
//			for (int j=0 ; j<processors.length ; j++) {
//				DeltaFeatureProcessor dfp = processors[j];
////				System.out.println("input feature");
////				MatrixUtil.showMatrix(feature);
////				System.out.println("Mapping with " + dfp);
//				double[][] mapped = dfp.map(xxx, feature);
////				System.out.println("Produced");
////				MatrixUtil.showMatrix(mapped);
//				results.add(mapped);
//			}
//		}
//		for (int j=0 ; j<results.size(); j++) {
//			double[][] matrix1 = results.get(j);
//			for (int k=j+1 ; k<results.size(); k++)  {
//				double[][] matrix2 = results.get(k);
//				Assert.assertTrue("j=" + j + ", k=" + k, !equals(matrix1,matrix2));
//			}
//		}
//
//	}

	private boolean equals(double[][] matrix1, double[][] matrix2) {
		Assert.assertTrue(matrix1.length == matrix2.length);
		for (int i=0 ; i<matrix1.length; i++)  {
			if (!Arrays.equals(matrix1[i], matrix2[i]))
				return false;
		}
		return true;
	}
}
