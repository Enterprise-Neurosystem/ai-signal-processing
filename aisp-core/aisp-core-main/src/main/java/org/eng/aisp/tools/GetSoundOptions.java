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
import java.util.Arrays;
import java.util.List;

import org.eng.aisp.SoundRecording;
import org.eng.aisp.dataset.LabeledSoundFiles;
import org.eng.aisp.dataset.MetaData;
import org.eng.util.CommandArgs;
import org.eng.util.IShuffleIterable;

/**
 * Support parsing list of wav files or csv directories of sounds with or w/o a metadata file.
 * Sounds are always returned as an IShuffleIterable<SoundRecording>.
 * @author DavidWood
 *
 */
public class GetSoundOptions {
	
	public final static String OptionsHelp = 
		  "  [list of wav files] : specifies 1 or more .wav files without labeling.\n"
		+ "     File names are space-separated.  The can not be used with the\n" 
		+ "     -sounds option.\n" 
		+ "  -sounds csv list of (dir|metadata.csv) : specifies 1 or more metadata.csv\n"
		+ "     files referencing sounds or directories containing a metadata.csv.\n"
		+ "     This is an alternative to a list of wav files, but adds labels from \n" 
		+ "     metadata files.\n" 
		+ "  -metadata (all|some) : require that all files listed in the metadata file \n"
		+ "      to be present. Only used with -sounds option. \n"
		+ "      Default does not require all files.\n" 
	;
	
	protected IShuffleIterable<SoundRecording> sounds;

	public IShuffleIterable<SoundRecording> getSounds() {
		return this.sounds;
	}

	/**
	 * Parse the options and establish the return value of {@link #getSounds()}.
	 * @param cmdargs
	 * @return true on now option errors.  false and issue an error message on stderr if an option error.
	 */
	public boolean parseOptions(CommandArgs cmdargs) {
		
		String requireMetaDataOption = cmdargs.getOption("metadata", "some");
		boolean requireAllMetaData = requireMetaDataOption.equals("all");
		String soundDirOrMetaDataFileArg = cmdargs.getOption("sounds",cmdargs.getOption("sound-dir"));
		String[] soundDirOrMetaDataFiles =  soundDirOrMetaDataFileArg == null ? null : soundDirOrMetaDataFileArg.split(",");

		// Get the actual sounds as a list of .wav files or a list of metadatas from the -sounds option.
		List<String> wavFiles = getWavFiles(cmdargs);
		if (wavFiles == null) {	
			return false;	// Error message was issued
		} else if (!wavFiles.isEmpty()) {	// List of existing .wav files.
			try {
				this.sounds = LabeledSoundFiles.loadUnlabeledSounds(wavFiles);
			} catch (IOException e) {
				System.err.println("Could not load 1 or more wave files. " + e.getMessage());
				return false;
			}
		} else if (soundDirOrMetaDataFiles != null) {
			this.sounds = GetSoundOptions.getRequestedSounds(soundDirOrMetaDataFiles, requireAllMetaData);
		} else {
			System.err.println("-sounds option or list of .wav files required to specify location of the sounds in the file system");
			return false;
		}


		return sounds != null;
	}

	/**
	 * Get the list of existing .wav file arguments. 
	 * @param cmdargs upon return, this no longer has the wav files.
	 * @return a list of existing wav files or null if one of the wav files is not found.  0 length list if none specified.
	 */
	private List<String> getWavFiles(CommandArgs cmdargs) {
		List<String> wavFiles = new ArrayList<>();
		
		for (int i=0 ; i<cmdargs.size() ; i++) {
			String arg = cmdargs.peekArg(i);
			if (arg.endsWith(".wav")) {
				File f = new File(arg);
				if (!f.exists()) {
					System.err.println("File " + arg + " does not exist.");
					return null;
				}
				cmdargs.removeArg(i);
				i--;	// next arg is now at the current index.
				wavFiles.add(arg);
			}
		}
		return wavFiles;
	}

	/**
	 * Read sounds according to inputs and issues status messages during progress.
	 * @param soundDirOrMetaDataFiles 1 or more directories and/or metadata files from which to load the sounds.
	 * @param requireAllMetaData if true, then require all sounds listed in the metadata files to be present.
	 */
	private static IShuffleIterable<SoundRecording> getRequestedSounds(String[] soundDirOrMetaDataFiles, boolean requireAllMetaData) {
		IShuffleIterable<SoundRecording> sounds;

		// Make sure files exist
		for (String fname : soundDirOrMetaDataFiles) {
			File f = new File(fname);
			if (!f.exists()) {
				System.err.println(fname + " was not found.");
				return null;
			}
		}

		List<String> soundDirOrMetaDataFilesList = Arrays.asList(soundDirOrMetaDataFiles);
		try {
			System.out.println("Loading sounds from " + soundDirOrMetaDataFilesList);
			sounds = LabeledSoundFiles.loadMetaDataSounds(soundDirOrMetaDataFiles, requireAllMetaData);
		} catch (IOException e) {
			System.err.println("Could not load sounds from one of " + soundDirOrMetaDataFilesList + ": " + e.getMessage());
			return null;
		}
		return sounds;
	}

	/**
	 * 
	 * @return  null and issue error message on stderr if sounds did not come from the file system or other error.
	 */
	public List<String> getSourceFiles() {
		List<String> files = new ArrayList<String>();
		for (SoundRecording sr: sounds) {
			String fileName = sr.getTag(MetaData.FILENAME_TAG);
			if (fileName == null) {
				System.err.println("Sound file name(s) could not be determined.");
				return null;
			}
			files.add(fileName);
		}
		return files;
	}

	/**
	 * Determine if the given options represent something that will be parsed by this instance to produced sounds.
	 * This includes the use of the -sounds option or the presence of .wav files in the list of arguments.
	 * @param cmdargs
	 * @return
	 */
	public boolean hasApplicableOptions(CommandArgs cmdargs) {
		if (cmdargs.hasArgument("sounds"))
			return true;
		for (int i=0 ; i<cmdargs.size() ; i++) {
			String arg = cmdargs.peekArg(i);
			if (arg.endsWith(".wav")) {
				return true;
			}
		}
		return false;
	}
}
