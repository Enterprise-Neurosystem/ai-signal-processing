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
package org.eng.util;

import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * Enabled the transformation of a value in one distribution to a value in a 2nd distribution using a linear transform. 
 * See <a href="https://proofwiki.org/wiki/Linear_Transformation_of_Gaussian_Random_Variable">https://proofwiki.org/wiki/Linear_Transformation_of_Gaussian_Random_Variable</a> 
 * for the basis of implementation here. 
 * Assuming,
 * <pre>
 * destValue = m * srcValue + b
 * </pre>
 * To determine m and b, we assume the following
 * <pre>
 * srcN[Us, Vs] = src distribution with mean Us and variance Vs.
 * destN[Ud,Vd] = dest distribution with mean Ud and variance Vd.
 * </pre>
 * To map a value in srcN[] to a value in destN[], we can determine
 * m and b from the following:
 * with 
 * <pre>
 * destN[Ud, Vd] = srcN[m * Us + b, m^2 * Vs]
 * </pre>
 * so that,
 * <pre>
 * m^2 * Vs = Vd, so that  m = sqrt(Vd/Vs)
 * m * Us + b = Ud, so that b = Ud - m * Us
 * </pre> 
 * and finally, from above,
 * <pre>
 * destValue = m * srcValue + b
 * </pre>
 * @author DavidWood
 *
 */
public class LinearDistributionTransform {
	
	final double slope, offset;
	
	public LinearDistributionTransform(NormalDistribution srcDist, NormalDistribution destDist) {
		this(srcDist.getMean(), srcDist.getNumericalVariance(), destDist.getMean(), destDist.getNumericalVariance());
	}

	public LinearDistributionTransform(OnlineStats srcStats, OnlineStats destStats) {
		this(srcStats.getMean(), srcStats.getVariance(), destStats.getMean(), destStats.getVariance());
	}

	public LinearDistributionTransform(double srcMean, double srcVariance, double destMean, double destVariance) {
		if (srcVariance == 0)
			throw new IllegalArgumentException("source variance can not be 0");
		this.slope = Math.sqrt(destVariance/srcVariance);
		this.offset = destMean - this.slope * srcMean; 
	}
	
	/**
	 * Transform the given value in the source distribution to the destination distribution.
	 * @param srcValue
	 * @return value transformed into the destination distribution.
	 */
	public double transform(double srcValue) {
		double destValue = this.slope * srcValue + this.offset;
		return destValue;
	}
	
	public static void main(String[] args) {
		OnlineStats src = new OnlineStats();
		OnlineStats dest1 = new OnlineStats();
		OnlineStats dest2 = new OnlineStats();
		for (int i=0 ; i<=10 ; i++)
			src.addSample(i);
		for (int i=100 ; i<=200 ; i++)
			dest1.addSample(i);
		for (int i=-100 ; i<=0 ; i++)
			dest2.addSample(i);
		LinearDistributionTransform ldt1 = new LinearDistributionTransform(src, dest1);
		for (int i=0 ; i<=10 ; i++) {
			double x = ldt1.transform(i);
			System.out.println("Transfered i=" + i + " to dest1 " + x);
		}
		LinearDistributionTransform ldt2 = new LinearDistributionTransform(src, dest2);
		for (int i=0 ; i<=10 ; i++) {
			double x = ldt2.transform(i);
			System.out.println("Transfered i=" + i + " to dest2 " + x);
		}
	}	
}
	
