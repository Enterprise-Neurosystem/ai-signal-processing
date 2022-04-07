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
package org.eng.aisp.monitor;

import java.util.Arrays;
import java.util.concurrent.Callable;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundClip;
import org.eng.aisp.util.PCMUtil;
import org.eng.util.RepeatingCallable;

/**
 * Use the Java Sound API to repeatedly capture audio clips and deliver them to a user-defined handler.
 * Extends the super class to define the Callable to be the mechanism by which audio is captured.
 * Per the super class, audio capture is started after the {@link #start()} method is called.
 * @author dawood
 *
 */
public class SoundCapture extends AbstractAsyncDataProvider<SoundClip> implements IAsyncDataProvider<SoundClip> {

	/** Sample rates we are willing to accept, in order of preference. */
	private final static int sampleFrequency[] = { 44100, 22050, 16000, 11025, 8000 };
	/** Bits/sample we are willing to accept, in order of preference. */
	private final static int bitsPerSamples[] = { 16, 8 };
	/** Number of channels we are willing to accept, in order of preference. */
	private final static int channels[] = { 2, 1 };

	protected TargetDataLine line;
	protected final int clipLenMsec;
	protected final int pauseMsec;
//	protected IDataHandler<SoundClip> dataHandler;
	private byte buffer[];
	private AudioFormat format;
	RepeatingCallable repeater;
	
//	/**
//	 * Interface implementation that handles the clips of audio captured by the SoundCapture instance.
//	 * @author dawood
//	 *
//	 */
//	public interface AudioHandler {
//		public void audioCaptured(byte pcm[], AudioFormat format) throws Exception;
//	}
	
	/**
	 * Create the instance to capture clips of the given length separated by the given pause and deliver
	 * them to the given handler.
	 * @param clipLenMsec length of desired clips.
	 * @param pauseMsec time between clips.
	 */
	public SoundCapture(int clipLenMsec, int pauseMsec) {
		this.clipLenMsec = clipLenMsec;
		this.pauseMsec = pauseMsec;
	}

	
	/**
	 * The callable that captures the clips of sound as defined by {@link SoundCapture#clipLenMsec} and {@link SoundCapture#pauseMsec}.
	 * @author dawood
	 */
	private static  class SoundCaptureWorker implements Callable<Integer> {

		protected SoundCapture monitor;
		
		public SoundCaptureWorker(SoundCapture soundCapture) {
			this.monitor = soundCapture;
		}

		@Override
		public Integer call() throws Exception {
			// Begin audio capture.
			monitor.line.start();

			int toRead = monitor.buffer.length;
			int totalRead = 0;
			while (monitor.repeater.isStarted() && toRead > 0) {
			   // Read the next chunk of data from the TargetDataLine.
			   int numBytesRead =  monitor.line.read(monitor.buffer, monitor.buffer.length-toRead, toRead);
			   toRead -= numBytesRead;
			   totalRead += numBytesRead;
			}  

			monitor.line.stop();
			byte[] pcm = Arrays.copyOf(monitor.buffer, totalRead);
			SoundClip clip = new SoundClip(monitor.format.getChannels(), monitor.format.getSampleSizeInBits(), (int)monitor.format.getSampleRate(), pcm);
			
//			monitor.dataHandler.newDataProvided(clip);
			monitor.notifyListeners(clip, null);
			return monitor.pauseMsec;
		}
	}


	/**
	 * Override to setup the audio capture.
	 * @throws AISPException 
	 */
	@Override
	public void start() throws Exception {
		if (repeater != null) {
			if (repeater.isStarted()) {
				throw new AISPException("Already started.");
			}
			repeater = null;
		}
		SoundCaptureWorker worker = new SoundCaptureWorker(this);
		repeater = new RepeatingCallable("Monitor", worker);
		connectAudio();
		repeater.start();
	}


	/**
	 * Search {@link #bitsPerSamples}, {@link #channels} and {@link #bitsPerSamples} for an acceptable audio format.
	 * @param lineClass class of the line object that is available to support the given format.
	 * @return null if none found.
	 */
	private AudioFormat findSupportedFormat(Class<?> lineClass) {
		 
		for (int i=0 ; i<sampleFrequency.length ; i++) {
			int frequency = sampleFrequency[i];
			for (int j=0 ; j<bitsPerSamples.length ; j++) {
				int bits = bitsPerSamples[j];
				for (int k=0 ; k<channels.length ; k++) {
					AudioFormat format = new AudioFormat(frequency, bits, channels[k], true, false);	// unsigned, little-endian
					DataLine.Info info = new DataLine.Info(lineClass, format); 
					if (AudioSystem.isLineSupported(info)) 
						return format;
				}
			}
		}
		return null;
	}

	
	/**
	 * Find and open a line that supports one of the formats we are willing to accept.
	 * @throws AISPException
	 */
	private void connectAudio() throws AISPException {
		if (line != null)
			return;
		
		format = findSupportedFormat(TargetDataLine.class);
		if (format == null)
			throw new AISPException("Could not find supported audio input");
		int bufferLen = (int)((format.getChannels() * format.getSampleRate() * format.getSampleSizeInBits() / 8) * this.clipLenMsec / 1000);
		this.buffer = new byte[bufferLen];

		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format); 
		try {
		    line = (TargetDataLine) AudioSystem.getLine(info);
		    line.open(format);
		    System.out.println("Capturing audio using format " + format);
		} catch (LineUnavailableException e) {
			buffer = null;
			line = null;
			format = null;
			throw new AISPException("Line is not available: " + e.getMessage(), e);
		}
		
	}


	/**
	 * Override to disconnect the audio.
	 * @throws Exception
	 */
	@Override
	public void stop() {
		this.repeater.stop();
		disconnectAudio();
	}


	/**
	 * Close the connected line and free up resources.
	 */
	private void disconnectAudio() {
		if (line != null)
			line.close();
		line = null;
		buffer = null;
		format = null;
	}
	
	public static void main(String[] args) throws Exception {
		QueuedDataHandler<SoundClip> queuedHandler = new QueuedDataHandler<SoundClip>(2);
		SoundCapture monitor = new SoundCapture(2000, 0);
		monitor.addListener(queuedHandler);
		monitor.start();
		Thread.sleep(5000);
		SoundClip clip;
		int count = 0;
		while ((clip = queuedHandler.next(3000)) != null) {
			System.out.println("Got a clip:" + clip);
			System.out.println("duration: " + clip.getDurationMsec());
			String fileName = "soundcapture" + count + ".wav";
			PCMUtil.PCMtoWAV(fileName, clip);
			if (count++ > 10)
				monitor.stop();	// Then blockingNext() should return null.
		}
	}
	
	
}
