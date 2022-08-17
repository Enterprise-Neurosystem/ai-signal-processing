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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.gson.Gson;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPRuntime;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.client.IAsyncSensorClient.ICommandListener;
import org.eng.aisp.dataset.IReferencedSoundSpec;
import org.eng.aisp.dataset.MetaData;
import org.eng.aisp.dataset.ReferencedSoundSpec;
import org.eng.util.CommandArgs;
import org.eng.util.OnlineStats;

public class Label {


	public final static String Usage = 
             //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
			  "Classifies sounds using a model to produce a metadata-formatted labeling of the\n"
			+ "sounds on stdou.  While the -clipLen option is not strictly required, it is \n"
			+ "typically used to classify sub-segments of the input sound(s).  Segments from\n"
			+ "the same file that are adjacent in time and have the same label name as \n" 
			+ "produced by the model will be merged.\n"
			+ "Classify mode options\n"
			+ GetModifiedSoundOptions.ClipLenOnlyOptionsHelp
			+ "Label examples: \n"
			+ "  ... -file myclassifier.cfr -clipLen 1000 number1.wav number2.wav\n"
			+ "  ... -file myclassifier.cfr -clipLen 1000 -pad duplicate number1.wav\n"
			+ "  ... -file myclassifier.cfr -clipLen 1000 -pad duplicate -sounds metadata.csv\n"
			;

	public static void main(String args[]) {
		// Try to redirect initial output to stderr, so users can redirectory classification output into a file.
		PrintStream stdout = System.out;
		System.setOut(System.err);

		// Force any framework initialization messages to come out first.
		AISPRuntime.getRuntime();
		System.out.println("\n");	// blank line to separate copyrites, etc.
		
		CommandArgs cmdargs = new CommandArgs(args);

		// Check for help request
		if (cmdargs.getFlag("h") || cmdargs.getFlag("-help") ) {
			System.out.println(Usage);
			return;
	    }
		System.setOut(stdout);

		Label c = new Label();
		try {
			if (!c.doMain(cmdargs))
				System.err.println("Use -help option to see usage");
		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage());
			if (cmdargs.getFlag("v"))
				e.printStackTrace();
		}
		
	}
	

	/**
	 * Do the work implied by the arguments.
	 * @param cmdargs
	 * @return false and issue an error message on error, otherwise true.
	 * @throws AISPException
	 * @throws IOException 
	 */
	protected boolean doMain(CommandArgs cmdargs) throws AISPException, IOException {
	
		// Point stdout at stderr so that messages produced during argument parsing do not appear in 
		// redirected command lines (ala classify > file.out).
		PrintStream stdout = System.out;
		System.setOut(System.err);
		
		// Load the named classifier or setup for remote classification
		GetTrainedModelOptions modelOptions = new GetTrainedModelOptions();
		if (!modelOptions.parseOptions(cmdargs)) 
			return false;
		IFixedClassifier<double[]> classifier = modelOptions.getClassifier();
		if (classifier == null)
			return false;
		
		OnlineStats stats = new OnlineStats();
		GetModifiedSoundOptions soundOptions = new GetModifiedSoundOptions(false, false,false);	// No -label, no -balance, no -clipShift
		if (!soundOptions.parseOptions(cmdargs)) 
			return false;
		Iterable<SoundRecording> sounds = soundOptions.getSounds();
		System.setOut(stdout);	
		
		labelSounds(classifier, sounds);

		return true;

	}


	private void labelSounds(IFixedClassifier<double[]> classifier, Iterable<SoundRecording> sounds) throws AISPException, IOException {

		// See if there are duplicate filenames so that we know whether or not to include indices and start/stop times.
		Map<String, List<ReferencedSoundSpec>> labelings = new HashMap<>();
		String trainedLabel = classifier.getTrainedLabel();
		for (SoundRecording sr : sounds) {
			Properties tags = sr.getTagsAsProperties();
			String fileName = tags.getProperty(MetaData.FILENAME_TAG);
			
			// Classify the sample and keep track of the amount of time. 
			SoundClip clip = sr.getDataWindow(); 
			Map<String,Classification> classifications = classifier.classify(clip);
			Classification c = classifications.get(trainedLabel);
			if (c == null) {
				System.err.println("Model did not produce a classification with label " + trainedLabel);
				continue;
			}
			int startMsec = (int)Math.round(clip.getStartTimeMsec()); 
			int endMsec = (int)Math.round(clip.getEndTimeMsec()); 
			Properties labels = new Properties();
			labels.put(trainedLabel, c.getLabelValue());
			ReferencedSoundSpec rss = new ReferencedSoundSpec(fileName, startMsec, endMsec, labels,null);
			List<ReferencedSoundSpec> rssList = labelings.get(fileName);
			if (rssList == null) {
				rssList = new ArrayList<>();
				labelings.put(fileName, rssList);
			}
			rssList.add(rss);
		}
		
		List<String> fileNames = new ArrayList<>();
		fileNames.addAll(labelings.keySet());
		Collections.sort(fileNames);
		
		MetaData md = new MetaData();
		for (String fn : fileNames) {
			List<ReferencedSoundSpec> rssList = labelings.get(fn);
			rssList = ReferencedSoundSpec.mergeLabelValues(rssList, trainedLabel);
			for (ReferencedSoundSpec rss : rssList) 
				md.add(rss);
		}
		md.write(System.out, true, false);

	}


}
