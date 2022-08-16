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

import org.eng.aisp.AISPLogger;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.IDataWindow.PadType;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.util.IMutator;

public abstract class AbstractSegmentingMutator<LDW extends ILabeledDataWindow<double[]>> implements IMutator<LDW,LDW> {
	
	private final double windowShiftMsec;
	private final double windowSizeMsec;
	private final IDataWindow.PadType padType;

	/**
	 * @param windowSizeMsec the size of the subwindows.
	 * @param windowShiftMsec if 0 or less then create rolling subwindows, if larger than 0 the create sliding subwindows.  In general,
	 * this should be smaller than the window size, but there is nothing requiring that.
	 * @param padType 
	 */
	public AbstractSegmentingMutator(double windowSizeMsec, double windowShiftMsec, PadType padType) {
		this.windowSizeMsec = windowSizeMsec;
		this.windowShiftMsec = windowShiftMsec;
		this.padType = padType;
	}

	protected abstract LDW newLDW(LDW item, IDataWindow<double[]> subwin);

	@Override
	public List<LDW> mutate(LDW item) {
		List<IDataWindow<double[]>> currentClips = null;
		IDataWindow<double[]> window = item.getDataWindow();
		double durationMsec = window.getDurationMsec();
		if (durationMsec == windowSizeMsec) {
			currentClips = new ArrayList<IDataWindow<double[]>>();
			currentClips.add(window);
		} else if (durationMsec < windowSizeMsec) {
			// Next recording is shorter than the requested window size. so just pad it as requested. 
			window = tryPadding(window);
			if (window != null) {
				currentClips = new ArrayList<IDataWindow<double[]>>();
				currentClips.add(window);
			}
		} else { 	// Window is larger than requested, so split it up.
			if (windowShiftMsec <=0 || windowShiftMsec == windowSizeMsec)
				currentClips = getRollingSubWindows(window);
			else
				currentClips = getSlidingSubWindows(window);
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

	private List<IDataWindow<double[]>> getSlidingSubWindows(IDataWindow<double[]> window) {
		List<IDataWindow<double[]>> subList = new ArrayList<>();
		double startMsec = 0;
		double nextEndMsec = this.windowSizeMsec; 
		double durationMsec = window.getDurationMsec();
		boolean keepTrailingWindow = padType != PadType.NoPad;
		while (nextEndMsec <= durationMsec)  {
			IDataWindow<double[]> sub = window.subWindow2(startMsec, nextEndMsec);
			if (sub == null) 
				throw new RuntimeException(window.getClass() + " could not compute sub-window from " + startMsec + " to " + nextEndMsec + " on window of length " + durationMsec + " msec.");
			subList.add(sub);
			startMsec += this.windowShiftMsec;
			nextEndMsec += this.windowShiftMsec;
			AISPLogger.logger.info("sub start=" + sub.getStartTimeMsec() + " end="+sub.getEndTimeMsec() 
			+ ",star/end=" + startMsec + "/" + nextEndMsec); 
		}
		
		if (keepTrailingWindow && startMsec < durationMsec) {
			IDataWindow<double[]> sub = window.subWindow2(startMsec, durationMsec);
			if (sub == null) 
				throw new RuntimeException(window.getClass() + " could not compute sub-window from " + startMsec + " to " + nextEndMsec + " on window of length " + durationMsec + " msec.");
			sub = this.tryPadding(sub); 
			if (sub != null)
				subList.add(sub);
		}

		return subList;
	}

	private List<IDataWindow<double[]>> getRollingSubWindows(IDataWindow<double[]> window) {
		List<IDataWindow<double[]>> currentClips;
		boolean keepTrailingWindow = padType != PadType.NoPad;
		currentClips = (List<IDataWindow<double[]>>) window.splitOnTime(windowSizeMsec, keepTrailingWindow);
		int size = currentClips.size();
		if (size > 0  && keepTrailingWindow) {
			IDataWindow<double[]> lastWindow = currentClips.get(size - 1);
			if (lastWindow.getDurationMsec() != this.windowSizeMsec) {
				// Make sure the last window is padded, if we're keeping trailing partial windows.
				lastWindow = tryPadding(lastWindow);
				if (lastWindow != null)
					currentClips.set(size-1, lastWindow);
				else
					currentClips.remove(size-1);
			}
		}
		return currentClips;
	}
	
	private IDataWindow<double[]> tryPadding(IDataWindow<double[]> window) {
		double durationMsec = window.getDurationMsec();
		assert durationMsec <= windowSizeMsec;
		// Never pad a window that is not at least half the requested length.
		if (durationMsec < this.windowSizeMsec/2) 
			return null;

		if (durationMsec == this.windowSizeMsec)
			return window;
		
		if (padType == PadType.NoPad) {		// Window is short and can't be padded.
			window = null;
		} else {
			// Padding windows to the requested size is allowed.
			window = window.resize(windowSizeMsec, padType);
		}	
		return window;
	}
	
}