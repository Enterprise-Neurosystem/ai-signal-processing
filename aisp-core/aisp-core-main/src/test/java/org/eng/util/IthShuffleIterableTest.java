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
package org.eng.util;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.IDataWindow.PadType;
import org.eng.aisp.classifier.TrainingSetInfo;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.util.FixedDurationSoundRecordingShuffleIterable;
import org.junit.Assert;
import org.junit.Test;



public class IthShuffleIterableTest extends IthIterableTest {
	
	public IthShuffleIterableTest() {
		super(false);
	}
	
	/**
	 * Get the iterable to test for Ith'ness.
	 * @param <ITEM>
	 * @param iterable
	 * @param setSize
	 * @param setIndex
	 * @param excludeIndex
	 * @param maxItemCount
	 * @param firstN
	 * @return
	 */
	protected <ITEM> Iterable<ITEM> getIthIterable(Iterable<ITEM> iterable, int setSize, int setIndex, boolean excludeIndex, int maxItemCount, int firstN) {
		IShuffleIterable<ITEM> si = new ShufflizingIterable<ITEM>(iterable);
//		si = si.shuffle();
		IShuffleIterable<ITEM> isi = new IthShuffleIterable<ITEM>(si, setSize, setIndex, excludeIndex, maxItemCount, firstN);
		isi = isi.shuffle();
		return isi;
	}

	/**
	 * Get an iterable that returns all items up to the given max.
	 * @param <ITEM>
	 * @param iterable
	 * @param maxItemCount
	 * @return
	 */
	protected <ITEM> Iterable<ITEM> getIthIterable(Iterable<ITEM> iterable, int maxItemCount) {
		IShuffleIterable<ITEM> si = new ShufflizingIterable<ITEM>(iterable);
		si = new IthShuffleIterable<ITEM>(si, maxItemCount);
		si = si.shuffle();
		return si;
	}
	
	/**
	 * Override to sort the items in the test iterable and then call super class with the sorted items.
	 */
	protected void validateIterable(Iterable<Integer> ith, List<Integer> expectedData) {
		List<Integer> ithInts = SoundTestUtils.iterable2List(ith);
		Collections.sort(ithInts);
		Collections.sort(expectedData);
		super.validateIterable(ithInts, expectedData);
	}
	
	/**
	 * Set the FixedDurationSoundRecordingIterable to break the original N sounds in M sounds (M>N)
	 * and be sure the the IthShuffleIterable produces the correct number of items both shuffled and unshuffled.
	 */
	@Test
	public void testMultiplyingIterable() {
		int htz = 1000;
		boolean addNoise = false;
		Properties labels = new Properties();
		
		int durationMsec = 3000;
		int fixedMsec = 1100;	// smaller than durationMsec
		int foldCount = 4;
		int soundCount = foldCount * 2;
		// WIth pad type other than None, we will include the last partial clip, thus the +1.
		int expectedFixedCount = soundCount * (1 + durationMsec / fixedMsec);
		// Each folds cont
		int expectedFoldCount = expectedFixedCount / foldCount; 
		
		Iterable<SoundRecording> srList= SoundTestUtils.createTrainingRecordings(soundCount, durationMsec, htz, labels, addNoise);
		IShuffleIterable<SoundRecording> sounds = new ShufflizingIterable<SoundRecording>(srList); 
		TrainingSetInfo tsi = TrainingSetInfo.getInfo(sounds);
		System.out.println("Initial: \n" + tsi.prettyFormat());
		Assert.assertTrue(tsi.getTotalSamples() == soundCount);	// required for test to work.

		// This iterable should produce more items out of the original sounds, because fixedMsec < durationMsec.
		IShuffleIterable<SoundRecording> fixed = new FixedDurationSoundRecordingShuffleIterable(sounds, fixedMsec, PadType.DuplicatePad);
		tsi = TrainingSetInfo.getInfo(fixed);
		System.out.println("Fixed : \n" + tsi.prettyFormat());
		Assert.assertTrue(tsi.getTotalSamples() == expectedFixedCount);	// required for test to work.

		// Make sure a basic IthIterable works.
		IthIterable<SoundRecording> ith = new IthIterable<SoundRecording>(fixed, foldCount, 0, false);
		tsi = TrainingSetInfo.getInfo(ith);
		System.out.println("Ith: \n" + tsi.prettyFormat());
		Assert.assertTrue(tsi.getTotalSamples() == expectedFoldCount);	

		// Make sure ithSHuffle works.
		IthShuffleIterable<SoundRecording> ithShuffle= new IthShuffleIterable<SoundRecording>(fixed, foldCount, 0, false);
		tsi = TrainingSetInfo.getInfo(ithShuffle);
		System.out.println("Ith: \n" + tsi.prettyFormat());
		Assert.assertTrue(tsi.getTotalSamples() == expectedFoldCount);	

		// Make sure a shuffle of ithShuffle works. 
		IShuffleIterable<SoundRecording> shuffledIth = ithShuffle.shuffle(); 
		tsi = TrainingSetInfo.getInfo(shuffledIth);
		System.out.println("Shuffled Ith Shuffle: \n" + tsi.prettyFormat());
		Assert.assertTrue(tsi.getTotalSamples() == expectedFoldCount);	
	}
}
