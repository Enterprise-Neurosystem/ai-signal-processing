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
package org.eng.aisp.classifier.dcase;

import org.eng.aisp.classifier.cnn.CNNClassifierBuilder;
import org.eng.aisp.feature.extractor.IFeatureExtractor;

public class DCASEClassifierBuilder extends CNNClassifierBuilder {

	private static final long serialVersionUID = -8565914346541958961L;
	
	public DCASEClassifierBuilder(IFeatureExtractor<double[], double[]> fe) { 
		super(fe);
		this.setNumberOfEpochs(DCASEClassifier.DEFAULT_NUM_EPOCH);
	}

	public DCASEClassifierBuilder() { 
		this(DCASEClassifier.DEFAULT_FEATURE_EXTRACTOR);	
	}

	@Override
	public DCASEClassifier build() {
		double minScoreChangePerEpoch;
		if (this.setEarlyStoppingEnabled) 
			minScoreChangePerEpoch = this.minScoreChangePerEpoch;
		else
			minScoreChangePerEpoch = 0; 
		return new DCASEClassifier(transform, this.getFeatureGramExtractors().get(0), true, useDiskCache, 
				nEpochs, batchSize, trainingFolds, epochScoreHistorySize, minScoreChangePerEpoch);

	}

}
