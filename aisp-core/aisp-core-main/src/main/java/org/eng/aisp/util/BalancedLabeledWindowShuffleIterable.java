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

import java.util.Iterator;
import java.util.List;

import org.eng.aisp.ILabeledDataWindow;
import org.eng.util.IShuffleIterable;
import org.eng.util.ISizedShuffleIterable;
import org.eng.util.ShufflizingIterable;

/**
 * A shufflable iterable that allows setting the number of samples for all label values associated with a given label. 

 * @author DavidWood
 *
 * @param <ITEM>
 */
public class BalancedLabeledWindowShuffleIterable<ITEM extends ILabeledDataWindow<?>> implements ISizedShuffleIterable<ITEM> {
	

	IShuffleIterable<ITEM> iterable;
	private String labelName;
	private boolean upSample;
	private BalancedLabeledWindowIterable<ITEM> balancedIterable;
	private ShufflizingIterable<ITEM> shufflizedIterable = null;
	private int samplesPerLabelValue;

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
		this.iterable = iterable;
		this.labelName = labelName;
		this.samplesPerLabelValue = samplesPerLabelValue;
		this.upSample = upSample;
		if (samplesPerLabelValue > 0)
			balancedIterable = new BalancedLabeledWindowIterable<ITEM>(iterable,labelName, samplesPerLabelValue);
		else
			balancedIterable = new BalancedLabeledWindowIterable<ITEM>(iterable,labelName, upSample);
		// TODO: why do we do this?
//		if (samplesPerLabelValue == 0 && !upSample) {	// min label counts
//			shufflizedIterable = new ShufflizingIterable<ITEM>(balancedIterable);
//			this.balancedIterable = null;
//		}
	}

	@Override
	public ISizedShuffleIterable<ITEM> newIterable(Iterable<String> references) {
		// Not sure this is really ever called since we implement our own shuffle.
		IShuffleIterable<ITEM> shuffled = ((IShuffleIterable<ITEM>)this.iterable).newIterable(references);
		return new BalancedLabeledWindowShuffleIterable<ITEM>(shuffled, labelName, this.samplesPerLabelValue, upSample);
	}

	@Override
	public Iterator<ITEM> iterator() {
		if (balancedIterable != null)
			return balancedIterable.iterator();
		else
			return shufflizedIterable.iterator();
	}

	@Override
	public int size() {
		if (balancedIterable != null)
			return balancedIterable.size();
		else
			return shufflizedIterable.size();
	}

	
	@Override
	public ISizedShuffleIterable<ITEM> shuffle() {
		return this.shuffle(123232);
	}

	@Override
	public ISizedShuffleIterable<ITEM> shuffle(long seed) {
		IShuffleIterable<ITEM> shuffled;
		if (balancedIterable != null)
			shuffled = iterable.shuffle(seed);
		else
			shuffled = shufflizedIterable.shuffle(seed);
		return new BalancedLabeledWindowShuffleIterable<ITEM>(shuffled, labelName, this.samplesPerLabelValue, upSample);
	}

	@Override
	public Iterable<String> getReferences() {
		if (balancedIterable != null)
			return iterable.getReferences(); 
		else
			return shufflizedIterable.getReferences(); 
	}

	@Override
	public ITEM dereference(String reference) {
		if (balancedIterable != null)
			return iterable.dereference(reference); 
		else
			return shufflizedIterable.dereference(reference); 
	}

	@Override
	public List<ITEM> dereference(List<String> references) {
		if (balancedIterable != null)
			return iterable.dereference(references); 
		else
			return shufflizedIterable.dereference(references); 
	}
	

}
