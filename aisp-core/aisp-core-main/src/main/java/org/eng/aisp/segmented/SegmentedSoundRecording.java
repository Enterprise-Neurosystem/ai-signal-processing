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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;

/**
 * Extends the super class to define the type of labeled data windows as a SoundRecording.
 * It also provides the implementation of {@link #getSegmentedLabeledDataWindows()} to apply the segments to the associated SoundRecording.
 * @author DavidWood
 *
 */
public class SegmentedSoundRecording extends AbstractSegmentedLabeledDoubleWindow<SoundRecording> implements ISegmentedLabeledDoubleWindow<SoundRecording>  {

	private static final long serialVersionUID = -3311570761201584787L;
	
	// For Jackson/Gson
	protected SegmentedSoundRecording() {
		super();
	}

	public SegmentedSoundRecording(SoundRecording labeledWindow, Collection<LabeledSegmentSpec> segmentSpecs) {
		super(labeledWindow, segmentSpecs);
		labeledWindow.setIsTrainable(true);
	}

	/**
	 * Copy constructor that replaces the labeled segments.
	 * @param labeledWindow
	 * @param segmentSpecs
	 */
	public SegmentedSoundRecording(SegmentedSoundRecording labeledWindow, Collection<LabeledSegmentSpec> segmentSpecs) {
		super(labeledWindow, segmentSpecs);
	}	

	/**
	 * A copy constructor that uses the new clip instead of the one in the original
	 * @param newClip
	 * @param sr
	 */
	public SegmentedSoundRecording(SegmentedSoundRecording ssr, SoundClip clip) {
		this(new SoundRecording(ssr.getEntireLabeledDataWindow(),clip), ssr.getSegmentSpecs());
	}


	/**
	 * Copy constructor that replaces the sound recording with the one given.
	 * @param ssr 
	 * @param sr
	 */
	public SegmentedSoundRecording(SegmentedSoundRecording ssr, SoundRecording sr) {
		this(sr, ssr.getSegmentSpecs());
	}


	/** Caches the segments returned by {@link #getSegmentedLabeledDataWindows()} */
	private transient List<SoundRecording> segmentCache = null;
	
	private static SoundSegmenter segmenter = new SoundSegmenter();

	/**
	 * Uses a SoundSegmenter to apply each segment contained in the instance to the recording contained in the instance.
	 * If there are no segments defined, then the whole recording is returned.
	 * @see SoundSegmenter#segment(SoundRecording, LabeledSegmentSpec) 
	 */
	@Override
	public List<SoundRecording> getSegmentedLabeledDataWindows() throws AISPException {
		if (segmentCache == null) {
			segmentCache = new ArrayList<SoundRecording>(); 
			if (getSegmentSpecs().isEmpty()) {
				segmentCache.add(this.labeledWindow);
			} else  {
				for (LabeledSegmentSpec spec : getSegmentSpecs()) {
					SoundRecording segment = segmenter.segment(this.labeledWindow, spec);
					segmentCache.add(segment);
				}
			}
		}
			
		return segmentCache;
	}








}
