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

import java.io.Serializable;

/**
 * Extends the ISerializableStorage to enable easy assigning of a name to an item in storage.
 * Names need not be  unique.
 * @author dawood
 *
 * @param <SITEM>
 * @param <METADATA>
 */
public interface INamedSerializableStorage<SITEM extends Serializable, METADATA> 
    extends INamedItemStorage<SITEM>, ISerializableStorage<SITEM, METADATA> {

//	/**
//	 * Add the item and metadata and assign the given name to the item for later lookups.
//	 * @param name name to assign to the stored item.  Names need not be unique.
//	 * @param item 
//	 * @return 
//	 * @throws StorageException 
//	 */
//	public String add(String name, SITEM item) throws StorageException;
}
