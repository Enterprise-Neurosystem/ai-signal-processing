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
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import org.eng.aisp.AbstractDataWindow;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.SensorRecording;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.IDataWindow.PadType;
import org.eng.aisp.storage.SoundRecordingMetaData.SoundClipMetaData;
import org.eng.aisp.util.PCMUtil;
import org.eng.util.Vector;

/**
 * A data container class used only for transmitting meta data about sound clips and sound recordings. 
 * @author dawood
 */
public class SoundRecordingMetaData extends SensorRecording<SoundClipMetaData> {

	private static final long serialVersionUID = -5361963784036021949L;
	public static String START_MSEC_FIELD_NAME = SensorRecording.DATA_WINDOW_FIELD_NAME + "." + SoundClipMetaData.START_MSEC_FIELD_NAME;
	public static String END_MSEC_FIELD_NAME =   SensorRecording.DATA_WINDOW_FIELD_NAME + "." + SoundClipMetaData.END_MSEC_FIELD_NAME;
	final String name;

	/**
	 * A sound clip w/o the pcm data.
	 * @author dawood
	 */
	public static class SoundClipMetaData extends AbstractDataWindow<double[]> {
		private static final long serialVersionUID = -3623867660796790431L;
		final int channels, bitsPerSample;
		final int interleavedDataDimensions;
		
		public SoundClipMetaData(SoundClip clip) {
			super(clip.getStartTimeMsec(), clip.getEndTimeMsec(), clip.getSamplingRate(), (Vector)null);
			this.channels = clip.getChannels();
			this.bitsPerSample = clip.getBitsPerSample();
			this.interleavedDataDimensions = clip.getInterleavedDataDimensions();
		}

		public SoundClipMetaData(double startTimeMsec, double endTimeMsec, int channels, int samplingRate, int bitsPerSample, int interleavedDataDimensions) {
			super(startTimeMsec, endTimeMsec, samplingRate,(Vector)null);
			this.channels = channels;  
			this.bitsPerSample = bitsPerSample; 
			this.interleavedDataDimensions  = interleavedDataDimensions;
		}


		@Override
		public double[] getData() { return null; }
		@Override
		protected IDataWindow<double[]> newSubWindow(double newStartMsec, int startSampleIndex, int endSampleIndex) { return null; }


		@Override
		protected IDataWindow<double[]> uncachedPad(double durationMsec, org.eng.aisp.IDataWindow.PadType padType) { return null; }

		/**
		 * @return the channels
		 */
		public int getChannels() {
			return channels;
		}

		/**
		 * @return the bitsPerSample
		 */
		public int getBitsPerSample() {
			return bitsPerSample;
		}

		public int getInterlavedDataCount() {
			return interleavedDataDimensions;
		}

		/**
		 * Create the clip from the given wav[] and metadata contained here
		 * @param wav
		 * @return never null.
		 * @throws IOException if wav is malformed.
		 */
		public SoundClip newSoundClip(byte[] wav) throws IOException {
			SoundClip clip = PCMUtil.WAVtoPCM(this.getStartTimeMsec(), wav);
			return clip;
		}

	}
	
	// For Jackson/Gson
	protected SoundRecordingMetaData() {
		super();
		this.name = null;
	}

	public SoundRecordingMetaData(SoundClipMetaData clipMetaData,String name, Properties labels, Properties tags, boolean isTrainable) {
		super(clipMetaData, labels, tags, isTrainable, null); 
		this.name = name;
	}

	public SoundRecordingMetaData(String name, SoundRecording item) {
		super(new SoundClipMetaData(item.getDataWindow()), item.getLabels(), item.getTagsAsProperties(), item.isTrainable(), null);
		this.name = name;
	}

	public SoundRecordingMetaData(String name, SoundRecordingMetaData item) {
		super(item.getDataWindow(), item.getLabels(), item.getTagsAsProperties(), item.isTrainable(), null);
		this.name = name;
	}



	public String getName() {
		return name;
	}
	
	/**
	 * Create a new SoundRecording instance that is built from the given SoundRecording and the metadata in this instance.
	 * Applied metadata includes
	 * <ol>
	 * <li> sensorID
	 * <li> clip start time
	 * <li> labels
	 * <li> tags
	 * <li> isTrainable bit
	 * </ol>
	 * @param sr the SoundRecording that is used as the base of data and is combined with this instance's metadata to
	 * create a new SoundRecording.  The given instance is NOT modified.
	 * @return never null
	 */
	public SoundRecording apply(final SoundRecording sr) {
		return apply(sr.getDataWindow());
//		SoundClipMetaData clipMD = this.getDataWindow();
//		SoundClip newClip = new SoundClip(clipMD.getStartTimeMsec(), sr.getDataWindow());	// Set the start time of the clip from the metadata
//		SoundRecording newSR = new SoundRecording(this.getDeployedSensorID(), newClip, this.getLabels(),this.getTagsAsProperties(), this.isTrainable());
//		return newSR;
	}
	
	/**
	 * Create a new SoundRecording instance that is built from the given SoundRecording and the metadata in this instance.
	 * Applied metadata includes
	 * <ol>
	 * <li> sensorID
	 * <li> clip start time
	 * <li> labels
	 * <li> tags
	 * <li> isTrainable bit
	 * </ol>
	 * @param sr the SoundRecording that is used as the base of data and is combined with this instance's metadata to
	 * create a new SoundRecording.  The given instance is NOT modified.
	 * @return never null
	 */
	public SoundRecording apply(SoundClip clip) {
		SoundClipMetaData clipMD = this.getDataWindow();
		SoundClip newClip = new SoundClip(clipMD.getStartTimeMsec(), clip);	// Set the start time of the clip from the metadata
		SoundRecording newSR = new SoundRecording(newClip, this.getLabels(),this.getTagsAsProperties(), this.isTrainable(), null);
//		SoundRecording newSR = new SoundRecording(this.getDeployedSensorID(), newClip, this.getLabels(),this.getTagsAsProperties(), this.isTrainable());
		return newSR;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof SoundRecordingMetaData))
			return false;
		SoundRecordingMetaData other = (SoundRecordingMetaData) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return "SoundRecordingMetaData [name=" + name + ", labels=" + labels + ", dataWindow=" + dataWindow
				+ ", isTrainable=" + isTrainable + ", tags="
				+ (tags != null ? toString(tags.entrySet(), maxLen) : null) + "]";
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

	/**
	 * Recreate a SoundRecording from the given wav[] and metadata contained here.
	 * @param wav
	 * @return never null
	 * @throws IOException if wav is malformed.
	 */
	public SoundRecording newSoundRecording(byte[] wav) throws IOException {
		SoundClip clip = this.dataWindow.newSoundClip(wav);
		SoundRecording sr = new SoundRecording(clip, this.getLabels(), this.getTagsAsProperties(), this.isTrainable(), null); 
		return sr;
	}

}
