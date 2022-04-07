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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eng.ENGTestUtils;
import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLogger;
import org.eng.aisp.DataTypeEnum;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.SoundTestUtils.VerifyMode;
import org.eng.aisp.classifier.ConfusionMatrix;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.IFixableClassifier;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.util.ClassUtilities;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public abstract class AbstractClassifierTest extends AbstractFixedClassifierTest {

	public AbstractClassifierTest() {
		super();
	}

	protected abstract IFixableClassifier<double[]> getClassifier() throws AISPException;

	protected int getMinimumTrainingClasses() {
		return 1;
	}

	/**
	 * Allow subclasses to override.
	 * @return
	 */
	protected int getMinimumTrainingDataPerClass() {
		return 3;
	}

	/**
	 * Get a set of training recordings for the classifier to be tested. 
	 * The number of classes is defined by {@link #getMinimumTrainingClasses()} and the number of
	 * samples per class is defined by {@link #getMinimumTrainingDataPerClass()}.
	 * The label values are defined by the implementation.
	 * @param trainingLabel
	 * @return never null.
	 */
	protected List<SoundRecording> getTrainingRecordings(String trainingLabel) {
		return getTrainingRecordings(trainingLabel, 0);
	}

	/**
	 * Get a set of training recordings for the classifier to be tested. 
	 * The number of classes is defined by {@link #getMinimumTrainingClasses()} and the number of
	 * samples per class is defined by {@link #getMinimumTrainingDataPerClass()}.
	 * The label values are defined by the implementation.
	 * @param trainingLabel
	 * @param additionalClasses number of classes in addition to those defined by {@link #getMinimumTrainingClasses()}.
	 * @return never null.
	 */
	protected List<SoundRecording> getTrainingRecordings(String trainingLabel, int additionalClasses) {
		int samplesPerClass = this.getMinimumTrainingDataPerClass();
		int numClasses = getMinimumTrainingClasses() + additionalClasses;
		return getTrainingRecordings(trainingLabel, numClasses, samplesPerClass);
	}

	/**
	 * @param trainingLabel
	 * @param numClasses
	 * @param samplesPerClass
	 * @return
	 */
	private List<SoundRecording> getTrainingRecordings(String trainingLabel, int numClasses, int samplesPerClass) {
		List<SoundRecording> srList = new ArrayList<SoundRecording>();
		int durationMsec = 1000;
		int htz = 1000;
		for (int i=0 ; i<numClasses ; i++) {
			Properties labels = new Properties();
			labels.put(trainingLabel, "htz:" + htz);
			srList.addAll(SoundTestUtils.createTrainingRecordings(samplesPerClass, durationMsec, htz, labels, false));
			htz += 2000;
		}
		return srList;
	}

	/**
	 * Provided to allow sub-classes (initially one of the python classifiers) to change the number
	 * of classifications done in each thread (roughly) within the {@link #testParallelClassifyUsingConfusionMatrix()} test.
	 * @return return 0 to skip the test. 
	 */
	protected int getParallelClassificationCountPerThread() {
		return 500;
	}

	@Test
	public void testParallelClassifyUsingConfusionMatrix() throws AISPException, IOException {
		String trainingLabel = "source";
		int perThreadCount = getParallelClassificationCountPerThread();
		Assume.assumeTrue("Skipping test because per thread classification says so.", perThreadCount > 0);
		List<SoundRecording> srList = getTrainingRecordings(trainingLabel);
		IClassifier<double[]> classifier = getClassifier();
		try {
			System.out.println("Start training");
			classifier.train(trainingLabel, srList);
			
			// Create a long list of the same sound.
	//		System.out.println("Start creating sound list");
			int sampleCount = perThreadCount * Runtime.getRuntime().availableProcessors();
			SoundRecording sr = srList.get(0);
			srList = new ArrayList<SoundRecording>(sampleCount);
			for (int i=0 ; i<sampleCount ; i++) {
	//			SoundClip clip = new SoundClip((double)i, sr.getDataWindow());
	//			sr = new SoundRecording(sr, clip);
				srList.add(sr);
			}
	//		System.out.println("Start classifying");
	//		long startMsec = System.currentTimeMillis();
			ConfusionMatrix matrix = ConfusionMatrix.compute(trainingLabel, classifier, srList, true);
	//		ConfusionMatrix matrix = ConfusionMatrix.compute(trainingLabel, classifier, srList, false);
	//		long durationMsec = System.currentTimeMillis() - startMsec;
	//		System.out.println("Classification time = " + durationMsec + " msec.");
			Assert.assertTrue(matrix.getTotalSamples() == sampleCount);
			Assert.assertTrue(matrix.getF1Score().getMean() == 1.0);
		} finally {
			if (classifier instanceof Closeable)
				((Closeable)classifier).close();	
		}
	}
	
//	private final static String SerializationDirectory = "./test-data/serializations/models";
	private final static String SerializationLabel = "serialization-test";
//
//
//	private void generateTrainedSerialization() throws AISPException, IOException {
//		List<SoundRecording> trainingData = getTrainingRecordings(SerializationLabel);
//		IClassifier<double[]> classifier = getClassifier();
//		classifier.train(SerializationLabel, trainingData);
//		// Make sure the model is 100% accurate so we can verify the same accuracy on the deserialized model.
//		SoundTestUtils.verifyClassifications(classifier, trainingData, SerializationLabel);
//
//		// TODO: should make sure we're not overwriting.
//		ENGTestUtils.generateSerialization(SerializationDirectory, classifier);
//		
//	}
//
//
//	@Override	// To operating on a trained classifier and test its classifications.
//	@Test
//	public void testPastSerialization() throws Exception { 
//		
//		IClassifier<double[]> classifier = getClassifier(); 
//		List<Serializable> slist = ENGTestUtils.verifyPastSerializations(SerializationDirectory, classifier, false);
//		// If we haven't yet generated the serialization on disk (done primarily for new test classes), then generate it.
//		if (slist.size() == 0) {
//			AISPLogger.logger.info("Generating new serialization of trained model for class " + classifier.getClass().getSimpleName());
//			generateTrainedSerialization();
//			slist = ENGTestUtils.verifyPastSerializations(SerializationDirectory, classifier, true);
//		}
//
//		// Get the trained serialized model
//		Assume.assumeTrue("Test assumes only 1 classifier will be loaded", slist.size() == 1);
//		@SuppressWarnings("unchecked")
//		IClassifier<double[]> trainedClassifier = (IClassifier<double[]>)slist.get(0); 
//
//		// Get the recording the model was trained on (see generateTrainedSerialization())
//		List<SoundRecording> trainingData = getTrainingRecordings(SerializationLabel);
//		
//		// Verify the model is still classifying at 100% as was verified when generated.
//		SoundTestUtils.verifyClassifications(trainedClassifier, trainingData, SerializationLabel);
//		
//	}

	/**
	 * Get a trained classifier that, when deserialized, is verified with {@link #verifySerializableEquivalence(Serializable, Serializable)}.
	 */
	@Override
	protected Serializable getSerializableTestObject() throws Exception {
		IFixableClassifier<double[]> classifier = getClassifier();
		// Get the recording the model was trained on (see generateTrainedSerialization())
		List<SoundRecording> trainingData = getTrainingRecordings(SerializationLabel);
		classifier.train(SerializationLabel, trainingData);
		// Verify the model is still classifying at 100% as was verified when generated.
		SoundTestUtils.verifyClassifications(classifier, trainingData, SerializationLabel);
		return classifier;
	}

	/**
	 * Verify that the classifier can still classify the sounds it was trained (in {@link #getSerializableTestObject()}) correctly.
	 */
	@Override
	protected void verifySerializableEquivalence(Serializable ser, Serializable deserialized) throws Exception {
		IClassifier<double[]> trainedClassifier = (IClassifier<double[]>)deserialized;

		// Get the recording the model was trained on (see generateTrainedSerialization())
		List<SoundRecording> trainingData = getTrainingRecordings(SerializationLabel);
		
		// Verify the model is still classifying at 100% as was verified when generated.
		SoundTestUtils.verifyClassifications(trainedClassifier, trainingData, SerializationLabel);
		
	}


	
	@Test
	public void testSerializability() throws IOException, AISPException, ClassNotFoundException {
		Iterable<SoundRecording> srList;
		String trainingLabel = "source";
		srList = getTrainingRecordings(trainingLabel);
		testTrainAndSerialize(trainingLabel, srList);
	}

	@Test
	public void testTrainNoData() throws AISPException, IOException { 
	
			List<SoundRecording> srList = new ArrayList<SoundRecording>(); 
	
			// Train a model on the sounds 
			String trainingLabel = "somelabel";
	//		TrainingConfiguration tconfig = new TrainingConfiguration(trainingLabel);
			IClassifier classifier = getClassifier();
			try {
				classifier.train(trainingLabel, srList);
				Assert.fail("Did not get exception when training on no data"); 
			} catch (IllegalArgumentException e) {
				System.out.println("Got expected exception: " + e.getMessage());
			} catch (AISPException e) {
				System.out.println("Got expected exception: " + e.getMessage());
			} catch (Exception e) {
				Assert.fail("Got other than the expected exceptions");
			} finally {
				if (classifier instanceof Closeable)
					((Closeable)classifier).close();
			}
		}

	@Test
	public void testTrainNoLabel() throws AISPException, IOException, InterruptedException  {
	
		// Load the sounds into the server.
//		Iterable<SoundRecording> srList = SoundDB.loadSounds(dataDir, true);
//		Iterable<SoundRecording> srList = getTrainingRecordings("source", "normal", "abnormal"); 
		Iterable<SoundRecording> srList = getTrainingRecordings("source");

		// Train a model on the sounds 
		String trainingLabel = "badlabel";
//		TrainingConfiguration tconfig = new TrainingConfiguration(trainingLabel);
		IClassifier classifier = getClassifier();
		try {
			Assert.assertTrue(classifier.getTrainedLabel() == null);
			try {
				classifier.train(trainingLabel, srList);
				Assert.fail("Did not get exception when training on non-existent label");
			} catch (IllegalArgumentException e) {
				System.out.println("Got expected exception: " + e.getMessage());
			} catch (AISPException e) {
				System.out.println("Got expected exception: " + e.getMessage());
			}
			Assert.assertTrue(classifier.getTrainedLabel() == null);	// Should still be null.
		} finally {
			if (classifier instanceof Closeable)
				((Closeable)classifier).close();
		}
	}

	@Test
	public void testAutoTypeSetting() throws AISPException, IOException, InterruptedException  {
		String trainingLabel = "source";
		Iterable<SoundRecording> srList;
	
		// Create some pure signal sounds and train on them
		// srList = getTrainingRecordings(trainingLabel, "normal", "abnormal"); 
		srList = getTrainingRecordings(trainingLabel); 
		for (SoundRecording sr : srList) {
			DataTypeEnum.setDataTypeTag(sr, DataTypeEnum.VibrationMagnitude);
		}
		IClassifier<double[]> classifier = this.getClassifier();
		try {
			classifier.train(trainingLabel, srList);	// Training on mag data should set the model type to mag
			Assert.assertTrue(DataTypeEnum.getDataTypeTag(classifier).equals(DataTypeEnum.VibrationMagnitude));
		} finally {
			if (classifier instanceof Closeable)
				((Closeable)classifier).close();
		}
		
		if (classifier instanceof IFixableClassifier) {
			IFixedClassifier<double[]> fc = ((IFixableClassifier)classifier).getFixedClassifier();
			Assert.assertTrue(DataTypeEnum.getDataTypeTag(fc).equals(DataTypeEnum.VibrationMagnitude));
		}
	}

	/**
	 * Get the number of sounds samples used for each of the normal and abnormal signals used by {@link #testNormalAbnormalSignals(byte, int, byte, int)}.
	 * This is exposed for algorithms that need to test large numbers of samples to exercise all the code paths (i.e. DistanceNN algorithms).
	 * @param sampleLengthMsec the length of the samples being used to train the models.
	 * @return
	 */
	protected int getSeparatedSignalCounts(int sampleLengthMsec) {
		return Math.max(4, this.getMinimumTrainingDataPerClass());
	}

	@Test
	public void testSignalSeparation() throws AISPException, IOException, InterruptedException {
		int sampleDurationMsec = 1000;
		int count = getSeparatedSignalCounts(sampleDurationMsec); 
		IClassifier<double[]> classifier = getClassifier();
		try {
			ModelTestUtils.testSignalSeparation(classifier, sampleDurationMsec, count, 1.0, 0.0, 100, 1.0, 0.0, 1000); 	// Frequency change
			ModelTestUtils.testSignalSeparation(classifier, sampleDurationMsec, count, 1.0, 0.0, 500,  0.2, 0.0, 500); 	// amplitude change 
//		    testNormalAbnormalSignals(0.1, 0.9, 500, 0.1, -0.9, 500); 	// offset change, commented out because the classifiers should not distinguish sounds with different DC offsets
			ModelTestUtils.testSignalSeparation(classifier, sampleDurationMsec, count, 1.0, 0.0, 500, 0.5, -0.5, 500); 	// amplitude and offset change.
			ModelTestUtils.testSignalSeparation(classifier, sampleDurationMsec, count, 1.0, 0.0, 500, 0.5, -0.5, 1000); 	// amplitude and offset change and frequency change.

			// These started failing for DCASE when we changed the max threshold ratio in ExtendedFFT from 1e6 to 1e20,
			// so use the two above instead - dawood 12/2020
//			testNormalAbnormalSignals(0.1, 0.9, 500, 0.5, -0.5, 500); 	// amplitude and offset change.
//			testNormalAbnormalSignals(0.1, 0.9, 500, 0.5, -0.5, 1000); 	// amplitude, offset and frequency change.
		} finally {
			if (classifier instanceof Closeable)
				((Closeable)classifier).close();
		}
	}

	private IFixableClassifier<double[]>  testTrainAndSerialize(String trainingLabel, Iterable<SoundRecording> srList) throws IOException, AISPException {
		IFixableClassifier<double[]> classifier = getClassifier();
		try {
			classifier.train(trainingLabel, srList);
			SoundTestUtils.verifyClassifications(classifier, srList, trainingLabel, VerifyMode.CompareEqual);	// validate the model first.
			testSerializability(classifier, trainingLabel,srList);
			IFixedClassifier<double[]> fixed = classifier.getFixedClassifier();
			testSerializability(fixed, trainingLabel,srList);
		} finally {	
			if (classifier instanceof Closeable)
				((Closeable)classifier).close();
		}
		return classifier;
	}

	private void testSerializability(IFixedClassifier<double[]> classifier, String trainingLabel, Iterable<SoundRecording> srList) throws IOException, AISPException {
		byte[] serialized; 
		try {
			serialized = ClassUtilities.serialize(classifier);
//			AISPLogger.logger.info("Serialized instance of " + classifier.getClass().getName() + " is " + serialized.length + " bytes");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Could not serialize model. See console for stacktrace.");
			return;	// to keep the compiler quiet
		}

		try {
			Object o = ClassUtilities.deserialize(serialized);
			Assert.assertTrue(o instanceof IFixedClassifier);
			classifier = (IFixedClassifier<double[]>)o;
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Could not deserialize model. See console for stacktrace.");
		}

		SoundTestUtils.verifyClassifications(classifier, srList, trainingLabel);
	}


	@Override
	protected Cloneable getCloneable() {
		return null;	// Does not implement cloneable.
	}



}
