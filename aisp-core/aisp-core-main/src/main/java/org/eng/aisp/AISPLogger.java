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

import org.eng.util.ComponentLogger;
import org.eng.util.ComponentProperties;


/**
 * Extends the ComponentLogger to define a single PMLLogger root instance.
 * See wpml.properties for additional configurables according
 * to the java.util.logging framework.
 * 
 * @author dawood
 *
 */
public class AISPLogger  extends ComponentLogger {
	

	
	/** The name of the global AISP logger */
	public final static String AISP_ROOT_LOGGER_NAME = "org.eng.aisp";
	
	public final static AISPLogger logger = new AISPLogger(AISP_ROOT_LOGGER_NAME, true);

	private AISPLogger(String name, boolean isRoot) {
		super(name,isRoot);
	}
	
	protected ComponentProperties getLoggerConfigProps() {
		return AISPProperties.instance();
	}
}
