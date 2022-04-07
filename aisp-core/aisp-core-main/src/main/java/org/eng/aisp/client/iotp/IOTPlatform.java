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

import java.util.Map;
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.sensor.DeployedSensor;
import org.eng.aisp.user.UserProfile;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class IOTPlatform {

	// Keys
	public static final String OrganizationIDKey 		= "Organization-ID";
	public static final String AuthenticationMethodKey 	= "Authentication-Method";
	public static final String AuthenticationTokenKey 	= "Authentication-Token";
	public static final String APIKeyKey				= "API-Key";
	// Key values
	public static final String AuthMethodToken 			= "token";
	public static final String AuthMethodAPI			= "apikey";
	
	/** The device type name for audio sensor in our framework */
	public final static String AudioDeviceTypeID = "AudioSensor";
	public final static String GWAudioDeviceTypeID = "GWAudioSensor";
	
//	private final boolean usingGWConnectedSensors;
	


	public IOTPlatform() {
		super();
//		this.usingGWConnectedSensors = usingGWConnectedSensors;
	}

//	private String getDeviceID2(String owningUserName, String sensorName) {
//		// String deviceId = "dev" + this.gatewayID + "_" + owningUserName  + "_" + sensorName;// + "_" + sensorIDTag; 
//		int maxLen = 34;
//		int remainingLen = maxLen - 1;	// 1 for joiner between name and sensor
//		remainingLen -= sensorName.length();
//		int end = Math.min(owningUserName.length(), remainingLen); 
//		owningUserName = owningUserName.substring(0,end);
//		String deviceId = owningUserName  + "-" + sensorName;
//		final String safeChar = "-";
//		deviceId = deviceId.replaceAll(" ", safeChar);
//		deviceId = deviceId.replaceAll("/", safeChar);
//		deviceId = deviceId.replaceAll("\\#", safeChar);
//		deviceId = deviceId.replaceAll("\\@", safeChar);
//		deviceId = deviceId.replaceAll("\\+", safeChar);	
//		if (deviceId.length() > maxLen)
//			deviceId = deviceId.substring(0, maxLen);
//		return deviceId;
//	}

	private static String getDeviceID(String owningUserName, String sensorName) {
		// String deviceId = "dev" + this.gatewayID + "_" + owningUserName  + "_" + sensorName;// + "_" + sensorIDTag; 
		owningUserName = owningUserName.trim();
		sensorName = sensorName.trim();
		final String safeChar = "-";
		int maxLen = 34;
		int index = owningUserName.indexOf("@");
		if (index >= 0)
			owningUserName = owningUserName.substring(0,index);
		String deviceId = owningUserName  + safeChar + sensorName;
		if (deviceId.length() > maxLen)
			deviceId = deviceId.substring(0, maxLen);

		deviceId = deviceId.replaceAll(" ", safeChar);
		deviceId = deviceId.replaceAll("/", safeChar);
		deviceId = deviceId.replaceAll("\\#", safeChar);
		deviceId = deviceId.replaceAll("\\@", safeChar);
		deviceId = deviceId.replaceAll("\\+", safeChar);	
		return deviceId;
	}
	

	/**
	 * @param deviceId2 
	 * @param uprofile
	 * @param sensor
	 * @param sensorID
	 * @param deviceId
	 * @return
	 */
	public static JsonObject getDeviceJson(String deviceId, UserProfile uprofile, DeployedSensor sensor, String sensorID) {
		JsonObject device;
		device = new JsonObject();
		device.addProperty("deviceId", deviceId);
		
		// Add device info
		JsonObject deviceInfo = new JsonObject();
		deviceInfo.addProperty("model", sensor.getSensorProfile().getType());
		deviceInfo.addProperty("descriptiveLocation", sensor.getSensorEnvironment().getLocationName() + "/" + sensor.getSensorEnvironment().getSubLocationName());
		deviceInfo.addProperty("description", "User: " + uprofile.getUserName() + ", Device Name: " + sensor.getSensorProfile().getName());
		device.add("deviceInfo", deviceInfo);
	
		// Add metadata
		JsonObject metadata = new JsonObject();
		metadata.addProperty("username", uprofile.getUserName());
		if (sensorID != null)
			metadata.addProperty("sensor_id", sensorID);
		metadata.addProperty("sensor_os", sensor.getSensorProfile().getOs());
		metadata.addProperty("sensor_name", sensor.getSensorProfile().getName());
		Map<String,String> tags = sensor.getTags();
		for (String tag: tags.keySet())
			metadata.addProperty(tag, tags.get(tag));
		device.add("metadata", metadata);
		return device;
	}

	public static JsonObject getDeviceJson(String deviceID, String userName, String deviceName, String deviceModel, String deviceLocation, Properties metaData) {
		JsonObject device;
		device = new JsonObject();
		device.addProperty("deviceId", deviceID);
		
		// Add device info
		JsonObject deviceInfo = new JsonObject();
		if (deviceModel != null)
			deviceInfo.addProperty("model", deviceModel);
		if (deviceLocation != null)
			deviceInfo.addProperty("descriptiveLocation", deviceLocation); 
		if (deviceName != null)
			deviceInfo.addProperty("description", "User: " + userName + ", Device Name: " + deviceName);
		device.add("deviceInfo", deviceInfo);
	
		// Add metadata
		JsonObject deviceMetadata = new JsonObject();
		deviceMetadata.addProperty("username", userName);
		if (deviceName != null)
			deviceMetadata.addProperty("sensor_name", deviceName);
		if (metaData != null) {
			for (Object key: metaData.keySet())
				deviceMetadata.addProperty(key.toString(), metaData.get(key).toString());
		}
		device.add("metadata", deviceMetadata);
		return device;
	}
	
	public static JsonObject getDeviceJson(String deviceID, Properties deviceInfo, Properties metaData) {
		JsonObject device;
		device = new JsonObject();
		device.addProperty("deviceId", deviceID);
		
		// Add device info
		JsonObject deviceInfoJson = new JsonObject();
		if (deviceInfo != null) {
			for (Object key: deviceInfo.keySet())
				deviceInfoJson.addProperty(key.toString(), deviceInfo.get(key).toString());
		}

		device.add("deviceInfo", deviceInfoJson);
	
		// Add metadata
		JsonObject deviceMetaDataJson = new JsonObject();
		if (metaData != null) {
			for (Object key: metaData.keySet())
				deviceMetaDataJson.addProperty(key.toString(), metaData.get(key).toString());
		}
		device.add("metadata", deviceMetaDataJson);
		return device;
	}
	
	/**
	 * Get the type name of sensors that are accessed through this instance, but don't created it in IOTP.
	 * @return
	 * @throws AISPException
	 */
	public static String getSensorDeviceTypeID() {
//		return usingGWConnectedSensors ? GWAudioDeviceTypeID : AudioDeviceTypeID;
		return AudioDeviceTypeID;
	}
	
	public static String getGatewayTypeName() {
		return "AudioGW";
	}

	// Provided to subclasses that implement IAsyncSensorClient
	public static String getSensorDeviceID(UserProfile userProfile, DeployedSensor sensor) {
		String deviceID = getDeviceID(userProfile.getUserName(), sensor.getSensorProfile().getName());
		return deviceID;
	}
	
	/**
	 * @param sensorData
	 * @return
	 */
	protected static JsonObject propertiesToJsonObject(Properties sensorData) {
		JsonObject event = new JsonObject();
		for (Object key : sensorData.keySet()) {
			Object value = sensorData.get(key);
			JsonPrimitive primitive; 
			if (value instanceof Number) 
				primitive = new JsonPrimitive((Number)value);
			else if (value instanceof Boolean)
				primitive = new JsonPrimitive((Boolean)value);
			else
				primitive = new JsonPrimitive(value.toString());
			event.add(key.toString(), primitive);
		}
		return event;
	}

}
