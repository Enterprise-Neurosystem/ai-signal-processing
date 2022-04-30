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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eng.ENGException;
import org.eng.aisp.SoundTestUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public abstract class AbstractShuffleIterableTest {

	private final int testedItemCount;
	private final int testedRepetableItemCount;
	protected AbstractShuffleIterableTest(int testedItemCount, int testedRepeatableItemCount) {
		this.testedItemCount = testedItemCount;
		this.testedRepetableItemCount = testedRepeatableItemCount;
	}

	
	protected AbstractShuffleIterableTest() {
		this(12,
			24		// A large multiple of 6 for some tests.
			);
	}

	/**
	 * Return an iterable test test, that iterates over items that implement equals().
	 * @return null if the test implementation does not support repeatable shuffle.
	 * @throws ENGException 
	 */
	protected abstract IShuffleIterable<?> getRepeatableShuffleIterable(int returnedItemCount) throws ENGException;

	/**
	 * Get an implementation that need not support repeatability of shuffling.
	 * Default implementation is to try to use the value returned by {@link #getRepeatableShuffleIterable()}, but
	 * if that returns null, as is allowed, then an exception is thrown.
	 * @return never null.
	 * @throws ENGException
	 */
	protected IShuffleIterable<?> getShuffleIterable(int returnedItemCount) throws ENGException {
		IShuffleIterable<?> si = getRepeatableShuffleIterable(returnedItemCount);
		if (si == null)
			throw new RuntimeException("Sub-class must override this method to produce.");
		return si;
	}

	
	@Test
	public void testNewIterable() throws ENGException {
		IShuffleIterable<?> si1 = getShuffleIterable(this.testedItemCount);
		Iterable<String> refs1 = SoundTestUtils.iterable2List(si1.getReferences());
		IShuffleIterable<?> si2 = si1.newIterable(refs1);
		Iterable<String> refs2 = SoundTestUtils.iterable2List(si2.getReferences());
		
		// Make sure the items return are the same
		Iterator<?> iter2 = si2.iterator();
		for (Object item1 : si1) {
			Assert.assertTrue(iter2.hasNext());
			Object item2 = iter2.next();
			Assert.assertTrue(item1.equals(item2));
		}
	}
	
	/**
	 * Allow sub-class tests to indicate whether or not the IShuffleIterable under test produces the same set of items
	 * after each shuffle() call.  In cases, where the iterable does filtering, this may not be the case.  Originally
	 * motivated by BalancedLabeledWindowShuffleIterable which does filtering.
	 * @return
	 */
	protected boolean isIterableRepeatable() {
		return true;
	}

	/**
	 * Make sure the same items are returned from the iterable after each shuffle.
	 * @throws ENGException 
	 */
	@Test
	public void testShuffle() throws ENGException {
		int count = this.testedRepetableItemCount; 
		IShuffleIterable<?> si1 = getRepeatableShuffleIterable(count);
		Assume.assumeNotNull(si1);
		shuffleTestHelper(si1, count);
	}

	/**
	 * @param si1
	 */
	protected void shuffleTestHelper(IShuffleIterable<?> si1, int expectedItemCount) {
//		validateShuffleEquivalence(si1, si1,true);		
		// Make sure test data is correct.
		Assert.assertTrue(SoundTestUtils.iterable2List(si1).size() == expectedItemCount);

		// Make sure the # of references matches the number of items.
		List<String> references = SoundTestUtils.iterable2List(si1.getReferences());
		int refCount = references.size();
		Assert.assertTrue("Reference count is " + refCount + " but expected " + expectedItemCount, refCount == expectedItemCount);
		
		IShuffleIterable<?> si2 = si1.shuffle(2); 
		// Test equality on 1st shuffle 
		validateShuffleEquivalence(si1, si2,false, expectedItemCount);		// First shuffle

		// Test equality on 2nd shuffle()
		IShuffleIterable<?> si3 = si1.shuffle(3); 
		validateShuffleEquivalence(si1, si3,false, expectedItemCount);		// Second shuffle

		// Test commutativity 
		validateShuffleEquivalence(si2, si3,false, expectedItemCount);	
		
		// Shuffle again after iteration and make sure they are still he same items.
		IShuffleIterable<?> si4 = si1.shuffle(4); 
		validateShuffleEquivalence(si1, si4,false, expectedItemCount);

		// Shuffle with the same seed and expect the same order.
		IShuffleIterable<?> si5 = si1.shuffle(2); 
		validateShuffleEquivalence(si2, si5,true, expectedItemCount);
		
		// Test a shuffle of a shuffle
		IShuffleIterable<?> si6 = si2.shuffle(); 
		validateShuffleEquivalence(si1, si6,false, expectedItemCount);
		
	}

	/**
	 * @param si1
	 * @param si2
	 * @param expectedItemCount 
	 */
	protected void validateShuffleEquivalence(IShuffleIterable<?> si1, IShuffleIterable<?> si2, boolean expectSameOrder, int expectedItemCount) {
		// Make sure the items return are the same, but not in the same order
		Set<Object> set1 = new HashSet<Object>();
		Set<Object> set2 = new HashSet<Object>();
		List<Object> list1 = new ArrayList<Object>();
		List<Object> list2 = new ArrayList<Object>();
		Iterator<?> iter2 = si2.iterator();
		int count = 0;
		for (Object item1 : si1) {
			Assert.assertTrue(iter2.hasNext());
			Object item2 = iter2.next();
			set1.add(item1);
			set2.add(item2);
			list1.add(item1);
			list2.add(item2);
			count++;
		}
		Assert.assertTrue(count == expectedItemCount);
		Assert.assertTrue(set1.size() == set2.size());
		if (this.isIterableRepeatable()) {
			Assert.assertTrue("Mis-configured test.  Iterators did not produce any items", expectedItemCount != 0);
			Assert.assertTrue(set1.equals(set2));
			if (expectSameOrder)
				Assert.assertTrue(list1.equals(list2));
			else
				Assert.assertTrue(!list1.equals(list2));
		}
	}

}
