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
import java.util.Collections;
import java.util.List;

import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.dataset.LabeledSoundFiles;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.MFCCFeatureExtractor;
import org.eng.aisp.feature.pipeline.LabeledFeatureIterable;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.feature.processor.vector.DeltaFeatureProcessor;
import org.eng.aisp.feature.processor.vector.IdentityFeatureProcessor;
import org.eng.util.IShuffleIterable;
import org.junit.Assert;
import org.junit.Test;



public class LabeledFeatureIterableTest {

	/** TODO: why do we need this?  The cache should be big enough to handle 25 items, but seems to be evicting items after 20 or so. 
	 *  HMMM?  Now this is not happening anymore, so put it back to a large number (i.e. 30).  Not sure why this was a problem for a short
	 *  period of time, but for now we'll just ignore that it happened.  -dawood 8/10/2018
	 */
	private static int MAX_ITEMS_TO_ITERATE = 30;

	@Test
	public void testCounts() {
		int count = 100;
		int channels = 1;
		int samplingRate = 4000;
		int bitsPerSample = 8;
		int startMsec = 0;
		int htz= 100;
		int durationMsec = 5000;
		int subWindowMsec = 100;
		int windowShiftMsec = subWindowMsec;

		int featuresPerRecording = durationMsec/subWindowMsec;
		int spacingMsec = 0;
		String trainingLabel = "label", labelValue="value";
		List<SoundRecording> ldwList = SoundTestUtils.createTrainingRecordings(count, channels, samplingRate, 
				bitsPerSample, startMsec, durationMsec, spacingMsec, htz, trainingLabel, labelValue);
		IFeatureExtractor<double[],double[]> extractor = new MFCCFeatureExtractor(40);
		IFeatureProcessor<double[]> processor = new IdentityFeatureProcessor();
		LabeledFeatureIterable<double[], double[]> iter = new LabeledFeatureIterable<double[],double[]>(ldwList, extractor, subWindowMsec, windowShiftMsec, processor);
	
		// Go through the iterable and make sure we get 1 feature array for each SoundRecording.
		// And make sure the array lengths are correct based on the window sizes. 
		int featureCount = 0;
		for (ILabeledFeatureGram<double[]>[] lf : iter) {
			featureCount++;
			Assert.assertTrue(lf != null);
			Assert.assertTrue(lf.length == 1);
			IFeature<double[]>[] features = lf[0].getFeatureGram().getFeatures();
			Assert.assertTrue(features.length == featuresPerRecording);
		}
		Assert.assertTrue(featureCount == ldwList.size());	// We should get back one array of features for every sound recording.
		
	}
	
	/**
	 * Test feature extraction caching over items loaded from the file system. 
	 */
	@Test
	public void testWavFileCaching() throws IOException {
		// The WavFileIterable caches its referenced values which is required for this test to pass.
		IShuffleIterable<SoundRecording>  sounds = LabeledSoundFiles.loadSounds("test-data/home", true);
		cachingTestHelper(sounds);
	}



	
	private static List<Long> getFeatureIDs(LabeledFeatureIterable<double[],double[]> lfi, int count) {
		// Go through the iterator and capture the ids of the resulting feature windows.
		List<Long> ids = new ArrayList<Long>();
//		System.out.println("New iterable");
		for (ILabeledFeatureGram<double[]>[] lfa : lfi) {
			for (ILabeledFeatureGram<double[]> lf : lfa)  {
				for (IFeature<double[]> f : lf.getFeatureGram().getFeatures()) {
					Long id = f.getInstanceID();
					ids.add(id);
//					System.out.println("id="+id);
				}
			}
			count--;
			if (count == 0)
				break;
		}
		Collections.sort(ids);;
		return ids;
	}

	/**
	 * 
	 * @param sounds
	 * @param count set the number of items to iterate through. We need this because if we use
	 * all the sounds, some cache items get evicted and the item get read in again with a new insance id.
	 * @return
	 */
	private static List<Long> getWindowIDs(Iterable<SoundRecording> sounds, int count) {
		// Go through the iterator and capture the ids of the resulting feature windows.
		List<Long> ids = new ArrayList<Long>();
//		for (SoundRecording dw : sounds) {
//		System.out.println("New iterable");
		for (ILabeledDataWindow<double[]> ldw : sounds)  {
			Long id = ldw.getDataWindow().getInstanceID();
//			System.out.println("id="+id);
			ids.add(id);
			count--;
			if (count == 0)
				break;
		}
		Collections.sort(ids);;
		return ids;
	}
	
	/**
	 * Test to make sure that running through an iterable multiple times does not create any new
	 * feature instances.
	 * <p>
	 * This is exposed so that other Iterable implementation can call this (especially iterables over db sounds).
	 * @param sounds an interable over sounds that does not itself create new instances when iterated multiple times.
	 */
	public static void cachingTestHelper(Iterable<SoundRecording> sounds) {
		List<Long> ids1, ids2;
		
		// Make sure the iterable is not creating new instances.  Required for this test to be  valid.
		ids1 = getWindowIDs(sounds,MAX_ITEMS_TO_ITERATE);
		ids2 = getWindowIDs(sounds,MAX_ITEMS_TO_ITERATE);
		Assert.assertTrue(ids1.equals(ids2));

		IFeatureExtractor<double[],double[]> extractor = new MFCCFeatureExtractor();
		IFeatureProcessor<double[]> processor = new DeltaFeatureProcessor(2, new double[] { 1,1,1});
		LabeledFeatureIterable<double[], double[]> lfi1 = new LabeledFeatureIterable<double[],double[]>(sounds, extractor, 500,500, processor);

		// First test using the same feature iterable.
		ids1 = getFeatureIDs(lfi1,MAX_ITEMS_TO_ITERATE);
		ids2 = getFeatureIDs(lfi1,MAX_ITEMS_TO_ITERATE);
		Assert.assertTrue(ids1.equals(ids2));


		
	}
}
