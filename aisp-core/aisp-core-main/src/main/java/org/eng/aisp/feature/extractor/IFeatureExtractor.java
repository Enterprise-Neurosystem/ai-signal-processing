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
package org.eng.aisp.feature.extractor;

import java.io.Serializable;

import org.eng.aisp.IDataWindow;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.processor.IWindowProcessor;

/**
 * Defines the input and output of a window processor that generates features from a window of data. 
 * 
 * @param <WINDATA> data returned by {@link IDataWindow#getData(int)} on window from which features are extracted.
 * This is usually an array, for example double[].
 * @param <FDATA> data returned by {@link IFeature#getData(int)} on feature created. This is usually an array,
 * for example double[] or IFeature&ltdouble[]&gt[].
 */
public interface IFeatureExtractor<WINDATA, FDATA> extends IWindowProcessor<IDataWindow<WINDATA>, IFeature<FDATA>>, Serializable {

	
}
