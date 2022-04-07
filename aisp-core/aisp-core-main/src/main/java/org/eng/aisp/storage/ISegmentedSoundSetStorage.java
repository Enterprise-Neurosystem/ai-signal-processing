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
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.classifier.TrainingSetInfo;
import org.eng.aisp.segmented.LabeledSegmentSpec;
import org.eng.aisp.segmented.SegmentedSoundRecording;
import org.eng.storage.IReferencingStringSetStorage;
import org.eng.storage.NamedStringSet;
import org.eng.storage.StorageException;

public interface ISegmentedSoundSetStorage extends IReferencingStringSetStorage<NamedStringSet, SegmentedSoundRecording>, IIndexedSoundIterableFactory {
	
	public String addWAV(String setID, double startTimeMsec, String name, byte[] wav, Properties labels, Properties tags, 
			List<LabeledSegmentSpec> segmentSpecs) throws StorageException;

	public byte[] getWAV(String memberID) throws StorageException;
	
	/**
	 * Get all the metadata for all the ids.  fail if any one of the metadata could not be found.
	 * @param memberIDs
	 * @return never null and a list the same size as the list of memberIDs.
	 * @throws StorageException if anyone of the ids are a bad or the metadata could not be found for any id.
	 */
	public Iterable<SegmentedSoundRecordingMetaData> getMetaData(Iterable<String> memberIDs) throws StorageException;

	public SegmentedSoundRecordingMetaData getMetaData(String memberID) throws StorageException;
	
	public void setSegmentLabels(String memberID, Collection<LabeledSegmentSpec> segmentSpecs) throws StorageException;
	
	public List<TrainingSetInfo> getTrainingSetInfo(Collection<String> setIDs) throws StorageException; 
	
	/**
	 * Collect the sounds contained in the sets with the given ids as .wav files into the stream representing a zip file and include a metadata.csv 
	 * file that specifies the labels for the sounds.  The names of the files in the zip are either the name assigned in storage (with 
	 * additional indexing. if duplicate names are found) or the id of the sound if a name is not assigned.  
	 * In either case  <code>.wav</code> is appended if not present. 
	 * @param setIDs ids of sets to export
	 * @param output stream to which zipped bytes are written.  Caller must close the stream.
	 */
	void writeMetaDataSoundZip(List<String> setIDs, OutputStream output) throws StorageException;
	
	/**
	 * Add 0 or more SegmentedSoundRecordings contained in the given byte array representing a zip of wav files and a metadata.csv 
	 * @param setID the destination set to receive the added sounds.
	 * @param zipStream the input stream containing the bytes to unzip.  Caller must close the stream.
	 * @return a list of ids, one for each sound added to storage.
	 * @throws StorageException if all sounds could not be added.
	 * @throws IOException if zippedSounds is not a zip. 
	 */
	List<String> addMetaDataSoundZip(String setID, InputStream zipStream) throws StorageException, IOException;
	
	/**
	 * Convenience on {@link #addMetaDataSoundZip(InputStream)}.
	 * @param setID set to add the sounds to.
	 * @param zip
	 * @return
	 * @throws IOException 
	 * @throws StorageException 
	 */
	List<String> addMetaDataSoundZip(String setID, byte[] zip) throws StorageException, IOException;


	/**
	 * Convenience on {@link #writeMetaDataSoundZip(List, OutputStream)}. 
	 * @param setIDs 
	 * @return
	 */
	byte[] getMetaDataSoundZip(List<String> setIDs) throws StorageException, IOException;
}
