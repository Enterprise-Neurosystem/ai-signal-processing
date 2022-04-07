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
package org.eng.aisp.client.iotp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eng.aisp.AISPLogger;
import org.eng.util.ComponentProperties;

/**
 * Defines a component properties search space with the name 'iotp'.
 * The IoTP properties are used to define whether
 * <ol>
 * <li> iotp is enabled at all, and if enabled
 * <li> 0 or more IOT application client connections (org id, application api key, auth token and enabled users)
 * </ol>
 * The instance can then be used to determine which IOT instance is assigned to a given user name as identified in the properties.
 * Each instance defines the API-Key, Authentication-Token and enabled.users as follows:
 * <pre>
 * &lt;orgid&gt;.API-Key=...
 * &lt;orgid&gt;.Authentication-Token=...
 * &lt;orgid&gt;.enabled.users=...
 * </pre>
 * Where the key, token and user correspond to an IOT instance with the <code>orgid</code>.
 * An example properties file might look as follows:
 * <pre>
 * iotp.enabled=true
 * 
 * tkswpm.API-Key=a-tskwpm-3124435
 * tkswpm.Authentication-Token=43mlkjfddasf
 * tkswpm.enabled.users=userA@gmail.com,userB@ibm.com
 * 
 * abcde.API-Key=a-abcde-90adf09
 * abcde.Authentication-Token=234ka;le0a;
 * abcde.enabled.users=userC@gmail.com,userD@ibm.com
 * 
 * </pre>
 * 
 * @author dawood
 *
 */
public class IOTPlatformProperties extends ComponentProperties {

	public static final String IOT_PLATFORM_ENABLED_PROPERTY = "iotp.enabled";
	public final static String ENABLED_USERS_PROPERTY_NAME = "enabled.users";
	public final static String ORG_ID_PROPERTY_NAME = "Organization-ID";
	public final static String API_KEY_PROPERTY_NAME = "API-Key";
	public final static String AUTH_TOKEN_PROPERTY_NAME = "Authentication-Token";
//	Organization-ID=
//	API-Key=
//	Authentication-Token=
	private static IOTPlatformProperties instance = null;
	
	static {
		instance = new IOTPlatformProperties();
	}
	
	/**
	 * Get the global instance of PML properties.
	 * @return
	 */
	public static IOTPlatformProperties instance() {
		// In addition to getting the instance, we make sure that all copyrights and logging messages have happened.
		return instance;
	}
	
	
	/** Map of properties for each IoT instance as parsed by {@link #parseOrganizationInstances()}, keyed by organization id. */
	private final Map<String,Properties> orgProperties = new HashMap<String,Properties>();
	/** The list of sort organization ids. Provided to enable predictable results when there are conflicts */
	private final List<String> sortedOrgIDs = new ArrayList<String>();
	/** Connected application clients, keyed by organization id */
	private final Map<String,IOTPApplicationClient> appClients = new HashMap<String, IOTPApplicationClient>();
	/** Connected application clients, keyed by user name */
	private final Map<String, IOTPApplicationClient> userAppClientCache = new HashMap<String, IOTPApplicationClient>();
	/** Contains user names for which we've already tried to lookup the IoT instance */
	private final Set<String> cachedUsers = new HashSet<String>();	// User's we've looked up already.

	/**
	 * Protected to force use of static {@link #instance()} method.
	 */
	protected IOTPlatformProperties() {
		super("iotp");
	}
	
	/**
	 * Parse all the properties into this instance's {@link #orgProperties} field.
	 * For each <code>orgid</code>, as follows:
     * <pre>
     * &lt;orgid&gt;.API-Key=...
     * &lt;orgid&gt;.Authentication-Token=...
     * &lt;orgid&gt;.enabled.users=...
     * </pre>
	 * is effectively parsed to
     * <pre>
     * Organization-ID=orgid
     * API-Key=...
     * Authentication-Token=...
     * enabled.users=...
     * </pre
	 * @param propName
	 * @return
	 */
	private void parseOrganizationInstances() {
		Map<String,String> usersToOrgs = new HashMap<String,String>();
		
		for (Object key : this.getProperties().keySet())  {
			String propName = key.toString();
			int index = propName.indexOf(API_KEY_PROPERTY_NAME);
			if (index >= 0) {
				String orgID = propName.substring(0, index-1);
				if (sortedOrgIDs.contains(orgID)) 
					continue;
				
				Properties p = parseProperties(orgID);
				if (p == null) {
					AISPLogger.logger.severe("Properties for organization " + orgID + " were incomplete or mis-specified. Skipping this organization.");
					continue;
				}
				if (orgID != null) {
					orgProperties.put(orgID, p);
					sortedOrgIDs.add(orgID);
				}
				// Warn about and remove duplicate users 
				String users = p.getProperty(ENABLED_USERS_PROPERTY_NAME);
				if (users != null) {
					String[] userList = users.split(",");
					String newUsers = null;
					for (String u : userList) {
						u = u.trim();
						String org = usersToOrgs.get(u); 
						if (org != null) {
							AISPLogger.logger.warning("User " +  u + " listed under IoT organization " + orgID  
									+ " is already listed with IoT organization " + org + ". Disabling user in organization " + orgID);
						} else {
							usersToOrgs.put(u, orgID);
							if (newUsers != null)
								newUsers += ","; 
							else
								newUsers = "";
							newUsers += u;
						}
					}
					p.setProperty(ENABLED_USERS_PROPERTY_NAME, newUsers);
				}
			}
		}
		Collections.sort(sortedOrgIDs);
	}


	/**
	 * Search for properties that use the given orgID as a prefix and return a properties object
	 * that contains those properties, but with the prefix removed from the keys.
	 * The following,
     * <pre>
     * &lt;orgid&gt;.API-Key=...
     * &lt;orgid&gt;.Authentication-Token=...
     * &lt;orgid&gt;.enabled.users=...
     * </pre>
	 * is effectively parsed to
     * <pre>
     * Organization-ID=orgid
     * API-Key=...
     * Authentication-Token=...
     * enabled.users=...
     * </pre>
	 *  
	 * @param orgID
	 * @return null if the API-Key or Authentication-Token were not found for the given orgID.
	 */
	private Properties parseProperties(String orgID) {
		Properties props = new Properties();
		props.setProperty(ORG_ID_PROPERTY_NAME, orgID);
		String prefix = orgID + ".";
		int prefixLen = prefix.length();
		for (Object key : this.getProperties().keySet())  {
			String propName = key.toString();
			if (propName.startsWith(prefix)) {
				String subPropName = propName.substring(prefixLen); 
				String value = this.getProperties().getProperty(propName);
				props.setProperty(subPropName, value);
			}
		}
		if (!props.containsKey(AUTH_TOKEN_PROPERTY_NAME) || !props.containsKey(API_KEY_PROPERTY_NAME)) 
			props = null;
		return props;
	}

	/**
	 * Get a connected IOTPApplicationClient assigned to the given user name if this instance is enabled.
	 * Should be called between calls {@link #connect()} and {@link #disconnect()} otherwise null is always returned. 
	 * @param userName
	 * @return null if not connected or the user is not assigned to an application client instance or if {@link #isEnabled()} returns false. 
	 */
	public IOTPApplicationClient getApplicationClient(String userName) {
		if (!isEnabled())
			return null;
		
		IOTPApplicationClient app = userAppClientCache.get(userName);
		if (!cachedUsers.contains(userName)) {	// Not cached yet, look through properties 
			cachedUsers.add(userName);		// Mark that we've looked up this user.
			Properties props = getApplicationClientProperties(userName);
			if (props != null) {	// User is enabled for IoT.
				String orgID = props.getProperty(ORG_ID_PROPERTY_NAME);
				app = appClients.get(orgID);
				userAppClientCache.put(userName, app);
			} else {
				app = null;
			}
		} else {
			app = userAppClientCache.get(userName);
		}
		return app;
	}
	
	public boolean isEnabled() {
		boolean enabled = getProperty(IOTPlatformProperties.IOT_PLATFORM_ENABLED_PROPERTY, false);
		return enabled;
	}

	/**
	 * Connect all application clients identified in the properties file.
	 */
	public synchronized void connect(boolean verbose) {
		parseOrganizationInstances();

		if (!isEnabled())  {
			if (verbose)
				AISPLogger.logger.info("IoT is not enabled");
			return;
		}
	

		for (String orgID : sortedOrgIDs) {
			Properties props = orgProperties.get(orgID);
			String[] users = getEnabledUsers(orgID);
			if (users != null && users.length == 0)
				continue;	// enabled.users defined, but empty means we don't enable it.
			try {
				IOTPApplicationClient app = new IOTPApplicationClient(props);
				app.connect();
				appClients.put(orgID, app);
				if (verbose) 
					AISPLogger.logger.info("Connected as an application client to IOT Platform with Organization-ID " + orgID);
			} catch (IOException e) {
				AISPLogger.logger.severe("Could not connect to IOT platform as an application using Organization-ID " + orgID + ": " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/** 
	 * Get the users enabled for a given IoT organization.
	 * By convention,
	 * <ul>
	 * <li> If null is returned, then notification is done for all users.
	 * <li> If an empty list is returned, then no users receive notification. 
	 * </ul>
	 * @param orgID
	 * @return
	 */
	public String[] getEnabledUsers(String orgID) {
		Properties props = this.orgProperties.get(orgID);
		if (props == null)
			return new String[0];
		String enabledUsers = props.getProperty(ENABLED_USERS_PROPERTY_NAME);
		if (enabledUsers == null)
			return null;		// All users are enabled.
		enabledUsers = enabledUsers.trim();
		if (enabledUsers.equals(""))	
			return new String[0];	// No users are enabled.
		return enabledUsers.split(",");
	}

	/**
	 * Disconnect all application clients connected during {@link #connect()}.
	 */
	public synchronized void disconnect(boolean verbose) {
		for (IOTPApplicationClient app : appClients.values()) {
			try {
				app.disconnect();
				if (verbose) 
					AISPLogger.logger.info("Disconnected application client from IOT Platform with Organization-ID " + app.getOrganizationID());
			} catch (Exception e) {
				AISPLogger.logger.warning("Could not disconnect from IOT platform as an application using Organization-ID " + app.getOrganizationID() + ": " + e.getMessage());
			}
		}
		appClients.clear();
		userAppClientCache.clear();
		cachedUsers.clear();
	}

	/**
	 * Provided primarily for junit testing.
	 * @return
	 */
	public List<Properties> getApplicationClientProperties() {
		parseOrganizationInstances();
		return new ArrayList<Properties>(this.orgProperties.values());
	}

	/**
	 * Find the 1st IOT properties enabled for the given user, regardless of whether or not IOT is enabled.
	 * Organizations are search in lexigraphic order for the given user. 
	 * If the user is not listed with any instance, then look for the 1st instance that allows all users.
	 * @param userName
	 * @return null if user is not enabled. 
	 */
	public Properties getApplicationClientProperties(String userName) {
		parseOrganizationInstances();
		// Look for an instance that specifically names the user.
		for (String orgID : sortedOrgIDs) {
			Properties props = this.orgProperties.get(orgID);
			String users = props.getProperty(ENABLED_USERS_PROPERTY_NAME);
			if (users != null && users.contains(userName)) {
				return props;
			}
		}
		// Now look for an instance that lets anyone publish.
		for (String orgID : sortedOrgIDs) {
			Properties props = this.orgProperties.get(orgID);
			String users = props.getProperty(ENABLED_USERS_PROPERTY_NAME);
			if (users == null)
				return props;
		}
		return null;
	}

}
