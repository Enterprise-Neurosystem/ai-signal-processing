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

import org.eng.util.ComponentProperties;

/**
 * Defines a component properties search space with the name 'caa'.
 * 
 * @author dawood
 *
 */
public class AISPProperties extends ComponentProperties {

	private static AISPProperties instance = null;
	
	static {
		instance = new AISPProperties();
//		Initialize();
	}
	

	/**
	 * Get the global instance of PML properties.
	 * @return
	 */
	public static AISPProperties instance() {
		// In addition to getting the instance, we make sure that all copyrights and logging messages have happened.
		return instance;
	}

	
	/**
	 * Protected to force use of static {@link #instance()} method.
	 */
	protected AISPProperties() {
		super("aisp");
	}

}
