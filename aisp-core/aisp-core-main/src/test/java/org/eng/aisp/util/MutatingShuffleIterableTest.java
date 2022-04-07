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
package org.eng.aisp.util;

import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.SoundTestUtils;
import org.eng.util.IMutator;
import org.eng.util.IShuffleIterable;
import org.eng.util.ItemReferenceIteratorTest;
import org.eng.util.MutatingShuffleIterable;
import org.eng.util.ShufflizingIterable;
import org.eng.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;

public class MutatingShuffleIterableTest extends ItemReferenceIteratorTest {

	public MutatingShuffleIterableTest() {
	}

	
	private class OneToOneMutator implements IMutator<Integer,Integer> {

		@Override
		public List<Integer> mutate(Integer item) {
			List<Integer> ints = new ArrayList<Integer>();
			ints.add(item);
			return ints;
		}
		
	} 
	
	@Test
	public void testOneToOneMutator() {
		int count = 40;
		List<Integer> intList = new ArrayList<Integer>();
		for (int i=0 ; i<count ; i++)
			intList.add(i);
		ShufflizingIterable<Integer> intIter = new ShufflizingIterable<Integer>(intList);
		IShuffleIterable<Integer> msi = new MutatingShuffleIterable<Integer,Integer>(intIter, new OneToOneMutator(), true);
		Assert.assertTrue(SoundTestUtils.iterable2List(msi).size() == count);
		List<Integer> ref1 = SoundTestUtils.iterable2List(msi);
		msi = msi.shuffle();
		List<Integer> ref2 = SoundTestUtils.iterable2List(msi);
		Assert.assertTrue(!ref1.equals(ref2));
		Assert.assertTrue(SoundTestUtils.iterable2List(msi).size() == count);
		TestUtil.verifyRepeatable(msi);
	}
	
	private class DoublingMutator implements IMutator<Integer, Integer> {

		@Override
		public List<Integer> mutate(Integer item) {
			List<Integer> ints = new ArrayList<Integer>();
			ints.add(item);
			ints.add(item);
			return ints;
		}
		
	} 
	
	@Test
	public void testDoublingMutator() {
		int count = 40;
		List<Integer> intList = new ArrayList<Integer>();
		for (int i=0 ; i<count ; i++)
			intList.add(i);
		ShufflizingIterable<Integer> intIter = new ShufflizingIterable<Integer>(intList);
		IShuffleIterable<Integer> msi = new MutatingShuffleIterable<Integer, Integer>(intIter, new DoublingMutator(), false);
		Assert.assertTrue(SoundTestUtils.iterable2List(msi).size() == 2*count);
		List<Integer> ref1 = SoundTestUtils.iterable2List(msi);
		msi = msi.shuffle();
		List<Integer> ref2 = SoundTestUtils.iterable2List(msi);
		Assert.assertTrue(!ref1.equals(ref2));;
		Assert.assertTrue(SoundTestUtils.iterable2List(msi).size() == 2* count);
		TestUtil.verifyRepeatable(msi);
	}
	
	private class DropoutMutator implements IMutator<Integer, Integer> {
		@Override
		public List<Integer> mutate(Integer item) {
			List<Integer> ints;
			if ((item & 0x01) == 0) {
				ints = new ArrayList<Integer>();
				ints.add(item);
			} else {
				ints = null;
			}
//			AISPLogger.logger.info("Item " + item + " mutated to " + ints);
			return ints;
		}
		
	} 
	
	@Test
	public void testDropoutMutator() {
		int count = 40;
		List<Integer> intList = new ArrayList<Integer>();
		for (int i=0 ; i<count ; i++)
			intList.add(i);
		ShufflizingIterable<Integer> intIter = new ShufflizingIterable<Integer>(intList);
		IShuffleIterable<Integer> msi = new MutatingShuffleIterable<Integer, Integer>(intIter, new DropoutMutator(), true);
		Assert.assertTrue(SoundTestUtils.iterable2List(msi).size() == count/2);
		List<Integer> ref1 = SoundTestUtils.iterable2List(msi);
		msi = msi.shuffle();
		List<Integer> ref2 = SoundTestUtils.iterable2List(msi);
		Assert.assertTrue(!ref1.equals(ref2));
		Assert.assertTrue(SoundTestUtils.iterable2List(msi).size() == count/2);
		TestUtil.verifyRepeatable(msi);
	}
	
	@Test
	public void testBadMutator() {
		int count = 40;
		List<Integer> intList = new ArrayList<Integer>();
		for (int i=0 ; i<count ; i++)
			intList.add(i);
		ShufflizingIterable<Integer> intIter = new ShufflizingIterable<Integer>(intList);

		IShuffleIterable<Integer> msi; 

		msi = new MutatingShuffleIterable<Integer, Integer>(intIter, new DoublingMutator(), false);	
		verifyIterationException(msi);
	}

	private void verifyIterationException(IShuffleIterable<Integer> msi) {
		try {
			for (Integer i : msi)
				i.notify();
			Assert.fail("Did not get exception iteratring");
		} catch (Exception e) {
			
		}
		
	}
}
