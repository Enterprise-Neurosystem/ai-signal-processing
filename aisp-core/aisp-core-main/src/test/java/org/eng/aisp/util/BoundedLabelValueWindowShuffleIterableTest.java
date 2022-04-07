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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eng.ENGTestUtils;
import org.eng.aisp.SoundRecording;
import org.eng.util.IShuffleIterable;
import org.eng.util.ShufflizingIterable;
import org.junit.Assert;
import org.junit.Test;


public class BoundedLabelValueWindowShuffleIterableTest extends AbstractLabeledWindowShuffleIterableTest {

	@Override
	protected IShuffleIterable<SoundRecording> getLabeledWindowShuffleIterable(String trainingLabel, IShuffleIterable<SoundRecording> iter, int count) {
		return new BoundedLabelValueWindowShuffleIterable<SoundRecording>(iter, trainingLabel, 1000);
	}
	
//	@Test
//	public void testShuffleSomeLabels() {
//		String trainingLabel = "source";
//		String[] labelValues = {"a", "a", "a", "a", "b", "b", "c", "b", "b", "b", "d", "d"};	 // 4 a's, 5 b's, 1 c, 2 d's
//		
//		List<SoundRecording> srList = ClientTestSuite.generateTestRecordings(trainingLabel, labelValues);
//		IShuffleIterable<SoundRecording> sounds = new ShufflizingIterable<SoundRecording>(srList);
//		List<String> allowedLabels = new ArrayList<String>();
//
//		// Max of 5 "b" labels
//		int count = 5;
//		allowedLabels.add("b");
//		IShuffleIterable<?> shuffled = new BoundedLabelValueWindowShuffleIterable<SoundRecording>(sounds, false, trainingLabel, count, allowedLabels);
//		shuffleTestHelper(shuffled, count);
//		Assert.assertTrue(SoundTestUtils.iterable2List(shuffled).size() == count);
//		shuffled = shuffled.shuffle();
//		Assert.assertTrue(SoundTestUtils.iterable2List(shuffled).size() == count);
//	}

	@Test
	public void testMaxLabels() {
		String trainingLabel = "source";
		String[] labelValues = {"a", "a", "a", "a", "b", "b", "c"};
		
		List<SoundRecording> srList = ENGTestUtils.generateTestRecordings(trainingLabel, labelValues);
		IShuffleIterable<SoundRecording> sounds = new ShufflizingIterable<SoundRecording>(srList);
		
		int maxLabels = 2;
		sounds = new BoundedLabelValueWindowShuffleIterable<SoundRecording>(sounds, trainingLabel, maxLabels);

		Map<String, AtomicInteger> counts = new HashMap<String,AtomicInteger>();
		int total = 0;
		for (SoundRecording sr : sounds ) {
			String labelValue = sr.getLabels().getProperty(trainingLabel);
			AtomicInteger count = counts.get(labelValue);
			if (count == null) {
				count = new AtomicInteger();
				counts.put(labelValue, count);
			}
			Assert.assertTrue(count.incrementAndGet() <= maxLabels);
			total++;
		}
		Assert.assertTrue(total == 2*maxLabels + 1);	// a, a, b, b, c
		
	}
	
	@Test
	public void testMinMaxLabels() {
		String trainingLabel = "source";
		String[] labelValues = {"a", "a", "a", "a", "b", "b", "c"};
		
		List<SoundRecording> srList = ENGTestUtils.generateTestRecordings(trainingLabel, labelValues);
		IShuffleIterable<SoundRecording> sounds = new ShufflizingIterable<SoundRecording>(srList);
		
		int minLabels = 2;
		int maxLabels = 2;
		sounds = new BoundedLabelValueWindowShuffleIterable<SoundRecording>(sounds, trainingLabel, minLabels, maxLabels, null);

		Map<String, AtomicInteger> counts = new HashMap<String,AtomicInteger>();
		int total = 0;
		for (SoundRecording sr : sounds ) {
			String labelValue = sr.getLabels().getProperty(trainingLabel);
			AtomicInteger count = counts.get(labelValue);
			if (count == null) {
				count = new AtomicInteger();
				counts.put(labelValue, count);
			}
			Assert.assertTrue(count.incrementAndGet() <= maxLabels);
			total++;
		}
		Assert.assertTrue(total == 2*maxLabels);	// a, a, b, b, 
		
	}

}
