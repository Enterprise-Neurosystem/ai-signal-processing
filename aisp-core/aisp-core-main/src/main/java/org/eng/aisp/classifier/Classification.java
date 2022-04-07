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

import java.util.Comparator;
import java.util.List;

/**
 * Captures the results of a classification produced by an IFixedClassifier. 
 * A single classification contains a label name, value confidence and optionally
 * a list of other ranked values for the label name. 
 * 
 * @author dawood
 */
public class Classification {
	public final static String UndefinedLabelValue = "";
	protected final String labelName;
	protected final String labelValue;
	protected final double confidence;
	protected final List<LabelValue> rankedValues;

	/**
	 * Used to sort the list of LabelValues into descending (highest first) confidence order.
	 * @param lvList
	 */
	public static class LabelValueComparator implements Comparator<LabelValue> {

		@Override
		public int compare(LabelValue o1, LabelValue o2) {
			return -Double.compare(o1.confidence, o2.confidence);
		}
		
	}
		
	public static class LabelValue {
		protected final String labelValue;
		protected final double confidence;


		// For Jackson/Jersey
		public LabelValue() { this(null, 0); }

		public LabelValue(String value, double conf) {
			this.labelValue = value;
			if (Double.isNaN(conf) || Double.isInfinite(conf))
				conf = 0;	// This to avoid JSON parsing errors.
			this.confidence = conf;
		}

		public String getLabelValue() {
			return labelValue;
		}

		public double getConfidence() {
			return confidence;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(confidence);
			result = prime * result + (int) (temp ^ (temp >>> 32));
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
			if (obj == null)
				return false;
			if (!(obj instanceof LabelValue))
				return false;
			LabelValue other = (LabelValue) obj;
			if (Double.doubleToLongBits(confidence) != Double.doubleToLongBits(other.confidence))
				return false;
			if (labelValue == null) {
				if (other.labelValue != null)
					return false;
			} else if (!labelValue.equals(other.labelValue))
				return false;
			return true;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "LabelValue [labelValue=" + labelValue + ", confidence=" + confidence + "]";
		}
	}

	// For Jackson/Jersey
	protected Classification() { this("", "", 0); }

	
	public Classification(String labelName, String labelValue, double confidence) {
		super();
		if (labelName == null)
			throw new IllegalArgumentException("Label name can not be null");
		if (labelValue == null)
			throw new IllegalArgumentException("Label value can not be null");
		if (confidence < 0 || confidence > 1)
			throw new IllegalArgumentException("confidence (" + confidence + ") must be in the range 0..1");
		this.labelName = labelName;
		this.labelValue = labelValue;
		this.confidence = confidence;
		this.rankedValues = null;
	}

	public Classification(String labelName, List<LabelValue> rankedValues) {
		if (labelName == null)
			throw new IllegalArgumentException("Label name can not be null");
		if (rankedValues.size() == 0)
			throw new IllegalArgumentException("rankedList must contain at least 1 element");
		LabelValue lv = rankedValues.get(0);
		this.labelName = labelName; 
		this.labelValue = lv.getLabelValue();
		this.confidence = lv.getConfidence(); 
		this.rankedValues = rankedValues;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return "Classification [labelName=" + labelName + ", labelValue=" + labelValue + ", confidence=" + confidence
				+ ", rankedValues="
				+ (rankedValues != null ? rankedValues.subList(0, Math.min(rankedValues.size(), maxLen)) : null) + "]";
	}

	/**
	 * @return the labelName
	 */
	public String getLabelName() {
		return labelName;
	}

	/**
	 * Get the highest scoring label value.
	 * @return the labelValue
	 */
	public String getLabelValue() {
		return labelValue;
	}

	/**
	 * Get the confidence for the highest scoring label value.
	 * @return the confidence
	 */
	public double getConfidence() {
		return confidence;
	} 
	
	/**
	 * Get the optional ranked list of values.
	 * @return null if not available.
	 */
	public List<LabelValue> getRankedValues() {
		return this.rankedValues;
	}
	

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(confidence);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((labelName == null) ? 0 : labelName.hashCode());
		result = prime * result + ((labelValue == null) ? 0 : labelValue.hashCode());
		result = prime * result + ((rankedValues == null) ? 0 : rankedValues.hashCode());
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
		if (!(obj instanceof Classification))
			return false;
		Classification other = (Classification) obj;
		if (Double.doubleToLongBits(confidence) != Double.doubleToLongBits(other.confidence))
			return false;
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
		if (rankedValues == null) {
			if (other.rankedValues != null)
				return false;
		} else if (!rankedValues.equals(other.rankedValues))
			return false;
		return true;
	}


	/**
	 * Determine if the high ranked classification result is a value known by the model that produced it. 
	 * Does this by checking the value against the {@link Classification#UndefinedLabelValue}.
	 * @param c may be null and false is returned in this case.
	 * @return true if the model produced a known label value (i.e. not the {@link Classification#UndefinedLabelValue} value).
	 */
	public static boolean isKnown(Classification c) {
		return c != null && !Classification.UndefinedLabelValue.equals(c.getLabelValue());
	}


	public boolean isKnown() {
		return Classification.isKnown(this);
	}
}
