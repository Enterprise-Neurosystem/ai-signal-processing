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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A class that converts an Iterable to IShuffleIterable. 
 * @author dawood
 *
 */
public class ShufflizingIterable<ITEM> extends AbstractReferenceShuffleIterable<ITEM, IShuffleIterable<ITEM>> implements IShuffleIterable<ITEM> {


	protected final Map<String, ITEM> referenceMap; 

	private static <ITEM> List<String> itemIndexes(Iterable<ITEM> items) {
		List<String> indexes = new ArrayList<String>();
		int count = 0;
		Iterator<ITEM> iter = items.iterator();
		while (iter.hasNext()) {
			indexes.add(String.valueOf(count));
			iter.next();
			count++;
		}
		return indexes;
	}

	public ShufflizingIterable(Iterable<ITEM> items) {
		super(itemIndexes(items));
		referenceMap = new HashMap<String, ITEM>();
		Iterator<String> refIter = this.references.iterator();
		for (ITEM item : items)  {
			if (!refIter.hasNext())
				throw new RuntimeException("references were not created correctly from the iterable of items given.");
			String ref = refIter.next();
			this.referenceMap.put(ref, item);
		}
	}

	private ShufflizingIterable(Iterable<String> refs, Map<String, ITEM> refMap) {
		super(refs);
		referenceMap = refMap;	// We're being called by newIterable() so just reference the callers map which is readonly after creation.
	}

	@Override
	public IShuffleIterable<ITEM> newIterable(Iterable<String> references) {
		return new ShufflizingIterable<ITEM>(references, referenceMap);
	}

	@Override
	public ItemReferenceIterator<ITEM> iterator() {
		return new ItemReferenceIterator<ITEM>(this, getReferences().iterator());
	}

	@Override
	public ITEM dereference(String reference) {
		return referenceMap.get(reference);
	}


}
