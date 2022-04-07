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
package org.eng.util;

public class Env {
	
	
	/**
	 * Read the value of the given environment variable and if not found issues a message on standard out. 
	 * @param varname
	 * @return null if not found.
	 */
	public static String getPassword(String varname) {
		String pwd = System.getenv(varname);
		if (pwd == null) 
			System.err.println("Excpect environment variable " + varname + " to contain a password string, but not found.");
		return pwd;
	}

}
