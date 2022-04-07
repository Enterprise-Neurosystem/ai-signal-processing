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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLogger;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.IFixableClassifier;
import org.eng.aisp.classifier.Classification.LabelValue;
import org.eng.aisp.dataset.LabeledSoundFiles;
import org.eng.cache.Cache;
import org.eng.util.IShuffleIterable;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public abstract class AbstractMulticlassClassifierTest extends AbstractClassifierTest {

	public final static String TestDataDir = "test-data/";
	private boolean modelerIsAccurateOnTrainingData;
	
	private AbstractMulticlassClassifierTest(boolean modelerIsAccurateOnTrainingData) {
		this.modelerIsAccurateOnTrainingData = modelerIsAccurateOnTrainingData;
	}
	
	@Before
	public void resetCache() {
		Cache.clearManagedCaches();
	
	}

	protected AbstractMulticlassClassifierTest() {
		this(true);
	}
	
	

//	private List<SoundRecording> getTrainingRecordings(String trainingLabel, String normalLabelValue, String abnormalLabelValue) {
//		return SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, normalLabelValue, 3, abnormalLabelValue, 3);
//	}


	@Test
	public void testIsTrainable() throws AISPException, IOException, InterruptedException  {
		String trainingLabel = "source";
		List<SoundRecording> srList;

		srList = getTrainingRecordings(trainingLabel); 
		String heldOutLabel =  srList.get(0).getLabels().keySet().iterator().next().toString();

		// Training and classifier w/o the "abnormal" data. 
		testTrainAndClassify(trainingLabel, srList, heldOutLabel);
	}
	
	@Test
	public void testTrainAndClassifySimulatedNormalAbnormal() throws AISPException, InterruptedException, IOException  {
		String trainingLabel = "source";
		Iterable<SoundRecording> srList;

//		 Create some pure signal sounds and train on them
//		 srList = getTrainingRecordings(trainingLabel, "normal", "abnormal"); 
		srList = getTrainingRecordings(trainingLabel); 
		testTrainAndClassify(trainingLabel, srList, null);
	}
	
	@Test 
	public void testTrainAndClassifyWaterCooler() throws IOException, AISPException, InterruptedException {
		String trainingLabel = "source";
		Iterable<SoundRecording> srList;

		// Load some real sounds and train on them 
		String dataDir = TestDataDir + "WaterCoolerYKT/";
		srList = LabeledSoundFiles.loadSounds(dataDir, true);
		testTrainAndClassify(trainingLabel, srList, null);

	}

	protected void testTrainAndClassify(String trainingLabel, Iterable<SoundRecording> srList, String untrainedValue ) throws AISPException, IOException, InterruptedException  {
		// Turn off training of the untrained label, if specified.
		if (untrainedValue != null) {
			for (SoundRecording sr : srList) {
				if (sr.getLabels().getProperty(trainingLabel).equals(untrainedValue))
					sr.setIsTrainable(false);
			}
		}

		IClassifier<double[]> classifier = getClassifier();
		try {
			Assert.assertTrue(classifier.getTrainedLabel() == null);	// Not trained
			classifier.train(trainingLabel, srList);			// TODO: should verify that input data is not modified?
			Assert.assertTrue(classifier.getTrainedLabel() != null);	// trained
			Assert.assertTrue(classifier.getTrainedLabel().equals(trainingLabel));	// trained

			// Test all the clips we trained in.
			int i=0;
			for (SoundRecording sr : srList) {
				boolean isTrainable = sr.isTrainable();
				SoundClip clip = (SoundClip)sr.getDataWindow();
				String actualLabel = sr.getLabels().getProperty(trainingLabel);
				if (actualLabel == null)
					Assert.fail("Test not configured properly. Data needs a " + trainingLabel + " label.");
				byte origPCM[] = Arrays.copyOf(clip.getPCMData(), clip.getPCMData().length);
				Assert.assertArrayEquals(origPCM, clip.getPCMData());		// Make sure classifier didn't corrupt the input data.
				Map<String, Classification> cmap = classifier.classify(clip);
				Assert.assertTrue(cmap != null);
				Assert.assertTrue(cmap.size() == 1);
				Classification c = cmap.get(trainingLabel);
				Assert.assertTrue(c != null);
				Assert.assertTrue(c.getLabelName() != null);
				Assert.assertTrue(c.getLabelName().equals(trainingLabel));
				Assert.assertTrue(c.getLabelValue() != null);
				Assert.assertTrue("Bad confidence value " + c.getConfidence(), c.getConfidence() >= 0 && c.getConfidence() <= 1);
				
				List<LabelValue> rankedValues = c.getRankedValues();
				
				if (rankedValues != null) {
					Assert.assertTrue(rankedValues.get(0).getLabelValue().equals(c.getLabelValue()));
					Assert.assertTrue(rankedValues.get(0).getConfidence() == c.getConfidence());
					
					//When providing ranked values, the confidence values should sum up to one
					double sumConfidence = 0.0;
					for (LabelValue lv : rankedValues) {
						sumConfidence += lv.getConfidence();
					}
					Assert.assertTrue("Sum confidence value not equal to 1, sum value is " + sumConfidence, 
							sumConfidence >= 0.99999 && sumConfidence <= 1.00001);
				} else {
					String msg = "Classifier " + classifier.getClass().getName() + " does not provide ranked label values.  Skipping tests related to ranked label values.";
					AISPLogger.logger.warning(msg);
				}


				if (isTrainable) {
					if (modelerIsAccurateOnTrainingData)
						Assert.assertTrue(i + " '" + actualLabel + "'!='" + c.getLabelValue() + "'", c.getLabelValue().equals(actualLabel));
				} else {	// Assumes that if one value for the label is found to not be trained, then all sounds with that label value were not trained.
					Assert.assertTrue(!c.getLabelValue().equals(actualLabel));
				}
				i++;
			}
		} finally {
			if (classifier instanceof Closeable)
				((Closeable)classifier).close();
		}

	}
	
	@Ignore("This test is an early one and others seem to do the same, so let's not run this one - dawood 1/2022")
	@SuppressWarnings("unchecked")
	@Test
	public void testSmallData() throws AISPException, IOException { 

		List<SoundRecording> srList = new ArrayList<SoundRecording>(); 

		String trainingLabel = "somelabel";
		int minDataPerClass = this.getMinimumTrainingDataPerClass();
		int minClasses = this.getMinimumTrainingClasses();
		int htz = 1000;
		for (int i=0 ; i<minClasses ; i++) {
			Properties labels = new Properties();
			String labelValue = "value" + i;
			labels.put(trainingLabel, labelValue);
			srList.addAll(SoundTestUtils.createTrainingRecordings(minDataPerClass, 1000, htz, labels, true)); 
			htz += 1000;
		}

		// Train a model on the sounds 
//		TrainingConfiguration tconfig = new TrainingConfiguration(trainingLabel);
		IClassifier classifier = getClassifier();
		try {
			try {
				classifier.train(trainingLabel, srList);
				SoundTestUtils.verifyClassifications(classifier, srList, trainingLabel);
//				Map<String,Classification> cmap = classifier.classify(srList.get(0).getDataWindow());
//				Assert.assertTrue(cmap != null);
//				Classification c = cmap.get(trainingLabel);
//				Assert.assertTrue(c != null);
//				Assert.assertTrue(labelValue.equals(c.getLabelValue()));
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail("Got other than the expected exceptions");
			}
		} finally {
			if (classifier instanceof Closeable)
				((Closeable)classifier).close();
		}
	}

	protected int getSoundParameterSignalCounts() {
		return this.getMinimumTrainingDataPerClass();
	}
	
	@Test
	public void testLabelValueCaseSensitivity() throws AISPException, IOException {
		String trainingLabel = "source";
		String lowerLabelValue= "value";
		String upperLabelValue = lowerLabelValue.toUpperCase(); 

		int durationMsec = 1000;	
		int lowerHtz = 1000;
		int upperHtz = 10000;
		int count = getSoundParameterSignalCounts();
		boolean addNoise = false;
		Properties lowerLabels = new Properties();
		lowerLabels.setProperty(trainingLabel, lowerLabelValue);
		List<SoundRecording> lowerSR = SoundTestUtils.createTrainingRecordings(count, durationMsec, lowerHtz, lowerLabels, addNoise);

		Properties upperLabels = new Properties();
		upperLabels.setProperty(trainingLabel, upperLabelValue);
		List<SoundRecording> upperSR = SoundTestUtils.createTrainingRecordings(count, durationMsec, upperHtz, upperLabels, addNoise);

		List<SoundRecording> sounds = new ArrayList<SoundRecording>();
		sounds.addAll(lowerSR);
		sounds.addAll(upperSR);
		IClassifier<double[]> classifier = getClassifier();
		
		try {
			// Test labels that have the same labelName, but the label values only differ in their case.
			classifier.train(trainingLabel, sounds);
			SoundTestUtils.verifyClassifications(classifier, lowerSR, trainingLabel, SoundTestUtils.VerifyMode.CompareEqual, null);
			SoundTestUtils.verifyClassifications(classifier, upperSR, trainingLabel, SoundTestUtils.VerifyMode.CompareEqual, null);
		} finally {
			if (classifier instanceof Closeable)
				((Closeable)classifier).close();
		}
	}
	
	@Test
	public void testLabelNameCaseSensitivity() throws AISPException, IOException {
		String lowerTrainingLabel = "source";
		String upperTrainingLabel = lowerTrainingLabel.toUpperCase(); 
		String labelValue1= "value1";
		String labelValue2 = "value2";
		String labelValue3= "value3";
		String labelValue4 = "value4";

		int durationMsec = 1000;	
		int lowerHtz = 1000;
		int upperHtz = 10000;
		int count = getSoundParameterSignalCounts();
		boolean addNoise = false;
		Properties labels;
		
		labels = new Properties();
		labels.setProperty(lowerTrainingLabel, labelValue1);
		List<SoundRecording> lowerValue1SR = SoundTestUtils.createTrainingRecordings(count, durationMsec, lowerHtz, labels, addNoise);

		labels = new Properties();
		labels.setProperty(lowerTrainingLabel, labelValue2);
		List<SoundRecording> lowerValue2SR = SoundTestUtils.createTrainingRecordings(count, durationMsec, upperHtz, labels, addNoise);

		labels = new Properties();
		labels.setProperty(upperTrainingLabel, labelValue3);
		List<SoundRecording> upperValue3SR = SoundTestUtils.createTrainingRecordings(count, durationMsec, lowerHtz, labels, addNoise);

		labels = new Properties();
		labels.setProperty(upperTrainingLabel, labelValue4);
		List<SoundRecording> upperValue4SR = SoundTestUtils.createTrainingRecordings(count, durationMsec, upperHtz, labels, addNoise);

		List<SoundRecording> sounds = new ArrayList<SoundRecording>();
		sounds.addAll(lowerValue1SR);
		sounds.addAll(lowerValue2SR);
		sounds.addAll(upperValue3SR);
		sounds.addAll(upperValue4SR);
		IClassifier<double[]> classifier = getClassifier();
		
		try {
			// Test to make sure only the lower case label names got trained in. 
			classifier.train(lowerTrainingLabel, sounds);
			SoundTestUtils.verifyClassifications(classifier, lowerValue1SR, lowerTrainingLabel, SoundTestUtils.VerifyMode.CompareEqual, null);
			SoundTestUtils.verifyClassifications(classifier, lowerValue2SR, lowerTrainingLabel, SoundTestUtils.VerifyMode.CompareEqual, null);
			SoundTestUtils.verifyClassifications(classifier, lowerValue1SR, upperTrainingLabel, SoundTestUtils.VerifyMode.ExpectNoClassification, null);
			SoundTestUtils.verifyClassifications(classifier, lowerValue2SR, upperTrainingLabel, SoundTestUtils.VerifyMode.ExpectNoClassification, null);
			SoundTestUtils.verifyClassifications(classifier, upperValue3SR, lowerTrainingLabel, SoundTestUtils.VerifyMode.CompareNotEqual, null);
			SoundTestUtils.verifyClassifications(classifier, upperValue4SR, lowerTrainingLabel, SoundTestUtils.VerifyMode.CompareNotEqual, null);
		
		} finally {
			if (classifier instanceof Closeable)
				((Closeable)classifier).close();
		}
	}
	
	

//	@Test
//	public void testArrayIndexOutOfBounds() throws AISPException, IOException, InterruptedException {
//		testRetrain();
//		testNormalAbnormalSignals();
//	}
	
	@Test
	public void testRetrain() throws AISPException, IOException {
		String trainingLabel = "status";

		IClassifier<double[]> classifier = getClassifier();

		// Train on the minimum number of sounds and verify accuracy. 
		List<SoundRecording> srList= getTrainingRecordings(trainingLabel, 0); 
		try { 
			classifier.train(trainingLabel, srList);	
			SoundTestUtils.verifyClassifications(classifier, srList, trainingLabel);

			// RETRAIN on sounds plus new sounds 
			srList= getTrainingRecordings(trainingLabel, 1); 
			classifier.train(trainingLabel, srList);	
			SoundTestUtils.verifyClassifications(classifier, srList, trainingLabel);

		} finally {
			if (classifier instanceof Closeable)
				((Closeable)classifier).close();
		}
	}
	
	/**
	 * Check the classifier's annotations to see if it support the generation of unknown classifications. 
	 * @param classifier
	 */
	protected boolean supportsUnknown(IClassifier<?> classifier) {
//		ModelProperties annotation = classifier.getClass().getAnnotation(ModelProperties.class);
//		Assert.assertTrue("Classifier " + classifier.getClass().getSimpleName() + " does not have " + ModelProperties.class.getName() + " annotation.", annotation != null);	
//		return annotation.supportsUnknownDetection();
		try {
			return this.getUnknownClassifier() != null;
		} catch (AISPException e) {
			return false;
		}
	}


//	/**
//	 * Stop the test with a failed assumption if the classifier does not generate unknown classification results, per the ModelProperties annotation.
//	 * @param classifier
//	 */
//	protected void assumeSupportsUnknown(IClassifier<?> classifier) {
//		if (!supportsUnknown(classifier)) {
//			String msg = "Classifier " + classifier.getClass().getName() + " does not support unknowns.  Skipping test.";
//			AISPLogger.logger.warning(msg);
//			Assume.assumeTrue(msg,false);
//		}
//	}

	@Test
	public void testClassifyUnknown() throws AISPException, IOException, InterruptedException  {
		String trainingLabel = "status";
		IClassifier classifier = this.getUnknownClassifier();
		Assume.assumeTrue("Classifier test does not appear to support unknown identification", classifier != null);	

//		assumeSupportsUnknown(classifier);

		// Load only NORMAL sounds into the server.
//		List<SoundRecording> srList = SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, "normal", 10, "abnormal", 0);
		List<SoundRecording> srList = getTrainingRecordings(trainingLabel, 1); 
		String heldOutLabelValue =  srList.get(0).getLabels().values().iterator().next().toString();	// An arbitrary label
		List<SoundRecording> trainingData = removeLabelValues(srList, trainingLabel, heldOutLabelValue); 

		// Train on data w/o the held out label value
		classifier.train(trainingLabel, trainingData);	

		try {
			// Test all ABNORMAL clips we DID NOT train in and make sure we don't get NORMAL classification.
			for (SoundRecording sr : srList) {
				String expectedLabel = sr.getLabels().getProperty(trainingLabel);
				if (expectedLabel == null)
					Assert.fail("Test not configured properly. Data needs a " + trainingLabel + " label.");
				if (!expectedLabel.equals(heldOutLabelValue))
					continue;	// Only test data not trained into model.
				// Test the held out label valued sound
				SoundClip clip = (SoundClip)sr.getDataWindow();
				Map<String,Classification> cList = classifier.classify(clip);
				Assert.assertTrue(cList != null);
				Assert.assertTrue(cList.size() == 1);
				Classification c = cList.values().iterator().next();
				Assert.assertTrue(c != null);
				Assert.assertTrue(c.getLabelName() != null);
				Assert.assertTrue(c.getLabelName().equals(trainingLabel));
				Assert.assertTrue(c.getLabelValue() != null);
				Assert.assertTrue(c.getLabelValue().equals(Classification.UndefinedLabelValue));	// Make sure it is an outlier. 
			}
		} finally {
			if (classifier instanceof Closeable)
				((Closeable)classifier).close();
		}
	}

	protected List<SoundRecording> removeLabelValues(List<SoundRecording> srList, String trainingLabel, String heldOutLabel) {
		List<SoundRecording> filtered = new ArrayList<SoundRecording>();
		for (SoundRecording sr : srList) {
			String labelValue = sr.getLabels().getProperty(trainingLabel);
			if (labelValue == null || !labelValue.equals(heldOutLabel))
				filtered.add(sr);
		}
		return filtered;
	}

//	@Test
//	public void testMixingSoundParameters() throws AISPException {
//		// A little one time test to make sure the signal generator is working wrt frequency and sampling rate.
////		SingleFrequencySignalGenerator sig1 = new SingleFrequencySignalGenerator(1000, 40000, 1, 0, 1000, false, false);
////		SingleFrequencySignalGenerator sig2 = new SingleFrequencySignalGenerator(1000, 20000, 1, 0, 1000, false,false);
////		while (sig1.hasNext() && sig2.hasNext()) {
////			double v1 = sig1.next(); 
////			double v2 = sig2.next();
////			sig1.next();	// sig1 has 2x the sampling rate, so skip every other one.
////			Assert.assertTrue(v1 == v2);
////		}
//
////		testSoundParametersHelper(2000,1,44100,16, 2000,1,44100, 16);		// Used only to verify that the test is written correctly.
////
//		testSoundParametersHelper(2000,1,44100,16, 1000,2,44100,16);	// channels  change
//
//
//	}
	
	@Test
	public void testMixedChannels() throws AISPException, IOException {
//		testSoundParametersHelper(2000,1,44100,16, 2000,1,44100, 16);		// Used only to verify that the test is written correctly.
		testTrainClassifySignalSeparation(1000,1,44100,16, 1000,2,44100,16);	// channels  change
	}
	
	@Test
	public void testMixedBitsPerSample() throws AISPException, IOException {
//		testSoundParametersHelper(2000,1,44100,16, 2000,1,44100, 16);		// Used only to verify that the test is written correctly.
		testTrainClassifySignalSeparation(2000,1,44100,16, 2000,1,44100, 8 );	// bits/sample change
	}

	@Test
	public void testMixedDurations() throws AISPException, IOException {
//		testSoundParametersHelper(2000,1,44100,16, 2000,1,44100, 16);		// Used only to verify that the test is written correctly.
		testTrainClassifySignalSeparation(2000,1,44100,16, 1000,1,44100,16);	// clip length change large -> small
		testTrainClassifySignalSeparation(1000,1,44100,16, 2000,1,44100,16);	// clip length change small -> large
	}
	
	@Test
	public void testMixedSamplingRate() throws AISPException, IOException {
//		testSoundParametersHelper(2000,1,44100,16, 2000,1,44100, 16);		// Used only to verify that the test is written correctly.
//		AISPLogger.logger.warning("sampling rate change test is commented out!");
		testTrainClassifySignalSeparation(2000,1,44100,16, 2000,1,30000,16);	// sampling rate change decrease
		testTrainClassifySignalSeparation(2000,1,30000,16, 2000,1,44100,16);	// sampling rate change increase 
	}
	
	/**
	 *  Train a model on both normal and abnormal sounds at the given sampling rate and bits/sample values for the training data.
	 *  Then generate normal and abnormal sound using the classify sample rate and bits/sample values.
	 *  The abnormal and normal sounds generated with the classify parameters should be correctly classified with the model trained on sounds
	 *  using the training sample rates and bit/sample values.
	 * @throws IOException 
	 */
	protected void testTrainClassifySignalSeparation(int trainingDurationMsec, int trainingChannels, int trainSamplingRate, int trainBitsPerSample, int classifyDurationMsec, int classifyChannels, int classifySamplingRate, int classifyBitsPerSample) throws AISPException, IOException {
		String trainingLabel = "status";
		String normalLabelValue = "normal";
		String abnormalLabelValue = "abnormal";
		int normalFrequency = 1000;	// These should be small relative to the sampling rate (10+ khz), but large enough to detect in small (20msec) subwindows.
		int abnormalFrequency = 5000;
//		int msecDuration = 2000;
		int trainingSoundCount = getSoundParameterSignalCounts();
		
		/// Train up a model on sounds using the indicated sampling rate and bits per sample values for training data.
		List<SoundRecording> normalSR =   SoundTestUtils.createTrainingRecordings(trainingSoundCount, trainingChannels, trainSamplingRate, 
				trainBitsPerSample, 0, trainingDurationMsec, 0,   normalFrequency, trainingLabel,   normalLabelValue, true);
		List<SoundRecording> abnormalSR = SoundTestUtils.createTrainingRecordings(trainingSoundCount, trainingChannels, trainSamplingRate, 
				trainBitsPerSample, 0, trainingDurationMsec, 0, abnormalFrequency, trainingLabel, abnormalLabelValue,true);
		List<SoundRecording> srData = new ArrayList<SoundRecording>();
		srData.addAll(normalSR);
		srData.addAll(abnormalSR);
		IClassifier<double[]> classifier = getClassifier();
		try {

			classifier.train(trainingLabel, srData);
			// Verify the model was trained correctly.
			SoundTestUtils.verifyClassifications(classifier,   normalSR, trainingLabel,   SoundTestUtils.VerifyMode.CompareEqual, normalLabelValue);
			SoundTestUtils.verifyClassifications(classifier, abnormalSR, trainingLabel, SoundTestUtils.VerifyMode.CompareEqual, abnormalLabelValue);
			// Now generate some sounds using the indicated sampling rate and bits per sample values for classify data.
			normalSR = SoundTestUtils.createTrainingRecordings(1, classifyChannels, classifySamplingRate, 
					classifyBitsPerSample, 0, classifyDurationMsec, 0,   normalFrequency, trainingLabel,   normalLabelValue, true);
			abnormalSR = SoundTestUtils.createTrainingRecordings(1, classifyChannels, classifySamplingRate, 
					classifyBitsPerSample, 0, classifyDurationMsec, 0, abnormalFrequency, trainingLabel, abnormalLabelValue,true);
	//		double[] nd = normalSR.get(0).getDataWindow().getData();
	//		double[] abd = abnormalSR.get(0).getDataWindow().getData();
			// Verify that the model works on these sounds with different sound characteristics
			SoundTestUtils.verifyClassifications(classifier,   normalSR, trainingLabel,   SoundTestUtils.VerifyMode.CompareEqual, normalLabelValue);
			SoundTestUtils.verifyClassifications(classifier, abnormalSR, trainingLabel, SoundTestUtils.VerifyMode.CompareEqual, abnormalLabelValue);
		} finally {
			if (classifier instanceof Closeable)
				((Closeable)classifier).close();
		}
	}
	
	@Test
	public void testHomeAnomalies() throws IOException, AISPException {
		homeAnomaliesHelper(true);
		Cache.clearManagedCaches();
		homeAnomaliesHelper(false);
	}

	/**
	 * Test anomaly (unknown) identification by using a small set of captured sounds having 4 labels.
	 * Sounds for a subset of labels are trained into a classifier, and then sounds from the other labels
	 * are classified with the expected result being the unknown value {@link Classification#UndefinedLabelValue}. 
	 * Using the classifier defined in {@link #getUnknownClassifier}.
	 * @param trainAllButOne true then all but one label is trained and the remaining label is tested to be unknown.
	 * if false, then only 1 label is trained in and sounds with the other labels are expected to produce the unknown value.
	 * @throws IOException
	 * @throws AISPException
	 */
	public void homeAnomaliesHelper(boolean trainAllButOne) throws IOException, AISPException {
		
		String dir = "test-data/home"; 
		String labelName = "cause";
		IClassifier<double[]> classifier = this.getUnknownClassifier();
		Assume.assumeTrue("Classifier test does not appear to support unknown identification", classifier != null);
		String[] labelValues;
		
		labelValues = new String[] { "keys",  "silence", "fan", "clapping" };
		
//		ModelProperties annotation = classifier.getClass().getAnnotation(ModelProperties.class);
//		Assert.assertTrue(annotation != null);	
//		if (!annotation.supportsUnknownDetection())
//			AISPLogger.logger.warning("Classifier " + classifier.getClass().getName() + " does not support unknowns.  Skipping test.");
//		Assume.assumeTrue(annotation.supportsUnknownDetection());
		
		IShuffleIterable<SoundRecording>  sounds = LabeledSoundFiles.loadSounds(dir, true);
		try {
			for (String labelValue : labelValues) {
	//			System.out.println("labelValue=" + labelValue);

				List<SoundRecording> trainingSounds;
				if (trainAllButOne) // All sounds but the given label value.
					trainingSounds = getMatchingSounds(sounds, labelName, labelValue, false);	
				else // Only the sounds with the given label. 
					trainingSounds = getMatchingSounds(sounds, labelName, labelValue, true);	
				classifier.train(labelName, trainingSounds);

				// Test to make sure the model learned all the sounds that were trained in.
				@SuppressWarnings("unused")
				int dbg = 0;
				for (SoundRecording sr: trainingSounds)
					SoundTestUtils.verifyClassification(classifier, sr, labelName, SoundTestUtils.VerifyMode.CompareEqual, sr.getLabels().getProperty(labelName));
			
				// Now make sure the sounds we didn't train in are classified as unknown. 
				List<SoundRecording> untrainedSounds;
				if (trainAllButOne) // Only sounds with the given label value.
					untrainedSounds = getMatchingSounds(sounds, labelName, labelValue, true);	
				else // All sounds but the one trained on.
					untrainedSounds = getMatchingSounds(sounds, labelName, labelValue, false);	
				SoundTestUtils.verifyClassifications(classifier, untrainedSounds, labelName, SoundTestUtils.VerifyMode.CompareEqual, Classification.UndefinedLabelValue);
			}
		} finally {	
			if (classifier instanceof Closeable)
				((Closeable)classifier).close();
		}
	}
	
	/**
	 * Method for specifying a different classifier for classifying unknown vs. known in addition to multi-class classification.
	 * Default is the null indicating that un/known detection is NOT supported. 
	 * Override as needed (for example, this can be a {@link org.eng.aisp.classifier.twophase.TwoPhaseClassifier} with two different specifications for multi-class and known/unknown).
	 * @return null if classifier should not be tested for  un/known detection. 
	 * @throws AISPException 
	 * 
	 */
	protected IFixableClassifier<double[]> getUnknownClassifier() throws AISPException {
		return null; 
	}


	protected List<SoundRecording> getMatchingSounds(IShuffleIterable<SoundRecording>  sounds, String labelName, String labelValue, boolean matching) {
		List<SoundRecording> srList = new ArrayList<SoundRecording>();
		for (SoundRecording sr : sounds) {
			String srLabelValue = sr.getLabels().getProperty(labelName);
			boolean matches = srLabelValue.equals(labelValue);
			if (matching == matches)
				srList.add(sr);
		}
		return srList;
	}
	
	// TODO: Enable this once it passes @Test
	/**
	 * Make sure confidence values 
	 * <ul>
	 * <li> get <b>lower</b> as <b>known</b> classifications are farther away from the trained sounds
	 * <li> get <b>larger</b> as <b>unknown</b> classifications are farther away from the trained sounds.
	 * </ul>
	 * @throws AISPException
	 * @throws IOException 
	 */
	// @Test	// Failing on NN still
	public void testConfidence() throws AISPException, IOException {
		String trainingLabel = "somelabel"; 
		String trainingValue = "anything"; 
		String htzLabel = "htz"; 
		int trainingCountPerHtz = 8;
		int trainingHtz[] = { 1000, 2000};
		int classifyHtz[]  = {1000, 1500, 3000, 4000, 5000, 15000, 40000 };  // fails nn	
//		int classifyHtz[]  = {1000, 2500, 3000, 4000, 5000, 15000, 40000 };  // fails gmm 	
//		int classifyHtz[]  = {1000, 3000, 3500, 4000, 5000, 15000, 40000 }; // fails gmm
//		int classifyHtz[]  = {1000, 2250, 3500, 4000, 5000, 15000, 40000 };  // fails gmm	
		IClassifier<double[]> classifier = this.getClassifier();
		
		// Create the training sounds 
		List<SoundRecording> labeledSounds = new ArrayList<SoundRecording>(); 
		for (int htz : trainingHtz) 
			labeledSounds.addAll(createLabeledSounds(trainingCountPerHtz, htz, htzLabel, trainingLabel, trainingValue));

		try {
			// Train the classifier
			classifier.train(trainingLabel, labeledSounds);

			// Create the sounds to be classified
			List<SoundRecording>  classifySounds = new ArrayList<SoundRecording>(); 
			for (int htz : classifyHtz) 
				classifySounds.addAll(createLabeledSounds(1, htz, htzLabel, trainingLabel, trainingValue));

			/**
			 * Classify sounds make sure that we first decrease in confidence with known values, then
			 * increase in confidence when we start getting unknown values.
			 */
			double lastConfidence = Double.MAX_VALUE;
			String lastHtz = "undefined";
			int knownCount = 0, unknownCount = 0;
			for (SoundRecording sr: classifySounds) {
				String htz = sr.getLabels().get(htzLabel).toString();
				Map<String,Classification> cmap = classifier.classify(sr.getDataWindow());
				Classification c = cmap.get(trainingLabel);
				Assert.assertTrue(c != null);
				double confidence = c.getConfidence();
				String labelValue = c.getLabelValue();
				Assert.assertTrue(labelValue != null);
				if (c.isKnown()) {	// Known and getting less confident
					String msg = "Known Sound with htz=" + htz + " has confidence=" + confidence + " was found after unknown sound at htz=" + lastHtz; 
					Assert.assertTrue(msg, unknownCount == 0);
					msg = "Known Sound with htz=" + htz + " has confidence=" + confidence + " which was not less than confidence= " + lastConfidence + " of sound with htz=" + lastHtz;
					Assert.assertTrue(msg, confidence <= lastConfidence);
					knownCount++;
				} else {			// Unknown
					String msg = "Unknown sound with htz=" + htz + " identified before seeing any known sounds"; 
					Assert.assertTrue(msg, knownCount != 0);
					unknownCount++;
					if (unknownCount > 1) {	// 2nd or later Unknown and getting more confident.
						msg = "Unknown sound (htz=" + htz + ") - confidence=" + confidence + " was not larger than confidence= " 
									+ lastConfidence + " of last unknown sound (htz=" + lastHtz + ")";
						Assert.assertTrue(msg, confidence == 1.0 || confidence >= lastConfidence);
					}
				}
				lastConfidence = confidence;
				lastHtz = htz; 
			}
			
			// Make sure the test was valid and that we got at least 2 known values.
			Assert.assertTrue("Did not see enough known sounds(" + knownCount + ") for test to be valid", knownCount >= 2);
			if (supportsUnknown(classifier))
				Assume.assumeTrue("Passed known sounds, but did not see enough unknown sounds(" + unknownCount + ") for unknown test to be valid", unknownCount >= 2);
			else
				Assume.assumeTrue("Passes known sounds test, but classifier does not support unknowns so skipping validation.", false);
		} finally {
			if (classifier instanceof Closeable)
				((Closeable)classifier).close();
		}
	}

	private static List<SoundRecording> createLabeledSounds(int count, int htz, String htzLabel, String trainingLabel, String trainingLabelValue) throws AISPException {
		int durationMsec = 5000;
		Properties labels = new Properties();
		labels.put(trainingLabel, trainingLabelValue); 
		labels.put(htzLabel, String.valueOf(htz));
		List<SoundRecording> srList = SoundTestUtils.createTrainingRecordings(count, durationMsec, htz, labels, true);
		return srList;
	}
}
