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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eng.aisp.ILabeledDataWindow;
import org.eng.util.FilteringIterable;
import org.eng.util.FilteringIterator;
import org.eng.util.IItemReferenceIterable;

/**
 * Wraps an iterable to order the windows so that the windows with common label values come out in sequence.
 * The implementation is such that the underlying iterable will be iterated N times if there are N different
 * values for the given labelName.
 * @author dawood
 *
 * @param <WINDATA>
 */
public class GroupedLabelValueWindowIterable<LDW extends ILabeledDataWindow<?>> implements Iterable<LDW> { 

	protected final Iterable<LDW> labeledDataWindows;
	protected final String labelName;

	public GroupedLabelValueWindowIterable(Iterable<LDW> ldwIterable, String labelName) {
		this.labeledDataWindows = ldwIterable;
		this.labelName = labelName;
	}


	@Override
	public Iterator<LDW> iterator() {
		return new ByLabelValueWindowIterator<LDW>(labeledDataWindows, labelName);
	}
	
	/**
	 * Match only windows having a given label and optional capture all label values encountered.
	 */
	private static class LabelMatcher<LDW extends ILabeledDataWindow<?>>  implements FilteringIterable.IFilter<LDW> {
		private String labelName;
		private String labelValue;
		private Map<String,List<String>> capturedLabelValues = null;
		public LabelMatcher(String labelName, String labelValue, Map<String, List<String>> capturedLabelValues) { 
			this.labelName = labelName; this.labelValue = labelValue; 
			this.capturedLabelValues = capturedLabelValues; 
		}
		private static List<String> dummyRefs = new ArrayList<String>();
		@Override
		public boolean include(LDW item) { 
			if (labelValue == null) {
				labelValue = item.getLabels().getProperty(labelName);
				if (labelValue == null)
					return false;
			}
			String windowLabelValue = item.getLabels().getProperty(labelName); 
			if (capturedLabelValues != null)
				capturedLabelValues.put(windowLabelValue, dummyRefs);
			return labelValue.equals(windowLabelValue);
		}
	}
	
	/**
	 * Holds a reference and it associated labeled data window.
	 */
	private static class ReferenceAndWindow<LDW extends ILabeledDataWindow<?>> {
		public String reference;
		public LDW labeledWindow;
		public ReferenceAndWindow(String reference, LDW labeledWindow) {
			this.reference = reference;
			this.labeledWindow = labeledWindow;
		}
	}

	
	/**
	 * Match only windows having a given label and optional capture all label values encountered.
	 */
	private static class ReferencingLabelMatcher<LDW extends ILabeledDataWindow<?>>  implements FilteringIterable.IFilter<ReferenceAndWindow<LDW>> {
		private String labelName;
		private String labelValue;
		private Map<String,List<String>> capturedLabelValues = null;
		
		/**
		 * @param labelName
		 * @param labelValue if null, use the first label value encountered.
		 * @param capturedLabelValues if not null, capture a map of label values to lists of references.
		 */
		public ReferencingLabelMatcher(String labelName, String labelValue, Map<String, List<String>> capturedLabelValues) { 
			this.labelName = labelName; this.labelValue = labelValue; 
			this.capturedLabelValues = capturedLabelValues; 
		}

		@Override
		public boolean include(ReferenceAndWindow<LDW> item) { 
			LDW window = item.labeledWindow;
			if (labelValue == null) {
				labelValue = window .getLabels().getProperty(labelName);
				if (labelValue == null)
					return false;
			}
			String windowLabelValue = window.getLabels().getProperty(labelName); 
			if (capturedLabelValues != null) {
				List<String> refs = capturedLabelValues.get(windowLabelValue);
				if (refs == null) {
					refs = new ArrayList<String>();
					capturedLabelValues.put(windowLabelValue, refs);
				}
				refs.add(item.reference);
			}
			return labelValue.equals(windowLabelValue);
		}
	}
	


	/**
	 * An iterator that groups the reference with the window.
	 */
	private static class DereferencingWindowIterator<LDW extends ILabeledDataWindow<?>> implements Iterator<ReferenceAndWindow<LDW>> {

		IItemReferenceIterable<LDW> itemReferenceIterable;
		Iterator<String > itemReferenceIterator;

		public DereferencingWindowIterator(IItemReferenceIterable<LDW> itemReferenceIterable, Iterable<String> references) {
			this.itemReferenceIterable = itemReferenceIterable;
			itemReferenceIterator = references.iterator();
		}

		@Override
		public boolean hasNext() {
			return itemReferenceIterator.hasNext();
		}

		@Override
		public ReferenceAndWindow<LDW> next() {
			if (!hasNext()) 
				throw new NoSuchElementException("Item exhausted from iterator");
			String ref = itemReferenceIterator.next();
			LDW w = itemReferenceIterable.dereference(ref);
			return new ReferenceAndWindow<LDW>(ref, w);
		}

	}
				
	/**
	 * Iterates over sets of values of a given label used on ILabeldDataWindows.
	 * The windows having a a given label value are all returned in sequence.  
	 * @author dawood
	 *
	 * @param <WINDATA>
	 */
	private static class ByLabelValueWindowIterator<LDW extends ILabeledDataWindow<?>> implements Iterator<LDW> {

		private Iterable<LDW> labeledDataWindows;
		private Iterator<?> windowIterator = null; 
//		String currentLabelValue = null;
//		List<String> processedLabelValues = new ArrayList<String>();
		Map<String,List<String>> capturedLabelValues = null;	// Map of label values to references.  Only keys used if itemReferenceIterable is null.
		List<String> unprocessedLabelValues = new ArrayList<String>();
		LDW nextItem = null;
		private String labelName;
		private boolean firstPass = true;
		private String firstLabelValue = null;
		private final IItemReferenceIterable<LDW> itemReferenceIterable;

		public ByLabelValueWindowIterator(Iterable<LDW> iterable, String labelName) {
			this.labeledDataWindows = iterable;
			this.labelName = labelName;
			if (iterable instanceof IItemReferenceIterable)
				itemReferenceIterable = (IItemReferenceIterable<LDW>)iterable;
			else
				itemReferenceIterable = null; 
		}


		public boolean hasNext() {
			
			while (nextItem == null) {
				
				// Make sure we have a valid iterator
				if (windowIterator == null)  {
					if (firstPass) {	
						// The first time, we scan the whole list of items capturing the labels.
						// If this wraps a referencing iterable, then we also capture the references in the map of label values to lists of references..
						capturedLabelValues = new HashMap<String,List<String>>(); 
						if (this.itemReferenceIterable == null) {
							// Use the first label encountered and capture all label values on the first time through the iterable.
							windowIterator = new FilteringIterator<LDW>(labeledDataWindows.iterator(), new LabelMatcher<LDW>( labelName, null, capturedLabelValues)); 
						} else {
							// Iterate through the references instead of the items directly, capturing the window when there is match.
							// This iterator then returns ReferenceAndWindow objects instead of LDW objects as in the non-referencing case.
							Iterator<ReferenceAndWindow<LDW>> ref = new DereferencingWindowIterator<LDW>(itemReferenceIterable, itemReferenceIterable.getReferences());	// Group window with reference
							windowIterator = new FilteringIterator<ReferenceAndWindow<LDW>>(ref, new ReferencingLabelMatcher<LDW>(labelName, null, capturedLabelValues));
						}
					} else {
						// We're looking at the 2nd of N labels
						if (unprocessedLabelValues.size() == 0)
							return false; // There are no more labels to process
						// Move to the next label. 
						String nextLabelValue = unprocessedLabelValues.remove(0);
						if (this.itemReferenceIterable == null) {
							// We reread the whole iterable only returning windows with the given label value.
							windowIterator = new FilteringIterator<LDW>(labeledDataWindows.iterator(), new LabelMatcher<LDW>( labelName, nextLabelValue, null)); 
						} else {
							// Now that we've done the first pass, we have the mapping of label values to list of references.
							// Just build an iterator over the references for this label that dereferences them into ReferenceAndWindow objects.
							List<String> refsWithLabelValues = capturedLabelValues.get(nextLabelValue);
							windowIterator = new DereferencingWindowIterator<LDW>(itemReferenceIterable, refsWithLabelValues);	// Group window with reference
						}
					}
				}

				// At this point we have an iterator over LDW or ReferenceAndWindow.
				// Test it for if it has next and set nextItem if there is a next, otherwise reset for the next labelValue.
				if (windowIterator.hasNext())  {
					if (this.itemReferenceIterable == null) { 
						nextItem = (LDW)windowIterator.next();
					} else {
						ReferenceAndWindow<LDW> raw = (ReferenceAndWindow<LDW>)windowIterator.next();
						nextItem = raw.labeledWindow;
					}
				} else { // We just finished up with all windows for a given label, so set up to go to the next label. 
					if (firstPass) {
						// Copy the set of label values into an ordered list and remove the lable we just processed.
						unprocessedLabelValues.addAll(capturedLabelValues.keySet());
						unprocessedLabelValues.remove(firstLabelValue);
						if (this.itemReferenceIterable == null)
							capturedLabelValues = null;	// Free some memory.
					}
					firstPass = false;
					windowIterator = null;
				}
			}
			assert nextItem != null;
			return true;
		}

		public LDW next() {
			if (nextItem == null  && !hasNext()) 
				throw new NoSuchElementException("Item exhausted from iterator");
			if (firstPass && firstLabelValue == null)
				firstLabelValue = nextItem.getLabels().getProperty(labelName);
			LDW t = nextItem;
			nextItem = null;
			return t;
		}
		
	}

}
