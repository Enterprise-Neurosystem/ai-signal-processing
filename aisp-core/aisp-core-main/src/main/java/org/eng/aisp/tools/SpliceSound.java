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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLibraryInitializer;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.segmented.LabelSequence;
import org.eng.aisp.util.VectorUtils;
import org.eng.util.CommandArgs;
import org.eng.util.IShuffleIterable;

public class SpliceSound {
	
//	String outputLabels = cmdargs.getOption("-labels","spliced.txt");
//	String outputWav = cmdargs.getOption("-wav","spliced.wav");
//	String labelName = cmdargs.getOption("l","source");
//	String gapLabel = cmdargs.getOption("-glabel","gap");
//	int gapDurationMsec = cmdargs.getOption("-gap", 1000);
//	boolean smoothTransitions = cmdargs.getFlag("smooth");

	public final static String Usage = 
//            xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
			  "Concatenate labeled sounds together to create a wav file and file describing\n"
			+ "the start and stop times of each segment and their labels.\n" 
			+ "Labeled sounds are specified by one or more metadata.csv files.\n"
			+ "By default output is spliced.wav containing all concatenated sounds and \n"
			+ "spliced.txt.  The latter contains one line for each segment, its\n"
			+ "start and stop time in seconds from the begining of the output wav file\n"
			+ "and the set of label=value from the source labels.\n"
			+ "Options:\n"
			+ "-labels <file> : specifies the output file containing the segmented labeling\n"
			+ "         Default is spliced.txt\n"
			+ "-wav <file> : the name of the wav file to write concatenated sounds\n"
			+ "         Default is spliced.wav\n"
			+ "-l <lname> : the label name applied to gap sounds. Default is 'source'\n"
			+ "-glabel <lvalue> : the label value to apply to gap sounds. \n"
			+ "         Default is 'gap'\n"
			+ "-gap msec : length of gap sound in milliseconds. Default is 1000.\n"
			+ "-smooth : applies a hamming filter to all sounds before concatentating.\n"
			+ "-shuffle: causes sound clips to be randomly shuffled before splicing.\n" 
			+ "-merge  : causes consecutive clips with the same label value to be listed\n"
			+ "          in the same segment in spliced.txt.  \n"  
			+ "Examples: \n"
			+ "  ...  metadata.csv\n"
			+ "  ...  -smooth metadata.csv\n"
			+ "  ...  -gap 500 -l class -glabel mygap -smooth dir1 dir2/metadata.csv\n"
			;

	public static void main(String args[]) {
		AISPLibraryInitializer.Initialize();
		System.out.println("\n");	// blank line to separate copyrights, etc.
		
		CommandArgs cmdargs = new CommandArgs(args);
		boolean verbose = cmdargs.getFlag("v");

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
			if (verbose)
				e.printStackTrace();
		}
		
	}



	private static class DoubleBuffer {
		List<double[]> data = new ArrayList<double[]>();
		
		public void append(double[] d) {
			data.add(d);
		}
		
		public double[] concatentate() {
			int len = 0;
			for (double[] d : data) 
				len += d.length;
			double concat[] = new double[len];
			int index = 0;
			for (double[] d : data) {
				System.arraycopy(d, 0, concat, index, d.length);
				index += d.length;
			}
			return concat;
		}
		
	}

	protected static boolean doMain(CommandArgs cmdargs) throws AISPException, IOException {
		
		String outputLabels = cmdargs.getOption("-labels","spliced.txt");
		String outputWav = cmdargs.getOption("-wav","spliced.wav");
		String labelName = cmdargs.getOption("l","source");
		String gapLabel = cmdargs.getOption("-glabel","gap");
		int gapDurationMsec = cmdargs.getOption("-gap", 1000);
		boolean smoothTransitions = cmdargs.getFlag("smooth");
		boolean merge = cmdargs.getFlag("merge");
		boolean shuffle = cmdargs.getFlag("shuffle");

		Properties gapLabels = new Properties();
		gapLabels.setProperty(labelName, gapLabel);

		SoundClip gapSound = null; 

		String fileArgs[] = cmdargs.getArgs();

		LabelSequence labels = new LabelSequence(merge);
		DoubleBuffer buffer = new DoubleBuffer();
		double durationMsec = 0;
		
		boolean first = true;
		for (String file : fileArgs) {
			IShuffleIterable<SoundRecording> sounds = SoundRecording.readMetaDataSounds(file);
			if (shuffle)
				sounds = sounds.shuffle();
			for (SoundRecording sr : sounds) {
				SoundClip clip = sr.getDataWindow();
				if (first && gapDurationMsec > 0) {	// Create the gap sound, if needed.
					double[] noise = new double[(int)(gapDurationMsec / 1000.0 * clip.getSamplingRate())];
					gapSound = new SoundClip(0, gapDurationMsec, noise); 
					if (gapSound != null && smoothTransitions) 
						VectorUtils.applyHammingWindow(gapSound.getData(), true);
				}
				if (!first && gapSound != null) {	// Insert the gap sound.
					buffer.append(gapSound.getData());
					labels.append(durationMsec, durationMsec + gapDurationMsec, gapLabels, null);
					durationMsec += gapSound.getDurationMsec();
				}
				if (smoothTransitions) 
					VectorUtils.applyHammingWindow(clip.getData(), true);
				double clipDurationMsec = clip.getDurationMsec() ;
				buffer.append(clip.getData());
				labels.append(durationMsec, durationMsec + clipDurationMsec , sr.getLabels(), sr.getTagsAsProperties());
				durationMsec += clipDurationMsec;
				first = false;
			}
		}
			
		SoundClip spliced = new SoundClip(0, durationMsec, buffer.concatentate());
		spliced.writeWav(outputWav);
		labels.write(outputLabels);
		
		return true;

	}
}
