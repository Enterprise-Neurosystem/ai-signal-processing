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
package org.eng.aisp.feature.processor;

import java.util.Arrays;

import org.eng.aisp.feature.IFeatureGram;

/**
 * Enables the sequential processing of a 1 or more processors with their outputs connected to their inputs.
 * @author dawood
 *
 * @param <FDATA>
 */
public class GenericPipelinedFeatureProcessor<FDATA> extends AbstractCachingMultiFeatureProcessor<FDATA> implements IFeatureProcessor<FDATA> {
	
	private static final long serialVersionUID = -4579724471418840640L;
	private final IFeatureProcessor<FDATA>[] featureProcessors;

	/**
	 * 
	 * @param processors
	 * @param weights a list of integers 0 or larger.  May be null, in which case equal weights are assumed.
	 * @param summarizeFeatureArrays
	 */
	@SafeVarargs
	public GenericPipelinedFeatureProcessor(IFeatureProcessor<FDATA>... processors) {
		this.featureProcessors = processors;
	}

	@Override
	protected IFeatureGram<FDATA> applyImpl(IFeatureGram<FDATA> featureGram) {
		
		for (IFeatureProcessor<FDATA> processor : featureProcessors) {
			featureGram = processor.apply(featureGram);
		}
		return featureGram;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return "GenericPipelinedFeatureProcessor [featureProcessors=" + (featureProcessors != null
				? Arrays.asList(featureProcessors).subList(0, Math.min(featureProcessors.length, maxLen)) : null) + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(featureProcessors);
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
		if (!(obj instanceof GenericPipelinedFeatureProcessor))
			return false;
		GenericPipelinedFeatureProcessor other = (GenericPipelinedFeatureProcessor) obj;
		if (!Arrays.equals(featureProcessors, other.featureProcessors))
			return false;
		return true;
	}


}
