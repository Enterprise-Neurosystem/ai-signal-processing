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
/**
 * 
 */
package org.eng.storage;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides simple implementations of methods that can be implemented using sub-class implementations.
 * @author dawood
 */
public abstract class AbstractDeleteableItemStorage<ITEM> extends AbstractReadOnlyStorage<ITEM> implements IDeleteableItemStorage<ITEM> {


	/**
	 * A non-optimal implementation that calls {@link #delete(Iterable)} on all ids returned by {@link #getIDs()}.
	 */
	@Override
	public void clear() throws StorageException {
		List<String> idList = new ArrayList<String>();
		for (String id : getIDs()) {
			idList.add(id);
		}
		delete(idList);
	}


	/**
	 * A convenience over {@link #delete(Iterable)}.
	 * @param id
	 * @return
	 * @throws StorageException
	 */
	@Override
	public boolean delete(String id) throws StorageException {
		List<String> idList = new ArrayList<String>();
		idList.add(id);
		return delete(idList);
	}

}
