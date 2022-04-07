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
package org.eng.aisp.classifier.gmm;

import java.util.Arrays;
import java.util.Random;

import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.gaussianmixture.FixedSingleGaussianDiagCovariance;
import org.eng.aisp.classifier.gaussianmixture.FixedSingleGaussianFullCovariance;
import org.eng.aisp.classifier.gaussianmixture.FixedSingleGaussianMixture;
import org.eng.aisp.classifier.gaussianmixture.IFixedSingleGaussian;
import org.eng.aisp.classifier.gaussianmixture.FixedSingleGaussianMixture.PosteriorAndSumProb;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.util.MatrixUtil;

/**
 * Class for training one Gaussian mixture model.
 * The implementation of the EM algorithm is based on the following references:
 * <br>
 * [a] Douglas Reynolds, "Gaussian Mixture Models," {@link https://ll.mit.edu/mission/cybersec/publications/publication-files/full_papers/0802_Reynolds_Biometrics-GMM.pdf}
 * <br>
 * [b] Robert D. Nowak, "Distributed EM algorithms for density estimation and clustering in sensor networks." IEEE transactions on signal processing 51.8 (2003): 2245-2253.
 * 
 * @author wangshiq
 */
class GMMTrainingUtil {

    private static final int DEFAULT_SEED = 1443523423;

	/**
	 * The covariance matrix of each Gaussian model is initialized as a diagonal matrix with elements equal to this value.
	 * This value should be large enough to make the initial model smooth to avoid producing zero probabilities.
	 */
	public static final double INITIAL_VALUE_DIAGONAL_COVARIANCE = 1000; 
	
	/**
	 * The EM process terminates when the relative increment of the probability (likelihood) is smaller than this value.
	 */
	public static final double TERMINATION_REL_INCREMENT = 0.00001; 
	
	/**
	 * Maximum number of rounds of the EM algorithm.
	 */
	public static final int EM_MAX_ROUND = 100;
	
	/**
	 * Added value on the diagonal components of the covariance matrix, to avoid singularity.
	 */
	public static final double ADDED_VALUE_ON_DIAGONAL_COVARIANCE = 0.0001;
	
	
	
	public static FixedSingleGaussianMixture train(int dim, int numGaussiansToMix, boolean diagonalCovariance, Iterable<IFeature<double[]>> features) throws AISPException {
		Random random = new Random(DEFAULT_SEED);	// Produce repeatable results.
		
		double[] mixtureWeights = new double[numGaussiansToMix];
		IFixedSingleGaussian[] fixedModels = new IFixedSingleGaussian[numGaussiansToMix];
		
		//initial model
		double mixtureWeight = 1.0 / numGaussiansToMix;
		for (int m=0; m<numGaussiansToMix; m++) {
			double[] newMean = new double[dim];
			double[][] newCovariance = new double[dim][dim];
			for (int i=0; i<dim; i++) {
				newMean[i] = random.nextDouble() - 0.5;  //initialize each Gaussian model with random mean
				Arrays.fill(newCovariance[i], 0.0);
				newCovariance[i][i] = INITIAL_VALUE_DIAGONAL_COVARIANCE;
			}
			
			if (diagonalCovariance) {
				fixedModels[m] = new FixedSingleGaussianDiagCovariance(newMean, MatrixUtil.selectDiagonalComponents(newCovariance));
			} else{ 
				fixedModels[m] = new FixedSingleGaussianFullCovariance(newMean, newCovariance);
			}
			
			mixtureWeights[m] = mixtureWeight;  //initialize posterior probabilities to uniform distribution
		}
		
		
		double prevSumProb = 0.0;
		
		//Perform EM to learn Gaussian mixture model
		for (int emRound = 0; emRound < EM_MAX_ROUND; emRound++) {
			boolean isLastRound = false;
			
			FixedSingleGaussianMixture tmpModel = new FixedSingleGaussianMixture(mixtureWeights, fixedModels);
			
			//initialize arrays for new mean and covariance
			double[][] newMean = new double[numGaussiansToMix][dim];
			double[][][] newCovariance = new double[numGaussiansToMix][dim][dim];
			for (int n=0; n<numGaussiansToMix; n++) {
				Arrays.fill(newMean[n], 0.0);
				for (int i=0; i<dim; i++) {
					Arrays.fill(newCovariance[n][i], 0.0);
				}
			}
			
			//initialize array for sum of posterior probability over all samples
			double[] posteriorProbAllSamples = new double[numGaussiansToMix];  //for each model in the mixture
			Arrays.fill(posteriorProbAllSamples, 0.0);
			
			int sampleCount = 0;
			double sumProb = 0.0;
			
			for (IFeature<double[]> fe : features) {
				double[] fv = fe.getData();
				
				PosteriorAndSumProb posteriorAndSumProb = tmpModel.computePosteriorAndSumProb(fv);
				double[] posteriorProb = posteriorAndSumProb.getPosteriorProb();
				sumProb += posteriorAndSumProb.getSumProb();
				
				for (int n=0; n<numGaussiansToMix; n++) {
					
					//Compute the numerator for mean
					for (int i=0; i<Math.min(dim, fv.length); i++) {
						newMean[n][i] += posteriorProb[n] * fv[i];
					}
					
					//compute sum posterior probability for all samples
					posteriorProbAllSamples[n] += posteriorProb[n]; 
					

					
					//Compute first part of the numerator for covariance
					for (int i=0; i<Math.min(dim, fv.length); i++) {
						if (diagonalCovariance) {
							newCovariance[n][i][i] += posteriorProb[n] * fv[i] * fv[i];
						} else {
							for (int j=0; j<Math.min(dim, fv.length); j++) {
								newCovariance[n][i][j] += posteriorProb[n] * fv[i] * fv[j];
							}
						}
					}
				}
				
				
				sampleCount++;
			}
			if (sampleCount == 0)
				throw new AISPException("No features found to train on.  Was the correct label specified?");
			
			//Compute the final value of mean
			for (int n=0; n<numGaussiansToMix; n++) {
				for (int i=0; i<dim; i++) {
					//Note: We should use division directly when dealing with probabilities for improved precision, using 1.0/probability may result in infinity
					newMean[n][i] /= posteriorProbAllSamples[n];
				}
			}
			
			//Compute final value of covariance
			for (int n=0; n<numGaussiansToMix; n++) {
				if (diagonalCovariance) {
					for (int i=0; i<dim; i++) {
						newCovariance[n][i][i] /= posteriorProbAllSamples[n];   //Complete the first part of variance
						newCovariance[n][i][i] -= newMean[n][i] * newMean[n][i];  //Subtract the square of mean value to complete the variance computation
						newCovariance[n][i][i] += ADDED_VALUE_ON_DIAGONAL_COVARIANCE;  //To avoid singularity
					}
				} else {
					for (int i=0; i<dim; i++) {
						for (int j=0; j<dim; j++) {
							//Note: We should use division directly when dealing with probabilities for improved precision, using 1.0/probability may result in infinity
							newCovariance[n][i][j] /= posteriorProbAllSamples[n];   //Complete the first part of covariance
							newCovariance[n][i][j] -= newMean[n][i] * newMean[n][j];  //Subtract the cross product of mean value to complete the covariance computation
							if (i == j) {
								newCovariance[n][i][j] += ADDED_VALUE_ON_DIAGONAL_COVARIANCE;  //To avoid singularity
							}
						}
					}
				}
			}

			
			//Check whether this is the last EM round, based on termination condition
//			double sumProbRatio = sumProb / prevSumProb;
//			if ((prevSumProb > 0) && (sumProbRatio >= 1.0) && (sumProbRatio < 1.0 + TERMINATION_REL_INCREMENT)) {
//				//The case sumProbRatio < 1 won't occur in theory, but we see it happening in practice, perhaps due to numerical inaccuracy
//				isLastRound = true; 
//			} else if (numGaussiansToMix == 1) {
//				isLastRound = true;   //When there is only one Gaussian, then only one round is needed
//			}
			if (numGaussiansToMix == 1) {
				isLastRound = true; // When there is only one Gaussian, then only one round is needed
			} else if (prevSumProb == 0) {
				; // come here to avoid div by 0 below.
			} else {
				double sumProbRatio = sumProb / prevSumProb;
				if ((sumProbRatio >= 1.0) && (sumProbRatio < 1.0 + TERMINATION_REL_INCREMENT)) 
					//The case sumProbRatio < 1 won't occur in theory, but we see it happening in practice, perhaps due to numerical inaccuracy
					isLastRound = true;   
			}
			prevSumProb = sumProb;
			
			
			//Update the models and mixture weights (Note: this has to happen at the very end)
			double scale = 1.0 / sampleCount;
			for (int n=0; n<numGaussiansToMix; n++) {
				if (diagonalCovariance) {
					fixedModels[n] = new FixedSingleGaussianDiagCovariance(newMean[n], MatrixUtil.selectDiagonalComponents(newCovariance[n]));
				} else{ 
					fixedModels[n] = new FixedSingleGaussianFullCovariance(newMean[n], newCovariance[n]);
				}

				mixtureWeights[n] = scale * posteriorProbAllSamples[n];
			}
			
			if (isLastRound) break;
		}
		
		return new FixedSingleGaussianMixture(mixtureWeights, fixedModels);
	}
	

}
