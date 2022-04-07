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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * @author DavidWood
 *
 */
public class ShuffleIterableUtil {


	static void compareIterables(Iterator<?> unfilteredIterator, Iterator<?> filteredIterator) {
		List<Object> unfiltered = new ArrayList<Object>();
		List<Object> filtered = new ArrayList<Object>();
		while (unfilteredIterator.hasNext())
			unfiltered.add(unfilteredIterator.next());
		while (filteredIterator.hasNext())
			filtered.add(filteredIterator.next());
		
		int filteredIndex = 0;
		for (Object obj : filtered) {
			int index = unfiltered.indexOf(obj);
			if (index < 0) {
				System.out.println("No match for filtered item with index " + filteredIndex);
			} else  {
				System.out.println("Match unfiltered index " + index + " with filtered index " + filteredIndex);
			}
			filteredIndex++;
		}
		
	}

	static void showIterable(String name, Iterator<?> iterator) {
//		if (iterable instanceof BoundedLabeledWindowShuffleIterable) {
//			iterable = ((BoundedLabeledWindowShuffleIterable)iterable).iterable;
//		}
		System.out.println("BEGIN iterator( " + iterator.getClass().getSimpleName() + "): " + name);
		int count=0;
		while (iterator.hasNext()) {
			Object obj = iterator.next();
//		for (Object obj : iterable)  { 
			System.out.println(obj);
			count++;
			iterator.hasNext();
		}
		System.out.println("END iterator: " + name + ", items=" + count);
	}

	public static List<String> shuffleReferences(long seed, Iterable<String> refs) {
		List<String> refList = new ArrayList<String>();
		for (String ref : refs)
			refList.add(ref);
		Collections.shuffle(refList, new Random(seed));
		return refList;
	}

	public static int size(Iterable<String> refs) {
		if (refs instanceof Collection)
			return ((Collection)refs).size();
		int count = 0;
		for ( String r : refs)  {
			count++;
		}
		return count;
	}
}