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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Wrap an IItemReferenceIterable to use a different (subset?) of references. 
 * @author DavidWood
 *
 * @param <T>
 */
public class ItemReferenceIterableProxy<T> implements IItemReferenceIterable<T> {

	protected final IItemReferenceIterable<T> iterable;
	protected final Iterable<String> references;
	
	/**
	 * @param iterable
	 */
	public ItemReferenceIterableProxy(IItemReferenceIterable<T> iterable, Iterable<String> references) {
		this.iterable = iterable;
		this.references = references;
	}

	@Override
	public Iterator<T> iterator() {
		return new ItemReferenceIterator<T>(iterable, references.iterator());
	}

	@Override
	public Iterable<String> getReferences() {
		return references;
	}

	@Override
	public T dereference(String reference) {
		return this.iterable.dereference(reference);
	}

	@Override
	public List<T> dereference(List<String> references) {
		return this.iterable.dereference(references);
	}
	
	int size = -1;
	public int size(){
		if (references instanceof Collection)
			return ((Collection)references).size();
		if (size >= 0)
			return size;
		
		size = 0;
		for (String tmp : references) 
			size++;
		return size;
	}

}
