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
/**
 * 
 */
package org.eng.aisp.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import org.eng.util.TaggedEntity;
import org.eng.validators.annotation.Alphanumeric;

/**
 * @author dawood
 *
 */
public class UserProfile extends TaggedEntity {

	public final static String USERNAME_FIELD_NAME = "userName";

	@Alphanumeric
	protected final String userName;
	@Alphanumeric
	protected final List<NotifyTarget> notifyTargets = new ArrayList<NotifyTarget>();
	@Alphanumeric
	protected String timeZoneID;
	// For Jackson
	protected UserProfile() {
		this("", (NotifyTarget)null, null);
	}
	
	public UserProfile(String userName, List<NotifyTarget> notifyTargets) {
		this(userName, notifyTargets, null);
	}

	public UserProfile(String userName, List<NotifyTarget> notifyTargets, Properties tags) {
		this(userName, notifyTargets, tags, null);
	}

	public UserProfile(String userName, List<NotifyTarget> notifyTargets, Properties tags, TimeZone timeZone) {
		super(tags);
		if (userName == null)
			throw new IllegalArgumentException("username must not be null");
		this.userName = userName.toLowerCase();
		if (notifyTargets != null)
			this.notifyTargets.addAll(notifyTargets);
		if (timeZone == null)
			timeZone = TimeZone.getDefault();
		timeZoneID = timeZone.getID();
	}
	
	
	public UserProfile(String userName, NotifyTarget notifyTarget) {
		this(userName, notifyTarget, null);
	}

	public UserProfile(String userName,  NotifyTarget notifyTarget, Properties tags) {
		this(userName, (List<NotifyTarget>)null, tags);
		if (notifyTarget != null)
			this.notifyTargets.add(notifyTarget);
	}

	public UserProfile(String userName) {
		this(userName, (List<NotifyTarget>)null);
	}

	/**
	 * Get the username, which is in lower case.
	 * @return the userID
	 */
	public String getUserName() {
		return userName;
	}



	/**
	 * @return the notifyTargets
	 */
	public List<NotifyTarget> getNotifyTargets() {
		return notifyTargets;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((notifyTargets == null) ? 0 : notifyTargets.hashCode());
		result = prime * result + ((timeZoneID == null) ? 0 : timeZoneID.hashCode());
		result = prime * result + ((userName == null) ? 0 : userName.hashCode());
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
		if (!(obj instanceof UserProfile))
			return false;
		UserProfile other = (UserProfile) obj;
		if (notifyTargets == null) {
			if (other.notifyTargets != null)
				return false;
		} else if (!notifyTargets.equals(other.notifyTargets))
			return false;
		if (timeZoneID == null) {
			if (other.timeZoneID != null)
				return false;
		} else if (!timeZoneID.equals(other.timeZoneID))
			return false;
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		return true;
	}


	public String getTimeZoneID() {
		return timeZoneID;
	}
	
	public void setTimeZoneID(String timeZoneID) {
		String tzids[] = TimeZone.getAvailableIDs();
		boolean found = false;
		for (String tzid : tzids) {
			if (tzid.equals(timeZoneID)) {
				found = true;
				break;
			}
		}
		if (!found)
			throw new IllegalArgumentException("Time zone id '" + timeZoneID + "' was not found in this runtime");
		this.timeZoneID = timeZoneID;

	}
	

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return "UserProfile [userName=" + userName + ", notifyTargets="
				+ (notifyTargets != null ? notifyTargets.subList(0, Math.min(notifyTargets.size(), maxLen)) : null)
				+ ", timeZoneID=" + timeZoneID + "]";
	}


}
