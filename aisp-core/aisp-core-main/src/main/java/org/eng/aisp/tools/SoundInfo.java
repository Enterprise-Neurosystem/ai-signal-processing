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

import java.io.IOException;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPRuntime;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.classifier.TrainingSetInfo;
import org.eng.util.CommandArgs;

public class SoundInfo {
	
	static final String DEFAULT_MODEL_SPEC = "ensemble";
	static final int DEFAULT_SUBWINDOW_MSEC = 5000; 

	public final static String Usage = 
			  "Get information about a set of sounds labeled with one or more \n"
			+ "metadata.csv files or directories containing one. Arguments that\n"
			+ "that are comma-separated will be treated as a single data set.\n"
			+ "Alternatively, the -sounds option may be used together with any\n"
			+ "supported options from the following:\n"
			+ GetModifiedSoundOptions.OptionsHelp
			+ "Examples: \n"
			+ "  ...  metadata.csv\n"
			+ "  ...  dir1,dir2/metadata.csv\n"
			+ "  ...  metadata1.csv metadat2.csv mydir1,mydir2\n"
			+ "  ...  -sounds dir -clipLen 1000\n" 
			;

	public static void main(String args[]) {
		// Force any framework initialization messages to come out first.
		AISPRuntime.getRuntime();
		System.out.println("\n");	// blank line to separate copyrights, etc.
		
		CommandArgs cmdargs = new CommandArgs(args);

		// Check for help request
		if (cmdargs.getFlag("h") || cmdargs.getFlag("-help") ) {
			System.out.println(Usage);
			return;
	    }

		try {
			if (!doMain(cmdargs))
				System.err.println("Use the -help option to see usage");;
		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage());
			if (cmdargs.getFlag("v"))
				e.printStackTrace();
		}
		
	}
	
	protected static boolean doMain(CommandArgs cmdargs) throws AISPException, IOException {
	
		Iterable<SoundRecording> sounds; 
		GetModifiedSoundOptions soundOptions = new GetModifiedSoundOptions(false);
		if (soundOptions.hasApplicableOptions(cmdargs)) {
			if (!soundOptions.parseOptions(cmdargs))
				return false;
			sounds = soundOptions.getSounds();
			TrainingSetInfo tsi= TrainingSetInfo.getInfo(sounds);
			System.out.println(tsi.prettyFormat());
		} else {
			// Get the name of the model to use.
			int count=0;
			for (String file : cmdargs.getArgs()) {
				if (count != 0)
					System.out.println("");
				String filesOrDirs[] = file.split(",");
				sounds = SoundRecording.readMetaDataSounds(filesOrDirs);
				TrainingSetInfo tsi= TrainingSetInfo.getInfo(sounds);
				System.out.println("Sounds referenced in "  + file + "...\n" + tsi.prettyFormat());
				count++;
			}
		}
				
		return true;

	}
}
