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

import java.util.Arrays;
import java.util.Collection;

import org.eng.aisp.ILabeledDataWindow;
import org.eng.util.FilteringIterable;

/**
 * An iterable that produces only labeled data windows that 1) are labeled with given label, and 2) optionally, have a set of allowed label values for the given label. 
 * @author DavidWood
 *
 * @param <ITEM>
 */
public class LabeledWindowIterable<ITEM extends ILabeledDataWindow<?>> extends FilteringIterable<ITEM> implements Iterable<ITEM> {

	static class LabelValueFilter<ITEM extends ILabeledDataWindow<?>>  implements FilteringIterable.IFilter<ITEM> {

		private Collection<String> allowedLabelValues;
		private String labelName;

		public LabelValueFilter(String labelName) {
			this(labelName, null);
		}

		public LabelValueFilter(String labelName, Collection<String> allowedLabelValues) {
			this.labelName = labelName;
			this.allowedLabelValues = allowedLabelValues;
		}

		@Override
		public boolean include(ITEM item) {
			String labelValue = item.getLabels().getProperty(labelName);
			return labelValue != null && (allowedLabelValues == null || allowedLabelValues.contains(labelValue));
		}
		
	}

	/**
	 * Allows windows with the given label and having the given values for that label.
	 * @param iterable
	 * @param labelName
	 * @param allowedLabelValues
	 */
	public LabeledWindowIterable(Iterable<ITEM> iterable, String labelName, Collection<String> allowedLabelValues) {
		super(iterable, new LabeledWindowIterable.LabelValueFilter<ITEM>(labelName, allowedLabelValues));
	}

	/**
	 * Convenience on {@link #LabeledWindowIterable(Iterable, String, Collection)} allowing a single label value.
	 */
	public LabeledWindowIterable(Iterable<ITEM> iterable, String labelName, String allowedLabel) {
		this(iterable, labelName, Arrays.asList(new String[] { allowedLabel}));
	}

	/**
	 * Allow windows with the given label and any label value.
	 * @param iterable
	 * @param labelName
	 */
	public LabeledWindowIterable(Iterable<ITEM> iterable, String labelName) {
		this(iterable, labelName, (Collection)null);
	}

	public LabeledWindowIterable(Iterable<ITEM> iterable, LabeledWindowIterable lwi) { 
		super(iterable, lwi.matcher); 
	}	
}