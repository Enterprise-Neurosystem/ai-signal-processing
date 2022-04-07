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
package org.eng.aisp.classifier.cnn;

import org.eng.aisp.classifier.AbstractClassifierBuilder;
import org.eng.aisp.classifier.IFixableClassifierBuilder;
import org.eng.aisp.feature.extractor.IFeatureExtractor;

public class CNNClassifierBuilder extends AbstractClassifierBuilder<double[], double[]> implements IFixableClassifierBuilder<double[], double[]>{ 
	
	private static final long serialVersionUID = -3069822675927912162L;

	protected int nEpochs = CNNClassifier.DEFAULT_NUM_EPOCH;
	protected int batchSize = CNNClassifier.DEFAULT_BATCH_SIZE;
	protected boolean useDiskCache = CNNClassifier.DEFAULT_USE_DISK_CACHE;
	protected int trainingFolds = CNNClassifier.DEFAULT_TRAINING_FOLDS;
	protected double minScoreChangePerEpoch = CNNClassifier.DEFAULT_MIN_SCORE_CHANGE_PER_EPOCH;
	protected int epochScoreHistorySize = CNNClassifier.DEFAULT_EPOCH_SCORE_HISTORY_SIZE;

	protected boolean setEarlyStoppingEnabled = CNNClassifier.DEFAULT_USE_EARLY_STOPPING;


//	private CNNClassifier modelerConstruct() {
////		return new CNNClassifier(transform, featureExtractor, featureProcessor, windowSizeMsec, windowShiftMsec, useDiskCache, nEpochs);
//		return new CNNClassifier(transform, this.getFeatureGramExtractor(), true, useDiskCache, nEpochs);
//
//	}
	
	public CNNClassifierBuilder(IFeatureExtractor<double[], double[]> fe) { 
		super(fe);
		this.transform = CNNClassifier.DEFAULT_TRANSFORMS;
		this.featureProcessor = CNNClassifier.DEFAULT_FEATURE_PROCESSOR;
		this.windowSizeMsec = CNNClassifier.DEFAULT_WINDOW_SIZE_MSEC;
		this.windowShiftMsec = CNNClassifier.DEFAULT_WINDOW_SHIFT_MSEC;
		this.windowShiftMsec = CNNClassifier.DEFAULT_WINDOW_SHIFT_MSEC;
	}

	public CNNClassifierBuilder() { 
		this(CNNClassifier.DEFAULT_FEATURE_EXTRACTOR);
	}
	
	@Override
	public CNNClassifier build() {
		double minScoreChangePerEpoch;
		if (this.setEarlyStoppingEnabled) 
			minScoreChangePerEpoch = this.minScoreChangePerEpoch;
		else
			minScoreChangePerEpoch = 0; 
		return new CNNClassifier(transform, this.getFeatureGramExtractors().get(0), false, useDiskCache, nEpochs, batchSize, trainingFolds, epochScoreHistorySize, minScoreChangePerEpoch);
	}
	
	public CNNClassifierBuilder setNumberOfEpochs(int nEpochs) {
		this.nEpochs = nEpochs;
		return this; 
	}
	
	public CNNClassifierBuilder setBatchSize(int batchSize) {
		this.batchSize = batchSize;
		return this; 
	}
	
	public CNNClassifierBuilder setUseDiskCache(boolean useDiskCache) {
		this.useDiskCache= useDiskCache;
		return this; 
	}

	/**
	 * Sets the number of training folds to effective define the validation data set as 1/trainingFolds of the training data.
	 * @param trainingFolds number of folds, 1 of which is used as the validation data set during training. 
	 * Set to 0 to use all the training data as the validation data.
	 * @return
	 */
	public CNNClassifierBuilder setTrainingFolds(int trainingFolds) {
		this.trainingFolds= trainingFolds;
		return this; 
	}

	/**
	 * @param minScoreChangePerEpoch the minScoreChangePerEpoch to set.  Set to 0 to disable early stopping of training.
	 */
	public CNNClassifierBuilder setMinScoreChangePerEpoch(double minScoreChangePerEpoch) {
		this.minScoreChangePerEpoch = minScoreChangePerEpoch;
		return this; 
	}

	/**
	 * @param epochScoreHistorySize the epochScoreHistorySize to set
	 */
	public CNNClassifierBuilder setEpochScoreHistorySize(int epochScoreHistorySize) {
		this.epochScoreHistorySize = epochScoreHistorySize;
		return this; 
	}
	
	/**
	 * Enable/disable early stopping of training based on early stopping metrics. 
	 * @param enabled 
	 * @return
	 */
	public CNNClassifierBuilder setEarlyStoppingEnabled(boolean enabled) {
		this.setEarlyStoppingEnabled = enabled;
		return this; 
	}
	
	
}
