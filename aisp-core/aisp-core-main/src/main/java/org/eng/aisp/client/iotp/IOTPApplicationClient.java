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
import java.util.Properties;

import org.eng.aisp.client.IAsyncApplicationClient;
import org.eng.aisp.client.IAsyncSensorClient;
import org.eng.aisp.sensor.DeployedSensor;
import org.eng.aisp.user.UserProfile;

import com.google.gson.JsonObject;
import com.ibm.iotf.client.IoTFCReSTException;
import com.ibm.iotf.client.api.APIClient;
import com.ibm.iotf.client.app.ApplicationClient;
import com.ibm.iotf.client.app.Command;
import com.ibm.iotf.client.app.Event;
import com.ibm.iotf.client.app.EventCallback;

/**
 * @author dawood
 *
 */
public class IOTPApplicationClient extends AbstractIOTPClient implements IAsyncApplicationClient {

	/** Listener used by applications */
	public interface IApplicationEventListener {
		public void processEvent(String event, byte[] bs);

		public void processCommand(String command, byte[] bs);
	}

	protected ApplicationClient appClient = null;

	public IOTPApplicationClient(String orgID, String apiKey, String authToken) {
		super(orgID, apiKey, authToken);
	}
	
	/**
	 * &lt;orgid&gt;.API-Key=...
	 * &lt;orgid&gt;.Authentication-Token=..
	 * @param props
	 */
	public IOTPApplicationClient(Properties props) {
		super(props); 
	}

	@Override
	public void connect() throws IOException {
		if (appClient != null)
			return;
		super.connect();
		
		// Provide the device specific data, as well as Auth-key and token using
		// Properties class
//		Properties options = new Properties();
//		options.setProperty("auth-method", "apikey");
		Properties options = this.getConnectionProperties();
		options.setProperty(AuthenticationMethodKey, AuthMethodAPI);
		try {
			appClient = new ApplicationClient(options);
		} catch (Exception e) {
			throw new IOException("Can not create ApplicationClient instance: " + e.getMessage(), e);
		}
		try {
			appClient.connect();
		} catch (Exception e) {
			appClient = null;
			throw new IOException("Could not connect application client: " + e.getMessage(), e);
		}
	}

	@Override
	public boolean isConnected() {
		return appClient != null; 
	}

	@Override
	public void disconnect() {
		if (appClient == null)
			return;
		appClient.disconnect();
		appClient = null;
		super.disconnect();
	}
	
	private static class EventDispatcher implements EventCallback {

		IApplicationEventListener eventListener;
		public EventDispatcher(IApplicationEventListener l) {
			this.eventListener = l;
		}

		@Override
		public void processEvent(Event evt) {
			eventListener.processEvent(evt.getEvent(), evt.getRawPayload());
		}

		@Override
		public void processCommand(Command cmd) {
			eventListener.processCommand(cmd.getCommand(), cmd.getRawPayload());
		}
		
	}

	/**
	 * Listen for both events and commands.
	 * @param deviceType	 if not null, then restrict subscription to devices of the given type.
	 * @param deviceID if not null and deviceType is not null, then further restrict subscriptions to this device.
	 * @param l the listener to call
	 */
	@Override
	public void setEventListener(String deviceType, String deviceID, IApplicationEventListener l) {
		if (l == null) 
			appClient.setEventCallback(null);
		else
			appClient.setEventCallback(new EventDispatcher(l));
		if (deviceType != null) {
			if (deviceID != null)
				appClient.subscribeToDeviceEvents(deviceType, deviceID);
			else
				appClient.subscribeToDeviceEvents(deviceType);
		} else {
			appClient.subscribeToDeviceEvents();
		}
	}
	
	public void setEventListener(String deviceType, String deviceID) {
		if (deviceType != null) {
			if (deviceID != null)
				appClient.subscribeToDeviceEvents(deviceType, deviceID);
			else
				appClient.subscribeToDeviceEvents(deviceType);
		} else {
			appClient.subscribeToDeviceEvents();
		}
	}

	@Override
	protected APIClient api() {
		if (appClient == null)
			return null;
		return appClient.api();
	}

	public boolean sendPause(UserProfile userProfile, DeployedSensor sensor) {
		return this.sendCommand(userProfile, sensor, IAsyncSensorClient.SensorCommand.StopMonitoring.name(),null );
	}
	
	public boolean sendUnpause(UserProfile userProfile, DeployedSensor sensor) {
		return this.sendCommand(userProfile, sensor, IAsyncSensorClient.SensorCommand.StartMonitoring.name(),null );
	}
	
	public boolean sendCommand(UserProfile uprof, DeployedSensor sensor, String command, Object gsonAble) {
		String deviceID = this.getSensorDeviceID(uprof, sensor);
		return sendCommand(deviceID, command, gsonAble);
	}

	@Override
	public boolean sendCommand(String deviceID, String command, Object gsonAble) {
		return this.sendCommand(getSensorDeviceTypeID(), deviceID, command, gsonAble);
	}

	@Override
	public boolean sendCommand(String deviceType, String deviceID, String command, Object gsonAble) {
		if (appClient ==  null)
			throw new IllegalStateException("not connected");
		return appClient.publishCommand(deviceType, deviceID, command, gsonAble);
	}

	public boolean isDeviceRegistered(String deviceID, String deviceType) {
		JsonObject json;
		try {
			json = appClient.api().getDevice(deviceType, deviceID);
		} catch (IoTFCReSTException e) {
			e.printStackTrace();
			json = null;
		}
		return json != null && !json.isJsonNull();
	}






}
