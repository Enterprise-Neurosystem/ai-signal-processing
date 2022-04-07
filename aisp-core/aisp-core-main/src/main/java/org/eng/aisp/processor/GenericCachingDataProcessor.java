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
package org.eng.aisp.processor;

import java.io.Serializable;

import org.eng.aisp.AISPProperties;
import org.eng.cache.Cache;
import org.eng.cache.IMultiKeyCache;

/**
 * Provides a simple framework for cachine results.
 * @author dawood
 *
 */
public class GenericCachingDataProcessor implements Serializable {

	private static final long serialVersionUID = 448726219345590329L;
	// We don't cache at the subwindow level anymore and use FeatureExtractionPipeline at the window level instead. dawood-10/13/2017
	public static boolean USE_FEATURE_CACHE = AISPProperties.instance().getProperty("data.processor.cache.enabled", false);
	private static final IMultiKeyCache CACHE = Cache.newManagedMemoryCache();
	
	public GenericCachingDataProcessor() {
	}


	/**
	 * Get the cache implementation that should be used by this instance
	 * @return never null if cache is not activated.
	 */
	public IMultiKeyCache getCache() {
		return CACHE; 
	}

}
