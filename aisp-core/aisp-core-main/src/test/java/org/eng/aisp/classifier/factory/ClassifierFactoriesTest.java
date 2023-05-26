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
package org.eng.aisp.classifier.factory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.IFixableClassifier;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.classifier.JunitClassifier;
import org.eng.aisp.classifier.anomaly.normal.NormalDistributionAnomalyClassifier;
import org.eng.aisp.classifier.cnn.CNNClassifier;
import org.eng.aisp.classifier.dcase.DCASEClassifier;
import org.eng.aisp.classifier.factory.ClassifierFactories;
import org.eng.aisp.classifier.gmm.GMMClassifier;
import org.eng.aisp.classifier.knn.merge.EuclidianDistanceMergeKNNClassifier;
import org.eng.aisp.classifier.knn.merge.L1DistanceMergeKNNClassifier;
import org.eng.aisp.classifier.knn.merge.LpDistanceMergeKNNClassifier;
import org.junit.Assert;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

public class ClassifierFactoriesTest {


	
	@Test
	public void testJS_AllDefault() throws AISPException {
		String extension = ".js";
		modelLoaderHelper(extension);
	}
	
	@Test
	public void testJSYT_AllDefault() throws AISPException {
		String extension = ".jsyt";
		modelLoaderHelper(extension);
	}

	/**
	 * @param extension
	 */
	protected void modelLoaderHelper(String extension) {
		String pkgName = this.getClass().getPackage().getName();
		Reflections reflections = new Reflections(pkgName, new ResourcesScanner());
		// Reflections reflections = new
		// Reflections("org.eng.aisp.classifier.factories", new
		// ResourcesScanner());
		Set<String> fileNames = reflections.getResources(Pattern.compile(".*\\" + extension));

		for (String js : fileNames) {
			String baseName = js.replaceAll("\\" + extension, "");
			baseName = new File(baseName).getName();
			try {
//				 System.out.println("loading " + baseName);
				IClassifier<double[]> classifier = ClassifierFactories.newDefaultClassifier(baseName);
				Assert.assertTrue(classifier != null);
			} catch (Exception e) { // Catch so we can show which js file failed.
				Assert.fail("Could not create classifier for name " + baseName + ": " + e.getMessage());
			}
		}
	}
	
	@Test
	public void testMacros() throws AISPException {
		Map<String,Object> bindings = new HashMap<String,Object>();
		String stringValue = "a";
		int integerValue = 10;
		double doubleValue = 20;
		bindings.put("stringValue", stringValue); 
		bindings.put("integerValue", integerValue); 
		bindings.put("doubleValue", doubleValue); 
		IClassifier<double[]> classifier = ClassifierFactories.newClassifier("junit-model", bindings);	// Loads junit-model.jsyt from this package.
		Assert.assertTrue(classifier != null);
		Assert.assertTrue(classifier instanceof JunitClassifier);
		JunitClassifier jc = (JunitClassifier)classifier;
		// make sure the values in the bindings showed up in the classifier.
		Assert.assertTrue(jc.getStringValue().equals(stringValue));
		Assert.assertTrue(jc.getIntegerValue() == integerValue);
		Assert.assertTrue(jc.getDoubleValue() == doubleValue);
	}
	
	@Test
	public void testJSYT_GMM() throws AISPException {
		int[] numGuassians = new int[] {  8, 4 };
		String[] useDiagonals = new String[] { "true", "false" };
		boolean doTrain = true;
		List<IFixedClassifier> allClassifiers= new ArrayList<>();
		for ( int ng : numGuassians) {
			Map<String,Object> bindings = new HashMap<String,Object>();
			bindings.put("numGaussians", ng); 
			for ( String diag : useDiagonals)  {
				bindings.put("useDiagonalCovariance", diag); 
				testFeatureGramArgs("gmm", null, true, bindings, allClassifiers, GMMClassifier.class, doTrain);
				doTrain = false;
			}
		}
		
	}

	@Test
	public void testFeatureGramArgs_LPKNN() throws AISPException {
		testFeatureGramArgs("lpknn", null, true, null, null, LpDistanceMergeKNNClassifier.class, true);
	}
	@Test
	public void testJSYT_KNNMergeLp() throws AISPException {
		int [] listSizes = new int[] {  3000, 4000 };
		float[] pValues = new float[] {  (float)0.5, 2 };
		float[] multipliers = new float[] {  3, 4 };
		String[] trueFalse = new String[] { "true", "false" };
		boolean doTrain = true;

		List<IFixedClassifier> allClassifiers= new ArrayList<>();
		for ( int size: listSizes) {
			Map<String,Object> bindings = new HashMap<String,Object>();
			bindings.put("distanceMetric", "Lp");
			bindings.put("maxListSize", size); 
			for ( float multiplier : multipliers)  {
				bindings.put("stddevMultiplier", multiplier); 
				for ( String outliers : trueFalse)  {
					bindings.put("enableOutliers", outliers); 
					for ( float p : pValues)  {
						bindings.put("pValue", p); 
						List<IFixedClassifier<double[]>> classifierList = testFeatureGramArgs("knn", null, true, bindings, allClassifiers, LpDistanceMergeKNNClassifier.class, true);
						doTrain = false;	// Train only the first time.
					}
				}
			}
		}
	}

	@Test
	public void testFeatureGramArgs_KNNMergeL1() throws AISPException {
		// Note we don't test all the arguments since that is tested in testJSYT_KNNMergeLp() and should not need to be tested again.
		Map<String,Object> bindings = new HashMap<String,Object>();
		bindings.put("distanceMetric", "L1");
		testFeatureGramArgs("knn", null, true, bindings, null, L1DistanceMergeKNNClassifier.class, true);
	}

	@Test
	public void testFeatureGramArgs_KNNMergeEuclidean() throws AISPException {
		Map<String,Object> bindings = new HashMap<String,Object>();
		bindings.put("distanceMetric", "Euclidian");
		testFeatureGramArgs("knn", null, true, bindings, null, EuclidianDistanceMergeKNNClassifier.class, true);
	}

	
	@Test
	public void testJSYT_NormalDistributionAnomalyClassifier() throws AISPException {
		int [] samplesToLearn = new int[] {  -1, 0, 10, 20 };
		float[] pValues = new float[] {  (float)0.5, 2 };
		float[] stddevMultipliers = new float[] {  (float)0.5, 2 };
		String[] trueFalse = new String[] { "true", "false" };
		String[] featureCompression = new String[] { "time", "feature", "both" };
		String[] distanceMetric = new String[] { "Lp", "L1" };

		String[] featureExtractors = new String[] { "Identity", "MFCC", "LogMel" };
		List<IFixedClassifier> allClassifiers= new ArrayList<>();
		for ( int samples : samplesToLearn) {
			Map<String,Object> bindings = new HashMap<String,Object>();
			bindings.put("samplesToLearnEnvironment", samples); 
			for (String metric : distanceMetric) {
				bindings.put("distanceMetric", metric);
				for ( float multiplier : stddevMultipliers)  {
					bindings.put("stddevMultiplier", multiplier); 
					for (String fc : featureCompression) {
						bindings.put("featureCompression", fc);
						for ( float p : pValues)  {
							bindings.put("pValue", p); 
							List<IFixedClassifier<double[]>> classifierList = testFeatureGramArgs("normal-dist-anomaly", featureExtractors, false, 
									bindings, allClassifiers, NormalDistributionAnomalyClassifier.class, false );
							verifyNormalDistributionAnomalyClassifiers(classifierList);
							allClassifiers.addAll(classifierList);
						}
					}
				}
			}
		}
	}
	
	private void verifyNormalDistributionAnomalyClassifiers(List<IFixedClassifier<double[]>> classifierList) {
		for (IFixedClassifier<double[]> fc : classifierList) {
			NormalDistributionAnomalyClassifier mdac = (NormalDistributionAnomalyClassifier)fc;
			// TODO: add more tests here.
		}
		
	}

	@Test
	public void testJSYT_NeuralNet_CNN() throws AISPException {
		Map<String,Object> bindings = new HashMap<String,Object>();
		bindings.put("layerDefinition", "CNN");
		// We don't need to test all algorithm parameters here since it was done in testJSYT_NeuralNet_DCASE()
		ClassifierFactoriesTest.testFeatureGramArgs("neural-net", null, true, bindings, null, CNNClassifier.class, false);
	}

	@Test
	public void testJSYT_NeuralNet_DCASE() throws AISPException {
		Map<String,Object> bindings = new HashMap<String,Object>();
		int[] epochs = new int[] { 100, 50};
		List<IFixedClassifier> allClassifiers= new ArrayList<>();
		boolean doTrain = false;	// Don't train (too little data) just make sure models are all different.
	
		bindings.put("layerDefinition", "DCASE");
		for (int epoch : epochs) {
			bindings.put("nEpochs", epoch);
			ClassifierFactoriesTest.testFeatureGramArgs("neural-net", null, true, bindings, allClassifiers, DCASEClassifier.class, doTrain);
			doTrain = false;
		}
	}

	/**
	 * Helper method to test a .jsyt file defining a classifier using various common feature gram extraction parameters.
	 * JSYT file is expected to have the following feature gram parameter bindings:
	 * <ul> 
	 * <li> deltaProcessor with string values of true or false
	 * <li> normalizeProcessor with string values of true or false.
	 * <li> featureExtractor with string values of MFCC, LogMel or FFT.
	 * </ul>
	 * 2 * 2 * 3 classifiers are built and optionally trained.  Each one is tested to be sure it is does not equal() any of the others. 
	 * @param modelName the base name of the .jsyt (e.g. gmm).  This is expected to be on the classpath somewhere. 
	 * @param featureExtractors list of tokens parsed in the jsyt to determine the feature extractor.  May be null in which case ["MFCC", "LogMel", "FFT"] are used.
	 * @param testNormalization TODO
	 * @param baseBindings any additional variable inputs to provide to the evaluation of the JSYT file when creating the classifier
	 * @param pastClassifiers if not null, then contains classifiers that should be compared against those created here to be sure that none are equal. 
	 * @param klass the expected class to be created.
	 * @param doTraining if true, then do some simple training to verify the model can do classification.
	 * @return 
	 * @throws AISPException
	 */
	protected static List<IFixedClassifier<double[]>> testFeatureGramArgs(String modelName, String[] featureExtractors, boolean testNormalization, Map<String, Object> baseBindings, List<IFixedClassifier> pastClassifiers, Class<? extends IClassifier> klass, boolean doTraining) throws AISPException {
		String[] deltaProcessors = new String[] { "true", "false" };
		String[] normalizeProcessors = testNormalization ? new String[] { "true", "false" } : new String[] { "ignored"} ;
		if (featureExtractors == null) 
			featureExtractors = new String[] { "MFCC", "LogMel", "FFT" };
		Assert.assertTrue("Test must define at least on feature extractor", featureExtractors.length > 0);

		String trainingLabel = "label";
		List<SoundRecording> srList = SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, "normal", 3, "abnormal", 3);
		List<IFixedClassifier<double[]>> classifierList = new ArrayList<>();
		if (pastClassifiers == null)
			pastClassifiers = new ArrayList<IFixedClassifier>();

		for ( String deltaProcessor: deltaProcessors) {
			Map<String,Object> bindings = new HashMap<String,Object>();
			if (baseBindings != null)
				bindings.putAll(baseBindings);
			bindings.put("deltaProcessor", deltaProcessor); 
			for ( String normalizeProcessor: normalizeProcessors) {
				bindings.put("normalizeProcessor", normalizeProcessor); 
				for ( String featureExtractor: featureExtractors) {
					bindings.put("featureExtractor", featureExtractor); 
					IFixableClassifier<double[]> untrainedClassifier = validateCreation(modelName,  bindings, klass);
					String msg = "bindings=" + bindings;
//					AISPLogger.logger.info(msg);
					
					// Make sure all untrained classifiers are different.
					Assert.assertTrue(msg, !classifierList.contains(untrainedClassifier));
					classifierList.add(untrainedClassifier);


					if (doTraining) {
						// Get a classifier that we will train and test.
						IFixableClassifier<double[]> classifier = validateCreation(modelName,  bindings, klass);

						classifier.train(trainingLabel, srList);
						
						// Make sure we have a model.
						IFixedClassifier fc = classifier.getFixedClassifier();
						Assert.assertTrue(msg, fc != null);	// Unfortunately this and classifier have to be manually inspected to see if they were built correctly.

						// Make sure all classifiers work on this data.
						SoundTestUtils.verifyClassifications(classifier, srList, trainingLabel);
					}

				}
			}
		}
		return classifierList;

	}

	/**
	 * @param modelName
	 * @param bindings
	 * @param class1 
	 * @return
	 * @throws AISPException
	 */
	protected static IFixableClassifier<double[]> validateCreation(String modelName, Map<String, Object> bindings, Class<? extends IFixedClassifier> clazz)
			throws AISPException {
		IFixableClassifier<double[]> classifier = (IFixableClassifier<double[]>) ClassifierFactories.newClassifier(modelName, bindings);	
		Assert.assertTrue(classifier != null);
		Assert.assertTrue(clazz.isAssignableFrom(classifier.getClass()));
		return classifier;
	}
	
	

}
