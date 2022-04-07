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
package org.eng.aisp.util;

import java.util.Arrays;
import java.util.Collection;

import org.eng.cache.Cache;
import org.eng.cache.IMultiKeyCache;
import org.eng.util.OnlineStats;

public class VectorUtils {

	/**
	 * Compute the index-wise average of all the given vectors.
	 * All vectors need not be the same length.  If a value contains Double.NaN it
	 * is not included in the average.
	 * @param vectors list of double arrays to be averaged.  They need not be the same length.
	 * @return never null.  If no valid values were seen for a given index across all vectors,
	 * then NaN is placed into the output at the corresponding index.  The returned array
	 * is of length equal to maximum length of the input vectors.
	 */
	public static  double[] average(Collection<double[]> vectors) {
		if (vectors == null || vectors.size() == 0)
			throw new IllegalArgumentException("List of vectors is null or empty");
		int maxSize = 0;
		for (double[] v : vectors) {
			if (v.length > maxSize)
				maxSize = v.length;
		}
		if (maxSize == 0) 
			return new double[] {};
		
		double[] output = new double[maxSize];
		int[] count = new int[maxSize];

		for (double v[] : vectors) {
			for (int i=0 ; i<v.length ; i++) {
				if (!Double.isNaN(v[i])) {
					output[i] += v[i];
					count[i] += 1;
				}
			}
		}

		for (int i=0 ; i<output.length ; i++) {
			if (count[i] == 0)
				output[i] = Double.NaN;
			else
				output[i] = output[i] / count[i];
		}

		return output;
	}
	
	public static Double[] averageDouble(Collection<Double[]> vectors) {

			if (vectors == null || vectors.size() == 0)
				throw new IllegalArgumentException("List of vectors is null or empty");
			int maxSize = 0;
			for (Double[] v : vectors) {
				if (v.length > maxSize)
					maxSize = v.length;
			}
			if (maxSize == 0) 
				return new Double[] {};
			
			Double[] output = new Double[maxSize];
			int[] count = new int[maxSize];

			for (Double v[] : vectors) {
				for (int i=0 ; i<v.length ; i++) {
					if (!Double.isNaN(v[i])) {
						if (output[i] == null) {
							output[i] = v[i];
						} else {
							output[i] = output[i] + v[i];
						}
						count[i] += 1;
					}
				}
			}

			for (int i=0 ; i<output.length ; i++) {
				if (count[i] == 0)
					output[i] = Double.NaN;
				else
					output[i] = output[i] / count[i];
			}

			return output;
	}
	
	/**
	 * Get the euclidean distance between two vectors of the same length.
	 * @param v1
	 * @param v2
	 * @return
	 */
	
	
	/**
	 * Compute the distance between the two points.  1 of the args may be null in which case it 
	 * effectively computes the distance from the non-null vector to the origin.
	 * @param v1Orig
	 * @param v2Orig
	 * @return
	 */
	public static double euclidianDistance(double[] v1Orig, double[] v2Orig) {
		if (v1Orig == null && v2Orig == null)
			throw new IllegalArgumentException("Both v1 and v2 are null");

		double[] v1, v2;
		if (v1Orig == null || v2Orig == null) {
			v1 = v1Orig == null ? v2Orig : v1Orig;
			v2 = null; 
		} else if (v1Orig.length != v2Orig.length) {
			int newLen = Math.max(v1Orig.length, v2Orig.length);
			v1 = new double[newLen];
			v2 = new double[newLen];

			for (int i=0; i<newLen; i++) {
				if (i<v1Orig.length) v1[i] = v1Orig[i];
				else v1[i] = 0;
				
				if (i<v2Orig.length) v2[i] = v2Orig[i];
				else v2[i] = 0;
			}
		} else {
			v1 = v1Orig;
			v2 = v2Orig;
		}
		
		double sum2 = 0;
		if (v2 == null) {
			for (int i=0 ; i<v1.length ; i++) {
				double diff = v1[i]; 
				sum2 += (diff * diff);
			}
		} else {
			for (int i=0 ; i<v1.length ; i++) {
				double diff = v1[i] - v2[i];
				sum2 += (diff * diff);
			}
			
		}
		return Math.sqrt(sum2)/v1.length;
			
	}
	
	
	/**
	 * Compute the distance between the two points.  1 of the args may be null in which case it 
	 * effectively computes the distance from the non-null vector to the origin.
	 * @param v1Orig
	 * @param v2Orig
	 * @return
	 */
	public static double l1Distance(double[] v1Orig, double[] v2Orig) {
		if (v1Orig == null && v2Orig == null)
			throw new IllegalArgumentException("Both v1 and v2 are null");

		double[] v1, v2;
		if (v1Orig == null || v2Orig == null) {
			v1 = v1Orig == null ? v2Orig : v1Orig;
			v2 = null; 
		} else if (v1Orig.length != v2Orig.length) {
			int newLen = Math.max(v1Orig.length, v2Orig.length);
			v1 = new double[newLen];
			v2 = new double[newLen];

			for (int i=0; i<newLen; i++) {
				if (i<v1Orig.length) v1[i] = v1Orig[i];
				else v1[i] = 0;
				
				if (i<v2Orig.length) v2[i] = v2Orig[i];
				else v2[i] = 0;
			}
		} else {
			v1 = v1Orig;
			v2 = v2Orig;
		}
		
		double sum2 = 0;
		if (v2 == null) {	// distance from 0. 
			for (int i=0 ; i<v1.length ; i++) {
				sum2 += Math.abs(v1[i]);
			}
		} else { 	//  distance between 2 points
			for (int i=0 ; i<v1.length ; i++) {
				double diff = v1[i] - v2[i];
				sum2 += Math.abs(diff);
			}
		}
		return sum2/v1.length;
			
	}

	public static double lpDistance(double[] v1, double p) {
//		double[] v2 = new double[v1.length];
//		Arrays.fill(v2, 0);
//		return lpDistance(v1,v2, p);
		return lpDistance(v1, null, p);
	}

	/**
	 * Compute the distance between the two points.  1 of the args may be null in which case it 
	 * effectively computes the distance from the non-null vector to the origin.
	 * @param v1Orig
	 * @param v2Orig
	 * @return
	 */
	public static double lpDistance(double[] v1Orig, double[] v2Orig, double p) {
		if (v1Orig == null && v2Orig == null)
			throw new IllegalArgumentException("Both v1 and v2 are null");
		if (p<0) 
			throw new IllegalArgumentException("p must be not smaller than zero");

		double[] v1, v2;
		if (v1Orig == null || v2Orig == null) {
			v1 = v1Orig == null ? v2Orig : v1Orig;
			v2 = null; 
		} else if (v1Orig.length != v2Orig.length) {
			int newLen = Math.max(v1Orig.length, v2Orig.length);
			v1 = new double[newLen];
			v2 = new double[newLen];

			for (int i=0; i<newLen; i++) {
				if (i<v1Orig.length) v1[i] = v1Orig[i];
				else v1[i] = 0;
				
				if (i<v2Orig.length) v2[i] = v2Orig[i];
				else v2[i] = 0;
			}
		} else {
			v1 = v1Orig;
			v2 = v2Orig;
		}
		
		double sum2 = 0;
		for (int i=0 ; i<v1.length ; i++) {
			double v2Value = v2 == null ? 0 : v2[i];
			double diff = Math.abs(v1[i] - v2Value);
			if (p==0.0) {
				if (diff > 0.0) sum2 += 1.0; 
			} else if (p==Double.MAX_VALUE) {
				sum2 = Math.max(sum2, diff);
			} else {
				sum2 += Math.pow(diff, p);
			}
		}
		if (p>0.0 && p<Double.MAX_VALUE) sum2 = Math.pow(sum2, 1.0/p);
		return sum2/v1.length;
			
	}

	public static double correlation(double[] v1Orig, double[] v2Orig) {
		if (v1Orig == null || v2Orig == null)
			throw new IllegalArgumentException("One of v1 or v2 is null");

		// TODO: this bit of code to get v1 and v2 is duplicated through various methods.  It should be put into a commonly used method.
		double[] v1, v2;
		if (v1Orig.length != v2Orig.length) {
			int newLen = Math.max(v1Orig.length, v2Orig.length);
			v1 = new double[newLen];
			v2 = new double[newLen];

			for (int i=0; i<newLen; i++) {
				if (i<v1Orig.length) v1[i] = v1Orig[i];
				else v1[i] = 0;
				
				if (i<v2Orig.length) v2[i] = v2Orig[i];
				else v2[i] = 0;
			}
		} else {
			v1 = v1Orig;
			v2 = v2Orig;
		}
		
		double sum2 = 0;
		for (int i=0 ; i<v1.length ; i++) {
			double diff = v1[i] * v2[i];
			sum2 += diff;
		}
		return sum2;
			
	}
	
	public static double[] objToPrim (Double[] d) {
		double[] dPrim = new double[d.length];
		for (int i=0; i<d.length; i++) {
			dPrim[i]=d[i];
		}
		return dPrim;
	}
	
	
	public static double cosineDistance(double[] v1Orig, double[] v2Orig) {
		if (v1Orig == null || v2Orig == null)
			throw new IllegalArgumentException("One of v1 or v2 is null");
		
		double[] v1, v2;
		if (v1Orig.length != v2Orig.length) {
			int newLen = Math.max(v1Orig.length, v2Orig.length);
			v1 = new double[newLen];
			v2 = new double[newLen];

			for (int i=0; i<newLen; i++) {
				if (i<v1Orig.length) v1[i] = v1Orig[i];
				else v1[i] = 0;
				
				if (i<v2Orig.length) v2[i] = v2Orig[i];
				else v2[i] = 0;
			}
		} else {
			v1 = v1Orig;
			v2 = v2Orig;
		}
		
		double dotProd = 0;
		for (int i=0 ; i<v1.length ; i++) {
			double dotProdTmp = v1[i] * v2[i];
			dotProd += dotProdTmp;
		}
		
		double v1Norm = 0;
		for (int i=0 ; i<v1.length ; i++) {
			double v1NormTmp = v1[i] * v1[i];
			v1Norm += v1NormTmp;
		}
		v1Norm = Math.sqrt(v1Norm);
		
		double v2Norm = 0;
		for (int i=0 ; i<v2.length ; i++) {
			double v2NormTmp = v2[i] * v2[i];
			v2Norm += v2NormTmp;
		}
		v2Norm = Math.sqrt(v2Norm);
		
//		double res = 1.0 - dotProd/(v1Norm * v2Norm);
		double res = Math.acos(dotProd/(v1Norm * v2Norm)) / Math.PI;
		
		return res;
			
	}
	
	/**
	 * Computes the difference (v1 - v2) between elements of two double arrays
	 * @param v1 
	 * @param v2
	 * @return new double array with the result
	 */
	public static double[] difference(double[] v1, double[] v2) {
		return difference(v1, v2, false);
	}
	
	/**
	 * Computes the difference (v1 - v2) between elements of two double arrays
	 * @param v1 
	 * @param v2
	 * @param inPlace if true, then v1 is modified to contain the difference, otherwise a new array is created.
	 * @return double array with the result
	 */
	public static double[] difference(double[] v1, double[] v2, boolean inPlace) {
		double[] diff;
		if (inPlace) {
			diff = v1;
		} else {
			int newLen = Math.min(v1.length, v2.length);
			diff = new double[newLen];
		}
		for (int i=0 ; i<diff.length; i++) {
			diff[i] = v1[i] - v2[i];
		}
		return diff;
	}
	
	/**
	 * Computes the sum (v1 + v2) between elements of two double arrays
	 * @param v1 
	 * @param v2
	 * @param inPlace if true, then v1 is modified to contain the sum, otherwise a new array is created.
	 * @return double array with the result
	 */
	public static double[] sum(double[] v1, double[] v2, boolean inPlace) {
		double[] sum;
		if (inPlace) {
			sum = v1;
		} else {
			int newLen = Math.min(v1.length, v2.length);
			sum = new double[newLen];
		}
		for (int i=0 ; i<sum.length; i++) 
			sum[i] = v1[i] + v2[i];
		
		return sum;
	}
	
	/**
	 * Divides a double array v1 with a scalar
	 * @param v1 
	 * @param scalar
	 * @return new double array with the result
	 */
	public static double[] divideByScalar(double[] v1, double scalar) {
		return divideByScalar(v1,scalar,false);
	}

	/**
	 * Divides a double array v1 with a scalar
	 * @param v1
	 * @param scalar
	 * @param inPlace if true, then modify the input and return it, otherwise create a new array to hold the results.
	 * @return
	 */
	public static double[] divideByScalar(double[] v1, double scalar, boolean inPlace) {
		int newLen = v1.length;
		double[] result = inPlace ? v1 : new double[newLen];
		for (int i=0 ; i<newLen; i++) {
			result[i] = v1[i] / scalar;
		}
		return result;
	}
	
	/**
	 * Multiplies a double array v1 with a scalar
	 * @param v1 
	 * @param scalar
	 * @return new double array with the result
	 */
	public static double[] multiplyByScalar(double[] v1, double scalar) {
		int newLen = v1.length;
		double[] mult = new double[newLen];
		for (int i=0 ; i<newLen; i++) {
			mult[i] = v1[i] * scalar;
		}
		return mult;
	}

	
	/**
	 * Normalize the standard deviation of the vector and optionally set the mean to 0.
	 * @param v data to base return values on
	 * @param zeroMean if true, then also remove the mean from the input vector.
	 * @param inPlace if true, then modify the input vector, otherwise create a new array.
	 * @return a vector of the same length as the input and allocated accordign to inPlace.
	 */
	public static double[] normalizeStandardDeviation(double[] v, boolean zeroMean, boolean inPlace) {
		return normalize(v, true, zeroMean, inPlace);
	}

	/**
	 * 
	 * @param v
	 * @param v data to base return values on
	 * @param normalizeStddev if true, then normalize the inputs relative to the standard deviation.  If false,
	 * then normalize to the range of values.
	 * @param zeroMean if true, then also remove the mean from the input vector.
	 * @param inPlace if true, then modify the input vector, otherwise create a new array.
	 * @return a vector of the same length as the input and allocated accordign to inPlace.
	 */
	public static double[] normalize(double[] v, boolean normalizeStddev, boolean zeroMean, boolean inPlace) {
		if (v.length < 2) {
			if (inPlace)
				return v;
			else
				return Arrays.copyOf(v, v.length);
		}

		// Compute the mean and stddev.
		OnlineStats stats = getStatistics(v);	 
//		AISPLogger.logger.info("stats=" + stats);
		
			
		// Do the normalization and optional zero mean adjustment
		double normalizer = normalizeStddev ? stats.getStdDev() : (stats.getMaximum() - stats.getMinimum());
		if (Double.isNaN(normalizer))
			normalizer = 1;	// Don't change the scale of the data.
		else if (normalizer == 0)
			normalizer = 1;	// Don't change the scale of the data.
		double scale = 1.0 / normalizer; 
		double mean = zeroMean ? stats.getMean() : 0;

		// No change implied, so if doing inplace, just return the original data.
		if (inPlace && mean == 0 && scale == 1)
			return v;
		
		// Set the destination for the new values.
		double vnew[];
		if (inPlace)
			vnew = v;
		else
			vnew = new double[v.length];
		for (int i = 0; i < v.length; i++)  {
			vnew[i] = scale * (v[i] - mean);  
//			if (Double.isNaN(vnew[i])) 
//				throw new IllegalArgumentException("NaN");
		}
		
		return vnew;
	}

	/**
	 * @param v
	 * @return
	 */
	public static OnlineStats getStatistics(double[] v) {
		OnlineStats stats = new OnlineStats();
		for (int i = 0; i < v.length; i++) 
			stats.addSample(v[i]);
		return stats;
	}

	/**
	 * Divide each element of a double array by the sum of all elements, without creating a new array.
	 * @param v
	 */
	public static void normalize(double[] v) {
		double tmpSum = 0.0;
		for (int i = 0; i < v.length; i++) {
			tmpSum += v[i];	// sum up frequency components before
										// taking moving average (for normalization later)
		}
		if (tmpSum==0) tmpSum=0.00001; //This is to avoid dividing by zero
	
		double invTmpSum = 1.0 / tmpSum;
		for (int i = 0; i < v.length; i++) {
			v[i] *= invTmpSum;
		}
	}
	
	/**
	 * Interpolates an array sampled with origSamplingRate to a new array sampled with newSamplingRate
	 * @param origArray Input array
	 * @param origSamplingRate
	 * @param newSamplingRate
	 * @return New array after interpolation
	 */
	public static double[] interpolate(double[] origArray, double origSamplingRate, double newSamplingRate) {
//		if (newSamplingRate < origSamplingRate) 
//			throw new IllegalArgumentException("newSamplingRate must be not smaller than origSamplingRate for interpolation.");
		
//		int newLength = (int)((double)newSamplingRate/origSamplingRate * (origArray.length - 1)) + 1;
		int newLength = (int)((double)newSamplingRate/origSamplingRate * (origArray.length) + 0.5);
		double[] newArray = new double[newLength];
		
//		double samplingRatio = (double)origSamplingRate / newSamplingRate;
		double samplingRatio = (double)(origArray.length - 1) / (newArray.length - 1);   //Minus one in both lengths because the ratio is computed only based on the (sampling) gap between two samples
		
		for (int i = 0; i < newLength; i++) {
			double samplePointLocation = samplingRatio * i;   //in original array
			int sampleIndexLow = (int)Math.floor(samplePointLocation);
			int sampleIndexHigh = Math.min((int)Math.ceil(samplePointLocation), origArray.length - 1);
			double sampleValueLow = origArray[sampleIndexLow];
			double sampleValueHigh = origArray[sampleIndexHigh];
			
			//When samplePointLocation is not an integer, find the value by connecting two neighboring sample points with a line and sampling on the line
			if (sampleIndexLow != sampleIndexHigh)
				newArray[i] = sampleValueLow + (sampleValueHigh - sampleValueLow) * (samplePointLocation - (double)sampleIndexLow) / ((double)sampleIndexHigh - (double)sampleIndexLow);
			else
				newArray[i] = sampleValueLow;
		}
		
		return newArray;
	}

	/**
	 * Calculate the auto-correlation vector for the given data.
	 * Index of returned array is the amount of shift of one vector relative to the other.
	 * So index 0 is the unshifted correlation. The correlation for correlation index CI s defined as 
	 * 1/N * (Sum over i of v[i] * v[i+CI]) where i+CI is a valid index and N is the number of valid
	 * indices for a given CI. 
	 * No checks are made for validity (i.e. not NaN or Infinity) of the data provided.
	 * @param vector
	 * @return an array of the same size as the input.
	 * @throws IllegalArgumentException if vector is null or zero length.
	 */
	public static double[] autoCorrelate(double[] vector) {
//		if (vector == null || vector.length == 0)
//			throw new IllegalArgumentException("vector must not be null or zero length");
//
//		double[] correlations = new double[vector.length];
//		for (int shift=0 ; shift<vector.length ; shift++) {
//			double correlation = 0;
//			int count = 0;
//			for (int i=0 ; i<vector.length ; i++)  {
//				int index = shift + i;
//				if (index < vector.length) {
//					correlation += vector[i] * vector[i+shift];
//					count++;
//				}
//			}
//			correlations[shift] = correlation / count;
//		}
//		return correlations;
		return correlationVector(vector,vector);
	}
	
	/**
	 * Compute the vector of correlations between the two vectors.  The correlation value at index i represents the
	 * correlation between the two vectors using a shift of i indices in which the shorter vector is shifted to index i 
	 * in the longer vector.
	 * @param vector1
	 * @param vector2
	 * @return an array  of length equal to the length of the longest vector provided.
	 */
	public static double[] correlationVector(double[] vector1, double[] vector2) {
		if (vector1 == null || vector1.length == 0)
			throw new IllegalArgumentException("vector1 must not be null or zero length");
		if (vector2 == null || vector2.length == 0)
			throw new IllegalArgumentException("vector2 must not be null or zero length");


		double longVector[], shortVector[]; 
		if (vector1.length > vector2.length) {
			longVector = vector1;
			shortVector = vector2;
		} else {
			longVector = vector2;
			shortVector = vector1;
		}
		double[] correlations = new double[longVector.length];
		for (int shift=0 ; shift<longVector.length ; shift++) {
			double correlation = 0;
			int count = 0;
			for (int i=0 ; i<shortVector.length ; i++)  {
				int index = shift + i;
				if (index < longVector.length) {
					correlation += shortVector[i] * longVector[index];
					count++;
				} else {
					break;
				}
			}
			if (count != 0)
				correlations[shift] = correlation / count;
		}
		return correlations;
	}
	
	public static double[] applyHammingWindow(double[] vector, boolean inPlace) {
		double[] window = getHammingWindow(vector.length);
		if (window.length != vector.length)
			throw new RuntimeException("Hamming window is not the correct size");
		double[] r = inPlace ? vector : new double[vector.length];
		for (int i=0 ; i<vector.length ; i++)
			r[i] = vector[i] * window[i];
		return r;
	}
	
	public static double[] applyHanningWindow(double[] vector, boolean inPlace) {
		double[] window = getHanningWindow(vector.length);
		if (window.length != vector.length)
			throw new RuntimeException("Hamming window is not the correct size");
		double[] r = inPlace ? vector : new double[vector.length];
		for (int i=0 ; i<vector.length ; i++)
			r[i] = vector[i] * window[i];
		return r;
	}

	/** Cache of hamming windows */
	private static IMultiKeyCache hammingWindowCache = Cache.newManagedMemoryCache(); 
	/** Cache of hanning windows */
	private static IMultiKeyCache hanningWindowCache = Cache.newManagedMemoryCache(); 

	/**
	 * Create/get a hamming window of the given width.
	 * Taken from the definition at https://en.wikipedia.org/wiki/Window_function.
	 * Uses a cache using the width as a key to try and avoid recomputing the window.
	 * @param width.
	 * @return
	 */
	private static double[] getHammingWindow(final int width) {

		double[] window = (double[])hammingWindowCache.get(width); 
		if (window != null)
			return window;
		
		synchronized(hammingWindowCache) {
			window = (double[])hammingWindowCache.get(width); 
			if (window != null)
				return window;
			window = new double[width];
			double angleScale = 2 * Math.PI / (width - 1);
			for (int i=0 ; i<width ; i++) 
				window[i] = 0.54 - 0.46 * Math.cos(i*angleScale);
			hammingWindowCache.put(window, width);
		}
		
		return window;
	}
	/**
	 * Create/get a hanning window of the given width.
	 * Taken from the definition at https://en.wikipedia.org/wiki/Hann_function
	 * Uses a cache using the width as a key to try and avoid recomputing the window.
	 * @param width.
	 * @return
	 */
	private static double[] getHanningWindow(final int width) {

		double[] window = (double[])hanningWindowCache.get(width); 
		if (window != null)
			return window;
		
		synchronized(hanningWindowCache) {
			window = (double[])hanningWindowCache.get(width); 
			if (window != null)
				return window;
			window = new double[width];
			double angleScale = 2 * Math.PI / width;
			int halfWidth = (int)(width /2. + .5);
			for (int i=0 ; i<width ; i++)  {
				int x = i - halfWidth; 
				window[i] =  (1 + Math.cos(x*angleScale)) / 2;
			}
			hanningWindowCache.put(window, width);
		}
		
		return window;
	}	
	public static boolean isConstant(final double[] vector) {
		if (vector == null)
			return true;
		boolean constantArray = true;
		for (int i=1 ; constantArray && i<vector.length; i++) {
			constantArray = constantArray && vector[i] == vector[0];
		}
		return constantArray;
	}

	/**
	 * Create a single array containing first the values of the first array followed by the values of the 2nd array.
	 * @param dctPowerReduced
	 * @param dctPhaseReduced
	 * @return never null.
	 */
	public static double[] concat(double[] d1, double[] d2) {
		if (d1 == null || d2 == null)
			throw new IllegalArgumentException("Both d1 and d2 must not be null");
		int newLen = d1.length + d2.length;
		double[] result = new double[newLen];
		System.arraycopy(d1, 0, result, 0, d1.length);
		System.arraycopy(d2, 0, result, d1.length, d2.length);
		return result;
	}
	
	
	/**
	 * Computes the dot product of two double arrays
	 * @param d1
	 * @param d2
	 * @return Dot product of d1 and d2
	 */
	public static double dotProduct(double[] d1, double[] d2) {
		if (d1 == null || d2 == null)
			throw new IllegalArgumentException("Both d1 and d2 must not be null");
		if (d1.length != d2.length)
			throw new IllegalArgumentException("Both d1 and d2 must have the same length");
		
		double result = 0.0;
		for (int i=0; i<d1.length; i++) {
			result += d1[i] * d2[i];
		}
		
		return result;
	}
	

	/**
	 * Get the ith dimension from a vector of data that is formatted with N-dimensional interleaved data.
	 * @param interleavedData data the has N dimensions of data interleaved/multiplexed.  For example,
	 * 3-D spatial data would be contained in the vector as (x0,y0,z0,x1,y1,z2,...xN,yN,zN). 
	 * The length of the input must be an integer times the number of dimensions (so that each 
	 * dimension has the same length).
	 * @param nDimensions Number of dimensions of data.
	 * @param index 0-based index of the dimension to extract.  0 would be x0..xN in the 3-D spatial example.
	 * @return an array of lenght interleavedData.length/nDimensions.
	 */
	public static double[] getInterleavedData(double[] interleavedData, int nDimensions, int index) {
		int len = interleavedData.length / nDimensions;
		double[] slice = new double[len];
		for (int i=index,j=0 ; i<interleavedData.length && j<len ; i+=nDimensions,j++) {
			slice[j] = interleavedData[i];
		}
		return slice;
	}
	
	/**
	 * Create a new array of the requested length, filled with data from the given array.  
	 * If there is not enough source data, then pad/fill the output with 0s.
	 * @param data
	 * @param newSamples size of output array. May be less then the given data array length.
	 * @return never null.
	 */
	public static double[] zeroPad(double[] data, int newSamples) { 
		double[] newData = new double[newSamples];
		System.arraycopy(data, 0, newData, 0, Math.min(newSamples,  data.length)); 
		// Just fill the end of newData with 0's probably already is, but just in case.
		if (newData.length > data.length)
			Arrays.fill(newData, data.length, newData.length, 0.0);
		return newData;
	}	

	/**
	 * Create a new array that is of the given size with data copied over 1 or more times to fill out the given size.
	 * @param data the source data to copy from
	 * @param newSamples the size of the output array.  May be less than the given data array.
	 * @return never null.  An array of length newSamples with data array copied into it as many times as needed.
	 * @throws IllegalArgumentException
	 */
	public static double[] duplicatePad(double[] data, int newSamples) { 
		double[] newData = new double[newSamples];
		System.arraycopy(data, 0, newData, 0, Math.min(newSamples,  data.length)); 
		// Copy (as many times as needed) this instance's data[] onto the end of newData.
		int srcIndex = 0;
		for (int i=data.length ; i<newData.length ; i++) {
			newData[i] = data[srcIndex];
			srcIndex++;
			if (srcIndex == data.length)
				srcIndex = 0;
		}
		return newData;
	}

	/**
	 * Apply the linear transform y = mx + b to the values in the given vector.
	 * @param data values to be transformed.
	 * @param m the slope
	 * @param b the offset
	 * @param inPlace if true, then the input array is modified in place, otherwise a 
	 * new array is created to contain the transformed values.
	 * @return an array of the same length as the input.
	 */
	public static double[] linearTransform(double[] data, double m, double b, boolean inPlace) {
		double newData[] = inPlace ? data : new double[data.length];
		for (int i=0 ; i<data.length ; i++) 
			newData[i] = m * data[i] + b;
		return newData;
	}
}
