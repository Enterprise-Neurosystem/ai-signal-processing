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
package org.eng.aisp.feature.extractor.vector.util;

import java.util.HashMap;
import java.util.Map;

import org.eng.aisp.IDataWindow;
import org.eng.aisp.util.Signal2D;
import org.eng.cache.Cache;
import org.eng.cache.IMultiKeyCache;
import org.eng.util.Vector;

/**
 * Support for Filter Bank computation.
 * See <a href="http://practicalcryptography.com/miscellaneous/machine-learning/guide-mel-frequency-cepstral-coefficients-mfccs/">
 * http://practicalcryptography.com/miscellaneous/machine-learning/guide-mel-frequency-cepstral-coefficients-mfccs/</a>
 */
public class TriangleFilter  {
	
	/** Only static methods. so make non-instantiable. */
	private TriangleFilter() {}

	/**
	 * A convenience on {@link #filter(double[], double, double, int, int, int)} passing in the window's data and sampling rate.
	 * @param maxPeakToNoiseRation 
	 */
	public static Signal2D filter(IDataWindow<double[]> recording, double targetSamplingRate, int numBands, int minFreq, int maxFreq, double maxPeakToNoiseRation) {
		return filter(recording.getData(), recording.getSamplingRate(), targetSamplingRate, numBands, minFreq, maxFreq, maxPeakToNoiseRation);
	}

	
	private static Map<String,Integer> paddingMultiplierCache = new HashMap<String,Integer>();

	private synchronized static Integer getCachedPaddingMultiplier(double[] data, double samplesPerSecond, double targetSamplingRate, int numBands) {
		String padKey = getPadKey(data, samplesPerSecond, targetSamplingRate, numBands);
		Integer multiplier = paddingMultiplierCache.get(padKey);
		return multiplier;
	}

	private synchronized static void addCachedPaddingMultiplier(double[] data, double samplesPerSecond, double targetSamplingRate, int numBands, int paddingMultiplier) {
		String padKey = getPadKey(data, samplesPerSecond, targetSamplingRate, numBands);
		Integer multiplier = paddingMultiplierCache.put(padKey,paddingMultiplier);
	}

	private static String getPadKey(double[] data, double samplesPerSecond, double targetSamplingRate, int numBands) {
		return "" + data.length + samplesPerSecond + targetSamplingRate + numBands;
	}

	/**
	 * Applies FFT extraction and triangular filter on the window's getData() value using the sampling rate for the 0th datum. 
	 * @param data  
	 * @param samplesPerSecond	 the sampling rate of the given data.
	 * @param targetSamplingRate the rate to re-sample the given data to.  If 0, then use the default as defined by ExtendedFFT.
	 * @param numBands
	 * @param minFreq
	 * @param maxFreq
	 * @param maxPeakToNoiseRation 
	 * @return a pair of arrays, each of length numBands.
	 */
	public static Signal2D filter(double[] data, double samplesPerSecond, double targetSamplingRate, int numBands, int minFreq, int maxFreq, double maxPeakToNoiseRation) {
		if (numBands > 128)
			// When the filter bank is large, we may need to pad the signal with extra zeros to get the needed frequency resolution.
			// If we don't do this, the result can be full of 0's where there is no frequency in the fft under the triangle.
			// That said, there is a more complex relationship between numBands and sampling rate so the test for greater than 128 is less than fully robust.
			return filterWithExtraPadding(data,samplesPerSecond, targetSamplingRate, numBands, minFreq, maxFreq, maxPeakToNoiseRation);
		else
			return filterWithoutExtraPadding(data,samplesPerSecond, targetSamplingRate, numBands, minFreq, maxFreq, maxPeakToNoiseRation);
	}

	private static Signal2D filterWithoutExtraPadding(double[] data, double samplesPerSecond, double targetSamplingRate, int numBands, int minFreq, int maxFreq, double maxPeakToNoiseRation) {
		Signal2D power = ExtendedFFT.power(data, samplesPerSecond, targetSamplingRate, 1, maxPeakToNoiseRation);
		double[] fftFreq = power.getXValues().getVector(); 
		double[] fftPower = power.getYValues(); 
		Signal2D triangle = TriangleFilter.filter2(fftFreq, fftPower, numBands, minFreq, maxFreq);
		return triangle;
	}

	private static Signal2D filterWithExtraPadding(double[] data, double samplesPerSecond, double targetSamplingRate, int numBands, int minFreq, int maxFreq, double maxPeakToNoiseRation) {
		boolean done = true;
        Integer cachedPaddingMultiplier = getCachedPaddingMultiplier(data,samplesPerSecond, targetSamplingRate, numBands); 
        int paddingMultiplier = cachedPaddingMultiplier == null ? 1 : cachedPaddingMultiplier;
        		
        Signal2D triangle; 
        do {
			Signal2D power = ExtendedFFT.power(data, samplesPerSecond, targetSamplingRate, paddingMultiplier, maxPeakToNoiseRation);
			double[] fftFreq = power.getXValues().getVector(); 
			double[] fftPower = power.getYValues(); 
			triangle = TriangleFilter.filter2(fftFreq, fftPower, numBands, minFreq, maxFreq);
			double[] values = triangle.getYValues();
			if (cachedPaddingMultiplier == null) {
				boolean hasZero = false;
				for (int i=0 ; !hasZero && i<values.length ; i++) {
					hasZero = values[i] == 0;
				}
				if (hasZero) {
//					AISPLogger.logger.info("Found zeros in the triangle filter");
					paddingMultiplier *= 2;
					done = paddingMultiplier >= 8;
				} else {
					done = true;
				}
			}
        } while (!done);
        if (cachedPaddingMultiplier == null)
        	addCachedPaddingMultiplier(data,samplesPerSecond, targetSamplingRate, numBands, paddingMultiplier);
        return triangle;
	}


	/**
	 * Applies triangular filter on FFT power coefficients within the specified frequency range.
	 * @param fftFreq
	 * @param fftPower
	 * @param numBands
	 * @param minFreq
	 * @param maxFreq
	 * @param maxPeakToNoiseRation 
	 * @return an array of length numBands.
	 */
	private static Signal2D filter2(final double[] fftFreq, final double[] fftPower, int numBands, int minFreq, int maxFreq) {
		//compute center frequencies
		minFreq= Math.max(minFreq, 0);
		maxFreq = Math.min(maxFreq, (int)fftFreq[fftFreq.length-1]);

		int[] centerFreqs = getCenteredFrequencies(numBands, minFreq, maxFreq);

		double[] power = new double[numBands];

		for(int i = 1; i < numBands+1; i++) {
			int startFreq  = centerFreqs[i-1];
			int centerFreq = centerFreqs[i];
			int stopFreq   = centerFreqs[i+1];

			double delta2 = Math.pow(centerFreq - startFreq, 2);
			double divisor1 = 1.0 / delta2;
			delta2 = Math.pow(centerFreq - stopFreq, 2);
			double divisor2 = 1.0 / delta2;
			power[i-1] = 0;
			for (int j=0; j<fftFreq.length; j++) {
				double freq = fftFreq[j];
//				if(freq >= startFreq && freq <= centerFreq) {
//					power[i-1] += fftPower[j] * (freq - startFreq) * divisor1; 
//				} else if(freq > centerFreq && freq <= stopFreq) {
//					power[i-1] += fftPower[j] * (stopFreq - freq) * divisor2;
//				}
				boolean past = freq > stopFreq;
				if (freq >= startFreq && !past) { 
					if (freq <= centerFreq)
						power[i-1] += fftPower[j] * (freq - startFreq) * divisor1; 
					else
						power[i-1] += fftPower[j] * (stopFreq - freq) * divisor2;
				} else if (past) {
					break;
				}
			}
			
		}
		Vector freqVector = getCachedVector(centerFreqs, numBands, minFreq, maxFreq);
		return new Signal2D(freqVector,power);
	}

	private final static IMultiKeyCache centeredFequencyCache = Cache.newMemoryCache();

	/**
	 * Get the array of frequencies between min/max of length equal to numBands+2. 
	 * @param fftFreq
	 * @param numBands
	 * @param minFreq
	 * @param maxFreq
	 * @returna an array of length numBands+2.
	 */
	protected static synchronized int[] getCenteredFrequencies(int numBands, int minFreq, int maxFreq) {
		int[] centerFreqs; 
		centerFreqs = (int[])centeredFequencyCache.get(numBands,minFreq,maxFreq);
		if (centerFreqs == null) {
			centerFreqs = new int[numBands+2];
			
			int minMel = freqToMel(minFreq);
			int maxMel = freqToMel(maxFreq);
			
			for (int i=0; i<numBands+2; i++) {
				int mel = minMel + i*(maxMel-minMel)/ (numBands+1);
				int linearFrequency = melToFreq(mel);
				centerFreqs[i] = linearFrequency;
				centerFreqs[i] = Math.max(centerFreqs[i], minFreq);
				centerFreqs[i] = Math.min(centerFreqs[i], maxFreq);
			}
			centeredFequencyCache.put(centerFreqs, numBands, minFreq,maxFreq);
		}

		return centerFreqs;
	}

	/** 
	 * This should always be true, but is put here to allow Inoue-san to turn off for his local testing of the OOM errors 
	 * found on 35 hours of training data.  Circa 6/2020.
	 */
//	private final static boolean doVectorCaching = false; // AISPProperties.instance().getProperty("triangle.frequency.caching.enabled", true);
	
	private final static IMultiKeyCache vectorCache = Cache.newMemoryCache();

	private static synchronized Vector getCachedVector(int[] centerFreqs,  int numBands, int minFreq, int maxFreq) {
		Vector v = (Vector)vectorCache.get(numBands,minFreq,maxFreq);
		if (v == null) {
			double[] frequency = new double[numBands];
			for(int i = 1; i < numBands+1; i++) 
				frequency[i-1] = centerFreqs[i];

			v = new Vector(frequency);
			vectorCache.put(v, numBands, minFreq,maxFreq);
		}
		return v;
	}

	public static int freqToMel(int freq) {
		return (int) (2595.0 * Math.log10(1.0 + (double)freq/700.0));
	}
	
	public static int melToFreq(int mel) {
		return (int) (700.0 * (Math.pow(10, (double)mel / 2595.0) - 1.0));
	}

}
