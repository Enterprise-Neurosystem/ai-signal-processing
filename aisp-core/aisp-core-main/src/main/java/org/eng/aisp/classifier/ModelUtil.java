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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.eng.aisp.AISPException;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.classifier.TrainingSetInfo.LabelInfo;
import org.eng.aisp.classifier.gmm.GMMClassifierBuilder;
import org.eng.aisp.util.BalancedLabeledWindowIterable;
import org.eng.aisp.util.BoundedLabelValueWindowShuffleIterable;
import org.eng.aisp.util.DataWindowLabelingIterable;
import org.eng.aisp.util.DataWindowLabelingShuffleIterable;
import org.eng.aisp.util.LabeledWindowToDataWindowIterable;
import org.eng.aisp.util.LabeledWindowToDataWindowShuffleIterable;
import org.eng.util.CSVTable;
import org.eng.util.ClassUtilities;
import org.eng.util.ExecutorUtil;
import org.eng.util.IShuffleIterable;
import org.eng.util.IthShuffleIterable;

import com.google.gson.Gson;



public class ModelUtil {

//	private static <T> Stream<T> parallelStream(Iterable<T> in) {
//	    return StreamSupport.stream(in.spliterator(), true);
//	}

	private static class ClassifyTask implements Callable<Object> {
		Map<String, AtomicInteger> counts = new HashMap<String, AtomicInteger>();
		Map<String, AtomicInteger> totals = new HashMap<String, AtomicInteger>();
		IFixedClassifier<?> classifier;
		Iterator<? extends ILabeledDataWindow<?>> data;
		Vector<Exception> exceptions = new Vector<Exception>();
		
		public ClassifyTask(IFixedClassifier<?> classifier,
				Iterator<? extends ILabeledDataWindow<?>> iterator) {
			this.classifier = classifier;
			this.data = iterator;
		}

		@Override
		public Object call() {
			while (true) {
				ILabeledDataWindow<?> datum;
				try {
					synchronized(data) {
						if (data.hasNext()) 
							datum = data.next();
						else
							break;
					}
					checkAccuracy(classifier, datum, counts, totals);
				} catch (Exception e) {
					e.printStackTrace();
					exceptions.add(e);
				}
			}
			return null;
			
		}

		public Map<String, Accuracy> getAccuracies() {
			Map<String, Accuracy> accuracies = new HashMap<String, Accuracy>();
				// Convert the counts into accuracy rates.
			for (String labelName : counts.keySet()) {
				int totalInt = totals.get(labelName).get();
				double count = counts.get(labelName).get();
				Accuracy a = new Accuracy(totalInt, count/totalInt);
				accuracies.put(labelName, a); 
			}
			
			return accuracies;
		}
		
	}

	private static class CheckPointer {
	
		private final static String CLASSIFIER_KEY_COLNAME  = "Classifier Key";
		private final static String CLASSIFIER_HASH_COLNAME = "Hash";
		private final static String PRECISION_COLNAME  = "Precision";
		private final static String RECALL_COLNAME  = "Recall";
		private final static String F1_COLNAME  = "F1";
		private final static String CONFUSION_MATRIX_COLNAME  = "Confusion Matrix (JSON)";
				
		/** Set the order of columns in the output file */
		private final static String CheckPointColumnNames[] = new String[] { F1_COLNAME, PRECISION_COLNAME, RECALL_COLNAME, CLASSIFIER_KEY_COLNAME, CLASSIFIER_HASH_COLNAME, 
				CONFUSION_MATRIX_COLNAME };
	
		CSVTable table;
		private String fileName;
		Gson gson = new Gson();
		
		public CheckPointer(String fileName) throws IOException {
			this.fileName = fileName;
			if (new File(fileName).exists()) 
				table = CSVTable.readFile(fileName, 1);	
			else
				table = new CSVTable(CheckPointColumnNames) ;
		}
		
		/**
		 * @param rowIndex 0-based row index.
		 * @return null if row does not exist.
		 */
		public ConfusionMatrix getResult(int hash) {
			// Search for the row with the given key
			Map<String, String> where = new HashMap<String, String>();
			where.put(CLASSIFIER_HASH_COLNAME, String.valueOf(hash));
			CSVTable rows = this.table.getRows(where);
			if (rows.getRowCount() == 0)		// Key not present in table.
				return null;

			CaseInsensitiveMap row = rows.getRow(0);	// Get the 1 row out of the table.

			String trainingLabel = (String)row.get(CONFUSION_MATRIX_COLNAME);
			ConfusionMatrix matrix = gson.fromJson(trainingLabel, ConfusionMatrix.class);
			return matrix;
		}

		/**
		 * Delete the result for the given hash if it exists.
		 * @param hash
		 */
		public void deleteResult(int hash) {
			// Search for the row with the given key
			Map<String, String> where = new HashMap<String, String>();
			where.put(CLASSIFIER_HASH_COLNAME, String.valueOf(hash));
			CSVTable rows = this.table.getRows(where);
			this.table.removeRows(where);
		}
		
		public void updateResult(int hash, String key, ConfusionMatrix matrix) throws IOException {
			// Remove any past results in case the key or matrix has changed.
			deleteResult(hash);
			// Add the new result.
			Map<String, Object> row = new HashMap<String,Object>();
			int rowCount = this.table.getRowCount();
			row.put(CLASSIFIER_KEY_COLNAME, key); 
			row.put(CLASSIFIER_HASH_COLNAME, String.valueOf(hash)); 
			row.put(PRECISION_COLNAME, String.valueOf(matrix.getPrecision().getMean()));
			row.put(RECALL_COLNAME, String.valueOf(matrix.getRecall().getMean()));
			row.put(F1_COLNAME, String.valueOf(matrix.getF1Score().getMean()));
			row.put(CONFUSION_MATRIX_COLNAME, String.valueOf(new Gson().toJson(matrix)));
			this.table.appendRow(row);
			table.write(fileName);
			
		}
	
	}

	public static class ClassifierPerformance {
	
		private IFixedClassifier<double[]> classifier;
		private ConfusionMatrix confusionMatrix;
		private String key;
	
		public ClassifierPerformance(String key, IFixedClassifier<double[]> classifier, ConfusionMatrix confusionMatrix) {
			this.key = key;
			this.classifier = classifier;
			this.confusionMatrix = confusionMatrix;
		}

		/**
		 * @return the classifier
		 */
		public IFixedClassifier<double[]> getClassifier() {
			return classifier;
		}

		/**
		 * @return the confusionMatrix
		 */
		public ConfusionMatrix getConfusionMatrix() {
			return confusionMatrix;
		}

		/**
		 * @return the key
		 */
		public String getKey() {
			return key;
		}
		
	}

	private static class ClassifierPerformanceComparator implements Comparator<ClassifierPerformance> {
	
		@Override
		public int compare(ClassifierPerformance o1, ClassifierPerformance o2) {
			double o1f1 = o1.confusionMatrix.getF1Score().getMean();
			double o2f1 = o2.confusionMatrix.getF1Score().getMean();
			return Double.compare(o2f1, o1f1);	// Sort in ascending order (larger f1 first)
		}
	
	}

	private static void checkAccuracy(IFixedClassifier classifier, ILabeledDataWindow datum, 
									Map<String,AtomicInteger> correctCount, Map<String,AtomicInteger> totalCount) throws AISPException {
		Properties labels = datum.getLabels();
		if (labels.size() != 0) {
			Map<String, Classification> results;
//			Long tid = Thread.currentThread().getId();
//			System.out.println("thread id: " + tid);
			results = classifier.classify(datum.getDataWindow());
			for (Object key : labels.keySet()) {
				String labelName = key.toString();
				String labelValue = labels.getProperty(labelName); 
				AtomicInteger count = correctCount.get(labelName); 
				if (count == null ) {
					synchronized(correctCount) {
						count = correctCount.get(labelName); 	// retest now that we have the lock
						if (count == null) {	
							count = new AtomicInteger(); 
							correctCount.put(labelName, count);
						}
					}
				}
				AtomicInteger total = totalCount.get(labelName); 	
				if (total == null) {
					synchronized(totalCount) {
						total = totalCount.get(labelName); 	// retest now that we have the lock
						if (total == null) {	
							total = new AtomicInteger();
							totalCount.put(labelName, total);
						}
					}
				}
				Classification c = results.get(labelName);
				if (c != null && c.getLabelValue().equals(labelValue)) 
					count.incrementAndGet();
				total.incrementAndGet(); 
			}
		}
	}

	/**
	 * Compute the accuracy of the classifier to predict the labels associated with the given data.
	 * @param classifier a trained classifier.
	 * @param data a set of data each with 1 or more labels.  Data without labels are not included in the calculation.
	 * @return a map of label names to accuracy scores (each label name gets its own score).
	 * @throws AISPException
	 */
	public static Map<String, Accuracy> getAccuracy(IFixedClassifier<?> classifier, Iterable<? extends ILabeledDataWindow<?>> data) throws AISPException {
		ClassifyTask task = new ClassifyTask(classifier, data.iterator());
//		ExecutorService executorService = Executors.newCachedThreadPool(new org.eng.aisp.util.DaemonThreadFactory());
//		ExecutorService executorService = ExecutorUtil.higherPriorityService(); 
		ExecutorService executorService = ExecutorUtil.getSharedService();
		int threads = Runtime.getRuntime().availableProcessors() - 1;
		List<Future<Object>> futures = new ArrayList<Future<Object>>();
		for (int i=0 ; i<threads ; i++) 
			futures.add(executorService.submit(task));
		
		// Wait for threads to complete.
		for  (Future<Object> f : futures) {
			try {
				f.get();
			} catch (InterruptedException | ExecutionException e) {
				Thread.currentThread().interrupt();
			}
		}
		
		return task.getAccuracies();
		
	}
	
	/**
	 * A convenience on {@link #reverseTestLabeled(IClassifier, Iterable, Iterable)} that trains the classifier using the given training label and training data.
	 */
	public static ConfusionMatrix reverseTestLabeled(IClassifier<double[]> classifier, String trainingLabel, Iterable<? extends ILabeledDataWindow<double[]>> trainingData,  Iterable<? extends ILabeledDataWindow<double[]>> testData) throws AISPException {
		classifier.train(trainingLabel, trainingData);
		return reverseTestLabeled(classifier, trainingData, testData);
	}

	/**
	 * A convenience on {@link #reverseTest(IClassifier, Iterable, Iterable)} when the test data happens to also be labeled.
	 */
	public static ConfusionMatrix reverseTestLabeled(IClassifier<double[]> classifier, Iterable<? extends ILabeledDataWindow<double[]>> trainingData,  Iterable<? extends ILabeledDataWindow<double[]>> testData) throws AISPException {
		if (testData instanceof IShuffleIterable)
//			return reverseTest(classifier, trainingData, new LabeledWindowToDataWindowShuffleIterable<double[]>((IShuffleIterable<ILabeledDataWindow<double[]>>)testData));
			return reverseTest(classifier, trainingData, new LabeledWindowToDataWindowShuffleIterable<double[],ILabeledDataWindow<double[]>,IDataWindow<double[]>>
					((IShuffleIterable<ILabeledDataWindow<double[]>>)testData));
		else
//			return reverseTest(classifier, trainingData, new LabeledWindowToDataWindowIterable<double[]>(testData));
			return reverseTest(classifier, trainingData, new LabeledWindowToDataWindowIterable       <double[],ILabeledDataWindow<double[]>,IDataWindow<double[]>>
					((Iterable        <ILabeledDataWindow<double[]>>)testData));
	}

	/**
	 * A convenience on {@link #reverseTest(IClassifier, Iterable, Iterable)} that trains the classifier using the given training label and training data.
	 */
	public static ConfusionMatrix reverseTest(IClassifier<double[]> classifier, String trainingLabel, Iterable<? extends ILabeledDataWindow<double[]>> trainingData,  Iterable<? extends IDataWindow<double[]>> testData) throws AISPException {
		classifier.train(trainingLabel, trainingData);
		return reverseTest(classifier, trainingData, testData);
	}

	/**
	 * Implement a reverse testing process to compute a confusion matrix.
	 * Reverse testing involves the following:
	 * <ol>
	 * <li> Labeling the given test data using the given classifier which is assumed to be trained on the given training data.
	 * <li> Make a copy of the classifier
	 * <li> Retrain the new classifier on the labeled test data.
	 * <li> Compute the confusion matrix using the newly trained classifier and the training data as test data.
	 * </ol>
	 * @param classifier a classifier train on the given training data.
	 * @param trainingData data used to previously train the classifier and which will be used as test data to compute the returned confusion matrix.
	 * @param testData unlabeled data to which labels will be applied using the given classifier.  The resulting labeled data is then used to train the classifier used to compute the 
	 * confusion matrix on the training data.
	 * @return never null.  A confusion matrix computed from a classifier trained on the test data using the training data as test data.
	 * @throws AISPException
	 */
	public static ConfusionMatrix reverseTest(IClassifier<double[]> classifier, Iterable<? extends ILabeledDataWindow<double[]>> trainingData,  Iterable<? extends IDataWindow<double[]>> testData) throws AISPException {
		
		String trainingLabel = classifier.getTrainedLabel();
		if (trainingLabel == null) 	// Not yet trained
			throw new IllegalArgumentException("Classifier does not appear to be trained");

		// Convert the test data into labeled data using the given classifier
		Iterable<ILabeledDataWindow<double[]>> labeledTestData; 
		if (testData instanceof IShuffleIterable)
			labeledTestData = new DataWindowLabelingShuffleIterable<double[]>((IShuffleIterable<IDataWindow<double[]>>) testData, classifier);
		else
			labeledTestData = new DataWindowLabelingIterable<double[]>(testData, classifier);

		// Make a copy of the classifier with which we will train using the test data
		IClassifier<double[]> testClassifier;
		try {
			testClassifier = ClassUtilities.copy(classifier);
		} catch (IOException e) {
			throw new AISPException("Could not copy classifier: " + e.getMessage(), e);
		}
		
		// Train the new classifier on the labeled test data.
		testClassifier.train(trainingLabel, labeledTestData);
		
		// Compute the confusion matrix against the training data.
		ConfusionMatrix cm = ConfusionMatrix.compute(trainingLabel, testClassifier, trainingData);
		return cm;
	}
	
	public static void main(String[] args) throws AISPException, IOException {
//		String trainingLabel = "state";
//		Iterable<SoundRecording> data = SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, "normal", 100, "abnormal", 100);
		String trainingLabel = "class";
		IShuffleIterable<SoundRecording> data = SoundRecording.readMetaDataSounds("/dev/sounds/hierarchical"); 
		Iterable<SoundRecording> trainingData = new IthShuffleIterable<SoundRecording>(data,2,0,true); 
		Iterable<SoundRecording> testData = new IthShuffleIterable<SoundRecording>(data,2,0,false); 
		IClassifier<double[]> classifier = new GMMClassifierBuilder().build();
		classifier.train(trainingLabel, trainingData);
		ConfusionMatrix cm = ConfusionMatrix.compute(trainingLabel, classifier, testData);
		ConfusionMatrix reverseCM = reverseTestLabeled(classifier, trainingData, testData); 
		
		System.out.println("Test Data: \n" + cm.formatCounts() + "\n" + cm.formatStats());
		System.out.println("\n Reverse Data: \n" + reverseCM.formatCounts() + "\n" + reverseCM.formatStats());


	}

	/**
	 * Use kfold cross validation on the given classifiers and data to produce a list of ranked classifier/confusion matrix pairs.
	 * @param classifiers map of classifiers where the keys are included in the return ClassifierPerformance instances to identify the classifier.
	 * Evaluation of the classifiers is done in the order defined by a sorting of the keys.
	 * @param labelName the name of the label in the training data on which the models are to be trained and tested.
	 * @param data training/evaluation data.  Data is partitioned using the KFoldModelEvaluator.
	 * @param foldCount number of folds to create in the taining data during Kfold evaluation.
	 * @param foldsToEvaluate the number of times kfold runs on different sets of training and test data.  Typically this is eeither foldCount or 1, to get a quick estimate of ranking.
	 * @param balancedTrainCount if greater than 0, then specifies the number of samples for each label value to use in training, up/downsampling as necessary.
	 * @param checkPointFileName optional file used to store results in to allow a restart of the same ranking, should 1 of the rankings fail or the process is otherwise terminated.
	 * @param verbose if true, then show progress on stdout.
	 * @return a list of sorted ClassificationPerformance instances which the best result at the head of the list.  The length of this is equal to the number of values in the given classifier
	 * map.  The ClassifierPerformance object holds the key in the original classifier map, the partially trained classifier (for N-1 folds) and the ConfusionMatrix from the evaluation across
	 * the the number of folds to evaluate. Never null.
	 * @throws AISPException
	 * @throws IOException
	 */
	public static List<ClassifierPerformance> rankClassifiers(Map<String, IClassifier<double[]>> classifiers, String labelName, Iterable<? extends ILabeledDataWindow<double[]>> data, 
			int foldCount, int foldsToEvaluate, int balancedTrainCount, String checkPointFileName, boolean verbose) throws AISPException, IOException {
		
		// Create the check pointer if requested.
		CheckPointer checkPointer = null;
		if (checkPointFileName != null)
			checkPointer = new CheckPointer(checkPointFileName);
		
	
		// Our results object
		List<ClassifierPerformance> caList = new ArrayList<ClassifierPerformance>();

		// Set the kfold evaluation to use the number of folds requested.
		KFoldModelEvaluator kfold = new KFoldModelEvaluator(foldCount);

		// Sort the keys to  give caller some control over ordering of evaluation.
		List<String> sortedKeys = new ArrayList<String>();
		sortedKeys.addAll(classifiers.keySet());
		Collections.sort(sortedKeys);

		// For each classifier, get the confusion matrix and store the result in our list.
		for (String key : sortedKeys) {	
			IClassifier<double[]> classifier = classifiers.get(key);
			int hash = classifier.hashCode();
			ConfusionMatrix matrix = checkPointer == null ? null : checkPointer.getResult(hash);
			if (matrix == null) {
				if (verbose)
					System.out.println("Evaluating " + classifier);
				matrix = kfold.getConfusionMatrix(classifier, data, labelName, foldsToEvaluate, balancedTrainCount);
			} else if (verbose){
				System.out.println("Using checkpoint results for " + classifier);
			}
			if (checkPointer != null)	
				// Always update the result in case the key has changed.
				checkPointer.updateResult(hash, key, matrix);
			if (verbose) 
				System.out.println(matrix.formatStats());
			ClassifierPerformance ca = new ClassifierPerformance(key, classifier, matrix);
			caList.add(ca);
		}

		// Sort the results by highest to lowest F1 score.
		caList.sort(new ClassifierPerformanceComparator());
		return caList;
	}


	/**
	 * A convenience on {@link #evaluateOutlierDetection(IClassifier, IShuffleIterable, String, Collection, int, int, boolean)} using all label values
	 * in the given data set.
	 */
	public static ConfusionMatrix evaluateOutlierDetection(IClassifier<double[]> classifier,
			IShuffleIterable<SoundRecording> sounds, String labelName, int foldCount, int foldsToEvaluate, boolean verbose)
			throws AISPException {
		LabelInfo li = TrainingSetInfo.getInfo(sounds).getLabelInfo(labelName);
		if (li == null)
			throw new IllegalArgumentException("Data does not contain any data labeled with label " + labelName);
		Set<String> labelValues = li.getLabelValues();
		return evaluateOutlierDetection(classifier,sounds, labelName, labelValues, foldCount, foldsToEvaluate, verbose);
	}

	/**
	 * Evaluate a model for its ability to detect outliers/unknowns.
	 * The resulting confusion matrix is a binary matrix on known vs. unknown detection capabilities.
	 * Evaluation is done as follows:
	 * <ol>
	 * <li> For each label value for each evaluation is requested and present in the data, consider it as the unknown label value. 
	 *   <ol>
	 *   <li> define the base training data as all data minus the data with the held out (unknown) label.
	 *   <li> for each remaining (known) label, 
	 *     <ol>
	 *     <li> define the base test data as the held out unknown data plus the current known label data and balance it.
	 *       <ol>
	 *         <li> For each train and test fold definition from the base test and base training data (1 to all pairings per request)
	 *           <ol>
	 *             <li> train the model on the training folds 
	 *             <li> Compute the confusion matrix using the trained classifier and test fold (known + unknown).
	 *             <li> Merge this confusion matrix with all previous confusion matrices.
	 *           </ol>
	 *       </ol>
	 *     </ol>
	 *   </ol>
	 * </ol>
	 * The resulting merged confusion matrix over all labels considered as  unknowns and all pairings of known/unknow labels is returned.
	 * @param classifier
	 * @param sounds
	 * @param labelName
	 * @param foldCount
	 * @param foldsToEvaluate
	 * @param verbose
	 * @param tsi
	 * @return
	 * @throws AISPException
	 */
	public static ConfusionMatrix evaluateOutlierDetection(IClassifier<double[]> classifier,
			IShuffleIterable<SoundRecording> sounds, String labelName, Collection<String> labelValues, int foldCount, int foldsToEvaluate, boolean verbose)
			throws AISPException {

		if (foldCount <= 1)
			throw new IllegalArgumentException("foldCount must be larger than 1");
		if (foldsToEvaluate <= 0)
			throw new IllegalArgumentException("foldsToEvaluate must be larger than 0");
		if (foldsToEvaluate > foldCount) 
			throw new IllegalArgumentException("foldsToEvaluate must be less than or equal to foldCount(" + foldCount + ")"); 
		if (labelValues.size() <= 1) {
			System.err.println("Data set must contain more than one label value for label " + labelName);
			return null;
		}

		// Restrict the data set to items having the given set of labels values.
		sounds= new BoundedLabelValueWindowShuffleIterable<SoundRecording>(sounds, labelName, Integer.MAX_VALUE, labelValues);
		
		List<String> trainingLabelValues = new ArrayList<String>();
		trainingLabelValues.addAll(labelValues);
		List<String> testLabelValues = new ArrayList<String>();
		ConfusionMatrix globalCM = null;
		// Declare each label value as an outlier (and train on the others)
		for (String unknownLabelValue: labelValues) {
			testLabelValues.add(unknownLabelValue);
			trainingLabelValues.remove(unknownLabelValue);	// Everything but the 1 unknown label value.
			for (String knownLabelValue : labelValues) {
				if (knownLabelValue.equals(unknownLabelValue))
					continue;
				testLabelValues.add(knownLabelValue);
				for (int foldIndex=0 ; foldIndex<foldsToEvaluate ; foldIndex++) {
					// Create the train and test folds
					IShuffleIterable<SoundRecording> trainingSounds = new IthShuffleIterable<SoundRecording>(sounds, foldCount, foldIndex, true); 
					IShuffleIterable<SoundRecording> testFold= new IthShuffleIterable<SoundRecording>(sounds, foldCount, foldIndex, false); 

					// Restrict training data to all labels except the current unknown 
//					System.out.println("Train on values : " + trainingLabelValues +", holding out " + unknownLabelValue);
//					System.out.println("Folds of training data\n" + TrainingSetInfo.getInfo(trainingSounds).prettyFormat());
					trainingSounds = new BoundedLabelValueWindowShuffleIterable<SoundRecording>(trainingSounds, labelName, Integer.MAX_VALUE, trainingLabelValues);
					// Train on all but the current unknown sounds in the current training folds.
					if (verbose)
						System.out.println("Bounded folds of training data\n" + TrainingSetInfo.getInfo(trainingSounds).prettyFormat());

					// Define test sounds as the test fold but with only the unknown label value and the single known value.
					// With an equal number of known and unknown sounds.
//					System.out.println("Testing on values : " + testLabelValues);
					Iterable<SoundRecording> testSounds  = new BoundedLabelValueWindowShuffleIterable<SoundRecording>(testFold, labelName, Integer.MAX_VALUE, testLabelValues);
//					System.out.println("Bounded test sounds \n" + TrainingSetInfo.getInfo(testSounds).prettyFormat());
					testSounds = new BalancedLabeledWindowIterable<SoundRecording>(testSounds, labelName, false);	// Use minimum count of any label as balancing number. 
					if (verbose)
						System.out.println("Balanced test sounds\n" + TrainingSetInfo.getInfo(testSounds).prettyFormat());
					
					// Train the model on the known training sounds w/o the unknown sound.
					classifier.train(labelName, trainingSounds);

					// Compute the confusion matrix using the test fold.
					List<String> predicted = new ArrayList<String>();
					List<String> actual = new ArrayList<String>();
					for (SoundRecording sr : testSounds) {
						SoundClip clip = sr.getDataWindow();
						String actualLabelValue = sr.getLabels().getProperty(labelName);
						// Determine the actual/expected value for this window
						boolean isKnown = !actualLabelValue.equals(unknownLabelValue);	// Label is NOT know to the model - was not trained in.
						if (isKnown)
							actual.add("known");
						else 
							actual.add("unknown");
						// Get the predicted value for this window.
						Classification c = classifier.classify(clip).get(labelName);
						isKnown = Classification.isKnown(c);
						if (isKnown)
							predicted.add("known");
						else
							predicted.add("unknown");
					}
//					ConfusionMatrix cm = ConfusionMatrix.compute(classifier.getTrainedLabel(), classifier, testSounds);
					ConfusionMatrix cm = new ConfusionMatrix(labelName, actual, predicted);

					// Merge in results from last fold
					if (globalCM == null)
						globalCM = cm;
					else
						globalCM = globalCM.add(cm);
					if (verbose) {
						System.out.println("Knowns: " + trainingLabelValues + " unknown: " + unknownLabelValue + ", test labels: " + testLabelValues);
	//					System.out.println("Iteration results\n" + cm.formatCounts());
	//					System.out.println(cm.formatStats());
						System.out.println("Accumulated results\n" + globalCM.formatCounts());
						System.out.println(globalCM.formatStats());
					}
				}
				// Remove the known label value from the test labels, so we can move to the next label value.
				testLabelValues.remove(knownLabelValue);
			}
			trainingLabelValues.add(unknownLabelValue);	// Make the current unknown label known to the next model.
			testLabelValues.remove(unknownLabelValue);	// Stop using this label value as the unknown label value in the test data set.
		}
//		ConfusionMatrix matrix = new ConfusionMatrix(labelName, actual, predicted);
		return globalCM;
	}

}
