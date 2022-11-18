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

import java.text.MessageFormat;
import java.text.ParseException;
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

	protected final String dataSource;
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
		int runLength = 0;
		for (ReferencedSoundSpec rss : rssList) {
//			AISPLogger.logger.info("rss=" + rss);
			if (lastRSS != null) {
				if (nextFirstRSS != null  && !isMergeable(lastRSS, rss)) {
					ReferencedSoundSpec mergedRSS = createContinuousReference(nextFirstRSS, lastRSS); 
					mergedList.add(mergedRSS);

					// Starting a new merged segment.
					nextFirstRSS = rss;
					runLength = 0;
				}
			} else  {	// First rss in the loop
				nextFirstRSS = rss;
				runLength = 0;
			}
			lastRSS = rss;
			runLength += 1;
		}
		// The last mergable segment still needs to be merged/created.
		if (nextFirstRSS != lastRSS || runLength == 1) {
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
		String dataSource = firstRSS.getDataSource();
//		String labelValue = firstRSS.getLabels().getProperty(labelName);
		int startMsec= firstRSS.getStartMsec();
		int endMsec= lastRSS.getEndMsec();
//		Properties labels = new Properties();
//		labels.put(labelName, labelValue);
		ReferencedSoundSpec mergedRSS = new ReferencedSoundSpec(dataSource, startMsec, endMsec, lastRSS.getLabels(), null);
		return mergedRSS;
	}


	
	/**
	 * Determine if the references and labels match and if the 2nd is adjacent in time following the 1st.
	 * @param rss1
	 * @param rss2
	 * @return
	 */
	private static boolean isMergeable(ReferencedSoundSpec rss1, ReferencedSoundSpec rss2) {
		String src1 = rss1.getDataSource();
		String src2 = rss2.getDataSource();
		if (!src1.equals(src2))
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



	public ReferencedSoundSpec(String dataSource, Properties labels, Properties tags) {
		this(dataSource, LabeledSegmentSpec.NON_SEGMENTING_VALUE, LabeledSegmentSpec.NON_SEGMENTING_VALUE, labels,tags);
	}

	private final static String SEGMENT_START = "[";
	private final static String SEGMENT_END= "]";
	private final static String SEGMENT_START_END_SEPARATOR= "-";
//	private final static String REFERENCE_FORMAT = "%s" + SEGMENT_START + "%d" + SEGMENT_START_STOP_SEPARATOR + "%d" + SEGMENT_END;
	private final static String REFERENCE_FORMAT = "{0}" + SEGMENT_START + "{1,number,#}" + SEGMENT_START_END_SEPARATOR + "{2,number,#}" + SEGMENT_END;
	private final static MessageFormat ReferenceMessageFormat = new MessageFormat(REFERENCE_FORMAT);

	public ReferencedSoundSpec(String reference, int segmentStartMsec, int segmentEndMsec, Properties labels, Properties tags) {
		super(segmentStartMsec, segmentEndMsec, labels, tags);
		if (reference == null)
			throw new IllegalArgumentException("Null reference not allowed");
		this.dataSource = reference;
	}

	/**
	 * A copy constructor that uses the given id.
	 * @param id
	 * @param newDataSource
	 */
	public ReferencedSoundSpec(IReferencedSoundSpec from, String newDataSource) {
		this(newDataSource, from.getStartMsec(), from.getEndMsec(), from.getLabels(), from.getTags());
	}
	
	@Override
	public String getReferenceText() {
		return formatReference(this, true);
//		String ref = this.getDataSource();
//		if (isSubSegment()) 
//			ref += SEGMENT_START + this.getStartMsec() + SEGMENT_START_END_SEPARATOR + this.getEndMsec() + SEGMENT_END;
//		return ref;
	}

	/**
	 * Parse the filename and start/end times from the reference.
	 * @param reference
	 * @return never null. an instance with only the reference and start/end times sets.  The contained 
	 * reference does NOT include the start/end time.  If the start/end are not present in
	 * the reference then the returned instance has 0 for both start/end time.
	 * @throws ParseException 
	 */
	public static IReferencedSoundSpec parseReference(String reference) throws ParseException {
		int index = reference.indexOf(SEGMENT_START);
		int startMsec = 0, endMsec = 0;
		String dataSource; 
		if (index >= 0)  {
			Object[] fields = ReferenceMessageFormat.parse(reference);
			if (fields == null || fields.length != 3)
				throw new ParseException("Reference does not contain expected number of arguments (3): " + reference,0);
			dataSource = fields[0].toString();
			if (!(fields[1] instanceof Number))
				throw new ParseException("Reference segment start is not a number: " + reference,0);
			if (!(fields[2] instanceof Number))
				throw new ParseException("Reference segment end is not a number: " + reference,0);
			startMsec = ((Number)fields[1]).intValue(); 
			endMsec = ((Number)fields[2]).intValue(); 
		} else {
			dataSource = reference;
		}
		return new ReferencedSoundSpec(dataSource, startMsec, endMsec, null,null);
	}

	/**
	 * Use the filename portion of the contained reference together with the spec's start/stop time to get a reference string.
	 * @param spec	 spec reference contains the raw file name.
	 * @param forLinux if true then remove any windows disk specification from the filename.
	 * @return
	 * @throws ParseException 
	 */
	public static String formatReference(IReferencedSoundSpec spec, boolean forLinux) { 
		// Parse filename out of the contained reference.
		String fileName = spec.getDataSource();

		if (forLinux) {
			int colonIndex = fileName.indexOf(':');
			if (colonIndex == 1) 
				fileName = fileName.substring(colonIndex+1);
			fileName = fileName.replace("\\", "/");
		}
		
		// If no segment spec, then just return the raw file. 
		int endMsec = spec.getEndMsec();
		if (spec.getEndMsec() <= 0)
			return fileName; 
		int startMsec = spec.getStartMsec();
		String reference = MessageFormat.format(REFERENCE_FORMAT, fileName, new Integer(startMsec), new Integer(endMsec));
		return reference;
	}
	
	@Override
	public String getDataSource() {
		return dataSource;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((dataSource == null) ? 0 : dataSource.hashCode());
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
		if (dataSource == null) {
			if (other.dataSource != null)
				return false;
		} else if (!dataSource.equals(other.dataSource))
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
		return "ReferencedSoundSpec [reference=" + dataSource + ", getStartMsec()=" + getStartMsec() + ", getEndMsec()="
				+ getEndMsec() + ", getLabels()=" + getLabels() + ", getTags()=" + getTags() + "]";
	}






}