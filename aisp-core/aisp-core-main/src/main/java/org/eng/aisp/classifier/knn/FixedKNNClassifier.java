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

import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.AbstractFixedFeatureExtractingClassifier;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.feature.FeatureGramNormalizer;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.IFeatureGramDescriptor;

/**
 * An untrainable (i.e. fixed) version of a nearest neighbor classifier.
 * @author wangshiq
 * @author dawood
 *
 */
public class FixedKNNClassifier  extends AbstractFixedFeatureExtractingClassifier<double[], double[]>  implements IFixedClassifier<double[]> {
	
	
	private static final long serialVersionUID = 3690410137589575110L;

	private final String primaryTrainingLabel;
//	private final double stdDevFactor;		// TODO: is this field needed?
	private final double maxDistAmplifyFactor; 
//	private final double maxDistBetweenSameLabel; //not considering max. dist if < 0
//	private final double lowerBoundDelta;
//	private final List<ILabeledFeature<double[]>> listOfLabeledFeatures;
	private final INearestNeighborFunction<double[]> nnFunc;

	private KNNDataSummaryClassifier<double[]> dataSummary;
	protected final FeatureGramNormalizer	normalizer;

	public FixedKNNClassifier(String primaryTrainingLabel, List<IFeatureGramDescriptor<double[], double[]>> fgeList, 
			double maxDistAmplifyFactor, INearestNeighborFunction<double[]> nnFunc, KNNDataSummaryClassifier<double[]> dataSummary, 
			FeatureGramNormalizer normalizer) {
		super(fgeList);
		this.primaryTrainingLabel = primaryTrainingLabel;
		this.nnFunc = nnFunc;
		this.maxDistAmplifyFactor = maxDistAmplifyFactor;
		this.dataSummary = dataSummary;
		this.normalizer = normalizer;
	}

	@Override
	protected List<Classification> classify(IFeatureGram<double[]>[] featureGrams) throws AISPException {
		if (featureGrams.length > 1)
			throw new IllegalArgumentException("More than 1 feature gram is not supported");
		if (normalizer != null) 
			featureGrams = normalizer.normalize(featureGrams);

		List<Classification> classifications = new ArrayList<Classification>();
		IFeature<double[]>[] features = featureGrams[0].getFeatures();
		if (features.length == 0) { // can happen if clip is shorter than subwindow
			classifications.add(new Classification(this.primaryTrainingLabel, Classification.UndefinedLabelValue, 1));
			return classifications;
		}
		
		List<double[]> freqComponentsList = nnFunc.featurePreProcessing(features);
		
		//TODO The with and without voting cases may be combined in a better way.
		if (freqComponentsList.size()==1) {
			//No voting
			
			double [] freqComponents = freqComponentsList.get(0);
//			
//			Double[] distances = new Double[this.listOfLabeledFeatures.size()];
//			for (int i=0; i<this.listOfLabeledFeatures.size(); i++) {
//				double[] reference = this.listOfLabeledFeatures.get(i).getFeature().getData();
//				distances[i] = nnFunc.distance(freqComponents, reference);
//			}
//			
//			ArrayIndexComparator comparator = new ArrayIndexComparator(distances);
//			Integer[] indexes = comparator.createIndexArray();   //this contains indexes of sorted distances
//			Arrays.sort(indexes, comparator);
//			
//			int minIndex = indexes[0];
//			double minDist = distances[minIndex];
//			
//			
//			//find min. distances for all labels
//			List<String> labels = new ArrayList<String>();
//			List<Double> minDistAllLabels = new ArrayList<Double>();
//			
//			for (int i=0; i<indexes.length; i++) {
//				String thisLabel = this.listOfLabeledFeatures.get(indexes[i]).getLabels().getProperty(primaryTrainingLabel);
//				
//				boolean labelExists = false;
//				for (String l : labels) {
//					if (l.equals(thisLabel)) {
//						labelExists = true;
//						break;
//					}
//				}
//				
//				if (labelExists == false) {
//					labels.add(thisLabel);
//					minDistAllLabels.add(distances[indexes[i]]);
//				}
//			}
//			
//			//softmax computation
//			
//			double maxMinDist = 0.0;
//			for (Double d : minDistAllLabels) {
//				if (d > maxMinDist) maxMinDist = d;
//			}
//			if (maxMinDist==0.0) maxMinDist = 0.0001;
//	
//			
//			double sumExpMinDists = 0.0;
//			for (Double d : minDistAllLabels) {
//				sumExpMinDists += Math.exp(-d/maxMinDist);
//			}
//			if (sumExpMinDists==0.0) sumExpMinDists = 0.0001;
//			
//			double[] softMaxValues = new double[minDistAllLabels.size()];
//			for (int i=0; i<minDistAllLabels.size(); i++) {
//				softMaxValues[i] = Math.exp(-minDistAllLabels.get(i)/maxMinDist) / sumExpMinDists;
//			}
//			
//			String status;
//			if(this.maxDistBetweenSameLabel < 0 || minDist < this.maxDistBetweenSameLabel * this.maxDistAmplifyFactor) {
//				status = this.listOfLabeledFeatures.get(minIndex).getLabels().getProperty(primaryTrainingLabel);
//				if (status == null)
//					throw new AISPException("Training on label '" + primaryTrainingLabel + "' which is not found in training data");
//			} else {
//				status = Classification.UndefinedLabelValue;
//			}
//				
//			double confidenceBaseKnown;
//			double confidenceUnknown;
//			if (!Classification.UndefinedLabelValue.equals(status)) {
//				confidenceBaseKnown = Math.max(0.5, 1.0 - Math.max(0.0, minDist - this.lowerBoundDelta)/this.maxDistBetweenSameLabel);
//				confidenceUnknown = 1-confidenceBaseKnown;
//			} else { 
//				confidenceUnknown = Math.max(0.5, Math.min(1.0, minDist/this.maxDistBetweenSameLabel - 1.0));
//				confidenceBaseKnown = 1-confidenceUnknown;
//			}
//			
//			
//			List<LabelValue> rankedValues = new ArrayList<LabelValue>();
//			if (!Classification.UndefinedLabelValue.equals(status)) {
//				for (int i=0; i<labels.size(); i++) {
//					rankedValues.add(new LabelValue(labels.get(i), softMaxValues[i] * confidenceBaseKnown));
//				}
//				rankedValues.add(new LabelValue(Classification.UndefinedLabelValue, confidenceUnknown));
//			} else {
//				rankedValues.add(new LabelValue(Classification.UndefinedLabelValue, confidenceUnknown));
//				for (int i=0; i<labels.size(); i++) {
//					rankedValues.add(new LabelValue(labels.get(i), softMaxValues[i] * confidenceBaseKnown));
//				}
//			}
	
//			classifications.add(new Classification(primaryTrainingLabel, rankedValues));	
			classifications.add(dataSummary.classify(this.primaryTrainingLabel, freqComponents));
		} else {
			//With voting

//			Map<String, Double> votes = new HashMap<>();
//
//			for (double[] freqComponents : freqComponentsList) {
////			freqComponentsList.parallelStream().map(freqComponents -> {
//				Double[] distances = new Double[this.listOfLabeledFeatures.size()];
//				for (int i=0; i<this.listOfLabeledFeatures.size(); i++) {
//					double[] reference = this.listOfLabeledFeatures.get(i).getFeature().getData();
//					distances[i] = nnFunc.distance(freqComponents, reference);
//				}
//				
//				ArrayIndexComparator comparator = new ArrayIndexComparator(distances);
//				Integer[] indexes = comparator.createIndexArray();   //this contains indexes of sorted distances
//				Arrays.sort(indexes, comparator);
//				
//				int minIndex = indexes[0];
//				double minDist = distances[minIndex];
//
//				String thisLabel = this.listOfLabeledFeatures.get(minIndex).getLabels().getProperty(primaryTrainingLabel);
//				
//				double distNearestOtherLabel = -1;
//				for (int i=0; i<indexes.length; i++) {
//					String comparingLabel = this.listOfLabeledFeatures.get(indexes[i]).getLabels().getProperty(primaryTrainingLabel);
//					if (!thisLabel.equals(comparingLabel)) {
//						distNearestOtherLabel = distances[i];
//						break;
//					}
//				}
//				
//				if (distNearestOtherLabel < 0.00001) distNearestOtherLabel = 0.00001;
//				
////				synchronized(votes) {
//					double votingWeight = Math.max(0, 1 - minDist/distNearestOtherLabel);  //weights between 0 and 1
//					Double currentVotesForThisLabel = votes.get(thisLabel);
//					if (currentVotesForThisLabel == null) {
//						votes.put(thisLabel, votingWeight);
//					} else {
//						votes.put(thisLabel, currentVotesForThisLabel+votingWeight);
//					}
////				}
////				return 0;
////			}).collect(Collectors.toList());
//			}
//			
//			
//			//Count votes
//			Set<String> labelsSet = votes.keySet();
//			Double[] votesValues = new Double[labelsSet.size()];
//			String[] labels = labelsSet.toArray(new String[0]);
//			for (int i=0; i<labelsSet.size(); i++) {
//				votesValues[i] = votes.get(labels[i]);
//			}
//			
//			
//			//Computation of the relative weight of each vote (for confidence value and ranking)
//			double sumVoteWeights = 0.0;
//			for (Double d : votesValues) {
//				sumVoteWeights += d;
//			}
//			if (sumVoteWeights==0.0) sumVoteWeights = 0.0001;
//			
//			double[] voteRelativeWeights = new double[votesValues.length];
//			for (int i=0; i<votesValues.length; i++) {
//				voteRelativeWeights[i] = votesValues[i] / sumVoteWeights;
//			}
//			
//			
//			//Rank and add ranking to classifications
//			ArrayIndexComparator comparatorVotes = new ArrayIndexComparator(votesValues);
//			Integer[] indexesVotes = comparatorVotes.createIndexArray();   //this contains indexes of sorted distances
//			Arrays.sort(indexesVotes, comparatorVotes);
//			
//			List<LabelValue> rankedValues = new ArrayList<LabelValue>();
//			for (int i=indexesVotes.length-1; i>=0; i--) {
//				if (labels[indexesVotes[i]] == null) {
//					throw new AISPException("Label value is null in classification.");
//				}
//				rankedValues.add(new LabelValue(labels[indexesVotes[i]], voteRelativeWeights[indexesVotes[i]]));
//			}
//			
//			
//			classifications.add(new Classification(primaryTrainingLabel, rankedValues));
			classifications.add(dataSummary.classify(this.primaryTrainingLabel, freqComponentsList));
		}
		
		return classifications;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return "FixedKNNClassifier [primaryTrainingLabel=" + primaryTrainingLabel
				+ ", featureGramDescriptors=" 
					+ (featureGramDescriptors != null ? featureGramDescriptors.subList(0, Math.min(featureGramDescriptors.size(), maxLen)) : null)
				+ ", maxDistAmplifyFactor=" + maxDistAmplifyFactor + ", nnFunc=" + nnFunc + ", dataSummary=" + dataSummary 
				+ "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((dataSummary == null) ? 0 : dataSummary.hashCode());
		long temp;
		temp = Double.doubleToLongBits(maxDistAmplifyFactor);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((nnFunc == null) ? 0 : nnFunc.hashCode());
		result = prime * result + ((primaryTrainingLabel == null) ? 0 : primaryTrainingLabel.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof FixedKNNClassifier))
			return false;
		FixedKNNClassifier other = (FixedKNNClassifier) obj;
		if (dataSummary == null) {
			if (other.dataSummary != null)
				return false;
		} else if (!dataSummary.equals(other.dataSummary))
			return false;
		if (Double.doubleToLongBits(maxDistAmplifyFactor) != Double.doubleToLongBits(other.maxDistAmplifyFactor))
			return false;
		if (nnFunc == null) {
			if (other.nnFunc != null)
				return false;
		} else if (!nnFunc.equals(other.nnFunc))
			return false;
		if (primaryTrainingLabel == null) {
			if (other.primaryTrainingLabel != null)
				return false;
		} else if (!primaryTrainingLabel.equals(other.primaryTrainingLabel))
			return false;
		return true;
	}

	@Override
	public String getTrainedLabel() {
		return this.primaryTrainingLabel;
	}

}
