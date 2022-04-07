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

import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.dataset.MetaData;

/**
 * Provides the functionality to apply a LabeledSegmentSpec to a SoundRecording or SoundClip.
 *  
 * <br>
 * TODO: Could generalize this to IDataWindow and ILabeledDataWindow using IDataWindowFactory and similar factory for ILabeledDataWindow.
 * @author DavidWood
 *
 */
public class SoundSegmenter { 


	/**
	 * A convenience on {@link #segment(SoundRecording, LabeledSegmentSpec)}.
	 */
	public SoundRecording segment(SoundClip clip, LabeledSegmentSpec seg) throws AISPException {
		SoundRecording sr = new SoundRecording(clip, null, null);
		return segment(sr,seg);
	}

	/**
	 * Create the segment taken out of the given SoundRecording based on the given segment spec.
	 * Labels and tags from the given window are copied to the resulting segment unless overridden by the given segment spec.
	 * @param data recording from which the segment is extracted/created.
	 * @param seg defines the start and stop offset of the segment within the given recording and labels and tags 
	 * to be applied to the resulting segment.
	 * @return never null.  A new SoundRecording holding the segment as defined.
	 * @throws AISPException if segment is defined outside of this given recording.
	 */
	public SoundRecording segment(SoundRecording data, LabeledSegmentSpec seg) throws AISPException {
		Properties finalLabels = new Properties();
		Properties finalTags = new Properties();
		
		// Load the recording and get any tags/labels.
		finalLabels.putAll(data.getLabels());
		finalTags.putAll(data.getTags());

		// Apply the tags/labels from the references after those from the SoundRecording
		// as the ones from the reference override any that might be applied
		finalLabels.putAll(seg.getLabels());
		finalTags.putAll(seg.getTags());
		
		// Apply optional segmentation.
		SoundClip clip = segmentClip(data.getDataWindow(), seg);
		
		// Create the recording with the  clip and final tags and labels.
		data = new SoundRecording(clip, finalLabels, finalTags); 
		data.removeTag(MetaData.START_TIME_MSEC_LABEL);

		return data;
	}

	private SoundClip segmentClip(SoundClip clip, LabeledSegmentSpec seg) throws AISPException {
		double endMsec = seg.getEndMsec();
		if (!seg.isSubSegment())
			return clip;

		double durationMsec = clip.getDurationMsec();
		endMsec = Math.min(endMsec, durationMsec);	// If end is off the end, just go until the end of the clip.
		double startMsec = seg.getStartMsec();
		if (startMsec >= durationMsec) 
			throw new AISPException("Segment start (" + startMsec + " msec) is after the end of this clip (" + durationMsec + " msec)");
		if (endMsec == startMsec)
			throw new AISPException("Segment start (" + startMsec + " msec) is after the segment end(" + endMsec + " msec)");

		clip = (SoundClip) clip.subWindow2(startMsec, endMsec);
		return clip;
	}

}
