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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.client.IAsyncSensorClient;
import org.eng.aisp.sensor.DeployedSensor;
import org.eng.aisp.user.UserProfile;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ibm.iotf.client.device.Command;
import com.ibm.iotf.client.device.CommandCallback;
import com.ibm.iotf.client.device.DeviceClient;

public class IOTPSensorClient extends IOTPlatform implements IAsyncSensorClient {

	// Implement the CommandCallback class to provide the way in which you want
	// the command to be handled
	class MyNewCommandCallback implements CommandCallback {
	
		/**
		 * This method is invoked by the library whenever there is command
		 * matching the subscription criteria
		 */
		@Override
		public void processCommand(Command cmd) {
			if (IOTPSensorClient.this.commandListener != null)  
				IOTPSensorClient.this.commandListener.handleCommand(cmd.getCommand(), cmd.getRawPayload());
		}
	
	}

	public static final String STATE_EVENT_NAME = "state";
	private DeviceClient deviceClient = null;
	protected ICommandListener commandListener = null;
	protected final String orgID;
	protected final String deviceTypeID;
	protected final String deviceID;
	protected final String authToken;

	/**
	 * Get the properties that should be published to the IOT Platform as an event after creating a classification result.
	 * @param classifications
	 * @param startMsec
	 * @param endMsec
	 * @param dataSet TODO
	 * @return
	 */
	public static Properties getSensorPublishData(Map<String, Classification> classifications, long startMsec, long endMsec, Properties metaData) {
		Properties data = new Properties();
		Properties classificationData = new Properties(); 	// "classifications" : { <labelName> : { "value": <value>, "confidence" : <conf> } ... };
		for (String key : classifications.keySet())  {
			Classification c = classifications.get(key);
			String labelValue = c.getLabelValue();
			if (key.equalsIgnoreCase(IAsyncSensorClient.STATUS_LABEL) || key.equalsIgnoreCase(IAsyncSensorClient.STATE_LABEL)) {
				double normal_conf;
				if (labelValue.equals("normal")) {
					normal_conf = c.getConfidence();
				} else {
					normal_conf = 0;
				}
				data.put("normal_confidence", normal_conf);
			} 
			// Old way, but kept for backwards compatibility
			data.put(key +"_confidence", c.getConfidence());
			data.setProperty(key, labelValue); 

			// New way.
			Properties tt = new Properties();	 // { "value" : <labelValue>, "confidence" : <conf> }
			tt.put("value", labelValue);
			tt.put("confidence", c.getConfidence()); 
			classificationData.put(key, tt);			

		}
		data.put("startMsec", startMsec);
		data.put("endMsec", endMsec);
		data.put("startTime", formatMsecAsDate(startMsec));
		data.put("endTime", formatMsecAsDate(endMsec));
		
		if (metaData != null && metaData.size() > 0)
			data.put("metadata", metaData);
		if (classificationData.size() > 0) 
			data.put("classifications", classificationData);
			
		return data;
	}

	private static String formatMsecAsDate(long msec) {
		Date date = new Date(msec);
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z");
		formatter.setTimeZone(TimeZone.getDefault());
		String formatted = formatter.format(date);
		return formatted; 
	}
	
	public IOTPSensorClient(String orgID, String deviceTypeID, String deviceID, String deviceToken) {
		super();
		this.orgID = orgID;
		this.deviceTypeID = deviceTypeID;
		this.deviceID = deviceID;
		this.authToken = deviceToken;
	}
	
	public IOTPSensorClient(UserProfile uprofile, DeployedSensor sensor, String orgID, String authToken) {
//		options.setProperty("type", this.deviceTypeID); // getSensorDeviceTypeID()); 
//		options.setProperty("id",   this.deviceID); // getSensorDeviceID(uprofile, sensor));
		this(orgID, getSensorDeviceTypeID(), getSensorDeviceID(uprofile, sensor), authToken);
//		this.uprofile = uprofile;
//		this.sensor = sensor;
//		this.orgID = orgID;
//		this.authToken = authToken;
	}

	/**
	 * 
	 * @param uprof
	 * @param ds
	 * @param props must contain organization id and authentication token.
	 */
	public IOTPSensorClient(UserProfile uprof, DeployedSensor ds, Properties props) {
		this(uprof, ds, props.getProperty(OrganizationIDKey), props.getProperty(AuthenticationTokenKey));
	}

	@Override
	public boolean setSensorCommandListener(ICommandListener callback) {
		this.commandListener = callback;
		return true;
	}

	@Override
	public boolean sendSensorState(SensorState state) {
		if (deviceClient == null)
			throw new IllegalStateException("Not connected");
		SensorStateMessage msg = new SensorStateMessage(state);
		JsonObject json = new JsonObject();
		json.addProperty("state", state.name());
		deviceClient.publishEvent(STATE_EVENT_NAME, json);
		return true;
	
	}

	@Override
	public void connect() throws IOException {
		if (deviceClient != null)
			return;
		
		// Provide the device specific data, as well as Auth-key and token using
		// Properties class
		Properties options = new Properties();
		options.put(OrganizationIDKey, orgID);
		options.put(AuthenticationMethodKey, AuthMethodToken);
		options.put(AuthenticationTokenKey, authToken);
		options.put("auth-token", authToken);	// Just to be sure 
		options.setProperty("type", this.deviceTypeID); // getSensorDeviceTypeID()); 
		options.setProperty("id",   this.deviceID); // getSensorDeviceID(uprofile, sensor)); 
		try {
			deviceClient = new DeviceClient(options);
		} catch (Exception e) {
			throw new IOException("Can not create DeviceClient instance: " + e.getMessage(), e);
		}
		deviceClient.setCommandCallback(new MyNewCommandCallback());
		try {
			deviceClient.connect(false);
		} catch (Exception e) {
			deviceClient = null;
			throw new IOException("Could not connect device client: " + e.getMessage(), e);
		}
	}

	@Override
	public void disconnect() {
		if (deviceClient == null)
			return;
		deviceClient.disconnect();
		deviceClient = null;
	}

	@Override
	public boolean publishClassifications(String eventName, Map<String, Classification> classifications, long startMsec, long endMsec, Properties metaData) throws IOException {
		Properties event = getSensorPublishData(classifications, startMsec, endMsec, metaData);
		return publishEvent(eventName, event);
	}

	Gson gson = new Gson();
	@Override
	public boolean publishEvent(String eventName, Properties sensorData) throws IOException {
//		JsonObject event = propertiesToJsonObject(sensorData);
		boolean r = this.deviceClient.publishEvent(eventName, sensorData);
		if (!r)	// Per agreement with Xiping, he makes a case that we should throw an exception to be consistent with other parts of the code.
			throw new IOException("Could not send event " + eventName + " for unknown reasons.");
		return r;
	}
	
	public static void main(String[] args) throws AISPException, IOException {
		//IOTPSensorClient client = new IOTPSensorClient("f425dw", "AudioSensor", "xiping", "Xiping1956");
		IOTPSensorClient client = new IOTPSensorClient("f425dw", "AudioSensor", "david", "David1962");
		client.connect();
		Map<String, Classification> map = new HashMap<String, Classification>();
		Classification c = new Classification("status", "normal", .9);
		map.put("status", c);
		client.publishClassifications("sensorStatus", map, 0, System.currentTimeMillis(), null);
		client.disconnect();
	}

}
