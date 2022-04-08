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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eng.util.ITaggedEntity;

public enum DataTypeEnum {
	Audio,
	VibrationXYZ,
	VibrationMagnitude;

	/**
	 * Allow 'audio', 'xyz', and 'mag' (more user-friendly strings) to map to DataTypeEnum
	 * values, but if none of those match, then match a name of an enumerated type.
	 * @param dataTypeValue
	 * @return
	 */
	public static DataTypeEnum parseDataType(String dataTypeValue) {
		DataTypeEnum type;
		if (dataTypeValue.equals("audio")) {
			type = DataTypeEnum.Audio;
		} else if (dataTypeValue.equals("xyz")) {
			type = DataTypeEnum.VibrationXYZ;
		} else if (dataTypeValue.equals("mag")) {
			type = DataTypeEnum.VibrationMagnitude;
		} else {
			type = DataTypeEnum.valueOf(dataTypeValue);
		}
		return type;
	}

	/**
	 * Add the given data type as a property to the given properties.
	 * <p>
	 * Consider using {@link #setDataTypeTag(ITaggedEntity, DataTypeEnum)} if it meets your needs.
	 * @param tags may be null, in which case a new instance will be created.
	 * @param dataType if null, remove from the given tags.
	 * @return never null and the same instance as given unless given instance is null, then a new instance.
	 * 
	 */
	public static Properties setDataType(Properties tags, DataTypeEnum dataType) {
		if (tags == null) 
			tags = new Properties();
		setDataTypeKey(tags, dataType);
		return tags;
	}
	
	/**
	 * @param tags may be null, in which case a new instance will be created.
	 * @param dataType if null, remove from the given tags.
	 * @return never null and the same instance as given unless given instance is null, then a new instance.
	 */
	public static Map setDataTypeKey(Map tags, DataTypeEnum dataType) {
		if (tags == null) 
			tags = new HashMap();
		if (dataType == null) {
			tags.remove(DataTypePropertyName);
		} else {
			tags.put(DataTypePropertyName, dataType.name());
		}
		return tags;
	}

	//	public SoundRecording(String deployedSensorID, SoundClip dataWindow, Properties labels, Properties tags) {
	//		this(deployedSensorID, dataWindow, labels, tags, true);
	//	}
				
	
	
		/**
	 * See if the properties contains a property indicating the given given data type.
	 * If a type is not indicated in the tags, treat them as if Audio is specified there.
	 * @param tags
	 * @param dataType
	 * @return
	 */
	public static boolean isDataType(Properties tags, DataTypeEnum dataType) {
		if (tags == null)
			return dataType.equals(DataTypeEnum.Audio);
		if (dataType == null)
			return true;
		String type = tags.getProperty(DataTypePropertyName);
		// If the type is not present, let it match Audio type. 
		if (type == null)	
			return dataType.equals(DataTypeEnum.Audio);	
		return type.equals(dataType.name());		// Both type tag and datatype are availabe, so return if they are equal.
	}

	/**
	 * The name of the tag that holds the type of data in the pcm/wav data.
	 * <p>
	 * <b>Note:</b>The use of this constant is discouraged in favor of {@link #setDataType(Properties, DataTypeEnum)} or
	 * {@link #getDataType(Properties)} or {@link #getDataType(Map)}.
	 */
	private final static String DataTypePropertyName = "data-type";

	/**
	 * See if the given properties contains our {@link #DataTypePropertyName} property specifying a type.
	 * <p>
	 * Consider using {@link #getDataTypeTag(ITaggedEntity)} if it meets your needs.
	 * @param props may be null.
	 * @return null if not specified.
	 */
	public static DataTypeEnum getDataType(Properties props) {
		if (props == null)
			return null;
		String typeName = props.getProperty(DataTypePropertyName);
		if (typeName == null)
			return null;
		return DataTypeEnum.valueOf(typeName); 
	}

	private static DataTypeEnum getDataType(Map<String, String> tags) {
		if (tags == null)
			return null;
		String typeName = tags.get(DataTypePropertyName);
		if (typeName == null)
			return null;
		return DataTypeEnum.valueOf(typeName); 
	}

	/**
	 * Get the data type tag from the given tagged entity.
	 * @param entity
	 * @return null if not set.
	 */
	public static DataTypeEnum getDataTypeTag(ITaggedEntity entity) {
		return getDataType(entity.getTags());
	}

	/**
	 * Set/overwrite the data type on the tagged entity.
	 * @param entity
	 * @param dataType if null, then remove the type
	 */
	public static void setDataTypeTag(ITaggedEntity entity, DataTypeEnum dataType) {
		if (dataType == null) {
			entity.removeTag(DataTypePropertyName);
		} else {
			Properties p = setDataType((Properties)null, dataType);
			entity.addTags(p);
		}
	}

	public static void removeDataTypeTag(ITaggedEntity entity) {
		setDataTypeTag(entity,null);
	}

	public static void removeType(Properties properties) {
		properties.remove(DataTypePropertyName);
	}

	public static int getDimensionality(DataTypeEnum dataType) {
		if (dataType != null && dataType.equals(DataTypeEnum.VibrationXYZ))
			return 3;
		return 1;
	}
		
}