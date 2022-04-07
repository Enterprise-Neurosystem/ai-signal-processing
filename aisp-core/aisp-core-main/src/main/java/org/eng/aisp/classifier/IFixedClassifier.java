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
package org.eng.aisp.classifier;

import java.io.Serializable;
import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.IDataWindow;
import org.eng.util.ITaggedEntity;

/**
 * A implementation that can classify a data sample, but which is not trainable.
 * 
 * @author dawood
 *
 * @param <WINDATA> return value of {@link org.eng.aisp.IDataWindow#getData(int)} of the data provided 
 * to {@link #classify(org.eng.aisp.IDataWindow)} (often, <code>double[]</code>).
 */
public interface IFixedClassifier<DATA> extends ITaggedEntity, Serializable {

	
	/** 
	 * Get the classification(s) of the given data window.
	 * This method must be thread-safe and if need-be the implementation should add synchronization if required.
	 * @param sample data to be classified.
	 * @return a map of label names to Classification instances.
	 * @throws AISPException if not trained.
	 */
	Map<String, Classification> classify(IDataWindow<DATA> sample) throws AISPException;
	
	/**
	 * Get the label(s) which this classifier will generate.  
	 * @return null if not currently available to generate classification results.
	 */
	public String getTrainedLabel();
	
;

}
