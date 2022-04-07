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

import org.eng.aisp.ILabeledDataWindow;
import org.eng.util.IShuffleIterable;
import org.eng.util.IShuffleMutator;
import org.eng.util.MutatingShuffleIterable;

/**
 * Provides filtering of labeled data windows to include only those with a given label name.
 * This implementation supports shuffling via a constructor that requires an IShuffleIterable instance.
 * If you don't have an IShuffleIterable, then use LabeledWindowIterable.
 */
//public class LabeledWindowShuffleIterable<ITEM extends ILabeledDataWindow<?>> extends AbstractFilteringShuffleIterable<ITEM> implements IShuffleIterable<ITEM> {
public class LabeledWindowShuffleIterable<ITEM extends ILabeledDataWindow<?>> extends MutatingShuffleIterable<ITEM, ITEM> implements IShuffleIterable<ITEM> {

	private static class LabelValueFilter<ITEM extends ILabeledDataWindow<?>>  implements IShuffleMutator<ITEM, ITEM> { 

		private String labelName;
		private String allowedLabeValue; 

		public LabelValueFilter(String labelName) {
			this(labelName, null);
		}

		public LabelValueFilter(String labelName, String labelValue) {
			this.labelName = labelName;
			this.allowedLabeValue = labelValue;
		}


		@Override
		public List<ITEM> mutate(ITEM item) {
			String labelValue = item.getLabels().getProperty(labelName);
			boolean include = labelValue != null && (allowedLabeValue == null || allowedLabeValue.equals(labelValue));
			List<ITEM> items = null;
			if (include)  {
				items = new ArrayList<ITEM>();
				items.add(item);
			}
			return items;
		}

		@Override
		public boolean isUnaryMutator() {
			return false;
		}
		
	}

	/**
	 * Convenience on {@link #LabeledWindowShuffleIterable(IShuffleIterable, boolean, String)} that forces shuffle repeatability.
	 */
	public LabeledWindowShuffleIterable(IShuffleIterable<ITEM> iterable, String labelName, String labelValue) {
		super(iterable,  new LabelValueFilter<ITEM>(labelName, labelValue));
	}

	public LabeledWindowShuffleIterable(IShuffleIterable<ITEM> sounds, String trainingLabel) {
		this(sounds, trainingLabel, null);	
	}

//	public LabeledWindowShuffleIterable(IShuffleIterable<ITEM> iterable, boolean enforceShuffleRepeatability, String labelName,String labelValue) {
//		super(iterable,  new LabelValueFilter<ITEM>(labelName, labelValue));
////		this.labeledWindowIterable = new LabeledWindowIterable<ITEM>(iterable, labelName, labelValue);
//	}
//
//	public LabeledWindowShuffleIterable(IShuffleIterable<ITEM> iterable, boolean enforceShuffleRepeatability, String labelName) {
//		super(iterable, null, enforceShuffleRepeatability);		// Null means we have to implement createFilteringIterator()
//		this.labeledWindowIterable = new LabeledWindowIterable<ITEM>(iterable, labelName);
//	}

//	protected LabeledWindowShuffleIterable(IShuffleIterable<ITEM> iterable, LabeledWindowIterable<ITEM> labeledWindowIterable, AbstractFilteringShuffleIterable<ITEM> requester) {
//		super(iterable, null, requester);	// Null means we have to implement createFilteringIterator()
//		this.labeledWindowIterable = new LabeledWindowIterable<ITEM>(iterable, labeledWindowIterable);
//	}

//	@Override
//	protected IShuffleIterable<ITEM> newIterable(Iterable<String> references, AbstractFilteringShuffleIterable<ITEM> requester) {
//		return new LabeledWindowShuffleIterable<ITEM>(((IShuffleIterable<ITEM>)this.iterable).newIterable(references), this.labeledWindowIterable, requester);
//	}
//
//	@Override
//	protected Iterator<ITEM> createFilteringIterator() {
//		return labeledWindowIterable.iterator();  
//	}

}
