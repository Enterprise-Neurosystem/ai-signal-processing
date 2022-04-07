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
package org.eng.aisp.monitor;

import org.eng.aisp.AISPException;

public interface IAsyncDataProvider<DATA extends Object> {

	/**
	 * Must be called by the implementation when data is available.
	 * @author DavidWood
	 *
	 * @param <DATA>
	 */
	public interface IDataHandler<HDATA> {
		public void newDataProvided(HDATA dataWindow) throws Exception;
	}
	
	public boolean addListener(IDataHandler<DATA> l);

	public boolean removeListener(IDataHandler<DATA> l);
		
	/**
	 * Start asynchronous data capture.
	 * Starting an already started instance should be expected and return safely. 
	 * @throws AISPException 
	 */
	public void start() throws Exception;

	/**
	 * Stop data capture. 
	 * Stopping an already stopped instance should be expected and return safely. 
	 * @throws Exception
	 */
	public void stop();

}