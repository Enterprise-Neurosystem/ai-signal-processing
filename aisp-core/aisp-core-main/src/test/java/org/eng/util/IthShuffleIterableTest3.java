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

import java.util.Collections;
import java.util.List;

import org.eng.aisp.SoundTestUtils;

public class IthShuffleIterableTest3 extends IthIterableTest {
	
	public IthShuffleIterableTest3() {
		super(false);
	}
	
	/**
	 * Get the iterable to test for Ith'ness.
	 * @param <ITEM>
	 * @param iterable
	 * @param setSize
	 * @param setIndex
	 * @param excludeIndex
	 * @param maxItemCount
	 * @param firstN
	 * @return
	 */
	protected <ITEM> Iterable<ITEM> getIthIterable(Iterable<ITEM> iterable, int setSize, int setIndex, boolean excludeIndex, int maxItemCount, int firstN) {
		IShuffleIterable<ITEM> si = new ShufflizingIterable<ITEM>(iterable);
//		si = si.shuffle();
		IShuffleIterable<ITEM> isi = new IthShuffleIterable<ITEM>(si, setSize, setIndex, excludeIndex, maxItemCount, firstN);
		isi = isi.shuffle();
		return isi;
	}

	/**
	 * Get an iterable that returns all items up to the given max.
	 * @param <ITEM>
	 * @param iterable
	 * @param maxItemCount
	 * @return
	 */
	protected <ITEM> Iterable<ITEM> getIthIterable(Iterable<ITEM> iterable, int maxItemCount) {
		IShuffleIterable<ITEM> si = new ShufflizingIterable<ITEM>(iterable);
		si = new IthShuffleIterable<ITEM>(si, maxItemCount);
		si = si.shuffle();
		return si;
	}
	
	/**
	 * Override to sort the items in the test iterable and then call super class with the sorted items.
	 */
	protected void validateIterable(Iterable<Integer> ith, List<Integer> expectedData) {
		List<Integer> ithInts = SoundTestUtils.iterable2List(ith);
		Collections.sort(ithInts);
		Collections.sort(expectedData);
		super.validateIterable(ithInts, expectedData);
	}
}
