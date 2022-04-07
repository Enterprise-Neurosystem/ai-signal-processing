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
package org.eng.aisp.classifier.anomaly.normal;

import java.io.Serializable;
import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.classifier.AbstractFixedClassifierTest;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.classifier.anomaly.normal.OnlineNormalDistributionAnomalyClassifier;
import org.junit.Test;

public class OnlineNormalDistributionAnomalyClassifierTest extends AbstractFixedClassifierTest {

	protected final static String trainingLabel = "state";

	@Override
	protected IFixedClassifier<?> getClassifier() throws AISPException {
		return new OnlineNormalDistributionAnomalyClassifier(trainingLabel, 10);
	}

	protected IFixedClassifier<?> getClassifier(String trainingLabel, int samplesToLearnEnv) throws AISPException {
		return new OnlineNormalDistributionAnomalyClassifier(trainingLabel, samplesToLearnEnv);
	}
	
	@Test
	public void testOnlineLearning() throws AISPException {
		int offset = 0;
		int normalCount = 10;
		double normalAmp = 0.1;
		int abnormalCount = 1;
		double abnormalAmp = 0.5;

		// Normal only data to be learned online
		IFixedClassifier<double[]> classifier = (IFixedClassifier<double[]>) getClassifier(trainingLabel, normalCount);
		List<SoundRecording> srList = NormalDistributionAnomalyClassifierTest.createTrainingSet(trainingLabel, offset, normalCount, normalAmp, 0, abnormalAmp);
		
		// Train up the classifier.
		for (SoundRecording sr : srList) {
			IDataWindow<double[]> clip = sr.getDataWindow();
			classifier.classify(clip);
		}
		// Verify normal recognition
		SoundTestUtils.verifyClassifications(classifier, srList, trainingLabel);

		// Generated some abnormals
		srList = NormalDistributionAnomalyClassifierTest.createTrainingSet(trainingLabel, offset, 0, normalAmp, abnormalCount, abnormalAmp);
		
		// Verify abnormal recognition
		SoundTestUtils.verifyClassifications(classifier, srList, trainingLabel);
		
		
		
	}

}
