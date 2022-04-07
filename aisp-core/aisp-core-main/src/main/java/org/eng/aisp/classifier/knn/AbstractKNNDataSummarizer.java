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
package org.eng.aisp.classifier.knn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.StatUtils;
import org.eng.aisp.AISPException;
import org.eng.aisp.AISPProperties;
import org.eng.aisp.util.Array2DIndexComparator;
import org.eng.aisp.util.IndexPair;
import org.eng.util.ExecutorUtil;

/**
 * Provides the primary mechanisms for managing a constrained list of labeled data.
 * Labeled data is merged into the instance and when necessary, data is merged within the instance with other data having the same label.
 * Merges are generally done with the closest neighbors as judged by a distance function provided by the caller.
 * To restart a summarization, call {@link #reset()}.
 * <p>
 * Subclasses must implement {@link #combineFeatures(int, Serializable, int, Serializable)} to merge two pieces of data. 
 * @author dawood
 *
 * @param <DATA>
 */
public abstract class AbstractKNNDataSummarizer<DATA extends Serializable> extends BaseKNNDataSummary<DATA> implements Serializable {

	private static final long serialVersionUID = -7728500215987003928L;

	/** Name of caa.properties property (or system property) that defines the reduction ratio of feature vectors when exceeding the maximum size of list of labeled features */
	public static final String DEFAULT_REDUCTION_RATIO_PROPERTY_NAME = "classifiers.nn.reduction_ratio";
	public static final double DEFAULT_REDUCTION_RATIO = AISPProperties.instance().getProperty(DEFAULT_REDUCTION_RATIO_PROPERTY_NAME, 0.5);
	protected final int maxListSize;
	protected final double reductionRatio;
	protected final double stdDevFactor;
	private boolean isDirty = true;
	private List<Integer> labeledDataWeights = new ArrayList<Integer>();
	
	/**
	 * 
	 * @param trainingLabel the label extracted from the data and associated with the summary. 
	 * @param maxListSize the maximum number of data items to keep in the summary.
	 * @param distFunc the function used to compute the distance between data items.
	 * @param stdDevFactor the number of standard deviations, as present in the distances, to use when computing
	 * the maxDistBetweenSameLabel and lowerBoundDelta, which can be used during classification. 
	 * @param reductionRatio the percentage to reduce the size of the list when doing a merge to meet the size requirements set by maxListSize.  Must
	 * be in the range 0.5 to 1.0
	 */
	public AbstractKNNDataSummarizer(int maxListSize, IDistanceFunction<DATA> distFunc, 
			double stdDevFactor, boolean enableOutlierDetection, double reductionRatio) {
		super(distFunc, null, 0.0, 0.0, enableOutlierDetection);
		if (reductionRatio < 0.5 || reductionRatio > 1.0) 
			throw new IllegalArgumentException("Value of reductionRatio must be within the range of [0.5, 1.0]");
		
		if (maxListSize <= 0)
			throw new IllegalArgumentException("maxListSize must be larger than 0");
		this.maxListSize = maxListSize;
		this.reductionRatio = reductionRatio;
		this.stdDevFactor = stdDevFactor;
	}

	/**
	 * <p> Make sure the {@link #listOfLabeledData} does not have more then {@link #maxListSize} elements.
	 * Reduce size to {@link #reductionRatio} times of current size of {@link #listOfLabeledData} if exceeding {@link #maxListSize}.
	 * </p>
	 * <p>If reductionRatio is 1.0, remove at least one feature vector from {@link #listOfLabeledData}.
	 * The minimum size of {@link #listOfLabeledData} is constrained to have one feature for each label in the code implementation.
	 * </p>
	 * <p>The value of {@link #reductionRatio} must be within [0.5, 1.0]. 
	 * For the feature vectors for each label, at most half of the feature vectors can be removed (combined) in each call of {@link #constrainlistOfLabeledDataSize()}.
	 * </p>
	 * @throws AISPException 
	 */
	private void shrinkListOfLabeledData(LabeledData<DATA> lastAdded) throws AISPException {
		while(listOfLabeledData.size()>maxListSize) {
//			System.out.println("constrainlistOfLabeledDataSize started " + listOfLabeledData.size());
//			double reductionRatio = DEFAULT_REDUCTION_RATIO;
			
			
			int totalTargetSize = (int)((listOfLabeledData.size() - 1) * reductionRatio);  
			
			/**
			 * Build the maps of label value to occurrence count and label value to list of feature indices.
			 */
			Map<String, Integer> labelFeatureCountMap = new HashMap<String, Integer>();	// The holds number of features for each label value.
			Map<String, List<Integer>> labelFeatureIndexMap = new HashMap<String, List<Integer>>();	// Holds the indices of features for each label value.
			
			int maxCountEachLabel = 0;
			int index = 0;
			for (LabeledData<DATA> l : listOfLabeledData) { 	
				String label = l.getLabel();
			
				//  Adjust label value count.
				Integer labelCount = labelFeatureCountMap.get(label);
				if (labelCount == null) {
					labelCount=1;
				} else {
					labelCount++;
				}
				labelFeatureCountMap.put(label, labelCount);
				if (labelCount > maxCountEachLabel) 
					maxCountEachLabel = labelCount;
				
				// Adjust label value indices.
				List<Integer> labelFeIndexList = labelFeatureIndexMap.get(label);
				if (labelFeIndexList == null) {
					labelFeIndexList = new ArrayList<Integer>();
					labelFeatureIndexMap.put(label, labelFeIndexList);
				}
				labelFeIndexList.add(index);
				index++;
			}
			
			if (totalTargetSize < labelFeatureCountMap.size()) 
				totalTargetSize = labelFeatureCountMap.size();  //Ensure that there is at least one feature for each label
			
			/*
			 * Find the maximum amount of features for any single label.  
			 * This tries to allow some labels to have larger number of features if some have smaller numbers of features. 
			 * Not all labels will reach this max.
			 * This is a more sophisticated alternative to capping each label value to an average number of features.
			 */
			int targetSizeEachLabel = -1;
			Integer[] labelCountArray = labelFeatureCountMap.values().toArray(new Integer[labelFeatureCountMap.size()]);
			for (int countEachLabel = 1; countEachLabel <= maxCountEachLabel; countEachLabel++) {
				int sumSize = 0;
				for (int lc : labelCountArray) {
					sumSize += Math.min(lc, countEachLabel);
				}
				if (sumSize > totalTargetSize) {
					targetSizeEachLabel = countEachLabel - 1;
					break;
				}
			}
			
			
			List<LabeledData<DATA>> listOfLabeledDataNew = new ArrayList<>();
			List<Integer> listOfLabeledDataWeightsNew = new ArrayList<>();	

			for (String label : labelFeatureIndexMap.keySet()) {
				List<Integer> featureIndexList = labelFeatureIndexMap.get(label);
				
				// Get all the features for the current label.
				List<LabeledData<DATA>> listOfLabeledDataNewSingleLabel = new ArrayList<>();
				List<Integer> listOfLabeledDataWeightsNewSingleLabel = new ArrayList<>();
				for (int i : featureIndexList)  {
					listOfLabeledDataNewSingleLabel.add(listOfLabeledData.get(i));
					listOfLabeledDataWeightsNewSingleLabel.add(labeledDataWeights.get(i));
				}
				
				int newSingleLabelSize = listOfLabeledDataNewSingleLabel.size();
				if (newSingleLabelSize <= targetSizeEachLabel) {
					//No need to combine features
					listOfLabeledDataNew.addAll(listOfLabeledDataNewSingleLabel);
					listOfLabeledDataWeightsNew.addAll(listOfLabeledDataWeightsNewSingleLabel);
				} else {
					//Need to combine features
					int numToCombine = newSingleLabelSize - targetSizeEachLabel;
					
					/*
					 * Compute the sorted distances between features.
					 */
					double[][] dist = new double[newSingleLabelSize][newSingleLabelSize];
					Array2DIndexComparator comparator = new Array2DIndexComparator(dist);
					IndexPair[] indexes = comparator.createIndexArray(true);   //this contains indexes of sorted distances
//					long start = System.nanoTime();
//					if (!sortTimer.recommendSerial(indexes.length)) {
					if (newSingleLabelSize == 1) {		// Based on some simple tests.	
						// Do the distance calc and sorting serially 
						for (int i=0; i<newSingleLabelSize; i++) {
							for (int j=i+1; j<newSingleLabelSize; j++) {
								DATA f1 = listOfLabeledDataNewSingleLabel.get(i).getData();
								DATA f2 = listOfLabeledDataNewSingleLabel.get(j).getData();
								dist[i][j] = distFunc.distance(f1,f2);
							}
						}
						Arrays.sort(indexes, comparator);
//						long nanos = System.nanoTime() - start;
//						sortTimer.recordSerial(indexes.length, nanos); 
//						System.out.println("arraySize: " + indexes.length + ", s-nanos: " + nanos/ Math.max(1, indexes.length));
					} else {
						// Do the distance calc and sorting in parallel
						Arrays.stream(indexes).parallel().forEach(indexPair-> 
							{
								int i = indexPair.getX();
								int j = indexPair.getY();
								DATA f1 = listOfLabeledDataNewSingleLabel.get(i).getData();
								DATA f2 = listOfLabeledDataNewSingleLabel.get(j).getData();
								dist[i][j] = distFunc.distance(f1,f2);
							}
						);
						Arrays.parallelSort(indexes, comparator);
//						long nanos = System.nanoTime() - start;
//						sortTimer.recordParallel(indexes.length, nanos); 
//						System.out.println("arraySize: " + indexes.length + ", p-nanos: " + nanos/ Math.max(1, indexes.length));
					}
					
					
					/*
					 * Go through the list of indices of features, sorted by distance from each other, and 
					 * select pairs of indices for which each index has not already been selected.
					 * For example, if the list of sorted indices contains [ (1,2), (1,3), (3,2)... ] we would merge
					 * (1,2) and (3,2) and skip (1,3) because (1,2) was picked first (and has index 1 in common).
					 * TODO: we are not merging (1,3) when perhaps we should.
					 */
					Map<Integer, Integer> combinedIndexesMap = new HashMap<Integer, Integer>();
					List<IndexPair> combinedIndexes = new ArrayList<IndexPair>();
					int combinedCount = 0;
					for (int i=0; ((i<indexes.length) && (combinedCount < numToCombine)); i++) {
						int x = indexes[i].getX();
						int y = indexes[i].getY();
						if ((combinedIndexesMap.get(x) == null) && (combinedIndexesMap.get(y) == null)) {
							combinedIndexesMap.put(x, y);
							combinedIndexesMap.put(y, x);
							combinedIndexes.add(indexes[i]);
							combinedCount++;
						}
					}
					
					
//					boolean noWeights = true;
					final boolean noWeights = false;
					for (IndexPair indexPair : combinedIndexes) {
						int xIndex = indexPair.getX();
						int yIndex = indexPair.getY();
						LabeledData<DATA> ld1 = listOfLabeledDataNewSingleLabel.get(xIndex);
						int weight1 =  listOfLabeledDataWeightsNewSingleLabel.get(xIndex);
						if (noWeights)
							weight1 =  1;
						LabeledData<DATA> ld2 = listOfLabeledDataNewSingleLabel.get(yIndex);
						int weight2 =  listOfLabeledDataWeightsNewSingleLabel.get(yIndex);
						if (noWeights)
							weight2 =  1; 
						LabeledData<DATA> ld = combineFeatures(weight1, ld1, weight2, ld2);
						listOfLabeledDataNew.add(ld);
						listOfLabeledDataWeightsNew.add(weight1 + weight2);
							
					}
				}
			}
			
			listOfLabeledData = listOfLabeledDataNew;
			labeledDataWeights = listOfLabeledDataWeightsNew;
			
//			System.out.println("constrainlistOfLabeledDataSize completed " + listOfLabeledData.size());

		}
	}

	/**
	 * Combine the 2 data items into a new item using the given weights.
	 * @param d1Weight
	 * @param d1
	 * @param d2Weight
	 * @param d2
	 * @return never null.
	 */
	protected abstract DATA combineFeatures(int d1Weight, DATA d1, int d2Weight, DATA d2);
	
	private LabeledData<DATA> combineFeatures(int ld1Weight, LabeledData<DATA> ld1, int ld2Weight, LabeledData<DATA> ld2) {
		DATA combined = combineFeatures(ld1Weight, ld1.getData(), ld2Weight, ld2.getData());
		return new LabeledData<DATA>(ld1.getLabel(), combined); 
	}


	private static class SerialParallelTimer {

		double serialTime = 0;
		double parallelTime = 0;
		int lastSerialSize = 0;
		int lastParallelSize = 0;
		
		public void recordSerial(int size, double time) {
			if (size < lastParallelSize || size < lastSerialSize)
				reset();
			lastSerialSize = size;
			if (size == 0) size = 1;
			serialTime= time / size ;
		}

		public void recordParallel(int size, double time) {
			if (size < lastParallelSize || size < lastSerialSize)
				reset();
			lastParallelSize = size;
			if (size == 0) size = 1;
			parallelTime = time / size; 
		}

		public boolean recommendSerial(int size) {
			if (size <= 4)
				return true;
			else if (lastSerialSize > lastParallelSize*2)
				return false;			// Get a new parallel timing.
//			else if (lastParallelSize < lastSerialSize)
//				return true;	// Heading down in # of ops and we crossed into the territory where serial may be better. 
			else
				return parallelTime >= serialTime;
		}

		public void reset() {
			serialTime = 0;
			parallelTime = 0;
			lastSerialSize = 0;
			lastParallelSize = 0;
		}
	}

//	transient SerialParallelTimer mergeTimer = new SerialParallelTimer();
//	transient SerialParallelTimer sortTimer = new SerialParallelTimer();

	private static Integer UnitWeight = new Integer(1);
	/**
	 * Add the given labeled data to the instance and merge it with previously added data if we hit the max number of data for this instance. 
	 * @param labelValue
	 * @param data
	 * @throws AISPException
	 */
	public void mergeData(String labelValue, DATA data) throws AISPException {
					
		LabeledData<DATA> ld = new LabeledData<DATA>(labelValue, data);
		listOfLabeledData.add(ld);
		labeledDataWeights.add(UnitWeight);
		if (listOfLabeledData.size() >  maxListSize)
			shrinkListOfLabeledData(ld);
		
		isDirty = true;	
	}
	
	private void computeStats() {
		//Find min. distance and standard deviation for detecting unknown labels
//		final OnlineStats stats = new OnlineStats();
		
		// TODO:  see if this is worth doing in parallel for small numbers.
		double[] minDistSingleSampleArrayPrim; 
		int size = listOfLabeledData.size(); 
//		long start = System.nanoTime();
		int arraySize = size*size - size;
//		if (mergeTimer.recommendSerial(arraySize)) {
		if (arraySize == 1) {
//			minDistSingleSampleArrayPrim = new double[arraySize];
//			int index=0;
//			for (int i=0 ; i<size ; i++) {
//				DATA d1 = listOfLabeledData.get(i).getData();
//			    for (int j=i+1 ; j<size ; j++) {
//			    	DATA d2 = listOfLabeledData.get(j).getData();
//			    	double tmpDist = distFunc.distance(d1,d2);
//			    	minDistSingleSampleArrayPrim[index] = tmpDist;
//			    	index++;
//			    }
//			}
//			double nanos = (System.nanoTime() - start);
//			mergeTimer.recordSerial(arraySize, nanos); 
//			System.out.println("arraySize: " + arraySize + ", s-nanos: " + nanos/ Math.max(1, arraySize));
			minDistSingleSampleArrayPrim = computeMinDistancesSerial();
		} else {
//			minDistSingleSampleArrayPrim = computeMinDistancesSerial();
			minDistSingleSampleArrayPrim = computeMinDistancesParallel();
			
//			double nanos = (System.nanoTime() - start); 
//			mergeTimer.recordParallel(arraySize, nanos);
//			System.out.println("arraySize: " + arraySize + ", p-nanos: " + nanos/ Math.max(1, arraySize));
		}	
//		System.out.println("Serial   speed: " + serial.getMean());
//		System.out.println("Parallel speed: " + parallel.getMean());
		
		double mean = StatUtils.mean(minDistSingleSampleArrayPrim);
		double stddev = Math.sqrt(StatUtils.variance(minDistSingleSampleArrayPrim, mean));
		
		if (Double.isNaN(stddev)) 
			stddev = 0.0001 * mean;  //This may happen if samples are too close to each other
		
		double maxDist = mean + stddev * stdDevFactor;
		
		if (Double.isNaN(maxDist) || maxDist <= 0.0001)  maxDist = 0.0001;   //This could happen if distances among all samples are too close to each other

		this.setMaxDistBetweenSameLabel(maxDist);
		this.setLowerBoundDelta(stddev*stdDevFactor);
		isDirty = false;
	}

	/**
	 * For each data point, calculate the distance to its nearest neighbor. 
	 * @return an array of nearest neighbor distances for all points, in undefined order.
	 */
	private double[] computeMinDistancesSerial() {
		List<Double> minDistSingleSampleList = 
				// TODO: We use stream() instead of a parallelStream() to avoid what seems to be a bug in streams on IBM's Java 8 JRE. -dawood 10/30/2017
				// The bug/hand was only seen when training a MultiClassifier inside tomcat 8 on Ubuntu.
				// See https://github.ibm.com/IoT-Sound/iot-sound/issues/221
//				listOfLabeledData.parallelStream()
				listOfLabeledData.stream()
				.map(fl1 -> 
				{
					double minDistSingleSample = Double.MAX_VALUE;
					for (LabeledData<DATA> fl2: listOfLabeledData) {
						double tmpDist = distFunc.distance(fl1.getData(), fl2.getData());
			
						if(tmpDist < minDistSingleSample && tmpDist > 0.0) 
							minDistSingleSample=tmpDist;
					}
					if (minDistSingleSample == Double.MAX_VALUE) 
						return Double.valueOf(0);
					else
						return new Double(minDistSingleSample);
				})
				.collect(Collectors.toList())
				;
			double[] minDistSingleSampleArrayPrim = new double[minDistSingleSampleList.size()];
			for (int i=0 ; i<minDistSingleSampleArrayPrim.length ; i++)
				minDistSingleSampleArrayPrim[i] = minDistSingleSampleList.get(i).doubleValue();;
			return minDistSingleSampleArrayPrim;
	}
	
	private class ComputeMinDistanceTask implements Callable<Object> {

		private AtomicInteger sharedIndex;
		private double[] minDistances;

		public ComputeMinDistanceTask(AtomicInteger sharedIndex, double[] minDistances) {
			this.sharedIndex = sharedIndex;
			this.minDistances = minDistances;
		}

		@Override
		public Object call() throws Exception {
			while (true) {
				int myIndex = sharedIndex.getAndIncrement();
				if (myIndex >= minDistances.length)
					break;
				double minDistSingleSample = Double.MAX_VALUE;
				LabeledData<DATA> fl1 = listOfLabeledData.get(myIndex);
				for (LabeledData<DATA> fl2: listOfLabeledData) {
					double tmpDist = distFunc.distance(fl1.getData(), fl2.getData());
					if(tmpDist < minDistSingleSample && tmpDist > 0.0) 
						minDistSingleSample=tmpDist;
				}
				if (minDistSingleSample == Double.MAX_VALUE) 
					return minDistSingleSample = 0; 
				minDistances[myIndex] = minDistSingleSample;
			}
			return null;
		}
		
	}
	
	/**
	 * For each data point, calculate the distance to its nearest neighbor. 
	 * @return an array of nearest neighbor distances for all points, in undefined order.
	 */
	protected double[] computeMinDistancesParallel() {
		double[] minDistances = new double[this.listOfLabeledData.size()];
		AtomicInteger sharedIndex = new AtomicInteger(0);
		List<ComputeMinDistanceTask> cdList = new ArrayList<ComputeMinDistanceTask>();
		for (int i=0 ; i<Runtime.getRuntime().availableProcessors(); i++) {
			ComputeMinDistanceTask cd = new ComputeMinDistanceTask(sharedIndex, minDistances);
			cdList.add(cd);
		}
		ExecutorService executor = ExecutorUtil.getPrioritizingSharedService();
		try {
			executor.invokeAll(cdList);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			//minDistances = computeMinDistancesSerial();
		}
		return minDistances;
	}

	/**
	 * Should be called to forget past results of {@link #mergeData(String, Serializable)}.
	 */
	public void reset() {
		this.listOfLabeledData.clear();
		this.labeledDataWeights.clear();
//		this.mergeTimer.reset();
	}

	/**
	 * Override to make sure we have computed this based on the last call to {@link #mergeData(String, Serializable)}
	 */
	@Override
	public double getLowerBoundDelta() {
		if (isDirty)
			computeStats();
		return super.getLowerBoundDelta();
	}

	/**
	 * Override to make sure we have computed this based on the last call to {@link #mergeData(String, Serializable)}
	 */
	@Override
	public double getMaxDistBetweenSameLabel() {
		if (isDirty)
			computeStats();
		return super.getMaxDistBetweenSameLabel();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (isDirty ? 1231 : 1237);
		result = prime * result + ((labeledDataWeights == null) ? 0 : labeledDataWeights.hashCode());
		result = prime * result + maxListSize;
		long temp;
		temp = Double.doubleToLongBits(reductionRatio);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(stdDevFactor);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof AbstractKNNDataSummarizer))
			return false;
		AbstractKNNDataSummarizer other = (AbstractKNNDataSummarizer) obj;
		if (isDirty != other.isDirty)
			return false;
		if (labeledDataWeights == null) {
			if (other.labeledDataWeights != null)
				return false;
		} else if (!labeledDataWeights.equals(other.labeledDataWeights))
			return false;
		if (maxListSize != other.maxListSize)
			return false;
		if (Double.doubleToLongBits(reductionRatio) != Double.doubleToLongBits(other.reductionRatio))
			return false;
		if (Double.doubleToLongBits(stdDevFactor) != Double.doubleToLongBits(other.stdDevFactor))
			return false;
		return true;
	}


}
