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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;



public class SoundClipXYZTest extends SoundClipTest {
	
	private static int DIMENSIONS_TESTED = 3;

	@Override
	protected List<IDataWindow> getSynchronizedDataWindow(double startTime, int samplingRate, int sampleSize) {
//		int count = 3;
		int durationMsec =  (int)(1000.0 * (sampleSize) / samplingRate);
		int htz = 20;
		List<IDataWindow> windowList = new ArrayList<IDataWindow>();
		SoundRecording sr = SoundTestUtils.create3DTrainingSounds("nomatter", 1, startTime, durationMsec, samplingRate, htz).get(0);
		windowList.add(sr.getDataWindow());
		return windowList;
	}

	
	protected int getInterleavedDataDimension() {
		return DIMENSIONS_TESTED;
	}

	
	/**
	 * Test to make sure we don't get indexes that are not on proper boundaries (i.e. index % 3 == 0)
	 */
	@Test
	public void testMoreSubWindows() {
		int count=0;
		
//		new SoundClip(1, 1000, new double[477], 3).subWindow(1, 994.1886792452831 );
//		new SoundClip(1, 1000, new double[15], 3).subWindow(131.0, 131.0 + 133.2);

		for (int len=15 ; len<1000 ; len += 21) {
//		for (int len=477 ; len<1000 ; len += 21) {
			double[] data = new double[len];
			double startMsec = 1;
			double endMsec = 1000;
			double samplingRate = len / (endMsec - startMsec) * 1000.0 / 3 ;
			SoundClip clip = new SoundClip(startMsec, endMsec, data, 3);
			for (double start=startMsec ; start< clip.getDurationMsec() ; start+= 10) {
//			for (double start=901.0; start< clip.getDurationMsec() ; start+= 10) {
				for (double duration=2*samplingRate; duration< clip.getDurationMsec() ; duration+= 11) {
//				for (double duration=10.01001001; duration< clip.getDurationMsec() ; duration+= 11) {
					count++;
					String msg = count + ": len=" + len + ", start=" + start + ", duration=" + duration;
//					System.out.println(msg);
//					System.out.flush();
					try {
						double end = start+duration;
						if (start >= clip.getStartTimeMsec() && end <= clip.getEndTimeMsec()) {
							IDataWindow<double[]>subWindow = clip.subWindow(start, start + duration);
							Assert.assertTrue(subWindow != null);
						}
					} catch (Exception e) {
						e.printStackTrace();
						Assert.fail(msg + ": " +  e.getMessage());
					}
				}
			}
			try {
				IDataWindow<double[]>subWindow = clip.subWindow(startMsec, endMsec + 3); 
				Assert.assertTrue(subWindow == null);
				subWindow = clip.subWindow(startMsec-1, endMsec + 3); 
				Assert.assertTrue(subWindow == null);
			} catch (Exception e) {
				Assert.fail("off end: "+ e.getMessage());
			}
		}

	}
}
