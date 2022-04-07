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
import org.eng.aisp.util.JScriptBuilder;


/**
 * Provides the ability to instantiate a list of IClassifier from java script.
 * The JavaScript runtime available to the script writer provides easy access to all of the classifier, feature extractor and feature processors classes that
 * are part of the library.  A simple script to create a list of 2 classifiers, might look like this: 
 * <pre>
 *   var classifiers = [
 *   	new GMMClassifier().build(),
 *      new DCASEClassifier().build()
 *   ] 
 *</pre>
 * 
 * @author dawood
 * @param <WINDATA>
 */
public class JScriptClassifierMapFactory<WINDATA> extends JScriptBuilder  {

	/** The name of the variable to retrieve when there are multiple instances of IClassifier in the JScriptBuilder script */
	public final static String PREFERRED_VAR_NAME = "classifiers";
	
	public JScriptClassifierMapFactory(String script) {
		super(script, Map.class, PREFERRED_VAR_NAME);
	}

	public static void main(String[] args) throws AISPException {

		String script = "\n"
				+  "var classifiers = {"
				+ " \"gmm1\" : new GMMClassifier(new MFCCFeatureExtractor(),  null, 40, 40),"
				+ " \"gmm2\" : new GMMClassifierBuilder().build()"
				+ "}"
				;
		System.out.println("script is\n" + script);

		JScriptClassifierMapFactory<double[]> desc = new JScriptClassifierMapFactory<double[]>(script); 
		try {
			Map<String,IClassifier<double[]>> clist = desc.build();
			System.out.println("classifiers = " + clist);
		} catch (AISPException e) {
			e.printStackTrace();
		}
		
	}

	public Map<String,IClassifier<WINDATA>> build() throws AISPException {
		Map<String,Object> results = this.buildAll();
		@SuppressWarnings("unchecked")
		Map<String, IClassifier<WINDATA>> r = (Map)results.get(PREFERRED_VAR_NAME);
		// The super class only validates the List object, not its elements, so we need to do it here.
		for (Object o : r.values()) {
			if (!(o instanceof IClassifier))
				throw new AISPException("List element contains instance of " + o.getClass().getName() + " and not " + IClassifier.class.getName());
		}
		return r;
	}

}
