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
package org.eng.aisp.classifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IClassifier;
import org.junit.Assert;

public class ModelTestUtils {



	/**
	 * Test the given classifier to distinguish 2 sets of sounds (normal and abnormal, although that distinction is not important).
	 * @param classifier
	 * @param sampleDurationMsec
	 * @param sampleCount
	 * @param amplitude1
	 * @param normalOffset
	 * @param normalHtz
	 * @param abnormalAmp
	 * @param abnormalOffset
	 * @param abnormalHtz
	 * @throws AISPException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void testSignalSeparation(IClassifier<double[]> classifier, int sampleDurationMsec, int sampleCount, double amplitude1, double offset1, int frequency1, 
			double amplitude2, double offset2, int frequency2) throws AISPException, IOException, InterruptedException {
			String trainingLabel = "source";
			// Use normal/abnormal label values so we can use this method with the anomaly classifier tests.  Otherwise, these could be any value.
			String signal1LabelValue = "normal", signal2LabelValue = "abnormal";
			Properties signal1Labels = new Properties();
			signal1Labels.put(trainingLabel, signal1LabelValue);
			Properties signal2Labels = new Properties();
			signal2Labels.put(trainingLabel, signal2LabelValue);
			Assert.assertTrue("One of these must be different", !(amplitude1 == amplitude2 && offset1 == offset2 && frequency1 == frequency2));
	
			int sampleSpacingMsec = 100;
			int sampleStartMsec = 0;
			int channels = 1;
			List<SoundRecording> srList = new ArrayList<SoundRecording>();
			int samplingRate = 44000;
			int bitsPerSample = 16;
			boolean addFreqNoise = false;
			
			// Create Signal 1 sounds
			List<SoundClip> normalClipList = SoundTestUtils.createClips(sampleCount, sampleStartMsec, sampleSpacingMsec, sampleDurationMsec, 
											channels, samplingRate, bitsPerSample, amplitude1, offset1, frequency1, addFreqNoise);
			SoundClip lastClip = null;
			for (SoundClip clip : normalClipList) {
				SoundRecording sr = new SoundRecording(clip, signal1Labels);
				srList.add(sr);
				lastClip = clip;
			}
	
			// Create Signal 2 sounds
			sampleStartMsec = (int)lastClip.getEndTimeMsec(); 
			List<SoundClip> abnormalClipList = SoundTestUtils.createClips(sampleCount, sampleStartMsec, sampleSpacingMsec, sampleDurationMsec, 
											channels, samplingRate, bitsPerSample, amplitude2, offset2, frequency2,addFreqNoise);
			for (SoundClip clip : abnormalClipList) {
				SoundRecording sr = new SoundRecording(clip, signal2Labels);
				srList.add(sr);
			}
	
			// Train the classifier
	//		System.out.println("Begin training");
			classifier.train(trainingLabel, srList);
	//		System.out.println("Done training");

			// Test the trained classifier
//			SoundTestUtils.verifyClassifications(classifier, srList, trainingLabel);

			Map<String,Classification> cmap = classifier.classify(abnormalClipList.get(0));
	//		System.out.println("Done classify 1");
			Assert.assertTrue(cmap != null);
			Classification c= cmap.get(trainingLabel);
			Assert.assertTrue(c != null);
			Assert.assertTrue(c.getLabelValue().equals("abnormal"));
	
			cmap = classifier.classify(normalClipList.get(0));
	//		System.out.println("Done classify 2");
			Assert.assertTrue(cmap != null);
			c = cmap.get(trainingLabel);
			Assert.assertTrue(c != null);
			Assert.assertTrue(c.getLabelValue().equals("normal"));
			
	
		}

}
