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

import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A generic item that has tags (i.e. name/value pairs) associated with it.
 * @author dawood
 *
 */
public interface ITaggedEntity {

	String getTag(String tagName);

	void addTag(String tagName, String tagValue);

	void addTags(Properties tags);

	String removeTag(String tagName);

	/**
	 * Get a copy of the tags contained herein.
	 * @return never null
	 */
	Map<String, String> getTags();

	/**
	 * Remove all tags.
	 */
	void clearTags();

	@JsonIgnore
	public Properties getTagsAsProperties() ;
}
