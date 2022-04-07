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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLogger;
import org.eng.aisp.AISPProperties;
import org.eng.aisp.SoundClip;

/**
 * Provides utilities for working with raw sounds samples (PCM) in MP3 and WAV formats.
 * WAV files supported are signed 8-, 16-, 24- and 32-bit signed little-endian values.
 * @see <a href="http://soundfile.sapp.org/doc/WaveFormat/">http://soundfile.sapp.org/doc/WaveFormat/</a>
 * @author dawood
 *
 */
public class PCMUtil {



	/**
	 * Get the next PCM data value as an integer out of the ByteBuffer representing the original byte[] of pcm data.
	 * The range of possible return values regardless of bitsPerSample is always centered around 0.
	 * So for byte data the value will be in the range -127..128, etc.
	 * @param byteBuffer
	 * @param channels 1,2,3... no check is made for correctness.  Caller should validate.
	 * @param bitsPerSample 8, 16,24. no check is made for correctness. Caller should validate. 
	 * @return
	 */
	static int getNextPCMValue(ByteBuffer byteBuffer, int channels, int bitsPerSample) {
		long pcmValue = 0;  //Needs long here because two channels may overflow int with 2 channels
		for (int c=0 ; c<channels ; c++) {
			switch (bitsPerSample) {
				case 8: pcmValue += (int)byteBuffer.get();	break;	
				case 16: pcmValue += byteBuffer.getShort();	break;
				case 24: 
					ByteBuffer byteBufferTmp = ByteBuffer.allocate(4);
					byteBufferTmp.order(ByteOrder.LITTLE_ENDIAN);
					byteBufferTmp.put((byte) 0x00);
					byteBufferTmp.put(byteBuffer.get());
					byteBufferTmp.put(byteBuffer.get());
					byteBufferTmp.put(byteBuffer.get());
					byteBufferTmp.flip();
					pcmValue += byteBufferTmp.getInt()/256;
					break;
				case 32: pcmValue += byteBuffer.getInt();	break;
				default: throw new IllegalArgumentException(bitsPerSample + " bits/sample is not supported (yet)."); 
			}
		}
		if (channels != 1)
			pcmValue = (long)((double)(pcmValue) / channels); 
		return (int)pcmValue;
	}	

	/**
	 * Convert an array of PCM samples to an array of double values scaled to -1..1. 
	 * @param samples signed pcm values (yes, even if 8 bit samples).
	 * @param channels the number of channels present in the samples.  
	 * @param bitsPerSample 8, 16,24, or 32. 
	 * @return An array of length samples.length / (bitsPerSample/8) / channels. 
	 * If more than one channel then the  result is the average of the separate channel signals.
	 */
	public static double[] pcm2Double(byte[] samples, int channels, int bitsPerSample) {
		if (bitsPerSample != 8 && bitsPerSample != 16  && bitsPerSample != 24  && bitsPerSample != 32) 
			throw new IllegalArgumentException("bits per sample must be 8, 16,24, or 32");
		if (channels <= 0) 
			throw new IllegalArgumentException("channels must be larger than 0");
		int sampleCount = samples.length * 8 / (bitsPerSample *  channels);
		double result[] = new double[sampleCount];
		ByteBuffer byteBuffer = ByteBuffer.wrap(samples);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
	
		for (int i=0 ; i<sampleCount ; i++) {
			int pcmValue = getNextPCMValue(byteBuffer, channels, bitsPerSample);
			double t = pcm2Double(pcmValue, bitsPerSample);
			result[i] = t; 
		}

		return result;
	}




	/**
	 * A convenience method over {@link #PCMtoWAV(OutputStream, byte[], int, int, int)}.
	 * @param os
	 * @param clip
	 * @throws IOException
	 */
	public static void PCMtoWAV(OutputStream os, SoundClip clip) throws IOException {
		PCMtoWAV(os, clip.getPCMData(), (int)clip.getSamplingRate(), clip.getChannels(), clip.getBitsPerSample(), clip.getInterleavedDataDimensions());
	}
	
	/**
	 * A convenience method over {@link #PCMtoWAV(OutputStream, byte[], int, int, int)}.
	 * @param fileName name of file to put the clip into
	 * @param clip
	 * @throws IOException
	 */
	public static void PCMtoWAV(String fileName, SoundClip clip) throws IOException {
		FileOutputStream fos = new FileOutputStream(fileName);
		PCMtoWAV(fos, clip);
		fos.close();
	}
	
	public static byte[] PCMtoWAV(SoundClip clip) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			PCMtoWAV(bos,clip);
			bos.close();
		} catch (IOException e) {	// This will never happen
			throw new RuntimeException("Got IOException on " + bos.getClass().getSimpleName() + ": " + e.getMessage(), e);
		}
		return bos.toByteArray();
	}

	/**
	 * Write the pcm file to the given file.
	 * @param fileName if .wav extension is not included it will be added.
	 * @param data raw unsigned PCM data in little-endian order.
	 * @param srate
	 * @param channels
	 * @param bitsPerSample
	 * @throws IOException
	 */
	public static void PCMtoWAV(String fileName, byte[] data, int srate, int channels, int bitsPerSample) throws IOException {
		if (!fileName.endsWith(".wav"))
			fileName += ".wav";
		FileOutputStream fos = new FileOutputStream(fileName);
		PCMtoWAV(fos,data, srate, channels, bitsPerSample);
		fos.close();
	}

	/**
	 * Convert an array of signed bytes to unsigned bytes by adding 128;
	 * @param data
	 * @return
	 */
	private static byte[] signed2unsigned(byte[] data) {
		byte[] newData = new byte[data.length];
		for (int i=0 ; i<data.length ; i++) {
			int t = ((int)data[i]) - BYTE_RANGE_MIN ; // BYTE_RANGE_MIN is negative 
			newData[i] = (byte)(t&0xff);
		}
		return newData;
	}

	/**
	 * Convert an array of unsigned byte values to signed bytes by subtracting 128;
	 * @param data
	 * @return
	 */
	private static byte[] unsigned2signed(byte[] data) {
		byte[] newData = new byte[data.length];
		for (int i=0 ; i<data.length ; i++) {
			int t = (int)data[i] & 0xff;
			t = t + BYTE_RANGE_MIN ; 	// BYTE_RANGE_MIN is negative
			newData[i] = (byte)(t&0xff);
		}
		return newData;
	}

	/**
	 * A convenience on {@link #PCMtoWAV(OutputStream, byte[], int, int, int, int)} that sets the number of dimensions to 1.
	 * @deprecated in favor of {@link #PCMtoWAV(OutputStream, byte[], int, int, int, int)} to set the dimensions.
	 */
	public static void PCMtoWAV(OutputStream os, byte[] pcmData, int srate, int channels, int bitsPerSample) throws IOException {
		PCMtoWAV(os,pcmData,srate,channels,bitsPerSample, 1);
	}

	/**
	 * Write the PCM data to a stream in WAV format.  Primarily this means writing the
	 * wav header followed by the actual PCM data.
	 * The stream is NOT closed on return.
	 * @param os stream to which the wav bytes are written. Not closed before returning. 
	 * @param pcmData raw PCM data (linear encoding).
	 * @param srate sampling rate of PCM data.
	 * @param channels number of channels, generally 1 or 2.
	 * @param bitsPerSample bits per sample, generally 8 or 16.
	 * @param nDims the number of dimensions of interleaved data in the give pcm data.  Placed into a LIST/DIMS chunk for reading back 
	 * by PCMtoWAV().
	 * @throws IOException
	 * @see <a href="http://soundfile.sapp.org/doc/WaveFormat/">http://soundfile.sapp.org/doc/WaveFormat/</a>
	 */
	public static void PCMtoWAV(OutputStream os, byte[] pcmData, int srate, int channels, int bitsPerSample, int nDims) throws IOException {
	    final int headerLen = MIN_WAV_HEADER_SIZE + (nDims == 1 ? 0 : LIST_SUBCHUNK_SIZE);
	    byte[] header = new byte[headerLen];
	
	    long bitrate = srate * channels * bitsPerSample;
	    int offset = 0;
	    
	    /** RIFF chunk, size=12 */
	    header[offset++] = 'R'; 
	    header[offset++] = 'I';
	    header[offset++] = 'F';
	    header[offset++] = 'F';
	    long totalRemainingLen = pcmData.length + header.length - 8;	// Length after this field in th header (i.e. eight bytes);
	    header[offset++] = (byte) (totalRemainingLen & 0xff);
	    header[offset++] = (byte) ((totalRemainingLen >> 8) & 0xff);
	    header[offset++] = (byte) ((totalRemainingLen >> 16) & 0xff);
	    header[offset++] = (byte) ((totalRemainingLen >> 24) & 0xff);
	    header[offset++] = 'W';
	    header[offset++] = 'A';
	    header[offset++] = 'V';
	    header[offset++] = 'E';	

	    /** custom INFO/DIMS sub-chunk, size=16 */
	    if (nDims > 1) {
			final int dimDataSize=8;	// DIMS + ndims as 4-byte little endian integer 
			header[offset++] = 'L'; 
			header[offset++] = 'I';
			header[offset++] = 'S';
			header[offset++] = 'T';
			header[offset++] = (byte)  (dimDataSize        & 0xff);
			header[offset++] = (byte) ((dimDataSize >> 8)  & 0xff);
			header[offset++] = (byte) ((dimDataSize >> 16) & 0xff);
			header[offset++] = (byte) ((dimDataSize >> 24) & 0xff);
			header[offset++] = 'D'; 
			header[offset++] = 'I';
			header[offset++] = 'M';
			header[offset++] = 'S';
			header[offset++] = (byte)nDims;
			header[offset++] = 0;	
			header[offset++] = 0;	
			header[offset++] = 0;	
	    }

	    /** fmt sub-chunk, size=24 */
	    header[offset++] = 'f'; 
	    header[offset++] = 'm';
	    header[offset++] = 't';
	    header[offset++] = ' ';
	    header[offset++] = (byte) 16; 
	    header[offset++] = 0;
	    header[offset++] = 0;
	    header[offset++] = 0;
	    header[offset++] = 1; 
	    header[offset++] = 0;
	    header[offset++] = (byte) channels; 
	    header[offset++] = 0;
	    header[offset++] = (byte) (srate & 0xff);
	    header[offset++] = (byte) ((srate >> 8) & 0xff);
	    header[offset++] = (byte) ((srate >> 16) & 0xff);
	    header[offset++] = (byte) ((srate >> 24) & 0xff);
	    int byteRate = (int) (bitrate / 8);
	    header[offset++] = (byte) ( byteRate & 0xff);
	    header[offset++] = (byte) ((byteRate >> 8) & 0xff);
	    header[offset++] = (byte) ((byteRate >> 16) & 0xff);
	    header[offset++] = (byte) ((byteRate >> 24) & 0xff);
	    header[offset++] = (byte) ((channels * bitsPerSample) / 8); 
	    header[offset++] = 0;
	    header[offset++] = (byte)bitsPerSample; 
	    header[offset++] = 0;

	    /** data sub-chunk, size=8*/
	    header[offset++] = 'd';
	    header[offset++] = 'a';
	    header[offset++] = 't';
	    header[offset++] = 'a';
	    header[offset++] = (byte) (pcmData.length  & 0xff);
	    header[offset++] = (byte) ((pcmData.length >> 8) & 0xff);
	    header[offset++] = (byte) ((pcmData.length >> 16) & 0xff);
	    header[offset++] = (byte) ((pcmData.length >> 24) & 0xff);
	
	    os.write(header, 0, offset); 
		if (bitsPerSample == 8)
			pcmData = signed2unsigned(pcmData);
	    os.write(pcmData);
	    os.flush();
	}
	
	
	/**
	 * Read a the named wav file and set the start time to 0. 
	 * @param file 
	 * @return never null
	 * @throws IOException  if the byte array does not represent PCM wav.
	 * @deprecated in favor of {@link #ReadPCM(URL, String)}.
	 */
	public static SoundClip WAVtoPCM(String file) throws IOException {
		return ReadPCM(0,file, WAV_FORMAT);
	}

	/**
	 * 
	 * @param startMsec
	 * @param file
	 * @return
	 * @throws IOException
	 * @deprecated in favor of {@link #ReadPCM(long, URL, String)}
	 */
	public static SoundClip WAVtoPCM(long startMsec, String file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		SoundClip clip = WAVtoPCM(startMsec, fis);
		fis.close();
		return clip;
		
	}

	public final static String WAV_FORMAT = "wav";
	public final static String MP3_FORMAT = "mp3";
	final static String SUPPORTED_FORMATS[] = { WAV_FORMAT, MP3_FORMAT };
	final static String SUPPORTED_FORMAT_EXTENSIONS[] = { "." + WAV_FORMAT, "." + MP3_FORMAT };

	/**
	 * Determine if {@link #ReadPCM(long, URL, String)} can handle the file who's format is identified by its extension.
	 * It is supported if the extension is in {@value #SUPPORTED_FORMAT_EXTENSIONS}.
	 * @param fileName
	 * @return
	 */
	public static boolean isFileFormatSupported(String fileName) {
		for (String ext : SUPPORTED_FORMAT_EXTENSIONS) {
			if (fileName.endsWith(ext))
				return true;
		}
		return false;
	}

		
	/**
	 * Read the SoundClip from the file or url using {@link #ReadPCM(long,String,String)}.
	 * The format is determined from the fileOrURL. 
	 * @param fileOrURL see {@link #ReadPCM(long,String,String)} 
	 * @return never null.
	 * @throws IOException
	 * @throws IllegalArgumentException if format is not recognized or could not be determined.
	 */
	public static SoundClip ReadPCM(String fileOrURL) throws IOException {
		return ReadPCM(0, fileOrURL,null);
	}
	
	/**
	 * Read the SoundClip from the network or file using {@link #ReadPCM(long,URL, String)}.
	 * @param startTimeMsec the start time of the returned clip.
	 * @param fileOrURL string containing either a filename (absolute or relative) or a url.
	 * @param format see {@link #ReadPCM(long, URL, String)}.
	 * @return never null.
	 * @throws IOException
	 * @throws IllegalArgumentException if format is not recognized or could not be determined.
	 */
	public static SoundClip ReadPCM(double startTimeMsec, String fileOrURL, String format) throws IOException {
		URL url;
		try {
			url = new URL(fileOrURL);
		} catch (MalformedURLException e) { 
			// Assume it is a file.
			File f = new File(fileOrURL);
			fileOrURL = f.getAbsolutePath();
			url = new URL("file:///" + fileOrURL);
		}
		return ReadPCM(startTimeMsec, url, format);
	}

	/**
	 * Read the SoundClip from the url using {@link #ReadPCM(URL, String)}.
	 * The format is determined from the url. 
	 * @param url
	 * @return never null.
	 * @throws IOException
	 * @throws IllegalArgumentException if format is not recognized or could not be determined.
	 */
	public static SoundClip ReadPCM(URL url) throws IOException {
		return ReadPCM(0, url,null);
	}

	/**
	 * Read a SoundClip from the given URL.
	 * Supported formats are WAV and MP3.
	 * @param startTimeMsec the start time of the returned clip.
	 * @param url  file or network url.
	 * @param format one of {@value #MP3_FORMAT} or {@value #WAV_FORMAT} or null.  If null,
	 * then try and determine the format from the url. 
	 * @return never null.
	 * @throws IOException
	 * @throws IllegalArgumentException if format is not recognized or could not be determined.
	 */
	public static SoundClip ReadPCM(double startTimeMsec, URL url, String format) throws IOException {
		if (format == null) { 
			String urlStr = url.toString();
			if (urlStr.endsWith(".mp3"))
				format = MP3_FORMAT;
			else if (urlStr.endsWith(".wav"))
				format = WAV_FORMAT;
			else
				throw new IllegalArgumentException("Could not determine format from URL: " + url);
		}
		SoundClip clip;
		if (format.equals(WAV_FORMAT)) {
			InputStream is = url.openStream();
			clip = WAVtoPCM(startTimeMsec, is); 
			is.close();
		}
		else if (format.equals(MP3_FORMAT))
			clip = MP3toPCM(startTimeMsec, url);
		else
			throw new IllegalArgumentException("Unsupported format " + format);
		return clip;	
	}


	/**
	 * Read a byte array from the stream (until EOF) which represent a PCM encoded wav file into a SoundClip with the start time
	 * set to  0.
	 * Stream is not closed.
	 * @param is
	 * @return never null
	 * @throws IOException  if the byte array does not represent PCM wav.
	 */
	public static SoundClip WAVtoPCM(InputStream is) throws IOException {
		return WAVtoPCM(0, is);
	}
	
	/**
	 * Read all bytes from the given stream until eof and parse a WAV file from them.
	 * Stream is not closed.
	 * @param startMsec
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public static SoundClip WAVtoPCM(double startMsec, InputStream is) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final int size = 1024*1024;
		byte[] buffer = new byte[size];
		int count;
		while ((count = is.read(buffer)) != -1) 
			bos.write(buffer,0,count);
		
		return WAVtoPCM(startMsec, bos.toByteArray());
	}
	
	/**
	 * Create the clip using the given byte array and a start time of 0. 
	 * @param wave
	 * @return
	 * @throws IOException
	 */
	public static SoundClip WAVtoPCM(byte[] wave) throws IOException {
		return WAVtoPCM(0, wave);
	}

	/** Taken from http://soundfile.sapp.org/doc/WaveFormat/  
	 *  Offset  | field Name     	| field size
	 *  0       | chunkID        	| 4		ID="RIFF" for little endian, "RIFX" for big endian
	 *  4       | chunk size 		| 4		Total size of the byte array including this header.
	 *  8       | format			| 4		format=WAVE" 
	 *  Begin any subchunk, search until finding 'fmt ' sub chunk
	 *  X 		| chunkid = 'fmt ' 	| 4		X=12 if no intermediate subchunks.
	 *  X+4		| subchunk size 	| 4		number of bytes in this subchunk after this field. 16 for uncompressed PCM 
	 *  X+8 	| audio format	 	| 2		format=1 for uncompressed PCM
	 *  X+10 	| num channels	 	| 2
	 *  X+12 	| sample rate	 	| 4
	 *  X+16 	| byte rate		 	| 4
	 *  X+20 	| block align		| 2
	 *  X+22 	| bits per sample 	| 2
	 *  Begin any subchunk, search until finding 'data' sub chunk
	 *  Y 		| chunkid = 'data' 	| 4		Y=36 if only fmt subchunk prior to this one
	 *  Y+4 	| subchunk size 	| 4		number of bytes in this subchunk after this field, =samples*channels* bitsPerSample/8.
	 *  Y+8 	| pcm data			| subchunk size 
	 *  Ignore any subchunks after this
	 *  http://www-mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/WAVE.html is also useful for extension information. */
	private final static int MIN_WAV_HEADER_SIZE = 44; // bytes, size with only fmt and data subchuncks.
	private final static int HEADER_RIFF_CHUNK_OFFSET= 0; // bytes;
	private final static int HEADER_CHUNK_SIZE_OFFSET= 4; // bytes;
	private final static int HEADER_FORMAT_OFFSET= 8; // bytes;
	private final static int HEADER_SIZE = 12; // bytes;

	private final static int SUBCHUNK_FORMAT_OFFSET= 0;	// Offset within the subchunk, always the same for all subchunks 
	private final static int SUBCHUNK_DATA_SIZE_OFFSET= 4;	// Offset within the subchunk, always the same for all subchunks 
	private final static int SUBCHUNK_DATA_OFFSET= 8;	// Offset within the subchunk, always the same for all subchunks 

	// "DIMS" subchunk data and Offsets within the "DIMS" subchunk
	private final static int LIST_TYPEID_OFFSET = 8; 
	private final static int DIMS_DIMENSION_OFFSET = 12; 
	private final static int LIST_SUBCHUNK_SIZE = 16;	// See PCMtoWAV().

	// "fmt " subchunk data and Offsets within the "fmt" subchunk
	private final static int FMT_AUDIO_FORMAT_OFFSET= 8; 
	private final static int FMT_CHANNELS_OFFSET= 10; 
	private final static int FMT_SAMPLE_RATE_OFFSET= 12; 
	private final static int FMT_BYTE_RATE_OFFSET= 16; 
	private final static int FMT_BLOCK_ALIGN_OFFSET= 18; 
	private final static int FMT_BITS_PER_SAMPLE_OFFSET= 22; 
	private final static int FMT_PCM_DATA_FORMAT= 1; 			// byte indicating format of data in "fmt " subchunk
	private final static int FMT_SUBCHUNK1_SIZE_FOR_PCM = 16; 	// The expected size field value in the "fmt " subchunk when it describes PCM data
	private final static int FMT_CHUNK_SIZE = FMT_SUBCHUNK1_SIZE_FOR_PCM + 8; 	// The expected total size of the "fmt " subchunk 
	
	/**
	 * Convert the given byte array which represent a PCM encoded wav file into a SoundClip.
	 * @param wav byte array from which to read wav formatted PCM.  Not used in the returned value.
	 * @return never null
	 * @throws IOException  if the byte array does not represent PCM wav.
	 */
	public static SoundClip WAVtoPCM(double startTimeMsec, byte[] wave) throws IOException {
		return WAVtoSoundClip(startTimeMsec, wave, false);
	}

	private static SoundClip WAVtoSoundClip(double startTimeMsec, byte[] wave, boolean reformatted) throws IOException {
		if (wave.length < MIN_WAV_HEADER_SIZE)
			throw new IOException("array is too small to contain wav-formatted PCM data");
//		String wavStr = new String(wave);
//		System.out.println("wavString=" + wavStr);
		// RIFF header
		checkArray(wave,HEADER_RIFF_CHUNK_OFFSET,"RIFF");
		int chunkSize = getLittleEndianInteger(wave,HEADER_CHUNK_SIZE_OFFSET);		// 4 bytes later is the total size of the byte array.
		int totalSize = chunkSize + 8;	// Chunk size plus first 2 words.
		if (totalSize != wave.length) {
			String msg = "Total size in WAV header+8 (" + (totalSize) + ") is inconsistent with the size of the given array (" + wave.length +")";
			if (totalSize < wave.length) 
				AISPLogger.logger.warning(msg);
			else // Only throw when size indicates bytes are missing.
				throw new IOException(msg);
		}
		checkArray(wave,HEADER_FORMAT_OFFSET,"WAVE");
		
		// Custom LIST sub-chunk 
		int dimensions = getDimensionality(wave); 

		// fmt header
		int fmtStart = findSubChunk(wave,HEADER_SIZE,"fmt ", "data");	// skip other chunks
		if (fmtStart == 0)  {
				throw new IOException("Did not find 'fmt ' sub-chunk");
		}
//		checkArray(wave,fmtStart,"fmt ");
		int fmt_subchunk_size = wave[fmtStart + SUBCHUNK_DATA_SIZE_OFFSET];
		if (fmt_subchunk_size != FMT_SUBCHUNK1_SIZE_FOR_PCM) {
			if (dimensions != 1)	// This is our file, so a bad size means something else is wronge.
				throw new IOException("fmt subchunk size of multidimensional wave does not indicate that the data is PCM.");
			else if (reformatted)	// We already tried reformatting/converting. 
				throw new IOException("fmt subchunk size does not indicate that the data is PCM after reformatting.");
			try {
				wave = convertToReadablePCM(wave);
			} catch (UnsupportedAudioFileException e) {
				throw new IOException("Unsupportted audio format when reparsing due to unrecognized fmt subchunk size " + fmt_subchunk_size + " : " + e.getMessage());
			}
			return WAVtoSoundClip(startTimeMsec, wave, true);
		}
		// Parse the 
		return WAVtoSoundClip(wave, fmtStart, dimensions, startTimeMsec);
	}


	/**
	 * @param wave the whole wav byte array
	 * @param fmtStart the offset into the byte array where the FMT chunk starts
	 * @param dimensions the dimensionality of the data in the wav 
	 * @param startTimeMsec the start time of the SoundClip to create.
	 * @return never null.
	 * @throws IOException
	 */
	private static SoundClip WAVtoSoundClip(byte[] wave, int fmtStart, int dimensions, double startTimeMsec)
			throws IOException {
		if (wave[fmtStart + FMT_AUDIO_FORMAT_OFFSET] != FMT_PCM_DATA_FORMAT) 
			throw new IOException("data is not linear (uncompressed) PCM data") ;
		int channels = wave[fmtStart+ FMT_CHANNELS_OFFSET];
		int samplingRate =  getLittleEndianInteger(wave,fmtStart+ FMT_SAMPLE_RATE_OFFSET);
		int byteRate = getLittleEndianInteger(wave,fmtStart+ FMT_BYTE_RATE_OFFSET);
		int bitsPerSample = wave[fmtStart+ FMT_BITS_PER_SAMPLE_OFFSET]; 
		int indicatedSamplingRate = (int)(8 * byteRate / channels / bitsPerSample);
		if (indicatedSamplingRate != samplingRate)
			throw new IOException("Sampling rate in the header (" + samplingRate + ") is not consistent with byte rate (" 
					+ byteRate + "), bitsPerSample (" + bitsPerSample + ") and channel (" + channels + ") values from the header.");

		// data header
		int dataStart = findSubChunk(wave,fmtStart+FMT_CHUNK_SIZE,"data", null);	// skip other chunks.
		if (dataStart == 0) 
			throw new IOException("Did not find 'data' sub-chunk");
		int pcmLength = getLittleEndianInteger(wave,dataStart+SUBCHUNK_DATA_SIZE_OFFSET);	// 1 word later is the size of the PCM data.
		int pcmStart = dataStart + SUBCHUNK_DATA_OFFSET;		
		
		// We allow extra data after the PCM data since it appears some utilities add extra subchunks.
		// See http://www.neurophys.wisc.edu/auditory/riff-format.txt.
		if (wave.length < pcmStart + pcmLength)
			throw new IOException("Size of PCM data specified in the 'data' subchunk (" + pcmLength 
						+ ") is larger than the given byte array (" + wave.length + ").");
		byte pcmData[] = Arrays.copyOfRange(wave, pcmStart, pcmStart + pcmLength) ;
		if (bitsPerSample == 8)
			pcmData = unsigned2signed(pcmData);

//		double endTimeMsec = startTimeMsec + SoundClip.getPCMMsec(channels, bitsPerSample, samplingRate, pcmData, dimensions) ;
		return new SoundClip(startTimeMsec, channels, bitsPerSample, samplingRate, pcmData,dimensions );	
//		return new SoundClip(startTimeMsec, endTimeMsec, channels, samplingRate, bitsPerSample, pcmData,dimensions );	
	}


	/**
	 * @param wave
	 * @return
	 */
	private static int getDimensionality(byte[] wave) {
		int dimensions = 0;
		int searchStart = HEADER_SIZE;
		while (dimensions == 0) {	// Go through multiple LIST chunks if necessary
			int start = findSubChunk(wave,searchStart,"LIST", "data");	// skip other chunks, but don't go beyond the "data" chunk.
			if (start == 0) {	// No more LIST chunks
				dimensions = 1;
			} else {			// See if this is  LIST chunk with our DIMS data.
				if (wave[start+LIST_TYPEID_OFFSET+0] == 'D' && 
					wave[start+LIST_TYPEID_OFFSET+1] == 'I' && 
				    wave[start+LIST_TYPEID_OFFSET+2] == 'M' && 
				    wave[start+LIST_TYPEID_OFFSET+3] == 'S') {
					dimensions = wave[start + DIMS_DIMENSION_OFFSET];
					if (dimensions <= 0)
						dimensions = 1;	// error?
				} else {
					int chunkDataSize = getLittleEndianInteger(wave,start + SUBCHUNK_DATA_SIZE_OFFSET);
					searchStart = start + chunkDataSize + SUBCHUNK_DATA_OFFSET;
				}
			}
		}
		return dimensions;
	}
	
	private static byte[] convertToReadablePCM(byte wav[]) throws UnsupportedAudioFileException, IOException {
		InputStream inStream = new ByteArrayInputStream(wav);

			AudioInputStream sourceAudioInputStream;
//			try {
				inStream = new BufferedInputStream(inStream);	
				sourceAudioInputStream = AudioSystem.getAudioInputStream(inStream);
//			} finally { 
//				try {
//					inStream.close();
//				} catch (IOException e) {
//					;
//				}
//				return null;
//			}
			
			AudioFormat srcFormat = sourceAudioInputStream.getFormat();

			boolean bigEndian = false;
			AudioInputStream targetAudioInputStream=AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, sourceAudioInputStream);
			AudioFormat targetFormat = new AudioFormat(new AudioFormat.Encoding("PCM_SIGNED"), 
					srcFormat.getSampleRate(), srcFormat.getSampleSizeInBits(), srcFormat.getChannels(), 
					srcFormat.getFrameSize(), srcFormat.getFrameRate(), bigEndian);
			AudioInputStream targetAudioInputStream1 = AudioSystem.getAudioInputStream(targetFormat, targetAudioInputStream);

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				AudioSystem.write(targetAudioInputStream1, AudioFileFormat.Type.WAVE, bos); 
				return bos.toByteArray();
			} finally {
				try {
					inStream.close();
					sourceAudioInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
	}

	/**
	 * 
	 * @param wave
	 * @param chunkOffset
	 * @param chunkID
	 * @param stopChunkID
	 * @param exhaustive
	 * @return 0 if not found, otherwise the offset of the found chunkID after chunkOffset.
	 */
	private static int findSubChunk(byte[] wave, int chunkOffset, String chunkID, String stopChunkID) {
		if (chunkOffset >= wave.length)
			return 0;
		if (stopChunkID != null && isMatch(wave, chunkOffset+SUBCHUNK_FORMAT_OFFSET, stopChunkID))
			return 0;
		if (isMatch(wave, chunkOffset+SUBCHUNK_FORMAT_OFFSET, chunkID))
			return chunkOffset;
		int chunkDataSize = getLittleEndianInteger(wave,chunkOffset+ SUBCHUNK_DATA_SIZE_OFFSET);
		int thisChunkSize = SUBCHUNK_DATA_OFFSET + chunkDataSize;
		int nextChunkOffset = chunkOffset + thisChunkSize;	
		if (nextChunkOffset >= wave.length)  {
			// Fall back to the old way of searching all bytes in the array for a match.  
			// Should not be needed, but since we used to do this, lets be sure we don't break anything.
			return findArray(wave, chunkOffset, chunkID);
		}
		// See if the next or any subsequent subchunks match the given chunk id. 
		return findSubChunk(wave, nextChunkOffset, chunkID, stopChunkID); 
	}


	/**
	 * Search from the given start index for the given string and return the index of that string.
	 * @param wave
	 * @param start
	 * @param match
	 * @return 0 if not found.
	 */
	private static int findArray(byte[] wave, int start, String match) {
		for ( ; start < wave.length-match.length(); start++) {
			if (isMatch(wave, start, match))
				return start;
		}
		return 0;
	}

	/**
	 * Read the next 4 bytes from the array at the given index into an integer assuming little endian ordering.
	 * @param wave
	 * @param index
	 * @return
	 */
	private static int getLittleEndianInteger(byte[] wave, int index) {
		return (int)  (( wave[index] & 0xff) 
					| (( wave[index+1] <<  8) & 0xff00)
					| (( wave[index+2] << 16) & 0xff0000) 
					| (( wave[index+3] << 24) & 0xff000000));
	}

	/**
	 * Convenience over {@link #isMatch(byte[], int, String)}) to throw an exception if
	 * the given String is not found at the given index in the byte array.
	 * @param wave
	 * @param index
	 * @param match
	 */
	private static void checkArray(byte[] wave, int index, final String match) {
		if (!isMatch(wave,index,match))
			throw new IllegalArgumentException("Header did not contain '" + match + "' beginning at byte " + index);
	}

	/**
	 * See if the given string of characters is matched in the given byte array starting at the given index. 
	 * @param wave
	 * @param index
	 * @param match
	 * @return
	 */
	private static boolean isMatch(byte[] wave, int index, final String match) {
		for (int i=0 ; i<match.length(); i++) {
			char c = match.charAt(i);
			if (index >= wave.length || c != wave[index])
				return false;
			index++;
		}
		return true;
		
	}

	public static SoundClip WAVtoPCMNow(String soundFile) throws IOException {
		return ReadPCM(System.currentTimeMillis(), soundFile, WAV_FORMAT);
	}

	
	private final static int BYTE_RANGE_SIZE= (1<<8) - 1;
	private final static int BYTE_RANGE_MIN = -128;
	private final static int BYTE_RANGE_MAX = 127;

	private final static int SHORT_RANGE_SIZE= (1<<16) - 1;
	private final static int SHORT_RANGE_MIN = Short.MIN_VALUE;
	private final static int SHORT_RANGE_MAX = Short.MAX_VALUE;
	
	private final static long THREEBYTE_RANGE_SIZE= (1<<24) - 1;

	private final static long INT_RANGE_SIZE= (((long)1)<<32) - 1;
	private final static int INT_RANGE_MIN = Integer.MIN_VALUE;
	private final static int INT_RANGE_MAX = Integer.MAX_VALUE;

	/**
	 * If set to true, then positive and negative pcm values will be mapped to floatng point values with the same scale.  
	 * In this case, the maximum double value mapped from a pcm value will be something less then 1, so that for example,
	 * the 8-bit range [-128, 127] maps to [-1, .992].
	 * If set to false, then different scaling values are used for positive and negative values so that the mapped range is [1,-1].
	 * This is NOT intended to be altered across deployments and is only really enabled with a property setting to do some
	 * easy comparison of performance in the python models - dawood 3/2021.
	 */
	final static boolean USE_SYMETRIC_RANGE = AISPProperties.instance().getProperty("pcmutils.symmetric-range.enabled", true);

	/**
	 * Get the normalization factor that converts an pcm value in the range allowed by the given
	 * bitsPerSample value, into the range [-1,1). 
	 * For example, with 8-bit pcm, values in [-128...0...127] are scaled to [-1, 1] by
	 * dividing the pcm value by the return value from this.
	 * Note that the scaling factor is different depending on whether the value to be scaled is positive or negative
	 * because the pcm range is not symmetric around 0 and we want to keep 0 mapped to zero in both double and pcm values.
	 * @param bitsPerSample
	 * @return a value larger than 1, that can scale a pcm value into the range [-1,1] with the correct offset.
	 */	
	private static double getPCM2DoubleScalar(double dataValue, int bitsPerSample) {
		double scale;
		if (bitsPerSample == 8) 
			scale = (BYTE_RANGE_SIZE);	
		else if (bitsPerSample == 16)
			scale = (SHORT_RANGE_SIZE);	
		else if (bitsPerSample == 24)
			scale = (THREEBYTE_RANGE_SIZE);
		else if (bitsPerSample == 32)
			scale = (INT_RANGE_SIZE);
		else
			throw new RuntimeException("unsupported bitsPerSampleValue. Must be 8, 16,24 or 32.");
		if (USE_SYMETRIC_RANGE || dataValue <= 0)
			scale = scale + 1;
		return scale;
	}

	/**
	 * Convert the given pcmValue in the range allowed by the given bitsPerSample to a double in the range [-1,1].
	 * For bitsPerSample=8, the range is assumed to be [-128,127].
	 * @param pcmValue
	 * @param bitsPerSample
	 * @return double value in the range -1 to 1.
	 */
	private static double pcm2Double(int pcmValue, int bitsPerSample) {
		long pcmMin = - (1<<(bitsPerSample-1));
		double range =  getPCM2DoubleScalar(pcmValue, bitsPerSample);
		long pcms = pcmValue - pcmMin;
		double percentPCMs = pcms / range;
		double t = -1.0 + 2 * percentPCMs; 
		if (t > 1) {
			t = 1;
		} else if (t < -1) {
			t = -1;
		}
		return t;
	}




	/**
	 * Scale array of doubles in the range -1..1 to an array of PCM bytes.
	 * @param data data to put in return byte array.  Values are centered around 0 for all values of bitsPerSample.
	 * @param channels number of channels in the output array.
	 * @param bitsPerSample bits per sample to use in the output array
	 * @return an array of length intData.length * channels * bitsPerSample/8.
	 * Values are in unsigned 8 bit for bitsPerSample=8, and signed for all others, per wav file specification.
	 */
	public static byte[] double2PCM(double[] data, int channels, int bitsPerSample) throws IllegalArgumentException, RuntimeException {

		if (channels < 1 || channels > 2)
			throw new RuntimeException("bad channels value");
		if (bitsPerSample != 8 && bitsPerSample != 16 && bitsPerSample != 32) 
			throw new RuntimeException("unsupported bitsPerSampleValue. Must be 8, 16 or 32.");


		if (data == null || data.length == 0 || channels <= 0 || bitsPerSample <= 0)
			throw new IllegalArgumentException("non-postive value given");
		double dlen = (double) data.length * channels * bitsPerSample / 8;
		if (dlen > Integer.MAX_VALUE)
			throw new IllegalArgumentException("Buffer is too large and can't be handled by ByteBuffer");
		int len = (int)dlen;
		ByteBuffer bb = ByteBuffer.allocate(len);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		for (int i=0 ; i< data.length ; i++) {
			double value = data[i];
			if (value < -1 || value > 1)
				throw new IllegalArgumentException("Value at index " + i + " is not in the range -1..1 and has value " + value);
			long t = double2PCM(value, bitsPerSample);
			if (bitsPerSample == 8) {
				byte v = (byte)(t);
				bb.put(v);
				if (channels == 2)
					bb.put(v);  //Assign same value for both channels
			} else if (bitsPerSample == 16) {
				short v = (short)t;
				bb.putShort(v);
				if (channels == 2)
					bb.putShort(v);  //Assign same value for both channels
			} else if (bitsPerSample == 32) {
				int v = (int)t;
				bb.putInt(v);
				if (channels == 2)
					bb.putInt(v);  //Assign same value for both channels
			} else {
				throw new RuntimeException("not expected here");
			}
		}
		return bb.array();
	}


	/**
	 * Convert a value in the range [-1,1] to a value in the range defined by the [-(2**bitsPerSample), 2**bitsPerSample - 1] 
	 * @param value
	 * @param bitsPerSample
	 * @return
	 * @throws IllegalArgumentException if unsupported number of bits or value is out of range.
	 */
	private static long double2PCM(double value, int bitsPerSample) {
		if (value < -1 || value > 1) 
			throw new IllegalArgumentException("Value " + value + " is not in the range -1..1");
		double scale = getPCM2DoubleScalar(value, bitsPerSample);
		long t;
		double t2 = (1+value)* scale / 2;
		switch (bitsPerSample) {
			case 8:
				t = BYTE_RANGE_MIN + Math.round(t2);
				if (t > BYTE_RANGE_MAX) 
					t = BYTE_RANGE_MAX;
				else if (t < BYTE_RANGE_MIN)
					t = BYTE_RANGE_MIN;
				break;
			case 16:
				t = SHORT_RANGE_MIN + Math.round(t2); 
				if (t > SHORT_RANGE_MAX) 
					t = SHORT_RANGE_MAX;
				else if (t < SHORT_RANGE_MIN)
					t = SHORT_RANGE_MIN;
				break;
			case 32:
				t = INT_RANGE_MIN + Math.round(t2);
				if (t > INT_RANGE_MAX) 
					t = INT_RANGE_MAX;
				else if (t < INT_RANGE_MIN)
					t = INT_RANGE_MIN;
				break;
			default:
				throw new IllegalArgumentException("Only supporting 8, 16 and 32 bits per sample.");
		}	
		return t;
	}


	/**
	 * Get the MP3 at the given URL and convert it to a SoundClip.
	 * @param startTimeMsec the start time of the returned clip.
	 * @param url
	 * @return
	 * @throws IOException
	 */
	private static SoundClip MP3toPCM(double startTimeMsec, URL url) throws IOException {
		// Android does not have javax.sound. so MP3 support is moved into the porting layer.
		try {
			return RuntimePortLayer.instance().MP3toPCM(startTimeMsec, url);
		} catch (AISPException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
	
	

}
