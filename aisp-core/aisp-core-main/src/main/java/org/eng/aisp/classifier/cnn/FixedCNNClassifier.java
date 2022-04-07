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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.AbstractFixedFeatureExtractingClassifier;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.classifier.Classification.LabelValue;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.LabeledFeatureGram;
import org.eng.aisp.util.ArrayIndexComparator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixedCNNClassifier extends AbstractFixedFeatureExtractingClassifier<double[], double[]>  implements IFixedClassifier<double[]>  {

	private static final long serialVersionUID = 2966528639796200129L;
	
//	public static final ch.qos.logback.classic.Logger logbackRootLogger = org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
	
    private static final Logger log = LoggerFactory.getLogger(FixedCNNClassifier.class);
    
    

	private final String primaryTrainingLabel;
    private final int batchSize;
    private final int outputNum;
    private Map<Integer, String> numToStrLabelMap;
	private final int numSamplesPerWindow;
	private final int numSubWindow;
	private final int nChannels;
//    private final byte[] serializedModel;

    private String modelConfig;
    private INDArray modelParams;
    
    
	public FixedCNNClassifier(String primaryTrainingLabel, List<IFeatureGramDescriptor<double[],double[]>> fgeList, 
			int batchSize, int outputNum, Map<Integer, String> numToStrLabelMap, 
			int numSamplesPerWindow, int numSubWindow, int nChannels, String modelConfig, INDArray modelParams) {
		super(fgeList);
		this.primaryTrainingLabel = primaryTrainingLabel;
		this.batchSize = batchSize;
		this.outputNum = outputNum;
		this.numToStrLabelMap = numToStrLabelMap;
		this.numSamplesPerWindow = numSamplesPerWindow;
		this.numSubWindow = numSubWindow;
		this.nChannels = nChannels;
//		this.serializedModel = serializedModel;
		this.modelConfig = modelConfig;
		this.modelParams = modelParams;
		
//		Nd4j.ENFORCE_NUMERICAL_STABILITY = true;  // beta2 This is for avoiding NaN values
//		Nd4j.ENFORCE_NUMERICAL_STABILITY = true;  // beta4  constant not available
		
	}

	private transient MultiLayerNetwork model = null;
//	private transient ParallelInference  pInferencer= null;
	private transient Object lock = null; 
	
	@Override
	protected List<Classification> classify(IFeatureGram<double[]>[] featureGrams) throws AISPException {
		
		ILabeledFeatureGram<double[]>[] lfgArray = new ILabeledFeatureGram[featureGrams.length];
		model = initForClassification();
		
		// Create LabeldFeatureGrams with empty labels so we can use FeatureDataSetIterator below.
		Properties p = new Properties();
		for (int i=0; i<featureGrams.length ; i++) {
			IFeatureGram<double[]> fg = featureGrams[i];
			ILabeledFeatureGram<double[]> lfg = new LabeledFeatureGram<double[]>(fg, p); 
			lfgArray[i] = lfg;
		}
		List<ILabeledFeatureGram<double[]>[]> labeledFeatureGrams = new ArrayList<>(); 
		labeledFeatureGrams.add(lfgArray);
        FeatureDataSetIterator featureDataSet = new FeatureDataSetIterator(labeledFeatureGrams, batchSize, primaryTrainingLabel, 
        		numSamplesPerWindow, numSubWindow, nChannels);

        //Load serialized model
//        MultiLayerNetwork model = null;
//        ByteArrayInputStream bis = new ByteArrayInputStream(serializedModel);
//        try {
//        	model = ModelSerializer.restoreMultiLayerNetwork(bis, false);
//		} catch (IOException e) {
//			throw new AISPException(e.getMessage());
//		}
        
        //The below is obtained from ModelSerializer.restoreMultiLayerNetwork() in dl4j package but is much simpler and does not create temp files
		List<Classification> classifications = new ArrayList<Classification>();

        Double[] outputArray = new Double[outputNum];
        for (int i=0; i<outputArray.length; i++) {
        	outputArray[i] = 0.0;
        }
        while (featureDataSet.hasNext()){
            INDArray output = null;
            DataSet ds = featureDataSet.next();
            INDArray matrix = ds.getFeatures();
            synchronized (lock) {	// Using ParallelInferencer does not pass the AbstractClassifierTest.testParallelClassifyUsingConfusionMatrix. (dl4j beta7, dawood 6/2021)
              output = model.output(matrix, false);
//            output = pInferencer.output(matrix);
            }
        
	        for (int i=0; i<outputArray.length; i++) {
	        	for (int j=0; j<output.rows(); j++) {
	        		outputArray[i] += output.getDouble(j,i);
	        	}
	        	outputArray[i] /= output.rows();
	        }
        }
        
		ArrayIndexComparator comparator = new ArrayIndexComparator(outputArray);
		Integer[] indexes = comparator.createIndexArray();   //this contains indexes of sorted distances
		Arrays.sort(indexes, comparator);
		
		

		List<LabelValue> rankedValues = new ArrayList<LabelValue>();
		
		for (int i=indexes.length-1; i>=0; i--) {
			rankedValues.add(new LabelValue(numToStrLabelMap.get(indexes[i]), outputArray[indexes[i]]));
		}

		classifications.add(new Classification(primaryTrainingLabel, rankedValues));	
		
		return classifications;
	}



	private synchronized MultiLayerNetwork initForClassification() {
        if (model == null) {
			MultiLayerConfiguration confFromJson = MultiLayerConfiguration.fromJson(modelConfig);
			model = new MultiLayerNetwork(confFromJson);
			model.init(modelParams, false);
			lock = new Object();
        }
        return model;
	}



	@Override
	public String getTrainedLabel() {
		return this.primaryTrainingLabel;
	}



	@Override
	protected void finalize() throws Throwable {
//		if (this.pInferencer != null)
//			this.pInferencer.shutdown();
		super.finalize();
	}

	
}
