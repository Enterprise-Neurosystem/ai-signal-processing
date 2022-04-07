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

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eng.ENGTestUtils;
import org.eng.aisp.SoundRecording;
import org.junit.Assert;
import org.junit.Test;


public class GroupedLabelValueWindowIterableTest {
	private static String trainingLabel = "labelValue";



	@Test
	public void testByLabelValueReferencing() {
		List<SoundRecording> srList; 
		String[] labelValues;

		labelValues = new String[] { "a", "b", "b", "c", "c" };
		srList = ENGTestUtils.generateTestRecordings(trainingLabel, labelValues); 
		testIterableByLabelHelper(srList, trainingLabel, 3);
		
		labelValues = new String[] { "a", "b", "b", "c", "d" };
		srList = ENGTestUtils.generateTestRecordings(trainingLabel, labelValues); 
		testIterableByLabelHelper(srList, trainingLabel, 4);
		
		labelValues = new String[] { "a", "b", "b", "c", "c", "b", "a" };
		srList = ENGTestUtils.generateTestRecordings(trainingLabel, labelValues); 
		testIterableByLabelHelper(srList, trainingLabel, 3);
		

		
	}


	/**
	 * Iterate through the sounds with a ByLabelValueWindowIterable and make sure the sounds
	 * come out in groups grouped by label value.
	 * @param srList
	 * @param trainingLabel 
	 */
	private void testIterableByLabelHelper(List<SoundRecording> srList, String trainingLabel, int expectedLabelValues) {
		GroupedLabelValueWindowIterable<SoundRecording> byLabelValues = new GroupedLabelValueWindowIterable<SoundRecording>(srList, trainingLabel);
		String lastLabelValue = null;
		Set<String> seenLabelValues = new HashSet<String>();
		int labelValues = 0;
		int count = 0;
		for (SoundRecording sr : byLabelValues) {
			Properties labels = sr.getLabels();
			Assert.assertTrue(labels != null && labels.size() > 0);
			String itemLabelValue = labels.getProperty(trainingLabel);
//			System.out.println((count == 0 ? "\n" : "" ) + "label = " + itemLabelValue);
			if (lastLabelValue != null) {
				if (!lastLabelValue.equals(itemLabelValue))	{
					// If we ever switch to a new label, the new label should not have been seen before
					Assert.assertTrue(!seenLabelValues.contains(itemLabelValue));
					labelValues++;
				}
			} else {
				labelValues++;
			}
			lastLabelValue = itemLabelValue;
			seenLabelValues.add(itemLabelValue);
			count++;
		}
		Assert.assertTrue(count == srList.size());
		Assert.assertTrue(labelValues == expectedLabelValues);
	}
}
