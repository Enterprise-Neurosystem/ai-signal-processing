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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.client.iotp.IOTPApplicationClient;
import org.eng.aisp.client.iotp.IOTPSensorClient;
import org.eng.aisp.sensor.DeployedSensor;
import org.eng.aisp.sensor.SensorEnvironment;
import org.eng.aisp.sensor.SensorProfile;
import org.eng.aisp.user.UserProfile;
import org.eng.util.CommandArgs;

/**
 * Parses -iotp-properties, iotp-org, iotp-api-key, iotp-auth-token, sensor-name, user, sensor-location, and sensor-sublocation option values.
 * @author dawood
 *
 */
class IOTPSensorClientOptions extends IOTPAppClientOptions {

	@SuppressWarnings("hiding")
	public final static String OptionsHelp = 
	  IOTPAppClientOptions.OptionsHelp 
	+ "  -sensor-name name : Name of the sensor used to name the device on the IOT Platform. \n"
	+ "         Resulting name is combination of user name and this value.  Required.\n" 
	+ "  -sensor-location location: Location for the device created on the IOT Platform.\n" 
	+ "  -sensor-sublocation location: Sub-location for the device created on the IOT Platform.\n" 
	;
	
	private IOTPSensorClient sensorClient;
	private String location;
	private String sensorName;
	private String subLocation;
	UserProfile userProfile;
	DeployedSensor sensor;
	String sensorID = null;
	
	public IOTPSensorClientOptions(String defaultUser, String defaultPwd) {
		super(defaultUser,defaultPwd);
	}

	/**
	 * Get the options.
	 * @param cmdargs
	 * @return false and issue an error message on stderr, otherwise true.
	 */
	public boolean parseOptions(CommandArgs cmdargs) {
		if (!super.parseOptions(cmdargs))
			return false;
		if (!isIOTPEnabled()) 
			return true;
		
		sensorName = cmdargs.getOption("sensor-name");
		location = cmdargs.getOption("sensor-location", "undefined"); 
		subLocation = cmdargs.getOption("sensor-sublocation", "undefined");
		if (sensorName == null) {
			System.err.println("Sensor name must be provided for IOT connections.");
			return false; 
		}
		if (user == null) {
			System.err.println("User name must be provided for IOT connections.");
			return false; 
		}
		
		userProfile = new UserProfile(user);
		SensorProfile sensorProfile = new SensorProfile(sensorName, null, null);
		SensorEnvironment se = new SensorEnvironment(location, subLocation);
		sensor =new DeployedSensor(sensorID, sensorProfile,se);

		return true;
	}

	/**
	 * Valid after {@link #parseOptions(CommandArgs)} has been called.
	 * @return
	 */
	public boolean isIOTPEnabled() {
		return iotpProperties != null && iotpProperties.size() != 0;
		
	}

	/**
	 * Create the sensor client instance and connect it.
	 * Should only be called after {@link #parseOptions(CommandArgs)} and only if {@link #isIOTPEnabled()} returns true.
	 * Use the orgid, api key and api-key auth token to register the new device if needed, and set up
	 * the connection to the new or existing device by initialized {@link #sensorClient} field.   
	 * The device token is stored in a local file 'sensorName'-token.properties. 
	 * @return null and issue an message on error, otherwise the sensor client. 
	 * @throws AISPException
	 */
	public IOTPSensorClient getSensorClient() throws IOException {
		if (sensorClient != null) 
			return sensorClient;
		
		IOTPApplicationClient appClient = new IOTPApplicationClient(iotpProperties);
		appClient.connect();

		String deviceToken; 
		String tokenFile = sensorName + "-token.properties";
		Properties tokenProps = new Properties();
		String orgID = iotpProperties.getProperty(ORGID_KEY);
		
		if (appClient.isDeviceCreated(userProfile, sensor)) {
			// Get the device token which is stored locally.
			try {
				tokenProps.load(new FileInputStream(tokenFile));
			} catch (IOException e) {
				System.out.println("Could not load token file " + tokenFile + ": " + e.getMessage());
				appClient.disconnect();
				return null;
			}
			deviceToken = tokenProps.getProperty(AUTHTOKEN_KEY);
		} else {
			// Create the new device and store the token locally.
			deviceToken = appClient.addDevice(userProfile, sensor, sensorID);
			tokenProps.put(AUTHTOKEN_KEY, deviceToken);
			try {
				tokenProps.store(new FileOutputStream(tokenFile), "Token for sensor named '" + sensorName + "' on IOTP organization " + orgID); 
			} catch (IOException e) {
				appClient.disconnect();
				System.out.println("Could not write token file " + tokenFile + ": " + e.getMessage());
				return null;
			}
		}
		appClient.disconnect();
		sensorClient = new IOTPSensorClient(userProfile, sensor, orgID, deviceToken);
		sensorClient.connect();
		return sensorClient;
	}

	/**
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * @return the sensorName
	 */
	public String getSensorName() {
		return sensorName;
	}

	/**
	 * @return the subLocation
	 */
	public String getSubLocation() {
		return subLocation;
	}

	/**
	 * @return the userProfile
	 */
	public UserProfile getUserProfile() {
		return userProfile;
	}

	/**
	 * @return the sensor
	 */
	public DeployedSensor getSensor() {
		return sensor;
	}

}
