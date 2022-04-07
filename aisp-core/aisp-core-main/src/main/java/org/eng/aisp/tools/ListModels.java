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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.factory.ClassifierFactories;
import org.eng.aisp.util.JavaScriptFactories.InstanceSpecification;
import org.eng.template.ITemplateIO;
import org.eng.template.TemplateException;
import org.eng.template.yaml.YAMLTemplateIO;
import org.eng.util.CommandArgs;

public class ListModels {

	public static String Usage = 
             //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
			  "List the models specifications (identifiers) known to the tools and which\n"
			+ "can generally be used as an argument to the -model option of tools such as\n"
			+ "train and evaluate. By default, the identifier, type and specification\n"
			+ "details (e.g., javascript) are shown.\n"
			+ "Usage : ... [-v|-q] [model-spec (model-spec ...)  ] \n" 
			+ "Options:\n"
			+ "  -v        : Print untrained Java model class detail.\n" 
			+ "Usage:\n"
			+ "  ...              # With no args, shows all model specifications/names\n"
			+ "  ... -v           # Adds the untrained Java class detail for models\n"
			+ "  ... ensemble gmm # Shows JavaScript for the listed model identifiers\n" 
			+ "  ... -v gmm       # Shows Java and JavaScript details for the gmm model\n"
			;

	public static void main(String[] args) throws IOException, AISPException {
		try {
			ClassifierFactories.newDefaultClassifier("ensemble");	// This forces the SLF4J errors/warnings to come out.
		} catch (Exception e) { 
			;
		}

		CommandArgs cmdargs = new CommandArgs(args);
		if (cmdargs.getFlag("-h") || cmdargs.getFlag("-help")) {
			System.out.println(Usage);
			return;
		}
		
		boolean quiet = cmdargs.getFlag("-q");
		boolean verbose = cmdargs.getFlag("-v");
		args = cmdargs.getArgs();
		Collection<String> specs;
		Map<String,InstanceSpecification> specMap = ClassifierFactories.getModelSpecifications();
		boolean showJavaScript;
		if (args != null && args.length > 0) {
			// List only specs specified on the command line. 
			specs = Arrays.asList(args); 
			showJavaScript = true;
		} else {
			// List all specs in alphabetical order
			specs = new ArrayList<String>();
			specs.addAll(specMap.keySet()); 
			Collections.sort((List)specs);
			showJavaScript = false ;	// WIth no specifications, only list the available models.
		}
		final String indent = "    ";
		
		ITemplateIO io = new YAMLTemplateIO();
		for (String spec : specs) {
			InstanceSpecification ms = specMap.get(spec);
			if (ms == null ) {
				System.out.println(" not found.");
			}  else if (quiet || !showJavaScript) {
				// Just print names of models, 1 per line
				System.out.println(spec);
			} else {
				// Add type and location and perhaps additional info.
				System.out.println("// " + spec + " " + ms.getType().toString() + " " + ms.getLocation());
				String text;
				try {
					text = ms.getSpecification().generate(null);
				} catch (TemplateException e) {
					text = "Exception: "  + e.getMessage();
				}
//				System.out.println(indent(indent,  ms.getSpecification().prettyPrint()));
				System.out.println(indent(indent, text));
				if (verbose) {
					System.out.println(indent(indent, "Untrained Java class..."));
					IClassifier<?> c = ClassifierFactories.newClassifier(spec, null);
					System.out.println(indent(indent, c.toString()));
				}
			}
		}
	}

	
	private static String indent(String indent, String text) {
		return indent + text.replaceAll("\n", "\n" + indent);
	}
}
