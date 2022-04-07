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
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public class FilteringIterator<ITEM> implements Iterator<ITEM> {

	private final Iterator<ITEM> iterator;
	private final FilteringIterable.IFilter<ITEM> matcher;
	private final Predicate<ITEM> predicate;
	private ITEM nextItem  = null;
	
	/**
	 * Iterator through the given items, but only produce those allowed by the given filter.
	 * @param iterator
	 * @param matcher if null, then allow all items in the given iterator.
	 */
	public FilteringIterator(Iterator<ITEM> iterator, FilteringIterable.IFilter<ITEM> matcher) {
		this.iterator = iterator;
		this.matcher = matcher;
		this.predicate = null; 
	}

	public FilteringIterator(Iterator<ITEM> iterator, Predicate<ITEM> predicate) {
		this.iterator = iterator;
		this.matcher = null;
		this.predicate = predicate;;
	}

	@Override
	public boolean hasNext() {
		while (nextItem == null) {
			if (!iterator.hasNext())
				return false;
			nextItem = iterator.next();
			if (   (matcher   != null && !matcher.include(nextItem))
				|| (predicate != null && !predicate.test(nextItem))) 
				nextItem = null;
		}
		return true;
	}

	@Override
	public ITEM next() {
		if (nextItem == null && !hasNext()) 
			throw new NoSuchElementException("Item exhausted from iterator");
		ITEM t = nextItem;
		nextItem = null;
		return t;
	}
	
}
