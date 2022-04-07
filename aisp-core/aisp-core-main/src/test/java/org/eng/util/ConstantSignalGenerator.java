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
package org.eng.util;

public class ConstantSignalGenerator implements ISignalGenerator {
	private final double value;
	
	public ConstantSignalGenerator(double value) {
		if (value < 0 || value > 1)
			throw new IllegalArgumentException("Value is out of range");
		this.value = value;
	}

	@Override
	public boolean hasNext() {
		return true;  //Always has next
	}

	@Override
	public Double next() {
		return value;
	}

	@Override
	public void remove() {
		throw new RuntimeException("not supposed to be used");
	}
	
}