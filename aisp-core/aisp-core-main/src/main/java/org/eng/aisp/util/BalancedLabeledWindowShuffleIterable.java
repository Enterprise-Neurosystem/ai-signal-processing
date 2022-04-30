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
	 * Implemented to mutate each item into 0, 1 or N copies of itself as indicated by the configuration.
	 * @param <ITEM>
	 */
	protected static class LevelingMutator<ITEM extends ILabeledDataWindow<?>> implements IShuffleMutator<ITEM,ITEM> {
		final int samplesPerLabelValue;
//		final Map<String, List<ITEM>> itemsByLabelValue;
		final Map<Integer, Integer> itemCounts; 
		final String labelName;
		public LevelingMutator(IShuffleIterable<ITEM> iterable, String labelName, int samplesPerLabelValue, boolean upSample) {
			this.labelName = labelName;
			TrainingSetInfo tsi = TrainingSetInfo.getInfo(iterable);
			LabelInfo linfo = tsi.getLabelInfo(labelName);
			if (samplesPerLabelValue <= 0) {
				int min = Integer.MAX_VALUE, max = 0;
				for (String labelValue : linfo.getLabelValues()) {
					LabelValueInfo lvi = linfo.getLabelInfo(labelValue);
					int samples = lvi.getTotalSamples();
					if (samples > max)
						max = samples;
					if (samples < min)
						min = samples;
				}
				if (upSample) 
					samplesPerLabelValue = max;
				else
					samplesPerLabelValue = min;
			}
			this.samplesPerLabelValue = samplesPerLabelValue;
			this.itemCounts = this.computeItemCounts(iterable, labelName, linfo.getLabelValues(), samplesPerLabelValue);
		}

		private Map<Integer, Integer> computeItemCounts(IShuffleIterable<ITEM> iterable, String labelName, Set<String> labelValues, int samplesPerLabelValue) {
			Map<Integer,Integer> items = new HashMap<>();
			List<String> itemLabelValues = new ArrayList<>();
			List<Integer> itemHashcodes = new ArrayList<>();
			Map<String,Integer> labelCounts = new HashMap<>();
			
//			AISPLogger.logger.info("Enter: " + TrainingSetInfo.getInfo(iterable).prettyFormat());
			for (ITEM item : iterable) {
				String labelValue = item.getLabels().getProperty(labelName);
				int hashCode = item.hashCode();
				itemLabelValues.add(labelValue);
				itemHashcodes.add(hashCode);
				if (labelValue != null) {
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
			// Create an array of counts for each item initialized to 0.
			int itemCount = itemHashcodes.size();
			List<Integer> itemCounts = new ArrayList<>();
			for (int i=0 ; i<itemCount ; i++) 
				itemCounts.add(Integer.valueOf(0));

			// Add in duplicate where necessary.
			for (String labelValue : labelCounts.keySet()) {
				boolean done = false;
				int allocated = 0;
				int index = 0;
				while (allocated < samplesPerLabelValue) {
					String thisItemLabel = itemLabelValues.get(index);
					if (thisItemLabel.equals(labelValue)) {
						int count = itemCounts.get(index) + 1;
						itemCounts.set(index, Integer.valueOf(count));
						allocated++;
					}
					index++;
					if (index == itemCount)
						index = 0;
				}
			}
			
			Map<Integer, Integer> mappedItemCounts = new HashMap<>();
			for (int i=0 ; i<itemCount ; i++) {
				Integer hashcode = itemHashcodes.get(i);
				Integer count = itemCounts.get(i);
//				AISPLogger.logger.info("item[" + i + "] count=" + count);
				mappedItemCounts.put(hashcode,count);
			}
			return mappedItemCounts;
			
		}
		
		@Override
		public List<ITEM> mutate(ITEM item) {
			int hash = item.hashCode();
			Integer count = this.itemCounts.get(hash);
			int dups = 0;
			if (count != null)
				dups = count.intValue();
			if (dups == 0)
				return null;
			List<ITEM> items = new ArrayList<>();
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
		super(iterable, new LevelingMutator(iterable,labelName, samplesPerLabelValue, upSample));

	}



}
