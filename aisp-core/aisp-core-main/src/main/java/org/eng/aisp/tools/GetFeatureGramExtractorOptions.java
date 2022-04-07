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

import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.FFTFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.LogMelFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.MFCCFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.MFFBFeatureExtractor;
import org.eng.util.CommandArgs;

/**
 * Support parsing list of wav files or csv directories of sounds with or w/o a metadata file.
 * Sounds are always returned as an IShuffleIterable<SoundRecording>.
 * @author DavidWood
 *
 */
public class GetFeatureGramExtractorOptions {
	final static int Default_featureLen = MFCCFeatureExtractor.DEFAULT_NUM_BANDS;

//	public static final int DEFAULT_CLIPLEN_MSEC = 0;

	final static int Default_minHtz = MFCCFeatureExtractor.DEFAULT_MIN_FREQ;

	final static int Default_maxHtz = MFCCFeatureExtractor.DEFAULT_MAX_FREQ;

	final static int DEFAULT_SUBWINDOW_MSEC = 50; 
	
	public final static String OptionsHelp = 
			  "Spectrogram extractor options:"
			+ "  -fft 	: use FFT feature extractor.\n" 
			+ "  -logmel : use LogMel feature extractor.\n" 
			+ "  -mffb 	: use MFFB feature extractor.\n" 
			+ "  -mfcc 	: use MFCC feature extractor.  The default.\n" 
			+ "  -numBands <n>	: Set the number of bands used for the mfcc,mffb and \n"
			+ "     logmel feature extractors. The default is " + MFCCFeatureExtractor.DEFAULT_NUM_BANDS + ".\n" 
			+ "  -subWindowMsec <msec> : specifies the size of the sub-windows in msec\n"
			+ "     on which features are extracted and spectrograms created.\n"
			+ "     Default is " + DEFAULT_SUBWINDOW_MSEC + ".\n"
			+ "  -minHtz <htz> : the lowest frequency in hertz to consider.\n" 
			+ "     Default is " + Default_minHtz + ".\n"
			+ "  -maxHtz <htz> : the largest frequency in hertz to consider.\n" 
			+ "     Default is " + Default_maxHtz + ".\n"
			+ "  -featureLen <N> : the number of features in the feature vector.\n" 
			+ "     Default is " + Default_featureLen + ".\n"
	;
	
	private FeatureGramDescriptor<double[],double[]> fge;

	/**
	 * Parse the options and establish the return value of {@link #getSounds()}.
	 * @param cmdargs
	 * @return true on now option errors.  false and issue an error message on stderr if an option error.
	 */
	public boolean parseOptions(CommandArgs cmdargs) {
		// FeatureExtractor options
		int minHtz = cmdargs.getOption("minHtz",Default_minHtz);
		int maxHtz = cmdargs.getOption("maxHtz",Default_maxHtz);
		int featureLen = cmdargs.getOption("featureLen",Default_featureLen);;
		int numBands= cmdargs.getOption("numBands", MFCCFeatureExtractor.DEFAULT_NUM_BANDS);
		int subWindowMsec = cmdargs.getOption("subWindowMsec", DEFAULT_SUBWINDOW_MSEC);

		int resamplingRate = 0;	// Don't resample
		boolean isMFCC= cmdargs.getFlag("mfcc"); 
		boolean isMFFB =cmdargs.getFlag("mffb");  
		boolean isFFT = cmdargs.getFlag("fft");
		boolean isLogMel= cmdargs.getFlag("logmel");
		int flagCount = (isMFCC? 1:0) + (isMFFB? 1:0) + (isFFT? 1:0) + (isLogMel? 1:0);
		if (flagCount == 0) {
			isMFCC = true;
		} else if ( flagCount > 1){
			System.err.println("More than one feature extractor specified.");
			return false;
		}

		IFeatureExtractor<double[],double[]> extractor;
		if (isFFT) 
			extractor = new FFTFeatureExtractor(false, featureLen, resamplingRate, minHtz, maxHtz);
		else if (isMFCC)	
			extractor = new MFCCFeatureExtractor(resamplingRate, numBands, minHtz, maxHtz, featureLen); 
		else if (isMFFB)	
			extractor = new MFFBFeatureExtractor(resamplingRate, featureLen, minHtz, maxHtz, MFFBFeatureExtractor.DEFAULT_NORMALIZE, MFFBFeatureExtractor.DEFAULT_USE_LOG);
		else if (isLogMel)	
			extractor = new LogMelFeatureExtractor(resamplingRate, numBands, minHtz, maxHtz, featureLen);
		else
			throw new RuntimeException("Can't determine feature extractor");

		this.fge = new FeatureGramDescriptor<>(subWindowMsec, 0, extractor, null);

		return true;
	}



	/**
	 * @param isFFT
	 * @param minHtz
	 * @param maxHtz
	 * @param featureLen
	 * @return
	 */
	public IFeatureGramDescriptor<double[], double[]> getFeatureGramExtractor() {
		return this.fge;
	}




}
