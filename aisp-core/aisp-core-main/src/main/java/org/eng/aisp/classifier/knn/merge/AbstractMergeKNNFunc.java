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

import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.classifier.knn.INearestNeighborFunction;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.util.ClassifierUtils;

/**
 * Abstract class for all merge nearest neighbor functions
 * @author wangshiq
 *
 */

// public abstract class AbstractMergeKNNFunc  extends TaggedEntity implements INearestNeighborFunction<double[]> {
public abstract class AbstractMergeKNNFunc  implements INearestNeighborFunction<double[]> {

	private static final long serialVersionUID = -8606058077888438252L;

	protected final boolean  normalizeWhenMerging;

	public AbstractMergeKNNFunc(boolean normalizeWhenMerging) {
		this.normalizeWhenMerging = normalizeWhenMerging;
	}
	
//	@Override
//	public List<double[]> featurePreProcessing(ILabeledFeature<double[]>[] wf) {
//		List<double[]> ret = new ArrayList<>();
//		ret.add(ClassifierUtils.averageListOfFeatures(wf, normalizeWhenMerging));
//		return ret;
//	}
	
	@Override
	public List<double[]> featurePreProcessing(IFeature<double[]>[] wf) {
		List<double[]> ret = new ArrayList<>();
		if (wf != null && wf.length > 0)
			ret.add(ClassifierUtils.averageListOfFeatures(wf, normalizeWhenMerging));
		return ret;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + " [normalizeWhenMerging=" + normalizeWhenMerging + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (normalizeWhenMerging ? 1231 : 1237);
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
		if (!(obj instanceof AbstractMergeKNNFunc))
			return false;
		AbstractMergeKNNFunc other = (AbstractMergeKNNFunc) obj;
		if (normalizeWhenMerging != other.normalizeWhenMerging)
			return false;
		return true;
	}
}
