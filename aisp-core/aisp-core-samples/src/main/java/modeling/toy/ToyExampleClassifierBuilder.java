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
package modeling.toy;

import org.eng.aisp.classifier.AbstractClassifierBuilder;
import org.eng.aisp.classifier.IFixableClassifierBuilder;
import org.eng.aisp.feature.extractor.IFeatureExtractor;

/**
 * This only needs to be implemented if you want this class to be part of the Ensemble classifier.
 * If so, then you also need to add an instance of this to the ClassifierRegistry.
 * @author dawood
 *
 */
public class ToyExampleClassifierBuilder extends AbstractClassifierBuilder<double[], double[]>  implements IFixableClassifierBuilder<double[], double[]>{

	private static final long serialVersionUID = 2270286308736530706L;
	private double someParameter = 0;

	
	public ToyExampleClassifierBuilder(IFeatureExtractor<double[], double[]> fe) { 
		super(fe);
	}

	public ToyExampleClassifierBuilder() { 
		super();
	}
	
	@Override
	public ToyFixableExampleClassifier build() {
		return new ToyFixableExampleClassifier(transform, featureExtractor, featureProcessor, windowSizeMsec, windowShiftMsec, someParameter);
	}
	
	public ToyExampleClassifierBuilder setSsomeParameter(double someParameter) {
		this.someParameter = someParameter;
		return this; 
	}
	

}
