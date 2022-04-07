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

import java.util.Properties;

import org.eng.aisp.feature.DoubleFeature;
import org.eng.aisp.feature.FeatureGram;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.processor.AbstractCachingMultiFeatureProcessor;

/**
 * Extends the super class enable implementations that collapse the matrix of feature data into a single feature.
 * Subclasses must implement {@link #applyImpl(double[][])} to produce a single array of double data that will be
 * contained in the DoubleFeature produced by this implementation.
 * @author dawood
 *
 */
public abstract class AbstractSingletonFeatureExtractor extends AbstractCachingMultiFeatureProcessor<double[]> {

	private static final long serialVersionUID = 4190697129398647873L;

	/**
	 * Extract the data from the features into a matrix of double values and pass
	 * it to {@link #applyImpl(double[][])} to produce the data that will be contained
	 * in the returned DoubleFeature.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected final IFeatureGram<double[]> applyImpl(IFeatureGram<double[]> featureGram) {
		IFeature<double[]>[] features = featureGram.getFeatures();
		if (features.length < 2) 
			return featureGram;
		
		double[][] data = new double[features.length][];
		for (int i=0 ; i<data.length ; i++)
			data[i] = features[i].getData();
		double[] newData;
		double startTimeMsec, endTimeMsec;
		Properties context;
		if (features.length == 0) {
			startTimeMsec = 0;
			endTimeMsec = 0;
			newData = new double[0]; 
		} else {
			newData = applyImpl(data); 	// MatrixUtil.averageRows(data);
			startTimeMsec =  features[0].getStartTimeMsec();
			endTimeMsec = features[features.length-1].getEndTimeMsec();
		}
		IFeature<double[]> f = new DoubleFeature(startTimeMsec, endTimeMsec, newData) ;
		return new FeatureGram<double[]>(new IFeature[] { f });
	}

	/**
	 * Extract the single dimensioned data from the given matrix of data which was extracted from the features
	 * provided to this instance.  The features are generally from a single window of data on which features
	 * are extracted from subwindows.
	 * @param featureData matrix of data with the first dimension representing the sub-windows (in time) and 
	 * the second dimension is the feature data for a given sub-window.
	 * @return never null.
	 */
	protected abstract double[] applyImpl(double[][] featureData);

}
