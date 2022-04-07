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

import org.eng.aisp.IDataWindow;
import org.eng.aisp.feature.DoubleFeature;
import org.eng.aisp.feature.FeatureGram;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.processor.AbstractCachingMultiFeatureProcessor;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.util.Vector;

/**
 * Extends the super class to map an array of feature data to a new array feature data. 
 * Subclasses must implement {@link #map(IFeature[], double[][])} to map from one matrix of feature data to another matrix of feature data. 
 * @author dawood 
 *
 */
public abstract class AbstractVectorMappingFeatureProcessor extends AbstractCachingMultiFeatureProcessor<double[]> implements IFeatureProcessor<double[]> {

	
	private static final long serialVersionUID = 7281114226877656483L;

	/**
	 * @param features array of IFeature<double[]>
	 * @return a feature gram containing the processed features. May be the input if no processing is performed.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected IFeatureGram<double[]> applyImpl(IFeatureGram<double[]> featureGram) {
		IFeature<double[]>[] features = featureGram.getFeatures();
		if (features.length == 0)
			return featureGram;
		
		// Extract the data from the features.
		double[][] featureData = new double[features.length][]; 
		for (int i=0 ; i<featureData.length; i++) 
			featureData[i] = features[i].getData();

		// Normalize the features, creating a new set of data.
		double[][] newFeatureData  = map(features, featureData);
		if (newFeatureData == null || newFeatureData.length == 0)
			throw new RuntimeException(this.getClass().getName() + " implementation of map() returned a null or empty matrix");
		if (newFeatureData == featureData)
			return featureGram;	// no modification.

		// See if the same shape was maintained.  If so, we can reuse the independent x values below
		boolean maintainedShape = featureData.length == newFeatureData.length 
				&& featureData[0].length == newFeatureData[0].length;

		featureData = newFeatureData;
		
		// Create the DoubleFeatures from the new data that we want to return.
		@SuppressWarnings("unchecked")
		IFeature<double[]>[] newFeatures = new IFeature[featureData.length];
		Vector xValues = null;
		for (int i=0 ; i<featureData.length ; i++)  {
			IFeature<double[]> t0Feature = features[i];
			if (maintainedShape && t0Feature instanceof IDataWindow) {
				// This was added to try and preserve the frequencies contained in the independent values of 
				// features extracted by extractors that provide these frequencies in their resulting IFeatures.  
				// It is not strictly necessary to preserve these independent values (as of 12/2020) as we don't 
				// generally use the them value for modeling (yet?). These values are nice to have when inspecting 
				// the features produced.
				if (xValues == null)
					xValues = new Vector(((IDataWindow)t0Feature).getIndependentValues());
				newFeatures[i] = new DoubleFeature(t0Feature.getStartTimeMsec(), t0Feature.getEndTimeMsec(), 
						xValues,
						featureData[i]) ;
			} else {
				newFeatures[i] = new DoubleFeature(t0Feature.getStartTimeMsec(), t0Feature.getEndTimeMsec(), featureData[i]) ;
			}
		}
		
		return new FeatureGram<double[]>(newFeatures);
	}

	/**
	 * Map the given array of double arrays to a new array of double arrays.  The number of double arrays returned
	 * need not be the same as in the input, and the length of each double array can be different than the input.
	 * @param features The original features from which the data is extracted. 
	 * @param featureData the data extracted from the given features.  Should NOT be modified.
	 * @return never null. May return the given featureData array in which case the features are not modified.
	 * May return a 0x0 matrix (new double[0][]) if the matrix of features could not be created (e.g., not enough data, etc).
	 */
	protected abstract double[][] map(IFeature<double[]>[] features, double[][] featureData); 

}
