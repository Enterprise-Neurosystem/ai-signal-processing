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
import org.eng.aisp.feature.processor.AbstractCachingMultiFeatureProcessor;
import org.eng.aisp.util.IDistanceFunction;
import org.eng.aisp.util.MatrixUtil;
import org.eng.aisp.util.VectorUtils;

/**
 * Uses a distance metric to compute the magnitude of the features.
 * Magnitude may be computed within a feature or across feature elements.
 * The magnitudes may optionally be averaged to single value.
 * For a NxM (time x feature len) feature gram,
 * <ul>
 * <li> If computing across time, the result will be a 1xM feature gram. 
 * <li> If computing across each feature, the result will be a Nx1 feature gram. 
 * <li> If averaging everything, the result will be a 1x1 feature gram. 
 * </ul>
 * @author DavidWood
 *
 */
public class MagnitudeFeatureProcessor extends AbstractCachingMultiFeatureProcessor<double[]> {

	
	private static final long serialVersionUID = -8667515155071754026L;
	private IDistanceFunction<double[]> distanceFunction;
	private boolean magnitudeAcrossTime;
	private boolean singleValue;

	public MagnitudeFeatureProcessor(IDistanceFunction<double[]> dist, boolean magnitudeAcrossTime, boolean singleValue) {
		this.distanceFunction = dist;
		this.magnitudeAcrossTime = magnitudeAcrossTime;
		this.singleValue = singleValue;
	}

	@Override
	protected IFeatureGram<double[]> applyImpl(IFeatureGram<double[]> featureGram) {
		IFeature<double[]>[] features = featureGram.getFeatures();
		int nFeatures = features.length;
		if (features.length == 0)
			return featureGram;
		
		// Extract the data from the features.
		double[][] featureData = new double[features.length][]; 
		for (int i=0 ; i<featureData.length; i++) 
			featureData[i] = features[i].getData();
		
		// If across time, then transpose matrix so we can always compute magnitudes of columns.
		if (magnitudeAcrossTime && !singleValue) 
			featureData = MatrixUtil.transpose(featureData);
		
		// Compute the magnitude of each column
		double[] distances = new double[featureData.length];
		for (int i=0 ; i<distances.length ; i++)
			distances[i] = distanceFunction.distance(featureData[i]);
		
		// Create the complete feature gram.
		double startMsec = featureGram.getFeatures()[0].getStartTimeMsec();
		double endMsec = featureGram.getFeatures()[nFeatures-1].getEndTimeMsec();
		DoubleFeature[] dfArray;
		if (this.singleValue) {
			//  Single value that is an average of all distances in a single feature.
			double d = VectorUtils.getStatistics(distances).getMean();
			dfArray = new DoubleFeature[] { createSingleDataFeature(startMsec, endMsec, d)};
		} else if (magnitudeAcrossTime) {	
			// 1 feature containing all distances computed across time.
			dfArray = new DoubleFeature[] { new DoubleFeature(startMsec, endMsec, distances) };
		} else {
			// 1 feature for each original column containing the magnitude of the feature.
			dfArray = new DoubleFeature[distances.length];
			for (int i=0 ; i<distances.length ; i++) 
				dfArray[i] = createSingleDataFeature(startMsec, endMsec, distances[i]);
		}

		IFeatureGram<double[]> fg = new FeatureGram<double[]>(dfArray);
		return fg;
	}

	/**
	 * @param startMsec
	 * @param endMsec
	 * @param d
	 * @return
	 */
	private static DoubleFeature createSingleDataFeature(double startMsec, double endMsec, double d) {
		double data[] = new double[] { d };
		DoubleFeature df = new DoubleFeature(startMsec, endMsec,  data);
		return df;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (magnitudeAcrossTime ? 1231 : 1237);
		result = prime * result + ((distanceFunction == null) ? 0 : distanceFunction.hashCode());
		result = prime * result + (singleValue ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof MagnitudeFeatureProcessor))
			return false;
		MagnitudeFeatureProcessor other = (MagnitudeFeatureProcessor) obj;
		if (magnitudeAcrossTime != other.magnitudeAcrossTime)
			return false;
		if (distanceFunction == null) {
			if (other.distanceFunction != null)
				return false;
		} else if (!distanceFunction.equals(other.distanceFunction))
			return false;
		if (singleValue != other.singleValue)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MagnitudeFeatureProcessor [distanceFunction=" + distanceFunction + ", magnitudeAcrossTime="
				+ magnitudeAcrossTime + ", singleValue=" + singleValue + "]";
	}

	
}
