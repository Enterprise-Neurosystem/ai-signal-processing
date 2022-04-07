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

import java.util.Map;

/**
 * Contains classifications results for a specific segment of an IDataWindow defined by a start and end time in the window. 
 * @author dawood
 *
 */
public class SegmentClassification {
	protected final double startMsec;
	protected final double endMsec;
	protected final Map<String, Classification> classification;

	// For Gson/Jackson
	protected SegmentClassification() {
		this(0,0,null);
	}

	public SegmentClassification(double startMsec, double endMsec, Map<String, Classification> classification) {
		this.startMsec = startMsec;
		this.endMsec = endMsec;
		this.classification = classification;
	}

	/**
	 * Get the offset into the associated IDataWindow of the segment start time for which the classification applies.
	 * @return 0 or larger.
	 */
	public double getStartMsec() {
		return startMsec;
	}

	/**
	 * Get the offset into the associated IDataWindow of the segment end time for which the classification applies.
	 * @return 0 or larger.
	 */
	public double getEndMsec() {
		return endMsec;
	}

	/**
	 * Get the classification results that apply for the segment of the associated clip.
	 * The returned map is keyed by the label name with values being the classification result for that label name.
	 * @return
	 */
	public Map<String, Classification> getClassification() {
		return classification;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classification == null) ? 0 : classification.hashCode());
		long temp;
		temp = Double.doubleToLongBits(endMsec);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(startMsec);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SegmentClassification))
			return false;
		SegmentClassification other = (SegmentClassification) obj;
		if (classification == null) {
			if (other.classification != null)
				return false;
		} else if (!classification.equals(other.classification))
			return false;
		if (Double.doubleToLongBits(endMsec) != Double.doubleToLongBits(other.endMsec))
			return false;
		if (Double.doubleToLongBits(startMsec) != Double.doubleToLongBits(other.startMsec))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SegmentClassification [startMsec=" + startMsec + ", endMsec=" + endMsec + ", classification=" + classification + "]";
	}
}
