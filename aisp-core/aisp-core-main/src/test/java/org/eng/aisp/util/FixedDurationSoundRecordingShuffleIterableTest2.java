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

import org.eng.aisp.IDataWindow.PadType;
import org.eng.aisp.SoundRecording;
import org.eng.util.IShuffleIterable;

public class FixedDurationSoundRecordingShuffleIterableTest2 extends AbstractProxySoundRecordingIterableTest {

	@Override
	protected List<Iterable<SoundRecording>> newProxyingIterables(IShuffleIterable<SoundRecording> recordings) {
		List<Iterable<SoundRecording>> iterablesList = new ArrayList<Iterable<SoundRecording>>();
		iterablesList.add(new FixedDurationSoundRecordingShuffleIterable(recordings, 100, PadType.ZeroPad));
	
		// Create an iterable that will have to pad the iterated sounds.
		double maxMsec = 0;
		for (SoundRecording sr : recordings) {
			if (sr.getDataWindow().getDurationMsec() > maxMsec)
				maxMsec = sr.getDataWindow().getDurationMsec();
		}
		maxMsec *= 2;
		iterablesList.add(new FixedDurationSoundRecordingShuffleIterable(recordings, (int)maxMsec, PadType.ZeroPad));

		return iterablesList;
	}

}
