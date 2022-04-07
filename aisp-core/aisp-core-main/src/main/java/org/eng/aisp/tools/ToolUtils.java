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
package org.eng.aisp.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eng.aisp.SoundRecording;
import org.eng.aisp.dataset.MetaData;
import org.eng.util.CommandArgs;
import org.eng.util.FileUtils;

public class ToolUtils {
	
	/**
	 * Make sure there are not remaining unprocessed arguments.
	 * @param cmdargs
	 * @return false if no arguments remaining, otherwise true and and error message is put on stderr.
	 */
	public static boolean hasUnusedArguments(CommandArgs cmdargs) {
		String[] args = cmdargs.getArgs();
		if (args.length != 0) {
			System.err.print("Unknown/unrecognized options: ");
			for (int i=0 ; i<args.length ; i++) {
				if (i != 0)
					System.err.print(", ");
				System.err.print(args[i]);
			}
			System.err.println("");
			return true;
		}
		return false;
	}

	/**
	 * Save the given sounds and metadata.csv to the given destination directory.  The names of the files under which to
	 * store the sounds are baesd on the FILENAME_TAG on the sound, if present.  If sounds are sourced from
	 * the same file, then an index is added to the file name when saving.  If no FILENAME_TAG is present, then
	 * a simple index is used.
	 * On success, an message is issued on stdout about numbers of files written.
	 * @param sounds
	 * @param destDir destination directory, must not be null.  Will be created if it does not exist.  If overwrite, then the contents
	 * of an existing directory is removed.
	 * @param overwrite if false, and this would result in overwriting any soounds, then issue an error message and return false.
	 * @return true on success, otherwise false and an error message is issued on stderr.
	 * @throws IOException
	 */
	static boolean saveMetadataSounds(Iterable<SoundRecording> sounds, String destDir, boolean overwrite)
			throws IOException {
		// Get the names of the files to assign to the modified sounds
		List<String> baseNames = getFileNames(sounds);
	
		// Clean up/establish the destination.
		File destFile = new File(destDir);
		boolean exists = destFile.exists();
		if (!exists) {
			destFile.mkdirs();
		} else if (overwrite) {
			FileUtils.deleteDirContents(destFile);
		}

		// Make sure we're not overwriting, unless allowed.
		if (!overwrite && !wouldOverwrite(destDir, baseNames))
			return false;	// Error message was issued.
	
		// Now write the sounds and metadata file.
		MetaData.writeMetaDataSounds(destDir, sounds, baseNames, true);
		
		System.out.println("Wrote " + MetaData.DEFAULT_METADATA_FILE_NAME + " and " + baseNames.size() + " sounds files to " + destDir);
		
		return true;
	}

	private static boolean wouldOverwrite(String destDir, List<String> baseNames) {
		if (!checkFileExists(destDir, destDir + "/" + MetaData.DEFAULT_METADATA_FILE_NAME))
			return false;	// Error message was issued.
		for (String baseName : baseNames) {
			if (!checkFileExists(destDir,baseName))
				return false;	// Error message was issued.
		}
		return true;
	}

	static boolean checkFileExists(String destDir, String baseName) {
		String fname = destDir + "/" + baseName;
		File f = new File(fname);
		if (f.exists())  {
			System.err.println("File " + fname + " already exists. Use -overwrite option to overwrite.");
			return false;
		}
		return true;
		
	}

	/**
	 * Create a list of file names for the given sounds using the FILENAME_TAG attached to each sound, if present.
	 * For each sound that is sourced with the same FILENAMT_TAG value, append an index to the name.
	 * For each sound in which the FILENAME_TAG is not present, use a simple integer index for the name.
	 * All nams are assumed to use the.wav extention.
	 * @param sounds
	 * @return A list of length equal to the number of items in the given iterable.  Never null.
	 */
	private static List<String> getFileNames(Iterable<SoundRecording> sounds) {
		List<String> names = new ArrayList<String>();
		
		// Find which filenames appear more than once
		boolean sawDups = false;
		Map<String,Boolean> fileNameHasDups = new HashMap<>();
		for (SoundRecording sr : sounds) {
			String fileName = sr.getTag(MetaData.FILENAME_TAG);
			if (fileName != null) {
				fileName = FileUtils.getFileName(fileName);
				Boolean hasDups = fileNameHasDups.get(fileName);
				if (hasDups == null) {
					fileNameHasDups.put(fileName, Boolean.FALSE);
				} else if (!hasDups) {
					fileNameHasDups.put(fileName, Boolean.TRUE);
					sawDups = true;
				}
			}
		}	// checking for duplicates
	
		// Loop through and add indices to the files, when multiple sounds are sourced from the same file. 
		int index = 0;
		Map<String,Integer> fileNameIndexes = new HashMap<>();
		for (SoundRecording sr : sounds) {
			String fileName = sr.getTag(MetaData.FILENAME_TAG);
			fileName = fileName == null ? null : FileUtils.getFileName(fileName);
			Integer findex; 
			if (fileName == null) {
				findex = index++;
			} else if (!sawDups) {
				findex = null;
			} else {	// Assign a correct index, if needed
				fileName = FileUtils.getFileName(fileName);
				boolean hasDup = fileNameHasDups.get(fileName); 
				if (!hasDup) {
					findex = null;	// Don't apply an index;
				} else {
					findex = fileNameIndexes.get(fileName); 
					if (findex == null) {
						findex = 0; 
					} else {	// Filename is present.
						findex = findex + 1;
					}
					fileNameIndexes.put(fileName, findex); 
				}
			}
	
			if (findex != null) 
				fileName = insertIndex(fileName, findex); 
			
			names.add(fileName);
		}	// For loop to create names list
		
		return names; 
	}

	static String insertIndex(String baseName, int index) {
		int lastDotIndex = baseName.lastIndexOf('.');
		String newFile = baseName.substring(0,lastDotIndex) + "_" + String.format("%06d",index) + ".wav";
		return newFile;
	}

}
