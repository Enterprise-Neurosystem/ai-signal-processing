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

import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.classifier.TrainingSetInfo;
import org.eng.aisp.classifier.TrainingSetInfo.LabelInfo;
import org.eng.aisp.classifier.TrainingSetInfo.LabelValueInfo;
import org.eng.util.IShuffleIterable;
import org.eng.util.ISizedIterable;
import org.eng.util.ISizedShuffleIterable;
import org.eng.util.ShufflizingIterable;
import org.junit.Assert;
import org.junit.Test;

public class BalancedLabeledWindowShuffleIterableTest extends AbstractLabeledWindowShuffleIterableTest {

	@Override
	protected IShuffleIterable<SoundRecording> getLabeledWindowShuffleIterable(String trainingLabel, IShuffleIterable<SoundRecording> iter, int count) {
		Assert.assertTrue("Count must a multple of 2 for test to work", count % 2 == 0);
		List<SoundRecording> srList = SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, "normal", count/2, "abnormal", count/2);
		iter = new ShufflizingIterable<SoundRecording>(srList);
//		return new BalancedLabeledWindowShuffleIterable<SoundRecording>(iter, trainingLabel, true);
		return new BalancedLabeledWindowShuffleIterable<SoundRecording>(iter, trainingLabel, true);
	}
	
	@Test
	public void testUpSampling() {
		String trainingLabel = "upSampleTest";
		
		List<SoundRecording> srList = SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, "1", 1, "2", 2);
		srList.addAll(SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, "3", 3, "4", 4));
		IShuffleIterable<SoundRecording> si = new ShufflizingIterable<SoundRecording>(srList);
		BalancedLabeledWindowShuffleIterable<SoundRecording> biter = new BalancedLabeledWindowShuffleIterable<SoundRecording>(si, trainingLabel, true);
		validateLabelValueCountsVarious(biter, trainingLabel, new String[] { "1", "2", "3", "4" }, new int[] { 4,4,4,4});
		
	}
	
	@Test
	public void testDownSampling() {
		String trainingLabel = "upSampleTest";
		
		List<SoundRecording> srList = SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, "1", 1, "2", 2);
		srList.addAll(SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, "3", 3, "4", 4));
		IShuffleIterable<SoundRecording> si = new ShufflizingIterable<SoundRecording>(srList);
		BalancedLabeledWindowShuffleIterable<SoundRecording> biter = new BalancedLabeledWindowShuffleIterable<SoundRecording>(si, trainingLabel, false);
		validateLabelValueCountsVarious(biter, trainingLabel, new String[] { "1", "2", "3", "4" }, new int[] { 1,1,1,1});
		
	}

	@Test
	public void testCountedSampling() {
		String trainingLabel = "countedSampleTest";
		
		List<SoundRecording> srList = SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, "1", 1, "2", 2);
		srList.addAll(SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, "3", 3, "4", 4));
		IShuffleIterable<SoundRecording> si = new ShufflizingIterable<SoundRecording>(srList);
		BalancedLabeledWindowShuffleIterable<SoundRecording> biter = new BalancedLabeledWindowShuffleIterable<SoundRecording>(si, trainingLabel, 2);
		validateLabelValueCountsVarious(biter, trainingLabel, new String[] { "1", "2", "3", "4" }, new int[] { 2,2,2,2});
	}
	@Test
	public void testCountedSampling2() {
		String trainingLabel = "countedSampleTest";
		
		List<SoundRecording> srList = SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, "1", 1, "2", 2);
		srList.addAll(SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, "3", 3, "4", 4));
		IShuffleIterable<SoundRecording> si = new ShufflizingIterable<SoundRecording>(srList);
		BalancedLabeledWindowShuffleIterable<SoundRecording> biter = new BalancedLabeledWindowShuffleIterable<SoundRecording>(si, trainingLabel, 20);
		validateLabelValueCountsVarious(biter, trainingLabel, new String[] { "1", "2", "3", "4" }, new int[] { 20,20,20,20});
	}
	private void validateLabelValueCountsVarious(BalancedLabeledWindowShuffleIterable<SoundRecording> iter, String trainingLabel, String[] labelValues, int[] labelValueCounts) {
		List<SoundRecording> items = validateLabelValueCounts(iter,trainingLabel, labelValues, labelValueCounts,null);
		validateLabelValueCounts(iter,trainingLabel, labelValues, labelValueCounts, items);	// 2nd iteration to check repeatability.
		ISizedShuffleIterable<SoundRecording> si = iter.shuffle();								
		items = validateLabelValueCounts(si,trainingLabel, labelValues, labelValueCounts,null);		// shuffled
		validateLabelValueCounts(si,trainingLabel, labelValues, labelValueCounts, items);		// 2nd iteration on shuffled to check repeatability.
	}

	private List<SoundRecording> validateLabelValueCounts(ISizedShuffleIterable<SoundRecording> iter, String trainingLabel, String[] labelValues, int[] labelValueCounts, 
			List<SoundRecording> expectedItems) {
		Assert.assertTrue("Test is broken. these need to be the same length", labelValues.length == labelValueCounts.length);
		List<String> refList = SoundTestUtils.iterable2List(iter.getReferences());
		List<SoundRecording> srList = SoundTestUtils.iterable2List(iter);	// Only iterate the iterable once.
		Assert.assertTrue("Number of references does not match number of items", refList.size() == srList.size());
		TrainingSetInfo tsi = TrainingSetInfo.getInfo(srList);
		LabelInfo li = tsi.getLabelInfo(trainingLabel);
//		AISPLogger.logger.info(tsi.prettyFormat());
		Assert.assertTrue(li != null);
		Assert.assertTrue(li.getLabelValues().size() == labelValues.length);
		int count = 0;
		Map<String,Integer> labelCounts = new HashMap<String,Integer>();
		for (int i=0 ; i<labelValues.length ; i++) {
			LabelValueInfo lvi = li.getLabelInfo(labelValues[i]);
//			AISPLogger.logger.info("lvi=" + lvi);
			Assert.assertTrue(lvi != null);
			Assert.assertTrue(lvi.getTotalSamples() == labelValueCounts[i]);
			count += labelValueCounts[i];
		}
		Assert.assertTrue(srList.size() == count);
		if (expectedItems != null) {
			Assert.assertTrue(expectedItems.size() == srList.size());
			for (int i=0 ; i<srList.size(); i++) {
				SoundRecording sr1 = srList.get(i);
				SoundRecording sr2 = expectedItems.get(i);
				SoundClip clip1 = (SoundClip)sr1.getDataWindow();
				SoundClip clip2 = (SoundClip)sr2.getDataWindow();
				Assert.assertTrue(clip1.getInstanceID() == clip2.getInstanceID());
			}
		}
		return srList;
		
	}

}
