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
package org.eng.aisp.util;

import org.eng.aisp.IDataWindow;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.util.IShuffleIterable;
import org.eng.util.MutatingShuffleIterable;

/**
 * Allows the conversion of an iterable of IDataWindows to ILabeledDataWindows.
 * 
 * @author DavidWood
 * @param <DATA>	the data type contained in the IDataWindow, for example double[].
 */
public class DataWindowLabelingShuffleIterable<DATA> extends MutatingShuffleIterable<IDataWindow<DATA>, ILabeledDataWindow<DATA>> {


	public DataWindowLabelingShuffleIterable(IShuffleIterable<? extends IDataWindow<DATA>> iterable, IFixedClassifier<DATA> classifier) {
		super((IShuffleIterable<IDataWindow<DATA>>) iterable, new DataWindowLabelingIterable.LabelingMutator<DATA>(classifier), true);
	}
}