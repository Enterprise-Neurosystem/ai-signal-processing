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

import java.util.Properties;
import java.util.function.Predicate;

import org.eng.aisp.ILabeledDataWindow;

/**
 * A basic predicate requiring the data windows used for training to 1) optionally have the training label and valid value, and 2) be trainable as defined by its isTrainable() method.
 * @author dawood
 *
 * @param <WINDATA>
 */
public class TrainingDataPredicate<WINDATA> implements Predicate<ILabeledDataWindow<WINDATA>> {

	private final String trainingLabel;
	private final boolean excludeUndefined;

	/**
	 *  Convenience on {@link #TrainingDataPredicate(String, boolean)} that does not check for a label name or value.
	 */
	public TrainingDataPredicate() {
		this(null, true);
	}
	/**
	 * Defined the training label that must be present 
	 * @param trainingLabel the label name to be present on sounds.  If null, then the label is not checked.
	 * @param excludeUndefined if true, then exclude training data that has the given label set to the {@link Classification#UndefinedLabelValue} value. Ignored
	 * if trainingLabel is null.
	 */
	public TrainingDataPredicate(String trainingLabel, boolean excludeUndefined) {
		this.trainingLabel = trainingLabel;
		this.excludeUndefined = excludeUndefined;
	}

	/**
	 * Return true (i.e. include the window when training)
	 * of the data is trainable (via ldw.isTrainable()) and the window includes the label provided to the constructor.
	 */
	@Override
	public boolean test(ILabeledDataWindow<WINDATA> ldw) {
		if (!ldw.isTrainable())
			return false;

		if (trainingLabel != null) {
			Properties labels = ldw.getLabels();
			String value = labels.getProperty(trainingLabel);
			if (value == null)	// Must have the required label.
				return false;
			if (excludeUndefined && value.trim().equalsIgnoreCase(Classification.UndefinedLabelValue))	// must have a defined value 
				return false;
		}

		return true;
	}

}
