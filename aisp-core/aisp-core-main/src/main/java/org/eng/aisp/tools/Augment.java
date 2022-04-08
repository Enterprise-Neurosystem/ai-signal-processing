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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.eng.aisp.AISPException;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.LabeledDataWindow;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.dataset.MetaData;
import org.eng.aisp.transform.ITrainingWindowTransform;
import org.eng.aisp.transform.MixingWindowTransform;
import org.eng.aisp.util.FrequencyUtils;
import org.eng.aisp.util.PCMUtil;
import org.eng.util.CommandArgs;

public class Augment {
	final static double CLIP_LENGTH = 5;  //clip length in seconds


	public static String Usage = 
//            "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
			  "Change 1 or more wave files using 1 or more of the available augmentations.\n"
			+ "Augmentations are done independently and are not chained in sequence.\n"
			+ "Augmented wav files are renamed to include a transform indicator. Ann index\n" 
			+ "is included in the file name if the augmentation produced more than a single\n"
			+ "result.\n"
			+ "Usage : ... [-o dir ] [-f list ] [-log r] [-m file ] <wav files> \n" 
			+ "Options:\n"
			+ "  -m <file> : sets the location of a metadata file defining labels. Causes\n"
			+ "              a new metadata.csv file to be created in the output directory.\n" 
			+ "  -o <dir>  : sets the directory to store wav files.\n"
			+ "              Required when metadata.csv is present in the source directory.\n"
			+ "              If not specified, then the source directory is used.\n"
			+ "  -n volume : specifies a white noise volume to be mixed into the sound. \n"
			+ "              Value is between 0 and 1.\n" 
			+ "  -f <list> : specifies a list of 1 or more frequencies by which to shift \n"
			+ "  -mix <list> : specifies a comma-separated list of wav file, directories of\n" 
			+ "              wav files and weights, in any order.  Each wav file is mixed\n"
			+ "              into the source files with the given weights such that the \n" 
			+ "              weight of the source is 1-weight of the mixed in sound.\n"
			+ "  -mix-raw  : a flag used in conjunction with the -mix option. If specified,\n"
			+ "              causes each mixed sound to NOT be scaled to match the range of\n"
			+ "              values in the source sound.  Without this flag, the mixes sounds\n"
			+ "              will be at the same overall volume as the source sound.\n" 
			+ "  -log <r>  : take the log of the signal values for which the absolute value\n"
			+ "              fainter sounds relative to the louder ones.  All signal values\n"
			+ "              is greater or equal to r.  Smaller values of r accentuate the\n"
			+ "              are in the range [-1,1].\n" 
			+ "Examples: \n"
			+ "   ... -log 0.01 one.wav two.wav\n"
			+ "   ... -m metadata.csv -log 0.001 one.wav two.wav \n"
			+ "   ... -o mydir -m metadata.csv -f -10,0,10 one.wav \n"
			+ "   ... -o mydir -m metadata.csv -mix mixin1.wav,mixindir,0.1,0.5 one.wav \n"
			;

	
	private interface ITransformer {
		List<SoundClip> transform(SoundClip clip);

		String getName();
	}

	
	private static class LogTransform implements ITransformer {

		private final double minThreshold;
		private final double logMinThreshold;

		public LogTransform(double minThreshold) {
			this.minThreshold = minThreshold;
			this.logMinThreshold = Math.abs(Math.log10(minThreshold));
		}

		@Override
		public List<SoundClip> transform(SoundClip clip) {
			double[] data = clip.getData();
			double[] newData = new double[data.length]; 
			double max = 0;
			for (int i=0 ; i<data.length ; i++) {
				double d = data[i];
				double absd = Math.abs(d);
				if (absd > max)
					max = absd;
				if (absd >= minThreshold) {
//					System.out.print("LOG: d=" + d);
					boolean wasNegative = d < 0;
					// -1 < d <= 1.  d != 0
					d = -Math.log10(absd);	// d now > 0  
					if (d > logMinThreshold)
						d = logMinThreshold;
					d = logMinThreshold - d;	// Invert so originally largest values are set to 8.
					d /= logMinThreshold;		// scale back into the range 0..1
					if (wasNegative)
						d = -d;
//					System.out.println("newd=" + d);
				}
				newData[i] = d;
			}
			// Scale back to something like the original range.
			if (max != 1.0) {
				for (int i=0 ; i<newData.length ; i++) 
					newData[i] *= max; 
			}

			byte[] newPCM = PCMUtil.double2PCM(newData, clip.getChannels(), clip.getBitsPerSample());
			// public SoundClip(double startTimeMsec, int channels, int bitsPerSample, double samplesPerSecond, byte pcmData[], int interleavedDataDimensions) {
			clip = new SoundClip(clip.getStartTimeMsec(), 
					clip.getChannels(), 
					clip.getBitsPerSample(),
					clip.getSamplingRate(), 
					newPCM, 
					clip.getInterleavedDataDimensions());
			List<SoundClip> clist = new ArrayList<SoundClip>();
			clist.add(clip);
			return clist;
		}

		@Override
		public String getName() {
			return "log";
		}

	}

	private static class FrequencyShifter implements ITransformer {

		private double[] freqShiftsHtz;

		public FrequencyShifter(double[] freqShiftsHtz) {
			this.freqShiftsHtz = freqShiftsHtz;
		}

		@Override
		public List<SoundClip> transform(SoundClip clip) {
			List<SoundClip> r = FrequencyUtils.shiftFrequency(clip, freqShiftsHtz);
			return r;
		}
		
		public String getName() {
			return "freq-shift";
		}
		
	}
	
	private static class AbstractTrainingTransformer implements ITransformer {
		
		ITrainingWindowTransform<double[]> trainingWindowTransform;
		Properties labels = new Properties();
		private String name;
		
		public AbstractTrainingTransformer(String name, ITrainingWindowTransform<double[]> transform) {
			this.name = name;
			this.trainingWindowTransform = transform;
			
		}

		@Override
		public List<SoundClip> transform(SoundClip clip) {
			LabeledDataWindow<double[]> tmpLDW = new LabeledDataWindow<double[]>(clip, labels);
			Iterable<ILabeledDataWindow<double[]>> mixedLDW= trainingWindowTransform.apply(null, tmpLDW);
			List<SoundClip> clipList = new ArrayList<SoundClip>();
			for (ILabeledDataWindow<double[]> ldw : mixedLDW) {
				IDataWindow<double[]> dw = ldw.getDataWindow();
				clip = new SoundClip(dw.getStartTimeMsec(), dw.getEndTimeMsec(), dw.getData());
				clipList.add(clip);
			}
			return clipList;
		}

		@Override
		public String getName() {
			return this.name;
		}
		
	}
	
	private static class Mixer extends AbstractTrainingTransformer implements ITransformer {

		public Mixer(List<String> filesOrDirs, double[] ratios, boolean normalize) throws IOException {
			super("mixer", new MixingWindowTransform(filesOrDirs, ratios, normalize));
		}
		
	}
	
	private static class WhiteNoiseTransform implements ITransformer {

		private double volume;
		private Random rand;

		public WhiteNoiseTransform(double volume, int seed) {
			this.volume = volume;
			this.rand = new Random(seed);
		}

		@Override
		public List<SoundClip> transform(SoundClip clip) {
			double[] data = clip.getData();
			double[] newData = new double[data.length]; 
			for (int i=0 ; i<data.length ; i++)  {
				double noise = (volume * rand.nextDouble() * 2) - 1;
				double newValue = data[i] + noise;
				if (newValue > 1)
					newValue = 1;
				else if (newValue < -1)
					newValue = -1;
				newData[i] = newValue;
			}
			List<SoundClip> clips = new ArrayList<SoundClip>();
			clip = new SoundClip(clip.getStartTimeMsec(), clip.getEndTimeMsec(), newData, clip.getInterleavedDataDimensions());
			clips.add(clip);
			return clips;
		}
		
		public String getName() {
			return "noise";
		}
		
	}

	public static void main(String[] args) throws IOException, AISPException {
		CommandArgs cmdargs = new CommandArgs(args);
		if (cmdargs.getFlag("-h") || cmdargs.getFlag("-help")) {
			System.out.println(Usage);
			return;
		}

		List<ITransformer> transforms = new ArrayList<ITransformer>();
		String freqList = cmdargs.getOption("f");
		double[] frequencyShifts = null;
		if (freqList != null) {
			String[] fl = freqList.split(",");
			frequencyShifts = new double[fl.length];
			for (int i=0 ; i<fl.length ; i++) {
				String freq = fl[i]; 
				frequencyShifts[i] = Double.parseDouble(freq);
			}
			ITransformer t = new FrequencyShifter(frequencyShifts);
			transforms.add(t);
		}

		String volume = cmdargs.getOption("n");
		if (volume != null) {
			double vol = Double.valueOf(volume); 
			ITransformer t = new WhiteNoiseTransform(vol, 3290234);
			transforms.add(t);
		}

		if (cmdargs.hasArgument("log")) {
			String logArg = cmdargs.getOption("log");
			double logThreshold;  
			try {
				logThreshold = Double.parseDouble(logArg);
				if (logThreshold <= 0) {
					System.err.println("Log threshold must be larger than 0");
					return;
				}
			} catch (Exception e) {
				System.err.println("Could not parse log argument '" + logArg + "' as a double value.");
				return;
			}
			if (logThreshold > 0) {
				ITransformer t = new LogTransform(logThreshold);
				transforms.add(t);
			}
		}
		
		if (cmdargs.hasArgument("mix")) {
			ITransformer t = parserMixerTranform(cmdargs);
			if (t == null)	// error message was issued
				return;
			transforms.add(t);
		}
		
		if (transforms.size() == 0) {
			System.err.println("No transforms defined");
			System.out.println("Use -help for usage");
			return;
		}
		
		

		List<String> srcFiles = new ArrayList<String>();

		String metaData = cmdargs.getOption("m"); 
		File metaDataFile = null;
		if (metaData != null) {
			metaDataFile = new File(metaData);
			if (!metaDataFile.exists()) {
				System.err.println("Meta data file " + metaData + " does not exists.");
				return;
			}
		}

		String destDir = cmdargs.getOption("o"); 
		if (destDir != null) {
			File destFile = new File(destDir);
			if (!destFile.exists()) 
				destFile.mkdirs();
		} else {
			destDir = ".";
		}

		for (String arg : cmdargs.getArgs()) {
			if (arg.startsWith("-")) {
				System.err.println("Unrecoginized option/flag: " + arg + ". Use -h option for help");
				return;
			}
			srcFiles.add(arg);
		}
		if (srcFiles.size() == 0) {
			System.err.println("No sound files specified. Use -h option for help.");
			return;
		}
		
		
//		System.out.println("Writing split " + clipLengthSec + " second PCM wav files to directory " + destDir);	
		if (metaDataFile == null) {
			for (String f : srcFiles)  {
				SoundClip clip = SoundClip.readClip(f);
				saveAugementedClips(clip, f, destDir, null, null, transforms);
			}
		} else {
			System.out.println("Writing metadata file in " + destDir); 
			File destDirFile = new File(destDir);
//			String metaDataDirName = metaDataFile.getParentFile().getAbsolutePath();
			String metaDataDirName = metaDataFile.getAbsoluteFile().getParentFile().getAbsolutePath();
			String destPath = destDirFile.getAbsolutePath();
			String trailingDot = File.separator + ".";
			if (destPath.endsWith(trailingDot)  && !metaDataDirName.endsWith(trailingDot))
				metaDataDirName += trailingDot;	// Try and make the equal if they are.
			if (destPath.equals(metaDataDirName)) {
				System.err.println("Must specify output directory when using metadata.csv in the current directory.");
				System.err.println("Use the -help option to see usage");
				return;
			}
			augmentMetaDataSounds(srcFiles, metaData, destDir, transforms);
		}

	}

	/**
	 * @param cmdargs
	 * @return
	 * @throws IOException
	 */
	protected static ITransformer parserMixerTranform(CommandArgs cmdargs) throws IOException {
		String mixArg= cmdargs.getOption("mix");	// csv of files,dirs,numbers in any and mixed order
		boolean mixUnleveled= cmdargs.getFlag("mix-raw");	// 
		String[] mixArgs = mixArg.split(",");
		List<String> filesOrDirs = new ArrayList<String>();
		double[] ratios = new double[100];
		int ratioCount = 0;
		for (String arg : mixArgs) {
			try {
				double r = Double.parseDouble(arg);
				if (ratioCount+1 == ratios.length) {
					System.err.println("Too many ratios provided for mixer transform.");
					return null;
				}
				ratios[ratioCount] = r;
				ratioCount++;
			} catch (Exception e) {
				File f = new File(arg);
				if (!f.exists()) {
					System.err.println("File/directory not found: " + f.getAbsolutePath() + " for mixer transform.");
					return null;
				}
				filesOrDirs.add(arg);
			}

		}
		if (filesOrDirs.size() == 0) {
			System.err.println("No files or directories of sounds specified for mixer transform");
			return null;
		}
		if (ratioCount == 0) {
			System.err.println("No ratios specified for mixer option for mixer transform."); 
			return null;
		}
		ratios = Arrays.copyOfRange(ratios, 0, ratioCount);
		ITransformer t = new Mixer(filesOrDirs, ratios, !mixUnleveled);
		return t;
	}

	private static void saveAugementedClips(SoundClip clip, String srcFileName, String destDir, Properties labels, MetaData destMetadata, List<ITransformer> transformers) throws IOException {
		for (ITransformer transform : transformers) {
			List<SoundClip> augmentedList = transform.transform(clip); 
			int i=0;
			String baseName = new File(srcFileName).getName();
			int lastDotIndex = baseName.lastIndexOf('.');
			if (lastDotIndex > 0)
				baseName = baseName.substring(0,lastDotIndex) + "_" + transform.getName() ;
			for(SoundClip newClip: augmentedList) {
				String newFile = baseName; 
				if (augmentedList.size() > 1)
					newFile = newFile + "_" + String.format("%03d",i);
				newFile = newFile + ".wav";
				FileOutputStream fos = new FileOutputStream(destDir + "/" + newFile);
				PCMUtil.PCMtoWAV(fos, (SoundClip) newClip);
				fos.close();
				if (labels != null && destMetadata != null) {
					SoundRecording sr = new SoundRecording(newClip, labels);
					destMetadata.add(newFile,sr);	// Use sr so we preserver startMsec
				}
				i++;
			}
		}
	}


	/**
	 * Split the given list of files into the requested length and place them in the given directory with a metadata file that contains labels as copied
	 * from the given metadata file, if they exist. 
	 * @param clipLengthSec
	 * @param files
	 * @param dataSet
	 * @param destDir
	 * @param gapSeconds
	 * @throws IOException
	 * @throws AISPException
	 */
	private static void augmentMetaDataSounds(List<String> files, String metaData, String destDir, List<ITransformer> transforms) throws IOException, AISPException {
		MetaData srcMetadata = MetaData.read(metaData);
		MetaData destMetadata = new MetaData(destDir);
		
		for (String wavFileName : files) {
			File wavFile =  new File(wavFileName);
			if (!wavFile.exists()) {
				System.out.println("Warning: File " + wavFile + " was not found. Skipping.");
				continue;
			}

			Properties labels = srcMetadata.getLabels(wavFileName);
			String baseName = getBaseName(wavFileName);
			SoundRecording sr;
			if (labels != null)  {
				sr = srcMetadata.readSound(wavFileName);	// Reads/preserves startMsec if present.
			} else {
				labels = srcMetadata.getLabels(baseName);
				sr = srcMetadata.readSound(baseName);	// Reads/preserves startMsec if present.
			}
			if (sr == null) {	// Could not read it using srcMetadata
				SoundClip clip = SoundClip.readClip(wavFileName);
				labels = new Properties();
				sr = new SoundRecording(clip, labels);
			}
			SoundClip clip = sr.getDataWindow(); 
			saveAugementedClips(clip, wavFileName, destDir, labels, destMetadata, transforms); 

//			List<SoundClip> augmentedList = FrequencyUtils.shiftFrequency(clip, frequencyShifts);
//			int i=0;
//			int lastDotIndex = baseName.lastIndexOf('.');
//			for(SoundClip newClip: augmentedList) {
//			    String newFile = baseName.substring(0,lastDotIndex) + "_" + String.format("%03d",i) + ".wav";
//				FileOutputStream fos = new FileOutputStream(destDir + "/" + newFile);
//				PCMUtil.PCMtoWAV(fos, (SoundClip) newClip);
//				sr = new SoundRecording(newClip, labels);
//				destMetadata.add(newFile,sensorName, sr);	// Use sr so we preserver startMsec
//				fos.close();
//				i++;
//			}

		}
		destMetadata.write(destDir + "/metadata.csv");
	}

	/**
	 * Get the name of the file w/o the path.
	 * @param file
	 * @return
	 */
	private static String getBaseName(String file) {
		int index = file.lastIndexOf("/");
		if (index < 0)
			index = file.lastIndexOf("\\");
		if (index < 0)
			return file;
		
		return file.substring(index+1, file.length());
	}

}
