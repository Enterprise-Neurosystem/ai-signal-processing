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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.eng.validators.annotation.Alphanumeric;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Class containing immutable string valued name=value pairs.
 * 
 * @author dawood
 *
 */
public class TaggedEntity implements Serializable, ITaggedEntity {	

	private static final long serialVersionUID = 3494538060309721384L;

	public final static String TAGS_FIELD_NAME = "tags";

	@Alphanumeric
	protected Map<String,String> tags = new HashMap<String,String>();

	protected TaggedEntity() {
		this((Properties)null);
	}
	
	protected TaggedEntity(Map<String,String> tags) {
		if (tags != null)
			this.tags.putAll(tags);;
	}
	
	protected TaggedEntity(Properties p) {
		if (p != null) {
			for (Object key : p.keySet()) 
				tags.put(key.toString(), p.get(key).toString());
		}
	}	

	
	/* (non-Javadoc)
	 * @see org.eng.aisp.ITaggedEntity#getTag(java.lang.String)
	 */
	@Override
	public String getTag(String tagName) {
		return tags.get(tagName);
	}
	
	/* (non-Javadoc)
	 * @see org.eng.aisp.ITaggedEntity#addTag(java.lang.String, java.lang.String)
	 */
	@Override
	public void addTag(String tagName, String tagValue) {
		this.tags.put(tagName, tagValue);
	}

	/* (non-Javadoc)
	 * @see org.eng.aisp.ITaggedEntity#addTags(java.util.Properties)
	 */
	@Override
	public void addTags(Properties tags) {
		if (tags == null)
			return;
		for (Object key : tags.keySet()) 
			this.tags.put(key.toString(), tags.getProperty(key.toString()));
	}

	/* (non-Javadoc)
	 * @see org.eng.aisp.ITaggedEntity#removeTag(java.lang.String)
	 */
	@Override
	public String removeTag(String tagName) {
		return this.tags.remove(tagName);
	}

	/* (non-Javadoc)
	 * @see org.eng.aisp.ITaggedEntity#getTags()
	 */
	@Override
	public Map<String,String> getTags() {
		Map<String,String> r = new HashMap<String,String>();
		r.putAll(this.tags);
		return r;
	}
	
	@Override 
	public void clearTags() { 
		this.tags.clear();
	}
	
	/**
	 * Get the tags as a new Properties object.
	 * @return never null and always an instance independent of this instance.
	 */
	@JsonIgnore
	public Properties getTagsAsProperties() {
		Properties p = new Properties();
		p.putAll(this.tags);
		return p;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tags == null) ? 0 : tags.hashCode());
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
		if (!(obj instanceof TaggedEntity))
			return false;
		TaggedEntity other = (TaggedEntity) obj;
		if (tags == null) {
			if (other.tags != null)
				return false;
		} else if (!tags.equals(other.tags))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 10;
		return "TaggedEntity [tags=" + (tags != null ? toString(tags.entrySet(), maxLen) : null) + "]";
	}
	

	private String toString(Collection<?> collection, int maxLen) {
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
