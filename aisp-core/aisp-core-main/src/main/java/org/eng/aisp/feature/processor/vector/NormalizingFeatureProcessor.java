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
package org.eng.aisp.feature.processor.vector;

import org.eng.aisp.feature.IFeature;
import org.eng.aisp.util.MatrixUtil;

/**
 * Enables the normalization and mean removal of features.
 * Normalization can be done on the range of values or the standard deviation.
 * Normalization and mean removal can be done on the full matrix of features, on 
 * individual feature vectors, or across time.
 * Range normalization and mean remove will produce results in the range [-0.5, 0.5].
 * @author dawood
 *
 */
public class NormalizingFeatureProcessor extends AbstractVectorMappingFeatureProcessor {

	private static final long serialVersionUID = 2685100162167267778L;
	private final boolean zeroMean;
	private final boolean normalizeStddev;
	private final boolean acrossFeature;
	private final boolean acrossTime;

	/**
	 * Normalize the standard deviation, remove the mean and do it across time. 
	 */
	public NormalizingFeatureProcessor() {
		this(true,true,false, true);
	}

	/**
	 * Normalize the feature for standard deviation or value range, and/or adjustment to a zero mean.
	 * Normalization may be done across one of 3 target components
	 * <ol>
	 * <li> the whole matrix/spectrogram of features
	 * <li> the feature values for a each time slot in the matrix (i.e. each column of the feature matrix).
	 * <li> each feature coefficient across time (i.e. each row of the feature matrix).
	 * </ol>
	 * @param normalizeStddev if true, then normalize the standard deviation in the target component, otherwise normalize using the range of values in the target component.
	 * @param zeroMean if true, then set the mean of the target component to 0.
	 * @param acrossFeature if true, then normalize the feature within a time slot. 
	 * If this is true and acrossTime is true, then normalize across the whole matrix/spectrogram.
	 * @param acrossTime if true, then normalize the features across time.
	 * If this is true and acrossFeature is true, then normalize across the whole matrix/spectrogram.
	 * if both are false, then no normalization is done.
	 */
	public NormalizingFeatureProcessor(boolean normalizeStddev, boolean zeroMean, boolean acrossFeature, boolean acrossTime) {
		this.normalizeStddev = normalizeStddev; 
		this.zeroMean = zeroMean;
		this.acrossFeature = acrossFeature;
		this.acrossTime = acrossTime;
	}

	@Override
	protected double[][] map(IFeature<double[]>[] features, double[][] featureData) {
		final boolean inPlace = false;
		if (!acrossFeature && !acrossTime)
			return featureData;
		MatrixUtil.NormalizationMode mode = acrossFeature && acrossTime ? MatrixUtil.NormalizationMode.Matrix 
						: (acrossTime ? MatrixUtil.NormalizationMode.Row : MatrixUtil.NormalizationMode.Column);
//		double[][] featureDataT= MatrixUtil.transpose(featureData, false);
		featureData = MatrixUtil.normalize(featureData, normalizeStddev, zeroMean, mode, inPlace);
//		double[][] normT = MatrixUtil.transpose(featureData, false);

		return featureData;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "NormalizingFeatureProcessor [zeroMean=" + zeroMean + ", normalizeStddev=" + normalizeStddev
				+ ", acrossFeature=" + acrossFeature + ", acrossTime=" + acrossTime + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (acrossTime ? 1231 : 1237);
		result = prime * result + (normalizeStddev ? 1231 : 1237);
		result = prime * result + (acrossFeature ? 1231 : 1237);
		result = prime * result + (zeroMean ? 1231 : 1237);
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
		if (!(obj instanceof NormalizingFeatureProcessor))
			return false;
		NormalizingFeatureProcessor other = (NormalizingFeatureProcessor) obj;
		if (acrossTime != other.acrossTime)
			return false;
		if (normalizeStddev != other.normalizeStddev)
			return false;
		if (acrossFeature != other.acrossFeature)
			return false;
		if (zeroMean != other.zeroMean)
			return false;
		return true;
	}
}
