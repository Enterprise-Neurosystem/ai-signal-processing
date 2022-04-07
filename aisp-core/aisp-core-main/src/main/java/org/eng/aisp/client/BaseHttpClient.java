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
package org.eng.aisp.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.eng.aisp.AISPException;
import org.eng.aisp.AISPProperties;
import org.eng.util.BasicAuthToken;
import org.eng.util.MultipartUtility;

/**
 * Base class for our Http clients.
 * Defines base url and supports https schema selection based on ports.
 * 
 * @author dawood
 *
 */
public class BaseHttpClient {
	
	protected final String baseURL;
	protected final String host;
	protected final int port;
	private final static boolean UseHttps = AISPProperties.instance().getProperty(BaseHttpClient.class.getName() + ".useHttps", false);

	public BaseHttpClient(URL url) {
		this(url.getHost(), getPortFromURL(url), url.getPath());
	}

	private static int getPortFromURL(URL url) {
		int port = url.getPort();
		if (port > 0) {
			String protocol = url.getProtocol();
			if (protocol.contains("https"))
				return 443;
			else if (protocol.contains("http"))
				return 80;
			else
				throw new IllegalArgumentException("Unsupported protocol " + protocol);
		}
		return port;
	}
	/**
	 * Set the base url (http vs https) based on the port.
	 * if port is *443, then use https, otherwise http.
	 * @param host
	 * @param port
	 * @param contextRoot
	 */
	public BaseHttpClient(String host, int port, String contextRoot) {
		this.host = checkNotNullNotEmpty("host", host);
		if (port <= 0)
			throw new IllegalArgumentException("port must be greater than 0");
		this.port = port;
		contextRoot = checkNotNullNotEmpty("contextRoot", contextRoot);
		String http = getURLScheme(port);
		if (!contextRoot.endsWith("/"))
			contextRoot += "/";
		if (!contextRoot.startsWith("/"))
			contextRoot = "/" + contextRoot;
		this.baseURL = http + host + ":" + port +  contextRoot;
	}

	protected static String checkNotNullNotEmpty(String argName, String arg) {
		if (arg == null) 
			throw new IllegalArgumentException(argName + " is null");
		arg = arg.trim();
		if (arg.length() == 0)
			throw new IllegalArgumentException(argName + " is empty or contains only white space");
		return arg;
		
	}
	
	/**
	 * @param port
	 * @return "https://" if port is X443 or "http://" otherwise. 
	 */
	public static String getURLScheme(int port) {
		String http;
		if (UseHttps 
			|| ((port - 443) % 1000 == 0)	// An xx443 port
			|| ((port - 448) % 1000 == 0))	// AI is using *448 ports with https 
			http = "https://";
		else
			http = "http://";
		return http;
	}
	
	public String getBaseAPIURL() {
		return this.baseURL;
	}
	
	/**
	 * Encode using a common method for both Android and non-Android platforms.
	 * @param value
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	protected static String safeURLEncode(String value) throws UnsupportedEncodingException {
//		return URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8.toString());	// Does not seem to work on Android
//		return URLEncoder.encode(name, Charset.forName("UTF-8").toString());					// Does not compile on Android
		return URLEncoder.encode(value,"UTF-8");
	}
	
	private static HttpClientBuilder httpsClientBuilder = null;

	private static HttpClientBuilder createHttpsClientBuilder() throws AISPException {
		try {
			SSLContextBuilder builder = new SSLContextBuilder();
//			builder.loadTrustMaterial(null, new TrustAllStrategy());
			builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(),  NoopHostnameVerifier.INSTANCE);
			return HttpClients.custom().setSSLSocketFactory(sslsf);
		} catch (Exception e1) {
			throw new AISPException("Could not create https client: " + e1.getMessage());
		}
	}
	
	/**
	 * @param url target of request.
	 * @param user if this and pwd are not null then include a BasicAuth header containing these values.
	 * @param pwd if this and user are not null then include a BasicAuth header containing these values.
	 * @param multipart
	 * @return the raw response as a UTF-8 String.
	 * @throws AISPException
	 */
	public static String MakeHttpEntityRequest(String url, String user, String pwd, HttpEntity multipart) throws AISPException {
		Properties headers= new Properties();
		String token = BasicAuthToken.getToken(user,pwd);;
		if (token != null)
			headers.setProperty("authorization",token);
		return MakeHttpEntityRequest(url,headers,multipart);
	}
	
	/**
	 * @param url target of request.
	 * @param  headers name/value pairs added to request as request headers.
	 * @param multipart
	 * @return the raw response as a UTF-8 String.
	 * @throws AISPException
	 */
	public static String MakeHttpEntityRequest(String url, Map<?,?> headers, HttpEntity multipart) throws AISPException {
		CloseableHttpClient httpClient; 
		if (url.startsWith("https")) {
			if (httpsClientBuilder == null)
				httpsClientBuilder = createHttpsClientBuilder();
			httpClient = httpsClientBuilder.build();
		} else {
			httpClient = HttpClients.createDefault();
		}

		HttpPost uploadFile = new HttpPost(url);
		uploadFile.setEntity(multipart);
		if (headers != null) {
			for (Object key : headers.keySet()) {
				String strKey = key.toString();
				uploadFile.addHeader(strKey, headers.get(key).toString());
			}
		}
		CloseableHttpResponse response = null; 
		HttpEntity responseEntity = null; 
		BufferedInputStream in = null; 
		ByteArrayOutputStream bos = null; 
		try {
			response = httpClient.execute(uploadFile);
			responseEntity = response.getEntity();
			in = new BufferedInputStream(responseEntity.getContent());
			bos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int count;
			while ((count = in.read(buffer)) >= 0)
				bos.write(buffer, 0, count);
			return bos.toString("UTF-8");
		} catch (Exception e) {
			throw new AISPException("Can't send send multipart form data: " + e.getMessage() ,e);
		} finally {
			try {
				if (bos != null)
					bos.close();
				else if (in != null)	// close bos, closes in.
					in.close();
				if (response != null)
					response.close();
				if (httpClient != null)	// Should never be null, but just to be safe. 
					httpClient.close();
			} catch  (Exception e) {
				// Ignore
			}
		}
		
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Create the instance with a BasicAuth header.
	 * @param url
	 * @param user
	 * @param pwd
	 * @return
	 * @throws IOException
	 */
	public MultipartUtility getMultipartUtility(String url, String user, String pwd) throws IOException {
		if (user == null || pwd == null)
			throw new IllegalArgumentException("one of user or pwd is null");
		Map<String,String> headers= new HashMap<String,String>();
		String token = BasicAuthToken.getToken(user,pwd);;
		if (token != null)
			headers.put("authorization", token);
		else
			throw new RuntimeException("Could not generate token.");
		return new MultipartUtility(url, headers, "UTF-8");
	}
}
