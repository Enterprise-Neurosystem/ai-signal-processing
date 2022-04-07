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

import java.util.concurrent.atomic.AtomicInteger;

import org.eng.aisp.AISPException;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.processor.GenericCachingDataProcessor;
import org.eng.cache.IMultiKeyCache;

/**
 * Provides support for caching of feature processing results.  
 * Subclasses must implement the processing method via  {@link #applyImpl(IFeature[])}. 
 * <b>Subclasses must be sure to properly implement {@link #hashCode()}. </b>
 * @author dawood
 *
 * @param <WINDOW>
 * @param <RESULT>
 */
public abstract class AbstractCachingMultiFeatureProcessor<FDATA> extends GenericCachingDataProcessor implements IFeatureProcessor<FDATA>  {
	
	private static final long serialVersionUID = 2789041634237464575L;

	private static AtomicInteger hits = new AtomicInteger(0);
	private static AtomicInteger misses = new AtomicInteger(0);

	/**
	 * If caching is enabled, then  see if the item is in the cache before calling {@link #applyImpl(IFeature)}.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public final IFeatureGram<FDATA> apply(IFeatureGram<FDATA> featureGram) {
		IFeatureGram<FDATA> f;
		
		if (USE_FEATURE_CACHE) {
			IMultiKeyCache cache = getCache();
			// TODO: only caching based on one of the feature data.  Seems to work though.
//			Object dataKey = features[0].getData();
			IFeature<FDATA>[] features = featureGram.getFeatures();
			Object dataKey = features[0].getInstanceID();
//			if (dataKey instanceof double[]) {
//				// Find the first non-constant valued vector of features and use it to hash on.
//				for (int i=1 ; i<features.length ;i++) {
//					if (!VectorUtils.isConstant((double[])dataKey))
//						break;
//					dataKey = features[i].getData();
//				}
//			}
			f = (IFeatureGram<FDATA>)cache.get(this, dataKey); 
			if (f == null) {
//				int v = misses.incrementAndGet();
//				if (v % 10 == 0)
//					AISPLogger.logger.info("Cache miss " + v + ". processor:",  this);
				f = applyImpl(featureGram);
				if (f != null)
					cache.put(f, this, dataKey);
			} else {
//				int v = hits.incrementAndGet();
//				if (v % 1000 == 0)
//					AISPLogger.logger.info("Cache hit ", v, ". processor:", this);
//				AISPLogger.logger.info("Cache hit. processor:", this);
			}
		} else {
			f = applyImpl(featureGram);

		}

		return f;
	}

//	List<BigInteger> primes = new ArrayList<BigInteger>();
	
//	/**
//	 * Get a hash code for all the hashed data across all features using {@link IFeature#hashData()}.
//	 * @param features
//	 * @return
//	 */
//	private long hashFeatures(IFeature<FDATA> features[]) {
//		long hash = 3;
//		for (int i=0 ; i<features.length ; i++) {
//			long hashedData = features[i].hashData();
//			BigInteger nextPrime;
//			if (primes.size() <= i) {
//				synchronized(primes) {
//					if (primes.size() <= i) {
//						BigInteger lastPrime;
//						if (primes.size() == 0)
//							lastPrime = new BigInteger("3");
//						else
//							lastPrime = primes.get(i - 1);
//						nextPrime = lastPrime.nextProbablePrime();
//						primes.add(nextPrime);
//					} else {
//						nextPrime = primes.get(i);
//					}
//				} 
//			} else {
//				nextPrime = primes.get(i);
//			}
//			hash += nextPrime.intValue() * hashedData; 
//		}
//		return hash;	
//	}

	/**
	 * The actual implementation of the processing.  
	 * This is only
	 * called when the result was not found in the cache.  The return value
	 * will be cached for later use.
	 * @param featureGram 
	 * @return a array of feature that need not be the same size as the input.  null can be returned, but it will not be cached.
	 * @throws AISPException
	 */
	protected abstract IFeatureGram<FDATA> applyImpl(IFeatureGram<FDATA> featureGram);


}
