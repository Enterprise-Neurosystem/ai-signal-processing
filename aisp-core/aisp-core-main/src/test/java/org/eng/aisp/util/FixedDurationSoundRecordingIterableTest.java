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
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.junit.Assert;
import org.junit.Test;


public class FixedDurationSoundRecordingIterableTest extends AbstractFixedDurationLabeledWindowIterableTest {

	@Override
	protected Iterable<SoundRecording> newSubWindowingIterable(Iterable<SoundRecording> recordings, int clipMsec, PadType padType) {
		FixedDurationSoundRecordingIterable iterable = new FixedDurationSoundRecordingIterable(recordings, clipMsec, padType);
		return iterable;
	}
	
	@Test
	public void testSlidingWindows() {
		int startMsec = 0;
		int pauseMsec = 0;		// keep this 0 otherwise assert below needs to be adjusted.
		int htz = 1000;
		int count = 1;
		int sourceDurationMsec = 10 * 1000;
		int clipLen = 1000;
		int clipShift = 500;
		PadType padType = PadType.NoPad;
		Properties p = new Properties();
		p.setProperty("status", "somevalue");
		List<SoundRecording> recordings = SoundTestUtils.createTrainingRecordings(count, startMsec, sourceDurationMsec, pauseMsec, htz,p);
		FixedDurationSoundRecordingIterable iterable = new FixedDurationSoundRecordingIterable(recordings, clipLen, clipShift, padType);
		List<SoundRecording> subWindows = SoundTestUtils.iterable2List(iterable);
		Assert.assertTrue(subWindows.size() == 20-1);	// The last 1/2 second of the last segment does not start any sub-windows.
		for (SoundRecording sr : subWindows) 
			Assert.assertTrue(sr.getDataWindow().getDurationMsec() == clipLen);			
	}

}
