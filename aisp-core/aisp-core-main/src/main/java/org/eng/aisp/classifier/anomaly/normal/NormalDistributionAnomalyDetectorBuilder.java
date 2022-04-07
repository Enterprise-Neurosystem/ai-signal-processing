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
package org.eng.aisp.classifier.anomaly.normal;

import org.eng.aisp.classifier.anomaly.IAnomalyDetector;
import org.eng.aisp.classifier.anomaly.IAnomalyDetectorBuilder;

public class NormalDistributionAnomalyDetectorBuilder implements IAnomalyDetectorBuilder<Double> {

	private static final long serialVersionUID = -5122311552097604513L;
	private final double normalStddevMultiplier;
	private final int samplesToAdaptToEnvironment;
	
	public NormalDistributionAnomalyDetectorBuilder(int samplesToAdaptToEnvironment, double normalStddevMultiplier) {
		this.normalStddevMultiplier = normalStddevMultiplier;
		this.samplesToAdaptToEnvironment = samplesToAdaptToEnvironment;
	}

	public NormalDistributionAnomalyDetectorBuilder(double normalStddevMultiplier) {
		this(-1, normalStddevMultiplier);
	}

	@Override
	public IAnomalyDetector<Double> build() {
		if (samplesToAdaptToEnvironment < 0)
			return new NormalDistributionAnomalyDetector(this.normalStddevMultiplier);
		else
			return new NormalDistributionAnomalyDetector(samplesToAdaptToEnvironment, this.normalStddevMultiplier);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(normalStddevMultiplier);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + samplesToAdaptToEnvironment;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof NormalDistributionAnomalyDetectorBuilder))
			return false;
		NormalDistributionAnomalyDetectorBuilder other = (NormalDistributionAnomalyDetectorBuilder) obj;
		if (Double.doubleToLongBits(normalStddevMultiplier) != Double.doubleToLongBits(other.normalStddevMultiplier))
			return false;
		if (samplesToAdaptToEnvironment != other.samplesToAdaptToEnvironment)
			return false;
		return true;
	}

}
