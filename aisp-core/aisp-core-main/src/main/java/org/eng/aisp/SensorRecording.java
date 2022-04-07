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
package org.eng.aisp;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import org.eng.util.TaggedEntity;
import org.eng.validators.annotation.Alphanumeric;

import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * Represents the combination of a sensor, a data window together with associated training labels. 
 * The recording may be classified as either a <i>sample</i> or a <i>baseline</i>. Baselines are recordings
 * used to characterize a situation or condition, for example, normal operation.  Samples are generally compared
 * against a set of baselines.  A recording may have features extracted and attached to it.
 * @author dawood
 *
 */
public class SensorRecording<WINDOW extends IDataWindow<double[]>> extends TaggedEntity implements ILabeledDataWindow<double[]> {

	private static final long serialVersionUID = -1307951121170542891L;

	public final static String DATA_WINDOW_FIELD_NAME = "dataWindow";
	public final static String LABELS_FIELD_NAME = "labels";

	@Alphanumeric
	protected final Properties labels = new Properties();
	
	protected final WINDOW dataWindow;

	protected boolean isTrainable = true;

	// For Json/Jackson
	protected SensorRecording() {
		super();
		this.dataWindow = null;
		this.isTrainable = false;
	}


	/**
	 * 
	 * @param dataWindow
	 * @param labels
	 * @param tags
	 * @param isTrainable
	 * @param dataType
	 */
	protected SensorRecording(WINDOW dataWindow, Properties labels, Properties tags, boolean isTrainable, DataTypeEnum dataType) {
		super(tags);
		if (dataWindow == null)
			throw new IllegalArgumentException("dataWindow must not be null");
		this.dataWindow = dataWindow;
		this.isTrainable = isTrainable;

		if (dataType != null) {
			Properties p = DataTypeEnum.setDataType(null, dataType);
			this.addTags(p);
		}
		
		// We require a limited set of types to be used in Properties so Gson can de/serialize them.
		if (labels != null) {
			for (Object key : labels.keySet()) {
				if (!(key instanceof String))
					throw new IllegalArgumentException("Labels must use only Strings as keys");
				Object value = labels.get(key);
				if (!(value instanceof String || value instanceof Number))
					throw new IllegalArgumentException("Label values must be one of String or Number. Value with key "
							+ key + " has value of type " + value.getClass().getName());
				this.labels.put(key, value);
			}
		}
	}

	@Override
	public WINDOW getDataWindow()  {
		return dataWindow;
	}

	@Override
	public Properties getLabels() {
		return labels; 
	}
	
	public void setLabels(Properties labels) {
		if (labels == this.labels)
			return;
		this.labels.clear();
		this.labels.putAll(labels);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((dataWindow == null) ? 0 : dataWindow.hashCode());
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
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof SensorRecording))
			return false;
		SensorRecording other = (SensorRecording) obj;
		if (dataWindow == null) {
			if (other.dataWindow != null)
				return false;
		} else if (!dataWindow.equals(other.dataWindow))
			return false;
		if (labels == null) {
			if (other.labels != null)
				return false;
		} else if (!labels.equals(other.labels))
			return false;
		return true;
	}


	private String toString(Collection<?> collection, int maxLen) {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		int i = 0;
		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
			if (i > 0)
				builder.append(", ");
			builder.append(iterator.next());
		}
		builder.append("]");
		return builder.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SensorRecording [labels=" + labels + ", dataWindow=" + dataWindow + "]";
	}

	@Override
	public boolean isTrainable() {
		return isTrainable;
	}

	@Override
	public void setIsTrainable(boolean isTrainable) {
		this.isTrainable = isTrainable;
		
	}

	

	//	public SoundRecording(String deployedSensorID, SoundClip dataWindow, Properties labels, Properties tags) {
	//		this(deployedSensorID, dataWindow, labels, tags, true);
	//	}
				
	
	
	/**
	 * Get the content type of the data in the PCM/Wav array.
	 * The type is expected to be specified in the tags using a tag as specified in {@link DataTypeEnum#setDataType(Properties, DataTypeEnum)}. 
	 * @return
	 */
	public DataTypeEnum getDataType() {
		return DataTypeEnum.getDataTypeTag(this);
//		if (this.tags == null)
//			return DataTypeEnum.Audio;
//
//		return DataTypeEnum.getDataType(tags);
	}


}


