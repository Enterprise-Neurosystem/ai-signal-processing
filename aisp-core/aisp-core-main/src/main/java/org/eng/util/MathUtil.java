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

import java.util.ArrayList;
import java.util.List;


/**
 * Provides some basic math operations, generally over arrays of values.
 * 
 * @author dawood, pzerfos@us.ibm.com, mzafer
 *
 */
public class MathUtil {

	public static double average(final double[] values) {
		return average(values,false);
	}
		
	public static double average(final double[] values, boolean checkValidity) {
		return average(values, 0, values.length-1, checkValidity);
	}
	
	/**
	 * Compute the average of the given values starting at the given index.
	 * @param values
	 * @param startIndex 0-based index into the given array of values from which to start computing the average.
	 * @param endIndex 0-based index indicating the last value to include in the average.
	 * @param checkValidity
	 * @return NaN if could not be calculated.
	 */
	private static double average(final double[] values, final int startIndex, final int endIndex, final boolean checkValidity) {
		if (startIndex < 0) 
			throw new IllegalArgumentException("startIndex must be non-negative");
		if (endIndex >= values.length) 
			throw new IllegalArgumentException("endIndex is too large for the given input data");
		
		if (values == null || values.length <= startIndex)
			return Double.NaN;
		double sum = 0;
		if (checkValidity) {
			int count = 0;
			for (int i=startIndex ; i<=endIndex ; i++) {
				final double v = values[i];
				if (!Double.isNaN(v) && !Double.isInfinite(v)) {
					sum += v;
					count++;
				}
			}
			if (count == 0)
				return Double.NaN;
			return sum / count;
		} else {
			for (int i=startIndex ; i<=endIndex ; i++)
				sum += values[i];
			return sum / (endIndex - startIndex + 1);
		}
	}
	
	/**
	 * Move the average of the given set of value to the given value.
	 * @param values
	 * @param avg
	 */
	public static void adjustAverage(double[] values, double avg) {
		double currentAvg = average(values);
		double delta = avg - currentAvg;
		for (int i=0 ; i<values.length ; i++) {
			values[i] += delta;
		}
	}
	
	/**
	 * Compute and remove the average slope from the given values.
	 * @param values
	 */
	public static void removeSlope(double[] values) {
		removeSlope(values, false);
	}
	/**
	 * Compute and remove the average slope from the given values.
	 * @param values
	 * @param checkValidity check for and ignore invalid data if true.  If you know all data is valid, then set this to false for better performance.
	 * You should generally pass in true, unless you know that all the data is valid.
	 */
	public static void removeSlope(double[] values, boolean checkValidity) {
		double slope = slope(values, values.length, checkValidity);
		double delta = 0;
		for (int i=0 ; i<values.length ; i++) {
			values[i] -= delta;
			delta += slope;
		}
	}
	
	/**
	 * Remove the slope and DC components from the given array
	 * @param values
	 */
	public static void removeSlopeAndAverage(double[] values) {
		MathUtil.removeSlope(values, true);
		MathUtil.adjustAverage(values, 0);
	}
	
	/**
	 * Compute the standard slope over all values in the given input including any NaN or Infinite if present.
	 * A convenience on {@link #slope(double[], boolean)}.
	 * @param values
	 * @param count # of value in values[] to use
	 * @return Double.NaN if could not be computed.
	 */
	public static double slope(double[] values, int count) {
		return slope(values, count, false);
	}
	
	
//	public static double slope2(double[] values, boolean validOnly) {
//		if (values == null || values.length < 2)
//			return Double.NaN;
//		if (values.length == 1)
//			return 0;
//		
//		int halfCount;
//		halfCount = (values.length & 1) == 0 ? values.length : values.length - 1;
//		halfCount /= 2;
//		double slope;
//		double valueSum1 = 0, valueSum2 = 0;
//		if (validOnly) {
//			long indexSum1 = 0, indexSum2 = 0, count1 = 0, count2 = 0;
//			for (int i=0,j=halfCount ; i<halfCount ; i++, j++) {
//				double v = values[i];
//				if (!Double.isNaN(v) && !Double.isInfinite(v)) {
//					valueSum1 += v;
//					indexSum1 += i;
//					count1++;
//				}
//				v = values[j];
//				if (!Double.isNaN(v) && !Double.isInfinite(v)) {
//					valueSum2 += v;
//					indexSum2 += j;
//					count2++;
//				}
//			}
//			if (count1 == 0 || count2 == 0)
//				return Double.NaN;
//			double deltaY =  (valueSum2/count2) - (valueSum1/count1);
//			double deltaX =  (indexSum2/count2) - (indexSum1/count1);
//			slope = deltaY / deltaX;
//		} else {
//			for (int i=0,j=halfCount ; i<halfCount ; i++, j++) {
//				valueSum1 += values[i];
//				valueSum2 += values[j];
//			}
//			slope = (valueSum2 - valueSum1) / halfCount / halfCount;
//		}
//		return slope;
//	}
	
	/**
	 * Compute a slope using single step differencing among elements in the input data.
	 * @param values series to compute slope from
	 * @param valCount number of values in the values array to use.
	 * @param checkValidity if true, then test for valid values and ignore them.  This takes a bit more time.
	 * You should generally pass in true, unless you know that all the data is valid.
	 * @return Double.NaN if slope could not be computed.
	 * 
	 * XXX : pzerfos@us.ibm.com 2011/10/03: should be checked for correctness 
	 */
	public static double slope(double[] values, int valCount, boolean checkValidity){ 
		if (values == null || values.length < 2  || valCount > values.length)
			return Double.NaN;
		
		double sum = 0;
		int count = 0;
		if (checkValidity) {

			boolean currentValueIsValid = !Double.isNaN(values[0]) && !Double.isInfinite(values[0]);
			for (int i=0 ; i<valCount-1 ; i++) {
				double nextValue = values[i+1];
				boolean nextValueIsValid = !Double.isNaN(nextValue) && !Double.isInfinite(nextValue);
				if (currentValueIsValid && nextValueIsValid) {
					double delta = nextValue - values[i];
					sum += delta;
					count++;
				}
				currentValueIsValid = nextValueIsValid;
			}
			if (count == 0)
				return Double.NaN;
		} else {
			for (int i=0 ; i<valCount-1 ; i++)
				sum += values[i+1] - values[i];
			count = valCount -1;
		}

		return sum/count;
	}

	/**
	 * Multiply all the values by a constant so that the sum of
	 * all the values is the value given.
	 * @param values
	 * @param desiredSum
	 */
	public static void adjustSum(double[] values, double desiredSum) {
		double avg = average(values);
		if (avg == 0)  
			throw new RuntimeException("Sum of given values is 0 so can not scale.");
			
		double currentSum = avg * values.length;
		double multiplier = desiredSum / currentSum;
		for (int i=0 ; i<values.length ; i++) 
			values[i] *= multiplier;
				
	}
	

	

	

//	/**
//	 * Compute the unbiased auto correlation for the given number of lags, including a lag of 0.
//	 * The auto correlation of <i>N</i> values from <i>v</i> at a lag value of <i>L</i> is defined as
//	 * <ul>
//	 * <li> ACF(v,L) = Cov(v,L) / Var(v)
//	 * <li> Cov(v,L) = Sum(i=L..N | (v[i]-Mean(v,L,N)) * (v[i-L]-Mean(v,0,N-L)) ) / (ValidLagCount(v,L)-1) for L=0,1,2,3...
//	 * <li> Mean(v,i,j) = Sum(i=i..j | v[i]) / ValidCount(i,j) 	for i,j=0,1,2,3.., j>=i, v[i] != NaN :: This is the mean of values 
//	 * in the range i to j, ignoring the NaN values.
//	 * <li> ValidLagCount(v,L) = Sum(i=L..N |  v[i] != NaN && v[i-L] != NaN ? 1 : 0) 	:: This is the number of valid multiplications
//	 * (neither value is NaN) that will happen between the lagged and unlagged values.
//	 * <li> ValidCount(v,i,j) = Sum(k=i..j |  v[k] != NaN ? 1 : 0) - the count of valid values in v in the range i..j.
//	 * <li> Var(v) = Cov(v,0)
//	 * </ul>
//	 * @param values
//	 * @param lags number of lags for which the auto correlation is calculated.  This must be 1 or larger
//	 * and smaller than the number of given values.
//	 * @param checkValidity if true, then ignore any NaN or Infinity values in the given values array.  
//	 * If all your data is valid, then this should be set to false to get slightly better performance.
//	 * @return null if no valid values in the given array or no lags could be computed.  
//	 * Otherwise an array of length <i>lags+1</i> where
//	 * the first entry is the order 0 lag (i.e. the variance), the 2nd entry is the lag=1 correlation, and so on.  
//	 * If any given lag could not be computed because of
//	 * insufficient valid data, then the corresponding lag value will be NaN.
//	 */
//	private static double[] autoCovariance2(double[] values, final int lags, final boolean checkValidity) {
//
//		if (lags > values.length) 
//			throw new RuntimeException("The number of given values must be larger than the request number of lags");
//		
//		double[] acf = null;	// Accumulates Sum(i=L..N | (v[i]-Mean(v,L,N)) * (v[i-L]-Mean(v,0,N-L)) ) from the forumla above.
//		double[] laggedMeans = new double[lags+1];			// For Mean(v,0,N-L) above, for all L, including L=0
//		double[] unlaggedMeans = new double[lags+1];		// For Mean(v,L,N)   above, for all L, including L=0
// 
//		/**
//		 * First compute the lagged and unlagged means, taking into account invalid data as necessary.
//		 */
//		boolean someValid = false;
//		for (int l=0 ; l<=lags ; l++) {
//			laggedMeans[l]   = average(values,0,values.length-1-l,checkValidity);
//			unlaggedMeans[l] = average(values,l, values.length-1, checkValidity);
//			if (!Double.isNaN(laggedMeans[l]) && !Double.isInfinite(laggedMeans[l])
//				&& !Double.isNaN(unlaggedMeans[l]) && !Double.isInfinite(unlaggedMeans[l]))
//				someValid = true;
//		}
//		
//		if (someValid) {
//			acf = new double[lags+1];			
//			int[] lagCounts = new int[lags+1];	// Count of valid data for each lag value.
//			Arrays.fill(acf, 0);				
//			Arrays.fill(lagCounts, 0);
//			for (int baseIndex=0 ; baseIndex<values.length ; baseIndex++) {
//				double baseValue = values[baseIndex];
//				if (checkValidity && (Double.isNaN(baseValue) || Double.isInfinite(baseValue))) {
//					continue;	// Skip this value
//				} 
//				/**
//				 * For each lag, compute the contribution from the value at the current value index (baseIndex).
//				 */
//				for (int l=0 ; l<lags+1 ; l++) {
//					int lagIndex = baseIndex-l;
//					if (lagIndex >= 0) {
//						double deMeanedBaseValue = baseValue - unlaggedMeans[l];		// (v[i]-Mean(v,L,N)) from the javadoc formula above
//						double deMeanedLagValue = values[lagIndex] - laggedMeans[l];	// v[i-L]-Mean(v,0,N-L) from the javadoc formula above
//						if (checkValidity && (Double.isNaN(deMeanedLagValue) || Double.isInfinite(deMeanedLagValue)
//										  || (Double.isNaN(deMeanedBaseValue) || Double.isInfinite(deMeanedBaseValue))))
//							continue;
//						acf[l] += deMeanedBaseValue * deMeanedLagValue; 				// (v[i]-Mean(v,L,N)) * (v[i-L]-Mean(v,0,N-L))  from the javadoc formula above
//						lagCounts[l] += 1;
//					}
//				}
//			}
//			if (lagCounts[0] == 1)	// We can't compute the unbiased variance of v
//				return null;
//			double variance = acf[0] / (lagCounts[0] - 1);				// Var(v) = Cov(v,0) from the javadoc formula above.
//			acf[0] = variance;
//			
//			for (int l=1 ; l<lags+1 ; l++) {
//				if (lagCounts[l] == 1) {
//					acf[l] = Double.NaN;	// Not enough data to compute the 'unbiased' correlation
//				} else if (variance == 0) {
//					acf[l] = Double.NaN;
//				} else {
//					double cov = acf[l] / (lagCounts[l]-1);	// Cov(v,L) = Sum(i=L..N | (v[i]-Mean(v,L,N)) * (v[i-L]-Mean(v,0,N-L)) ) / (ValidLagCount(v,L)-1)
////					acf[l] = cov / variance;				// ACF(v,L) = Cov(v,L) / Var(v) from the javadoc forumula above
//					acf[l] = cov ;				// ACF(v,L) = Cov(v,L) / Var(v) from the javadoc forumula above
//				}
//			}
//		}
//		return acf;
//	}
	
	/**
	 * Determine if the delta between the two values is within the given precision.	 * 
	 * @param v1
	 * @param v2
	 * @param precision Defines the minimum difference between two doubles, before they are considered not equal.

	 * @return
	 */
	public static boolean isEqualWithinPrecision(double v1, double v2, double precision) {
		if (Math.abs(v2) > precision)
			return Math.abs(v1 / v2 - 1.0) <= precision;

		return Math.abs(v1 - v2) <= precision;
	}

	/**
	 * Return the indices into the given array where a positive peak is seen in the data.
	 * If a peak is mult-valued, that is, two or more consecutive indices with the same
	 * value, all indices for that flat max are included in the returned array.
	 * @param values
	 * @return never null, but perhaps empty array of indices representing the locations of the maxima.
	 * @throws IllegalArgumentException if array has less then 2 elements.
	 */
	public static int[] findMaxima(double values[]) {
		return findMaxima(values,0);
	}

	/**
	 * Determine if the relative difference between the two values exceeds a threshold percent.
	 * @param v1
	 * @param v2
	 * @param relativeDiff a number from 0 to 1.
	 * @return
	 */
	private static boolean largeDelta(double v1, double v2, double thresholdPercent) {
		if (thresholdPercent == 0)
			return true;
		double lower = Math.min(v1, v2);
		double delta = Math.abs(v1-v2);
		return Math.abs(delta / lower) >= thresholdPercent;
	}

	public static int[] findMaxima(double values[], double thresholdPercent) {
		List<Integer> max = new ArrayList<Integer>();
		List<Integer> possibles = new ArrayList<Integer>();

		if (values[0] > values[1] && largeDelta(values[0], values[1], thresholdPercent))
			max.add(0);
		double prev = values[0];
		boolean prevLargeDelta = false;
		for (int i=1 ; i<values.length-1 ; i++) { 	
			double current = values[i]; 
			double next = values[i+1]; 
			boolean nextLargeDelta = largeDelta(current,next, thresholdPercent);
			if (current > prev && prevLargeDelta) { 
				// The current is a potential start of a new peak, as its value jumped up over 
				// the previous by the required threshold.
				if (current > next && nextLargeDelta) {
					// The next one drops back down past the peak and so this is a single point peak.  
					max.add(i);
					possibles.clear();
//				} else if (current == next)  {
				} else if (!nextLargeDelta)	{	// next is under the threshold so include it in the possibles.
					possibles.add(i);
				}
			} else if (current > next  && nextLargeDelta) {
				// The previous was not a jump, but we are headed down the back side of a peak. 
				// This generally means a plateau and so lets keep all possibles and the current.
				if (!possibles.isEmpty()) {
					max.addAll(possibles);
					max.add(i);
					possibles.clear();
				}
			} else if (!prevLargeDelta && !nextLargeDelta) {
				// Neither prev or next represent a large delta, so if we're currently
				// in a peak, then keep accumulating these points.
				if (!possibles.isEmpty()) 	// We already jumped up over the threshold.
					possibles.add(i);
			}
			prev = current;
			prevLargeDelta = nextLargeDelta;
		}
		if (values.length > 2) {
			int lastIndex = values.length-1;
			if (values[lastIndex] > values[lastIndex-1] && largeDelta(values[lastIndex], values[lastIndex-1], thresholdPercent))
				max.add(lastIndex);
		}
		int indexes[] = new int[max.size()];
		for (int i=0 ; i<indexes.length ; i++)
			indexes[i] = max.get(i);
		return indexes;
	}

	/**
	 * Compute a number that is a power of 2 that is equal to or greater than 
	 * the given value.
	 * @param val non-negative integer
	 * @return 1 or greater.
	 */
	private static int upperPowerOfTwo(int val) {
		int n=1;
		 
	    for (int i=0 ; n < val ; i++) {
	    	n = n << 1;
	    	if (n == val)
	    		break;
	    }
	    return n;
	}

	/**
	 * Return an array that is a size that is a power of two, copy the values
	 * from the given array into it and then pad the remaining with 0s.  If
	 * the given array is already a power of 2 size, then just return it.
	 * @param values
	 * @return
	 */
	public static double[] padToPowerOfTwo(double[] values) {
		int n = upperPowerOfTwo(values.length);
		if (n == values.length)
			return values;
		double values2[] = new double[n];
		System.arraycopy(values, 0, values2, 0, values.length);
		for (int i=values.length ; i<values2.length ; i++)
			values2[i] = 0;
		return values2;
	}

//	public static void main(String[] args) {
//		double rmsError = Math.pow(10, 12);
//		double conf = 0.99;
//		double interval = MathUtil.confidenceInterval(rmsError, conf);
//		System.out.println("New confidenceInterval: " + interval);
//	}

}
