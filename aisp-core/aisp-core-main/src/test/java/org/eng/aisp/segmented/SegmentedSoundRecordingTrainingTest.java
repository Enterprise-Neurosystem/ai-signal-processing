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

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.TrainingSetInfo;
import org.eng.aisp.classifier.TrainingSetInfo.LabelInfo;
import org.eng.aisp.classifier.gmm.GMMClassifierBuilder;
import org.eng.aisp.storage.ISegmentedSoundStorage;
import org.eng.storage.StorageException;
import org.eng.util.IShuffleIterable;
import org.eng.util.ShufflizingIterable;
import org.junit.Assert;
import org.junit.Test;


public class SegmentedSoundRecordingTrainingTest {

	
	@Test
	public void testShuffledSegmentedSoundRecordingTrainingFromMemory() throws StorageException, IOException, AISPException {
		helperShuffledSegmentedSoundRecordingTraining(null);
	}



	protected static void helperShuffledSegmentedSoundRecordingTraining(ISegmentedSoundStorage storage) throws StorageException, IOException, AISPException {
		int count = 5;
		int segDurationMsec = 1000;
		int htz[] = new int[] { 1000, 4000, 10000 };
		int subSegmentCount = htz.length;		// Number of segments defined within a recording.
		String trainingLabel = "htz"; 
		String globalLabel = "global-label";
		Properties globalLabels = new Properties();
		globalLabels.setProperty(trainingLabel, "to-be-ignored");
		globalLabels.setProperty(globalLabel, "global-label-value");
		String tagIndexName = "seg-index-tag";
				
		// Create the segmented sounds recordings in which each segment is at a different frequency and labeled as such
		List<SegmentedSoundRecording> sounds = SoundTestUtils.createSegmentedSound(count, segDurationMsec, htz, globalLabels, trainingLabel, tagIndexName); 

		SegmentingSoundRecordingShuffleIterable segmentedSounds; 
		if (storage != null) {
			String partition1 = "junit-part1";
			String partition2 = "junit-part2";
			
			storage.connect();
			storage.clear();

			// Add some other sounds which should not be included (see the counting below).
			List<SegmentedSoundRecording> otherSounds = SoundTestUtils.createSegmentedSound(count, segDurationMsec, htz, globalLabels, trainingLabel, tagIndexName); 
			List<String> ids1 = SoundTestUtils.storeItems(storage, otherSounds);
			Assert.assertTrue(ids1.size() == count);	// Make sure they got stored

			// Build the iterable of the segments over the sounds we care about.
			List<String> ids2 = SoundTestUtils.storeItems(storage, sounds);
			Assert.assertTrue(ids2.size() == count);	// Make sure they got stored, otherwise test can't succeed.
			segmentedSounds = new SegmentingSoundRecordingShuffleIterable(storage, ids2);
		} else {
			// Create the iterable that will extract the sub-segments for training.
			IShuffleIterable<SegmentedSoundRecording> shuffled = new ShufflizingIterable(sounds);
			segmentedSounds = new SegmentingSoundRecordingShuffleIterable(shuffled);
		}
	
		// Make sure we have the expected number of extracted sub-segments and labeling. 
		TrainingSetInfo tsi = TrainingSetInfo.getInfo(segmentedSounds);
		Assert.assertTrue(tsi.getTotalSamples() == count * subSegmentCount);
		Assert.assertTrue(tsi.getTotalMsec() == count * subSegmentCount * segDurationMsec);
		LabelInfo linfo = tsi.getLabelInfo(trainingLabel);
		Assert.assertTrue(linfo.getTotalSamples() == count * subSegmentCount);
		Assert.assertTrue(linfo.getLabelInfo("1000").getTotalMsec() == count * segDurationMsec);
		Assert.assertTrue(linfo.getLabelInfo("4000").getTotalMsec() == count * segDurationMsec);
		Assert.assertTrue(linfo.getLabelInfo("10000").getTotalMsec() == count * segDurationMsec);
		// Make sure the global label got copied.
		linfo = tsi.getLabelInfo(globalLabel);
		Assert.assertTrue(linfo.getTotalSamples() == count * subSegmentCount);

		// Check the expected tags. Segment index is 0,1,...subSegmentCount,0,1,...
		int index = 0;
		for (SoundRecording sr : segmentedSounds) {
			String tagIndexValue = sr.getTag(tagIndexName);
			Assert.assertTrue(tagIndexValue != null);
			Assert.assertTrue(tagIndexValue.equals(String.valueOf(index)));
			index++;
			if (index == subSegmentCount)
				index = 0;	// Move to the next recording.
		}
		
		// Create any classifier on which we will train a model.
		IClassifier<double[]> classifier = new GMMClassifierBuilder().build();
		
		// Train on the separately labeled sub-segments.
		classifier.train(trainingLabel, segmentedSounds);
		
		// Create one test data for each frequency.
		List<SoundRecording> testData = SoundTestUtils.createTrainingData(1, segDurationMsec, htz, trainingLabel);

		// Verify that the classifier recognizes the test sounds which should match the segments extracted and tested on above during train()
		SoundTestUtils.verifyClassifications(classifier, testData, trainingLabel);

		if (storage != null) {
			storage.connect();
			storage.clear();
		}
	}

}
