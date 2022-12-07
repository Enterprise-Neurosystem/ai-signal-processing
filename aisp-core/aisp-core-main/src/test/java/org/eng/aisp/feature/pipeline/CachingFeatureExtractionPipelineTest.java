package org.eng.aisp.feature.pipeline;

import java.util.List;

import org.eng.aisp.feature.IFeatureGramDescriptor;

public class CachingFeatureExtractionPipelineTest extends FeatureExtractionPipelineTest {

	/**
	 * Get the pipeline over which the tests should be run.
	 * This allows sub-classes that define implementation-specific tests to redefine this to use their implementation. 
	 * @param fgList
	 * @return
	 */
	protected FeatureExtractionPipeline<double[],double[]> getTestPipeline(List<IFeatureGramDescriptor<double[],double[]>> fgList) {
		return new CachingFeatureExtractionPipeline<double[],double[]>(fgList);
	}
	
	protected boolean isFeatureExtractionCached() {
		return true;
	}

}
