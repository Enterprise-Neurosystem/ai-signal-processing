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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * Providings filtering of items in an IShuffleIterable by extending the super class and forcing the use of IShuffleIterable in the constructor.
 * <p>
 * This class supports implementations that would like to modify the items in a proxied IShuffleIterable to produce a modified set of items from its iterator.
 * Implementations must provide the following methods
 * <ul>
 * <li> {@link #newIterable(Iterable, AbstractFilteringShuffleIterable)} to create a new instance from a given set of item references, which are fundamental 
 * to IShuffleIterables.
 * </ul>
 * An important consideration when extending this class is whether or not to make the shuffles provide a repeatable set of items, albeit in a different order, or
 * in the case where items are filtered out, to filter out a different set of items on each shuffle.  The implementation allows of support of 
 * repeatability albeit at the cost of potentially having to iterate all the items to align items with their references.
 * To enable repeatability, simply pass {@link #enforceShuffleRepeatability} as true to the constructor can cause the following:
 * <ul>
 * <li> the values returned by {@link #getReferences()} to be consistent
 * <li> the items returned by {@link #iterator()} to be the same set of items, although perhaps shuffled if {@link #shuffle(long)} has been called.
 * </ul>
 * That said, other methods that might be overridden, especially when filtering on the references and not the values, include the following:
 * <ul>
 * <li> {@link #getFilteredReferences()} to filter items on references.  For example the IthShuffleIterable does this to sub-select items only based on the
 * order of the references and not on the values of the items themselves.
 * </ul>
 * Finally, consider that depending on the filter being used, {@link #newStatefulFilter(IFilter)} from the super class may need to be implemented.
 * 
 * 
 * @author dawood
 *
 * @param <ITEM>
 */
public abstract class AbstractFilteringShuffleIterable<ITEM> extends FilteringIterable<ITEM> implements IShuffleIterable<ITEM> {

	/**
	 * 
	 * Create the iterable to apply the given filter to the iterable to control which items are actually produced by this instance.
	 * @param iterable
	 * @param matcher if non-null, then filter the given iterable and if a stateful filter, then override {@link #newStatefulFilter(IFilter)}.  
	 * If null, then sub-classes must override {@link #createFilteringIterator()}.
	 * @param enableShuffleRepeatability if true, then the set of items produced by shuffling is always the same.
	 */
	protected AbstractFilteringShuffleIterable(IShuffleIterable<ITEM> iterable, IFilter<ITEM> matcher, boolean enforceRepeatableShuffle) {
		super(iterable, matcher);
		this.enforceShuffleRepeatability = enforceRepeatableShuffle;
	}

	protected AbstractFilteringShuffleIterable(IShuffleIterable<ITEM> iterable, IFilter<ITEM> matcher, AbstractFilteringShuffleIterable<ITEM> requester) {
		super(iterable, matcher);
		if (requester == null)
			throw new IllegalArgumentException("Sub-class is not allowed to pass null for requester");
		this.enforceShuffleRepeatability = requester.enforceShuffleRepeatability;
		 
	}
	private boolean enforceShuffleRepeatability = false;

	private IShuffleIterable<ITEM> fixedShuffleIterable;

	/**
	 * Gets the set of references directly from the proxied IShuffleIterable, unless repeatability is enabled in which case 
	 * {@link #getFilteredReferences()} is used to return only the references that produce items for this instance.
	 * @throws NoSuchElementException if enforcing repeatability and the fitlered references could not be determined.
	 */
	@Override
	public Iterable<String> getReferences() {
		if (enforceShuffleRepeatability) {
			return getFilteredReferences();
		} else
			return ((IShuffleIterable<?>)this.iterable).getReferences();
	}


	/**
	 * Override to use the ItemReferenceIterator once we have determined the item references to be used for this instance.
	 */
	@Override
	public Iterator<ITEM> iterator() {
		if (this.enforceShuffleRepeatability) {
			return getFilteredShuffleIterable().iterator();
		} else {
			return  createFilteringIterator();
		}
	}

	/**
	 * Get an iterable of only the filtered items for shuffle repeatability.
	 * If the {@link #getFilteredReferences()} can not determine the references, then 
	 * all items are run through this instance's filter and brought into memory in a ShufflizingIterable.
	 * @return never null.
	 */
	protected IShuffleIterable<ITEM> getFilteredShuffleIterable() {
		Iterable<String> refs;
		try {
			refs = getFilteredReferences();
		} catch (NoSuchElementException e) {
			refs = null;		// Try and just  use the 
		}
		if (refs == null) {
			if (fixedShuffleIterable == null) {
				//  Iterate through the items using our filter and create ShufflizingIterable from the list of those items.
				// Bringing all items into memory!
				List<ITEM> items = new ArrayList<ITEM>();
				Iterator<ITEM> iterator =  createFilteringIterator();
				while (iterator.hasNext()) 
					items.add(iterator.next());
				fixedShuffleIterable = new ShufflizingIterable<ITEM>(items);
			}
			return fixedShuffleIterable;
		} else {
			return new ShufflizingItemReferenceIterableProxy<ITEM>((IShuffleIterable)this.iterable, refs);
		}
	}
	
	/**
	 * Create the iterator that will iterate through the references and filter them out.  This is the less efficient implementation
	 * returned by {@link #iterator()} that goes through each item in the underlying iterator and dis/allows as an output of this iterable. 
	 * This must be overridden if passing matcher=null to the constructor.  If matcher!=null, then this implementation uses the super 
	 * class to create the constructor that uses the matcher to filter out items.
	 * @return
	 */
	protected Iterator<ITEM> createFilteringIterator() {
		if (this.matcher == null)
			throw new IllegalArgumentException("Constructor called without a matcher, which requires the implementation to provide one of createFilterIterator() or iterator() methods");
		return super.iterator();	// Get the iterator that uses the matcher to filter items.
	}

	@Override
	public ITEM dereference(String reference) {
		return ((IShuffleIterable<ITEM>)this.iterable).dereference(reference);
	}

	@Override
	public List<ITEM> dereference(List<String> references) {
		return ((IShuffleIterable<ITEM>)this.iterable).dereference(references);
	}

	private final Random random = new Random(12300237);	// repeatable shufflability for this instance
	
	@Override
	public IShuffleIterable<ITEM> shuffle() {
		return this.shuffle(random.nextLong());
	}

	/**
	 * If {@link #enforceShuffleRepeatability} is enabled, then using {@link #getFilteredShuffleIterable()}, otherwise
	 * just shuffle the 
	 */
	@Override
	public IShuffleIterable<ITEM> shuffle(long seed) {
//		if (enforceShuffleRepeatability) {	// Filter the references and only use those g oing forward.
//			return getFilteredShuffleIterable().shuffle(seed);
//		} else {							// Use the unfiltered references from this.iterable
//			Iterable<String> refs = this.getReferences();
//			List<String> newRefs = ShuffleIterableUtil.shuffleReferences(seed, refs);
//			return newIterable(newRefs); 
//		}
			Iterable<String> refs = this.getReferences();
			List<String> newRefs = ShuffleIterableUtil.shuffleReferences(seed, refs);
			return newIterable(newRefs); 
	}

	/**
	 * Simply call {@link #newIterable(Iterable, AbstractFilteringShuffleIterable)} passing in the given argument and this instance.
	 */
	@Override
	public final IShuffleIterable<ITEM> newIterable(Iterable<String> references) {
		return newIterable(references, this);
	}

	/**
	 * Called by {@link #newIterable(Iterable)} to perform similar function, but the given instance is expected to be based back
	 * in any constructor to enable copying of state relevant to its implementation.
	 * @param references
	 * @param requestor
	 * @return never null.
	 */
	protected abstract IShuffleIterable<ITEM> newIterable(Iterable<String> references, AbstractFilteringShuffleIterable<ITEM> requestor);

	private List<String> filteredReferences = null;
	
	/**
	 * Get an iterable of the references that are responsible for producing the items from this iterable.
	 * This implementation iterates through the references of the underlying proxied iterable to try
	 * and match up the references found there with the items produced by this instance (i.e. the filtered
	 * items). 
	 * <p>
	 * This implementation <b>requires</b> the following:
	 * <ol>
	 * <li> the filter of this instance does not <b>add</b> items to the set of items it produces. 
	 * <li> the proxied IShuffleIterable's {@link #iterator()} produces an <b>one</b> item for each of the references it returns in its {@link #getReferences()} method.
	 * </ol>
	 * For example,
	 * <pre>
	 * Unfiltered iterable references : A B C D 
	 * Unfiltered iterator Items      : a b c d
	 * Filtered   iterator Items      : a   c 
	 * Filtered   references          : A   C  
	 * </pre>
	 * 
	 * If you find this to be the case, then either turn of the {@link #enforceShuffleRepeatability} or override this implementation.
	 * @return never null
	 * @throws NoSuchElementException
	 */
	protected synchronized Iterable<String> getFilteredReferences() throws NoSuchElementException {
			if (filteredReferences == null) {
				IShuffleIterable<?> unfilteredIterable = (IShuffleIterable<?>)this.iterable;
//				ShuffleIterableUtil.showIterable("Unfiltered", unfilteredIterable.iterator());
//				ShuffleIterableUtil.showIterable("Filtered", this.createFilteringIterator());
//				ShuffleIterableUtil.compareIterables(unfilteredIterable.iterator(),this.createFilteringIterator());
//				System.out.println("=====================================");
				List<String> references = new ArrayList<String>();
				Iterator<String> unfilteredReferences = unfilteredIterable.getReferences().iterator();
//				List<String> unfilteredReferenceList = SoundTestUtils.iterable2List(unfilteredIterable.getReferences());
//				System.out.println("Ref count=" + unfilteredReferenceList.size());
				Iterator<?> unfilteredItems = unfilteredIterable.iterator();
				int filteredCount = 0, unfilteredCount=0;
				Iterator<?> filteredItemIterator =  this.createFilteringIterator(); 	// Over filtered out items.
				while (filteredItemIterator.hasNext()) {
					Object filteredItem = filteredItemIterator.next(); 	// The item we're looking for in the unfiltered 
					Object unfilteredItem = null; 
					String matchingRef = null;
					// Search forward in the unfiltered items until we find the matching item and its reference.
					while (unfilteredItem != filteredItem && !filteredItem.equals(unfilteredItem)) {
						unfilteredCount++;
//						System.out.println("Skipping forward to unfiltered index " + unfilteredCount);
						if (!unfilteredItems.hasNext()) 
							throw new NoSuchElementException("Could not find filtered item in unfiltered iterable.");
						if (!unfilteredReferences.hasNext()) 
							throw new NoSuchElementException("Ran out of unfiltered references."
								+ " This can happen when"
								+ " a) the proxied iterable is producing more than one item per reference, or"
								+ " b) the filtering iterable is adding items to or changing the order of items relative to the unfiltered iterabled."
								+ " In case (a), you will not be able to provided repeatable shufflability." 
								+ " If case (b), the filtering iterable (" + this.getClass().getName() + ") should not be using this parent class.");
						unfilteredCount++;
						unfilteredItem = unfilteredItems.next();
						matchingRef = unfilteredReferences.next();
					}
//					System.out.println("Match unfiltered index " + (unfilteredCount-1) + " with filtered index " + filteredCount);
					references.add(matchingRef);
					filteredCount++;
				}
				filteredReferences = references;
			}
			return filteredReferences;
		}

}
