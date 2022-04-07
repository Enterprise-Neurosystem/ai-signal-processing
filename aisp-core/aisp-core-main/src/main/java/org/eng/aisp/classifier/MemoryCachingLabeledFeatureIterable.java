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

import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.util.CachingIterable;

/**
 * Extends the superclass as a convenience to make the generic type system easier to use for ILabeledFeature<>.
 * It is recommended to use this only when the iterable of features is very large, causing the cache
 * behind LabeledFeatureIterable (inside FeatureExtractingPipeline) to be thrashed.
 * 
 * @param <FDATA> type of data returned by {@link IFeature.getData()}
 */
public class MemoryCachingLabeledFeatureIterable<FDATA> extends CachingIterable<ILabeledFeatureGram<FDATA>[]> implements Iterable<ILabeledFeatureGram<FDATA>[]> {

	@SuppressWarnings("unchecked")
	public MemoryCachingLabeledFeatureIterable(Iterable<? extends ILabeledFeatureGram<FDATA>[]> iterable) {
		super(((Iterable<ILabeledFeatureGram<FDATA>[]>)iterable));
	}
}
