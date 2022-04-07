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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import org.eng.aisp.classifier.MemoryCachingLabeledFeatureIterable;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.pipeline.FeatureExtractionPipeline;
import org.eng.aisp.feature.pipeline.LabeledFeatureIterable;
import org.eng.cache.MemoryCache;
import org.eng.util.CachingIterable;

/**
 * Helper class to allow us to dynamically load classes that may or may not be on the class path.
 * This is to help use migrate to the AI product which does not want to include some code, that is, Mongo, Spark, etc.
 * <p>
 * TODO: This class could/should eventually be moved into the client side project.  Be aware, it would need to pull some other classes for labeled feature extraction.
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

	/**
	 * Get the active AISP runtime that can be used to alter behavior per methods implemented here.
	 * @return never null.
	 */
	public static AISPRuntime getRuntime() { return runtime; }
	
	/**
	 * Get a disk caching iterable over the labeled features if a disk cache implementation is on the class path.
	 * @param lfg the iterable over which we are caching.
	 * @param memCache optional memory cache to back the disk cache. may be null.
	 * @return null if we could not load the class to help us to caching.
	 * @throws RuntimeException if class found, but does not have the expected constructors.
	 */
	protected <FDATA> Iterable<ILabeledFeatureGram<FDATA>[]> getDiskCachingIterable(Iterable<ILabeledFeatureGram<FDATA>[]> lfg, MemoryCache<?,?> memCache) {
		Class<?> klass; 
		String className = "org.eng.cache.mongo.MongoCachingLabeledFeatureIterable";
		try {
			klass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
		Constructor constructor;
		try {
			constructor = klass.getConstructor(Iterable.class, MemoryCache.class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Found class " + klass.getName() 
				+ ", but could not find the expected constructor taking the following argument types"
				+ Iterable.class.getName() + " and " + MemoryCache.class.getName(), e);
		}

		Object newObj;
		try {
			newObj = constructor.newInstance(lfg, memCache);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException("Found constructor " + constructor.getName() 
				+ ", but could not invoke to create new instance", e); 
		}
		if (newObj instanceof CachingIterable)
			return  (CachingIterable)newObj;

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
		return new FeatureExtractionPipeline<WINDATA,FDATA>(featureGramDescriptors);

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
