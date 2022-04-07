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
import org.eng.aisp.classifier.knn.merge.EuclidianDistanceMergeKNNClassifierBuilder;
import org.eng.aisp.classifier.knn.merge.LpDistanceMergeKNNClassifierBuilder;
import org.eng.aisp.feature.extractor.vector.MFCCFeatureExtractor;
import org.eng.aisp.transform.IdentityWindowTransform;
import org.junit.Assume;

/**
 * This class is implemented primarily to make sure that we can serialize the model when normalization is turned on.
 * For this reason, it seems we don't need to have an instance of this for Euclidian or L1 distance methods.
 * @author DavidWood
 *
 */
public class NormalizeLpDistanceMergeKNNModelerTest extends LpDistanceMergeKNNModelerTest {

//	@Override
//	protected IFixableClassifier<double[]> getClassifier() {
//		return new LpDistanceMergeKNNClassifierBuilder().setNormalizeFeatures(true).build();
//	}
	@Override
	protected IFixableClassifier<double[]> getClassifier() {
		// Here we have to use MFCC(20), which is not the default for LpNN, to get testHomeAnomalies() to pass.
		LpDistanceMergeKNNClassifierBuilder builder = new LpDistanceMergeKNNClassifierBuilder();
		builder.setNormalizeFeatures(true);
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
	
	@Override
	public void testUpdateClassifier() throws AISPException {
		Assume.assumeFalse("SKipping as normalized features do not yet support updating of the model", true);
	}

//	@Override
//	public void testHomeAnomalies() throws IOException, AISPException {
//		Assume.assumeFalse("Skipping as normalized features do not pass this test, but that is probably ok as MFCC features should probably not be normalized anyway", true);
//	}

}
