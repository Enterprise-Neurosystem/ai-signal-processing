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

import java.util.concurrent.atomic.AtomicInteger;

import org.eng.aisp.AISPException;
import org.eng.aisp.IDataWindow;
import org.eng.cache.IMultiKeyCache;

/**
 * Provides support for caching of data processing results.  
 * Subclasses must implement the processing method via {@link #applyImpl(IDataWindow)} and define the cache that is used
 * by implementing {@link #getCache()}.
 * Results are cached on two keys.  One is the instance id of the IDataWindow, the other is the hash code of this instance.
 * <b>Subclasses must be sure to properly implement {@link #hashCode()}. </b>
 * @author dawood
 *
 * @param <WINDOW>
 * @param <RESULT>
 */
public abstract class AbstractCachingWindowProcessor<WINDOW extends IDataWindow<?>, RESULT > extends GenericCachingDataProcessor
			implements IWindowProcessor<WINDOW, RESULT>  {
	
	private static final long serialVersionUID = 8235694905168662878L;
	private static AtomicInteger hits = new AtomicInteger();
	
	/**
	 * If caching is enabled, then  see if the item is in the cache before calling {@link #applyImpl(IDataWindow)}.
	 */
	@Override
	public final RESULT apply(WINDOW window) {
		RESULT f;
		
		if (USE_FEATURE_CACHE) {
			IMultiKeyCache cache = getCache();
			Object dataKey = window.getInstanceID();
//			Object dataKey = window.getData();	// use the whole array 

			f = (RESULT)cache.get(this, dataKey);
			if (f == null) {
//				AISPLogger.logger.info("Cache miss. processor:",  this,  ", window:",  window);
				f = applyImpl(window);
				if (f != null)
					cache.put(f, this, dataKey);
			} else {
//				int v = hits.getAndIncrement();
//				if (v % 1000 == 0)
//					AISPLogger.logger.info("Cache hit ", v, ". extractor:", this);
//				AISPLogger.logger.info("Cache hit. processor:", this, ", window:", window);
			}
		} else {
			f = applyImpl(window);
		}

		return (RESULT) f;
	}

	/**
	 * The actual implementation of the processing.  This is only
	 * called when the result was not found in the cache.  The return value
	 * will be cached for later use.
	 * @param window 
	 * @return null can be returned, but it will not be cached.
	 * @throws AISPException
	 */
	protected abstract RESULT applyImpl(WINDOW window);


}
