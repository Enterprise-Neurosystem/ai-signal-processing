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

import org.eng.ENGException;
import org.eng.aisp.SoundTestUtils;
import org.eng.util.AbstractShuffleIterableTest;
import org.eng.util.IShuffleIterable;
import org.eng.util.ShufflizingIterable;
import org.junit.Assert;



public class SegmentingSoundRecordingShuffleIterableTest extends AbstractShuffleIterableTest {


	/** Number of sounds recordings */
	private final static int soundCount = 3;
	
	/** Number of items (SoundRecordings) the super class test will request in the tested iterable). */
	private final static int testedSegmentCount = 1 + 2 + 3;

	public SegmentingSoundRecordingShuffleIterableTest() {
		super(testedSegmentCount, testedSegmentCount);
	}

	@Override
	protected IShuffleIterable<?> getRepeatableShuffleIterable(int returnedItemCount) throws ENGException {
		Assert.assertTrue(testedSegmentCount == returnedItemCount);	// We expect this value to be passed in here.
		
		Properties globalLabels = new Properties();
		globalLabels.setProperty("global", "gvalue");
		List<SegmentedSoundRecording> ssrList = new ArrayList<SegmentedSoundRecording>();
		String labelName = "anylabelname";
		String tagIndexName = "seg-index";
		int segDurationMsec = 200;
		
		ssrList.addAll(SoundTestUtils.createSegmentedSound(1, segDurationMsec, new int[] { 1000}, globalLabels, labelName, tagIndexName));
		ssrList.addAll(SoundTestUtils.createSegmentedSound(1, segDurationMsec, new int[] { 1000, 2000}, globalLabels, labelName, tagIndexName));
		ssrList.addAll(SoundTestUtils.createSegmentedSound(1, segDurationMsec, new int[] { 1000, 2000, 3000}, globalLabels, labelName, tagIndexName));
		// For a total of testedSegmentCount = 1 + 2 + 3 SoundRecordings expected after segmenting.

		IShuffleIterable<SegmentedSoundRecording> siter = new ShufflizingIterable<SegmentedSoundRecording>(ssrList);
		IShuffleIterable<?> iterable = new SegmentingSoundRecordingShuffleIterable(siter);
		return iterable;
	}

}
