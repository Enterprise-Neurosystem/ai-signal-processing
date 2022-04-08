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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.LabeledDataWindow;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.util.IMutator;
import org.eng.util.MutatingIterable;

/**
 * Allows the conversion of an iterable of IDataWindows to ILabeledDataWindows using a given classifier. 
 * 
 * @author DavidWood
 * @param <DATA>	the data type contained in the IDataWindow, for example double[].
 */
public class DataWindowLabelingIterable<DATA> extends MutatingIterable<IDataWindow<DATA>, ILabeledDataWindow<DATA>> {

	/**
	 * Mutator that converts the ILabeledDataWindow to an IDataWindow.
	 * @author DavidWood
	 * @param <DATA>
	 */
	static class LabelingMutator<DATA> implements IMutator<IDataWindow<DATA>, ILabeledDataWindow<DATA>> {

		private IFixedClassifier<DATA> classifier;
		private String[] trainingLabels;
		LabelingMutator(IFixedClassifier<DATA> classifier) {
			this.classifier = classifier;
			String trainingLabel = classifier.getTrainedLabel();
			if (trainingLabel == null)
				throw new IllegalArgumentException("Could not get training label from classifier.  Has it been trained?");
				
			this.trainingLabels = IClassifier.parseMultiLabels(trainingLabel);
			if (trainingLabels == null || trainingLabels.length == 0)
				throw new IllegalArgumentException("Could not identify training label(s)");
		}

		@Override
		public List<ILabeledDataWindow<DATA>> mutate(IDataWindow<DATA> item) {
			Properties labels = new Properties();
			if (classifier != null) {
				Map<String, Classification> cmap;
				try {
					cmap = classifier.classify(item);
				} catch (AISPException e) {
					return null;
				}
				for (String labelName : trainingLabels) {
					Classification c = cmap.get(labelName);
					if (c != null)
						labels.setProperty(labelName, c.getLabelValue());
				}
			}
			List<ILabeledDataWindow<DATA>> ldwList = new ArrayList<ILabeledDataWindow<DATA>>();
			ILabeledDataWindow<DATA> ldw = new LabeledDataWindow<DATA>(item, labels);
			ldwList.add(ldw);
			return ldwList;
		}
		
	}

	/**
	 * Convert IDataWindows to ILabeledDataWindows w/o any labels.
	 * This is sometime useful depending on the iterables needed.
	 * @param iterable
	 */
	public DataWindowLabelingIterable(Iterable<? extends IDataWindow<DATA>> iterable) {
		super((Iterable<IDataWindow<DATA>>) iterable, new DataWindowLabelingIterable.LabelingMutator<DATA>(null));
	}

	public DataWindowLabelingIterable(Iterable<? extends IDataWindow<DATA>> iterable, IFixedClassifier<DATA> classifier) {
		super((Iterable<IDataWindow<DATA>>) iterable, new DataWindowLabelingIterable.LabelingMutator<DATA>(classifier));
		if (classifier == null)
			throw new IllegalArgumentException("null clasifier not allowed");
			
	}
}