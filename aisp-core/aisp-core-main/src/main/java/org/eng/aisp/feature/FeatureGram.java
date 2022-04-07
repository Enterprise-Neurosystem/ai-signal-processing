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
package org.eng.aisp.feature;

import java.util.Arrays;
/**
 * Contains an array of features, generally computed from subwindows of a single IDataWindow.
 * @author dawood
 *
 * @param <FDATA>
 */
public class FeatureGram<FDATA> implements IFeatureGram<FDATA> {
	
	private static final long serialVersionUID = 4099782000013783624L;
	protected final IFeature<FDATA>[] features;

	/**
	 * @param features
	 */
	public FeatureGram(IFeature<FDATA>[] features) {
		super();
		this.features = features;
	}

	@Override
	public IFeature<FDATA>[] getFeatures() {
		return features;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(features);
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
		if (!(obj instanceof FeatureGram))
			return false;
		FeatureGram other = (FeatureGram) obj;
		if (!Arrays.equals(features, other.features))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return "FeatureGram [features="
				+ (features != null ? Arrays.asList(features).subList(0, Math.min(features.length, maxLen)) : null)
				+ "]";
	}

}
