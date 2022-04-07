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

import java.util.List;

import org.eng.aisp.DoubleWindow;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.feature.DoubleFeature;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.extractor.vector.util.ExtendedFFT;
import org.eng.aisp.util.VectorUtils;
import org.eng.util.ISignalGenerator;
import org.eng.util.MathUtil;
import org.eng.util.SingleFrequencySignalGenerator;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;


public abstract class AbstractFeatureExtractorTest {
	

	protected abstract IFeatureExtractor<double[], double[]> getFeatureExtractor();

	protected abstract IFeatureExtractor<double[], double[]> getFeatureExtractor(int samplingRate, int minHz, int maxHz, int featureLen);

	protected double getThresholdFactor() { return 0.0625; }
//	protected double getThresholdFactor() { return 0.10; }
	protected boolean useThreholdLogging() { return false; }	
	
	/**
	 * Used by {@link #testFrequencyRange()} to allow sub-classes to indicate whether or not the feature extracted on a 
	 * sinusoidal signal will generate a feature that has a peak at/near the expected frequency.
	 * @return
	 */
	protected boolean doesSineFrequencyGiveFeaturePeaks() { return true; }
	

	@Test
	public void testDifferentSamplingRates() {
		IFeatureExtractor<double[], double[]> fe = getFeatureExtractor();
		double threshFactor = getThresholdFactor();
		
		int bitsPerSample = 16;
		int channels = 1;
		int durationMsec = 2000;
		differentSoundParameters(fe, durationMsec, durationMsec, channels, channels,
				44100, 16000, bitsPerSample, bitsPerSample, threshFactor);
		differentSoundParameters(fe, durationMsec, durationMsec,channels, channels, 
				44100, 22050, bitsPerSample, bitsPerSample, threshFactor);
		
	}
	
	@Test
	public void testDifferentDurations() {
		IFeatureExtractor<double[], double[]> fe = getFeatureExtractor();
		double threshFactor = getThresholdFactor();
		
		int bitsPerSample = 16;
		int channels = 1;
		int samplingRate = 44100;
		differentSoundParameters(fe, 2000, 2500, channels, channels, 
				samplingRate, samplingRate, bitsPerSample, bitsPerSample, threshFactor);
		differentSoundParameters(fe, 2000, 3000, channels, channels, 
				samplingRate, samplingRate, bitsPerSample, bitsPerSample, threshFactor);
		differentSoundParameters(fe, 2000, 4000, channels, channels, 
				samplingRate, samplingRate, bitsPerSample, bitsPerSample, threshFactor);
		
	}

	@Test
	public void testDifferentBitsPerSample() {
		IFeatureExtractor<double[], double[]> fe = getFeatureExtractor();
		double threshFactor = getThresholdFactor();
		
		int samplingRate= 44100;
		int channels = 1;
		int durationMsec = 1000;
		differentSoundParameters(fe, durationMsec, durationMsec, channels, channels, 
				samplingRate, samplingRate, 8, 16, threshFactor);
	}

	@Test
	public void testDifferentChannels() {
		IFeatureExtractor<double[], double[]> fe = getFeatureExtractor();
		double threshFactor = getThresholdFactor();
		
		int samplingRate= 44100;
		int bitsPerSample = 16;
		int durationMsec = 2000;;
		differentSoundParameters(fe, durationMsec, durationMsec, 1,2, 
				samplingRate, samplingRate, bitsPerSample, bitsPerSample, threshFactor);
	}

	


	protected double[] getFeatures(IFeatureExtractor<double[], double[]> fe, int durationMsec, int channels, int samplingRate, int bitsPerSample,
			double amp, double offset, int htz, boolean addWhiteNoise) {
		ISignalGenerator siggen = new SingleFrequencySignalGenerator(samplingRate, amp, offset, htz, addWhiteNoise, false);
		int startMsec = 0, pauseMsec = 0;
		List<SoundClip> sounds = SoundTestUtils.createClips(1, startMsec, pauseMsec, durationMsec, channels, samplingRate, bitsPerSample, siggen);
		SoundClip clip = sounds.get(0);
		double[] data = clip.getData();
		IFeature<double[]> f = fe.apply(clip);
		double[] fdata = f.getData();
		return fdata;
	}
	
	protected void differentSoundParameters(IFeatureExtractor<double[], double[]> fe, int duration1Msec, int duration2Msec, int channels1, int channels2, 
			int samplingRate1, int samplingRate2, int bitsPerSample1, int bitsPerSample2, double threshFactor) {
		double[] feature =  getFeatures(fe, duration1Msec, channels1, samplingRate1, bitsPerSample1, 1.0, 0.0, 1000, false);
		double[] feature2 = getFeatures(fe, duration2Msec, channels2, samplingRate2, bitsPerSample2, 1.0, 0.0, 1000, false);
		
        Assert.assertTrue(feature.length == feature2.length);

        boolean useLog = this.useThreholdLogging();
        if (useLog) {
			for (int i=0; i<feature.length; i++) {
				feature[i] = Math.log10(feature[i]);
				feature2[i]= Math.log10(feature2[i]);
			}
        }
        double v1 = VectorUtils.lpDistance(feature,  2);
        double v2 = VectorUtils.lpDistance(feature2, 2);
       	double diffAbsolute =  VectorUtils.lpDistance(feature,  feature2, 2);
       	double diffRelative = 2.0 * diffAbsolute / (Math.abs(v1 + v2));
       	Assert.assertTrue("v1=" + v1 + ", v2=" + v2, diffRelative <= threshFactor || diffAbsolute == 0.0);  
	}

	@Test
	public void testFrequencyRange() {
		int maxFFTSize = 32;
		int minHtz = 200, maxHtz = 1000;
		int samplingRate = ExtendedFFT.DEFAULT_FFT_MAX_SAMPLING_RATE;	// Avoid resampling to get more perfect results.
		int samples = (int)Math.pow(2,18);		// Use a power of 2 samples so we can expect more perfect results from the FFT.
		double durationMsec = 1000.0 * samples / samplingRate; 
		double percentFrequencyErrorTolerance = 0.03;

		IFeatureExtractor<double[],double[]> fe = getFeatureExtractor(samplingRate, minHtz, maxHtz, maxFFTSize); 
		Assume.assumeTrue("Frequency limited feature extraction testing not support. Skipping.", fe != null);
		
		// Test a flat signal.
		double[] data = new double[samples];
		IDataWindow<double[]> dw = new DoubleWindow(0, durationMsec, data);
		DoubleFeature feature = (DoubleFeature)fe.apply(dw);
		verifyPowerSpectrum(feature, minHtz, maxHtz, maxFFTSize, null, percentFrequencyErrorTolerance );
	
		// Test a single sine wave.
		int msecStart = 0, msecSpacing = 0, channels = 1, bitsPerSample = 16;
		/** This value is a compromise of sorts. >504 gives a wide peak on LogMel/MFFB, which breaks the tests.
		 *  But less than 502 gives a larger value of percentFrequencyErrorTolerance above.
		 */
		int  htz = 502;
		double amp = 0.5, offset = 0.1;
		boolean addFreqNoise = false, addAmpNoise= false;		// Pure sine signal
		dw = SoundTestUtils.createClips(1, msecStart, msecSpacing, durationMsec, channels, samplingRate, bitsPerSample, amp, offset, htz, addAmpNoise, addFreqNoise).get(0);
		feature = (DoubleFeature)fe.apply(dw);
		int [] expectedPeaks = this.doesSineFrequencyGiveFeaturePeaks() ? new int[] {htz} : null;
		verifyPowerSpectrum(feature, minHtz, maxHtz, maxFFTSize, expectedPeaks, percentFrequencyErrorTolerance);
		
	}

	private void verifyPowerSpectrum(DoubleFeature feature, int minHtz, int maxHtz, int maxFFTSize, int[] htzPeaks, double precentAllowedFrequencyError) {
		double[] freq = feature.getIndependentValues();
		Assert.assertTrue(freq[0] >= minHtz);
		Assert.assertTrue(freq.length == maxFFTSize);
		for (int i=0 ; i<freq.length ;i++) 
			Assert.assertTrue(freq[i] <= maxHtz); 

		if (htzPeaks != null) {
			double[] power = feature.getData();
			int peaks[] = MathUtil.findMaxima(power, .10); 	// Find peaks that stand out by more than 10% relative to the neighbors.
			Assert.assertTrue(peaks.length == htzPeaks.length);
			for (int i=0 ; i<peaks.length ; i++) {
				double expectedFreq = htzPeaks[i];
				Assert.assertTrue(peaks[i] < freq.length);	// Make sure findPeakIndices() didn't fail somehow.
				double detectedFreq = freq[peaks[i]];
				double deltaFreq = Math.abs(expectedFreq - detectedFreq);
				double percentError = deltaFreq/expectedFreq;
				Assert.assertTrue("Percent error = " + percentError, percentError < precentAllowedFrequencyError);
			}
		}
	
		
	}


}
