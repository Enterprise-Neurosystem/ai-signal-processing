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
package org.eng.aisp.classifier.cnn;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.EarlyStoppingResult;
import org.deeplearning4j.earlystopping.listener.EarlyStoppingListener;
import org.deeplearning4j.earlystopping.scorecalc.ClassificationScoreCalculator;
import org.deeplearning4j.earlystopping.scorecalc.ScoreCalculator;
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingTrainer;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLogger;
import org.eng.aisp.AISPProperties;
import org.eng.aisp.classifier.AbstractFixableFeatureExtractingClassifier;
import org.eng.aisp.classifier.IFixableClassifier;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.LogMelFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.transform.ITrainingWindowTransform;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class CNNClassifier extends AbstractFixableFeatureExtractingClassifier<double[], double[]> implements IFixableClassifier<double[]> {

	private static final long serialVersionUID = 2966528639796200129L;
	
	//Used for setting log level of dl4j in the code
//	public static final ch.qos.logback.classic.Logger logbackRootLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
	
	/**
	 * Recommended default sub window size of subwindows on which features are extracted. 
	 * Value is {@value #DEFAULT_WINDOW_SIZE_MSEC}.
	 */
	public final static int DEFAULT_WINDOW_SIZE_MSEC = 40;
	/**
 	 * Recommended default window shift size when extracting subwindows for feature extraction. 0 should indicate rolling windows. 
	 * Value is {@value #DEFAULT_WINDOW_SHIFT_MSEC}.
	 */
	public final static int DEFAULT_WINDOW_SHIFT_MSEC = 20;
	
	
	public final static int DEFAULT_NUM_EPOCH = 75;

	/**
	 * Defines the default batch size for learning when it is not specified via a constructor.
	 */
	public final static String DEFAULT_BATCH_SIZE_PROPERTY_NAME = "classifiers.cnn.batch_size";
	public final static int DEFAULT_BATCH_SIZE = AISPProperties.instance().getProperty(DEFAULT_BATCH_SIZE_PROPERTY_NAME, 32);
	protected final int batchSize;

	public final static String DEFAULT_USE_DISK_CACHE_PROPERTY_NAME = "classifiers.cnn.use_disk_cache";
	public static final boolean DEFAULT_USE_DISK_CACHE = AISPProperties.instance().getProperty(DEFAULT_USE_DISK_CACHE_PROPERTY_NAME, false);	

	/** Name of property to configure stride sizes in layers 1 and 3.  Height corresponds to time indexing. */
	public final static String STRIDE_HEIGHT_PROPERTY_NAME = "classifiers.cnn.stride.height";
	public final static int STRIDE_HEIGHT_DEFAULT = 2;
	private final int STRIDE_HEIGHT = AISPProperties.instance().getProperty(STRIDE_HEIGHT_PROPERTY_NAME, STRIDE_HEIGHT_DEFAULT);

	/** Name of property to configure stride sizes in layers 1 and 3.  Width corresponds to feature indexing. */
	public final static String STRIDE_WIDTH_PROPERTY_NAME = "classifiers.cnn.stride.width";
	public final static int STRIDE_WIDTH_DEFAULT = 2;
	private final int STRIDE_WIDTH= AISPProperties.instance().getProperty(STRIDE_WIDTH_PROPERTY_NAME, STRIDE_WIDTH_DEFAULT);
    
	/** 
	 * Controls whether or not training uses early stopping based on meeting a min score using DL4J's mechanism or ours 
	 * @deprecated in favor os setting MIN_SCORE_CHANGE_PER_EPOCH to 0.
	 */
	public final static String USE_EARLY_STOPPING_PROPERTY_NAME = "classifiers.cnn.early_stopping.dl4j.enabled";
	/**
	 * @deprecated in favor os setting MIN_SCORE_CHANGE_PER_EPOCH to 0.
	 */
	protected final static boolean DEFAULT_USE_EARLY_STOPPING = true;
	/**
	 * @deprecated in favor os setting MIN_SCORE_CHANGE_PER_EPOCH to 0.
	 */
	private final static boolean USE_EARLY_STOPPING = AISPProperties.instance().getProperty(USE_EARLY_STOPPING_PROPERTY_NAME, DEFAULT_USE_EARLY_STOPPING);
	
	/** Defines the minimum score that will early terminate epoch training if early stopping is enabled */
	public final static String MIN_EARLY_STOPPING_SCORE_PROPERTY_NAME = "classifiers.cnn.early_stopping.min_score";
	public final static double DEFAULT_MIN_EARLY_STOPPING_SCORE= 0.999;
	private final static double MIN_EARLY_STOPPING_SCORE = AISPProperties.instance().getProperty(MIN_EARLY_STOPPING_SCORE_PROPERTY_NAME, DEFAULT_MIN_EARLY_STOPPING_SCORE);

	/** Defines the number of folds used when defining training and test data.  Set to 0 to have test=train data */ 
	public final static String TRAINING_FOLDS_PROPERTY_NAME = "classifiers.cnn.training_folds";
	public final static int DEFAULT_DEFAULT_TRAINING_FOLDS = 5; 
	public final static int DEFAULT_TRAINING_FOLDS = AISPProperties.instance().getProperty(TRAINING_FOLDS_PROPERTY_NAME, DEFAULT_DEFAULT_TRAINING_FOLDS);
	private final int trainingFolds;

	/** Defines the minimum slope across EPOCH_SCORE_HISTORY_SIZE epochs that will terminate training a new epoch */ 
	public final static String MIN_SCORE_CHANGE_PER_EPOCH_PROPERTY_NAME = "classifiers.cnn.early_stopping.min_score_change_per_epoch";
	public final static double DEFAULT_MIN_SCORE_CHANGE_PER_EPOCH = AISPProperties.instance().getProperty(MIN_SCORE_CHANGE_PER_EPOCH_PROPERTY_NAME, 
							USE_EARLY_STOPPING ? 0.001 : 0.0); 	// Continue to support USE_EARLY_STOPPING, by setting slope to 0 which turns off early stopping.
	private final double minScoreChangePerEpoch;
	
	/** The number of epochs across which a slope is computed for the change in score */ 
	public final static String EPOCH_SCORE_HISTORY_SIZE_PROPERTY_NAME = "classifiers.cnn.early_stopping.epoch_score_history_size";
	public final static int DEFAULT_EPOCH_SCORE_HISTORY_SIZE = AISPProperties.instance().getProperty(EPOCH_SCORE_HISTORY_SIZE_PROPERTY_NAME, 3); 
	private final int epochScoreHistorySize;
	
	/** The epoch number at which scoring and tracking of score history for early stopping should start int he OptimizedScorerEpochTerminator */ 
	public final static String START_SCORE_TRACKING_EPOCH_NUM_PROPERTY_NAME = "classifiers.cnn.early_stopping.start_score_tracking_epoch";
	public final static int DEFAULT_START_SCORE_TRACKING_EPOCH_NUM = -1; 	// Let Optimizer pick
//	public final static int DEFAULT_START_SCORE_TRACKING_EPOCH_NUM = 0; 	// Probably for debugging Optimizer
	private final static int START_SCORE_TRACKING_EPOCH_NUM = AISPProperties.instance().getProperty(START_SCORE_TRACKING_EPOCH_NUM_PROPERTY_NAME, DEFAULT_START_SCORE_TRACKING_EPOCH_NUM);
	
	public final static String VERBOSE_PROPERTY_NAME = "classifiers.cnn.verbose";
	protected final static boolean VERBOSE = AISPProperties.instance().getProperty(VERBOSE_PROPERTY_NAME, false);

	/**
	 * Defines the default feature extractor to use.
	 * Default is a MFCCFeatureExtractor constructed with new MFCCFeatureExtractor(40).
	 */
	public final static IFeatureExtractor<double[],double[]> DEFAULT_FEATURE_EXTRACTOR = new LogMelFeatureExtractor(64);
//	public final static IFeatureExtractor<double[],double[]> DEFAULT_FEATURE_EXTRACTOR = new MFCCFeatureExtractor(40);
//	public final static IFeatureExtractor<double[],double[]> DEFAULT_FEATURE_EXTRACTOR = new MFCCFeatureExtractor();
	
	/**
	 * Defines the default feature processor to use.
	 * Default is not use a feature processor. 
	 */
	public final static IFeatureProcessor<double[]> DEFAULT_FEATURE_PROCESSOR = null;

	public static final ITrainingWindowTransform<double[]> DEFAULT_TRANSFORMS = null; 
	

	private class VerboseTrainingListener implements EarlyStoppingListener<MultiLayerNetwork> {
		
		long startMsec;
		long lastEpochMsec = 0;
//		final CSVTable scoreTable;
		private final ScoreCalculator scorer;

		public VerboseTrainingListener(ScoreCalculator scorer) {
			this.scorer = scorer;
//			if (scorer == null)
//				scoreTable = new CSVTable(Arrays.asList("Epoch", "Score"));
//			else
//				scoreTable = new CSVTable(Arrays.asList("Epoch", "Score", scorer.getClass().getSimpleName()));
		}
		
		@Override
		public void onStart(EarlyStoppingConfiguration<MultiLayerNetwork> esConfig, MultiLayerNetwork net) { 
			AISPLogger.logger.info(1, "Training started...");
			this.startMsec = System.currentTimeMillis();
		}

		@Override
		public void onEpoch(int epochNum, double score, EarlyStoppingConfiguration<MultiLayerNetwork> esConfig, MultiLayerNetwork net) {
			long now = System.currentTimeMillis();
			double deltaSec = (now - (lastEpochMsec == 0 ? startMsec : lastEpochMsec)) / 1000.;
			lastEpochMsec = now;
			if (scorer == null) {
				AISPLogger.logger.info(1,"Completed epoch " + epochNum + " in " + deltaSec + " seconds, score=" + score);
//				scoreTable.appendRow("" + epochNum, "" + score);
			} else {
				double otherScore = scorer.calculateScore(net);
				AISPLogger.logger.info(1,"Completed epoch " + epochNum + " in " + deltaSec + " seconds, score=" + score + ", secondary score=" + otherScore);
//				scoreTable.appendRow("" + epochNum, "" + score, "" + otherScore);
			}
//			if (epochNum % 10 == 0)
//				System.out.println(scoreTable.write());
		}

		@Override
		public void onCompletion(EarlyStoppingResult<MultiLayerNetwork> esResult) {
			long now = System.currentTimeMillis();
			double deltaSec = (now - startMsec) / 1000.;
			String msg = "Termination reason: " + esResult.getTerminationReason()
				+ "\n\tTermination details: " + esResult.getTerminationDetails()
				+ "\n\tTotal epochs: " + esResult.getTotalEpochs()
				+ "\n\tBest epoch number: " + esResult.getBestModelEpoch()
				+ "\n\tScore at best epoch: " + esResult.getBestModelScore()
				+ "\n\tTotal training time: " + deltaSec + " seconds." 
			;
//			msg += "\n\n" + scoreTable.write();
			AISPLogger.logger.info(1,msg);;
		}
	}

	protected final int nChannels = 1;
    protected int outputNum;
    protected final int nEpochs;
//    private int iterations = 1; /** No long used  after upgrading to dl4j 1.0.0 beta2 10/2018 */
    protected final int seed = 123;

	public CNNClassifier() {
		this(DEFAULT_NUM_EPOCH);
	}

    public CNNClassifier(int nEpochs) {
		this(DEFAULT_WINDOW_SIZE_MSEC, DEFAULT_WINDOW_SHIFT_MSEC, nEpochs);
	}

	public CNNClassifier(int windowSizeMsec, 
			int windowShiftMsec, int nEpochs) {
		this(windowSizeMsec, windowShiftMsec, DEFAULT_USE_DISK_CACHE, nEpochs);
	}

	public CNNClassifier(int windowSizeMsec, 
			int windowShiftMsec, boolean useDiskCache, int nEpochs) {
		this(null, DEFAULT_FEATURE_EXTRACTOR, DEFAULT_FEATURE_PROCESSOR, windowSizeMsec, windowShiftMsec, useDiskCache, nEpochs);
	}

	/**
	 * 
	 * @param transforms
	 * @param extractor
	 * @param featureProcessor
	 * @param windowSizeMsec
	 * @param windowShiftMsec
	 * @param useDiskCache
	 * @param nEpochs
	 * @deprecated in favor of {@link #CNNClassifier(ITrainingWindowTransform, IFeatureGramDescriptor, boolean, boolean, int, int, int)} 
	 */
	public CNNClassifier(ITrainingWindowTransform<double[]> transforms,			
			IFeatureExtractor<double[], double[]> extractor, 
			IFeatureProcessor<double[]> featureProcessor, 
			int windowSizeMsec, int windowShiftMsec, boolean useDiskCache, int nEpochs) {
		// Always use either a memory or disk cache since make multiple passes.
		this(transforms, extractor, featureProcessor, windowSizeMsec, windowShiftMsec, false, useDiskCache, nEpochs);  
		
//		Nd4j.ENFORCE_NUMERICAL_STABILITY = true;  //Enabling this line causes some unexpected behavior of the system, see issue #295. This is for avoiding NaN values.
		
	}

	/**
	 * Convenience on {@link #CNNClassifier(ITrainingWindowTransform, IFeatureExtractor, IFeatureProcessor, int, int, boolean, boolean, int, int)}
	 * with batchSize=-1 so to use system defaults per properties.
	 */
	public CNNClassifier(ITrainingWindowTransform<double[]> transforms, 			
			IFeatureExtractor<double[], double[]> extractor,
			IFeatureProcessor<double[]> featureProcessor, 
			int windowSizeMsec, int windowShiftMsec, boolean useMemCache, boolean useDiskCache, int nEpochs) {
		this(transforms, extractor, featureProcessor, windowSizeMsec, windowShiftMsec, useMemCache, useDiskCache, nEpochs, -1, DEFAULT_TRAINING_FOLDS);
	}
	
	public CNNClassifier(ITrainingWindowTransform<double[]> transform, 	IFeatureGramDescriptor<double[], double[]> fge,
			boolean useMemCache, boolean useDiskCache, int nEpochs) {
		this(transform, fge, useMemCache, useDiskCache, nEpochs, 0, DEFAULT_TRAINING_FOLDS, DEFAULT_EPOCH_SCORE_HISTORY_SIZE, DEFAULT_MIN_SCORE_CHANGE_PER_EPOCH);
	}


	/**
	 * 
	 * @param transform
	 * @param fge
	 * @param useMemCache
	 * @param useDiskCache
	 * @param nEpochs the maximum number of epochs to use in training
	 * @param batchSize the size of the batch of training data used during training
	 * @param trainingFolds sets the size of the validation data set used during training. If greater than 2,
	 * then N-1 folds are used for training and 1 fold is used for validation (error estimation) during training.
	 * If set to 0, then the whole training data set is used as the validation set.
	 * @param epochScoreHistorySize the length of history over which to compute the score change per epoch (see minScoreChangePerEpoch parameter)
	 * @param minScoreChangePerEpoch used to control early stopping such that if the change in score per epoch is less than this number, then
	 * training will stop before the max number of epochs.  Set to 0 to disable early stopping.
	 */
	public CNNClassifier(ITrainingWindowTransform<double[]> transform, 	IFeatureGramDescriptor<double[], double[]> fge,
			boolean useMemCache, boolean useDiskCache, int nEpochs, int batchSize, int trainingFolds, int epochScoreHistorySize, double minScoreChangePerEpoch) {
		super(true, transform, fge, useMemCache, useDiskCache);
		if (nEpochs <= 0) 
			throw new IllegalArgumentException("The number of epochs must be greater than 0.");
		this.nEpochs = nEpochs;

		if (batchSize <= 0) 
			batchSize = AISPProperties.instance().getProperty(DEFAULT_BATCH_SIZE_PROPERTY_NAME, DEFAULT_BATCH_SIZE);
		this.batchSize = batchSize;

		if (trainingFolds < 0 || trainingFolds == 1)
			throw new IllegalArgumentException("The number of training folds must be 0 or greater than 1.");
		this.trainingFolds = trainingFolds;
			
		if (minScoreChangePerEpoch < 0)
			throw new IllegalArgumentException("The score change per epoch must be 0 or larger"); 
		if (minScoreChangePerEpoch > 1)
			throw new IllegalArgumentException("The score change per epoch must be less or equal to 1"); 
		this.minScoreChangePerEpoch = minScoreChangePerEpoch;
		
		if (epochScoreHistorySize < 0)
			throw new IllegalArgumentException("The epoch score history size must be 0 or larger"); 
		if (epochScoreHistorySize > nEpochs)
			throw new IllegalArgumentException("The epoch score history size must be less or equal to the number of epochs (" + nEpochs + ")"); 
		this.epochScoreHistorySize = epochScoreHistorySize;

	}


	/**
	 * @deprecated in favor of {@link #CNNClassifier(ITrainingWindowTransform, IFeatureGramDescriptor, boolean, boolean, int, int, int)} 
	 */
	public CNNClassifier(ITrainingWindowTransform<double[]> transform, 			
			IFeatureExtractor<double[], double[]> extractor,
			IFeatureProcessor<double[]> featureProcessor, 
			int windowSizeMsec, int windowShiftMsec, boolean useMemCache, boolean useDiskCache, int nEpochs, int batchSize, int trainingFolds) {
		// Always use either a memory or disk cache since make multiple passes.
		this(transform, new FeatureGramDescriptor<>(windowSizeMsec, windowShiftMsec, extractor, featureProcessor),  useMemCache, useDiskCache, 
				nEpochs, batchSize, trainingFolds, DEFAULT_EPOCH_SCORE_HISTORY_SIZE, DEFAULT_MIN_SCORE_CHANGE_PER_EPOCH);
//		super(true, transforms, extractor, featureProcessor, windowSizeMsec, windowShiftMsec, useMemCache, useDiskCache);  
//		this.nEpochs = nEpochs;
//		if (batchSize <= 0) 
//			batchSize = AISPProperties.instance().getProperty(DEFAULT_BATCH_SIZE_PROPERTY_NAME, DEFAULT_BATCH_SIZE);
//		this.batchSize = batchSize;
//		this.trainingFolds = trainingFolds;
		
//		Nd4j.ENFORCE_NUMERICAL_STABILITY = true;  //Enabling this line causes some unexpected behavior of the system, see issue #295. This is for avoiding NaN values.
		
	}	
	
	@Override
	protected IFixedClassifier<double[]> trainFixedClassifierOnFeatures(
			Iterable<? extends ILabeledFeatureGram<double[]>[]> features) throws AISPException {
		if (!USE_EARLY_STOPPING) {
			AISPLogger.logger.severe("Property " + USE_EARLY_STOPPING_PROPERTY_NAME + " is deprecated in favor of setting property " + MIN_SCORE_CHANGE_PER_EPOCH_PROPERTY_NAME 
					+ " to 0 or otherwise setting the min change per epoch to 0.  This effectively turns off early stopping based on scores.");
		}
		return this.trainFixedClassifierOnFeaturesEarlyStop(features);	
//		else
//			return this.trainFixedClassifierOnFeaturesFixedEpochs(features);
		
	}

	/**
	 * Get an array of 2 FeatureDataSetIterator, the first element is the training data and the second the test data.
	 * We encapsulate to make it easy to test switching between test=training and test=f(training).
	 * @param features
	 * @param folds the number of folds to divide the data into.  N-1 of the folds are training data, 1 of the folds is test.
	 * If 1 or smaller, then test data is the same as training data.
	 * @return never null, always an array of length 2. [0] is training, [1] is test.
	 * @throws AISPException
	 */
	private FeatureDataSetIterator[] getTrainAndTestData(Iterable<? extends ILabeledFeatureGram<double[]>[]> features, int folds) throws AISPException {
		int bs = this.batchSize;
      	FeatureDataSetIterator trainingDataSet;
      	FeatureDataSetIterator testDataSet;
      	if (folds > 1) {
      		if (VERBOSE)
      			AISPLogger.logger.info("Defining training and test data sets using " + folds + " folds");
      		Iterable<? extends ILabeledFeatureGram<double[]>[]> trainingFeatures = getItemFolds(features, folds,0,true);
      		Iterable<? extends ILabeledFeatureGram<double[]>[]> testFeatures     = getItemFolds(features, folds,0,false);
      		trainingDataSet = new FeatureDataSetIterator(trainingFeatures, bs, primaryTrainingLabel, null, null, nChannels);
      		testDataSet = new FeatureDataSetIterator(testFeatures, bs, primaryTrainingLabel, null, null, nChannels);
      		if (!trainingDataSet.getNumToStrLabelMap().equals(testDataSet.getNumToStrLabelMap()))
      			// TODO: We should really be building a map of label values to integers and passing it in to both FeatureDataSetIterator constructors.
      			throw new AISPException("Training and test data sets do not have the same mapping of labels to indexes. "
      					+ " This can happen if there is too little data in the test data.  Try fewer folds (" + folds + ") or more data.");
      	} else {
      		if (VERBOSE)
      			AISPLogger.logger.info("Defining training set == test data"); 
      		trainingDataSet = new FeatureDataSetIterator(features, bs, primaryTrainingLabel, null, null, nChannels);
      		testDataSet = trainingDataSet; 
      	}
		FeatureDataSetIterator[] r = new FeatureDataSetIterator[2];
      	r[0] = trainingDataSet;
      	r[1] = testDataSet;
      	return r;
	}

	private IFixedClassifier<double[]> trainFixedClassifierOnFeaturesEarlyStop(
			Iterable<? extends ILabeledFeatureGram<double[]>[]> features) throws AISPException {

		if (VERBOSE)
			AISPLogger.logger.info("Load data....");
		
//		// TODO: make sure there is only 1 channel in the input data.
//        nChannels = 1;

        // Get the test and training data, which might be the same;.
        FeatureDataSetIterator[] data = getTrainAndTestData(features,this.trainingFolds);
        FeatureDataSetIterator trainingDataSet = data[0];
        FeatureDataSetIterator testDataSet = data[1]; 
//        if (VERBOSE) {
//        	AISPLogger.logger.info("Training data : " +  trainingDataSet.getStrToNumLabelMap());
//        	AISPLogger.logger.info("Test data     : " +  testDataSet.getStrToNumLabelMap());
//        } 
		if (trainingDataSet.getNumLabels() == 0)
			throw new AISPException("Training on label '" + primaryTrainingLabel + "' which is not found in training data");

		int numSamplesPerWindow = trainingDataSet.getFeatureLength();
		int numSubWindow = trainingDataSet.getNumSubWindow();

        //TODO The model should be able to adapt to different number of subwindows.
		
        Map<Integer, String> numToStrLabelMap = trainingDataSet.getNumToStrLabelMap();
        
        if (VERBOSE)
        	AISPLogger.logger.info("Build model....");
        MultiLayerConfiguration conf = createNetworkConfig(trainingDataSet); 

//        Metric scoreMetric = Metric.F1;	// beta2
        Evaluation.Metric scoreMetric = Evaluation.Metric.F1;	// beta4
//        Metric scoreMetric = Metric.RECALL;
        if (VERBOSE)
        	AISPLogger.logger.info("Will train with early stopping using min " + scoreMetric + " score=" + MIN_EARLY_STOPPING_SCORE);
        
        ScoreCalculator<Model> scorer = new ClassificationScoreCalculator(scoreMetric, testDataSet);
//        ScoreCalculator<Model> scorer = new DataSetLossCalculator(testDataSet, true);  
//        int startScoringAtEpoch = Math.max(10, nEpochs/8);	
//        int startScoringAtEpoch = nEpochs/2;	
        OptimizedScorerEpochTerminator scoreTerminator = new OptimizedScorerEpochTerminator(nEpochs, scorer, MIN_EARLY_STOPPING_SCORE, START_SCORE_TRACKING_EPOCH_NUM,
        		this.epochScoreHistorySize, this.minScoreChangePerEpoch);
//        		EPOCH_SCORE_HISTORY_SIZE, MIN_SCORE_CHANGE_PER_EPOCH);
        EarlyStoppingConfiguration<MultiLayerNetwork> esConf = new EarlyStoppingConfiguration.Builder<MultiLayerNetwork>()
        		.epochTerminationConditions(
//        				new MaxEpochsTerminationCondition(nEpochs)
        									scoreTerminator
//        									,new MaxScoreEpochTerminationCondition(MAX_EARLY_STOPPING_SCORE)
//        									,new ScoreImprovementEpochTerminationCondition(16)	// Search forward for N epochs to find a better epoch
//        									,new ConsecutiveScoreEpochTerminationCondition(4))	// Only allow M consecutive epochs with the same score.
        		)
//        		.iterationTerminationConditions(new MaxTimeIterationTerminationCondition(1, TimeUnit.MINUTES))
//        		.scoreCalculator(new MinEpochScoreCalculator(scorer, nEpochs/2))		// Train 1/2 the epochs before we start tracking the score 
        		.scoreCalculator(scoreTerminator)
                .evaluateEveryNEpochs(1)
//        		.modelSaver(new LocalFileModelSaver(tmpDir.getAbsolutePath()))
        		.build();

        EarlyStoppingTrainer trainer = new EarlyStoppingTrainer(esConf,conf,trainingDataSet);

        if (VERBOSE) {
        	ScoreCalculator otherScorer = null; // new ClassificationScoreCalculator(scoreMetric, trainingDataSet);  
        	trainer.setListener(new VerboseTrainingListener(otherScorer));
        }

        //Conduct early stopping training:
        EarlyStoppingResult<MultiLayerNetwork> result = trainer.fit();

        //Get the best model:
        MultiLayerNetwork model = result.getBestModel();

        //The below is obtained from ModelSerializer.writeModel() in dl4j package but is much simpler
        String modelConfig = model.getLayerWiseConfigurations().toJson();
        INDArray modelParams = model.params();


		return new FixedCNNClassifier(primaryTrainingLabel, featureGramDescriptors, batchSize, outputNum, numToStrLabelMap, 
				numSamplesPerWindow, numSubWindow, nChannels, modelConfig, modelParams);
        
	}

//	private IFixedClassifier<double[]> trainFixedClassifierOnFeaturesFixedEpochs(
//			Iterable<? extends ILabeledFeatureGram<double[]>[]> features) throws AISPException {
//
//		if (VERBOSE)
//			AISPLogger.logger.info("Load data....");
//		
////		// TODO: make sure there is only 1 channel in the input data.
////        nChannels = 1;
//
//        // Get the test and training data, which might be the same;.
//        FeatureDataSetIterator[] data = getTrainAndTestData(features, this.trainingFolds);
//        FeatureDataSetIterator trainingDataSet = data[0];
//        FeatureDataSetIterator testDataSet = data[1];  
//        
////        if (VERBOSE) {
////        	AISPLogger.logger.info("Training data : " +  trainingDataSet.getStrToNumLabelMap());
////        	AISPLogger.logger.info("Training data : " +  testDataSet.getStrToNumLabelMap());
////        }
//        
//		if (trainingDataSet.getNumLabels() == 0)
//			throw new AISPException("Training on label '" + primaryTrainingLabel + "' which is not found in training data");
//
//		int numSamplesPerWindow = trainingDataSet.getFeatureLength();
//		int numSubWindow = trainingDataSet.getNumSubWindow();
//
//        //TODO The model should be able to adapt to different number of subwindows.
//		
//        Map<Integer, String> numToStrLabelMap = trainingDataSet.getNumToStrLabelMap();
//        
//        if (VERBOSE)
//        	AISPLogger.logger.info("Build model....");
//        MultiLayerConfiguration conf = createNetworkConfig(trainingDataSet); 
//
//
//        MultiLayerNetwork model;
//        model = new MultiLayerNetwork(conf);
//        model.init();
//        Evaluation.Metric scoreMetric = Evaluation.Metric.F1;	// beta4
////        ScoreCalculator<Model> scorer = new ClassificationScoreCalculator(scoreMetric, testDataSet);
////        OptimizedScorerEpochTerminator scoreTerminator = new OptimizedScorerEpochTerminator(nEpochs, scorer, nEpochs/2, MAX_EARLY_STOPPING_SCORE);
////        int startScoringAtEpoch = -1;	// Every epoch
////        int startScoringAtEpoch = Math.max(10, nEpochs/8); // nEpochs/2;	// Every epoch
////        OptimizedScorerEpochTerminator scoreTerminator = new OptimizedScorerEpochTerminator(nEpochs, scorer, MIN_EARLY_STOPPING_SCORE, START_SCORE_TRACKING_EPOCH_NUM,
////        		EPOCH_SCORE_HISTORY_SIZE, MIN_SCORE_CHANGE_PER_EPOCH);
////        ScoreCalculator otherScorer = VERBOSE ? new ClassificationScoreCalculator(scoreMetric, trainingDataSet) : null;  
//        OptimizedScorerEpochTerminator scoreTerminator = null; 
//        ScoreCalculator otherScorer = null; 
//        InMemoryModelSaver<MultiLayerNetwork> modelSaver = new InMemoryModelSaver<MultiLayerNetwork>();
//        
//        if (VERBOSE)
//        	AISPLogger.logger.info("Training model...\n\tscorer=" + scoreTerminator);
//        double bestScore = scoreTerminator == null || scoreTerminator.minimizeScore() ? Double.MAX_VALUE : -Double.MAX_VALUE;
//        long startTrainingTime = System.currentTimeMillis();
//        for( int i=0; i<nEpochs; i++ ) {
//			long startTime = System.currentTimeMillis();
//
//            model.fit(trainingDataSet);
//
//            // Include scoring in timing of epoch
//			double score = scoreTerminator == null ? 0 : scoreTerminator.calculateScore(model);
//			boolean terminate = scoreTerminator == null ? false : (scoreTerminator.terminate(i, score, false));	// beta4
//			double otherScore = otherScorer == null ? 0 :  otherScorer.calculateScore(model);
//
//			long endTime   = System.currentTimeMillis();
//			double totalSeconds = (endTime - startTime) / 1000.0;
//
//			if (VERBOSE) {
//				if (scoreTerminator != null) {
//					if (otherScorer != null)
//						AISPLogger.logger.info("*** Completed epoch " + i + ", " + scoreMetric + " score=" + score + ", 2nd score=" + otherScore + ", took " + totalSeconds + " sec ***");
//					else
//						AISPLogger.logger.info("*** Completed epoch " + i + ", " + scoreMetric + " score=" + score + ", took " + totalSeconds + " sec ***");
//				} else {
//					AISPLogger.logger.info("*** Completed epoch " + i + ", took " + totalSeconds + " sec ***");
//				}
//			}
//			if (scoreTerminator == null) {
//				;
//			} else if ((scoreTerminator.minimizeScore() && score < bestScore)
//			    || (!scoreTerminator.minimizeScore() && score > bestScore)) {
//				try {
//					modelSaver.saveBestModel(model, score);
//				} catch (IOException e) {
//					// should never get here with memory model saver
//					e.printStackTrace();;
//				}
//			}
//			if (terminate) {
//				if (VERBOSE)
//					AISPLogger.logger.info("*** Terminating training as result of meeting score requirement"); 
//				break;
//			}
//			
//			trainingDataSet.reset();
//        }
//		
//        
//        //The below is obtained from ModelSerializer.writeModel() in dl4j package but is much simpler
//        MultiLayerNetwork bestModel = null;
//		try {
//			bestModel = modelSaver.getBestModel();
//		} catch (IOException e) {
//			// should never get here with memory model saver
//			e.printStackTrace();
//		}
//        if (bestModel == null)
//        	bestModel = model;
//        String modelConfig =  bestModel.getLayerWiseConfigurations().toJson();
//        INDArray modelParams =  bestModel.params();
//
//		if (VERBOSE) {
//			double totalTrainingSeconds = (System.currentTimeMillis() - startTrainingTime) / 1000.0;
//			AISPLogger.logger.info("*** Total training time " + totalTrainingSeconds + " seconds"); 
//		}
//
//		return new FixedCNNClassifier(primaryTrainingLabel, featureGramDescriptors, batchSize, outputNum, numToStrLabelMap, 
//				numSamplesPerWindow, numSubWindow, nChannels, modelConfig, modelParams);
//        
//	}
	/**
	 * @param featureDataSet
	 * @return
	 * @throws AISPException 
	 */
	protected MultiLayerConfiguration createNetworkConfig(FeatureDataSetIterator featureDataSet) throws AISPException {
        outputNum = featureDataSet.getNumLabels();
		MultiLayerConfiguration.Builder builder = new NeuralNetConfiguration.Builder()

                .seed(seed)
//                .iterations(iterations)
//                .regularization(true)
//                .l2(0.001)   //beta 2
                .l2(0.001 / batchSize)     //beta 4, due to this change: https://github.com/eclipse/deeplearning4j/blob/87167e91c616584a296abe637d408a8efd9e05b7/deeplearning4j/deeplearning4j-nn/src/main/java/org/deeplearning4j/nn/conf/NeuralNetConfiguration.java#L1034-L1045
//                .learningRate(0.005)//.biasLearningRate(0.02)
                .weightInit(WeightInit.XAVIER)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
//                .updater(new Adam(0.005))  //beta 2
                .updater(new Adam(0.005 / batchSize))   //beta 4, due to this change: https://github.com/eclipse/deeplearning4j/blob/87167e91c616584a296abe637d408a8efd9e05b7/deeplearning4j/deeplearning4j-nn/src/main/java/org/deeplearning4j/nn/conf/NeuralNetConfiguration.java#L1034-L1045
                .list()
                .layer(0, new ConvolutionLayer.Builder(5, 5)
                        .stride(1, 1)
                        .nOut(24)
                        .activation(Activation.RELU)
                        .build())
                .layer(1, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(STRIDE_HEIGHT,STRIDE_WIDTH)
                        .stride(STRIDE_HEIGHT,STRIDE_WIDTH)
                        .build())
                .layer(2, new ConvolutionLayer.Builder(5, 5)
                        .stride(1, 1)
                        .nOut(48)
                        .activation(Activation.RELU)
                        .build())
                .layer(3, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(STRIDE_HEIGHT,STRIDE_WIDTH)
                        .stride(STRIDE_HEIGHT,STRIDE_WIDTH)
                        .build())
                .layer(4, new ConvolutionLayer.Builder(5, 5)
                        .stride(1, 1)
                        .nOut(48)
                        .activation(Activation.RELU)
                        .build())
//                .layer(5, new DropoutLayer.Builder().dropOut(0.5).activation(Activation.IDENTITY)
//                        .nOut(64).build())
                .layer(5, new DenseLayer.Builder().activation(Activation.RELU)
                        .nOut(64).build())
//                .layer(7, new DropoutLayer.Builder().dropOut(0.5).activation(Activation.IDENTITY)
//                        .nOut(64).build())
                .layer(6, new DenseLayer.Builder().activation(Activation.SIGMOID)
                        .nOut(64).build())
                .layer(7, new OutputLayer.Builder(LossFunctions.LossFunction.RECONSTRUCTION_CROSSENTROPY)
                        .nOut(outputNum)
                        .activation(Activation.SOFTMAX)
                        .build())
                .setInputType(InputType.convolutional(featureDataSet.getNumSubWindow(), featureDataSet.getFeatureLength(), nChannels))
//                .backprop(true).pretrain(false)		// beta2
                .backpropType(BackpropType.Standard)	// beta4
                .validateOutputLayerConfig(false)  //beta4, so that an error is not thrown even if there is only one class
                ;
		MultiLayerConfiguration conf;
        try {
        	conf = builder.build();
        } catch (Exception e) {
        	throw new AISPException("Error building network. Try more sub-windows or smaller stride sizes, currently (" 
        				+ STRIDE_HEIGHT + "," + STRIDE_WIDTH + ").  Configure with " + STRIDE_HEIGHT_PROPERTY_NAME + " and " + STRIDE_WIDTH_PROPERTY_NAME + " properties.", e);
        }
		return conf;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return "CNNClassifier [batchSize=" + batchSize + ", nChannels=" + nChannels + ", outputNum=" + outputNum
				+ ", nEpochs=" + nEpochs + ", seed=" + seed 
				+ ", primaryTrainingLabel=" + primaryTrainingLabel
				+ ", trainingWindowTransform=" + this.trainingWindowTransform 
				+ ", featureGramDescriptors="
				+ (featureGramDescriptors != null ? toString(featureGramDescriptors, maxLen) : null) + ", useMemoryCache="
				+ useMemoryCache + ", useDiskCache=" + useDiskCache + ", classifier=" + classifier + "]";
	}

	protected String toString(Collection<?> collection, int maxLen) {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		int i = 0;
		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
			if (i > 0)
				builder.append(", ");
			builder.append(iterator.next());
		}
		builder.append("]");
		return builder.toString();
	}





}
