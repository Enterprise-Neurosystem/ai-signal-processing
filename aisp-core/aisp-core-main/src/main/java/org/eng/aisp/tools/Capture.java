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
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPRuntime;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.dataset.MetaData;
import org.eng.aisp.monitor.QueuedSoundCapture;
import org.eng.aisp.util.PCMUtil;
import org.eng.util.CommandArgs;

public class Capture {

	public static final String ORGID_KEY = "Organization-ID";
	public static final String APIKEY_KEY = "API-Key";
	public static final String AUTHTOKEN_KEY = "Authentication-Token";
	public final static String TOKEN_FILE = "device-token.properties";
	public final static int DEFAULT_CLIP_MSEC = 5000;
	public final static int DEFAULT_PAUSE_MSEC = 0;
	public final static String Usage = 
			  "Captures, labels and stores sound clips.\n"
			+ "Sounds may be stored locally and labeled in a metadata.csv file and/or\n"
			+ "they can be stored in a named server under a specific name.\n"
			+ "Usage: ... -label <label=value> [-n N] [-name <sound name>] \n"
			+ "           (-dir dir | -host host) [ host options]\n"
			+ "Options:\n"
			+ "  -label name=value: specifies a name/value pair to stored as labels for all\n"
			+ "               sounds captured during the run. More than one of this options\n" 
			+ "               can be provided to apply multiple labels to the sounds. If the\n" 
			+ "               -name option is not used, then the first label value will be\n"
			+ "               used as the base name for stored files and files stored in the\n"
			+ "               server. \n" 
			+ "               NOTE: double-quotes must surround name=value on Windows.\n" 
			+ "  -dir dir : a directory in which to store sounds and metadata.csv.\n" 
			+ "               If a metadata.csv file already exists there, referenced sounds\n"
			+ "               will not be overwritten and metadata.csv will be appended to.\n" 
			+ "  -clipLen msec : the length of the captured sounds in milliseconds. Default is " + DEFAULT_CLIP_MSEC +".\n"
			+ "  -pauseLen msec : the number of milliseconds between captured sounds. Default is " + DEFAULT_PAUSE_MSEC +".\n"
			+ "  -n count : the number of sounds recordings to capture. Default is unlimited.\n" 
			+ "  -name name : the based name used to store sounds in the file system.\n"
			+ "               If not provided then the value of the first label is used.\n"
			+ "Examples: \n"
			+ "  ... -label status=normal -dir .\n"
			+ "  ... -label status=normal -dir . -n 10 \n"
			+ "  ... -label status=normal -label source=fan -dir .\n"
			+ "  ... -label -name chiller status=normal -dir . \n"
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
			Capture c = new Capture();
			if (!c.doMain(cmdargs))
				System.err.println("Use -help option to see usage");;
		} catch (AISPException e) {
			System.err.println("ERROR: " + e.getMessage());
			if (cmdargs.getFlag("v"))
				e.printStackTrace();
		}
		
	}
	
	/** General options */
	private String soundName;
	private Properties labels;


	
	private int clipLenMsec;
	private int pauseMsec;
	private String directory;
	private int soundCount;
	

	/**
	 * Do the work implied by the arguments.
	 * @param cmdargs
	 * @return false and issue an error message on error, otherwise true.
	 * @throws AISPException
	 */
	protected boolean doMain(CommandArgs cmdargs) throws AISPException {
	
		// Parse the options that apply to most operating modes.
		if (!parseOptions(cmdargs))
			return false;

			
		try {
			MetaData md;
			String metaDataFile = directory + "metadata.csv";
			if (directory != null && new File(metaDataFile).exists()) 
				md = MetaData.read(metaDataFile);
			else
				md = new MetaData();

			QueuedSoundCapture monitor = new QueuedSoundCapture(clipLenMsec, pauseMsec);

			monitor.start();
			SoundClip clip;
			int maxWaitMsec = 10 * (clipLenMsec + pauseMsec);
			int index = getNextSoundCounter(md,soundName);
			int count = 0;
			while ((clip = monitor.next(maxWaitMsec)) != null) {
				System.out.print("Captured sound #" + count);
				 
				if (directory != null) {
					String fname = String.format("%s_%04d.wav", soundName, index);
					String fullname = directory + fname; 
					PCMUtil.PCMtoWAV(fullname, clip);
					System.out.print(" in file " + fullname);
					SoundRecording sr = new SoundRecording(clip,labels);
					md.add(fname, sr);	// Use sr so we preserve startMsec in metadata
					md.write(metaDataFile);
					index++;
				}
				System.out.println("");
				count++;
				if (soundCount > 0 && count == soundCount)
					break;
			}
			return true;
		} catch (Exception e) {
			System.err.println("Could not capture : " + e.getMessage());
			return false;
		}

	}

	private int getNextSoundCounter(MetaData md, String soundName) {
		int counter =  -1;
		for (String file : md.getFiles()) {
			if (file.startsWith(soundName)) {
				file = file.replaceAll("\\.wav", "");
				int index = file.lastIndexOf("_");
				file = file.substring(index+1, file.length());
				int t = Integer.parseInt(file);
				if (t > counter)
					counter = t;
			}
		}
		return counter + 1;
		
	}

	/**
	 * @param cmdargs
	 */
	private boolean parseOptions(CommandArgs cmdargs) {
		// Get the name of the model to use.
		soundName = cmdargs.getOption("name");
		labels = new Properties();
		String labelspec;
		while ((labelspec = cmdargs.getOption("label")) != null) {
			if (!labelspec.contains("=")) {
				System.err.println("Label specification must be in the format of <label>=<value>.  Got " + labelspec + ".\nWindows requires double-quotes around the value.");

				return false;
			}
			String v[] = labelspec.split("=");
			labels.put(v[0], v[1]);
			if (soundName == null)
				soundName = v[1];
		}
		if (labels.size() == 0) {
			System.err.println("At least one label must be provided");
			return false;
		}

		
		directory = cmdargs.getOption("dir");
		if (directory != null) {
			if (!directory.endsWith("/") && !directory.endsWith("\\"))
				directory += "/";
			File d = new File(directory);
			if (!d.exists())  {
				d.mkdirs();
			} else if (!d.isDirectory()) {
				System.err.println(directory + " is not a directory.");
				return false;
			}
		}
		
		soundCount = cmdargs.getOption("n", 0);
		clipLenMsec = cmdargs.getOption("clipLen", DEFAULT_CLIP_MSEC);
		pauseMsec = cmdargs.getOption("pauseLen", DEFAULT_PAUSE_MSEC);	
		// Done parsing options, make sure there are none we don't recognize.
		if (ToolUtils.hasUnusedArguments(cmdargs))
			return false;
		return true;
	}


}
