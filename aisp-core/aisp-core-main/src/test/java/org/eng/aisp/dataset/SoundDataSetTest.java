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
import java.util.List;

import org.eng.ENGTestUtils;
import org.eng.aisp.SoundTestUtils;
import org.junit.Assert;
import org.junit.Test;


public class SoundDataSetTest {


	/**
	 * Make sure the caching of windows within the iterable/iterator is working.
	 * We could generalize this test to allow easy support of the StoredItemByIDIterable, but since
	 * they share the same code we'll pass for now.
	 * @throws IOException
	 */
	@Test
	public void testLabels() throws IOException {
		// Load some sounds
		ISoundDataSet  sounds = LabeledSoundFiles.loadMetaDataSounds(ENGTestUtils.GetTestData("chiller"), true);
		List<String> values; 
	
		Assert.assertTrue(sounds.size() == 10);

		values = sounds.getLabels();
		SoundTestUtils.validateList(values, new String[] { "source", "status", "symptom", "age" });

		values = sounds.getLabelValues("status");
		SoundTestUtils.validateList(values, new String[] { "normal", "noisy" });

		values = sounds.getLabelValues("source");
		SoundTestUtils.validateList(values, new String[] { "HVAC1", "HVAC2", "HVAC3" });

	}
}
