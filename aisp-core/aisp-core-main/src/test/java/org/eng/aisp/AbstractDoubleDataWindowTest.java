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

import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractDoubleDataWindowTest extends AbstractSynchronizedDataWindowTest {
	
	protected abstract IDataWindow<double[]> getDataWindow(double startMsec, double endMsec, double[] data);
	
	/**
	 * Build a straight line signal with positive slope and make sure all
	 * sub-windows follow that line.
	 * Note: this test is not so specific to SoundClips and could be generalized
	 * to the superclass.
	 */
	@Test
	public void testSubWindowProgression() {
		// Create signal that is a line starting at 0 with a positive slope. 
		int samplingRate = 2000;
		int durationMsec = 1000;
		int subWindowMsec = 100;
		double slopeResolution = 1.0 / (2^16);	// 16 is the default bits/sample in our PCM
		double allowedSlopeError = 2* slopeResolution;
		int samplesPerSubWindow = (int)(samplingRate * subWindowMsec / 1000.0);
		int startMsec = 0;
		int endMsec = startMsec + durationMsec;
		int totalSamples = (int)(durationMsec / 1000.0 * samplingRate);
		double[] data = new double[totalSamples];
		
		// Generate our straight line signal and associated clip.
		double slope = 1.0 / totalSamples;
		data[0] = 0;
		for (int i=1 ; i<data.length ; i++) {
			data[i] = data[i-1] + slope;
		}
//		SoundClip lastSub = null, clip = new SoundClip(startMsec, endMsec, data);
		IDataWindow<double[]> lastSub = null, clip = getDataWindow(startMsec,endMsec, data);

		// Cut out successive/adjacent sub-windows making sure they follow each other
		// according to the straight line.
		double lastData = Double.NaN;
		for (int i=startMsec ; i<endMsec ; i += subWindowMsec) {
			String msg = "startMsec=" + i;
			IDataWindow<double[]>  sub = (IDataWindow<double[]>)clip.subWindow(i, i+subWindowMsec);
			Assert.assertTrue(msg, sub != null);
			Assert.assertTrue(msg, sub.getDurationMsec() == subWindowMsec);
			double subData[] = sub.getData();
			// Expected length/amount of data in the sub
			Assert.assertTrue(msg, subData.length == samplesPerSubWindow);

			// Make sure the data in the sub-window is increasing the same as our original signal.
			for (int k=1 ; k<subData.length ; k++) {
				String kmsg = msg + ", k=" + i;
				Assert.assertTrue(kmsg, subData[k] > subData[k-1]);
				double delta = subData[k] - subData[k-1];
				Assert.assertTrue(kmsg,(delta - slope) < allowedSlopeError);
			}
			// Check this sub-window relative to the last window.
			if (lastSub != null) {
				double firstData = subData[samplesPerSubWindow-1];
				Assert.assertTrue(msg, firstData > lastData);
				double delta = firstData - lastData;
				Assert.assertTrue(msg, (delta - slope) < allowedSlopeError);
			}
			lastData = sub.getData()[samplesPerSubWindow-1];
			lastSub = sub;
		}
	}



}
