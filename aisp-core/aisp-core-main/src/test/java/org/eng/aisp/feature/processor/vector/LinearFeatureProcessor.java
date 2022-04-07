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
import org.eng.aisp.feature.processor.IFeatureProcessor;

public class LinearFeatureProcessor extends AbstractVectorMappingFeatureProcessor implements IFeatureProcessor<double[]> {



	private static final long serialVersionUID = -7421961091655629433L;

	protected final double slope;
	protected final double intercept;

	public LinearFeatureProcessor(double slope, double intercept) {
		super();
		this.slope = slope;
		this.intercept = intercept;
	}
	
	@Override
	protected double[][] map(IFeature<double[]>[] features, double[][] featureData) {
		double[][] newData = new double[featureData.length][];
		for (int i=0 ; i<featureData.length ; i++)  {
			newData[i] = new double[featureData[i].length];
			for (int j=0 ; j<newData[i].length ; j++) 
				newData[i][j] = slope * featureData[i][j] + intercept;
		}
		return newData;
	}


}
