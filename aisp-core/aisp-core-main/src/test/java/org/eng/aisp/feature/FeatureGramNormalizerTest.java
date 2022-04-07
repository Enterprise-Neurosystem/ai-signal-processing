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
package org.eng.aisp.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eng.util.OnlineStats;
import org.junit.Assert;
import org.junit.Test;

public class FeatureGramNormalizerTest {
	
	@Test
	public void testNormalization() {
		double min1 =0, min2=100;
		double max1 =100, max2=200;
		OnlineStats stats1 = new OnlineStats();
		stats1.addSample(min1);
		stats1.addSample(max1);
		OnlineStats stats2 = new OnlineStats();
		stats2.addSample(min2);
		stats2.addSample(max2);
		double[] f1data = new double[] { min1, min2 };
		double[] f2data = new double[] { max1, max2 };

		FeatureGramNormalizer normalizer = createNormalizer(f1data, f2data);
		validateNormalization(normalizer, new double[] { stats1.getMean(), stats2.getMean() }, new double[] { 0,0 });
		validateNormalization(normalizer, new double[] { stats1.getMean() + stats1.getStdDev(), 
				stats2.getMean() - stats2.getStdDev() }, new double[] { 1,-1 });
		
	}


	/**
	 * @param f1data
	 * @param f2data
	 * @return
	 */
	protected FeatureGramNormalizer createNormalizer(double[]...featureVectors) {
		ILabeledFeatureGram<double[]> lfg = makeLFG(featureVectors);
		ILabeledFeatureGram<double[]>[] lfga = new  ILabeledFeatureGram[1];
		lfga[0] = lfg;
		List<ILabeledFeatureGram<double[]>[]> lfgaList = new ArrayList<ILabeledFeatureGram<double[]>[]>();
		lfgaList.add(lfga);
		FeatureGramNormalizer normalizer = new FeatureGramNormalizer(lfgaList);
		return normalizer;
	}
	

	private void validateNormalization(FeatureGramNormalizer normalizer, double[] inputFeature, double[] expectedNormalization) {
		IFeatureGram<double[]> fg1 = makeFG(inputFeature);
		IFeatureGram<double[]> fga[] = new IFeatureGram[] { fg1};
		IFeatureGram<double[]>[] result = normalizer.normalize(fga);
		for (int fgIndex=0 ; fgIndex<fga.length ; fgIndex++) {
			double resultData[] = result[fgIndex].getFeatures()[0].getData();
			Assert.assertTrue(resultData.length == expectedNormalization.length);
			for (int i=0 ; i<expectedNormalization.length ; i++) 
				Assert.assertTrue(expectedNormalization[i] == resultData[i]);
		}
	}


	private ILabeledFeatureGram<double[]> makeLFG(double[]...fdata) {
		IFeatureGram<double[]> fg = makeFG(fdata);
		ILabeledFeatureGram<double[]> lfg = new LabeledFeatureGram<double[]>(fg, new Properties());
		return lfg;
	}

	/**
	 * @param fdata
	 * @return
	 */
	protected IFeatureGram<double[]> makeFG(double[]... fdata) {
		int featureCount = fdata.length;
		IFeature<double[]> farray[] = new IFeature[featureCount];
		for (int i=0 ; i<fdata.length ; i++) {
			IFeature<double[]> feature = new DoubleFeature(0, 1000, fdata[i]);
			farray[i] = feature;
		}
		IFeatureGram<double[]> fg = new FeatureGram<double[]>(farray);
		return fg;
	}



}
