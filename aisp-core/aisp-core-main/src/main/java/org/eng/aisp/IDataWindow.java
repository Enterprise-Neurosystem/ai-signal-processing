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

import java.io.Serializable;
import java.util.List;

import org.eng.util.IInstanceID;

/**
 * Defines the base functionality for a window of generic data.
 * Data windows have a begin and start time.
 * The interface anticipates multi-variate data through 0-based indexing to get the data, sampling sizes and sampling rates.
 * 
 * @author dawood
 *
 * @param <DATA>
 */
public interface IDataWindow<DATA> extends IInstanceID, Serializable {

	/**
	 * Value passed to {@link IDataWindow#resize(double, PadType)}. 
	 * @author dawood
	 *
	 */
	public enum PadType {
		/** Copy the original data into the padding as many times as needed */
		DuplicatePad,
		/** No padding is done.  IDataWindow.pad() does not support this. */ 
		NoPad,	
		/** Pad the added data with zeros. */
		ZeroPad
	}

	DATA getData();

	/**
	 * Get the time corresponding to the beginning of the data window.
	 * Units are undefined, but strongly recommended to be milliseconds since the epoch.
	 * @return number of milliseconds since the time 00:00:00 UTC on January 1, 1970.
	 */
	double getStartTimeMsec();

	/**
	 * Get the time corresponding to the endo of the data window.
	 * Units are undefined, but strongly recommended to be milliseconds since the epoch.
	 * @return number of milliseconds since the time 00:00:00 UTC on January 1, 1970.
	 */
	double getEndTimeMsec();

	/**
	 * Get the length of the clip in milliseconds.
	 * @return 0 or larger.
	 */
	double getDurationMsec();

//	/**
//	 * Get any optional context that may be relevant to the data window.
//	 * @return null if no context is available for this window.
//	 */
//	Properties getContext();

	/**
	 * Split this window into 1 or more consecutive sub-windows of the given length specified in time. 
	 * All context is copied to the new windows.  
	 * Start and stop times are adjusted accordingly in the new windows.
	 * @param durationMsec length of each window in milliseconds. 
	 * @param keepPartialWindow if true, then the last window may be made of up less
	 * than the requested number of samples.  If false, then a partial last window is
	 * not included in the returned list.
	 * @return a list of consecutive windows that, if possible, overlap at their start and end and the are as close to the given duration
	 * as possible given the window's sampling rate.
	 * @throws IllegalArgumentException if samplesPerWindow 0 or less, or iis larger than this window.
	 */
	List<? extends IDataWindow<DATA>> splitOnTime(double durationMsec, boolean keepPartialWindow);

	/**
	 * Get a subwindow with the given start and end time.
	 * If either time does not fall exactly on a sample time, then only samples
	 * inside, inclusive, of the given times are included in the returned window.
	 * @param startMsec start time in milliseconds of resulting window in the same units returned by {@link #getStartTimeMsec()}, inclusive.
	 * @param endMsec end time in milliseconds of resulting window in the same units returned by {@link #getStartTimeMsec()}, inclusive.
	 * @return null if either startMsec or endMsec are outside the window.
	 */
	IDataWindow<DATA> subWindow(double startMsec, double endMsec);
	
	/**
	 * Get a subwindow with the given offsets into this window (an alternative to {@link #subWindow(double, double)}).
	 * If either time does not fall exactly on a sample time, then only samples
	 * inside, inclusive, of the given times are included in the returned window.
	 * @param startMsec 0-based time offset in milliseconds into this window from which to start the returned window, inclusive.  Must be 0 or larger and less
	 * than the duration of this window.
	 * @param endMsec 0-based time offset in milliseconds into this window that defines the end of the returned window, inclusive.  Must be 0 or larger and less
	 * than the duration of this window. 
	 * @param endMsec end time of resulting window in the same units returned by {@link #getStartTimeMsec()}, inclusive.
	 * @return null if either startMsec or endMsec are outside the window.
	 */
	IDataWindow<DATA> subWindow2(double startOffsetMsec, double endOffsetMsec);

	/**
	 * Create a new window based on this instance that is of the given duration. 
	 * If the current window is larger than the requested size, then a sub clip starting at the
	 * front is generated.
	 * If the current window is smaller than the requested size, then then padding is added
	 * to create a new window of the requested length.
	 * @param durationMsec length of time in milliseconds the returned window is to cover.
	 * @param padType one of {@link PadType#ZeroPad} or {@link PadType#DuplicatePad} controlling
	 * what data is used to pad the data.
	 * @return a new window with the same start time, and sampling rate as this instance and with the requested duration.
	 */
	IDataWindow<DATA> resize(double durationMsec, PadType padType);

	/**
	 * Get the independent values on which the dependent data values in this window dependent.
	 * In many cases this may be time and derived from the start time, end time and sampling rate.
	 * However, some implementations (e.g. features such as FFT and DCT) may define their own.
	 * @return
	 */
	double[] getIndependentValues();

	/**
	 * Get the size of the window in the number of samples.
	 * @return the number of samples in this window.
	 */
	int getSampleSize();

	/**
	 * Get the rate (samples/second) at which the data in this window was sampled.
	 * @return  samples per second.  0 if the window has 0 or 1 datum.
	 */
	double getSamplingRate();

}