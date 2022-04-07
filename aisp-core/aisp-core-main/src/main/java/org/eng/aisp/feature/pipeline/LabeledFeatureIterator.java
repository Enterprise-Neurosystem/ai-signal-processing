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
package org.eng.aisp.feature.pipeline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.pipeline.FeatureExtractionPipeline;

/**
 * Package-local class to  support the iterative extraction of features from data windows.
 * @author dawood
 *
 * @param <WINDATA>
 * @param <FDATA>
 */
class LabeledFeatureIterator<WINDATA, FDATA> implements Iterator<ILabeledFeatureGram<FDATA>[]> {
	// private Iterable<? extends ILabeledDataWindow<WINDATA>> dataIterable;
	// private Iterator<? extends ILabeledDataWindow<WINDATA>> dataIterator;
	//
	// private IFeatureExtractor<WINDATA, FDATA> featureExtractor;
	//
	// private Iterable <? extends ILabeledFeature<FDATA>[]> nextBatchResult
	// = null;
	private Iterator <? extends ILabeledFeatureGram<FDATA>[]> nextBatchResultIt = null;
	private final IBatchedFeatureExtractor<WINDATA, FDATA> batchedFeatureExtractor;
	private final Iterator<? extends ILabeledDataWindow<WINDATA>> data;
	private final FeatureExtractionPipeline<WINDATA,FDATA> featureExtractionPipeline;
	private final int batchSize;
	private final Predicate<ILabeledDataWindow<WINDATA>> dataPredicate;


	/**
	 * 
	 * @param data data windows from which arrays of labeled features are to be extracted.
	 * @param dataPredicate	if not null, then must return true for a given data window to have it used to produce features from {@link #next()}.
	 * @param batchedFeatureExtractor the implementation that extracts labeled features from a batch of data windows.  This typically is 
	 * an implementation that can work in parallel on the feature extractions.
	 * @param fep the feature extraction pipeline to be applied to extract an array of features from a given data window.
	 * @param batchSize size of the group of data windows that have their features extracted in parallel using the batchedFeatureExtractor.
	 */
	public LabeledFeatureIterator(Iterator<? extends ILabeledDataWindow<WINDATA>> data, Predicate<? extends ILabeledDataWindow<WINDATA>> dataPredicate,
			IBatchedFeatureExtractor<WINDATA, FDATA> batchedFeatureExtractor, 
			FeatureExtractionPipeline<WINDATA,FDATA> fep, int batchSize) {
		if (data == null) 
			throw new IllegalArgumentException("data must not be null");
		if (batchedFeatureExtractor == null) 
			throw new IllegalArgumentException("batchedFeatureExtractor must not be null");
		if (fep == null) 
			throw new IllegalArgumentException("fep must not be null");
		if (batchSize <= 0)
			throw new IllegalArgumentException("batchSize (" + batchSize + ") must not be a positive integer.");
		this.data = data;
		this.dataPredicate = (Predicate<ILabeledDataWindow<WINDATA>>) dataPredicate;
		this.batchedFeatureExtractor = batchedFeatureExtractor; 
		this.featureExtractionPipeline = fep;
		this.batchSize = batchSize;
	}



	@Override
	public boolean hasNext() {
		boolean status;

		// Check the current batch and get a new one if the current one is exhausted
		status = nextBatchResultIt != null && nextBatchResultIt.hasNext();
		if (!status) { // Need to try the next batch.
			List<ILabeledDataWindow<WINDATA>> ldwList = new ArrayList<>();
			
			// TODO: we might want to parallelize this so we don't serialize on potential db/file system access.
			while ( ldwList.size()<batchSize && data.hasNext()) {
				ILabeledDataWindow<WINDATA> ldw = data.next();
				if (this.dataPredicate == null || this.dataPredicate.test(ldw)) {
//					System.out.println("ldw=" + ldw);
					ldwList.add(ldw);
				}
			}
			Iterable<? extends ILabeledFeatureGram<FDATA>[]> iterable = ldwList.size() > 0 ? batchedFeatureExtractor.nextBatch(ldwList, featureExtractionPipeline) : null;
			if (iterable == null) {
				status = false;
				nextBatchResultIt = null;
			} else {
				nextBatchResultIt = iterable.iterator();
				status = nextBatchResultIt.hasNext();
			}
		}

		return status;
	}

	@Override
	public ILabeledFeatureGram<FDATA>[] next() {
		if (nextBatchResultIt == null && !hasNext())
			throw new NoSuchElementException();

		ILabeledFeatureGram<FDATA>[] f = this.nextBatchResultIt.next();
		return f; 
	}

}
