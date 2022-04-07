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
package org.eng.aisp;

import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.IdentityFeatureExtractor;
import org.eng.aisp.feature.pipeline.IBatchedFeatureExtractor;
import org.eng.aisp.feature.pipeline.LabeledFeatureIterable;
import org.eng.cache.MemoryCache;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class AISPRuntimeTest {

	@Test
	public void testGetDiskCachingIterable() {
		Iterable<ILabeledDataWindow<double[]>> data = new ArrayList<>();
		IFeatureExtractor<double[],double[]> extractor = new IdentityFeatureExtractor();
		IFeatureGramDescriptor<double[],double[]> fge = new FeatureGramDescriptor<double[],double[]>(100,0, extractor,null);
		List<IFeatureGramDescriptor<double[],double[]>> fgeList = new ArrayList<>();
		fgeList.add(fge);	
		Iterable<ILabeledFeatureGram<double[]>[]> lfg = new LabeledFeatureIterable<double[],double[]>(data, null, fgeList); 
		MemoryCache<?, ?> memCache = new MemoryCache();
		Object o;
		if (!SoundTestUtils.isExtendedRuntimeTesting()) {
			o = AISPRuntime.getRuntime().getDiskCachingIterable(lfg, null);
			Assert.assertTrue( o == null);
			o = AISPRuntime.getRuntime().getDiskCachingIterable(lfg, memCache);
			Assert.assertTrue( o == null);
		} else {
			o = AISPRuntime.getRuntime().getDiskCachingIterable(lfg, null);
			Assert.assertTrue( o != null);
			o = AISPRuntime.getRuntime().getDiskCachingIterable(lfg, memCache);
			Assert.assertTrue( o != null);
		}
	}


}
