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
package org.eng.aisp.storage;

import java.util.Properties;

import org.eng.aisp.classifier.IFixedClassifier;

public class SerializedClassifierMetaData {

	public final static String TAGS_FIELD_NAME = "tags";
	public final static String TRAIN_LABEL_FIELD_NAME = "trainingLabel";
	
	protected final Properties tags = new Properties();
	protected final String trainingLabel;
	private String name;
	
	public SerializedClassifierMetaData(String name, IFixedClassifier<double[]> model) {
		this.name = name;
		this.tags.putAll(model.getTagsAsProperties());
		this.trainingLabel = model.getTrainedLabel();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tags == null) ? 0 : tags.hashCode());
		result = prime * result + ((trainingLabel == null) ? 0 : trainingLabel.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SerializedClassifierMetaData))
			return false;
		SerializedClassifierMetaData other = (SerializedClassifierMetaData) obj;
		if (tags == null) {
			if (other.tags != null)
				return false;
		} else if (!tags.equals(other.tags))
			return false;
		if (trainingLabel == null) {
			if (other.trainingLabel != null)
				return false;
		} else if (!trainingLabel.equals(other.trainingLabel))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SerializedClassifierMetaData [tags=" + tags + ", trainingLabel=" + trainingLabel + "]";
	}


}
