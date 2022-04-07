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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Provides mutation of items in an IShuffleIterable by extending the super class and forcing the use of IShuffleIterable in the constructor.
 * Mutation is defined with an IShuffleMutator instance that indicates whether or not the the mutator produces more than 1 output for any of its inputs.
 * Mutation is applied in the {@link #dereference(String)} implementation, but in addition, the determine a full set of shufflable references, the constructor
 * will iterate and mutate all items to determine a unique reference for each OUTPUT.
 * @author dawood
 *
 * @param <INPUT> type of item iterated over to produce OUTPUT items.
 * @param <OUTPUT> type of item produced from an INPUT item.
 */
public class MutatingShuffleIterable<INPUT,OUTPUT> extends AbstractReferenceShuffleIterable<OUTPUT, ISizedShuffleIterable<OUTPUT>>  implements ISizedShuffleIterable<OUTPUT> {

	protected final IShuffleIterable<INPUT> iterable;
	protected final IShuffleMutator<INPUT, OUTPUT> mutator;

	private final static String SEPARATOR = "/";

	public static class ShuffleMutatorProxy<INPUT,OUTPUT> implements IShuffleMutator<INPUT,OUTPUT> {

		final IMutator<INPUT,OUTPUT> mutator;
		final boolean isUnary;
		/**
		 * @param mutator
		 * @param isUnary
		 */
		public ShuffleMutatorProxy(IMutator<INPUT, OUTPUT> mutator, boolean isUnary) {
			this.mutator = mutator;
			this.isUnary = isUnary;
		}
		@Override
		public List<OUTPUT> mutate(INPUT item) {
			return mutator.mutate(item);
		}

		@Override
		public boolean isUnaryMutator() {
			return isUnary;
		}
	}

	/**
	 * Create the instance to mutate the given iterable of INPUT to OUTPUT.
	 * If the given mutator is NOT unary, this will iterator and mutate all instances to determine a unique reference for each mutated OUTPUT. 
	 * @param iterable the INPUT data to transform to 0 or more OUTPUT values.
	 * @param mutator a stateless function to convert an INPUT to a list of OUTPUT.
	 */
	public MutatingShuffleIterable(IShuffleIterable<INPUT> iterable, IShuffleMutator<INPUT,OUTPUT> mutator) {
		this(iterable, buildReferences(iterable, mutator), mutator);
	}

	/**
	 * Convenience on {@link #MutatingShuffleIterable(IShuffleIterable, IShuffleMutator)} that wraps the given mutator and flag into
	 * an IShuffleMutator implementation.
	 */
	public MutatingShuffleIterable(IShuffleIterable<INPUT> iterable,IMutator<INPUT,OUTPUT> mutator, boolean isUnaryMutator) {
		this(iterable, new  ShuffleMutatorProxy<INPUT,OUTPUT>(mutator, isUnaryMutator));
	}

	private MutatingShuffleIterable(IShuffleIterable<INPUT> iterable, Iterable<String> references, IShuffleMutator<INPUT,OUTPUT> mutator) {
		super(references);
		this.iterable = iterable;
		this.mutator = mutator;
	}

	/**
	 * Get the set of references for the output we will generate.
	 * if the mutator is unary, we can use the references from the given iterable.
	 * If the mutator is NOT unary, we must iterate the items and mutate them and create a reference for each.
	 * @param iterable
	 * @param mutator
	 * @return
	 */
	private static <INPUT,OUTPUT> Iterable<String> buildReferences(IShuffleIterable<INPUT> iterable, IShuffleMutator<INPUT, OUTPUT> mutator) {
		if (mutator.isUnaryMutator()) 
			return iterable.getReferences();

		// For each output, define its reference as the source reference  + an index into the List of OUTPUT produced by the mutator.
		List<String> references = new ArrayList<String>();
		for (String ref : iterable.getReferences()) {
			INPUT item = iterable.dereference(ref);
			List<OUTPUT> outputs = mutator.mutate(item);
			if (outputs != null && outputs.size() >  0)
				for (int i=0 ; i<outputs.size(); i++) 
					references.add(ref + SEPARATOR + i);
		}
		return references;
	}

	/**
	 * If we 
	 */
	@Override
	public OUTPUT dereference(String reference) {
		INPUT item;
		int listIndex;
		if (mutator.isUnaryMutator())  {
			item = this.iterable.dereference(reference);
			listIndex = 0;
		} else {
			int index = reference.lastIndexOf(SEPARATOR);
			if (index < 0)
				return null;
			String iterRef = reference.substring(0, index);
			item = iterable.dereference(iterRef);
			listIndex = Integer.parseInt(reference.substring(index+SEPARATOR.length()));
		}
		List<OUTPUT> outputs = mutator.mutate(item);
		if (outputs == null)
			return null;
		int size = outputs.size();
		if (mutator.isUnaryMutator() && size > 1)	// Mutator produced more than 1 item even though it is declared as unary.
			throw new IllegalArgumentException(mutator.getClass().getName() + " declared itself as unary, but mutated an input into more than 1 output");
		if (listIndex >= size) 	// Mutate produced fewer items than expected.
			throw new IllegalArgumentException(mutator.getClass().getName() + " generated an inconsistent number of outputs across mutations of the same item"); 
		OUTPUT output = outputs.get(listIndex);
		return output;
	}

	@Override
	public ISizedShuffleIterable<OUTPUT> newIterable(Iterable<String> references) {
		return new MutatingShuffleIterable<INPUT,OUTPUT>(this.iterable, references, mutator);
	}

	/**
	 * Override to use a MutatingIterator, which skips over mutations that generate a null item.
	 */
	@Override
	public Iterator<OUTPUT> iterator() {
		// Our dereferencer may return null in the case where it does not pre-traverse the items.
		return new ItemReferenceIterator<OUTPUT>(this, getReferences().iterator(), 1, true);
	}	
}