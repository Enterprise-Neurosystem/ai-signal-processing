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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eng.aisp.dataset.LabeledSoundFiles;
import org.eng.aisp.util.LabeledWindowToDataWindowShuffleIterable;
import org.eng.aisp.util.PCMUtil;
import org.eng.util.AbstractDefaultIterator;
import org.eng.util.IShuffleIterable;
import org.eng.util.Vector;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Stores the raw bits of a sound recording, modeled after PCM.
 * Sound is stored in a byte array and is characterized by the number of channels, bits per sample, and samples per second.
 * Data may be N-dimensional in which case it must be sampled at the same sampling rate and perfectly interleaved.
 * <p>
 * Care should be taken when creating new instances.  If possible, you should try to reuse and cache instances when possible in order to
 * be able to leverage the {@link #getInstanceID()} based caching that goes on in in our FeatureExtractionPipeline and elsewhere.
 * @author dawood
 *
 */
public class SoundClip extends DoubleWindow implements Serializable, IDoubleWindow { // Serializable for Spark.

	private static final long serialVersionUID = 8691515836352202592L;

	protected final int bitsPerSample;

	protected final int channels;
	
	/** Defines the number of double values per reading.  This is most often 1, but can be 3 for example for xyz accelerometer data.  */ 
	protected int interleavedDataDimensions = 0;

	protected transient byte[] pcmData = null;

	// For JSON/Jackson
	protected SoundClip() { 
		this(0, 1,8,1, new byte[] { 0,1 }); 
	}

	/**
	 * A convenience method on {@link PCMUtil#ReadPCM(double, java.net.URL, String)}. 
	 */
	public static SoundClip readClip(double startMsec, String fileOrURL, String format) throws IOException {
		return PCMUtil.ReadPCM(startMsec, fileOrURL, format);
	}

	/**
	 * A convenience method calling {@link PCMUtil#ReadPCM(long, String, String)} passing in 0 as
	 * the start time and null as the format.
	 */
	public static SoundClip readClip(String fileOrURL) throws IOException {
		return PCMUtil.ReadPCM(0, fileOrURL, null);
	}
	

	/**
	 * A convenience on {@link #getPCMMsec(int, int, double, byte[], int)} interleavedDataDimensions xx = 1.
	 */
	public static double getPCMMsec(int channels, int bitsPerSample, double samplesPerSecond, byte pcmData[]) {
		return getPCMMsec(channels, bitsPerSample, samplesPerSecond, pcmData, 1);
	}

	/**
	 * Get the length of the PCM data in milliseconds assuming there are also the given number of interleaved data samples in the pcmData..
	 * @param channels 
	 * @param bitsPerSample
	 * @param samplesPerSecond the logical sampling rate, independent of the size of a sample.
	 * @param pcmData
	 * @param sampleSize  the number of interleaved data contained in the PCM data.
	 * @return
	 */
	public static double getPCMMsec(int channels, int bitsPerSample, double samplesPerSecond, byte pcmData[], int sampleSize) {
		if (pcmData == null || pcmData.length == 0)
			return 0;
		int sampleCount = getSampleCount(channels,bitsPerSample,pcmData) / sampleSize ;
//		if (sampleCount <= 2)
//			sampleCount = 2;
		double msec = 1000.0 * (sampleCount)   / samplesPerSecond; 
//		double msec = 1000.0 * (sampleCount - 1)   / samplesPerSecond ;
//		double msec = 1000.0 * (sampleCount - interleavedDataDimensions)   / samplesPerSecond ;
		return msec; 
	}
	
	/**
	 * Get the number of distinct samples in the given pcm data which uses the given number of channels and bits/sample values.
	 * @param channels
	 * @param bitsPerSample
	 * @param pcmData
	 * @return
	 */
	public static int getSampleCount(int channels, int bitsPerSample, byte[] pcmData) {
		return pcmData.length / bytesPerPCMSample(channels, bitsPerSample);
	}

	
	/**
	 * A convenience on {@link #SoundClip(double, int, int, double, byte[])} with start time set to the current time and assuming the pcmData is 1-dimensional.
	 */
	public SoundClip(int channels, int bitsPerSample, double samplesPerSecond, byte pcmData[]) {
		this(System.currentTimeMillis() - getPCMMsec(channels, bitsPerSample, samplesPerSecond, pcmData), channels, bitsPerSample, samplesPerSecond, pcmData);
	}

	/**
	 * Get the number of bytes in a PCM sample for the given number of channels ands bits/sample values.
	 * @param channels
	 * @param bitsPerSample
	 * @return
	 */
	private static int bytesPerPCMSample(int channels, int bitsPerSample) {
		return channels * bitsPerSample / 8;
	}

	/**
	 * Used primarily in performance analysis to quickly copy a clip, but with a different start time.
	 * <p>
	 * Care should be taken when creating new instances.  If possible, you should try to reuse and cache instances when possible in order to
	 * be able to leverage the {@link #getInstanceID()} based caching that goes on in in our FeatureExtractionPipeline and elsewhere.
	 * <br>
	 * Note, this has the side effect of decoding the PCM data in the given clip.
	 * @param startTimeMsec
	 * @param clip
	 */
	public SoundClip(double startTimeMsec, SoundClip clip) { 
		this(startTimeMsec, startTimeMsec + clip.getDurationMsec(), clip.independentVector, clip.channels, clip.bitsPerSample, clip.samplesPerSecond, clip.getPCMData(), clip.getInterleavedDataDimensions());
	}
	
	/**
	 * Used to convert the interleave data count for a ciip, probably based on the DataTypeEnum.
	 * A convenience on {@link #SoundClip(double, double, int, int, byte[], int)} copying all data from the clip except the given interleavedDataDimensions.
	 */
	public SoundClip(int interleavedDataDimensions, SoundClip clip) { 
		this(clip.getStartTimeMsec(), clip.getEndTimeMsec(), clip.independentVector, clip.channels, clip.bitsPerSample, clip.samplesPerSecond, clip.getPCMData(), interleavedDataDimensions); 
		if (interleavedDataDimensions > 10) 
			AISPLogger.logger.warning("Interleaved data dimensions(" +interleavedDataDimensions + ") seems large.  Did you call the correct constructor?");
	}

	/**
	 * Shallow copy constructor. 
	 */
	public SoundClip(SoundClip clip) { 
		this(clip.startTimeMsec, clip);
	}

	/**
	 * Convenience on {@link #SoundClip(double, int, int, double, byte[], int)}  with interleavedDataDimensions=1 
	 */
	public SoundClip(double startTimeMsec, int channels, int bitsPerSample, double samplesPerSecond, byte pcmData[]) {
		this(startTimeMsec, channels, bitsPerSample, samplesPerSecond,pcmData,1);
	}

	/**
	 * 
	 * @param startTimeMsec
	 * @param channels
	 * @param bitsPerSample
	 * @param samplesPerSecond the number of logical (multi-dimensional) samples per second.
	 * @param pcmData the data representing the possibly multi-dimensional data.
	 * @param sampleSize the size of each sample in the given data.  Generally 1, but could be N for N-dimensional samples contained in the given data.
	 */
	public SoundClip(double startTimeMsec, int channels, int bitsPerSample, double samplesPerSecond, byte pcmData[], int sampleSize) {
		this(startTimeMsec, 
				startTimeMsec + getPCMMsec(channels, bitsPerSample, samplesPerSecond, pcmData, sampleSize), 
				(Vector)null, 
				channels, bitsPerSample, samplesPerSecond, pcmData, sampleSize);
	}

//	/**
//	 * A convenience on {@link #SoundClip(double, double, Vector, int, int, byte[], int)} with independent data set to null. 
//	 */
//	public SoundClip(double startTimeMsec, double endTimeMsec, int channels, int bitsPerSample, byte pcmData[], int interleavedDataDimensions) {
//		this(startTimeMsec, endTimeMsec, (Vector)null, channels, bitsPerSample, pcmData, interleavedDataDimensions);
//
//	}

	/**
	 * Create the clip from a set of bytes representing a wav formatted audio.
	 * @param startTimeMsec start time for this clip in milliseconds.
	 * @param wav array of bytes holding a wav audio clip.
	 * @throws IOException if there is a format error in the byte array.
	 */
	public SoundClip(double startTimeMsec, byte[] wav) throws IOException { 
		this(startTimeMsec, PCMUtil.WAVtoPCM(wav)); 
	}
	

	/**
	 * A convenience on {@link #SoundClip(double, double, Vector, double[], int)} with dimensions set to 1 and no independent data.
	 */
	public SoundClip(double startTimeMsec, double endTimeMsec, double[] data) {
		this(startTimeMsec, endTimeMsec, (Vector)null, data, 1);
	}

	/**
	 * A convenience on {@link #SoundClip(double, double, Vector, double[], int)} with no independent data.
	 */
	public SoundClip(double startTimeMsec, double endTimeMsec, double[] data, int interleavedDataDimensions) {
		this(startTimeMsec, endTimeMsec, (Vector)null, data, interleavedDataDimensions);
	}

	/**
	 * Create a SoundClip with single channel 16 bits/sample instance  using the given data data, possibly interleaved with multiple data items, covering the given start and end times. 
	 * @param startTimeMsec
	 * @param endTimeMsec
	 * @param independentData optional data at which the given data is located.
	 * @param data an array of values in the range -1 to 1 spanning the start and end time.  N-dimensional data captured at the same sampling rate may be interleaved where
	 * N is defined by the interleavedDataDimensions parameter.   Data is interleaved with no missing data so that for example, with N=2
	 * data would contain (x1,y1,x2,y2,x3,y3...xN,yN) and interleavedDataDimensions would be set to 2.
	 * @param interleavedDataDimensions if larger than 1, then the given data is N-dimensional data perfectly interleaved. 
	 */
	public SoundClip(double startTimeMsec, double endTimeMsec, Vector independentData, double[] data, int interleavedDataDimensions) {
		this(startTimeMsec, endTimeMsec, independentData, data, 1, 16, interleavedDataDimensions);
	}
	
	protected SoundClip(double startTimeMsec, double endTimeMsec, double[] data, int channels, int bitsPerSample) {
		this(startTimeMsec, endTimeMsec, null, data, channels, bitsPerSample, 1);
	}

	/**
	 * Provided primarliy for testing to allow setting of channels and bits per sample.
	 * @param startTimeMsec
	 * @param endTimeMsec
	 * @param independentData
	 * @param data
	 * @param channels
	 * @param bitsPerSample
	 * @param interleavedDataDimensions
	 */
	protected SoundClip(double startTimeMsec, double endTimeMsec, Vector independentData, double[] data, int channels, int bitsPerSample, int interleavedDataDimensions) {
		this(startTimeMsec, endTimeMsec, data == null ? 1 // And throw exception below
											: getSamplesPerSecond(startTimeMsec, endTimeMsec, data.length)/interleavedDataDimensions, 
											independentData, data, 
											channels, bitsPerSample,
											interleavedDataDimensions);
	}
	
	protected SoundClip(double startTimeMsec, double endTimeMsec, double samplingRate, Vector independentData, double[] data, int interleavedDataDimensions) {
		this(startTimeMsec, endTimeMsec, samplingRate, independentData,  data, 1, 16, interleavedDataDimensions); 
	}

	
	/**
	 * }
	 * This is protected  as it is discourage to provided all of start/stop time, sampling rate and data[] as the user
	 * could make those inconsistent.
	 * @param startTimeMsec
	 * @param endTimeMsec
	 * @param samplingRate the number of logical (i.e. possibly multi-dimensional) samples per second.
	 * @param independentData
	 * @param data the array of data containing the N-dimensional samples, per interleavedDataDimensions
	 * @param interleavedDataDimensions the dimensionality of a sample.
	 */
	protected SoundClip(double startTimeMsec, double endTimeMsec, double samplingRate, Vector independentData, double[] data, int channels, int bitsPerSample, int interleavedDataDimensions) {
		super(startTimeMsec, endTimeMsec, samplingRate, independentData, data);

		if (data == null )
			throw new IllegalArgumentException("data can not be null");
		else if (data.length < 2)
			throw new IllegalArgumentException("data must be length 2 or larger");
		if (interleavedDataDimensions < 0)
			throw new IllegalArgumentException("interleaved data count be 1 or larger");;
		if (interleavedDataDimensions > 1) {
			if (data.length % interleavedDataDimensions != 0)
				throw new IllegalArgumentException("data length (" + data.length +") must be a multiple of the interleaved data length( " + interleavedDataDimensions +")") ;
		}
		this.channels = channels;
		this.bitsPerSample = bitsPerSample;
		this.interleavedDataDimensions = interleavedDataDimensions;
	}
	
	/**
	 * Get the number of PCM bytes for the given inputs.
	 * @param samples
	 * @param channels
	 * @param bitsPerSample
	 * @return
	 */
	private static int getPCMSize(int samples, int channels, int bitsPerSample) {
		int bytes = samples * channels * bitsPerSample>>3; 	// div by 8
		return bytes;
	}


	/**
	 * Protected in order to keep end users from providing all of start/stop time, sapmling rate, and pcmData[] which need
	 * to consistent.
	 * @param startTimeMsec
	 * @param endTimeMsec
	 * @param independentData
	 * @param channels
	 * @param bitsPerSample
	 * @param samplingRate the logical sampling rate independent of the size of a sample, independent of the sample size.
	 * @param pcmData
	 * @param sampleSize the size of the N-dimensional vector sample contained in the given PCM data.  Generally 1, but can be N for sample with N readings. 
	 */
	protected SoundClip(double startTimeMsec, double endTimeMsec, Vector independentData, int channels, int bitsPerSample, double samplingRate, byte pcmData[], int sampleSize) {
		super(startTimeMsec, 
				endTimeMsec,
				samplingRate,
				independentData,
				PCMUtil.pcm2Double(pcmData, channels, bitsPerSample)
				);
			
		if (bitsPerSample != 8 && bitsPerSample != 16 && bitsPerSample != 24 && bitsPerSample != 32)
			throw new IllegalArgumentException("bitsPerSample must be one of 8, 16, 24 or 32");
		if (channels <= 0 || channels > 8)
			throw new IllegalArgumentException("channels must be a positive integer (<= 8)");
		if (pcmData == null ) {
			throw new IllegalArgumentException("pcmData can not be null");
		} else if (pcmData.length < getPCMSize(1, channels, bitsPerSample)) {
			int len = channels * bitsPerSample>>3; 	// div by 8
			throw new IllegalArgumentException("pcmData must be contain at least 2 samples, each of size " + len);
		} else if (getPCMSize(pcmData.length, channels,bitsPerSample) % sampleSize != 0) {
			throw new IllegalArgumentException("Number of samples in pcmData is not a multiple of interleave " + sampleSize);  

		}

		if (sampleSize <= 0)
			throw new IllegalArgumentException("interleavedDataDimensions must be positive and non-zero"); 
			
		this.channels = channels;
		this.bitsPerSample = bitsPerSample;
		this.pcmData = pcmData;
		this.interleavedDataDimensions = sampleSize;

		
	}


	/**
	 * Get the sound samples as signed values.
	 * For 8-bit sampling rate, wav files contain unsigned values, but the values returned here will be signed. 
	 * Calls {@link #decodePCMData()} if necessary.
	 * @return never null.
	 */
	@JsonIgnore
	public byte[] getPCMData() {
		if (pcmData == null) 
			decodePCMData();
		return pcmData;
	}


	/**
	 * Turn the encoded PCM data that was encoded with {@link #encodePCMData()} into to the original byte[], that
	 * was the PCM data.  On success, 
	 * the instance is modified to contain a non-null byte[] for {@link #pcmData}and to clear the {@link #encodedPCM}.
	 */
	public void decodePCMData() {
		if (pcmData == null) {
			try {
				pcmData = PCMUtil.double2PCM(this.getData(), channels, bitsPerSample);
			} catch (Exception e) {
				throw new RuntimeException("Could not decompress sound data: " + e.getMessage(),e);
			}
		}
	}
	
	
	/**
	 * Get the number of bits/samples as would be seen in the PCM data (i.e. not a number of bits per logical sample).
	 * @return the bitsPerSample
	 */
	public int getBitsPerSample() {
		return bitsPerSample;
	}


	/**
	 * Get the number of channels as seen/represented in the PCM data. 
	 * @return the channels
	 */
	public int getChannels() {
		return channels;
	}

//	@JsonIgnore
//	@Override
//	public double[] getData() {
//		if (sampleAmplitude == null)
//			sampleAmplitude = PCMUtil.pcm2Double(this.getPCMData(), channels, bitsPerSample);
//		return sampleAmplitude;
//	}


	/**
	 * Write the clip in wav format to the given file.
	 * @param fileName
	 * @throws IOException 
	 */
	public void writeWav(String fileName) throws IOException {
		PCMUtil.PCMtoWAV(fileName,this);
	}

	/**
	 * Write the clip in wav format to the stream.
	 * The stream is not closed on return.
	 * @param stream
	 * @throws IOException
	 */
	public void writeWav(OutputStream stream) throws IOException {
		PCMUtil.PCMtoWAV(stream,this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + bitsPerSample;
		result = prime * result + channels;
		result = prime * result + interleavedDataDimensions;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof SoundClip))
			return false;
		SoundClip other = (SoundClip) obj;
		if (bitsPerSample != other.bitsPerSample)
			return false;
		if (channels != other.channels)
			return false;
		if (interleavedDataDimensions != other.interleavedDataDimensions)
			return false;
		return true;
	}


	@Override
	protected SoundClip newSubWindow(double newStartMsec, int startSampleIndex, int endSampleIndex) {
		/**   
		 * Because we override {@link #getIndex(double, boolean)}, the given indexes are raw indexes
		 * into the array returned by {@link #getData()}.  As such, we don't need to scale/adjust
		 * the indexes by the interleave.  All we do is make sure they are multiples of the interleave
		 * as a sanity check.
		 */
		int idc = this.getInterleavedDataDimensions();
		if (idc == 1) {
			DoubleWindow dw = super.newSubWindow(newStartMsec, startSampleIndex, endSampleIndex);
			double[] iv = dw.getIndependentValues();
			Vector vector = iv == null ? null : new Vector(iv);
			return new SoundClip(dw.getStartTimeMsec(), dw.getEndTimeMsec(), vector, dw.getData(), idc);
		}
//		if (idc != 1) {
//			if (startSampleIndex % idc != 0)
//				throw new RuntimeException("Sub-window start index(" + startSampleIndex + ") is not a multiple of " + idc);
//			if (endSampleIndex % idc != 0)
//				throw new RuntimeException("Sub-window end index(" + endSampleIndex + ") is not a multiple of " + idc);
//		}

		// TODO: now that we subclass from DoubleWindow, we should use the double[] data array instead of the pcmData to compute the new window.
		// Actual index into the PCM samples.
		int actualStartIndex = startSampleIndex * idc;
		int actualEndIndex = endSampleIndex * idc;
		// Now compute the actual byte index based on channels and bits/sample within the PCM data
		int channels = getChannels();
		int bps = getBitsPerSample();
		int bytesPerIndex = bytesPerPCMSample(channels, bps) ;
		int startByteIndex = actualStartIndex * bytesPerIndex; 
		int endByteIndex   = (actualEndIndex*bytesPerIndex); 
		byte[] newBytes; 
		if (actualStartIndex == actualEndIndex) {	// zero length return buffer;
			actualEndIndex = actualStartIndex + 1;	// Should never happen, but just in case.
		}
//		} else {
			byte[] pcmData = this.getPCMData();
			int pcmLength = pcmData.length;
			if (startByteIndex >  pcmLength)
				throw new RuntimeException("Starting at non-existent index in the data");
			if (endByteIndex > pcmLength) 		// endByteIndex is exclusive, so can equal the length of the array.
				endByteIndex = pcmLength;	// Must do this since copyOfRange() pads with zeros and we don't want that.
			newBytes = Arrays.copyOfRange(pcmData, startByteIndex, endByteIndex);
			if (newBytes.length % bytesPerIndex != 0)
				throw new RuntimeException("Internal error: sub-window length (" + newBytes.length 
						+") is not a multiple of " + bytesPerIndex);
//		}
		SoundClip clip = new SoundClip(newStartMsec, channels, bps, getSamplingRate(), newBytes, idc);
		return clip;
	}

	@Override
	public String toString() {
		return "SoundClip [bitsPerSample=" + bitsPerSample +  ", channels=" + channels
				+ ", interleavedDataDimensions=" + interleavedDataDimensions + "]";
	}
	

	private static class SoundClipIterator extends AbstractDefaultIterator<SoundClip> implements Iterator<SoundClip> {

		private Iterator<SoundRecording> recordings;

		public SoundClipIterator(Iterator<SoundRecording> recordings) {
			this.recordings = recordings;
		}

		@Override
		public boolean hasNext() {
			return recordings.hasNext();
		}

		@Override
		public SoundClip next() {
			return recordings.next().getDataWindow();
		}



	}

	/**
	 * Extends the super class to refine the inpput and output types. 
	 * @author DavidWood
	 *
	 */
	private static class SoundClipIterable extends LabeledWindowToDataWindowShuffleIterable<double[], SoundRecording, SoundClip> implements IShuffleIterable<SoundClip> {

		public SoundClipIterable(IShuffleIterable<SoundRecording> iterable) {
			super(iterable);
		}


	}
	
	private static boolean isSoundFile(String fileOrDir) {
		return fileOrDir.endsWith(".wav");
	}

	private static List<String> getSoundFilesFromDir(File dir) {
		List<String> soundFiles = new ArrayList<String>();
		if (!dir.isDirectory())
			return soundFiles;
		String[] files = dir.list();
		if (files == null)
			return soundFiles;
		String dirName = dir.getPath();
		for (String f: files) {
			if (isSoundFile(f))
				soundFiles.add(dirName + "/" + f);
		}

		return soundFiles;
	}
	
	public static List<String> getSoundFiles(Iterable<String> soundFilesOrDirs) {
		List<String> soundFiles = new ArrayList<String>();
		for (String fileOrDir : soundFilesOrDirs) {
			File f = new File(fileOrDir);
			if (f.isDirectory()) {
				soundFiles.addAll(getSoundFilesFromDir(f));
			} else if (isSoundFile(fileOrDir)) {
				soundFiles.add(fileOrDir);
			}
		}
		
		return soundFiles;
		
	}

	public static IShuffleIterable<SoundClip> readClips(List<String> soundFilesOrDirs) throws IOException {
//		IShuffleIterable<SoundRecording> recordings = new LabeledSoundFileIterable(getSoundFiles(soundFilesOrDirs));
		IShuffleIterable<SoundRecording> recordings = LabeledSoundFiles.loadUnlabeledSounds(soundFilesOrDirs);
		return new SoundClipIterable(recordings);
	}

	/** 
	 * Read all the sounds in the given directory.
	 * Formats supported are those supported by {@link LabeledSoundFiles#loadSounds(String, boolean)}.
	 * @param dir
	 * @return
	 * @throws IOException
	 */
	public static IShuffleIterable<SoundClip> readClips(String dir) throws IOException {
		IShuffleIterable<SoundRecording> recordings = LabeledSoundFiles.loadSounds(dir, false);
		return new SoundClipIterable(recordings);
	}

	@Override
	protected SoundClip uncachedPad(double durationMsec, org.eng.aisp.IDataWindow.PadType padType) {
		int idd = this.getInterleavedDataDimensions();
		double[] newData;
		if (idd == 1) {
			newData = DoubleWindow.pad(this.getData(), samplesPerSecond, durationMsec, padType);
		}  else {
			int newSamples = getSampleCount(durationMsec, samplesPerSecond*this.interleavedDataDimensions, false);
			int extras = newSamples % idd;
			if (extras != 0) {
				newSamples = newSamples - extras;
				if ((double)extras / idd > .5)
					newSamples += idd;
			}
			newData = DoubleWindow.pad(this.getData(), newSamples, padType);
		}
		byte[] newPCM = PCMUtil.double2PCM(newData, channels, bitsPerSample);
		return new SoundClip(startTimeMsec, startTimeMsec + durationMsec, null, channels, bitsPerSample, this.samplesPerSecond, newPCM, interleavedDataDimensions);
	}
	
	
	public byte[] getWAVBytes() {
		return PCMUtil.PCMtoWAV(this);
	}

	
	/**
	 * Get the number of dimensions in the data contained in this instance.
	 * When returning a value larger than 1, then the data is perfectly interleaved (xyzxyz...) and the amount of data is a multiple of
	 * the returned value.
	 * @return
	 */
	public int getInterleavedDataDimensions() {
		if (interleavedDataDimensions == 0)	// Test is only needed for instances deserialized from serializatiomns created prior to the addition of this field.
			interleavedDataDimensions = 1;
		return interleavedDataDimensions;
	}

	
	@Override
	public int getSampleSize() {
		int idd = this.getInterleavedDataDimensions();
		int sampleCount = getSampleCount(this.getDurationMsec(), getSamplingRate(), false); 
		if (idd != 1) {
			int extras = sampleCount % idd;
			if (extras != 0)  {
				sampleCount -= extras;
				if ((double)extras / idd > .5)
					sampleCount += idd;
			}
			sampleCount += idd;	// The end point is needed to fully cover the duration.
		}
		return sampleCount;
	}
	
	private int getClosestInterleave(int n) {
		int idc = this.getInterleavedDataDimensions();
		if (idc == 1)
			return n;
		
		int extras = n % idc; 
		n -= extras;
		if ((double)extras / idc > .5)
			n += idc; 
		return n;
	}


}
