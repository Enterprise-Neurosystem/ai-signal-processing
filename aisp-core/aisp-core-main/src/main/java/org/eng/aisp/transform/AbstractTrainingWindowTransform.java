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
package org.eng.aisp.transform;

import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.IDataWindow;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.LabeledDataWindow;

public abstract class AbstractTrainingWindowTransform<WINDATA> implements ITrainingWindowTransform<WINDATA> {

	private static final long serialVersionUID = -926522158826074913L;
	protected final boolean keepOriginal;

	/**
	 * 
	 * @param keepOriginal If true, include the original and unmodified labeled window in the output.
	 */
	public AbstractTrainingWindowTransform(boolean keepOriginal) {
		super();
		this.keepOriginal = keepOriginal;
	}

	/**
	 * Mix a previous window with the current window.
	 * Always produces an iterable with either 1 or 2 elements.  The first is the input window and the 2nd, if
	 * present, is the input window mixed with a previous window at the ratio specified in the constructor.
	 * The 2nd window is an instance of LabeledDataWindow containing an instance of DoubleWindow. 
	 */
	@Override
	public final Iterable<ILabeledDataWindow<WINDATA>> apply(String trainingLabel, ILabeledDataWindow<WINDATA> ldw) {
		List<ILabeledDataWindow<WINDATA>> windowList = new ArrayList<ILabeledDataWindow<WINDATA>>();
		if (keepOriginal)
			windowList.add(ldw);

		// Let the subclass generate the window mutations.  
		// Then we will wrap them back up into labeled data windows with those of the input labeled window.
		List<IDataWindow<WINDATA>> dwList = getMutatedWindows(trainingLabel, ldw);
		if (dwList != null && dwList.size() > 0) {
			for (IDataWindow<WINDATA> newDW : dwList) {
				ILabeledDataWindow<WINDATA> newLDW = new LabeledDataWindow<WINDATA>(newDW, ldw.getLabels(), ldw.getTagsAsProperties(), ldw.isTrainable());
				windowList.add(newLDW);
			}
		}
	
		return windowList;
	}

	/**
	 * Create a list of 0 or more transformed data windows from the given labeled data window.
	 * The caller, {@link #apply(String, ILabeledDataWindow)}, will wrap the returned windows into a LabeledDataWindow
	 * with the same labels, tags and trainability.
	 * Implementations need not include the original given window as this is included by {@link #apply(String, ILabeledDataWindow)} according
	 * to the keepOriginal parameter given to the {@link #AbstractTrainingWindowTransform(boolean)}.
	 * @param trainingLabel
	 * @param ldw
	 * @return never null, but a list of 0 or more transformed windows, not including the one given..
	 */
	protected abstract List<IDataWindow<WINDATA>> getMutatedWindows(String trainingLabel, ILabeledDataWindow<WINDATA> ldw);

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (keepOriginal ? 1231 : 1237);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AbstractTrainingWindowTransform))
			return false;
		AbstractTrainingWindowTransform other = (AbstractTrainingWindowTransform) obj;
		if (keepOriginal != other.keepOriginal)
			return false;
		return true;
	}

}
