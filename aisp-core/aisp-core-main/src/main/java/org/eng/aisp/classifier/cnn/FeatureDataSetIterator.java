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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLogger;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.pipeline.LabeledFeatureIterable;
import org.eng.util.AbstractDefaultIterator;
import org.nd4j.common.util.ArrayUtil;
import org.nd4j.linalg.api.ndarray.INDArray;
//import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

/**
 * A DataSetIterator for use with deeplearning4j pipeline
 * @author wangshiq
 *
 */
public class FeatureDataSetIterator extends AbstractDefaultIterator<DataSet> implements DataSetIterator {
	private final int batchSize;
	private final String primaryTrainingLabel;
	private final Iterable<? extends ILabeledFeatureGram<double[]>[]> labeledFeatures;
	private Iterator<? extends ILabeledFeatureGram<double[]>[]> labeledFeaturesIt; //This is the active iterator being used
	private final Map<String,Integer> strToNumLabelMap;
	private final Map<Integer,String> numToStrLabelMap;
	private final int featureLength;
	private final int numLabels;
	private final int nChannels;
	private int cursor;
	private final int numSubWindow;  //Number of sub-windows in each sound clip

	/**
	 * Constructor for FeatureDataSetIterator.
	 * @param features
	 * @param batchSize
	 * @param primaryTrainingLabel
	 * @param featureLength feature length of a feature computed on a sub-window. If null, the value is determined from the first sound clip. 
	 *        The value should not be null when using the iterator for classification, to avoid errors caused by datasize mismatch.
	 * @param numSubWindow Number of subwindows. If null, the value is determined from the first sound clip.
	 *        The value should not be null when using the iterator for classification, to avoid errors caused by datasize mismatch.
	 * @throws AISPException
	 */
	public FeatureDataSetIterator(Iterable<? extends ILabeledFeatureGram<double[]>[]> features, int batchSize, 
			String primaryTrainingLabel, Integer featureLength, Integer numSubWindow, int nChannels) throws AISPException {
		this.batchSize = batchSize;
		this.labeledFeatures = features;
		this.primaryTrainingLabel = primaryTrainingLabel;
		this.labeledFeaturesIt = features.iterator();
		this.nChannels = nChannels;
		cursor=0;
		
		
		//Get label list and convert to one-hot encoding
		List<String> labelValues; 
    	if (features instanceof LabeledFeatureIterable) {	// TODO: this is often an IthIterable and is not optimized in this case. 
    		try {
				labelValues = ((LabeledFeatureIterable)features).getAllLabelValues(this.primaryTrainingLabel);
			} catch (IOException e) {
				throw new AISPException("Could not read label values", e);
			}
    	} else {
    		Set<String> tmp = new HashSet<String>();
    		for (ILabeledFeatureGram<double[]>[] fe : features) {
    			String labelValue = fe[0].getLabels().getProperty(this.primaryTrainingLabel);
    			tmp.add(labelValue);
    		}
    		labelValues = new ArrayList<String>();
    		labelValues.addAll(tmp);
    	}
    		
		if (labelValues.size() == 0) 
			throw new IllegalArgumentException("Features appear to be unlabeled.");
		
		
		ILabeledFeatureGram<double[]>[] lfgArray= features.iterator().next();
		if (lfgArray.length > 1)
			throw new IllegalArgumentException("Only a single feature gram is currently supported");
		
		if (numSubWindow != null) {
			this.numSubWindow = numSubWindow;
		} else {
			this.numSubWindow = lfgArray[0].getFeatureGram().getFeatures().length;  // TODO: Fix this. Assuming all sound clips have equal size
		}
		
		if (featureLength != null) {
			this.featureLength = featureLength;
		} else {
			IFeature<double[]> feTmp = lfgArray[0].getFeatureGram().getFeatures()[0];
			this.featureLength = feTmp.getData().length;
		}
		

		
    	strToNumLabelMap = new HashMap<String,Integer>();
    	numToStrLabelMap = new HashMap<Integer,String>();
//    	if (false) {
//    		int numLabelsTmp = 0;
//			for (Properties p : prop) {
//				String labelValue = p.getProperty(primaryTrainingLabel);
//				
//				if (labelValue != null && strToNumLabelMap.get(labelValue) == null) {
//					strToNumLabelMap.put(labelValue, numLabelsTmp);
//					numToStrLabelMap.put(numLabelsTmp, labelValue);
//					numLabelsTmp++;
//				}
//			}
//			this.numLabels = numLabelsTmp;
//    	} else {
        	// Try and make the mapping of labels to label names repeatable
//        	List<String> labelValues = new ArrayList<String>();
//        	for (Properties p : prop) {
//        		String labelValue = p.getProperty(primaryTrainingLabel);
//        		if (labelValue != null && !labelValues.contains(labelValue))
//        			labelValues.add(labelValue);
//        	}
        	Collections.sort(labelValues);
        	
        	int counter = 0;
        	for (String labelValue : labelValues) {
        		if (strToNumLabelMap.get(labelValue) == null) {
        			// Make the integer reproducible across different invocations of this constructor.
        			// This is important when defining separate test and training data sets.
        			int hash = counter++; // labelValue.hashCode();	
//        			while (numToStrLabelMap.get(hash) != null)	// Avoid collisions
//        				hash++;
        			strToNumLabelMap.put(labelValue, hash);
        			numToStrLabelMap.put(hash, labelValue);
        		}
        	}
        	this.numLabels = labelValues.size();
//    	}
    	
	}
	
	public Map<String, Integer> getStrToNumLabelMap() {
		return strToNumLabelMap;
	}

	public Map<Integer, String> getNumToStrLabelMap() {
		return numToStrLabelMap;
	}

	public int getLabelIndex(String labelValue) {
		return strToNumLabelMap.get(labelValue);
	}

	public String getLabelString(int labelIndex) {
		return numToStrLabelMap.get(labelIndex);
	}

	
	
	public int getNumSubWindow() {
		return numSubWindow;
	}

	@Override
	public boolean hasNext() {
		return labeledFeaturesIt.hasNext();
	}

	@Override
	public DataSet next() {
		return next(this.batchSize);
	}

	@Override
	public DataSet next(int num) {
		List<ILabeledFeatureGram<double[]>[]> labeledFeatureGramArrayList = new ArrayList<>();
		
		
		for (int i=0; i<num; i++) {
			if (labeledFeaturesIt.hasNext()) {
				ILabeledFeatureGram<double[]>[] labeledFeatureArray = labeledFeaturesIt.next();
//				numSubWind = Math.max(numSubWind, labeledFeatureArray.length);
				labeledFeatureGramArrayList.add(labeledFeatureArray);
				
				cursor++;
			} else {
				break;
			}
		}
		
		double[][][][] featuresPrim = new double[labeledFeatureGramArrayList.size()][nChannels][numSubWindow][featureLength];  //NCHW
		double[][] labelsPrim = new double[labeledFeatureGramArrayList.size()][numLabels];
		
		for (int index = 0; index < labeledFeatureGramArrayList.size(); index++) {
			ILabeledFeatureGram<double[]>[] lfgArray = labeledFeatureGramArrayList.get(index);
			if (lfgArray.length > 1)
				throw new IllegalArgumentException("Only a single feature gram is currently supported");
		
			ILabeledFeatureGram<double[]> lfg = lfgArray[0];
			IFeature<double[]>[] lfea = lfg.getFeatureGram().getFeatures();
			for (int j=0; j<numSubWindow; j++) {
				
				int feIndex;
				if (j < lfea.length) {
					feIndex = j;
				} else {
					// Handle windows that are shorter than our training data by padding them with the last feature.
					// The alternative is to leave them filled with 0s, but that fails our testMixedDurations() test.
					// This seems like a better choice - dawood 1/2022
					feIndex = lfea.length - 1;
					// Another alternative would be to duplicate multiple features starting at the beginning
					// feIndex = j % lfea.length;
				}
				double[] feSubWin = lfea[feIndex].getData();
				for (int k = 0; k < featureLength; k++) {
					if (k < feSubWin.length) {
						featuresPrim[index][0][j][k] = feSubWin[k];
					} else {
						featuresPrim[index][0][j][k] = 0.0;
					}
				}
			}
			
			for (int ch=1; ch<nChannels; ch++) {
				AISPLogger.logger.warning("Currently only supports a single channel, filling channel " + ch + " with 0.0."); //TODO Change later
				Arrays.fill(featuresPrim[index][ch], 0.0);
			}
			
			
			String labVal = lfg.getLabels().getProperty(primaryTrainingLabel);
			int labelIndex = -1;
			if (labVal != null) labelIndex = getLabelIndex(labVal);
			for (int i=0; i<numLabels; i++) {
				if (labVal != null && i==labelIndex) {
					labelsPrim[index][i] = 1.0;
				} else {
					labelsPrim[index][i] = 0.0;
				}
			}
		}
		
		//Creating NDArray following the instructions at http://nd4j.org/userguide#createfromjava
		double[] featuresPrimFlat = ArrayUtil.flattenDoubleArray(featuresPrim);
		INDArray features = Nd4j.create(featuresPrimFlat, 
				new int[] {labeledFeatureGramArrayList.size(), nChannels, numSubWindow, featureLength}, 'c');
		INDArray labels = Nd4j.create(labelsPrim);


		return new DataSet(features, labels);
		
	}

//	@Override
//	public int totalExamples() {
//		//TODO Perhaps a better way for this
//		
//    	int ret;
//    	if (labeledFeatures instanceof ISizedIterable) {
//    		ret = ((ISizedIterable)labeledFeatures).size();
//    	} else {
//    		int i=0;
//    		for (ILabeledFeatureGram<double[]>[] fe : labeledFeatures) {
//    			i++;
//    		}
//    		ret = i;
//    	}
//		return ret;
//		
//	}

	@Override
	public int inputColumns() {
		return this.featureLength;
	}

	@Override
	public int totalOutcomes() {
		return strToNumLabelMap.size();
	}

	@Override
	public boolean resetSupported() {
		return true;
	}

	@Override
	public void reset() {
		this.labeledFeaturesIt = labeledFeatures.iterator();
		cursor=0;
		
	}

	@Override
	public int batch() {
		return batchSize;
	}


	@Override
	public void setPreProcessor(DataSetPreProcessor preProcessor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public DataSetPreProcessor getPreProcessor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getLabels() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getFeatureLength() {
		return featureLength;
	}

	public int getNumLabels() {
		return numLabels;
	}

	@Override
	public boolean asyncSupported() {
		return false;
	}


}
