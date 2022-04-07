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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.eng.aisp.AISPException;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.IDataWindowFactory;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundClipFactory;
import org.eng.util.MathUtil;

public class FrequencyUtils {

	/**
	 * Create new clips that hvae their frequencies shifted by the request amounts.
	 * @param clip the clip to create new clips from.
	 * @param freqShifts a list of frequencies, in hertz and positive or negative, for which to 
	 * shift the frequency of the given clip to create a new clips.
	 * @return a list of SoundClips that same length as the given frequency shifts array.
	 */
	public static List<SoundClip> shiftFrequency(SoundClip clip, double[] freqShifts) {
		return shiftFrequency(clip, freqShifts, new SoundClipFactory());

//		double[] allData = clip.getData();
//		double samplesPerSecond = clip.getSamplingRate();
//
////		allData = VectorUtils.applyHammingWindow(allData, false);
//
//		// Split the window up into chunks and take shift the frequence of the data in each chunk.
//		Map<Integer, List<double[]>> shiftedDataMap = new HashMap<Integer, List<double[]>>();
//		int chunkSize = 8192;
//		for (int i=0 ; i<allData.length ; i += chunkSize) {
//			int lastIndex = i + chunkSize;
//			if (lastIndex > allData.length)
//				lastIndex = allData.length;
//			double[] subWindow = Arrays.copyOfRange(allData, i, lastIndex);
//			List<double[]> augList = FrequencyUtils.shiftFrequency(subWindow, samplesPerSecond, freqShifts);
//			
//			// For each frequency shift, keep a list of the chunks.
//			int augmentationIndex = 0;
//			for (double[] aug : augList) {
//				List<double[]> list = shiftedDataMap.get(augmentationIndex);
//				if (list == null) {
//					list = new ArrayList<double[]>();
//					shiftedDataMap.put(augmentationIndex, list);
//				}
//				list.add(aug);
//				augmentationIndex++;
//			}
//		}
//		
//		List<SoundClip> augmentedClips = new ArrayList<SoundClip>();
//		int channels = clip.getChannels();
//		int bitsPerSample = clip.getBitsPerSample();
//		
//		for (List<double[]> chunkList : shiftedDataMap.values()) {	
//			double[] unchunked = concatentate(chunkList);
////			VectorUtils.normalize(unchunked, false, false, true);
//			for  (int i=0 ; i<unchunked.length ; i++) {
//				if (unchunked[i] > 1)
//					unchunked[i] = 1;
//				else if (unchunked[i] < -1)
//					unchunked[i] = -1;
//			}
//			byte[] pcm = PCMUtil.double2PCM(unchunked, channels, bitsPerSample);
//			SoundClip newClip = new SoundClip(clip.getStartTimeMsec(), channels, bitsPerSample, samplesPerSecond, pcm);	
//			augmentedClips.add(newClip);;
//		}
//		return augmentedClips;
	}
	/**
	 * Create new clips that hvae their frequencies shifted by the request amounts.
	 * @param t the clip to create new clips from.
	 * @param freqShifts a list of frequencies, in hertz and positive or negative, for which to 
	 * shift the frequency of the given clip to create a new clips.
	 * @return a list of SoundClips that same length as the given frequency shifts array.
	 */
	public static <WINDOW extends IDataWindow<double[]>> List<WINDOW> shiftFrequency(IDataWindow<double[]> t, double[] freqShifts, IDataWindowFactory<double[], WINDOW> windowFactory) {
		double[] allData = t.getData();
		if (!(t instanceof IDataWindow))
			throw new IllegalArgumentException("Window must be an instance of " + IDataWindow.class.getName());
		IDataWindow<double[]> sw = (IDataWindow<double[]>)t;
		double samplesPerSecond = sw.getSamplingRate();

//		allData = VectorUtils.applyHammingWindow(allData, false);

		// Split the window up into chunks and take shift the frequence of the data in each chunk.
		Map<Integer, List<double[]>> shiftedDataMap = new HashMap<Integer, List<double[]>>();
		int chunkSize = 8192;
		for (int i=0 ; i<allData.length ; i += chunkSize) {
			int lastIndex = i + chunkSize;
			if (lastIndex > allData.length)
				lastIndex = allData.length;
			double[] subWindow = Arrays.copyOfRange(allData, i, lastIndex);
			List<double[]> augList = FrequencyUtils.shiftFrequency(subWindow, samplesPerSecond, freqShifts);
			
			// For each frequency shift, keep a list of the chunks.
			int augmentationIndex = 0;
			for (double[] aug : augList) {
				List<double[]> list = shiftedDataMap.get(augmentationIndex);
				if (list == null) {
					list = new ArrayList<double[]>();
					shiftedDataMap.put(augmentationIndex, list);
				}
				list.add(aug);
				augmentationIndex++;
			}
		}
		
		List<WINDOW> augmentedClips = new ArrayList<WINDOW>();
//		int channels = clip.getChannels();
//		int bitsPerSample = clip.getBitsPerSample();
		
		double startTimeMsec = t.getStartTimeMsec();
		double endTimeMsec = t.getEndTimeMsec();
		for (List<double[]> chunkList : shiftedDataMap.values()) {	
			double[] unchunked = concatentate(chunkList);
//			VectorUtils.normalize(unchunked, false, false, true);
			for  (int i=0 ; i<unchunked.length ; i++) {
				if (unchunked[i] > 1)
					unchunked[i] = 1;
				else if (unchunked[i] < -1)
					unchunked[i] = -1;
			}
			WINDOW newClip =  windowFactory.newDataWindow(startTimeMsec, endTimeMsec, unchunked);
			augmentedClips.add(newClip);;
		}
		return augmentedClips;
	}
	/**
	 * Create a single array from the list of arrays.
	 * @param chunks
	 * @return never null.  An array of length equal to the sum of the lengths of all arrays in the given list.
	 */
	private static double[] concatentate(List<double[]> chunks) {
		int dataSize = 0;
		for (double[] data : chunks) 
			dataSize += data.length;
		double[] unchunked = new double[dataSize];
		int index = 0;
		for (double[] data : chunks)  {
			System.arraycopy(data, 0, unchunked, index, data.length);
			index += data.length; 
		}
		return unchunked;
	}
	
	/**
	 * Shift the given sampled data by the requested frequencies, either positive or negative.
	 * Operation is performed by taking the FFT, shifting the FFT by a number of indexes, and then
	 * performing the inverse transform.
	 * @param data data to shift frequency for.  
	 * @param samplesPerSecond the rate at which the given data was sampled in samples per second.
	 * @param freqShifts array of frequencies, in hertz, by which to shift the data.
	 * @return a list of double arrays, one for each requested frequency shift.  Each returned array
	 * is the same length as the input.
	 */
	public static List<double[]> shiftFrequency(double[] data, double samplesPerSecond, double[] freqShifts) {
		int[] frequencyIndexShifts = new int[freqShifts.length];
		for (int i=0 ; i<freqShifts.length ; i++) 
			frequencyIndexShifts[i] = (int)(freqShifts[i] * samplesPerSecond / data.length / 2 + .5);
		return shiftFrequency(data, frequencyIndexShifts);
	}

	/**
	 * Shift the given data in frequency by shifting the complex FFT of the data by the given
	 * number of indices, either positive or negative, and then taking the inverse transform. 
	 * @param data
	 * @param freqIndexShifts array of integers representing the shift in index of the FFT of the data.
	 * t 
	 * @return a list of double arrays, one for each requested frequency shift.  Each returned array is the same
	 * length as the input.
	 */
	private static List<double[]> shiftFrequency(double[] data, int[] freqIndexShifts) {
		
//		data = VectorUtils.applyHammingWindow(data, false);
		int origDataLen = data.length;
		data = MathUtil.padToPowerOfTwo(data);
		//Using Apache maths to compute FFT (only supports input length that is a power of two)
	    FastFourierTransformer trans=new FastFourierTransformer(DftNormalization.UNITARY);
	    Complex[] fft = trans.transform(data,TransformType.FORWARD);		
	    List<double[]> augmented = new ArrayList<double[]>();
	    for (int shift : freqIndexShifts) {
	    	if (Math.abs(shift)*2 > data.length)
	    		throw new IllegalArgumentException("frequency shift (" + shift + ") is to large for given amount of data (" + data.length + ").");
	    	Complex[] shiftedFFT = shift(fft, shift);
			Complex[] newComplexData = trans.transform(shiftedFFT,TransformType.INVERSE);
			double[] newData = new double[origDataLen];
			for (int j=0 ; j<newData.length ; j++)
//				newData[j] = newComplexData[j].abs();
				newData[j] = newComplexData[j].getReal();
			augmented.add(newData);
			
	    }
	    return augmented;
	}

	/**
	 * Shift the the frequency spectrum by the requested number of array indices. 
	 * The given FFT is symmetric about the middle of the array, so that shifting
	 * is done on the lower half of the array and then mirrored with the complex
	 * conjugate of the lower half.  The DC component (0'th element) is copied into
	 * the destination.  Elements that are shifted into from missing data are
	 * set to complex(0,0).
	 * @param srcFFT a complex FFT that is symmetric about the middle of the array.
	 * @param shiftIndexCount the number of indices to shift the FFT.
	 * @return an array of the same size as the input.
	 */
	private static Complex[] shift(Complex[] srcFFT, int shiftIndexCount) {
		Complex[] shiftedFFT = new Complex[srcFFT.length];
	    if ((srcFFT.length & 1) != 0)
	    	throw new RuntimeException("src length must be a power of 2");

	    int midIndex = srcFFT.length / 2;	
	    // Shift the lower half.
	    shiftLower(srcFFT,shiftedFFT, shiftIndexCount, midIndex);

	    // Now mirror the complex conjugate into the upper half around the middle index.
	    for (int srcIndex=midIndex-1 ; srcIndex != 0; srcIndex--)  {
	    	int distance = midIndex - srcIndex;	// Number of indices from the middle
	    	int destIndex = midIndex + distance;	// 
	    	if (destIndex >= shiftedFFT.length)
	    		break;	// don't go off the end 
	    	shiftedFFT[destIndex] = shiftedFFT[srcIndex].conjugate();
	    }

	    shiftedFFT[0] = srcFFT[0];		// keep DC component
	    shiftedFFT[midIndex] = srcFFT[midIndex];
	    
	    return shiftedFFT;
	}

	/**
	 * Copy the lower half of the src array shifted by the request index, into the destination
	 * starting at index 1.
	 * @param src
	 * @param dest an array of at least size maxSrcIndex into which src values will be copied.
	 * @param shiftIndexCount the amount to shift by when copying from src to dest.
	 * @param maxSrcIndex the maximum index in the src to shift into dest.
	 */
	private static void shiftLower(Complex[] src, Complex[] dest, int shiftIndexCount, int maxSrcIndex) {
		int destIndex = 1;
		if (shiftIndexCount < 0) {	// Shift spectrum to the left (lower the frequency).
			shiftIndexCount = -shiftIndexCount;
			for (int srcIndex=shiftIndexCount+1 ; destIndex < dest.length && srcIndex < maxSrcIndex; srcIndex++, destIndex++) 
				dest[destIndex] = src[srcIndex];
			for ( ; destIndex < maxSrcIndex ; destIndex++) 
				dest[destIndex] = new Complex(0,0); 
		} else {					// Shift the spectrum to the right (raise the frequency)
			for ( ; destIndex <= shiftIndexCount && destIndex < dest.length; destIndex++) 
				dest[destIndex] = new Complex(0,src[destIndex].getImaginary()); 
			for (int srcIndex=1 ; destIndex < dest.length && srcIndex < maxSrcIndex; srcIndex++, destIndex++) 
				dest[destIndex] = src[srcIndex];
		}
	}
	
	public static void main(String[] args) throws IOException, AISPException {
//		SoundClip clip = SoundClip.readClip("/Users/IBM_ADMIN/git/iot-sounds/Sounds/industrial/OneCylinderEngine.wav");
//		SoundClip clip = SoundClip.readClip("/dev/sounds/harmon/base/Racing1.wav");
		SoundClip clip = SoundClip.readClip("/dev/sounds/harmon/base/Tornado1.wav");
		clip.writeWav("unshifted.wav");
	    SoundClip clip2= new SoundClip(clip.getStartTimeMsec(), clip.getChannels(),clip.getBitsPerSample(), clip.getSamplingRate(), clip.getPCMData());
	    clip2.writeWav("clip2.wav");
		List<SoundClip> augmented = FrequencyUtils.shiftFrequency(clip, new double[] { 0} );
		int index = 0;
		for (SoundClip aug : augmented) {
			aug.writeWav("shifted-" + index + ".wav");
			index++;
		}
	}

}
