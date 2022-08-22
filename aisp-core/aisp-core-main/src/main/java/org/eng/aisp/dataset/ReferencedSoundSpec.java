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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLogger;
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
	

	/**
	 * Merge adjacent references that have the same label values and that are adjacent in time.
	 * Currently it is assumed that references having the same reference string will be sequential in the given iterable.
	 * @param rssList
	 * @return
	 */
	public static List<ReferencedSoundSpec> mergeLabelValues(Iterable<ReferencedSoundSpec> rssList) {
		List<ReferencedSoundSpec> mergedList = new ArrayList<>();

//		// First filter out references that don't have the given label.  This makes the subsequent traversal a bit easier.
//		boolean missing = false;
//		for (ReferencedSoundSpec rss : rssList) {
//			String rssLabelValue = rss.getLabels().getProperty(labelName);
//			if (rssLabelValue != null)  {
//				mergedList.add(rss);
//			} else {
//				missing = true;
//			}
//		}
//		if (missing)  {
//			rssList = mergedList;
//			mergedList = new ArrayList<>();
//		} else {
//			mergedList.clear();
//		}

		ReferencedSoundSpec lastRSS = null, nextFirstRSS = null;;
		for (ReferencedSoundSpec rss : rssList) {
//			AISPLogger.logger.info("rss=" + rss);
			if (lastRSS != null) {
				if (nextFirstRSS != null  && !isMergeable(lastRSS, rss)) {
					ReferencedSoundSpec mergedRSS = createContinuousReference(nextFirstRSS, lastRSS); 
					mergedList.add(mergedRSS);

					// Starting a new merged segment.
					nextFirstRSS = rss;
				}
			} else  {	// First rss in the loop
				nextFirstRSS = rss;
			}
			lastRSS = rss;
		}
		// The last mergable segment still needs to be merged/created.
		if (nextFirstRSS != lastRSS) {
			ReferencedSoundSpec mergedRSS = createContinuousReference(nextFirstRSS, lastRSS); 
			mergedList.add(mergedRSS);
		}
		return mergedList;
	}



	/**
	 * Assuming the first and last RSS are the beginning and ending of a continuous mergable set of references, then create
	 * a single reference that represents that continuous segment.
	 * <p>
	 * NO checking is done to make sure the label values are the same.
	 * @param firstRSS
	 * @param lastRSS
	 * @return a reference with the star time of the first and end time of the 2nd and with only the given label name and value copied to the new segment.
	 */
	private static ReferencedSoundSpec createContinuousReference(ReferencedSoundSpec firstRSS, ReferencedSoundSpec lastRSS) {
		String ref = firstRSS.getReference();
//		String labelValue = firstRSS.getLabels().getProperty(labelName);
		int startMsec= firstRSS.getStartMsec();
		int endMsec= lastRSS.getEndMsec();
//		Properties labels = new Properties();
//		labels.put(labelName, labelValue);
		ReferencedSoundSpec mergedRSS = new ReferencedSoundSpec(ref, startMsec, endMsec, lastRSS.getLabels(), null);
		return mergedRSS;
	}


	
	/**
	 * Determine if the references and labels match and if the 2nd is adjacent in time following the 1st.
	 * @param rss1
	 * @param rss2
	 * @return
	 */
	private static boolean isMergeable(ReferencedSoundSpec rss1, ReferencedSoundSpec rss2) {
		String ref1 = rss1.getReference();
		String ref2 = rss2.getReference();
		if (!ref1.equals(ref2))
			return false;
		Properties l1 = rss1.getLabels();	
		Properties l2 = rss2.getLabels();
		if (!l1.equals(l2))	// Compares possibly multiple labels.
			return false;
		int end1   = rss1.getEndMsec();
		int start2 = rss2.getStartMsec();
		if (end1 != start2)
			return false;

		return true;
	}



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