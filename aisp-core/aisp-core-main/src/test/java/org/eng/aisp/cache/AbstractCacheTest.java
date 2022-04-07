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
package org.eng.aisp.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eng.cache.IMultiKeyCache;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractCacheTest {


	public abstract IMultiKeyCache<Serializable,Serializable> getCache();

	private Serializable instanceKey = UUID.randomUUID().toString();	// for disk cache tests.
	
	@Test
	public void testPutGetScalar() {
		IMultiKeyCache<Serializable,Serializable> cache = getCache(); 
		Object o;
		double[] data = new double[] { 1.2, 3.4, 5.6 };
		
		// Put a single item
		for (int i=0 ; i<10 ; i++) {
			Integer t = new Integer(i);
			cache.put(t, i+1, i+2);
		}
	
		for (int i=0 ; i<10 ; i++) {
			// Get the object and make sure it is the right one.
			o = cache.get(i+1, i+2);
			Assert.assertTrue(o != null);
			Assert.assertTrue(o instanceof Integer); 
			Integer value = (Integer)o;
			Assert.assertTrue(value.equals(i));
	
			// Make sure that reversing the keys does not find the item.
			o = cache.get(i+2,i+1);
			Assert.assertTrue(o == null);
	
			// Make sure that same 1st 2 keys plus another key does not find the item. 
			o = cache.get(i+1,i+2,i);
			Assert.assertTrue(o == null);
		}
	
		
		// Now clear the cache and make sure the items were removed.
		cache.clear();
		for (int i=0 ; i<10 ; i++) {
			o = cache.get(i+1,i+2);
			Assert.assertTrue(o == null);
		}
		
		cache.clear();
	}

	protected double[] newData(double start) {
		double[] data = new double[3];
		data[0] = start;
		data[1] = start + 1.1;
		data[2] = start + 1200.5;
		return data;
	}

	protected double[] newData(double start, int count) {
		double[] data = new double[count];
		for (int i=0 ; i<data.length; i++)
			data[i] = start+i;
		return data;
	}

	protected double[] newDataShuffle(double start, int count) {
		double[] data = newData(start,count); 
		List<Double> dlist = new ArrayList<Double>();
		for (int i=0 ; i<data.length; i++)
			dlist.add(data[data.length-i-1]);
//		Collections.shuffle(dlist);
		for (int i=0 ; i<data.length; i++)
			data[i] = dlist.get(i);
		return data;
	}

	@Test
	public void testPutGetArray() {
		IMultiKeyCache<Serializable,Serializable> cache = getCache(); 
		Object o;
		double[] data = new double[] { 1.2, 3.4, 5.6 };
		
		// Put a single item
		for (int i=0 ; i<10 ; i++) {
			Integer t = new Integer(i);
			cache.put(t,  i+1, newData(i));
			o = cache.get(i+1, newData(i));
			Assert.assertTrue(o != null);
		}
	
		for (int i=0 ; i<10 ; i++) {
			// Get the object and make sure it is the right one.
			o = cache.get(i+1, newData(i));
			Assert.assertTrue(o != null);
			Assert.assertTrue(o instanceof Integer); 
			Integer value = (Integer)o;
			Assert.assertTrue(value.equals(i));
	
			// Make sure that reversing the keys does not find the item.
			o = cache.get(newData(i), i+1);
			Assert.assertTrue(o == null);
	
			o = cache.get(i+1, newData(i+1));
			Assert.assertTrue(o == null);
	
			// Make sure that same 1st 2 keys plus another key does not find the item. 
			o = cache.get(i+1,newData(i+1), i+2);
			Assert.assertTrue(o == null);
		}
		cache.clear();
	}

	@Test
	public void testCollisions() {
		testCollisions2();
		testCollisions2();
	}

	public void testCollisions2() {
		int keySize=2;
		int itemCount = 20;
		IMultiKeyCache<Serializable,Serializable > cache = getCache(); 

		cache.clear();
		
		// Load up the cache
		for (int i=0 ; i<itemCount ; i++)  {
			double[] key = newData(i, keySize);
			cache.put(i, instanceKey, key);
			Object o = cache.get(instanceKey,key);
			if (o == null)
				o = cache.get(instanceKey,key);
			Assert.assertTrue(o != null);
			o = cache.get(instanceKey,key);
			if (o == null)
				o = cache.get(instanceKey,key);
			Assert.assertTrue(o != null);
			Assert.assertTrue(o instanceof Integer);
			Integer value = (Integer)o;
			Assert.assertTrue(value == i);
		}
	
//		// Read everything now that the cache is fully loaded. 
		for (int i=0 ; i<itemCount ; i++)  {
			double[] key = newData(i, keySize);
			if (i == 0)
				i=0;
			Object o = cache.get(instanceKey, key);
			if (o == null)
				o = cache.get(instanceKey, key);
			Assert.assertTrue("i=" + i, o != null);
			Assert.assertTrue(o instanceof Integer);
			Integer value = (Integer)o;
			Assert.assertTrue(value == i);
		}
	
		// Make sure keys that weren't used dont'return anything.
		for (int i=0 ; i<itemCount ; i++)  {
			double[] key = newDataShuffle(i, keySize);
			Object o = cache.get(instanceKey, key);
			Assert.assertTrue("i=" + i, o == null);
			key = newData(i, keySize-1);
			o = cache.get(instanceKey, key);
			Assert.assertTrue(o == null);
			key = newData(i, keySize+1);
			o = cache.get(instanceKey, key);
			Assert.assertTrue(o == null);
			key = newData(itemCount+i, keySize);
			o = cache.get(instanceKey, key);
			Assert.assertTrue(o == null);
		}
		
		// Now overwrite the values with i+1  
		for (int i=0 ; i<itemCount ; i++)  {
			double[] key = newData(i, keySize);
			cache.put(i+1, instanceKey, key);
			Object o = cache.get(instanceKey, key);
			Assert.assertTrue(o != null);
			Assert.assertTrue(o instanceof Integer);
			Integer value = (Integer)o;
			Assert.assertTrue(value == i+1);
		}
	
		cache.clear();
	}

}
