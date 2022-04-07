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
package org.eng.aisp.segmented;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eng.aisp.ILabeledDataWindow;
import org.eng.validators.annotation.Alphanumeric;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Initial implementation of a labeled data window that has labeled and tagged segments defined over it.
 * Each segment is defined a start and stop offset time and a set of optional labels and tags.
 * Each segment is represented by an instance of LabeledSegmentSpec.  
 * A list of such segments is provided. 
 * <p>
 * Note that this class is a container for the segments and the labeled window and does not itself implement ILabeledDataWindow.  
 * This is designed this way to help avoid unintentionally using (i.e. training on) the whole segment instead of segments that might be defined on it.
 * <p>
 * Sub-classes must implement the {@link #getSegmentedLabeledDataWindows()} to extract the segments from the larger labeled data window.
 * @author DavidWood
 *
 * @param <DATA>
 * @param <LDW>
 */
public abstract class AbstractSegmentedLabeledDataWindow<DATA, LDW extends ILabeledDataWindow<DATA>> implements ISegmentedLabeledDataWindow<DATA, LDW> {

	public final static String WINDOW_FIELD_NAME = "labeledWindow";
	public final static String SEGMENT_SPECS_FIELD_NAME = "segmentSpecs";
	
	private static final long serialVersionUID = 2842158658602211916L;
	@Alphanumeric
	private final List<LabeledSegmentSpec> segmentSpecs = new ArrayList<LabeledSegmentSpec>();
	protected final LDW labeledWindow;

	public AbstractSegmentedLabeledDataWindow(LDW labeledWindow, Collection<LabeledSegmentSpec> segmentSpecs) {
		this.labeledWindow = labeledWindow;
		if (segmentSpecs != null)
			this.segmentSpecs.addAll(segmentSpecs);
	}

	// For Jackson/Gson
	protected AbstractSegmentedLabeledDataWindow() {
		this.labeledWindow = null;
	}

	/**
	 * Copy constructor that replaces the labeled segments.
	 * @param sldw
	 * @param segmentSpecs
	 */
	public AbstractSegmentedLabeledDataWindow(AbstractSegmentedLabeledDataWindow<DATA,LDW> sldw, Collection<LabeledSegmentSpec> segmentSpecs) {
		this(sldw.labeledWindow, segmentSpecs);
	}

	@Override
	public LDW getEntireLabeledDataWindow() {
		return this.labeledWindow;
	}

	@Override
	public List<LabeledSegmentSpec> getSegmentSpecs() {
		return segmentSpecs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((labeledWindow == null) ? 0 : labeledWindow.hashCode());
		result = prime * result + ((segmentSpecs == null) ? 0 : segmentSpecs.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AbstractSegmentedLabeledDataWindow))
			return false;
		AbstractSegmentedLabeledDataWindow other = (AbstractSegmentedLabeledDataWindow) obj;
		if (labeledWindow == null) {
			if (other.labeledWindow != null)
				return false;
		} else if (!labeledWindow.equals(other.labeledWindow))
			return false;
		if (segmentSpecs == null) {
			if (other.segmentSpecs != null)
				return false;
		} else if (!segmentSpecs.equals(other.segmentSpecs))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final int maxLen = 8;
		return "AbstractSegmentedLabeledDataWindow [segmentSpecs="
				+ (segmentSpecs != null ? segmentSpecs.subList(0, Math.min(segmentSpecs.size(), maxLen)) : null)
				+ ", labeledWindow=" + labeledWindow + "]";
	}

	@Override
	public String getTag(String tagName) {
		return labeledWindow.getTag(tagName);
	}

	@Override
	public void addTag(String tagName, String tagValue) {
		labeledWindow.addTag(tagName, tagValue);
	}

	@Override
	public void addTags(Properties tags) {
		labeledWindow.addTags(tags);
		
	}

	@Override
	public String removeTag(String tagName) {
		return labeledWindow.removeTag(tagName);
	}

	@Override
	public Map<String, String> getTags() {
		return labeledWindow.getTags();
	}

	@Override
	public void clearTags() {
		labeledWindow.clearTags();
	}

	@Override
	@JsonIgnore
	public Properties getTagsAsProperties() {
		return labeledWindow.getTagsAsProperties();
	}


}
