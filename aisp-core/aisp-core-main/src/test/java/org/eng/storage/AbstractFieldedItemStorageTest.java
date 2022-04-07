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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eng.aisp.SoundTestUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public abstract class AbstractFieldedItemStorageTest<ITEM> extends AbstractItemStorageTest<ITEM> {

	/**
	 * Get 1 or more matching fields of the object that was returned by
	 * {@link #getItemToStore(int)} with the same index value.
	 * 
	 * @param index
	 * @return return null if item fields are not exposed by the storage implementation. 
	 */
	protected abstract FieldValues getMatchingFields(int index);

	/**
	 * Get 1 or more non-matching fields of the object that was returned by
	 * {@link #getItemToStore(int)} with the same index value.
	 * 
	 * @param index
	 * @return return null if item fields are not exposed by the storage implementation. 
	 */
	protected abstract FieldValues getNonMatchingFields(int index);

	/**
	 * Get the instance of IFieldedItemStorage to test.
	 * @return never null.
	 */
	protected IFieldedItemStorage<ITEM> getFieldedItemStorage() {
		IDeleteableItemStorage<?> iis = getStorage();
		if (iis instanceof IFieldedItemStorage)
			return (IFieldedItemStorage<ITEM>)iis;
		throw new RuntimeException("Sub class did not provide an instance of getStorage() that returned " + IFieldedItemStorage.class.getName() + ".  Override to fix.");
	}
	/**
	 * Look up the stored item created with {@link #getItemToStore(int)} passing
	 * in index. May be overridden by subclasses, which should call the super
	 * class method.
	 * 
	 * @param s
	 * @param expectedItem
	 * @param storedItemIndex
	 *            of item to lookup.
	 * @throws StorageException
	 */
	protected void lookupAndValidateItem(IFieldedItemStorage<ITEM> s, ITEM expectedItem, int storedItemIndex)
			throws StorageException {
		FieldValues match = getMatchingFields(storedItemIndex);
		// First try findItems()
		Iterable<ITEM> iterable = s.findItems(match);
		Iterator<ITEM> iter = iterable.iterator();
		Assert.assertTrue(iter.hasNext());
		ITEM found = iter.next();
		Assert.assertTrue(found != null);
		Assert.assertTrue(found.equals(expectedItem));

		// Next try findIDs()
		Iterable<String> ids= s.findIDs(match);
		Iterator<String> idsIter = ids.iterator();
		Assert.assertTrue(idsIter.hasNext());
		String id = idsIter.next();
		Assert.assertTrue(id != null);
		found = s.findItem(id);
		Assert.assertTrue(found.equals(expectedItem));

	}

	@Test
	public void testFieldMatching() throws IOException, StorageException {
		ITEM item0 = getItemToStore(0);
		ITEM item1 = getItemToStore(1);
		
		/// The test depends on this
		Assert.assertTrue(item1.equals(getItemToStore(1)));
		Assert.assertTrue(!item1.equals(getItemToStore(0)));

		IFieldedItemStorage<ITEM> s = getFieldedItemStorage();
		s.connect();
		s.clear();
		Assert.assertTrue(s.count() == 0);
		// SensorProfile sp = new SensorProfile("myname", "mytype");
		ITEM sp = item0; 
		String id = s.add(sp);
		Assert.assertTrue(s.count() == 1);

		sp = item1; 
		id = s.add(sp);
		
		// Make sure items exist.
		Assert.assertTrue(s.count() == 2);
		Iterable<String> ids = s.getIDs();
		Assert.assertTrue(ids != null);
		Iterator<String> idIter = ids.iterator();
		Assert.assertTrue(idIter != null);
		Assert.assertTrue(idIter.hasNext());
		Assert.assertTrue(idIter.hasNext());

		FieldValues match = getNonMatchingFields(1);
		Assume.assumeTrue("Field testing not supported", match != null);
//		if (match == null)
//			return;	// item does not expose fields.

		// Look for a real item
		lookupAndValidateItem(s, sp, 1);

		// Look for a non-existent item
		match = getNonMatchingFields(1);
		Iterable<ITEM> iterable = s.findItems(match);
		Iterator<ITEM> iter = iterable.iterator();
		Assert.assertTrue(!iter.hasNext());

		s.clear();
		s.disconnect();

	}
	
	@Test
	public void testPagedIDs() throws IOException, StorageException {
		IFieldedItemStorage<ITEM> storage = getFieldedItemStorage();
		storage.connect();
		storage.clear();
		int maxPageSize = 10;
		int pageCount = 3;
		int count = maxPageSize * pageCount;
		List<String> ids = new ArrayList<String>();
		for (int i=0 ; i<count ; i++) {
			ITEM item = getItemToStore(i);	
			String id = storage.add(item);
			ids.add(id);
		}
		// Order the ids so that the most recently added ones are first in the list, 
		// which is how we expect them to come back from storage.
		Collections.reverse(ids);
		
		for (int pageSize = 2 ; pageSize<maxPageSize ; pageSize++) {
			int pages = (int)Math.ceil((double)count / pageSize);
			List<String> pagedIDs;
			List<String> foundIDs = new ArrayList<String>();
			for (int pageIndex= 0 ; pageIndex<pages ; pageIndex++) {
				pagedIDs = SoundTestUtils.iterable2List(storage.findPagedIDs(pageIndex, pageSize, null));
				foundIDs.addAll(pagedIDs);
				if (pageIndex == 0)	// Make sure the first page is full.  If so, others will likely be also.
					Assert.assertTrue("pageSize=" + pageSize, pagedIDs.size() == pageSize);
			}
			Assert.assertTrue("pageSize=" + pageSize, ids.equals(foundIDs));
			pagedIDs = SoundTestUtils.iterable2List(storage.findPagedIDs(pages, pageSize, null));
			Assert.assertTrue("pageSize=" + pageSize, pagedIDs.size() == 0);
		}

		storage.clear();
	}
}
