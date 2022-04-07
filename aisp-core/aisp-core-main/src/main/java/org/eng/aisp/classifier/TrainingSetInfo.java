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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eng.aisp.IDataWindow;
import org.eng.aisp.ILabeledDataWindow;

/**
 * Contains information about a set of ILabeledDataWindows.
 * Information includes
 * <ul>
 * <li> total number of samples
 * <li> total milliseconds of sampled data
 * <li> label names present
 * <li> for each label name
 *    <ul>
 *    <li> the set of label values present
 *    <li> for each label value
 *        <ul>
 *       <li> the total number of samples
 *       <li> the total number of sample milleseconds.
 *    </ul>
 * </ul>
 * @author dawood
 *
 */
public class TrainingSetInfo extends SampleCounter implements Iterable<TrainingSetInfo.LabelInfo>, Serializable {

	private static final long serialVersionUID = -2961875492648203973L;
	

	static String formatMsec(double msec) {
		double seconds = msec/1000.0;
		final int secondsPerMinute = 60;
		final int secondsPerHour = 60 * secondsPerMinute;
		int hours = (int)(seconds / secondsPerHour);
		seconds = seconds % secondsPerHour;
		int minutes = (int)(seconds / secondsPerMinute);
		seconds = seconds % secondsPerMinute;
		return String.format("%02d:%02d:%02.3f", hours, minutes, seconds);
	}
	
	/**
	 * Holds information about all the values that a given label name takes.
	 * Each label value has an associated LabelValueInfo object that gives
	 * counts and time for that label value.
	 * @author dawood
	 
	 */
	public static class LabelInfo extends SampleCounter implements Iterable<LabelValueInfo>, Serializable {

		private static final long serialVersionUID = -2424272009268336709L;
		protected final String labelName;
		protected Map<String,LabelValueInfo> labelValues = new HashMap<String,LabelValueInfo>();
		
		public LabelInfo(String labelName) {
			if (labelName == null)
				throw new IllegalArgumentException("null values not allowed");
			this.labelName = labelName;
		}
		
		
		protected void appendLabelValue(String labelValue, double durationMsec) {
			LabelValueInfo lvi = labelValues.get(labelValue);
			if (lvi == null) {
				lvi = new LabelValueInfo(labelName, labelValue);
				labelValues.put(labelValue, lvi);
			}
			lvi.appendSample(durationMsec);
			this.appendSample(durationMsec);
		}
		
		/**
		 * Get the set of label values contained here.
		 * @return never null.
		 */
		public Set<String> getLabelValues() {
			return labelValues.keySet();
		}
		
		/**
		 * Get the info for a given label value
		 * @param labelValue
		 * @return null if label value not contained here.
		 */
		public LabelValueInfo getLabelInfo(String labelValue) {
			return labelValues.get(labelValue);
		}
		
		public String getLabelName() {
			return labelName;
		}

		
		public String prettyFormat() {
			String msg = "Label: " + this.getLabelName() + ", " + this.totalSamples + " samples, " + this.getLabelValues().size() + " value(s), " + formatMsec(this.totalMsec) + " hr:min:sec";
		    List<String> sorted = new ArrayList<String>();
		    sorted .addAll(this.labelValues.keySet());
		    Collections.sort(sorted);
		    for (String value: sorted) {
				LabelValueInfo lvi = this.labelValues.get(value); 
				msg += "\n  ";
				msg += lvi.prettyFormat();
			}
			return msg;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			final int maxLen = 5;
			return "LabelInfo [totalSamples=" + totalSamples + ", totalMsec=" + totalMsec + ", labelName=" + labelName
					+ ", labelValues=" + (labelValues != null ? toString(labelValues.entrySet(), maxLen) : null) + "]";
		}


		private String toString(Collection<?> collection, int maxLen) {
			StringBuilder builder = new StringBuilder();
			builder.append("[");
			int i = 0;
			for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
				if (i > 0)
					builder.append(", ");
				builder.append(iterator.next());
			}
			builder.append("]");
			return builder.toString();
		}


		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((labelName == null) ? 0 : labelName.hashCode());
			result = prime * result + ((labelValues == null) ? 0 : labelValues.hashCode());
			return result;
		}


		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (!(obj instanceof LabelInfo))
				return false;
			LabelInfo other = (LabelInfo) obj;
			if (labelName == null) {
				if (other.labelName != null)
					return false;
			} else if (!labelName.equals(other.labelName))
				return false;
			if (labelValues == null) {
				if (other.labelValues != null)
					return false;
			} else if (!labelValues.equals(other.labelValues))
				return false;
			return true;
		}


		@Override
		public Iterator<LabelValueInfo> iterator() {
			return this.labelValues.values().iterator();
		}

	}

	
	/**
	 * Holds information about a single label name/value pair.
	 * Information for the label value includes total number of samples and
	 * duration for all samples with a given label value.
	 * @author dawood
	 *
	 */
	public static class LabelValueInfo extends SampleCounter implements Serializable {
		private static final long serialVersionUID = -3473245316819214458L;
		protected final String labelName;
		protected final String labelValue;

		public LabelValueInfo(String labelName, String labelValue) {
			super();
			if (labelName == null || labelValue == null)
				throw new IllegalArgumentException("null values not allowed");
			this.labelName = labelName;
			this.labelValue = labelValue;
		}


		public String prettyFormat() {
			String msg = "Value: " + this.getLabelValue() + ", " + this.totalSamples + " samples, " + formatMsec(this.totalMsec) + " hr:min:sec";
			return msg;
		}


		/**
		 * @return the labelName
		 */
		public String getLabelName() {
			return labelName;
		}

		/**
		 * @return the labelValue
		 */
		public String getLabelValue() {
			return labelValue;
		}


		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "LabelValueInfo [totalSamples=" + totalSamples + ", totalMsec=" + totalMsec + ", labelName="
					+ labelName + ", labelValue=" + labelValue + "]";
		}


		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((labelName == null) ? 0 : labelName.hashCode());
			result = prime * result + ((labelValue == null) ? 0 : labelValue.hashCode());
			return result;
		}


		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (!(obj instanceof LabelValueInfo))
				return false;
			LabelValueInfo other = (LabelValueInfo) obj;
			if (labelName == null) {
				if (other.labelName != null)
					return false;
			} else if (!labelName.equals(other.labelName))
				return false;
			if (labelValue == null) {
				if (other.labelValue != null)
					return false;
			} else if (!labelValue.equals(other.labelValue))
				return false;
			return true;
		}
	}

//	protected final int totalSamples;
//	protected final double totalMsec;
	protected final Map<String,LabelInfo> labelNames;
	

	protected TrainingSetInfo(int totalSamples, double totalMsec, Map<String, LabelInfo> labelInfoMap) {
		super(totalSamples, totalMsec);
		this.labelNames = labelInfoMap;
	}

	/**
	 * Method use to iterate through a set of labeled data windows to extract info on each label and value.
	 * @param labeledData
	 * @return never null.
	 * @throws IOException
	 */
	public static TrainingSetInfo getInfo(Iterable<? extends ILabeledDataWindow<?>> labeledData) { 
		int count = 0;
		double totalMsec = 0;
		Map<String,LabelInfo> labelInfoMap = new HashMap<String,LabelInfo>(); 
		for (ILabeledDataWindow<?> ldw : labeledData) {
			IDataWindow<?> dw = ldw.getDataWindow();
			Properties labels = ldw.getLabels();
			double durationMsec = dw.getDurationMsec();
			for (Object key : labels.keySet()) {
				String labelName = key.toString();
				String labelValue = labels.getProperty(labelName);
				LabelInfo labelInfo = labelInfoMap.get(labelName);
				if (labelInfo == null)  {
					labelInfo = new LabelInfo(labelName);
					labelInfoMap.put(labelName, labelInfo);
				}
				labelInfo.appendLabelValue(labelValue, durationMsec);
			}
			totalMsec += durationMsec; 
			count++;
		}
		return new TrainingSetInfo(count, totalMsec, labelInfoMap); 
	}
	
	/**
	 * Get the list of label names.
	 * @return never null.
	 */
	public Set<String> getLabelNames() {
		return this.labelNames.keySet();
	}
	
	/**
	 * Get the label info for a given label.
	 * @param labelName name of label
	 * @return null label name is not present.
	 */
	public LabelInfo getLabelInfo(String labelName) {
		return this.labelNames.get(labelName);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return "TrainingSetInfo [totalSamples=" + totalSamples + ", totalMsec=" + totalMsec + ", labelInfo="
				+ (labelNames != null ? toString(labelNames.entrySet(), maxLen) : null) + "]";
	}

	private String toString(Collection<?> collection, int maxLen) {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		int i = 0;
		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
			if (i > 0)
				builder.append(", ");
			builder.append(iterator.next());
		}
		builder.append("]");
		return builder.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((labelNames == null) ? 0 : labelNames.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof TrainingSetInfo))
			return false;
		TrainingSetInfo other = (TrainingSetInfo) obj;
		if (labelNames == null) {
			if (other.labelNames != null)
				return false;
		} else if (!labelNames.equals(other.labelNames))
			return false;
		return true;
	}

	public String prettyFormat() {
		String msg = "Total: " + this.totalSamples + " samples, " + this.getLabelNames().size() + " label(s), " + formatMsec(this.totalMsec) + " hr:min:sec";
		List<String> sortedNames = new ArrayList<String>();
		sortedNames.addAll(labelNames.keySet());
		Collections.sort(sortedNames);
		for (String name : sortedNames) {
			LabelInfo li = labelNames.get(name);
			msg += "\n";
			msg += li.prettyFormat();
		}
		return msg;
	}

	@Override
	public Iterator<LabelInfo> iterator() {
		return labelNames.values().iterator();
	}

	/**
	 * Remove a label.  
	 * Useful for the 'monitor' label.
	 * @param labelName
	 */
	public void removeLabel(String labelName) {
		labelNames.remove(labelName);
	}
	
//	public Iterable<LabelValueInfo> iterable(String labelName) {
//		LabelInfo linfo = labelNames.get(labelName);
//		return linfo; // this.labelNames.get(labelName).iterator();
//	}

}
