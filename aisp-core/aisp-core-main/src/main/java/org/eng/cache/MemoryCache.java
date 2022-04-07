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

import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections.map.ReferenceMap;

public class MemoryCache<KEYS, ITEM> extends AbstractMultiKeyCache<KEYS, ITEM> implements IMultiKeyCache<KEYS, ITEM> {

//	protected final SoftHashMap cache = new SoftHashMap(true, 1000);
	@SuppressWarnings("unchecked")
	protected final Map<Long, KeyItemContainer> cache = Collections.synchronizedMap(new ReferenceMap(ReferenceMap.SOFT, ReferenceMap.SOFT, 4096, (float).75, true));
//	protected final Map cache = Collections.synchronizedMap(new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT, 4096, (float).75, false));
//	protected final Map cache = Collections.synchronizedMap(new ReferenceMap(ReferenceMap.SOFT, ReferenceMap.SOFT, 4096, (float).75, true));

	@Override
	public void clear() {
		cache.clear();
	}

	@Override
	protected void store(Long key, KeyItemContainer container) {
		cache.put(key, container);
	}

	@Override
	protected KeyItemContainer lookup(Long key) {
		return cache.get(key);
	}

	@Override
	protected void remove(Long key) {
		cache.remove(key);
	}

	@Override
	public int size() {
		return cache.size();
	}


	
	
}
