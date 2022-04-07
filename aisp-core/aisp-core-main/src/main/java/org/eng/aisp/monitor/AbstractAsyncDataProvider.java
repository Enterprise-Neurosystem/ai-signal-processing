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

import org.eng.aisp.AISPLogger;
import org.eng.aisp.monitor.IAsyncDataProvider.IDataHandler;
import org.eng.util.AbstractListenerManager;

public abstract class AbstractAsyncDataProvider<DATA> extends AbstractListenerManager<IDataHandler<DATA>, DATA, Object> implements IAsyncDataProvider<DATA> {


	protected AbstractAsyncDataProvider() {
		super(AISPLogger.logger);
	}

	@Override
	protected void callListener(IDataHandler<DATA> listener, DATA data, Object notifyData) {
		try {
			listener.newDataProvided(data);
		} catch (Exception e) {
			AISPLogger.logger.severe("Exception during data handling: " +e.getMessage());
			e.printStackTrace();
		}
	}

}
