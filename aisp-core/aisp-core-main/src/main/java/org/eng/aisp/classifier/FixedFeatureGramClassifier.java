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
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.IFeatureGramDescriptor;

/**
 * Extends the super class to implement its methods using an instance of IFixedFeatureGramClassifier.
 * This class uses the super class to extract the features and the  IFixedFeatureGramClassifier to actually do the classification.
 * 
 * @author dawood 
 *
 * @param <WINDATA> data type of an IDataWindow. For example, double[].
 * @param <FDATA> data type of the features extracted from the IDataWindow<WINDATA>. For example, double[].
 */
public class FixedFeatureGramClassifier<WINDATA, FDATA> extends AbstractFixedFeatureExtractingClassifier<WINDATA,FDATA> implements IFixedClassifier<WINDATA> {

	private static final long serialVersionUID = 2161442380031989763L;
	private final IFixedFeatureGramClassifier<FDATA> fgClassifier;

	public FixedFeatureGramClassifier(String label, List<IFeatureGramDescriptor<WINDATA,FDATA>> fgeList, IFixedFeatureGramClassifier<FDATA> fgClassifier) {
		super(fgeList);
		this.fgClassifier = fgClassifier;
	}

	@Override
	public String getTrainedLabel() {
		return this.fgClassifier.getTrainedLabel();
	}

	@Override
	protected List<Classification> classify(IFeatureGram<FDATA>[] features) throws AISPException {
		return fgClassifier.classify(features);
	}

}