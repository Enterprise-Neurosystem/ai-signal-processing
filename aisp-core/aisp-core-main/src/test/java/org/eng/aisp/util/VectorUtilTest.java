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
package org.eng.aisp.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author wangshiq
 *
 */

public class VectorUtilTest {
	
	@Test
	public void testInterpolate() {
		interpolateInstance(44100, 44100, 100);
		interpolateInstance(8000, 44100, 100);
		interpolateInstance(16000, 44100, 100);
		interpolateInstance(22050, 44100, 100);
		interpolateInstance(44000, 44100, 100);
		interpolateInstance(44099, 44100, 100);
	}
	
	private void interpolateInstance(int origSamplingRate, int newSamplingRate, int origArrayLength) {
		double[] origArray = new double[origArrayLength];
		for (int i=0; i<origArrayLength; i++) {
			origArray[i] = 3.5 * i;  //3.5 is an arbitrary constant
		}
		
		double[] newArray = VectorUtils.interpolate(origArray, origSamplingRate, newSamplingRate);
		
		Assert.assertTrue(Math.abs((newArray.length - 1) * (double)origSamplingRate / (double)newSamplingRate - (double)(origArrayLength - 1)) <= 1.0);
		
		double epsilon = 0.0000001;
		Double step = null;
		for (int i=1; i<newArray.length; i++) {
			if (step == null) step = newArray[i] - newArray[i-1];
			else Assert.assertTrue(Math.abs((newArray[i] - newArray[i-1]) - step) <= epsilon);
		}
		
		Assert.assertTrue(newArray[newArray.length - 1] == origArray[origArray.length - 1]);
	}
	
	@Test
	public void testAutoCorrelation() {
		double[] vector = new double[] { 1, 2, 1, 2 };

		double[] ac = VectorUtils.autoCorrelate(vector);
		Assert.assertTrue(ac.length == vector.length);
		Assert.assertTrue(ac[0] == 10.0 / 4);
		Assert.assertTrue(ac[0] == ac[2]); 
		Assert.assertTrue(ac[0] > ac[1]); 

		
	}
}
