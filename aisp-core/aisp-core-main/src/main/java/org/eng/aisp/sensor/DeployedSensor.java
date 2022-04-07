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

import org.eng.util.TaggedEntity;
import org.eng.validators.annotation.Alphanumeric;

/**
 * Defines a sensor that has been registered/deployed and associated with a user through a database key.
 * @author dawood
 *
 */
public class DeployedSensor extends TaggedEntity implements Serializable {

	private static final long serialVersionUID = -5864971580914023762L;

//	public final static String SENSOR_FIELD_NAME = "sensorProfile";
//	public final static String ENVIRONMENT_FIELD_NAME = "sensorEnvironment";
	public final static String USERPROFILE_ID_FIELD_NAME = "userProfileID";
	
	/** The unique key/id identifying a user. Might be a db key or a user name. */
	@Alphanumeric
	protected final String userProfileID;
	@Alphanumeric
	protected final SensorProfile sensorProfile;
	@Alphanumeric
	protected final SensorEnvironment sensorEnvironment;
	
	// For Jackson/Jersey
	protected DeployedSensor() {
		this(null, null,null);
	}
	
//	protected DeployedSensor(SensorProfile sensor, SensorEnvironment environment) {
//		this(null, sensor, environment);
//	}
	
	/**
	 * 
	 * @param userKey a unique identifier for a user.  Could be a db key or username, or other unique identifier for a user.
	 * @param sensor
	 * @param environment
	 */
	public DeployedSensor(String userKey, SensorProfile sensor, SensorEnvironment environment) {
		super();
		this.userProfileID = userKey;
		this.sensorProfile = sensor;
		this.sensorEnvironment = environment;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((sensorEnvironment == null) ? 0 : sensorEnvironment.hashCode());
		result = prime * result + ((sensorProfile == null) ? 0 : sensorProfile.hashCode());
		result = prime * result + ((userProfileID == null) ? 0 : userProfileID.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof DeployedSensor))
			return false;
		DeployedSensor other = (DeployedSensor) obj;
		if (sensorEnvironment == null) {
			if (other.sensorEnvironment != null)
				return false;
		} else if (!sensorEnvironment.equals(other.sensorEnvironment))
			return false;
		if (sensorProfile == null) {
			if (other.sensorProfile != null)
				return false;
		} else if (!sensorProfile.equals(other.sensorProfile))
			return false;
		if (userProfileID == null) {
			if (other.userProfileID != null)
				return false;
		} else if (!userProfileID.equals(other.userProfileID))
			return false;
		return true;
	}

	public SensorProfile getSensorProfile() {
		return this.sensorProfile;
	}

	public SensorEnvironment getSensorEnvironment() {
		return this.sensorEnvironment;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "DeployedSensor [userKey=" + userProfileID + ", sensor=" + sensorProfile + ", environment=" + sensorEnvironment + "]";
	}

	/**
	 * @deprecated in favor of {@link #getUserKey()}
	 * @return the userProfileID
	 */
	public String getUserProfileID() {
		return userProfileID;
	}

	public String getUserKey() {
		return userProfileID;
	}


}
