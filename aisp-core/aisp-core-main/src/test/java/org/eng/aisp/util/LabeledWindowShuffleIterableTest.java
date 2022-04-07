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

import java.util.List;
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.util.IShuffleIterable;
import org.eng.util.ShufflizingIterable;
import org.junit.Assert;
import org.junit.Test;

public class LabeledWindowShuffleIterableTest extends AbstractLabeledWindowShuffleIterableTest {

	
	protected IShuffleIterable<SoundRecording> getLabeledWindowShuffleIterable(String trainingLabel, IShuffleIterable<SoundRecording> sounds, int count) {
		return new LabeledWindowShuffleIterable<SoundRecording>(sounds, trainingLabel);
	}
	


	@Test
	public void testFiltering() throws AISPException {
		String TrainingLabel1 = "status1";
		String TrainingLabel2 = "status2";
		List<SoundRecording> srList1 = SoundTestUtils.createNormalAbnormalTrainingRecordings(TrainingLabel1, "normal", 4, "abnormal", 4);
		List<SoundRecording> srList2 = SoundTestUtils.createNormalAbnormalTrainingRecordings(TrainingLabel2, "normal", 4, "abnormal", 4);
		srList1.addAll(srList2);
		IShuffleIterable<SoundRecording> sounds= new ShufflizingIterable<SoundRecording>(srList1);
		sounds =  new LabeledWindowShuffleIterable<SoundRecording>(sounds, TrainingLabel1);
		for (SoundRecording sr : sounds) {
			Properties labels = sr.getLabels();
			Assert.assertTrue(labels.getProperty(TrainingLabel1) != null);
			Assert.assertTrue(labels.getProperty(TrainingLabel2) == null);
		}
	}

}
