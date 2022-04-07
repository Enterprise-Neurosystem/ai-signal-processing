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
package org.eng.aisp.classifier.dcase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.classifier.AbstractMulticlassClassifierTest;
import org.eng.aisp.classifier.IFixableClassifier;
import org.eng.aisp.classifier.dcase.DCASEClassifierBuilder;
import org.eng.aisp.dataset.LabeledSoundFiles;
import org.junit.Test;

public class DCASEClassifierTest extends AbstractMulticlassClassifierTest {

	@Override
	protected IFixableClassifier<double[]> getClassifier() {
		DCASEClassifierBuilder builder = new DCASEClassifierBuilder();
		builder
			.setTrainingFolds(2)
			.setNumberOfEpochs(165)	// Smaller than default but enough for testMixed*() 
		;
		return builder.build();
		
	}


//	@Override
//	public void testMixingSoundParameters() throws AISPException {
//		// A little one time test to make sure the signal generator is working wrt frequency and sampling rate.
////		IUnitSignalGenerator sig1 = new SingleFrequencySignalGenerator(40000, 0, 1000, false);
////		IUnitSignalGenerator sig2 = new SingleFrequencySignalGenerator(20000, 0, 1000, false);
////		for (int i=0 ; i<60000 ; i++)
////			Assert.assertTrue(sig1.getValue(2*i) == sig2.getValue(i));
//
////		testSoundParametersHelper(2000,1,44000,16, 2000,1,44000, 16);		// Used only to verify that the test is written correctly.
////
//		testSoundParametersHelper(2000,1,44000,16, 2000,2,44000,16);	// channels - OK
////		testSoundParametersHelper(2000,1,44000,16, 2000,1,44000, 8 );	// bits/sample change - NG
////		AISPLogger.logger.warning("sampling rate change test is commented out!");
//		testSoundParametersHelper(2000,1,44000,16, 2000,1,22000,16);	// sampling rate change - OK
////		testSoundParametersHelper(2000,1,44000,16, 1000,1,44000,16);	// clip length change - NG
//
//	}
	
	@Override // for testMixedSamplingRate
	protected int getSoundParameterSignalCounts() {
		return 16;
	}


 	@Override
 	@Test
 	public void testMixedChannels() throws AISPException, IOException {
		testTrainClassifySignalSeparation(2000,1,44100,16, 2000,2,44100,16);	// change only number of channels
 	}

//	@Override
//	@Test
//	public void testMixedDurations() throws AISPException, IOException {
//		// DCASE can only handle small changes in duration 
//		testTrainClassifySignalSeparation(2000,1,44100,16, 1800,1,44100,16);	// clip length change large -> small
//		testTrainClassifySignalSeparation(1800,1,44100,16, 2000,1,44100,16);	// clip length change small -> large
//	}
	

	@Test 
	@Override // to increase the number of training sounds.
	public void testTrainAndClassifyWaterCooler() throws IOException, AISPException, InterruptedException {
		String trainingLabel = "source";
		List<SoundRecording> srList;

		// Load some real sounds and train on them 
		String dataDir = TestDataDir + "WaterCoolerYKT/";
		srList = SoundTestUtils.iterable2List(LabeledSoundFiles.loadSounds(dataDir, true));
		List<SoundRecording> multiplied = new ArrayList<SoundRecording>();
		multiplied.addAll(srList);
		multiplied.addAll(srList);
		testTrainAndClassify(trainingLabel, multiplied, null);
	}

	
}
