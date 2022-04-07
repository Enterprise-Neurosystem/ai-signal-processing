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
 * Extends the super interface to define methods allowing search for items by item field values. 
 * @author dawood
 *
 * @param <ITEM>
 */
public interface IFieldedItemStorage<ITEM> extends IItemStorage<ITEM> {

	/**
	 * Get the items with fields having values matching those in the provided map.
	 * @param itemFieldValues map of field names to field values.  Nested fields can be specified 
	 * with a '.', as in <code>foo.bar</code> which specifies the field <code>bar</code> of the <code>foo</code>
	 * field within the stored item.
	 * @return never null.
	 * @throws StorageException
	 */
	public Iterable<ITEM> findItems(FieldValues itemFieldValues) throws StorageException;



	/**
	 * Get the ids of items with fields having values matching those in the provided map.
	 * @param itemFieldValues map of field names to field values.  Nested fields can be specified 
	 * with a '.', as in <code>foo.bar</code> which specifies the <code>bar</code> of the <code>foo</code>
	 * field within the stored item.  If null or empty, then return all ids.
	 * @return never null.
	 * @throws StorageException
	 */
	public Iterable<String> findIDs(FieldValues itemFieldValues) throws StorageException;

	/**
	 * Get a page of item identifiers matching the given item values.. 
	 * @param pageIndex 0-based index specifying the page of ids to return.
	 * @param pageSize maximum number of ids to return in a page.  If 0, then return all ids.
	 * @param itemFieldValues map of field names to field values.  Nested fields can be specified 
	 * with a '.', as in <code>foo.bar</code> which specifies the <code>bar</code> of the <code>foo</code>
	 * field within the stored item.  If null or empty, then all items match the request.  
	 * @return never null.
	 * @throws StorageException
	 */
	public Iterable<String> findPagedIDs(int pageIndex, int pageSize, FieldValues itemFieldValues) throws StorageException;
}
