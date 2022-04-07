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
package org.eng.aisp.sensor;

import java.io.Serializable;

import org.eng.validators.annotation.Alphanumeric;

/**
 * Represents something about the location in which the sensor has been deployed.
 * <b>Static</b> information about sensor location and other environmentals.
 * 
 * @author dawood
 *
 */
public class SensorEnvironment implements Serializable { // implements IJSONable {

	@Alphanumeric
	protected final String locationName;
	@Alphanumeric
	protected final String subLocationName;

	// For Jackson/Jersey
	public SensorEnvironment() {
		this.locationName = null;
		this.subLocationName = null;

	}
	public SensorEnvironment(String locationName, String subLocation) {
		if (locationName == null)
			locationName = "";
		this.locationName = locationName;
		this.subLocationName = subLocation;
	}


	/**
	 * @return the name
	 */
	public String getLocationName() {
		return locationName;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((locationName == null) ? 0 : locationName.hashCode());
		result = prime * result + ((subLocationName == null) ? 0 : subLocationName.hashCode());
		return result;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SensorEnvironment))
			return false;
		SensorEnvironment other = (SensorEnvironment) obj;
		if (locationName == null) {
			if (other.locationName != null)
				return false;
		} else if (!locationName.equals(other.locationName))
			return false;
		if (subLocationName == null) {
			if (other.subLocationName != null)
				return false;
		} else if (!subLocationName.equals(other.subLocationName))
			return false;
		return true;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SensorEnvironment [locationName=" + locationName + ", subLocationName=" + subLocationName + "]";
	}
	/**
	 * @return the subLocationName
	 */
	public String getSubLocationName() {
		return subLocationName;
	}

	
}
