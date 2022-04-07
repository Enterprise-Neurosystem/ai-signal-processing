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
import java.util.Iterator;
import java.util.List;

import org.eng.ENGTestUtils;
import org.eng.aisp.SoundTestUtils;
import org.junit.Assert;
import org.junit.Test;

public class IthIterableTest {

	final boolean testIterablePreservesOrder;
	

	public IthIterableTest() {
		this.testIterablePreservesOrder = true;
	}

	protected IthIterableTest(boolean testIterablePreservesOrder) {
		this.testIterablePreservesOrder = testIterablePreservesOrder;
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
		return new IthIterable<ITEM>(iterable, setSize, setIndex, excludeIndex, maxItemCount, firstN);
	}

	/**
	 * Get an iterable that returns all items up to the given max.
	 * @param <ITEM>
	 * @param iterable
	 * @param maxItemCount
	 * @return
	 */
	protected <ITEM> Iterable<ITEM> getIthIterable(Iterable<ITEM> iterable, int maxItemCount) {
		return new IthIterable<ITEM>(iterable, maxItemCount);
	}

	@Test
	public void testIthIterable() {
		List<Integer> intList = new ArrayList<Integer>();
		final int maxListSize = 30;
		for (int i=0 ; i<maxListSize ; i++)
			intList.add(i);
		
		boolean tf[] = new boolean[] { true, false};
		Iterable<Integer> ith; 
		List<Integer> testData;

		for (boolean exclude : tf) {
			int setIndex = 0;
			int setSize = 0;
			int firstN = 0;
			int maxItemCount = 12;

//			ith = new IthIterable<Integer>(intList, setSize, setIndex, exclude, maxItemCount, firstN);
			ith = getIthIterable(intList, setSize, setIndex, exclude, maxItemCount, firstN);
			testData = makeIntegerList(setSize, setIndex, exclude, maxItemCount, firstN, maxListSize );
			validateIterable(ith, testData);
			validateIterable(ith, testData);

			setSize = 3;
			for (int i=0 ; i<setSize ; i++) {
				setIndex = i;
//				ith = new IthIterable<Integer>(intList, setSize, setIndex, exclude, maxItemCount, firstN);
				ith = getIthIterable(intList, setSize, setIndex, exclude, maxItemCount, firstN);
				testData =    makeIntegerList(setSize, setIndex, exclude, maxItemCount, firstN, maxListSize );
				validateIterable(ith, testData);
				validateIterable(ith, testData);
				
//				ith = new IthIterable<Integer>(intList, setSize, setIndex, exclude, maxItemCount, firstN);
				ith = getIthIterable(intList, setSize, setIndex, exclude, maxItemCount, firstN);
				testData =    makeIntegerList(setSize, setIndex, exclude, maxItemCount, firstN, maxListSize);
				validateIterable(ith, testData);
				validateIterable(ith, testData);
			}
			
			firstN = 5;
			maxItemCount = -1;
			for (int i=0 ; i<setSize ; i++) {
				setIndex = i;
//				ith = new IthIterable<Integer>(intList, setSize, setIndex, exclude, maxItemCount, firstN);
				ith = getIthIterable(intList, setSize, setIndex, exclude, maxItemCount, firstN);
				testData =    makeIntegerList(setSize, setIndex, exclude, maxItemCount, firstN, maxListSize);
				validateIterable(ith, testData);
				validateIterable(ith, testData);
				
//				ith = new IthIterable<Integer>(intList, setSize, setIndex, exclude, maxItemCount, firstN);
				ith = getIthIterable(intList, setSize, setIndex, exclude, maxItemCount, firstN);
				testData =    makeIntegerList(setSize, setIndex, exclude, maxItemCount, firstN, maxListSize);
				validateIterable(ith, testData);
				validateIterable(ith, testData);
			}

			try {
//				new IthIterable<Integer>(intList, setSize, setSize, exclude, maxItemCount, 0);
				ith =     getIthIterable(intList, setSize, setSize, exclude, maxItemCount, 0);
				Assert.fail("Did not get excpetion for bad set index");
			} catch (IllegalArgumentException e) {
				;
			}
		}

	}

	@Test
	public void testIthOfIth() {
		List<Integer> ints = ENGTestUtils.makeList(8);	// 0..7
		int setSize = 4, setIndex = 0 ;

		// Do this to be sure we can count on inclusion working
		Iterable<Integer> iter1 =  getIthIterable(ints, setSize, setIndex, true, ints.size(), 0);
		List<Integer> testData1 =  makeIntegerList(     setSize, setIndex, true, ints.size(), 0, ints.size());
		validateIterable(iter1,testData1);

		// Do this to be sure we can count on exclusion working
		Iterable<Integer> fiter1 =  getIthIterable(ints, setSize, setIndex, false, ints.size(), 0);
		List<Integer> ftestData1 =  makeIntegerList(     setSize, setIndex, false, ints.size(), 0, ints.size());
		validateIterable(fiter1,ftestData1);

		setSize = 2;
		List<Integer> ints1 = SoundTestUtils.iterable2List(testData1);
		Iterable<Integer> iter2 = getIthIterable(iter1, setSize, setIndex, false, ints1.size(), 0);
//		List<Integer> iter3 =  getExpectedItems(         iter1, setSize, setIndex, false, ints1.size(), 0, ints1.size());
		List<Integer> iter3 =  getExpectedItems(         iter1, setSize, setIndex, false, ints1.size(), 0);
		validateIterable(iter2,iter3);

		iter2 =  getIthIterable(iter1, setSize, setIndex, false, ints1.size(), 0);
//		iter3 =  getExpectedItems(      iter1, setSize, setIndex, false, ints1.size(), 0, ints1.size());
		iter3 =  getExpectedItems(      iter1, setSize, setIndex, false, ints1.size(), 0);
		validateIterable(iter2,iter3);
		
	}

	protected void validateIterable(Iterable<Integer> ith, Iterable<Integer> expectedDataIter) {
		List<Integer> expectedData = SoundTestUtils.iterable2List(expectedDataIter);
		List<Integer> ithList = SoundTestUtils.iterable2List(ith);
		Assert.assertTrue(ithList.size() == expectedData.size());
		for (int times=0 ; times<2 ; times++) {			// Do it twice to make sure that iterator() is repeatable
			if (this.testIterablePreservesOrder) {
				Assert.assertTrue(ithList.equals(expectedData));
//			} else {
//				for (Integer i : ith) {
//					Assert.assertTrue("Missing " + i + ", times=" + times, expectedData.contains(i));
//				}
			}
		}

//		for (int times=0 ; times<2 ; times++) {			// Do it twice to make sure that iterator() is repeatable
//			Iterator<Integer> expectedIter = expectedData.iterator();
//			Iterator<Integer> ithIter = ith.iterator();
//			int count =0;
//			while (ithIter.hasNext()) {
//				Integer i = ithIter.next();
//				Integer expected = expectedIter.next();
//				if (!expected.equals(i))
//					i = null;
//				Assert.assertTrue("count=" + count + ",i=" + i, expected.equals(i));
//				count++;
//			}
//			Assert.assertTrue(count == expectedData.size());
//			Assert.assertTrue(!expectedIter.hasNext());
//		}
	}


	protected static List<Integer> makeIntegerList(int setSize, int setIndex, boolean excludeSetIndex, int maxItems, int firstN, int maxListSize) {
			
		List<Integer> intList = new ArrayList<Integer>();
		int currentSetIndex = 0;
		for (int i=0 ; i<maxListSize && (maxItems < 0 || intList.size() < maxItems); i++) {
			boolean addInt;
			if (i < firstN) {
				addInt = true;
			} else {
				if (setSize != 0) {
					currentSetIndex = currentSetIndex % setSize;
					addInt = (excludeSetIndex ? currentSetIndex != setIndex : currentSetIndex == setIndex);
				} else {
					addInt = true;
				}
				currentSetIndex++;
			}
			if (addInt)
				intList.add(i);
		}
		Assert.assertTrue("test misconfigured", maxItems < 0 || intList.size() <= maxItems);
			
		return intList;
	}
	
	public static <T> List<T> getExpectedItems(Iterable<T> iter, int setSize, int setIndex, boolean excludeSetIndex, int maxItems, int firstN) { // , int maxListSize) {
		
		List<T> intList = new ArrayList<T>();
		int currentSetIndex = 0;
		Iterator<T> iterator = iter.iterator();
		for (int i=0 ; iterator.hasNext() && (maxItems < 0 || intList.size() < maxItems); i++) {
			boolean addInt;
			if (i < firstN) {
				addInt = true;
			} else {
				if (setSize != 0) {
					currentSetIndex = currentSetIndex % setSize;
					addInt = (excludeSetIndex ? currentSetIndex != setIndex : currentSetIndex == setIndex);
				} else {
					addInt = true;
				}
				currentSetIndex++;
			}
			T item = iterator.next();
			if (addInt)
				intList.add(item);
		}
		Assert.assertTrue("test misconfigured", maxItems < 0 || intList.size() <= maxItems);
			
		return intList;
	}

	public static int getExpectedItemCount(int count, int firstN, int mthAfterFirstN, boolean excludeIndex, int maxItems) {
		int total = count;
		if (mthAfterFirstN == 0)
			mthAfterFirstN = 1;	// every item.
		
		if (firstN < count) {
			total = firstN;
			if (!excludeIndex) {
				// Only the mth item 
				int item = firstN + 1;
				while (item <= count) {
//					System.out.println("Adding item " + item);
					total++;
					item += mthAfterFirstN;
				}
			} else {
				// All but the mth item.
				int item = firstN+1; 
				while (item <= count) {
					for (int i=0 ; i<mthAfterFirstN && item <= count; i++) {
						if (i != 0)	{ // 0 is the index of the skipped item
//							System.out.println("Adding item " + item);
							total++;
						} else {
//							System.out.println("Skipping item " + item);
							
						}
						item++;
					}
				}
				
			}
		} else {
			total = count;
		}
		if (maxItems >= 0)
			total = Math.min(total, maxItems);
		return total;
	}

	@Test
	public void testCounts() {
//		int count = 1000;
		int count = 40;
		int durationMsec = 1000;
		int subClipMsec = 1000;
		int subClipsPerClip = durationMsec / subClipMsec;
		
		// Create a map of ints 
		List<Integer> intList = new ArrayList<Integer>(); 
		for (int i=1 ; i<=count ; i++)
			intList.add(i);

		for (int firstN=0 ; firstN<=10 ; firstN+=10) {
//		for (int firstN=10 ; firstN<=10 ; firstN+=10) {
			for (int mthAfterFirstN=0 ; mthAfterFirstN<=4 ; mthAfterFirstN+=4) {
//			for (int mthAfterFirstN=4 ; mthAfterFirstN<=4 ; mthAfterFirstN+=4) {
				if (firstN > 0 && mthAfterFirstN == 0)
					continue;	// Not allowed 
				for (int maxItems=0 ; maxItems<=20; maxItems+=20) {
//				for (int maxItems=30 ; maxItems<=30; maxItems+=30) {
					if (maxItems == 0)
						maxItems = -1;	// -1 now means no max instead of 0.
					String msg =  "firstN=" + firstN + ", mthAfterFirstN=" + mthAfterFirstN + ", maxItems=" + maxItems; 
					// Everything but the mth item
//					Iterable<Integer> trainingData = new IthIterable<Integer>(intList, mthAfterFirstN, 0, true, maxItems,  firstN);
					Iterable<Integer> trainingData =           getIthIterable(intList, mthAfterFirstN, 0, true, maxItems,  firstN);
					int expectedTrainingSize = getExpectedItemCount(count, firstN, mthAfterFirstN, true, maxItems);
					// Only the mth item
					Iterable<Integer> testData;
					if (mthAfterFirstN == 0) 
						testData = new ArrayList<Integer>();
					else
//						testData = new IthIterable<Integer>(intList, mthAfterFirstN, 0, false, maxItems, firstN);
					    testData =           getIthIterable(intList, mthAfterFirstN, 0, false, maxItems,  firstN);
					int expectedTestingSize = getExpectedItemCount(count, firstN, mthAfterFirstN, false, maxItems);
					List<Integer> trainingInts= getItems(trainingData);
					List<Integer> testingInts = getItems(testData); 
					System.out.println("\n" + msg);
					System.out.println("all      data items=" + intList      + ", size=" + intList.size());
					System.out.println("training data items=" + trainingInts + ", size=" + trainingInts.size());
					System.out.println("testing  data items=" + testingInts  + ", size=" + testingInts.size());
					for (Integer i : intList) {
						if (i < firstN) {
							Assert.assertTrue(msg + " test int=" + i, trainingInts.contains(i));
							Assert.assertTrue(msg + " test int=" + i, testingInts.contains(i));
						}
					}
					int actualTrainingSize = trainingInts.size();
					int actualTestingSize = testingInts.size();
					int expectedTotalSize = expectedTrainingSize + expectedTestingSize;
					String trainingMsg = msg + ", expectedTrainingSize=" + expectedTrainingSize + ", actualSize=" + actualTrainingSize; 
					String testingMsg = msg + ",  expectedTestingSize=" + expectedTestingSize + ", actualSize=" + actualTestingSize; 
					if (mthAfterFirstN == 0) {
						// return all items up to maxItems.
						if (maxItems < 0) {	// All items
							Assert.assertTrue(msg, intList.equals(trainingInts));
							Assert.assertTrue(msg, actualTrainingSize == intList.size());
						} else {
							Assert.assertTrue(msg, actualTrainingSize == Math.min(intList.size(), maxItems));
						}
						// Assert.assertTrue(msg, actualTrainingSize == actualTestingSize);

					} else {
						int actualTotalSize = actualTrainingSize + actualTestingSize;
						for (Integer i: trainingInts)  {
							if (i > firstN)
								Assert.assertTrue(msg + " test int=" + i, !testingInts.contains(i));
						}
						Assert.assertTrue(trainingMsg,actualTrainingSize  == expectedTrainingSize); 
						Assert.assertTrue(testingMsg, actualTestingSize  == expectedTestingSize);
						String totalMsg = msg + ", expectedTotalSize=" + expectedTotalSize + ", actualTotalSize=" + actualTotalSize;
						Assert.assertTrue(totalMsg, actualTotalSize == expectedTotalSize);
					}
					if (maxItems >= 0) {
						Assert.assertTrue(msg, actualTrainingSize <= maxItems);
						Assert.assertTrue(msg, actualTestingSize <= maxItems);
					}
				}
			}
		}
	}


	private List<Integer> getItems(Iterable<Integer> items) {
		List<Integer> ints = new ArrayList<Integer>();
		for (Integer sr : items)
			ints.add(sr);
		if (!this.testIterablePreservesOrder)
			Collections.sort(ints);
		return ints;
	}

	@Test
	public void testMaxItems() {
		int testCount, baseCount = 20;
		List<Integer> expectedList, intList = ENGTestUtils.makeList(baseCount);  
		Iterable<Integer> testIter;
		
		testCount = baseCount - 1;
		expectedList = ENGTestUtils.makeList(Math.min(testCount, baseCount));
//		testIter = new IthIterable<Integer>(intList, testCount);
		testIter =           getIthIterable(intList, testCount);
		validateIterable(testIter, expectedList);

		testCount = baseCount + 1;
		expectedList = ENGTestUtils.makeList(Math.min(testCount, baseCount));
//		testIter = new IthIterable<Integer>(intList, testCount);
		testIter =           getIthIterable(intList, testCount);
		validateIterable(testIter, expectedList);

		testCount = baseCount;
		expectedList = ENGTestUtils.makeList(Math.min(testCount, baseCount));
//		testIter = new IthIterable<Integer>(intList, testCount);
		testIter =           getIthIterable(intList, testCount);
		validateIterable(testIter, expectedList);

		testCount = 0;
		expectedList = ENGTestUtils.makeList(Math.min(testCount, baseCount));
//		testIter = new IthIterable<Integer>(intList, testCount);
		testIter =           getIthIterable(intList, testCount);
		validateIterable(testIter, expectedList);

		testCount = 1;
		expectedList = ENGTestUtils.makeList(Math.min(testCount, baseCount));
//		testIter = new IthIterable<Integer>(intList, testCount);
		testIter =           getIthIterable(intList, testCount);
		validateIterable(testIter, expectedList);
	}
	
	@Test
	public void testTrainingAndTestData() { // Iterable<SoundRecording> sounds, int soundCount, String labelName) {
		int folds=4;	// Another power of two works nicely
		int counterPerFold = 6;		// Needs to be even for this test.
		int count = folds * counterPerFold;
		int fullTrainingSize = (folds-1) * count / folds;
		int fullTestSize = count - fullTrainingSize;
		List<Integer> itemList = ENGTestUtils.makeList(count); 
		Iterable<Integer> items = new ShufflizingIterable<Integer>(itemList);
		Iterable<Integer> ith; 
//		items = itemList;
	
		for (int fold=0 ; fold<folds; fold++) {
//				System.out.println("fold=" + fold);
				int firstNPerFold = 0, mthAfterFirstN = 0 , maxItemsPerFold = -1, firstN=0, nextMth;
				List<Integer> training, test;
				
				/** First test full, unfiltered folds */
				ith = IthIterable.getFoldData(items, folds, firstNPerFold, maxItemsPerFold, mthAfterFirstN, fold, true, false);
				training = SoundTestUtils.iterable2List(ith);
				Assert.assertTrue(training.size() == fullTrainingSize); 
				ith = IthIterable.getFoldData(items, folds, firstNPerFold, maxItemsPerFold, mthAfterFirstN, fold, false, false);
				test = SoundTestUtils.iterable2List(ith);
				Assert.assertTrue(test.size() == fullTestSize); 
				// Make sure there is no overlap between training and testing
				Assert.assertTrue(itemList.size() == training.size() + test.size());
				validateAsDisjoint(training,test);

				/** Next test taking very 2nd item  after the first 2 from each fold */ 
				mthAfterFirstN = 2;
				firstNPerFold = 2;
				ith = IthIterable.getFoldData(items, folds, firstNPerFold, maxItemsPerFold, mthAfterFirstN, fold, true, false);
				training = SoundTestUtils.iterable2List(ith);
				firstN = 2 * (folds - 1);					// 2 from each fold, plus...
				nextMth = (fullTrainingSize - firstN) / 2;	// every other after that
				Assert.assertTrue(training.size() == firstN + nextMth); 
				ith = IthIterable.getFoldData(items, folds, firstNPerFold, maxItemsPerFold, mthAfterFirstN, fold, false, false);
				test = SoundTestUtils.iterable2List(ith);
				Assert.assertTrue(test.size() == 2 + (fullTestSize-2)/2); 
				// Make sure there is no overlap between training and testing
				validateAsDisjoint(training,test);

				/** Next test taking very 2nd item  after the first 2 from each fold, with a max of 5 per fold*/ 
				mthAfterFirstN = 2;
				firstNPerFold = 2;
				maxItemsPerFold = 2; 
				ith = IthIterable.getFoldData(items, folds, firstNPerFold, maxItemsPerFold, mthAfterFirstN, fold, true, false);
				training = SoundTestUtils.iterable2List(ith);
				Assert.assertTrue(training.size() == (folds - 1) * maxItemsPerFold); 
				ith = IthIterable.getFoldData(items, folds, firstNPerFold, maxItemsPerFold, mthAfterFirstN, fold, false, false);
				test = SoundTestUtils.iterable2List(ith);
				Assert.assertTrue(test.size() == maxItemsPerFold); 
				// Make sure there is no overlap between training and testing
				validateAsDisjoint(training,test);;
		}
	}	
		
	/**
	 * This is largely the same test as {@link #testTrainingTestDataSeparation()} but uses Integers instead of sound recordings.
	 * This is a bit easier to debug with than the other when there are problems.
	 */
	@Test
	public void testTrainingTestDataSeparationIntegers() {
//		int count = 1000;
		int count = 24;
		boolean shuffle = false;
//		int subClipsPerClip = durationMsec / subClipMsec;
		
		// Create a map of clips 
		List<Integer>  soundList = ENGTestUtils.makeList(count); 

		// Create the shuffleable iterable needed by the 
		Iterable<Integer> iterable ; 
		if (shuffle) {
			ShufflizingIterable<Integer> si = new ShufflizingIterable<Integer>(soundList);
//			iterable = new FixedDurationInteger(si, subClipMsec, PadType.ZeroPad);
			iterable = si;
		} else {
			iterable = soundList;
//			iterable = new FixedDurationSoundRecordingIterable(si, subClipMsec, PadType.ZeroPad);
		}
		List<Integer> srList = SoundTestUtils.iterable2List(iterable); 
		int actualTotalSize = srList.size();
		Assert.assertTrue(actualTotalSize == count);

		for (int folds=2 ; folds<=5 ; folds++) {
//		for (int folds=3 ; folds<=5 ; folds++) {
			for (int firstNPerFold=0 ; firstNPerFold<=10 ; firstNPerFold+=10) {
//			for (int firstNPerFold=10 ; firstNPerFold<=10 ; firstNPerFold+=10) {
				for (int mthAfterFirstN=0 ; mthAfterFirstN<=4 ; mthAfterFirstN+=4) {
//				for (int mthAfterFirstN=4 ; mthAfterFirstN<=4 ; mthAfterFirstN+=4) {
					if (firstNPerFold > 0 && mthAfterFirstN == 0)
						continue;	// Not allowed by KFoldModelEvaluator
					for (int maxPerFold=0 ; maxPerFold<=100 ; maxPerFold+=100) {
						for (int fold=0 ; fold<folds ; fold++) {
							Iterable<Integer> trainingData = IthIterable.getFoldData(iterable, folds, firstNPerFold, maxPerFold, mthAfterFirstN, fold, true, false);
							Iterable<Integer> testData = IthIterable.getFoldData(iterable, folds, firstNPerFold, maxPerFold, mthAfterFirstN, fold, false, false);
							String msg = "fold=" + fold + " of " + folds + ", firstNPerFold=" + firstNPerFold + ", mthAfterFirstN=" + mthAfterFirstN 
									+ ", maxPerFold=" + maxPerFold; //  + ", countPerFold=" + expectedCountPerFold;
//							String msg = "fold=" + fold + " of " + folds ;

							List<Integer> trainingDataList = SoundTestUtils.iterable2List(trainingData);
							List<Integer> testDataList = SoundTestUtils.iterable2List(testData);
							int actualTrainingSize = trainingDataList.size();
							int actualTestingSize = testDataList.size();
//							AISPLogger.logger.info("actualTotalSize=" + actualTotalSize + ", " + msg);
							// expected sizes are hard to calcalculate, just make sure the training data is a multiple of the testing data
							double maxRatio = (double)(actualTestingSize+1) / (actualTrainingSize-1);
							double minRatio = (double)(actualTestingSize-1) / (actualTrainingSize+1);
							int foldSize = actualTotalSize / folds;
							double expectedTestCount = firstNPerFold + (foldSize - firstNPerFold) /  (mthAfterFirstN == 0 ? 1 : mthAfterFirstN);
							if (maxPerFold > 0 && expectedTestCount > maxPerFold)
								expectedTestCount = maxPerFold;
							double expectedTrainingCount = expectedTestCount * (folds -1); 
							double targetRatio = expectedTestCount / expectedTrainingCount; 
							Assert.assertTrue(msg + ", ratio=(" + minRatio + ".." + maxRatio + "), targetRatio=" + (folds-1), 
									targetRatio > minRatio && targetRatio < maxRatio);
						}
					}
				}
			}
		}
	}
	
//	public static <ITEM> Iterable<ITEM> getFoldData(Iterable<ITEM> labeledData, int foldCount, int firstNPerFold,
//			int maxItemsPerFold, int mthAfterFirstN, int testFoldIndex, boolean asTrainingData, boolean verbose) {
//		if (testFoldIndex < 0 || testFoldIndex >= foldCount)
//			throw new IllegalArgumentException("fold index must be from 0 to " + (foldCount - 1));
//		Iterable<ITEM> r;
//		int nMinus1 = foldCount - 1; // Here we're iterating on all folds except the one being held out.
//		int maxItems = -1; // no max on the first iterable
//		if (labeledData instanceof IShuffleIterable) { // Keep it a shuffle iterator for better performance.
//			// Get every item other than those for the given fold.
//			IShuffleIterable<ITEM> iterable = new IthShuffleIterable<ITEM>((IShuffleIterable<ITEM>) labeledData,
//					foldCount, testFoldIndex, asTrainingData, maxItems, 0);
//			// Limit the number of items we train on to the First N and every Mth item after
//			// that, with the given maximum.
//			if (mthAfterFirstN == 0 && maxItemsPerFold == 0) {
//				if (verbose)
//					AISPLogger.logger.info("Using 1st shuffle training iterable (w/o 2nd)");
//				r = iterable;
//			} else {
//				if (verbose)
//					AISPLogger.logger.info("Getting 2nd shuffle training iterable over the first");
//				maxItems = maxItemsPerFold <= 0 ? -1 : nMinus1 * maxItemsPerFold;
//				r = new IthShuffleIterable<ITEM>(iterable, mthAfterFirstN, 0, false, maxItems, firstNPerFold * nMinus1);
//			}
//		} else { // Same code below, just use new IthIterable instead of new IthSuffleIerable.
//			// Get every item other than those for the given fold.
//			Iterable<ITEM> iterable = new IthIterable<ITEM>(labeledData, foldCount, testFoldIndex, asTrainingData, maxItems,
//					0);
//			// Limit the number of items we train on to the First N and every Mth item after
//			// that, with the given maximum.
//			maxItems = maxItemsPerFold <= 0 ? -1 : nMinus1 * maxItemsPerFold;
//			if (mthAfterFirstN == 0 && maxItems <= 0 && firstNPerFold == 0) {
//				if (verbose)
//					AISPLogger.logger.info("Using 1st non-shuffle training iterable (w/o 2nd)");
//				r = iterable;
//			} else {
//				if (verbose)
//					AISPLogger.logger.info("Getting 2nd non-shuffle training iterable over the first");
//				r = new IthIterable<ITEM>(iterable, mthAfterFirstN, 0, false, maxItems, firstNPerFold * nMinus1);
//			}
//		}
//		return r;
//	}
	
	private void validateAsDisjoint(List<Integer> training, List<Integer> test) {
//		System.out.println("training:" + getLabels(training, labelName));
//		System.out.println("testing :" + getLabels(test, labelName));
	for (Integer i : training) {
		Assert.assertTrue(!test.contains(i));
	}
	for (Integer i : test) {
		Assert.assertTrue(!training.contains(i));
	}
		
}

}
