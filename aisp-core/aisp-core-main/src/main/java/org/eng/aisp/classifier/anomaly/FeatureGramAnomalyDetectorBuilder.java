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

import org.eng.aisp.feature.IFeatureGram;

class FeatureGramAnomalyDetectorBuilder implements IFeatureGramAnomalyDetectorBuilder {

	private IAnomalyDetectorBuilder<Double> detectorBuilder;
	private double withinFeatureVotePercentage;
	private double acrossFeaturesVotePercentage;

	FeatureGramAnomalyDetectorBuilder(IAnomalyDetectorBuilder<Double> detectorBuilder, double withinFeatureVotePercentage, double acrossFeaturesVotePercentage) {
		this.detectorBuilder = detectorBuilder;
		this.withinFeatureVotePercentage = withinFeatureVotePercentage;
		this.acrossFeaturesVotePercentage = acrossFeaturesVotePercentage	;
	}
	
	@Override
	public IAnomalyDetector<IFeatureGram<double[]>> build(int featureLen) {
		return new FeatureAnomalyDetector(detectorBuilder, featureLen, withinFeatureVotePercentage, acrossFeaturesVotePercentage);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(acrossFeaturesVotePercentage);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((detectorBuilder == null) ? 0 : detectorBuilder.hashCode());
		temp = Double.doubleToLongBits(withinFeatureVotePercentage);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof FeatureGramAnomalyDetectorBuilder))
			return false;
		FeatureGramAnomalyDetectorBuilder other = (FeatureGramAnomalyDetectorBuilder) obj;
		if (Double.doubleToLongBits(acrossFeaturesVotePercentage) != Double
				.doubleToLongBits(other.acrossFeaturesVotePercentage))
			return false;
		if (detectorBuilder == null) {
			if (other.detectorBuilder != null)
				return false;
		} else if (!detectorBuilder.equals(other.detectorBuilder))
			return false;
		if (Double.doubleToLongBits(withinFeatureVotePercentage) != Double
				.doubleToLongBits(other.withinFeatureVotePercentage))
			return false;
		return true;
	}
	
}