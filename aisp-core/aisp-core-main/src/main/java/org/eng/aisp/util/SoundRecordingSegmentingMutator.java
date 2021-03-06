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

import org.eng.aisp.IDataWindow;
import org.eng.aisp.IDataWindow.PadType;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;

public class SoundRecordingSegmentingMutator extends AbstractSegmentingMutator<SoundRecording> {

	public SoundRecordingSegmentingMutator(double windowMsec, PadType padType) {
		super(windowMsec, padType);
	}

	@Override
	protected SoundRecording newLDW(SoundRecording item, IDataWindow<double[]> subwin) {
		return new SoundRecording(item, (SoundClip)subwin);
	}

}
