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
package org.eng.aisp.classifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPRuntime;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.pipeline.FeatureExtractionPipeline;
import org.eng.aisp.feature.processor.IFeatureProcessor;

/**
 * 
 * Extends the super class to implement {@link #classify(IDataWindow)}.
 * {@link #classify(IDataWindow)} is implemented  by first extracting the features and then calling {@link #classify(IFeature)} which must
 * be implemented by subclasses.
 * @author dawood
 *
 */
public abstract class AbstractFixedFeatureExtractingClassifier<WINDATA, FDATA> extends AbstractClassifier<WINDATA> implements IFixedClassifier<WINDATA> {
	private static final long serialVersionUID = 2771555097424261230L;


	protected List<IFeatureGramDescriptor<WINDATA,FDATA>> featureGramDescriptors;

//	private transient SubFeatureExtractor7<WINDATA,FDATA> subFeatureExtractor;
	private transient FeatureExtractionPipeline<WINDATA,FDATA> featureExtractionPipeline;

	private static <WINDATA,FDATA> List<IFeatureGramDescriptor<WINDATA,FDATA>> makeFGEList(IFeatureGramDescriptor<WINDATA,FDATA> fge) {
		List<IFeatureGramDescriptor<WINDATA,FDATA>> plist = new ArrayList<IFeatureGramDescriptor<WINDATA,FDATA>>();
		if (fge != null)
			plist.add(fge);
		return plist;
	}
	/**
	 * 
	 * @param extractor
	 * @param processor optional feature processor applied on array of features developed from sub feature extraction.
	 * @param windowSizeMsec
	 * @param windowShiftMsec
	 */
	public AbstractFixedFeatureExtractingClassifier(IFeatureExtractor<WINDATA, FDATA> extractor, IFeatureProcessor<FDATA> processor, int windowSizeMsec, int windowShiftMsec) {
		this(makeFGEList(new FeatureGramDescriptor<WINDATA,FDATA>(windowSizeMsec, windowShiftMsec, extractor, processor)));
	}

	/**
	 * 
	 * @param extractor the single feature extractor applied to the data
	 * @param processors the 0 or more feature processors applied to the extracted features. Each produces a single IFeatureGram.
	 * @param windowSizeMsec sub-window size on which features are extracted to produce the feature gram across the whole window of data.
	 * @param windowShiftMsec the window shift of sub-windows.  0 means rolling windows (i.e. no overlap).
	 */
	public AbstractFixedFeatureExtractingClassifier(List<IFeatureGramDescriptor<WINDATA,FDATA>> featureGramExtractors) {
		super();
		this.featureGramDescriptors = featureGramExtractors;
	}
	
	@Override
	public final Map<String, Classification> classify(IDataWindow<WINDATA> sample) throws AISPException {
		if (featureExtractionPipeline == null)
			featureExtractionPipeline = AISPRuntime.getRuntime().getFeatureExtractionPipeline(featureGramDescriptors);
//		IFeature<FDATA> features[] = featureExtractionPipeline.extract(sample); 
		IFeatureGram<FDATA> features[] = featureExtractionPipeline.extract(sample); 
		List<Classification> clist = classify(features);
		Map<String, Classification> cmap = new HashMap<String, Classification>();
		for (Classification c : clist) 
			cmap.put(c.getLabelName(), c);
		return cmap;
	}
	

	/**
	 * Implemented by subclasses of AbstractFixedFeatureExtractingClassifier to do the classification required by a classifier.
	 * @param feature the feature extracted from an IDataWindow and on which the classification is to be done.
	 * @return never null.
	 * @throws AISPException
	 */
	protected abstract List<Classification> classify(IFeatureGram<FDATA>[] features) throws AISPException;
	
//	@Override
//	public String showModel() {
//		return toString(); 
//	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return "AbstractFixedFeatureExtractingClassifier [featureGramDescriptors="
				+ (featureGramDescriptors != null
						? featureGramDescriptors.subList(0, Math.min(featureGramDescriptors.size(), maxLen)) : null)
				+ "]";
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((featureGramDescriptors == null) ? 0 : featureGramDescriptors.hashCode());
		return result;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof AbstractFixedFeatureExtractingClassifier))
			return false;
		AbstractFixedFeatureExtractingClassifier other = (AbstractFixedFeatureExtractingClassifier) obj;
		if (featureGramDescriptors == null) {
			if (other.featureGramDescriptors != null)
				return false;
		} else if (!featureGramDescriptors.equals(other.featureGramDescriptors))
			return false;
		return true;
	}

}
