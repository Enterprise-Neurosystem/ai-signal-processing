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

import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.IUpdatableClassifier;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public abstract class AbstractUpdatableModelerTest extends AbstractMulticlassClassifierTest {
	
	protected IUpdatableClassifier getUpdatableClassifier(String trainingLabel) throws AISPException {
		IClassifier<double[]> classifier = getClassifier();
		
		//Only run this test for classifiers that are incrementally trainable
		Assume.assumeTrue(classifier instanceof IUpdatableClassifier);
		
		return (IUpdatableClassifier)classifier;
		
	}

	@Test
	public void testUpdateClassifier() throws AISPException {
		String trainingLabel = "status";
		String labelValue1 = "l1";
		String labelValue2 = "l2";
		String labelValue3 = "l3";
		int countOrigTraining = 50;	// 10 fails on the LpDistanceMergeNN and LpDistanceVotingNN
		int countUpdateTraining = 25;  //Use smaller number of samples in update training to avoid failures caused by random fluctuation of training time
		IUpdatableClassifier<double[]> classifier = getUpdatableClassifier(trainingLabel); //Use two classifiers for time comparison
		
		List<SoundRecording> l1SR = SoundTestUtils.createTrainingRecordings(0, 1000, 0, 1200, countOrigTraining, trainingLabel, labelValue1);
		List<SoundRecording> l2SR = SoundTestUtils.createTrainingRecordings(0, 1000, 0, 400, countOrigTraining, trainingLabel, labelValue2);
		List<SoundRecording> l3SR = SoundTestUtils.createTrainingRecordings(0, 1000, 0, 100, countUpdateTraining, trainingLabel, labelValue3);
	
		// Train on two classes of sounds and make sure classification is correct
		List<SoundRecording> allTrainSR = new ArrayList<>();
		allTrainSR.addAll(l1SR);
		allTrainSR.addAll(l2SR);
		
//		System.gc();  Thread.yield();
		long startTime = System.currentTimeMillis();
		classifier.train(trainingLabel, allTrainSR);	
		long endTime = System.currentTimeMillis();
		long trainingMsec = endTime - startTime;
		
		SoundTestUtils.verifyClassifications(classifier, l1SR, trainingLabel, SoundTestUtils.VerifyMode.CompareEqual, labelValue1);
		SoundTestUtils.verifyClassifications(classifier, l2SR, trainingLabel, SoundTestUtils.VerifyMode.CompareEqual, labelValue2);
		SoundTestUtils.verifyClassifications(classifier, l3SR, trainingLabel, SoundTestUtils.VerifyMode.CompareNotEqual, labelValue3);
	
		// Incrementally train on the third sound class
//		System.gc();  Thread.yield();
		long startTimeInc = System.currentTimeMillis();
		classifier.update(l3SR);	
		long endTimeInc = System.currentTimeMillis();
		long updateMsec = endTimeInc - startTimeInc;
		
		Assert.assertTrue("trainingMsec=" + trainingMsec + ", updateMsec=" + updateMsec,trainingMsec > updateMsec);  //Check that incremental training takes less time than retraining
		
		SoundTestUtils.verifyClassifications(classifier, l1SR, trainingLabel, SoundTestUtils.VerifyMode.CompareEqual, labelValue1);
		SoundTestUtils.verifyClassifications(classifier, l2SR, trainingLabel, SoundTestUtils.VerifyMode.CompareEqual, labelValue2);
		SoundTestUtils.verifyClassifications(classifier, l3SR, trainingLabel, SoundTestUtils.VerifyMode.CompareEqual, labelValue3);
	
	
	}



}
