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
/**
 * 
 */
package org.eng.util;

/**
 * An iterable that can report the number of items it will produce.
 * This can be important for some algorithms during training and so it is best if iterable trained into classifiers implements this.
 * @author dawood
 *
 */
public interface ISizedIterable<T> extends Iterable<T> {
	
	/**
	 * Get the number of items that will be produced by iteration.
	 * @return 0 or more.
	 */
	public int size();
	
}
