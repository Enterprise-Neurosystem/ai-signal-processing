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
 * Stores items that have names and allows search on those names and the item fields via the super class.
 * @author dawood
 *
 * @param <ITEM>
 */
public interface INamedItemStorage<ITEM> extends IFieldedItemStorage<ITEM> {

	/**
	 * Add the given item to the storage and assign it the given name.
	 * @param name name for item.  Need not be unique.  May be null, in which case a default empty name is assigned.
	 * @param item
	 * @return never null. id of item in storage.
	 * @throws StorageException 
	 */
	public String addNamed(String name, ITEM item) throws StorageException;
	
	/**
	 * Get the id(s) of item(s) stored with the given name and having the optional field values.
	 * @param name 
	 * @return never null, but perhaps an empty iterable.
	 * @throws StorageException 
	 */
	public Iterable<String> findNamedIDs(String name) throws StorageException;
	
	/**
	 * Get all items with the given name.
	 * @param name
	 * @return never null.
	 */
	public Iterable<ITEM> findNamedItems(String name) throws StorageException;

	/**
	 * Get the name of an item stored in this instance.
	 * @param id
	 * @return null if item was not assigned a name or does not exist in storage.
	 * @throws StorageException  
	 */
	public String getItemName(String id) throws StorageException;

	/**
	 * Get the id(s) of item(s) stored with the given name and having the optional field values.
	 * @param name  the name assigned to the items stored.
	 * @param itemFieldValues optional field values, may be null. 
	 * @return never null, but perhaps an empty iterable.
	 * @throws StorageException 
	 */
	public Iterable<String> findNamedIDs(String name, FieldValues itemFieldValues) throws StorageException;

	/**
	 * Get all items with the given name.
	 * @param name  the name assigned to the items stored.
	 * @param itemFieldValues optional field values, may be null. 
	 * @return never null.
	 */
	public Iterable<ITEM> findNamedItems(String name, FieldValues itemFieldValues) throws StorageException;

}
