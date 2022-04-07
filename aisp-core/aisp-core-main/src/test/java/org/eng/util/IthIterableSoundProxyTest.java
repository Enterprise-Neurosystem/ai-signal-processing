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
package org.eng.util;

import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.SoundRecording;
import org.eng.aisp.util.AbstractProxySoundRecordingIterableTest;
import org.junit.Assert;

public class IthIterableSoundProxyTest extends AbstractProxySoundRecordingIterableTest {

	@Override
	protected List<Iterable<SoundRecording>> newProxyingIterables(IShuffleIterable<SoundRecording> recordings) {
		List<Iterable<SoundRecording>> iterablesList = new ArrayList<Iterable<SoundRecording>>();
		int count = 0;
		for (String ref : recordings.getReferences())
			count++;
		Assert.assertTrue(count > 4);	// Otherwise numbers below probably won't work
		iterablesList.add(new IthIterable<SoundRecording>(recordings, 2, 1, false, count/2, 2));
		iterablesList.add(new IthIterable<SoundRecording>(recordings, 2, 1, true, count/2, 2));
		return iterablesList;
	}

	

	
}
