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

import org.eng.aisp.AISPLogger;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.feature.DoubleFeature;
import org.eng.aisp.feature.IDoubleFeature;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.extractor.IDoubleFeatureExtractor;
import org.eng.aisp.processor.AbstractCachingWindowProcessor;
import org.eng.aisp.util.VectorUtils;
import org.eng.util.Sample;

/**
 * <p>Created on 9/21/16.</p>
 *
 * @author swang
 */
public class IdentityFeatureExtractor extends AbstractCachingWindowProcessor<IDataWindow<double[]>, IFeature<double[]>> implements IDoubleFeatureExtractor {
	
	private static final long serialVersionUID = -3002886101819718505L;
	private final int targetSamplingRate;
	
	public IdentityFeatureExtractor() {
		this(0);	// no resampling

	}
	/**
	 * Support optional resampling of the data to the give sampling rate.
	 * @param targetSamplingRate if 0 or less, then no resampling is done.
	 */
	public IdentityFeatureExtractor(int targetSamplingRate) {
		this.targetSamplingRate = targetSamplingRate;
	}

	@Override
	public IDoubleFeature applyImpl(IDataWindow<double[]> recording) {

		@SuppressWarnings("deprecation")
		double[] data = recording.getData();
		if (targetSamplingRate > 0) {
			if (!(recording instanceof IDataWindow))
				throw new IllegalArgumentException("Given window must be an instance of " + IDataWindow.class.getName() + " to apply resampling.");
			double samplingRate = ((IDataWindow)recording).getSamplingRate();
			if (samplingRate == targetSamplingRate) {
				; // no op
			} else if (samplingRate < targetSamplingRate) {
				//do interpolation to get more values
				data = VectorUtils.interpolate(data, samplingRate, targetSamplingRate);
			} else {
				// down sample to get the expected number of samples.
				AISPLogger.logger.fine("Input sampling rate (" + samplingRate + " samples/sec) is higher than " + targetSamplingRate 
						+ ", downsampling to " + targetSamplingRate  + " ");

				double durationMsec = recording.getDurationMsec();
				int samples = (int)(targetSamplingRate * durationMsec / 1000.0 + .5);
				double dataInWindow[] = new double[samples];
				Sample.downSample(data , dataInWindow, true);
				data = dataInWindow;
			}
		}
		IDoubleFeature feature = new DoubleFeature(recording.getStartTimeMsec(), recording.getEndTimeMsec(), data); 

		return feature;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + targetSamplingRate;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof IdentityFeatureExtractor))
			return false;
		IdentityFeatureExtractor other = (IdentityFeatureExtractor) obj;
		if (targetSamplingRate != other.targetSamplingRate)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "IdentityFeatureExtractor [targetSamplingRate=" + targetSamplingRate + "]";
	}



}
