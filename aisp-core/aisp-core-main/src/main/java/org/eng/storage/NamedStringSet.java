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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eng.util.TaggedEntity;

public class NamedStringSet extends TaggedEntity {

	private static final long serialVersionUID = -8628561827656882005L;
	private final Set<String> members = new HashSet<>();
	private final long creationTime;
	private long updateTime;
	private String name = "-unmamed-"; 
	
	public NamedStringSet(String name, Collection<String> members) {
		this.creationTime = System.currentTimeMillis(); 
		this.updateTime = creationTime; 
		this.name = name;
		if (members != null)
			this.members.addAll(members);
	}

	/**
	 * Create an empty set.
	 * @param name
	 */
	public NamedStringSet(String name) {
		this(name, null);
	}

	public Collection<String> getMembers() {
		return members;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		refreshUpdateTime();
	}

	public void addMember(String member) {
		this.members.add(member);
		refreshUpdateTime();
	}

	public void removeMember(String member) {
		this.members.remove(member);
		refreshUpdateTime();
	}


	/**
	 * @return the creationTime
	 */
	public long getCreationTime() {
		return creationTime;
	}
	
	protected void refreshUpdateTime() {
		this.updateTime = System.currentTimeMillis(); 
	}

	/**
	 * @return the updateTime
	 */
	public long getUpdateTime() {
		return updateTime;
	}

	/**
	 * Override to update updated time stamp. 
	 */
	@Override
	public void addTag(String tagName, String tagValue) {
		refreshUpdateTime();
		super.addTag(tagName, tagValue);
	}

	/**
	 * Override to update updated time stamp. 
	 */
	@Override
	public void addTags(Properties tags) {
		refreshUpdateTime();
		super.addTags(tags);
	}

	/**
	 * Override to update updated time stamp. 
	 */
	@Override
	public String removeTag(String tagName) {
		refreshUpdateTime();
		return super.removeTag(tagName);
	}

	/**
	 * Override to update updated time stamp. 
	 */
	@Override
	public void clearTags() {
		refreshUpdateTime();
		super.clearTags();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (creationTime ^ (creationTime >>> 32));
		result = prime * result + ((members == null) ? 0 : members.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (int) (updateTime ^ (updateTime >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof NamedStringSet))
			return false;
		NamedStringSet other = (NamedStringSet) obj;
		if (creationTime != other.creationTime)
			return false;
		if (members == null) {
			if (other.members != null)
				return false;
		} else if (!members.equals(other.members))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (updateTime != other.updateTime)
			return false;
		return true;
	}

	@Override
	public String toString() {
		final int maxLen = 8;
		return "NamedStringSet [members=" + (members != null ? toString(members, maxLen) : null) + ", creationTime="
				+ creationTime + ", updateTime=" + updateTime + ", name=" + name + "]";
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

	public void addMembers(List<String> ids) {
		this.members.addAll(ids);
	}

	public void removedMembers(List<String> ids) {
		this.members.removeAll(ids);
	}
	



}
