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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class ItemReferenceIterator<ITEM> extends AbstractDefaultIterator<ITEM> implements Iterator<ITEM> {

	protected final Iterator<String> references;
	protected ITEM nextItem = null;
	protected final IItemReferenceIterable<ITEM> refIterable;
	private final int batchSize;
	private List<ReferenceItemPair<ITEM>> batchReferencedItems = new LinkedList<ReferenceItemPair<ITEM>>();
	private boolean skipNulLDereferences;
	
	/**
	 * Convenience on {@link #ItemReferenceIterator(IItemReferenceIterable, Iterator, int)} with batchSize=1.
	 */
	public ItemReferenceIterator(IItemReferenceIterable<ITEM> refIterable, Iterator<String> references) {
		this(refIterable, references, 1);
	}

	public ItemReferenceIterator(IItemReferenceIterable<ITEM> refIterable, Iterator<String> references, int batchSize) {
		this(refIterable, references,batchSize, false);
	}
	
	public ItemReferenceIterator(IItemReferenceIterable<ITEM> refIterable, Iterator<String> references, int batchSize, boolean skipNullDereferences) {
		super();
		this.refIterable = refIterable;
		this.references = references;
		this.batchSize = batchSize;
		this.skipNulLDereferences = skipNullDereferences;
	}

	private static class ReferenceItemPair<ITEM> {
		public final String reference;
		public final ITEM item;
		public ReferenceItemPair(String reference, ITEM item) {
			this.reference = reference;
			this.item = item;
		}
		
	}

	/**
	 * Get the item at the given reference
	 * @param reference
	 * @return never null
	 * @throws NoSuchElementException if refIterable could not dereference all items. 
	 */
	protected List<ReferenceItemPair<ITEM>> dereference(List<String> references) {
		List<ITEM> items = refIterable.dereference(references);
		if (items == null)
			throw new NoSuchElementException("Could not derefrence one or more references:" +references);
		if (items.size() != references.size())
			throw new NoSuchElementException("expected " + references.size() + " items, but dereferencing produced " + items.size() + ", references=" + references);
		List<ReferenceItemPair<ITEM>> ripList = new ArrayList<ReferenceItemPair<ITEM>>();
		Iterator<String> refIter = references.iterator();
		for (ITEM item : items) {
			String ref = refIter.next();
			ReferenceItemPair<ITEM> rip = new ReferenceItemPair<ITEM>(ref,item);
			ripList.add(rip);
		}
		return ripList;
	}

	protected ITEM dereference(String reference) {
		return refIterable.dereference(reference);
	}
	
	
	@Override
	public boolean hasNext() {
		String nextRef = null; 
		while (nextItem == null) {

			// First try and pull the next item from the previously dereferenced items, if any.
			if (batchReferencedItems.size() != 0) {
				ReferenceItemPair<ITEM> rip = batchReferencedItems.remove(0);
				if (rip != null) {	// Just to be sure
					nextRef = rip.reference;
					nextItem = rip.item;
				}
			} else { 	// Must find the next item using the next reference.
				if (batchSize == 1) {
					if (!references.hasNext())
						return false;
					nextRef = references.next();
					nextItem = dereference(nextRef);
				} else {	// Load a batch.
					nextBatch();
				}
			}
			if (nextItem == null && !this.skipNulLDereferences)
				return false;
		} 
		return true; 

	}

	/**
	 * Load up the batchReferenceItems and set nextItem with the first item there or null if none in next batch..
	 * @throws NoSuchElementException
	 */
	private void nextBatch() throws NoSuchElementException {
		String nextRef;
		List<String> refList = new ArrayList<String>();
		for (int i=0 ; i<batchSize && references.hasNext(); i++) { 
			String ref = references.next();
			refList.add(ref);
		}
		// Get all the items that haven't yet been dereferenced, if any.
		if (refList.size() > 0)  {
			List<ReferenceItemPair<ITEM>> rip = dereference(refList);
			if (rip == null)
				throw new NoSuchElementException("Could not derefrence one or more references:" +refList);
			this.batchReferencedItems.addAll(rip);
		}
		if (batchReferencedItems.size() > 0) {
			// We have a next item to make available. 
			ReferenceItemPair<ITEM> rip = batchReferencedItems.remove(0);
			nextRef = rip.reference;
			nextItem = rip.item;
		} else {
			nextItem = null;
		}
	}

	@Override
	public ITEM next() {
		if (nextItem == null && !hasNext()) 
			throw new NoSuchElementException("no more items");
		ITEM r = nextItem;
		nextItem = null;
		return r;
	}

	/**
	 * Skip over the next item instead of dereferencing it with {@link #next()}.
	 */
	public void skipNext() {
		if (nextItem == null) {
//			hasNext();	// populate this.nextItem
//			if (nextItem == null)
//				throw new NoSuchElementException("no more items");
			if (references.hasNext())
				references.next();
		}
		nextItem = null;	// Cause the next item to be used in the references.
	}

}
