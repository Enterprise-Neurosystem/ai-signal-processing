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
package org.eng.aisp.util;

import java.io.Serializable;

public interface IDistanceFunction<DATA> extends Serializable {

	/**
	 * Compute the distance between the two data points.
	 * @param d1
	 * @param d2
	 * @return
	 */
	public double distance(DATA d1, DATA d2); 
	
	/**
	 * Compute the distance between the given data point and the "origin".
	 * @param d1
	 * @return
	 */
	public double distance(DATA d1);
}
