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
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.Classification.LabelValue;
import org.eng.aisp.util.ArrayIndexComparator;
import org.eng.util.ExecutorUtil;

public class KNNDataSummaryClassifier<DATA extends Serializable> extends BaseKNNDataSummary<DATA> implements Serializable { 

//	protected final double lowerBoundDelta;
//	protected final double maxDistBetweenSameLabel;

	
	private static final long serialVersionUID = 5030910693361467215L;

	public KNNDataSummaryClassifier(IDistanceFunction<DATA> distFunc, List<LabeledData<DATA>> data, 
			double lowerBoundDelta, double maxDistBetweenSameLabel, boolean enableOutlierDetection) {
		super(distFunc, data, lowerBoundDelta, maxDistBetweenSameLabel, enableOutlierDetection);
//		this.listOfLabeledData.addAll(data);
//		this.lowerBoundDelta = lowerBoundDelta;
//		this.maxDistBetweenSameLabel = maxDistBetweenSameLabel;
	}
	

	public Classification classify(String trainingLabel, DATA data) {
		
//		Double[] distances = computeDistancesSerial(data);
		Double[] distances = computeDistancesParallel(data);
		
		ArrayIndexComparator comparator = new ArrayIndexComparator(distances);
		Integer[] indexes = comparator.createIndexArray();   //this contains indexes of sorted distances
		Arrays.sort(indexes, comparator);	// Can't use parallelSort() until we get off of 1.7 JVM
		
		int minIndex = indexes[0];
		double minDist = distances[minIndex];
		
		
		//find min. distances for all labels
		List<String> labels = new ArrayList<String>();
		List<Double> minDistAllLabels = new ArrayList<Double>();
		
		for (int i=0; i<indexes.length; i++) {
			String thisLabel = this.listOfLabeledData.get(indexes[i]).getLabel();
			
			boolean labelExists = false;
			for (String l : labels) {
				if (l.equals(thisLabel)) {
					labelExists = true;
					break;
				}
			}
			
			if (labelExists == false) {
				labels.add(thisLabel);
				minDistAllLabels.add(distances[indexes[i]]);
			}
		}
		
		//softmax computation
		
		double maxMinDist = 0.0;
		for (Double d : minDistAllLabels) {
			if (d > maxMinDist) maxMinDist = d;
		}
		if (maxMinDist==0.0) maxMinDist = 0.0001;
	
		
		double sumExpMinDists = 0.0;
		for (Double d : minDistAllLabels) {
			sumExpMinDists += Math.exp(-d/maxMinDist);
		}
		if (sumExpMinDists==0.0) sumExpMinDists = 0.0001;
		
		double[] softMaxValues = new double[minDistAllLabels.size()];
		for (int i=0; i<minDistAllLabels.size(); i++) {
			softMaxValues[i] = Math.exp(-minDistAllLabels.get(i)/maxMinDist) / sumExpMinDists;
		}
		
		String status;
		if(!this.isEnableOutlierDetection() || this.getMaxDistBetweenSameLabel() < 0 || minDist < this.getMaxDistBetweenSameLabel() ) { 
			status = this.listOfLabeledData.get(minIndex).getLabel();
		} else {
			status = Classification.UndefinedLabelValue;
		}
			
		double confidenceBaseKnown;
		double confidenceUnknown;
		if (!Classification.UndefinedLabelValue.equals(status)) {
			confidenceBaseKnown = Math.max(0.5, 1.0 - Math.max(0.0, minDist - this.getLowerBoundDelta())/this.getMaxDistBetweenSameLabel());
			if (softMaxValues[0] * confidenceBaseKnown < 1-confidenceBaseKnown) {
				confidenceBaseKnown = Math.min(1.0, 1.0 / (1.0 + softMaxValues[0]) + 0.01);  // Make sure that confidence of max. known label is not smaller than confidence for unknown
			}
			confidenceUnknown = 1-confidenceBaseKnown;
		} else { 
			confidenceUnknown = Math.max(0.5, Math.min(1.0, minDist/this.getMaxDistBetweenSameLabel() - 1.0));
			confidenceBaseKnown = 1-confidenceUnknown;
		}
		
		List<LabelValue> rankedValues = new ArrayList<LabelValue>();
		if (!Classification.UndefinedLabelValue.equals(status)) {
			boolean unknownLabelAdded = false;
			for (int i=0; i<labels.size(); i++) {
				if (softMaxValues[i] * confidenceBaseKnown < confidenceUnknown && !unknownLabelAdded) {
					rankedValues.add(new LabelValue(Classification.UndefinedLabelValue, confidenceUnknown));
					unknownLabelAdded = true;
				}
				rankedValues.add(new LabelValue(labels.get(i), softMaxValues[i] * confidenceBaseKnown));
			}
			if (!unknownLabelAdded) {
				rankedValues.add(new LabelValue(Classification.UndefinedLabelValue, confidenceUnknown));
				unknownLabelAdded = true;
			}
		} else {
			rankedValues.add(new LabelValue(Classification.UndefinedLabelValue, confidenceUnknown));
			for (int i=0; i<labels.size(); i++) {
				rankedValues.add(new LabelValue(labels.get(i), softMaxValues[i] * confidenceBaseKnown));
			}
		}
	
		return new Classification(trainingLabel, rankedValues);
	}


	/**
	 * @param data
	 * @return
	 */
	protected Double[] computeDistancesSerial(DATA data) {
		Double[] distances = new Double[this.listOfLabeledData.size()];
		for (int i=0; i<this.listOfLabeledData.size(); i++) {
			DATA reference = this.listOfLabeledData.get(i).getData();
			distances[i] = distFunc.distance(data, reference);
		}
		return distances;
	}
	
	private class ComputeDistance implements Callable<Object> {

		private DATA data;
		private AtomicInteger sharedIndex;
		private Double[] distances;

		public ComputeDistance(DATA data, AtomicInteger sharedIndex, Double[] distances) {
			this.data = data;
			this.sharedIndex = sharedIndex;
			this.distances = distances;
		}

		@Override
		public Object call() throws Exception {
			while  (true) {
				int myIndex = sharedIndex.getAndIncrement();
				if (myIndex >= distances.length)
					break;	// done
				DATA reference = KNNDataSummaryClassifier.this.listOfLabeledData.get(myIndex).getData();
				distances[myIndex] = distFunc.distance(data, reference);
			}
			return null;
		}
		
	}
	protected Double[] computeDistancesParallel(DATA data) {
		Double[] distances = new Double[this.listOfLabeledData.size()];
		AtomicInteger sharedIndex = new AtomicInteger(0);
		List<ComputeDistance> cdList = new ArrayList<ComputeDistance>();
		for (int i=0 ; i<Runtime.getRuntime().availableProcessors(); i++) {
			ComputeDistance cd = new ComputeDistance(data, sharedIndex, distances);
			cdList.add(cd);
		}
		ExecutorService executor = ExecutorUtil.getPrioritizingSharedService();
		try {
			executor.invokeAll(cdList);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			distances = computeDistancesSerial(data);
		}
		return distances;
	}

	public Classification classify(String trainingLabel, List<DATA> data) {
			Map<String, Double> votes = new HashMap<>();
	
			for (DATA freqComponents : data) {
	//		freqComponentsList.parallelStream().map(freqComponents -> {
				Double[] distances = new Double[this.listOfLabeledData.size()];
				for (int i=0; i<this.listOfLabeledData.size(); i++) {
					DATA reference = this.listOfLabeledData.get(i).getData();
					distances[i] = distFunc.distance(freqComponents, reference);
				}
				
				ArrayIndexComparator comparator = new ArrayIndexComparator(distances);
				Integer[] indexes = comparator.createIndexArray();   //this contains indexes of sorted distances
				Arrays.sort(indexes, comparator);
				
				int minIndex = indexes[0];
				double minDist = distances[minIndex];
	
				String thisLabel = this.listOfLabeledData.get(minIndex).getLabel();
				
				double distNearestOtherLabel = -1;
				for (int i=0; i<indexes.length; i++) {
					String comparingLabel = this.listOfLabeledData.get(indexes[i]).getLabel();
					if (!thisLabel.equals(comparingLabel)) {
						distNearestOtherLabel = distances[i];
						break;
					}
				}
				
				if (distNearestOtherLabel < 0.00001) distNearestOtherLabel = 0.00001;
				
	//			synchronized(votes) {
					double votingWeight = Math.max(0, 1 - minDist/distNearestOtherLabel);  //weights between 0 and 1
					Double currentVotesForThisLabel = votes.get(thisLabel);
					if (currentVotesForThisLabel == null) {
						votes.put(thisLabel, votingWeight);
					} else {
						votes.put(thisLabel, currentVotesForThisLabel+votingWeight);
					}
	//			}
	//			return 0;
	//		}).collect(Collectors.toList());
			}
			
			
			//Count votes
			Set<String> labelsSet = votes.keySet();
			Double[] votesValues = new Double[labelsSet.size()];
			String[] labels = labelsSet.toArray(new String[0]);
			for (int i=0; i<labelsSet.size(); i++) {
				votesValues[i] = votes.get(labels[i]);
			}
			
			
			//Computation of the relative weight of each vote (for confidence value and ranking)
			double sumVoteWeights = 0.0;
			for (Double d : votesValues) {
				sumVoteWeights += d;
			}
			
			double[] voteRelativeWeights = new double[votesValues.length];
			for (int i=0; i<votesValues.length; i++) {
				if (sumVoteWeights > 0.0) 
					voteRelativeWeights[i] = votesValues[i] / sumVoteWeights;
				else
					voteRelativeWeights[i] = 1.0 / votesValues.length;  // If sumVoteWeights is zero (shouldn't usually happen), assign equal weights
			}
			
			
			//Rank and add ranking to classifications
			ArrayIndexComparator comparatorVotes = new ArrayIndexComparator(votesValues);
			Integer[] indexesVotes = comparatorVotes.createIndexArray();   //this contains indexes of sorted distances
			Arrays.sort(indexesVotes, comparatorVotes);
			
			List<LabelValue> rankedValues = new ArrayList<LabelValue>();
			for (int i=indexesVotes.length-1; i>=0; i--) {
	//			if (labels[indexesVotes[i]] == null) {
	//				throw new AISPException("Label value is null in classification.");
	//			}
				rankedValues.add(new LabelValue(labels[indexesVotes[i]], voteRelativeWeights[indexesVotes[i]]));
			}
			
			
			return new Classification(trainingLabel, rankedValues);
		}

}
