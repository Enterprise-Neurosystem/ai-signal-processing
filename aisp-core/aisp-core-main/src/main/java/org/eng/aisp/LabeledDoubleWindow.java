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

import java.util.Properties;

public class LabeledDoubleWindow extends LabeledDataWindow<double[]> implements ILabeledDataWindow<double[]> {
	
	private static final long serialVersionUID = -3487832402564913520L;
	
	// For Jackson/Gson
	protected LabeledDoubleWindow() {
		super();
	}	
	
	/**
	 * @param window typically a DoubleWindow or other implementation of IDataWindow.
	 * @param labels the labels associated with the window.
	 */
	public LabeledDoubleWindow(IDataWindow<double[]> window, Properties labels) {
		super(window, labels);
	}

	protected LabeledDoubleWindow(IDataWindow<double[] >dataWindow, Properties labels, Properties tags, boolean isTrainable, DataTypeEnum dataType) {
		super(dataWindow, labels, tags, isTrainable, dataType);
//		if (dataWindow == null)
//			throw new IllegalArgumentException("dataWindow must not be null");
//		this.dataWindow = dataWindow;
//		this.isTrainable = isTrainable;
//
//		if (dataType != null) {
//			Properties p = DataTypeEnum.setDataType(null, dataType);
//			this.addTags(p);
//		}
//		
//		// We require a limited set of types to be used in Properties so Gson can de/serialize them.
//		if (labels != null) {
//			for (Object key : labels.keySet()) {
//				if (!(key instanceof String))
//					throw new IllegalArgumentException("Labels must use only Strings as keys");
//				Object value = labels.get(key);
//				if (!(value instanceof String || value instanceof Number))
//					throw new IllegalArgumentException("Label values must be one of String or Number. Value with key "
//							+ key + " has value of type " + value.getClass().getName());
//				this.labels.put(key, value);
//			}
//		}
	}


}
