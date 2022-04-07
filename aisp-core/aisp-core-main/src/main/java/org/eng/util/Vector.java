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

import java.io.Serializable;
import java.util.Arrays;

/**
 * Represents a vector of regularly spaced values defined by an origin, delta and count. 
 * @author dawood
 *
 */
public class Vector implements Serializable {

	private static final long serialVersionUID = -2380827493976241161L;
	private final double origin;
	private final double delta;
	private final int length;
	private final double[] vector;
	private transient double[] regularVector = null;
	
	public Vector(double[] vector) {
		this.origin = 0;
		this.delta = 0;
		this.length = 0;
		this.vector = vector;
	}

	public Vector(double origin, double delta, int length) {
		this.origin = origin;
		this.delta = delta;
		this.length = length;
		this.vector = null;
	}
	
	/**
	 * Get the expanded vector of values as defined by the constructor.
	 * @return
	 */
	public double[] getVector() {
		if (vector != null)
			return vector;
		
		if (vector == null && regularVector == null) {
			regularVector = new double[length];
			double current = origin;
			for (int i=0 ; i<length; i++) {
				regularVector[i] = current;
				current += delta;
			}
		}
		return regularVector;
	}
	
	public boolean isRegular() {
		return vector == null;
	}

	/**
	 * @return the origin or the NaN if not regular. 
	 */
	public double getOrigin() {
		return isRegular() ? origin : Double.NaN; 
	}

	/**
	 * @return the delta or NaN if not regular.
	 */
	public double getDelta() {
		return isRegular() ? delta : Double.NaN;
	}

	/**
	 * @return the count
	 */
	public int length() {
		return isRegular() ? length : vector.length;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(delta);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + length;
		temp = Double.doubleToLongBits(origin);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + Arrays.hashCode(vector);
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
		if (!(obj instanceof Vector))
			return false;
		Vector other = (Vector) obj;
		if (Double.doubleToLongBits(delta) != Double.doubleToLongBits(other.delta))
			return false;
		if (length != other.length)
			return false;
		if (Double.doubleToLongBits(origin) != Double.doubleToLongBits(other.origin))
			return false;
		if (!Arrays.equals(vector, other.vector))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return "RegularVector [origin=" + origin + ", delta=" + delta + ", length=" + length + ", vector="
				+ (vector != null ? Arrays.toString(Arrays.copyOf(vector, Math.min(vector.length, maxLen))) : null)
				+ "]";
	}

}
