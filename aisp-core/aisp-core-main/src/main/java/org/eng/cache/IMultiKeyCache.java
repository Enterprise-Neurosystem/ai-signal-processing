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

public interface IMultiKeyCache<KEYS, ITEM> {

	@SuppressWarnings("unchecked")
	ITEM get(KEYS... keys);

	@SuppressWarnings("unchecked")
	void put(ITEM itemToCache, KEYS... keys);
	
	void clear();

	@SuppressWarnings("unchecked")
	void remove(KEYS...keys);

	/**
	 * Get the number of items stored in the cache.
	 * @return
	 */
	int size();

}
