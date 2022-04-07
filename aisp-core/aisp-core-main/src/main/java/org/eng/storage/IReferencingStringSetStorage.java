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
 * Models a storage instance that stores items and a set of ids referencing those items. 
 * The primary item stored is the set of ids with additional methods for adding/deleting items from the sets
 * and storing them (both the set of ids and the items) persistently.
 * <p>
 * Special care must be used when implementing the super class methods to delete id sets in that
 * the items referenced by the set must also be deleted.
 * <p>
 * Note that we extend IDeleteableItemStorage and not IItemStorage as the latter adds add() and update()
 * methods which would allow callers to place item references in more than one set (w/o some serious db scanning
 * to assure non-duplication).
 * 
 * @author DavidWood
 *
 * @param <ITEM>
 */
public interface IReferencingStringSetStorage<SETITEM extends NamedStringSet, ITEM> extends IDeleteableItemStorage<SETITEM> {

	/**
	 * Creates an empty set with the given name.
	 * @param name not null name.  It need not be unique.
	 * @return an opaque key that can be used to delete, update and retrieve the set.
	 * @throws StorageException  if could not be added.
	 */
	public String createEmptySet(String name) throws StorageException;
	
	/**
	 * Change the name of the set
	 * @param setID
	 * @param name
	 * @return 
	 * @throws StorageException if set with given id does not exist.
 	 */
	public boolean setName(String setID, String name) throws StorageException;
	
	/**
	 * Add the given item to the set and store both persistently.
	 * @param setID the id of the set to which the item is to be added.  
	 * @param itemName the name to assign to the item in the set. 
	 * @param item item to be stored and for which to generate an id which will be stored in the set.
	 * @return the id of the item stored.
	 * @throw StorageException if the set with the given id is not present or the item could not be stored and updated.
	 */
	public String addSetItem(String setID, String itemName, ITEM item) throws StorageException;

	/**
	 * Remove the item from the set.  Update the stored set and remove the item associated with the given item id from persistent storage so that no
	 * item can be retrieved with the given id.. 
	 * @param setID the identifier for a previously created set.
	 * @param itemID the id of an item within the set.
	 * @return true if item was removed
	 * @throw StorageException if the set with the given id is not present.
	 */
	public boolean deleteSetItems(String setID, Iterable<String> itemIDs) throws StorageException;
	
	/**
	 * Generally a convenience on {@link #deleteSetItems(String, Iterable)}.
	 */
	public boolean deleteSetItem(String setID, String itemID) throws StorageException;
	
	/**
	 * Find the referenced item with the given identifier.
	 * @param id	 an id that is currently listed in a set contained here.
	 * @return null if item with given id does not exist in storage.
	 * @throws StorageException if the id is invalid.
	 */
	public ITEM findSetItem(String id) throws StorageException;

	/**
	 * Find the referenced items. 
	 * @param ids ids that are currently listed in a set contained here.
	 * @return a iterable of items containing the same number of items as the input id list.
	 * @throws StorageException if not all items could be retrieved. 
	 * Note that an exception such as NoSuchElementException
	 * may be thrown while iterating the return iterable if items could not be retrieved.
	 */
	public Iterable<ITEM> findSetItems(Iterable<String> ids) throws StorageException;
	
	
	
}
