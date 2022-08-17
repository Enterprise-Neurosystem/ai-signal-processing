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
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPRuntime;
import org.eng.aisp.DataTypeEnum;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.IFixableClassifier;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.classifier.TrainingSetInfo;
import org.eng.aisp.util.FixedClassifiers;
import org.eng.aisp.util.GsonUtils;
import org.eng.util.CommandArgs;
import org.eng.util.IShuffleIterable;

import com.google.gson.Gson;

public class Train {
	
	static final int DEFAULT_SUBWINDOW_MSEC = 5000; 

	public final static String Usage = 
			"Trains a model with set of labeled sounds.\n"
			+ "Sounds may be referenced locally or in a server and the model may be trained\n"
			+ "locally or in a server. The trained classifier can be stored in a local file.\n"
			+ "Default is to train locally on local sounds and not store the classifier.\n"
			+ "Required options:\n"
			+ "  -label, -sounds: see below\n"
			+ "Model specification options:\n"
			+ GetModelOptions.OptionsHelp
			+ "Sound specification options:\n"
			+ GetModifiedSoundOptions.OptionsHelp
            // xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
			+ "  -seed <seed> : randomly shuffle the training data using the given random \n"
			+ "        integer seed.\n" 
			+ "Additional options:\n"
			+ "  -noinfo : causes no information about the training data to be displayed.\n"
			+ "      This may be useful to speed training on large training sets.\n"
			+ "  -file <file> : a file into which the classifier can be written. Optional.\n" 
			+ "  -data-type (audio|xyz|mag) : specifies the type of data contained in the wav\n"
			+ "      files and attaches an associated data type tag to the trained model.\n"
			+ "      Default is audio.\n"
			+ "  -fixed : flag that indicates the " + IFixedClassifier.class.getSimpleName() + " of the model should\n"
			+ "      be stored instead of the trainable " + IClassifier.class.getSimpleName() + " version.\n"
			+ "  -verbose : cause extra messaging to be dipslayed on the console.\n"

			+ "Examples: \n"
			+ "  ... -label status -sounds m1.csv,m2.csv-output classifier.cfr \n" 
			+ "  ... -label status -sounds m3.csv -file classifier.cfr -model model.js\n" 
			+ "  ... -label status -sounds mydir -file classifier.cfr -model gmm\n" 
			+ "  ... -label status -sounds mydir -file classifier.cfr -model gmm -data-type xyz\n" 
			;

	public static void main(String args[]) {
		// Force any framework initialization messages to come out first.
		AISPRuntime.getRuntime();
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
				System.err.println("Use the -help option to see usage");;
		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage());
			if (verbose)
				e.printStackTrace();
		}
		
	}
	

	
	protected static boolean doMain(CommandArgs cmdargs, boolean verbose) throws AISPException, IOException {
		
		// Get the name of the model to use.
		String outputFileName = cmdargs.getOption("file");	// Use the same option as CLIs that load models from the file system.
		if (outputFileName == null)
			outputFileName = cmdargs.getOption("output");	// For backwards compatability (used to be -output).
		boolean noinfo = cmdargs.getFlag("noinfo");
		boolean storeFixed = cmdargs.getFlag("fixed");
		DataTypeEnum dataType = DataTypeEnum.Audio;
		String dataTypeValue = cmdargs.getOption("data-type");
		int seed = cmdargs.getOption("seed", 0); 


		if (dataTypeValue != null)  
			dataType = DataTypeEnum.parseDataType(dataTypeValue);
		Properties dataTypeTag = DataTypeEnum.setDataType(null, dataType);	
		
		GetModelOptions modelOptions = new GetModelOptions();
		if (!modelOptions.parseOptions(cmdargs)) 
			return false;		// Error message was issued
		IClassifier<double[]> classifier =  modelOptions.getClassifier(); 
		
		// Parse the options the specify the sounds to be used.
		GetModifiedSoundOptions soundOptions = new GetModifiedSoundOptions(true, true, true);
		if (!soundOptions.parseOptions(cmdargs))
			return false;
		String trainingLabel = soundOptions.getLabel();
		// Done parsing options, make sure there are none we don't recognize.
		if (ToolUtils.hasUnusedArguments(cmdargs))
			return false;
			


		// Get an iterator for the sounds to train on. 
		IShuffleIterable<SoundRecording> sounds = null;

		TrainingSetInfo trainingSetInfo = null;
				sounds = soundOptions.getSounds(); 
				if (sounds == null)
					return false;
	 
		if (seed != 0)
			sounds = sounds.shuffle(seed);

		trainingSetInfo = noinfo ? null : TrainingSetInfo.getInfo(sounds);
		
		long startTrainingMsec;
		long endTrainingMsec;

//		if (localTraining) {
			// Local training
			System.out.println("Training local model on label " + trainingLabel );
			System.out.println("Training using classifier " + classifier);
			if (!noinfo) {
			    if (trainingSetInfo == null)
			    		trainingSetInfo = TrainingSetInfo.getInfo(sounds);
				System.out.println("Training sounds: " + trainingSetInfo.prettyFormat());
			}
			startTrainingMsec = System.currentTimeMillis();
			classifier.train(trainingLabel, sounds);
			classifier.addTags(dataTypeTag);
			endTrainingMsec = System.currentTimeMillis();

//		} else { 
//			// Remote training
//			System.out.println("Training remote model on label " + trainingLabel );
//			List<String> soundIDs = null; 
//			if (localSounds) {	// Store the local sounds in the server, temporarily. 
//				assert sounds != null;
//				soundIDs = new ArrayList<String>();
//				soundName = UUID.randomUUID().toString();
//				for (SoundRecording sr : sounds) {
//					byte[] wav;
////					try {
//						wav = PCMUtil.PCMtoWAV(sr.getDataWindow());
//						String id = sensorClient.storeWav(sr.getDataWindow().getStartTimeMsec(), soundName, "dummy", wav, sr.getLabels(), null);
//						soundIDs.add(id);
////					} catch (IOException e) {
////						System.err.println("Could not convert recording to PCM: " + e.getMessage());
////					}
//				}
//			}
			
//			// Train the model in the server.
//			String tmpModelName;
//			if (modelName == null)
//				tmpModelName = UUID.randomUUID().toString();
//			else
//				tmpModelName = modelName;
//			SoundSelector soundSelection = new SoundSelector(DataTypeEnum.Audio,soundName, null);
//			if (!noinfo) {
//				if (trainingSetInfo == null)
//					trainingSetInfo = sensorClient.getTrainingSetInfo(soundSelection);
//				System.out.println(trainingSetInfo.prettyFormat());
//			}
//			ModelTrainingConfig mtc = new ModelTrainingConfig(modelSpecification, tmpModelName,dataTypeTag, soundSelection, trainingLabel);
//			startTrainingMsec = System.currentTimeMillis();
//			Object trainCookie = sensorClient.train(mtc);
//			while (modelID == null) {
//				System.out.println("Waiting for remote model to be trained.");
//				try {
//					modelID = sensorClient.isTrainingComplete(trainCookie); //  sensorClient.getModelID(tmpModelName);
//				} catch (TrainingException e1) {
//					System.out.println("Training generated exception on the server: " + e1.getMessage());
//					return false;
//				} 
//				try { Thread.sleep(5000); } catch (InterruptedException e) { ; }
//			}
//			endTrainingMsec = System.currentTimeMillis();
//			System.out.println("Remote model training complete");
//
//			// We're going to save some form of the classifier, so get it into the JVM.
//			if (outputFileName != null) {
//				IFixedClassifier c = modelStorageClient.findItem(modelID);
//				if (!(c instanceof IClassifier)) {
//					System.err.println("Remote model trained to an untrainable classifier");
//					return false;
//				}
//				classifier = (IClassifier)c;
//			}

//			// Clean up the sounds if we added to the server.
//			if (localSounds) {
//				assert soundStorage != null && soundIDs != null;
//				soundStorage.delete(soundIDs);
//			}
//			if (modelName == null ) {	// User did not want to store the model in the server
//				assert modelStorage != null;
//				assert modelID != null;
//				modelStorage.delete(modelID);
//			}
//		}	// Done local/remote training.

		// Print out the summary stats.
		if (verbose)  {
			Gson gson = GsonUtils.getInterfaceSerializer(true).create();
			System.out.println("Trained classifier: " + classifier.getClass().getName() + "\n" + gson.toJson(classifier));
		} else  {
			System.out.println("Trained classifier: " + classifier.getClass().getSimpleName());
		}
		double trainingMsec = endTrainingMsec - startTrainingMsec;
		String msg = "Completed model training in " + (trainingMsec)/ 1000.0 + " seconds";
		if (trainingSetInfo != null) {
			double percent = trainingSetInfo == null ? Double.NaN : trainingMsec / trainingSetInfo.getTotalMsec(); 
			msg += ", " + 100.0* percent + "% of training data";
		}
		System.out.println(msg);
	

		// Store the classifier locally if requested.
		if (outputFileName != null) { 
			IFixedClassifier classifier2Save = classifier;
			if (storeFixed) { 
				if (classifier instanceof IFixableClassifier) {
					classifier2Save = ((IFixableClassifier)classifier).getFixedClassifier();
					System.out.println("Trained fixed classifier: " + classifier2Save);
				} else {
					System.err.println("Trained classifier is not fixable. Model will not be stored."); 
					classifier2Save = null;
				}
			}
			if (classifier2Save != null) {
				try {
					FixedClassifiers.write(outputFileName, classifier2Save);
					System.out.println("Wrote trained " + (storeFixed ? "fixed " : "") + "classifier to file " + outputFileName);
				} catch (IOException e) {
					System.err.println("Could not write classifer to file " + outputFileName + ": " + e.getMessage());
					return false;
				}
			}
			
		}
		
		return true;

	}
}
