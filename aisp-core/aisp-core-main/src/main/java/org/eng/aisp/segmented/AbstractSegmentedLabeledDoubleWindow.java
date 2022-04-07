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
package org.eng.aisp.segmented;

import java.util.Collection;

import org.eng.aisp.ILabeledDataWindow;

/**
 * Extends the super class just to define the data in the labeled data window as an array of double.
 * @author DavidWood
 *
 * @param <LDW>
 */
public abstract class AbstractSegmentedLabeledDoubleWindow<LDW extends ILabeledDataWindow<double[]>> extends AbstractSegmentedLabeledDataWindow<double[], LDW>
						implements ISegmentedLabeledDoubleWindow<LDW> {

	private static final long serialVersionUID = -8522339693723223383L;

	// For Jackson/Gson
	protected AbstractSegmentedLabeledDoubleWindow() {
		super();
	}
	
	public AbstractSegmentedLabeledDoubleWindow(LDW labeledWindow, Collection<LabeledSegmentSpec> segmentSpecs) {
		super(labeledWindow, segmentSpecs);
	}

	/**
	 * Copy constructor that replaces the labeled segments.
	 * @param sldw
	 * @param segmentSpecs
	 */
	public AbstractSegmentedLabeledDoubleWindow(AbstractSegmentedLabeledDoubleWindow<LDW> sldw , Collection<LabeledSegmentSpec> segmentSpecs) {
		super(sldw, segmentSpecs);
	}


}
