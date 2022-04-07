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

import java.util.List;

import org.eng.util.IDereferencer;
import org.eng.util.IShuffleIterable;
import org.eng.util.ISizedIterable;

/**
 * Holds a set of sound references with associated labeling and tagging. 
 * Allows both shuffleable iteration and direct access to the contained references via {@link #dereference(String)}. 
 * The data set may be examined in total to get the full set of contained label names and label values for any of the contained label names.
 */
public interface ISoundReferenceSet extends IShuffleIterable<IReferencedSoundSpec>, IDereferencer<String, IReferencedSoundSpec>, ISizedIterable<IReferencedSoundSpec> {

	/**
	 * Get the set of all label (names) found in this data set.
	 * @return never null.  An empty list if all of the contained data is unlabeled.
	 */
	List<String> getLabels();

	/**
	 * Get the list of all label values found in this data set associated with the given label name.
	 * @param labelName
	 * @return never null.	An empty list if no data has the given label name assigned.
	 */
	List<String> getLabelValues(String labelName);
	

}
