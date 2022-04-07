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

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.Classification;

/**
 * Defines the core services to send and receive events.
 * @author dawood
 *
 */
public interface IAsyncSensorClient {

	public static String STATUS_LABEL = "status";
	public static String STATE_LABEL = "state";

	public static enum SensorState {
		Idle,
		Capturing,
		Training,
		Monitoring
	}
	
	public static enum SensorCommand {
		StopMonitoring,
		StartMonitoring,
		/** 
		 * The classifier has changed in the server either by a train, retrain, update, etc  
		 * This is more an event than a command.  We define it here though since the watson-iot library does not support
		 * the DeviceClient subscribing to events, only commands.
		 */
		ClassifierChanged
	}

	public static class SensorStateMessage {
		String currentState;	// String value of SensorStatus
		public SensorStateMessage(SensorState status) {
			this(status.toString());
		}
		public SensorStateMessage(String state) {
			currentState = state; 
		}
	}

	/** Used by sensors to handle commands sent to it */
	public static interface ICommandListener{ 
		public void handleCommand(String command, byte[] payload);
	}


	public void connect() throws IOException;

	public void disconnect(); 
	
	/**
	 * Register a single command listener.  Only one can be registered at a time.
	 * @param callback set to null to turn off callback.
	 * @return true if installed 
	 */
	public boolean setSensorCommandListener(ICommandListener callback);


	/**
	 * Send and event named <i>state</i> to those that are listening.
	 * The body of the event is a JSON map with the following:
	 * <ul>
	 * <li> "state" : "&lt;value&gt;"
	 * </ul>
	 * @param state 
	 * @return
	 */
	public boolean sendSensorState(SensorState state);

//	/**
//	 * Get the type id of the sensor devices this instance interacts with.
//	 * If this is a GW instance, then the devices may have a different type.
//	 * @return
//	 */
//	public String getSensorDeviceTypeID();
//	
//	public String getSensorDeviceID(UserProfile userProfile, DeployedSensor sensor);

	/**
	 * Publish the name value pairs under the given event name for this sensor instance.
	 * @param eventName the name of the event.  
	 * @param sensorData
	 * @return
	 * @throws AISPException
	 */
	public boolean publishEvent(String eventName, Properties sensorData) throws IOException;

	/**
	 * Publish the given classifications for this sensor.
	 * @param eventName the name of the event as seen by the IoT Platform.
	 * @param classifications
	 * @param startMsec
	 * @param endMsec
	 * @param dataSet TODO
	 * @return
	 * @throws IOException 
	 */
	public boolean publishClassifications(String eventName, Map<String, Classification> classifications, long startMsec, long endMsec, Properties metaData) throws IOException;
		
}
