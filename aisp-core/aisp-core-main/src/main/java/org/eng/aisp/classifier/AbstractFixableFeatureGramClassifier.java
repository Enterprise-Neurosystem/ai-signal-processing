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

import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.transform.ITrainingWindowTransform;

/**
 * A trainable classifier that uses instances of IFeatureGramClassifier and IFixedFeatureGramClassifier to do the train and classify work. 
 * Labeled feature extraction is done in this instance, the resulting feature grams are used by  IFeatureGramClassifier instance to do the training.
 * Training produces a FixedMVIClassifier and is used for to create FixedFeatureGramClassifier that does the classification.
 * <p>
 * Note: All classifiers could have been implemented using this class design, but we will not migrate pre-existing classes to maintain backwards serialization compatibility.
 * This class was originally developed to support the MVI server where REST APIs on feature grams are used for training and classification.  This class
 * is designed, then, to isolate the feature gram-based train and classify operations.
 * 
 * @see {@link IFeatureGramClassifier}, {@link IFixedFeatureGramClassifier}, and {@link FixedFeatureGramClassifier}.
 *
 */
public abstract class AbstractFixableFeatureGramClassifier extends AbstractFixableFeatureExtractingClassifier<double[],double[]> implements IFixableClassifier<double[]> {

	private static final long serialVersionUID = -6661364138783762368L;

	protected AbstractFixableFeatureGramClassifier(boolean preShuffleData, ITrainingWindowTransform<double[]> transform,
			List<IFeatureGramDescriptor<double[], double[]>> featureGramExtractors, boolean useMemoryCache,
			boolean useDiskCache, boolean softReferenceFeatures) {
		super(preShuffleData, transform, featureGramExtractors, useMemoryCache, useDiskCache, softReferenceFeatures);
	}

	@Override
	protected IFixedClassifier<double[]> trainFixedClassifierOnFeatures( Iterable<? extends ILabeledFeatureGram<double[]>[]> features) throws AISPException {
		IFeatureGramClassifier<double[]>	fgClassifier = newFeatureGramClassifier();
		fgClassifier.train(this.primaryTrainingLabel, features);
		IFixedFeatureGramClassifier<double[]>	ffgClassifier = newFixedFeatureGramClassifier(fgClassifier);
		IFixedClassifier<double[]> fixedClassifier = new FixedFeatureGramClassifier<double[],double[]>(this.primaryTrainingLabel, this.featureGramDescriptors, ffgClassifier); 
		return fixedClassifier;
	}

	/**
	 * Called during {@link #trainFixedClassifierOnFeatures(Iterable)} to create
	 * a new IFeatureGramClassifier instance that is used for training and then provided to {@link #newFixedFeatureGramClassifier(IFeatureGramClassifier)}
	 * to create the  IFixedFeatureGramClassifier returned from training.
	 * @return never null.
	 */
	protected abstract IFeatureGramClassifier<double[]> newFeatureGramClassifier();

	/**
	 * Called during {@link #trainFixedClassifierOnFeatures(Iterable)} to create IFixedFeatureGramClassifier instance after training 
	 * using the given IFeatureGramClassifier to train.  This is used to creat the FixedFeatureGramClassifier.
	 * @param fgc
	 * @return never null.  Implementations may choose to return the given IFeatureGramClassifier.  For example, if there is no training state in the given
	 * instance, as for example in a REST API implementation (e.g. MVI).
	 */
	protected abstract IFixedFeatureGramClassifier<double[]> newFixedFeatureGramClassifier(IFeatureGramClassifier<double[]> fgc);

}
