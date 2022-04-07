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

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.dataset.MetaData;

/**
 * Represents a series of labeled time ranges.
 * @author dawood
 *
 */
public class LabelSequence implements Iterable<LabeledSegmentSpec> {

	private static String NAME_VALUE_PAIR_SEP = ";";
	private static MessageFormat MESSAGE_FORMAT = new MessageFormat("{0,number,#.##}, {1,number,#.##}, {2}, {3}");

	private final List<LabeledSegmentSpec> segmentList =  new ArrayList<LabeledSegmentSpec>(); 
	private final boolean mergeSegments;
	
	public LabelSequence(boolean mergeSegments) {
		this.mergeSegments = mergeSegments;
	}

	public void append(double startMsec, double endMsec, Properties labels, Properties tags) {
		int labelCount = this.segmentList.size();
		boolean merge = false;
		LabeledSegmentSpec ls = new LabeledSegmentSpec(startMsec, endMsec, labels, tags);
		if (mergeSegments) {
			LabeledSegmentSpec last = labelCount == 0 ? null : this.segmentList.get(labelCount - 1);
			merge = last != null && last.getEndMsec() == startMsec && propsEqual(labels, last.getLabels());
			if (merge)  {
				last.merge(ls);
				ls = null;		// Kill add below
			}
		}
		if (ls != null) 
			this.segmentList.add(ls);
	}
	
	private boolean propsEqual(Properties p1, Properties p2) {
		if (p1 == p2)
			return true;
		if (p1 == null) {
			if (p2 == null)
				return true;
			else if (p2.size() == 0)
				return true;
			else
				return false;
		} else if (p2 == null) {
			if (p1.size() == 0)
				return true;
		}
		return p1.equals(p2);
	}

	public void write(String fileName) throws IOException {
		try ( FileOutputStream fos = new FileOutputStream(fileName)) {
			
			Object[] data = new Object[4];
			for (LabeledSegmentSpec ls : segmentList) {
				String labels = MetaData.formatNameValuePairs(ls.getLabels(), NAME_VALUE_PAIR_SEP);
				String tags = MetaData.formatNameValuePairs(ls.getTags(), ";");
				data[0] = ls.getStartMsec()/1000.0;
				data[1] = ls.getEndMsec()/1000.0;
				data[2] = labels;
				data[3] = tags;
				String line = MESSAGE_FORMAT.format(data) + "\n"; 
				fos.write(line.getBytes());
			}
		}
	}
	
	public static LabelSequence read(String fileName) throws IOException {
		LabelSequence sequence = new LabelSequence(false);
		
		String line;
		try (Reader reader = new FileReader(fileName);
			BufferedReader buffered = new BufferedReader(reader)) {
			while ((line = buffered.readLine()) != null) {
				if (line.length() == 0 || line.startsWith("#") || line.startsWith("//"))
					continue;
				try {
					Object[] data = MESSAGE_FORMAT.parse(line);
					if (data == null || data.length != 4)
						throw new IOException("Format error");
					double startMsec = Double.parseDouble(data[0].toString()) * 1000;
					double endMsec = Double.parseDouble(data[1].toString()) * 1000;
					Properties labels = MetaData.parseNameValuePairs(data[2].toString(), NAME_VALUE_PAIR_SEP); 
					Properties tags = MetaData.parseNameValuePairs(data[3].toString(), NAME_VALUE_PAIR_SEP); 
					sequence.append(startMsec, endMsec, labels, tags);
				} catch (ParseException e) {
					throw new IOException("Format error: " + e.getMessage(), e);
				}
			}
		}
		
		return sequence;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return "LabelSequence [labels=" + (segmentList != null ? segmentList.subList(0, Math.min(segmentList.size(), maxLen)) : null)
				+ "]";
	}

	@Override
	public Iterator<LabeledSegmentSpec> iterator() {
		return this.segmentList.iterator();
	}

	public int size() {
		return segmentList.size();
	}
}
