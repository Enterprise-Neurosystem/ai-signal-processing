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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.eng.util.TaggedEntity;

/**
 * Based class for all classifiers.
 * Subclasses must implement at least IFixedClassififer, but may implement its subclasses as needed.
 * @author dawood
 *
 * @param <WINDATA> return value of {@link org.eng.aisp.IDataWindow#getData(int)} of the data provided 
 * to {@link #classify(org.eng.aisp.IDataWindow)} (often, <code>double[]</code>).
 */
public abstract class AbstractClassifier<WINDATA> extends TaggedEntity implements IFixedClassifier<WINDATA>  {

	private static final long serialVersionUID = -7428250233527677017L;
	
	/**
	 * Recommended default sub window size of subwindows on which features are extracted. 
	 * Value is {@value #DEFAULT_WINDOW_SIZE_MSEC}.
	 */
	public final static int DEFAULT_WINDOW_SIZE_MSEC = 100;
	/**
 	 * Recommended default window shift size when extracting subwindows for feature extraction. 0 should indicate rolling windows. 
	 * Value is {@value #DEFAULT_WINDOW_SHIFT_MSEC}.
	 */
	public final static int DEFAULT_WINDOW_SHIFT_MSEC = 50;
	
	public AbstractClassifier() {
		super();
	}

	public AbstractClassifier(Map<String, String> tags) {
		super(tags);
	}

	public AbstractClassifier(Properties p) {
		super(p);
	}
	
//	@Override
//	public String showModel() {
//		return toString();
//	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return this.getClass().getSimpleName() + " [tags=" + (tags != null ? toString(tags.entrySet(), maxLen) : null) + "]";
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

}
