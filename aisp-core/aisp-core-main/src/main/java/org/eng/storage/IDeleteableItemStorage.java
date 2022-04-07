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

public interface IDeleteableItemStorage<ITEM> extends IReadOnlyStorage<ITEM> {

	/**
	 * Remove all items from storage.  
	 * Generally used only during testing.  
	 * Please use with care.
	 * @throws StorageException
	 */
	void clear() throws StorageException;

	/**
	 * Delete the items with the given identifiers.
	 * @param ids identifiers of previously stored items.
	 * @return true if no items with the given ids remaining the db.
	 * @throws StorageException
	 */
	boolean delete(Iterable<String> ids) throws StorageException;

	/**
	 * Delete the item with the given identifier.
	 * @param id identifier of a previously stored item.
	 * @return true if no item with the given id remains in storage on return.  false if could not be deleted. 
	 * @throws StorageException if a bad id given, 
	 */
	boolean delete(String id) throws StorageException;

}