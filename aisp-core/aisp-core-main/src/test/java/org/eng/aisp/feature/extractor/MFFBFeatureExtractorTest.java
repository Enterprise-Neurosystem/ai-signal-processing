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

import org.eng.aisp.feature.extractor.vector.MFFBFeatureExtractor;

public class MFFBFeatureExtractorTest extends AbstractFeatureExtractorTest {

	@Override
	protected IFeatureExtractor<double[], double[]> getFeatureExtractor() {
		return new MFFBFeatureExtractor();
	}
	protected boolean useThreholdLogging() {
		return true;
	}
	@Override
	protected IFeatureExtractor<double[], double[]> getFeatureExtractor(int samplingRate, int minHz, int maxHz, int featureLen) {
		return new MFFBFeatureExtractor(samplingRate, featureLen, minHz, maxHz, false, true);
	}

}
