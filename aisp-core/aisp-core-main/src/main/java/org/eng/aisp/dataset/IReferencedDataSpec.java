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
package org.eng.aisp.dataset;

import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.ILabeledDataWindow;

/**
 * Represents a reference to data that will be assigned the labels and tags contained in this implementation instance. 
 * The <i>reference</i> is an implementation-specific string that is opaque to the user of the implementation.
 * The reference can be used to load the underlying data.  For example, the reference might be filename, URL or database key.
 * The labels and tags are string=string pairs applied to the reference data when it is applied with the {@link #apply(ILabeledDataWindow)} method.
 * <p>
 * This is expected to be used as an item in a larger training data set, such that the data is only referenced, but the references
 * have the labeling and tagging information to be applied to the referenced data when it is dereferenced.
 * <p>
 */
public interface IReferencedDataSpec<DATA extends ILabeledDataWindow<?>> {

	/**
	 * The reference that is used to load the associated data.
	 * @return never null. 
	 */
	String getReference();

	/**
	 * The tags to be assigned to the dereferenced item when {@link #getReference()} is called.
	 * @return never null. 
	 */
	Properties getLabels();

	/**
	 * The tags to be assigned to the dereferenced item when {@link #getReference()} is called.
	 * @return never null. 
	 */
	Properties getTags();
	
	/**
	 * Apply this instance's metadata to the given data item. 
	 * @param data the data to merge with the metadata from this instance to create a new DATA instance.
	 * @return a new instance or the given one if not modified.
	 * @throws AISPException 
	 */
	DATA apply(DATA data) throws AISPException;


}