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
import java.util.List;
import java.util.NoSuchElementException;

public class MutatingIterator<INPUT,OUTPUT> extends AbstractDefaultIterator<OUTPUT> implements Iterator<OUTPUT>{

	private final Iterator<INPUT> items;
	private OUTPUT nextItem = null;
	private List<OUTPUT> nextItems = null;
	private int nextItemIndex = 0;
	private final IMutator<INPUT,OUTPUT> mutator;
	
	public MutatingIterator(Iterator<INPUT> items, IMutator<INPUT,OUTPUT> mutator) {
		this.items = items;
		this.mutator = mutator;
	}


	@Override
	public boolean hasNext() {
		if (nextItem != null)
			return true;
		// Loop until we have an array of nextItems or we've exhausted the source of items.
		while (nextItems == null || nextItemIndex >= nextItems.size()) {
			nextItems = null;					// Free up some memory.
			if (!items.hasNext())
				return false;
			INPUT item = items.next();
			nextItems = mutator.mutate(item);	// Produces null, or 1 or more items
			nextItemIndex = 0;					// Always go to the first item in the array.
		}
		nextItem = nextItems.get(nextItemIndex++);
		if (nextItem == null)	// null entry in returned array.  should not happen, but we can ignore it.
			return hasNext();
		else
			return true;
	}

	@Override
	public OUTPUT next() {
		if (nextItem == null && !hasNext())
			throw new NoSuchElementException("out of items");
		OUTPUT t = nextItem;
		nextItem = null;
		return t;
	}
	
}