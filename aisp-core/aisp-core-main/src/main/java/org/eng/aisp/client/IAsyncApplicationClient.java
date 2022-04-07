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

import org.eng.aisp.client.iotp.IOTPApplicationClient.IApplicationEventListener;

public interface IAsyncApplicationClient {

	void connect() throws IOException;

	boolean isConnected();

	void disconnect();

	/**
	 * Listen for both events and commands.
	 * @param deviceType	 if not null, then restrict subscription to devices of the given type.
	 * @param deviceID if not null and deviceType is not null, then further restrict subscriptions to this device.
	 * @param l the listener to call
	 */
	void setEventListener(String deviceType, String deviceID, IApplicationEventListener l);

	boolean sendCommand(String deviceID, String command, Object gsonAble);

	boolean sendCommand(String deviceType, String deviceID, String command, Object gsonAble);

}