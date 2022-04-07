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
package org.eng.util;

import java.io.IOException;

import org.eng.ENGException;

/**
 * Provides the ability to control a process that can be started and stopped and restarted.
 * Implementations call the listeners to start/stop the process according to the implementation.
 * For example, an implementation might be itself listen on network messages which
 * control the starting and stopping of the attached process.  The process is "attached"
 * via the listeners in the instance.  The listeners are expected to perform the actual
 * starting and stopping of the process. 
 * Implementations must be {@link #start()}'d and {@link #stop()}'d.
 * @author DavidWood
 *
 */
public interface IProcessController {
	
	/**
	 * Implemented to actually start or stop the process according
	 * to the wishes of the IProcessController implementation.
	 * @author DavidWood
	 *
	 */
	public interface IProcessControllerListener {
		/**
		 * Start the process associated with this listener.
		 * @throws ENGException
		 * @throws IOException
		 */
		public void startProcessing() throws ENGException, IOException;

		/**
		 * Stop the process associated with this listener.
		 * @throws ENGException
		 * @throws IOException
		 */
		public void stopProcessing() throws ENGException, IOException;
	}
	
	/**
	 * Start controlling and calling any registered listeners.
	 * Must be called before listeners will be called.
	 * Calling this after already started must have no effect.
	 * @return true if started successfully or already started.
	 * @throws Exception 
	 */
	public boolean start() throws Exception;

	/**
	 * Stop controlling and calling any registered listeners.
	 * Generally this cleans up any threads or other resources
	 * created during {@link #start()}. 
	 * Calling this after already stopped must have no effect.
	 */
	public void stop();
	
	public boolean addListener(IProcessControllerListener l);

	public boolean removeListener(IProcessControllerListener l);

}
