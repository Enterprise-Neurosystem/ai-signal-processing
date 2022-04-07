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
package org.eng.aisp.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.eng.aisp.IDataWindow;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.feature.IFeature;
import org.eng.util.ExecutorUtil;

public class ClassifierUtils {

//	/**
//	 * Turn the matrix of features into a single vector appending columns end-to-end.
//	 * @param wf matrix of features.
//	 * @param subVectorLengthReturn if non-null, then the 0th element contains the size of the columns in the input matrix.
//	 * @return the vector of features.
//	 */	
//	public static double[] averageListOfFeatures(ILabeledFeature<double[]>[] wf, boolean normalize) {
//		double [] freqComponents=new double[wf[0].getFeature().getData().length];
//		for (int i=0; i<freqComponents.length; i++) freqComponents[i] = 0.0;
//		
//		//Sum up FFT coefficients in all windows and normalize again
//		for (ILabeledFeature<double[]> f: wf) {
//			IFeature<double[]> feature = f.getFeature();
//			double [] tmp = feature.getData();
//			for (int i=0; i<Math.min(freqComponents.length, tmp.length); i++) freqComponents[i]+=tmp[i];
//		}
//		double invLength = 1.0 / wf.length;
//		for (int i=0; i < freqComponents.length; i++) freqComponents[i] *=  invLength;
//
//		if (normalize) 
//			VectorUtils.normalize(freqComponents);
//	
//		return freqComponents;
//		
//	}

	/**
	 * Average across rows of the columns in the matrix of features to give a single column feature.
	 * @param wf matrix of features.
	 * @param normalize if true, the normalize the resulting vector using {@link VectorUtils#normalize(double[])}
	 * @return an array of length equal to the length of a column in the input matrix of features.
	 */
	public static double[] averageListOfFeatures(IFeature<double[]>[] wf, boolean normalize) {
		double [] freqComponents=new double[wf[0].getData().length];
		for (int i=0; i<freqComponents.length; i++) freqComponents[i] = 0.0;
					
		//Sum up FFT coefficients in all windows and normalize again
		for (IFeature<double[]> feature: wf) {
			double [] tmp = feature.getData();
			for (int i=0; i<Math.min(freqComponents.length, tmp.length); i++) 
				freqComponents[i]+=tmp[i];
		}
		
		double invLength = 1.0 / wf.length;
		for (int i=0; i < freqComponents.length; i++) 
			freqComponents[i] *= invLength; 

		if (normalize) 
			VectorUtils.normalize(freqComponents);
		
		return freqComponents;
	}

//	/**
//	 * Turn the matrix of features into a single vector appending columns end-to-end.
//	 * @param wf matrix of features.
//	 * @param subVectorLengthReturn if non-null, then the 0th element contains the size of the columns in the input matrix.
//	 * @return the vector of features.
//	 */
//	public static double[] chainListOfFeatures(ILabeledFeature<double[]>[] wf, int[] subVectorLengthReturn) {
//
//		/**
//		 * Flattens a list feature into double array, the size of each vector is given by subVectorLengthReturn[0]
//		 */
//		double [] freqComponents=null;
//		if (wf == null || wf.length == 0)
//			return new double[0];
//		
//		ILabeledFeature<double[]> firstFeature = wf[0];
//		int subVectorLength = firstFeature.getFeature().getData().length;
//		freqComponents = new double[subVectorLength * wf.length];
//
//		//Sum up FFT coefficients in all windows and normalize again
//		int index = 0;
//		for (ILabeledFeature<double[]> f: wf) {
//			IFeature<double[]> feature = f.getFeature();
//			System.arraycopy(feature.getData(), 0, freqComponents, index, Math.min(feature.getData().length, subVectorLength));
//			index += subVectorLength;
//		}
//		
//		if (subVectorLengthReturn != null) subVectorLengthReturn[0] = subVectorLength;
//		return freqComponents;
//		
//	}
	
	/**
	 * Turn the matrix of features into a single vector appending columns end-to-end.
	 * @param wf matrix of features.
	 * @param subVectorLengthReturn if non-null, then the 0th element contains the size of the columns in the input matrix.
	 * @return the vector of features.
	 */
	public static double[] chainListOfFeatures(IFeature<double[]>[] wf, int[] subVectorLengthReturn) {
		/**
		 * Flattens a list feature into double array, the size of each vector is given by subVectorLengthReturn[0]
		 */
		double [] freqComponents=null;
		if (wf == null || wf.length == 0)
			return new double[0];
		
		IFeature<double[]> firstFeature = wf[0];
		int subVectorLength = firstFeature.getData().length;
		freqComponents = new double[subVectorLength * wf.length];

		//Sum up FFT coefficients in all windows and normalize again
		int index = 0;
		for (IFeature<double[]> feature: wf) {
			System.arraycopy(feature.getData(), 0, freqComponents, index, Math.min(feature.getData().length, subVectorLength));
			index += subVectorLength;
		}
		
		if (subVectorLengthReturn != null) subVectorLengthReturn[0] = subVectorLength;
		return freqComponents;
		
	}

	/**
	 * A list that allows insert elements more than 1 element beyond the end of the list.
	 * @author DavidWood
	 *
	 */
	private static class GappableLinkedList<ITEM> extends LinkedList<ITEM> {

		private static final long serialVersionUID = -415951314663400699L;

		private void makeRoom(int index) {
			for (int i=this.size() ; i<=index ; i++)
				super.add(i,null);
		}

		@Override
		public boolean addAll(int index, Collection<? extends ITEM> c) {
			this.makeRoom(index);;
			return super.addAll(index, c);
		}

		@Override
		public ITEM set(int index, ITEM element) {
			this.makeRoom(index);
			return super.set(index, element);
		}

		@Override
		public void add(int index, ITEM element) {
			this.makeRoom(index);;
			super.add(index, element);
		}

		
	}

	private static class BulkClassifier<DATA> implements Callable<Object> {

		private IFixedClassifier<DATA> classifier;
		private Iterator<IDataWindow<DATA>> dataIter;
		private AtomicInteger sharedIndex;
		private GappableLinkedList<Map<String,Classification>> results;

		public BulkClassifier(IFixedClassifier<DATA> classifier, Iterator<IDataWindow<DATA>> dataIter, 
					AtomicInteger sharedIndex, GappableLinkedList<Map<String,Classification>> results) {
			this.classifier = classifier;
			this.dataIter = dataIter;
			this.sharedIndex = sharedIndex;
			this.results = results;
		}

		@Override
		public Object call() throws Exception {

			while (true) {
				IDataWindow<DATA> data;
				int myIndex;
				synchronized (sharedIndex) {
					if (!dataIter.hasNext())
						break;
					data = dataIter.next();
					myIndex = sharedIndex.getAndIncrement();
				}
//				AISPLogger.logger.info("Classifying index=" + myIndex);
				Map<String,Classification> cmap = classifier.classify(data);
				synchronized(results) {
//					AISPLogger.logger.info("adding classification for index=" + myIndex);
					results.set(myIndex, cmap);
				}
			}
			return null;
		}
		
	}
	
	/**
	 * Spawn multiple threads to classify the given data with the given trained classifier.
	 * @param <DATA>
	 * @param classifier
	 * @param data
	 * @return a list of classification results in 1:1 correspondence with the values in the given Iterable of data.
	 * @throws InterruptedException
	 */
	public static <DATA> List<Map<String,Classification>> bulkClassify(IFixedClassifier<DATA> classifier, Iterable<IDataWindow<DATA>> data) throws InterruptedException {
		GappableLinkedList<Map<String,Classification>> results = new GappableLinkedList<Map<String,Classification>>();;
		List<Callable<Object>> taskList = new ArrayList<Callable<Object>>();
		Iterator<IDataWindow<DATA>> dataIter = data.iterator();
		AtomicInteger sharedIndex = new AtomicInteger();
		for (int i=0 ; i<Runtime.getRuntime().availableProcessors() ; i++) {
			 Callable<Object> task = new BulkClassifier<DATA>(classifier, dataIter, sharedIndex, results);
			 taskList.add(task);
		 }
		ExecutorService executor = ExecutorUtil.getPrioritizingSharedService();
		executor.invokeAll(taskList);
		return results;
	}

}
