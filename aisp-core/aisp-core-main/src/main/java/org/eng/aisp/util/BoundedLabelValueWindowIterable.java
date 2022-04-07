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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.classifier.TrainingSetInfo;
import org.eng.aisp.classifier.TrainingSetInfo.LabelInfo;
import org.eng.aisp.classifier.TrainingSetInfo.LabelValueInfo;
import org.eng.util.FilteringIterable;

/**
 * An iterable that allows only a maximum number of labeled windows for a given set of label values of a given label name.
 * 
 * @author DavidWood
 * @param <ITEM>
 */
public class BoundedLabelValueWindowIterable<ITEM extends ILabeledDataWindow<?>> extends FilteringIterable<ITEM>  implements Iterable<ITEM> {

	protected final Iterable<ITEM> iterable;
	protected final String labelName;
	protected final Iterable<String> allowedLabelValues;
	protected final int maxPerLabelValue;

	public static class MaxLabelValueFilter<ITEM extends ILabeledDataWindow<?>> implements FilteringIterable.IFilter<ITEM>, Cloneable {
		private String labelName;
		private int maxPerLabelValue;

		private Map<String, MutableInt> valueCounts;
		private Map<String, MutableInt> remainingValueCounts;
		private Iterable<String> allowedLabelValues;

		public MaxLabelValueFilter(String labelName, int maxPerLabelValue, Iterable<String> allowedLabelValues) {
			this.labelName = labelName;
			if (maxPerLabelValue < 0)
				throw new IllegalArgumentException("maxPerLabelValue must be 0 or larger");
			this.maxPerLabelValue = maxPerLabelValue;
			this.allowedLabelValues = allowedLabelValues;
			if (allowedLabelValues == null) {
				// Allow all labels to the maximum count, so set up to count the accurences.
				valueCounts = new HashMap<String,MutableInt>();
			} else {
				// Allow only the specified label values to the given count.
				remainingValueCounts = new HashMap<String,MutableInt>();
				for (String labelValue : allowedLabelValues) 
					remainingValueCounts.put(labelValue, new MutableInt(maxPerLabelValue));
			}
			
		}
		
		@Override
		public boolean include(ITEM item) {
			if (remainingValueCounts != null)
				return includeSomeLabels(item);
			else
				return includeAllLabels(item);
		}
		
		private boolean includeSomeLabels(ITEM item) {
				String labelValue = item.getLabels().getProperty(labelName); 
				MutableInt mi = labelValue == null ? null : remainingValueCounts.get(labelValue);
				if (mi == null)
					return false;
				mi.decrement();
				int remainingCount = mi.getValue(); 
				if (remainingCount == 0)  
					remainingValueCounts.remove(labelValue);
				return true;
		}
		
		private boolean includeAllLabels(ITEM item) {
			String labelValue = item.getLabels().getProperty(labelName); 
			if (labelValue == null) 
				return false;
			
			MutableInt mi = valueCounts.get(labelValue);
			if (mi == null) {	// Our first value for this label. 
				mi = new MutableInt(0);
				valueCounts.put(labelValue, mi);
			} else if (mi.getValue() >= maxPerLabelValue) {
				return false;
			}
			mi.increment();
			return true;
		}

		@Override
		protected MaxLabelValueFilter<ITEM> clone() throws CloneNotSupportedException {
			return new MaxLabelValueFilter<ITEM>(labelName, maxPerLabelValue, allowedLabelValues);
		}
	
	}

	/**
	 * A convenience on {@link #BoundedLabelValueWindowIterable(Iterable, String, int, Iterable)} that allows all label values through
	 * with the given maximum by using allowedLabelValues=null.
	 */
	public BoundedLabelValueWindowIterable(Iterable<ITEM> iterable, String labelName, int maxPerLabelValue) {
		this(iterable,labelName, 0, maxPerLabelValue, null);
	}

	public BoundedLabelValueWindowIterable(Iterable<ITEM> iterable, String labelName, int minPerLabelValue, int maxPerLabelValue) {
		this(iterable,labelName, minPerLabelValue, maxPerLabelValue, null);
	}

	/**
	 * Create an iterable that restricts the set of items produced according to the parameters. 
	 * @param iterable the labeled items to filter accordingly.
	 * @param labelName name of label in which label values are to be found within the labeled items.
	 * @param minPerLabelValue	minimum number of label values for any or the specifies label values.  Ignored if less or equal to 0. <b>Important</b>: If non-zero,
	 * the iterable will be traversed once to find the set of labels that meet the minimum requirements.
	 * @param maxPerLabelValue the maximum number of items for a given label name.  Must be 0 or larger.
	 * @param allowedLabelValues if non-null, then further restricts the label values that are allowed to be produced by the iterable.
	 */
	public BoundedLabelValueWindowIterable(Iterable<ITEM> iterable, String labelName, int minPerLabelValue, int maxPerLabelValue, Iterable<String> allowedLabelValues) {
		this(iterable, labelName, maxPerLabelValue, minPerLabelValue <= 0 ? allowedLabelValues : getMinimumOccurringLabels(iterable, labelName, minPerLabelValue, allowedLabelValues));
	}

	private static <ITEM extends ILabeledDataWindow<?>> Iterable<String> getMinimumOccurringLabels(Iterable<ITEM> iterable, String labelName, int minPerLabelValue, Iterable<String> allowedLabelValues) {
		TrainingSetInfo tsi = TrainingSetInfo.getInfo(iterable);
		LabelInfo li = tsi.getLabelInfo(labelName);
		List<String> labelValues = new ArrayList<String>();
		if (li == null)	// No labels present on the data.
			return labelValues;
		if (allowedLabelValues != null) {
			for (String labelValue : allowedLabelValues) {
				labelValues.add(labelValue);
			}
		}
		for (String labelValue : li.getLabelValues()) {
			LabelValueInfo lvi = li.getLabelInfo(labelValue);
			if (lvi.getTotalSamples() >= minPerLabelValue) {
				if (allowedLabelValues == null)	// No restriction on the label values (and it was not added above).
					labelValues.add(labelValue);
				// else already added above.
			} else if (allowedLabelValues != null) {	
				// There are specific list of allowed values, make sure this one is not included now since it does not meet the min requirements
				labelValues.remove(labelValue);
			}

		}
		return labelValues;
	}

	/**
	 * 
	 * @param iterable source of labeled windows 
	 * @param labelName name of label used to extract label values
	 * @param maxPerLabelValue the maximum number of labels for each label value of the given label name.
	 * @param allowedLabelValues if null, then all label values are allowed through to the given maximum.  Otherwise, contains set of 
	 * label values that are allowed to match the windows in the iterable, all others are excluded.
	 */
	public BoundedLabelValueWindowIterable(Iterable<ITEM> iterable, String labelName, int maxPerLabelValue, Iterable<String> allowedLabelValues) {
		super(iterable, new MaxLabelValueFilter<ITEM>(labelName, maxPerLabelValue, allowedLabelValues));
		this.iterable = iterable;
		this.labelName = labelName;
		this.allowedLabelValues = allowedLabelValues;
		if (maxPerLabelValue < 0)
			throw new IllegalArgumentException("maxPerLabelValue must be 0 or larger");
		this.maxPerLabelValue = maxPerLabelValue;
	}
	
	@Override
	protected IFilter<ITEM> newStatefulFilter(IFilter<ITEM> filter) {
		MaxLabelValueFilter<ITEM> mlvf = (MaxLabelValueFilter<ITEM>)filter;
		try {
			return (IFilter<ITEM>) mlvf.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();	// Should never get here!
			return null;
		} 
	}


}
