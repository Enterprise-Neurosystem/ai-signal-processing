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

import java.util.ArrayList;
import java.util.List;

public class AbstractToolTest {

	protected final static String TESTED_PAD_TYPE = "duplicate";

	public AbstractToolTest() {
		super();
	}

	protected void runTrainCommand(List<String> largs) {
	//		largs.add("-host");
	//		largs.add("localhost");
			String args[] = new String[largs.size()];
			largs.toArray(args);
			Train.main(args);
	}

	protected void runClassifyCommand(List<String> largs) {
	//		largs.add("-host");
	//		largs.add("localhost");
			String args[] = new String[largs.size()];
			largs.toArray(args);
			Classify.main(args);
		}

	/**
	 * Train a model, store it in the file system and return the name of the file.
	 * @param index
	 * @param modelType
	 * @param soundDir
	 * @param labelName
	 * @param padSizeMsec
	 * @return
	 */
	protected String trainAndSaveToFile(int index, String modelType, String soundDir, String labelName, int padSizeMsec ) {
		List<String> largs = new ArrayList<String>();
		String modelName = "modeldbtest " + index;	// names with white space have caused us problems, so be sure to test that.
		String tmpFile = modelName + ".cfr"; 
		tmpFile = tmpFile.replaceAll(" ", "_");
	
		/// Use the Train tool to train the model and put it in a temp file.
		largs.add("-sounds");
		largs.add(soundDir);
		largs.add("-label");
		largs.add(labelName);
		largs.add("-model");
		largs.add(modelType);
		largs.add("-output");
		largs.add(tmpFile);
		if (padSizeMsec > 0) {
			largs.add("-clipLen");
			largs.add(String.valueOf(padSizeMsec));
			largs.add("-pad");
			largs.add(TESTED_PAD_TYPE);
		}
		runTrainCommand(largs);	
		return tmpFile;
	}

}
