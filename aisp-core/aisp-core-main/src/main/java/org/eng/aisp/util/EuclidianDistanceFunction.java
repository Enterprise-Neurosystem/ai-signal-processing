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
public class EuclidianDistanceFunction implements IDistanceFunction<double[]> {

	private static final long serialVersionUID = 5807463933845520740L;

	@Override
	public double distance(double[] d1, double[] d2) {
		return VectorUtils.euclidianDistance(d1, d2);
	}

	@Override
	public double distance(double[] d1) {
		return VectorUtils.euclidianDistance(d1, null);
	}

	@Override
	public boolean equals(Object o) {
		// All instances of this class are considered equal (at this level)
		// Sub-classes should override.
		return o instanceof EuclidianDistanceFunction;
	}

	@Override
	public int hashCode() {
		// All instances are equivalent and have the same hashcode, which is different than the default implementation. 
		// Sub-classes should override.
		return 1; 
	}

	@Override
	public String toString() {
		return "EuclidianDistanceFunction []";
	}

}
