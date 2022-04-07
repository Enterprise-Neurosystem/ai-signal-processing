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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

public abstract class AbstractReferenceShuffleIterable<ITEM,ITERABLE extends IShuffleIterable<ITEM>> implements IShuffleIterable<ITEM>, ISizedIterable<ITEM> {
	
	protected Iterable<String> references;

	/**
	 * Don't set the references initially, and expect the sub-class to override {@link #getReferences()} to provide them after construction.
	 */
	protected AbstractReferenceShuffleIterable() {
		this(null);
	}

	protected AbstractReferenceShuffleIterable(Iterable<String> references) {
		this.references = references;
	}

 

	/**
	 * Use {@link #newIterable(Iterable)} to create the instance.
	 */
	@Override
	public ITERABLE shuffle(long seed) {
		List<String> refList = new ArrayList<String>();
		for (String id : this.getReferences())
			refList.add(id);
		Collections.shuffle(refList, new Random(seed));
		return newIterable(refList);
	}
	
	/**
	 * Override only to set the return type.
	 */
	@Override
	public abstract ITERABLE newIterable(Iterable<String> newReferences);

	private final Random random = new Random(12310331);	// repeatable shufflability for this instance
	
	@Override
	public ITERABLE shuffle() {
		return this.shuffle(random.nextLong());
	}

	/**
	 * @return the references
	 */
	@Override
	public Iterable<String> getReferences() {
		return references;
	}

	int size = -1;
	@Override
	public int size(){
		Iterable<String> refs = this.getReferences();
		if (refs instanceof Collection)
			return ((Collection)refs).size();
		if (size >= 0)
			return size;
		
		size = 0;
		for (String tmp : refs) 
			size++;
		return size;
	}


	/**
	 * A non-optimal implementation that used {@link #dereference(String)} for each reference in the list.
	 */
	@Override
	public List<ITEM> dereference(List<String> references) {
		List<ITEM>  items = new ArrayList<ITEM>(); 
		for (String ref : references)  {
			ITEM item = dereference(ref);
			if (item == null)	/** deference() should have already done this, but just in case...  */
				throw new NoSuchElementException("Could not load item for reference: " + ref);
			items.add(item);
		}
		return items;
	}

	/**
	 * Uses an ItemReferenceIterator that then uses this instance to dereference the references.
	 */
	@Override
	public Iterator<ITEM> iterator() {
		return new ItemReferenceIterator<ITEM>(this, getReferences().iterator());
	}

}
