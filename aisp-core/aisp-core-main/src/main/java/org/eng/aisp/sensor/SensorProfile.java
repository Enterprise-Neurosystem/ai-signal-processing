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
package org.eng.aisp.sensor;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.eng.util.TaggedEntity;
import org.eng.validators.annotation.Alphanumeric;

/**
 * Captures information about a recording device. 
 * @author dawood
 *
 */
public class SensorProfile extends TaggedEntity implements Serializable { // implements IJSONable {

	private static final long serialVersionUID = -883593532904996028L;

	public final static String NAME_FIELD_NAME = "name";
	public final static String TYPE_FIELD_NAME = "type";
	public final static String OS_FIELD_NAME = "os";

	@Alphanumeric
	protected final String name;
	@Alphanumeric
	protected final String type;
	@Alphanumeric
	protected final String os;
	
	/** For JSON/Jackson */
	protected SensorProfile() {
		this(null,null,null, (Properties)null);
	}

	public SensorProfile(String name, String type, String os, Map<String,String> tags) {
		super(tags);
		this.name = name;
		this.type = type;
		this.os = os;
	}
	public SensorProfile(String name, String type, String os, Properties tags) {
		super(tags);
		this.name = name;
		this.type = type;
		this.os = os;
	}
	
	public SensorProfile(String name, String type, Properties tags) {
		this(name, type, System.getProperties().getProperty("os.name"), tags);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 10;
		return "SensorProfile [name=" + name + ", type=" + type + ", os=" + os + ", tags="
				+ (tags != null ? privateToString(tags.entrySet(), maxLen) : null) + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((os == null) ? 0 : os.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		if (!(obj instanceof SensorProfile))
			return false;
		SensorProfile other = (SensorProfile) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (os == null) {
			if (other.os != null)
				return false;
		} else if (!os.equals(other.os))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the os
	 */
	public String getOs() {
		return os;
	}

	private String privateToString(Collection<?> collection, int maxLen) {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		int i = 0;
		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
			if (i > 0)
				builder.append(", ");
			builder.append(iterator.next());
		}
		builder.append("]");
		return builder.toString();
	}


}
