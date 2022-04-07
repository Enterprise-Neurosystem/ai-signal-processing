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

import org.eng.ENGProperties;

public abstract class AbstractMultiKeyCache<KEYS extends Object, ITEM extends Object> implements IMultiKeyCache<KEYS, ITEM> {

	private static boolean ENABLE_CACHING = ENGProperties.instance().getProperty("multikeycache.enabled", true);
//	private static boolean ENABLE_CACHING = false; 
	
	public ITEM get(KEYS...keys) {
		if (!ENABLE_CACHING)
			return null;
		
		Long key = CacheUtil.hash(keys);
		KeyItemContainer ci = lookup(key); 

		if (ci == null)
			return null;
		if (!CacheUtil.equalObjectArrays(keys, ci.keys))
			return null;
		return (ITEM)ci.itemToCache;
	}

	public void put(ITEM itemToCache, KEYS...keys) {
		if (!ENABLE_CACHING)
			return;
		Long key = CacheUtil.hash(keys);
		KeyItemContainer  container = new KeyItemContainer(keys, itemToCache);
		store(key,container);
	}
	
	@Override
	public void remove(KEYS... keys) {
		if (!ENABLE_CACHING)
			return;
		Long key = CacheUtil.hash(keys);
		remove(key);
	}

	/**
	 * Store the items contained in the container associated with the given
	 * key so that {@link #lookup(Long)} can reconstitute the KeyItemContainer
	 * when given the same key.
	 */
	protected abstract void store(Long key, KeyItemContainer container) ;
	
	/**
	 * Look for the contents of a KeyItemContainer stored under the given key using {@link #store(Long, KeyItemContainer)}.
	 * @param key
	 * @return null if not found/evicted from the cache.
	 */
	protected abstract KeyItemContainer lookup(Long key);

	/**
	 * Remove the item stored under the given key.
	 * @param key
	 */
	protected abstract void remove(Long key);
}
