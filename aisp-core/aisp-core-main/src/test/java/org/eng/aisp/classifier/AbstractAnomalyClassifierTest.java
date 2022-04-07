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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.util.IDistanceFunction;
import org.eng.aisp.util.L1DistanceFunction;
import org.eng.util.OnlineStats;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;


public abstract class AbstractAnomalyClassifierTest extends AbstractClassifierTest {

	@Override
	protected int getMinimumTrainingClasses() {
		return 2; 
	}
	
	protected abstract IClassifier<double[]> getNonAdaptingScorer() throws AISPException;
	
	/**
	 * Override to only generate label values that include the "normal" and "abnormal" value. 
	 */
	@Override
	protected List<SoundRecording> getTrainingRecordings(String trainingLabel, int additionalClasses) {
		Assert.assertTrue(additionalClasses == 0);	// can't do that.
		List<SoundRecording> srList = new ArrayList<SoundRecording>();
		int durationMsec = 1000;
		int htz = 1000;
		int samplesPerClass = this.getMinimumTrainingDataPerClass();
		int trainingClasses = getMinimumTrainingClasses();
		for (int i=0 ; i<trainingClasses ; i++) {
			Properties labels = new Properties();
			String labelValue = i == 0 ? "abnormal" : "normal";
			labels.put(trainingLabel,  labelValue);
			double amp = (i+ 1.0)/trainingClasses;
			srList.addAll(SoundTestUtils.createTrainingRecordings(samplesPerClass, durationMsec, amp, htz, labels, false));
			htz += 2000;
		}
		return srList;
	}

	/**
	 * Defines parameters to be used by the testAnomalyScore(). 
	 * Parameters define the normal data and sets of abnormal data to be tested for increasing anomaly score
	 * relative to the previous set.
	 * @author DavidWood
	 *
	 */
	protected static class AnomalyScoreTestParams {
		public final double normalAmp;
		public final int normalHz;
		public final double[] abnormalOffsets;
		public final double[] abnormalAmplitudes;
		public final int[] abnormalHz;
		public final double maxNormalAnomalyScore;
		public final boolean testAscendingScores;
		/**
		 * Defines the norml and sets of abnormals to be tests.  Abnormal set i, uses the i'th values from
		 * abnormalOffsets, abnormalAmplitudes and abnormalHz arrays. Successive sets should represent
		 * abnormals with larger and larger anomaly scores.
		 * @param normalAmp
		 * @param normalHz
		 * @param abnormalOffsets array of  offsets used to create the anomalies
		 * @param abnormalAmplitudes array of  amplitudes used to create the anomalies. Sample length as offsets.
		 * @param abnormalHz array of  frequencies used to create the anomalies. Same length as offsets.
		 */
		public AnomalyScoreTestParams(double normalAmp, int normalHz, double[] abnormalOffsets, double[] abnormalAmplitudes, int[] abnormalHz,
					double maxNormalAnomalyScore, boolean testAscendingScores) {
			this.normalAmp = normalAmp;
			this.normalHz = normalHz;
			if (abnormalOffsets == null || abnormalAmplitudes == null || abnormalHz == null)
				throw new IllegalArgumentException("abnormal params can not be null");
			if (abnormalOffsets.length  != abnormalAmplitudes.length || abnormalOffsets.length != abnormalHz.length)
				throw new IllegalArgumentException("abnormal arrays must all be the same length"); 
			this.abnormalOffsets = abnormalOffsets;
			this.abnormalAmplitudes = abnormalAmplitudes;
			this.abnormalHz = abnormalHz;
			this.maxNormalAnomalyScore =  maxNormalAnomalyScore;
			this.testAscendingScores = testAscendingScores;
		}
		
	}

	/**
	 * Get the sets of parameters used to control {@link testAnomalyScore}. 
	 * @return null if test should be skipped.
	 */
	protected abstract AnomalyScoreTestParams getAnomalyScoreTestParams();

	/**
	 * Train a model on a fixed frequency then test to make sure that frequencies farther away have higher anomaly scores.
	 * @throws AISPException
	 * @throws IOException
	 * @see #getAnomalyScoreTestParams() which configures the parameters for the test.
	 */
	@Test
	public void testAnomalyScore() throws AISPException, IOException {
		int trainCount = 25;
		int classifyCount = 10;
		int durationMsec = 1000;
		boolean addNoise = true;
		String trainingLabel = "status";
		Properties labels = new Properties();
		labels.setProperty(trainingLabel, "normal");	
		AnomalyScoreTestParams params = getAnomalyScoreTestParams();
		Assume.assumeTrue("Skipped anomaly score test due to lack of test parameters", params != null);
		Assert.assertTrue("Test misconfigured", params.abnormalHz.length == params.abnormalOffsets.length);
		Assert.assertTrue("Test misconfigured", params.abnormalHz.length == params.abnormalAmplitudes.length);
		int normalHz = params.normalHz;
		int[] abnormalHz = params.abnormalHz;
		double offsets[] = params.abnormalOffsets;
		double amplitudes[] = params.abnormalAmplitudes;
		double maxNormalAnomalyScore = params.maxNormalAnomalyScore;
		boolean testAscendingScores = params.testAscendingScores;
		
		// Training a model on a single frequency
		double amp = params.normalAmp;
		List<SoundRecording> srList = SoundTestUtils.createTrainingRecordings(trainCount, durationMsec, amp, normalHz, labels, addNoise);
		IClassifier<double[]> scorer = getNonAdaptingScorer();
		scorer.train(trainingLabel, srList);
		
		// Get clips from the sounds and the base level anomaly score for the normal sounds.
		List<SoundClip> clipList = new ArrayList<SoundClip>();
		OnlineStats normalStats = new OnlineStats();
		for (SoundRecording sr : srList) {
			accumulateDistanceStats("normal ", sr.getDataWindow(), normalStats);
			clipList.add((SoundClip)sr.getDataWindow());
		}
		System.out.println("norm distance stats=" + normalStats);
		double normalAnomalyScore = getAverageAnomalyScore(scorer, clipList);
		System.out.println("base normal score =" + normalAnomalyScore);
		Assert.assertTrue(normalAnomalyScore < maxNormalAnomalyScore);
		
		// Create a sets of abnormals and make sure each set has a larger anomaly score than the previous.
		double lastAnomalyScore = normalAnomalyScore;
		List<Integer> hzList = new ArrayList<Integer>();
		List<Double> ampList = new ArrayList<Double>();
		for (int i=0 ; i< abnormalHz.length ; i++ ) {
			int hz = abnormalHz[i];
			double offset = offsets[i];
			amp = amplitudes[i];
			ampList.add(amp);
			hzList.add(Integer.valueOf(hz));		// Add complexity to the signal.

			clipList = SoundTestUtils.createMultiFrequencySounds(classifyCount, durationMsec, offset, ampList, hzList, addNoise);

			OnlineStats abnormalStats = new OnlineStats();
			for (SoundClip clip : clipList) 
				accumulateDistanceStats("abnormal ", clip, abnormalStats);
			System.out.println("abnorm distance stats=" + abnormalStats);

			double score = getAverageAnomalyScore(scorer, clipList);
			System.out.println("hzList=" + hzList + ", anomaly score=" + score);
			Assert.assertTrue("hzList=" + hzList + ", score=" + score + " <= " + normalAnomalyScore, score == 1 || score > normalAnomalyScore);
			if (testAscendingScores)
				Assert.assertTrue("hzList=" + hzList + ", score=" + score + " <= " + lastAnomalyScore, score == 1 || score > lastAnomalyScore);
			lastAnomalyScore = score;
		}
		
		if (scorer instanceof Closeable)
			((Closeable)scorer).close();
	}

	private static final IDistanceFunction<double[]> df = new L1DistanceFunction();
	protected static void accumulateDistanceStats(String msg, SoundClip clip, OnlineStats accumulator) {
//		OnlineStats dataStats = new OnlineStats();
//		dataStats.addSamples(clip.getData());
		double d = df.distance(clip.getData());
//		System.out.println("d=" + d + ", mean = " + dataStats.getMean());
//		System.out.print(msg + " stats=" + stats);
		if (accumulator != null) {
			accumulator.addSample(d);
//			System.out.print(", accumulated=" + accumulator);
		}
//		System.out.println("");
	}

	protected double getAverageAnomalyScore(IClassifier<double[]> scorer, List<SoundClip> clipList) throws AISPException {
		if (clipList.isEmpty()) {
			Assert.fail("Test not configured correctly");
			return 0;	// Keeps SonarQube/static analysis quiet.
		}
		
		double sum = 0;
		int count = 0;
		String trainingLabel = scorer.getTrainedLabel();
		Assert.assertTrue(trainingLabel != null);
		for (SoundClip clip: clipList ) {
			Map<String,Classification> cmap = scorer.classify(clip);
//			Assert.assertTrue(cmap != null);
//			Classification c = cmap.get(trainingLabel);
//			Assert.assertTrue(c != null);
//			double conf = c.getConfidence();
			double score = getAnomalyScore(trainingLabel, cmap);
			Assert.assertTrue(score >= 0);
			Assert.assertTrue(score <= 1);
			sum += score;
			count += 1;
		}
		return sum / count;
	}

	/**
	 * Get the anomaly score out of a classification result on a sample.
	 * @param label the label on which the model was trained
	 * @param classifyResult
	 * @return number between 0 and 1 that is the anomaly score of this result.
	 */
	private double getAnomalyScore(String trainingLabel, Map<String, Classification> classifyResult) {
		Classification c = classifyResult.get("abnormal");	// Expect abnormal=true, with confidence holding the anomaly score.
		Assert.assertTrue(c != null);
		Assert.assertTrue(c.getLabelValue().equals("true"));
		return c.getConfidence();
	}



}
