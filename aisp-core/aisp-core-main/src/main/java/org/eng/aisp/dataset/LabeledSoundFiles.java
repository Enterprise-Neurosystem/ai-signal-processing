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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.util.IShuffleIterable;

/**
 * Helper class to manage sounds and associated metadata files.
 * <br>
 * TODO: some of this function should be moved to MetaData class.
 * @author dawood
 *
 */
public class LabeledSoundFiles {



	/**
	 * @param metaDataCSV file containing metadata about sounds to be loaded. 
	 * @param requireMetaData if true require the given metadata file to exist. See the return conventions.
	 * and the file does not exist, then just return an empty MetaData instance.
	 * @return null if the metdata file does not exist and requireMetaData is false, otherwise throw an exception 
	 * if required and not found.  If found, return the MetaData object.
	 * @throws IOException
	 */
	private static MetaData loadMetaData(String metaDataCSV , boolean requireMetaData) throws IOException {
		MetaData metadata = null;
		
		File f = new File(metaDataCSV);
		if (f.exists()) {
			try {
				metadata = MetaData.read(metaDataCSV);
			} catch (IOException e) {
				throw new IOException("Could not read the meta data file at " + metaDataCSV + ": " + e.getMessage());
			}
		} else if (requireMetaData) {
			throw new IOException(metaDataCSV + " file does not exist.");
			
		}
		return metadata;
	}
	
	/**
	 * Find all sound files in the iterable of files and/or dirs (using {@link SoundClip#getSoundFiles(Iterable)}
	 * @param soundFilesOrDirs
	 * @return
	 * @throws IOException 
	 */
	public static ISoundDataSet loadUnlabeledSounds(Iterable<String> soundFilesOrDirs) throws IOException {
		MetaData md = new MetaData(SoundClip.getSoundFiles(soundFilesOrDirs));
		return md.readSounds();
	}

	/**
	 * Read sounds from 1 or more metadata files.
	 * @param src list containing names of directories with metadata.csv files and/or metadata.csv files.
	 * @param requireAllFiles if true, then throw an exception if the files referenced in the metadata file 
	 * were not found in the given directory.  Otherwise, just ignore any missing files. 
	 * @return never null.
	 * @throws IOException
	 */
	public static  ISoundDataSet  loadMetaDataSounds(String[] src, boolean requireAllFiles) throws IOException {
		MetaData md = MetaData.read(src);
		return md.readSounds(requireAllFiles);
	}

	/**
	 * Load sounds listed in the {@value #DEFAULT_METADATA_FILENAME} file and found in the given directory.
	 * File formats supports are those supported by {@link LabeledSoundFileIterable} (at least mp3 and wav). 
	 * @param src the name of a file with the '.csv' extension containing meta data about sounds or a directory containing the {@value #DEFAULT_METADATA_FILENAME} file.
	 * @param requireAllFiles if true, then throw an exception if the files referenced in the metadata file 
	 * were not found at the location specified in the metadata file..  Otherwise, just ignore any missing files. 
	 * @return
	 * @throws IOException if metadata file can not be found.
	 */
	public static  ISoundDataSet loadMetaDataSounds(String src, boolean requireAllFiles) throws IOException {
		File metaDataCSV = MetaData.getMetaDataFile(src, true);
		MetaData md = MetaData.read(metaDataCSV.getAbsolutePath());
		return md.readSounds(requireAllFiles);
	}

	
	/**
	 * Load sound files in the given directory and optionally metadata from {@value #DEFAULT_METADATA_FILENAME} in that directory.
	 * If a metadata file is present, then only load sound files referenced and don't require all listed to be present.
	 * If a metadata file is not present, then load all files in the directory, but all will have no labels. 
	 * File formats supported are those supported by {@link LabeledSoundFileIterable} (at least mp3 and wav), which
	 * ultimately uses {@link MetaData#readSound(String)} which adds the tag {@link MetaData#FILENAME_TAG}.
	 * @param srcDir directory containing sounds and optionally a metadata.csv file.
	 * @param requireMetaData if true then throw an exception if the metadata.csv file is not found in the given directory.
	 * @return
	 * @throws IOException
	 * @see {@link MetaData}
	 */
	public static  ISoundDataSet  loadSounds(String srcDir, boolean requireMetaData) throws IOException {
		// Parse the metadata 
		MetaData metadata = loadMetaData(srcDir + "/" + MetaData.DEFAULT_METADATA_FILE_NAME, requireMetaData);

		if (metadata != null) { 
			return loadMetaDataSounds(srcDir, false);
		} else { 
			return  loadUnlabeledSounds(Arrays.asList(srcDir));
//			List<String> soundFiles = new ArrayList<String>();
//			// Get the array of potential wav files.
//			File srcFile = new File(srcDir);
//			String[] files = srcFile.list();
//			if (files != null) {
//				// Filter out non-wav files from the list of potential files
//				for (String file: files) {
//					if (PCMUtil.isFileFormatSupported(file)) {
//						soundFiles.add(file);
//					}
//				}
//			}
//			// Read them in lexigraphic order
//			Collections.sort(soundFiles);
//			// Create the MetaData with refernces to the files, but unlabeled.
//			metadata = new MetaData(srcDir); 
//			for (String f : soundFiles) {
//				Record r = new Record(f, null, null);
//				metadata.add(r);
//			}
		}
	
	}

	


	/**
	 * Write the given sounds and associated metadata file to the given directory.
	 * Sounds are written as WAV files with .wav extension. The names of the sounds
	 * are integer indexes, for example, 000001.wav unless names are provided.
	 * @param dest a directory or the name of a metadata file with a .csv extension.  If the latter sounds are written to the directory specified.
	 * @param sounds
	 * @param names names to be used to store the corresponding sounds.  If null, empty or smaller than the iterable of sounds, then an 
	 * incremented index is used to name the files.  The iterable may contain null values in which case the corresponding sound will be saved
	 * in an indexed file name.
	 * @return never null. the names of the sound files as written to the destination directory. 
	 * @throws IOException
	 */
	public static List<String> writeMetaDataSounds(String dest, Iterable<SoundRecording> sounds, Iterable<String> names) throws IOException {
		return MetaData.writeMetaDataSounds(dest, sounds, names, true );

	}


	private static String getSoundFilePath(String metaDataDir, String soundFileName) {
		boolean absPath = soundFileName.startsWith("/") || soundFileName.startsWith("\\") || soundFileName.indexOf(":") == 1;
		if (absPath)
			return soundFileName;
		return metaDataDir + "/" + soundFileName;
	}

	/**
	 * Delete the sounds specified by a metadata file indicated by the location parameter.
	 * @param location a directory containing {@value #DEFAULT_METADATA_FILENAME} file or the name of a metadata file.
	 * @throws IOException
	 */
	public static void deleteSounds(String location) throws IOException {
		File metaDataCSV = MetaData.getMetaDataFile(location, false);
		if (!metaDataCSV.exists())
			return;
		
		String metaDataDir = metaDataCSV.getParent();
		 IShuffleIterable<SoundRecording>  sounds = loadMetaDataSounds(metaDataCSV.getAbsolutePath(), true); 
		for (String soundFile : sounds.getReferences()) {
			String absFile = getSoundFilePath(metaDataDir, soundFile);
			File f = new File(absFile);
			if (f.exists() && !f.delete())
				throw new IOException("Could not delete file " + f.getAbsolutePath());
		}
		if (!metaDataCSV.delete())
			throw new IOException("Could not delete file " + metaDataCSV.getAbsolutePath());
			
	}

	public static ISoundDataSet loadUnlabeledSounds(String fileOrDir) throws IOException {
		List<String> f = new ArrayList<String>();
		f.add(fileOrDir);
		return loadUnlabeledSounds(f);
	}
	


}
