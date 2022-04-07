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
package org.eng.aisp.classifier.anomaly.normal;

import org.apache.commons.math3.distribution.RealDistribution;
import org.eng.util.OnlineStats;

/**
 * Holds some methods on OnlineStats that may be candidates for moving there.
 * @author DavidWood
 *
 */
public class StatUtils {

	/**
	 * Find the point between the two distributions that is an equal number of standard deviations
	 * away from each of the means.
	 * Find d, such that
	 *    m1 + d*s1 = m2 - d*s2, where m is the mean, s is the standard deviation and m1<m2
	 * then 
	 *    d = (m2 - m1) / (s2 + s1)
	 * Then compute the point as m1 + d * s1;
	 * @param normStats
	 * @param abnormStats
	 * @return
	 */
	public static double getEqualProbabiltyMidPoint(OnlineStats stats1, OnlineStats stats2) {
		double m1 = stats1.getMean();
		double m2 = stats2.getMean();
		double s1 = stats1.getStdDev();
		double s2 = stats2.getStdDev();
		double d;
		if (m1 < m2) {
			d = (m2 - m1) / (s1 + s2);
			d = m1 + d * s1;
		} else {
			d = (m1 - m2) / (s1 + s2);
			d = m2 + d * s2;
		}
//		RealDistribution dist1 = new NormalDistribution(m1, s1);
//		RealDistribution dist2 = new NormalDistribution(m2, s2);
//		AISPLogger.logger.info("d=" + d + ", cdf1=" + dist1.cumulativeProbability(d) + ", cdf2=" 
//					+ dist2.cumulativeProbability(d));
		return d;
	}

//	/**
//	 * Transform a value in one distribution to a value in a 2nd distribution using a linear transform. 
//	 * See <a href="https://proofwiki.org/wiki/Linear_Transformation_of_Gaussian_Random_Variable">https://proofwiki.org/wiki/Linear_Transformation_of_Gaussian_Random_Variable</a> 
//	 * for the basis of implementation here. 
//	 * Assuming,
//	 * <pre>
//	 * destValue = m * srcValue + b
//	 * </pre>
//	 * To determine m and b, we assume the following
//	 * <pre>
//	 * srcN[Us, Vs] = src distribution with mean Us and variance Vs.
//	 * destN[Ud,Vd] = dest distribution with mean Ud and variance Vd.
//	 * </pre>
//	 * To map a value in srcN[] to a value in destN[], we can determine
//	 * m and b from the following:
//	 * with 
//	 * <pre>
//	 * destN[Ud, Vd] = srcN[m * Us + b, m^2 * Vs]
//	 * </pre>
//	 * so that,
//	 * <pre>
//	 * m^2 * Vs = Vd, so that  m = sqrt(Vd/Vs)
//	 * m * Us + b = Ud, so that b = Ud - m * Us
//	 * </pre> 
//	 * and finally, from above,
//	 * <pre>
//	 * destValue = m * srcValue + b
//	 * </pre>
//	 * @param value
//	 * @param srcStats
//	 * @param destStats
//	 * @return a value mapped from the src distribution to the destination distribution assuming a linear transform between distributions.
//	 */
//	public static double transfer(double srcValue, OnlineStats srcStats, OnlineStats destStats) {
//		if (srcStats.getSampleCount() == 0)
//			throw new IllegalArgumentException("src stats has no samples");
//		if (destStats.getSampleCount() == 0)
//			throw new IllegalArgumentException("dest stats has no samples");
//		double Vd = destStats.getVariance();
//		double Vs = srcStats.getVariance();
//		double Ud  = destStats.getMean();
//		double Us  = srcStats.getMean();
//		double m = Math.sqrt(Vd/Vs);
//		double b = Ud - m * Us; 
//		double destValue = m * srcValue + b;
//		return destValue;
//	}
	


	public static double getEqualProbabiltyMidPoint(RealDistribution dist1, RealDistribution dist2) {
		double dist1Mean = dist1.getNumericalMean();
		double dist2Mean = dist2.getNumericalMean();
		dist1Mean = dist1.inverseCumulativeProbability(.5);
		dist2Mean = dist2.inverseCumulativeProbability(.5);
		if (dist1Mean < dist2Mean)
			return searchMidPoint(dist1, dist2, dist1Mean, dist2Mean);
		else 
			return searchMidPoint(dist2, dist1, dist2Mean, dist1Mean);

	}

	private static double searchMidPoint(RealDistribution dist1, RealDistribution dist2, double min, double max) {
		double mid = (max + min) / 2.0;
		double cdf1 = dist1.cumulativeProbability(mid);
		double cdf2 = 1.0 - dist2.cumulativeProbability(mid);
		double diff = cdf1 - cdf2;
		if (Math.abs(diff) < .0001)
			return mid;
		else if (diff < 0)
			return searchMidPoint(dist1,dist2, mid, max);
		else
			return searchMidPoint(dist1,dist2, min, mid);
	}
	
//	private boolean isDisjoint(OnlineStats stats1, OnlineStats stats2, double n) {
//	boolean disjoint;
//	double mean1 = stats1.getMean();
//	double mean2 = stats2.getMean();
//	double stddev1 = stats1.getStdDev();
//	double stddev2 = stats2.getStdDev();
//	if (mean1 < mean2) {
//		disjoint = (mean1 + n*stddev1) <= (mean2 - n*stddev2); 
//	} else if (mean2 < mean1){	
//		disjoint = (mean2 + n*stddev2) <= (mean1 - n*stddev1); 
//	} else {	// they are equal
//		disjoint = false;
//	}
//	return disjoint;
//
//}
	
}
