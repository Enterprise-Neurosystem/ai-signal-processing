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

import java.util.Map;
import java.util.Properties;

import org.eng.util.TaggedEntity;

public class LabeledDataWindow<WINDATA> extends TaggedEntity implements ILabeledDataWindow<WINDATA> {
	
	public final static String DATA_WINDOW_FIELD_NAME = "dataWindow";
	public final static String LABELS_FIELD_NAME = "labels";

	private static final long serialVersionUID = 2889207103001296924L;
	protected final Properties labels = new Properties();
	protected final IDataWindow<WINDATA> dataWindow;

	protected boolean isTrainable;

	// For Jackson/Gson
	protected LabeledDataWindow() {
		super();
		dataWindow = null;
	}

	public LabeledDataWindow(IDataWindow<WINDATA> dataWindow, Properties labels) {
		this(dataWindow,labels,(Properties)null);
	}

	public LabeledDataWindow(IDataWindow<WINDATA> dataWindow, Properties labels, Map<String,String> tags) {
		this(dataWindow, labels, tags == null ? null : TaggedEntity.getTagsAsProperties(tags));
	}
	
	public LabeledDataWindow(IDataWindow<WINDATA> dataWindow, Properties labels, Properties tags) {
		this(dataWindow, labels, tags, true);
	}

	public LabeledDataWindow(IDataWindow<WINDATA> dataWindow, Properties labels, Properties tags, boolean trainable) {
		this(dataWindow, labels, tags, trainable, null);
	}
	
	public LabeledDataWindow(IDataWindow<WINDATA> dataWindow, Properties labels, Properties tags, boolean isTrainable, DataTypeEnum dataType) {
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

	/**
	 * A copy constructor of sorts that copies everything but the data window from the input and uses
	 * the given data window instead.
	 * @param ldw
	 * @param dw
	 */
	public LabeledDataWindow(ILabeledDataWindow<WINDATA> ldw, IDataWindow<WINDATA> dw) {
		this(dw, ldw.getLabels(), ldw.getTagsAsProperties(), ldw.isTrainable());
	}

	@Override
	public IDataWindow<WINDATA> getDataWindow() {
		return dataWindow;
	}

	@Override
	public Properties getLabels() {
		return labels; 
	}

	public void setLabels(Properties labels) {
		this.labels.clear();
		this.labels.putAll(labels);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "LabeledDataWindow [labels=" + labels + ", dataWindow=" + dataWindow + "]";
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
		if (!(obj instanceof LabeledDataWindow))
			return false;
		LabeledDataWindow other = (LabeledDataWindow) obj;
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

	@Override
	public boolean isTrainable() {
		return this.isTrainable;
	}

	@Override
	public void setIsTrainable(boolean isTrainable) {
		this.isTrainable = isTrainable;
		
	}
	/**
	 * Get the content type of the data in the array.
	 * The type is expected to be specified in the tags using a tag as specified in {@link DataTypeEnum#setDataType(Properties, DataTypeEnum)}. 
	 * @return null if not set. 
	 */
	public DataTypeEnum getDataType() {
		return DataTypeEnum.getDataTypeTag(this);
	}
}
