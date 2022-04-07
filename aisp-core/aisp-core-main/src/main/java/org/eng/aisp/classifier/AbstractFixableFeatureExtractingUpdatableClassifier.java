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
package org.eng.aisp.classifier;

import java.util.function.Predicate;

import org.eng.aisp.AISPException;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.pipeline.LabeledFeatureIterable;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.transform.ITrainingWindowTransform;

/**
 * Extends the super class to extract the features from the training data windows.
 * Implements {@link #updateFixedClassifierOnData(Iterable)} using the subclass implementation 
 * of {@link #updateFixedClassifierOnFeatures(Iterable)}.
 * 
 * @author dawood
 */
public abstract class AbstractFixableFeatureExtractingUpdatableClassifier<WINDATA, FDATA> extends AbstractFixableFeatureExtractingClassifier<WINDATA, FDATA> implements IFixableUpdatableClassifier<WINDATA> {


	private static final long serialVersionUID = 6785620591840039879L;
	

	/**
	 * @param preShuffleData
	 * @param transforms
	 * @param featureExtractor
	 * @param windowSizeMsec
	 * @param windowShiftMsec
	 * @param featureProcessor
	 * @param useMemoryCache
	 * @param useDiskCache
	 */
	protected AbstractFixableFeatureExtractingUpdatableClassifier(boolean preShuffleData, ITrainingWindowTransform<WINDATA> transforms, 
				IFeatureExtractor<WINDATA, FDATA> featureExtractor, int windowSizeMsec, int windowShiftMsec, 
				IFeatureProcessor<FDATA> featureProcessor, boolean useMemoryCache, boolean useDiskCache) {
		super(preShuffleData, transforms, featureExtractor, featureProcessor, windowSizeMsec, windowShiftMsec, useMemoryCache, useDiskCache);
	}
	
	public AbstractFixableFeatureExtractingUpdatableClassifier(boolean preShuffleData, ITrainingWindowTransform<WINDATA> transforms,
			IFeatureGramDescriptor<WINDATA,FDATA> fge, boolean useMemoryCache, boolean useDiskCache) {
		super(preShuffleData, transforms, fge, useMemoryCache, useDiskCache);
	}

	/**
	 * Check to make sure there is some data, then call the subclass implementation of {@link #trainFixedClassifierOnData(String, Iterable)}.
	 */
	@Override
	public void update(Iterable<? extends ILabeledDataWindow<WINDATA>> incrementalData) throws AISPException {
		if (incrementalData == null || incrementalData.iterator().hasNext() == false) 
			throw new IllegalArgumentException("Incremental training data set is null or empty");
		this.classifier = updateFixedClassifierOnData(incrementalData);
		
	}
	
	
	protected IFixedClassifier<WINDATA> updateFixedClassifierOnData(
			Iterable<? extends ILabeledDataWindow<WINDATA>> incrementalData) throws AISPException {
	
		Predicate<ILabeledDataWindow<WINDATA>> dataPredicate = new TrainingDataPredicate<WINDATA>(primaryTrainingLabel,true);
		Iterable<ILabeledFeatureGram<FDATA>[]> incrementalFi = new LabeledFeatureIterable<WINDATA,FDATA>(incrementalData, 
								dataPredicate, featureGramDescriptors); 
		return updateFixedClassifierOnFeatures(incrementalFi);
	}


	
	/**
	 * Incremental training implementations should override this method.
	 * @param incrementalData
	 * @return
	 * @throws AISPException
	 */
	protected abstract IFixedClassifier<WINDATA> updateFixedClassifierOnFeatures(Iterable<? extends ILabeledFeatureGram<FDATA>[]> incrementalFeatures) throws AISPException;


}
