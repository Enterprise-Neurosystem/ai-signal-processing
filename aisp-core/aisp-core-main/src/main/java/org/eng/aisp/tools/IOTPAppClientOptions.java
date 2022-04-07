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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.client.iotp.IOTPApplicationClient;
import org.eng.util.CommandArgs;
/**
 * Parses -iotp-properties, iotp-org, iotp-api-key, iotp-auth-token option values.
 * @author dawood
 *
 */
public class IOTPAppClientOptions extends UserPasswordOptions {
	@SuppressWarnings("hiding")
	public final static String OptionsHelp = 
	  "  -iotp-properties file : Specifies the file containing IOT platform connections.\n" 
	+ "     properties.  The following must be specified, if not specified on the command line.\n"
	+ "         Organization-ID, API-Key and Authentication-Token\n"
	+ "  -iotp-org org: Specifies the orginization ID of the IOT Platform to publish events.\n" 
	+ "  -iotp-api-key key: Specifies the API key for the IOT Platform organization.\n" 
	+ "  -iotp-auth-token token : Specifies the IOT Platform API key authorization token.\n" 
	+ "  The organization ID, api key and auth token must all be specified via one of the\n"
	+ "  mechanisms above.\n"
	+ UserPasswordOptions.OptionsHelp
	+ "   User/password authentication with IBM ID enables access to the IOT Platform.\n"
	;
	
	
	protected IOTPApplicationClient appClient;
	protected Properties iotpProperties;
	public static final String APIKEY_KEY = "API-Key";
	public static final String AUTHTOKEN_KEY = "Authentication-Token";
	public static final String ORGID_KEY = "Organization-ID";

	public IOTPAppClientOptions() {
		super();
	}

	protected IOTPAppClientOptions(String defaultUser, String defaultPwd) {
		super(defaultUser,defaultPwd);
	}
	/**
	 * Get the connected app client.
	 * Should only be called after {@link #parseOptions(CommandArgs)} and only if {@link #isIOTPEnabled()} returns true.
	 * @return never null.
	 * @throws AISPException 
	 */
	public IOTPApplicationClient getApplicationClient() throws IOException {
		if (appClient == null) {
			appClient = new IOTPApplicationClient(iotpProperties);
			appClient.connect();
		}
		return appClient;
	}
	
	/**
	 * Get the options.
	 * @param cmdargs
	 * @return false and issue an error message on stderr, otherwise true.
	 */
	public boolean parseOptions(CommandArgs cmdargs) {
		if (!super.parseOptions(cmdargs))
			return false;
		String orgID = cmdargs.getOption("iotp-org");
		String apiKey= cmdargs.getOption("iotp-api-key");
		String authToken = cmdargs.getOption("iotp-auth-token");
		String iotpPropertiesFile = cmdargs.getOption("iotp-properties");
		iotpProperties = new Properties();
		if (iotpPropertiesFile != null) {
			File f = new File(iotpPropertiesFile);
			if (!f.exists()) {
				System.err.println("IOTP properties file " + iotpPropertiesFile + " not found.");
				return false;
			}
			try {
				iotpProperties.load(new FileInputStream(f));
			} catch (IOException e) {
				System.err.println("Error loading IOTP properties " + iotpPropertiesFile + ": " + e.getMessage());
				return false;
			}
		}
		if (orgID != null)
			iotpProperties.put(ORGID_KEY, orgID);
		if (orgID != null)
			iotpProperties.put(APIKEY_KEY, apiKey);
		if (orgID != null)
			iotpProperties.put(AUTHTOKEN_KEY, authToken);
		
		boolean hasOne = iotpProperties.getProperty(ORGID_KEY) != null || 
						iotpProperties.getProperty(APIKEY_KEY) != null  ||
						iotpProperties.getProperty(AUTHTOKEN_KEY) != null;
		if (!hasOne) {	// 
			if (iotpPropertiesFile != null) {
				System.err.println("IOT properties file did not include necessary keys " + ORGID_KEY + ", " + APIKEY_KEY + " and " + AUTHTOKEN_KEY);
				return false;
			}
			iotpProperties.clear();	// Signal not to create the iotpClient.
		} else if (iotpProperties.getProperty(ORGID_KEY) == null) {
			System.err.println(ORGID_KEY + " was not specified in a properties file or on the command line.");
			return false;
		} else if (iotpProperties.getProperty(APIKEY_KEY) == null) {
			System.err.println(APIKEY_KEY + " was not specified in a properties file or on the command line.");
			return false;
		} else if (iotpProperties.getProperty(AUTHTOKEN_KEY) == null) {
			System.err.println(AUTHTOKEN_KEY + " was not specified in a properties file or on the command line.");
			return false;
		} 

		return true;
	}

}
