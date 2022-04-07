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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eng.AbstractObjectTest;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;

public class SegmentedSoundRecordingTest extends AbstractObjectTest {

	@Override
	protected Cloneable getCloneable() {
		return null;
	}

	@Override
	protected Object getTestObject() {
		return getSerializableTestObject();
	}

	
//	/**
//	 * Verify that previously stored serializations work, and regenerate them if they have been removed.
//	 */
//	@Test
//	public void testPastSerialize() throws ClassNotFoundException, IOException {
//
//		SegmentedSoundRecording ssr = getSerializedTestObject();
//		List<Serializable> slist = ENGTestUtils.verifyPastSerializations(SerializationDir, ssr, false);
//		if (slist.size() == 0) {
//			AISPLogger.logger.info("Regenerating serialization of " + ssr.getClass().getSimpleName());
//			ENGTestUtils.generateSerialization(SerializationDir, ssr);
//		}
//		slist = ENGTestUtils.verifyPastSerializations(SerializationDir, ssr, true);
//		Assert.assertTrue("Expected only 1 object", slist.size() == 1);
//		SegmentedSoundRecording deserialized = (SegmentedSoundRecording)slist.get(0); 
//		Assert.assertTrue(ssr.equals(deserialized));
//	}
	

	/**
	 * @return
	 */
	@Override
	protected SegmentedSoundRecording getSerializableTestObject() {
		Properties labels = new Properties();
		labels.setProperty("label1", "1");
		SoundRecording sr = SoundTestUtils.createTrainingRecordings(1, 0, 1000, 0, 1000, labels).get(0);
		sr.addTag("tag1", "tag1");
		sr.addTag("tag2", "tag2");
		
		List<LabeledSegmentSpec> segList= new ArrayList<LabeledSegmentSpec>();
		labels = new Properties();
		labels.setProperty("seglabel1", "seg1");
		Properties tags = new Properties();
		tags = new Properties();
		tags .setProperty("segtag1", "tag1seg1");
		segList.add(new LabeledSegmentSpec(1,100, labels,tags));
		SegmentedSoundRecording ssr = new SegmentedSoundRecording(sr, segList);
		return ssr;
	}



}
