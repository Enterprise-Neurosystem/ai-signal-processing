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
package org.eng.aisp.classifier;


import org.eng.aisp.classifier.anomaly.normal.NormalDistributionAnomalyClassifierBuilderTest;
import org.eng.aisp.classifier.anomaly.normal.NormalDistributionAnomalyClassifierTest;
import org.eng.aisp.classifier.anomaly.normal.OnlineNormalDistributionAnomalyClassifierTest;
import org.eng.aisp.classifier.dcase.DCASEClassifierTest;
import org.eng.aisp.classifier.factory.ClassifierFactoriesTest;
import org.eng.aisp.classifier.gmm.GMMClassifierBuilderTest;
import org.eng.aisp.classifier.gmm.GMMClassifierTest;
import org.eng.aisp.classifier.knn.KNNTestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * @author dawood
 * Each package under this one should declare its own test suite and list it below.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	LabeledFeatureIterableTest.class,
	ClassifierFactoriesTest.class,
	ConfusionMatrixTester.class,
	KFoldModelEvaluatorTest.class,


	GMMClassifierTest.class,	
	GMMClassifierBuilderTest.class,	
	KNNTestSuite.class,

	
	OnlineNormalDistributionAnomalyClassifierTest.class,
	NormalDistributionAnomalyClassifierTest.class,
	NormalDistributionAnomalyClassifierBuilderTest.class,

	FixedSegmentClassifierTest.class,
	

	// This is passing, but we usually use DCASE and not CNN, so only test DCASE (to speed up manual testing)
//	CNNModelerTest.class,					//CNN test passes as of 3/22/2018, but it takes long to run, thus commented out
	DCASEClassifierTest.class,
        })
public class ModelingTestSuite {


}
