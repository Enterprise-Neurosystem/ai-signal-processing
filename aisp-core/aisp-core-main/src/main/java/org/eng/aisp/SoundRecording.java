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
package org.eng.aisp;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.dataset.LabeledSoundFiles;
import org.eng.aisp.dataset.MetaData;
import org.eng.util.IShuffleIterable;

/**
 * Contains PCM sound data and associated labels.
 * Extends the super class to define the data window as a SoundClip.
 * @author dawood
 *
 */
public class SoundRecording extends LabeledDoubleWindow {

	private static final long serialVersionUID = -6929026660073315189L;
	public static final String CLIP_FIELD_NAME = DATA_WINDOW_FIELD_NAME;

	/**
	 * Load sound files and labels from the files specified in a metadata file.
	 * A convenience on {@link LabeledSoundFiles#loadMetaDataSounds(String, boolean)}.
	 * Formats supported are defined there, but include at least mp3 and wav.
	 * @param src
	 * @return
	 * @throws IOException
	 */
	public static IShuffleIterable<SoundRecording>  readMetaDataSounds(String src) throws IOException {
		return LabeledSoundFiles.loadMetaDataSounds(src, true);
	}
	
	/**
	 * Load sound files and labels from the files specified in a metadata file.
	 * A convenience on {@link LabeledSoundFiles#loadMetaDataSounds(String, boolean)}.
	 * Formats supported are defined there, but include at least mp3 and wav.
	 * @param src directory containing metadata.csv or a metadata.csv file.
	 * @param requireAllFiles if true, then throw an exception if every sound file listed in the wav file could not be located.
	 * Otherwise, allow wav files to be missing and don't include those in the resulting iterable.
	 * @return never null.
	 * @throws IOException
	 */
	public static IShuffleIterable<SoundRecording>  readMetaDataSounds(String src, boolean requireAllFiles) throws IOException {
		return LabeledSoundFiles.loadMetaDataSounds(src, requireAllFiles);
	}

	/**
	 * Load sound files and labels from the files specified in one or more metadata files.
	 * A convenience on {@link LabeledSoundFiles#loadMetaDataSounds(String[], boolean)}.
	 * Formats supported are defined there, but include at least mp3 and wav.
	 * @param src directory containing metadata.csv or a metadata.csv file.
	 * @return
	 * @throws IOException
	 */
	public static IShuffleIterable<SoundRecording>  readMetaDataSounds(String[] src) throws IOException {
		return LabeledSoundFiles.loadMetaDataSounds(src, true);
	}
	
	/**
	 * Load sound files and labels from the files specified in one or more metadata files.
	 * A convenience on {@link LabeledSoundFiles#loadMetaDataSounds(String[], boolean)}.
	 * Formats supported are defined there, but include at least mp3 and wav.
	 * @param src directory containing metadata.csv or a metadata.csv file.
	 * @param requireAllFiles if true, then throw an exception if every sound file listed in the wav file could not be located.
	 * Otherwise, allow wav files to be missing and don't include those in the resulting iterable.
	 * @return never null.
	 * @throws IOException
	 */
	public static IShuffleIterable<SoundRecording>  readMetaDataSounds(String[] src, boolean requireAllFiles) throws IOException {
		return LabeledSoundFiles.loadMetaDataSounds(src, requireAllFiles);
	}

	
	/**
	 * Load sound files and labels from the given directory and make use of a metadata.csv file if it exists to apply labels. 
	 * A convenience on {@link LabeledSoundFiles#loadSounds(String, boolean)}.
	 * Formats supported are defined there, but include at least mp3 and wav.
	 * @param src directory containing sounds and optionally a metadata.csv. 
	 * @return
	 * @throws IOException
	 * @see LabeledSoundFiles.loadSounds(String,boolean)
	 */
	public static IShuffleIterable<SoundRecording> readSounds(String srcDir) throws IOException {
		return LabeledSoundFiles.loadSounds(srcDir, false);
	}

	/**
	 * Write the sounds to the given directory in wav format along with a metdata file.
	 * A convenience on {@link LabeledSoundFiles#writeMetaDataSounds(String, Iterable)}.
	 * @param dir
	 * @param sounds
	 * @return the names of sound files in the given directory. 
	 * @throws IOException 
	 */
	public static List<String> writeMetaDataSounds(String dir, Iterable<SoundRecording> sounds) throws IOException {
		return MetaData.writeMetaDataSounds(dir, sounds);
	}

//	/**
//	 * Turn on/off the automatic setting of the data type to Audio when he type is not set in the tags given to a constructor.
//	 * <p>
//	 * The default is to set the type automatically and turning this off should be used with care.
//	 * @param setting
//	 * @return the previous value.
//	 */
//	public static boolean SetAutoAudioTyping(boolean setting) {
////		boolean oldValue= AutoTypeToAudio;
////		AutoTypeToAudio = setting;
////		return oldValue;
//		AISPLogger.
//		return setting;
//	}

//	private static boolean AutoTypeToAudio = AISPProperties.instance().getProperty("soundrecording.default.audio", true);
	
	// For Jackson/Jersey
	protected SoundRecording() {
		super();
	}
	
	/**
	 * Create the instance with no labels and no tags.
	 * @param deployedSensorID may be null.
	 * @param dataWindow provides the sound data for the recording.
	 */
//	public SoundRecording(String deployedSensorID, SoundClip dataWindow, DataTypeEnum dataType) {
//		this(deployedSensorID, dataWindow, null, null, true);
//	}

//	/**
//	 * Create the instance with no tags.
//	 * @param deployedSensorID may be null.
//	 * @param dataWindow provides the sound data for the recording.
//	 * @param labels provides labels for the recording. may be null.
//	 */
//	public SoundRecording(String deployedSensorID, SoundClip dataWindow, Properties labels, DataTypeEnum dataType) {
//		this(deployedSensorID, dataWindow, labels, null, true);
//	}
//
//	public SoundRecording(SoundClip dataWindow, Properties labels, DataTypeEnum dataType) {
//		this(null, dataWindow, labels, null, true);
//	}
//
//	public SoundRecording(SoundRecording sr, Properties labels, DataTypeEnum dataType) {
//		this(sr.getDeployedSensorID(), sr.getDataWindow(), labels, sr.getTagsAsProperties(), sr.isTrainable());
//	}
//
	public SoundRecording(SoundRecording sr, SoundClip clip) {
		this(clip, sr.getLabels(), sr.getTagsAsProperties(), sr.isTrainable(), 
				DataTypeEnum.getDataTypeTag(sr),
				false); // Take the type from the given recording.	
	}


	/**
	 * If set to true, then allow data-type tag of the SoundRecording object to set the dimensionality of the SoundClip provided to constructors.
	 * This may be useful when older 3-D SoundClips were saved without the dimensionality stored in the wav file.
	 * Defaults to false in which case an exception is thrown when the SoundRecording data type does not match the dimensionality of the SoundClip.
	 */
	public final static String SET_DIMENSION_FROM_TYPE_PROPERTY = "soundrecording.set.dimension.from.type";
	public final static boolean SetDimensionFromType = AISPProperties.instance().getProperty(SET_DIMENSION_FROM_TYPE_PROPERTY, false);

	/**
	 * Create a new clip with an interleaved data dimensionality that matches the given data type.
	 * @param clip
	 * @param tags 
	 * @param dataType
	 * @return a clip with the data interleave that matches the given data type, if specified.
	 * @throw IllegalArgumentException if the clip has a dimensionality other than 0 or 1 and which does not match the dimensionality of the given data type.
	 */
	private static SoundClip getInterleaveAdjustedClip(SoundClip clip, Properties tags, DataTypeEnum dataType) {
		if (dataType == null) {
			dataType = DataTypeEnum.getDataType(tags);
			if (dataType == null)
				return clip;
		}
		int dimensions = DataTypeEnum.getDimensionality(dataType);
		int clipDims = clip.getInterleavedDataDimensions();
		if (dimensions != clipDims) { 
			if (!SetDimensionFromType)
				throw new IllegalArgumentException("Dimensionality of given data type (" + dimensions + ") does not match that of given clip (" + clipDims + ")");
			if (clipDims == 1)	// Assume it is not set and reset it for this instance.
				clip = new SoundClip((int)dimensions,clip);
			else
				throw new IllegalArgumentException("Dimensionality of given data type (" + dimensions + ") does not match that of given clip (" + clipDims + ")");
		}
		return clip;
		
	}

	/**
	 * Create the new instance and set the data interleave on the given clip to match the given/implied data type. 
	 * @param dataWindow
	 * @param labels
	 * @param tags
	 * @param trainable
	 * @param dataType
	 * @param useDefaultTypeIfNullType if true, and the type is null, then set the type to Audio in the tags for this instance, overwriting any existing type there.
	 */
	private SoundRecording(SoundClip dataWindow, Properties labels, Properties tags, boolean trainable, DataTypeEnum dataType, boolean useDefaultTypeIfNullType) {
		super(getInterleaveAdjustedClip(dataWindow, tags, dataType), labels, tags, trainable, dataType == null && useDefaultTypeIfNullType ? DataTypeEnum.Audio : dataType);
	}

	/**
	 * Convenience on {@link #SoundRecording(String, SoundClip, Properties, Properties, boolean, DataTypeEnum)}
	 * with no sensorID, trainability=true, type as Audio unless tags specify the type tag 
	 */
	public SoundRecording(SoundClip clip, Properties labels, Properties tags) {
		this(clip, labels, tags, true, null,				
				DataTypeEnum.getDataType(tags) == null);	// use default type only if given tags do not specify the type.);
	}

	public SoundRecording(SoundClip clip, Properties labels, Properties tags, boolean isTrainable, DataTypeEnum dataType) {
		this(clip, labels, tags, isTrainable, dataType,				
				DataTypeEnum.getDataType(tags) == null);	// use default type only if given tags do not specify the type.);
	}
	/**
	 * Convenience on {@link #SoundRecording(String, SoundClip, Properties, Properties, boolean, DataTypeEnum)}
	 * with no sensorID, trainability=true, type as given. 
	 */
	public SoundRecording(SoundClip clip, Properties labels, Properties tags, DataTypeEnum dataType) {
		this(clip, labels, tags, true, dataType, 				
				DataTypeEnum.getDataType(tags) == null);	// use default type only if given tags do not specify the type.);
	}


	/**
	 * Convenience on {@link #SoundRecording(SoundClip, Properties, Properties, boolean, DataTypeEnum)}
	 * with trainability=true and type as Audio. 
	 */
	public SoundRecording(SoundClip clip, Properties labels) {
		this(clip, labels, (Properties)null, true, (DataTypeEnum)null, true);
	}

	public SoundRecording(SoundRecording sr, Properties newLabels) {
		this(sr.getDataWindow(), newLabels, sr.getTagsAsProperties(), sr.isTrainable(), 
				(DataTypeEnum)null, false);	// Don't overwrite type tag
	}

	/**
	 * A convenience on {@link #SoundRecording(String, SoundClip, Properties, Properties, boolean, DataTypeEnum)}
	 * that sets the labels and tags to null, makes it trainable and defaults the type.
	 * @param sensorID
	 * @param clip
	 */
	public SoundRecording(SoundClip clip) {
		this(clip, null, null, true, (DataTypeEnum)null, true);	// use default type 
	}

	/**
	 * A convenience on {@link #SoundRecording(String, SoundClip, Properties, Properties, boolean, DataTypeEnum)}
	 * that overwrites the tags and label, respectively, if given and sets the type to default if the given tags to not define it per DataTypeEnum. 
	 * @param sr
	 * @param labels
	 * @param tags
	 */
	public SoundRecording(SoundRecording sr, Properties labels, Properties tags) {
		this(sr.getDataWindow(), labels == null ? sr.getLabels() : labels, tags == null ? sr.getTagsAsProperties() : tags, sr.isTrainable(), 
				(DataTypeEnum)null, 
				(DataTypeEnum.getDataType(tags) == null)	// use default type only if given tags do not specify the type. 
			);
	}

	/**
	 * Create a shallow copy with the given start time.
	 * @param sr
	 * @param startTimeMsec
	 */
	public SoundRecording(SoundRecording sr, double startTimeMsec) {
		this(new SoundClip(startTimeMsec, sr.getDataWindow()), sr.getLabels());
	}



	@Override	// To refine return type.
	public SoundClip getDataWindow() {
		return (SoundClip)super.getDataWindow();
	}

	//	public SoundRecording(String deployedSensorID, SoundClip dataWindow, Properties labels, Properties tags) {
	//		this(deployedSensorID, dataWindow, labels, tags, true);
	//	}
				
	
	
//	/**
//	 * If {@link #AutoTypeToAudio} is set to true and  tags is null or does not set the the data type, then set it to Audio, 
//	 * @param tags
//	 * @return never null.
//	 */
//	private static Properties DefaultToAudioType(Properties tags) {
//		if (!AutoTypeToAudio)
//			return tags;
//		DataTypeEnum type = DataTypeEnum.getDataType(tags);
//		if (type != null)
//			return tags;
//		return DataTypeEnum.setDataType(tags, DataTypeEnum.Audio);
//		
//	}
	

}
