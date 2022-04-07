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
package org.eng.aisp.segmented;

import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.storage.ISegmentedSoundStorage;
import org.eng.storage.StoredItemByIDIterable;
import org.eng.util.IShuffleIterable;
import org.eng.util.IShuffleMutator;
import org.eng.util.MutatingShuffleIterable;

/**
 * A shuffleable iterable producing SoundRecordings from segments defined in an iterable of SegmentedSoundRecordings.
 * @author DavidWood
 *
 */
public class SegmentingSoundRecordingShuffleIterable extends  MutatingShuffleIterable<SegmentedSoundRecording,SoundRecording> implements IShuffleIterable<SoundRecording> {


	public SegmentingSoundRecordingShuffleIterable(ISegmentedSoundStorage storage, Iterable<String> ids) { 
		this(new StoredItemByIDIterable<SegmentedSoundRecording>(storage, ids));
	}

	public SegmentingSoundRecordingShuffleIterable(IShuffleIterable<SegmentedSoundRecording> segmentedRecordings) {
		super(segmentedRecordings, new SegmentingMutator());
	}
	
	private static class SegmentingMutator implements IShuffleMutator<SegmentedSoundRecording, SoundRecording> {

		@Override
		public List<SoundRecording> mutate(SegmentedSoundRecording item) {
			List<SoundRecording> srList = null;
			try {
				srList = item.getSegmentedLabeledDataWindows();
			} catch (AISPException e) {
				e.printStackTrace();
			} 
			return srList;
		}

		@Override
		public boolean isUnaryMutator() {
			return false;	// Because we MAY generate 1 or more outputs for each input.
		}
		
	}


}
