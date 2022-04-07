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
package org.eng.aisp.feature.processor.vector;

import org.eng.aisp.feature.processor.GenericPipelinedFeatureProcessor;
import org.eng.aisp.feature.processor.IFeatureProcessor;

/**
 * Allows sequential/pipelined execution of IFeatureProcessor<double[]> instances.
 * Extends the super class to define double[] as the feature data.
 * @author dawood
 *
 */
public class PipelinedFeatureProcessor extends GenericPipelinedFeatureProcessor<double[]> {

	private static final long serialVersionUID = -5421832398343091596L;

	@SafeVarargs
	public PipelinedFeatureProcessor(IFeatureProcessor<double[]>...processors) {
		super(processors);
	}

}
