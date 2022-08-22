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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eng.aisp.AISPLogger;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.classifier.TrainingSetInfo;
import org.eng.aisp.classifier.TrainingSetInfo.LabelInfo;
import org.eng.aisp.classifier.TrainingSetInfo.LabelValueInfo;
import org.eng.util.IShuffleIterable;
import org.eng.util.IShuffleMutator;
import org.eng.util.ISizedShuffleIterable;
import org.eng.util.MutatingShuffleIterable;

/**
 * A shufflable iterable that allows setting the number of samples for all label values associated with a given label. 

 * @author DavidWood
 *
 * @param <ITEM>
 */
public class BalancedLabeledWindowShuffleIterable<ITEM extends ILabeledDataWindow<?>> extends MutatingShuffleIterable<ITEM, ITEM> implements ISizedShuffleIterable<ITEM> {
	

	
	/**
	 * Implemented to mutate each item into 0, 1 or N copies of itself as indicated by the up/down/count configuration.
	 * @param <ITEM>
	 */
	private static class LevelingMutator<ITEM extends ILabeledDataWindow<?>> implements IShuffleMutator<ITEM,ITEM> {

		/**
		 * A map of item hashcodes to the number of copies to mutate to for each item in the iterable.
		 */
		final Map<Integer, Integer> itemCounts; 

		public LevelingMutator(IShuffleIterable<ITEM> iterable, String labelName, int samplesPerLabelValue, boolean upSample) {
			this.itemCounts = this.computeItemCounts(iterable, labelName, samplesPerLabelValue, upSample); 
		}

		private Map<Integer, Integer> computeItemCounts(IShuffleIterable<ITEM> iterable, String labelName, int samplesPerLabelValue, boolean upSample) {
			List<String> itemLabelValues = new ArrayList<>();
			List<Integer> itemHashcodes = new ArrayList<>();
			Map<String,Integer> labelCounts = new HashMap<>();
			
			AISPLogger.logger.info("Enter: " + TrainingSetInfo.getInfo(iterable).prettyFormat());
			for (ITEM item : iterable) {
				String labelValue = item.getLabels().getProperty(labelName);
				if (labelValue != null) {
					int hashCode = item.hashCode();
					itemLabelValues.add(labelValue);
					itemHashcodes.add(hashCode);
					Integer count = labelCounts.get(labelValue);
					int icount;
					if (count == null)
						icount = 0; 
					else 
						icount = count.intValue();
					count = Integer.valueOf(icount+1); 
					labelCounts.put(labelValue, count);
				}
			}
			
			// Compute the requested samplesPerLabelValue
			if (samplesPerLabelValue <= 0) {
				int min = Integer.MAX_VALUE, max = 0;
				for (Integer count : labelCounts.values()) {
					if (count > max)
						max = count;
					if (count < min)
						min = count;
				}
				if (upSample)
					samplesPerLabelValue = max;
				else
					samplesPerLabelValue = min;
			}
				
			// Create an array of counts for each item initialized to 0.
			int itemCount = itemHashcodes.size();
			List<Integer> itemCounts = new ArrayList<>();
			for (int i=0 ; i<itemCount ; i++) 
				itemCounts.add(Integer.valueOf(0));

			// For each item, compute how many duplicates we should provide.
			// Done by, for each label value, iterating through the items perhaps more than once to
			// get the total number of instances of each item to produce the requested number of label. 
			for (String labelValue : labelCounts.keySet()) {
				int allocated = 0;
				int index = 0;
				// Loop until we've accumulated enough items having this label value.
				while (allocated < samplesPerLabelValue) {
					String thisItemLabel = itemLabelValues.get(index);
					if (thisItemLabel == null)
						index = index;
					if (thisItemLabel.equals(labelValue)) {
						// This is an item with the label, so add an instance of it to the items for this label.
						int count = itemCounts.get(index) + 1;
						itemCounts.set(index, Integer.valueOf(count));
						allocated++;
					}
					index++;
					if (index == itemCount)
						index = 0;
				}
			}
			
			// Finally, produce the map of item hashcodes to item counts.
			Map<Integer, Integer> mappedItemCounts = new HashMap<>();
			for (int i=0 ; i<itemCount ; i++) {
				Integer hashcode = itemHashcodes.get(i);
				Integer count = itemCounts.get(i);
//				AISPLogger.logger.info("item[" + i + "] count=" + count);
				mappedItemCounts.put(hashcode,count);
			}
			return mappedItemCounts;
			
		}
		
		/**
		 * Use the item's hash code to look up the number of instances/copies of this item to return.
		 */
		@Override
		public List<ITEM> mutate(ITEM item) {
			int hash = item.hashCode();
			Integer count = this.itemCounts.get(hash);
			int dups = 0;
			if (count != null)
				dups = count.intValue();
			if (dups == 0)
				return null;	// No copies of this item to be used.
			List<ITEM> items = new ArrayList<>();

			// Create the copies of this item to return.
			// For now, no need to create separate instances. 
			for (int i=0 ; i<dups ; i++)
				items.add(item);
			return items; 
		}

		@Override
		public boolean isUnaryMutator() {
			return false;
		}
		
	}

	/**
	 * 
	 * @param iterable
	 * @param labelName
	 * @param upSample if true, then up sample/copy sounds to get all sounds to the same number.  If false, then randomly de-select items
	 * so that the lower-bound of sound counts is used.  WARNING: if using false, then to support shufflability, all items are brough into memory.
	 */
	public BalancedLabeledWindowShuffleIterable(IShuffleIterable<ITEM> iterable, String labelName, boolean upSample) {
		this(iterable,labelName,0, upSample);
	}

	/**
	 * Create the iterable to select the requested number of samples per label value.
	 * Samples will be up-sampled (copied) and down-sampled (randomly) to meet the requested number of samples per label value. 
	 * @param iterable labeled data windows to restrict to be balanced.
	 * @param labelName the label name for which the label values will be balanced.
	 * @param samplesPerLabelValue the number of samples produced for each label value assigned to the given label name.
	 */
	public BalancedLabeledWindowShuffleIterable(IShuffleIterable<ITEM> iterable, String labelName, int samplesPerLabelValue) { 
		this(iterable,labelName, samplesPerLabelValue, false);
	}

	private BalancedLabeledWindowShuffleIterable(IShuffleIterable<ITEM> iterable, String labelName, int samplesPerLabelValue, boolean upSample) {
		super(iterable, new LevelingMutator<ITEM>(iterable,labelName, samplesPerLabelValue, upSample));

	}



}
