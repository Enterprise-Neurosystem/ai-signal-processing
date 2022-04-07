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
package org.eng.aisp.feature.extractor.vector;

import org.eng.aisp.IDataWindow;
import org.eng.aisp.feature.DoubleFeature;
import org.eng.aisp.feature.IDoubleFeature;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.extractor.IDoubleFeatureExtractor;
import org.eng.aisp.processor.AbstractCachingWindowProcessor;

/**
 * Creates a single scalar feature which is the rms of the signal.
 * @author Joshua Rosenkranz, dawood
 */
public class RMSFeatureExtractor extends AbstractCachingWindowProcessor<IDataWindow<double[]>, IFeature<double[]>> implements IDoubleFeatureExtractor {

    private static final long serialVersionUID = -9073697935524099861L;

	public RMSFeatureExtractor() { }

    @Override
    protected IDoubleFeature applyImpl(IDataWindow<double[]> recording) {
        double samples[] = recording.getData();

		double sum = 0;
		for (double sample : samples) {
			sum += Math.pow(sample, 2);
		}

		double rms = Math.sqrt(sum / samples.length);
		return new DoubleFeature(recording.getStartTimeMsec(), recording.getEndTimeMsec(), new double[] { rms,rms });

    }
}
