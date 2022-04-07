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
package org.eng.cache;

import java.util.ArrayList;
import java.util.List;

/**
 * A central utility for creating and managing caches.
 * The motivation for this class is to centralize caching so that the caches can be cleared during performance analysis.
 * @author dawood
 *
 */
public class Cache {

//	public final static String PROCESSOR_CACHE_TYPE_PROPERTY_NAME = "processor.cache.type";
//	
//	private static IMultiKeyCache processorResultsCache;
//	
//	public static IMultiKeyCache processorResultsCache() {
//		if (processorResultsCache == null) {
//			synchronized(Cache.class) {
//				if (processorResultsCache == null) {
//					String propValue = ENGProperties.instance().getProperty(PROCESSOR_CACHE_TYPE_PROPERTY_NAME, "memory");
//					if (propValue.equalsIgnoreCase("memory")) {
//						processorResultsCache = new MemoryCache();
//					} else {
//						throw new IllegalArgumentException("Unrecognized feature cache type: " + propValue);
//					}
//				}
//			}
//		}
//		return processorResultsCache;
//	}
	
	private final static List<IMultiKeyCache> activeCaches = new ArrayList<IMultiKeyCache>();

	/**
	 * Get a new cache that is managed by the caller.  The returned cache is
	 * not considered "global" and will not be cleared on a call to {@link #clearManagedCaches()}.
	 * @return
	 */
	public static <KEYS,ITEM> IMultiKeyCache<KEYS,ITEM> newMemoryCache() {
		IMultiKeyCache<KEYS,ITEM> cache = new MemoryCache<KEYS,ITEM>();
		return cache;
	}
	
	/**
	 * Get a new instance of a cache that can be cleared by a call to {@link #clearManagedCaches()}.
	 * The returned cache should be removed from the list of caches when it is done being used.
	 * @return
	 */
	public static IMultiKeyCache newManagedMemoryCache() {
		IMultiKeyCache cache = new MemoryCache();
		synchronized (activeCaches) {
			activeCaches.add(cache);
		}
		return cache;
	}
	
	/**
	 * Free an instance returned by a call to {@link #newManagedMemoryCache()}.
	 * This cache will no longer be referenced here and will not be cleared on a
	 * call to {@link #clearManagedCaches()}.
	 * @param cache
	 */
	public static void freeManagedCache(IMultiKeyCache cache) {
		synchronized (activeCaches) {
			activeCaches.remove(cache);
		}
	}

	/**
	 * Clear all caches created by {@link #newManagedMemoryCache()} and not yet removed
	 * through {@link #freeManagedCache(IMultiKeyCache)}.
	 */
	public static void clearManagedCaches() {
		synchronized (activeCaches) {
			for (IMultiKeyCache c : activeCaches) 
				c.clear();
		}
	}
}
