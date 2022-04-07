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

import java.util.Iterator;

import org.eng.util.ItemReferenceIterator;

/**
 * An iterator over items referenced in an IItemStorage instance.
 * 
 * Extends the super class to define the item references as ids of items in a specified IItemStorage instance.
 *
 * @author dawood
 * @param <ITEM>
 */
class StoredItemByIDIterator<ITEM> extends ItemReferenceIterator<ITEM> implements Iterator<ITEM> {

	/**
	 * Create the instance to iterator over the items with the given ids in the given storage instance.
	 * @param storage
	 * @param ids
	 */
	public StoredItemByIDIterator(StoredItemByIDIterable<ITEM> iterable, Iterator<String> ids,int batchSize) {
		super(iterable, ids,batchSize);
	}




}
