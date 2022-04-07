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
package org.eng.aisp;

import java.io.Serializable;

public interface IDataWindowFactory<WINDATA, WINDOW extends IDataWindow<WINDATA>> extends Serializable {

	/**
	 * Create a new window containing the given data spanning the requested time.
	 * @param startTimeMsec
	 * @param endTimeMsec
	 * @param data
	 * @return
	 */
	public WINDOW newDataWindow(double startTimeMsec, double endTimeMsec, WINDATA data);

}
