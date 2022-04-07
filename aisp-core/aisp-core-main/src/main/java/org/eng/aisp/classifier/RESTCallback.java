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
package org.eng.aisp.classifier;

import java.io.IOException;

import org.eng.aisp.util.RuntimePortLayer;
import org.eng.util.HttpUtil;
import org.eng.util.HttpUtil.HttpResponse;

/**
 * Defines a single URL 
 * @author dawood
 *
 */
public class RESTCallback {
	
	protected final String url;
	protected final String user;
	protected final String pwd64;

	/**
	 * @param url
	 * @param user
	 * @param pwd
	 */
	public RESTCallback(String url, String user, String pwd) {
		super();
		this.url = url;
		this.user = user;
		this.pwd64 = RuntimePortLayer.instance().toBase64(pwd.getBytes());
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
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
	public String getPwd() {
		byte[] pwd = RuntimePortLayer.instance().fromBase64(pwd64);
		return new String(pwd);
	}
	
	public HttpResponse invoke() throws IOException {
		HttpResponse resp = HttpUtil.BasicAuthGET(this.getUrl(), null, this.getUser(), this.getPwd());
		return resp;
	}

	@Override
	public String toString() {
		return "RESTCallback [url=" + url + ", user=" + user + ", pwd64=" + pwd64 + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pwd64 == null) ? 0 : pwd64.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof RESTCallback))
			return false;
		RESTCallback other = (RESTCallback) obj;
		if (pwd64 == null) {
			if (other.pwd64 != null)
				return false;
		} else if (!pwd64.equals(other.pwd64))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}

}
