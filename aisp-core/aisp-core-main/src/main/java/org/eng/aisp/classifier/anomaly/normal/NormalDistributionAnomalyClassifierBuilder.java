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

import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.anomaly.AnomalyDetectorClassifierBuilder;

public class NormalDistributionAnomalyClassifierBuilder extends AnomalyDetectorClassifierBuilder {

	private static final long serialVersionUID = -8846511285902901787L;
	private double normalStddevMultiplier = NormalDistributionAnomalyClassifier.DEFAULT_NORMAL_STDDEV_MULTIPLIER;
	private int samplesToAdaptToEnvironment = NormalDistributionAnomalyClassifier.DEFAULT_SAMPLES_TO_ADAPT_TO_ENV;


	/**
	 * @param normalStddevMultiplier the normalStddevMultiplier to set
	 */
	public NormalDistributionAnomalyClassifierBuilder setNormalStddevMultiplier(double normalStddevMultiplier) {
		this.normalStddevMultiplier = normalStddevMultiplier;
		return this;
	}

	/**
	 * @param samplesToLearnEnv the samplesToLearnEnv to set
	 */
	public NormalDistributionAnomalyClassifierBuilder setSamplesToAdaptToEnvironment(int samplesToAdaptToEnvironment) {
		this.samplesToAdaptToEnvironment = samplesToAdaptToEnvironment;
		return this;
	}


	/**
	 * Override so we don't have to set the builder.
	 */
	@Override
	public NormalDistributionAnomalyClassifier build() throws AISPException {
		return new  NormalDistributionAnomalyClassifier(this.getTransform(), this.getFeatureGramExtractors(), this.votePercent, this.normalStddevMultiplier, this.samplesToAdaptToEnvironment);
	}

}
