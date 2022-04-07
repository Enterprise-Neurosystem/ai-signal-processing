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

import org.eng.aisp.feature.extractor.vector.IdentityFeatureExtractor;
import org.junit.Assert;
import org.junit.Test;

public class IdentityFeatureExtractorTest extends AbstractFeatureExtractorTest {

	@Override
	protected IFeatureExtractor<double[], double[]> getFeatureExtractor() {
		return new IdentityFeatureExtractor();
	}
	
	@Override
	protected IFeatureExtractor<double[], double[]> getFeatureExtractor(int samplingRate, int minHz, int maxHz, int featureLen) {
		return null;
	}	
	
	// Override to not do the test which assumes the features are the same lengths for different duration sounds.
	// This is not the case for this feature extractor
	@Override
	@Test
	public void testDifferentDurations()  {
	}

	// Override to not do the test which assumes the features are the same lengths for different sampling rates.
	// This is not the case for this feature extractor
	@Override
	@Test
	public void testDifferentSamplingRates()  {
	}


	@Test
	public void testResampling() {
		int channels = 1, bitsPerSample = 16;
		double amp = 1, offset = 0, feature[];
		int htz = 1000;
		int samplingRate = 1000;
		boolean addNoise = false;
		IdentityFeatureExtractor fe = new IdentityFeatureExtractor();
		int durationMsec = 1000;
		feature = getFeatures(fe, durationMsec, channels, samplingRate, bitsPerSample, amp, offset, htz, addNoise);
		int baseLen = feature.length;

		// Resample at twice the original sampling rate and get twice the original feature length
		fe = new IdentityFeatureExtractor(samplingRate*2);
		feature = getFeatures(fe, durationMsec, channels, samplingRate, bitsPerSample, amp, offset, htz, addNoise);
		Assert.assertTrue(2*baseLen == feature.length);
	}

}
