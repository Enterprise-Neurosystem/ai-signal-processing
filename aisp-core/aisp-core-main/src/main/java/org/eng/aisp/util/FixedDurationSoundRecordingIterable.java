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

import org.eng.aisp.IDataWindow;
import org.eng.aisp.SoundRecording;
import org.eng.util.MutatingIterable;

/**
 * Provides a way to iterate through subwindows of SoundRecordings found in given iterable.
 * @author dawood
 */
//public class FixedSubWindowSoundRecordingIterable extends AbstractFixedDurationLabeledSubWindowingIterable<SoundRecording> implements Iterable<SoundRecording> {
public class FixedDurationSoundRecordingIterable extends MutatingIterable<SoundRecording,SoundRecording> implements Iterable<SoundRecording> {

	/**
	 * Create the iterable to take sub windows of the given size from the SoundRecordings in the given iterable. 
	 * @param sounds iterable over SoundRecordings for which subwindows are desired.
	 * @param windowSizeMsec the size of all subwindows extracted from the SoundRecordings in the given iterable.
	 * @param windowShiftMsec the difference in time between the start of adjacent sub windows.  Set to 0 or windowSizeMsec to get rolling windows.
	 * Set to some other value (generally smaller then windowSizeMsec) to get sliding windows.
	 * @param padType
	 */
	public FixedDurationSoundRecordingIterable(Iterable<SoundRecording> sounds, double windowSizeMsec, double windowShiftMsec, IDataWindow.PadType padType) {
		super(sounds, new SoundRecordingSegmentingMutator(windowSizeMsec, windowShiftMsec, padType));
	}

	public FixedDurationSoundRecordingIterable(Iterable<SoundRecording> sounds, double windowSizeMsec, IDataWindow.PadType padType) {
		this(sounds, windowSizeMsec, 0, padType); 
	}

}
