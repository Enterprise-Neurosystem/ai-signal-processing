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
package org.eng.aisp.feature.extractor.vector;

/**
 * LogMelfeature extractor.
 * A simple extension of MFFBFeatureExtractor to configure normalization and useLog.
 */
public class LogMelFeatureExtractor extends MFFBFeatureExtractor { 

	private static final long serialVersionUID = -1435877796702995515L;


	/**
	 * Constructor for LogMelFeatureExtractor with all default settings from MFFBFeatureExtractor.
	 */
	public LogMelFeatureExtractor() {
		this(MFFBFeatureExtractor.DEFAULT_NUM_BANDS);
	}
	
	/**
	 * A convenience on {@link #LogMelFeatureExtractor(double, int, int, int, int)} using sampling rate of the sound encountered, default min/max frequency, 
	 * and unrestricted number of final coefficients. The resulting feature vector length is equal to numBands. 
	 */
	public LogMelFeatureExtractor(int numBands) {
		this(MFFBFeatureExtractor.DEFAULT_RESAMPLING_RATE, numBands, MFFBFeatureExtractor.DEFAULT_MIN_FREQ, MFFBFeatureExtractor.DEFAULT_MAX_FREQ,0);
	}
	
	
	/**
	 * Constructor for LogMelFeatureExtractor that simply configures the super class to not normalize, but to take the log of the MFFB.
	 * The feature can be restricted to a number of features within a given frequency range.
	 * @param numBands defines the number of bands used in the Triangle filter.  
	 * THe base feature produced covers the frequency range with this number of values.  
	 * The feature length ultimately produces can be further controlled by minFinalCoefs parameter.
	 * @param minFreq the minimum frequency present in the output feature. 
	 * @param maxFreq the maximum frequency present in the output feature. 
	 * @param normalize Specifies whether normalization is applied to the output of triangle filter.  Normalization involves scaling each feature value such that the
	 * sum of all features equals 1.
	 * @param useLog Specifies whether log is applied to the output of triangle filter and normalization (if enabled)
	 * @param finalCoeffs may be use to truncate the base feature vector computed using numBands.  
	 * if 0, then the feature length is the number of bands.  
	 * If larger than 0, then the feature vector will be padded as necessary to the given length.
	 */
	public LogMelFeatureExtractor(double targetSamplingRate, int numBands, int minFreq, int maxFreq, int finalCoeffs) {
		super(targetSamplingRate, numBands, minFreq, maxFreq, false, true, finalCoeffs );
	}

	@Override
	public String toString() {
		return "LogMelFeatureExtractor [numBands=" + numBands + ", minFreq=" + minFreq + ", maxFreq=" + maxFreq
				+ ", normalize=" + normalize + ", useLog=" + useLog + ", targetSamplingRate=" + targetSamplingRate
				+ ", maxFinalCoefficients=" + maxFinalCoefficients + "]";
	}
	

}
