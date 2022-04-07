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

import org.junit.Assert;
import org.junit.Test;

public class ItemReferenceIteratorTest {



	@Test
	public void testSkip() {
		List<Integer> refs = new ArrayList<Integer>();
		for (int i=0 ; i<10 ; i++) {
			refs.add(i);
		}
//		GenericTestShuffleIterable<Integer> iterable = new GenericTestShuffleIterable<Integer>(refs,refMap); 
		ShufflizingIterable<Integer> iterable = new ShufflizingIterable<Integer>(refs); 
		
		for (int i=0 ; i<2 ; i++) {
			ItemReferenceIterator<Integer> iter = iterable.iterator();
			Assert.assertTrue(iter.hasNext());
			Assert.assertTrue(iter.next() == 0);
			iter.skipNext();	// skip 
			Assert.assertTrue(iter.next() == 2);
			Assert.assertTrue(iter.next() == 3);
			iter.skipNext();	// skip 
			Assert.assertTrue(iter.next() == 5);
		}

	}
}
