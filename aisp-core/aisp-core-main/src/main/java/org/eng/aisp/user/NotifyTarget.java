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
package org.eng.aisp.user;

import java.io.Serializable;

import org.eng.validators.annotation.Alphanumeric;

public class NotifyTarget implements Serializable {
	private static final long serialVersionUID = -6621637119927741184L;
	protected final NotificationType notificationType;
	@Alphanumeric
	protected final String destination;
	@Alphanumeric
	protected final CarrierReference carrier;

	public enum NotificationType implements Serializable {
		Email,
		SMS,
		RemoteApplication
	}
	
	// For Jackson
	protected NotifyTarget() { this(null,null, null); }

	public NotifyTarget(NotificationType type, String destination, CarrierReference carrier) {
		this.notificationType = type; 
		this.destination = destination;
		this.carrier = carrier;
	}

	/**
	 * Define an email notification type.
	 * @param emailAddress
	 */
	public NotifyTarget(String emailAddress) {
		this(NotificationType.Email, emailAddress, null);

	}

	/**
	 * Define an SMS/text message notification type.
	 * @param countryNumber the in-country number w/o the country code.
	 * @param carrier specifies the country code and the carrier.  May be null.
	 */
	public NotifyTarget(String countryNumber, CarrierReference carrier) {
		this(NotificationType.SMS, countryNumber, carrier);
	}

	/**
	 * @return the notificationType
	 */
	public NotificationType getNotificationType() {
		return notificationType;
	}

	/**
	 * @return the destination
	 */
	public String getDestination() {
		return destination;
	}

	/**
	 * Get the carrier if defined.
	 * @return null if not defined and always null for email notification.
	 */
	public CarrierReference getCarrier() {
		return carrier;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((carrier == null) ? 0 : carrier.hashCode());
		result = prime * result + ((destination == null) ? 0 : destination.hashCode());
		result = prime * result + ((notificationType == null) ? 0 : notificationType.hashCode());
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
		if (!(obj instanceof NotifyTarget))
			return false;
		NotifyTarget other = (NotifyTarget) obj;
		if (carrier == null) {
			if (other.carrier != null)
				return false;
		} else if (!carrier.equals(other.carrier))
			return false;
		if (destination == null) {
			if (other.destination != null)
				return false;
		} else if (!destination.equals(other.destination))
			return false;
		if (notificationType != other.notificationType)
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "NotifyTarget [notificationType=" + notificationType + ", destination=" + destination + ", carrier="
				+ carrier + "]";
	}
}
