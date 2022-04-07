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

import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.IDataWindow;
import org.eng.aisp.IDataWindow.PadType;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.util.IMutator;

public abstract class AbstractSegmentingMutator<LDW extends ILabeledDataWindow<double[]>> implements IMutator<LDW,LDW> {
	
	final double windowMsec;
	final IDataWindow.PadType padType;

	/**
	 * @param windowMsec
	 * @param padType
	 */
	public AbstractSegmentingMutator(double windowMsec, PadType padType) {
		this.windowMsec = windowMsec;
		this.padType = padType;
	}

	protected abstract LDW newLDW(LDW item, IDataWindow<double[]> subwin);

	@Override
	public List<LDW> mutate(LDW item) {
		List<IDataWindow<double[]>> currentClips = null;
		IDataWindow<double[]> window = item.getDataWindow();
		double durationMsec = window.getDurationMsec();
		if (durationMsec == windowMsec) {
			currentClips = new ArrayList<IDataWindow<double[]>>();
			currentClips.add(window);
		} else if (durationMsec < windowMsec) {
			// Next recording is shorter than the requested window size. so just pad it as requested. 
			window = tryPadding(window);
			if (window != null) {
				currentClips = new ArrayList<IDataWindow<double[]>>();
				currentClips.add(window);
			}
		} else { 	// Window is larger than requested, so split it up.
			boolean keepTrailingWindow = padType != PadType.NoPad;
			currentClips = (List<IDataWindow<double[]>>) window.splitOnTime(windowMsec, keepTrailingWindow);
			int size = currentClips.size();
			if (size > 0  && keepTrailingWindow) {
				IDataWindow<double[]> lastWindow = currentClips.get(size - 1);
				if (lastWindow.getDurationMsec() != this.windowMsec) {
					// Make sure the last window is padded, if we're keeping trailing partial windows.
					lastWindow = tryPadding(lastWindow);
					if (lastWindow != null)
						currentClips.set(size-1, lastWindow);
					else
						currentClips.remove(size-1);
				}
			}
		}
		if (currentClips == null)
			return null;
		
		List<LDW> ldwList = new ArrayList<LDW>();
		for (IDataWindow<double[]> subwin : currentClips) {
			LDW subldw = newLDW(item, subwin);
			ldwList.add(subldw);
		}
		return ldwList;
	}
	
	private IDataWindow<double[]> tryPadding(IDataWindow<double[]> window) {
		double durationMsec = window.getDurationMsec();
		assert durationMsec <= windowMsec;
		// Never pad a window that is not at least half the requested length.
		if (durationMsec < this.windowMsec/2) 
			return null;

		if (durationMsec == this.windowMsec)
			return window;
		
		if (padType == PadType.NoPad) {		// Window is short and can't be padded.
			window = null;
		} else {
			// Padding windows to the requested size is allowed.
			window = window.resize(windowMsec, padType);
		}	
		return window;
	}
	
}