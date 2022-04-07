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
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.classifier.TrainingSetInfo;
import org.eng.aisp.classifier.TrainingSetInfo.LabelInfo;
import org.eng.util.DelegatingShuffleIterable;
import org.eng.util.IShuffleIterable;
import org.eng.util.ShufflizingIterable;

public class Partitioner {

	/**
	 * Partition the given data into N partitions each having the same number (+/- 1) of label values for the given label name.
	 * @param labeledData iterable of labeled data to partition.
	 * @param labelName the name of the label used to extract label values from the LDW.
	 * @param partitions the number of partitions to create.  Must be larger than 1.
	 * @param clipLenMsec currently not used.
	 * @return a list of iterables, each iterable corresponding to one of the partitions generated. If the given instance is actually an IShuffleIteable, the elements of the returned
	 * list wil also be IShuffleIterable instances.
	 * @throws AISPException if data contains no data with the given label name.
	 * @param <LDW>
	 */
	public static <WINDATA, LDW extends ILabeledDataWindow<WINDATA>> List<Iterable<LDW>> partition(Iterable<LDW> labeledData, String labelName, int partitions, int clipLenMsec) throws AISPException {
		if (partitions <= 1) 
			throw new IllegalArgumentException("The number of partitions must be larger than 1");
		
		// If possible, always try to return shuffle iterables.
		if (labeledData instanceof IShuffleIterable)
			return partition((IShuffleIterable)labeledData, labelName, partitions, clipLenMsec);

		TrainingSetInfo tsi = TrainingSetInfo.getInfo(labeledData);
		LabelInfo linfo = tsi.getLabelInfo(labelName);
		if (linfo == null)
			throw new AISPException("Data does not contain and data with label " + labelName);
		
		// Create initial empty partitions.
		List<List<LDW>>  partitionList = new ArrayList<>();
		for (int i=0 ; i<partitions ; i++) 
			partitionList.add(new ArrayList<>());
		
		// Build a map of labelValues to lists of LDW with the that label value.
		Map<String, List<LDW>> labelValues = new HashMap<String,List<LDW>>();
		for (LDW ldw : labeledData)  {
			Properties labels = ldw.getLabels();
			add(labelValues, labelName, labels, ldw);
		}
		if (labelValues.isEmpty())
			throw new AISPException("Data does not contain and data with label " + labelName);
		
		// Distributed the LDW across the partitions
		for (String labelValue : labelValues.keySet()) {
			List<LDW> ldwList = labelValues.get(labelValue);
			if (ldwList.size() >= partitions) 
				distributeRoundRobin(partitionList, ldwList);
		}
		
		// Convert to defined return type (can't figure out how to avoid this).
		boolean isShufflable = labeledData instanceof IShuffleIterable;
		List<Iterable<LDW>>  partitionIterables = new ArrayList<>();
		for (int i=0 ; i<partitions ; i++)  {
			Iterable<LDW> part = partitionList.get(i);
			if (isShufflable)
				part = new ShufflizingIterable<LDW>(part);
			partitionIterables.add(part);
		}
		return partitionIterables;
		
	}

	/**
	 * A version of {@link #partition(Iterable, String, int, int)} that accepts and generated IShuffleIterables.
	 * <b>
	 * See {@link #partition(Iterable, String, int, int)} 
	 */
	public static <WINDATA, LDW extends ILabeledDataWindow<WINDATA>> List<IShuffleIterable<LDW>> partition(IShuffleIterable<LDW> labeledData, String labelName, int partitions, int clipLenMsec) throws AISPException {
		if (partitions <= 1) 
			throw new IllegalArgumentException("The number of partitions must be larger than 1");

		// Create initial empty partitions.
		List<List<String>>  partitionList = new ArrayList<>();
		for (int i=0 ; i<partitions ; i++) 
			partitionList.add(new ArrayList<>());

		// Build a map of labelValues to lists of references to LDW with the that label value.
		Map<String, List<String>> labelValues = new HashMap<String,List<String>>();
		for (String ref : labeledData.getReferences()) {
			LDW ldw = labeledData.dereference(ref); 
			Properties labels = ldw.getLabels();
			add(labelValues, labelName, labels, ref);
		}
		if (labelValues.isEmpty())
			throw new AISPException("Data does not contain and data with label " + labelName);	
		
		// Distributed the references across the partitions
		for (String labelValue : labelValues.keySet()) {
			List<String> refList = labelValues.get(labelValue);
			if (refList.size() >= partitions) 
				distributeRoundRobin(partitionList, refList);
		}
		
		// For each partition, build a shuffle iterable of the references that can be dereferenced by the original labeledData
		List<IShuffleIterable<LDW>>  partitionIterables = new ArrayList<>();
		for (int i=0 ; i<partitions ; i++)  {
			List<String> references = partitionList.get(i);
			IShuffleIterable<LDW> si = new DelegatingShuffleIterable<LDW>( references, labeledData);
			partitionIterables.add(si);
		}
		return partitionIterables;
		
	}

	/**
	 * Add the item to the list of items for the given label value for he given labelName in the given labels.
	 * @param <ITEM>
	 * @param labelValues map of label values to lists of items with that label value.
	 * @param labelName name of label to use to get lable value from given Properties of labels.
	 * @param labels all labels associate with the given item.
	 * @param item item to add to a list if the labels contain a the given labelName.
	 */
	private static <ITEM> void add(Map<String, List<ITEM>> labelValues, String labelName, Properties labels , ITEM item) {
		List<ITEM> itemList = getListForLabelValue(labelValues, labels, labelName);
		if (itemList != null)
			itemList.add(item);
	}

//	private static <LDW extends ILabeledDataWindow<?>> void add(Map<String, List<LDW>> labelValues, String labelName, LDW ldw) {
//		Properties labels = ldw.getLabels();
//		add(labelValues, labelName, labels, ldw);
////		List<LDW> ldwList = getListForLabelValue(labelValues, labels, labelName);
////		if (ldwList != null)
////			ldwList.add(ldw);
//	}

	/**
	 * Get the list of ITEMs for the given label value in the given map.  
	 * @param <LDW>
	 * @param labelValues
	 * @param labels
	 * @param labelName
	 * @return null if the labelName is not found in the labels.
	 */
	private static <ITEM>  List<ITEM> getListForLabelValue(Map<String, List<ITEM>> labelValues, Properties labels, String labelName) {
		String labelValue = labels.getProperty(labelName);
		if (labelValue == null)
			return null;
		List<ITEM> ldwList = labelValues.get(labelValue);
		if (ldwList == null) {
			ldwList = new ArrayList<ITEM>();
			labelValues.put(labelValue, ldwList);
		}
		return ldwList;
	}


	
	private static <ITEM> void distributeRoundRobin(List<List<ITEM>> partitionList, List<ITEM> itemList) {
		int partitions = partitionList.size();
		// Distribute list of LDW ith the current label value across the partitions in a round-robin fashion.
		int partitionIndex = 0;
		for (ITEM item : itemList) {
			partitionList.get(partitionIndex).add(item);
			partitionIndex++;
			if (partitionIndex == partitions)
				partitionIndex = 0;
		}
	}


}
