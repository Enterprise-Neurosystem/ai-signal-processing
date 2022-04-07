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
package org.eng.aisp.classifier.knn.merge;

import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.IClassifierBuilder;
import org.eng.aisp.classifier.IFixableClassifier;
import org.eng.aisp.classifier.knn.AbstractKNNModelerTest;
import org.eng.aisp.classifier.knn.merge.EuclidianDistanceMergeKNNClassifierBuilder;
import org.eng.aisp.classifier.knn.merge.LpDistanceMergeKNNClassifierBuilder;
import org.eng.aisp.feature.extractor.vector.MFCCFeatureExtractor;
import org.eng.aisp.transform.IdentityWindowTransform;

public class LpDistanceMergeKNNModelerTest extends AbstractKNNModelerTest {

	@Override
	protected IFixableClassifier<double[]> getClassifier() {
		// Here we have to use MFCC(20), which is not the default for LpNN, to get testHomeAnomalies() to pass.
		LpDistanceMergeKNNClassifierBuilder builder = new LpDistanceMergeKNNClassifierBuilder();
		builder.setEnableOutlierDetection(false);
		builder.setFeatureExtractor(new MFCCFeatureExtractor(20));
		builder.setTransform(new IdentityWindowTransform(1)); 
		return (IFixableClassifier<double[]>) builder.build();
//		return new LpDistanceMergeKNNClassifier(trainingLabel);
	}
	
	
	@Override
	protected IFixableClassifier<double[]> getUnknownClassifier() throws AISPException {
		IClassifierBuilder<double[],double[]> b = new EuclidianDistanceMergeKNNClassifierBuilder().setEnableOutlierDetection(true);
		return (IFixableClassifier<double[]>)b.build(); 
	}
	

}
