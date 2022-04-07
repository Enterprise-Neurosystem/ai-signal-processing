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

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundRecording;
import org.eng.util.IDereferencer;

/**
 * Class to convert from a reference to an IReferencedSoundSpec into the associated SoundRecording including the application of the 
 * IReferencedSoundSpec metadata.
 * <p>
 * Process (in {@link #dereference(String)}) is as follows for a given String reference to a IReferencedSoundSpec:
 * <ol>
 * <li> Load the IReferencedSoundSpec using the String reference.
 * <li> Load the SoundRecording using the IReferencedSoundSpec as a key. 
 * <li> Apply the IReferencedSoundSpec to the SoundRecording to assign labels, tags, segmentation and perhaps other operations as defined by the IReferencedSoundSpec.
 * </ol>
 *
 * @author DavidWood
 */
class ReferencedSoundSpecDereferencer implements IDereferencer<String, SoundRecording > {

	/**
	 * Given a reference String, loads an associated IReferencedSoundSpec instance.
	 */
	IDereferencer<String, ? extends IReferencedSoundSpec> soundRefDerefencer ;

	/**
	 * Given a IReferencedSoundSpec, loads an associated SoundRecording and applies the metadata from the IReferencedSoundSpec. 
	 */
	IDereferencer<IReferencedSoundSpec, SoundRecording> recordingDereferencer;

	private final boolean applySpec;

	/**
	 * Create the instance.
	 * @param soundRefDeref an IDereferencer that turns String references (think database keys) into ISoundReferences (think database read).
	 * @param recordingDereferencer an IDeferencer that converts and IReferencedSoundSpec into a SoundRecording, and ALSO calls {@link IReferencedSoundSpec#apply(SoundRecording)} 
	 * on the returned SoundRecording.
	 * @param applySpec if true, then this instances {@link #dereference(String)} method will call {@link IReferencedSoundSpec#apply(SoundRecording)}, otherwise it is assumed
	 * that the given recordingDereferencer will call it.  Control over this is externalized since the method should only be called once..
	 */
	public ReferencedSoundSpecDereferencer(IDereferencer<String, ? extends IReferencedSoundSpec> soundRefDeref, IDereferencer<IReferencedSoundSpec, SoundRecording> recordingDereferencer,
			boolean applySpec) {
		this.soundRefDerefencer = soundRefDeref;
		this.recordingDereferencer = recordingDereferencer;
		this.applySpec = applySpec;
	}

	/**
	 * Takes the given reference, loads the associated IReferencedSoundSpec, then loads the SoundRecording and applies the {@link IReferencedSoundSpec#apply(SoundRecording)} to it.
	 */
	@Override
	public SoundRecording loadReference(String reference) throws IOException {
		// Load the sound reference 
		IReferencedSoundSpec soundRef = soundRefDerefencer.loadReference(reference);
		
		// Load the recording and apply the IReferencedSoundSpec. 
		SoundRecording sr = recordingDereferencer.loadReference(soundRef);
		
		if (applySpec) {
			try {
				sr = soundRef.apply(sr);
			} catch (AISPException e) {
				throw new IOException("Could not apply " + soundRef, e);
			}
		}

		return sr;
	}
	
}