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

import java.util.List;

public interface IItemReferenceIterable<T> extends Iterable<T>  {

	/**
	 * Get the references that are used to iterate through the items.
	 * Implementations are free to skip references and/or produce 1 or more items for each reference.
	 * The Strings returned must be dereferencable through the {@link #dereference(List)} and {@link #dereference(String)} methods.
	 * @return an iterable that produces references in an order used to produce the items that are available from the iterator.
	 */
	Iterable<String> getReferences();
	
	/**
	 * Get the item(s) at the underlying reference.
	 * @param reference a reference string, presumably one that is a member of the iterable returned by {@link #getReferences()}.
	 * @return null is supported but may trigger a NoSuchElementException elsewhere, such as {@link #dereference(List)}.
	 * @throw NoSuchElementException if any references could not be loaded.
	 */
	T dereference(String reference);
	


	/**
	 * Get the item(s) at the underlying reference.
	 * Implementations may choose to loop on {@link #dereference(String)} or in some cases it may be more efficient to dereference in bulk.
	 * @param reference a reference string, presumably one that is a member of the iterable returned by {@link #getReferences()}.
	 * @return a list of dereferenced items in 1:N correspondence with the input references. 
	 * @throw NoSuchElementException if any references could not be loaded.
	 */
	List<T> dereference(List<String> references);

}
