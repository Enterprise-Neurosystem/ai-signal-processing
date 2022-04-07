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
package org.eng.aisp.tools;

import org.eng.util.CommandArgs;

/**
 * Parses -user and -pwd options.
 * @author dawood
 *
 */
class UserPasswordOptions {

	protected String	user;
	protected String 	pwd;
	
	
	public final static String OptionsHelp = 
			  "  -user name: user name used for authentication. No default.\n"
			+ "  -pwd  pwd: password used for authentication. No default.\n"
			;

	public UserPasswordOptions() {

	}

	public UserPasswordOptions(String defaultUser, String defaultPwd) {
		this.user = defaultUser;
		this.pwd = defaultPwd;
	}
	/**
	 * Parse the options.
	 * Options are as follows:
	 * <ul>
	 * <li> user
	 * <li> pwd 
	 * @param cmdargs
	 * @return true on success, otherwise false and a message is issued on stderr.
	 */
	public boolean parseOptions(CommandArgs cmdargs) {
		user = cmdargs.getOption("user", user);
		pwd = cmdargs.getOption("pwd", pwd);
		return true;
	}

	public String getUser() {
		return this.user;
	}

	public String getPwd() {
		return pwd;
	}

}
