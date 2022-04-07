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

import java.util.List;

import org.eng.ENGException;
import org.eng.aisp.AISPException;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.storage.ISoundStorage;
import org.eng.storage.StorageException;
import org.eng.storage.StoredItemByIDIterable;
import org.eng.util.IShuffleIterable;
import org.eng.util.IthShuffleIterable;
import org.junit.After;

public abstract class AbstractShuffledStoredWindowIterableTest  extends AbstractLabeledWindowShuffleIterableTest {

	private List<String> testIDs = null;;
	
	@After	// after each test
	public void afterTest() throws StorageException {
		if (testIDs != null)  {
			this.getSoundStorage().connect();
			this.getSoundStorage().delete(testIDs);
			testIDs = null;
		}
	}

	@Override
	protected IShuffleIterable<SoundRecording> getLabeledWindowShuffleIterable(String trainingLabel, IShuffleIterable<SoundRecording> iter, int count) throws ENGException {
		ISoundStorage storage = getSoundStorage();
		try {
			storage.connect();
			storage.clear();
			testIDs = SoundTestUtils.storeItems(storage, iter);
		} catch (StorageException e) {
			throw new ENGException("Could not initialize db and/or store items", e);
		}
		IShuffleIterable<SoundRecording> stored = new StoredItemByIDIterable<SoundRecording>(storage, testIDs);
		IShuffleIterable<SoundRecording> ith = new IthShuffleIterable<SoundRecording>(stored, 2,0, true);
		return ith; 
	}

	protected abstract ISoundStorage getSoundStorage();

}
