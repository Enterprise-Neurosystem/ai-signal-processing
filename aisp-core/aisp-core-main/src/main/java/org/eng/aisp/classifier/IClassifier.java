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
package org.eng.aisp.classifier;

import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.ILabeledDataWindow;

/**
 * Adds trainability to the IFixedClassifier.
 *  
 * @author dawood
 *
 */
public interface IClassifier<DATA> extends  IFixedClassifier<DATA> {

	/**
	 * If a model supports multiple labels, it should expect the label names to be separated by this separator in the training label provided..
	 */
	public final static String LABEL_NAME_SEPARATOR = ",";
	
	/**
	 * Train this instance of the classify on the given labeled data.
	 * This is not an update and instead should effectively reset the model to its initial state before training on the given data.
	 * Upon return, the instance is ready to have its {@link #classify(org.eng.aisp.IDataWindow)} method called to classify new data.
	 * @param label on which to train the model. 
	 * @param data labeled data to train the model.  Data must contain at enough data with the given label for the model to be trained.
	 * @throws AISPException not enough date, etc.
	 */
	public void train(String trainingLabel, Iterable<? extends ILabeledDataWindow<DATA>> data) throws AISPException;

	/**
	 * Format a list of a training labels into a single string as expected by the MultiClassifier.
	 * @param labels
	 * @return null if labels is null or 0 length.
	 */
	static String formatMultiLabels(List<String> labels) {
		if (labels == null || labels.size() == 0)
			return null;
		String mlabel = labels.get(0);
		if (labels.size() == 1)
			return mlabel;
		for (int i=1; i<labels.size() ; i++) {
			mlabel += ",";
			mlabel += labels.get(i);
		}
		return mlabel;
	}

	/**
	 * Parse the single string that specified multiple labels into an array of those labels.
	 * Labels are separated by commas with no spaces.
	 * @param multiLabelTrainingLabel
	 * @return an array of length 1 or more.
	 * @throws IllegalArgumentException if a comma-separated list of values produces zero or empty label names.
	 */
	static String[] parseMultiLabels(String multiLabelTrainingLabel) {
		String[] r;
		if (multiLabelTrainingLabel.contains(LABEL_NAME_SEPARATOR)) {
			r = multiLabelTrainingLabel.split(LABEL_NAME_SEPARATOR);
			if (r == null || r.length == 0)
				throw new IllegalArgumentException("Got null or empty label values when parsing " + multiLabelTrainingLabel);
			for (int i=0 ; i<r.length ; i++) {
				r[i] = r[i].trim();
				if (r[i].length() == 0)
					throw new IllegalArgumentException("Zero length label found in " + multiLabelTrainingLabel);
			}
		} else {
			r = new String[] { multiLabelTrainingLabel };
		}
		return r;
	} 
	



}

