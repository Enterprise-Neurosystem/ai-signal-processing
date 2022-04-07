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
package org.eng.aisp.segmented;

import java.io.Serializable;
import java.util.Properties;

import org.eng.validators.annotation.Alphanumeric;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Contains the data defining the labels and tags for segment of sound. 
 * The sound being referred to is NOT defined here and must be managed outside of this class.
 * 
 * @author DavidWood
 */
public class LabeledSegmentSpec implements Serializable {

	private static final long serialVersionUID = -7863857524518782817L;
	
	protected int startMsec;
	protected int endMsec;
	@Alphanumeric
	private Properties labels = new Properties();
	@Alphanumeric
	private Properties tags = new Properties();
	public final static int NON_SEGMENTING_VALUE = 0;

	// For Jersey/Jackson
	protected LabeledSegmentSpec() { this(0,1, null, null); }

	
	public LabeledSegmentSpec(double startMsec, double endMsec, Properties labels, Properties tags) {
		if (labels != null)
			this.labels.putAll(labels);
		if (tags != null)
			this.tags.putAll(tags);
		if (startMsec < NON_SEGMENTING_VALUE)
			throw new IllegalArgumentException("Start offset can not be less than " + NON_SEGMENTING_VALUE); 
		if (endMsec < startMsec)
			throw new IllegalArgumentException("End offset can not be less than start offset");
		this.startMsec = (int)(startMsec + .5);
		this.endMsec = (int)(endMsec + .5);
	}

	@Override
	public String toString() {
		return "LabelSegment [startMsec=" + startMsec + ", endMsec=" + endMsec + ", labels=" + labels + ", tags=" + tags
				+ "]";
	}

	/**
	 * @return the startMsec
	 */
	public int getStartMsec() {
		return startMsec;
	}

	/**
	 * @return the endMsec
	 */
	public int getEndMsec() {
		return endMsec;
	}

	/**
	 * @return the labels
	 */
	public Properties getLabels() {
		return labels;
	}

	/**
	 * @return the tags
	 */
	public Properties getTags() {
		return tags;
	}

	/**
	 * Merge the given props into the receiver, as necessary.
	 * @param receiver
	 * @param props
	 * @return null if given props are null or empty and the given receiver, is null.  Otherwise, the receiver with the addition props.
	 */
	private Properties mergeProps(Properties receiver, Properties props) {
		if (props != null && props.size() > 0) {
			if (receiver == null) 
				receiver = new Properties();
			for (Object key : props.keySet()) {
				String name = key.toString();
				String value = props.getProperty(name);
				String receiverValue = receiver.getProperty(name);
				if (value != null && !value.equals(receiverValue) ) 	{ // merge values.
					value = receiverValue + "," + value;
					receiver.setProperty(name, value);
				}
			}
		}
		return receiver;
	}
	
	/**
	 * Determine if this implies a sub-segment of the sound this applies to.
	 * Generally, if the end is 0, then this is NOT a sub-segment and instead this instance
	 * applies to the whole sound.
	 * @return
	 */
	@JsonIgnore
	public boolean isSubSegment() {
		return this.getEndMsec() > 0;
	}
	

	/**
	 * Merge the given segment into this one, setting the end time of this segment to that of the given segment.
	 * @param ls
	 */
	public void merge(LabeledSegmentSpec ls) {
		this.endMsec = ls.getEndMsec();
		mergeProps(this.labels, ls.getLabels());
		mergeProps(this.tags, ls.getTags());
		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endMsec;
		result = prime * result + ((labels == null) ? 0 : labels.hashCode());
		result = prime * result + startMsec;
		result = prime * result + ((tags == null) ? 0 : tags.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof LabeledSegmentSpec))
			return false;
		LabeledSegmentSpec other = (LabeledSegmentSpec) obj;
		if (endMsec != other.endMsec)
			return false;
		if (labels == null) {
			if (other.labels != null)
				return false;
		} else if (!labels.equals(other.labels))
			return false;
		if (startMsec != other.startMsec)
			return false;
		if (tags == null) {
			if (other.tags != null)
				return false;
		} else if (!tags.equals(other.tags))
			return false;
		return true;
	}
	
}	// LabelSegment class