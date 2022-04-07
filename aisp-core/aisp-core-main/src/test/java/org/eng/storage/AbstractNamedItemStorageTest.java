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
import java.util.Iterator;
import java.util.List;

import org.eng.aisp.SoundTestUtils;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractNamedItemStorageTest<ITEM> extends AbstractFieldedItemStorageTest<ITEM>  {
	

	public abstract INamedItemStorage<ITEM> getStorage();

	/**
	 * subclasses should use this if they embed the name in their ITEM, as is the case with UserProfile.
	 * @param index
	 * @return
	 */
	protected String getName(int index) {
		if (index == 0)
			return "cognitiveear@gmail.com";	// make more of the REST api test work.
		else if (index % 2 == 0)
			return "junit-itemname-" + index;
		else
			return "junit-itemname " + index;	// Odd items get a space in the name.
	}

	protected void lookupAndValidateNamedItem(INamedItemStorage<ITEM> s, ITEM expectedItem, int storedItemIndex)
			throws StorageException {
		String name = getName(storedItemIndex); 
		Iterable<ITEM> iterable = s.findNamedItems(name);
		Iterator<ITEM> iter = iterable.iterator();
		Assert.assertTrue(iter.hasNext());
		ITEM found = iter.next();
		Assert.assertTrue(found != null);
		Assert.assertTrue(found.equals(expectedItem));

	}
			
	@Test
	public void testAddAndFindNamed() throws IOException, StorageException {
		testAddAndFindNamed(false);
		testAddAndFindNamed(true);
	}

	private void testAddAndFindNamed(boolean includeMatchingFields) throws IOException, StorageException {
		ITEM item0 = getItemToStore(0);
		ITEM item1 = getItemToStore(1);
		if (item0 == null)	// Test is not required since storage does not expose fields.
			return;
		
		/// The test depends on this
		Assert.assertTrue(item1.equals(getItemToStore(1)));
		Assert.assertTrue(!item1.equals(getItemToStore(0)));
		
		INamedItemStorage<ITEM> s = getStorage();
		s.connect();
		s.clear();
		Assert.assertTrue(s.count() == 0);
		// SensorProfile sp = new SensorProfile("myname", "mytype");
		int index= 0;
		ITEM sp = item0; 
		String name = getName(index);
		String id = s.addNamed(name, sp); 
		Assert.assertTrue(id != null);
		Assert.assertTrue(s.count() == 1);

		index++;
		sp = item1; 
		name = getName(index);
		id = s.addNamed(name, sp);
		// Make sure both are there 

		// Make sure both are there 
		Assert.assertTrue(s.count() == 2);
		Iterable<String> ids = s.getIDs();
		Assert.assertTrue(ids != null);
		Iterator<String> idIter = ids.iterator();
		Assert.assertTrue(idIter != null);
		Assert.assertTrue(idIter.hasNext());
		Assert.assertTrue(idIter.hasNext());

		// Look for a real item.
		Iterable<ITEM> iterable;
		String name1 = getName(1);
		if (includeMatchingFields) {
			FieldValues matches = getMatchingFields(1);
			iterable = s.findNamedItems(name1, matches);
		} else {
			iterable = s.findNamedItems(name1);
		}
		Iterator<ITEM> iter = iterable.iterator();
		Assert.assertTrue(iter.hasNext());
		lookupAndValidateNamedItem(s, sp, 1);

		// Look for a non-existent item
		index++;	// non-existent item
		iterable = s.findNamedItems(getName(index));
		iter = iterable.iterator();
		Assert.assertTrue(!iter.hasNext());

		s.clear();

		Assert.assertTrue(s.count() == 0);

	}

	/**
	 * Make sure updating an item preservers the name of the item.
	 * @throws IOException
	 * @throws StorageException
	 */
	@Test
	public void testUpdateNamedItem() throws IOException, StorageException {
		Assert.assertTrue(this.getItemToStore(0).equals(this.getItemToStore(0))); 	// Test depends on this.
		Assert.assertTrue(!this.getItemToStore(1).equals(this.getItemToStore(0))); 	// Test depends on this.
		
		INamedItemStorage<ITEM> s = getStorage(); 
		s.connect();
		s.clear();
		Assert.assertTrue(s.count() == 0);
	
		List<String> ids = new ArrayList<String>();
		List<ITEM> items = new ArrayList<ITEM>();
		int count = 5;
		List<String> names = new ArrayList<String>();
		for (int i=0 ; i<count ; i++) {
			ITEM item = getItemToStore(i); 
			String name = "name" + i + ".wav";
			String id =  s.addNamed(name, item);
			Assert.assertTrue(id != null);
			Assert.assertTrue(s.count() == i+1);
			names.add(name);
			ids.add(id);
			items.add(item);
		}

		for (int i=0 ; i<count ; i++) {
			ITEM item = getItemToStore(count + i); 
			ITEM oldItem = items.get(i);
			Assert.assertTrue(!item.equals(oldItem)); 	// Test depends on this.
			String id = ids.get(i);
			s.update(id, item);
			Assert.assertTrue(s.count() == count);
			String name = names.get(i);
			List<ITEM> itemList = SoundTestUtils.iterable2List(s.findNamedItems(name));
			Assert.assertTrue(itemList != null && itemList.size() == 1);
			ITEM updatedItem = itemList.get(0);
			Assert.assertTrue(updatedItem != null);
			Assert.assertTrue(updatedItem.equals(item));
			Assert.assertTrue(!updatedItem.equals(oldItem));
		}
			
		s.clear();
		s.disconnect();
		
	}
}
