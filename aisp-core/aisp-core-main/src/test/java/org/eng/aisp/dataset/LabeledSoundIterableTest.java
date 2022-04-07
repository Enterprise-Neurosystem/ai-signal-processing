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
package org.eng.aisp.dataset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.SoundRecording;
import org.eng.util.IShuffleIterable;
import org.junit.Assert;
import org.junit.Test;


public class LabeledSoundIterableTest {


	/**
	 * Make sure the caching of windows within the iterable/iterator is working.
	 * We could generalize this test to allow easy support of the StoredItemByIDIterable, but since
	 * they share the same code we'll pass for now.
	 * @throws IOException
	 */
	@Test
	public void testCaching() throws IOException {
		// Load some sounds
		IShuffleIterable<SoundRecording>  sounds = LabeledSoundFiles.loadSounds("test-data/chiller", true);
		
		// Get the instance ids of the windows.
		List<Long> ids = new ArrayList<Long>();
		for (SoundRecording sr : sounds) {
			long id = sr.getDataWindow().getInstanceID();
			ids.add(id);
		}

		// Go through the iterable again and make sure we get back the same instances.
		int index = 0;
		for (SoundRecording sr : sounds) {
			long id = sr.getDataWindow().getInstanceID();
			Assert.assertTrue(id == ids.get(index));
			index++;
		}
	}
}
