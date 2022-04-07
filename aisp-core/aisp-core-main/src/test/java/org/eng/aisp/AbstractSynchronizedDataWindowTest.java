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
package org.eng.aisp;

import java.util.List;

import org.eng.aisp.IDataWindow.PadType;
import org.junit.Assert;
import org.junit.Test;


public abstract class AbstractSynchronizedDataWindowTest {


	/**
	 * Create one or more windows, each with samples starting at the given time containing the given number of samples. 
	 * @param startMsec start time of windows.
	 * @param samplingRate determines end time of window.
	 * @param sampleSize number of samples in each window.
	 * @return
	 */
	protected abstract List<IDataWindow> getSynchronizedDataWindow(double startMsec, int samplingRate, int sampleSize);
	
	@SuppressWarnings("rawtypes")
	@Test
	public void testSubWindowTimes() {
		final int samplesPerSecond = 1;
		final double baseStartMsec = 0;
		final int numSamples = 101;
		List<? extends IDataWindow> windowList = getSynchronizedDataWindow(baseStartMsec,samplesPerSecond,numSamples);
		double durationMsec = 1000 * (numSamples-1) / samplesPerSecond;

		for (IDataWindow window : windowList) {
			IDataWindow sw;
			double endMsec = baseStartMsec + durationMsec; 
//			validateSubWindow(window, baseStartMsec, endMsec, numSamples);	// Validate that the created windows are as expected.
//			
			sw = (IDataWindow)window.subWindow(baseStartMsec, endMsec);
			validateSubWindow(sw, baseStartMsec, endMsec, numSamples);

			// A half window centered in the original
			double startMsec = baseStartMsec + durationMsec /4;
			endMsec  	= startMsec + durationMsec / 2; 
			sw = (IDataWindow)window.subWindow(startMsec, endMsec);
			validateSubWindow(sw, startMsec, endMsec, numSamples/2);

			// A full window that extends beyond the original
			endMsec   = startMsec + durationMsec; 
			sw = (IDataWindow)window.subWindow(startMsec, endMsec);
			Assert.assertTrue(sw == null);

			// A full window that extends beyond the original to the left
			startMsec = startMsec -  durationMsec/2; 
			endMsec = startMsec + durationMsec; 
			sw = (IDataWindow)window.subWindow(startMsec, endMsec);
			Assert.assertTrue(sw == null);
			

		}
	}

	protected void validateSubWindow(IDataWindow window, double startMsec, double endMsec, int numSamples) {
		double durationMsec = endMsec - startMsec;
		int idc = this.getInterleavedDataDimension();
		double allowedTimeError = 1000 * (idc - 1) / window.getSamplingRate();
		Assert.assertTrue(SoundTestUtils.doubleEpsilonEquality(window.getStartTimeMsec(),startMsec, allowedTimeError));
		// Multiply allowed error by 2 since to account for error in both the start and end times.
		Assert.assertTrue(SoundTestUtils.doubleEpsilonEquality(window.getDurationMsec(),durationMsec, 2*allowedTimeError)); 
		// Multiply allowed error by 2 since to account for error in both the start and end times.
		Assert.assertTrue(SoundTestUtils.doubleEpsilonEquality(window.getEndTimeMsec(),endMsec, 2*allowedTimeError)); 
	}
	
//	protected void validateSubWindows(IDataWindow window, IDataWindow sw, int startIndex, int size) {
//		double startTime = window.getStartTimeMsec();
//		double samplesPerSecond = window.getSamplingRate();
//		double expectedStartTime = startTime + 1000 * startIndex * (1.0/samplesPerSecond);
//		double expectedEndTime = expectedStartTime + 1000 * (size - 1) * (1.0/samplesPerSecond);
//		Assert.assertTrue(sw.toString(), sw.getStartTimeMsec() == expectedStartTime);
//		Assert.assertTrue(sw.toString(),sw.getEndTimeMsec() == expectedEndTime);
//		Assert.assertTrue(sw.toString(), sw.getSampleSize() == size);
////		double[] samples = sw.getSampledData();
////		Assert.assertTrue(samples != null);
////		Assert.assertTrue(samples.length == size);
////		for (int i=0 ; i<samples.length ; i++) {
////			Assert.assertTrue(sw.toString() + ", index = " + i, samples[i] == startIndex + i);
////		}
//		
//	}

//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	@Test 
//	public void testSplitOnIndex() {
//		double samplesPerSecond = 1;
//		double startTime = 0;
//		int numSamples = 100;
//		List<? extends IDataWindow> windowList = getSynchronizedDataWindow(startTime,samplesPerSecond,numSamples);
//		List<? extends IDataWindow> splitWindows;
//
//		for (IDataWindow window : windowList) {
//			splitWindows = window.splitOnSamples(10, false);
//			verifyWindowSequence(window, 10, splitWindows);
//
//			splitWindows = window.splitOnSamples(30, true);
//			Assert.assertTrue(splitWindows.size() == 4);
//
//			splitWindows = window.splitOnSamples(30, false);
//			Assert.assertTrue(splitWindows.size() == 3);
//		}
//	}

//
	protected void verifyWindowSequence(IDataWindow window, int subWindowCount, List<? extends IDataWindow> splitWindows, boolean matchLastEndTime) {
		Assert.assertTrue(splitWindows.size() == subWindowCount);
		IDataWindow  last = null;
		Assert.assertTrue(splitWindows != null);
		Assert.assertTrue(splitWindows.size() > 0);
		Assert.assertTrue(splitWindows != null);
//		double sampleDeltaTime = 1000 * 1.0 / window.getSamplingRate();
		double allowedTimeError = 2 * 1000 * (this.getInterleavedDataDimension()) / window.getSamplingRate();
		for (IDataWindow dw : splitWindows) {
//			Assert.assertTrue(dw.getSampleSize() == subSize);
//			Assert.assertTrue(dw.getSamplingRate() == window.getSamplingRate());
			if (last == null) {
				Assert.assertTrue(dw.getStartTimeMsec() == window.getStartTimeMsec());
			} else {
				Assert.assertTrue(SoundTestUtils.doubleEpsilonEquality(dw.getStartTimeMsec(), last.getEndTimeMsec(), allowedTimeError));
			}
			last = dw;
		}
		if (matchLastEndTime)
			Assert.assertTrue(SoundTestUtils.doubleEpsilonEquality(window.getEndTimeMsec(), last.getEndTimeMsec(), allowedTimeError));

	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test 
	public void testSplitOnTime() {
		int samplesPerSecond = 100;
		double startTime = 0;
		int numSamples = 101;
		// Create 1 seconds clips, each with 100 samples. 
		List<? extends IDataWindow> windowList = getSynchronizedDataWindow(startTime,samplesPerSecond,numSamples);
		List<? extends IDataWindow> splitWindows;
		
		IDataWindow w = SoundTestUtils.createClips(1, 1000, 1000).get(0);
		List<IDataWindow> wl = w.splitOnTime(100, false);
		verifyWindowSequence(w, 10, wl,false);

		for (IDataWindow window : windowList) {
			splitWindows = window.splitOnTime(100, false);	
			verifyWindowSequence(window, 10, splitWindows,false);
			
			splitWindows = window.splitOnTime(300, true);
			verifyWindowSequence(window, 4, splitWindows,true);

			splitWindows = window.splitOnTime(300, false);
			verifyWindowSequence(window, 3, splitWindows, false);
		}
	}
	

	/** THis is to support classes under test that have multi-dimensional data in their data window */
	protected int getInterleavedDataDimension() {
		return 1;
	}

	@SuppressWarnings("rawtypes")
	@Test 
	public void testPaddingDataLengths() {
		int samplesPerSecond = 100;
		double startTime = 0;
		int numSamples = 100;
		List<? extends IDataWindow> windowList = getSynchronizedDataWindow(startTime,samplesPerSecond,numSamples);

		int idd = this.getInterleavedDataDimension();

		for (IDataWindow window : windowList) {
			double durationMsec = window.getDurationMsec();
			IDataWindow padded = (IDataWindow) window.resize(durationMsec*2, PadType.ZeroPad);
			double paddedDurationMsec = padded.getDurationMsec();
			Assert.assertTrue(durationMsec * 2 == paddedDurationMsec);
			// The last point of the old data is shared with the first point in the padding.
			int wSize = window.getSampleSize();
			int pSize = padded.getSampleSize();
			Assert.assertTrue(wSize * 2 == pSize); 
		}
	}
}
