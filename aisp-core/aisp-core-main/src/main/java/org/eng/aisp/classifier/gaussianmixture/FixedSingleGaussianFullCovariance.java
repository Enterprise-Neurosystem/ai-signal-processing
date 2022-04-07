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

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

public class FixedSingleGaussianFullCovariance implements IFixedSingleGaussian { 
/**
 * @author wangshiq
 * Gaussian model for full covariance matrix that computes the pseudo-inverse covariance matrix.
 */
	private static final long serialVersionUID = -6992013373781896673L;
	
	private final double coeffLog;
	private final int dim;
	private final double[] mean;
	private final double[][] covarianceInv;
	
	public FixedSingleGaussianFullCovariance(double[] mean, double[][] covariance) {
		this.mean = mean;
		dim = mean.length;
		
		if (!((mean.length == covariance.length)
				&& (covariance.length == covariance[0].length))) {
			throw new IllegalArgumentException("mean/covariance matrix dimensions mismatch");
		}
		
		RealMatrix covMat = new Array2DRowRealMatrix(covariance);
		
		SingularValueDecomposition svd = new SingularValueDecomposition(covMat);
		RealMatrix UT = svd.getUT();
		RealMatrix S = svd.getS();
		RealMatrix V = svd.getV();
		
		//Construct pseudo-inverse
		for (int i=0; i<S.getRowDimension(); i++) {
			for (int j=0; j<S.getColumnDimension(); j++) {
				if (S.getEntry(i, j) != 0) {
					S.setEntry(i, j, 1.0/S.getEntry(i, j));
				}
			}
		}
		
		RealMatrix covInvMat = V.multiply(S.transpose()).multiply(UT);
		this.covarianceInv = covInvMat.getData();

		double covInvMatDetSqrtLog = 0.5 * Math.log(new LUDecomposition(covInvMat).getDeterminant());
		this.coeffLog = covInvMatDetSqrtLog - (double)dim/2.0 * Math.log(2 * Math.PI);
	}

	@Override
	public double density(double[] sample) {
		
		double tmpSum = 0.0;
		for (int i=0; i<Math.min(covarianceInv.length, sample.length); i++) {
			double diff1 = sample[i] - mean[i];
			for (int j=0; j<Math.min(covarianceInv[0].length, sample.length); j++) {
				double diff2 = sample[j] - mean[j];
				tmpSum += diff1 * covarianceInv[i][j] * diff2;
			}
		}
		
		double exponent = -0.5 * tmpSum;
		
		return Math.exp(coeffLog + exponent);
		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(coeffLog);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + Arrays.deepHashCode(covarianceInv);
		result = prime * result + dim;
		result = prime * result + Arrays.hashCode(mean);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof FixedSingleGaussianFullCovariance))
			return false;
		FixedSingleGaussianFullCovariance other = (FixedSingleGaussianFullCovariance) obj;
		if (Double.doubleToLongBits(coeffLog) != Double.doubleToLongBits(other.coeffLog))
			return false;
		if (!Arrays.deepEquals(covarianceInv, other.covarianceInv))
			return false;
		if (dim != other.dim)
			return false;
		if (!Arrays.equals(mean, other.mean))
			return false;
		return true;
	}
}
