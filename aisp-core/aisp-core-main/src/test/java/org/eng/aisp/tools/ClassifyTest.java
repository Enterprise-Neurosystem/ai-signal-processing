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
package org.eng.aisp.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.classifier.TrainingSetInfo;
import org.eng.aisp.dataset.LabeledSoundFiles;
import org.eng.aisp.util.PCMUtil;
import org.eng.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;

public class ClassifyTest  extends AbstractToolTest {



	private String classify(boolean inDB, String modelRef, String soundFile, String labelName, int paddedMsec) {
		List<String> largs = new ArrayList<String>();
		if (inDB) {
			largs.add("-name");
			largs.add(modelRef);
		} else {
			largs.add("-file");
			largs.add(modelRef);
			
		}
		largs.add(soundFile);
		if (paddedMsec > 0) {
			largs.add("-clipLen");
			largs.add( String.valueOf(paddedMsec));
			largs.add("-pad");
			largs.add(TESTED_PAD_TYPE); 
		}
		PrintStream orig = System.out;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(bos);
		System.setOut(ps);
		runClassifyCommand(largs);
		System.setOut(orig);
		ps.flush();
		String output = bos.toString();
		// Looking for "... <labelName>=<labelValue> confidence= ..."
		String searchText = labelName + "="; 
		int index = output.indexOf(searchText);
		String classification = null;
		if (index > 0) {
			index += searchText.length();
			int endIndex = output.indexOf(" confidence", index);
			classification = output.substring(index, endIndex);
		}
		return classification;
	}


	
//	@Test
//	public void testClassifyUnpaddedNoStorage() throws AISPException, IOException {
//		testClassifyHelper(false);
//	}

	@Test
	public void testClassifyPaddedNoStorage() throws AISPException, IOException {
		testClassifyHelper(true);
	}

	private void testClassifyHelper(boolean padded) throws AISPException, IOException {
		// TODO: subclasses return a client that uses REST with a server, in which case we should run this test.
		// But, for now we will assume this is only used for the AI product, which does not have subclasses
//		Assume.assumeTrue(MainTestSuite.dbStorageTestsEnabled());	

//		IClassifierStorage storage = getModelStorage(); 
//		storage.clear();
		
		// Test -add using -dir
		String soundDir = "test-data/chiller";
		int paddedMsec = 0; 
		Iterable<SoundRecording> srList = LabeledSoundFiles.loadSounds(soundDir, true); 
		if (padded) {
			TrainingSetInfo tsi = TrainingSetInfo.getInfo(srList);
			double avgMsec = tsi.getTotalMsec() / tsi.getTotalSamples();
			paddedMsec = (int)(avgMsec * 1.5);
		}
		String labelName = "status";
		// Sounds are 5 seconds, so when padding do something larger than that
		String modelRef = trainAndSaveToFile(0,"knn", soundDir, labelName, paddedMsec);
	
		File tmpDir = FileUtils.createTempDir();
		for (SoundRecording sr : srList) { 
			String tmpFile = writeRecording(tmpDir, sr);
			String expectedValue = sr.getLabels().getProperty(labelName);
			String classifiedValue = classify(false, modelRef, tmpFile, labelName, paddedMsec);
			FileUtils.deleteFile(tmpFile);
			if (!expectedValue.equals(classifiedValue)) {
				FileUtils.deleteFile(tmpDir);
				Assert.fail(expectedValue + "!=" + classifiedValue);
			}
		}
		FileUtils.deleteFile(tmpDir);
		FileUtils.deleteFile(new File(modelRef));
		
//		storage.clear();
		
		
	}


	private String writeRecording(File tmpDir, SoundRecording sr) throws IOException {
		String tmpFile = tmpDir.getAbsolutePath() + "/sound.wav";
		FileUtils.deleteFile(tmpFile);	// Just in case
		PCMUtil.PCMtoWAV(tmpFile, sr.getDataWindow());
		return tmpFile;
	}



}
