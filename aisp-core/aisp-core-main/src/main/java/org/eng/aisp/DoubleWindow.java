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
package org.eng.aisp;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.util.VectorUtils;
import org.eng.util.Vector;

/**
 * Implements a data window holding an array of double values.
 * @author dawood
 *
 */
public class DoubleWindow extends AbstractDataWindow<double[]> implements IDoubleWindow {

	private static final long serialVersionUID = -3281224402675779573L;
	
	protected final double[] data;

	/**
	 * Create the instance to use a time-based independent variable.
	 * @param startTimeMsec
	 * @param endTimeMsec
	 * @param data
	 */
	public DoubleWindow(double startTimeMsec, double endTimeMsec,  double[] data) {
		this(startTimeMsec, endTimeMsec, (Vector)null, data);
	}	

	/**
	 * 
	 * @param startTimeMsec
	 * @param endTimeMsed
	 * @param independentData optionally specifies the independent variables that map to the dependent data.
	 * May be null, in which case a time-based array will be generated internally.
	 * @param data
	 * @param context
	 * @deprecated in favor of {@link #DoubleWindow(double, double, Vector, double[], Properties)}
	 */
	public DoubleWindow(double startTimeMsec, double endTimeMsec, double[] independentData, double[] data) {
		super(startTimeMsec, endTimeMsec, getSamplesPerSecond(startTimeMsec, endTimeMsec, data.length), independentData == null ? (Vector)null : new Vector(independentData));
		this.data = data;
	}
	
	/**
	 * 
	 * @param startTimeMsec
	 * @param independentData
	 * @param data
	 * @param samplesPerSecond
	 * @param context
	 * @deprecated in favor of {@link #DoubleWindow(double, Vector, double[], double, Properties)}
	 */
	public DoubleWindow(double startTimeMsec, double[] independentData, double[] data, double samplesPerSecond ) {
		super(startTimeMsec, startTimeMsec + data.length / samplesPerSecond * 1000.0, samplesPerSecond, independentData == null ? (Vector)null : new Vector(independentData));
		this.data = data;
	}
	
	public DoubleWindow(double startTimeMsec, double endTimeMsec, Vector independentData, double[] data) {
		super(startTimeMsec, endTimeMsec, getSamplesPerSecond(startTimeMsec, endTimeMsec, data.length), independentData);
		this.data = data;
	}
	
	public DoubleWindow(double startTimeMsec, Vector independentData, double[] data, double samplesPerSecond) {
		super(startTimeMsec, startTimeMsec + data.length / samplesPerSecond * 1000.0, samplesPerSecond, independentData);
		this.data= data;
	}

	@Override
	public int getSampleSize() {
		if (data == null)
			return 0;
		return data.length;
	}

	@Override
	public double[] getData() {
		return this.data;
	}
	

	public DoubleWindow(double startTimeMsec, double endTimeMsec, double samplingRate, Vector independentData, double[] data) {
		super(startTimeMsec, endTimeMsec, samplingRate, independentData);
		this.data = data;
	}

	@Override
	protected DoubleWindow newSubWindow(double newStartMsec, int startSampleIndex, int endSampleIndex) { 
		double[] newData;
		if (startSampleIndex == endSampleIndex) 	// zero length return buffer;
			newData  = new double[] {}; 
		else 
			newData= Arrays.copyOfRange(this.getData(), startSampleIndex, endSampleIndex);
		double newEndMsec;
		int newSampleCount = endSampleIndex - startSampleIndex;
		if (newSampleCount <= 0)
			newEndMsec = 0;
		else
			newEndMsec = newStartMsec + 1000.0 * newSampleCount / getSamplingRate();
		DoubleWindow w = new DoubleWindow(newStartMsec, newEndMsec, newData );
		return w;
	}

	@Override
	protected DoubleWindow uncachedPad(double durationMsec, org.eng.aisp.IDataWindow.PadType padType) {
		double[] newIndependentData = null;	// Let the super class regenerated
		double[] newData = pad(data, samplesPerSecond, durationMsec, padType);
		return new DoubleWindow(startTimeMsec, startTimeMsec + durationMsec, newIndependentData, newData);
	}
	
	/**
	 * Create a new array that is a copy of the given array and padded with data per padType.
	 * @param data data to copy and perhaps to use as padding.
	 * @param samplesPerSecond the sampling rate of the given data.
	 * @param durationMsec the duration of the returned data, according to the sampling rate.
	 * @param padType 
	 * @return a new array of size determined by the duration and sampling rate that is padded according to padType.
	 */
	public static double[] pad(double[] data, double samplesPerSecond,  double durationMsec, IDataWindow.PadType padType) {
		int newSamples = getSampleCount(durationMsec, samplesPerSecond, false);
		return pad(data, newSamples, padType);
	}

	/**
	 * @param data
	 * @param newSamples
	 * @param padType
	 * @return
	 * @throws IllegalArgumentException
	 */
	protected static double[] pad(double[] data, int newSamples, IDataWindow.PadType padType) throws IllegalArgumentException {
//		double[] newData = new double[newSamples];
		double[] newData; 
////		System.arraycopy(data, 0, newData, 0, Math.min(newData.length, data.length));	// min is being defensive.
//		System.arraycopy(data, 0, newData, 0, data.length);	
		if (padType == PadType.ZeroPad) {
//			// Just fill the end of newData with 0's probably already is, but just in case.
//			Arrays.fill(newData, data.length, newData.length, 0.0);
			newData = VectorUtils.zeroPad(data, newSamples);
		} else if (padType == PadType.DuplicatePad) {
			newData = VectorUtils.duplicatePad(data, newSamples);
//			// Copy (as many times as needed) this instance's data[] onto the end of newData.
//			int srcIndex = 0;
//			for (int i=data.length ; i<newData.length ; i++) {
//				newData[i] = data[srcIndex];
//				srcIndex++;
//				if (srcIndex == data.length)
//					srcIndex = 0;
//			}
		} else {
			throw new IllegalArgumentException("Unexpected/unsupported pad type: " + padType);
		}
		return newData;
	}

	/**
	 * Concatenate 2 or more windows into a new window.
	 * @param <DW>
	 * @param clipList
	 * @param dwFactory the factory used to create the new instance that is returned.
	 * @return never null.
	 */
	public static <DW extends IDataWindow<double[]>> DW concatWindows(List<DW> clipList, IDataWindowFactory<double[], DW> dwFactory) {
		double[] mergedData = null;
//		double samplingRate = 0;
		double duration = 0;
		for (DW clip : clipList) {
			double[]  data = clip.getData();
			if (mergedData != null) {
//				if (samplingRate != clip.getSamplingRate())
//					throw new IllegalArgumentException("Sampling rates are not the same for these clips");
				int newDataLen = mergedData.length+ data.length;
				double[] newData = new double[newDataLen];
				System.arraycopy(mergedData, 0, newData, 0, mergedData.length);
				System.arraycopy(data, 0, newData, mergedData.length, data.length);
				mergedData = newData;
			} else {
//				samplingRate = clip.getSamplingRate();
				mergedData = data; 
			}
			duration += clip.getDurationMsec();
		}
//		return new SoundClip(0,duration, mergedData);
		return dwFactory.newDataWindow(0, duration, mergedData);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(data);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof DoubleWindow))
			return false;
		DoubleWindow other = (DoubleWindow) obj;
		if (!Arrays.equals(data, other.data))
			return false;
		return true;
	}


//	@Override
//	public DoubleWindow append(IDataWindow<double[]> window) {
//		if (this.getSamplingRate() != window.getSamplingRate())
//			throw new IllegalArgumentException("Sampling rates must be the same.");
//		int  newDataLen = this.data.length + window.getData().length;
//		double newData[] = Arrays.copyOf(this.data, newDataLen);
//		System.arraycopy(window.getData(), 0, newData, this.data.length, window.getData().length);
//		double newDurationMsec = 1000.0 * this.getSamplingRate() / newData.length; 
//		return new DoubleWindow(this.startTimeMsec, this.startTimeMsec + newDurationMsec, newData);
//	}

//	/** 
//	 * Caches the {@link #hashCode()} result.
//	 */
//	private transient long hashData = 0;
//
//	@Override
//	public long hashData() {
//		if (hashData == 0) {
//			double[] data = this.getData();
//			hashData = Arrays.hashCode(data);
//		}
//		return hashData;
//	}

}
