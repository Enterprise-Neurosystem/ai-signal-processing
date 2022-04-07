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

import org.eng.ENGLogger;
import org.eng.aisp.util.RuntimePortLayer;

/**
 * Provides HTTP basic authentication formatting.
 * @author dawood
 *
 */
public class BasicAuthToken {

	protected final String user;
	protected final String pwd;
	protected final String authToken;
	
	public BasicAuthToken(String authToken) {
		String[] user_pwd = getUserPassword(authToken);
		if (user_pwd == null)
			throw new IllegalArgumentException("bad credentials");
		user = user_pwd[0];
		pwd = user_pwd[1];
		this.authToken = authToken;
	}
	
	public BasicAuthToken(String user, String pwd) {
		this.authToken = getToken(user,pwd);
		this.user = user;
		this.pwd = pwd;
	}

	/**
	 * Parse the basic auth token for a user and password.
	 * @param authToken
	 * @return null if a problem parsing the token.  If no password was found, set it to "".
	 */
	public static String[] getUserPassword(String authToken) {
		if (!authToken.contains("Basic ") && !authToken.contains("basic "))
			return null;
		authToken = authToken.replaceAll("[B|b]asic ", "");
		byte[] credBytes = RuntimePortLayer.instance().fromBase64(authToken);
		if (credBytes == null) { 
//			ENGLogger.logger.info("Could not base64 decode '" + authToken + "'");
			return null;
		}
		authToken = new String(credBytes);
		if (authToken.length() == 0) {
			ENGLogger.logger.info("zero length auth token");
			return null;
		}

		int index = authToken.indexOf(':');
		String[] split = new String[2];
		if (index < 0) {
			split = new String[2];
			split[0] = authToken;
			split[1] = "";
		} else {
			split[0] = authToken.substring(0, index);
			split[1] = authToken.substring(index+1);
//			if (split == null || split.length != 2) {
//				ENGLogger.logger.info("decoded token did not include any or 1 colon ");
//				return null;
//			}
		}
//		ENGLogger.logger.info("basic auth token for user " + split[0] + " parsed");
		return split;
	}

	/**
	 * Get the basic auth header value.
	 * @param user
	 * @param pwd
	 * @return null if either user or pwd are null.
	 */
	public static String getToken(String user, String pwd) {
		String ret;
		if (user != null && pwd != null) {
			String auth = RuntimePortLayer.instance().toBase64((user +":" + pwd).getBytes());
			ret = "Basic " + auth;
		} else {
			ret = null;
		}
//		ENGLogger.logger.info("basic auth token for user " + user + ": '" + ret + "'");
		return ret;
		
	}

	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @return the pwd
	 */
	public String getPassword() {
		return pwd;
	}

	/**
	 * @return the authToken
	 */
	public String getAuthToken() {
		return authToken;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BasicAuthToken [user=" + user + ", pwd=" + pwd + ", authToken=" + authToken + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((authToken == null) ? 0 : authToken.hashCode());
		result = prime * result + ((pwd == null) ? 0 : pwd.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof BasicAuthToken))
			return false;
		BasicAuthToken other = (BasicAuthToken) obj;
		if (authToken == null) {
			if (other.authToken != null)
				return false;
		} else if (!authToken.equals(other.authToken))
			return false;
		if (pwd == null) {
			if (other.pwd != null)
				return false;
		} else if (!pwd.equals(other.pwd))
			return false;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}

}
