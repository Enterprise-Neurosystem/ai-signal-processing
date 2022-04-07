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
package org.eng.aisp.transform;

import java.io.Serializable;

import org.eng.aisp.ILabeledDataWindow;

/**
 * Takes a labeled window and transforms it into 1 or more 
 * new labeled windows, possibly including the original.
 * <p>
 * Implementations are expected to be used in the model training 
 * data processing pipeline, in which multiple passes over the data 
 * may be made.  In order to support stateful transforms in this 
 * situation * a new instance will be needed for each pass through the 
 * data. The {@link #newInstance()} method is used in this situation. 
 * @see TransformingIterable
 */
public interface ITrainingWindowTransform<WINDATA> extends Serializable {

	/**
	 * Transform the window into 0 or more windows.
	 * @param trainingLabel the label being trained.  
	 * @param ldw the labeled window to transform into 0 or more windows.  
	 * If the given window does not have the given training label its data 
	 * should be ignored.
	 * @return This method always returns an iterable, but may buffer inputs 
	 * to delay the output of transformed windows. In this case, the returned 
	 * iterable should be empty until the transformed windows can be generated 
	 * from the buffered ones.
	 */
	Iterable<ILabeledDataWindow<WINDATA>> apply(String trainingLabel, 
										ILabeledDataWindow<WINDATA> ldw); 

	/**
	 * Create a new instance from this instance.
	 * New instances will be created for each Iterator so that an implementation 
	 * can be stateful over the iterated data. 
	 * @return
	 */
	public ITrainingWindowTransform<WINDATA> newInstance();

	/**
	 * The number of windows produced, on average, by each window provided {@link #apply(String, ILabeledDataWindow)}. 
	 * @return 0 or greater 
	 */
	int multiplier();
	
}
