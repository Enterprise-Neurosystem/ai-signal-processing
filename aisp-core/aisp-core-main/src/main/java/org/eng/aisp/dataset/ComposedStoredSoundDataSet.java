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
import org.eng.aisp.storage.IReferencedSoundSpecStorage;
import org.eng.aisp.storage.ISoundStorage;
import org.eng.cache.IMultiKeyCache;
import org.eng.storage.StorageException;
import org.eng.util.IDereferencer;

/**
 * Extends the super class to use IReferencedSoundSpecStorage and ISoundStorage as the IReferencedSoundSpec and SoundRecording dereferencers needed by the super class.
 * @author DavidWood
 *
 */
public class ComposedStoredSoundDataSet extends ComposedSoundDataSet {

	/**
	 * Simple class to implement IDereferencer<String, IReferencedSoundSpec> using an IReferencedSoundSpecStorage instance.
	 */
	private static class SoundReferenceStorageDereferencer implements IDereferencer<String, IReferencedSoundSpec>{

		protected final IReferencedSoundSpecStorage storage;
		
		public SoundReferenceStorageDereferencer(IReferencedSoundSpecStorage storage) {
			this.storage = storage;
		}

		@Override
		public IReferencedSoundSpec loadReference(String reference) throws IOException {
			try {
				IReferencedSoundSpec ref = storage.findItem(reference);
				if (ref == null)
					throw new IOException("Item with reference " + reference + " not found.");
				return ref;
			} catch (StorageException e) {
				throw new IOException(e.getMessage(), e);
			}
		}

	}

	/**
	 * Simple class to implement IDereferencer<IReferencedSoundSpec, SoundRecording> using an ISoundStorage instance.
	 * that also call {@link IReferencedSoundSpec#apply(SoundRecording)}.
	 */
	private static class SoundStorageDereferencer implements IDereferencer<IReferencedSoundSpec, SoundRecording> {

		private ISoundStorage storage;

		public SoundStorageDereferencer(ISoundStorage storage) {
			this.storage = storage;
		}

		@Override
		public SoundRecording loadReference(IReferencedSoundSpec reference) throws IOException {
			SoundRecording sr;
			try {
				sr = storage.findItem(reference.getReference());
			} catch (StorageException e) {
				throw new IOException("Could not load sound using reference " + reference.getReference(), e);
			}
			try {
				sr = reference.apply(sr);
			} catch (AISPException e) {
				throw new IOException("Could not apply " + reference + " apply() method.", e);
			}
			return sr; 
		}

	}
	

	/**
	 *  
	 * @param references keys/references to the I
	 * @param refStorage
	 * @param soundStorage
	 */
	public ComposedStoredSoundDataSet(Iterable<String> references,
			IReferencedSoundSpecStorage refStorage,
			ISoundStorage soundStorage) {
		this(references, refStorage, soundStorage, null);
	}

	public ComposedStoredSoundDataSet(Iterable<String> references,
			IReferencedSoundSpecStorage refStorage,
			ISoundStorage soundStorage,
			IMultiKeyCache cache) {
		super(references, 
				new SoundReferenceStorageDereferencer(refStorage), 
				new SoundStorageDereferencer(soundStorage), 
				false,
				cache);
	}

}
