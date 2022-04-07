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
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLogger;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.classifier.TrainingSetInfo.LabelInfo;
import org.eng.aisp.util.BalancedLabeledWindowIterable;
import org.eng.aisp.util.BalancedLabeledWindowShuffleIterable;
import org.eng.aisp.util.LabeledWindowIterable;
import org.eng.aisp.util.Partitioner;
import org.eng.util.DelegatingShuffleIterable;
import org.eng.util.IShuffleIterable;
import org.eng.util.IterableIterable;


/**
 * Implements a model evaluation algorithm by holding out 1 of K subsets of training data K times.
 * @author dawood
 *
 */
public class KFoldModelEvaluator {
	
	
	private final int foldCount;
	private final int seed;
	private boolean verbose;
	private boolean preShuffle;
	private boolean parallelCM;
	
	public final static int DEFAULT_SEED = 123345123;
	public final static boolean DEFAULT_PRESHUFFLE = true;
	public static final int DEFAULT_FOLD_COUNT = 3;
	public final static boolean DEFAULT_VERBOSE = false;
	public final static boolean DEFAULT_PARALLEL= false;

	public KFoldModelEvaluator() {
		this(DEFAULT_FOLD_COUNT,DEFAULT_SEED, DEFAULT_PRESHUFFLE, DEFAULT_VERBOSE, DEFAULT_PARALLEL);	// Every element from the first and no maximum

	}

	/**
	 * Create the evaluator to use the given number of folds the default seed and non-verbose. 
	 * @param foldCount
	 */
	public KFoldModelEvaluator(int foldCount) {
		this(foldCount,DEFAULT_SEED, DEFAULT_PRESHUFFLE, DEFAULT_VERBOSE, DEFAULT_PARALLEL);	// Every element from the first and no maximum
	}
	


	/**
	 * Create the instance to use the given number of folds. 
	 * @param foldCount must be larger than 1.
	 * @param seed used to shuffle the the sounds prior to evaluation.
	 * @param preShuffle if true, then shuffle the data prior to defining folds. 
	 * @param verbose if true, then put out extra info during evaluation.
	 */
	public KFoldModelEvaluator(int foldCount, int seed, boolean preShuffle, boolean verbose, boolean parallelCM) {
		this.foldCount = foldCount;
		this.seed = seed;
		this.verbose = verbose;
		this.preShuffle = preShuffle;
		this.parallelCM = parallelCM;
	}

	
	public KFoldModelEvaluator(int folds, boolean verbose) {
		this(folds, DEFAULT_SEED, DEFAULT_PRESHUFFLE, verbose, DEFAULT_PARALLEL);
	}



	private static boolean warned;


	/**
	 * Compute a confusion matrix given a model and set of labeled data. 
	 * Data is separated into K folds (or partitions).  K-1 partitions are used to train the model and the held out
	 * partition is used to determine a confusion matrix.  This process is done K times with a different partition being
	 * held out as test data each time.  All resulting confusion matrices are combined into a single matrix
	 * (see {@link ConfusionMatrix#add(ConfusionMatrix)}).  The data is randomly shuffled before computing the matrices,
	 * which will require all data being brought into memory if a other than an IShuffleIterable contains the labeled data.
	 * @param modeler a classifier to be trained on the given labeled data.
	 * @param data data to test against the given model to determine the confusion matrix.
	 * @param label the label for which the model can generate results and which is found on at least of some of the given data. 
	 * @param foldsToEvaluate the number of test folds to evaluate to produce the result.  Value must be from 1 to the number of folds defined in the constructor.
	 * @param balancedTrainingCount if greater than 0, then sets the number of samples per partition for each label value used during training.  Data is up/downsampled as necessary.
	 * @return never null. A confusion matrix that is the combination of all matrices computed from each of the K folds being
	 * held out as test data.
	 * @throws AISPException
	 */
	public <WINDATA,LDW extends ILabeledDataWindow<WINDATA>> ConfusionMatrix getConfusionMatrix(IClassifier<WINDATA> modeler, Iterable<LDW> data, 
					String label, int foldsToEvaluate, int balancedTrainingCount) throws AISPException {
		return this.getConfusionMatrix(modeler, data, label, foldsToEvaluate, balancedTrainingCount, null);
	}
	
	interface IPartitionListener<WINDATA,LDW extends ILabeledDataWindow<WINDATA>> {
		/**
		 * Called when the test and train datasets are defined, but before training and evaluation.
		 * @param trainingData
		 * @param testData
		 * @return true if training and evaluation should continue.  If false, then 
		 */
		boolean partitioned(Iterable<LDW> trainingData, Iterable<LDW> testData);
	}

	/**
	 * See {@link #getConfusionMatrix(IClassifier, Iterable, String, int, int)}. 
	 */
	protected <WINDATA,LDW extends ILabeledDataWindow<WINDATA>> ConfusionMatrix getConfusionMatrix(IClassifier<WINDATA> modeler, Iterable<LDW> data, 
					String label, int foldsToEvaluate, int balancedTrainingCount, IPartitionListener<WINDATA, LDW> partitionListener) throws AISPException {

		Iterable<LDW> dataIterable;
		if (foldsToEvaluate < 1 || foldsToEvaluate > foldCount)
			throw new IllegalArgumentException("foldsToEvaluate must range from 1 to fold count (" + foldCount + ")");	
		if (preShuffle) {
			dataIterable = shuffleData(data, label);
			if (verbose)
				AISPLogger.logger.info("Shuffling all data prior to defining folds.");
		} else {
			dataIterable = data; 
			if (verbose)
				AISPLogger.logger.info("Not shuffling any data");
		}
		
//		AISPLogger.logger.info("Initial data:\n" + TrainingSetInfo.getInfo(data).prettyFormat());

		// Partition the data evenly spreading equal numbers of label values across each partition.  Do NOT balance data - we only optionally balance the training data, below.
		List<Iterable<LDW>> partitionList = Partitioner.partition(dataIterable, label, this.foldCount,0);
		
		if (verbose) {
			String msg = "";
			for (int i=0 ; i<this.foldCount ; i++) {
				Iterable<LDW> part = partitionList.get(i);
				msg = msg + "\nPartition " + (i+1) + ":\n" + TrainingSetInfo.getInfo(part).prettyFormat();
			}
			AISPLogger.logger.info(msg);
		}
		
		ConfusionMatrix mergedMatrix = null; 
		for (int testPartitionIndex=0 ; testPartitionIndex<foldsToEvaluate; testPartitionIndex++) {	// over a held out set 

			// Select N-1 partitions for training and balance that data so all label values have the same number of samples, if requested.
			Iterable<LDW> trainingData = getTrainingData(dataIterable, partitionList, testPartitionIndex, label, balancedTrainingCount);
			// Select the test partition, but do NOT balance it.  If caller want's balanced test (and training) data, they should balance before calling this method..
			Iterable<LDW> testData = getTestData(partitionList, testPartitionIndex, label);
			
			TrainingSetInfo trainingInfo= null, testInfo = null;
			if (verbose) {
				trainingInfo = TrainingSetInfo.getInfo(trainingData);
				testInfo = TrainingSetInfo.getInfo(testData);
				String msg = "\nUsing partition " + (testPartitionIndex+1) + " of " + this.foldCount + " as test partition."
						+ "\nTraining fold:\n" + trainingInfo.prettyFormat()
						+ "\nTest fold:\n" + testInfo.prettyFormat();
				AISPLogger.logger.info(msg);
			}

			// Provided primarily for junit tests to check on the definition of the training and test data sets.
			if (partitionListener != null && !partitionListener.partitioned(trainingData, testData))
				continue;	// Listener (junit test?) requested confusion matrix computation to be skipped.
			

			ConfusionMatrix matrix = getConfusionMatrix(modeler, trainingData, testData, label, trainingInfo, testInfo, testPartitionIndex); 
			if (matrix != null) {
				if (verbose)
					System.out.println("Accuracy on held out fold #" + testPartitionIndex + ": " + matrix.getAccuracy());
				if (mergedMatrix == null)
					mergedMatrix = matrix;
				else
					mergedMatrix = mergedMatrix.add(matrix);
			}
		}
		
		return mergedMatrix;
	}



	/**	
	 * 
	 * @param <WINDATA>
	 * @param <LDW>
	 * @param modeler the classifier to train and test
	 * @param trainingData training data
	 * @param testData test data
	 * @param label label name on which model is trained and tested.
	 * @param trainingInfo if not running in verbose mode, then this is null, otherwise it is the TrainingSetInfo for the given trainingData.  Used only for logging.
	 * @param testInfo if not running in verbose mode, then this is null, otherwise it is the TrainingSetInfo for the given testData. Used only for logging.
	 * @param testPartitionIndex the 0-based index of the partition being used as the test data.  Used only for logging. 
	 * @return
	 * @throws AISPException
	 */
//	private <WINDATA,LDW extends ILabeledDataWindow<WINDATA>> ConfusionMatrix getConfusionMatrix(IClassifier<WINDATA> modeler, Iterable<LDW> trainingData, Iterable<LDW> testData,
	private <WINDATA,LDW extends ILabeledDataWindow<WINDATA>> ConfusionMatrix getConfusionMatrix(IClassifier<WINDATA> modeler, Iterable<LDW> trainingData, 
			Iterable<LDW> testData, String label, TrainingSetInfo trainingInfo, TrainingSetInfo testInfo, int testPartitionIndex) throws AISPException {
		LabelInfo labelInfo = null;
		if (trainingInfo != null)  {
			labelInfo = trainingInfo.getLabelInfo(label);
			if (labelInfo == null)
				System.err.println("No data found for training label " + label);
			else
				System.out.println("Begin training. Training data with partition #" + (testPartitionIndex+1) + " held out: " + labelInfo.prettyFormat()); 
		}
//			AISPLogger.logger.info("Test fold: \n" + TrainingSetInfo.getInfo(testData).prettyFormat());
//			if (areOverlapping(trainingData, testData)) 
//				System.err.println("Non-overlapping test and training data");
		
		long start = System.currentTimeMillis();
//			AISPLogger.logger.info(TrainingSetInfo.getInfo(data).prettyFormat());
//			AISPLogger.logger.info(TrainingSetInfo.getInfo(dataIterable).prettyFormat());
//			AISPLogger.logger.info(TrainingSetInfo.getInfo(trainingData).prettyFormat());
//			AISPLogger.logger.info(TrainingSetInfo.getInfo(testData).prettyFormat());
		modeler.train(label, trainingData); 
		if (labelInfo != null) {
			long end = System.currentTimeMillis();
			assert labelInfo != null;
			double duration = (end - start);
			double percentTraining = 100 * duration / labelInfo.getTotalMsec();
			duration /= 1000.0;
			System.out.println("Held out partition #" + (testPartitionIndex+1) + " training time " + duration + " msec, " + percentTraining + "% of training data length");
//			System.out.println("Held out partition #" + (testPartitionIndex+1) + " classifier:  " + modeler); 
		}
		
		
		ConfusionMatrix matrix =  ConfusionMatrix.compute(label, modeler, testData, parallelCM);
		return matrix;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <WINDATA,LDW extends ILabeledDataWindow<WINDATA>> Iterable<LDW> shuffleData(Iterable<LDW> data, String label) {
		Iterable<LDW> dataIterable;
		if (data instanceof IShuffleIterable) {
			dataIterable = ((IShuffleIterable)data).shuffle(this.seed);	// Make the results repeatable
//			AISPLogger.logger.info("Original data:\n" + TrainingSetInfo.getInfo(dataIterable).prettyFormat());
			// Make sure that when we split the data into training and test, we have folds that have windows that all have the given label. 
//			final boolean enableRepeatableShufflability = false; // False since we don't know if the data is repeatable. 
//			dataIterable = new LabeledWindowShuffleIterable<ILabeledDataWindow<WINDATA>>((IShuffleIterable)dataIterable, enableRepeatableShufflability, label);
//			AISPLogger.logger.info("Label-only data:\n" + TrainingSetInfo.getInfo(dataIterable).prettyFormat());
//			AISPLogger.logger.info("Label-only data:\n" + TrainingSetInfo.getInfo(dataIterable).prettyFormat());
		} else {
			// Need to pull all data into memory so we can shuffle it.
			if (!warned) {
				AISPLogger.logger.warning("The use of an Iterable other than IShuffleIterable has higher memory requirements."); 
				warned = true;
			}
			List<LDW> dataList = new ArrayList<LDW>();
			for (LDW ldw : data)
				dataList.add(ldw);
			Collections.shuffle(dataList, new Random(this.seed));  //randomize the data sequence
			dataIterable = dataList;
			// Make sure that when we split the data into training and test, we have folds that have windows that all have the given label. 
			dataIterable = new LabeledWindowIterable<LDW>(dataIterable, label);
		}
		return dataIterable;
	}



	private <WINDATA, LDW extends ILabeledDataWindow<WINDATA>> Iterable<LDW> getTestData(List<Iterable<LDW>> partitionList, int testPartitionIndex, String labelName) {
		Iterable<LDW> data = partitionList.get(testPartitionIndex); 
		return data;
	}

	/**
	 * @param <LDW>
	 * @param data
	 * @param labelName
	 * @param balancedTrainingCount
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <WINDATA, LDW extends ILabeledDataWindow<WINDATA>> Iterable<LDW> balance(Iterable<LDW> data, String labelName,
			int balancedTrainingCount) {
		if (data instanceof IShuffleIterable) 
			data = new BalancedLabeledWindowShuffleIterable<LDW>((IShuffleIterable)data, labelName, balancedTrainingCount);
		else
			data = new BalancedLabeledWindowIterable<LDW>(data, labelName, balancedTrainingCount);
		return data;
	}

	/**
	 * 
	 * @param <WINDATA>
	 * @param <LDW>
	 * @param sourceData
	 * @param partitionList
	 * @param testPartitionIndex
	 * @param labelName
	 * @param balancedCount of larger than 0, then this is the number of samples per partition, up/downsampled as necessary.
	 * @return
	 */
	private <WINDATA, LDW extends ILabeledDataWindow<WINDATA>> Iterable<LDW>  getTrainingData(Iterable<LDW> sourceData,  List<Iterable<LDW>> partitionList, int testPartitionIndex, String labelName, int balancedCount) {
		Iterable<LDW> data; 
		int trainingPartitionCount=0;	// Should probably always end up being been partitionList.size() - 1.
		if (sourceData instanceof IShuffleIterable) {
			// Accumulate all references from across the partitions into 1 list.
			List<String> trainingPartitionRefs = new ArrayList<>();
			for (int i=0 ; i<partitionList.size(); i++) {
				if (i != testPartitionIndex)  {
					trainingPartitionCount++; 
					IShuffleIterable<LDW> si = (IShuffleIterable<LDW>)partitionList.get(i); 
					si.getReferences().forEach(new Consumer<String>() {
						public void accept(String item) {
							trainingPartitionRefs.add(item);
						}
					});
				}
			}
			// Create the shuffle iterable over the training references.
			data = new DelegatingShuffleIterable<LDW>(trainingPartitionRefs, (IShuffleIterable<LDW>)sourceData); 
		} else {
			// Here collect the training iterables into a list.
			List<Iterable<LDW>> trainingPartitions = new ArrayList<>();
			for (int i=0 ; i<partitionList.size(); i++) {
				if (i != testPartitionIndex)  {
					trainingPartitions.add(partitionList.get(i));
					trainingPartitionCount++;
				}
			}
			data = new IterableIterable<LDW>(trainingPartitions);
		}

		if (balancedCount != 0) 
			data = balance(data, labelName, balancedCount * trainingPartitionCount);
		return data;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + foldCount;
		result = prime * result + seed;
		result = prime * result + (verbose ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof KFoldModelEvaluator))
			return false;
		KFoldModelEvaluator other = (KFoldModelEvaluator) obj;
		if (foldCount != other.foldCount)
			return false;
		if (seed != other.seed)
			return false;
		if (verbose != other.verbose)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "KFoldModelEvaluator [foldCount=" + foldCount + ", seed=" + seed + ", verbose=" + verbose + "]";
	}
	
}
