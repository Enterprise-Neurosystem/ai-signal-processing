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

import org.eng.aisp.util.L1DistanceFunction;

public class L1MagnitudeFeatureProcessor extends MagnitudeFeatureProcessor {

	private static final long serialVersionUID = 5207848429213809125L;

	/**
	 * Compute a single magnitude value for the feature.
	 */
	public L1MagnitudeFeatureProcessor() {
		this(false, true);
	}

	/**
	 * Compute the distance across time or within the feature.
	 * @param magnitudeAcrossTime
	 */
	public L1MagnitudeFeatureProcessor(boolean magnitudeAcrossTime) {
		this(magnitudeAcrossTime, false);
	}

	private L1MagnitudeFeatureProcessor(boolean magnitudeAcrossTime, boolean singleValue) {
		super(new L1DistanceFunction(), magnitudeAcrossTime, singleValue);
	}

}
