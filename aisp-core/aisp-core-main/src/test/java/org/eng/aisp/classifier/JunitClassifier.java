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

import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.classifier.AbstractClassifier;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IClassifier;

/**
 * This class is used by ClassifierFactoriesTest and junit-model.jsyt to test parawmeterization.
 * @author DavidWood
 *
 */
public class JunitClassifier extends AbstractClassifier<double[]> implements IClassifier<double[]> {

	private static final long serialVersionUID = 5670143717207962673L;
	private String stringValue;
	private int integerValue;
	private double doubleValue;

	public JunitClassifier(String stringValue, int integerValue, double doubleValue) {
		this.stringValue = stringValue;
		this.integerValue = integerValue;
		this.doubleValue = doubleValue;
	}

	@Override
	public Map<String, Classification> classify(IDataWindow<double[]> sample) throws AISPException {
		throw new RuntimeException("Not expected to be here");
	}

	@Override
	public String getTrainedLabel() {
		throw new RuntimeException("Not expected to be here");
	}

	@Override
	public void train(String trainingLabel, Iterable<? extends ILabeledDataWindow<double[]>> data) throws AISPException {
		throw new RuntimeException("Not expected to be here");
		
	}

	/**
	 * @return the stringValue
	 */
	public String getStringValue() {
		return stringValue;
	}

	/**
	 * @return the integerValue
	 */
	public int getIntegerValue() {
		return integerValue;
	}

	/**
	 * @return the doubleValue
	 */
	public double getDoubleValue() {
		return doubleValue;
	}

}
