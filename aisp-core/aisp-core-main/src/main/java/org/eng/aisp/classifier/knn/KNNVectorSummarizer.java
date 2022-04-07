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
package org.eng.aisp.classifier.knn;

/**
 * Extends the super class to declare the data type as double[] and to do the weighted combinations of such data.
 * @author dawood
 *
 */
public class KNNVectorSummarizer extends AbstractKNNDataSummarizer<double[]> {

	private static final long serialVersionUID = -2747728534627112375L;

	public KNNVectorSummarizer(int maxListSize, IDistanceFunction<double[]> distFunc, double stdDevFactor, 
			boolean enableOutlierDetection, double reductionFactor) {
		super(maxListSize, distFunc, stdDevFactor, enableOutlierDetection, reductionFactor);
;
	}

	protected double[] combineFeatures(int d1Weight, double[] d1, int d2Weight, double[] d2) {
		int newSize = Math.min(d1.length, d2.length);
		double[] avr = new double[newSize];
		double divisor = 1.0 / (d1Weight + d2Weight);
		for (int i=0; i<newSize; i++) 
			avr[i] = (d1Weight * d1[i] + d2Weight*d2[i]) * divisor;
		
		return avr;
	}




}
