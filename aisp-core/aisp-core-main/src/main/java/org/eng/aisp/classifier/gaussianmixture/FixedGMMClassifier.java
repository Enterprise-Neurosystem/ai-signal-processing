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
package org.eng.aisp.classifier.gaussianmixture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.AbstractFixedFeatureExtractingClassifier;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.classifier.Classification.LabelValue;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.util.ArrayIndexComparator;

/**
 * Implements an untrainable fixed Gaussian Mixture Model suitable for use on the edge.
 * @author dawood
 *
 */
public class FixedGMMClassifier extends AbstractFixedFeatureExtractingClassifier<double[], double[]>  implements IFixedClassifier<double[]> {

	/**
	 * Fixed Gaussian Mixture Classifier.
	 * @author wangshiq
	 *
	 */

	private static final long serialVersionUID = -2603535640873951986L;

	private final String primaryTrainingLabel;
    private final List<String> listOfLabelValues;
    private final List<FixedSingleGaussianMixture> listOfModels;
    private final double unknownThreshold;

	public FixedGMMClassifier(String primaryTrainingLabel,
			List<IFeatureGramDescriptor<double[], double[]>> featureGramExtractors,
			List<String> listOfLabelValues, List<FixedSingleGaussianMixture> listOfModels, double unknownThreshold) {
		super(featureGramExtractors);
		if (listOfLabelValues.size() != listOfModels.size())
			throw new IllegalArgumentException("Lengths of listOfLabelValues and listOfModels do not match.");
		
		this.primaryTrainingLabel = primaryTrainingLabel;
		this.listOfLabelValues = listOfLabelValues;
		this.listOfModels = listOfModels;
		this.unknownThreshold = unknownThreshold;
	}

	public static FixedGMMClassifier merge(FixedGMMClassifier ... classifiers) throws AISPException {
		List<IFeatureGramDescriptor<double[], double[]>> featureGramExtractors = new ArrayList<>();
		String primaryTrainingLabel = null;
		List<String> listOfLabelValues = new ArrayList<>();
		List<FixedSingleGaussianMixture> listOfModels = new ArrayList<>();
		double unknownThreshold = 0.0;
		
		int numClassifiers = 0;
		for (FixedGMMClassifier c : classifiers) {
			if (featureGramExtractors.size() == 0) {
				featureGramExtractors.addAll(c.featureGramDescriptors);
			} else{
				if (!featureGramExtractors.equals(c.featureGramDescriptors))
					throw new AISPException("featureGramDescriptors of models to be merged must be equal");
			}
			
			if (primaryTrainingLabel == null) {
				primaryTrainingLabel = c.primaryTrainingLabel;
			} else {
				if (!primaryTrainingLabel.equals(c.primaryTrainingLabel))
					throw new AISPException("primaryTrainingLabel of models to be merged must be equal");
			}
			
			List<String> tmpListOfLabelValues = c.listOfLabelValues;
			List<FixedSingleGaussianMixture> tmpListOfModels = c.listOfModels;
			
			for (int i=0; i<tmpListOfLabelValues.size(); i++) {
				int index = listOfLabelValues.indexOf(tmpListOfLabelValues.get(i));
				if (index < 0) {
					listOfLabelValues.add(tmpListOfLabelValues.get(i));
					listOfModels.add(tmpListOfModels.get(i));
				} else {
					FixedSingleGaussianMixture curModel = listOfModels.get(index);
					FixedSingleGaussianMixture newModel = tmpListOfModels.get(i);
					listOfModels.set(index, FixedSingleGaussianMixture.merge(curModel, newModel));
				}
			}
			
			unknownThreshold += c.unknownThreshold;
			numClassifiers++;
		}
		
		if (numClassifiers != 0)
			unknownThreshold /= numClassifiers;
		
		return new FixedGMMClassifier(primaryTrainingLabel, featureGramExtractors, listOfLabelValues, listOfModels, unknownThreshold);
		
	}

	@Override
	protected List<Classification> classify(IFeatureGram<double[]>[] featureGrams) throws AISPException {
		if (featureGrams.length > 1)
			throw new IllegalArgumentException("More than 1 feature gram is not supported");
		IFeature<double[]>[] feSingleArray = featureGrams[0].getFeatures();
		double[] sumLogLikelihoodsPerLabel = new double[listOfLabelValues.size()];
		Arrays.fill(sumLogLikelihoodsPerLabel, 0.0);
		
		double sumLogDensityOverall = 0.0;
		
//		int aboveThreshProbCount = 0;

		for (int j=0; j<feSingleArray.length; j++) {
			double densityOverall = 0.0;
			double[] densityEachLabel = new double[sumLogLikelihoodsPerLabel.length];
			
			for (int i=0; i<listOfLabelValues.size(); i++) {
				densityEachLabel[i] = listOfModels.get(i).density(feSingleArray[j].getData());
				densityOverall += densityEachLabel[i];
			}
			

			double logDensityOverall = Math.log(densityOverall);
			sumLogDensityOverall += logDensityOverall;

			for (int i=0; i<listOfLabelValues.size(); i++) {
				sumLogLikelihoodsPerLabel[i] += Math.log(densityEachLabel[i]) - logDensityOverall;
			}

		}
		
		Double[] avrLogLikelihoodsPerLabel = new Double[sumLogLikelihoodsPerLabel.length];
		for (int i=0; i<avrLogLikelihoodsPerLabel.length; i++) {
			avrLogLikelihoodsPerLabel[i] = sumLogLikelihoodsPerLabel[i]/feSingleArray.length;
		}

		double avrLogDensityOverall = sumLogDensityOverall / feSingleArray.length;

		
		List<Classification> classifications = new ArrayList<Classification>();
//		System.out.println("avrLogDensityOverall: " + avrLogDensityOverall);
//		System.out.println("unknownThreshold: " + unknownThreshold);

		if (avrLogDensityOverall > unknownThreshold) {
			ArrayIndexComparator comparator = new ArrayIndexComparator(avrLogLikelihoodsPerLabel);
			Integer[] indexes = comparator.createIndexArray();   //this contains indexes of sorted distances
			Arrays.sort(indexes, comparator);
			
			List<LabelValue> rankedValues = new ArrayList<LabelValue>();
			
			double sum = 0.0;
			for (int i=0; i<avrLogLikelihoodsPerLabel.length; i++) {
				sum += Math.exp(avrLogLikelihoodsPerLabel[i]);
			}
			if (sum == 0.0) sum = 0.00001;
			
			for (int i=0; i<indexes.length; i++) {
				int j= indexes[indexes.length - i - 1];  //The array was sorted in ascending order, thus reverse indexing
				double conf;
				if (sum == 0 || feSingleArray.length == 0)
					conf = 0;
				else 
					conf = Math.exp(avrLogLikelihoodsPerLabel[j])/sum;
				rankedValues.add(new LabelValue(listOfLabelValues.get(j), conf)) ;
			}
			
	
			classifications.add(new Classification(primaryTrainingLabel, rankedValues));	
		} else { //Model gives zero probability on all subfeatures. Output undefined label value.

			//Shift density value so that the "unknown threshold" is considered as 100, may need to find a better way for this later
			double confidenceSubtract = avrLogDensityOverall + (100.0 - unknownThreshold) / 100;  
			
			if (confidenceSubtract > 1.0) confidenceSubtract = 1.0;
			else if (confidenceSubtract < 0.0) confidenceSubtract = 0.0;
			
			classifications.add(new Classification(primaryTrainingLabel, Classification.UndefinedLabelValue, 
					1.0 - confidenceSubtract));	
		}
		
		return classifications;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((listOfLabelValues == null) ? 0 : listOfLabelValues.hashCode());
		result = prime * result + ((listOfModels == null) ? 0 : listOfModels.hashCode());
		result = prime * result + ((primaryTrainingLabel == null) ? 0 : primaryTrainingLabel.hashCode());
		long temp;
		temp = Double.doubleToLongBits(unknownThreshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		if (!(obj instanceof FixedGMMClassifier))
			return false;
		FixedGMMClassifier other = (FixedGMMClassifier) obj;
		if (listOfLabelValues == null) {
			if (other.listOfLabelValues != null)
				return false;
		} else if (!listOfLabelValues.equals(other.listOfLabelValues))
			return false;
		if (listOfModels == null) {
			if (other.listOfModels != null)
				return false;
		} else if (!listOfModels.equals(other.listOfModels))
			return false;
		if (primaryTrainingLabel == null) {
			if (other.primaryTrainingLabel != null)
				return false;
		} else if (!primaryTrainingLabel.equals(other.primaryTrainingLabel))
			return false;
		if (Double.doubleToLongBits(unknownThreshold) != Double.doubleToLongBits(other.unknownThreshold))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return "FixedGMMClassifier [featureGramDescriptors="
				+ (featureGramDescriptors != null
						? featureGramDescriptors.subList(0, Math.min(featureGramDescriptors.size(), maxLen)) : null)
				+ ", primaryTrainingLabel=" + primaryTrainingLabel + ", unknownThreshold=" + unknownThreshold
				+ ", listOfLabelValues="
				+ (listOfLabelValues != null ? listOfLabelValues.subList(0, Math.min(listOfLabelValues.size(), maxLen))
						: null)
				+ ", listOfModels="
				+ (listOfModels != null ? listOfModels.subList(0, Math.min(listOfModels.size(), maxLen)) : null) + "]";
	}

	@Override
	public String getTrainedLabel() {
		return this.primaryTrainingLabel;
	}


}
