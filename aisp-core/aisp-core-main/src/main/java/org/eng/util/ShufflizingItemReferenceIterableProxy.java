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
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ShufflizingItemReferenceIterableProxy<T> extends ItemReferenceIterableProxy<T> implements ISizedShuffleIterable<T> {

	public ShufflizingItemReferenceIterableProxy(IShuffleIterable<T> iterable) {
		this(iterable, iterable.getReferences());
	}

	public ShufflizingItemReferenceIterableProxy(IShuffleIterable<T> iterable, Iterable<String> references) {
		super(iterable, references);
	}

	@Override
	public ISizedShuffleIterable<T> shuffle() {
		return this.shuffle(4300121);
	}

	@Override
	public ISizedShuffleIterable<T> shuffle(long seed) {
		Iterable<String> refs = this.getReferences();
		List<String> refList = new ArrayList<String>();
		for (String ref : refs)
			refList.add(ref);
		Collections.shuffle(refList,new Random(seed));
		return newIterable(refList);
	}

	@Override
	public ISizedShuffleIterable<T> newIterable(Iterable<String> references) {
		return new ShufflizingItemReferenceIterableProxy<T>((IShuffleIterable<T>)this.iterable, references);
	}

}
