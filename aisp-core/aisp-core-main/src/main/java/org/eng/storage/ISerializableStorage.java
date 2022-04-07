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
import java.io.Serializable;

/**
 * Captures methods used for storage of a serializable Java type with associated metadata on which searches can be performed.
 * An instance must first be connected using {@link #connect()} and should be disconnected using {@link #disconnect()} when no longer needed.
 * A unique and opaque String id (think database key) is associated with a stored metadata.
 * Serialized items are stored along with an instance of METADATA that can contain data to index the serializable item on,
 * thereby allowing for searches on the serializable via those metadata.
 * @author dawood
 * @param <SITEM> the item to be serialized.
 * @param <METADATA> meta data that can be used to search for the serialized item.
 */
public interface ISerializableStorage<SITEM extends Serializable, METADATA> extends IItemStorage<SITEM> {

	/**
	 * Get the metadata associated with the serialized item stored under the given id.
	 * @param id
	 * @return null if not found.
	 * @throws StorageException
	 * @throws IOException 
	 */
	public METADATA getMetaData(String id) throws StorageException, IOException;

	/**
	 * Get the metadata for all the serialized item stored under the given ids.
	 * @param ids
	 * @return an iterable of metadata items containing the same number of items as the input id list.
	 * @throws StorageException if not all items could be retrieved. 
	 * Note that an exception such as NoSuchElementException
	 * may be thrown while iterating the return iterable if items could not be retrieved.
	 * @throws IOException 
	 */
	public Iterable<METADATA> getMetaData(Iterable<String> ids) throws StorageException, IOException;
	

	
//	/**
//	 * Get an iterable that goes through all metadatas in the db.
//	 * @return never null.
//	 */
//	public Iterable<ITEM> items() throws StorageException;


}
