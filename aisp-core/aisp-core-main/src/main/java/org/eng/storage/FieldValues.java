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
package org.eng.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Holds key/value pairs that specify the values of fields of a java object stored in a NoSQL db.
 * 
 * @author dawood
 *
 */
public class FieldValues extends HashMap<String, Object> implements Map<String,Object> {  

	private static final long serialVersionUID = -7249858065167465332L;

	/**
	 * Convert this instance to a properties object. 
	 * @return never null.
	 */
	public Properties asProperties() {
		Properties props = new Properties();
		for (String key : this.keySet()) {
			props.put(key, this.get(key));
		}
		return props;
	}

	/**
	 * Put the given field values under the given name in the returned object. 
	 * @param tags
	 * @return
	 */
	public static FieldValues toFieldValues(String fieldName, Properties fieldValues) {
		if (fieldValues == null)
				return null;
		FieldValues fv = new FieldValues(); 
		fv.put(fieldName, fieldValues);
		// Convert the tags to field values.  
//		if (fieldValues != null && fieldValues.size() != 0) {
//			fv = new FieldValues();
//			String prefix = fieldName +  ".";
//			for (Object key : fieldValues.keySet()) {
//				String tagName = key.toString();
//				String value = fieldValues.getProperty(tagName);
//				fv.put(prefix + tagName, value);
//			}
//		}
		return fv;
	}

	public static Properties toProperties(FieldValues tags) {
		if (tags == null)
			return null;
		Properties p = new Properties();
		for (String key : tags.keySet()) 
			p.setProperty(key, tags.get(key).toString());
		return p;
	}

	public static FieldValues fromProperties(Properties props) {
		if (props == null)
			return null;
		FieldValues fv = new FieldValues();
		for (Object key : props.keySet()) 
			fv.put(key.toString(), props.get(key.toString()));
		return fv;
	}

}
