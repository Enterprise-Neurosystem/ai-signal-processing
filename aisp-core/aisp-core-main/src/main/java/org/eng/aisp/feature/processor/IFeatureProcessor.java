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
package org.eng.aisp.feature.processor;

import java.io.Serializable;

import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGram;

/**
 * Provides a way to operate over a sequence of features to modify them as a group.
 * The array of features can be thought of as a spectrogram as they should be assumed
 * to be in time sequence, although they may overlap in time.
 * @author dawood
 *
 * @param <FDATA> the data type returned by {@link IFeature#getData(int)} used in the implementation.
 */
public interface IFeatureProcessor<FDATA> extends Serializable { 

	/**
	 * Process a group of features to produce another group of 1 or more features.
	 * @param features a set of 1 or more features ordered by start time.  The time windows may however overlap.
	 * @return an array of 1 or more features ordered by time.  The number of features returned
	 * need not be the same as those provided.
	 */
    IFeatureGram<FDATA> apply(IFeatureGram<FDATA> features );
    
}
