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
package org.eng.aisp.feature;

import org.eng.aisp.DoubleWindow;
import org.eng.util.Vector;

/**
 * An implementation of IDoubleFeature.
 * 
 * @author dawood
 *
 */
public class DoubleFeature extends DoubleWindow implements IFeature<double[]>, IDoubleFeature {

	private static final long serialVersionUID = 7723768966505486788L;
	
	/**
	 * Create the feature with the given independent and dependent arrays of values.
	 * @param x an array of values sorted in order.  If not in ascending order, then the
	 * instance will store a new array that is sorted as such.
	 * @param y actual features corresponding to the values in x.  If x is resorted, then
	 * y is also resorted.
	 * 
	 */
	
	public DoubleFeature(double startTimeMsec, double endTimeMsec, Vector x, double y[]) {
		super(startTimeMsec, endTimeMsec, x, y );
	}

	public DoubleFeature(double startTimeMsec, double endTimeMsec, double[] ds) {
		super(startTimeMsec, endTimeMsec, ds);
	}

	
}
