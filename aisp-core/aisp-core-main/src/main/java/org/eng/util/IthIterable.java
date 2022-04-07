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

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eng.ENGLogger;

/**
 * An iterable that supports getting the N values of an iterable and thereafter every N or everything but every N.
 * @author dawood
 *
 * @param <T>
 */
public class IthIterable<T> implements Iterable<T> {
	protected final Iterable<T> iterable;
	protected final int firstN;
	protected final int strideIndex;
	protected final int strideSize;
	protected final boolean isIndexExcluded;
	protected final int maxItems;

	public IthIterable(Iterable<T> iterable, IthIterable<?> ith) {
		this(iterable,ith.strideSize, ith.strideIndex, ith.isIndexExcluded, ith.maxItems, ith.firstN);
	}
	
	/**
	 * Create the iterable to return all items up to the give maximum number.
	 * @param iterable
	 * @param maxItems if set to -1, then get all items.
	 */
	public IthIterable(Iterable<T> iterable, int maxItems) {
		this(iterable,0,0,true,maxItems,0);
	}

	/**
	 * A convenience on {@link #IthIterable(Iterable, int, int, boolean, int, int)} with no firstN or max item settings.
	 */
	public IthIterable(Iterable<T> iterable, int strideSize,  int strideIndex, boolean isIndexExcluded) {
		this(iterable, strideSize, strideIndex, isIndexExcluded, -1,-1);
	}

	/**
	 * Traverse the iterable to retrieve the first N items and thereafter retrieve every Mth or everything but every Mth item up to an optional maximum 
	 * number of items.
	 * @param iterable iterable to traverse.
	 * @param strideSize size of the stride (i.e. i) over the items in the iterable. 
	 * Must be 0 or 2 or larger.  If 0, then strideIndex and isIndexExluded are ignored and all items are iterated through up to the maxItems.
	 * @param strideIndex 0-based index of item within stride to keep (isIndexExcluded = false) or skip and take all others (isExclude=true). 
	 * Must be in the range 0..strideSize-1, if strideSize is 2 or larger.  If strideSize is 0, then this is ignored.
	 * @param isIndexExcluded true if we exclude the item at the given strideIndex and take all others, false 
	 * if we only take the item at the given stride index. If strideSize is 0, then this is ignored.
	 * @param maxItems the maximum number of items for the iterator to return.  -1 if no max.
	 * @param firstN if larger than zero, retrieve the first firstN values from the iterable and then others as defined my strideIndex and strideSize
	 * up to the max, if defined.
	 */
	public IthIterable(Iterable<T> iterable, int strideSize,  int strideIndex, boolean isIndexExcluded, int maxItems, int firstN) {
		if (strideSize == 1 || strideSize < 0)
			throw new IllegalArgumentException("stride size must be 0 or larger than 1");
		if (strideIndex < 0)
			throw new IllegalArgumentException("stride index must range from 0 to strideSize-1 one");
		if (strideSize != 0 && strideIndex >= strideSize)
			throw new IllegalArgumentException("stride index must range from 0 to strideSize-1 one");
		if (maxItems >= 0 && firstN > maxItems)
			throw new IllegalArgumentException("firstN must be smaller than a non-zero maxItems");
		this.iterable = iterable;
		this.firstN = firstN;
		this.strideIndex = strideIndex;
		this.strideSize = strideSize;
		this.isIndexExcluded = isIndexExcluded;
		this.maxItems = maxItems;
	}

	@Override
	public Iterator<T> iterator() {
		return new IthIterator<T>(iterable.iterator(), strideSize, strideIndex, isIndexExcluded, maxItems, firstN);
	}
	
	/**
	 * Get a shuffle iterable if the given iterable is a shuffle iterable, otherwise a non-shuffle.
	 * @return never null.
	 */
	public static <ITEM> Iterable<ITEM> newIthIterable(Iterable<ITEM> iterable, int folds, int foldIndex,  boolean isExcluded, int maxItems, int firstN) {
		if (iterable instanceof IShuffleIterable) 	// Keep it a shuffle iterator for better performance.
			// Get every item other than those for the given fold. 
			return new IthShuffleIterable<ITEM>((IShuffleIterable<ITEM>)iterable, folds, foldIndex, isExcluded, maxItems, firstN);
		else 
			return new IthIterable<ITEM>(iterable, folds, foldIndex, isExcluded, maxItems, firstN);
	}

	/**
	 * @param foldCount
	 * @param firstNPerFold
	 * @param mthAfterFirstN
	 * @param maxItemsPerFold2 
	 */
	public static void validateFoldParameters(int foldCount, int firstNPerFold, int mthAfterFirstN, int maxItemsPerFold) {
		if (foldCount <= 1)
			throw new IllegalArgumentException("foldCount must be larger than 1");
		if (firstNPerFold < 0) {
			throw new IllegalArgumentException("firstN must be 0 or larger");
		} else if (firstNPerFold > 0) {
			if (mthAfterFirstN < 0 || mthAfterFirstN == 1)
				throw new IllegalArgumentException(
						"mthAfterFirstN must be 0 or larger than 1 when firstNPerFold is positive");
		}
		if (mthAfterFirstN < 0 || mthAfterFirstN == 1) // IthIterable
			throw new IllegalArgumentException("mthAfterFirstN must be 0 or 2 or larger");
	}

	public static <ITEM> Iterable<ITEM> getFoldData(Iterable<ITEM> labeledData, int foldCount, int firstNPerFold, int maxItemsPerFold,  int mthAfterFirstN, 
											int testFoldIndex, boolean asTrainingData, boolean verbose) {
			if (testFoldIndex < 0 || testFoldIndex >= foldCount)
				throw new IllegalArgumentException("fold index must be from 0 to " + (foldCount-1));
			IthIterable.validateFoldParameters(foldCount, firstNPerFold, mthAfterFirstN, maxItemsPerFold);
	
			Iterable<ITEM> r;
			int nMinus1 = foldCount-1;	// Here we're iterating on all folds except the one being held out.
			int maxItems = -1;	// no max  on the first iterable
	
			// Get every item other than those for the given fold. 
	//		ENGLogger.logger.info(TrainingSetInfo.getInfo((Iterable<? extends ILabeledDataWindow<?>>) labeledData).prettyFormat());
			Iterable<ITEM> iterable = IthIterable.newIthIterable(labeledData, foldCount, testFoldIndex, asTrainingData, maxItems, 0);
	//		ENGLogger.logger.info(TrainingSetInfo.getInfo((Iterable<? extends ILabeledDataWindow<?>>) iterable).prettyFormat());
	//		ENGLogger.logger.info("iterable: " + SoundTestUtils.iterable2List(iterable));
			// Limit the number of items we train on to the First N and every Mth item after that, with the given maximum.
			maxItems = (asTrainingData ? nMinus1 : 1) * (maxItemsPerFold <= 0 ? -1 : maxItemsPerFold);	// This iterable has n-1 folds and each folds has MaxItermsPerFold
			if (mthAfterFirstN == 0 && maxItems <= 0 && firstNPerFold <= 0) {
				if (verbose)
					ENGLogger.logger.info("Using 1st non-shuffle training iterable (w/o 2nd)");
				r = iterable;
			} else {
				if (verbose)
					ENGLogger.logger.info("Getting 2nd non-shuffle training iterable over the first");
				int firstN = (asTrainingData ? firstNPerFold*nMinus1 : firstNPerFold);			// Get the first unlimited set of fold
				int folds = mthAfterFirstN;			// The folds size AFTER the firstN
				r = IthIterable.newIthIterable(iterable, folds, 0,  false, maxItems, firstN);		// Only take every mth after the firstN.
			}
	
	//		ENGLogger.logger.info("iterable: " + SoundTestUtils.iterable2List(r));
			return r;
		}

	protected static class IthIterator<T> implements Iterator<T> {

		protected final Iterator<T> iterator;
		protected final int firstN;
		protected final int strideIndex;
		protected final int strideSize;
		protected final boolean isIndexExcluded;
		protected int nextIndex = 0;
		protected int retrievedCount = 0;
		protected int maxItems = 0;
		protected final ItemReferenceIterator<T> itemReferenceIterator;

		public IthIterator(Iterator<T> iterator, int strideSize, int strideIndex, boolean isIndexExcluded, int maxItems, int firstN) {
			this.iterator = iterator;
			this.firstN = firstN;
			this.strideIndex = strideIndex;
			this.strideSize = strideSize;
			this.isIndexExcluded = isIndexExcluded;
			this.maxItems = maxItems;
			if (iterator instanceof ItemReferenceIterator)
				this.itemReferenceIterator = (ItemReferenceIterator<T>)iterator;
			else
				this.itemReferenceIterator = null; 
		}

		@Override
		public boolean hasNext() {
			boolean r;
			if (strideSize == 0) {	// All we care about is maxItems
				if (maxItems >= 0 && retrievedCount >= maxItems) 
					return false;
				else if (maxItems <= 0 || retrievedCount < maxItems) 
					r = iterator.hasNext();
				else
					r = false;
			} else if (maxItems >= 0 && retrievedCount >= maxItems)  {
				return false;
			} else if (firstN > 0 && retrievedCount < firstN) {
				r = iterator.hasNext();
			} else if (isIndexExcluded) {	// Everything but the given index.
				// Move our iterator over the index we need to avoid 
				while (nextIndex == strideIndex && iterator.hasNext()) {
					nextIndex++;
					if (nextIndex == strideSize)
						nextIndex = 0;
					skipNext();
				}
				// If we were able to find an index that is not the strideIndex, then use hasNext(). 
				if (nextIndex != strideIndex) 
					r = iterator.hasNext();
				else 
					r = false;
				
			} else { // Only the item with the given index.
				// Move our iterator up to the item with the strideIndex
				while (nextIndex != strideIndex && iterator.hasNext()) {
					nextIndex++;
					if (nextIndex == strideSize)
						nextIndex = 0;
					skipNext();
				}
				// If we were able to go up to the next index, use the iterator's hasNext, otherwise there isn't a next.
				if (nextIndex == strideIndex) 
					r = iterator.hasNext();
				else 
					r = false;
			}
			return r;
		}
		@Override
		public T next() {
	    	if (!hasNext())	// Make sure we are pointed at the next element even if the user hasn't call hasNext();
	    		throw new NoSuchElementException("nextIndex = " + nextIndex);
			T item = iterator.next();
			retrievedCount++;
			if (strideSize > 0 && retrievedCount > firstN) {
				nextIndex++;
				if (nextIndex == strideSize)
					nextIndex = 0;
			}
			return item;
		}
		
		private void skipNext() {
			if (itemReferenceIterator == null)
				iterator.next();
			else
				itemReferenceIterator.skipNext();
		}
	}

}