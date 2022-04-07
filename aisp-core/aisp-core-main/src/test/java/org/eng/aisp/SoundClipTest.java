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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eng.ENGTestUtils;
import org.eng.aisp.IDataWindow.PadType;
import org.eng.aisp.util.PCMUtil;
import org.eng.util.ClassUtilities;
import org.junit.Assert;
import org.junit.Test;


public class SoundClipTest extends AbstractDoubleDataWindowTest {

	@Override
	protected List<IDataWindow> getSynchronizedDataWindow(double startTime, int samplingRate, int sampleSize) {
//		Assert.assertTrue("Must be zero so we can assume msec as the time unit", startTime == 0);
		List<IDataWindow> windowList = new ArrayList<IDataWindow>();
		double endTime = startTime + 1000.0 * (sampleSize) * (1.0 / samplingRate);	// SoundClip requires msec.
		
		for (int channels = 1 ; channels <= 2; channels++) {
			for (int bitsPerSample = 8 ; bitsPerSample <= 16 ; bitsPerSample += 8) {
				byte[] samples = fill(sampleSize, channels, bitsPerSample/8);
				SoundClip clip = new SoundClip(startTime, channels, bitsPerSample, samplingRate, samples);
				Assert.assertTrue(clip.getEndTimeMsec() == endTime);
				windowList.add(clip);
			}
		}
		
		return windowList;
	}

	private byte[] fill(int sampleSize, int channels, int bytesPerValue) {
		byte[] samples = new byte[sampleSize * channels * bytesPerValue];
		if (sampleSize > 128)
			throw new IllegalArgumentException("Only handles values held in a single byte");
		int bytesPerIndex = channels * bytesPerValue;
		for (int i=0 ; i<sampleSize ; i++) {
			for (int j=0 ; j<channels ; j++) {
				int index = i * bytesPerIndex + (j*bytesPerValue);
				samples[index] = (byte)i;
			}
		}
		return samples;
	}
	@Test 
	public void testZeros() 	 {
		SoundClip clip1 = new SoundClip(0,1000, new double[] { 0, 0, 0});
		SoundClip clip2 = new SoundClip(1, 8, 3, clip1.getPCMData());
		double[] data2 = clip2.getData();
		for (int i=0 ; i<data2.length ; i++) {
			Assert.assertTrue(data2[i] == 0);
		}
	}

	@Test 
	public void testPaddingValues() {
		int samplesPerSecond = 100;
		double startTime = 0;
		int numSamples = 100;
		List<IDataWindow> windowList = getSynchronizedDataWindow(startTime,samplesPerSecond,numSamples);
		PadType[] types = { PadType.ZeroPad, PadType.DuplicatePad};
		
		for (IDataWindow window : windowList) {
			SoundClip clip = (SoundClip)window;
			double clipData[] = clip.getData();
			double durationMsec = clip.getDurationMsec();
			for (PadType pt : types) {
				SoundClip padded = (SoundClip)clip.resize(durationMsec*2.5, pt);
				double paddedData[] = padded.getData();
				Assert.assertTrue(paddedData.length > clipData.length * 2);
				for (int i=0 ; i<clipData.length ; i++) 
					Assert.assertTrue(paddedData[i] == clipData[i]);
				if (pt == PadType.ZeroPad) {
					for (int i=clipData.length ; i< paddedData.length ; i++) 
						Assert.assertTrue(paddedData[i] == 0);
				} else if (pt == PadType.DuplicatePad) {
					int clipIndex = 0;
					for (int i=0 ; i+clipData.length< paddedData.length ; i++)  {
						Assert.assertTrue("i=" + i, paddedData[i+clipData.length] == clipData[clipIndex++]);
						if (clipIndex == clipData.length)
							clipIndex = 0;
					}
				}
			}
		}
	}
	
	/**
	 * Test pcm->double->pcm->double gives same double values.
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 */
	@Test
	public void testPCM2Double2PCM2Double() throws IOException, ClassNotFoundException {
		int samplesPerSecond = 100;
		double startTime = 0;
		int numSamples = 100;
		List<IDataWindow> windowList = new ArrayList<IDataWindow>(); 
		windowList.addAll(getSynchronizedDataWindow(startTime,samplesPerSecond,numSamples));
		Iterable<SoundClip> chillers = SoundClip.readClips(ENGTestUtils.GetTestData("chiller"));
		for (SoundClip c : chillers)
			windowList.add(c);
		
		// Create windows loading them with PCM data.
		windowList.addAll(SoundTestUtils.createClips(1, 0, 0, 1000, 1, 44100,  8, 1, 0, 1000, true));
		windowList.addAll(SoundTestUtils.createClips(1, 0, 0, 1000, 1, 44100, 16, 1, 0, 1000, true));
		windowList.addAll(SoundTestUtils.createClips(1, 0, 0, 1000, 1, 44100, 32, 1, 0, 1000, true));
		windowList.addAll(SoundTestUtils.createClips(1, 0, 0, 1000, 2, 44100,  8, 1, 0, 1000, true));
		windowList.addAll(SoundTestUtils.createClips(1, 0, 0, 1000, 2, 44100, 16, 1, 0, 1000, true));
		windowList.addAll(SoundTestUtils.createClips(1, 0, 0, 1000, 2, 44100, 32, 1, 0, 1000, true));
		SoundClip bits32 = SoundClip.readClip(ENGTestUtils.GetTestData("bitPerSample32.wav"));	// BTW, Audacity thinks this is floating point, but fmt format chunk says PCM 
		windowList.add(bits32);
		int index = 0;
		for (IDataWindow window : windowList) {	
			String msg = "clip index=" + index;
			SoundClip clip = (SoundClip)window;
			double[] data = clip.getData(); 	// Get data converted from PCM to double[]
			double allowedPercentError = 1.0 / (2^clip.getBitsPerSample() - 1); 
			
			// Recreate the clip with the data we convert from double[] to PCM externally.
			byte[] pcm = PCMUtil.double2PCM(data, clip.getChannels(), clip.getBitsPerSample()); 
			SoundClip newClip = new SoundClip(0, clip.getChannels(), clip.getBitsPerSample(), clip.getSamplingRate(), pcm);
			double[] newData = newClip.getData();
			Assert.assertTrue(msg, newData.length == data.length);
			for (int i=0 ; i<newData.length ; i++) {
				Assert.assertTrue(msg +",i=" + i,SoundTestUtils.doublePercentEquality(newData[i], data[i], allowedPercentError));
			}
			// Now recreate the clip using the data[] that was created from the PCM data internal to the clip 
			newClip = new SoundClip(0, 1000, data, clip.getChannels(), clip.getBitsPerSample()); 
			newClip = (SoundClip) ClassUtilities.deserialize(ClassUtilities.serialize(newClip));	// Drop the internal transient data[]
			newData = newClip.getData();	// This will then be recomputed from the internal PCM data and not the transient data[]
			Assert.assertTrue(msg, newData.length == data.length);
			for (int i=0 ; i<newData.length ; i++) {
				Assert.assertTrue(msg +",i=" + i,SoundTestUtils.doublePercentEquality(newData[i], data[i], allowedPercentError));
			}
			index++;
		}
	}

	@Override
	protected IDataWindow<double[]> getDataWindow(double startMsec, double endMsec, double[] data) {
		return new SoundClip(startMsec, endMsec, data);
	}

}
