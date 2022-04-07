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
/**
 * 
 */
package org.eng.util;

/**
 * @author dawood
 *
 */
public interface IShuffleIterable<T> extends IItemReferenceIterable<T> {
	
	/**
	 * Returns a new instance of the items in this iterable that are shuffled randomly relative to the original instance.
	 * Two shuffles on the same instance should not give the same order of items. 
	 * Does NOT change this instance.
	 * @return new iterable with a different ordering than previous calls to this method on this instance.
	 */
	public IShuffleIterable<T> shuffle();

	/**
	 * Returns a new instance of the items in this iterable that are shuffled randomly relative to the original instance.
	 * Two shuffles with the same seed on the same instance <b>must</b> give the same order of items.
	 * Does NOT change this instance.
	 * @return
	 */
	public IShuffleIterable<T> shuffle(long seed); 

	/**
	 * Create a new instance of the concrete class that will use the given references as the source of items produced.
	 * These references are considered the source for all items produced.
	 * If the implementation does any filtering/mutation of the referenced items it is based on these references and must
	 * be applied to this set of references.
	 * Often called during {@link #shuffle()} and {@link #shuffle(long)} with the shuffled references returned by {@link #getReferences()}.
	 * @param references the list of references to serve as a <i>source</i> for the iterable returned by 
	 * {@link #getReferences()} of this instance and which ultimately produces the items from the iterable.  
	 * Implementations are free to sub-select from the given list of references, but if they do then this sub-selection must
	 * be returned by the implementation's {@link #getReferences()} method.
	 * @return never null.
	 */
	public IShuffleIterable<T> newIterable(Iterable<String> references);
	
}
