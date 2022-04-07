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

import org.eng.aisp.segmented.LabeledSegmentSpec;
import org.eng.aisp.segmented.SegmentedSoundRecording;
import org.eng.storage.INamedSerializableStorage;
import org.eng.storage.StorageException;

/**
 * Extends the super class to allow access to individual segments defined within the stored clip. 
 * @author DavidWood
 *
 */
public interface ISegmentedSoundStorage extends INamedSerializableStorage<SegmentedSoundRecording, SegmentedSoundRecordingMetaData>, IIndexedSoundIterableFactory {

	String addWAV(double startTimeMsec, String name, byte[] wav, Properties labels, Properties tags, 
			List<LabeledSegmentSpec> segmentSpecs)throws StorageException, IOException;;


	byte[] getWAV(String id) throws StorageException, IOException;
	
	/**
	 * Collect the sounds with the given ids as .wav files into the stream representing a zip file and include a metadata.csv file that 
	 * specifies the labels for the sounds.  The names of the files in the zip are either the name assigned in storage (with additional indexing
	 * if duplicate names are found) or the id of the sound if a name is not assigned.  In either case  <code>.wav</code> is appended if not present. 
	 * @param ids
	 * @param output stream to which zipped bytes are written.  Caller must close the stream.
	 */
	void writeMetaDataSoundZip(List<String> ids, OutputStream output) throws StorageException;

	/**
	 * Add 0 or more SoundRecordings contained in the given byte array representing a zip of wav files and a metadata.csv 
	 * @param zipStream the input stream containing the bytes to unzip.  Caller must close the stream.
	 * @return a list of ids, one for each sound added to storage.
	 * @throws StorageException if all sounds could not be added.
	 * @throws IOException if zippedSounds is not a zip. 
	 */
	List<String> addMetaDataSoundZip(InputStream zipStream) throws StorageException, IOException;


	/**
	 * Convenience on {@link #addMetaDataSoundZip(InputStream)}.
	 * @param zip
	 * @return
	 * @throws IOException 
	 * @throws StorageException 
	 */
	List<String> addMetaDataSoundZip(byte[] zip) throws StorageException, IOException;


	/**
	 * Convenience on {@link #writeMetaDataSoundZip(List, OutputStream)}. 
	 * @param ids
	 * @return
	 */
	byte[] getMetaDataSoundZip(List<String> ids) throws StorageException, IOException;
	

//	boolean setIsTrainable(String id, boolean isTrainable) throws StorageException, IOException;;
}
