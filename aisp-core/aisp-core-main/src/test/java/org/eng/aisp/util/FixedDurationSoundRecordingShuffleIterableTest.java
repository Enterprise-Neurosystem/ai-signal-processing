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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.IDataWindow.PadType;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.util.IShuffleIterable;
import org.eng.util.ShufflizingIterable;
import org.junit.Assert;
import org.junit.Test;

public class FixedDurationSoundRecordingShuffleIterableTest extends FixedDurationSoundRecordingIterableTest {

	@Test
	public void testShufflingSubWindows() {
		int count = 10;
		int durationMsec = 5000;
		int subClipMsec = 1000;
		int subClipsPerClip = durationMsec / subClipMsec;
		int expectedSubClips = count * subClipsPerClip;
		String tagName = "index";
		
		// Generate a map of sounds recordings, each recording from a different sensor, mapped by the sensorID.
		List<SoundRecording>  refMap = new ArrayList<SoundRecording>();
		for (int i=0 ; i<count ; i++ ) {
			String index = String.valueOf(i);
			Properties tags  = new Properties();
			tags.setProperty(tagName, index);
			SoundClip clip = SoundTestUtils.createClips(1, 0, 0, durationMsec, 0).get(0); 
			SoundRecording recording = new SoundRecording(clip, null, tags);
			refMap.add(recording);
		}
	
//		IShuffleIterable<SoundRecording> shuffleIterable = new TestShuffleIterable(refMap.keySet(), refMap); 
		IShuffleIterable<SoundRecording> shuffleIterable = new ShufflizingIterable<SoundRecording>(refMap); 
		IShuffleIterable<SoundRecording> subIterable = new FixedDurationSoundRecordingShuffleIterable(shuffleIterable, subClipMsec, PadType.ZeroPad);

		// First go through the unshuffled subIterable to make sure ids and counts are correct.
		int counter = 0;
		int index = 0;
		for (SoundRecording sr : subIterable) {
			String expectedIndex = String.valueOf(index);
			String actualIndex = sr.getTag(tagName);
			Assert.assertTrue(actualIndex!= null);
			Assert.assertTrue(actualIndex.equals(expectedIndex)); 
			Assert.assertTrue(sr.getDataWindow().getDurationMsec() <= subClipMsec);
			counter++;
			if (counter % subClipsPerClip == 0)
				index++;
		}
		Assert.assertTrue(counter == expectedSubClips);
		
//		counter = 0;
//		Map<String, AtomicInteger> countsByID = new HashMap<String, AtomicInteger>();	// Use AtomicInteger because it is mutable
//		subIterable = subIterable.shuffle();
//
//		for (String id : countsByID.keySet()) {
//			AtomicInteger sensorCount = countsByID.get(id);
//			Assert.assertTrue(sensorCount.get() == subClipsPerClip);
//		}
	}

	@Override
	protected Iterable<SoundRecording> newSubWindowingIterable(Iterable<SoundRecording> recordings, int clipMsec, PadType padType) {
//		Map<String, SoundRecording> referenceMap = new HashMap<String,SoundRecording>();
//		int index = 0;
//		for (SoundRecording sr : recordings) { 
//			referenceMap.put(String.valueOf(index), sr);
//			index++;
//		}
//		TestShuffleIterable shuffleIter = new TestShuffleIterable(referenceMap.keySet(), referenceMap);
		IShuffleIterable<SoundRecording> shuffleIter = new ShufflizingIterable<SoundRecording>(recordings); 
		
		FixedDurationSoundRecordingShuffleIterable iterable = new FixedDurationSoundRecordingShuffleIterable(shuffleIter, clipMsec, padType);
		return iterable;
	}

	@Test
	public void testShorterWindows() {
		int durationMsec = 500;
		int htz =1000;
		Properties labels = new Properties();
		boolean addNoise = false;
		List<SoundRecording> srList = SoundTestUtils.createTrainingRecordings(4, durationMsec, htz, labels, addNoise);
		IShuffleIterable<SoundRecording> shuffleIter = new ShufflizingIterable<SoundRecording>(srList); 
		FixedDurationSoundRecordingShuffleIterable iterable = new FixedDurationSoundRecordingShuffleIterable(shuffleIter, 2*durationMsec, PadType.NoPad);
		int count = 0;
		for (SoundRecording sr: iterable) {
			count++;
		}
		Assert.assertTrue(count == 0); 
		
	}
}
