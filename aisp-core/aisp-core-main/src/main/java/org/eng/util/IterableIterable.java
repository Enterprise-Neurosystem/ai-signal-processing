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

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterable that create an iterator that spans 1 or more Iterables.
 * @author dawood
 *
 * @param <T>
 */
public class IterableIterable<T> implements Iterable<T> {

	protected final Iterable<? extends Iterable<T>> iterables;
	
	/**
	 * The Iterator created by the IterableIterable class.
	 * @author dawood
	 *
	 * @param <T>
	 */
	private static class IteratableIterator<T> implements Iterator<T> {

		protected Iterator<T> currentIterator = null;
		protected final Iterator<? extends Iterable<T>> iterables;

		public IteratableIterator(Iterator<? extends Iterable<T>> iterables) {
			this.iterables = iterables;
			if (iterables.hasNext())
				currentIterator = iterables.next().iterator();
			else
				currentIterator = null; 
		}

		@Override
		public boolean hasNext() {
			if(currentIterator == null)
				return false;
			if (!currentIterator.hasNext()) {	// Reached the end of the current iterator.
				if (iterables.hasNext())
					currentIterator = iterables.next().iterator();
				else
					currentIterator = null;
			}
			return currentIterator != null && currentIterator.hasNext();
		}

		@Override
		public T next() {
			if (!hasNext())
				throw new NoSuchElementException("");
			return currentIterator.next();
		}

		@Override
		public void remove() {
			throw new RuntimeException("Not supported");
			
		}
		
	}

	/**
	 * Create the instance to create an Iterator that iterates of each of the items in the iterables as if they were in a single Iterable.
	 * @param iterables
	 */
	public IterableIterable(Iterable<? extends Iterable<T>> iterables) {
		this.iterables = iterables;
	}

	public IterableIterable(Iterable<T>...iterables) {
		this(Arrays.asList(iterables));
	}

	@Override
	public Iterator<T> iterator() {
		return new IteratableIterator<T>(iterables.iterator());
	}


}
