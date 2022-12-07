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
package org.eng.aisp;

import java.util.Arrays;
import java.util.List;

import org.eng.aisp.classifier.MemoryCachingLabeledFeatureIterable;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.pipeline.CachingFeatureExtractionPipeline;
import org.eng.aisp.feature.pipeline.FeatureExtractionPipeline;
import org.eng.aisp.feature.pipeline.LabeledFeatureIterable;
import org.eng.cache.MemoryCache;

/**
 * Helper class to allow us to dynamically load classes that may optionally be on the class path.
 * @author dawood
 *
 */
public class AISPRuntime {
	
	/**
	 * Holds an optional name of a class extending AISPRuntime to be used for runtime methods.
	 */
	public final static String AISP_RUNTIME_CLASS_PROPERTY_NAME = "aisp.runtime.class";

	private static AISPRuntime runtime = null;
	
	static {
		String runtimeClass = AISPProperties.instance().getProperty(AISP_RUNTIME_CLASS_PROPERTY_NAME);
		Object obj = null;
		if (runtimeClass != null) {
			try {
				obj = Class.forName(runtimeClass);
				if (!(obj instanceof AISPRuntime))  {
					AISPLogger.logger.severe("Loaded runtime class, but it is not an instance of " + AISPRuntime.class.getName() + ". Ignoring.");
					obj = null;
				}
			} catch (ClassNotFoundException e) {
				AISPLogger.logger.severe("Could not load class " + runtimeClass + ". " + e.getMessage() + ". Ignoring."); 
			}
		}
		runtime = obj == null ? new AISPRuntime() : (AISPRuntime)obj;
	}

	protected AISPRuntime() {
		
	}
	/**
	 * Get the active AISP runtime that can be used to alter behavior per methods implemented here.
	 * @return never null.
	 */
	public static AISPRuntime getRuntime() { return runtime; }
	
	/**
	 * Get a disk caching iterable over the given iterable.  This implementation returns null, but extending run-times may override this to provide a disk caching implementation. 
	 * @param <FDATA>
	 * @param lfg
	 * @param memCache	the memory cache to back that the disk cache.
	 * @return null if not available.
	 */
	protected <FDATA> Iterable<ILabeledFeatureGram<FDATA>[]> getDiskCachingIterable(Iterable<ILabeledFeatureGram<FDATA>[]> lfg, MemoryCache<?,?> memCache) {
		return null;
		
	}


	private final static String FEATURE_CACHING_ENABLED_PROPERTY_NAME = "labeled.feature.gram.caching.enabled";
	private final static boolean FEATURE_CACHING_ENABLED_DEFAULT = false;
	private final static boolean LabeledFeatureGramCachingEnabled = AISPProperties.instance().getProperty(FEATURE_CACHING_ENABLED_PROPERTY_NAME, FEATURE_CACHING_ENABLED_DEFAULT);
	private static boolean WarnedAlready = false;

	/**
	 * A convenience on {@link #getFeatureExtractionPipeLine(List)}.
	 */
	public final <WINDATA, FDATA> FeatureExtractionPipeline<WINDATA, FDATA> getFeatureExtractionPipeline(IFeatureGramDescriptor<WINDATA, FDATA> featureGramDescriptor) {
		return this.getFeatureExtractionPipeline(Arrays.asList(featureGramDescriptor));
	}

	/**
	 * Get a feature gram extraction pipeline that produces 1 feature gram for each descriptor provided.
	 * @param <WINDATA>	type of data in the window from which features are extracted.  For example, double[].
	 * @param <FDATA>	type of data produced in the the resulting features.  For example, double[].
	 * @param featureGramDescriptor
	 * @return never null.
	 */
	public <WINDATA, FDATA> FeatureExtractionPipeline<WINDATA, FDATA> getFeatureExtractionPipeline(List<IFeatureGramDescriptor<WINDATA, FDATA>> featureGramDescriptors) {
//		return new FeatureExtractionPipeline<WINDATA,FDATA>(featureGramDescriptors);
		return new CachingFeatureExtractionPipeline<WINDATA,FDATA>(featureGramDescriptors);

	}


	public <FDATA> Iterable<ILabeledFeatureGram<FDATA>[]> getCachingFeatureGramIterable( Iterable<ILabeledFeatureGram<FDATA>[]> fi, boolean useDiskCache, boolean useMemoryCache) {

		if (!LabeledFeatureGramCachingEnabled) {
			if ((useDiskCache || useMemoryCache) && !WarnedAlready)  {
				WarnedAlready = true;
				AISPLogger.logger.info("Memory and/or disk caching of labeled feature/spectograms has been requested, but is ignored because caching has been disabled with the"
					+ " AISP/System property " +  FEATURE_CACHING_ENABLED_PROPERTY_NAME + ".  To stop this message, set this property to true to enable caching,  or change"
					+ " your feature gram extraction to not request caching.");
			}
			
			return fi;
		}

		boolean streamed =  !(fi instanceof LabeledFeatureIterable) || ((LabeledFeatureIterable)fi).isStreamed(); 
		if (streamed) {
			if (useDiskCache) {
				Iterable<ILabeledFeatureGram<FDATA>[]> diskCachedFI; 
				if (useMemoryCache) 	
					diskCachedFI = getDiskCachingIterable(fi, new MemoryCache()); 
				else 
					diskCachedFI = getDiskCachingIterable(fi, null); 
				if (diskCachedFI == null) { 	// Runtime may not support a disk cache
					if (useMemoryCache) {
						AISPLogger.logger.warning("Disk cache not supported in this runtime.  Using a memory cache only."); 
						fi = new MemoryCachingLabeledFeatureIterable<FDATA>(fi); 
					} else {
						// Use current value of fi
						AISPLogger.logger.warning("Disk cache not supported in this runtime. No cache will be used.");
					}
				} else {
					fi = diskCachedFI;
				}
			} else if (useMemoryCache)  {
				fi = new MemoryCachingLabeledFeatureIterable<FDATA>(fi); 
			}
		} else if (useDiskCache || useMemoryCache) {
			AISPLogger.logger.info("Memory and/or disk caching of labeled feature/spectograms has been requested, but is being ignored because the "
					+ fi.getClass().getSimpleName() + " is not using streaming (i.e. maintains its own hard references) and so caching is not required."); 
		}
		return fi;
	}


	
}
