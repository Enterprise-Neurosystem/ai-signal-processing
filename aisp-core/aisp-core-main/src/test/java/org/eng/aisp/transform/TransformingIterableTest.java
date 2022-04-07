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
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.junit.Assert;
import org.junit.Test;



public class TransformingIterableTest {

	@Test
	public void testIteration() {
		List<SoundRecording> srList = new ArrayList<SoundRecording>();
		int count = 3;
		int startMsec = 0;
		int durationMsec = 1000;
		String trainingLabel = "index";
		for (int i=0 ; i<count ; i++) {
			Properties labels = new Properties();
			labels.setProperty(trainingLabel, String.valueOf(i));
			List<SoundRecording> clips = SoundTestUtils.createTrainingRecordings(count, startMsec, durationMsec, 0, 1000, labels, false);
			srList.addAll(clips);
			startMsec += durationMsec;
		}
		
		int nCopies = 4;
		ITrainingWindowTransform<double[]> transform = new IdentityWindowTransform(nCopies);
		Iterable<ILabeledDataWindow<double[]>> iterable = new TransformingIterable<double[]>(trainingLabel, srList, transform);
		testIterable(iterable, srList, nCopies);
		// Test again to make sure it is really an iterable and can be re-iterated.
		testIterable(iterable, srList, nCopies);

	}

	private void testIterable(Iterable<ILabeledDataWindow<double[]>> windows, List<SoundRecording> srList, int nCopies) {
		Iterator<ILabeledDataWindow<double[]>> iterator =  windows.iterator();
		int total = 0;
		for (int i=0 ; i<srList.size(); i++) {
			ILabeledDataWindow<double[]> base = iterator.next();
			total++;
			int copies = 1;
			for (int j=1 ; j<nCopies ; j++) {
				ILabeledDataWindow<double[]> next = iterator.next();
				Assert.assertTrue(base.equals(next));
				copies++;
				total++;
			}
			Assert.assertTrue(copies == nCopies);
		}
		
		Assert.assertTrue(total == srList.size() * nCopies);
	}
}
