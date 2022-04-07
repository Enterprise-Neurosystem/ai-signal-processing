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
package org.eng.aisp.dataset;

import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.segmented.LabeledSegmentSpec;
import org.eng.aisp.segmented.SoundSegmenter;

/**
 * A simple/obvious implementation of IReferencedSoundSpec.
 * Uses a LabeledSegment to define the segment, if any, and and added reference field to hold the reference.
 * 
 * @author DavidWood
 *
 */
public class ReferencedSoundSpec extends LabeledSegmentSpec implements IReferencedSoundSpec {
	
	private static final long serialVersionUID = 7801210252788544712L;

	protected final String reference;
	protected transient SoundSegmenter segmenter = null;
	
	public ReferencedSoundSpec(String reference, Properties labels, Properties tags) {
		this(reference, LabeledSegmentSpec.NON_SEGMENTING_VALUE, LabeledSegmentSpec.NON_SEGMENTING_VALUE, labels,tags);
	}


	public ReferencedSoundSpec(String reference, int segmentStartMsec, int segmentEndMsec, Properties labels, Properties tags) {
		super(segmentStartMsec, segmentEndMsec, labels, tags);
		if (reference == null)
			throw new IllegalArgumentException("Null reference not allowed");
		this.reference = reference;
	}

	/**
	 * A copy constructor that uses the given id.
	 * @param id
	 * @param r
	 */
	public ReferencedSoundSpec(IReferencedSoundSpec from, String newReference) {
		this(newReference, from.getStartMsec(), from.getEndMsec(), from.getLabels(), from.getTags());
	}

	@Override
	public String getReference() {
		return reference;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((reference == null) ? 0 : reference.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ReferencedSoundSpec))
			return false;
		ReferencedSoundSpec other = (ReferencedSoundSpec) obj;
		if (reference == null) {
			if (other.reference != null)
				return false;
		} else if (!reference.equals(other.reference))
			return false;
		return true;
	}






	/**
	 * Uses a SoundSegmenter to segment the given sound.
	 */
	@Override
	public SoundRecording apply(SoundRecording data) throws AISPException {
		if (segmenter == null)
			segmenter = new SoundSegmenter();
		return segmenter.segment(data, this);
	}


	@Override
	public String toString() {
		return "ReferencedSoundSpec [reference=" + reference + ", getStartMsec()=" + getStartMsec() + ", getEndMsec()="
				+ getEndMsec() + ", getLabels()=" + getLabels() + ", getTags()=" + getTags() + "]";
	}






}