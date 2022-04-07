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
/**
 * 
 */
package org.eng.util;

import java.io.Serializable;


/**
 * Implements online calculation of mean and variance and also keeps track of min and max.
 * Taken from <a href="http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#On-line_algorithm">
 * http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#On-line_algorithm</a>
 * 
 * @author dawood
 *
 */
public class OnlineStats implements Serializable, Cloneable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 221604364524587892L;
	
	double mean; 
	double variance;
	int count;
	// ## replace m2 by mean2 for correct calculation of variance; Ting 8/13/2015
	// double m2;
	double mean2; // mean of x^2
	double min, max ;
	
	public OnlineStats() {
		reset();
	}
	
	/**
	 * For now we only need this for {@link #combine(OnlineStats)}.
	 */
	protected OnlineStats(OnlineStats stats) {
		this();
		this.count = stats.count;
		this.max = stats.max;
		this.mean = stats.mean;
		this.mean2 = stats.mean2;
		this.min = stats.min;
		this.variance = stats.variance;
	}

	/**
	 * Get the maximum of the data if samples present.
	 * @return Double.NaN if no data provided.
	 */
	public double getMaximum() {
		return max;
	}
	
	/**
	 * Get the minimum of the data if samples present.
	 * @return Double.NaN if no data provided.
	 */
	public double getMinimum() {
		return min;
	}
	/**
	 * Get the mean of squared samples; this is useful when samples are errors, and returned value is (empirical) MSE.
	 * @param v
	 */
	public double getMeanSquared(){
		return mean2;
	}
	
	/**
	 * Add a sample to the stats, unless it is NaN or Infinite.
	 * @param v
	 */
	public void addSample(double v) {
		this.addSamples(v,1);
	}

	/**
	 * Add a sample to the stats, unless it is NaN or Infinite.
	 * @param v
	 */
	public void addSamples(double v, int nTimes) {
		if (Double.isNaN(v) || Double.isInfinite(v))
			return;
		
		if (count == 0) {
			mean = v;
			mean2 = Math.pow(v, 2);
			variance = 0;
			min = max = v;
		} else {
			mean = ((count * mean) + nTimes*v) / (count + nTimes);
			mean2 = ((count * mean2) + nTimes*Math.pow(v, 2)) / (count + nTimes);
			variance = mean2 - Math.pow(mean, 2); // variance = E[X^2] - E[X]^2;  // m2 / count;
			if (v < min)
				min = v;
			else if (v > max)
				max = v;
		}
		count += nTimes;
	}

	/**
	 * Combine this and the given stats instance into a new object that represents 
	 * the equivalent of * separately adding this and the given stat's samples to the returned instance.
	 * @param stats
	 * @return never null.
	 */
	public OnlineStats combine(OnlineStats stats) {
		OnlineStats merged; 
		
		if (this.count == 0) {
			merged = new OnlineStats(stats);
		} else if (stats.count == 0) {
			merged = new OnlineStats(this);
		} else {	// Both have data.
			merged = new OnlineStats();
			merged.count = this.count + stats.count;
			merged.max = Math.max(this.max, stats.max);
			merged.min = Math.min(this.min, stats.min);
			merged.mean =  (this.count * this.mean  + stats.count * stats.mean)  / (this.count + stats.count);
			merged.mean2 = (this.count * this.mean2 + stats.count * stats.mean2) / (this.count + stats.count);
			merged.variance = merged.mean2 - Math.pow(merged.mean, 2); // variance = E[X^2] - E[X]^2;  
		}
					
		return merged;
	}
	
	public void addSamples(double... values) {

		for (int i=0 ; i<values.length ; i++) {
			double v  = values[i];
			addSample(v);
		}
	}

	public void addSamples(double[] v1, boolean checkValidity) {
		if (checkValidity)
			addValidSamples(v1);
		else
			addSamples(v1);
		
	}
	
	/**
	 * Only add samples that are not Infinte or NaN.
	 * @param values
	 */
	public void addValidSamples(double... values) {

		for (int i=0 ; i<values.length ; i++) {
			double v  = values[i];
			if (!Double.isNaN(v) && !Double.isInfinite(v))
				addSample(v);
		}
	}
	
	@Override
	public OnlineStats clone() {
		Object o;
		try {
			o = super.clone();
			return (OnlineStats)o;
		} catch (CloneNotSupportedException e) {
			return null;
		}
		
	}
	
	/**
	 * Get the mean of the data if samples present.
	 * @return Double.NaN if no data provided.
	 */
	public double getMean() {
		return mean;
	}
	
	/**
	 * Get the number of samples provided to {@link #addSample(double)}.
	 * If {@link #addValidSamples(double...)} or {@link #addSamples(double[], boolean)} is used, then only 
	 * valid samples are included in this number.
	 * @return non-negative value.
	 */
	public int getSampleCount() {
		return count;		
	}
	
	/**
	 * Get the standard deviation of the data if samples present.
	 * @return Double.NaN if no data provided.
	 */
	public double getStdDev() {
		return Math.sqrt(variance);
	}
	
	/**
	 * Get the variance of the data if samples present.
	 * @return Double.NaN if no data provided.
	 */
	public double getVariance() {
		return variance;
	}

	public void reset() {
		mean = Double.NaN;
		count = 0;
		variance = Double.NaN;
		mean2 = Double.NaN;
		//m2 = 0;
		min = Double.MAX_VALUE;
		max = -Double.MAX_VALUE;
	}

	@Override
	public String toString() {
		return "N= " + getSampleCount() + 
				" min=" + getMinimum() +
				" max=" + getMaximum() +
				" mean= " + getMean() + 
				" stddev= " + getStdDev();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + count;
		long temp;
		temp = Double.doubleToLongBits(max);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(mean);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(mean2);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(min);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(variance);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OnlineStats other = (OnlineStats) obj;
		if (count != other.count)
			return false;
		if (Double.doubleToLongBits(max) != Double.doubleToLongBits(other.max))
			return false;
		if (Double.doubleToLongBits(mean) != Double.doubleToLongBits(other.mean))
			return false;
		if (Double.doubleToLongBits(mean2) != Double.doubleToLongBits(other.mean2))
			return false;
		if (Double.doubleToLongBits(min) != Double.doubleToLongBits(other.min))
			return false;
		if (Double.doubleToLongBits(variance) != Double.doubleToLongBits(other.variance))
			return false;
		return true;
	}

	public static void main(String[] args) {
		OnlineStats ref = new OnlineStats();
		
		for (int i=0 ; i<10 ; i++) 
			ref.addSample(i);
		
		OnlineStats stats = new OnlineStats();
		for (int i=0 ; i<10 ; i++) 
			stats.addSamples(i, 3);
		
		System.out.println("stats are equals: " + ref.equals(stats));
		System.out.println("means are equals: " + (ref.getMean() == stats.getMean()));
		System.out.println("mins  are equals: " + (ref.getMinimum() == stats.getMinimum()));
		System.out.println("maxs  are equals: " + (ref.getMaximum() == stats.getMaximum()));
		System.out.println("stddev are equals: " + (ref.getStdDev() == stats.getStdDev()));
		System.out.println("count are equals: " + (ref.getSampleCount() == stats.getSampleCount()));
		System.out.println("Merged: " + stats.combine(ref) );
	}
}
