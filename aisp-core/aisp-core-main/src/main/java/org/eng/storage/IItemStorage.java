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

public interface IItemStorage<ITEM> extends  IDeleteableItemStorage<ITEM> {

	/**
	 * Store the given item in storage and return an associated identifier
	 * @param item 
	 * @return never null.  A unique identifier that is considered opaque to the caller.
	 * @throws StorageException
	 */
	String add(ITEM item) throws StorageException;

	/**
	 * Updates the item with the given id.
	 * @param id identifier of previously stored item.
	 * @param item new value for item at the given id.
	 * @return false if the item with id was not found, otherwise true and item was updated.
	 * @throws StorageException if the id is invalid.
	 */
	public boolean update(String id, ITEM item) throws StorageException;

}
