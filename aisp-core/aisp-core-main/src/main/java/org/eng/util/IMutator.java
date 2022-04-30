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

/**
 * Defines the method required by the MutatingIterable.
 * Allows filtering, changing and add of items in the iterable.
 * @author dawood
 *
 * @param <INPUT>
 */
public interface IMutator<INPUT, OUTPUT> {
	/**
	 * Converts the given item into 0 or more altered/mutated items.
	 * The method may filter out items by returning null on a given item,
	 * or it may map the given item into 1 or more items that will 
	 * then be included in the {@link MutatingIterable}.  The mapped items 
	 * can include the original if desired.
	 * @param item never null.
	 * @return null, or list of length 1 or more of items. For a given item, this must return the same result for each call with the item.
	 */
	List<OUTPUT>  mutate(INPUT item);
}