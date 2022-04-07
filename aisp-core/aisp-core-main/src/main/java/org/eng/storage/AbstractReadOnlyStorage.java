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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractReadOnlyStorage<ITEM> implements IReadOnlyStorage<ITEM> {

	public AbstractReadOnlyStorage() {
		super();
	}

	/**
	 * A non-optimal implementation that counts all the ids returned by {@link #getIDs()}.
	 */
	@Override
	public long count() throws StorageException {
		int count = 0;
		for (String id : getIDs())
			count++;
		return count;
	}

	/**
	 * Uses the ids from #getIDs() to create an iterable that dereferences the ids.
	 */
	@Override
	public Iterable<ITEM> items() throws StorageException {
		Iterable<String> ids = getIDs();
		return findItems(ids);
	}
	
	/**
	 * Non-optimal implementation that uses StoredItemByIDIterable with the given ids.
	 */
	@Override
	public Iterable<ITEM> findItems(Iterable<String> ids) throws StorageException {
		// Must use a batch-size of 1 here, otherwise if we're being called through another
		// StoredItemByIDIterable that uses batchSize>1, we get infinite recursion.
		// To get true batched storage reads, sub-classes must implement findItems()
//		return new StoredItemByIDIterable<ITEM>(this,ids,1);
		return StorageUtil.batchLoad(this, ids, null);
	}

	/**
	 * A helper method to allow subclasses to cheat when implementing paged ids.
	 * @param pageIndex
	 * @param pageSize
	 * @param ids
	 * @return never null.
	 * @throws StorageException
	 */
	protected static List<String> getPagedIDs(int pageIndex, int pageSize, Iterable<String> ids) throws StorageException {
		int firstIndex = pageIndex * pageSize;
		List<String> page = new ArrayList<String>();
		Iterator<String> allIDs	= ids.iterator();	

		// Skip over the earlier pages.
		for (int i=0;i<firstIndex ; i++) {
			if (!allIDs.hasNext())
				return page;
			allIDs.next();
		}

		// Slurp up the items from the desired page.
		for (int i=0;i<pageSize; i++) {
			if (!allIDs.hasNext())
				return page;
			String id = allIDs.next();
			page.add(id);
		}
		
		return page;
	}	

}
