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
import java.util.Properties;

import org.eng.aisp.IDataWindow.PadType;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.util.ShufflizingIterable;
import org.junit.Assert;
import org.junit.Test;




public abstract class AbstractFixedDurationLabeledWindowIterableTest {

	@Test
	public void testNoPadding() {
		testHelper(5000, 10000, PadType.NoPad);
	}

	@Test
	public void testPadding() {
		testHelper(5000, 10000, PadType.ZeroPad);
	}

	@Test
	public void testSubWindowing() {
		testHelper(5000, 1000, PadType.NoPad);
	}
	
	protected abstract Iterable<SoundRecording> newSubWindowingIterable(Iterable<SoundRecording> recordings, int clipMsec, PadType padType);
	
	private void testHelper(int sourceDurationMsec, int iteratedDurationMsec, PadType padType) {
		int startMsec = 0;
		int pauseMsec = 0;		// keep this 0 otherwise assert below needs to be adjusted.
		int htz = 1000;
		int count = 3;
		Properties p = new Properties();
		p.setProperty("status", "somevalue");
		List<SoundRecording> recordings = SoundTestUtils.createTrainingRecordings(count, startMsec, sourceDurationMsec, pauseMsec, htz,p);

		Iterable<SoundRecording> iterable =  newSubWindowingIterable(recordings, iteratedDurationMsec, padType);
		int expectedCount;
		switch(padType) {
			case NoPad: ; 
				if (iteratedDurationMsec > sourceDurationMsec) {
					expectedCount = 0; 
				} else {
					int nSubWindows =  sourceDurationMsec/iteratedDurationMsec;
					expectedCount = count * nSubWindows; 
				}
				break;
			case DuplicatePad:
		    case ZeroPad:
				expectedCount = count * (Math.max(1, sourceDurationMsec/iteratedDurationMsec));
				break;
			default:
				Assert.fail("Unxpected pad value");
				return;	// keeps compiler quiet.
		}

		int actualCount = 0;
		double lastEndMsec = 0; 
		for (SoundRecording sr : iterable)   {
			Assert.assertTrue(sr.getLabels().equals(p));
			SoundClip clip = sr.getDataWindow();
			if (iteratedDurationMsec < sourceDurationMsec)	{ // Doing subwindowing
				Assert.assertTrue(clip.getStartTimeMsec() == startMsec + actualCount * (iteratedDurationMsec));
			} else if (padType == PadType.NoPad) {	// Just take the clips as they come since iteratedDurationMsec >= sourceDurationMsec
				Assert.assertTrue(clip.getStartTimeMsec() == lastEndMsec); 
				Assert.assertTrue(clip.getDurationMsec() == iteratedDurationMsec);
			} else if (iteratedDurationMsec > sourceDurationMsec) {	// Some type of padding 
				if (actualCount > 0)
					Assert.assertTrue(clip.getStartTimeMsec() < lastEndMsec); 
				Assert.assertTrue(clip.getDurationMsec() == iteratedDurationMsec);
			}
			Assert.assertTrue(clip.getDurationMsec() <= iteratedDurationMsec);
			lastEndMsec = clip.getEndTimeMsec();
			actualCount++;
		}
		Assert.assertTrue(expectedCount == actualCount);
		
	}

	
	/**
	 * Make sure we get out the correct number of sounds when some of them are shorter than the clip len.
	 */
	@Test
	public void testShorterWindows() {
		int shortDurationMsec = 500;
		int longDurationMsec = 1000;
		int testDurationMsec = 750;
		int shortDurationCount = 4;
		int longDurationCount = shortDurationCount+1;
		int htz =1000;
		Properties labels = new Properties();
		boolean addNoise = false;

		List<SoundRecording> srList = SoundTestUtils.createTrainingRecordings(shortDurationCount, shortDurationMsec, htz, labels, addNoise);
		srList.addAll(SoundTestUtils.createTrainingRecordings(longDurationCount, longDurationMsec, htz, labels, addNoise));
		Iterable<SoundRecording> sounds = new ShufflizingIterable<SoundRecording>(srList);
		Iterable<SoundRecording> iterable = this.newSubWindowingIterable(sounds, testDurationMsec, PadType.NoPad);
//		IShuffleIterable<SoundRecording> shuffleIter = new ShufflizingIterable<SoundRecording>(srList); 
//		FixedDurationSoundRecordingShuffleIterable iterable = new FixedDurationSoundRecordingShuffleIterable(shuffleIter, 2*durationMsec, PadType.NoPad);
		int count = 0;
		for (@SuppressWarnings("unused") SoundRecording sr: iterable) {
			count++;
		}
		Assert.assertTrue(count == longDurationCount); 	// All windows are shorter than the testDurationMsec
		
	}
}
