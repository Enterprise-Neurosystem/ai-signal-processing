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

import org.apache.commons.math3.transform.DctNormalization;
import org.apache.commons.math3.transform.FastCosineTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.extractor.IDoubleFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.util.ExtendedFFT;
import org.eng.aisp.processor.AbstractCachingWindowProcessor;
import org.eng.aisp.util.Signal2D;

/**
 * MFCC feature extractor.
 * @author wangshiq
 * Note: this was implemented before MFFBFeatureExtractor and LogMelFeatureExtractor, which are sub-sets of this extractor and so does not sub-class from them
 * and should not change to do that in order to maintain backwards compatibility. .  That said, this does now use the MFFB methods to do the computation/construction
 * of the feature.
 */

public class MFCCFeatureExtractor extends AbstractCachingWindowProcessor<IDataWindow<double[]>, IFeature<double[]>> implements IDoubleFeatureExtractor { 

	private static final long serialVersionUID = -8897937414560435754L;
	// These were found to be best when using the DCASE 2016 Task 2 data with GMM classifier
	public final static int DEFAULT_NUM_BANDS = 40;
	public final static int DEFAULT_MIN_FREQ = 5;
	public final static int DEFAULT_MAX_FREQ = 20000;
	private final static int DEFAULT_MAX_FINAL_COEFFS = Integer.MAX_VALUE;  //Return full array (from DCT) by default
	public final static int DEFAULT_FINAL_COEFFS = 64;  

	protected final int numBands;
	protected final int minFreq;  //in Hz
	protected final int maxFreq;
	/** As of 5/2020, this is now the final coefficients.  That is, if non-zero, defines the length of the feature vector produced. */
	protected final int maxFinalCoeffs;	
	protected final double targetSamplingRate;

	
	public final static int DEFAULT_RESAMPLING_RATE = ExtendedFFT.DEFAULT_FFT_MAX_SAMPLING_RATE;
	/**
	 * Constructor for MFCCFeatureExtractor with all default settings.
	 */
	public MFCCFeatureExtractor() {
		this(DEFAULT_NUM_BANDS, DEFAULT_MAX_FINAL_COEFFS);
	}
	
	/**
	 * Constructor for MFCCFeatureExtractor
	 * Defaults the number of final coefficients to be all the coefficients from DCT after zero-padding.
	 * The number of final coefficients is a power of 2 plus one.
	 * @param numBands Number of Mel-frequency filter bands
	 */
	public MFCCFeatureExtractor(int numBands) {
		this(numBands, true);
	}
	
	/**
	 * Constructor for MFCCFeatureExtractor
	 * @param numBands Number of Mel-frequency filter bands
	 * @param finalCoeffs If non-zero, then the number of coefficients in the final feature vector. 
	 *   If the length of the resulting array from DCT (after zero-padding) is less than maxFinalCoeffs, then the full array will be returned.
	 */
	public MFCCFeatureExtractor(int numBands, int finalCoeffs) {
		this(DEFAULT_RESAMPLING_RATE, numBands, DEFAULT_MIN_FREQ, DEFAULT_MAX_FREQ,finalCoeffs);
	}
	
	/**
	 * Constructor for MFCCFeatureExtractor
	 * @param numBands Number of Mel-frequency filter bands
	 * @param returnAllFinalCoeffs If true, all the coefficients from DCT (after zero-padding) are returned; if false, return the number of coefficients equal to numBands.
	 */
	public MFCCFeatureExtractor(int numBands, boolean returnAllFinalCoeffs) {
		this(numBands, returnAllFinalCoeffs ? Integer.MAX_VALUE : numBands);
	}	
	

	/**
	 * Constructor for MFCCFeatureExtractor
	 * @param targetSamplingRate if non-zero, specifies the sampling rate to re-sample the data to, as necessary, when computing the FFT.
	 * @param numBands Number of Mel-frequency filter bands
	 * @param minFreq Minimum frequency in Hz
	 * @param maxFreq Maximum frequency in Hz
	 * @param finalCoeffs If non-zero, then the number of coefficients in the final feature vector.  
	 *   If the length of the resulting array from DCT is less than maxFinalCoeffs, then a 0-padded array will be returned.
	 */
	public MFCCFeatureExtractor(double targetSamplingRate, int numBands, int minFreq, int maxFreq, int finalCoeffs) {
		super();
		if (finalCoeffs == DEFAULT_MAX_FINAL_COEFFS)
			finalCoeffs = 0;	// Previous default max was to NOT truncate. 0 now indicates this. 
		if (numBands <= 0 || minFreq < 0 || maxFreq < 0 || finalCoeffs < 0)
			throw new IllegalArgumentException("all arguments must be positive");
		this.numBands = numBands;
		if (minFreq >= maxFreq)
			throw new IllegalArgumentException("min frequency (" + minFreq + ") is not smaller than max frequency (" + maxFreq + ")");
		this.minFreq = minFreq;
		this.maxFreq = maxFreq;
		this.maxFinalCoeffs = finalCoeffs;
		this.targetSamplingRate = targetSamplingRate;
	}
	
	@Override
	protected IFeature<double[]> applyImpl(IDataWindow<double[]> window) {
		
		if (!(window instanceof IDataWindow))
			throw new IllegalArgumentException("window must be an instance of " + IDataWindow.class.getName());
		IDataWindow<double[]> recording = (IDataWindow<double[]>) window;
		
		// Don't normalize but take the log of the MFFB.
		Signal2D feature = MFFBFeatureExtractor.computeMFFB(recording, this.targetSamplingRate, numBands, minFreq, maxFreq, false, true, ExtendedFFT.MAX_PEAK_TO_NOISE_FLOOR_RATIO);
		double[] trigLogPower = feature.getYValues();
		
		
		//*** DCT of trigLogPower
		
		//This is using DCT from Apache maths which is a DCT-I implementation
		int numDCTPoints = (int) (Math.pow(2, Math.ceil(Math.log(numBands -1)/Math.log(2))) + 1);
		if (numDCTPoints < feature.size()) {
			feature = feature.trim(0,  numDCTPoints);
			trigLogPower = feature.getYValues();
		} else if (numDCTPoints > feature.size()){
			feature = feature.pad(numDCTPoints,0);
			trigLogPower = feature.getYValues();
		}

		double[] dctInputPower = trigLogPower; 
		FastCosineTransformer transDCT=new FastCosineTransformer(DctNormalization.STANDARD_DCT_I);
		double[] dctPowerAll = transDCT.transform(dctInputPower,TransformType.FORWARD);
		feature = new Signal2D(feature.getXValues(), dctPowerAll);
		
		//Truncate if resulting DCT array is longer than maxFinalCoeffs
		IFeature<double[]> df = MFFBFeatureExtractor.buildFeature(recording, feature, this.maxFinalCoeffs);
;
			
		return df; 
	}

    
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + maxFinalCoeffs;
		result = prime * result + maxFreq;
		result = prime * result + minFreq;
		result = prime * result + numBands;
		long temp;
		temp = Double.doubleToLongBits(targetSamplingRate);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof MFCCFeatureExtractor))
			return false;
		MFCCFeatureExtractor other = (MFCCFeatureExtractor) obj;
		if (maxFinalCoeffs != other.maxFinalCoeffs)
			return false;
		if (maxFreq != other.maxFreq)
			return false;
		if (minFreq != other.minFreq)
			return false;
		if (numBands != other.numBands)
			return false;
		if (Double.doubleToLongBits(targetSamplingRate) != Double.doubleToLongBits(other.targetSamplingRate))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MFCCFeatureExtractor [numBands=" + numBands + ", minFreq=" + minFreq + ", maxFreq=" + maxFreq
				+ ", maxFinalCoeffs=" + maxFinalCoeffs + ", targetSamplingRate=" + targetSamplingRate + "]";
	}



}
