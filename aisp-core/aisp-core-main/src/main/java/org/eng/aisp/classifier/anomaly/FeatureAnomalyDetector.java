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
package org.eng.aisp.classifier.anomaly;

import org.eng.aisp.AISPException;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGram;

/**
 * Uses an IAnomalyDetector for each element of the IFeature instances within an IFeatureGram to identify anomalies within a given feature gram.
 * A threshold percentage within a feature and across all features of the spectrogram is used to define thresholds for each.
 * The 1st threshold defines the percentage of elements within a feature to be declared an anomaly before a feature itself is considered an anomaly.
 * The 2nd threshold defines the percentage of features within a feature gram to be declared an anomaly before the feature gram is considered an anomaly. 
 * 
 * @author DavidWood
 */
public class FeatureAnomalyDetector implements IAnomalyDetector<IFeatureGram<double[]>> {

	private static final long serialVersionUID = 4239405358351579667L;
	private MultidimensionalAnomalyDetector multiDetector;
	private double acrossFeaturesVotePercentage;

	public FeatureAnomalyDetector(IAnomalyDetectorBuilder<Double> detectorBuilder, int featureLen, double withinFeatureVotePercentage, double acrossFeaturesVotePercentage) {
		if (acrossFeaturesVotePercentage < 0 || acrossFeaturesVotePercentage > 1) 
			throw new IllegalArgumentException("acrossFeaturesVotePercentage must be range from 0 to 1");
		int minVotes = Math.max(1, (int)Math.round(featureLen*withinFeatureVotePercentage));
		this.multiDetector = new MultidimensionalAnomalyDetector(detectorBuilder, featureLen, minVotes);
		this.acrossFeaturesVotePercentage = acrossFeaturesVotePercentage;
	}

	public void update(boolean isOfflineTraining, boolean isNormal, long atTime, IFeatureGram<double[]> featureGram) throws AISPException {
		IFeature<double[]>[] featureGramFeatures = featureGram.getFeatures();
		int featureCount = featureGramFeatures.length;
		long startTime = atTime * featureCount;
		for (IFeature<double[]> f : featureGramFeatures) { 
			double data[] = f.getData();
			multiDetector.update(isOfflineTraining, isNormal, startTime++, data);
		}
	}

	public AnomalyResult isAnomaly(long atTime, IFeatureGram<double[]> featureGram) throws AISPException {
		double abnormalConf = 0;
		double normalConf = 0;
		int voteCount = 0;
		IFeature<double[]>[] featureGramFeatures = featureGram.getFeatures();
		int featureCount = featureGramFeatures.length;
		long startTime = atTime * featureCount;
		for (IFeature<double[]> f : featureGramFeatures) { 
			double data[] = f.getData();
			AnomalyResult ar = multiDetector.isAnomaly(startTime++, data); 	
			boolean isFeatureAnomaly = ar.isAnomaly(); 
			if (isFeatureAnomaly)
				voteCount++;
			abnormalConf += ar.getAnomalyConfidence();
			normalConf += ar.getNormalConfidence();
		}
		
		double requiredVotes = featureCount * acrossFeaturesVotePercentage;
		if (requiredVotes < 0)
			requiredVotes = 1;
		abnormalConf = abnormalConf / featureCount; 
		normalConf = normalConf / featureCount; 
		boolean isAnomaly = voteCount >= requiredVotes;
		return new AnomalyResult(isAnomaly, abnormalConf, normalConf); 
	}

	@Override
	public void beginNewDeployment() {
		multiDetector.beginNewDeployment();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(acrossFeaturesVotePercentage);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((multiDetector == null) ? 0 : multiDetector.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof FeatureAnomalyDetector))
			return false;
		FeatureAnomalyDetector other = (FeatureAnomalyDetector) obj;
		if (Double.doubleToLongBits(acrossFeaturesVotePercentage) != Double
				.doubleToLongBits(other.acrossFeaturesVotePercentage))
			return false;
		if (multiDetector == null) {
			if (other.multiDetector != null)
				return false;
		} else if (!multiDetector.equals(other.multiDetector))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FeatureAnomalyDetector [multiDetector=" + multiDetector + ", acrossFeaturesVotePercentage="
				+ acrossFeaturesVotePercentage + "]";
	}

}
