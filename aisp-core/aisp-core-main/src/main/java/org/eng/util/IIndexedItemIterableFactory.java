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
package org.eng.util;

import org.eng.ENGException;
import org.eng.storage.StorageException;

/**
 * Defines methods to get an iterable of items given a set of ids/indexes.
 * The ids are mappable to 1 or more ITEMs, for example, a storage implementation.
 * @author DavidWood
 *
 */
public interface IIndexedItemIterableFactory<ITEM> {

	/**
	 *  Get an iterable for the sounds associated with the given ids.  
	 *  Sounds should be produced in the order of the ids in the iterable.
	 *  Ideally, the iterable returned is an IShuffleIterable<SoundRecording>.
	 *  @param indexes the pointers to items, each index may produce 0, 1 or n items.
	 * @throws StorageException 
	 */
	Iterable<ITEM> iterable(Iterable<String> indexes) throws ENGException;
}
