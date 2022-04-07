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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eng.ENGLogger;
import org.eng.ENGProperties;

import com.google.gson.Gson;

public class HttpUtil {

	public static final int DEFAULT_TIMEOUT_MSEC = ENGProperties.instance().getProperty("httputil.timeout.seconds", 30) * 1000;	// 15 seconds
	protected static int TIMEOUT_MSEC = DEFAULT_TIMEOUT_MSEC;

	public static int getTimeoutMsec() {
		return TIMEOUT_MSEC;
	}

	public static void setTimeoutMsec(int msec) {
		TIMEOUT_MSEC = msec;
	}

//	public static final boolean FORCE_SSL_VERIFY_CERTIFICATES = ENGProperties.instance().getProperty("httputil.forceSSLCertificateVerification", false);
	public static final String SSLCertificateVerification_PropertyName = "httputil.SSLCertificateVerification.enabled";
	public static final boolean SSL_VERIFY_CERTIFICATES = ENGProperties.instance().getProperty(SSLCertificateVerification_PropertyName, false);

	private static boolean SSL_CONFIGURED = false;

	/**
	 * Turn off some SSL verification (especially hostname) when the hostname being used is a dotted ip address.
	 * This is done so that we can continue to use the 129.34.40.49 dev server which uses a self-signed cert.
	 * @param url
	 */
	public synchronized static void configureSSL(URL url) {
		if (SSL_CONFIGURED)
			return;
		
		String host = url.getHost();
		String[] pieces = host.split("\\.");
		boolean dottedIPAddress= false;
		if (pieces != null && pieces.length == 4) {
			try {
				int n = Integer.parseInt(pieces[0]);
				dottedIPAddress = true;
			} catch (Exception e) {
				;
			}
		}
		if (dottedIPAddress || !SSL_VERIFY_CERTIFICATES) {
			ENGLogger.logger.info("SSL verification disabled. Use -D" + SSLCertificateVerification_PropertyName + "=true to enable.");
			SSLTool.reduceSSLCertificateValidation();
		} else {
			ENGLogger.logger.info("SSL verification enabled. Use -D" + SSLCertificateVerification_PropertyName + "=false to disable.");
		}
		SSL_CONFIGURED = true;
	}

//	private static boolean Verbose = false;
	
	public static String DEFAULT_CONTENT_TYPE = "text/html";
	public static String DEFAULT_REQUEST_METHOD = "GET";
	public static Gson gson = new Gson();
	
//	public static void setVerbose(boolean v) {
//		Verbose = v;
//	}

	public static HttpResponse BasicAuthPOST(String urlString, String user, String pwd,  Object json) throws IOException {
		return BasicAuthPOST(urlString, null, user, pwd, json);
	}
	
	
	
	public static HttpResponse BasicAuthPOST(String urlString, Map<String,String> headers, String user, String pwd,  Object json) throws IOException {
		String body = toJson(json);
		return BasicAuthPOST(urlString, headers, user, pwd, body,  "application/json");
	}

//	public static HttpResponse BasicAuthPUT(String urlString, String user, String pwd,  Object json) throws IOException {
//		String body = toJson(json);
//		return BasicAuthPUT(urlString, null, user, pwd, body,  "application/json");
//	}
		
	public static HttpResponse POST(String urlString, Map<String,String> headers, String body, String contentType) throws IOException {
		return MakeHttpRequest(urlString, headers, body, "POST", contentType);
	}
	
	public static HttpResponse GET(String urlString, Map<String,String> headers) throws IOException {
		return MakeHttpRequest(urlString, headers, (String)null, "GET", null);
	}

//	public static HttpResponse DELETE(String urlString, Map<String,String> headers) throws IOException {
//		return DELETE(urlString, headers, null, "application/text"); 
//	}
	
	public static HttpResponse DELETE(String urlString, Map<String,String> headers, String body, String contentType) throws IOException {
		return MakeHttpRequest(urlString, headers, body, "DELETE", contentType);
	}
	
	public static HttpResponse PUT(String urlString, Map<String,String> headers, String body, String contentType) throws IOException {
		return MakeHttpRequest(urlString, headers, body, "PUT", contentType);
	}

	public static HttpResponse BasicAuthPOST(String urlString, Map<String,String> headers, String user, String pwd, byte[] body, String contentType) throws IOException {
		headers = getAuthHeader(headers, user,pwd);
		return MakeHttpRequest(urlString, headers, body, "POST", contentType);
	}

	public static HttpResponse BasicAuthPOST(String urlString, Map<String,String> headers, String user, String pwd, InputStream stream, String contentType) throws IOException {
		headers = getAuthHeader(headers, user,pwd);
		return MakeHttpRequest(urlString, headers, stream, 0, "POST", contentType);
	}
	
	public static HttpResponse BasicAuthPOST(String urlString, Map<String,String> headers, String user, String pwd, String body, String contentType) throws IOException {
		headers = getAuthHeader(headers, user,pwd);
		return POST(urlString, headers, body, contentType);
	}
	
	public static HttpResponse BasicAuthPUT(String urlString, Map<String,String> headers, String user, String pwd, String body, String contentType) throws IOException {
		headers = getAuthHeader(headers, user,pwd);
		return PUT(urlString, headers, body, contentType);
	}

	public static HttpResponse BasicAuthPUT(String urlString, String user, String pwd,  Object json) throws IOException {
		return BasicAuthPUT(urlString, null, user, pwd, json);
	}
	
	public static HttpResponse BasicAuthPUT(String urlString, Map<String,String> headers, String user, String pwd,  Object json) throws IOException {
		String body = toJson(json);
		return BasicAuthPUT(urlString, headers, user, pwd, body,  "application/json");
	}

	public static HttpResponse BasicAuthDELETE(String urlString, Map<String,String> headers, String user, String pwd, String body, String contentType) throws IOException {
		headers = getAuthHeader(headers, user,pwd);
		return DELETE(urlString, headers, body, contentType);
	}

	public static HttpResponse BasicAuthDELETE(String urlString, String user, String pwd) throws IOException {
		return BasicAuthDELETE(urlString, null, user, pwd, null, "application/json");
	}

	public static HttpResponse BasicAuthDELETE(String urlString, String user, String pwd, Object json) throws IOException {
		String body = toJson(json);
		return BasicAuthDELETE(urlString, null, user, pwd, body, "application/json");
	}

	public static HttpResponse BasicAuthGET(String urlString, Map<String,String> headers, String user, String pwd) throws IOException {
		headers = getAuthHeader(headers, user,pwd);
		return GET(urlString, headers);
	}


	protected static Map<String,String> getAuthHeader(Map<String,String> headers, String user, String pwd) {
		String auth = BasicAuthToken.getToken(user, pwd);
		if (auth != null) {
			if (headers == null)
				headers = new HashMap<String,String>();
			headers.put("authorization", auth);
		}
			
		return headers;
	}

	public static class HttpResponse { 
		protected final int responseCode;
		protected final byte[] body;
		protected final byte[] errorMessage;
		protected transient String bodyString = null;
		protected transient String errorString = null;
		protected final String contentEncoding;
		protected final Map<String,List<String>> headers;

		private HttpResponse(int responseCode, byte[] body, byte[] errorMsg, Map<String, List<String>> headers, String contentEncoding) {
			super();
			this.responseCode = responseCode;
			this.body = body;
			this.errorMessage = errorMsg;
			this.headers = headers;
			this.contentEncoding = contentEncoding;
			// Set these for debugging reasons.
			getErrorString();
			getBodyString();

		}

		/**
		 * Create a non-error response
		 * @param responseCode
		 * @param body
		 * @param headers
		 * @param contentEncoding
		 */
		public HttpResponse(int responseCode, byte[] body, Map<String, List<String>> headers, String contentEncoding) {
			this(responseCode, body, null, headers, null);
		}


		/**
		 * Create an error response.
		 * @param responseCode
		 * @param errorMsg
		 * @param headers
		 */
		public HttpResponse(int responseCode, byte[] errorMsg, Map<String, List<String>> headers) {
			this(responseCode, null, errorMsg, headers, null);
		}


//		public HttpResponse(int responseCode, byte[] body, Map<String, List<String>> headers) {
//			this(responseCode, String.valueOf(body), headers);
//		}

		public int getResponseCode() {
			return this.responseCode;
		}
		
		Charset UTF8_CHARSET = Charset.forName("UTF-8");
		Charset UTF16_CHARSET = Charset.forName("UTF-16");
	
		public boolean isSuccess() {
			return this.responseCode >= 200 && responseCode < 300;
		}

		/**
		 * @return the body, null if not a successful request.
		 */
		public String getBodyString() {
			if (isSuccess() && bodyString == null) {
				Charset charSet = UTF8_CHARSET;
				if (contentEncoding != null) {
					charSet = Charset.forName(contentEncoding);
				}
				bodyString = new String(body,charSet); 

			}
			return bodyString;
		}
		
		/**
		 * Returns null if no error occurred.
		 * @return 
		 */
		public String getErrorString() {
			if (!isSuccess() && errorString == null) {
				Charset charSet = UTF8_CHARSET;
				if (contentEncoding != null) {
					charSet = Charset.forName(contentEncoding);
				}
				errorString = new String(errorMessage,charSet); 

			}
			return errorString;
		}

		/**
		 * Returns null if an error occurred.
		 * @return
		 */
		public byte[] getBody() {
			return body;
		}

		/**
		 * @return the headers
		 */
		public Map<String, List<String>> getHeaders() {
			return headers;
		}

		/**
		 * Can be called when {@link #isSuccess()} returns false to creaet a message from the response code and error message..
		 * @param msg
		 * @return
		 */
		public String getExceptionMessage(String msg) {
			msg = msg + ": (code " + this.getResponseCode() + ") " + this.getErrorString();
			return msg;
		}
		/**
		 * Can be called when {@link #isSuccess()} returns false.
		 * @param msg
		 * @return
		 */
		public IOException getIOException(String msg) {
			return new IOException(getExceptionMessage(msg));
		}
	}

	

	
	/**
	 * A convenience over {@link #MakeHttpRequest(String, Map, byte[], String, String)}.
	 */
	public static HttpResponse MakeHttpRequest(String urlString, Map<String,String> headers, String body, String method, String contentType) throws IOException	{

		return MakeHttpRequest(urlString, headers, body == null ? null : body.getBytes(), method, contentType);
	}
	
	static String UserAgent = "Java/" + System.getProperty("java.version", "1.6.0_26");
	private static boolean DetailedLogging = false;
	private static PrintStream PrintLogger = null;
	private static String ClassName = HttpUtil.class.getSimpleName();

	public static boolean isDetailedLogging() {
		return PrintLogger != null && DetailedLogging;
	}

	/**
	 * Enable logging as either detailed or errors only.
	 * Logging will include timestamps and other prefix information.
	 * @param stream the stream where log messages are written. 
	 * @param detailed if true, then show all requests, otherwise only exceptions and errors.
	 */
	public static void setLogging(OutputStream stream, boolean detailed) {
		if (stream == null)
			PrintLogger = null;
		else if (stream instanceof PrintStream) 
			PrintLogger = (PrintStream)stream;
		else
			PrintLogger = new PrintStream(stream);
		DetailedLogging = detailed;
	}
	
	private static String getDateTime() {
		Date now = new Date();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return simpleDateFormat.format(now);
	}

	private static String formatLogMessage(String logType, String logmsg) {
		return getDateTime() + " " + ClassName + "(" + logType + "): " + logmsg;
	}

	static void logResponse(HttpResponse resp) {
		if (PrintLogger != null && DetailedLogging) {
			String msg  = "Response(" + resp.getResponseCode() + "): " + (resp.isSuccess() ? resp.getBodyString() : resp.getErrorString());
			logInfo(msg);
		}
	}

	static void logDetailed(String msg) {
		if (PrintLogger != null && DetailedLogging) {
			logInfo(msg);
		}
	}

	static void logInfo(String msg) {
		log("I", msg);
	}

	static void logException(String when, Exception e) {
		String name = e.getClass().getSimpleName();
		if (when == null)
			when = name + " - "; 
		else
			when = name + " " + when + " - ";
		logError(when + e.getMessage() );
	}

	static void logError(String msg) {
		log("E", msg);
	}

	private static void log(String logType, String msg) {
		if (PrintLogger != null) {
			PrintLogger.println(formatLogMessage(logType, msg));
			PrintLogger.flush();
		}
	}

	private static void sendStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = is.read(buffer)) != -1) {
        	os.write(buffer, 0, bytesRead);
        }	
	}

	static void logRequest(HttpURLConnection connection, InputStream stream) throws IOException {
		if (PrintLogger != null && DetailedLogging) {
			String method = connection.getRequestMethod();
			String url = connection.getURL().toString();
			Map<String,List<String>> headers = connection.getRequestProperties();
			String msg = method + " to " + url + " headers=" + headers; 
			if (stream == null)  { 
				msg += " body=null";
			} else {	
				boolean isByteArray = stream instanceof ByteArrayInputStream;
				if (isByteArray) {
					ByteArrayInputStream bis = (ByteArrayInputStream)stream;
					bis.mark(0);
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					sendStream(bis,bos);
					bis.reset();
					byte[] body = bos.toByteArray();
					msg += " body=" + (body == null ? "(null)" : new String(body));
				} else {
					msg += " sending streamed data";
				}
			}
			logInfo(msg);
		}
	}
	
	/**
	  * Makes an HTTP request to a given url
	  * @param urlString destination url
	  * @param headers optional headers to add
	  * @param body optional body to add.
	  * @param method request method to use, optional. defaults to {@value #DEFAULT_REQUEST_METHOD}.
	  * @param contentType content type of body.  Optional. defaults to {@value #DEFAULT_CONTENT_TYPE}.
	  * @return
	  * @throws IOException if bad url or other error write/reading to/from the url connection.
	  */
	public static HttpResponse MakeHttpRequest(String urlString, Map<String,String> headers, byte[] body, String method, String contentType) throws  IOException {
		ByteArrayInputStream bis;
		int bodyLength;
		if (body == null)  {
			bis = null;
			bodyLength = 0;
		} else {
			bis = new ByteArrayInputStream(body);
			bodyLength = body.length;
		}
		return MakeHttpRequest(urlString, headers, bis, bodyLength, method, contentType);
	}

	private static HttpResponse MakeHttpRequest(String urlString, Map<String,String> headers, InputStream body, int bodyLength, String method, String contentType) throws  IOException {
//		logRequest(method, urlString, body);
		try {
				if (urlString == null)
					throw new RuntimeException("Missing url input parameter");
				URL url;
					url = new URL(urlString);

				if (method == null)
					method = DEFAULT_REQUEST_METHOD;
				if (contentType == null)
					contentType = DEFAULT_CONTENT_TYPE;


					configureSSL(url);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();      
					connection.setConnectTimeout(TIMEOUT_MSEC);
					connection.setReadTimeout(TIMEOUT_MSEC);
					connection.setRequestMethod(method); 

					connection.setDoInput(true);
					connection.setDoOutput(body != null ? true : false);
					connection.setInstanceFollowRedirects(false); 
					connection.setUseCaches (false);

					// Some web sites want only a browser connecting, so pretend we are one.
					connection.setRequestProperty("User-Agent", UserAgent); 
					if (bodyLength > 0)
						connection.setRequestProperty("Content-Length", "" + Integer.toString(bodyLength));	
					connection.setRequestProperty("charset", "UTF-8");
					connection.setRequestProperty("Content-Type", contentType); 
					connection.setRequestProperty("Accept","*/*");
					if (headers != null) {
						for (String key : headers.keySet()) {
							String value = headers.get(key);
							connection.setRequestProperty(key, value);
						}
					}
					logRequest(connection,body);
					if (body != null) {
						// Write the request body.
						DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				        sendStream(body, wr);
						wr.flush();
						wr.close();
					}
					// Read the response
					HttpResponse resp = getResponse(connection);
					logResponse(resp);
					return resp;
		} catch (Exception e) {
			logException("on " + method + " to " + urlString, e);
			throw e;
		}
	}



	/**
	 * @param connection
	 * @param bos
	 * @return
	 * @throws IOException
	 */
	public static HttpResponse getResponse(HttpURLConnection connection) throws IOException {
		boolean success; 
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		InputStream is;
		try {
			is = connection.getInputStream();
			success = true;
		} catch (Exception e) {
			is = connection.getErrorStream();
			success = false;
		}
		if (is != null) {
			BufferedInputStream in = new BufferedInputStream(is);
			byte[] buffer = new byte[1024];
			int count;
			while ((count = in.read(buffer)) >= 0)
				bos.write(buffer, 0, count);
			in.close();
			is.close();
		}
		bos.flush();
		bos.close();
//			        ENGLogger.logger.info("Headers: " + connection.getHeaderFields());
//		connection.disconnect();
//				} catch (Exception e) {
//					throw new PMLException(e.getMessage(), e);
//				}
		int responseCode = connection.getResponseCode();
//					if (responseCode == 302) {	// A redirect
//						Object loc = connection.getHeaderFields().get("Location");
//						if (loc != null) {
//							String urlStr = loc.toString().replaceAll("\\[","").replaceAll("\\]", "");
//							return MakeHttpRequest(urlStr, headers, body, method, contentType);
//						}
//					} 
		HttpResponse response;
		if (!success) {
			response = new HttpResponse(responseCode, bos.toByteArray(), connection.getHeaderFields());
//						throw  new IOException("Response code " + responseCode + ":" + response.getErrorString());
		} else {
			response = new HttpResponse(responseCode, bos.toByteArray(), connection.getHeaderFields(), connection.getContentEncoding());
		}
		if (success != response.isSuccess())
			throw new RuntimeException("Success state is not as expected.");
		return response;
	}

	private static String requestToString(HttpURLConnection connection) {
		String msg = "url: " + connection.getURL();
		msg += ", headers:" + connection.getHeaderFields();
		return msg;
	}

//	private static void logRequest(String urlString, byte[] body, HttpURLConnection connection) {
//		String method = connection.getRequestMethod();
//
//		String headers = null;
//		for (String key : connection.getRequestProperties().keySet()) {
//			if (headers==null)
//				headers="";
//			else
//				headers += ";";
//			String value = connection.getRequestProperties().get(key).toString();
//			headers += key + "=" + value;
//		}
//		if (urlString.contains("https"))
//			ENGLogger.logger.fine("Making ",method, " request to ", urlString, ", headers=", headers); // No body
//		else
//			ENGLogger.logger.fine("Making ",method, " request to ", urlString, ", headers=", headers, ", body=",  new String(body));
//	}

	public HttpUtil() {
		super();
	}

		
	public static <T> T jsonBasicAuthGET(String url, String user, String pwd, Class<T> clazz) throws IOException {
		return jsonBasicAuthGET(url, null, user,pwd,clazz);
	}

	public static <T> T jsonBasicAuthGET(String url, Map<String,String> headers, String user, String pwd, Class<T> clazz) throws IOException {
		HttpResponse resp = BasicAuthGET(url, headers, user, pwd);
		T json = handleJsonResponse(clazz, resp);
		return json;
	}

	public static <T> List<T> jsonBasicAuthGETList(String url, String user, String pwd, Class<T> clazz) throws IOException {
		HttpResponse resp = BasicAuthGET(url, null, user, pwd);
		List<T> json = handleJsonListResponse(clazz, resp);
		return json;
	}

		
	public static <T> T jsonBasicAuthPOST(String url, Object jsonBody, String user, String pwd, Class<T> clazz) throws IOException {
		HttpResponse resp = BasicAuthPOST(url, user, pwd, jsonBody);
		T json = handleJsonResponse(clazz, resp);
		return json;
	}

	public static <T> T jsonPOST(String url, Map<String,String> headers, Object jsonBody, Class<T> clazz) throws IOException {
		String body = toJson(jsonBody);
		HttpResponse resp = POST(url, headers, body, "application/json");
		T json = handleJsonResponse(clazz, resp);
		return json;
	}

	public static <T> T jsonDELETE(String url, Map<String,String> headers, Object jsonBody, Class<T> clazz) throws IOException {
		String body = toJson(jsonBody);
		HttpResponse resp = DELETE(url, headers, body, "application/json");
		T json = handleJsonResponse(clazz, resp);
		return json;
	}

	public static <T> T jsonGET(String url, Map<String,String> headers, Class<T> clazz) throws IOException {
		HttpResponse resp = GET(url, headers) ;
		T json = handleJsonResponse(clazz, resp);
		return json;
	}

	public static <T> List<T> jsonGETList(String url, Map<String,String> headers, Class<T> clazz) throws IOException {
		HttpResponse resp = GET(url, headers) ;
		return handleJsonListResponse(clazz, resp);
	}
	
	public static <T> List<T> jsonDELETEList(String url, Map<String,String> headers, Class<T> clazz) throws IOException {
		HttpResponse resp = DELETE(url, headers, null, "application/json") ;
		return handleJsonListResponse(clazz, resp);
	}

//	String respStr = resp.getBodyString();

	public static <T> List<T> jsonBasicAuthPOSTList(String url, Object jsonBody, String user, String pwd, Class<T> clazz) throws IOException {
		HttpResponse resp = BasicAuthPOST(url, user, pwd, jsonBody);
		List<T> json = handleJsonListResponse(clazz, resp);
		return json;
	}
	
	/**
	 * @param clazz
	 * @param resp
	 * @return
	 * @throws IOException if the resp is not a success or if the body is empty.
	 */
	private static <T> T handleJsonResponse(Class<T> clazz, HttpResponse resp) throws IOException {
		if (clazz == null)
			throw new RuntimeException("clazz can not be null");
		if (!resp.isSuccess()) 
			throw new IOException(resp.getResponseCode() + ": " + resp.getErrorString());
		String r = resp.getBodyString();
		if (r == null)
			throw new IOException("Response body is unexpectedly empty and should have contained a JSON object.");
		T json = null;
		json = fromJson(r, clazz);
		return json;
	}


	/**
	 * Parse a list of the given class from the given response.
	 * @param clazz class of objects in the expected list.
	 * @param resp 
	 * @return 
	 * @throws IOException if resp is not successful or respond body is null.
	 */
	public static <T> List<T> handleJsonListResponse(Class<T> clazz, HttpResponse resp) throws IOException {
		if (clazz == null)
			throw new RuntimeException("clazz can not be null");
		if (!resp.isSuccess()) 
			throw new IOException(resp.getResponseCode() + ": " + resp.getErrorString());
		String r = resp.getBodyString();
		if (r == null)	// This is probably redundant with isSuccess(), but trying to extra safe in the face of issue #181 (duplicate devices registered)
			throw new IOException("null response body");
		List<T> json = null;
		json = fromJsonList(r, clazz);
		return json;
	}

	public static String toJson(Object obj) {
		if (obj == null)
			return "";
		return gson.toJson(obj);
	}

	/**
	 * Parse the string into an instance of the given class.
	 * @param json
	 * @param clazz
	 * @return
	 * @throws IOException if json could not be parsed into the given class.
	 */
	public static <T> T fromJson(String json, Class<T> clazz) throws IOException {
		return fromJson(json,clazz,false);
	}
	
	private static <T> T fromJson(String json, Class<T> clazz, boolean alreadyCaughtException) throws IOException {
		try {
			return gson.fromJson(json, clazz);
		} catch (Exception e){
			if (!alreadyCaughtException && clazz.equals(String.class)) 	{ // Strings with whitespace seem to fail conversion unless surrounded by "
				json = StringUtil.escape(json, '"', '\\');
				json = (String)fromJson("\"" + json + "\"", clazz, true); 
				json = StringUtil.deescape(json, '"', '\\');
				return (T)json;
			}
			throw new IOException(json + " : does not represent an instance of class + " + clazz.getName());
		}
	}

	public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
		Object [] array = (Object[])java.lang.reflect.Array.newInstance(clazz, 0);
		array = gson.fromJson(json, array.getClass());
		List<T> list = new ArrayList<T>();
		for (int i=0 ; i<array.length ; i++)
			list.add(clazz.cast(array[i]));
		return list; 
	}
	

	public static <T> T jsonBasicAuthDELETE(String url, Object jsonBody, String user, String pwd, Class<T> clazz) throws IOException {
		HttpResponse resp = BasicAuthDELETE(url, user, pwd, jsonBody);
		return handleJsonResponse(clazz, resp);
//		String respStr = resp.getBodyString();
//		if (!resp.isSuccess() || respStr == null) 
//			throw new IOException("Error on url " + url + ": " + resp.getErrorString());
//		T json = fromJson(respStr, clazz);
//		return json;
	}
	

	public static <T> List<T> jsonBasicAuthDELETEList(String url, Object jsonBody, String user, String pwd, Class<T> clazz) throws IOException {
		HttpResponse resp = BasicAuthDELETE(url, user, pwd, jsonBody);
//		String respStr = resp.getBodyString();
		return handleJsonListResponse(clazz, resp);
//		if (!resp.isSuccess() || respStr == null) 
//			throw new IOException("Error on url " + url + ": " + resp.getErrorString());
//		List<T> json = fromJsonList(respStr, clazz);
//		return json;
	}

	/**
	 * Parse through a map to get the requested nest element.
	 * @param map
	 * @param keys an array of keys defining the path to the requested element.   
	 * @return null if there is not path in the given map as defined by the set of keys.
	 */
	public static Object getNestedKeyValue(Map<String, Object> map, String[] keys) {
		Object lastValue = map;
		for (String key : keys) {
			if (lastValue == null || !(lastValue instanceof Map)) 
				return null;
			map = (Map)lastValue;
			lastValue = map.get(key);
		}
		return lastValue;
	}







//	public static <T> T getNestedKeyValue(Map<String, Object> map, String[] keys, Class<T> clazz) throws ClassCastException {
//		Object obj = getNestedKeyValue(map, keys);
//		if (obj != null)  
//			return clazz.cast(obj);
//		else
//			return null;
//	}

}
