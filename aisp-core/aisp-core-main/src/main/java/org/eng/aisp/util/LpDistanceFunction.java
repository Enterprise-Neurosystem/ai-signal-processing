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
package org.eng.aisp.util;

/**
 * @author DavidWood
 *
 */
public class LpDistanceFunction implements IDistanceFunction<double[]> {

	private static final long serialVersionUID = -6253158097767879776L;
	private double p;

	public final static double DEFAULT_P = 0.5;
	
	public LpDistanceFunction(double p) {
		this.p = p;
	}

	/**
	 * Default p = 0.5.
	 */
	public LpDistanceFunction() {
		this(DEFAULT_P);
	}

	@Override
	public double distance(double[] d1, double[] d2) {
		return VectorUtils.lpDistance(d1, d2, p);
	}

	@Override
	public double distance(double[] d1) {
		return VectorUtils.lpDistance(d1, null, p);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(p);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof LpDistanceFunction))
			return false;
		LpDistanceFunction other = (LpDistanceFunction) obj;
		if (Double.doubleToLongBits(p) != Double.doubleToLongBits(other.p))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "LpDistanceFunction [p=" + p + "]";
	}

	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	/**
	 * @return the p
	 */
	public double getP() {
		return p;
	}



}
