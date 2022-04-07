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
import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.storage.IReferencedSoundSpecStorage;
import org.eng.aisp.storage.ISoundStorage;
import org.eng.storage.StorageException;
import org.junit.Assert;
import org.junit.Test;


public abstract class AbstractComposedStoredSoundDataSetTest {

	protected abstract ISoundStorage getSoundtorage();

	protected abstract IReferencedSoundSpecStorage getSoundReferenceStorage();

	@Test
	public void testSimple() throws IOException, StorageException { 
		IReferencedSoundSpecStorage srefStorage = getSoundReferenceStorage();
		ISoundStorage soundStorage = getSoundtorage();
		srefStorage.connect();
		soundStorage.connect();
		srefStorage.clear();
		soundStorage.clear();
		
		// Rad in a known set of data.
		MetaData md = MetaData.read("test-data/chiller");

		// Store the equivalent in the storage instances.
		List<String> refs = store(srefStorage,soundStorage, md);
		
		// Create an instance to read the storage instances using the references
		ISoundDataSet dataSet = new ComposedStoredSoundDataSet(refs, srefStorage, soundStorage);

		// Make sure the data set on the storage is equivalent to the MetaData content from the file system.
		validate(md,dataSet);
		
		// Clean up
		srefStorage.clear();
		soundStorage.clear();
		srefStorage.disconnect();
		soundStorage.disconnect();
		
	}

	private void validate(MetaData expected, ISoundDataSet actual) throws IOException {
		Assert.assertTrue(actual.getLabels().equals(expected.getLabels()));
		for (String label : actual.getLabels()) 
			Assert.assertTrue(actual.getLabelValues(label).equals(expected.getLabelValues(label)));

		List<String> mdRefs = SoundTestUtils.iterable2List(expected.getReferences());
		List<String> actualRefs = SoundTestUtils.iterable2List(actual.getReferences());
		Assert.assertTrue(mdRefs.size() == actualRefs.size());

		for (int i=0 ; i<mdRefs.size(); i++) {
			String mdRef = mdRefs.get(i);
			SoundRecording mdSound = expected.readSound(mdRef);
			String actualRef = actualRefs.get(i);
			SoundRecording dataSetSound = actual.dereference(actualRef);
			Assert.assertTrue(mdSound != null);
			Assert.assertTrue(dataSetSound != null);
			Assert.assertTrue(mdSound.equals(dataSetSound));
		}
		
	}

	private List<String> store(IReferencedSoundSpecStorage srefStorage, ISoundStorage soundStorage, MetaData md) throws StorageException, IOException {

		List<String> refIDs =  new ArrayList<String>(); 

		for (IReferencedSoundSpec r : md) {
			SoundRecording sound = md.readSound(r.getReference());
			String id = soundStorage.add(sound);
			Assert.assertTrue(id != null);
			ReferencedSoundSpec sr = new ReferencedSoundSpec(r, id);
			id = srefStorage.add(sr);
			Assert.assertTrue(id != null);
			refIDs.add(id);
		}

		return refIDs;
	}

}
