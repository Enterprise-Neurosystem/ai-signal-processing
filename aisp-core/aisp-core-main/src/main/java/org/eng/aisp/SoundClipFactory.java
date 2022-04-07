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

public class SoundClipFactory implements IDataWindowFactory<double[], SoundClip> {

	private static final long serialVersionUID = -2748801250357011581L;

	@Override
	public SoundClip newDataWindow(double startTimeMsec, double endTimeMsec, double[] pcmData) {
		return new SoundClip(startTimeMsec, endTimeMsec, pcmData);
	}

}
