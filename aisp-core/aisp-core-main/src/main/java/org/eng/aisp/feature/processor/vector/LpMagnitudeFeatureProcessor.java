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

import org.eng.aisp.util.LpDistanceFunction;

/**
 * Extend the super class to use LpDistanceFunction.
 
 */
public class LpMagnitudeFeatureProcessor extends MagnitudeFeatureProcessor {

	private static final long serialVersionUID = 5207848429213809125L;

	public LpMagnitudeFeatureProcessor() {
		this(LpDistanceFunction.DEFAULT_P);
	}
	/**
	 * Compute a single magnitude value for the feature.
	 */
	public LpMagnitudeFeatureProcessor(double p) {
		this(p, false, true);
	}

	public LpMagnitudeFeatureProcessor(boolean magnitudeAcrossTime) {
		this(LpDistanceFunction.DEFAULT_P, magnitudeAcrossTime);
	}

	/**
	 * Compute the distance across time or within the feature.
	 * @param magnitudeAcrossTime
	 */
	public LpMagnitudeFeatureProcessor(double p, boolean magnitudeAcrossTime) {
		this(p, magnitudeAcrossTime, false);
	}

	private LpMagnitudeFeatureProcessor(double p, boolean magnitudeAcrossTime, boolean singleValue) {
		super(new LpDistanceFunction(p), magnitudeAcrossTime, singleValue);
	}

}
