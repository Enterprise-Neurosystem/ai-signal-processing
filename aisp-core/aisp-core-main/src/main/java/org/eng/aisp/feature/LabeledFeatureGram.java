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

import java.util.Properties;

/**
 * Contains an array of features, generally computed from subwindows of a single IDataWindow, together with the labels from the source IDataWindow.
 * @author dawood
 *
 * @param <FDATA>
 */
public class LabeledFeatureGram<FDATA> implements ILabeledFeatureGram<FDATA> {

	private static final long serialVersionUID = -2378918618334424943L;
	private final IFeatureGram<FDATA> featureGram;
	private final Properties labels;

	public LabeledFeatureGram(IFeatureGram<FDATA> fg, Properties labels) {
		if (fg == null)
			throw new IllegalArgumentException("feature gram can not be null");
		this.featureGram = fg;
		if (labels == null)
			throw new IllegalArgumentException("labels can not be null");
		this.labels = labels;
	}
	
	@Override
	public IFeatureGram<FDATA> getFeatureGram() {
		return featureGram;
	}

	@Override
	public Properties getLabels() {
		return labels;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((featureGram == null) ? 0 : featureGram.hashCode());
		result = prime * result + ((labels == null) ? 0 : labels.hashCode());
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
		if (!(obj instanceof LabeledFeatureGram))
			return false;
		LabeledFeatureGram other = (LabeledFeatureGram) obj;
		if (featureGram == null) {
			if (other.featureGram != null)
				return false;
		} else if (!featureGram.equals(other.featureGram))
			return false;
		if (labels == null) {
			if (other.labels != null)
				return false;
		} else if (!labels.equals(other.labels))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "LabeledFeatureGram [featureGram=" + featureGram + ", labels=" + labels + "]";
	}

}
