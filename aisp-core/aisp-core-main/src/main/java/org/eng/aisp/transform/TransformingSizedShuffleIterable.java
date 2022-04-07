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
package org.eng.aisp.transform;

import org.eng.aisp.ILabeledDataWindow;
import org.eng.util.IShuffleIterable;
import org.eng.util.ISizedShuffleIterable;
import org.eng.util.MutatingShuffleIterable;

/**
 * A transforming iterable over an ISizedShuffleIterable of labeled windows.
 * 
 * @author dawood
 * @param <WINDATA> data type returned by the getData() method of the labeled windows.
 */
public class TransformingSizedShuffleIterable<WINDATA> extends MutatingShuffleIterable<ILabeledDataWindow<WINDATA>, ILabeledDataWindow<WINDATA>>  
			implements ISizedShuffleIterable<ILabeledDataWindow<WINDATA>> {



	public TransformingSizedShuffleIterable(String trainingLabel,
//			ISizedShuffleIterable<ILabeledDataWindow<WINDATA>> data, ITrainingWindowTransform<WINDATA> transform) {
			IShuffleIterable<ILabeledDataWindow<WINDATA>> data, ITrainingWindowTransform<WINDATA> transform) {
		super(data, 
				new TransformingIterable.TransformingMutator<WINDATA>(trainingLabel, transform), 
				false);
	}

	protected TransformingIterable.TransformingMutator<WINDATA> getTransformingMutator() {
		return ((TransformingIterable.TransformingMutator<WINDATA>)this.mutator);
	}

	/**
	 * Override to recreate the mutator with a new transform instance, which may be stateful.
	 */
	@Override
	public ISizedShuffleIterable<ILabeledDataWindow<WINDATA>> newIterable(Iterable<String> references) {
		TransformingIterable.TransformingMutator<WINDATA> mutator = getTransformingMutator(); 
		return new  TransformingSizedShuffleIterable<WINDATA>(
				mutator.trainingLabel,
				((IShuffleIterable<ILabeledDataWindow<WINDATA>>)this.iterable).newIterable(references),
				mutator.transform.newInstance()
				);
	}

//	@Override
//	public ISizedShuffleIterable<ILabeledDataWindow<WINDATA>> shuffle(long seed) {
//		return (ISizedShuffleIterable<ILabeledDataWindow<WINDATA>>)super.shuffle(seed);
//	}
//
//	@Override
//	public ISizedShuffleIterable<ILabeledDataWindow<WINDATA>> shuffle() {
//		return (ISizedShuffleIterable<ILabeledDataWindow<WINDATA>>)super.shuffle();
//	}



}
