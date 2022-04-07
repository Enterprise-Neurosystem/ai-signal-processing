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
package org.eng.aisp.classifier.knn;


import org.eng.aisp.classifier.knn.merge.EuclideanDistanceMergeKNNModelerTest;
import org.eng.aisp.classifier.knn.merge.L1DistanceMergeKNNModelerTest;
import org.eng.aisp.classifier.knn.merge.LpDistanceMergeKNNClassifierBuilderTest;
import org.eng.aisp.classifier.knn.merge.LpDistanceMergeKNNModelerTest;
import org.eng.aisp.classifier.knn.merge.NormalizeLpDistanceMergeKNNModelerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * @author dawood
 * Each package under this one should declare its own test suite and list it below.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	EuclideanDistanceMergeKNNModelerTest.class,
	L1DistanceMergeKNNModelerTest.class,
	LpDistanceMergeKNNModelerTest.class,
	LpDistanceMergeKNNClassifierBuilderTest.class,
	NormalizeLpDistanceMergeKNNModelerTest.class,
        })
public class KNNTestSuite {


}
