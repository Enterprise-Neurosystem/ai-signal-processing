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
package org.eng.aisp.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.DataTypeEnum;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.storage.AbstractNamedItemStorageTest;
import org.eng.storage.FieldValues;
import org.eng.storage.StorageException;
import org.eng.util.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FileSoundStorageTest extends AbstractNamedItemStorageTest<SoundRecording>{

	final static String TEST_DIR = "./junit-sound-dir";

	@Before
	@After
	public void beforeAfterTest() {
		FileUtils.deleteDirContents(new File(TEST_DIR));
		FileUtils.deleteFile(TEST_DIR);
	}

	@Override
	public FileSoundStorage getStorage() {
		return new FileSoundStorage(TEST_DIR);
	}

	@Override
	protected FieldValues getMatchingFields(int index) {
//		String sensorID = "sensorid" + index;
//		FieldValues fv = new FieldValues();
//		fv.put(SoundRecording.SENSORID_FIELD_NAME, sensorID);
		FieldValues fv = new FieldValues();
		fv.put(SoundRecording.LABELS_FIELD_NAME, getLabel("index", index));	// correct case 
		return fv;
	}

	@Override
	protected FieldValues getNonMatchingFields(int index) {
//		String sensorID = "sensorID" + index;	// case mismatch
//		fv.put(SoundRecording.SENSORID_FIELD_NAME, sensorID);
		FieldValues fv = new FieldValues();
		fv.put(SoundRecording.LABELS_FIELD_NAME, getLabel("INDEX", index));	// case mismatch
		return fv;
	}

	
	private Properties getLabel(String label, int index) {
		Properties labels = new Properties();
		labels.setProperty(label, String.valueOf(index));
		return labels;
		
	}
	
	private List<SoundRecording> getTestRecordings(int count, int startMsec, DataTypeEnum dataType, Properties labels) {
		List<SoundRecording> srList = new ArrayList<SoundRecording>();
		for (int i=0 ; i<count ; i++) {
			SoundClip clip; 
			switch(dataType) {
			case Audio: 				clip = SoundTestUtils.createClips(1, startMsec, 0, 500, 2, 44100, 16, 1, 0, 2000, false).get(0); break;
			case VibrationMagnitude: 	clip = SoundTestUtils.createClips(1, startMsec, 0, 500, 2,   100, 16, 1, 0, 10, false).get(0); break;
			case VibrationXYZ: 			clip = SoundTestUtils.createClips(1, startMsec, 0, 600, 2,   100, 16, 1, 0, 10, false).get(0); break;
			default: 
				Assert.fail("Unknown data type");
				clip = null;		// keep compiler quiet
			}
			SoundRecording sr = new SoundRecording(clip, labels, null, dataType);
			srList.add(sr);
		}
		return srList;
	}
	
	protected DataTypeEnum getDataTypeBeingTested() {
		return DataTypeEnum.Audio;
	}

	@Override
	public SoundRecording getItemToStore(int index) throws IOException, StorageException {
		Properties labels = getLabel("index", index); 
//		SoundClip clip = SoundTestUtils.createClips(1, index, 0, 500, 0).get(0);
//		SoundRecording sr = new SoundRecording(clip, labels );
		SoundRecording sr =getTestRecordings (1, index, getDataTypeBeingTested(), labels).get(0); 
		return sr; 
	}

	@Test
	public void testGetFile() throws StorageException {
		FileSoundStorage storage = getStorage();
//		List<SoundRecording> srList = SoundTestUtils.createTrainingRecordings(3, 500, 1000, new Properties(), false);
		Properties labels = new Properties();
		labels.put("label", "value");
		List<SoundRecording> srList = getTestRecordings(3, 0, getDataTypeBeingTested(), labels); 
		List<String> idList = new ArrayList<String>();
		for (SoundRecording sr : srList) {
			String id = storage.add(sr);
			Assert.assertTrue(id != null);
			idList.add(id);
		}
		for (String id : idList) {
			File f = storage.getSoundFile(id);
			Assert.assertTrue(f != null);
			Assert.assertTrue(f.exists());
		}
		Assert.assertTrue(storage.getSoundFile("xxx") == null);
		storage.clear();

		for (String id : idList) {
			File f = storage.getSoundFile(id);
			Assert.assertTrue(f == null);
		}
		Assert.assertTrue(storage.getSoundFile("xxx") == null);
	}

}
