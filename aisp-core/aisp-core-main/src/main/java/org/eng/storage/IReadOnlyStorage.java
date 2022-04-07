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

/**
 * Defines the operations on a storage instance that are independent of the items being stored.
 * An instance must first be connected using {@link #connect()} and should be disconnected using {@link #disconnect()} when no longer needed.
 * A unique and opaque String id (think database key) is associated with a stored item.
 * Searches on items may be done by providing a map of field values to match in the stored items.
 * @author dawood
 *
 */
public interface IReadOnlyStorage<ITEM> {

	/**
	 * Called to connect to the storage.
	 * Must be called before otherwise using the instance.
	 * @throws StorageException
	 */
	void connect() throws StorageException;

	/**
	 * Called when the instance is no longer needed.
	 * {@link #connect()} may be called after this to reestablish the connection to storage.
	 */
	void disconnect();
	
	
	/**
	 * Determine if the instance is connected to the DB.
	 * If it is not, most operations will likely fail.
	 * @return
	 */
	boolean isConnected();

	/**
	 * Get all ids of items in the repository in LIFO order.
	 * @return never null.
	 * @throws StorageException
	 */
	Iterable<String> getIDs() throws StorageException;

	/**
	 * Return the number of items in storage.
	 * @return 0 or larger.
	 * @throws StorageException
	 */
	long count() throws StorageException;

	/**
	 * Find the item with the given identifier.
	 * @param id
	 * @return null if item with given id does not exist in storage.
	 * @throws StorageException if the id is invalid.
	 */
	public ITEM findItem(String id) throws StorageException;

	/**
	 * Find the items with the given identifiers.
	 * @param ids
	 * @return a iterable of items containing the same number of items as the input id list.
	 * @throws StorageException if not all items could be retrieved. 
	 * Note that an exception such as NoSuchElementException
	 * may be thrown while iterating the return iterable if items could not be retrieved.
	 */
	public Iterable<ITEM> findItems(Iterable<String> ids) throws StorageException;
	
	/**
	 * Get an iterable that goes through all items in the db.
	 * @return never null.
	 */
	Iterable<ITEM> items() throws StorageException;

;

}
