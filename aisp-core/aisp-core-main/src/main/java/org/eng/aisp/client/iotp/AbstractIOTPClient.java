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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.sensor.DeployedSensor;
import org.eng.aisp.user.UserProfile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ibm.iotf.client.IoTFCReSTException;
import com.ibm.iotf.client.api.APIClient;

/**
 * Provides useful methods that require and APIClient.
 * The APIClient is provided by the subclasses through the api() method.
 * @author dawood
 *
 */
public abstract class AbstractIOTPClient extends IOTPlatform {

//	protected APIClient apiClient = null;
	protected String sensorDeviceTypeName = null;
	protected final String orgID;
	protected final String apiKey;
	private String authToken;

	/**
	 * @param usingGWConnectedSensors if sensor devices accessed are created under a GW instead of as a free-standing device.
	 * @param orgID the organization identifier of an IOT platform instance
	 * @param apiKey the api key to use to authenticate with the platform.
	 * @param authToken the token used to authenticate with the platform.
	 */
	public AbstractIOTPClient(String orgID, String apiKey, String authToken) {
		super();
		this.orgID = orgID;
		this.apiKey = apiKey;
		this.authToken = authToken;
		if (orgID == null)
			throw new IllegalArgumentException("Org id is null");
		if (apiKey== null)
			throw new IllegalArgumentException("API key is null");
		if (authToken == null)
			throw new IllegalArgumentException("Auth token is null");
	}

	/**
	 * 
	 * @param usingGWConnectedSensors if sensor devices accessed are created under a GW instead of as a free-standing device.
	 * @param props properties containing keys for the values provided to {@link #IOTPApiClient(String, String, String)}. 
	 */
	public AbstractIOTPClient(Properties props) {
		this(props.getProperty(OrganizationIDKey), props.getProperty(APIKeyKey), props.getProperty(AuthenticationTokenKey)); 
	}

	/**
	 * Called on the subclass to get the APIClient instance after #connect() has been called.
	 * @return null if not connected.
	 */
	protected abstract APIClient api(); 

	/**
	 * A convenience method over {@link #api()} to throw an IllegalStateException("Not connected") if {@link #api()} returns null. 
	 * @return
	 */
	private APIClient getAPI() {
		APIClient apiClient = api();
		if (apiClient == null)
			throw new IllegalStateException("Not connected");
		return apiClient;
	}
	
	public String getOrganizationID() {
		return orgID;
	}

	public String getAPIKey() {
		return this.apiKey;
	}

	/**
	 * Get the APIClient instance configured by the properties in this instance.
	 * @return never null.
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 */
	protected APIClient getAPIClient() throws KeyManagementException, NoSuchAlgorithmException {
		Properties options = this.getConnectionProperties(); 
		APIClient myClient = new APIClient(options);
		return myClient;
	}

	/**
	 * Get the properties for connecting with API key.
	 * Properties include
	 * <ul>
	 * <li> OrganizationIDKey = value provided to constructor
	 * <li> APIKeyKey = value provided to constructor
	 * <li> AuthenticationTokenKey = value provided to constructor
	 * <li> AuthenticationMethodKey = "apikey"
	 * <li> id = "API" + random
	 * </ul>
	 * @return
	 */
	protected Properties getConnectionProperties() {
		Properties options = new Properties();
		// options.put("org", "jr9k9o");
		// options.put("API-Key", "a-jr9k9o-wgyf3jkdlo");
		//options.put("Authentication-Token", "gNPi+6L91Y8-qu(mUt");
		options.put(OrganizationIDKey,orgID);
		options.put(AuthenticationMethodKey,AuthMethodAPI);
		options.put(APIKeyKey, apiKey);
		options.put(AuthenticationTokenKey, authToken);
		options.put("id", "API" + (Math.random() * 10000));
		return options;
	}

	/**
	 * See if the given device is created.
	 * A side effect is that the device type for then sensor will be created.
	 * @param userName
	 * @param sensorName
	 * @return
	 * @throws AISPException
	 */
	public boolean isDeviceCreated(UserProfile userProfile, DeployedSensor sensor) throws IOException {
		String devID = getSensorDeviceID(userProfile, sensor);
//		String devType = getSensorDeviceTypeID();
		return isDeviceCreated(devID);
//		try {
//			return getAPI().isDeviceExist(devType, devID);
//		} catch (IoTFCReSTException e) {
//			throw new IOException("Could not check if device exists: " + e.getMessage(), e);
//		}
	}

	/**
	 * See if the device with the given user-defined name is created.
	 * @param devID user-defined name/id of device.
	 * @return true if exists.
	 * @throws IOException
	 */
	public boolean isDeviceCreated(String devID) throws IOException {
		String devType = getSensorDeviceTypeID();
		try {
			return getAPI().isDeviceExist(devType, devID);
		} catch (IoTFCReSTException e) {
			throw new IOException("Could not check if device exists: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Create a device that does not already exist.
	 * @param uprofile
	 * @param sensor
	 * @param sensorID
	 * @return null if device exists, otherwise the auth-token that must be saved by the caller for subsequent connections
	 * to this device.  This token will generally need to be provided to IOTSensorClient's constructor.
	 * @throws AISPException
	 */
	public String addDevice(UserProfile uprofile, DeployedSensor sensor, String sensorID) throws IOException {
		String userName = uprofile.getUserName(); 
		String sensorName = sensor.getSensorProfile().getName();
		if (isDeviceCreated(uprofile, sensor))
			return null;
		
		String deviceID = getSensorDeviceID(uprofile, sensor);
		JsonObject device = getDeviceJson(deviceID, uprofile, sensor, sensorID); 
		try {
			String typeID = this.assureSensorTypeNameExists();
			JsonObject resp = getAPI().registerDevice(typeID, device);
			JsonElement el = resp.get("authToken");
			String token = el.getAsString();
			return token;
		} catch (IoTFCReSTException e) {
			throw new IOException("Can not add device named " + sensorName + " for user " + userName, e);
		}
	}

	/**
	 * Add the device with the given user-defined ID/name.
	 * @param deviceID name/id to assign to the device. must be unique on the platform.
	 * @param deviceInf optional properties to attach to the device in it's deviceInfo field.
	 * @param dataSet optional properties to attach to the device in it's metadata field.
	 * @return null if device with given deviceID already exists, otherwise the deviceToken.
	 * @throws IOException
	 */
	public String addDevice(String deviceID, Properties deviceInfo, Properties metaData) throws IOException {
		if (isDeviceCreated(deviceID))
			return null;
		
//		String deviceID = getSensorDeviceID(uprofile, sensor);
		JsonObject device = getDeviceJson(deviceID, deviceInfo, metaData); 
		try {
			String typeID = this.assureSensorTypeNameExists();
			JsonObject resp = getAPI().registerDevice(typeID, device);
			JsonElement el = resp.get("authToken");
			String token = el.getAsString();
			return token;
		} catch (IoTFCReSTException e) {
			throw new IOException("Can not add device ID " + deviceID,e);
		}
	}
	
	public boolean removeDevice(UserProfile uprofile, DeployedSensor sensor) throws IOException {
//		String userName = uprofile.getUserName(); 
//		String sensorName = sensor.getSensorProfile().getName();
		if (!isDeviceCreated(uprofile, sensor))
			return true;	// act like we succeeded
		
		String deviceID = this.getSensorDeviceID(uprofile, sensor);
		String deviceType = this.getSensorDeviceTypeID();
		return removeDevice(deviceType, deviceID);
		
	}

	/**
	 * @param deviceType
	 * @param deviceID
	 * @return
	 * @throws AISPException
	 */
	public boolean removeDevice(String deviceType, String deviceID) throws IOException {
		try {
			return getAPI().deleteDevice(deviceType, deviceID);
		} catch (IoTFCReSTException e) {
			throw new IOException("Could not delete device with id " + deviceID, e);
		}
	}

	/**
	 * Get, and add if necessary,  create the type name for the sensor.
	 * @return
	 * @throws AISPException
	 */
	protected String assureSensorTypeNameExists() throws IOException {
		if (sensorDeviceTypeName != null)
			return sensorDeviceTypeName;
		String tmp = this.getSensorDeviceTypeID(); 
		addDeviceType(tmp, false);
		sensorDeviceTypeName = tmp; 
		return sensorDeviceTypeName;
	}

	public void connect() throws IOException {
//		if (this.api() != null)
//			return;
//	
//		try {
//			api() = getAPIClient(); 
//		} catch (Exception e) {
//			throw new AISPException(e.getMessage(), e);
//		}
	}

	public void disconnect() {
		// apiClient does not have disconnect()
	}

	/**
	 * Make sure the given device name exists
	 * @param deviceTypeName
	 * @throws AISPException
	 */
	protected void addDeviceType(String deviceTypeName, boolean isGateway) throws IOException {
		JsonObject deviceType = null;
		try {
			deviceType = api().getDeviceType(deviceTypeName);
		} catch (Exception e ) {	// 
		}
		if (deviceType == null) {
			deviceType = new JsonObject();
			deviceType.addProperty("id", deviceTypeName);
			try {
				if (isGateway)
					getAPI().addGatewayDeviceType(deviceType);
				else
					getAPI().addDeviceType(deviceType);
			} catch (IoTFCReSTException e) {
				throw new IOException("Can not add device type " + deviceTypeName, e);
			}
		}
	}

}
