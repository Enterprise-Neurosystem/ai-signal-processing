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

/**
 * Extends the super class to optimize the ability to sub-select items from an existing IShuffleIterable. 
 * This is optimized by sub-selecting on the references and only dereferencing when an item is returned by next();
 * TODO: this does NOT avoid dereferencing skipped items in the iterable given to the constructor unless the
 * given IShuffleIterable is an instance of IItemReferenceIterable (I guess this is usually the case though). It c/should.
 * @author dawood
 *
 * @param <T>
 */
public class IthShuffleIterable<T> extends AbstractFilteringShuffleIterable<T> implements IShuffleIterable<T>, ISizedIterable<T> {

	protected final IthIterable<T> ithIterable;

	/**
	 *  Maps directly to {@link IthIterable#IthIterable(Iterable, int)}.
	 */
	public IthShuffleIterable(IShuffleIterable<T> iterable, int maxItems) {
		super(iterable, null, true);
		this.ithIterable = new IthIterable<T>(iterable, maxItems);
	}

	/**
	 * Maps directly {@link IthIterable#IthIterable(Iterable, int, int, boolean, int, int)}
	 */
	public IthShuffleIterable(IShuffleIterable<T> iterable, int strideSize, int strideIndex, boolean isExcluded, int maxItems, int firstN) {
		super(iterable, null, true);
		this.ithIterable = new IthIterable<T>(iterable, strideSize, strideIndex, isExcluded, maxItems, firstN);
	}
	
	/**
	 * Maps directly {@link IthIterable#IthIterable(Iterable, int, int, boolean)}
	 */
	public IthShuffleIterable(IShuffleIterable<T> iterable, int strideSize, int strideIndex, boolean isExcluded) {
//		super(iterable, null, true);
//		this.ithIterable = new IthIterable<T>(iterable, strideSize, strideIndex, isExcluded); 
		this(iterable, strideSize, strideIndex, isExcluded, -1, 0); 
	}

	protected IthShuffleIterable(IShuffleIterable<T> iterable, AbstractFilteringShuffleIterable<T> requester, IthShuffleIterable<T> ithShuffleIterable) {
		super(iterable, null, requester);
		this.ithIterable = ithShuffleIterable.ithIterable;
	}

//	@Override
//	public IShuffleIterable<T> shuffle(long seed) {
//		// Iterate through the selected items and then shuffle them.
//		IthIterable<String> ithIter = new IthIterable<String>(((IShuffleIterable<T>)this.iterable).getReferences(), this.ithIterable); 
//		List<String> refs = new ArrayList<String>();
//		for (String ref : ithIter) 
//			refs.add(ref);
//		Collections.shuffle(refs,new Random(seed));
//		return ((IShuffleIterable<T>)this.iterable).newIterable(refs);
//	}

//	final static boolean allowViolation=true;
//	final static boolean allowViolation=false;
	@Override
	public Iterable<String> getReferences() {
//		if (allowViolation) 
			return getFilteredReferences();	// this is equivalent to the above code. comment still stands.
//		else
//			return super.getReferences();
	}

	@Override
	public Iterator<T> iterator() {
//		return new ItemReferenceIterator<T>((IShuffleIterable<T>)iterable, this.getFilteredReferences().iterator());
		return ithIterable.iterator();
	}

	@Override
	protected IShuffleIterable<T> newIterable(Iterable<String> references, AbstractFilteringShuffleIterable<T> requester) {
//
//	}
//
//	public IShuffleIterable<T> newIterable(Iterable<String> references) {
//		if (allowViolation) {	// we returned filtered references.
			return ((IShuffleIterable<T>)iterable).newIterable(references);
//			return new ShufflizingItemReferenceIterableProxy<T>(((IShuffleIterable<T>)iterable.newIterable(references)), references);
//		} else { 		// we returned all references.
//			return new IthShuffleIterable<T>(((IShuffleIterable<T>)iterable).newIterable(references), requester, this); 
//			return new IthShuffleIterable<T>(((IShuffleIterable<T>)iterable).newIterable(references), ithIterable.strideSize, ithIterable.strideIndex, ithIterable.isIndexExcluded, 
//					ithIterable.maxItems, ithIterable.firstN);
//		}
	}

	int count = -1;
	@Override
	public int size() {
		if (count >= 0)
			return count;
		count = 0;
		for (String tmp : getFilteredReferences()) 
			count++;
		return count;
	}

	@Override
	protected synchronized Iterable<String> getFilteredReferences() {
		return getFilteredItems(((IShuffleIterable<T>)this.iterable).getReferences());
	}

	private <X> Iterable<X> getFilteredItems(Iterable<X> items) {
		IthIterable<X> ithIter = new IthIterable<X>(items, this.ithIterable);
		return ithIter;
	}

}