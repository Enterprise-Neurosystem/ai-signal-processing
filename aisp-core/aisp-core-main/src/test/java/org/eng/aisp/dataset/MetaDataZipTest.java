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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.dataset.MetaDataZip.NamedSound;
import org.eng.aisp.util.FileZipper;
import org.eng.aisp.util.PCMUtil;
import org.junit.Assert;
import org.junit.Test;

public class MetaDataZipTest {

	@Test
	public void testWriteRead() throws IOException {
		Properties labels = new Properties();
		labels.put("name", "value");
		int count = 3;
		List<SoundRecording> sounds = SoundTestUtils.createTrainingRecordings(count, System.currentTimeMillis(), 1000, 0, 1000, labels);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		MetaDataZip.write(bos, sounds, null);
		
//		File targetFile = new File("test.zip");
//		OutputStream os = new FileOutputStream(targetFile);
//		MetaDataZip.write(os, sounds, null);
//		os.close();
//		
	    
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		Iterable<SoundRecording> zipSounds = MetaDataZip.readSoundRecordings(bis);
		
		int found = 0;
		for (SoundRecording output : zipSounds) {
			SoundRecording input = sounds.get(found);
			output.removeTag(MetaData.FILENAME_TAG);	// Reading using metadata adss this tag.
			Assert.assertTrue("found=" + found, input.equals(output));
			found++;
		}
		Assert.assertTrue(found == sounds.size());
	}

	@Test
	public void testWriteReadWithoutMetadata() throws IOException {
		Properties labels = new Properties();
		labels.put("name", "value");
		int count = 3;
		List<SoundClip> sounds = SoundTestUtils.createClips(count, 1000, 1000);
		ByteArrayOutputStream zos = new ByteArrayOutputStream();
		FileZipper zipper = new FileZipper(zos);
		int index = 0;
		List<String> fileNames = new ArrayList<String>();
		for (SoundClip clip: sounds) {
			String fileName= index++ + ".wav";
			ByteArrayOutputStream clipos = new ByteArrayOutputStream();
			PCMUtil.PCMtoWAV(clipos, clip);
			zipper.addFile(fileName, clipos.toByteArray());
			fileNames.add(fileName);
		}
		zipper.finalizeZip();
		
		ByteArrayInputStream bis = new ByteArrayInputStream(zos.toByteArray());
		Iterable<NamedSound> namedSounds = MetaDataZip.readNamedSounds(bis);

		int found = 0;
		for (NamedSound output : namedSounds) {
			Assert.assertTrue(output.getName().equals(fileNames.get(found)));
			found++;
		}
		Assert.assertTrue(found == count);
	}

}
