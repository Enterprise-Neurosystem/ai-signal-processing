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
package org.eng.aisp.classifier.factory;

import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.IClassifierFactory;
import org.eng.aisp.util.JScriptBuilder;


/**
 * Provides the ability to instantiate an IClassifier from java script.
 * The JavaScript runtime available to the script writer provides easy access to all of the classifier, feature extractor and feature processors classes that
 * are part of the libary.  A simple script to create a GaussianMixtureClassifier might look like the following:
 * <pre>
 *   var mfcc = new MFCCFeatureExtractor(25);
 *   var normalizer = new NormalizingFeatureProcessor(true,true, false, true);
 *   var classifier = new GaussianMixtureClassifier(trainingLabel, mfcc,  normalizer, 100, 100, 1, .9);
 *</pre>
 * The script has variables declared for each of the known classes (i.e. MFCCFeatureExtractor, NormalizingFeatureProcessor, etc)
 * and also defines the trainingLabel optionally provided to the constructor.
 * 
 * @author dawood
 * @param <WINDATA>
 */
public class JScriptClassifierFactory<WINDATA> extends JScriptBuilder implements IClassifierFactory<WINDATA> {

	/** The name of the variable to retrieve when there are multiple instances of IClassifier in the JScriptBuilder script */
	public final static String PREFERRED_VAR_NAME = "classifier";
	
	public JScriptClassifierFactory(String script) {
		super(script, IClassifier.class, PREFERRED_VAR_NAME);
	}

	public static void main(String[] args) throws AISPException {

		String script = "\n"
				+  "var extractor = new MFCCFeatureExtractor(25);\n"
				+  "var transform1 = new FrequencyWindowTransform(true, [.98, 1.01]);\n"
				+  "var transform2 = new TimeMutatingWindowTransform(false, [.98, 1.01]);\n"
				+  "var transform3 = new TrainingWindowMultiTransform([transform1,transform2]);\n"
				+  "var processor1 = new NormalizingFeatureProcessor(true,true, false, true);\n"
				+  "var processor2 = new DeltaFeatureProcessor(2, [1,1,1]); \n"
				+  "var processor = new PipelinedFeatureProcessor([processor1, processor2]);\n"
				+  "var classifier = new GMMClassifier(extractor,  processor, 40, 40);\n"
				;
		System.out.println("script is\n" + script);

		JScriptClassifierFactory<double[]> desc = new JScriptClassifierFactory<double[]>(script); 
		try {
			IClassifier<double[]> c = desc.build();
			System.out.println("classifier = " + c);
		} catch (AISPException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public IClassifier<WINDATA> build() throws AISPException {
		Map<String,Object> results = this.buildAll();
		@SuppressWarnings("unchecked")
		IClassifier<WINDATA> r = (IClassifier<WINDATA>)results.get(PREFERRED_VAR_NAME);
		return r;
	}

}
