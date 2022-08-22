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
import org.eng.util.IMutator;
import org.eng.util.IShuffleIterable;
import org.eng.util.MutatingShuffleIterable;

/**
 * Provides a way to iterate through subwindows of SoundRecordings found in given iterable.
 * <p>
 * <b>Note:</b> This class does <b>NOT</b> currently provide repeatability of items across calls to {@link #shuffle()} or {@link #shuffle(long)}.  Instead the underlying
 * items are shuffled and then they are filtered by this class
 * @author dawood
 *
 */
public class FixedDurationSoundRecordingShuffleIterable extends MutatingShuffleIterable<SoundRecording,SoundRecording> implements IShuffleIterable<SoundRecording> {


	/**
	 * Defines a IShuffleMutator that does declares itself as NOT unary.
	 */
	protected static class Segmenter extends ShuffleMutatorProxy<SoundRecording,SoundRecording> {

		public Segmenter(IMutator<SoundRecording, SoundRecording> mutator) {
			super(mutator, false);
		}
		
	}

	/**
	 * Create the iterable to take sub windows of the given size from the SoundRecordings in the given iterable. 
	 * @param sounds iterable over SoundRecordings for which subwindows are desired.
	 * @param windowMsec the size of all subwindows extracted from the SoundRecordings in the given iterable.
	 */
	public FixedDurationSoundRecordingShuffleIterable(IShuffleIterable<SoundRecording> sounds, double windowMsec, IDataWindow.PadType padType) {
		this(sounds, windowMsec, 0, padType); 
	}

	/**
	 * 
	 * @param sounds iterable over SoundRecordings for which subwindows are desired.
	 * @param windowSizeMsec the size of all subwindows extracted from the SoundRecordings in the given iterable.
	 * @param windowShiftMsec the time between each sub-window.  Set to 0 or windowSizeMsec value to get rolling windows.  
	 * @param padType
	 */
	public FixedDurationSoundRecordingShuffleIterable(IShuffleIterable<SoundRecording> sounds, double windowSizeMsec, double windowShiftMsec, IDataWindow.PadType padType) {
		super(sounds, new Segmenter(new SoundRecordingSegmentingMutator(windowSizeMsec, windowShiftMsec, padType)));
	}



}
