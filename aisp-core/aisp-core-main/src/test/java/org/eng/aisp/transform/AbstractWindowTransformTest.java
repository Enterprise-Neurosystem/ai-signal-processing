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
import java.util.Properties;

import org.eng.aisp.IDataWindow;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.junit.Assert;
import org.junit.Test;



public abstract class AbstractWindowTransformTest {
	
	
	/**
	 * Populate the two lists with test data
	 * @param transforms a list of transforms to test.
	 * @param expectedCounts a list of integers identifying how many instances each transform should produce.
	 */
	protected abstract void getTransforms(List<ITrainingWindowTransform<double[]>> transforms, List<Integer> expectedCounts);

	protected abstract boolean isDurationPreserved();
	
	@Test
	public void testCounts() {
		List<ITrainingWindowTransform<double[]>> transforms = new ArrayList<>();
		List<Integer> expectedCounts = new ArrayList<Integer>();
		getTransforms(transforms, expectedCounts);
		Assert.assertTrue(transforms.size() == expectedCounts.size());	// sub-class has not defined getTransforms() correctly if this fails.
		
		String trainingLabel = "tlabel";
		String labelValue = "value";
		Properties labels = new Properties();
		labels.setProperty(trainingLabel, labelValue);
		int count = 1, durationMsec = 1000, htz = 1000;
		boolean addNoise = false;

		SoundRecording baseSR = SoundTestUtils.createTrainingRecordings(count, durationMsec, htz, labels, addNoise).get(0);
		
		boolean preservesDuration = isDurationPreserved();
		for (int i=0 ; i<transforms.size() ; i++) {
			ITrainingWindowTransform<double[]> t = transforms.get(i);
			int expectedCount = expectedCounts.get(i);
			testHelper(baseSR, trainingLabel, t, expectedCount, preservesDuration);
		}
		

	}
	/**
	 * @param baseSR
	 * @param trainingLabel
	 * @param t
	 * @param expectedCount
	 * @param labels
	 */
	protected void testHelper(SoundRecording baseSR, String trainingLabel, ITrainingWindowTransform<double[]> t, int expectedCount, boolean preservesDuration) {
		int count;
		Properties labels = baseSR.getLabels();
		IDataWindow<?> baseDW = baseSR.getDataWindow();
		Iterable<ILabeledDataWindow<double[]>> transformed = t.apply(trainingLabel,baseSR);
		count = 0; 
		for (ILabeledDataWindow<double[]> ldw : transformed) {
			IDataWindow<?> dw = ldw.getDataWindow();
			Assert.assertTrue(dw.getStartTimeMsec() == baseDW.getStartTimeMsec());
			if (preservesDuration)
				Assert.assertTrue(dw.getDurationMsec()  == baseDW.getDurationMsec());
			Properties tLabels = ldw.getLabels();
			Assert.assertTrue(tLabels.equals(labels));
			count++;
		}
		Assert.assertTrue(count == expectedCount);
	}

}
