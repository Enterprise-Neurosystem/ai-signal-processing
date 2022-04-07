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
package org.eng.aisp.classifier.knn.merge;

import org.eng.aisp.classifier.knn.INearestNeighborFunction;
import org.eng.aisp.util.VectorUtils;

public class LpDistanceMergeKNNFunc extends AbstractMergeKNNFunc implements INearestNeighborFunction<double[]> {

	public static final double DEFAULT_P = .5;
	/**
	 * 
	 */
	private static final long serialVersionUID = -8843089651157360909L;
	private double p;

	public LpDistanceMergeKNNFunc(boolean normalizeWhenMerging) {
		this(DEFAULT_P, normalizeWhenMerging);
	}

	public LpDistanceMergeKNNFunc(double p, boolean normalizeWhenMerging) {
		super(normalizeWhenMerging);
		this.p = p;
	}
	
	
	@Override
	public double distance(double[] a, double[] b) {
		return VectorUtils.lpDistance(a, b, p);
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "LpDistanceMergeKNNFunc [p=" + p + ", normalizeWhenMerging=" + normalizeWhenMerging + "]";
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		long temp;
		temp = Double.doubleToLongBits(p);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof LpDistanceMergeKNNFunc))
			return false;
		LpDistanceMergeKNNFunc other = (LpDistanceMergeKNNFunc) obj;
		if (Double.doubleToLongBits(p) != Double.doubleToLongBits(other.p))
			return false;
		return true;
	}

	


}
