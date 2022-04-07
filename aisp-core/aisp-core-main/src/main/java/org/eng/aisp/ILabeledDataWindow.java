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

import java.io.Serializable;
import java.util.Properties;

import org.eng.util.ITaggedEntity;

/**
 * Combines a IDataWindow with a set of labels.
 * This is the input to classifier training.
 *  
 * @author dawood
 *
 * @param <DATA>
 */
public interface ILabeledDataWindow<DATA> extends ITaggedEntity, Serializable {

	/**
	 * Get the window being labeled.
	 * @return never null.
	 */
	IDataWindow<DATA> getDataWindow();

	/**
	 * Get the labels for this window.
	 * @return never null, and probably not empty.
	 */
	Properties getLabels();
	
	boolean isTrainable();
	
	void setIsTrainable(boolean isTrainable);
	

}
