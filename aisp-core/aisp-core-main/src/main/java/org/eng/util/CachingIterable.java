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

import org.eng.cache.Cache;
import org.eng.cache.IMultiKeyCache;

/**
 * Provides caching over items from another Iterable from which it is presumed to be time consuming to call {@link Iterator#next()}. 
 * Any cache may be used, but the default is {@link Cache#newMemoryCache()}.
 * <p>
 * This class is only <b>partially</b> thread-safe.  The first iteration through the items is used to load the cache and as such 
 * needs to complete before all subsequent iterations.  Once the first iteration has completed, subsequent iterations may
 * proceed in parallel.
 * <p>
 * Implementation Note: a cache (something that evicts items) is not strictly required and probably
 * not recommended, but rather a storage mechanism that does not evict things is preferred.
 * That said, we rely on the cache to clear itself when this instance is done being used. 
 * <p>
 * Does not support null values in the iterable. 
 * @author dawood
 *
 * @param <ITEM> items to be iterated and cached as needed.  Must be storable in the cache being used.
 */
public class CachingIterable<ITEM> extends InstanceIdentifiedObject implements Iterable<ITEM> {

	/** The base iterator from which items are first retrieved */
	private Iterator<ITEM> firstIterator;
	/** Hold items from the firstIterator each time one is retrieved */
	private final IMultiKeyCache<Long, ITEM> cache; 
	
	/** Current number of items that we've retrieved and cached from the first iterator */
	private long cachedItemCount = 0; 
	private Iterable<ITEM> iterable;
	private Object syncLock = new Object();
	
	/**
	 * Create the iterator and use the default memory cache.
	 * @param iterable
	 */
	public CachingIterable(Iterable<ITEM> iterable) {
		this(iterable,null);
	}

	/**
	 * Create the instance over the given iterable and use the given cache to cache items. 
	 * @param iterable
	 * @param cache if null, then use {@link Cache#newMemoryCache()}.  
	 */
	public CachingIterable(Iterable<ITEM> iterable, IMultiKeyCache<Long, ITEM> cache) {
		this.iterable = iterable;
		if (cache == null)
			cache = Cache.newMemoryCache();
		this.cache = cache;
		this.firstIterator = iterable.iterator();
	}

	@Override
	public Iterator<ITEM> iterator() {
		return new CachingIterator();
	}

	/**
	 * This needs to be an inner class so it holds a reference to the instance
	 * that created it.  This keeps the creating instance from being finalized, which
	 * in turn keeps the underlying cache from being cleared.
	 * @author dawood
	 *
	 */
	private class CachingIterator implements Iterator<ITEM>  {
		
		/** zero-based index of an item previously stored */
		long nextIndex = 0; 
		ThreadLocal<ITEM> next = new ThreadLocal<ITEM>();
		
		CachingIterator() {
			next.set(null);
		}
		
		@Override
		public boolean hasNext() {
			if (next.get() != null)
				return true;
			ITEM nextItem; 
			synchronized (syncLock) {
				if (nextIndex >= cachedItemCount) {	// Pull a new item from the firstIterator
					if (firstIterator == null)	// This instance has hit the end.
						return false;
					boolean r = firstIterator.hasNext();
					if (!r) {
						firstIterator = null;	// release any memory associated with this
						return false;
					}
					nextItem = firstIterator.next();
					cache.put(nextItem, getInstanceID(), cachedItemCount);	// Items stored using a 0-based index.
					cachedItemCount++;	// Eventually this is the total number of items in the iterator
				} else {	// Its already been retrieved once by some other instance, try and find it in the cache.
					nextItem = cache.get(getInstanceID(), nextIndex);
					if (nextItem == null) { // item was evicted so put back in the cache 
						long index=0 ;
	//					ENGLogger.logger.info("Cache eviction.  Reiterating to load next cache items");
						for (ITEM t : iterable) {	// Sadly, we have to go through the iterable again.
							if (index > nextIndex) {
								// Here we put potential future items in the cache to avoid N^2 performance on multiple cache misses.
								cache.put(t, getInstanceID(), index);
							} else if (index == nextIndex) {
								nextItem = t;
							}
							index++;
						}
					}
				}
				nextIndex++;
			} // synchronized
			if (nextItem == null)
				throw new RuntimeException("Unexpected null for nextItem");
			next.set(nextItem);
			return true;
		}

		@Override
		public ITEM next() {
			if (!hasNext())
				throw new NoSuchElementException();
			ITEM item = next.get();
			next.set(null);
			return item;
		}

	}

	public void clearCachedItems() {
		for (long i=0 ; i<cachedItemCount ;i++)
			cache.remove(getInstanceID(), i);
	}

	@Override
	protected void finalize() throws Throwable {
		clearCachedItems();
		super.finalize();
	}
}
