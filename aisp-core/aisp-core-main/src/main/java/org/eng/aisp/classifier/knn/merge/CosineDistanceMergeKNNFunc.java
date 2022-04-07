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

public class CosineDistanceMergeKNNFunc extends AbstractMergeKNNFunc implements INearestNeighborFunction<double[]> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8843089651157360909L;

	public CosineDistanceMergeKNNFunc(boolean normalizeWhenMerging) {
		super(normalizeWhenMerging);
	}
	
	
	@Override
	public double distance(double[] a, double[] b) {
		return VectorUtils.cosineDistance(a, b);
	}



}
