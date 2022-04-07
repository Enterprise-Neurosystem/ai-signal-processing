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

/**
 * Captures model accuracy information.
 * @author dawood
 *
 */
public class Accuracy {

	protected int count;
	protected double accuracy;
	/**
	 * 
	 * @param count a non-negative value
	 * @param accuracy a value in the range 0 to 1.
	 */
	public Accuracy(int count, double accuracy) {
		super();
		if (count < 0) 
			throw new IllegalArgumentException("count must be 0 or larger");
		if (accuracy < 0 || accuracy > 1) 
			throw new IllegalArgumentException("accuracy must range from 0 to 1");
		this.count = count;
		this.accuracy = accuracy;
	}

	/**
	 * @return the count, a non-negative number
	 */
	public int getCount() {
		return count;
	}
	/**
	 * @return the accuracy, a value in the range 0..1
	 */
	public double getAccuracy() {
		return accuracy;
	}
	
	@Override
	public String toString() {
		return "Accuracy [count=" + count + ", accuracy=" + accuracy + "]";
	}
}
