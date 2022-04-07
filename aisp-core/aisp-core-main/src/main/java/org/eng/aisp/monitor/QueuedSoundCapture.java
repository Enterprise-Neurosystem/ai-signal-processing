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

import org.eng.aisp.SoundClip;
import org.eng.aisp.util.PCMUtil;

/**
 * Extends the super class to use a QueuedDataHandler to capture/maintain a queue of recent sounds that can be accessed with {@link #next(int)}.
 * 
 * @author dawood
 *
 */
public class QueuedSoundCapture extends QueuedDataCapture<SoundClip>  {
	
	public QueuedSoundCapture(int clipLenMsec, int pauseMsec) {
		super(new SoundCapture(clipLenMsec, pauseMsec));
	}
	
	public static void main(String[] args) throws Exception {
		QueuedSoundCapture capture = new QueuedSoundCapture(1000, 0);
		capture.start();
		Thread.sleep(5000);
		SoundClip clip;
		int count = 0;
		while ((clip = capture.next(3000)) != null) {
			System.out.println("Got a clip:" + clip);
			System.out.println("duration: " + clip.getDurationMsec());
			String fileName = "soundcapture" + count + ".wav";
			PCMUtil.PCMtoWAV(fileName, clip);
			if (count++ > 10)
				capture.stop();	// Then blockingNext() should return null.
		}
	}

	/**
	 * Override to maintain the semantics that this start method stars the provider.
	 */
	@Override
	public void start() throws Exception {
		super.start();
		this.dataProvider.start();
	}

	@Override
	public void stop() {
		this.dataProvider.stop();
		super.stop();
	}

}
