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
package org.eng.aisp.feature;

import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;

/**
 * Implements the full feature extraction pipeline to produce an array of sub-features from a given IDataWindow or ILabeledDataWindow.
 * This is a core class and generally should be used during both training and classification.
 * <p>
 * The process involves the following distinct steps for a given IDataWindow (or ILabeledDataWindow) instance
 * <ol>
 * <li> Optionally breaking the window into sub-windows defined by a window size and shift, in milliseconds.
 * <li> Applying a single instance of IFeatureExtractor to all sub-windows to create an array of IFeature (or ILabeledFeature) instances.
 * <li> Optionally applying a single IFeatureProcessor to the array of IFeature instances to produce another array of IFeature (or ILabeledFeature) instances.
 * </ol>
 * This results in an array of IFeature (or ILabeledFeature) instances.  The size of this array is defined by the number of sub-windows and for each
 * sub-window there is exactly one IFeature (or ILabeledFeature) instance.
 * 
 * @author dawood
 *
 * @param <WINDATA>
 * @param <FDATA>
 */
public class FeatureGramDescriptor<WINDATA,FDATA> implements IFeatureGramDescriptor<WINDATA, FDATA> {

	private static final long serialVersionUID = -1797485898385673869L;
	protected final int windowSizeMsec;
	protected final int windowShiftMsec;
	protected final IFeatureExtractor<WINDATA,FDATA> extractor;
	protected final IFeatureProcessor<FDATA> processor;

	/**
	 * Defines the pipeline.
	 * @param windowSizeMsec the size of the subwindows into which each labeled data window is divided into.  Set to 0 to not subdivide the labeled data windows.
	 * @param windowShiftMsec the amount of shift in time between subsequent subwindows.  Typical values are {@link #windowSizeMsec} or {@link #windowSizeMsec}/2.  
	 * @param extractor extracts feature from an individual window.
	 * @param processor feature processor that operations across the features from a whole window.
	 */
	public FeatureGramDescriptor(int windowSizeMsec, int windowShiftMsec, IFeatureExtractor<WINDATA, FDATA> extractor, IFeatureProcessor<FDATA> processor) {
		super();
		if (windowSizeMsec < 0)
			throw new IllegalArgumentException("Window size must be greater or equal to 0");
		if (windowShiftMsec < 0)
			throw new IllegalArgumentException("Window size must be greater or equal to 0");
		if (extractor == null)
			throw new IllegalArgumentException("Feature extractor can not be null");
		if (windowShiftMsec == 0)
			windowShiftMsec = windowSizeMsec;
			
		this.windowSizeMsec = windowSizeMsec;
		this.windowShiftMsec = windowShiftMsec;
		this.extractor = extractor;
		this.processor = processor;
	}

	
	
//	public ILabeledFeatureGram<FDATA> extract(ILabeledDataWindow<WINDATA> labeledDataWindow) {
////		AISPLogger.logger.info("Getting features from labeled window with id " + labeledDataWindow.getDataWindow().getInstanceID());
//		IFeatureGram<FDATA> features = extract(labeledDataWindow.getDataWindow());
//	    Properties labels = labeledDataWindow.getLabels();
//		return new LabeledFeatureGram<FDATA>(features,labels);
//	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((extractor == null) ? 0 : extractor.hashCode());
		result = prime * result + ((processor == null) ? 0 : processor.hashCode());
		result = prime * result + windowShiftMsec;
		result = prime * result + windowSizeMsec;
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
		if (!(obj instanceof FeatureGramDescriptor))
			return false;
		FeatureGramDescriptor other = (FeatureGramDescriptor) obj;
		if (extractor == null) {
			if (other.extractor != null)
				return false;
		} else if (!extractor.equals(other.extractor))
			return false;
		if (processor == null) {
			if (other.processor != null)
				return false;
		} else if (!processor.equals(other.processor))
			return false;
		if (windowShiftMsec != other.windowShiftMsec)
			return false;
		if (windowSizeMsec != other.windowSizeMsec)
			return false;
		return true;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FeatureGramDescriptor [windowSizeMsec=" + windowSizeMsec + ", windowShiftMsec=" + windowShiftMsec
				+ ", extractor=" + extractor + ", processor=" + processor + "]";
	}


	@Override
	public double getWindowSizeMsec() {
		return windowSizeMsec;
	}


	@Override
	public double getWindowShiftMsec() {
		return windowShiftMsec;
	}


	@Override
	public IFeatureExtractor<WINDATA, FDATA> getFeatureExtractor() {
		return this.extractor;
	}


	@Override
	public IFeatureProcessor<FDATA> getFeatureProcessor() {
		return this.processor;
	}
}
