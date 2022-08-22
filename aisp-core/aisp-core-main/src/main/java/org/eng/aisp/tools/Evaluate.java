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
package org.eng.aisp.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPRuntime;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.classifier.ConfusionMatrix;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.classifier.KFoldModelEvaluator;
import org.eng.aisp.classifier.ModelUtil;
import org.eng.aisp.classifier.ModelUtil.ClassifierPerformance;
import org.eng.aisp.classifier.TrainingSetInfo;
import org.eng.aisp.classifier.TrainingSetInfo.LabelInfo;
import org.eng.aisp.classifier.TrainingSetInfo.LabelValueInfo;
import org.eng.aisp.classifier.factory.JScriptClassifierMapFactory;
import org.eng.util.CommandArgs;
import org.eng.util.FileUtils;
import org.eng.util.IShuffleIterable;
import org.eng.util.OnlineStats;

public class Evaluate {
	private final static int DEFAULT_HISTORICAL_ITERATIONS = 3;
	
	public final static String Usage = 
			"Perform classifier evaluation in one of three ways using a set of labeled data:\n"
			+ "1) Evaluate a pre-trained model/classifer (-file option),\n"
			+ "2) use k-fold evaluation on a single classifier (-model option), or\n"
			+ "3) use k-fold evaluation on multiple classifiers and rank their performance.\n" 
			+ "    (-models option).\n"
			+ "Pre-trained models are loaded from the file system. \n"
			+ "Classifier definitions are pre-defined or defined in a custom JavaScript file.\n"
			+ "Test/training sounds are read from the file system.\n" 
			+ "Required options:\n"
			+ "  -sounds (see below)\n" 
			+ "  -label (see below)\n" 
			+ "Options for pre-trained models:\n"
			+ GetTrainedModelOptions.OptionsHelp
//			+ "  -iterations N: used with the -historize option to define the number of shuffled\n"
//			+ "      iterations over the sounds.  This is helpful to smooth the statistics.\n"
//			+ "      Default is " + DEFAULT_HISTORICAL_ITERATIONS + ".\n" 

			+ "Options for model training and K-fold evaluation (-model or -models option) :\n"
			+ GetModelOptions.OptionsHelp
			+ "  -models jsfile : specifies a javascript file that creates a map of IClassifier\n" 
			+ "      keyed by an arbitrary string.  All classifiers will be evaluated using k-fold\n" 
			+ "      evaluation (as with a single model) to produce a ranked list of classifiers\n" 
			+ "      For example, \n" 
			+ "          classifiers = { \"gmm\"    : new GMMClassifierBuilder.build(),\n"
			+ "                          \"dcase\"  : new DCASEClassifierBuilder.build() }\n"
			+ "  -cp <file>: used with -models option to specify a 'check point' file to avoid\n" 
			+ "      recomputation across interrupted/failed runs.   Default is checkpoint.csv.\n"
			+ "  -folds N : the number of folds to use in a K-fold evaluation. Must be at least 2.\n"
			+ "      Default is 4.\n"
			+ "  -singleFold : flag used to get a single accuracy value using 1 of N folds.\n"
			+ "  -kfoldBalance (N|max|min): forces k-fold evaluation to balance each training\n" 
			+ "       set.  The balance count corresponds to the full input data set, but will\n"
			+ "       be used so that the total training data for K-1 folds will be N*(K-1)/K.\n"
			+ "       The min or max sets the value of N to the number samples with min or max\n"
			+ "       counts, respectively.  Test partitions are not modified.  If you want both\n"
			+ "       train and test data balanced, then use the -balance-with option. \n"
			+ "  -noShuffle: flag to turn off shuffling of the data during k-fold evaluation.\n" 
			+ "       Useful when ordering of the data is important to the model being evaluated.\n" 
			+ "  -seed <n>  : sets the seed used when shuffling the data prior to fold creation.\n"
			+ "      The default is a fixed value for repeatability.\n" 
			+ "Sound specification options:\n"
			+ GetModifiedSoundOptions.OptionsWithoutBalanceHelp
			+ "Additional options for either mode:\n"
			+ "  -cm : flag requesting that the confusion matrix be printed. Requires the \n"
			+ "      -label option\n" 
			+ "  -exportCM <csv file> :  compute and write the confusion matrix to a CSV file.\n"
			+ "  -serialCM : cause classification to be done serially.  Only needed if a \n" 
			+ "      classifier is found to not be thread-safe.\n" 
			+ "Examples (local pre-trained model): \n"
			+ "  ... -file classifier.cfr -sounds mydir\n" 
			+ "  ... -file classifier.cfr -sounds m1.csv\n" 
			+ "  ... -file classifier.cfr -sounds m1.csv -historize 5\n" 
			+ "  ... -file classifier.cfr -sounds m1.csv,m2.csv\n" 
			+ "  ... -file multi-label-classifier.cfr m1.csv -label label1 -cm\n" 
			+ "Examples (k-fold evaluation of single model): \n"
			+ "  ... -model gmm -sounds mydir -label mylabel\n" 
			+ "  ... -model jsfile:model.js -sounds m1.csv,m2.csv -label mylabel\n" 
			+ "  ... -model dcase -sounds mydir1,mydir2 -label mylabel -clipLen 3000 -pad duplicate\n" 
			+ "  ... -model dcase -sounds mydir1 -label mylabel -clipLen 3000 -clipShift 1500\n" 
			+ "  ... -model lpnn -sounds mydir -label mylabel -folds 2\n" 
			+ "  ... -model multilabel -sounds mydir -label mylabel -singleFold -cm\n" 
			+ "Examples (k-fold evaluation of multiple model): \n"
			+ "  ... -models models.js -sounds mydir -label mylabel\n" 
			+ "  ... -models models.js -sounds mydir -label mylabel -folds 3 -kfoldBalance max\n"
			+ "  ... -models models.js -sounds mydir -label mylabel -clipLen 4000 -pad duplicate\n"
			;

	public static void main(String args[]) {
		// Force any framework initialization messages to come out first.
		AISPRuntime.getRuntime();
		System.out.println("\n");	// blank line to separate copyrights, etc.
		CommandArgs cmdargs = new CommandArgs(args);
		boolean verbose = cmdargs.getFlag("v") || cmdargs.getFlag("verbose"); 

		// Check for help request
		if (cmdargs.getFlag("h") || cmdargs.getFlag("-help") ) {
			System.out.println(Usage);
			return;
	    }

		try {
			if (!doMain(cmdargs, verbose))
				System.err.println("Use the -help option to see usage");
		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage());
			if (verbose)
				e.printStackTrace();
		}
		
	}
	
	protected static boolean doMain(CommandArgs cmdargs, boolean verbose) throws AISPException, IOException {
		
		// Get the name of the model to use.
		boolean evalMultipleModels = cmdargs.hasArgument("models");
		boolean trainModel = !cmdargs.hasArgument("file") && !evalMultipleModels; // a trained model is being provided to test. 
		boolean trainFlag = cmdargs.getFlag("train");
		
		if (trainModel && trainFlag)
			System.out.println("-train option is deprecated (-file determines if training is implied)");

		boolean r;
		if (trainModel) {
			r = trainAndEvaluateClassifier(cmdargs, verbose);
		} else if (evalMultipleModels) {
			r = rankMultipleModels(cmdargs,verbose);
		} else {	// Evaluate an existing model
			r = evaluateExistingModel(cmdargs,verbose);
		}

		return r;

	}

	/**
	 * Expect a JavaScript file to provide a map of classifiers to test and rank using a labeled data set and kfold cross validation.
	 * @param cmdargs
	 * @param verbose2 
	 * @return
	 * @throws IOException
	 * @throws AISPException
	 */
	private static boolean rankMultipleModels(CommandArgs cmdargs, boolean verbose) throws IOException, AISPException {
		String modelsSpec= cmdargs.getOption("models");	// Should not get here unless this option has been specified.
		String checkPointFileName = cmdargs.getOption("cp", "checkpoint.csv");
		boolean showCM = cmdargs.getFlag("cm");
		boolean singleFold = cmdargs.getFlag("singleFold");
		int foldCount = cmdargs.getOption("folds", 4); 
		int foldsToEvaluate = singleFold ? 1 : foldCount;
		int balancedKfoldTrainCount = 0;
		
		File modelSpecsFile = new File(modelsSpec);
		if (!modelSpecsFile.exists())  {
			System.err.println("File " + modelsSpec + " not found");
			return false;
		}
		GetModifiedSoundOptions soundOptions = new GetModifiedSoundOptions(true, false, true);
		if (!soundOptions.parseOptions(cmdargs)) 
			return false;

		// Done parsing options, make sure there are none we don't recognize.
		if (ToolUtils.hasUnusedArguments(cmdargs))
			return false;
		
		// Read the javascript file and create the builder of maps of classifiers.
		String modelListJS;
		modelListJS = FileUtils.readTextFileIntoString(modelSpecsFile.getAbsoluteFile());
		JScriptClassifierMapFactory<double[]> buildersJS = new JScriptClassifierMapFactory<double[]>(modelListJS);
		
		// Create the map of classifiers.
		Map<String, IClassifier<double[]>> classifiers = buildersJS.build(); 

		// Get the sounds requested.
//		IShuffleIterable<SoundRecording> sounds = Evaluate.getRequestedSounds(soundDirOrMetaDataFiles, requireAllMetaData,
//				repeatableShuffle, clipLenMsec, padType, balancedLabels, useUpSampling, labelName);
		IShuffleIterable<SoundRecording> sounds = soundOptions.getSounds(); 
		System.out.println("Training data as follows:\n" + TrainingSetInfo.getInfo(sounds).prettyFormat());

		// Call the real workhorse to do the heavy lifting.
		// Returns a sorted list of performance info.
		System.out.println("Evaluating " + classifiers.size() + " models/classifiers.");
		if (new File(checkPointFileName).exists()) 
			System.out.println("Using existing checkpoint file " + checkPointFileName);
		String labelName = soundOptions.getLabel(); 
		List<ClassifierPerformance> plist = ModelUtil.rankClassifiers(classifiers, labelName, sounds, foldCount, foldsToEvaluate, balancedKfoldTrainCount, checkPointFileName, verbose);
		
		// Display the performance info.
		System.out.println("Classifier key,Classifier Class, \tF1,\tPrecision,\tRecall");
		for (ClassifierPerformance cp : plist) {
			ConfusionMatrix matrix = cp.getConfusionMatrix();
			System.out.println(cp.getKey() 
					+ "," + cp.getClassifier().getClass().getSimpleName() 
					+ "," + matrix.getF1Score().getMean()
					+ "," + matrix.getPrecision().getMean()
					+ "," + matrix.getRecall().getMean()
			);
		}
		
		ClassifierPerformance best = plist.get(0);
		System.out.println("\nBest Classifier: " + best.getClassifier());
		if (showCM)
			showAndOrExportResults(best.getConfusionMatrix(), true, null);
		
		return true;
	}

	private static boolean trainAndEvaluateClassifier(CommandArgs cmdargs, boolean verbose) throws AISPException {
		boolean evaluateOutliers = cmdargs.getFlag("outliers") || cmdargs.getFlag("outlier");
		boolean showCM = cmdargs.getFlag("cm");
		String exportCM = cmdargs.getOption("exportCM");
		boolean preShuffle= !cmdargs.getFlag("noShuffle");
		boolean singleFold = cmdargs.getFlag("singleFold");
		String kfoldBalanceArg = cmdargs.getOption("kfoldBalance");
		int kfoldBalanceCount = 0;
		int foldCount = cmdargs.getOption("folds", 4); 
		int foldsToEvaluate = singleFold ? 1 : foldCount; 
		int seed = cmdargs.getOption("seed", KFoldModelEvaluator.DEFAULT_SEED); 
		boolean parallelCM = !cmdargs.getFlag("serialCM");

		GetModifiedSoundOptions soundOptions = new GetModifiedSoundOptions(true, false, true);
		if (!soundOptions.parseOptions(cmdargs)) 
			return false;

		GetModelOptions modelOptions = new GetModelOptions();
		if (!modelOptions.parseOptions(cmdargs)) 
			return false;		// Error message issued.
		IClassifier<double[]> classifier = modelOptions.getClassifier(); 
	
		String labelName = soundOptions.getLabel(); 
		// Done parsing options, make sure there are none we don't recognize.
		if (ToolUtils.hasUnusedArguments(cmdargs))
			return false;

		
		if (foldCount < 2) {
			System.err.println("Must specify at least 2 folds");
			return false;
		}
			
//		if (classifier instanceof HistoryCombiningClassifier)  {
//			System.err.println("Training and evaluating a " + HistoryCombiningClassifier.class.getSimpleName() + " will give misleading\n"
//					+ "and poor results as the samples must be grouped by label to correctly evaluate the\n"
//					+ "classifier and k-fold evaluation does not support this. Exiting.");
//			return true;
//		} else 
		if (verbose) {
			System.out.println("Training and evaluating classifier " + classifier);
		} else {
			System.out.println("Training and evaluating classifier (" + classifier.getClass().getSimpleName() + ")");
		}

			
//		IShuffleIterable<SoundRecording> sounds = getRequestedSounds(soundDirOrMetaDataFiles, requireAllMetaData,
//				repeatableShuffle, clipLenMsec, padType, balancedLabels, useUpSampling, labelName);
		IShuffleIterable<SoundRecording> sounds = soundOptions.getSounds(); 
		
		TrainingSetInfo tsi = TrainingSetInfo.getInfo(sounds);
		if (kfoldBalanceArg != null)  {
			kfoldBalanceCount = parseKfoldBalanceArg(kfoldBalanceArg, labelName, tsi);
			if (kfoldBalanceCount < 0) 
				return false;	// Error was issued
		}
		
		System.out.println("Sounds : " + tsi.prettyFormat());
		
		/**
		 * Now that we identified the classifier and sounds to use, do the evaluation work.
		 */
		ConfusionMatrix cm;
		long startMsec = System.currentTimeMillis();
		if (evaluateOutliers)  {
			LabelInfo li = TrainingSetInfo.getInfo(sounds).getLabelInfo(labelName);
			if (li == null) {
				System.err.println("Data does not contain any data labeled with label " + labelName);
				return false;
			}
			cm = ModelUtil.evaluateOutlierDetection(classifier, sounds, labelName, li.getLabelValues(), foldCount, foldsToEvaluate, verbose);
		} else {
			cm = trainAndEvaluateClassifier(classifier, sounds, labelName, foldCount, foldsToEvaluate, kfoldBalanceCount, preShuffle, verbose, seed, parallelCM);
		}
		if (cm == null)	// error
			return false;
		if (!verifyCM(cm,labelName, tsi, parallelCM))
			return false;	// message issued.
		
		long endMsec = System.currentTimeMillis();
//		System.out.println(cm.formatStats());
//		System.out.println(cm.toString());
		long elapsedMsec = (endMsec - startMsec);
		if (verbose)
			System.out.println("Trained classifier (from last fold): " + classifier);
		System.out.println("Evaluation completed in " + elapsedMsec + " msec. " + (elapsedMsec / foldCount) + " msec/fold (computed in " + (parallelCM ? "parallel" : "serial") + ")");

		showAndOrExportResults(cm, showCM, exportCM);
		
//		showOnlineStats("Accuracy:", accuracy);
		return true;
	}

	/**
	 * Parse the kfoldBlance argument as 'max', 'min', <integer>.
	 * @param kfoldBalanceArg
	 * @param labelName	 the label on which training/evaluation is being done.
	 * @param tsi TrainingSetInfo for the full training set (test + train = all folds).
	 * @return -1 and issue an error is problems parsing, otherwise a value 0 or larger.
	 */
	private static int parseKfoldBalanceArg(String kfoldBalanceArg, String labelName, TrainingSetInfo tsi) {
		int count = 0;
		LabelInfo linfo = tsi.getLabelInfo(labelName);
		if (linfo == null) {
			System.err.println("Training set does not have samles with the label " + labelName);
			return -1;
		}
		if (kfoldBalanceArg.equals("max") ) {
			for (LabelValueInfo lvi : linfo) {
				if (lvi.getTotalSamples() > count)
					count = lvi.getTotalSamples();
			}
		} else  if (kfoldBalanceArg.equals("min") ) {
			count = Integer.MAX_VALUE;
			for (LabelValueInfo lvi : linfo) {
				if (lvi.getTotalSamples() < count)
					count = lvi.getTotalSamples();
			}
		} else {
			try {
				count = Integer.parseInt(kfoldBalanceArg);
				if (count < 0) {
					System.out.println("Kfold balance integer value must be 0 or larger");
					return -1;
				}
			} catch (Exception e) {
				System.out.println("Could not parse " + kfoldBalanceArg + " into an integer.");
				return -1;
			}
		}
		return count;
	}




	/**
	 * @param classifier
	 * @param sounds
	 * @param tsi 
	 * @param trainingLabel
	 * @param foldsToEvaluate 
	 * @param foldCount 
	 * @param verbose
	 * @param seed
	 * @param parallelCM 
	 * @return
	 * @throws AISPException
	 */
	protected static ConfusionMatrix trainAndEvaluateClassifier(IClassifier<double[]> classifier,
			IShuffleIterable<SoundRecording> sounds, String trainingLabel, int foldCount, int foldsToEvaluate, int balancedTrainCount, boolean preShuffle,  boolean verbose, int seed, boolean parallelCM)
			throws AISPException {

		KFoldModelEvaluator kfold = new KFoldModelEvaluator(foldCount, seed, preShuffle, verbose, parallelCM); 	
		System.out.println("Evaluating " + foldsToEvaluate + " of " + foldCount + " folds " 
				+ (balancedTrainCount == 0 ? "." : " with balanced training data at " + balancedTrainCount + " samples per label value."));

		if (balancedTrainCount > 0)
			balancedTrainCount = balancedTrainCount / foldCount; 
		ConfusionMatrix cm = kfold.getConfusionMatrix(classifier, sounds, trainingLabel, foldsToEvaluate, balancedTrainCount);

		return cm;
	}


	private static String showOnlineStats(String prefix, OnlineStats stats) {
		String values = String.format("%7.3f +/- %6.4f%%",100.0 * stats.getMean(), 100.0 * stats.getStdDev());
		return prefix + values; 
//		return prefix + (100.0 * stats.getMean())  + "% +/- " + (100.0 * stats.getStdDev()) + "%";
		
	}
	private static void showAndOrExportResults(ConfusionMatrix cm, boolean showCM, String exportCMPath) {
		System.out.println("Evaluated label name: " + cm.getLabelName());
		if (showCM) {
			System.out.println(cm.toString());
			System.out.println(cm.formatStats());
		}
		System.out.print(showOnlineStats("Precision: ", cm.getPrecision(true)));
		System.out.print(showOnlineStats(" (micro), ", cm.getPrecision(false)));
		System.out.println(" (macro)");
		System.out.print(showOnlineStats("Recall   : ", cm.getRecall(true)));
		System.out.print(showOnlineStats(" (micro), ", cm.getRecall(false)));
		System.out.println(" (macro)");
		System.out.print(showOnlineStats("F1       : ", cm.getF1Score(true)));
		System.out.print(showOnlineStats(" (micro), ", cm.getF1Score(false)));
		System.out.println(" (macro)");

		if (exportCMPath != null) 
			exportCM(cm, exportCMPath);
	}
	
	private static void exportCM(ConfusionMatrix cm, String path){
    	String matrixAsCsv = cm.mapToCsv(cm.getPercents());
        FileWriter fileWriter = null; 
        try {
            fileWriter = new FileWriter(path);
			fileWriter.write("Evaluated label name: " + cm.getLabelName());
			fileWriter.write(matrixAsCsv + "\n\n\n");
			fileWriter.write(showOnlineStats("Precision:, ", cm.getPrecision()) + "\n");
			fileWriter.write(showOnlineStats("Recall:, ", cm.getRecall()) + "\n");
			fileWriter.write(showOnlineStats("F1:, ", cm.getF1Score()) + "\n");
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}  finally {
			if (fileWriter != null)
				try {
					fileWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	/**
	 * @param cmdargs
	 * @param verbose 
	 * @param soundDir
	 * @throws AISPException
	 * @throws IOException 
	 */
	private static boolean evaluateExistingModel(CommandArgs cmdargs, boolean verbose)
			throws AISPException, IOException {
		boolean showCM = cmdargs.getFlag("cm");
		String exportCM = cmdargs.getOption("exportCM");
//		int iterations = cmdargs.getOption("iterations", DEFAULT_HISTORICAL_ITERATIONS);
		boolean parallelCM = !cmdargs.getFlag("-serialCM");

		GetTrainedModelOptions modelOptions = new GetTrainedModelOptions();
		if (!modelOptions.parseOptions(cmdargs)) 
			return false;				// Error message was issued
		
		GetModifiedSoundOptions soundOptions = new GetModifiedSoundOptions(false,true,true);	// If label not given, use the model's label
		if (!soundOptions.parseOptions(cmdargs)) 
			return false;

//		if (iterations < 1) {
//			System.err.println("Number of iterations must be 1 or larger.");
//			return false;
//		}
		
		// Done parsing options, make sure there are none we don't recognize.
		if (ToolUtils.hasUnusedArguments(cmdargs))
			return false;
		
		// Make sure labels are grouped if evaluating a HistoryCombiningClassifier.
		IFixedClassifier<double[]> c = modelOptions.getClassifier();
		if (c == null)
			return false;	// message was issued
//		if (c instanceof HistoryCombiningClassifier || c instanceof FixedCombiningClassifier) {
//			if (!groupLabelValues) {
//				System.err.println("Evaluating a " + HistoryCombiningClassifier.class.getSimpleName() 
//						+ " requires grouping sounds by label value,\nbut grouping was not requested."  
//						+ " Setting this automatically.\nUse -group-label-values to avoid this message in the future.\n" );
//				groupLabelValues = true;
//			}
//		}
		
		// Get all the labels that are being evaluated (from a comma-separated listed provided by the command line). 
		String trainingLabel = soundOptions.getLabel(); 
		String trainingLabels[] = null;
		if (trainingLabel != null)  
			trainingLabels = IClassifier.parseMultiLabels(trainingLabel);
		
		// Get an iterable over the sounds to be tested. 
		IShuffleIterable<SoundRecording> sounds = soundOptions.getSounds();
		TrainingSetInfo tsi = TrainingSetInfo.getInfo(sounds);
		System.out.println("Sounds : " + tsi.prettyFormat());

		// Finally, determine the accuracy of the model. 
		double duration = 0;	// To classify all sounds once.
		duration = evaluateExistingModelUngroupedSounds(modelOptions, trainingLabels, sounds, tsi, showCM, exportCM, parallelCM);
		if (duration < 0)
			return false;	// An error was encountered with message issued.

		double ratePercent = 100 * duration / tsi.getTotalMsec();
		System.out.printf("Classification performance: %7.5f %% of clip length (computed in %s)", ratePercent, parallelCM ? "parallel" : "serial");

		return true;
	}

	/**
	 * @param modelOptions
	 * @param trainingLabels if null, then evaluate all labels from the classifier.
	 * @param sounds
	 * @param tsi 
	 * @param showCM
	 * @param exportCM
	 * @param duration
	 * @return the length of time to classify/evaluate all sounds once. less than 0 on error and issue an error message.
	 * @throws AISPException
	 * @throws IOException
	 */
	private static double evaluateExistingModelUngroupedSounds(GetTrainedModelOptions modelOptions, String[] trainingLabels,
			IShuffleIterable<SoundRecording> sounds, TrainingSetInfo tsi, boolean showCM, String exportCM, boolean parallelCM)
			throws AISPException, IOException {
		IFixedClassifier<double[]> classifier = modelOptions.getClassifier();
		if (classifier == null)
			return -1;	// And error message was issued
		System.out.println("Evaluating classifier: " + classifier);
		if (trainingLabels == null)
			trainingLabels = IClassifier.parseMultiLabels(classifier.getTrainedLabel());
		final double labelCount = trainingLabels.length;
		if (labelCount == 0) {
			System.out.println("No training labels specified or found on the model");
			return -1;
		}
		
		double duration = 0;
		for (String label : trainingLabels) {
			long start = System.currentTimeMillis();
			ConfusionMatrix cm = ConfusionMatrix.compute(label, classifier, sounds, parallelCM);
			duration += System.currentTimeMillis() - start;
			if (!verifyCM(cm,label, tsi,parallelCM)) 
				return -1;	// Error message issued
			showAndOrExportResults(cm, showCM, exportCM);
		}
		duration /= labelCount;
		return duration;
	}

	/**
	 * Make sure the Confusion matrix counts match the TrainingSetInfo counts for the given label.
	 * @param cm
	 * @param label
	 * @param tsi
	 * @param parallelCM 
	 * @return false if the counts don't match, indicating there may be a problem with the classify() method's thread-safety.
	 */
	private static boolean verifyCM(ConfusionMatrix cm, String label, TrainingSetInfo tsi, boolean parallelCM) {
		if (!parallelCM)
			return true;
		int cmCount = cm.getTotalSamples();
		LabelInfo linfo = tsi.getLabelInfo(label);
		int tsiCount = linfo == null ? 0 : tsi.getLabelInfo(label).getTotalSamples();
		if (cmCount != tsiCount) {
			System.err.println("Confusion matrix sample count (" + cmCount + ") is not equal to training set info count for label " + label + "(" + tsiCount + ")."
					+ " This may indicate that the classifier being used is not thread-safe in its classify() method. Try using the -serialCM option.");
			return false;
		}
		return true;
	}


}
