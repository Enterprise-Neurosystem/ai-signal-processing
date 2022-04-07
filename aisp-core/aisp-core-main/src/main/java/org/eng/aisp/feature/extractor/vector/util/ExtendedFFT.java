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

import java.util.Arrays;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.eng.aisp.AISPLogger;
import org.eng.aisp.AISPProperties;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.util.Signal2D;
import org.eng.aisp.util.VectorUtils;
import org.eng.util.MathUtil;
import org.eng.util.Sample;
import org.eng.util.Vector;
import org.jtransforms.fft.DoubleFFT_1D;

/**
 * Class for computing the power of FFT from IDataWindow<double[]>. 
 * The frequency range is extended to the FFT_MAX_SAMPLING_RATE / 2, so that the features of sounds with different sampling rates are comparable.
 * When the signal length is zero-padded to power of two, upsampling is performed through linear interpolation.
 * When the signal is not padded (improves the precision but requires longer processing time), upsampling is performed directly on the frequency domain.
 * The feature computation also ensures that the sum energy of features remains (approximately) the same with different sampling rates.
 * @author wangshiq
 *
 */

public class ExtendedFFT {
	
	//For normalizing against the sampling rate, so that the sum energy of sounds with different sampling rates remain the same.
	public final static String DEFAULT_FFT_MAX_SAMPLING_RATE_PROPERTY_NAME = "feature.fft.max_sampling_rate";
	/**
	 * Defines the default target sampling rate if not defined in calls to this class.
	 */
	public final static int DEFAULT_FFT_MAX_SAMPLING_RATE = AISPProperties.instance().getProperty(DEFAULT_FFT_MAX_SAMPLING_RATE_PROPERTY_NAME, 44100);  
	
	
	public final static String DEFAULT_MAX_PEAK_TO_NOISE_FLOOR_RATIO = "feature.fft.max_peak_to_noise_floor_ratio";
	
	private final static double MAX_PEAK_TO_NOISE_FLOOR_RATIO_NEW_DEFAULT = 1E20;
	
	/**
	 * Specifies the maximum peak to noise floor ratio of FFT power coefficients.
	 * Any coefficient that is too small such that the ratio of the peak value to the value of the coefficient exceeds 
	 * the maximum will be rounded up to satisfy the maximum ratio. This prevents the classifiers from capturing the noise floor 
	 * difference of different training samples, which could have an impact with different bits per sample and different sampling rates.
	 * Default is {@link #MAX_PEAK_TO_NOISE_FLOOR_RATIO_NEW_DEFAULT}. 
	 */
	public final static double MAX_PEAK_TO_NOISE_FLOOR_RATIO = AISPProperties.instance().getProperty(DEFAULT_MAX_PEAK_TO_NOISE_FLOOR_RATIO, 
			MAX_PEAK_TO_NOISE_FLOOR_RATIO_NEW_DEFAULT);
//			MAX_PEAK_TO_NOISE_FLOOR_RATIO_OLD_DEFAULT);
	
	
	public final static String DEFAULT_PAD_TO_POWER_OF_TWO_PROPERTY_NAME = "feature.fft.pad_to_power_of_two";
	/**
	 * Specifies whether to pad FFT to power of two. Padding improves the speed of feature extraction but reduces the accuracy of features extraction.
	 * Default is true.
	 */
	public final static boolean DEFAULT_PAD_TO_POWER_OF_TWO = AISPProperties.instance().getProperty(DEFAULT_PAD_TO_POWER_OF_TWO_PROPERTY_NAME, true);

	public final static String WINDOWING_PROPERTY_NAME = "feature.fft.windowing";
	public final static String HAMMING_WINDOWING_PROPERTY_VALUE= "hamming";
	public final static String HANNING_WINDOWING_PROPERTY_VALUE= "hanning";
	public final static String DEFAULT_WINDOWING= HAMMING_WINDOWING_PROPERTY_VALUE;
	public final static boolean USE_HAMMING_WINDOW = HAMMING_WINDOWING_PROPERTY_VALUE.equals(AISPProperties.instance().getProperty(WINDOWING_PROPERTY_NAME, DEFAULT_WINDOWING));
	public final static boolean USE_HANNING_WINDOW = HANNING_WINDOWING_PROPERTY_VALUE.equals(AISPProperties.instance().getProperty(WINDOWING_PROPERTY_NAME, DEFAULT_WINDOWING));


	public static Signal2D power(IDataWindow<double[]> recording, double maxPeakToNoiseRation) {
//		final double durationMsec = recording.getDurationMsec();
//		final double[] dataOrigWindow = recording.getData();
//		if (dataOrigWindow.length == 0)
//			throw new IllegalArgumentException("recording data window can not be zero length");
//		final int origSamplingRate = (int) recording.getSamplingRate(dataIndex);
		double[] data = recording.getData();
		double samplesPerSecond = recording.getSamplingRate();
//		double durationMsec = recording.getDurationMsec();
//		double expectedDurationMsec = 1000 * (data.length-1) / samplesPerSecond;
//		double delta  = expectedDurationMsec - (durationMsec / 1000) ;
//		if (Math.abs(delta) > .0001)
//			throw new RuntimeException("duration does not match samples and samples/second");
		return power(data,samplesPerSecond, maxPeakToNoiseRation);
	}

//	/**
//	 * A convenience on {@link #power(double[], double, double)} that sets the the duration according to the amount of data an sampling rate.
//	 * @param data
//	 * @param samplesPerSecond number of samples per second
//	 * @return
//	 */
//	public static Signal2D power(double[] data, double samplesPerSecond) {
//		if (data.length == 0)
//			throw new IllegalArgumentException("recording data window can not be zero length");
//		return power(data, samplesPerSecond, 1000 * (data.length-1) / samplesPerSecond);
//	}

	/**
	 * A convenience on {@link #power(double[], double, double, int, boolean)} that uses the default max sampling rate ( {@link #DEFAULT_FFT_MAX_SAMPLING_RATE} 
	 * and default pad to power of 2 setting ({@link #DEFAULT_PAD_TO_POWER_OF_TWO}).
	 * @param data
	 * @param samplesPerSecond sampling rate of given data.
	 * @param durationMsec
	 * @return
	 */
	public static Signal2D power(double[] data, double samplesPerSecond, double maxPeakToNoiseRation ) {
		return power(data,samplesPerSecond,DEFAULT_FFT_MAX_SAMPLING_RATE, DEFAULT_PAD_TO_POWER_OF_TWO, 0, maxPeakToNoiseRation);
	}
	
	/**
	 * Convenience on {@link #power(double[], double, double, boolean)} that uses the default pad-to-power-of-2 setting ({@link #DEFAULT_PAD_TO_POWER_OF_TWO}.
	 */
	public static Signal2D power(double[] data, double samplesPerSecond, double targetSamplingRate, double maxPeakToNoiseRation ) {
		return power(data,samplesPerSecond,targetSamplingRate, DEFAULT_PAD_TO_POWER_OF_TWO, 0, maxPeakToNoiseRation);
	}

	public static Signal2D power(double[] data, double samplesPerSecond, double targetSamplingRate, int extraPowerOf2Padding, double maxPeakToNoiseRation ) {
		return power(data,samplesPerSecond,targetSamplingRate, DEFAULT_PAD_TO_POWER_OF_TWO, extraPowerOf2Padding, maxPeakToNoiseRation);
	}

	/**
	 * Compute a signal (power vs frequency) for the given signal which is sampled at the given sampling rate.
	 * If the given sampling rate is not the same as the target rate, then the data will be resampled to match the given target rate.
	 * And if padding to power of two is desired, then it will be padded with 0s after resampling, if any.
	 * @param data
	 * @param samplesPerSecond sampling rate of given data.
	 * @param targetSamplingRate sampling rate to resample the given data to and on which the FFT will be computed.  
	 * Data will not be resampled if samplesPerSecond == targetSamplingRate. 
	 * If 0, then use the default sampling rate as defined by {@link #FFT_MAX_SAMPLING_RATE}.
	 * @param padToPowerOf2 if true then pad, as necessary, the given data with 0's before computing the FFT.
	 * @param extraPowerOf2Padding if larger than 1, defines a multiplier on the (possibly already padded) data length.  This is useful when
	 * higher frequency resolution is required.
	 * @param maxPeakToNoiseRation 	 Specifies the maximum peak to noise floor ratio of FFT power coefficients.
	 * Any coefficient that is smaller than the max (peak) value in the spectrum divided by this value will be reset to that ratio.
	 * This effectively limits the range of values such that the max divided by the min is not larger than this value.  This can
	 * be important when taking the log of these returned values thereby limiting the range to the order of this parameter.
	 * This prevents the classifiers from capturing the noise floor 
	 * difference of different training samples, which could have an impact with different bits per sample and different sampling rates.
	 * @return
	 */
	private static Signal2D power(final double[] data, final double samplesPerSecond,  double targetSamplingRate, final boolean padToPowerOf2, final int extraPowerOf2Padding, double maxPeakToNoiseRation) {
		if (data.length == 0)
			throw new IllegalArgumentException("recording data window can not be zero length");
		if (targetSamplingRate <= 0)
			targetSamplingRate = DEFAULT_FFT_MAX_SAMPLING_RATE;
	
		final boolean inPlace;
		
		final double scalingParam;
		final int usefulFFTLength;
		final double freqStep;
		final int totalFFTLength;
		final Complex[] resultsComplex;
		
		
		double[] dataInWindow;
		if (samplesPerSecond == targetSamplingRate) {
			dataInWindow = data;
			inPlace = false;
		} else if (samplesPerSecond < targetSamplingRate) {
			if(padToPowerOf2) {
				//do interpolation to get more values
				dataInWindow = VectorUtils.interpolate(data, samplesPerSecond, targetSamplingRate);
				inPlace = true;
			} else {
				// We don't need to interpolate because we can allow the FFT to be computed on the given data
				// and then at the end we adjust the frequencies based on the original sampling rate and not the target sampling rate.
				dataInWindow = data;
				inPlace = false;
			}
		} else {	// samplesPerSecond > targetSamplingRate
			// down sample to get the expected number of samples.
			double durationMsec = 1000 * data.length / samplesPerSecond;
			AISPLogger.logger.fine("Input sampling rate (" + samplesPerSecond + " samples/sec) is higher than " + targetSamplingRate 
					+ ", downsampling to " + targetSamplingRate  + " ");

			int samples = (int)(targetSamplingRate * durationMsec / 1000.0 + .5);
			dataInWindow = new double[samples];
			Sample.downSample(data , dataInWindow, true);
			inPlace = true;
		}

//		double newSamplingRate = targetSamplingRate;  //New sampling rate is always equal to targetSamplingRate (sounds with higher sampling rates are downsampled)
		
		//Apply windowing
		if (USE_HAMMING_WINDOW)
			dataInWindow = VectorUtils.applyHammingWindow(dataInWindow, inPlace);	
		else if (USE_HANNING_WINDOW)
			dataInWindow = VectorUtils.applyHanningWindow(dataInWindow, inPlace);	
		
		if(padToPowerOf2) {
			dataInWindow = MathUtil.padToPowerOfTwo(dataInWindow);
			if (extraPowerOf2Padding > 1)
				dataInWindow = Arrays.copyOf(dataInWindow, extraPowerOf2Padding*dataInWindow.length);
			//Using Apache maths to compute FFT (only supports input length that is a power of two)
	        FastFourierTransformer trans=new FastFourierTransformer(DftNormalization.UNITARY);
	        resultsComplex = trans.transform(dataInWindow,TransformType.FORWARD);		
	        usefulFFTLength = resultsComplex.length / 2;   //Divide by two because the amplitude of FFT is symmetric and the highest frequency is in the middle of x-axis
		} else {
			if (extraPowerOf2Padding > 1)
				dataInWindow = Arrays.copyOf(dataInWindow, extraPowerOf2Padding*dataInWindow.length);
			//Using JTransform to compute FFT (supports any input length)
	        DoubleFFT_1D doubleFFT = new DoubleFFT_1D(dataInWindow.length);
	        double[] resultsFFT = Arrays.copyOf(dataInWindow, dataInWindow.length);
	        doubleFFT.realForward(resultsFFT);
	        resultsComplex = new Complex[resultsFFT.length / 2];
	        
	        for (int i=0; i < resultsComplex.length; i++) {
	        	if (i*2 + 1 < resultsFFT.length)
	        		resultsComplex[i] = new Complex(resultsFFT[i*2], resultsFFT[i*2 + 1]);
	        	else
	        		resultsComplex[i] = new Complex(resultsFFT[i*2], 0.0);
	        }
	        usefulFFTLength = resultsComplex.length;
		}

        double lenDouble = (double)dataInWindow.length;  //Use double to avoid integer overflow when the length is large
        scalingParam = 1.0 / (lenDouble * lenDouble);  //TODO Check the correctness of this scaling factor (theoretically). Checking with the test code seems to confirm that this is correct.
        
		if(padToPowerOf2) {
//	        freqStep =  1.0/ (double)usefulFFTLength * targetSamplingRate / 2.0;  //Max. frequency is recording.getSamplingRate() / 2.0 due to Nyquist sampling theorem.
	        freqStep =  (double)targetSamplingRate / usefulFFTLength / 2.0 ;  //Max. frequency is recording.getSamplingRate() / 2.0 due to Nyquist sampling theorem.
	        totalFFTLength = usefulFFTLength;
		} else {
	        //Max. frequency is recording.getSamplingRate() / 2.0 due to Nyquist sampling theorem.
	        //Note: Use original sampling rate here because new sampling rate will be obtained through zero padding in the frequency domain.
	        freqStep =  1.0/ (double)usefulFFTLength * samplesPerSecond / 2.0;  
	        totalFFTLength = (int) (targetSamplingRate / 2.0 / freqStep);
	        
        
		}
        
        
        
        // Fill frequency array.
//        double[] fftFreq= new double[totalFFTLength];
//        for (int i=0; i<fftFreq.length; i++) 
//        	fftFreq[i] = i * freqStep;
        Vector fftFreq = new Vector(0, freqStep, totalFFTLength);

        // Fill power array
        double[] fftPower = new double[totalFFTLength];
        double thresh = 0.0;
        for (int i=0; i<fftPower.length; i++) {
        	if (i < resultsComplex.length) {
        		double amp = resultsComplex[i].abs();
		        fftPower[i] = amp * amp * scalingParam;
		        thresh = Math.max(thresh, fftPower[i]);  //Compute threshold
        	} 
        }
        
        // For frequency components that are below threshold or outside the frequency 
        // range of original sampling rate, round up to threshold
        // This is useful when taking the log of the resulting values.  Consider a value that is 
        // on the order of 1E-20 togther with most other values on the order of 1E-6. The logged
        // value will greatly extend the range which may reduce sensitivity for some computations.
        thresh /= maxPeakToNoiseRation;
        for (int i=0; i<fftPower.length; i++) {
        	if ((fftPower[i] < thresh) || (i >= resultsComplex.length)) {
        		fftPower[i] = thresh;
        	}
        }        
        
        return new Signal2D(fftFreq, fftPower);
	}
	
	
	
	

}
