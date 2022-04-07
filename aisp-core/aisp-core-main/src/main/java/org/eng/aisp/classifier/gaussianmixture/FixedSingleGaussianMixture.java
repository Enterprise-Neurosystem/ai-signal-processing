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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Class for a single Gaussian mixture model with multiple Gaussian models and mixture weights
 * @author wangshiq
 *
 */
public class FixedSingleGaussianMixture implements Serializable { 

	private static final long serialVersionUID = 3310227949203743645L;
	
	/**
	 * Minimum value of posterior probability, to avoid posterior probability to be equal to zero.
	 */
	public static final double MIN_POSTERIOR_PROBABILITY = Double.MIN_VALUE;

	private final double[] mixtureWeights;
	private final IFixedSingleGaussian[] fixedModels;
	private final int numGaussiansToMix;
	
	public FixedSingleGaussianMixture(double[] mixtureWeights, IFixedSingleGaussian[] fixedModels) {
		if (mixtureWeights.length != fixedModels.length) 
			throw new IllegalArgumentException("Lengths of mixtureWeights and fixedModels do not match.");
		
		this.numGaussiansToMix = mixtureWeights.length;
		this.mixtureWeights = mixtureWeights;
		this.fixedModels = fixedModels;
	}
	
	public static FixedSingleGaussianMixture merge(FixedSingleGaussianMixture ... models) {
		List<Double> mixtureWeightsList = new ArrayList<>();
		List<IFixedSingleGaussian> fixedModelsList = new ArrayList<>();
		
		for (FixedSingleGaussianMixture m : models) {
			for (int i=0; i<m.mixtureWeights.length; i++) {
				mixtureWeightsList.add(m.mixtureWeights[i]);
				fixedModelsList.add(m.fixedModels[i]);
			}
		}
		
		Double[] mixtureWeightsObject = mixtureWeightsList.toArray(new Double[0]);
		double[] mixtureWeights = new double[mixtureWeightsObject.length];
		double sum = 0.0;
		for (int i=0; i<mixtureWeights.length; i++) {
			mixtureWeights[i] = mixtureWeightsObject[i];
			sum += mixtureWeights[i];
		}
		if (sum != 0) {
			for (int i=0; i<mixtureWeights.length; i++) 
				mixtureWeights[i] /= sum;   // Normalize mixture weights to one
		}
		
		IFixedSingleGaussian[] fixedModels = fixedModelsList.toArray(new IFixedSingleGaussian[0]);
		
		return new FixedSingleGaussianMixture(mixtureWeights, fixedModels);
	}
	
	public double density(double[] sample) {
		double result = 0.0;
		for (int i=0; i<numGaussiansToMix; i++) {
			result += Math.max(MIN_POSTERIOR_PROBABILITY, mixtureWeights[i] * fixedModels[i].density(sample));
		}
		return result;
	}
	
	public class PosteriorAndSumProb {
		private final double[] posteriorProb;
		private final double sumProb;
		
		public PosteriorAndSumProb(double[] posteriorProb, double sumProb) {
			this.posteriorProb = posteriorProb;
			this.sumProb = sumProb;
		}
		public double[] getPosteriorProb() {
			return posteriorProb;
		}
		public double getSumProb() {
			return sumProb;
		}
	}
	
	public PosteriorAndSumProb computePosteriorAndSumProb(double[] fv) {
		//compute posterior probabilities
		double[] posteriorProb = new double[numGaussiansToMix];
		double sumProb = 0.0;
		for (int n=0; n<numGaussiansToMix; n++) {
			posteriorProb[n] = Math.max(MIN_POSTERIOR_PROBABILITY, mixtureWeights[n] * fixedModels[n].density(fv));
			sumProb += posteriorProb[n];
		}
		if (sumProb != 0) {
			for (int n=0; n<numGaussiansToMix; n++) {
				//Note: We should use division directly when dealing with probabilities for improved precision, using 1.0/probability may result in infinity
				posteriorProb[n] /= sumProb;  //compute final posterior probability for the current feature (sample)
			}
		}
		
		return new PosteriorAndSumProb(posteriorProb, sumProb);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return "FixedSingleGaussianMixture [numGaussiansToMix=" + numGaussiansToMix + ", mixtureWeights="
				+ (mixtureWeights != null
						? Arrays.toString(Arrays.copyOf(mixtureWeights, Math.min(mixtureWeights.length, maxLen)))
						: null)
				+ ", fixedModels=" + (fixedModels != null
						? Arrays.asList(fixedModels).subList(0, Math.min(fixedModels.length, maxLen)) : null)
				+ "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(fixedModels);
		result = prime * result + Arrays.hashCode(mixtureWeights);
		result = prime * result + numGaussiansToMix;
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
		if (!(obj instanceof FixedSingleGaussianMixture))
			return false;
		FixedSingleGaussianMixture other = (FixedSingleGaussianMixture) obj;
		if (!Arrays.equals(fixedModels, other.fixedModels))
			return false;
		if (!Arrays.equals(mixtureWeights, other.mixtureWeights))
			return false;
		if (numGaussiansToMix != other.numGaussiansToMix)
			return false;
		return true;
	}
}
