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

import java.io.Serializable;

import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.transform.ITrainingWindowTransform;

/**
 * Defines function to instantiate new classifiers.
 * @author dawood
 *
 * @param <WINDATA>
 * @param <FDATA>
 */
public interface IClassifierBuilder<WINDATA, FDATA> extends IClassifierFactory<WINDATA>, Cloneable, Serializable {

	/**
	 * Set the feature extractor for this builder.
	 * @param fe never null.
	 * @return
	 */
	public IClassifierBuilder<WINDATA, FDATA> setFeatureExtractor(IFeatureExtractor<WINDATA,FDATA> fe);
	
	public IClassifierBuilder<WINDATA,FDATA> clone();

	public IClassifierBuilder<WINDATA, FDATA> setWindowShiftMsec(int msec);

	public IClassifierBuilder<WINDATA, FDATA> setWindowSizeMsec(int msec);

	/**
	 * Set the feature processor for this builder.  
	 * @param featureProcessor null is allowed.
	 * @return
	 */
	IClassifierBuilder<WINDATA, FDATA> setFeatureProcessor(IFeatureProcessor<FDATA> featureProcessor);

	public IFeatureExtractor<WINDATA, FDATA> getFeatureExtractor();

	public IFeatureProcessor<FDATA> getFeatureProcessor();

	public int getWindowSizeMsec();

	public int getWindowShiftMsec();

	ITrainingWindowTransform<WINDATA> getTransform();

	IClassifierBuilder<WINDATA, FDATA> setTransform(ITrainingWindowTransform<WINDATA> transforms);
	
	/**
	 * This is currently intended as a convenience method that can be used in placed of calling
	 * <ul>
	 * <li> {@link #setWindowSizeMsec(int)},
	 * <li> {@link #setWindowShiftMsec(int)},
	 * <li> {@link #setFeatureExtractor(IFeatureExtractor)}, and
	 * <li> {@link #setFeatureProcessor(IFeatureProcessor)}
	 *</ul> 
	 * It may in the future replace these methods, so for now it is the preferred method to use.
	 * @param fge
	 */
	public IClassifierBuilder<WINDATA, FDATA> setFeatureGramDescriptor(IFeatureGramDescriptor<WINDATA, FDATA> fge);

}
