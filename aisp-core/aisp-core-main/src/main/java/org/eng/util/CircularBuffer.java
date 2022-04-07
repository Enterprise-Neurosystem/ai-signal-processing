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

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class that implements a circular buffer
 * 
 * @author pzerfos@us.ibm.com
 * @version 0.1, 2008-06-05
 */ 
public class CircularBuffer implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1369578912054671446L;
	
	/**
	 * The array that implements the circular buffer
	 */
	private double[] values = null;
	private double initialValue = 0;
	
//	/**
//	 * Number of elements in the valueArray[]
//	 */
//	private int arrayLength = 0;

	/**
	 * Allocate memory and create the actual buffer (as an array)
	 * 
	 * @param len The length of the buffer
	 * @param initialValue is the initial value for each element.
	 */
	public CircularBuffer(int len, double initialValue) {
		this.values = new double[len];
		this.initialValue = initialValue;
		for (int i=0; i<len; i++){
			this.values[i] = initialValue;
		}
	}
	
	public CircularBuffer(double[] values) {
		if (values == null || values.length == 0)
			throw new RuntimeException("value array must not be null or 0 length");
		this.values = values;
	}

	/**
	 * Set the value of the index item in the array
	 * 
	 * @param index The index of the item whose value will be set
	 * 				Index can be _arbitrarily_ long (longer than the
	 * 				length of the array)
	 * @param value The value to be set
	 */
	public void setValue(final int index, final double value) {
//		if (Double.isNaN(value))
//			System.out.println("setValue() to NaN.  ouch!");
//		if (Double.isInfinite(value))
//			System.out.println("setValue() to Infinity.  ouch!");
		if (index < 0)
			throw new RuntimeException("index must be non-negative");
		
		final int i = index < values.length ? index : index % values.length;
//			if (i == 1 && value < 5 && value > -5)
//				System.out.println("setValue()");
		values[i] = value;

			
	}
	
	/**
	 * Return the value of position (index % buffer_length) in the circular buffer
	 * @param index An arbitrarily large index
	 * @return the value at that index (% buffer_length)
	 */
	public double getValue(int index) {
		index = index + values.length;
		if (index < values.length) // Try and avoid the division operation
			return values[index];	
		return (values[index % values.length]);
	}
	

	/** 
	 * Get the underlying array of values.
	 * @return
	 */
	public double[] getValues() {
		return values;
	}
	

	/**
	 * Reset the array to be filled with the initialValue.
	 */
	public void clear() {
		if (values != null) {
			for (int i = 0; i < values.length; i++)
				values[i] = initialValue;
//			arrayLength = values.length;
		}
	}
	
	/**
	 * @return the length of the buffer
	 */
	public int getLength() {
		return values == null ? 0 : values.length;
	}

	/**
	 * Get the difference between the min and max values in the buffer.
	 * @return Double.NaN if no values in the buffer.
	 */
	public double range() {
		if (values == null || values.length == 0)
			return Double.NaN;
		
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		for (int i=0 ; i<values.length ; i++) {
			double v = values[i];
			if (v < min)
				min = v;
			if (v > max)
				max = v;
		}
		return max - min;
	}
	
	/**
	 * Average all values in the buffer, including buffer elements containing the initial value.
	 * @return
	 */
	public double average() {
		return MathUtil.average(values);
	}

	/**
	 * Divide all values by the average value so that the
	 * average of all values is 1.  If the average is 0, then
	 * add 1 to all values.
	 */
	public void normalizeAverage() {
		setAverage(1.0);
	}
	
	public void setAverage(double newAverage) {
		MathUtil.adjustAverage(values, newAverage);
	}
	
	/**
	 * Shift the values up or down by the average of 
	 * all values so that all values add together to equal 0.
	 */
	public void normalizeSum() {
		setAverage(0);
	}
	
	public void toCSV(PrintStream out) {
		for (int i=0 ; i<values.length ; i++) {
			out.println(i + ",\t " + values[i]);
		}
	}

	/**
	 * Multiply each value by the given multiplier.
	 * @param multiplier
	 */
	public void scale(double multiplier) {
		for (int i=0 ; i<values.length ; i++) {
			values[i] *= multiplier;
		}
	}

	
	public List<Double> toList() {
		ArrayList<Double> list = new ArrayList<Double>(values.length);
		for (int i=0 ; i<values.length ; i++) {
			list.add(new Double(values[i]));
		}
		return list;
		
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(values);
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
		if (getClass() != obj.getClass())
			return false;
		CircularBuffer other = (CircularBuffer) obj;
		if (!Arrays.equals(values, other.values))
			return false;
		return true;
	}


}
