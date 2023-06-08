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

import java.util.List;

import org.eng.ENGTestUtils;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	BasicAuthTokenTest.class,	
	IthIterableTest.class,
	IthIterableSoundProxyTest.class,
	IthShuffleIterableTest.class,
	IthShuffleIterableTest2.class,
	ShufflizingIterableTest.class,
	ItemReferenceIteratorTest.class,
	ShufflizingItemReferenceIterableProxyTest.class,
	JScriptEngineTest.class
})
	
public class ENGUtilTestSuite {

	public static List<Integer> makeList(int count) {
		return ENGTestUtils.makeList(count);
//		List<Integer> intList = new ArrayList<Integer>();
//		for (int i=0 ; i<count; i++)
//			intList.add(i);
//		return intList;
	}

//
//	public static List<SoundRecording> generateTestRecordings(String trainingLabel, String[] labelValues) {
//		return ClientUtilTestSuite.generateTestRecordings(trainingLabel, labelValues);
//		int channels = 1;
//		int samplingRate = 100;
//		int bitsPerSample = 8;
//		int startMsec = 0;
//		int durationMsec = 100;
//		int spacingMsec = 0;
//		int htz = 10;
//	
//		List<SoundRecording> srList = new ArrayList<SoundRecording>();
//		for (String labelValue : labelValues) {
//			List<SoundRecording> sr = SoundTestUtils.createTrainingRecordings(1, channels, samplingRate, bitsPerSample, startMsec, durationMsec, spacingMsec, htz, trainingLabel, labelValue); 
//			srList.addAll(sr);
//		}	
//		return srList;
//	}


}
