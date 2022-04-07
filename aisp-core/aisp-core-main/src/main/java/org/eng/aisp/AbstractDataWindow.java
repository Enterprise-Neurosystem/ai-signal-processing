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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.eng.cache.Cache;
import org.eng.cache.IMultiKeyCache;
import org.eng.util.InstanceIdentifiedObject;
import org.eng.util.Vector;

/**
 * A generic data window providing the base capabilities around time, samples, and independent data for IDataWindow implementations.
 * Subclasses will generally extend this to define the type of data in the window. 
 * <p>
 * <b>Note</b>: We would have liked to call this class AbstractSynchronizedWindow and extract the non-synchronized portion to a
 * a superclass, but for backwards compatibility with old serializations, it is being left as is.  5/9/2019 dawood
 * <p>
 * @author dawood
 * @param <WINDATA>
 */
public abstract class AbstractDataWindow<WINDATA> extends InstanceIdentifiedObject implements Serializable, IDataWindow<WINDATA> {

	private static final long serialVersionUID = -8108577615473348361L;

	public final static String START_MSEC_FIELD_NAME = "startTimeMsec";
	public final static String END_MSEC_FIELD_NAME = "endTimeMsec";
	
	// All these names need to match their getter methods so that JSON de/serialization works.
	protected final double startTimeMsec;	// msec
	protected final double endTimeMsec;			// msec
	
	/** 
	 * The number of data logical samples divided by the duration of the window in seconds 
	 * So, in the case where WINDATA is double[], this would be (double[].length - 1)  / (end - start)
	 * WINDATA may be an array containing multi-dimensional samples so that this sampling rate is
	 * independent of the dimensionality of the samples.  That is, this really is the number
	 * samples, regarless of dimensionality of a sample, per second.
	 * Truth is, this field should probably be moved down in the class hierarchy since the structure of WINDATA is unknown at this level.
	 * However, this works as long as WINDATA is an array  of something.
	 */
	protected final double samplesPerSecond;

;
	/** Optional independent data in 1:1 correspondence with the data */
	protected final Vector independentVector;

	private transient double[] autoGeneratedIndependentData = null;


	protected static double getSamplesPerSecond(double startMsec, double endMsec, long sampleCount ) {
		if (sampleCount < 1)
			return 0;
		if (startMsec == endMsec)
			throw new RuntimeException("Can't compute sampling rate when start and end times are the same.");
		double s = 1000.0 * (sampleCount) / (endMsec - startMsec);
		return (s); 
	}

	/**
	 * Get the number of samples (irrespective of sample length).
	 * @param durationMsec
	 * @param samplesPerSecond
	 * @param overShoot
	 * @return
	 */
	public static int getSampleCount(double durationMsec, double samplesPerSecond, boolean overShoot) {
		int samples = (int)Math.floor(durationMsec * (samplesPerSecond / 1000.)); 	 // +1 to cover the duration.
		double resultingDurationMsec = (samples) / (samplesPerSecond / 1000);
		if (resultingDurationMsec <= durationMsec || (resultingDurationMsec > durationMsec && overShoot))
			return  samples;
		else 
			return samples-1;
	}

	
	private static int getSampleCount(double startTimeMsec, double endTimeMsec, double samplesPerSecond) {
		return getSampleCount(endTimeMsec - startTimeMsec, samplesPerSecond, false);
	}


	/**
	 * 
	 * @param startTimeMsec msec
	 * @param endTimeMsec msec
	 * @param samplesPerSecond  samples per second
	 * @param independentData optionally specifies the independent variables that map to the
	 *            dependent data. May be null, in which case a time-based array
	 *            will be generated internally.
	 */
	protected AbstractDataWindow(double startTimeMsec, double endTimeMsec, double samplesPerSecond, Vector independentData) {
		super();
		if (endTimeMsec < startTimeMsec)
			throw new IllegalArgumentException("end time must be larger than or equal to start time");
		if (samplesPerSecond <= 0)
			throw new IllegalArgumentException("samplesPerSecond must be a positive number");
			
		this.startTimeMsec = startTimeMsec;
		this.endTimeMsec = endTimeMsec;
		this.samplesPerSecond = samplesPerSecond;
//		if (context != null)
//			this.context.putAll(context);
		this.independentVector = independentData;
		int sampleSize = getSampleCount(startTimeMsec, endTimeMsec, samplesPerSecond); 

		if (independentVector != null && getSamplesPerSecond(startTimeMsec, endTimeMsec, independentVector.length()) != samplesPerSecond) 
			throw new IllegalArgumentException("length of regular independent data array (" + independentVector.length()
					+ ") is not the expected size(" + sampleSize + ")");

	}
	
	@Override
	public int getSampleSize() {
		return getSampleCount(startTimeMsec, endTimeMsec, samplesPerSecond); 
	}
	
	/**
	 * @return the samplingRate
	 */
	@Override
	public double getSamplingRate() {
		return samplesPerSecond; 
	}
	

	/**
	 * @return the startTime
	 */
	@Override
	public double getStartTimeMsec() {
		return startTimeMsec;
	}

	/**
	 * @return the endTime
	 */
	@Override
	public double getEndTimeMsec() {
		return endTimeMsec;
	}



	private static IMultiKeyCache cache = Cache.newManagedMemoryCache();
	
	/**
	 * Create/find a cached sub-window using the given logical (independent of sample dimensionality) sample indices.
	 * @param startSampleIndex first logical index of data to be taken from WINDATA as provided by {@link #getIndex(double, boolean)}.
	 * @param endSampleIndex last logical index, exclusive,  unless start == end as provided by {@link #getIndex(double, boolean)}.
	 * @return
	 */
	private IDataWindow<WINDATA> subWindowByIndex(int startSampleIndex, int endSampleIndex) {
		// Remember that the end index is exclusive, unless start==end.

		if (startSampleIndex < 0 || endSampleIndex < 0)
			throw new IllegalArgumentException("one or both of start and end index is less than 0");
		if (startSampleIndex > endSampleIndex)
			throw new IllegalArgumentException("start index must be less or equal to end index");
		int sampleCount = getSampleSize();
		if (startSampleIndex > sampleCount)
			throw new IllegalArgumentException("start index is too large");
		if (endSampleIndex > sampleCount)
			endSampleIndex = sampleCount;
		
		IDataWindow<WINDATA> clip = (IDataWindow)cache.get(this.getInstanceID(), startSampleIndex, endSampleIndex);
		if (clip != null)
			return clip;

		double newStartTime = this.getStartTimeMsec() + 1000.0 * startSampleIndex / getSamplingRate();
		clip = newSubWindow(newStartTime, startSampleIndex, endSampleIndex);
//		AISPLogger.logger.info("[" + startSampleIndex + "," + endSampleIndex + "] -> [" 
//					+ clip.getStartTimeMsec() + ", " + clip.getEndTimeMsec() + " (" + clip.getDurationMsec() + " msec)");
		cache.put(clip, this.getInstanceID(), startSampleIndex, endSampleIndex);
		return clip;
	}
	



	private transient int maxIndex = 0;
	
	/**
	 * Get the <b>logical</b> index of the datum corresponding to the given absolute time.
	 * A sample may be multi-dimensional.  The index returned is the index into the possibly
	 * multi-dimensional data, w/o regard to the sample dimensionality.  For example,
	 * @param msecOffset offset into this instance in units of the start and end times.  
	 * greater or equal to start time and less or equal to end time.
	 * @param isLowerBound if true, this is the first index to use, if false then this is 
	 * the last index, exclusive, for the given  offset. 
	 * @return -1 if offset is outside this window, otherwise the logical index as requested.
	 */
	protected int getIndex(double msecOffset, boolean isLowerBound) {
//		double start = this.getStartTimeMsec();
//		double end  = this.getEndTimeMsec();
//		double span = end - start;
		if (msecOffset < 0 || msecOffset > this.getDurationMsec())
			return -1;

		// Logical sampling rate
		double samplesPerMsec = getSamplingRate() / 1000.0; 

		int index;
		if (isLowerBound) { 	// If lower bound of a window, then compute the next index >= this time.
			index = (int)(msecOffset * samplesPerMsec + .5);
			if (maxIndex == 0) {
				double duration = this.getDurationMsec();
				maxIndex = (int)(duration * samplesPerMsec) - 1;
			}
			if (index > maxIndex)
				index = maxIndex;
		} else {				// If an upper bound of a window, then compute the index <= this time.
			index = (int)(msecOffset * samplesPerMsec) ; 	// Index of 1st sample after requested offset.
		}
		return index;
		
	}
	
	@Override
	public IDataWindow<WINDATA> subWindow(double startMsec, double endMsec) {
//		AISPLogger.logger.info("startMsec = " + startMsec + ", endMsec=" + endMsec );
//		if (startMsec == 2000)
//			startMsec = 2000;
		double start   = getStartTimeMsec();
		int startIndex = getIndex(startMsec - start, true);		// inclusive
		int endIndex   = getIndex(  endMsec - start, false);	// exclusive
		if (startIndex < 0 || endIndex < 0)
			return null;
		if (endIndex <= startIndex)
			endIndex = startIndex+1;
		return this.subWindowByIndex(startIndex, endIndex);		// 	end index is exclusive
	}
	
	@Override
	public IDataWindow<WINDATA> subWindow2(double startOffsetMsec, double endOffsetMsec) {
		if (startOffsetMsec < 0 || startOffsetMsec > getDurationMsec())
			throw new IllegalArgumentException("start offset must be 0 or larger and less than the duration");
		if (endOffsetMsec < 0   ||  endOffsetMsec > getDurationMsec())
			throw new IllegalArgumentException("end offset must be larger than 0 and less than the duration");
		if (endOffsetMsec <= startOffsetMsec)
			throw new IllegalArgumentException("end must be larger than start)");
		return subWindow(this.startTimeMsec + startOffsetMsec, this.startTimeMsec + endOffsetMsec);
	}

	/**
	 * Used by {@link #subWindow(int, int)} to create a new instance of a window
	 * that has the new start time and given data using the given logical indexes into the data. 
	 * The new window is a subwindow
	 * of this instance and so should, in general, copy the sampling rate and
	 * context from this instance. The new end time can be computed from the new
	 * start time, sampling rate and amount of new data.
	 * 
	 * @param newStartMsec
	 * @param startSampleIndex first logical index (0-based) of sampled data to include in new window.
	 * @param endSampleIndex last logical index, exclusive, of the sampled data to include in the window.
	 * @return
	 */
	protected abstract IDataWindow<WINDATA> newSubWindow(double newStartMsec, int startSampleIndex, int endSampleIndex);



	@Override
	public List<? extends IDataWindow<WINDATA>> splitOnTime(double durationMsec, boolean keepPartialWindow) {
		int indexCount = getIndex(durationMsec, true);		// inclusive gives us the count of indices per window 
		int lastIndex = this.getSampleSize(); 				// Exclusive
		int startIndex = 0;
		List<IDataWindow<WINDATA>> windowList = new ArrayList<IDataWindow<WINDATA>>();
		boolean done = false;
		do {
			int endIndex = startIndex + indexCount;	// exclusive
			if (endIndex > lastIndex) {
				if (keepPartialWindow) {
					endIndex = lastIndex;
					done = true;
				} else {
					break;
				}
			}
			if (endIndex - startIndex > 2) {		// 0 or 1 length window. probably the last
				IDataWindow<WINDATA> dw = this.subWindowByIndex(startIndex, endIndex);
				windowList.add(dw);
			}
			startIndex = endIndex;
		} while (!done);
		return windowList;
		
	}


	/**
	 * Generate an array of times based on start time and sampling rate
	 * 
	 * @return array of length equal to {@link #getData(int)}.length
	 */
	private double[] generateIndependentData() {
		double delta = 1000.0 / getSamplingRate();
		double[] values = new double[getSampleSize()];
		values[0] = this.getStartTimeMsec();
		for (int i = 1; i < values.length; i++)
			values[i] = values[i - 1] + delta;

		return values;
	}

	@Override
	public double[] getIndependentValues() {
		double[] r;
		if (this.independentVector == null) {
			if (autoGeneratedIndependentData == null)
				autoGeneratedIndependentData = generateIndependentData();
			r = autoGeneratedIndependentData;
		} else { 
			r = independentVector.getVector();
		}
		return r;
	}
	
	@Override
	public double getDurationMsec() {
		return getEndTimeMsec() - getStartTimeMsec();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result; //  + ((context == null) ? 0 : context.hashCode());
		long temp;
		temp = Double.doubleToLongBits(endTimeMsec);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((independentVector == null) ? 0 : independentVector.hashCode());
		temp = Double.doubleToLongBits(samplesPerSecond);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(startTimeMsec);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AbstractDataWindow))
			return false;
		AbstractDataWindow other = (AbstractDataWindow) obj;
		if (Double.doubleToLongBits(endTimeMsec) != Double.doubleToLongBits(other.endTimeMsec))
			return false;
		if (independentVector == null) {
			if (other.independentVector != null)
				return false;
		} else if (!independentVector.equals(other.independentVector))
			return false;
		if (Double.doubleToLongBits(samplesPerSecond) != Double.doubleToLongBits(other.samplesPerSecond))
			return false;
		if (Double.doubleToLongBits(startTimeMsec) != Double.doubleToLongBits(other.startTimeMsec))
			return false;
		return true;
	}

	/**
	 * Implemented as defined by the interface but adds caching to avoid create duplicate instances, which 
	 * would throw off feature extracting caching.  Calls {@link #uncachedPad(double, org.eng.aisp.IDataWindow.PadType)}
	 * in the sub-class to create the padded instance when not found in the cache. 
	 */
	@Override
	public IDataWindow<WINDATA> resize(double durationMsec, org.eng.aisp.IDataWindow.PadType padType) {
		IDataWindow<WINDATA> resized; 
		if (durationMsec == this.getDurationMsec()) {
			return this;
		} else if (durationMsec < this.getDurationMsec()) {
			// Don't cache this since subWindow() is already doing caching.
			resized = this.subWindow(this.startTimeMsec, this.startTimeMsec + durationMsec);
		} else {
			resized = (IDataWindow<WINDATA>) cache.get(this.getInstanceID(), durationMsec, padType);
			if (resized == null) {
				resized = uncachedPad(durationMsec, padType);
				cache.put(resized,  this.getInstanceID(), durationMsec, padType);
			}
		}
		return resized;
		
	}

	/**
	 * Create a new window of the requested length in which the data from this instance are the first samples in the returned 
	 * window and the remaining samples are padded according to padType.
	 * @param durationMsec the length of the returned window.  This will always be larger than the instance calling this method.
	 * @param padType
	 * @return null if padding is not supported/needed (for example SoundClipMetaData). 
	 */
	protected abstract IDataWindow<WINDATA> uncachedPad(double durationMsec, org.eng.aisp.IDataWindow.PadType padType); 

	@Override
	public String toString() {
		return "AbstractDataWindow [startTimeMsec=" + startTimeMsec + ", endTimeMsec=" + endTimeMsec + ", samplesPerSecond="
				+ samplesPerSecond +  ", independentVector=" + independentVector + "]";
	}

}