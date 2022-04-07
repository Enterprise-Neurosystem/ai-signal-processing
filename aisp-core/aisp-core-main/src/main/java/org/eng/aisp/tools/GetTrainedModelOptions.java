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
package org.eng.aisp.tools;

import java.io.IOException;

import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.util.FixedClassifiers;
import org.eng.util.CommandArgs;

public class GetTrainedModelOptions {

	public final static String OptionsHelp =
			""
			+ "  -file file : specifies the file containing the model to load.\n"
			;


	private String modelFile;

	/**
	 * Parse options.  
	 * @param cmdargs
	 * @return true if successful and nothing on stdout, other false and error message put on stderr.
	 */
	public boolean parseOptions(CommandArgs cmdargs) {
		this.modelFile= cmdargs.getOption("file");
		return true;
		
	}
	

	/**
	 * Get a new instance of the classifier specified by the options.
	 * @return non-null if successful and nothing on stdout, otherwise null and error message put on stderr.
	 * @throws AISPException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public IFixedClassifier<double[]> getClassifier() throws AISPException, IOException {
		IFixedClassifier<double[]> classifier = null; 

		if (modelFile != null) { // Load the local model , takes precedence over remote options.
			try {
				classifier = (IFixedClassifier<double[]>) FixedClassifiers.read(modelFile);
			} catch (Exception e) {
				System.err.println("Could not load classifier from file " + modelFile + ": " + e.getMessage());
				return null;
			}
		} else {
			System.err.println("A local model was not specified");
			return null;
		}

		return classifier;
	
	}

}

