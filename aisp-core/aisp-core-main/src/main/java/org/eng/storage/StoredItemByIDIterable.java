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
package org.eng.storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.eng.cache.Cache;
import org.eng.cache.IMultiKeyCache;
import org.eng.util.AbstractReferenceShuffleIterable;
import org.eng.util.IShuffleIterable;

/**
 * An iterable over items referenced in an IItemStorage instance.
 * Extends the super class to define the item references as ids of items in a specified IItemStorage instance.
 * 
 * @author dawood
 *
 * @param <ITEM>
 */
public class StoredItemByIDIterable<ITEM> extends AbstractReferenceShuffleIterable<ITEM, StoredItemByIDIterable<ITEM>> implements IShuffleIterable<ITEM> {

	private IReadOnlyStorage<ITEM> storage;
	private IMultiKeyCache cache; //  = Cache.newMemoryCache();
	private int batchSize;

	/**
	 * Create the instance to reference the items by ID in the given storage instance.
	 * @param storage contains items referenced by given ids Iterable.
	 * @param ids  an Iterable of ids of items to retrieve from the given storage.
	 */
	public StoredItemByIDIterable(IReadOnlyStorage<ITEM> storage, Iterable<String> ids) {
		this(storage,ids,Cache.newMemoryCache(),1);
	}

	public StoredItemByIDIterable(IReadOnlyStorage<ITEM> storage, Iterable<String> ids, IMultiKeyCache cache, int batchSize) {
		super(ids);
		this.storage = storage;
		if (cache == null)
			cache = Cache.newMemoryCache(); 
		this.cache = cache;
		if (batchSize < 1) 
			throw new IllegalArgumentException("batchSize must be 1 or larger.");
		this.batchSize = batchSize;
	}

	/**
	 * Create the instance to allow batch sizes of other than 1 when reading from storage.
	 * @param storage storage instance providing mapping of refernces/ids to items.
	 * @param ids references to items to be returned by this iterable through reading from the given storage instance.
	 * @param batchSize  1 or larger.
	 */
	public StoredItemByIDIterable(IReadOnlyStorage<ITEM> storage, Iterable<String> ids, int batchSize) {
		this(storage, ids, Cache.newMemoryCache(), batchSize);
	}

	@Override
	public StoredItemByIDIterable<ITEM> newIterable(Iterable<String> references) {
		return new StoredItemByIDIterable<ITEM>(storage, references, cache,batchSize);
	}

	@Override
	public Iterator<ITEM> iterator() {
		// We do all the cache management here so no need to pass the cache to the iterator.
		return new StoredItemByIDIterator<ITEM>(this, getReferences().iterator(), batchSize);
	}
	
	@Override	// Just to override return type.
	public StoredItemByIDIterable<ITEM> shuffle(long seed) {
		return (StoredItemByIDIterable<ITEM>)super.shuffle(seed);
	}
	
	/**
	 * Get the item from storage where the given reference is the id of an item in storage.
	 */
	@Override
	public ITEM dereference(String reference) {
		ITEM r = cache == null ? null : (ITEM)cache.get(reference);
		if (r != null)
			return r;
		try {
			r = storage.findItem(reference);
			if (cache != null)
				cache.put(r,reference);
		} catch (StorageException e) {
			;
		}
		if (r == null)
			throw new NoSuchElementException("Could not find item with id " + reference + " in storage");
			
		return r;
	}

	/** This could always be set to true now that StorageUtil.batchLoad() enforces ParallelLoadEnabled, but this is a bit more efficient */
	private static boolean useBatch = StorageUtil.ParallelLoadEnabled;

	/**
	 * Get the items from storage in bulk using  {@link IReadOnlyStorage#findItems(List)}.
	 */
	@Override
	public List<ITEM> dereference(List<String> references) {
		List<ITEM> items = new ArrayList<ITEM>();
		if (!useBatch) {
			for (String ref : references) {
				ITEM item = dereference(ref);
				items.add(item);
			}
			if (items.size() != references.size())
				throw new NoSuchElementException("Could not load all references: " + references);
		} else {
			try {
				int index = 0;
				for (ITEM item : storage.findItems(references)) {
					items.add(item);
					if (cache != null)
						cache.put(item,references.get(index));
					index++;
				}
			} catch (StorageException e) {
				throw new NoSuchElementException("Could not find one or more of the items item with id " + references + " in storage: " + e.getMessage());
			}
		}
		return items; 
	}

}
