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
package org.eng.aisp.feature.io;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.pipeline.LabeledFeatureIterable;
import org.eng.aisp.feature.processor.IFeatureProcessor;

/**
 * Provides most of the work needed to extract and then write features extracted from a set of IDataWindows.
 * Subclasses must implement {@link #write(Writer, int, int, ILabeledFeature)}.
 * @author dawood
 *
 */
public abstract class AbstractFeatureWriter {

	private final List<IFeatureGramDescriptor<double[],double[]>> featureGramDescriptors = new ArrayList<IFeatureGramDescriptor<double[],double[]>>();

	/**
	 * @param extractor
	 * @param processor
	 * @param windowSizeMsec
	 * @param windowShiftMsec
	 */
	protected AbstractFeatureWriter(IFeatureExtractor<double[], double[]> extractor, IFeatureProcessor<double[]> processor,
			int windowSizeMsec, int windowShiftMsec) {
		super();
		IFeatureGramDescriptor<double[],double[]> fge = new FeatureGramDescriptor<double[],double[]>(windowSizeMsec, windowShiftMsec, extractor, processor);
		this.featureGramDescriptors.add(fge);
	}



	/**
	 * Extract the features according to the constructor and write them out using the subclass implementation 
	 * of {@link #write(Writer, int, int, ILabeledFeature)}. 
	 * @param writer
	 * @param data
	 * @throws IOException 
	 */
	public void write(Writer writer, Iterable<? extends ILabeledDataWindow<double[]>> data) throws IOException {
		Iterable<ILabeledFeatureGram<double[]>[]> fi = new LabeledFeatureIterable<double[],double[]>(data, null, featureGramDescriptors);
		int windowIndex = 0;
		for (ILabeledFeatureGram<double[]>[] featureGrams : fi) {
			if (featureGrams.length > 1)
				throw new IllegalArgumentException("Only a single feature gram is supported");
			ILabeledFeatureGram<double[]> lfg = featureGrams[0]; 
			int featureIndex = 0;
			IFeature<double[]>[] featureArray = lfg.getFeatureGram().getFeatures(); 
			Properties labels = lfg.getLabels();
			for (IFeature<double[]> feature : featureArray) {
				write(writer, windowIndex, featureIndex, feature, labels);
				featureIndex++;
			}
			windowIndex++;
		}
	}
	
	/**
	 * Extract and write the features to the named file.
	 * @param fileName
	 * @param data
	 * @throws IOException
	 */
	public void write(String fileName, Iterable<? extends ILabeledDataWindow<double[]>> data) throws IOException {
		FileWriter fw = new FileWriter(fileName);
		this.write(fw,data);
		fw.close();
	}	

	/**
	 * Write out the given labeled feature.
	 * @param writer
	 * @param windowIndex 0-based index of window given to {@link #write(Writer, Iterable)} from which the feature was extracted.
	 * @param subwindowIndex 0-based index of the sub-window within a data window from which the given feature was extracted.
	 * @param feature feature extracted from the Nth sub-window of the Mth data window.
	 * @param labels labels for those features.
	 * @throws IOException 
	 */
	public abstract void write(Writer writer, int windowIndex, int subwindowIndex, IFeature<double[]> feature, Properties labels) throws IOException; 

}
