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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.SoundRecording;
import org.eng.storage.INamedSerializableStorage;
import org.eng.storage.StorageException;

/**
 * Interface to needed to store SoundRecording.
 * Extends are added to support WAV formatted data.
 * 
 * @author dawood
 *
 */
public interface ISoundStorage extends INamedSerializableStorage<SoundRecording, SoundRecordingMetaData>, IIndexedSoundIterableFactory {

	public final static String IS_ARCHIVED_LABEL_NAME = "isArchived";


	/**
	 * Get the audio recording from the stored item with the given id.
	 * @param id
	 * @return null if not found.
	 * @throws StorageException
	 */
	byte[] getWAV(String id) throws StorageException;
	
	
	String addWav(double startTimeMsec, String name, byte[] wav, Properties labels, Properties tags) throws StorageException;

	/**
	 * Update the labels on the stored recording with the given id.
	 * @param id
	 * @param labels
	 * @return false if item with id does not exist.
	 * @throws StorageException
	 */
	boolean setLabels(String id, Properties labels) throws StorageException;
	
	/**
	 * Update the tags on the recording with the given id.
	 * @param id
	 * @param tags
	 * @return
	 * @throws StorageException
	 */
	boolean setTags(String id, Properties tags) throws StorageException;

	/**
	 * Update the isTrainable flag on the recording with the given id.
	 * @param id
	 * @param isTrainable
	 * @return
	 * @throws StorageException
	 */
	boolean setIsTrainable(String id, boolean isTrainable) throws StorageException;
	
	/**
	 * Collect the sounds with the given ids as .wav files into the stream representing a zip file and include a metadata.csv file that 
	 * specifies the labels for the sounds.  The names of the files in the zip are either the name assigned in storage (with additional indexing
	 * if duplicate names are found) or the id of the sound if a name is not assigned.  In either case  <code>.wav</code> is appended if not present. 
	 * @param ids
	 * @param output stream to which zipped bytes are written.  Caller must close the stream.
	 */
	void writeMetaDataSoundZip(List<String> ids, OutputStream output) throws StorageException;
	
	/**
	 * A convenience over {@link #writeMetaDataSoundZip(List, OutputStream)}.
	 * @param ids
	 * @return
	 * @throws StorageException
	 */
	byte[] getMetaDataSoundZip(List<String> ids) throws StorageException;

	/**
	 * Add 0 or more SoundRecordings contained in the given byte array representing a zip of wav files and a metadata.csv 
	 * @param zipStream the input stream containing the bytes to unzip.  Caller must close the stream.
	 * @return a list of ids, one for each sound added to storage.
	 * @throws StorageException if all sounds could not be added.
	 * @throws IOException if zippedSounds is not a zip. 
	 */
	List<String> addMetaDataSoundZip(InputStream zipStream) throws StorageException, IOException;

	/**
	 * A convenience over {@link #addMetaDataSoundZip(InputStream)}.
	 * @param zippedSounds
	 * @return
	 * @throws StorageException
	 * @throws IOException
	 */
	List<String> addMetaDataSoundZip(byte[] zippedSounds) throws StorageException, IOException;
;
	
	Iterable<String> findIDsByTimeRange(double startMsec, double endMsec) throws StorageException, IOException;
	
}
