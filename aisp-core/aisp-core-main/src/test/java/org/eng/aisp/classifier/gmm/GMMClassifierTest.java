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
package org.eng.aisp.classifier.gmm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.classifier.AbstractMulticlassClassifierTest;
import org.eng.aisp.classifier.IFixableClassifier;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.classifier.gaussianmixture.FixedGMMClassifier;
import org.eng.aisp.classifier.gmm.GMMClassifier;
import org.eng.aisp.classifier.gmm.GMMClassifierBuilder;
import org.eng.aisp.dataset.LabeledSoundFiles;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.feature.processor.vector.DeltaFeatureProcessor;
import org.eng.util.IShuffleIterable;
import org.eng.util.IthShuffleIterable;
import org.junit.Test;

public class GMMClassifierTest extends AbstractMulticlassClassifierTest {

	@Override
	protected IFixableClassifier<double[]> getClassifier() {
		return new GMMClassifier();
	}
	
	protected IFixableClassifier<double[]> getClassifier(String trainingLabel, int numGaussians) {
		
		IFeatureProcessor<double[]> processor = new DeltaFeatureProcessor(2, new double[]{1,1,1}); 
		//failed:
//		return new GMMClassifier(trainingLabel, GMMClassifier.DEFAULT_FEATURE_EXTRACTOR, processor, 40, 20, false, numGaussians, false);
	
//		return new GMMClassifier(GMMClassifier.DEFAULT_TRANSFORMS, 
//				GMMClassifier.DEFAULT_FEATURE_EXTRACTOR, 
//				GMMClassifier.DEFAULT_FEATURE_PROCESSOR, 
//				GMMClassifier.DEFAULT_WINDOW_SIZE_MSEC, 
//				GMMClassifier.DEFAULT_WINDOW_SHIFT_MSEC, GMMClassifier.DEFAULT_USE_DISK_CACHE, 
//				numGaussians, GMMClassifier.DEFAULT_USE_DIAGONAL_COVARIANCE, 
//				GMMClassifier.DEFAULT_UNKNOWN_THRESH_COEFF);
		return new GMMClassifierBuilder().build();

	}
	
	protected IFixableClassifier<double[]> getClassifierWithoutProcessor(String trainingLabel, int numGaussians, boolean diagonalCMatrix) {
		
		return new GMMClassifier(GMMClassifier.DEFAULT_TRANSFORMS, 
				GMMClassifier.DEFAULT_FEATURE_EXTRACTOR, 
				GMMClassifier.DEFAULT_FEATURE_PROCESSOR, 
				GMMClassifier.DEFAULT_WINDOW_SIZE_MSEC, 
				GMMClassifier.DEFAULT_WINDOW_SHIFT_MSEC, GMMClassifier.DEFAULT_USE_DISK_CACHE, 
				numGaussians, diagonalCMatrix, 
				GMMClassifier.DEFAULT_UNKNOWN_THRESH_COEFF);

	}
	
	protected IFixableClassifier<double[]> getClassifierWithDeltaProcessor(String trainingLabel, int numGaussians, boolean diagonalCMatrix) {
		
		IFeatureProcessor<double[]> processor = new DeltaFeatureProcessor(2, new double[]{1,1,1}); 
		
		return new GMMClassifier(GMMClassifier.DEFAULT_TRANSFORMS, 
				GMMClassifier.DEFAULT_FEATURE_EXTRACTOR, 
				processor, 
				GMMClassifier.DEFAULT_WINDOW_SIZE_MSEC, 
				GMMClassifier.DEFAULT_WINDOW_SHIFT_MSEC, GMMClassifier.DEFAULT_USE_DISK_CACHE, 
				numGaussians, diagonalCMatrix, 
				GMMClassifier.DEFAULT_UNKNOWN_THRESH_COEFF);

	}
	
	@Override
	protected IFixableClassifier<double[]> getUnknownClassifier() {
		//Use a different GMM classifier configuration for detecting known vs. unknown.
		return null;	// GMM does not support outliers.
//		return new TwoPhaseClassifier(this.getClassifier(),
//				new GMMClassifier(null, new MFCCFeatureExtractor(20), null, 1000, 500, false, 10, true, 0.01));
	}
	
	@Test
	public void testModelMerging() throws IOException, AISPException {
		// TODO This test should be moved to some abstract class after an interface for mergeable classifier is defined.
		testModelMergingHelper(true);
		testModelMergingHelper(false);
	}

	
	//@Test
	public void testMergingModelsWithDifferentCovarianceMatrix() throws IOException, AISPException{
		
		// subset of sounds from DCASE task2
		String dirTask2_5cls = "../../iot-sounds/Sounds/dcase2016_task2/5classes";

		String dir = dirTask2_5cls;
		String labelName = "source";

		IShuffleIterable<SoundRecording>  sounds = LabeledSoundFiles.loadMetaDataSounds(dir, false);

		IShuffleIterable<SoundRecording> trainingSounds_p1 = new IthShuffleIterable<SoundRecording>(sounds, 2, 0, false, -1, 0);
		IShuffleIterable<SoundRecording> trainingSounds_p2 = new IthShuffleIterable<SoundRecording>(sounds, 2, 1, false, -1, 0);

		IFixableClassifier<double[]> classifier1 = this.getClassifierWithDeltaProcessor(labelName, 8, true);
		IFixableClassifier<double[]> classifier2 = this.getClassifierWithDeltaProcessor(labelName, 8, true);
		
		trainAndEvaluateTwoClassigfiersAndMergedClassifier(labelName, trainingSounds_p1, trainingSounds_p2, classifier1, classifier2);

		classifier1 = this.getClassifierWithDeltaProcessor(labelName, 8, false);
		classifier2 = this.getClassifierWithDeltaProcessor(labelName, 8, false);
		
//		// fails when model uses full covariance matrix for dcase16_task2/5classes
//		trainAndEvaluateTwoClassigfiersAndMergedClassifier(labelName, trainingSounds_p1, trainingSounds_p2, classifier1, classifier2);
		
		
		classifier1 = this.getClassifierWithDeltaProcessor(labelName, 8, true);
		classifier2 = this.getClassifierWithDeltaProcessor(labelName, 8, false);
		
//		// Fails when covariance matrices are different for the models for dcase16_task2/5classes (true-false or false-true model pairs)
//		trainAndEvaluateTwoClassigfiersAndMergedClassifier(labelName, trainingSounds_p1, trainingSounds_p2, classifier1, classifier2);
		
		// soounds from home
		dir = "test-data/home";
		labelName = "cause";

		IShuffleIterable<SoundRecording>  soundsHome = LabeledSoundFiles.loadMetaDataSounds(dir, false);

		IShuffleIterable<SoundRecording> trainingSounds_Homep1 = new IthShuffleIterable<SoundRecording>(soundsHome, 2, 0, false, -1, 0);
		IShuffleIterable<SoundRecording> trainingSounds_Homep2 = new IthShuffleIterable<SoundRecording>(soundsHome, 2, 1, false, -1, 0);

		IFixableClassifier<double[]> classifierHome1 = this.getClassifierWithoutProcessor(labelName, 8, false);
		IFixableClassifier<double[]> classifierHome2 = this.getClassifierWithoutProcessor(labelName, 8, true);
		
		//fails when first model with diagonal matrix (true), and second model with full matrix (false), not fails the other way around
		trainAndEvaluateTwoClassigfiersAndMergedClassifier(labelName, trainingSounds_Homep1, trainingSounds_Homep2, classifierHome1, classifierHome2);
		
	}

	/**
	 * Receives a label on which two classifiers have been trained on, 
	 * two classifier instances, and two sets of
	 * sounds where classifiers will be trained on.
	 * 
	 * Test each classifier individually and then create and test a new 
	 * classifier result of merging the two given classifiers. 
	 *
	 * **/
	private void trainAndEvaluateTwoClassigfiersAndMergedClassifier(String labelName, IShuffleIterable<SoundRecording> soundsClasif1,
			IShuffleIterable<SoundRecording> soundsClasif2, IFixableClassifier<double[]> classifier1,
			IFixableClassifier<double[]> classifier2) throws AISPException {
		
		classifier1.train(labelName, soundsClasif1);
		
		for (SoundRecording sr: soundsClasif1)
			SoundTestUtils.verifyClassification(classifier1, sr, labelName, SoundTestUtils.VerifyMode.CompareEqual, sr.getLabels().getProperty(labelName));

		classifier2.train(labelName, soundsClasif2);
		
		for (SoundRecording sr: soundsClasif2)
			SoundTestUtils.verifyClassification(classifier2, sr, labelName, SoundTestUtils.VerifyMode.CompareEqual, sr.getLabels().getProperty(labelName));

		IFixedClassifier<double[]> mergedClassifier = (IFixedClassifier<double[]>) FixedGMMClassifier.merge(
				(FixedGMMClassifier)classifier1.getFixedClassifier(), (FixedGMMClassifier)classifier2.getFixedClassifier());
		
		List<SoundRecording> testingSoundsAll = new ArrayList<>();
		
		for (SoundRecording sr : soundsClasif1){
			testingSoundsAll.add(sr);
		}
		
		for (SoundRecording sr : soundsClasif2){
			testingSoundsAll.add(sr);
		}
		
		for (SoundRecording sr: testingSoundsAll)
			SoundTestUtils.verifyClassification(mergedClassifier, sr, labelName, SoundTestUtils.VerifyMode.CompareEqual, sr.getLabels().getProperty(labelName));
	}
	
	//@Test
	public void testMergingModelsWithDifferentGaussians() throws IOException, AISPException{
		
		
		String dir = "../../iot-sounds/Sounds/dcase2016_task2/5classes";
		String labelName = "source";

		IShuffleIterable<SoundRecording>  sounds = LabeledSoundFiles.loadMetaDataSounds(dir, false);
		IShuffleIterable<SoundRecording> trainingSounds_p1 = new IthShuffleIterable<SoundRecording>(sounds, 2, 0, false, -1, 0);
		IShuffleIterable<SoundRecording> trainingSounds_p2 = new IthShuffleIterable<SoundRecording>(sounds, 2, 1, false, -1, 0);

		IFixableClassifier<double[]> classifier1 = this.getClassifierWithDeltaProcessor(labelName, 3, true);
		IFixableClassifier<double[]> classifier2 = this.getClassifierWithDeltaProcessor(labelName, GMMClassifier.DEFAULT_NUM_GAUSSIANS, true);
		// FAILS on dcase2016 task2 5classes if uses full covariance matrix for GMM model
		trainAndEvaluateTwoClassigfiersAndMergedClassifier(labelName, trainingSounds_p1, trainingSounds_p2, classifier1, classifier2);

		
		dir = "test-data/home";
		labelName = "cause";

		IShuffleIterable<SoundRecording>  soundsHome = LabeledSoundFiles.loadMetaDataSounds(dir, false);
		IShuffleIterable<SoundRecording> trainingSounds_homep1 = new IthShuffleIterable<SoundRecording>(soundsHome, 2, 0, false, -1, 0);
		IShuffleIterable<SoundRecording> trainingSounds_homep2 = new IthShuffleIterable<SoundRecording>(soundsHome, 2, 1, false, -1, 0);

		IFixableClassifier<double[]> classifierHome1 = this.getClassifierWithoutProcessor(labelName, 3, true);
		IFixableClassifier<double[]> classifierHome2 = this.getClassifierWithoutProcessor(labelName, GMMClassifier.DEFAULT_NUM_GAUSSIANS, true);
		
		trainAndEvaluateTwoClassigfiersAndMergedClassifier(labelName, trainingSounds_homep1, trainingSounds_homep2, classifierHome1, classifierHome2);
				
	}
	
	public void testModelMergingHelper(boolean matching) throws IOException, AISPException {
		String dir = "test-data/home"; 
		String labelName = "cause";
		String[] labelValues;
		
		labelValues = new String[] { "keys",  "silence", "fan", "clapping" };
		
		IShuffleIterable<SoundRecording>  sounds = LabeledSoundFiles.loadSounds(dir, true);

		IFixableClassifier<double[]> classifier1 = this.getClassifier();
		List<SoundRecording> trainingSounds1 = new ArrayList<>();
		if (matching) {
			trainingSounds1.addAll(getMatchingSounds(sounds, labelName, labelValues[0], true));	
			trainingSounds1.addAll(getMatchingSounds(sounds, labelName, labelValues[1], true));
		} else {
			trainingSounds1.addAll(getMatchingSounds(sounds, labelName, labelValues[0], false));	// All except labelValues[0]
		}
		classifier1.train(labelName, trainingSounds1);
		for (SoundRecording sr: trainingSounds1)
			SoundTestUtils.verifyClassification(classifier1, sr, labelName, SoundTestUtils.VerifyMode.CompareEqual, sr.getLabels().getProperty(labelName));

		IFixableClassifier<double[]> classifier2 = this.getClassifier();
		List<SoundRecording> trainingSounds2 = new ArrayList<>();
		if (matching) {
			trainingSounds2.addAll(getMatchingSounds(sounds, labelName, labelValues[2], true));	
			trainingSounds2.addAll(getMatchingSounds(sounds, labelName, labelValues[3], true));
		} else {
			trainingSounds2.addAll(getMatchingSounds(sounds, labelName, labelValues[1], false));	// All except labelValues[1]
		}
		classifier2.train(labelName, trainingSounds2);
		for (SoundRecording sr: trainingSounds2)
			SoundTestUtils.verifyClassification(classifier2, sr, labelName, SoundTestUtils.VerifyMode.CompareEqual, sr.getLabels().getProperty(labelName));

		// The below line is what is currently used for merging two GMM classifiers
		IFixedClassifier<double[]> classifierMerged = (IFixedClassifier<double[]>) FixedGMMClassifier.merge(
				(FixedGMMClassifier)classifier1.getFixedClassifier(), (FixedGMMClassifier)classifier2.getFixedClassifier());
		List<SoundRecording> trainingSoundsAll = new ArrayList<>();
		trainingSoundsAll.addAll(trainingSounds1);
		trainingSoundsAll.addAll(trainingSounds2);
		for (SoundRecording sr: trainingSoundsAll)
			SoundTestUtils.verifyClassification(classifierMerged, sr, labelName, SoundTestUtils.VerifyMode.CompareEqual, sr.getLabels().getProperty(labelName));

	}

}
