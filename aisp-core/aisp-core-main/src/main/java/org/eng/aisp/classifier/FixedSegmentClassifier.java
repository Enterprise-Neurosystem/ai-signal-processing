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
package org.eng.aisp.classifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.IDataWindow;

/**
 * Provides ability to classify fixed sized subwindows of a given sound.
 * @author dawood
 *
 * @param <WINDATA>
 */
public class FixedSegmentClassifier<WINDATA> {
	
	protected final IFixedClassifier<WINDATA> classifier;
	protected final double subwindowMsec;
	/**
	 * @param classifier
	 * @param subwindowMsec 0 or larger size of windows to classify of the larger windows givens to {@link #classify(IDataWindow)}.
	 * If 0, then don't segment the window and provide only a single classification for the whole window.
	 */
	public FixedSegmentClassifier(IFixedClassifier<WINDATA> classifier, double subwindowMsec) {
		this.classifier = classifier;
		this.subwindowMsec = subwindowMsec;
	}

	/**
	 * Provide a segmented classification of the given data window.
	 * The window is broken into fix-sized subwindows defined by a subwindow size passed to the constructor
	 * (the last segment may be smaller than the subwindow size).
	 * The subwindows are then classified using the classifier provided to the constructor.
	 * Consecutive subwindows with the same classification results (labels and label values)
	 * are grouped into larger subwindows and their classification results captured in SegmentClassification
	 * defining the start and stop time of the segment within the given window.
	 * If the segment size is 0, then the whole clip is classified into a single SegmentClassification.
	 * @param window
	 * @return never null, but empty if the given window is smaller than the subwindow size.
	 * @throws AISPException
	 */
	public List<SegmentClassification> classify(IDataWindow<WINDATA> window) throws AISPException {
		List<SegmentClassification> classifications = new ArrayList<SegmentClassification>();
		double clipLenMsec = window.getDurationMsec();
		if (clipLenMsec == 0)
			return classifications;
		
		List<SegmentClassification> segmentClassifications = new ArrayList<SegmentClassification>();

		if (subwindowMsec == 0) {
			Map<String, Classification> cmap = classifier.classify(window);
			SegmentClassification sl = new SegmentClassification(0, window.getDurationMsec(), cmap);
			classifications.add(sl);	
		} else {
			// Compute classifications for all segments of the given window
			// Segments are of fixed length equal to subwindowMsec, except the last which is likely a partial window.
			double endMsec = window.getStartTimeMsec();
			boolean done = false;
			while (!done) {
				double startMsec = endMsec; 
				endMsec = startMsec + subwindowMsec;
				if (endMsec > window.getEndTimeMsec()) {
					// This is the last subwindow
					endMsec = clipLenMsec;
					done = true;
				}
				if (endMsec - startMsec < subwindowMsec / 2)
					break;
				IDataWindow<WINDATA> subWindow= window.subWindow(startMsec, endMsec);
				if (subWindow != null) {	// Should never happen, but just in case...
					Map<String, Classification> cmap = classifier.classify(subWindow);
					SegmentClassification sl = new SegmentClassification(startMsec, endMsec, cmap);
					segmentClassifications.add(sl);
				}
			}
//			Gson gson = new GsonBuilder().setPrettyPrinting().create();
//			System.out.println("Segments...\n" + gson.toJson(segmentClassifications));
			// Group segments that are similar.
			classifications = groupSegments(segmentClassifications);
		}
		
		return classifications;
	}
		
		
	/**	
	 * Build the list of SegmentClassification instances, in which each instance represents a consecutive
	 * list of SegmentClassifications with the same classification results (labels and label values). 
	 * @param segmentClassifications
	 * @return never null.
	 */
	private List<SegmentClassification> groupSegments(List<SegmentClassification> segmentClassifications) {
		List<SegmentClassification> groupedClassifications = new ArrayList<SegmentClassification>();

		SegmentClassification first = null;	// First of a sequence.
		SegmentClassification last = null;	// last segment we examined.
		for (SegmentClassification sc : segmentClassifications) {
			if (first == null) {
				// The first window in a series
				first = sc;
			} else if (!sameClassification(sc.getClassification(),first.getClassification())) {
				// This subwindow is not the same as the previous subwindow.
				SegmentClassification gsc = new SegmentClassification(first.getStartMsec(), last.getEndMsec(), last.getClassification());
				groupedClassifications.add(gsc);
				first = sc;
			}
			last = sc;
		}
		
		// Include the last segment(s) that we were in the middle of accumulating when we hit the end of the list. 
		if (first != null) {
			SegmentClassification gsc = new SegmentClassification(first.getStartMsec(), last.getEndMsec(), last.getClassification());
			groupedClassifications.add(gsc);
		}

		return groupedClassifications;
	}


	/**
	 * Determine if the two classification maps have the same set of labelname=labelvalue pairs.
	 * Do not consider the ranked values in the Classifications.
	 * @param c1Map
	 * @param c2Map
	 * @return true if the classifications have the same set of labels and label values.
	 */
	private boolean sameClassification(Map<String, Classification> c1Map, Map<String, Classification> c2Map) {
		for (String labelName : c1Map.keySet()) {
			Classification c1 = c1Map.get(labelName);
			Classification c2 = c2Map.get(labelName);
			if (c2 == null)
				return false;		// Probably never get here
			String labelValue1 = c1.getLabelValue();
			String labelValue2 = c2.getLabelValue();
			if (!labelValue1.equals(labelValue2))
				return false;
		}
		return true;
	}



}
