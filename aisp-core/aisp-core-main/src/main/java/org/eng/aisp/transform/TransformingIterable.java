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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.ILabeledDataWindow;
import org.eng.util.IMutator;
import org.eng.util.MutatingIterable;
import org.eng.util.MutatingIterator;

/**
 * Applies an IWindowTransform to each input labeled window to produce 1 or more labeled windows.
 * @author dawood
 *
 * @param <WINDATA>
 */
public class TransformingIterable<WINDATA> extends MutatingIterable<ILabeledDataWindow<WINDATA>, ILabeledDataWindow<WINDATA>> implements Iterable<ILabeledDataWindow<WINDATA>> {
	
//	protected final Iterable<? extends ILabeledDataWindow<WINDATA>> data;
	protected final ITrainingWindowTransform<WINDATA> transform;
	protected final String trainingLabel;

	static class TransformingMutator<WINDATA> implements IMutator<ILabeledDataWindow<WINDATA>, ILabeledDataWindow<WINDATA>>, Serializable {

		private static final long serialVersionUID = -3304912447782479159L;
		protected final String trainingLabel;
		protected final ITrainingWindowTransform<WINDATA> transform;

		public TransformingMutator(String trainingLabel, ITrainingWindowTransform<WINDATA> transform) {
			this.trainingLabel = trainingLabel;
			this.transform = transform;
		}

		@Override
		public List<ILabeledDataWindow<WINDATA> >  mutate(ILabeledDataWindow<WINDATA> item) {
			Properties labels = item.getLabels();
			String labelValue = labels.getProperty(trainingLabel);
			List<ILabeledDataWindow<WINDATA>> ldwList = null;
			if (labelValue != null) {	// Only pass windows with the training label to the transformer.
				// Get the 1 or more transformed windows for the current labeled data window.
				Iterable<ILabeledDataWindow<WINDATA>> transformedLDW = (Iterable<ILabeledDataWindow<WINDATA>>) transform.apply(trainingLabel, item);
				ldwList = new ArrayList<>();
				for  (ILabeledDataWindow<WINDATA> ldw : transformedLDW) 
					ldwList.add(ldw);
				if (ldwList.isEmpty())
					ldwList = null;
			}
			return ldwList;
		}
		
	}
	
	protected ITrainingWindowTransform<WINDATA> getTransform() {
		return this.transform; 
	}
	/**
	 * 
	 * @param data the data to be transformed.
	 * @param transform used to transform the window over which we are iterating. 
	 */
	public TransformingIterable(String trainingLabel, Iterable<? extends ILabeledDataWindow<WINDATA>> data, ITrainingWindowTransform<WINDATA> transform) {
		super((Iterable<ILabeledDataWindow<WINDATA>>)data, new TransformingMutator<WINDATA>(trainingLabel, transform));
		this.trainingLabel = trainingLabel;
		this.transform = transform;
	}

	/**
	 * Override only to create the new instance of the transform.
	 */
	@Override
	public Iterator<ILabeledDataWindow<WINDATA>> iterator() {
		// We create a new instance for each iterator in case the transform is stateful.
		return new MutatingIterator<ILabeledDataWindow<WINDATA>,ILabeledDataWindow<WINDATA>>(this.iterable.iterator(), 
				new TransformingMutator<WINDATA>(trainingLabel, this.transform.newInstance()));
	}
	


}
