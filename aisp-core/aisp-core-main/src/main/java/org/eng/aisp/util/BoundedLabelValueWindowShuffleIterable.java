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

import java.util.Iterator;

import org.eng.aisp.ILabeledDataWindow;
import org.eng.util.AbstractFilteringShuffleIterable;
import org.eng.util.IShuffleIterable;

/**
 * Extension of the super class to provide shuffleability.
 * 
 * @author DavidWood
 *
 * @param <ITEM>
 */
public class BoundedLabelValueWindowShuffleIterable<ITEM extends ILabeledDataWindow<?>> extends AbstractFilteringShuffleIterable<ITEM> implements IShuffleIterable<ITEM> {

	private BoundedLabelValueWindowIterable<ITEM> maxLabelIterable;

	/**
	 * A convenience on {@link #BoundedLabelValueWindowShuffleIterable(IShuffleIterable, boolean, String, int, int, Iterable)} with
	 * enableShuffleRepeatability=true, minPerLabelValue=0 and allowedLabelValues=null.
	 */
	public BoundedLabelValueWindowShuffleIterable(IShuffleIterable<ITEM> iterable, String labelName, int maxPerLabelValue) {
		this(iterable, true, labelName, 0, maxPerLabelValue, null);
	}

//	/**
//	 * A convenience on {@link #BoundedLabelValueWindowShuffleIterable(IShuffleIterable, boolean, String, int, int, Iterable)} with
//	 * minPerLabelValue=0 and  allowedLabelValues=null.
//	 */
//	private BoundedLabelValueWindowShuffleIterable(IShuffleIterable<ITEM> iterable, boolean enableShuffleRepeatability, String labelName, int maxPerLabelValue) {
//		this(iterable, enableShuffleRepeatability, labelName, 0, maxPerLabelValue, null);
//	}

	
	/**
	 * A convenience on {@link #BoundedLabelValueWindowShuffleIterable(IShuffleIterable, boolean, String, int, int, Iterable)} with
	 * minPerLabelValue=0.
	 */
	public BoundedLabelValueWindowShuffleIterable(IShuffleIterable<ITEM> iterable, String labelName, int maxPerLabelValue, Iterable<String> allowedLabelValues) {
		this(iterable, true, labelName, 0, maxPerLabelValue, allowedLabelValues);
	}

	
	/**
	 * Create an iterable that restricts the set of items produced according to the parameters. 
	 * @param iterable the labeled items to filter accordingly.
	 * @param enableShuffleRepeatability
	 * @param labelName name of label in which label values are to be found within the labeled items.
	 * @param minPerLabelValue	minimum number of label values for any or the specifies label values.  Ignored if less or equal to 0. <b>Important</b>: If non-zero,
	 * the iterable will be traversed once to find the set of labels that meet the minimum requirements.
	 * @param maxPerLabelValue the maximum number of items for a given label name.  Must be 0 or larger.
	 * @param allowedLabelValues if non-null, then further restricts the label values that are allowed to be produced by the iterable.
	 */
	private BoundedLabelValueWindowShuffleIterable(IShuffleIterable<ITEM> iterable, boolean enableShuffleRepeatability, String labelName, int minPerLabelValue, int maxPerLabelValue, Iterable<String> allowedLabelValues) {
		super(iterable, null, enableShuffleRepeatability);
		maxLabelIterable = new BoundedLabelValueWindowIterable<ITEM>(iterable, labelName, minPerLabelValue, maxPerLabelValue, allowedLabelValues);
	}

    private BoundedLabelValueWindowShuffleIterable(IShuffleIterable<ITEM> iterable, BoundedLabelValueWindowIterable<ITEM> maxLabelIterable, AbstractFilteringShuffleIterable<ITEM> requester) {
	   super(iterable, null, requester);
	   this.maxLabelIterable = new BoundedLabelValueWindowIterable<ITEM>(iterable, 
			   maxLabelIterable.labelName, maxLabelIterable.maxPerLabelValue, maxLabelIterable.allowedLabelValues);
	}

	/**
	 * A convenience on {@link #BoundedLabelValueWindowShuffleIterable(IShuffleIterable, boolean, String, int, int, Iterable)} with
	 * enableShuffleRepeatability=true.
	 */
	public BoundedLabelValueWindowShuffleIterable(IShuffleIterable<ITEM> iterable, String labelName, int minPerLabelValue, int maxPerLabelValue, Iterable<String> allowedLabelValues) {
		this(iterable, true, labelName, minPerLabelValue, maxPerLabelValue, allowedLabelValues);
	}


	@Override
	protected Iterator<ITEM> createFilteringIterator() {
		return this.maxLabelIterable.iterator();
	}

	@Override
	protected IShuffleIterable<ITEM> newIterable(Iterable<String> references, AbstractFilteringShuffleIterable<ITEM> requestor) {
		IShuffleIterable<ITEM> si = ((IShuffleIterable<ITEM>)this.iterable).newIterable(references);
		return  new BoundedLabelValueWindowShuffleIterable<ITEM>(si, maxLabelIterable, requestor); 
	}

}
