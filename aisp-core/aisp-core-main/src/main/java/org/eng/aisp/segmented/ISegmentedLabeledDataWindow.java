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
package org.eng.aisp.segmented;

import java.io.Serializable;
import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.util.ITaggedEntity;

/**
 * Design for for labeled data window that has labeled and tagged segments defined over it.
 * Each segment is defined with a start and stop offset time and a set of optional labels and tags.
 * Each segment is represented by an instance of LabeledSegmentSpec.  
 * An opitonally empty  list of such segments is available. 
 * <p>
 * Note that this is a container for the segments and the labeled window and does not itself implement ILabeledDataWindow.  
 * This is designed this way to help avoid unintentionally using (i.e. training on) the whole segment instead of segments that might be defined on it. 
 * That said, implementations should provide the whole labeled data window if no segments are defined.
 * 
 * @param <DATA> the type of data contained in the IDataWindow contained in the ILabeledDataWindow.
 * @param LDW the extension of ILabeledDataWindow<DATA>.
 */
public interface ISegmentedLabeledDataWindow<DATA, LDW extends  ILabeledDataWindow<DATA>> extends ITaggedEntity, Serializable { 


	/**
	 * Get the list of segment specifications, if any.
	 * @return never null, but perhaps empty if no segments are defined.
	 * @see {@link #getSegmentedLabeledDataWindows()} 
	 */
	List<LabeledSegmentSpec> getSegmentSpecs();
	
	/**
	 * Get the full labeled data window w/o any segmentation.
	 * @return never null
	 */
	LDW getEntireLabeledDataWindow();
	
	
	/**
	 * Get all the segments defined on this instance.
	 * If no segments are defined, then the returned segment is effectively the whole labeled data window.
	 * @return never null and never empty.
	 * @throws AISPException
	 */
	List<LDW> getSegmentedLabeledDataWindows() throws AISPException;
	

}
