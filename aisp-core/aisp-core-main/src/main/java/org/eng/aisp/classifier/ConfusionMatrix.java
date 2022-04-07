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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.eng.aisp.AISPException;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.util.BinaryConfusionMatrix;
import org.eng.util.ExecutorUtil;
import org.eng.util.OnlineStats;

/**
 * Generates a Confusion Matrix for two lists of labels.
 * It does so by comparing index X to index Y and seeing if they are the same.
 * Main Data Structure is a HashMap of HashMaps. RealLabel->PredicteLabel->Count.
 *
 * @author Eduardo Morales
 *
 */
public class ConfusionMatrix {
	
    private Map<String, Map<String, Integer>> countsMap= new HashMap<String, Map<String,Integer>>();
    /** Computed on demand via {@link #getPercents()}, so be careful when accessing directly */
    private transient Map<String, Map<String, Double>> allPercentsMap = null; // new HashMap<String, Map<String,Double>>();
    private transient Map<String, Map<String, Double>> recallPercentsMap = null; // new HashMap<String, Map<String,Double>>();
    private final int totalSamples;
    private final String labelName;
//    private OnlineStats recall;		// Set during construction.
//    private OnlineStats precision;	// Set during construction.
//	private OnlineStats f1Score;

	private AccuracyStats microAccuracy;
	private AccuracyStats macroAccuracy;
	private int truePositives;

	/**
	 * Holds a single set of accuracy values.
	 * Design to hold either micro or macro-averaged values.
	 * @author dawood
	 *
	 */
	private static class AccuracyStats {
		public final OnlineStats accuracy;
		public final OnlineStats precision;
		public final OnlineStats recall;
		public final OnlineStats f1Score;

		public AccuracyStats(OnlineStats accuracy, OnlineStats precision, OnlineStats recall, OnlineStats f1Score) {
			this.accuracy = accuracy;
			this.precision = precision;
			this.recall = recall;
			this.f1Score = f1Score;
		}
		
		public AccuracyStats combine(AccuracyStats stats) {
			OnlineStats newAccuracy = this.accuracy.combine(stats.accuracy);
			OnlineStats newPrecision = this.precision.combine(stats.precision);
			OnlineStats newRecall = this.recall.combine(stats.recall);
			OnlineStats newF1Score = this.f1Score.combine(stats.f1Score);
			return new AccuracyStats(newAccuracy, newPrecision, newRecall, newF1Score);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((accuracy == null) ? 0 : accuracy.hashCode());
			result = prime * result + ((f1Score == null) ? 0 : f1Score.hashCode());
			result = prime * result + ((precision == null) ? 0 : precision.hashCode());
			result = prime * result + ((recall == null) ? 0 : recall.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof AccuracyStats))
				return false;
			AccuracyStats other = (AccuracyStats) obj;
			if (accuracy == null) {
				if (other.accuracy != null)
					return false;
			} else if (!accuracy.equals(other.accuracy))
				return false;
			if (f1Score == null) {
				if (other.f1Score != null)
					return false;
			} else if (!f1Score.equals(other.f1Score))
				return false;
			if (precision == null) {
				if (other.precision != null)
					return false;
			} else if (!precision.equals(other.precision))
				return false;
			if (recall == null) {
				if (other.recall != null)
					return false;
			} else if (!recall.equals(other.recall))
				return false;
			return true;
		}


		@Override
		public String toString() {
			return "AccuracyStats [accuracy=" + accuracy + ", precision=" + precision + ", recall=" + recall
					+ ", f1Score=" + f1Score + "]";
		}
	}

	/**
     * Get a sorted list of the real labels.
     * @return realLabels
     */
    public List<String> getRealLabels(){
        return getSortedRealLabels(countsMap); 
    }


    /**
     * Get a sorted list of the predictedLabels
     * @return predictedLabels
     */
    public List<String> getPredictedLabels(){
        return getSortedPredictedLabels(countsMap); 
    }

	
//    /**
//     * Getter for overall accuracy of the matrix
//     * @return never null.  We return OnlineStats instead of double to capture standard deviations of accuracies when {@link #add(ConfusionMatrix)} is used
//     * to incorporate multiple test data sets, particularly for k-fold evaluations.
//     */
//    public OnlineStats getAccuracy() {
//    	return this.accuracy;
//   }

    /**
     * Get the count of predicted labels for each actual label vs the predicted label. 
     * A map of true classification labels is used as the keys in the returned map.
     * Each value of a key in the returned map is a map with keys as the predicted values.
     * The value under the predicted value keys is the number
     * of times a datum was classified as the predicted value.
     * @return never null.
     */
    public Map<String, Map<String, Integer>> getCounts() {
        return countsMap;
    }
    
    /**
     * A convenience on {@link #getPercents(Normalization)} using {@link Normalization#notifyAll()} normalization.
     */
    public Map<String, Map<String, Double>> getPercents() {
    	return this.getPercents(Normalization.All);
    }

    /**
     * Defines the way the percentages are calculated in the Percents confusion matrix via #get 
     * @author 285782897
     *
     */
    public enum Normalization {
    	/**
    	 * Normalization with the row (recall) values. Percentages across a row add to 100.
    	 */
    	Recall,
    	/**
    	 * Normalization with all values in the matrix. Percentages across the whole matrix add to 100. 
    	 */
    	All
    }

    /**
     * Get the percentage of predicted labels for each actual label vs the predicted label. 
     * A map of true classification labels is used as the keys in the returned map.
     * Each value of a key in the returned map is a map with the predicted values as keys.
     * The value under the predicted value keys is the percentage of all values predicted according to the normalization type. 
     * (i.e. this is normalized across the whole matrix so that the sum of values in all cells is 100). 
     * @param normalization if All, then percentages across the whole matrix sum to 100.  if Recall, then all values within
     * a row (actual values) sum to 100.  Precision is not yet supported.
     * @return never null.
     */
    public Map<String, Map<String, Double>> getPercents(Normalization normalization) {
    	Map<String, Map<String, Double>> percentMap;
    	// See if we've already computed it.
    	switch (normalization) {
    		case All:  percentMap = this.allPercentsMap; break;
    		case Recall:  percentMap = this.recallPercentsMap; break;
    		default: throw new RuntimeException("Unexpected normalization " + normalization.toString());
    	}
    	// Compute it since haven't done so before.
    	if (percentMap == null) {
    		percentMap = new HashMap<String, Map<String,Double>>();
        	for (String realLabel : countsMap.keySet()){
            	Map<String, Integer> countsMap = this.countsMap.get(realLabel);
            	int rowCount = getRowCount(realLabel);
        		for (String predictedLabel : countsMap.keySet()){
        			double num = countsMap.get(predictedLabel);
        			//correctlyLabeled/numberofRealLabels to calculate percentage
        			double v;
        			if (normalization == Normalization.All) {
        				v = num / this.totalSamples;
        			} else if (normalization == Normalization.Recall) {
        				if (rowCount == 0)
        					v = Double.NaN;
        				else
        					v = num / rowCount; 
        			} else {
        				v = 0;	// Should never get here, but quiets the compiler/ide.
        			}
        	        setCell(percentMap, realLabel, predictedLabel, 100 * v); 
        		}
        	}
    	}
    	// Store the cached value.
    	switch (normalization) {
    		case All:  allPercentsMap = percentMap; break;
    		case Recall:  recallPercentsMap = percentMap; break;
    		default: throw new RuntimeException("Unexpected normalization " + normalization.toString());
    	}
        return percentMap;
    }


    /**
     * Getter for the total amount of labels in the ConfusionMatrix
     * @return totalLabels
     */
    protected int getTotalLabels() {
        return totalSamples;
    }


    /**
     * Getter for the labelName that the classifier will use
     * @return labelName
     */
    public String getLabelName() {
        return labelName;
    }

    
    /**
     * Get the weighted average  (i.e. micro) recall across all labels. 
     * @return recall
     */
    public OnlineStats getRecall(){
//    	return this.microAccuracy.recall;
    	return this.getRecall(true);
    }

    /**
     * Get the micro or macro-averaged recall across all label values in the matrix.
     * @param asMicro if true, then get the micro-averaged recall value, otherwise the macro-averaged value.
     * @return
     */
    public OnlineStats getRecall(boolean asMicro){
    	return asMicro ? this.microAccuracy.recall : this.macroAccuracy.recall;
    }
    
    /**
     * Get the weighted average (i.e. micro) precision across all labels. 
     * @return precision
     */
    public OnlineStats getPrecision(){
//    	return this.microAccuracy.precision;
    	return this.getPrecision(true);
    }

    /**
     * Get the micro or macro-averaged precision across all label values in the matrix.
     * @param asMicro if true, then get the micro-averaged precision value, otherwise the macro-averaged value.
     * @return
     */
    public OnlineStats getPrecision(boolean asMicro){
    	return asMicro ? this.microAccuracy.precision : this.macroAccuracy.precision;
    }

    /**
     * Get the weighted average (i.e. micro) f1 score across all labels. 
     * @return f1Score 
     */
    public OnlineStats getF1Score(){
//    	return this.microAccuracy.f1Score;
    	return this.getF1Score(true);
    }
    
    /**
     * Get the micro or macro-averaged f1 score across all label values in the matrix.
     * @param asMicro if true, then get the micro-averaged f1 score, otherwise the macro-averaged value.
     * @return
     */
    public OnlineStats getF1Score(boolean asMicro){
    	return asMicro ? this.microAccuracy.f1Score: this.macroAccuracy.f1Score;
    }
    /**
     * Given two list of labels of the same length, compute ConfusionMatrix
     * @param realLabels
     * @param predictedLabels
     */
    public ConfusionMatrix(String trainingLabel, List<String> realLabels, List<String> predictedLabels){
    	this.labelName = trainingLabel;
//    	this.realLabels = realLabels;
//    	this.predictedLabels = predictedLabels;
    	//If lists are not the same length, throw Exception
    	if (realLabels.size() != predictedLabels.size())
        	throw new IllegalArgumentException("Error: Both label lists must be of the same length");

    	//If any list is empty, throw Exception
        if (realLabels.isEmpty() || predictedLabels.isEmpty())
            throw new IllegalArgumentException("Error: Label list cannot be empty.");
        
    	//Generate the list of labels without duplicates
        //Remove duplicates by converting to a HashSet
        Set<String> realLabelsSet= new HashSet<String>(realLabels);
        Set<String> prediectedLabelsSet= new HashSet<String>(predictedLabels);

        //Set the lists to the new non-duplicate label names -- header names
//        this.noDupRealLabels = new ArrayList<String>(realLabelsSet);
//        Collections.sort(noDupRealLabels);
//        this.noDupPredictedLabels = new ArrayList<String>(predictedLabelsSet);
//        Collections.sort(noDupPredictedLabels);
        
        // Build the full NxM counts matrix
//        Set<String> allLabels = new HashSet<String>();
//        allLabels.addAll(realLabelsSet);
//        allLabels.addAll(predictedLabelsSet);
        for (String realLabel : realLabelsSet) {
        	for (String predictedLabel : prediectedLabelsSet) 
        		setCell(countsMap, realLabel, predictedLabel, 0);
        }
        
        //Insert realLabel/predictedLabel to countsMap
        this.totalSamples = realLabels.size();
        this.truePositives = 0;
    	for (int i = 0; i < totalSamples; i++) {
    		String real = realLabels.get(i);
    		String predicted = predictedLabels.get(i);
    		if (real.equals(predicted))
    			this.truePositives++;
        	Integer count = getSetCell(countsMap, real, predicted);
        	setCell(countsMap, real, predicted, count+1);
    	}

        //realLabel and predictedLabel are equal, there is no confusion -- correctLabels + 1
    	
//        //Add one to the amount of total labels
//    	//Generate allPercentsMap from countsMap
//    	fillPercentsMap();
    	
    	//Generate the accuracy, precision and recall of the matrix
    	computeSummaryStatistics();
    }

    private ConfusionMatrix(String trainingLabel, Map<String, Map<String,Integer>> counts) { 
    	this.labelName = trainingLabel;

    	// Make sure we have all the predicted labels.  The matrix should be fully filled, in 
    	// which case we wouldn't need to do this, 
    	Set<String> allPredictedLabels = getPredictedLabels(counts);
    	
    	int count = 0;
    	int tp = 0;
    	for (String realLabel : counts.keySet()) {
    		for (String predictedLabel : allPredictedLabels) { 
    			Integer c = getCell(counts, realLabel, predictedLabel);
    			if (c == null)	
    				c = new Integer(0);
    			count += c;
    			setCell(this.countsMap, realLabel, predictedLabel, c);
    			if (realLabel.equals(predictedLabel))
    				tp += c;
    		}
    	}
    	this.totalSamples = count;
    	this.truePositives = tp;
    	computeSummaryStatistics();
    	

    }
   
    /**
     * Get all the column names from the given map with no duplicates.
     * @param map
     * @return
     */
    private static <CELLTYPE>  Set<String> getPredictedLabels(Map<String, Map< String, CELLTYPE>> map) {
    	Set<String> allPredictedLabels = new HashSet<String>();
    	for (String realLabel : map.keySet()) {
    		Map<String, CELLTYPE> row = map.get(realLabel);
    		allPredictedLabels.addAll(row.keySet());
    	}	
    	return allPredictedLabels;
    }
    
    private static <CELLTYPE>  List<String> getSortedPredictedLabels(Map<String, Map< String, CELLTYPE>> map) {
    	List<String> labels = new ArrayList<String>(getPredictedLabels(map));
    	Collections.sort(labels);
    	return labels;
    }

    private static <CELLTYPE>  List<String> getSortedRealLabels(Map<String, Map< String, CELLTYPE>> map) {
    	List<String> labels = new ArrayList<String>(map.keySet());
    	Collections.sort(labels);
    	return labels;
    }

	private static class GetPredictionsWorker<WINDATA> implements Callable<Object> {



		private IFixedClassifier<WINDATA> classifier;
		private Iterator<? extends ILabeledDataWindow<WINDATA>> data;
		private List<String> realLabels;
		private List<String> predictedLabels;
		private String labelName;
		private AtomicInteger dataCount;
		private AtomicInteger labeledDataCount;

		GetPredictionsWorker(IFixedClassifier<WINDATA> classifier, String labelName,
				Iterator<? extends ILabeledDataWindow<WINDATA>> iterator, List<String> realLabels,
				List<String> predictedLabels, AtomicInteger dataCount, AtomicInteger labeledDataCount) {
			this.classifier = classifier;
			this.labelName = labelName;
			this.data = iterator;
			this.realLabels = realLabels;
			this.predictedLabels = predictedLabels;
			this.dataCount = dataCount;
			this.labeledDataCount = labeledDataCount;
		}

		@Override
		public Object call() throws Exception { 
			int count = 0, labelCount = 0;
			boolean done = false;
			while (!done) {
				ILabeledDataWindow<WINDATA> sr;
				synchronized(data) {
					if (data.hasNext())
						sr = data.next();
					else
						sr = null;
				}	// synch
				if (sr == null) {
					done = true;
				} else {
					count++;
					//Get all the sound's labels and store it as a Property
					Properties labels = sr.getLabels();
					//Make sure that the label is part of the sound's labels.
					String expectedValue = labels.getProperty(labelName);
					//If it's not part of the labels, getProperty() returns Null.
					//If label is part of labels of Sr
					IDataWindow<WINDATA> clip = sr.getDataWindow();  // Raw data window without labels
					if (expectedValue != null) {
						labelCount++;
						//Classify the sound and store the
						Map<String, Classification> cmap = classifier.classify(clip);
						Classification c = cmap.get(labelName);
		                if (c == null)
		                	throw new AISPException("Model did not produce a classification containing label " + labelName);
						String classifiedValue = c.getLabelValue();
						synchronized(realLabels) {
							realLabels.add(expectedValue);
							predictedLabels.add(classifiedValue);
						}
					}
				} 
	        } // while

			this.dataCount.addAndGet(count);
//			AISPLogger.logger.info("count=" + count + ", dataCount=" + dataCount.get());
			this.labeledDataCount.addAndGet(labelCount);
			return null;
		}

	}

	/**
	 * A convenience on {@link ConfusionMatrix#compute(String, IFixedClassifier, Iterable, boolean)} that does not go parallel to compute. 
	 */
    public static <WINDATA> ConfusionMatrix compute(String labelName, IFixedClassifier<WINDATA> classifier, Iterable<? extends ILabeledDataWindow<WINDATA>> srList) throws AISPException {
    	return compute(labelName, classifier, srList, false);
    }

    /**
     * Given a classifier, a list of sounds and a labelName -- classify the sounds and compute Confusion Matrix
     * @param labelName
     * @param classifier
     * @param srList
     * @param inParallel if true, then perform classification parallel.  In this case, ordering of predicted and real labels in the resulting confusion matrix 
     * will not match the ordering of windows in the given srList.  In general, this should be set to true except when the classifier being used is not thread-safe, which
     * it should be.
     * @throws AISPException
     */
    public static <WINDATA> ConfusionMatrix compute(String labelName, IFixedClassifier<WINDATA> classifier, Iterable<? extends ILabeledDataWindow<WINDATA>> srList, boolean inParallel) throws AISPException {
    	if (inParallel)
    		return computeParallel(labelName, classifier, srList);
    	else
    		return computeSerial(labelName, classifier, srList);
    }
    
    private static <WINDATA> ConfusionMatrix computeParallel(String labelName, IFixedClassifier<WINDATA> classifier, Iterable<? extends ILabeledDataWindow<WINDATA>> srList) throws AISPException {
        List<String> tempRealLabels = new ArrayList<String>();
        List<String> tempPredictedLabels = new ArrayList<String>();
        
		ExecutorService eservice = ExecutorUtil.getPrioritizingSharedService();
		List<Future<Object>> flist = new ArrayList<Future<Object>>();
		
		AtomicInteger count = new AtomicInteger();
		AtomicInteger labeledDataCount = new AtomicInteger();
		int threads = Runtime.getRuntime().availableProcessors();
		Iterator<? extends ILabeledDataWindow<WINDATA>> srListIter = srList.iterator();
		for (int i = 0  ; i<threads ; i++) {
			GetPredictionsWorker<WINDATA> w = new GetPredictionsWorker<WINDATA>(classifier, labelName, srListIter, tempRealLabels, tempPredictedLabels, count, labeledDataCount);
			Future<Object> f = eservice.submit(w);
			flist.add(f);
		}
		// Wait for completion of all tasks.
		for (Future<Object> f : flist) {
			try {
				f.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				throw new AISPException("Error during parallel classification: " + e.getMessage(),e);
			}
		}
//		eservice.shutdown();
		
    	if (count.get() == 0)
    		throw new IllegalArgumentException("No data provided for evaluation.");
    	int dataCount = labeledDataCount.get();
    	if (dataCount  == 0)
    		throw new IllegalArgumentException("Found data, but none with the given label: " + labelName);
    	if (dataCount != tempRealLabels.size()) 
    		throw new RuntimeException("Real labels (" + tempRealLabels.size() + ") not equal to data count (" + dataCount + ")");
    	if (dataCount != tempPredictedLabels.size()) 
    		throw new RuntimeException("Predicted labels (" + tempPredictedLabels.size() + ") not equal to data count (" + dataCount + ")");
    	//Given the classifiers output, generate confusion matrix and return it
    	return new ConfusionMatrix(labelName, tempRealLabels, tempPredictedLabels);
    }
    
    private static <WINDATA> ConfusionMatrix computeSerial(String labelName, IFixedClassifier<WINDATA> classifier, Iterable<? extends ILabeledDataWindow<WINDATA>> srList) throws AISPException {
        List<String> tempRealLabels = new ArrayList<String>();
        List<String> tempPredictedLabels = new ArrayList<String>();
        int count = 0;
        int labelCount = 0;
        for (ILabeledDataWindow<WINDATA> sr : srList) {
    		count++;
            //Get all the sound's labels and store it as a Property
            Properties labels = sr.getLabels();
            //Make sure that the label is part of the sound's labels.
            String expectedValue = labels.getProperty(labelName);
            //If it's not part of the labels, getProperty() returns Null.
            //If label is part of labels of Sr
            IDataWindow<WINDATA> clip = sr.getDataWindow();  // Raw data window without labels
            if (expectedValue != null) {
            	labelCount++;
                //Classify the sound and store the
                Map<String, Classification> cmap = classifier.classify(clip);
                Classification c = cmap.get(labelName);
                if (c == null)
                	throw new AISPException("Model did not produce a classification containing label " + labelName);
                String classifiedValue = c.getLabelValue();
                tempRealLabels.add(expectedValue);
                tempPredictedLabels.add(classifiedValue);
            }
        }
    	if (count == 0)
    		throw new IllegalArgumentException("No data provided for evaluation.");
    	if (labelCount == 0)
    		throw new IllegalArgumentException("Found data, but none with the given label: " + labelName);
    	//Given the classifiers output, generate confusion matrix and return it
    	return new ConfusionMatrix(labelName, tempRealLabels, tempPredictedLabels);
    	
    	//Return the new confusion matrix
    }
    
    /**
     * Generates getErrorMatrix and and uses the data to calculate the matrix accuracy, precision and recall
     */
    private void computeSummaryStatistics(){
    	double sumWeightedAccuracy = 0.0;
    	double sumWeightedPrecision = 0.0;
    	double sumWeightedRecall = 0.0;
    	double sumWeightedF1Score = 0.0;
    	double sumAccuracy = 0.0;
    	double sumPrecision = 0.0;
    	double sumRecall = 0.0;
    	double sumF1Score = 0.0;
    	int sum = 0;
    	int count=0;
    	
    	//For every label, generate an errrorMatrix
    	for (String label : countsMap.keySet()){

    		BinaryConfusionMatrix binaryConfusionMatrix = getBinaryMatrix(label);

    		int weight = getRowCount(label);	// Weight by the number of true label values.
//    		AISPLogger.logger.info("label=" + label + ", weight=" + weight + ", r=" + errorMatrix.getRecall() + ", p=" + errorMatrix.getPrecision());
    		sumAccuracy  += binaryConfusionMatrix.getAccuracy(); 
    		sumPrecision += binaryConfusionMatrix.getPrecision(); 
    		sumRecall    += binaryConfusionMatrix.getRecall(); 
    		sumF1Score   += binaryConfusionMatrix.getF1Score(); 

    		sumWeightedAccuracy  += weight * binaryConfusionMatrix.getAccuracy(); 
    		sumWeightedPrecision += weight * binaryConfusionMatrix.getPrecision(); 
    		sumWeightedRecall    += weight * binaryConfusionMatrix.getRecall(); 
    		sumWeightedF1Score   += weight * binaryConfusionMatrix.getF1Score(); 

    		//Keep track of the sum. This is useful to weigh the precision/recall
    		sum += weight; 
    		count++;
    	}
    
		//Set this Confusion Matrix's _micro_ precision, recall f1 
		// Taken from https://towardsdatascience.com/multi-class-metrics-made-simple-part-ii-the-f1-score-ebe8b2c2ca1, where
		// f1 = recall = precision = recall = accuracy
		OnlineStats accuracy = new OnlineStats();
		double score = (double)this.truePositives / this.getTotalSamples();
//		accuracy.addSample(sumWeightedAccuracy / (double) sum);
		accuracy.addSample(score);
		OnlineStats precision = new OnlineStats();
//		precision.addSample(sumWeightedPrecision / (double) sum);
		precision.addSample(score);
		OnlineStats recall = new OnlineStats();
//		recall.addSample(sumWeightedRecall / (double) sum);
		recall.addSample(score);
		OnlineStats f1Score = new OnlineStats();
//    	f1Score.addSample((double) sumWeightedF1Score / (double) sum);
		f1Score.addSample(score);
		this.microAccuracy = new AccuracyStats(accuracy, precision,  recall, f1Score);

		//Set this Confusion Matrix's _macro_ precision, recall f1 
		accuracy = new OnlineStats();
		precision = new OnlineStats();
		recall = new OnlineStats();
		f1Score = new OnlineStats();
		if (count > 0) {
			accuracy.addSample(sumAccuracy / (double) count);
			precision.addSample(sumPrecision / (double) count);
			recall.addSample(sumRecall / (double) count);
			f1Score.addSample((double) sumF1Score / (double) count);
		}
		this.macroAccuracy = new AccuracyStats(accuracy, precision, recall, f1Score);

		
    }
  
    
    /**
     * Calls the errorMatrix's toString() method.
     * @param label
     * @return errorMatrix
     */
    public String errorMatrixToString(String label){
    	return getBinaryMatrix(label).toString();
    }

    /**
     * Converts the allPercentsMap into a 2D array for count and percentage and returns it
     * Provided primarily for junit tests.   Try not to use internally.
     * @return percentsArray
     */
    protected double[][] getPercentsArray(Normalization normalization){
    	Map<String, Map<String,Double>> percentsMap = getPercents(normalization);	
         //Generate 2D Arrays based on the length of the noDupLabel lists
    	List<String> noDupPredictedLabels = getSortedPredictedLabels(countsMap);
    	List<String> noDupRealLabels = getSortedRealLabels(countsMap);
         double percentsArray[][] = new double[noDupRealLabels.size()][noDupPredictedLabels.size()];
         for (int i=0 ; i<noDupRealLabels.size() ; i++) {
         	String realLabel = noDupRealLabels.get(i);
         	for (int j=0 ; j<noDupPredictedLabels.size() ; j++) {
         		String predictedLabel = noDupPredictedLabels.get(j);
         		percentsArray[i][j] = getSetCell(percentsMap, realLabel, predictedLabel).doubleValue(); // innerMap.getValue();
         	}
         }

         return percentsArray;
       }

    /**
     * Converts the countsMap to a 2Darray and returns it.
     * Provided primarily for junit tests.   Try not to use internally.
     * The problem with this is that the row and column associations are not stated.
     * @return countsArray
     */
    protected double[][] getCountsArray(){
        //Generate 2D Arrays based on the length of the noDupLabel lists
    	List<String> noDupRealLabels = getSortedRealLabels(countsMap);
    	List<String> noDupPredictedLabels = getSortedPredictedLabels(countsMap);
        double countsArray[][] = new double[noDupRealLabels.size()][noDupPredictedLabels.size()];
        
        for (int i=0 ; i<noDupRealLabels.size() ; i++) {
        	String realLabel = noDupRealLabels.get(i);
        	for (int j=0 ; j<noDupPredictedLabels.size() ; j++) {
        		String predictedLabel = noDupPredictedLabels.get(j);
                countsArray[i][j] = getSetCell(countsMap, realLabel, predictedLabel).doubleValue(); // innerMap.getValue();
        	}
        }
        return countsArray;
    }
    

    /**
     * Set the datum in the given cell indexed by real and predicted labels.
     * Create the row and cell if needed.
     * @param matrix one of the counts or percents maps.
     * @param realLabel
     * @param predictedLabel
     */
    private static <CELLDATA> void setCell(Map<String,Map<String,CELLDATA>> matrix, String realLabel, String predictedLabel, CELLDATA data) {
    	Map<String,CELLDATA> row = matrix.get(realLabel);
    	if (row == null) {
            row = new HashMap<String, CELLDATA>();
            matrix.put(realLabel, row);
    	}
    	row.put(predictedLabel, data);
    }

    private static <CELLDATA> CELLDATA getSetCell(Map<String,Map<String,CELLDATA>> matrix, String realLabel, String predictedLabel) {
    	CELLDATA data = getCell(matrix, realLabel, predictedLabel);
        if (data == null)
        	throw new RuntimeException("INTERNAL ERROR: Cell (" + realLabel + ", " + predictedLabel + ") should have already been set");
    	return data;
    }
    /**
     * Get the datum in the given cell indexed by real and predicted labels.
     * @param matrix one of the counts or percents maps.
     * @param realLabel
     * @param predictedLabel
     * @returna null if cell does not  yet exist.
     */
    private static <CELLDATA> CELLDATA getCell(Map<String,Map<String,CELLDATA>> matrix, String realLabel, String predictedLabel) {
    	Map<String,CELLDATA> row = matrix.get(realLabel);
    	if (row == null)
    		return null;
    	CELLDATA data = row.get(predictedLabel);
    	return data;
    }

    
//    private final static int MAX_LABEL_DISPLAY_LEN = 16;

    private static String centerAbbrevLabel(String label, int max_label_len){
    	label = generateAbbrevLabel(label,max_label_len);
    	return centerString(max_label_len, label);	
    }

    /**
     * It turns a label and pads it or substrings is to be exactly stringSize characters, this is used to make the Confusion Matrix 2D Array look pretty
     * @param label
     * @param stringSize
     * @return abbrevLabel of strinSize characters
     */
    private static String generateAbbrevLabel(String label, int max_label_len){
    	if (label.equals(""))
    		label = UNDEFINED;
    	
    	if (label.length() > max_label_len)
    		label = label.substring(0, max_label_len);

    	return label; 
    }

    private static String centerString (int width, String s) {
        return String.format("%-" + width  + "s", String.format("%" + (s.length() + (width - s.length()) / 2) + "s", s));
    }

    private final static String PREDICTED = "Predicted";
    private final static String UNDEFINED = "*UNDEFINED*";
    /**
     * Generate String arrays to be printed later. If csv=True, then separate each column with commas
     * @param array, csv
     * @return matrixAsString
     */
    private static <DATATYPE extends Number> String mapToString(Map<String,Map<String,DATATYPE>> matrix, boolean csv) {
    	List<String> yAxis = new ArrayList<String>(matrix.keySet());
    	List<String> xAxis = new ArrayList<String>(getPredictedLabels(matrix));
    	Collections.sort(xAxis);
    	Collections.sort(yAxis);
    	int max_label_len = getMaxLabelLen(matrix.keySet()); 
;

        String cellFormat = "[ %" + (max_label_len) + "s ]";
        String rowLabelFormat = "%-" + max_label_len +"s->";
        	
        //If csv, remove brackets and add a comma after for csv format
        if (csv) {
           cellFormat = " %" + (max_label_len) + "s ";
           rowLabelFormat = "%-" + max_label_len +"s->, ";

        }

        String matrixAsString = String.format(rowLabelFormat, PREDICTED); 

        //Prints the X headers
    	boolean first = true;
        for (String label : xAxis){
        	if (!first && csv)
        		matrixAsString += ",";
    		first = false;
        	matrixAsString += String.format(cellFormat,  centerAbbrevLabel(label, max_label_len)); 
        }
        matrixAsString += "\n";
        
        //Prints the Y headers and their innerArray
        for (String yLabel: yAxis) {
        	first = true;
            matrixAsString +=  String.format(rowLabelFormat, generateAbbrevLabel(yLabel,max_label_len));
            for (String xLabel : xAxis) {
            	if (!first && csv)
            		matrixAsString += ",";
        		first = false;
            	DATATYPE num = getSetCell(matrix,yLabel, xLabel);
            	String numString;
            	if (num instanceof Double)
            		numString = String.format("%5.2f", num.doubleValue());
            	else
            		numString = String.format("%6d", num.intValue()); // num.toString();
            	if (yLabel.equals(xLabel) && !csv)
            		numString = "* " + numString + " *";
            	String centered = centerString(max_label_len, numString);
            	String cellString = String.format(cellFormat, centered); 
            	matrixAsString += cellString; 
            }
            matrixAsString += "\n";
        }
        return matrixAsString;
    }
    



	/**
     * Prints the matrix as String. Calls mapToString with csv parameter = False. 
     * @param matrix
     * @return
     */
    private <DATATYPE extends Number> String mapToString(Map<String,Map<String,DATATYPE>> matrix){
    	return mapToString(matrix, false);
    }
    
    /**
     * Prints the matrix as csv. Calls mapToString with csv parameter = True.
     * @param matrix
     * @return
     */
    public <DATATYPE extends Number> String mapToCsv(Map<String,Map<String,DATATYPE>> matrix){
    	return mapToString(matrix, true);
    }
    

    @Override
    /**
     * Overrides the toString method to print the two ConfusionMatrix arrays
     */
    public String toString(){
       	return "COUNT MATRIX:\n"   + mapToString(this.getCounts()) + "\n" +
               "PERCENT MATRIX:\n" + mapToString(this.getPercents());
    }

    private transient Map<String,BinaryConfusionMatrix> errorMatrixCache; 

    /**
     * Creates and returns a specific binary confusion matrix for a given label. Every label has its own matrix.
     * @return matrix for that specific label
     */
    private BinaryConfusionMatrix getBinaryMatrix(String label){
		int correctlyClassified;
		int rowTotal;
		int columnTotal;
		initErrorMatrixCache();
		
		BinaryConfusionMatrix matrix = errorMatrixCache.get(label);
		if (matrix != null)
			return matrix;

        //We try to get the label, if it doesn't exist, set correctlyClassified to 0
		Integer cell = getCell(countsMap, label, label); 
    	if (cell == null)
    		correctlyClassified = 0;
    	else
    		correctlyClassified = cell.intValue();
    		

    	rowTotal = getRowCount(label);
    	columnTotal = getColumnCount(label);

    	//Set the variables to the correct values
		int truePositive = correctlyClassified;
    	int falsePositive = columnTotal - correctlyClassified;
    	int falseNegative = rowTotal - correctlyClassified;
		int trueNegative = this.totalSamples - truePositive - falseNegative - falsePositive;


    	matrix = new BinaryConfusionMatrix(truePositive, trueNegative, falsePositive, falseNegative);
    	errorMatrixCache.put(label, matrix);
    	return matrix;
    }


	/**
	 *  Initialize our transient {@link #errorMatrixCache} field.
	 */
	private synchronized void initErrorMatrixCache() {
		if (errorMatrixCache == null) 
			errorMatrixCache = new Hashtable<String, BinaryConfusionMatrix>();
	}


    /**
     * Given a label, get how many labels were classified as that label
     * @param predictedLabel
     * @return sum of the column count
     */
    private int getColumnCount(String predictedLabel){
	   int sum = 0;

	   for (String realLabel : countsMap.keySet()) {
		   Map<String,Integer> row = countsMap.get(realLabel);
		   Integer v = row.get(predictedLabel);
		   if (v != null)
			   sum += v;
	   }
	   return sum;
   }
    
    /**
     * Given a label, get how many labels had that realLabel
     * @param label
     * @return the sum of the row count
     * @throws IllegalArgument if the label is not a true label known by the matrix.
     */
    private int getRowCount(String label){
	   int sum = 0;
	   Map<String,Integer> row = countsMap.get(label);
	   if (row == null)
		   throw new IllegalArgumentException("Label " + label + " not found in the matrix.");
	   for (Integer v : row.values()) 
		   sum += v;
	   return sum;
   }
    
	private static Map<String, Map<String, Integer>> addCountMaps(Map<String, Map<String, Integer>> counts1, Map<String, Map<String, Integer>> counts2) {
		Set<String> real = new HashSet<String>();
		real.addAll(counts1.keySet());
		real.addAll(counts2.keySet());
		Set<String> predicted1 = getPredictedLabels(counts1);
		Set<String> predicted2 = getPredictedLabels(counts2);
		predicted1.addAll(predicted2);
		Map<String, Map<String, Integer>> sum = new HashMap<String, Map<String, Integer>>();
		Integer zero = new Integer(0);
		for (String realLabel : real) {
			for (String predictedLabel : predicted1) { // all predicted labels
				Integer v1 = getCell(counts1, realLabel, predictedLabel);
				v1 = v1 == null ? zero : v1;
				Integer v2 = getCell(counts2, realLabel, predictedLabel);
				v2 = v2 == null ? zero : v2;
				setCell(sum, realLabel, predictedLabel, v1 + v2);
			}
		}
		return sum;
	}
    
    /**
     * Add the two matrices together to create a new one - do not modify this instance or the given matrix.
     * The returned matrix must add the means from the two matrices and keep statistics on the various accuracies
     * so that {@link #getAccuracy()} and other methods can return OnlineStats objects.
     * @param matrix
     * @return a new matrix that maintains the accuracy statistics in the addition of the two matrices.
     * @throws IllegalArgumentException if the two matrices were not computed from the same label name.
     */
    public ConfusionMatrix add(ConfusionMatrix matrix) {
        if (!matrix.getLabelName().equals(this.getLabelName()))
        	throw new IllegalArgumentException("Matrices are defined on different labels");
        
        Map<String,Map<String,Integer>> sum = addCountMaps(this.countsMap, matrix.countsMap);
        //Instantiates a brand new ConfusionMatrix
        ConfusionMatrix newMatrix = new ConfusionMatrix(matrix.getLabelName(), sum); 
        
        //Combine accuracies from both matrices
        newMatrix.microAccuracy = this.microAccuracy.combine(matrix.microAccuracy);
        newMatrix.macroAccuracy = this.macroAccuracy.combine(matrix.macroAccuracy);
        
        return newMatrix;
    }

    /**
     * Get the accuracy as defined by 
     * @param label
     * @return
     */
	public double getF1Score(String label) {
		return getBinaryMatrix(label).getF1Score();
	}    

	public double getPrecision(String label) {
		return getBinaryMatrix(label).getPrecision();
	}    

	public double getRecall(String label) {
		return getBinaryMatrix(label).getRecall();
	}


	public int getRealLabelCount(String l) {
		return getRowCount(l); 
	}    
	
	private int getMaxLabelLen() {
		return getMaxLabelLen(this.countsMap.keySet());
	}
	
    private static int getMaxLabelLen(Collection<String> labels) {
		int max = 0;
		for (String label : labels) {
			if (label.length() > max)
				max = label.length();
		}
		max = Math.max(max, PREDICTED.length());
		max = Math.max(max, UNDEFINED.length());
		return max;
	}
	public String formatStats() {
		int max_label_len = getMaxLabelLen();
		String headerFormatString =  "%-" + (max_label_len + 4) + "s | %6s | %9s | %9s | %9s\n";
		String formatString =        "%-" + (max_label_len + 4) + "s | %6d | %9.3f | %9.3f | %9.3f\n";
		String msg = String.format(headerFormatString, "Label", "Count", "F1", "Precision", "Recall");
		for (String l : getRealLabels()) 
			msg += String.format(formatString, generateAbbrevLabel(l, max_label_len), getRealLabelCount(l), 100 * getF1Score(l), 100 * getPrecision(l), 100 * getRecall(l));
		msg += String.format(formatString, "Micro-averaged", totalSamples , 100 * getF1Score(true).getMean(), 100 * getPrecision(true).getMean(), 100 * getRecall(true).getMean());
		msg += String.format(formatString, "Macro-averaged", totalSamples , 100 * getF1Score(false).getMean(), 100 * getPrecision(false).getMean(), 100 * getRecall(false).getMean());
		return msg;
	}

	/**
	 * Get a string presentation of the matrix containing counts. 
	 * @return
	 */
	public String formatCounts() {
		 return mapToString(this.getCounts());
	}

	/**
	 * Get a string presentation of the matrix containing percentages of actual labels. 
	 * @return
	 */
	public String formatPercents() {
		 return mapToString(this.getPercents());
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((countsMap == null) ? 0 : countsMap.hashCode());
		result = prime * result + ((labelName == null) ? 0 : labelName.hashCode());
		result = prime * result + ((macroAccuracy == null) ? 0 : macroAccuracy.hashCode());
		result = prime * result + ((microAccuracy == null) ? 0 : microAccuracy.hashCode());
		result = prime * result + totalSamples;
		return result;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ConfusionMatrix))
			return false;
		ConfusionMatrix other = (ConfusionMatrix) obj;
		if (countsMap == null) {
			if (other.countsMap != null)
				return false;
		} else if (!countsMap.equals(other.countsMap))
			return false;
		if (labelName == null) {
			if (other.labelName != null)
				return false;
		} else if (!labelName.equals(other.labelName))
			return false;
		if (macroAccuracy == null) {
			if (other.macroAccuracy != null)
				return false;
		} else if (!macroAccuracy.equals(other.macroAccuracy))
			return false;
		if (microAccuracy == null) {
			if (other.microAccuracy != null)
				return false;
		} else if (!microAccuracy.equals(other.microAccuracy))
			return false;
		if (totalSamples != other.totalSamples)
			return false;
		return true;
	}


	/**
	 * Get the micro-averaged accuracy across the whole matrix of labels.
	 * @return
	 */
	public OnlineStats getAccuracy() {
		return getAccuracy(true); 
	}

    /**
     * Get the micro or macro-averaged accuracy across all label values in the matrix.
     * @param asMicro if true, then get the micro-averaged value, otherwise the macro-averaged value.
     * @return
     */
    public OnlineStats getAccuracy(boolean asMicro){
    	return asMicro ? this.microAccuracy.accuracy : this.macroAccuracy.accuracy;
    }
    
	/**
	 * @return the totalSamples
	 */
	public int getTotalSamples() {
		return totalSamples;
	}
}
