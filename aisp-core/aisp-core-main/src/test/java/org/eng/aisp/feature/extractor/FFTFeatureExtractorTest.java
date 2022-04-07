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
package org.eng.aisp.feature.extractor;

import org.eng.aisp.feature.extractor.vector.FFTFeatureExtractor;


public class FFTFeatureExtractorTest extends AbstractFeatureExtractorTest {

	@Override
	protected IFeatureExtractor<double[], double[]> getFeatureExtractor() {
		return new FFTFeatureExtractor();
	}

	protected boolean useThreholdLogging() {
		return true;
	}

	@Override
	protected IFeatureExtractor<double[], double[]> getFeatureExtractor(int samplingRate, int minHz, int maxHz, int featureLen) {
//		FFTFeatureExtractor fe = new FFTFeatureExtractor(false, featureLen, samplingRate, minHz, maxHz);
		FFTFeatureExtractor fe = new FFTFeatureExtractor(samplingRate, minHz, maxHz, false, true, featureLen);
		return fe;
	}
	protected double getThresholdFactor() { return 0.066;  }	// TODO: Why do we need this higher error threshold.
//	@Test
//	public void testFrequencyRange() {
//		boolean normalize = false;
//		int maxFFTSize = 32;
//		int minHtz = 200, maxHtz = 1000;
//		int samplingRate = ExtendedFFT.DEFAULT_FFT_MAX_SAMPLING_RATE;	// Avoid resampling to get more perfect results.
//		int samples = (int)Math.pow(2,18);		// Use a power of 2 samples so we can expect more perfect results from the FFT.
//		double durationMsec = 1000.0 * samples / samplingRate; 
//		double percentFrequencyErrorTolerance = 0.01;
//
//		FFTFeatureExtractor fe = new FFTFeatureExtractor(normalize, maxFFTSize, samplingRate, minHtz, maxHtz);
//		
//		// Test a flat signal.
//		double[] data = new double[samples];
//		IDataWindow<double[]> dw = new DoubleWindow(0, durationMsec, data);
//		DoubleFeature feature = (DoubleFeature)fe.apply(dw);
//		verifyPowerSpectrum(feature, minHtz, maxHtz, maxFFTSize, null, percentFrequencyErrorTolerance );
//	
//		// Test a single sine wave.
//		int msecStart = 0, msecSpacing = 0, channels = 1, bitsPerSample = 16, htz = 512;
//		double amp = 0.5, offset = 0.1;
//		boolean addFreqNoise = false, addAmpNoise= false;		// Pure sine signal
//		dw = SoundTestUtils.createClips(1, msecStart, msecSpacing, durationMsec, channels, samplingRate, bitsPerSample, amp, offset, htz, addAmpNoise, addFreqNoise).get(0);
//		feature = (DoubleFeature)fe.apply(dw);
//		verifyPowerSpectrum(feature, minHtz, maxHtz, maxFFTSize, new int[] { htz },percentFrequencyErrorTolerance);
//		
//	}
//
//	private void verifyPowerSpectrum(DoubleFeature feature, int minHtz, int maxHtz, int maxFFTSize, int[] htzPeaks, double precentAllowedFrequencyError) {
//		double[] freq = feature.getIndependentValues();
//		Assert.assertTrue(freq[0] >= minHtz);
//		Assert.assertTrue(freq.length == maxFFTSize);
//		for (int i=0 ; i<freq.length ;i++) 
//			Assert.assertTrue(freq[i] <= maxHtz); 
//
//		if (htzPeaks != null) {
//			double[] power = feature.getData();
//			int peaks[] = MathUtil.findMaxima(power, .10); 	// Find peaks that stand out by more than 10% relative to the neighbors.
//			Assert.assertTrue(peaks.length == htzPeaks.length);
//			for (int i=0 ; i<peaks.length ; i++) {
//				double expectedFreq = htzPeaks[i];
//				Assert.assertTrue(peaks[i] < freq.length);	// Make sure findPeakIndices() didn't fail somehow.
//				double detectedFreq = freq[peaks[i]];
//				double deltaFreq = Math.abs(expectedFreq - detectedFreq);
//				double percentError = deltaFreq/expectedFreq;
//				Assert.assertTrue(percentError < precentAllowedFrequencyError);
//			}
//		}
//	
//		
//	}
//


}
