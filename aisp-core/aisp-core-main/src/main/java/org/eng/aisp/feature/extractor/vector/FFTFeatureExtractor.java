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

import org.eng.aisp.IDataWindow;
import org.eng.aisp.feature.DoubleFeature;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.util.ExtendedFFT;
import org.eng.aisp.processor.AbstractCachingWindowProcessor;
import org.eng.aisp.util.Signal2D;
import org.eng.aisp.util.VectorUtils;
import org.eng.util.Sample;
import org.eng.util.Vector;

public class FFTFeatureExtractor extends AbstractCachingWindowProcessor<IDataWindow<double[]>, IFeature<double[]>> implements IFeatureExtractor<double[],double[]>{

	private static final long serialVersionUID = 645185293977218415L;
	
	public static final int DEFAULT_MIN_HTZ = 20;
	public static final int DEFAULT_MAX_HTZ = 20000;
	public static final int DEFAULT_MAX_FFT_SIZE = 64;
	public static final boolean DEFAULT_NORMALIZE = false;
	public static final boolean DEFAULT_USE_LOG = false;
	
	public final static int DEFAULT_RESAMPLING_RATE = ExtendedFFT.DEFAULT_FFT_MAX_SAMPLING_RATE;
	
	protected final boolean normalize;
	protected int maxFFTSize;
	protected final double targetSamplingRate;
	protected final boolean useLog;
	
	private int minHtz;

	private int maxHtz;

	
	public FFTFeatureExtractor(boolean norm, int maxFFTSize ) {
		this(norm, maxFFTSize, DEFAULT_RESAMPLING_RATE, DEFAULT_MIN_HTZ, DEFAULT_MAX_HTZ);
	}


	/**
	 * A convenience on {@link #FFTFeatureExtractor(boolean, boolean, int, double, int, int)} with useLog={@value #DEFAULT_USE_LOG}.
	 * @deprecated in favor of {@link #FFTFeatureExtractor(double, int, int, boolean, boolean, int)}
	 */
	protected FFTFeatureExtractor(boolean norm, int maxFFTSize, double targetSamplingRate, int minHtz, int maxHtz) {
		this(DEFAULT_USE_LOG, norm, maxFFTSize, targetSamplingRate, minHtz, maxHtz);
		
	}
	/**
	 * 
	 * @deprecated in favor of {@link #FFTFeatureExtractor(double, int, int, boolean, boolean, int)} for arguments being more consistent with other extractors. 
	 */
	public FFTFeatureExtractor(boolean useLog, boolean norm, int maxFFTSize, double targetSamplingRate, int minHtz, int maxHtz) {
		this(targetSamplingRate,  minHtz, maxHtz, norm, useLog,  maxFFTSize);
	}

	
	/**
	 * 
	 * @param targetSamplingRate if 0, then don't resample the input.
	 * @param minHtz 
	 * @param maxHtz
	 * @param norm normalize the signal to a fixed range prior to taking the log, if any.
	 * @param useLog  take the of the power values if true.
	 * @param maxFFTSize
	 */
	public FFTFeatureExtractor(double targetSamplingRate,  int minHtz, int maxHtz, boolean norm, boolean useLog, int maxFFTSize  ) {
		super();
		if (maxFFTSize < 0)
			throw new IllegalArgumentException("FFT size must be greater than or equal to 0");
		if (minHtz < 0)
			throw new IllegalArgumentException("Min frequency must be non-negative");
		if (maxHtz < 0)
			throw new IllegalArgumentException("Max frequency must be non-negative");
		if (targetSamplingRate < 0)
			throw new IllegalArgumentException("Target sampling reate must be non-negative");
		if (minHtz >= maxHtz)
			throw new IllegalArgumentException("Minimum frequency must be smaller than the maximum frequency");
		this.useLog = useLog;
		this.normalize=norm;
		this.maxFFTSize = maxFFTSize;
		this.targetSamplingRate = targetSamplingRate;
		this.minHtz = minHtz;
		this.maxHtz = maxHtz;

	}
	public FFTFeatureExtractor(boolean normalize, int maxFFTSize, double targetSamplingRate) {
		this(normalize, maxFFTSize, targetSamplingRate,DEFAULT_MIN_HTZ, DEFAULT_MAX_HTZ);
	}

	public FFTFeatureExtractor(int maxFFTSize) {
		this(DEFAULT_NORMALIZE, maxFFTSize);
	}	
	public FFTFeatureExtractor(boolean norm) {
		this(norm, DEFAULT_MAX_FFT_SIZE);
	}	
	
	public FFTFeatureExtractor() {
		this(DEFAULT_NORMALIZE, DEFAULT_MAX_FFT_SIZE);
	}



	@Override
	protected IFeature<double[]> applyImpl(IDataWindow<double[]> window) {
		// Generate an FFTFeature.
		if (!(window instanceof IDataWindow))
			throw new IllegalArgumentException("window must be an instance of " + IDataWindow.class.getName());
		IDataWindow<double[]> recording = (IDataWindow<double[]>) window;

		Signal2D power;
		if (targetSamplingRate > 0)
			power = ExtendedFFT.power(recording.getData(), recording.getSamplingRate(), targetSamplingRate, ExtendedFFT.MAX_PEAK_TO_NOISE_FLOOR_RATIO);
		else
			power = ExtendedFFT.power(recording, ExtendedFFT.MAX_PEAK_TO_NOISE_FLOOR_RATIO);
        double[] fftPower = power.getYValues(); 
		fftPower[0] = 0;	// Remove DC component.	 TODO: is it better to normalize the full window mean to 0

		power = power.trimX(minHtz, maxHtz);
        Vector fftFreq = power.getXValues(); 
        fftPower = power.getYValues(); 
		
		if (maxFFTSize > 0 && fftPower.length != maxFFTSize) {
			double newPower[] = new double[maxFFTSize];
			double newFreq[] = new double[maxFFTSize];
			double[] freq = fftFreq.getVector();
			if (fftPower.length > maxFFTSize) { 
				if (!Sample.downSample(fftPower, newPower, true) || !Sample.downSample(freq, newFreq, true))
//					AISPLogger.logger.warning("Double.NaN found in FFT results");
					throw new RuntimeException("NaN found in FFT results");
			} else   {
				newPower = VectorUtils.interpolate(fftPower, fftPower.length, maxFFTSize);
				newFreq = VectorUtils.interpolate(freq, fftPower.length, maxFFTSize);
			}
			fftPower = newPower;
			fftFreq = new Vector(newFreq);
		}
		
		if(this.normalize) {
			fftPower = VectorUtils.normalize(fftPower, true, true, true);	// normalize stddev and zero the mean and do it in place.
		} 
        if (useLog) {
			for(int i=0; i<fftPower.length; i++) {
				fftPower[i] = Math.log10(Math.max(1e-50, fftPower[i]));  //Do not allow zero values for log operation
			}
        }
        
		
		DoubleFeature feature = new DoubleFeature(recording.getStartTimeMsec(), recording.getEndTimeMsec(), fftFreq, fftPower);

		return feature;
	}
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + maxFFTSize;
		result = prime * result + maxHtz;
		result = prime * result + minHtz;
		result = prime * result + (normalize ? 1231 : 1237);
		long temp;
		temp = Double.doubleToLongBits(targetSamplingRate);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (useLog ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof FFTFeatureExtractor))
			return false;
		FFTFeatureExtractor other = (FFTFeatureExtractor) obj;
		if (maxFFTSize != other.maxFFTSize)
			return false;
		if (maxHtz != other.maxHtz)
			return false;
		if (minHtz != other.minHtz)
			return false;
		if (normalize != other.normalize)
			return false;
		if (Double.doubleToLongBits(targetSamplingRate) != Double.doubleToLongBits(other.targetSamplingRate))
			return false;
		if (useLog != other.useLog)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "FFTFeatureExtractor [normalize=" + normalize + ", maxFFTSize=" + maxFFTSize + ", targetSamplingRate="
				+ targetSamplingRate + ", useLog=" + useLog + ", minHtz=" + minHtz + ", maxHtz=" + maxHtz + "]";
	}


}
