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

import org.eng.aisp.AISPRuntime;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.util.FixedClassifiers;
import org.eng.aisp.util.GsonUtils;
import org.eng.util.CommandArgs;

import com.google.gson.Gson;

public class ModelInfoTool {
	
	public final static String Usage = 
			  "Get information about one or more models stored in the file system.\n"
			+ "Options: \n"
			+ "   - verbose : give full detail on model, otherwise just the class name.\n"
			+ "Examples: \n"
			+ "  ...  model.cfr\n"
			+ "  ...  model1.cfr model2.cfr\n"
			;


	
	public static void main(String args[]) {
		// Force any framework initialization messages to come out first.
		AISPRuntime.getRuntime();
		System.out.println("\n");	// blank line to separate copyrights, etc.
		
		CommandArgs cmdargs = new CommandArgs(args);

		// Check for help request
		if (cmdargs.getFlag("h") || cmdargs.getFlag("-help") ) {
			System.out.println(Usage);
			return;
	    }
		boolean verbose = cmdargs.getFlag("v") || cmdargs.getFlag("verbose");
		
		try {
			if (!doMain(cmdargs, verbose))
				System.err.println("Use the -help option to see usage");;
		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage());
			if (verbose)
				e.printStackTrace();
		}
		
	}
	
	protected static boolean doMain(CommandArgs cmdargs, boolean verbose) { 
		
		// Get the name of the model to use.
		int count=0;
		for (String file : cmdargs.getArgs()) {
			if (count != 0 && verbose)
				System.out.println("");
			try {
//				Object obj = ClassUtilities.deserialize(file);
//				FixedClassifiers.read(file);
//				if (!(obj instanceof IFixedClassifier)) {
//					System.err.println("File contain an instance of " + obj.getClass().getName() + " not "  + IFixedClassifier.class.getName()); 
//					return false;
//				}
				IFixedClassifier c = FixedClassifiers.read(file); 
				System.out.println(file + ": " + c.getClass().getSimpleName());
				if (verbose) {
					Gson gson = GsonUtils.getInterfaceSerializer(true).create();
					System.out.println(gson.toJson(c));
				}
			} catch (ClassNotFoundException e) {
				System.err.println("Could not load class: " + e.getMessage());
				return false;
			} catch (IOException e) {
				System.err.println("Could not load file: " + e.getMessage());
				return false;
			}
			count++;
		}
		if (count == 0) {
			System.out.println("No files provided");
			return false;
		}
			
		return true;

	}
}
