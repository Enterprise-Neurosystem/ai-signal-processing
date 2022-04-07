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
package org.eng.aisp.feature.pipeline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.AISPLogger;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.LabeledFeatureGram;
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
public class FeatureExtractionPipeline<WINDATA,FDATA> {

//	protected final List<IFeatureGramDescriptor<WINDATA,FDATA>> extractorList = new ArrayList<IFeatureGramDescriptor<WINDATA,FDATA>>();
	protected final List<FeatureGramExtractor<WINDATA,FDATA>> extractorList = new ArrayList<>();

	
	public FeatureExtractionPipeline(IFeatureGramDescriptor<WINDATA, FDATA> featureGram) {
		this(Arrays.asList(featureGram));
	}

	public FeatureExtractionPipeline(List<IFeatureGramDescriptor<WINDATA, FDATA>> featureGramExtractors) {
		for (IFeatureGramDescriptor<WINDATA,FDATA> fgd : featureGramExtractors) 
			this.extractorList.add(new FeatureGramExtractor<WINDATA,FDATA>(fgd));
	}

	public ILabeledFeatureGram<FDATA>[] extract(ILabeledDataWindow<WINDATA> labeledDataWindow) {
//		AISPLogger.logger.info("Getting features from labeled window with id " + labeledDataWindow.getDataWindow().getInstanceID());
		IFeatureGram<FDATA>[] features = extract(labeledDataWindow.getDataWindow());
	    @SuppressWarnings("unchecked")
		ILabeledFeatureGram<FDATA>[] labeledFeatureGramArray = new ILabeledFeatureGram[features.length]; 
	    Properties labels = labeledDataWindow.getLabels();
		for (int i=0 ; i<features.length ; i++) {
			labeledFeatureGramArray[i] = new LabeledFeatureGram<FDATA>(features[i],labels);
		}
	    return labeledFeatureGramArray;
	}


	public IFeatureGram<FDATA>[] extract(IDataWindow<WINDATA> dataWindow) {
		IFeatureGram<FDATA>[] fgArray = new IFeatureGram[extractorList.size()];
		int index = 0;
		for (FeatureGramExtractor<WINDATA, FDATA> sfe : extractorList) {
			IFeatureGram<FDATA> fg = sfe.extractFeatureGram(dataWindow);
			fgArray[index] = fg;
			index++;
		}
		return fgArray;
		
	}



	private void showFeatures(String prefix, IDataWindow<WINDATA> dataWindow, IFeature<FDATA>[] featureArray) {
		List<Long> ids = new ArrayList<Long>();
		for (IFeature f : featureArray) {
			ids.add(f.getInstanceID());
		}
		AISPLogger.logger.info(prefix + " window id: " + dataWindow.getInstanceID() + " feature ids=" + ids);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((extractorList == null) ? 0 : extractorList.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof FeatureExtractionPipeline))
			return false;
		FeatureExtractionPipeline other = (FeatureExtractionPipeline) obj;
		if (extractorList == null) {
			if (other.extractorList != null)
				return false;
		} else if (!extractorList.equals(other.extractorList))
			return false;
		return true;
	}

}
