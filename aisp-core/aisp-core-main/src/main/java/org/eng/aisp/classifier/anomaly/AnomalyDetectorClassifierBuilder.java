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
import org.eng.aisp.classifier.AbstractClassifierBuilder;
import org.eng.aisp.classifier.IClassifierBuilder;
import org.eng.aisp.classifier.anomaly.FixedAnomalyDetectorClassifier.UpdateMode;

public class AnomalyDetectorClassifierBuilder extends AbstractClassifierBuilder<double[], double[]> implements IClassifierBuilder<double[], double[]> {
	
	private static final long serialVersionUID = -1717217394788614117L;
	protected UpdateMode updateMode =  AnomalyDetectorClassifier.DEFAULT_UPDATE_MODE;
	protected IAnomalyDetectorBuilder<Double> detectorBuilder = null; 
	protected double votePercent =  AnomalyDetectorClassifier.DEFAULT_VOTE_PERCENT;

	
	public AnomalyDetectorClassifierBuilder() {
		this.setFeatureGramDescriptor(AnomalyDetectorClassifier.DEFAULT_FEATURE_GRAM_EXTRACTOR);
	}

	@Override
	public AnomalyDetectorClassifier build() throws AISPException {
		if (detectorBuilder == null)
			throw new AISPException("Detector builder must be set.");
		return new AnomalyDetectorClassifier(this.getTransform(), this.getFeatureGramExtractors(), this.detectorBuilder, this.votePercent, this.updateMode);
	}

	/**
	 * @param mode the mode to set
	 */
	public AnomalyDetectorClassifierBuilder setUpdateMode(UpdateMode mode) {
		this.updateMode = mode;
		return this;
	}


	/**
	 * @param votePercent the votePercent to set
	 */
	public AnomalyDetectorClassifierBuilder setVotePercent(double votePercent) {
		this.votePercent = votePercent;
		return this;
	}

	/**
	 * @param detectorBuilder the detectorBuilder to set
	 */
	public AnomalyDetectorClassifierBuilder setDetectorBuilder(IAnomalyDetectorBuilder<Double> detectorBuilder) {
		this.detectorBuilder = detectorBuilder;
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((detectorBuilder == null) ? 0 : detectorBuilder.hashCode());
		result = prime * result + ((updateMode == null) ? 0 : updateMode.hashCode());
		long temp;
		temp = Double.doubleToLongBits(votePercent);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AnomalyDetectorClassifierBuilder))
			return false;
		AnomalyDetectorClassifierBuilder other = (AnomalyDetectorClassifierBuilder) obj;
		if (detectorBuilder == null) {
			if (other.detectorBuilder != null)
				return false;
		} else if (!detectorBuilder.equals(other.detectorBuilder))
			return false;
		if (updateMode != other.updateMode)
			return false;
		if (Double.doubleToLongBits(votePercent) != Double.doubleToLongBits(other.votePercent))
			return false;
		return true;
	}


}
