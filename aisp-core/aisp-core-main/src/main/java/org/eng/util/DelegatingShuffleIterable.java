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
package org.eng.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eng.cache.Cache;
import org.eng.cache.IMultiKeyCache;

/**
 * A shuffleable iterable that delegates the process for dereferencing single references to a separate implementation. 
 * 
 * @author DavidWood
 *
 */
public class DelegatingShuffleIterable<DATA> extends AbstractReferenceShuffleIterable<DATA, DelegatingShuffleIterable<DATA>> implements IShuffleIterable<DATA> {

//	protected IDoubleDataSet<?> dataSet;
	protected IMultiKeyCache cache;
	private IDereferencer<String, DATA> dereferencer;

	/**
	 * Simple class to implement {@link IDereferencer#loadReference(Object)} with an {@link IItemReferenceIterable#dereference(String)}. 
	 */
	private static class ShuffleDereferencer<DATA> implements IDereferencer<String, DATA> {

		private IItemReferenceIterable<DATA> itemRefIterable;

		public ShuffleDereferencer(IItemReferenceIterable<DATA> iterable) {
			this.itemRefIterable = iterable;
		}

		@Override
		public DATA loadReference(String reference) throws IOException {
			return this.itemRefIterable.dereference(reference);
		}
		
	}

	public DelegatingShuffleIterable(Iterable<String> references, IItemReferenceIterable<DATA> iterable) { 
		this(references, new ShuffleDereferencer<DATA>(iterable));
	}

	public DelegatingShuffleIterable(Iterable<String> references, IDereferencer<String, DATA> dereferencer) {
		this(references, dereferencer, null);
	}

	public DelegatingShuffleIterable(Iterable<String> references, IDereferencer<String, DATA> dereferencer, IMultiKeyCache cache) {
		super(references);
		this.dereferencer = dereferencer;
		if (cache == null)
			cache = Cache.newMemoryCache(); 
		this.cache = cache;
	}


	/**
	 * Implemented for the super class to extract a given data associated with a specific reference.
	 * The references are provided through the constructor. 
	 */
	@Override
	public DATA dereference(String reference) { //  file, MetaData dataSet, String soundName, Properties soundLabels) {
		DATA sr = (DATA) cache.get(reference);
		if (sr != null) 
			return sr;
		try {
			sr = this.dereferencer.loadReference(reference);
		} catch (IOException e) {
			throw new NoSuchElementException("Could not load item under reference " + reference + ". " + e.getMessage());
		}
		cache.put(sr, reference);
		return sr;

	}


	@Override
	public DelegatingShuffleIterable<DATA> newIterable(Iterable<String> references) {
		return new DelegatingShuffleIterable<DATA>(references, this.dereferencer, this.cache); 
	}


	@Override
	public Iterator<DATA> iterator() {
	    return new ItemReferenceIterator<DATA>(this, getReferences().iterator()); 
	}



}