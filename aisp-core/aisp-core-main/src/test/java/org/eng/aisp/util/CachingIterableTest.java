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

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.eng.ENGTestUtils;
import org.eng.cache.Cache;
import org.eng.cache.IMultiKeyCache;
import org.eng.util.CachingIterable;
import org.junit.Assert;
import org.junit.Test;

public class CachingIterableTest {

	/**
	 * Let other implementations override this and still get the implementation tested with the methods here.
	 * @param intList
	 * @param cache may be null in which case the default cache is used.
	 * @return
	 */
	protected CachingIterable<Integer> getCachingIterable(List<Integer> intList, IMultiKeyCache<Long,Integer> cache) {
		CachingIterable<Integer> ci = new CachingIterable<Integer>(intList, cache);
		return ci;
	}
	
	protected IMultiKeyCache<Long,Integer> getCache() {
		IMultiKeyCache<Long,Integer> cache = Cache.newMemoryCache();
		return cache;
		
	}
	
	@Test
	public void testBasicIteration() {
		List<Integer> intList= ENGTestUtils.makeList(5);
		Assert.assertTrue(intList.get(0) == 0);	// Make sure the list is 0 based.
		CachingIterable<Integer> ci = getCachingIterable(intList, null); 
		
		Assert.assertTrue(ci.iterator().hasNext());
		Assert.assertTrue(ci.iterator().next() == 0);
		Assert.assertTrue(ci.iterator().next() == 0);

		Iterator<Integer> iter = ci.iterator();
		for (int i=0 ; i<intList.size() ; i++) {
			Assert.assertTrue("i=" + i, iter.hasNext());
			Assert.assertTrue("i=" + i, iter.hasNext());
			Assert.assertTrue("i=" + i, iter.next() == i);
			Assert.assertTrue(ci.iterator().next() == 0);
		}
		Assert.assertTrue(!iter.hasNext());
		try {
			iter.next();
			Assert.fail("Did not get NoSuchElementException");
		} catch (NoSuchElementException e) {
			;
		}
		Assert.assertTrue(ci.iterator().next() == 0);
		
		int index = 0;
		for (Integer i : ci) {
			Assert.assertTrue(i == index);
			index++;
		}
	}
	
	@Test
	public void testCacheMiss() throws InterruptedException {
		List<Integer> intList= ENGTestUtils.makeList(5);
		Assert.assertTrue(intList.get(0) == 0);	// Make sure the list is 0 based.
		IMultiKeyCache<Long,Integer> cache = getCache(); 
		cache.clear();
		Assert.assertTrue(cache.size() == 0);	// Otherwise test will not pass. 
		
		CachingIterable<Integer> ci = getCachingIterable(intList, cache); 
		
		// Fill the cache
		Iterator<Integer> iter = ci.iterator();
		for (int i=0; i<intList.size(); i++)
			Assert.assertTrue(iter.next() == i);
		
		// Now go through and emulate cache misses but we should get the same items.
		for (int modulus=1 ; modulus<=3 ; modulus++) {
			iter = ci.iterator();
			for (int i=0; i<intList.size(); i++) {
				Assert.assertTrue(iter.next() == i);
				if (i % modulus == 0)
					cache.clear();
			}
		}
		
		
		// Make sure the cache gets emptied on finalization of the CachingIterable.
		ci = null;
		System.gc();
		System.runFinalization();
		Thread.sleep(50);
		Assert.assertTrue(cache.size() == 0);
		
	}
}
