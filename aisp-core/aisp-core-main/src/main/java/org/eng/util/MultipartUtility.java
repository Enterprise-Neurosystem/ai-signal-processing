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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eng.util.HttpUtil.HttpResponse;

/**
 * This utility class provides an abstraction layer for sending multipart HTTP
 * POST requests to a web server.
 *
 * @author www.codejava.net
 */
public class MultipartUtility {
    private final String boundary;
    private static final String LINE_FEED = "\r\n";
    private HttpURLConnection httpConn;
    private String charset;
    private OutputStream outputStream;
    private PrintWriter writer;
	private final String requestURL;
    
    private static class DebugPrintWriter extends PrintWriter {


		public DebugPrintWriter(OutputStreamWriter outputStreamWriter, boolean b) {
			super(outputStreamWriter, b);
		}

		/* (non-Javadoc)
		 * @see java.io.PrintWriter#append(char)
		 */
		@Override
		public PrintWriter append(CharSequence c) {
			System.out.print(c);
			System.out.flush();
			return super.append(c);
		}

    	
    }
    
    private static class DebugOutputStream extends OutputStream {

    	OutputStream os;
    	
    	int len = 0;
    	private void show(byte b) {
    		if (b == '\r' || b == '\n')
    			len = 0;
    			
    		if (len > 80) {
    			System.out.println("");
    			len = 0;
    		}
    		String s = new String(new byte[] {b} ,0);
    		System.out.print(s);
    		System.out.flush();
    		len++;
    	}

  		public DebugOutputStream(OutputStream os) {
  			this.os = os;
  		}

		@Override
		public void write(int b) throws IOException {
			show((byte)b);
			os.write(b);
		}

		/* (non-Javadoc)
		 * @see java.io.OutputStream#write(byte[])
		 */
		@Override
		public void write(byte[] b) throws IOException {
			this.write(b,0,b.length);
		}

		/* (non-Javadoc)
		 * @see java.io.OutputStream#write(byte[], int, int)
		 */
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			for (int i=0 ; i<len ; i++)
				show(b[off+i]);
			os.write(b, off, len);
		}

		/* (non-Javadoc)
		 * @see java.io.OutputStream#flush()
		 */
		@Override
		public void flush() throws IOException {
			os.flush();
		}

		/* (non-Javadoc)
		 * @see java.io.OutputStream#close()
		 */
		@Override
		public void close() throws IOException {
			os.close();
		}



      	
      }
    
    /**
     * A convenience over {@link #MultipartUtility(String, Map, String)} using no headers and UTF-8 charset.
     * @param requestURL
     * @throws IOException
     */
    public MultipartUtility(String requestURL) throws IOException {
    	this(requestURL, null, "UTF-8");
    }

    /**
     * This constructor initializes a new HTTP POST request with content type
     * is set to multipart/form-data
     *
     * @param requestURL
     * @param map 
     * @param charset
     * @throws IOException
     */
    public MultipartUtility(String requestURL, Map<String, String> headers, String charset) throws IOException {
        this.charset = charset;

        // creates a unique boundary based on time stamp
        this.requestURL = requestURL;	// Saved for error messages later.
        boundary = "===" + System.currentTimeMillis() + "===";
        URL url = new URL(requestURL);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setConnectTimeout(HttpUtil.TIMEOUT_MSEC);
        httpConn.setReadTimeout(HttpUtil.TIMEOUT_MSEC);
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true); 
        httpConn.setDoInput(true);
		httpConn.setRequestMethod("POST");
        httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=\"" + boundary + "\"");
        httpConn.setRequestProperty("User-Agent", HttpUtil.UserAgent); 
		httpConn.setRequestProperty("Accept","*/*");
		httpConn.setRequestProperty("charset", charset);
		httpConn.setInstanceFollowRedirects(false);
		addRequestHeaders(headers);
//		httpConn.setRequestProperty("Transfer-Encoding","identity");
		
//        addRequestHeaders(headers);
        HttpUtil.logRequest(httpConn, null);
        outputStream = httpConn.getOutputStream();
//        outputStream = new DebugOutputStream(outputStream);
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);
//        writer = new DebugPrintWriter(new OutputStreamWriter(outputStream, charset), true);
    }

	/**
	 * @param headers
	 */
	public void addRequestHeaders(Map<String, String> headers) {
		if (HttpUtil.isDetailedLogging())
			HttpUtil.logInfo("Adding headers: " + headers); 
		if (headers != null && headers.size() > 0) {
        	for (String key : headers.keySet()) {
        		String value = headers.get(key);
        		httpConn.setRequestProperty(key,value);
        	}
        }
	}

    public void addPlainTextField(String name, String value) {
    	this.addFormField(name, value, "text/plain");
    }

    /**
     * Adds a form field to the request
     *
     * @param name  field name
     * @param value field value
     */
    public void addFormField(String name, String value, String contentType) {
		if (HttpUtil.isDetailedLogging())
			HttpUtil.logInfo("Adding field: name=" + name+ ", value=" + value + ", contentType=" + contentType);
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + name + "\"") .append(LINE_FEED);
//        writer.append("Content-Type: " + contentType + "; charset=" + charset).append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(value).append(LINE_FEED);
//        writer.append(value);
        writer.flush();
    }

    /**
     * Adds a upload file section to the request
     *
     * @param fieldName  name attribute in <input type="file" name="..." />
     * @param uploadFile a File to be uploaded
     * @throws IOException
     */
    public void addFilePart(String fieldName, File uploadFile) throws IOException {
        FileInputStream inputStream = new FileInputStream(uploadFile);
        String fileName = uploadFile.getName();
        addFilePart(fieldName, fileName, URLConnection.guessContentTypeFromName(fileName), inputStream);
    }

	/**
	 * @param fieldName
	 * @param fileName
	 * @param inputStream closed internally. 
	 * @throws IOException
	 */
	public void addFilePart(String fieldName, String fileName, String contentType, InputStream inputStream) throws IOException {
		if (HttpUtil.isDetailedLogging())
			HttpUtil.logInfo("Adding stream: name=" + fileName + ", fileName=" + fileName + ", contentType=" + contentType);
		writer.append("--" + boundary).append(LINE_FEED);
		if (fileName == null)
			fileName = "";
		writer.append( "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"")
                .append(LINE_FEED);
        writer.append( "Content-Type: " + contentType)
                .append(LINE_FEED);
//        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();
//        System.out.println("begin bytes here");

        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();
//        System.out.println("end bytes above");

        writer.append(LINE_FEED);
        writer.flush();
	}

    /**
     * Adds a header field to the request.
     *
     * @param name  - name of the header field
     * @param value - value of the header field
     */
    public void addHeaderField(String name, String value) {
		if (HttpUtil.isDetailedLogging())
			HttpUtil.logInfo("Adding header: name=" + name+ ", value=" + value);
        writer.append(name + ": " + value).append(LINE_FEED);
        writer.flush();
    }

    /**
     * Completes the request and receives response from the server.
     *
     * @return a list of Strings as response in case the server returned
     * status OK, otherwise an exception is thrown.
     * @throws IOException
     */
    public HttpResponse finish() throws IOException {

//        writer.append(LINE_FEED).flush();
    	try {
			writer.append("--" + boundary + "--").append(LINE_FEED);
			writer.close();
			HttpResponse resp = HttpUtil.getResponse(httpConn);
			HttpUtil.logResponse(resp);
			return resp;
    	} catch (Exception e) {
    		HttpUtil.logException("getting response from multipart POST to " + requestURL, e);
    		throw e;
    	}
    
//        // checks server's status code first
//        int status = httpConn.getResponseCode();
//        if (status == HttpURLConnection.HTTP_OK) {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(
//                    httpConn.getInputStream()));
//            String line = null;
//            while ((line = reader.readLine()) != null) {
//                response.add(line);
//            }
//            reader.close();
//            httpConn.disconnect();
//        } else {
//            throw new IOException("Server returned non-OK status: " + status);
//        }
//
//        return response;
    }
    
    private List<String> finishOrig() throws IOException {
        List<String> response = new ArrayList<String>();

        writer.append(LINE_FEED).flush();
        writer.append("--" + boundary + "--").append(LINE_FEED);
        writer.close();

        // checks server's status code first
        int status = httpConn.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpConn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            reader.close();
            httpConn.disconnect();
        } else {
            throw new IOException("Server returned non-OK status: " + status);
        }

        return response;
    }
}

