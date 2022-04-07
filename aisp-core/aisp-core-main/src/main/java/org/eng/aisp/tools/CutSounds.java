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

import org.eng.aisp.AISPLibraryInitializer;
import org.eng.aisp.SoundRecording;
import org.eng.util.CommandArgs;

public class CutSounds {
	final static double CLIP_LENGTH = 5;  //clip length in seconds

	public static String Usage = "Modifies 1 or more wav files according to the options.\n" 
			+ "Raw unlabeled sounds may be specified as a list of wav files, or 1 or more\n"
			+ "metadata files specifying labels may be used.  Labels are preserved in the\n"
			+ "destination metadata file which is written with the sounds to a destination\n"
			+ "directory. No files will be overwritten unless the -overwrite option is used.\n"
			+ "Options:\n"
			+ GetModifiedSoundOptions.OptionsHelp
			+ "  -o <dir>      : sets the directory to store wav files.  This will be created\n"
			+ "      if it does not already exist. Default is the current directory.\n"
			+ "  -dest-dir <dir> : same as -o option.\n" 
			+ "  -overwrite    : allows overwrite of existing files.\n" 
			+ "Examples: \n"
			+ "   ... -clipLen 500 -o mydir one.wav two.wav\n"
			+ "   ... -clipLen 1000 -dest-dir mydir -sounds dir1,dir2\n" 
			+ "   ... -clipLen 1000 -dest-dir mydir -sounds dir1,metadata.csv -overwrite\n" 
			;

	public static void main(String args[]) {
		AISPLibraryInitializer.Initialize();
		System.out.println("\n");	// blank line to separate copyrights, etc.
		CommandArgs cmdargs = new CommandArgs(args);
		boolean verbose = cmdargs.getFlag("v") || cmdargs.getFlag("verbose"); 

		// Check for help request
		if (cmdargs.getFlag("h") || cmdargs.getFlag("-help") ) {
			System.out.println(Usage);
			return;
	    }

		try {
			if (!doMain(cmdargs, verbose))
				System.err.println("Use the -help option to see usage");
		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage());
			if (verbose)
				e.printStackTrace();
		}
		
	}


	private static boolean doMain(CommandArgs cmdargs, boolean verbose) throws IOException {
		
		GetModifiedSoundOptions soundOptions = new GetModifiedSoundOptions(false);
		if (!soundOptions.parseOptions(cmdargs))
			return false;		// Error message was issued.
		Iterable<SoundRecording> sounds = soundOptions.getSounds();
		if (sounds == null)
			return false;		// Error message was issued.

		String destDir = cmdargs.getOption("o"); 
		if (destDir == null)
			destDir = cmdargs.getOption("dest-dir");
		if (destDir != null) {
			File destFile = new File(destDir);
			if (!destFile.exists()) 
				destFile.mkdirs();
		} else {
			destDir = ".";
		}
		boolean overwrite = cmdargs.getFlag("overwrite"); 

		// Done parsing options, make sure there are none we don't recognize.
		if (ToolUtils.hasUnusedArguments(cmdargs))
			return false;

		return ToolUtils.saveMetadataSounds(sounds, destDir, overwrite);
	}


}
