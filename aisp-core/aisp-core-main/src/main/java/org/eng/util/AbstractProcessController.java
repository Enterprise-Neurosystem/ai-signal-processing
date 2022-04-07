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
import org.eng.util.IProcessController.IProcessControllerListener;

/**
 * Help IProcessController implementations manage the listeners.
 * Subclasses must arrange to call {@link #notifyListeners(Object, Boolean)} with the 1st argument ignored and
 * the 2nd argument true if the listeners'  startProcessing() methods are to be called, or false if stopProcessing().
 * @author DavidWood
 *
 */
public abstract class AbstractProcessController extends AbstractListenerManager<IProcessControllerListener, Object, Boolean>
		implements IProcessController {

	protected AbstractProcessController(ComponentLogger logger) {
		super(logger);
	}

	@Override
	protected void callListener(IProcessControllerListener listener, Object dynamicCallbackData, Boolean starting) {
		try {
			if (starting)
				listener.startProcessing();
			else
				listener.stopProcessing();
		} catch (ENGException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
