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



import org.eng.aisp.cache.CacheTestSuite;
import org.eng.aisp.classifier.ModelingTestSuite;
import org.eng.aisp.client.iotp.IOTPlatformPropertiesTest;
import org.eng.aisp.dataset.DataSetTestSuite;
import org.eng.aisp.feature.FeatureTestSuite;
import org.eng.aisp.segmented.SegmentedTestSuite;
import org.eng.aisp.storage.AISPStorageTestSuite;
import org.eng.aisp.tools.ToolsTestSuite;
import org.eng.aisp.transform.TransformTestSuite;
import org.eng.aisp.util.AISPUtilTestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Tests for both the src/main and src/client source.
 * @author dawood
 * Each package under this one should declare its own test suite and list it below.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({

	DoubleWindowTest.class,
	SoundClipTest.class,
	SoundRecordingTest.class,
	SoundClipXYZTest.class,

	IOTPlatformPropertiesTest.class,

	CacheTestSuite.class,
	DataSetTestSuite.class,
	AISPRuntimeTest.class,
	AISPUtilTestSuite.class,
	FeatureTestSuite.class,
	TransformTestSuite.class,
	ToolsTestSuite.class,
	SegmentedTestSuite.class,
	AISPStorageTestSuite.class,


	// Leave this one to last since it is the longest and, in general, depends on the others.
	ModelingTestSuite.class,

	
        })
public class AISPTestSuite {

}
