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

import java.util.Collections;
import java.util.List;

import org.eng.ENGException;
import org.eng.aisp.AISPException;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.util.AbstractShuffleIterableTest;
import org.eng.util.IShuffleIterable;
import org.eng.util.ShufflizingIterable;
import org.junit.Assert;




public abstract class AbstractLabeledWindowShuffleIterableTest extends AbstractShuffleIterableTest {

	public AbstractLabeledWindowShuffleIterableTest() {
		super();
	}

	@Override
	protected IShuffleIterable<SoundRecording> getRepeatableShuffleIterable(int returnedItemCount) throws ENGException {
		String trainingLabel = "getShuffleIterable";
		IShuffleIterable<SoundRecording> iter = getShuffleIterableSounds(trainingLabel, returnedItemCount); 
		iter = getLabeledWindowShuffleIterable(trainingLabel,iter, returnedItemCount) ;
		List<SoundRecording> srList = SoundTestUtils.iterable2List(iter);
		Assert.assertTrue("Test is not set up correctly to generate the correct number of items", srList.size() == returnedItemCount);
		return iter;
	}

	/**
	 * Used by {@link #getRepeatableShuffleIterable()} to create a the iterable using the sounds created by {@link #getIterableSounds(String)}.
	 * @param trainingLabel the same value that was passed to {@link #getIterableSounds(String)}.
	 * @param iter the iterable to be tested.
	 * @param count TODO
	 * @return never null
	 * @throws AISPException 
	 */
	protected abstract IShuffleIterable<SoundRecording> getLabeledWindowShuffleIterable(String trainingLabel, IShuffleIterable<SoundRecording> iter, int count) throws ENGException;


	/**
	 * Get the set of sounds to be placed into the shuffling iterable under test.
	 * @return never null or empty.
	 */
	protected Iterable<SoundRecording> getIterableSounds(String trainingLabel, int returnedItemCount) {
		List<SoundRecording> srList = SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, "normal", returnedItemCount, "abnormal", 0);
		Collections.shuffle(srList);	// Not strictly necessary, but might be helpful to some implementations
		return srList;
	}

	protected IShuffleIterable<SoundRecording> getShuffleIterableSounds(String trainingLabel, int returnedItemCount) {
		Iterable<SoundRecording> sounds = getIterableSounds(trainingLabel, returnedItemCount);
		IShuffleIterable<SoundRecording> iter = new ShufflizingIterable<SoundRecording>(sounds);
		return iter;
	}

}