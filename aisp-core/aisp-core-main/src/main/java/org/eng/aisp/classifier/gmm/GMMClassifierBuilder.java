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
package org.eng.aisp.classifier.gmm;

import org.eng.aisp.classifier.AbstractClassifierBuilder;
import org.eng.aisp.classifier.IFixableClassifierBuilder;

public class GMMClassifierBuilder extends AbstractClassifierBuilder<double[], double[]>  implements IFixableClassifierBuilder<double[], double[]>{
    private static final long serialVersionUID = -7613602990314448183L;
	private int numGaussians = GMMClassifier.DEFAULT_NUM_GAUSSIANS;
    protected boolean diagonalCovariance = GMMClassifier.DEFAULT_USE_DIAGONAL_COVARIANCE;
    protected double knownConfidenceCutoff = GMMClassifier.DEFAULT_UNKNOWN_THRESH_COEFF;
    private boolean useDiskCache = GMMClassifier.DEFAULT_USE_DISK_CACHE;

	
//	public GMMClassifierBuilder(String primaryTrainingLabelName, IFeatureExtractor<double[], double[]> fe) { 
//		super(primaryTrainingLabelName, fe);
//	}

	public GMMClassifierBuilder() { 
		super(GMMClassifier.DEFAULT_FEATURE_EXTRACTOR, GMMClassifier.DEFAULT_FEATURE_PROCESSOR);
		this.windowSizeMsec = GMMClassifier.DEFAULT_WINDOW_SIZE_MSEC;
		this.windowShiftMsec = GMMClassifier.DEFAULT_WINDOW_SHIFT_MSEC;
		this.transform = GMMClassifier.DEFAULT_TRANSFORMS;
	}
	
	@Override
	public GMMClassifier build() {
		return new GMMClassifier(transform, featureExtractor, featureProcessor, windowSizeMsec, windowShiftMsec, useDiskCache, 
				numGaussians, diagonalCovariance, knownConfidenceCutoff);
	}
	
	public GMMClassifierBuilder setNumGaussians(int numGaussiansToMix) {
		this.numGaussians = numGaussiansToMix;
		return this; 
	}
	
	public GMMClassifierBuilder setKnownConfidenceCutoff(double knownConfidenceCutoff) {
		this.knownConfidenceCutoff = knownConfidenceCutoff;
		return this;
	}

	public GMMClassifierBuilder setDiagonalCovariance(boolean diagonalCovariance) {
		this.diagonalCovariance = diagonalCovariance;
		return this;
	}

	public GMMClassifierBuilder setUseDiskCache(boolean useDiskCache) {
		this.useDiskCache = useDiskCache;
		return this;
	}



	/**
	 * @return the numGaussians
	 */
	public int getNumGaussians() {
		return numGaussians;
	}

	/**
	 * @return the diagonalCovariance
	 */
	public boolean isDiagonalCovariance() {
		return diagonalCovariance;
	}

	/**
	 * @return the knownConfidenceCutoff
	 */
	public double getKnownConfidenceCutoff() {
		return knownConfidenceCutoff;
	}

	/**
	 * @return the useDiskCache
	 */
	public boolean isUseDiskCache() {
		return useDiskCache;
	}
	
	

}
