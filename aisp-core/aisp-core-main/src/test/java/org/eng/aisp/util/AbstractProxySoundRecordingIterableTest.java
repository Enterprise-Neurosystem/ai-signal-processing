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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.util.IShuffleIterable;
import org.junit.Assert;
import org.junit.Test;





public abstract class AbstractProxySoundRecordingIterableTest {

	protected abstract List<Iterable<SoundRecording>> newProxyingIterables(IShuffleIterable<SoundRecording> recordings);
	
	@Test
	public void testSameIDs() throws IOException {
//		int startMsec = 0;
//		int pauseMsec = 0;		// keep this 0 otherwise assert below needs to be adjusted.
//		int htz = 1000;
//		int durationMsec = 1000;
//		int count = 3;
//		Properties p = new Properties();
//		p.setProperty("status", "somevalue");
		IShuffleIterable<SoundRecording> recordings  = SoundRecording.readSounds("test-data/chiller");
//		/** The iterable returned by readSounds() does not currently avoid re-reading and thus recreating IDataWindow instances.
//		 *  As a result, we must read them all into a List first.
//		 */
//		List<SoundRecording> recordingList = new ArrayList<SoundRecording>();
//		for (SoundRecording sr : recordings)
//			recordingList.add(sr);
//		recordings = new GenericTestShuffleIterable<SoundRecording>(recordingList);

		List<Iterable<SoundRecording>> iterableList =  newProxyingIterables(recordings);
		
		for (Iterable<SoundRecording> iterable : iterableList) {
			repeatableInstanceIDsTest(iterable);
		}
		
	}
	
	public static void repeatableInstanceIDsTest(Iterable<SoundRecording> iterable) {
		// Try an keep the GC running during the code below, otherwise there could be cache evictions,
		// which would invalidate the test below.
		System.gc();	
		
		List<Long> firstPassIDs = new ArrayList<Long>();
		for (SoundRecording sr : iterable) {
			Assert.assertTrue(sr != null);
			SoundClip clip = sr.getDataWindow();
			Assert.assertTrue(clip != null);
			long id = clip.getInstanceID();
			Assert.assertTrue(!firstPassIDs.contains(id));	// Make sure we don't get duplicate ids.
			firstPassIDs.add(id);
		}
		System.out.println("firstPassIDs : " + firstPassIDs);

		List<Long> secondPassIDs = new ArrayList<Long>();
		for (SoundRecording sr : iterable) {
			SoundClip clip = sr.getDataWindow();
			long id = clip.getInstanceID();
			Assert.assertTrue(!secondPassIDs.contains(id));	// Make sure we don't get duplicate ids in this pass 
//			Assert.assertTrue(firstPassIDs.contains(id));	// Make sure we get the same ids as in the first pass.
			secondPassIDs.add(id);
		}
		System.out.println("secondPassIDs: " + secondPassIDs);
		Assert.assertTrue(secondPassIDs.size() == firstPassIDs.size());	
		Assert.assertTrue(firstPassIDs.equals(secondPassIDs));
	}
}
