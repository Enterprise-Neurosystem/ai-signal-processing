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
package org.eng.aisp.feature.processor.vector;

import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.processor.IFeatureProcessor;

/**
 * Provides no-op feature processor.
 * NOTE: We override hashCode() and equals() because this class has no fields.  If we don't override,
 * then two instances would not be considered equal or have the same hashCode().  This is important for
 * comparing classifiers and the equals and hash code tests fail otherwise.
 * @author DavidWood
 *
 */
public class IdentityFeatureProcessor extends AbstractVectorMappingFeatureProcessor implements IFeatureProcessor<double[]> {

	private static final long serialVersionUID = -7421961091655629433L;

	@Override
	protected double[][] map(IFeature<double[]>[] features, double[][] featureData) {
		return featureData; 
	}

	/**
	 * See note above.
	 */
	@Override
	public int hashCode() {
		return 31; 
	}

	/**
	 * See note above.
	 */
	@Override
	public boolean equals(Object obj) {
		return obj instanceof IdentityFeatureProcessor; 
	}

}
