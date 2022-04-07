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

import java.util.Arrays;

public class FixedSingleGaussianDiagCovariance implements IFixedSingleGaussian { 
/**
 * @author wangshiq
 * Gaussian model for diagonal covariance matrix with simplified calculations.
 */
	private static final long serialVersionUID = 7530756173819191183L;
	
	private final double[] mean;
	private final double[] diagVariance;
	private final int dim;
	private final double coeffLog;
	private final double[] diagVarianceInv;
	
	public FixedSingleGaussianDiagCovariance(double[] mean, double[] diagVariance) {
		if (mean.length != diagVariance.length) {
			throw new IllegalArgumentException("mean and diagVariance must have same length.");
		}
		this.mean = mean;
		this.diagVariance = diagVariance;
		this.dim = mean.length;
		this.diagVarianceInv = new double[dim];
		
		double tmpSumInvVar = 0.0;
		for (int i=0; i<dim; i++) {
			this.diagVarianceInv[i] = 1.0 / diagVariance[i];
			tmpSumInvVar -= 0.5 * Math.log(this.diagVariance[i]);
		}
		this.coeffLog = tmpSumInvVar - (double)dim/2.0 * Math.log(2 * Math.PI);
	}
	
	@Override
	public double density(double[] sample) {
//		if (dim != sample.length)
//			throw new IllegalArgumentException("sample is not the expected length");
		double exponent = 0.0;
		for (int i=0; i<Math.min(dim, sample.length); i++) {
			double diff = sample[i] - mean[i];
			exponent -= 0.5 * diff * diff  * diagVarianceInv[i];
		}
		
		return Math.exp(coeffLog + exponent);
		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(coeffLog);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + Arrays.hashCode(diagVariance);
		result = prime * result + Arrays.hashCode(diagVarianceInv);
		result = prime * result + dim;
		result = prime * result + Arrays.hashCode(mean);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof FixedSingleGaussianDiagCovariance))
			return false;
		FixedSingleGaussianDiagCovariance other = (FixedSingleGaussianDiagCovariance) obj;
		if (Double.doubleToLongBits(coeffLog) != Double.doubleToLongBits(other.coeffLog))
			return false;
		if (!Arrays.equals(diagVariance, other.diagVariance))
			return false;
		if (!Arrays.equals(diagVarianceInv, other.diagVarianceInv))
			return false;
		if (dim != other.dim)
			return false;
		if (!Arrays.equals(mean, other.mean))
			return false;
		return true;
	}
	
}
