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
package org.eng.aisp.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.SoundRecording;
import org.eng.aisp.segmented.LabeledSegmentSpec;
import org.eng.aisp.segmented.SegmentedSoundRecording;

/**
 * Extends the super class to add a list of LabeledSegmentSpec
 * @author DavidWood
 *
 */
public class SegmentedSoundRecordingMetaData extends SoundRecordingMetaData {

	private static final long serialVersionUID = -4542360291910144254L;
	private final List<LabeledSegmentSpec> segmentSpecs = new ArrayList<LabeledSegmentSpec>();
	
	// For Jackson/Gson 
	protected SegmentedSoundRecordingMetaData() {
		super();
	}
	
//	public SegmentedSoundRecordingMetaData(SoundClipMetaData clipMetaData, String sensorID, String name,
//			Properties labels, Properties tags, boolean isTrainable, Collection<LabeledSegmentSpec> segmentSpecs) {
//		super(clipMetaData, sensorID, name, labels, tags, isTrainable);
//		if (segmentSpecs != null)
//			this.segmentSpecs.addAll(segmentSpecs);
//	}

	public SegmentedSoundRecordingMetaData(String name, SegmentedSoundRecording item) {
		super(name, item.getEntireLabeledDataWindow());
		this.segmentSpecs.addAll(item.getSegmentSpecs());
	}

//	/**
//	 * A copy constructor that replaces the name.
//	 * @param name
//	 * @param item
//	 */
//	public SegmentedSoundRecordingMetaData(String name, SegmentedSoundRecordingMetaData item) {
//		super(name, item);
//		this.segmentSpecs.addAll(item.getSegmentSpecs());
//	}

	/**
	 * @return the segmentSpec
	 */
	public List<LabeledSegmentSpec> getSegmentSpecs() {
		return segmentSpecs;
	}

	/**
	 * Use the metadata in this instance and the given wav bytes to create a new SegmentedSoundRecording.
	 * @param wav
	 * @return never null.
	 * @throws IOException  if the wav file is malformatted.
	 */
	public SegmentedSoundRecording newSegementedSoundRecording(byte[] wav) throws IOException {
		SoundRecording sr = super.newSoundRecording(wav);
		SegmentedSoundRecording ssr = new SegmentedSoundRecording(sr, this.getSegmentSpecs());
		return ssr;
	}

}
