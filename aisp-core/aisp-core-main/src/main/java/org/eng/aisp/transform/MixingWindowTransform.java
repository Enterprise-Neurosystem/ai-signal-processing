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
package org.eng.aisp.transform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eng.aisp.AISPLogger;
import org.eng.aisp.DoubleWindow;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.SoundClip;
import org.eng.aisp.util.VectorUtils;
import org.eng.util.ISizedIterable;
import org.eng.util.OnlineStats;
import org.eng.util.Sample;

/**
 * Mixes two windows of data together using a specified weight to produce both the original and the mixed window.
 * The output is optionally volume leveled to be the same range of values as the original sound.
 * @author dawood
 *
 */
public class MixingWindowTransform extends AbstractTrainingWindowTransform<double[]> implements ITrainingWindowTransform<double[]> {
	
	private static final long serialVersionUID = 3208997314536928410L;
	private final Iterable<? extends IDataWindow<double[]>> mixins;
	/** An array of values between 0 and 1 to define the relative weight of the mixed in sounds */ 
	private final double[] ratios;
	private final boolean volumeLevelingEnabled;
	private transient int mixinCount = -1;
	
	public MixingWindowTransform(List<String> soundFilesOrDirs, double[] ratios, boolean volumeLevelingEnabled) throws IOException {
		this(SoundClip.readClips(soundFilesOrDirs), ratios, volumeLevelingEnabled);
	}

	/**
	 * Create the instance to mix in the previous window with the given ratio with a following window.
	 * @param ratio a number between 0 and 1 that represents the percentage of a  mixed in window.  The original
	 * window will be weighted (1-ratio).  Generally anything larger than .5 is discouraged as this would weight
	 * the mix in more than the original and so the labels would not really be applicable anymore.
	 * @param volumeLevelingEnabled if true, then adjust the range of the output audio signal to be the same as that of
	 * the source data into which the sounds are being mixed.  Each output is leveled separately based on
	 * the range of the source sound being mixed into.
	 */
	public MixingWindowTransform(Iterable<? extends IDataWindow<double[]>> mixins, double[] ratios, boolean volumeLevelingEnabled) {
		super(true);
		if (mixins == null) 
			throw new IllegalArgumentException("List of mixed in windows is null");
		if (ratios == null || ratios.length == 0)
			throw new IllegalArgumentException("List of ratios is null or empty");
		this.mixins = mixins;
		for (int i=0 ; i<ratios.length ; i++) {
			if (ratios[i] <= 0 || ratios[i] > 1)
				throw new IllegalArgumentException("ration value at index " + i + " is less than or equal to 0 or greater than 1");
		}
		this.ratios = ratios;
		this.volumeLevelingEnabled = volumeLevelingEnabled;
	}

	/**
	 * Mix the given window with the mixins at the specified ratios.
	 */
	@Override
	protected List<IDataWindow<double[]>> getMutatedWindows(String trainingLabel, ILabeledDataWindow<double[]> ldw) {
		List<IDataWindow<double[]>> windowList = new ArrayList<IDataWindow<double[]>>();

		if (!(ldw.getDataWindow() instanceof IDataWindow)) 
			throw new IllegalArgumentException("Labeled data window must contain an instance of " + IDataWindow.class.getName());
		IDataWindow<double[]> dataWindow = (IDataWindow)ldw.getDataWindow();

		for (IDataWindow<double[]> mixin: mixins) {
			for (double ratio: ratios) {
				double[] data = mixData(dataWindow, mixin, ratio, this.volumeLevelingEnabled);
				if (data != null) {
					IDataWindow<double[]> newDW = new DoubleWindow(dataWindow.getStartTimeMsec(), dataWindow.getEndTimeMsec(), data);
					windowList.add(newDW);
				}
			}
		}
		
		return windowList;
	}

	/**
	 * Mix the given window of data using the given ratio, into the 2nd window of data to produce a new data array.
	 * If the mixingWindow is shorter than the base window, then it is reused to mix with the rest of the base window.
	 * @param baseWindow
	 * @param mixingWindow
	 * @param ratio the ratio of the amplitude of the base window to the mixing window.  Values larger than 1 are not allowed.
	 * @param volumeLevelingEnabled if true, then adjust the range of the output audio signals to be the same as that of
	 * the source data into which the sounds are being mixed.
	 * @return An array the same length as the data[] of the given baseWindow.  null if could not be mixed (because of sampling rate differences).
	 * 
	 */
	private static double[] mixData(IDataWindow<double[]> baseWindow, IDataWindow<double[]> mixingWindow, double ratio, boolean volumeLevelingEnabled) {
		double[] baseData = baseWindow.getData();
		double[] mixingData = mixingWindow.getData();
		
		// Resample the data if necessary to get them to be the same sampling rate.
		int mixingSamplingRate = (int)mixingWindow.getSamplingRate();
		int baseSamplingRate = (int)baseWindow.getSamplingRate();
		if (baseSamplingRate > mixingSamplingRate) {
			mixingData = VectorUtils.interpolate(mixingData, mixingSamplingRate, baseSamplingRate);
		} else if (baseSamplingRate < mixingSamplingRate) {
			int newSize = (int)Math.round(mixingData.length * ((double)baseSamplingRate / mixingSamplingRate)); 
			mixingData = Sample.downSample(mixingData, newSize);
		}
		// TODO: cache the resampled mixingData.
		
		// Compute mixing weights so that the range of values is not changed.
		double baseWeight = 1.0-ratio; 
		double mixWeight = ratio; 

		// Create a new data[] that is a mix of the base data + mixinData. 
		int mixingIndex = 0;
		double[] newData = new double[baseData.length];
		for (int i=0 ; i<baseData.length ; i++) {
			newData[i] = (baseWeight * baseData[i] + mixWeight * mixingData[mixingIndex++]);
			if (newData[i] > 1.0)	// Should never happen, but just in case.
				newData[i] = 1.0;
			if (newData[i] < -1.0)	// Should never happen, but just in case.
				newData[i] = -1.0;
			if (mixingIndex >= mixingData.length)
				mixingIndex = 0;
		}
		
		if (volumeLevelingEnabled) {
			// We want to scale the new range of values into the same range as the base sound.
			OnlineStats baseStats = VectorUtils.getStatistics(baseData);
			OnlineStats newStats = VectorUtils.getStatistics(newData);
			double baseRange = baseStats.getMaximum() - baseStats.getMinimum();
			double newRange = newStats.getMaximum() - newStats.getMinimum();
			double scale =   baseRange / newRange ;	// we want scale * newRange == baseRange
			for (int i=0 ; i<newData.length ; i++) {
				newData[i]  = scale * newData[i]; 
				if (newData[i] > 1.0)	
					newData[i] = 1.0;
				if (newData[i] < -1.0)	
					newData[i] = -1.0;
			}
			
			newStats = VectorUtils.getStatistics(newData);
			newRange = newStats.getMaximum() - newStats.getMinimum();
			AISPLogger.logger.info("baseRange=" + baseRange + ", newRange=" + newRange);
		}
		return newData;
	}

	@Override
	public ITrainingWindowTransform<double[]> newInstance() {
		return new MixingWindowTransform(this.mixins, this.ratios, this.volumeLevelingEnabled);
	}

	@Override
	public synchronized int multiplier() {
		if (mixinCount < 0) {
			if (mixins instanceof Collection) {
				this.mixinCount = ((Collection)mixins).size();
			} else if (mixins instanceof ISizedIterable) {
				this.mixinCount = ((ISizedIterable)mixins).size();
			} else {
				this.mixinCount = 0;
				for (Object o : mixins) 
					mixinCount++;
			}
		}
		return mixinCount + 1;	// 1 because we are keeping the original 
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((mixins == null) ? 0 : mixins.hashCode());
		result = prime * result + Arrays.hashCode(ratios);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof MixingWindowTransform))
			return false;
		MixingWindowTransform other = (MixingWindowTransform) obj;
		if (mixins == null) {
			if (other.mixins != null)
				return false;
		} else if (!mixins.equals(other.mixins))
			return false;
		if (!Arrays.equals(ratios, other.ratios))
			return false;
		return true;
	}



	private String toString(Collection<?> collection, int maxLen) {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		int i = 0;
		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
			if (i > 0)
				builder.append(", ");
			builder.append(iterator.next());
		}
		builder.append("]");
		return builder.toString();
	}

	@Override
	public String toString() {
		final int maxLen = 8;
		return "MixingWindowTransform [mixins=" + mixins + ", ratios="
				+ (ratios != null ? Arrays.toString(Arrays.copyOf(ratios, Math.min(ratios.length, maxLen))) : null)
				+ ", keepOriginal=" + keepOriginal + "]";
	}


}
