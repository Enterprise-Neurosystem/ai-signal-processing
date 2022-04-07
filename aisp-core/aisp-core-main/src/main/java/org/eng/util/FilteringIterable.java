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
package org.eng.util;

import java.util.Iterator;
import java.util.function.Predicate;

/**
 * Enables an arbitrary Iterable to be iterated through with only items matching a given Filter criteria being returned.
 * @author dawood
 *
 * @param <ITEM>
 */
public class FilteringIterable<ITEM> implements Iterable<ITEM> {
	

	/**
	 * Implementations define what members of an Iterable will be returned.
	 * @author dawood
	 *
	 * @param <ITEM>
	 */
	public interface IFilter<ITEM> {
		/**
		 * Determine if the item should be returned by the iterable. 
		 * @param item the item from the referenced iterable that is under consideration for returning by the FilteringIterable.
		 * @return true if the item should be returned by the FilteringIterable, otherwise false.
		 */
		public boolean include(ITEM item);
	}

	protected final Iterable<ITEM> iterable;
	protected final FilteringIterable.IFilter<ITEM> matcher;
	protected final Predicate<ITEM> predicate;

	/**
	 * Create the iterable to apply the given filter to the iterable to control which items are actually produced by this instance.
	 * @param iterable the iterable which filtered by the given filter.
	 * @param matcher if non-null, then determine which items of the given iterable are produced by this instance's iterator. 
	 * If this is a stateful filter, {@link #newStatefulFilter(IFilter)} should be overridden to produce a new filter for
	 * each iterator.
	 */
	public FilteringIterable(Iterable<ITEM> iterable, FilteringIterable.IFilter<ITEM> matcher) {
		this.iterable = iterable;
		this.matcher = matcher;
		this.predicate = null;
	}
	public FilteringIterable(Iterable<ITEM> iterable, Predicate<ITEM> predicate) {
		this.iterable = iterable;
		this.matcher = null ;
		this.predicate = predicate;

	}

	@Override
	public Iterator<ITEM> iterator() {
		if (matcher != null) {
			IFilter<ITEM> f = newStatefulFilter(matcher);
			if (f == null)
				f = this.matcher;
			return new FilteringIterator<ITEM>(iterable.iterator(), f);
		} else {
			return new FilteringIterator<ITEM>(iterable.iterator(), predicate);
		}
	}

	/**
	 * Subclasses MUST override if their matcher is stateful wrt to an iteration.
	 * The default implementation returns null indicating that this instance's IFilter
	 * is NOT stateful and can be re-used across multiple iterators (as returned by {@link #iterator()}.
	 * @param matcher2 
	 * @return non-null to define a new IFilter instance for each iterator() call. 
	 */
	protected IFilter<ITEM> newStatefulFilter(IFilter<ITEM> matcher2) { return null; }


	
}
