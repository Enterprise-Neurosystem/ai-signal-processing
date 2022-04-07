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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eng.aisp.SoundRecording;
import org.eng.cache.IMultiKeyCache;
import org.eng.util.DelegatingShuffleIterable;
import org.eng.util.IDereferencer;

/**
 * Extends the super class to compose SoundRecordings using the apply() method of a referencing IReferencedSoundSpec. 
 * For each reference to an IReferencedSoundSpec, the associated SoundRecording is loaded and {@link IReferencedSoundSpec#apply(SoundRecording)} is applied.
 * SoundRecordings are indirectly referenced by references to ISoundReferences.
 * <p>
 * String --dereference--> ISoundReference --dereference--> SoundRecording --apply()--> SoundRecording.
 * <p>
 * The first dereference is provided by an implementation of IDereferencer<String,ISoundReference>.
 * <br>
 * The second dereference is provided by an implementation of IDereferencer<ISoundReference, SoundRecording>.
 * <p>
 * Typical usage is to have a set of database keys or other identifiers (i.e. references) that can be used to load the stored
 * IReferencedSoundSpec (clip reference + labels + tags + segmentation + etc), which is then used to load the actual sound 
 * using the associated IReferencedSoundSpec as a key.  This allows us to, among other things, apply labels to different segments
 * of the same clip.  This can be done by having two IReferencedSoundSpec that reference the same SoundRecording, but have different
 * labels different for  segments of the clip.  
 * 
 * @author DavidWood
 *
 */
public class ComposedSoundDataSet extends DelegatingShuffleIterable<SoundRecording>  implements ISoundDataSet { 

	protected final IDereferencer<String, ? extends IReferencedSoundSpec> soundRefDeref;
	
	/**
	 * A convenience on {@link #ComposedSoundDataSet(Iterable, IDereferencer, IDereferencer, boolean, IMultiKeyCache)} using a local cache.
	 */
	public ComposedSoundDataSet(Iterable<String> references, IDereferencer<String, ? extends IReferencedSoundSpec> soundRefDeref, 
				IDereferencer<IReferencedSoundSpec, SoundRecording> recordingDereferencer, boolean applySpec) {
		this(references, soundRefDeref, recordingDereferencer, applySpec, null);
	}
	
	/**
	 * @param references the references to the IReferencedSoundSpec instances.  These are the strings passed to the soundRefDeref instance to load the IReferencedSoundSpec.
	 * @param soundRefDeref an IDereferencer that turns String references (think database keys) into IReferencedSoundSpec (think database read).
	 * @param recordingDereferencer an IDeferencer that converts and IReferencedSoundSpec into a SoundRecording.  This can optionally call the
	 * the associated {@link IReferencedSoundSpec#apply(SoundRecording)}, but know that this class will also arrange to call that method.  
	 * @param applySpec if true, then this instances {@link #dereference(String)} method will call {@link IReferencedSoundSpec#apply(SoundRecording)}, otherwise it is assumed
	 * that the given recordingDereferencer will call it.  Control over this is externalized since the method should only be called once.
	 * @param cache an optional cache to store the SoundRecordings in.  If null, a cache local to this instance is used.
	 * @see  DelegatingShuffleIterable#DelegatingShuffleIterable(Iterable, IDereferencer, IMultiKeyCache)
	 */
	public ComposedSoundDataSet(Iterable<String> references, IDereferencer<String, ? extends IReferencedSoundSpec> soundRefDeref, 
			IDereferencer<IReferencedSoundSpec, SoundRecording> recordingDereferencer, boolean applySpec, IMultiKeyCache cache) {
		super(references, new ReferencedSoundSpecDereferencer(soundRefDeref, recordingDereferencer, applySpec), cache);
		this.soundRefDeref = soundRefDeref;
	}

	/**
	 * Get all the label values present across all records for the given label name.
	 * @param labelName
	 * @return never null.
	 * @throws IOException 
	 */
	@Override
	public List<String> getLabelValues(String labelName) throws IOException {
		List<String> valueList = new ArrayList<String>();
		Set<String> valueSet = new HashSet<String>();
		for (String ref : this.getReferences()) {
			IReferencedSoundSpec dataRef = soundRefDeref.loadReference(ref);
			String value = dataRef.getLabels().getProperty(labelName);
			if (value != null)
				valueSet.add(value);
		}
		valueList.addAll(valueSet);
		return valueList;
	}


	@Override
	public List<String> getLabels() throws IOException {
		List<String> nameList = new ArrayList<String>();
		Set<String> nameSet = new HashSet<String>();
		for (String ref : this.getReferences()) {
			IReferencedSoundSpec dataRef = soundRefDeref.loadReference(ref);
			for (Object labelName : dataRef.getLabels().keySet())	// Over the label names
				nameSet.add(labelName.toString());
		}
		nameList.addAll(nameSet);
		return nameList;
	}

	@Override
	public SoundRecording loadReference(String reference) throws IOException {
		return this.dereference(reference);
	}

	

}