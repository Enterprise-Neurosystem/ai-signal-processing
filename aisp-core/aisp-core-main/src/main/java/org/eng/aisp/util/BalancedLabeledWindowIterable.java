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

import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.classifier.TrainingSetInfo;
import org.eng.aisp.classifier.TrainingSetInfo.LabelInfo;
import org.eng.aisp.classifier.TrainingSetInfo.LabelValueInfo;
import org.eng.util.IMutator;
import org.eng.util.ISizedIterable;
import org.eng.util.MutatingIterable;
import org.eng.util.MutatingIterator;

/**
 * Provides label value balancing in an iterable of labeled data windows.
 * Data may be balanced in 1 of 3 ways:
 * <ol>
 * <li> down sampled - the <b>minimum</b> count of all label values is used as the count for all labels values produced.
 * Label values that occur <b>more</b> frequently are <b>randomly excluded to reduce</b> their number to the <b>minimum</b> count.
 * <li> up sampled - the <b>maximum</b> count of all label values is used as the count for all labels values produced.
 * Label values that occur <b>less</b> frequently are <b>duplicated<b> to the <b>maximum</b> count.  A balanced copy 
 * across each window is attempted.
 * <li> by count per label value.
 * </ol>
 * @author DavidWood
 *
 * @param <ITEM>
 */
public class BalancedLabeledWindowIterable<ITEM extends ILabeledDataWindow<?>> implements ISizedIterable<ITEM> {


	protected final Iterable<ITEM> iterable;
	protected final String labelName;
	protected transient Iterable<ITEM> balancingIterable = null;
	protected final boolean upCopy;
	protected final int samplesPerLabelValue; 
	protected int size = -1;

	/**
	 * @param iterable labeled data windows to restrict to be balanced.
	 * @param labelName the label name for which the label values will be balanced.
	 * @param upCopy  if true, then up copy samples as needed so that each label value has a number of samples 
	 * equal to the maximum number of samples for any label value.  If false, then down sample randomly to bring
	 * the number of samples per label to the maximum number of samples for any label value.
	 */
	public BalancedLabeledWindowIterable(Iterable<ITEM> iterable, String labelName, boolean upCopy)  { 
		this(iterable, labelName, 0, upCopy);
	}

	/**
	 * Create the iterable to select the requested number of samples per label value.
	 * Samples will be up-sampled (copied) and down-sampled (randomly) to meet the requested number of samples per label value. 
	 * @param iterable labeled data windows to restrict to be balanced.
	 * @param labelName the label name for which the label values will be balanced.
	 * @param samplesPerLabelValue the number of samples produced for each label value assigned to the given label name.
	 */
	public BalancedLabeledWindowIterable(Iterable<ITEM> iterable, String labelName, int samplesPerLabelValue)  { 
		this(iterable, labelName, samplesPerLabelValue, false);
		if (samplesPerLabelValue <= 0)
			throw new IllegalArgumentException("Samples per label value must be 1 or larger");
	}

	/**
	 * A private constructor that takes both  samplesPerLabelValue and upCopy.
	 * @param iterable
	 * @param labelName
	 * @param samplesPerLabelValue if greater than 0 then sets the number of samples for each label value.
	 * @param upCopy if count == 0 and true, then max out the samples. if count==0 and false, then ignored if count is > 0.  
	 */
	private BalancedLabeledWindowIterable(Iterable<ITEM> iterable, String labelName, int samplesPerLabelValue, boolean upCopy) {
		this.iterable = iterable;
		this.labelName = labelName;
		this.upCopy = upCopy;
		this.samplesPerLabelValue = samplesPerLabelValue;
	}


	private static <ITEM extends ILabeledDataWindow<?>> int getMinCount(TrainingSetInfo tsi,  String labelName) {
		LabelInfo li = tsi.getLabelInfo(labelName);
		if (li == null)
			return 0;
		int minCount = Integer.MAX_VALUE;
		for (String labelValue : li.getLabelValues()) {
			int count = li.getLabelInfo(labelValue).getTotalSamples();
			if (count < minCount)
				minCount = count;
		}
		return minCount;
	}

	private class LevelingIterable extends MutatingIterable<ITEM,ITEM> implements ISizedIterable<ITEM> {

		protected final String labelName;
		protected Map<String, Double> labelValueMultiplier;
		protected Map<String,Integer> labelValueCounts = new HashMap<String,Integer>();
		protected int countPerLabelValue;
		
		public class Duplicator implements IMutator<ITEM,ITEM> {

			protected Map<String, Integer> labelValuesEmitted = new HashMap<String,Integer>();
			protected Map<String, Integer> labelValuesEncountered = new HashMap<String,Integer>();

			@Override
			public List<ITEM> mutate(ITEM item) {
				String labelValue = item.getLabels().getProperty(LevelingIterable.this.labelName);
				if (labelValue == null)
					return null;
//				if (labelValue.equals("3"))
//					labelValue = "3";	// Debugging
				Double multiplierDouble = LevelingIterable.this.labelValueMultiplier.get(labelValue);
				if (multiplierDouble == null)
					return null;		// Should never get here though.
				double multiplier = multiplierDouble;
				int emitted, encountered;
				Integer encounteredInteger = labelValuesEncountered.get(labelValue);
				if  (encounteredInteger == null) {
					encountered = 1;
					emitted = 0;
				} else {
					encountered = encounteredInteger + 1;
					emitted = labelValuesEmitted.get(labelValue);
				}
				if (emitted >= countPerLabelValue)
					return null; 	// No more for this label value
				int labelValueCount = labelValueCounts.get(labelValue);
				int nextEmittedCount;
				if (labelValueCount == encountered) {	// Last sound in the iterable with this label
					nextEmittedCount = countPerLabelValue; 
				} else {
					nextEmittedCount = (int)((encountered) * multiplier);
				}
//				int toEmitCount = Math.max(1, nextEmittedCount - emitted);
				int toEmitCount = nextEmittedCount - emitted;
				List<ITEM> dups = new ArrayList<ITEM>();
				for (int i=0 ; i<toEmitCount ; i++)
					dups.add(item);
//				AISPLogger.logger.info("Emitting " + labelValue + " " + dups.size() + " times.");
				labelValuesEncountered.put(labelValue, encountered);
				labelValuesEmitted.put(labelValue, nextEmittedCount);

				return dups;
			}
			
		}

		/**
		 * Go up to the maximum number of samples across all label values.
		 * @param iterable
		 * @param labelName
		 */
		public LevelingIterable(Iterable<ITEM> iterable, String labelName) {
			this(iterable,labelName,0);
		}

		/**
		 * Set the number of samples per label value.
		 * @param iterable
		 * @param labelName
		 * @param samplesPerLabelValue
		 */
		public LevelingIterable(Iterable<ITEM> iterable, String labelName, int samplesPerLabelValue) { 
			super(iterable, null);
			this.labelName = labelName;
			this.countPerLabelValue = samplesPerLabelValue;
		}

		/**
		 * Override to create a new mutator instance for each iterator. 
		 * This is required since the mutator is stateful and needs a new instance for each iteration.
		 */
		@Override
		public Iterator<ITEM> iterator() {
			initialize();
			IMutator<ITEM,ITEM> mutator = new Duplicator();
			return new MutatingIterator<ITEM,ITEM>(iterable.iterator(), mutator);
		}


		private void initialize() {
			if (labelValueMultiplier != null)
				return;
			labelValueMultiplier = new HashMap<String,Double>();
			TrainingSetInfo tsi = TrainingSetInfo.getInfo(iterable);

			LabelInfo li = tsi.getLabelInfo(labelName);
			if (li != null) {
				// If not set already, set the count per label value to be the maximum.
				if (this.countPerLabelValue <= 0) {
					int maxCount = 0;
					for (LabelValueInfo lvi : li) {
						if (lvi.getTotalSamples() > maxCount)
							maxCount = lvi.getTotalSamples();
					}
					this.countPerLabelValue = maxCount;
				}
				// For each label, compute the multiplier needed on the number of samples.
				for (LabelValueInfo lvi : li) {
					int count = lvi.getTotalSamples();
					String labelValue = lvi.getLabelValue();
					labelValueCounts.put(labelValue, count);
					double multiplier = (double)this.countPerLabelValue / count;
					labelValueMultiplier.put(labelValue, multiplier);
				}
			}

		}

		@Override
		public int size() {
			initialize();
			return labelValueMultiplier.size() * countPerLabelValue;
		}
		
	}
	
	@Override
	public Iterator<ITEM> iterator() {
		initialize();
		return balancingIterable.iterator();
	}


	/**
	 * 
	 */
	protected void initialize() {
		if (balancingIterable != null)
			return;
		if (samplesPerLabelValue > 0) {
			LevelingIterable li = new LevelingIterable(this.iterable, labelName, samplesPerLabelValue);
			this.size = li.size();
			this.balancingIterable = li; 
		} else if (upCopy)  {	// up sample to the maximum amount.
			LevelingIterable li = new LevelingIterable(this.iterable, labelName);
			this.size = li.size();
			this.balancingIterable = li; 
		} else {				// down sample to the minimum count.
			TrainingSetInfo trainingSetInfo = TrainingSetInfo.getInfo(iterable);
			int minCount = getMinCount(trainingSetInfo, labelName);
			if (minCount != 0) {
				this.size = minCount * trainingSetInfo.getLabelInfo(labelName).getLabelValues().size();
		    } else {
				this.size = 0;
		    }
			this.balancingIterable = new BoundedLabelValueWindowIterable<ITEM>(iterable, labelName, minCount, null);
		}
	}


	@Override
	public int size() {
		initialize();
		return this.size;
	}


//	/**
//	 * Create the iterable that will filter the given allowed labels using our fields
//	 * <ul>
//	 * <li> iterable - the original data provided to this instance
//	 * <li> labelName - the name of the label having the given allowed values.
//	 * <li> count - the number of each of the allowed labels.
//	 * </ul>
//	 * This is separated out as a separate method so that subclasses can override as necessary.
//	 * @param allowedLabelValues
//	 */
//	protected Iterable<ITEM> newBalancedIterable(List<String> allowedLabelValues) {
//	}






	
}
