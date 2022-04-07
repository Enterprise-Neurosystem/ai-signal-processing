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
package org.eng.aisp.classifier.dcase;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ActivationLayer;
import org.deeplearning4j.nn.conf.layers.BatchNormalization;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.GlobalPoolingLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.PoolingType;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLogger;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.cnn.CNNClassifier;
import org.eng.aisp.classifier.cnn.FeatureDataSetIterator;
import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.MFFBFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.transform.ITrainingWindowTransform;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;


public class DCASEClassifier extends CNNClassifier implements IClassifier<double[]> {

	public static final long serialVersionUID = -5796140631934160535L;

	// default values for DCASEClassifier
	public static final ITrainingWindowTransform<double[]>   DEFAULT_TRANSFORMS        = null;
	public final static IFeatureExtractor<double[],double[]> DEFAULT_FEATURE_EXTRACTOR = new MFFBFeatureExtractor(128, false, true);
	public final static IFeatureProcessor<double[]>          DEFAULT_FEATURE_PROCESSOR = null;
	public final static int     DEFAULT_WINDOW_SIZE_MSEC  = 46;
	public final static int     DEFAULT_WINDOW_SHIFT_MSEC = 12;
	public static final boolean DEFAULT_USE_DISK_CACHE    = false;
	public static final boolean DEFAULT_USE_MEM_CACHE    = false;
	public final static int     DEFAULT_NUM_EPOCH         = 200;
//	public final static int     DEFAULT_BATCH_SIZE        = 32;

	public DCASEClassifier() {
		this(DEFAULT_NUM_EPOCH);
	}

	public DCASEClassifier(int nEpochs) {
		this(DEFAULT_TRANSFORMS, 
				new FeatureGramDescriptor<>(DEFAULT_WINDOW_SIZE_MSEC, DEFAULT_WINDOW_SHIFT_MSEC,DEFAULT_FEATURE_EXTRACTOR,DEFAULT_FEATURE_PROCESSOR),
				DEFAULT_USE_MEM_CACHE, DEFAULT_USE_DISK_CACHE, 
				nEpochs, CNNClassifier.DEFAULT_BATCH_SIZE, CNNClassifier.DEFAULT_TRAINING_FOLDS, CNNClassifier.DEFAULT_EPOCH_SCORE_HISTORY_SIZE, CNNClassifier.DEFAULT_MIN_SCORE_CHANGE_PER_EPOCH);
	}

//	public DCASEClassifier(int windowSizeMsec, int windowShiftMsec, int nEpochs) {
//		this(windowSizeMsec, windowShiftMsec, DEFAULT_USE_DISK_CACHE, nEpochs);
//	}
//
//	public DCASEClassifier(int windowSizeMsec, int windowShiftMsec, boolean useDiskCache, int nEpochs) {
//		this(DEFAULT_TRANSFORMS, DEFAULT_FEATURE_EXTRACTOR, DEFAULT_FEATURE_PROCESSOR,
//			 windowSizeMsec, windowShiftMsec, useDiskCache, nEpochs);
//	}
//
//	public DCASEClassifier(ITrainingWindowTransform<double[]> transforms,
//			IFeatureExtractor<double[], double[]> extractor, IFeatureProcessor<double[]> featureProcessor,
//			int windowSizeMsec, int windowShiftMsec, boolean useDiskCache, int nEpochs) {
//		this(transforms, extractor, featureProcessor, windowSizeMsec, windowShiftMsec, DEFAULT_USE_MEM_CACHE, useDiskCache, nEpochs, DEFAULT_BATCH_SIZE, CNNClassifier.DEFAULT_TRAINING_FOLDS);
//	}
//
//	public DCASEClassifier(ITrainingWindowTransform<double[]> transforms,
//			IFeatureExtractor<double[], double[]> extractor, IFeatureProcessor<double[]> featureProcessor,
//			int windowSizeMsec, int windowShiftMsec, boolean useDiskCache, int nEpochs, int nBatchSize) {
//		this(transforms, extractor, featureProcessor, windowSizeMsec, windowShiftMsec, DEFAULT_USE_MEM_CACHE, useDiskCache, nEpochs, nBatchSize, CNNClassifier.DEFAULT_TRAINING_FOLDS);
//	}
//
//	public DCASEClassifier(ITrainingWindowTransform<double[]> transforms,
//			IFeatureExtractor<double[], double[]> extractor, IFeatureProcessor<double[]> featureProcessor,
//			int windowSizeMsec, int windowShiftMsec, boolean useMemCache, boolean useDiskCache, int nEpochs) {
//		this(transforms, extractor, featureProcessor, windowSizeMsec, windowShiftMsec, useMemCache, useDiskCache, nEpochs, DEFAULT_BATCH_SIZE, CNNClassifier.DEFAULT_TRAINING_FOLDS);
//	}
//
//
//	public DCASEClassifier(ITrainingWindowTransform<double[]> transforms,
//			IFeatureExtractor<double[], double[]> extractor, IFeatureProcessor<double[]> featureProcessor,
//			int windowSizeMsec, int windowShiftMsec, boolean useMemCache, boolean useDiskCache, int nEpochs, int nBatchSize, int trainingFolds) {
////		super(transforms, extractor, featureProcessor, windowSizeMsec, windowShiftMsec, useMemCache, useDiskCache, nEpochs, nBatchSize, trainingFolds);
//		super(transforms, new FeatureGramDescriptor<>(windowSizeMsec, windowShiftMsec, extractor, featureProcessor), useMemCache, useDiskCache, nEpochs, nBatchSize, trainingFolds);
//	}
//	
	public DCASEClassifier(ITrainingWindowTransform<double[]> transform, 	IFeatureGramDescriptor<double[], double[]> fge,
			boolean useMemCache, boolean useDiskCache, int nEpochs, int batchSize, int trainingFolds, int epochScoreHistorySize, double minScoreChangePerEpoch) {
		super(transform, fge, useMemCache, useDiskCache, nEpochs, batchSize, trainingFolds, epochScoreHistorySize, minScoreChangePerEpoch);
	}

	/* (non-Javadoc)
	 * @see org.eng.aisp.classifier.cnn.CNNClassifier#createNetworkConfig(org.eng.aisp.classifier.cnn.FeatureDataSetIterator)
	 */
	@Override
	protected MultiLayerConfiguration createNetworkConfig(FeatureDataSetIterator featureDataSet) throws AISPException {
		outputNum = featureDataSet.getNumLabels();

		int poolSize;

		int featureLen = featureDataSet.getFeatureLength();

		if (featureLen < 2) {
			throw new AISPException("Unsupported band number of extracted features: featureDataSet.getNumSamplesPerWindow = " + featureLen);
		} else if (featureLen <= 16) {
			poolSize = 2;
		} else if (featureLen <= 64) {
			poolSize = 4;
		} else if (featureLen <= 128) {
			poolSize = 8;
		} else if (featureLen <= 512) {
			poolSize = 16;
		} else if (featureLen <= 2048) {
			poolSize = 32;
		} else {
			poolSize = 64;
		}

		int secondKernelSize = featureLen / poolSize;
		// System.out.printf("featureLen = %d, poolSize = %d, secondKernelSize = %d\n", featureLen, poolSize, secondKernelSize);

		MultiLayerConfiguration.Builder builder = new NeuralNetConfiguration.Builder()
			.cudnnAlgoMode(ConvolutionLayer.AlgoMode.NO_WORKSPACE)
			.seed(seed)
			.weightInit(WeightInit.XAVIER)
			.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
			.updater(new Adam(0.001, 0.5, 0.999, 1e-8))
			.list()
			.layer(0, new ConvolutionLayer.Builder()
			       .kernelSize(1, 7)
			       .stride(1, 1)
			       .convolutionMode(ConvolutionMode.Same)
			       .nOut(64)
			       .build())
			.layer(1, new BatchNormalization.Builder()
			       .nOut(64)
			       .build())
			.layer(2, new ActivationLayer.Builder()
			       .activation(Activation.RELU)
			       .build())
			.layer(3, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
			       .kernelSize(1, poolSize)
			       .stride(1, poolSize)
			       .build())
			.layer(4, new ConvolutionLayer.Builder()
			       .kernelSize(1, secondKernelSize)
			       .stride(1, 1)
			       .padding(0, 0)
			       .convolutionMode(ConvolutionMode.Truncate)
			       .nOut(128)
			       // .dropOut(0.8) // 1 - p => 0 / p => x (inverted) // NG
			       .build())
			.layer(5, new BatchNormalization.Builder()
			       .nOut(128)
			       .build())
			.layer(6, new ActivationLayer.Builder()
			       .activation(Activation.RELU)
			       .build())
			.layer(7, new ConvolutionLayer.Builder()
			       .kernelSize(7, 1)
			       .stride(1, 1)
			       .convolutionMode(ConvolutionMode.Same)
			       .nOut(256)
			       .build())
			.layer(8, new BatchNormalization.Builder()
			       .nOut(256)
			       .build())
			.layer(9, new ActivationLayer.Builder()
			       .activation(Activation.RELU)
			       .build())
			.layer(10, new GlobalPoolingLayer.Builder()
			       .poolingType(PoolingType.MAX)
			       .build())
			.layer(11, new DenseLayer.Builder()
			       .nOut(128)
			       .dropOut(0.5) // 1 - p => 0 / p => x (inverted)
			       .build())
			.layer(12, new OutputLayer.Builder()
			       .lossFunction(LossFunctions.LossFunction.RECONSTRUCTION_CROSSENTROPY)
			       .activation(Activation.SOFTMAX)
			       .nOut(outputNum)
			       .build())
			.setInputType(InputType.convolutional(featureDataSet.getNumSubWindow(), featureDataSet.getFeatureLength(), nChannels))
//			.backprop(true).pretrain(false)	// beta2
			.backpropType(BackpropType.Standard)	// beta4
			.validateOutputLayerConfig(false) //beta4, so that an error is not thrown even if there is only one class
			;	

  		if (VERBOSE)
  			AISPLogger.logger.info("Feature-gram(s): subWindows/clip=" + featureDataSet.getNumSubWindow() + ", featureLength="  +  featureDataSet.getFeatureLength() + ", featureGrams=" +  nChannels );
		MultiLayerConfiguration conf;
		try {
			conf = builder.build();
		} catch (Exception e) {
			throw new AISPException("Error building network. ", e);
		}
		return conf;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return "DCASEClassifier [batchSize=" + batchSize + ", nChannels=" + nChannels + ", outputNum=" + outputNum
				+ ", nEpochs=" + nEpochs + ", seed=" + seed
				+ ", primaryTrainingLabel=" + primaryTrainingLabel
				+ ", trainingWindowTransform=" + this.trainingWindowTransform
				+ ", featureGramDescriptors="
				+ (featureGramDescriptors != null ? toString(featureGramDescriptors, maxLen) : null) + ", useMemoryCache="
				+ useMemoryCache + ", useDiskCache=" + useDiskCache + ", classifier=" + classifier + "]";
	}
}
