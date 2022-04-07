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
import java.util.List;

import org.eng.aisp.SoundTestUtils;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractItemStorageTest<ITEM> {

	public AbstractItemStorageTest() {
		super();
	}

	/**
	 * Get the storage for the items being used.
	 * The instance should have full privileges.
	 * @return never null
	 */
	public abstract IItemStorage<ITEM> getStorage() ;
	
	
	@Test
	public void testAddDeleteAndIDOrdering() throws IOException, StorageException {
	
		IItemStorage<ITEM> s = getStorage(); 
		s.connect();
		s.clear();
		Assert.assertTrue(s.count() == 0);
	
		List<String> plist = new ArrayList<String>();
		for (int i=0 ; i<5 ; i++) {

			ITEM item = getItemToStore(i); 
			String id =  s.add(item);
	
			Assert.assertTrue(id != null);
			Assert.assertTrue(s.count() == i+1);
	
			plist.add(id);
		}
	
		List<String> ids = new ArrayList<String>();
		Iterable<String> idIter = s.getIDs(); 
		Assert.assertTrue(idIter != null);
		for (String id : idIter) 
			ids.add(id);
	
		Assert.assertTrue(ids.size() == 5);

		// Make sure the ids come back in LIFO order.
		Collections.reverse(ids);
		Assert.assertTrue(plist.equals(ids));
		
		for (String id : ids) {
			boolean resp = s.delete(id); 
			Assert.assertTrue(resp);
		}
		Assert.assertTrue(s.count() == 0);
		
		boolean resp = s.delete(ids.get(0));
		Assert.assertTrue(!resp);
			
		s.clear();
		s.disconnect();
	}
	
	@Test
	public void testFindItem() throws IOException, StorageException {
	
		IItemStorage<ITEM> s = getStorage(); 
		s.connect();
		s.clear();
		Assert.assertTrue(s.count() == 0);
		ITEM item = null; 
		try {
			item = s.findItem("junit-expected-bad-id");	// Use a bad id
		} catch (Exception e) {
			;	// Might throw an exception.
		}
		Assert.assertTrue(item == null);

		item = getItemToStore(0); 
		String id = s.add(item);
		Assert.assertTrue(id != null);
		item = s.findItem(id);
		Assert.assertTrue(item != null);
	
		// Delete the item and try and find the item with good id.
		boolean r = s.delete(id);
		Assert.assertTrue(r);
		item = s.findItem(id);
		Assert.assertTrue(item == null);
		
		s.clear();
		s.disconnect();
	}
	
	@Test
	public void testUpdate() throws IOException, StorageException {
		Assert.assertTrue(this.getItemToStore(0).equals(this.getItemToStore(0))); 	// Test depends on this.
		Assert.assertTrue(!this.getItemToStore(1).equals(this.getItemToStore(0))); 	// Test depends on this.
		
		IItemStorage<ITEM> s = getStorage();
		s.connect();
		s.clear();
		Assert.assertTrue(s.count() == 0);
	
		List<String> ids = new ArrayList<String>();
		List<ITEM> items = new ArrayList<ITEM>();
		int count = 5;
		for (int i=0 ; i<count ; i++) {
			ITEM item = getItemToStore(i); 
			String id =  s.add(item);
			Assert.assertTrue(id != null);
			Assert.assertTrue(s.count() == i+1);
			ids.add(id);
			items.add(item);
		}

		for (int i=0 ; i<count ; i++) {
			ITEM item = getItemToStore(count + i); 
			String id = ids.get(i);
			s.update(id, item);
			Assert.assertTrue(s.count() == count);
			ITEM oldItem = items.get(i);
			ITEM updatedItem = s.findItem(id);
			Assert.assertTrue(updatedItem != null);
			Assert.assertTrue(updatedItem.equals(item));
			Assert.assertTrue(!updatedItem.equals(oldItem));
		}
			
		s.clear();
		s.disconnect();
	}
	
	@Test
	public void testItems() throws StorageException, IOException {
		Assert.assertTrue(this.getItemToStore(0).equals(this.getItemToStore(0))); 	// Test depends on this.
		Assert.assertTrue(!this.getItemToStore(1).equals(this.getItemToStore(0))); 	// Test depends on this
		
		IItemStorage<ITEM> storage = getStorage();
		storage.connect();
		storage.clear();
		Assert.assertTrue(storage.count() == 0);
		
		int count = 3;
		List<String> idList = new ArrayList<String>();
		List<ITEM> items = new ArrayList<>();
		for (int i=0 ; i<count ; i++) {
			ITEM item = this.getItemToStore(i);
			String id = storage.add(item);
			Assert.assertTrue(id != null);
			idList.add(id);
			List<String> currentIDs = SoundTestUtils.iterable2List(storage.getIDs());
			Collections.reverse(currentIDs);	// getIDs() returns the items in most recent to least recently added order.
			Assert.assertTrue(idList.equals(currentIDs));
			items.add(item);
		}
		
		// Make sure items() returns the correct count. 
		int stored = 0;
		for (ITEM item : storage.items()) 
			stored++;
		Assert.assertTrue(stored == count);
		
		// Make sure the retrieve item equals the original
		for (int i=0 ; i<idList.size(); i++) {
			ITEM originalItem = items.get(i);
			ITEM storedItem = storage.findItem(idList.get(i));
			Assert.assertTrue(storedItem != null);
			Assert.assertTrue(originalItem.equals(storedItem));
		}
		
		// Now clear the repo and make sure there are no items left.
		storage.clear();
		stored = 0;
		for (ITEM item : storage.items()) 
			stored++;
		Assert.assertTrue(stored == 0);
		
		storage.disconnect();

	}

	/**
	 * Get an instance of the given item, that is differentiated by the given index.
	 * @param index
	 * @return never null
	 * @throws IOException 
	 * @throws StorageException 
	 */
	public abstract ITEM getItemToStore(int index) throws IOException, StorageException ;

	@Test
	public void testDeleteBadID() throws IOException, StorageException  {
		IItemStorage<ITEM> s = getStorage(); 
		s.connect();
		s.clear();
		Assert.assertTrue(s.count() == 0);
		try {
			s.delete("");
			Assert.fail("Did not get expected exception");
		} catch (Exception e ) {
			;	// got expected exception.
		}
		ITEM item0 = getItemToStore(0);
		String id = s.add(item0);
	
		List<String> ids = new ArrayList<String>();
		ids.add(id);
		ids.add("");
		try {
			s.delete(ids);
			Assert.fail("Did not get expected exception");
		} catch (Exception e ) {
			;	// got expected exception.
		}
		
		s.clear();
		s.disconnect();
	}

	@Test
	public void testListDelete() throws IOException, StorageException {
		ITEM item0 = getItemToStore(0);
		ITEM item1 = getItemToStore(1);
		
		IItemStorage<ITEM> s = getStorage(); 
		s.connect();
		s.clear();
		Assert.assertTrue(s.count() == 0);
	
		String id0 = s.add(item0);
		Assert.assertTrue(id0 != null);
		Assert.assertTrue(s.count() == 1);
	
		String id1 = s.add(item1);
		Assert.assertTrue(s.count() == 2);
		
		List<String> ids = new ArrayList<String>();
		ids.add(id0);
		ids.add(id1);
		boolean r = s.delete(ids);
		Assert.assertTrue(r);
		Assert.assertTrue(s.count() == 0);
		
		s.clear();
		s.disconnect();
	}

	@Test
	public void testAddFindAndDeleteByID() throws IOException, StorageException {
		ITEM item0 = getItemToStore(0);
		ITEM item1 = getItemToStore(1);
	
		/// The test depends on this
		Assert.assertTrue(item1.equals(getItemToStore(1)));
		Assert.assertTrue(!item1.equals(getItemToStore(0)));
		
		IItemStorage<ITEM> s = getStorage(); 
		s.connect();
		s.clear();
		Assert.assertTrue(s.count() == 0);
	
		String id0 = s.add(item0);
		Assert.assertTrue(id0 != null);
		Assert.assertTrue(s.count() == 1);
	
		String id1 = s.add(item1);
		Assert.assertTrue(s.count() == 2);
		ITEM sp = s.findItem(id1);
		Assert.assertTrue(sp != null);
		Assert.assertTrue(sp.equals(item1));
		
		List<String> ids = new ArrayList<String>();
		ids.add(id0);
		ids.add(id1);
		List<ITEM> items = SoundTestUtils.iterable2List(s.findItems(ids));
		Assert.assertTrue(items.size() == 2);
		Assert.assertTrue(item0.equals(items.get(0)));
		Assert.assertTrue(item1.equals(items.get(1)));

		
		s.delete(id1);
		sp = s.findItem(id1);
		Assert.assertTrue(sp == null);
	
		Assert.assertTrue(s.count() == 1);
		sp = s.findItem(id0);
		Assert.assertTrue(sp != null);
		Assert.assertTrue(sp.equals(item0));
	
		s.delete(id0);
		sp = s.findItem(id0);
		Assert.assertTrue(sp == null);
		
		Assert.assertTrue(s.count() == 0);
		
		s.disconnect();
	}
	
	@Test
	public void testTwoInstances() throws IOException, StorageException {
		ITEM item0 = getItemToStore(0);
		ITEM item1 = getItemToStore(1);
	
		/// The test depends on this
		Assert.assertTrue(item1.equals(getItemToStore(1)));
		Assert.assertTrue(!item1.equals(getItemToStore(0)));
		
		IItemStorage<ITEM> s1 = getStorage(); 
		IItemStorage<ITEM> s2 = getStorage(); 
		s1.connect(); s2.connect();
		s1.clear();
		Assert.assertTrue(s1.count() == 0);
		Assert.assertTrue(s2.count() == 0);
	
		String id0 = s1.add(item0);
		Assert.assertTrue(id0 != null);
		Assert.assertTrue(s2.count() == 1);
		List<String> ids = SoundTestUtils.iterable2List(s2.getIDs());
		Assert.assertTrue(ids.size() == 1);
		Assert.assertTrue(ids.get(0).equals(id0));

		List<ITEM> items = SoundTestUtils.iterable2List(s2.items());
		Assert.assertTrue(items.size() == 1);
		Assert.assertTrue(items.get(0).equals(item0));

		s1.clear();
		s2.clear();
	
		Assert.assertTrue(s1.count() == 0);
		Assert.assertTrue(s2.count() == 0);
		
		s1.disconnect();
		s2.disconnect();
	}
	
	@Test
	public void testStoredItemByIDIterable() throws StorageException, IOException {
		/// The test depends on this
		
		IItemStorage<ITEM> storage = getStorage(); 
		storage.connect();
		List<ITEM> items = new ArrayList<ITEM>();
		List<String> ids = new ArrayList<String>();
		for (int i=0 ; i<20 ; i++) {
			ITEM item = getItemToStore(i);
			String id =storage.add(item);
			items.add(item);
			ids.add(id);
		}
		validateStoredItemsBatchedIterable(storage, ids, items, 1);
		validateStoredItemsBatchedIterable(storage, ids, items, 10);
		validateStoredItemsBatchedIterable(storage, ids, items, 3);

		storage.clear();	
		storage.disconnect();
	}


	protected  void validateStoredItemsBatchedIterable(IItemStorage<ITEM> storage, List<String> ids, List<ITEM> items, int batchSize) {
		Iterable<ITEM> iterable = new StoredItemByIDIterable<ITEM>(storage, ids, batchSize);
		int index = 0;
		for (ITEM item : iterable) {
			ITEM expectedItem = items.get(index);
			Assert.assertTrue(item != null);
			Assert.assertTrue(expectedItem.equals(item));
			index++;
		}
		Assert.assertTrue(index == items.size());
	}

	/**
	 * Used in RBAC testing to create the item that is stored in admin accessible storage.
	 * @return
	 * @throws IOException
	 * @throws StorageException
	 */
	public ITEM getAdminItemToStore() throws IOException, StorageException {
		return this.getItemToStore(0);
	}

	/**
	 * Used in RBAC testing to create the item that is stored in user accessible storage.
	 * @return
	 * @throws IOException
	 * @throws StorageException
	 */
	public ITEM getUserItemToStore() throws IOException, StorageException {
		return this.getItemToStore(1);
	}

}
