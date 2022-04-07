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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.bind.DatatypeConverter;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundClip;

public class J2SEPortLayer implements IPortLayer {

	@Override
	public String toBase64(byte[] bytes) {
		return DatatypeConverter.printBase64Binary(bytes);
	}

	@Override
	public byte[] fromBase64(String base64) {
		return DatatypeConverter.parseBase64Binary(base64);
	}
	
	/**
	 * Get the MP3 at the given URL and convert it to a SoundClip.
	 * @param startTimeMsec the start time of the returned clip.
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws AISPException
	 */
	public SoundClip MP3toPCM(double startTimeMsec, URL url) throws IOException, AISPException {
		AudioInputStream ais;
		try {
			ais = AudioSystem.getAudioInputStream(url);
		} catch (UnsupportedAudioFileException e) {
			throw new AISPException("Unsupported audio format: " + e.getMessage(), e);
		} 
//		AudioFormat mp3Format = ais.getFormat();
//		AudioFormat[] targets = AudioSystem.getTargetFormats(Encoding.PCM_SIGNED, mp3Format);
//		for (AudioFormat f : targets) 
//			System.out.println("target=" + f);
		float samplingRate = 44100;
		int bitsPerSample = 16;
		int channels = 2;
		int frameSize = bitsPerSample * channels / 8;
		int frameRate= (int)samplingRate;
		boolean bigEndian = false;
		AudioFormat format = new AudioFormat(Encoding.PCM_SIGNED, samplingRate, bitsPerSample, channels, frameSize,frameRate,bigEndian);
		ais = AudioSystem.getAudioInputStream(format, ais);
		ByteArrayOutputStream pcm = new ByteArrayOutputStream();
		byte[] buffer = new byte[44100];
		int count;
		while ((count = ais.read(buffer)) > 0) 
			pcm.write(buffer,0,count);
		SoundClip clip = new SoundClip(startTimeMsec, format.getChannels(), format.getSampleSizeInBits(), (int)format.getSampleRate(), pcm.toByteArray()); 
		return clip;
	}

}
