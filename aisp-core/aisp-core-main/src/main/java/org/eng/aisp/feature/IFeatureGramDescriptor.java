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

import java.io.Serializable;

import org.eng.aisp.IDataWindow;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;

public interface IFeatureGramDescriptor<WINDATA, FDATA> extends Serializable {

	/**
	 * The window size when computing sub-windows.
	 * @return larger than 0.
	 */
	public double getWindowSizeMsec();

	/**
	 * The window shift (or hop) when computing sub-windows.
	 * @return larger than 0.
	 */
	public double getWindowShiftMsec();
	
	/**
	 * 
	 * @return never null
	 */
	public IFeatureExtractor<WINDATA, FDATA> getFeatureExtractor();

	/**
	 * @return null if none assigned.
	 */
	public IFeatureProcessor<FDATA> getFeatureProcessor();
	
	
}
