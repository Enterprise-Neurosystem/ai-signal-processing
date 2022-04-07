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

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;


public abstract class AbstractReferencingStringSetStorageTest<SETITEM extends NamedStringSet, REFITEM> extends AbstractItemStorageTest<SETITEM> {
	
	public abstract IReferencingStringSetStorage<SETITEM, REFITEM> getSetStorage();

	protected abstract REFITEM getReferencedItem(int index);

	/**
	 * Make sure that times for an item stored after another are later.
	 * @throws StorageException
	 * @throws InterruptedException
	 */
	@Test
	public void testUpdateCreateTimes() throws StorageException, InterruptedException {
		IReferencingStringSetStorage<SETITEM, REFITEM> storage = this.getSetStorage();
		storage.connect();
		storage.clear();
		Assert.assertTrue(storage.count() == 0);
		
		String name1 = "set1";
		String id1 = storage.createEmptySet(name1);
		SETITEM setItem1 = storage.findItem(id1);	
		Thread.sleep(10);

		String name2 = "set2";
		String id2 = storage.createEmptySet(name2);
		SETITEM setItem2 = storage.findItem(id2);	
		verifyTimestamps(setItem1, setItem2, false);
	}

	@Test
	public void testSetName() throws StorageException, InterruptedException {
		IReferencingStringSetStorage<SETITEM, REFITEM> storage = this.getSetStorage();
		storage.connect();
		storage.clear();
		Assert.assertTrue(storage.count() == 0);
		
		String name = "junit";
		String id = storage.createEmptySet(name);
		SETITEM setItem = storage.findItem(id);	
		Assert.assertTrue(setItem.getName().equals(name));
		
		// Change the name
		Thread.sleep(2000);
		name = "newname";
		storage.setName(id, name);
		SETITEM setItem2 = storage.findItem(id);	
		Assert.assertTrue(setItem2.getName().equals(name));

		verifyTimestamps(setItem, setItem2,true);
	}
	
	/**
	 * Make sure the two creattion times are the same and the update time of the second is later than the first
	 * @param setItem1
	 * @param setItem2
	 * @param b 
	 */
	protected void verifyTimestamps(SETITEM setItem1, SETITEM setItem2, boolean sameCreation) {
		Assert.assertTrue(setItem1 != setItem2);
		long c1 = setItem1.getCreationTime();
		long c2 = setItem2.getCreationTime();
		if (sameCreation)
			Assert.assertTrue(c1 == c2);
		else
			Assert.assertTrue(c2 > c1);
			
		long u1 = setItem1.getUpdateTime();
		long u2 = setItem2.getUpdateTime();
		Assert.assertTrue( u2 > u1);
		
	}

	@Test
	public void testCreateEmptySetAndDelete() throws StorageException {
		IReferencingStringSetStorage<SETITEM, REFITEM> storage = this.getSetStorage();
		storage.connect();
		storage.clear();
		Assert.assertTrue(storage.count() == 0);
		
		String name = "junit";
		String id = storage.createEmptySet(name);
		SETITEM setItem = storage.findItem(id);
		validateSetMembers(setItem, null);
		Assert.assertTrue(setItem.getName().equals(name));

		// Add a 2nd item
		id = storage.createEmptySet(name);
		validateStoredSet(storage, id , null);
		Assert.assertTrue(setItem != null);
		Assert.assertTrue(storage.count() == 2);
		storage.delete(id);
		Assert.assertTrue(storage.count() == 1);
		storage.clear();
		Assert.assertTrue(storage.count() == 0);

		storage.disconnect();
	}

//	@Test
//	public void testAddBadSet() throws StorageException {
//		IReferencingStringSetStorage<SETITEM, REFITEM> storage = this.getSetStorage();
//		storage.connect();
//		storage.clear();
//		Assert.assertTrue(storage.count() == 0);
//		String name="junit2";	
//		
//		String setId = storage.createEmptySet(name);
//		SETITEM set = storage.findItem(setId);
//		set.addMember("abc");	// this is not the id of a stored item.
//
//		// Try to add this set with bad member.
//		try {
//			String id = storage.add(set);
//			Assert.assertTrue(id == null);
//		} catch (Exception e) {
//			;	// some implementations might throw an exception. 
//		}
//		validateStoredSet(storage, setId, null);	
//		
//		// Now try to use update() to change the set.
//		try {
//			boolean r = storage.update(setId, set);
//			Assert.assertTrue(r == false);
//		} catch (Exception e) {
//			;	// some implementations might throw an exception. 
//		}
//		validateStoredSet(storage, setId, null);	
//		
//		storage.clear();
//		storage.disconnect();
//	}

	@Test
	public void testDeleteReferencedItemByDelete() throws StorageException {
		Assert.assertTrue(!getReferencedItem(1).equals(getReferencedItem(2)));	// Test depends on this.
		IReferencingStringSetStorage<SETITEM, REFITEM> storage = this.getSetStorage();
		storage.connect();
		storage.clear();
		Assert.assertTrue(storage.count() == 0);
		String name="junit2";
		String item1Name = "item1";
		String item2Name = "item2";
		
		// Create a set with two items.
		String setId = storage.createEmptySet(name);
		REFITEM refItem1 = getReferencedItem(1);
		String refId1 = storage.addSetItem(setId, item1Name, refItem1);
		REFITEM refItem2 = getReferencedItem(2);
		String refId2 = storage.addSetItem(setId, item2Name, refItem2);

		// Verify the set contents and stored items
		validateStoredSet(storage, setId, new String[] { refId1, refId2 });
		validateRefItem(storage, setId, refId1, refItem1);
		validateRefItem(storage, setId, refId2, refItem2);
		
		// Delete 1 member (refId1) and expect it to be removed from the set and storage.
//		if (useUpdateToDelete) {
//			SETITEM set = storage.findItem(setId);
//			set.removeMember(refId1);
//			storage.update(setId, set);
//		} else {
			storage.deleteSetItem(setId, refId1);
//		}
		validateStoredSet(storage, setId, new String[] { refId2});
		validateRefItem(storage, setId, refId1, null);
		validateRefItem(storage, setId, refId2, refItem2);	// Set still contains refItem2.

		// Delete the set and expect it and the referenced item (refid2) to be deleted.
		storage.delete(setId);
		SETITEM set = storage.findItem(setId);
		Assert.assertTrue(set == null);
		refItem2 = storage.findSetItem(refId2);
		Assert.assertTrue(refItem2 == null);
	
		Assert.assertTrue(storage.count() == 0);
		storage.disconnect();

	}
	/**
	 * 
	 * @param storage
	 * @param setId
	 * @param refId
	 * @param refItem if null, then expect the set to NOT contain the refID and to not find an item under refID.  If not null, then expect to find 
	 * the id in the set members and make sure the item is stored and equals this.
	 * @throws StorageException
	 */
	protected void validateRefItem(IReferencingStringSetStorage<SETITEM, REFITEM> storage, String setId, String refId, REFITEM refItem) throws StorageException {
		SETITEM set = storage.findItem(setId);
		Assert.assertTrue(set != null);
		REFITEM ref = storage.findSetItem(refId);
		Collection<String> members = set.getMembers();
		if (refItem == null) {
			Assert.assertTrue(!members.contains(refId));
		} else { 
			Assert.assertTrue(members.contains(refId));
			Assert.assertTrue(ref != null);
			Assert.assertTrue(ref.equals(refItem));
		}
	}

	protected void validateStoredSet(IReferencingStringSetStorage<SETITEM, REFITEM> storage, String setId, String[] expectedMembers) throws StorageException {
		SETITEM setItem = storage.findItem(setId);
		Assert.assertTrue(setItem != null);
		validateSetMembers(setItem, expectedMembers);
		
	}

	/**
	 * @param setItem
	 * @param expectedMembers
	 */
	private void validateSetMembers(SETITEM setItem, String[] expectedMembers) {
		Collection<String> members = setItem.getMembers();
		if (expectedMembers == null) {
			Assert.assertTrue(members.isEmpty()); 
		} else {
			Assert.assertTrue(members.size() == expectedMembers.length);
			for (String m : expectedMembers) {
				Assert.assertTrue(members.contains(m));
			}
		}
	}
}
