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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.IDataWindow.PadType;
import org.eng.aisp.classifier.AbstractClassifier;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.ConfusionMatrix;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.KFoldModelEvaluator;
import org.eng.aisp.classifier.TrainingSetInfo;
import org.eng.aisp.classifier.KFoldModelEvaluator.IPartitionListener;
import org.eng.aisp.classifier.TrainingSetInfo.LabelInfo;
import org.eng.aisp.classifier.TrainingSetInfo.LabelValueInfo;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.util.FixedDurationSoundRecordingIterable;
import org.eng.aisp.util.FixedDurationSoundRecordingShuffleIterable;
import org.eng.util.IShuffleIterable;
import org.eng.util.OnlineStats;
import org.eng.util.ShufflizingIterable;
import org.junit.Assert;
import org.junit.Test;



public class KFoldModelEvaluatorTest {
	
	List<Double> getStartTimes(Iterable<SoundRecording> recordings) {
		List<Double> startTimes= new ArrayList<Double>();
		for (SoundRecording sr : recordings) 
			startTimes.add(sr.getDataWindow().getStartTimeMsec());
		Collections.sort(startTimes);
		return startTimes;
	}
	

	/**
	 * A classifier that just returns successive values from a given array of label values.
	 * @author dawood
	 *
	 */
	private static class TestClassifier extends AbstractClassifier<double[]> implements IClassifier<double[]> {

		private static final long serialVersionUID = -1056467371010199351L;
		
		private final String[] labelValues ;
		private final String trainingLabel;
		private int labelIndex = 0;
		
		public TestClassifier(String trainingLabel, String[] labelValues) {
			this.trainingLabel = trainingLabel;
			this.labelValues = labelValues;
		}

		/**
		 * Just loop through the array of label values.
		 */
		@Override
		public Map<String,Classification> classify(IDataWindow<double[]> sample) throws AISPException {
			int localIndex = labelIndex % labelValues.length;
			String value = labelValues[localIndex];
			Classification c= new Classification(trainingLabel, value, 1.0);
			Map<String, Classification> cmap = new HashMap<>();
			cmap.put(trainingLabel, c);
			labelIndex++;
			return cmap;
		}

		/**
		 * Reset to have {@link #classify(IDataWindow)} next return the 1st element of the list of label values.
		 */
		@Override
		public void train(String trainingLabel, Iterable<? extends ILabeledDataWindow<double[]>> data) throws AISPException {
			labelIndex = 0;
		}
		

		@Override
		public String getTrainedLabel() {
			throw new RuntimeException("Not expected to be called during testing");
		}
	}

	/**
	 * A classifier that does NO TRAINING and always classifies to the undefined value (i.e. we don't care about accuracy).
	 */
	private static class DumbClassifier extends AbstractClassifier<double[]> implements IClassifier<double[]> {

		private static final long serialVersionUID = 1363532319172857618L;
		private String trainingLabel;

		@Override
		public Map<String, Classification> classify(IDataWindow<double[]> sample) throws AISPException {
			Classification c= new Classification(trainingLabel, Classification.UndefinedLabelValue, 1.0);
			Map<String, Classification> cmap = new HashMap<>();
			cmap.put(trainingLabel, c);
			return cmap;
		}

		@Override
		public String getTrainedLabel() {
			return trainingLabel;
		}

		@Override
		public void train(String trainingLabel, Iterable<? extends ILabeledDataWindow<double[]>> data) throws AISPException {
			this.trainingLabel = trainingLabel;
		}

	}

	@Test
	public void testConfusionMatrix() throws AISPException {
		String trainingLabel = "source";
		String labelValue = "constant";
		int startMsec = 0;
		int durationMsec = 500;
		int htz= 1000;
		boolean addNoise = true;
		int numSounds = 16;
		int folds = 4;
		KFoldModelEvaluator kfold;
		IClassifier<double[]> classifier;
		ConfusionMatrix matrix;
		OnlineStats acc; 

		// Create training sounds that all have the same label value.
		List<SoundRecording> sounds = new ArrayList<SoundRecording>();
		for (int i=0 ; i<numSounds ; i++) {
			Properties labels = new Properties();
			labels.setProperty(trainingLabel, labelValue); 
			SoundRecording sr = SoundTestUtils.createTrainingRecording(startMsec, durationMsec, htz, labels, addNoise);
			sounds.add(sr);
		}

		// Simple test to make sure we get back 100% accuracy when all labels are generated to match training data.
		kfold = new KFoldModelEvaluator(folds);
		classifier = new TestClassifier(trainingLabel, new String[] { labelValue} );
		matrix = kfold.getConfusionMatrix(classifier, sounds,trainingLabel,folds,0);
		acc = matrix.getAccuracy();
		Assert.assertTrue(acc.getMean() == 1);

		// Simple test to make sure we get back 50%% accuracy when classify() returns 2 different values. 
		kfold = new KFoldModelEvaluator(folds);
		classifier = new TestClassifier(trainingLabel, new String[] { labelValue, labelValue + "1"});
		matrix = kfold.getConfusionMatrix(classifier, sounds, trainingLabel, folds, 0);
		acc = matrix.getAccuracy();
		Assert.assertTrue(acc.getMean() == 0.5);
		
	}
	

	@Test
	public void testSmallDataFolds() throws AISPException {
		int folds = KFoldModelEvaluator.DEFAULT_FOLD_COUNT;
		int soundCount = folds;		
		String labelName = "index";
		String trainingLabel = "training";
		List<SoundRecording> sounds = getIndexLabeledSounds(soundCount,labelName, trainingLabel, 1);
		KFoldModelEvaluator kfold; 
		List<SoundRecording> training, test;
		PartitionListener listener ;
		IClassifier<double[]> classifier = new DumbClassifier();
		
		kfold = new KFoldModelEvaluator(folds);
		for (int i=0 ; i<folds ; i++) {
			listener = new PartitionListener(trainingLabel);
			kfold.getConfusionMatrix(classifier, sounds, trainingLabel, i+1, 0, listener);
			training = SoundTestUtils.iterable2List(listener.trainingData.get(i));
			test = SoundTestUtils.iterable2List(listener.testData.get(i));
			Assert.assertTrue(training.size() != 0);
			Assert.assertTrue(test.size() != 0);
		}

	}
	
	private class PartitionListener implements IPartitionListener<double[], SoundRecording> {

		
		List<List<SoundRecording>> trainingData;
		List<List<SoundRecording>> testData;
		private String trainingLabel;

		PartitionListener(String trainingLabel) {
			this.trainingData = new ArrayList<List<SoundRecording>>();
			this.testData = new ArrayList<List<SoundRecording>>();
			this.trainingLabel = trainingLabel;
		}

		@Override
		public boolean partitioned(Iterable<SoundRecording> trainingData, Iterable<SoundRecording> testData) {
			this.trainingData.add(SoundTestUtils.iterable2List(trainingData));
			this.testData.add(SoundTestUtils.iterable2List(testData));
//			AISPLogger.logger.info("Training partition \n" + TrainingSetInfo.getInfo(trainingData).getLabelInfo(trainingLabel).prettyFormat());
//			AISPLogger.logger.info("Test partition \n" + TrainingSetInfo.getInfo(testData).getLabelInfo(trainingLabel).prettyFormat());
//			System.out.println("\n");
			return false;
		}
	}
	
	@Test
	public void testTrainingAndTestDataUnshuffled() throws AISPException {
		this.testTrainingAndTestData(false, false);
	}

	@Test
	public void testTrainingAndTestDataShuffled() throws AISPException {
		this.testTrainingAndTestData(true, false);
	}

	@Test
	public void testBalancedTrainingAndTestDataShuffled() throws AISPException {
		this.testTrainingAndTestData(true, true);
	}

	@Test
	public void testBalancedTrainingAndTestDataUnshuffled() throws AISPException {
		this.testTrainingAndTestData(false,true);
	}
	

	private void testTrainingAndTestData(boolean shuffle, boolean testBalancing) throws AISPException {
		for (int folds=3 ; folds< 6 ; folds++) {
			testTrainingAndTestData	(folds, shuffle, testBalancing);
		}
	}

	private void testTrainingAndTestData(int folds, boolean shuffle, boolean testBalancing) throws AISPException {
		int soundCount = 64;	// A power of two works nicely
		boolean foldsDivideSoundCount = soundCount % folds == 0;
		int samplesPerLabelValue ;
		int balancedTrainingCount; 
		String indexLabelName = "index";
		int expectedTrainingSize; 
		int expectedTestSize;
		String trainingLabel = "training";
		
		if (testBalancing) {
			samplesPerLabelValue = 0;
			balancedTrainingCount = 5;	 	// Something that doesn't divided soundCount to make test harder.. 
		} else {
			samplesPerLabelValue = 8;		// Something that divides soundCount
			balancedTrainingCount = 0; 
		}

		Iterable<SoundRecording> sounds = getIndexLabeledSounds(soundCount,indexLabelName, trainingLabel, samplesPerLabelValue);
		int minTestSize, minTrainingSize;
		List<String> partitionableLabelValues = getPartitionableLabelValues(sounds, trainingLabel, folds); 
		int labelValueCount =  partitionableLabelValues.size();
		if (testBalancing) {
			expectedTrainingSize = (folds-1) * balancedTrainingCount * labelValueCount; 
			expectedTestSize = 0;	// Can't determine this . 
			minTrainingSize = expectedTrainingSize;
			minTestSize = labelValueCount;
		} else {
			expectedTrainingSize = (folds -1) * soundCount / folds;
			expectedTestSize = soundCount - expectedTrainingSize;
			minTrainingSize =   (folds - 2) * soundCount / folds;
			int minLabelCount = getMinLabelValueCount(sounds, trainingLabel, partitionableLabelValues);
			minTestSize = minLabelCount / folds  * labelValueCount; 
		}

		if (shuffle) {
			IShuffleIterable<SoundRecording> shuffledSounds = new ShufflizingIterable<SoundRecording>(sounds);
			sounds = shuffledSounds.shuffle();
		}
		List<SoundRecording> soundList = SoundTestUtils.iterable2List(sounds); 
		IClassifier<double[]> classifier = new DumbClassifier();
		PartitionListener listener; 
	
//		AISPLogger.logger.info("Full set\n" + TrainingSetInfo.getInfo(sounds).getLabelInfo(trainingLabel).prettyFormat());

		KFoldModelEvaluator kfold = new KFoldModelEvaluator(folds);
		listener = new PartitionListener(trainingLabel);
		kfold.getConfusionMatrix(classifier, sounds, trainingLabel, folds, balancedTrainingCount, listener);
		for (int fold=0 ; fold < folds ; fold++) {
//			System.out.println("fold=" + fold);
			List<SoundRecording> training, test;
			
			/** First test full, unfiltered folds */
			training = listener.trainingData.get(fold);
			test = listener.testData.get(fold);
			int actualTrainingSize = training.size();
			int actualTestingSize = test.size();
//			AISPLogger.logger.info("Training\n" + TrainingSetInfo.getInfo(training).prettyFormat());
//			AISPLogger.logger.info("Test\n" + TrainingSetInfo.getInfo(test).prettyFormat());
				
			String msg = "fold=" + fold + " of " + folds;
//						System.out.println("\n" + msg);
//						System.out.println("all data times=" + allTimes + ", size=" + allTimes.size());
//						System.out.println("training data times=" + trainingTimes + ", size=" + trainingTimes.size());
//						System.out.println("testing  data times=" + testingTimes + ", size=" + testingTimes.size());

			if (testBalancing) {
				Assert.assertTrue(actualTestingSize >= minTestSize);
				Assert.assertTrue(actualTrainingSize == expectedTrainingSize); 
			} else {
				if (foldsDivideSoundCount) {
					Assert.assertTrue(actualTrainingSize == expectedTrainingSize); 
					Assert.assertTrue(actualTestingSize == expectedTestSize); 
				} else {
					Assert.assertTrue(actualTrainingSize >= minTrainingSize); 
					Assert.assertTrue(actualTestingSize >=  minTestSize); 
				}
				Assert.assertTrue(soundList.size() == actualTestingSize + actualTrainingSize); 
			}

			// Make sure there is no overlap between training and testing
			validateAsDisjoint(training,test, indexLabelName);
		}
	}

	/**
	 * Find the count of samples for the given labels values that is lowest among them.
	 * @param sounds
	 * @param trainingLabel
	 * @param labelValues
	 * @return
	 */
	private int getMinLabelValueCount(Iterable<SoundRecording> sounds, String trainingLabel, List<String> labelValues) {
		TrainingSetInfo tsi = TrainingSetInfo.getInfo(sounds);
		LabelInfo linfo = tsi.getLabelInfo(trainingLabel);
		int min= Integer.MAX_VALUE;
		for (LabelValueInfo lvi : linfo) {
			if (labelValues.contains(lvi.getLabelValue()) && lvi.getTotalSamples() < min)
				min = lvi.getTotalSamples();
		}
		return min; 
	}


	/**
	 * Get the label values that the KFold evaluator will spread across the partitions.  It ignores any label values
	 * that have fewer samples than the number of folds so we count up the label values that have at least fold samples.
	 * @param sounds
	 * @param folds
	 * @return
	 */
	private List<String> getPartitionableLabelValues(Iterable<SoundRecording> sounds, String trainingLabel, int folds) {
		TrainingSetInfo tsi = TrainingSetInfo.getInfo(sounds);
		List<String> valueList = new ArrayList<String>();
		LabelInfo linfo = tsi.getLabelInfo(trainingLabel);
		int count = 0;
		for (LabelValueInfo lvi : linfo) {
			if (lvi.getTotalSamples() >= folds)
				valueList.add(lvi.getLabelValue());
		}
		return valueList;
	}


	//	private void testTrainingAndTestData(Iterable<SoundRecording> sounds, int soundCount, String indexLabelName, String trainingLabelName) throws AISPException {
//		int folds=4;	// Another power of two works nicely
//		int fullTrainingSize = (folds -1) * soundCount / folds;
//		int fullTestSize = soundCount - fullTrainingSize;
//		List<SoundRecording> soundList = SoundTestUtils.iterable2List(sounds); 
//		IClassifier<double[]> classifier = new DumbClassifier();
//		PartitionListener listener; 
//	
//		AISPLogger.logger.info("Full set\n" + TrainingSetInfo.getInfo(sounds).prettyFormat());
//
//		KFoldModelEvaluator kfold = new KFoldModelEvaluator(folds);
//		listener = new PartitionListener();
//		kfold.getConfusionMatrix(classifier, sounds, trainingLabelName, folds, 0, listener);
//
//		for (int fold=0 ; fold < folds ; fold++) {
////			System.out.println("fold=" + fold);
//			List<SoundRecording> training, test;
//			
//			/** First test full, unfiltered folds */
//			training = listener.trainingData.get(fold);
//			test = listener.testData.get(fold);
////			AISPLogger.logger.info("Training\n" + TrainingSetInfo.getInfo(training).prettyFormat());
////			AISPLogger.logger.info("Test\n" + TrainingSetInfo.getInfo(test).prettyFormat());
//			Assert.assertTrue(training.size() == fullTrainingSize); 
//			Assert.assertTrue(test.size() == fullTestSize); 
//			// Make sure there is no overlap between training and testing
//			Assert.assertTrue(soundList.size() == training.size() + test.size());
//			validateAsDisjoint(training,test, indexLabelName);
//
//
//		}
//	}
	private void validateAsDisjoint(List<SoundRecording> training, List<SoundRecording> test, String indexLabelName) {
//		System.out.println("training:" + getLabels(training, labelName));
//		System.out.println("testing :" + getLabels(test, labelName));
		for (SoundRecording trainSR : training) {
			Properties trainLabels = trainSR.getLabels();
			String trainLabelValue = trainLabels.getProperty(indexLabelName);
			Assert.assertTrue(trainLabelValue != null);
			for (SoundRecording testSR : test) {
				Properties testLabels = testSR.getLabels();
				String testLabelValue = testLabels.getProperty(indexLabelName);
				Assert.assertTrue(testLabelValue != null);
				Assert.assertTrue(!trainLabelValue.equals(testLabelValue));
			}
		}
		
	}

//	private static List<String> getLabels(List<SoundRecording> sounds, String labelName) {
//		List<String> labels = new ArrayList<String>();
//		for (SoundRecording sound: sounds) {
//			Properties trainLabels = sound.getLabels();
//			String labelValue = trainLabels.getProperty(labelName);
//			labels.add(labelValue);
//		}
//		Collections.sort(labels);
//		return labels;
//	}

	/**
	 * Get a list of sounds with the index in the given label name.
	 * @param count
	 * @param indexLabel label name to hold index values.
	 * @param trainingLabel	 the training label to attach to the sounds.
	 * @param trainingLabelValues if > 0 then generate a max of this many training label values.  If 0, then generate a varied number of label values.
	 * @return list with count SoundRecordings, each with a different index value.
	 */
	private List<SoundRecording> getIndexLabeledSounds(int count, String indexLabel, String trainingLabel, int trainingLabelValues) {
		if (trainingLabelValues > 0 && count < trainingLabelValues) 
			throw new IllegalArgumentException("trainingLabelValues must be less or equal to count");
		List<SoundRecording> sounds = new ArrayList<SoundRecording>();
		SoundClip clip = SoundTestUtils.createClips(1, 0, 0, 100, 0).get(0);
		int targetLabelValueCount = 1, currentLabelValueCount = 0;
		String trainingLabelValue = null;
		for (int i=0 ; i<count ; i++) {
			Properties labels = new Properties();
			labels.put(indexLabel, String.valueOf(i));
			if (trainingLabelValues > 0) {
				trainingLabelValue = String.valueOf(i % trainingLabelValues);
			} else if (trainingLabelValue == null || currentLabelValueCount == targetLabelValueCount) {
				trainingLabelValue = String.valueOf(i); 
				targetLabelValueCount += targetLabelValueCount;
				currentLabelValueCount = 0;
			}
//			System.out.println("label = " + trainingLabelValue);
			labels.put(trainingLabel, trainingLabelValue);
			SoundRecording sr = new SoundRecording(clip, labels);
			sounds.add(sr);
			currentLabelValueCount++;
		}
		return sounds;
	}
	
	/**
	 * Tests to makes sure that evaluation can handle when a large portion of the input data does not contain the label name
	 * being used to evaluate the model.
	 * This was originally created to test issue 435 which throws an exception if a fold did not have data with the evaluated label.
	 * There does not seem to be a way to guarantee that a a fold does not have the target label, but this was tested to generate
	 * the exception w/o the use of LabeledWindow(Shuffle)Iterable.
	 * @throws AISPException
	 */
	@Test
	public void testMissingLabelData() throws AISPException {
		String label = "label1";
		String labelValue = "value1";
		int folds = 10;		// Probability of a fold w/o the target label increases with the # of folds.
		int labeledCount = 10*folds;	// Enough labeled data to fill folds if we only use data with the target label.
		int unlabeledCount = 10*labeledCount;	
		ConfusionMatrix cm;
		
		// Create some data with the label beign evaluated.
		Properties labels = new Properties(); 
		labels.setProperty(label, labelValue);
		List<SoundRecording> labeledSounds = SoundTestUtils.createTrainingRecordings(labeledCount, 100, 1000, labels, false);

		// Create lots more data w/o the label being evaluated so we get folds that do not include the label if we don't consider only the data with the target labels.
		List<SoundRecording> allSounds = SoundTestUtils.createTrainingRecordings(unlabeledCount, 100, 1000, new Properties(), false);
		allSounds.addAll(labeledSounds);

		// Create a classifier that always returns the labelValue for all sounds.  We don't really care what values are returned.
		TestClassifier classifier = new TestClassifier(label, new String[] { labelValue});

		// Now evaluate with the assumption that some folds will not have any sounds containing the label being evaluated.
		KFoldModelEvaluator kfold = new KFoldModelEvaluator(folds);

		// This throws an exeption in issue 435.
		cm = kfold.getConfusionMatrix(classifier, allSounds, label,folds,0);

		// Some basic tests, but if we get this far, we've avoid issue 435.
		Assert.assertTrue(cm != null);
		Assert.assertTrue(cm.getAccuracy().getMean() == 1.0);
		
		// Re-run with a shuffle iterable which is handled differently in KFold. 
		cm = kfold.getConfusionMatrix(classifier, new ShufflizingIterable<SoundRecording>(allSounds), label,folds,0);

		// Some basic tests, but if we get this far, we've avoid issue 435.
		Assert.assertTrue(cm != null);
		Assert.assertTrue(cm.getAccuracy().getMean() == 1.0);
	}
	
	@Test
	public void testClippedUnbalancedData() throws AISPException {
		testClippedUnbalancedData(false,false);
		testClippedUnbalancedData(true,false);
		testClippedUnbalancedData(false,true);
		testClippedUnbalancedData(true,true);
	}

	public void testClippedUnbalancedData(boolean shuffle, boolean useClipping) throws AISPException {
		String trainingLabel = "trainlabel";
		String normalLabelValue = "normal";
		String abnormalLabelValue = "abnormal";
		int labelValueCount = 2;
		int folds = 4;
		int abnormalSamplesPerFold = 2;
		int normalSamplesPerFold = 100 *abnormalSamplesPerFold;
		int abnormalCount= abnormalSamplesPerFold*folds, normalCount = normalSamplesPerFold * folds ; 
		/** The number of samples for each label value in each fold. See KfoldModelEvaluator */
		int balancePerLabelValueTrainingCountPerFold = 100;
		int sampleDurationMsec = 100, clipLenMsec = 10;

		Iterable<SoundRecording> sounds = SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, normalLabelValue, 
							sampleDurationMsec, normalCount, abnormalLabelValue, abnormalCount);
		if (shuffle)  {
			IShuffleIterable<SoundRecording> shuffledSounds = new ShufflizingIterable<SoundRecording>(sounds);
			sounds = shuffledSounds;
			if (useClipping) 
				sounds = new FixedDurationSoundRecordingShuffleIterable(shuffledSounds, clipLenMsec, PadType.DuplicatePad);
		} else if (useClipping) {
			sounds = new FixedDurationSoundRecordingIterable(sounds, clipLenMsec, PadType.DuplicatePad);
		}
		if (useClipping) {
			int multiplier = sampleDurationMsec / clipLenMsec;
			abnormalCount *= multiplier; 
			normalCount *= multiplier; 
		}

		boolean verbose = false;
		KFoldModelEvaluator kfold = new KFoldModelEvaluator(folds, verbose);
		PartitionListener listener = new PartitionListener(trainingLabel);
		kfold.getConfusionMatrix(new DumbClassifier(), sounds, trainingLabel, folds, balancePerLabelValueTrainingCountPerFold, listener);
		int expectedAbormalTestCount = abnormalCount / folds;
		int expectedNormalTestCount = normalCount / folds;
		int expectedAbormalTrainCount = (folds - 1) * balancePerLabelValueTrainingCountPerFold; 
		int expectedNormalTrainCount  = (folds - 1) * balancePerLabelValueTrainingCountPerFold;
		for (int fold =0 ; fold<folds ; fold++) {
			List<SoundRecording> train = SoundTestUtils.iterable2List(listener.trainingData.get(fold));
			List<SoundRecording> test = SoundTestUtils.iterable2List(listener.testData.get(fold));
			TrainingSetInfo trainInfo = TrainingSetInfo.getInfo(train);
			TrainingSetInfo testInfo = TrainingSetInfo.getInfo(test);
			LabelInfo trainLabelInfo = trainInfo.getLabelInfo(trainingLabel);
			LabelInfo testLabelInfo = testInfo.getLabelInfo(trainingLabel);
			LabelValueInfo trainNormalInfo = trainLabelInfo.getLabelInfo(normalLabelValue);
			LabelValueInfo trainAbormalInfo = trainLabelInfo.getLabelInfo(abnormalLabelValue);
			LabelValueInfo testNormalInfo = testLabelInfo.getLabelInfo(normalLabelValue);
			LabelValueInfo testAbormalInfo = testLabelInfo.getLabelInfo(abnormalLabelValue);
			
			Assert.assertTrue(trainNormalInfo.getTotalSamples() == expectedNormalTrainCount);
			Assert.assertTrue(trainAbormalInfo.getTotalSamples() == expectedAbormalTrainCount);
			Assert.assertTrue(testNormalInfo.getTotalSamples() == expectedNormalTestCount);
			Assert.assertTrue(testAbormalInfo.getTotalSamples() == expectedAbormalTestCount);
		}

	}
}
