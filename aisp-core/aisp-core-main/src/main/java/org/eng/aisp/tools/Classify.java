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
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLogger;
import org.eng.aisp.AISPRuntime;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.Classification.LabelValue;
import org.eng.aisp.classifier.ConfusionMatrix;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.client.IAsyncSensorClient;
import org.eng.aisp.client.IAsyncSensorClient.ICommandListener;
import org.eng.aisp.dataset.MetaData;
import org.eng.aisp.monitor.QueuedSoundCapture;
import org.eng.aisp.util.PCMUtil;
import org.eng.util.CommandArgs;
import org.eng.util.IShuffleIterable;
import org.eng.util.OnlineStats;

import com.google.gson.Gson;

public class Classify implements ICommandListener {

	/** General options */
	IFixedClassifier<double[]> classifier = null;
	
	/** Classify and exit options */
	private boolean showTiming;
	
	/** Server mode options */
	private boolean asServer;
//	private int serverPort;

	/** Monitor mode options */
	private boolean asMonitor;
	private boolean verbose;
	private boolean quiet;
	public final static int DEFAULT_MONITOR_CLIP_MSEC = 5000;
	public final static int DEFAULT_MONITOR_PAUSE_MSEC = 0;

	private static final String DEFAULT_EVENT_NAME = "sensorStatus";

	private int clipLenMsec;
	private int pauseMsec;
	
	public static Gson gson = new Gson();
	public final static String Usage = 
			  "Classifies sounds using a local model or one on a server.\n"
			+ "Three different operating modes are available as follows:\n"
			+ "  1) Classify 1 or more sounds and exit.\n"
			+ "  2) Start a server that will listen for http requests containing a wav \n"
			+ "      file to classify.\n"
			+ "  3) Start a monitor process that will capture sounds and classify them. \n" 
			+ "In all 3 cases, classification results may be sent to an IOT platform.\n"
			+ "Mode options:\n"
			+ "  -classify : classify 1 or more files and exit.  The default mode.\n"
			+ "  -monitor : Starts a monitor that captures sounds to be classified. (WIP)\n" 
			+ "  -server  : Causes a server to be created that listens for http classify requests.\n" 
			+ "Model/Classifier options:\n"
			+ "  A classifier must be specified using the following:\n" 
			+ "  -quiet : limit the output to just the label and confidence for the result\n"
			+ "      This is the default.\n"
			+ "  -compare: when labels are present (from a metadata file) on the sounds being\n"
			+ "      classified, include a comparison of the predicted and actual classifications\n" 
			+ "      in the output. Only available in -quiet mode.\n" 
			+ "  -ranked: show all ranked results (if available)\n" 
			+ "  -csv: output a CSV format with file name, clip index, start offset, stop offset,\n"
			+ "      label name=value, confidence,....  Offsets are in msec.\n" 
			+ "  -audacity: output 3 column tab-separated values expected by Audacity.\n" 
			+ "      Start offset, stop offset, label value.  Offsets are in seconds.\n"
			+ "  -cm : output a confusion matrix.  Sounds must be labeled.\n" 
			+ "Classify mode options\n"
			+ GetModifiedSoundOptions.ClipLenOnlyOptionsHelp
			+ "  -t : Show classification time. Times are not effected by console output.\n" 
			+ "Server mode options:\n"
			+ "  -server-port port : Specifies the port for the server to run on.  Default is 80.\n" 
			+ "Monitor mode options:\n"
			+ "  -clipLen msec : Specifies the length of the clip to capture and classify. Default is 5000.\n" 
			+ "  -pauseLen msec : Specifies the time length between captured clips. Default is 0.\n" 
			+ "IOT options (if not specified, then events will not be sent to an IOT Platform):\n"
			+ "  -event name : name of event to publish classifications under. Default is " + DEFAULT_EVENT_NAME + ".\n"
			+ "Classify examples: \n"
			+ "  ... -file myclassifier.cfr number1.wav number2.wav\n"
			+ "  ... -file myclassifier.cfr -sounds yourmetadata.csv -compare\n"
			+ "Monitor examples: \n"
			+ "  ... -monitor -file myclassifier.cfr\n"
			+ "  ... -monitor -file myclassifier.cfr -iotp-properties iotp.props wavfile1\n"
			+ "Server examples: \n"
			+ "  ... -server -file myclassifier.cfr \n"
		    + "Server request: curl --data-binary '@your.wav' -H 'Content-Type:audio/wav' \\\n"
			+ "                      http://localhost/classifyWAV\n"
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

		Classify c = new Classify();
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
	
		// Parse the options that apply to most operating modes.
		if (!parseOurOptions(cmdargs))
			return false;
		
		// Load the named classifier or setup for remote classification
		GetTrainedModelOptions modelOptions = new GetTrainedModelOptions();
		if (!modelOptions.parseOptions(cmdargs)) 
			return false;
		this.classifier = modelOptions.getClassifier();
		if (this.classifier== null)
			return false;



		// Perform the requested operation.
		if (asServer) {
			int serverPort = cmdargs.getOption("server-port", 80);
			// Done parsing options, make sure there are none we don't recognize.
			if (ToolUtils.hasUnusedArguments(cmdargs))
				return false;
			try {
				return runServer(serverPort);
			} catch (Exception e) {
				System.err.println("Could not start server: " + e.getMessage());
				return false; 
			}
		} else if (asMonitor) {
			if (!getMonitoringOptions(cmdargs))
				return false;
			// Done parsing options, make sure there are none we don't recognize.
			if (ToolUtils.hasUnusedArguments(cmdargs))
				return false;
			try {
				return runMonitor();
			} catch (Exception e) {
				System.err.println("Could not start monitor: " + e.getMessage());
				e.printStackTrace();
				return false;
			}
		} else {
			// Load and classify each file 
			return classifySoundFiles(cmdargs);
		}

	}

	/**
	 * Get and verify options needed for running as a sound monitor logging events to IOTP.
	 * @param cmdargs
	 * @return false and issue an error message on error, otherwise true.
	 */
	private boolean getMonitoringOptions(CommandArgs cmdargs) {
		clipLenMsec = cmdargs.getOption("clipLen", DEFAULT_MONITOR_CLIP_MSEC);
		pauseMsec = cmdargs.getOption("pauseLen", DEFAULT_MONITOR_PAUSE_MSEC);
		return true;

	}

	/**
	 * Go into continuous sound capture/classification mode sending classifications to the IOT platform.
	 * @return
	 * @throws Exception
	 */
	private boolean runMonitor() throws Exception {
		QueuedSoundCapture monitor = new QueuedSoundCapture(clipLenMsec, pauseMsec);

		monitor.start();
		SoundClip clip;
		int maxWaitMsec = 10 * (clipLenMsec + pauseMsec);
//		int count=0;
//		isMonitorRunning = true;
		
		isStopRequested = false;

		int count = 0;
		while ((clip = monitor.next(maxWaitMsec)) != null) {
//			PCMUtil.PCMtoWAV("classify"+count+".wav", clip);
//			System.out.println("Wrote wav file #" + count);
//			count++;
			Map<String, Classification> c = this.classifyClip(clip);
			System.out.println("Classifications: " + c.values());
			if (isStopRequested) {
				synchronized(this) {
//					isMonitorRunning = false;
					isStopRequested = false;
					System.out.println("Monitoring paused.  Waiting for start command.");
					this.wait();
					System.out.println("Monitoring restarted.");
//					isMonitorRunning = true;
				}
			}
		}
		return true;
	}
	
	private boolean isStopRequested = false;
//	private boolean isMonitorRunning = false;

	private String eventName;
	private boolean outputAsDefault;
	private boolean outputAsCSV;
	private boolean outputAsAudacity;
	private boolean ranked;
	private boolean compare;
	private boolean outputConfusionMatrix;
	
	@Override
	public void handleCommand(String command, byte[] payload) {
		command = command.toLowerCase();
		if (command.equalsIgnoreCase(IAsyncSensorClient.SensorCommand.StopMonitoring.name())) {
			synchronized(this) {
				isStopRequested = true;
			}
		} else if (command.equalsIgnoreCase(IAsyncSensorClient.SensorCommand.StartMonitoring.name())) {
			synchronized(this) {
				isStopRequested = false;
				this.notifyAll();	// Wake up the wait() call. 
			}
	 	} else {
	 		System.err.println("Unexpected command '" + command + "' received.  Ignoring.");
	 	}
			
	}
		
	
	
	/**
	 * @param cmdargs
	 */
	private boolean parseOurOptions(CommandArgs cmdargs) {
		// Get the name of the model to use.
//		modelName = cmdargs.getOption("name");
//		fileName = cmdargs.getOption("file");
		asServer = cmdargs.getFlag("server");
		asMonitor = cmdargs.getFlag("monitor");
		verbose = cmdargs.getFlag("verbose");
		ranked= cmdargs.getFlag("ranked");
		outputConfusionMatrix = cmdargs.getFlag("cm");
		outputAsCSV = cmdargs.getFlag("csv");
		outputAsAudacity = cmdargs.getFlag("audacity");
		quiet = cmdargs.getFlag("quiet"); 
		eventName = cmdargs.getOption("event", DEFAULT_EVENT_NAME);
		compare = cmdargs.getFlag("compare");
	
		int count = (outputConfusionMatrix ? 1 : 0) +  (outputAsCSV ? 1 : 0) +  (outputAsAudacity ? 1 : 0) ;
		if (count > 1)  {
			System.err.println("Only one of -cm, -csv, and -audacity may be specified.");
			return false;
		}
		this.outputAsDefault = !(outputAsCSV || outputAsAudacity || outputConfusionMatrix);
		if (!outputAsDefault && (quiet || verbose || ranked || compare)) {
			System.err.println("CSV, ConfusionMatrix and Audacity output each precludes quiet, verbose, ranked and compare modes.");
			return false;
		} 

		quiet = quiet || (!cmdargs.getFlag("raw") && !ranked); 
		if (quiet && verbose) {
			System.err.println("Cannot be both verbose and quiet.");
			return false;
		}
		if (quiet && ranked) {
			System.err.println("Cannot be both quiet and ranked.");
			return false;
		}
		if (compare && !quiet) {
			System.err.println("Cannot specify compare in ranked or raw modes. Ignoring compare option."); 
			compare = false;
		}

		showTiming = cmdargs.getFlag("t");
//		if (fileName != null) {
//			File f = new File(fileName);
//			if (!f.exists()) {
//				System.err.println("File " + fileName + " was not found.");
//				return false;
//			}
//		} else if (modelName == null) {
//			System.err.println("Classifier name must be specified with -name or -file option");
//			return false;
//		}
		
		return true;

	}

//	private boolean setUpClassifier(CommandArgs cmdargs) throws AISPException, IOException {
//		GetModelOptions modelOptions = new GetModelOptions();
//		if (!modelOptions.parseOptions(cmdargs)) 
//			return false;
//		this.classifier = modelOptions.getClassifier();
//		return classifier != null;
//	}

//	/**
//	 * Set {@link #classifier}. 
//	 * @return true on success, otherwise false and issue a message on stderr.
//	 * @throws AISPException
//	 * @throws IOException 
//	 */
//	private boolean setUpClassifier() throws AISPException, IOException {
//		modelID = null;
//		if (fileName != null) {
//			// Classifier is being loaded from a file.
//			try {
//				classifier = (IFixedClassifier<double[]>) FixedClassifiers.read(fileName);
//			} catch (Exception e) {
//				System.err.println("Could not load classifier from file " + fileName + ": " + e.getMessage());
//				return false;
//			}
//		} else {
//			if (sensorClient == null) {
//				System.err.println("Sound server host or model file must be specified");
//				return false;
//
////				// File was not specified, so try and read the model out of the local database.
////				@SuppressWarnings("rawtypes")
////				Iterator<IClassifier> namedClassifiers = Storage.modelStorage().findNamedItems(modelName).iterator();
////				if (!namedClassifiers.hasNext()) {
////					System.err.println("Classifier with name " + modelName + " not found.");
////					return true;
////				}
////				classifier = (IFixedClassifier<double[]>) namedClassifiers.next();
////				if (namedClassifiers.hasNext()) {
////					System.err.println("More than one classifier with name " + modelName + ".");
////					return true;
////				}
//			} else {
//				// Get a reference to the model in the server.
//				modelID = sensorClient.getModelID(DataTypeEnum.Audio, modelName, null, true);	// TODO: allow other model types.
//				if (modelID == null) {
//					System.err.println("Model with name " + modelName + " does not exist.");
//					return false;
//				}
//			}
//
//		}
//		if (classifier != null && !quiet) {	// Classifier pulled in locally
//			if (verbose)
//				System.out.println("Loaded classifier " + classifier);
//			else
//				System.out.println("Loaded classifier of type " + classifier.getClass().getSimpleName());
//		} else if (verbose) {	 // && modelID != null;
//			assert modelID != null;
//			ModelStorageClient msc = remoteServerOptions.getModelStorageClient();
//			classifier = msc.findItem(modelID);
//			if (classifier == null) {
//				System.err.println("Could not find model on server with id " + modelID);
//				return false;
//			}
//			System.out.println("Loaded classifier " + classifier);
//		}
//		return true;
//	}
//	
	



	private static Classify instance;
	
	private boolean runServer(int port) throws Exception {
		instance = this;

        // Create a basic jetty server object that will listen on the given port. 
        // Note that if you set this to port 0 then a randomly available port
        // will be assigned that you can either look in the logs for the port,
        // or programmatically obtain it for use in test cases.
        Server server = new Server(port);

        // The ServletHandler is a dead simple way to create a context handler
        // that is backed by an instance of a Servlet.
        // This handler then needs to be registered with the Server object.
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        // Passing in the class for the Servlet allows jetty to instantiate an
        // instance of that Servlet and mount it on a given context path.

        // IMPORTANT:
        // This is a raw Servlet, not a Servlet that has been configured
        // through a web.xml @WebServlet annotation, or anything similar.
        handler.addServletWithMapping(ClassifyServlet.class, "/classifyWAV");

        // Start things up!
        server.start();

        // The use of server.join() the will make the current thread join and
        // wait until the server is done executing.
        // See http://docs.oracle.com/javase/7/docs/api/java/lang/Thread.html#join()
        System.out.println("Classify server started on port " + port + ".");
        ServletMapping[] mappings = handler.getServletMappings();
        for (ServletMapping sm : mappings) {
        	System.out.println(sm);
        }
        server.join();
        
        return true;
	}

    @SuppressWarnings("serial")
    public static class ClassifyServlet extends HttpServlet
    {
        @Override
        protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("<h1>Hello from Servlet</h1>");
        }

        @Override
        protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        	String contentType = request.getContentType();
        	if (contentType == null || !contentType.equalsIgnoreCase("audio/wav")) {
        		response.setContentType("text/html");
        		response.getWriter().println("Content type must be 'audio/wav'");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
        	}
        		
        	InputStream is = request.getInputStream();
			SoundClip clip; 
        	try {
				clip = PCMUtil.WAVtoPCM(is);
        	} catch (IOException e) {
        		response.setContentType("text/html");
        		response.getWriter().println("Could not read/parse input wav stream: " + e.getMessage());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
        	}
        		
        	try {
				Map<String,Classification> classifications = instance.classifyClip(clip);
				response.setContentType("application/json");
				String url = request.getRequestURI().toString();
				response.setStatus(HttpServletResponse.SC_OK);
				String json = gson.toJson(classifications);
				AISPLogger.logger.info("Responding with json=" + json);
				response.getWriter().println(json);
//			} catch (NoClassifierException e) {
//        		response.setContentType("text/html");
//        		response.getWriter().println("Classifier does not exist: " + e.getMessage());
//				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			} catch (AISPException e) {
        		response.setContentType("text/html");
        		response.getWriter().println("Could classify audio: " + e.getMessage());
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
        }
    }
    
	private boolean classifySoundFiles(CommandArgs cmdargs) throws AISPException, IOException {
		// Point stdout at stderr so that messages produced during argument parsing do not appear in 
		// redirected command lines (ala classify > file.out).
		PrintStream stdout = System.out;
		System.setOut(System.err);
		OnlineStats stats = new OnlineStats();
		GetModifiedSoundOptions soundOptions = new GetModifiedSoundOptions(false, false, false);	// No -label, -balance, no -clipShift
		if (!soundOptions.parseOptions(cmdargs)) 
			return false;
		IShuffleIterable<SoundRecording> sounds = soundOptions.getSounds();
		System.setOut(stdout);

		// See if there are duplicate filenames so that we know whether or not to include indices and start/stop times.
		boolean hasDupFilenames = checkForDuplicateFileNames(sounds);

		String trainedLabel = this.classifier.getTrainedLabel();
		List<String> actualLabelValues = new ArrayList<String>();
		List<String> predictedLabelValues = new ArrayList<String>();
		String lastFileName = null;
		int subClipIndex = 0;
		for (SoundRecording sr : sounds) {
			Properties tags = sr.getTagsAsProperties();
			String fileName = tags.getProperty(MetaData.FILENAME_TAG);
			if (fileName == null)
				fileName = "(unknown location).wav";
			if (!fileName.equals(lastFileName)) 
				subClipIndex = 0;
			
			// Classify the sample and keep track of the amount of time. 
			SoundClip clip = sr.getDataWindow(); 
			long startMsec = System.currentTimeMillis();
			Map<String,Classification> classifications = classifyClip(clip);	// Do the work.
			long endMsec = System.currentTimeMillis();
			long classifyMsec = (endMsec - startMsec);
			double classifiedClipMsec = clip.getDurationMsec();
			double rate = classifyMsec / classifiedClipMsec; 
			stats.addSample(rate);
			
			// Show the result.
			showClassificationResult(fileName, sr, classifications, hasDupFilenames ? subClipIndex : -1);
			
			// Keep track of actual and predicted for the ConfusionMatrix
			if (outputConfusionMatrix)
				updateActualAndPredictedLabels(actualLabelValues, predictedLabelValues, trainedLabel, sr, classifications);

			subClipIndex++;
			lastFileName = fileName;
		}

		if (outputConfusionMatrix) {
			if (actualLabelValues.isEmpty()) {
				System.err.println("Confusion matrix was requested, but it appears the given sounds are not labeled with " + trainedLabel);
			} else {
				ConfusionMatrix matrix = new ConfusionMatrix(trainedLabel, actualLabelValues, predictedLabelValues); 
				System.out.println("COUNT MATRIX");
				System.out.println(matrix.formatCounts());
				System.out.println("PERCENT MATRIX");
				System.out.println(matrix.formatPercents());
				System.out.println(matrix.formatStats());
			}
		}
		if (showTiming) 
			System.out.println("\nClassification performance: "+ 100*stats.getMean() + " +/- " + 100*stats.getStdDev() + " (% of clip length)");
		return true;
	}


	/**
	 * @param sounds
	 * @return
	 */
	private boolean checkForDuplicateFileNames(IShuffleIterable<SoundRecording> sounds) {
		boolean hasDupFilenames = false;
		Set<String> fileNames = new HashSet<String>();
		for (SoundRecording sr : sounds) {
			Properties tags = sr.getTagsAsProperties();
			String fileName = tags.getProperty(MetaData.FILENAME_TAG);
			if (fileName == null)
				continue;
			if (fileNames.contains(fileName)) {
				hasDupFilenames = true;
				break;
			}
			fileNames.add(fileName);
		}
		return hasDupFilenames;
	}


	/**
	 * @param actualLabelValues
	 * @param predictedLabelValues
	 * @param trainedLabel
	 * @param sr
	 * @param classifications
	 */
	private static void updateActualAndPredictedLabels(List<String> actualLabelValues, List<String> predictedLabelValues,
			String trainedLabel, SoundRecording sr, Map<String, Classification> classifications) {
		String actualLabelValue = getActualLabelValue(sr, trainedLabel); 
		if (actualLabelValue != null) {
			String predictedValue = getPredictedValue(classifications, trainedLabel);
			if (predictedValue == null) {	// Should never be the case, but just in case.
				System.err.println("Classifier is trained on label " + trainedLabel 
						+ " but did not produce a classification for that label");
			} else {
				actualLabelValues.add(actualLabelValue);
				predictedLabelValues.add(predictedValue);
			}
		}
	}

	private static String getPredictedValue(Map<String, Classification> classifications, String trainedLabel) {
		Classification c = classifications.get(trainedLabel);
		if (c == null)
			return null;
		return c.getLabelValue();
	}


	private static String getActualLabelValue(SoundRecording sr, String trainedLabel) {
		Properties labels = sr.getLabels();
		String value = labels.getProperty(trainedLabel);
		return value;
	}

	//	private boolean warnedOnCompareOfUnlabeled = false;
	/**
	 * @param fileName
	 * @param classifications
	 * @param subClipCount
	 */
	private void showClassificationResult(String fileName, SoundRecording sr, Map<String, Classification> classifications, int subClipIndex)  {

		if (outputAsCSV) {
			showClassificationResultAsCSV(fileName, sr, classifications, subClipIndex);
		} else if (outputAsAudacity) {
			showClassificationResultAsAudacity(fileName, sr, classifications, subClipIndex);
		} else if (outputAsDefault) {
			showClassificationResultDefault(fileName, sr, classifications, subClipIndex);
		}
	}


	
	boolean isFirstCSVLine  = true;
	private void showClassificationResultAsCSV(String fileName, SoundRecording sr, Map<String, Classification> classifications, int subClipIndex) {

		List<String> labelNames = new ArrayList<String>();
		labelNames.addAll(classifications.keySet());
		Collections.sort(labelNames);

		if (isFirstCSVLine) {
			System.out.print("File, Segment Index, Segment Start Offset(msec), Segment End Offset(msec)");
			for (int i=0 ; i<labelNames.size(); i++)
				System.out.print(", Label Name, Label Value, Label Confidence");
			System.out.println("");
			isFirstCSVLine = false;
		}

		SoundClip clip = sr.getDataWindow();
		System.out.print(String.format("%s, %s, %d, %d",
				fileName, 
				subClipIndex,
				(int)clip.getStartTimeMsec(),
				(int)clip.getEndTimeMsec()
				)
			);

		for (String labelName : labelNames) {
			Classification c = classifications.get(labelName);
			System.out.print(String.format(", %s, %s, %f", c.getLabelName(), c.getLabelValue(), c.getConfidence())); 
		}
		System.out.println("");
				
	}

	private void showClassificationResultAsAudacity(String fileName, SoundRecording sr, Map<String, Classification> classifications, int subClipIndex) {

		SoundClip clip = sr.getDataWindow();
		System.out.print(String.format("%11.5f\t%11.5f",
				(double)clip.getStartTimeMsec()/1000.0,
				(double)clip.getEndTimeMsec()/1000.0
				)
			);
		// Put out all label values even though audicity seems to only parse the first one and ignore anything after it.
		List<String> labelNames = new ArrayList<String>();
		labelNames.addAll(classifications.keySet());
		Collections.sort(labelNames);
		for (String labelName : labelNames) {
			Classification c = classifications.get(labelName);
			System.out.print(String.format("\t%s", c.getLabelValue()));
		}
		System.out.println("");
				
	}

	/**
	 * @param fileName
	 * @param sr
	 * @param classifications
	 * @param subClipIndex
	 */
	private void showClassificationResultDefault(String fileName, SoundRecording sr,
			Map<String, Classification> classifications, int subClipIndex) {
		System.out.print(fileName);

		if (subClipIndex >= 0)  {
			System.out.print("[" + subClipIndex + "]");
			SoundClip clip = sr.getDataWindow();
			System.out.print(String.format("[%d-%d msec]", (int)clip.getStartTimeMsec(), (int)clip.getEndTimeMsec()));
		}
		System.out.print(":");
		Properties labels = sr.getLabels();
		

		if (ranked || quiet){
			//For every subclip and every classification, get the label name
			if (ranked)
				System.out.println("");
			int count = 0;
			for (String key: classifications.keySet()){
				String label = classifications.get(key).getLabelName();
				//print out its value
				Classification c = classifications.get(label);
				if (ranked) {
					for (LabelValue labelValue : c.getRankedValues()){
						System.out.println(String.format(" %s=%s confidence=%5.4f",label,labelValue.getLabelValue(), labelValue.getConfidence()));
					}
				} else {	// quiet
	//					System.out.print(label + "=" + c.getLabelValue() + ", confidence=" + c.getConfidence());
					if (count != 0)
						System.out.print(", ");
					String predictedLabelValue = c.getLabelValue();
					System.out.print(String.format(" %s=%s" ,label,predictedLabelValue));
					if (compare) { 
						String expectedLabelValue = labels.getProperty(label);
						if (expectedLabelValue != null) {	// Sound being classified has a label that the classifier should have matched.
							if (expectedLabelValue.equals(predictedLabelValue))
								System.out.print(String.format("(==%s)",expectedLabelValue));
							else
								System.out.print(String.format("(!=%s)",expectedLabelValue));
	//						} else if (!warnedOnCompareOfUnlabeled) {
	//							System.err.print("Labels are not present on given sound(s) for comparison with predicted value. Ignoring any/all comparisons");
	//							warnedOnCompareOfUnlabeled = true;
						}
					}
					System.out.print(String.format(" confidence=%5.4f",c.getConfidence()));
					count++;
				}
			}
			if (!ranked)
				System.out.println("");
		} else {
			System.out.println(classifications.values());
		}
	}

	/**
	 * Classify the sound 
	 * @param clip
	 * @return never null.
	 * @throws AISPException
	 * @throws IOException 
	 */
	private Map<String,Classification> classifyClip(SoundClip clip) throws AISPException, IOException {
		Map<String,Classification> classifications;
		classifications = classifier.classify(clip);
		return classifications;
	}

}
