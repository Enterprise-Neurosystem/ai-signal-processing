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

import java.util.logging.Level;
import java.util.logging.Logger;


public class AISPLibraryInitializer {

	public static void Initialize() {
		AISPProperties.instance().reload();	// Force message about where it is loaded from 
		
		// Try  and tone down mongo's default log levels.
		String mongoLevel =  AISPProperties.instance().getProperty("mongo.loglevel");
		Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
		Level l = Level.WARNING;
		if (mongoLevel != null) {
			try {
				l = Level.parse(mongoLevel); 
			} catch (Exception e) {
				System.err.println("Could not parse mongo log level from caa.properties: " + mongoLevel);
			}
		}
	    mongoLogger.setLevel(l);
	    
	    // Flush out any messages.
	    System.out.flush();
	    System.err.flush();
		
	}
}
