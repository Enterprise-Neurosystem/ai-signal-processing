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

import java.util.Arrays;
import java.util.List;

import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.classifier.anomaly.FixedAnomalyDetectorClassifier;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.IdentityFeatureGramDescriptor;
import org.eng.aisp.feature.processor.vector.LpMagnitudeFeatureProcessor;

/**
 * Extends the super class to make use of it as an online learning classifier.
 * <i>Training</i> of the model is done during a warm up time during calls to {@link #classify(org.eng.aisp.IDataWindow)}.
 * Until the warm up number of samples have been classified, {@link #classify(org.eng.aisp.IDataWindow)} will always
 * generate "normal" classifications.  After warmup, anomalies can be generated if they exceed a threshold from the single mean
 * for each feature element, as defined by the stddev multiplier.
 * @author DavidWood
 *
 */
public class OnlineNormalDistributionAnomalyClassifier extends FixedAnomalyDetectorClassifier implements IFixedClassifier<double[]> {

	public static final double DEFAULT_NORMAL_STDDEV_MULTIPLIER = 3.0;
	public static final int DEFAULT_SAMPLES_TO_LEARN_ENV = 100;
	public static final UpdateMode DEFAULT_UPDATE_MODE = UpdateMode.NONE;
	public static final double DEFAULT_VOTE_PERCENT = 0.50;
	/**
	 * Creates to  operate on the magnitude of the raw signal where magnitude is calculated using Lp distance with p=0.5.
	 */
	public final static IFeatureGramDescriptor<double[], double[]> DEFAULT_FEATURE_GRAM_EXTRACTOR = new IdentityFeatureGramDescriptor(new LpMagnitudeFeatureProcessor()); 

	private static final long serialVersionUID = -1419009631826304616L;


	public OnlineNormalDistributionAnomalyClassifier(String labelName, int samplesToLearnNormalEnv) {
		this(labelName, Arrays.asList(DEFAULT_FEATURE_GRAM_EXTRACTOR), DEFAULT_VOTE_PERCENT, DEFAULT_UPDATE_MODE, samplesToLearnNormalEnv, DEFAULT_NORMAL_STDDEV_MULTIPLIER);
	}

	public OnlineNormalDistributionAnomalyClassifier(String labelName,
			List<IFeatureGramDescriptor<double[], double[]>> fgeList,
			double votePercent,
			UpdateMode updateMode, 
			int samplesToLearnNormalEnv, 
			double normalStddevMultiplier) {
		super(labelName, fgeList, 
				new NormalDistributionAnomalyDetectorBuilder(normalStddevMultiplier),	// With no online adaptation.
				votePercent, updateMode, samplesToLearnNormalEnv);
	}

}
