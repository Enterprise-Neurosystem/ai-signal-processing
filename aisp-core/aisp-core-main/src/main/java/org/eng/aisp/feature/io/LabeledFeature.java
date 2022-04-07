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
package org.eng.aisp.feature.io;

import java.util.Properties;

import org.eng.aisp.feature.IFeature;
import org.eng.util.TaggedEntity;

/**
 * Implements a generic ILabeledFeature.
 * 
 * @author dawood
 * @param <FDATA>
 */
public class LabeledFeature<FDATA> extends TaggedEntity implements ILabeledFeature<FDATA> {
	private static final long serialVersionUID = -8848289285946225185L;

	private IFeature<FDATA> features;
	private Properties labels;
	
	public LabeledFeature(IFeature<FDATA> features, Properties labels) {
		this.features = features;
		if (labels == null)
			labels = new Properties();
		this.labels = labels;
	}
	
	@Override
	public IFeature<FDATA> getFeature() {
		return features;
	}

	@Override
	public Properties getLabels() {
		return labels;
	}


}
