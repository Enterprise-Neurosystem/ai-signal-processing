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
	
	/**
	 * @param window typically a DoubleWindow or other implementation of IDataWindow.
	 * @param labels the labels associated with the window.
	 */
	public LabeledDoubleWindow(IDataWindow<double[]> window, Properties labels) {
		super(window, labels, null);
	}

}
