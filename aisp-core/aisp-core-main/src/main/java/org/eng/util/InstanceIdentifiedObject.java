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
import java.io.ObjectInputStream;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Base implementation of IInstancID to provide a unique run-time id for each instance.
 * @author dawood
 *
 */
public class InstanceIdentifiedObject implements IInstanceID {

	@JsonIgnore
	private transient long instanceID;
	private static AtomicLong instanceIDs = new AtomicLong();

	public InstanceIdentifiedObject() {
		instanceID = instanceIDs.incrementAndGet();
	}

	@Override
	public long getInstanceID() {
		if (instanceID == 0)	// Just in case the default constructor was not called on deserialization?
			instanceID = instanceIDs.incrementAndGet();
		return instanceID;
	}
	

}
