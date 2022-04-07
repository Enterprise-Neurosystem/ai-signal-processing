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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.classifier.AbstractAnomalyClassifierTest;
import org.eng.aisp.classifier.anomaly.FixedAnomalyDetectorClassifier;
import org.eng.aisp.classifier.anomaly.normal.NormalDistributionAnomalyClassifier;
import org.eng.aisp.classifier.anomaly.normal.NormalDistributionAnomalyClassifierBuilder;
import org.junit.Assert;
import org.junit.Test;

public class NormalDistributionAnomalyClassifierTest extends AbstractAnomalyClassifierTest {

	@Override
	protected NormalDistributionAnomalyClassifier getClassifier() throws AISPException {
		return new NormalDistributionAnomalyClassifier();
	}

	protected NormalDistributionAnomalyClassifier getAdaptingScorer(int samplesToLearnEnv) throws AISPException {
		NormalDistributionAnomalyClassifierBuilder builder = new NormalDistributionAnomalyClassifierBuilder();
		builder.setSamplesToAdaptToEnvironment(samplesToLearnEnv);
		return builder.build();
		
	}

	@Override
	protected NormalDistributionAnomalyClassifier getNonAdaptingScorer() throws AISPException {
		NormalDistributionAnomalyClassifierBuilder builder = new NormalDistributionAnomalyClassifierBuilder()
				.setSamplesToAdaptToEnvironment(-1);
		return builder.build();
	}
	
	@Test
	public void testAdaptationSpikeIsAbnornmalWithAbnormalTrainingData() throws AISPException, IOException {
		testAdaptation(false, true, true);
		testAdaptation(false, true, false);
	}

	@Test
	public void testAdaptationDropIsAbnornmalWithAbnormalTrainingData() throws AISPException, IOException {
		testAdaptation(false, false,true );
		testAdaptation(false, false,false);
	}


	@Test
	public void testAdaptationSpikeIsAbnornmalNormalOnlyTrainingData() throws AISPException, IOException {
		testAdaptation(true, true, true);
		testAdaptation(true, true, false);
	}

	@Test
	public void testAdaptationDropIsAbnornmalNormaOnlyTrainingData() throws AISPException, IOException {
		testAdaptation(true, false,true );
		testAdaptation(true, false,false);
	}

	/**
	 * Test model adaptability.
	 * @param trainNormalOnly if true, only train the model on normal data.
	 * @param abnormalLargerThanNormal if true, then abnormals are larger than normals, otherwise smaller.
	 * @param scaleUp if true, then test scaling of depoyed sounds to be larger than the trained sounds, otherwise smaller.
	 * @throws AISPException
	 * @throws IOException
	 */
	protected void testAdaptation(boolean trainNormalOnly, boolean abnormalLargerThanNormal, boolean scaleUp) throws AISPException, IOException {
		String trainingLabel = "status";

		double normalAmp,  abnormalAmp;
		if (abnormalLargerThanNormal) {
			normalAmp = 0.001; abnormalAmp = 0.05;
		} else {
			normalAmp = 0.05; abnormalAmp = 0.001;
		}
		double offset  = 0;
		double offsetChange = Math.max(normalAmp,  abnormalAmp);
		final int normalCount = 10;
		final int abnormalTrainCount = trainNormalOnly ? 0 : normalCount; 
		// Models trained on only normal are not expected to adapt to large numbers of abnormals.
		final int abnormalTestCount = trainNormalOnly ? 1 : normalCount;	
		final int samplesToLearnEnv = normalCount / 2;	// Make it classify normals after learning.
		double scaleChange = Math.min(normalAmp, abnormalAmp) / Math.max(normalAmp,abnormalAmp);	// Scale abnormals down to original normal size.
		if (scaleUp)
			scaleChange = (1-offsetChange)/Math.max(normalAmp, abnormalAmp);	
		else
			scaleChange = Math.min(normalAmp, abnormalAmp) / Math.max(normalAmp,abnormalAmp);	
		
		// Training a model on normal only, or normal and abnormal. 
		List<SoundRecording> srList = createTrainingSet(trainingLabel, offset, normalCount, normalAmp, abnormalTrainCount, abnormalAmp); 
		NormalDistributionAnomalyClassifier scorer = getAdaptingScorer(samplesToLearnEnv);
		scorer.train(trainingLabel, srList);
		
		// Make sure the model learned the training data.
		SoundTestUtils.verifyClassifications(scorer, srList, trainingLabel);
		// Make sure rerunning the data does not change classifications.
		SoundTestUtils.verifyClassifications(scorer, srList, trainingLabel);

		// Test recognition of abnormals w/o resetting the model (primarily useful when not trained on abnormals)..
		srList = createTrainingSet(trainingLabel, offset, normalCount, normalAmp, abnormalTestCount, abnormalAmp); 
		SoundTestUtils.verifyClassifications(scorer, srList, trainingLabel);

		// Scale the normal and abnormals, and expect it to have adapted so as to still properly classify. 
		srList = createTrainingSet(trainingLabel, offset, normalCount, scaleChange*normalAmp, abnormalTestCount, scaleChange*abnormalAmp); 
		scorer.reset();	// Let model start relearning a new environment for the modified sounds.
		SoundTestUtils.verifyClassifications(scorer, srList, trainingLabel);
			
		// Offset the signals relative to the original. 
		offset += offsetChange; 
		srList = createTrainingSet(trainingLabel, offset, normalCount, normalAmp, abnormalTestCount, abnormalAmp); 
		scorer.reset();	// Let model start relearning a new environment for the modified sounds.
		SoundTestUtils.verifyClassifications(scorer, srList, trainingLabel);	
		
		// Test with both scale and offset change
		srList = createTrainingSet(trainingLabel, offset, normalCount, scaleChange*normalAmp, abnormalTestCount, scaleChange*abnormalAmp); 
		scorer.reset();	// Let model start relearning a new environment for the modified sounds.
		SoundTestUtils.verifyClassifications(scorer, srList, trainingLabel);
	
	}
	
	/** 
	 * Create a list of normal sounds followed by abnormal sounds (order is important here).
	 * @param trainingLabel
	 * @param offset
	 * @param normalCount
	 * @param normalAmp
	 * @param abnormalCount
	 * @param abnormalAmp
	 * @return
	 */
	public static List<SoundRecording> createTrainingSet(String trainingLabel, double offset, int normalCount, double normalAmp, int abnormalCount, double abnormalAmp) { 
		Properties normalLabels = new Properties();
		normalLabels.setProperty(trainingLabel, "normal");	
		Properties abnormalLabels = new Properties();
		abnormalLabels.setProperty(trainingLabel, FixedAnomalyDetectorClassifier.ABNORMAL_LABEL_VALUE);	
		List<SoundRecording> normalList = createConstantTrainingData(normalCount, offset, normalAmp, normalLabels); 
		List<SoundRecording> abnormalList = createConstantTrainingData(abnormalCount, offset, abnormalAmp, abnormalLabels);
		List<SoundRecording> srList = new ArrayList<>();
		srList.addAll(normalList);
		srList.addAll(abnormalList);
		return srList;
	}

	private static List<SoundRecording> createConstantTrainingData(int count, double offset, double amp, Properties labels) { 

		int sampleLen = 44100;
		double data[] ;
		Random rand = new Random(123);

		List<SoundRecording> srList = new ArrayList<>();
		for (int i=0 ; i<count ; i++) {
			data = new double[sampleLen];
			for (int j=0 ; j<data.length; j++) {
				data[j] = offset + amp * rand.nextDouble();
				Assert.assertTrue(data[j] >= -1 && data[j] <= 1);	// Otherwise bad values for offset and amp from test.
			}

			SoundClip clip = new SoundClip(0, 1000, data);
			SoundRecording sr = new SoundRecording(clip, labels, null);
			sr.getDataWindow().getData();	// For debugging so we can look at the data[] in the clips.
			srList.add(sr);

		}
		return srList;
	}

	@Override
	protected AnomalyScoreTestParams getAnomalyScoreTestParams() {
		double normalAmp = 0.1;
		int normalHz = 0;
		int abnormalHz[]  = new int[] { normalHz, normalHz, normalHz };
		double abnormalAmps[]  = new double [] { normalAmp, normalAmp, normalAmp };
		double abnormalOffsets[]  = new double [] { 0.0005, 0.0006, 0.0007 }; 
		double maxNormalAnomalyScore = 0.6;
		boolean testAscendingScores = true;
		return new AnomalyScoreTestParams(normalAmp, normalHz, abnormalOffsets, abnormalAmps, abnormalHz, maxNormalAnomalyScore, testAscendingScores);
	}

}
