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
package org.eng.aisp.dataset;

import org.eng.aisp.SoundRecording;

/**
 * Extends the super class to add optional segmenting of the referenced sound.
 * Segmentation is only intended to be done when the end offset msec is non-zero.
 * An implementations {@link #apply(SoundRecording)} method must also apply the segmentation, if any.
 */
public interface IReferencedSoundSpec extends IReferencedDataSpec<SoundRecording> {

	/**
	 * Get the start offset of this segment within the referenced segment.
	 * @return a number from 0 to 1 less than the length of the clip in msec. 
	 */
	int getStartMsec();

	/**
	 * Get the end offset of this segment within the referenced segment.
	 * @return 0 there is no segmentation to be done, otherwise a number larger than the value returned by {@link #getStartMsec()}. 
	 */
	int getEndMsec();
	
	/**
	 * Determine if this spec applies to/specifies a sub-segment of the referenced sound.
	 * This is really a convenience on testing getSegmentEndMsec() > 0.
	 * @return true if this represents a sub-segment of the referenced sound.  false if the whole referenced sound.
	 */
	boolean isSubSegment();


}