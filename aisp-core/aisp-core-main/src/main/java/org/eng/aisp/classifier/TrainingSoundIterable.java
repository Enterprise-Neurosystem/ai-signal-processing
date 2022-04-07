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
package org.eng.aisp.classifier;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundRecording;
import org.eng.storage.IFieldedItemStorage;
import org.eng.storage.INamedItemStorage;
import org.eng.storage.StorageException;
import org.eng.storage.StoredItemByIDIterable;
import org.eng.util.IShuffleIterable;

/**
 * A trivial extension of the super class to allow and encourage users to use the shufflable by-ID iterables.
 * @author dawood
 *
 */
public class TrainingSoundIterable extends StoredItemByIDIterable<SoundRecording> implements IShuffleIterable<SoundRecording> {

	public TrainingSoundIterable(IFieldedItemStorage<SoundRecording> storage, Iterable<String> ids) {
		super(storage, ids);
	}

	/**
	 * Create an iterable that selects items with the given name from storage.
	 * @param storage
	 * @param itemName
	 * @throws AISPException
	 */
	public TrainingSoundIterable(INamedItemStorage<SoundRecording> storage, String itemName) throws StorageException {
		super(storage, storage.findNamedIDs(itemName));
	}
}
