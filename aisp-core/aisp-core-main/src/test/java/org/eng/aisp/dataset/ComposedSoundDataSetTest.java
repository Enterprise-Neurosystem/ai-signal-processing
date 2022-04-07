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
package org.eng.aisp.dataset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.util.IDereferencer;
import org.junit.Assert;
import org.junit.Test;



public class ComposedSoundDataSetTest {

	protected static class HashedSoundDereferencer implements IDereferencer<IReferencedSoundSpec, SoundRecording> {

		private Map<String, SoundRecording> sounds;

		public HashedSoundDereferencer(Map<String, SoundRecording> sounds) {
			this.sounds = sounds;
		}

		@Override
		public SoundRecording loadReference(IReferencedSoundSpec reference) throws IOException {
			String soundRef = reference.getReference();
			SoundRecording sr = sounds.get(soundRef);
			if (sr == null)	
				throw new IOException("No sound for " + reference);
			return sr; 
		}
		
	}
	
	@Test
	public void testComposeMetaData() {
		int durationMsec = 500;
		int count = 3;
		String specLabelName = "specLabel";
		String specTagName = "specTag";
		String srcLabelName = "srcLabel";
		String srcTagName = "srcTag";
		int[] levels = new int[] { -1, 1};
		List<String> srReferences= new ArrayList<String>();
		List<String> specReferences = new ArrayList<String>();
		Map<String, SoundRecording> sounds = new HashMap<String,SoundRecording>(); 
		List<SoundRecording> expectedSounds = new ArrayList<SoundRecording>();
		
		// Holds the IReferenceSoundSpecs  (as MetaData.Record)
		MemorySoundReferenceDataSet md = new MemorySoundReferenceDataSet();
		for (int i=0 ; i<count ; i++) {

			// Create a labeled soundRecording
			SoundClip clip = SoundTestUtils.createChangePointSound(durationMsec, levels); 
			Properties srcLabels = new Properties();
			srcLabels.setProperty(srcLabelName, "label" + i);
			Properties srcTags = new Properties();
			srcTags.setProperty(srcTagName, "tag" + i);
			SoundRecording sr = new SoundRecording(clip, srcLabels, srcTags);

			// Create the reference for this sound and store it in a map under that reference.
			String srRef = String.valueOf(i);
			srReferences.add(srRef);
			sounds.put(srRef, sr);

			// Create a segment record for each segment and record the expected matching sound recording for each segment..
			int startMsec = 0;
			for (int k=0 ; k<levels.length ; k++) {	// Over each segment within the sr.
				// Create a reference spec that should produce the kth segment.
				int endMsec = startMsec + durationMsec;
				Properties specLabels = new Properties();
				specLabels.setProperty(specLabelName, "label-segment" + k);
				Properties specTags = new Properties();
				specTags.setProperty(specTagName, "tag-StartMsec is " + startMsec);
				ReferencedSoundSpec spec = new ReferencedSoundSpec(srRef, startMsec, endMsec, specLabels, specTags);
				String specRef = md.add(spec);
				
				// Put this spec in our spec set.
				specReferences.add(specRef);
				
				// Build the expected segment
				SoundClip segment = (SoundClip)clip.subWindow2(startMsec, endMsec);
				Properties expectedLabels = new Properties();
				expectedLabels.putAll(srcLabels);
				expectedLabels.putAll(specLabels);
				Properties expectedTags = new Properties();
				expectedTags.putAll(srcTags);
				expectedTags.putAll(specTags);
				SoundRecording expected = new SoundRecording(segment, expectedLabels, expectedTags); 
				expectedSounds.add(expected);
				
				// Next segment
				startMsec += durationMsec;
			}
		}

		// Create an IDereference<IReferencedSoundSpec, SoundRecording> 
		HashedSoundDereferencer soundDereferencer = new HashedSoundDereferencer(sounds);
		
		// Create the instance we are finally testing to see if it has the expected sounds.
		ComposedSoundDataSet dataSet = new ComposedSoundDataSet(specReferences, md, soundDereferencer,true);
		
		validate(dataSet, expectedSounds);
		
	}

	private void validate(ComposedSoundDataSet dataSet, List<SoundRecording> expectedSounds) {
		int index = 0;
		for (SoundRecording sr : dataSet) {
			Assert.assertTrue(index < expectedSounds.size());
			SoundRecording expected = expectedSounds.get(index);
			Assert.assertTrue("index=" + index, expected.equals(sr));
			index++;
		}
		Assert.assertTrue(expectedSounds.size() == index);
		
	}

}
