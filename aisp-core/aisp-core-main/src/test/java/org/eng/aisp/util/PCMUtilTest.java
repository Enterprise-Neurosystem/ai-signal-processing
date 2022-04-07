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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Properties;
import java.util.Random;

import org.eng.ENGTestUtils;
import org.eng.aisp.AISPException;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundTestUtils;
import org.junit.Assert;
import org.junit.Test;

public class PCMUtilTest {
	
	@Test
	public void testPCMRangeMapping() {
		testRange(1, 8);
		testRange(2, 8);
		testRange(1, 16);
		testRange(2, 16);
		testRange(1, 32);
		testRange(2, 32);
	}

	public void testRange(int channels, int bitsPerSample) {
		double data[] = new double[] { -1, 0, 1};
		byte[] pcm = PCMUtil.double2PCM(data, channels, bitsPerSample);
		validateBoundsAndZeroValue(pcm, channels, bitsPerSample);
		double data2[] = PCMUtil.pcm2Double(pcm, channels, bitsPerSample);
		Assert.assertTrue(data2[0] == -1);
		Assert.assertTrue(data2[1] == 0);
		if (PCMUtil.USE_SYMETRIC_RANGE)
			Assert.assertTrue(data2[2] > .99 && data2[2] <= 1);	
		else
			Assert.assertTrue(data2[2] == 1);	
		pcm = PCMUtil.double2PCM(data, channels, bitsPerSample);
		validateBoundsAndZeroValue(pcm, channels, bitsPerSample);
	}

	/**
	 * @param pcm with 3 values. first is min pcm value, 2nd is 0, and 3rd is max pcm value.
	 * @param bitsPerSample
	 * @throws IllegalArgumentException
	 */
	private void validateBoundsAndZeroValue(byte[] pcm, int channels, int bitsPerSample) throws IllegalArgumentException {
		ByteBuffer byteBuffer = ByteBuffer.wrap(pcm);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		switch (bitsPerSample) {
			case 8: 
				Assert.assertTrue((int)byteBuffer.get() == -128);
				if (channels == 2) Assert.assertTrue((int)byteBuffer.get() == -128);
				Assert.assertTrue((int)byteBuffer.get()== 0);
				if (channels == 2) Assert.assertTrue((int)byteBuffer.get()== 0);
				Assert.assertTrue((int)byteBuffer.get()== 127);
				if (channels == 2) Assert.assertTrue((int)byteBuffer.get()== 127);
				break;
			case 16: 
				Assert.assertTrue((int)byteBuffer.getShort() == Short.MIN_VALUE);
				if (channels == 2) Assert.assertTrue((int)byteBuffer.getShort() == Short.MIN_VALUE);
				Assert.assertTrue((int)byteBuffer.getShort()== 0);
				if (channels == 2) Assert.assertTrue((int)byteBuffer.getShort()== 0);
				Assert.assertTrue((int)byteBuffer.getShort()== Short.MAX_VALUE);
				if (channels == 2) Assert.assertTrue((int)byteBuffer.getShort()== Short.MAX_VALUE);
				break;
			case 32: 
				Assert.assertTrue((int)byteBuffer.getInt() == Integer.MIN_VALUE);
				if (channels == 2) Assert.assertTrue((int)byteBuffer.getInt() == Integer.MIN_VALUE);
				Assert.assertTrue((int)byteBuffer.getInt()== 0);
				if (channels == 2) Assert.assertTrue((int)byteBuffer.getInt()== 0);
				Assert.assertTrue((int)byteBuffer.getInt()== Integer.MAX_VALUE);
				if (channels == 2) Assert.assertTrue((int)byteBuffer.getInt()== Integer.MAX_VALUE);
				break;
			default: throw new IllegalArgumentException(bitsPerSample + " bits/sample is not supported (yet)."); 
		}
	}

	@Test
	public void testDoublePCMInMemory() throws IOException, AISPException {
		doublePCM(1,8, false);
		doublePCM(1,16, false);
		doublePCM(2,8, false);
		doublePCM(2,16, false);
	}

	@Test
	public void testDoublePCMInWavFile() throws IOException, AISPException {
		doublePCM(1,8, true);
		doublePCM(1,16, true);
		doublePCM(2,8, true);
		doublePCM(2,16, true);
	}
	
	@Test
	public void testExtraSubChunks() throws IOException, AISPException {
		String file = ENGTestUtils.GetTestData("extra-subchunk.wav");
		SoundClip clip = PCMUtil.ReadPCM(file);
		file = "ExtraSubChunks-test.wav";
		File f = new File(file);
		SoundClip clip2 = null;
		Exception exception = null;
		try {
			PCMUtil.PCMtoWAV(f.getAbsolutePath(), clip);
			clip2 = PCMUtil.ReadPCM(f.getAbsolutePath());
		} catch (Exception e) {
			exception = e;
		} finally {
			f.delete();
		}
		if (exception != null)
			Assert.fail("Could not write or read: " + exception.getMessage());
		Assert.assertTrue("did not read the clip", clip2 != null);
		Assert.assertTrue(clip.equals(clip2));

	}
	
	@Test 
	public void testCopy() {
		int msecSpacing = 0;
		int count = 1;
		int msecDuration = 1000;
		for (double msecStart = 0 ; msecStart <= 1; msecStart++) {
			for (int channels = 1; channels <= 2 ; channels++) {
				for (int samplingRate = 20000 ; samplingRate <= 40000 ; samplingRate += 20000) {
					for (int bitsPerSample=8 ; bitsPerSample<=16 ; bitsPerSample += 8) {
						SoundClip clip = SoundTestUtils.createClips(count, msecStart, msecSpacing, 
							msecDuration, channels, samplingRate, bitsPerSample, 1, 0, 1000, false).get(0);
						SoundClip copy = new SoundClip(msecStart, clip);
						validateEqual(clip, copy);

						byte[] bytes = clip.getPCMData();
						copy = new SoundClip(msecStart, channels, bitsPerSample, samplingRate, bytes);
						validateEqual(clip, copy);
						
						bytes = clip.getWAVBytes();
						try {
							copy = new SoundClip(msecStart, bytes);
						} catch (IOException e) {
							e.printStackTrace();
							Assert.fail("Got exception on wav bytes: " + e.getMessage());
						}
						validateEqual(clip, copy);
						

					}
				}
			}
		}
	}

	/**
	 * @param clip
	 * @param copy
	 */
	protected void validateEqual(SoundClip clip, SoundClip copy) {
		Assert.assertTrue(copy.equals(clip));
		
		// Just an extra check that the data is preserved.
		double[] data = clip.getData();
		double[] dataCopy = copy.getData();
		Assert.assertArrayEquals(data, dataCopy, .0001);

		// Just an extra check that the PCM data is preserved.
		byte[] bytes = clip.getPCMData();
		byte[] bytesCopy = copy.getPCMData();
		Assert.assertArrayEquals(bytes,bytesCopy);
	}
	
	
	
	private void doublePCM(int channels, int bitsPerSample, boolean useFile) throws IOException, AISPException {
		System.out.println("***** Channels: " + channels + " BitsPerSample: " + bitsPerSample);
		
        Random rand = new Random(32432412);
        
        double[] data = new double[1000];
        
        for (int i=0; i<data.length; i++) {
        	data[i] = 2 * (rand.nextDouble() - 0.5);
        }
        data[0] = 1;
        data[1] = -1;
        
        byte[] pcm = PCMUtil.double2PCM(data, channels, bitsPerSample);
        double[] data2;
        if (useFile) {
        	// Send the bytes through a wav file and make sure they come back the same.
        	File wav = new File("PCMUtilTest.wav");
        	int samplingRate = data.length;
        	wav.delete();
        	PCMUtil.PCMtoWAV(wav.getAbsolutePath(), pcm, samplingRate, channels, bitsPerSample);
        	SoundClip clip = PCMUtil.ReadPCM(wav.getAbsolutePath());
        	data2 = clip.getData();
        	wav.delete();
        } else {
        	data2 = PCMUtil.pcm2Double(pcm, channels, bitsPerSample);
        }
        
        Assert.assertTrue(data.length == data2.length);
        
        double threshold = 1.0 / (Math.pow(2, bitsPerSample-1) - 1);
        for (int i=0; i<data.length; i++) {
        	Assert.assertTrue(data2[i] >= -1);
        	Assert.assertTrue(data2[i] <= 1);
        	double diff = Math.abs(data[i] - data2[i]);
//        	System.out.println("data: " + data[i] + " data2: " + data2[i] + " difference: " + diff);
        	Assert.assertTrue(diff + " > " + threshold, diff <= threshold);  //Difference should not exceed the precision of corresponding number of bits
        }
	}
	
	
	@Test
	public void testWriteReadDimensionality() throws IOException {
		SoundClip clip = SoundTestUtils.create3DTrainingSounds("nomatter", 1, 1000).get(0).getDataWindow();
		Assert.assertTrue(clip.getInterleavedDataDimensions() == 3);	// make sure we start off correctly
		byte[] wav = PCMUtil.PCMtoWAV(clip);
		SoundClip clip2 = PCMUtil.WAVtoPCM(wav);
		Assert.assertTrue(clip2.getInterleavedDataDimensions() == 3);
		Assert.assertTrue(clip2.equals(clip));	// And for good measure, test equality too.

		clip = SoundTestUtils.createTrainingRecordings(1, 1000, 1000, new Properties(), false).get(0).getDataWindow();
		Assert.assertTrue(clip.getInterleavedDataDimensions() == 1);	// make sure we start off correctly
		wav = PCMUtil.PCMtoWAV(clip);
		clip2 = PCMUtil.WAVtoPCM(wav);
		Assert.assertTrue(clip2.getInterleavedDataDimensions() == 1);
		Assert.assertTrue(clip2.equals(clip));	// And for good measure, test equality too.
	}
}
