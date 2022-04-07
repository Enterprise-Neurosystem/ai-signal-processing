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
package org.eng.aisp.util;

import java.util.List;

import org.eng.cache.IMultiKeyCache;
import org.eng.cache.MemoryCache;
import org.eng.util.CachingIterable;

public class MemoryCachingIterableTest extends CachingIterableTest {

	protected CachingIterable<Integer> getCachingIterable(List<Integer> intList) {
		CachingIterable<Integer> ci = new CachingIterable<Integer>(intList);
		return ci;
	}

	@Override
	protected IMultiKeyCache<Long, Integer> getCache() {
		return new MemoryCache<Long,Integer>(); 
	}
}
