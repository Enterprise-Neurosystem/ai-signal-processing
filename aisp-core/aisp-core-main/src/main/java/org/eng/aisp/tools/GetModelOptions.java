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

import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.factory.ClassifierFactories;
import org.eng.util.CommandArgs;

/**
 * Support parsing list of wav files or csv directories of sounds with or w/o a metadata file.
 * Sounds are always returned as an IShuffleIterable<SoundRecording>.
 * @author DavidWood
 *
 */
public class GetModelOptions {
	
	public final static String OptionsHelp = 
			  "  -model spec : specifies the type of model to use. Currently supported values\n"
			+ "      include 'gmm', 'lpnn', 'cnn' 'dcase' and others. Use the \n"
			+ "      ls-models tool to see all supported model names. You may define a model\n" 
			+ "      in a local JavaScript file with the .js extension, as in yourmodel.js.\n"
			+ "      Default is " + GetModelOptions.DEFAULT_MODEL_SPEC + ".\n"
	;
	
	protected IClassifier<double[]> classifier; 

	static final String DEFAULT_MODEL_SPEC = "ensemble";

	public IClassifier<double[]> getClassifier() {
		return this.classifier;
	}

	/**
	 * Parse the options and establish the return value of {@link #getClassifier()}.
	 * @param cmdargs
	 * @return true on now option errors.  false and issue an error message on stderr if an option error.
	 */
	public boolean parseOptions(CommandArgs cmdargs) {
		String modelSpecification = cmdargs.getOption("model", GetModelOptions.DEFAULT_MODEL_SPEC);

		// If a JavaScript file given make sure it has the jsfile prefix as needed by the ClassifierFactories below.
		if (modelSpecification.endsWith(".js") && !modelSpecification.startsWith("jsfile:"))
			modelSpecification = "jsfile:" + modelSpecification;
		
		try {
			this.classifier =  ClassifierFactories.newDefaultClassifier(modelSpecification);
		} catch (AISPException e) {
			System.err.println("Could not parse model specification '" + modelSpecification + "': " + e.getMessage());
			return false;
		}
		
		return true;
	}




}
