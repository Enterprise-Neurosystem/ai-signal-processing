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

import java.util.ArrayList;
import java.util.List;

public class CollectionUtils {

	/**
	 * Generate a list of the given iterable.  If the iterable is already a list, cast and return. 
	 * @param iterable
	 * @return null if the given iterable is null.
	 */
	public static <T> List<T> asList(Iterable<T> iterable) {
		if (iterable == null)
			return null;
		if (iterable instanceof List)
			return (List<T>)iterable;
		List<T> list = new ArrayList<T>();
		for (T t : iterable) 
			list.add(t);
		return list;
	}
}
