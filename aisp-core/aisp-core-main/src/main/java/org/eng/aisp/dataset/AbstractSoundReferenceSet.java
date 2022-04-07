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
package org.eng.aisp.dataset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.eng.util.AbstractReferenceShuffleIterable;

/**
 * Holds labeling, tags and other metadata on a set of reference sounds.
 * @author dawood
 *
 */
//public abstract class AbstractSoundReferenceSet<DATA extends IReferencedSoundSpec, IMPL extends AbstractSoundReferenceSet<DATA,?>> extends AbstractReferenceShuffleIterable<DATA> implements IDoubleDataSet<DATA> { 
//public abstract class AbstractSoundReferenceSet<DATA extends IReferencedSoundSpec, IMPL extends AbstractSoundReferenceSet<DATA,?>> extends AbstractReferenceShuffleIterable<IReferencedSoundSpec> implements ISoundReferenceSet<IReferencedSoundSpec> { 
public abstract class AbstractSoundReferenceSet extends AbstractReferenceShuffleIterable<IReferencedSoundSpec, ISoundReferenceSet> implements ISoundReferenceSet { 

	/** Label used to capture the start time of a SoundClip in the tags fields */ 
	public final static String START_TIME_MSEC_LABEL = "__startMsec";


	/**
	 * Create an empty instance that will be written in the given directory or file.
	 * @param filePath  the location of this instance.  May be absolute or relative or null.
	 */
	public AbstractSoundReferenceSet() {
		super(); 
	}
	
//	@Override
//	public DATA getReference(String reference) {
//		return this.metaData.get(reference);
//	}
//	
//	protected abstract DATA newSoundMetaData(String reference, Properties labels, Properties tags);

//	/**
//	 * Get an iterator over the records in the meta data instance.
//	 * Labels returned are the public labels.
//	 * @return
//	 */
//	public Iterable<DATA> getRecords() {
//		List<DATA> soundMetaDatas = new ArrayList<DATA>();
//		for (String reference: this.getReferences()) {
//			DATA r = dereference(reference); 
//			if (r != null)
//				soundMetaDatas.add(r);
//		}
//		return soundMetaDatas;
//	}

//	public Iterator<DATA> iterator() {
//		return getRecords().iterator();
//	}


	/**
	 * Get all the label values present across all records for the given label name.
	 * @param labelName
	 * @return never null.
	 */
	@Override
	public List<String> getLabelValues(String labelName) {
		List<String> valueList = new ArrayList<String>();
		Set<String> valueSet = new HashSet<String>();
		for (IReferencedSoundSpec dataRef : this) {
			String value = dataRef.getLabels().getProperty(labelName);
			if (value != null)
				valueSet.add(value);
		}
		valueList.addAll(valueSet);
		return valueList;
	}


	@Override
	public List<String> getLabels() {
		List<String> nameList = new ArrayList<String>();
		Set<String> nameSet = new HashSet<String>();
		for (IReferencedSoundSpec dataRef : this) {
			for (Object labelName : dataRef.getLabels().keySet())	// Over the label names
				nameSet.add(labelName.toString());
		}
		nameList.addAll(nameSet);
		return nameList;
	}
	
	/**
	 * Add the given spec and return the reference for it.
	 * @param dataRef
	 * @return never null.
	 */
	protected abstract String add(IReferencedSoundSpec dataRef); 

	
	/**
	 * Provided to support subclass implementations of {@link #newIterable(Iterable)}, but otherwise not exposed.
	 * @param dataSet
	 * @param references
	 */
	protected AbstractSoundReferenceSet(AbstractSoundReferenceSet dataSet, Iterable<String> references) {

    }


	/**
	 * Get the labels for the reference in this instance.
	 * @param reference
	 * @return null if not found. 
	 */
	public Properties getLabels(String reference) {
		return this.getLabels(reference, false);
	}

	protected Properties getLabels(String reference, boolean includeHidden) {
		IReferencedSoundSpec dataRef = this.dereference(reference); 
		if (dataRef == null)
			return null;
		Properties labels = dataRef.getLabels(); 
		if (labels != null && !includeHidden && labels.containsKey(START_TIME_MSEC_LABEL)) {
			Properties l2 = new Properties();
			l2.putAll(labels);
			l2.remove(START_TIME_MSEC_LABEL);
			labels = l2;
		}
		return labels;
	}
	
	/**
	 * Get the tags for the reference in this instance.
	 * The given reference is used first as the key and if not found, we use the absolute path and 
	 * then the base name to look up the labels in this instance..
	 * @param reference
	 * @return null if not found or none set for the reference (this is different than labels).
	 */
	public Properties getTags(String reference) {
		return this.getTags(reference,false);
	}

	protected Properties getTags(String reference, boolean includeHidden) {
		IReferencedSoundSpec dataRef = this.dereference(reference);
		if (dataRef == null)
			return null;
		Properties tags = dataRef.getTags(); ;
		return tags;
	}
	



	// TODO: should this be promoted to the interface?
	/**
	 * A convenience on {@link #split(String, int, int)} with a fix seed so folds are easily repeatable.
	 */
	public List<ISoundReferenceSet> split(String labelName, int numberOfFolds) {
		return this.split(labelName, numberOfFolds, 123511);
	}

	/**
	 * Break this instance into the number of requested folds based on the contained label values for the given label.
	 * Each fold will have the same number of sounds (+/- 1) for each label value found for the given label name.
	 * The instances records are effectively shuffled according to the seed in order to randomly place sounds across the returned instances.
	 * The returned instances are disjoint and refer to the references. 
	 * Records without the given label name will not be included in the output.
	 * @param labelName
	 * @param numberOfFolds
	 * @param seed seed used for the shuffle operation.
	 * @return the number of requested folds.  
	 * @throws IllegalArgumentException if each label value present does not have at least numberOfFolds records. 
	 */
	protected List<ISoundReferenceSet> split(String labelName, int numberOfFolds, int seed) {
		// Create our return instances, which get filled farther down.
		List<ISoundReferenceSet> foldList = new ArrayList<>();
		for (int i=0 ; i<numberOfFolds ; i++)
			foldList.add(this.newInstance());
		
		Random rand = new Random(seed);

		// Over each label value found for the request label name.
		for (String labelValue: this.getLabelValues(labelName)) {

			// Build a list of all records for this label value.
			List<IReferencedSoundSpec> labelRecords = new ArrayList<IReferencedSoundSpec>();
			for (String key : this.getReferences()) {
				Properties labels = getLabels(key,true);
				Properties tags = getTags(key,true);
				if (labels == null)	// Should never be the case, but... 
					continue;
				String lv = labels.getProperty(labelName);
				if (lv == null)
					continue;
				if (lv.equals(labelValue)) {
					IReferencedSoundSpec spec = this.dereference(key);
					labelRecords.add(spec);
				}
			}
			if (labelRecords.size() < numberOfFolds) 
				throw new IllegalArgumentException("Instance does not have enough records for " + labelName + "=" + labelValue);

			// Shuffle the records so they are randomly placed across the output
			Collections.shuffle(labelRecords, rand);

			// Split records for this label across the output folds. 
			for  (int i=0 ; i<labelRecords.size(); i++) {
				IReferencedSoundSpec soundMetaData = labelRecords.get(i);
				int index = i % numberOfFolds;
				AbstractSoundReferenceSet dest = (AbstractSoundReferenceSet) foldList.get(index);
				dest.add(soundMetaData);
			}
		}	
		return foldList;
	}

	/**
	 * Create a new empty instance.
	 * @return
	 */
	protected abstract AbstractSoundReferenceSet newInstance();

	// TODO: should this be promoted to the interface?
	/**
	 * Create a new instance that contains records in which the given training label has one of the values given. 
	 * @param labelName
	 * @param allowedLabelValues list of label values for the given labelName in the returned instance.
	 * @return
	 */
	public ISoundReferenceSet selectOnLabelValues(String labelName, List<String> allowedLabelValues) {
		List<String> matchingRefs = new ArrayList<String>();
		for (String ref : this.getReferences()) {
			Properties labels = this.getLabels(ref);
			Properties tags = this.getTags(ref);
			String value = labels.getProperty(labelName);
			if (value != null && allowedLabelValues.contains(value))
				matchingRefs.add(ref);
		}
		ISoundReferenceSet md = (ISoundReferenceSet)this.newIterable(matchingRefs);
		return md;
	}

	@Override
	public IReferencedSoundSpec loadReference(String reference) throws IOException {
		return this.dereference(reference);
	}




	
}
