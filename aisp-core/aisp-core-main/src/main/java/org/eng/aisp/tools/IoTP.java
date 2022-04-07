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

import java.io.IOException;
import java.util.Properties;

import org.eng.aisp.client.iotp.IOTPApplicationClient;
import org.eng.aisp.client.iotp.IOTPApplicationClient.IApplicationEventListener;
import org.eng.util.CommandArgs;

public class IoTP {

	private static String Usage = 
			  "Enables subscription to a events and commands issued by a single device\n"
			+ "Usage: ... -orgid <org> -key <appkey> -token <authtoken> -device <deviceid>\n"
			+ "Required:\n"
			+ "   -orgid <org> : sets the organization of the IoT instance to connect to\n"
			+ "   -key <key> : the API Key to use to connect to the IoT instance\n" 
			+ "   -token <key> : the auth token to use to connect to the IoT instance\n" 
			+ "   -device <id> : the id (name) of the device to subscribe to\n" 
			+ "Optional:\n"
			+ "   -waitSeconds <N> : number of seconds to wait for events before exiting\n" 
			+ "   -type <typeid> : type id of device to listen to. Default is " + IOTPApplicationClient.AudioDeviceTypeID + "\n" 
			;
			
	private static class EventListener implements IApplicationEventListener {

		@Override
		public void processEvent(String event, byte[] bs) {
			log("Event", event, bs);
		}

		@Override
		public void processCommand(String command, byte[] bs) {
			log("Command", command, bs);
		}
		
		private void log(String type, String name, byte[] bs) {
			String payload = new String(bs);
			System.out.println(type + ": " + name + ", payload=" + payload);
		}
	}

	public static void main(String[] args) throws InterruptedException {
		CommandArgs cmdargs = new CommandArgs(args);
		String deviceID = cmdargs.getOption("device");
		String deviceType = cmdargs.getOption("type", IOTPApplicationClient.AudioDeviceTypeID);
		String orgID = cmdargs.getOption("orgid");
		String appKey = cmdargs.getOption("key");
		String appToken = cmdargs.getOption("token");
		int waitSeconds = cmdargs.getOption("waitSeconds", Integer.MAX_VALUE);
		// Check for help request
		if (cmdargs.getFlag("h") || cmdargs.getFlag("-help") ) {
			System.out.println(Usage);
			return;
	    }	
		if (waitSeconds <= 0) {
			System.out.println("Zero wait time specified.  Exiting.");
			return;
		}
		if (orgID == null) {
			System.err.println("Organization id must be provided with -orgid option");
			return;
		}
		if (appKey== null) {
			System.err.println("App key must be provided with -key option");
			return;
		}
		if (appToken == null) {
			System.err.println("Auth token must be provided with -token option");
			return;
		}
		if (deviceID == null) {
			System.err.println("Device id must be provided with -device option");
			return;
		}
			
		Properties p = new Properties();
		p.put(IOTPApplicationClient.OrganizationIDKey, orgID);
		p.put(IOTPApplicationClient.APIKeyKey, appKey);
		p.put(IOTPApplicationClient.AuthenticationTokenKey, appToken);
		IOTPApplicationClient appClient = new IOTPApplicationClient(p);;
		
		try {
			appClient.connect();
		} catch (IOException e) {
			System.err.println("Connection failure: " + e.getMessage());
		}

		if (!appClient.isDeviceRegistered(deviceID, deviceType)) {
			System.out.println("WARNING: device " + deviceID + " of type " + deviceType + " is not currently registered on the IoT platform.");
		}
		IApplicationEventListener listener = new EventListener();
		appClient.setEventListener(deviceType, deviceID, listener);
		
		while (waitSeconds > 0) {
			Thread.sleep(1000);
			waitSeconds--;
		}
	
		appClient.disconnect();
		System.exit(0);

	}

}
