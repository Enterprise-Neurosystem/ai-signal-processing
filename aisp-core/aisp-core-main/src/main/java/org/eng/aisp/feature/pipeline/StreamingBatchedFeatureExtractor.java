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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.eng.aisp.AISPProperties;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.pipeline.FeatureExtractionPipeline;
import org.eng.util.ExecutorUtil;

class StreamingBatchedFeatureExtractor<WINDATA, FDATA> implements IBatchedFeatureExtractor<WINDATA, FDATA> {

	/** Defines the number of threads for feature extraction in newFixedSizeService.  */ 
	public final static String NUM_BATCHED_THREADS_PROPERTY_NAME = "feature.extraction.batched.threads";
	
	/**
	 * The default allows some parallelism, but not so much that we break the thread stack and get OOM per Inoue-san's experience with 35 hours of training 
	 * data on Power 9 with 160 cores (circa 6/2020).  Spectrogram computation still goes parallel so we should still get a large CPU utilization.
	 * The value of 16 was chosen based on Inoue-san's runs on both Power 9 and Intel.  Seems a good compromise.
	 */
	public final static int DEFAULT_NUMBER_OF_BATCHED_THREADS = 16;

	/** This is the property Inoue-san uses, keep support for it so he doesn't have to change all his scripts */ 
	private final static String INOUE_FEATURE_EXTRACTION_NUMBER_OF_THREADS = "feature.extraction.number_of_threads";

	private final static int INOUE_NUMBER_OF_THREADS = AISPProperties.instance().getProperty(INOUE_FEATURE_EXTRACTION_NUMBER_OF_THREADS, DEFAULT_NUMBER_OF_BATCHED_THREADS);

	private final static int NUM_BATCHED_THREADS = Math.max(1, AISPProperties.instance().getProperty(NUM_BATCHED_THREADS_PROPERTY_NAME, INOUE_NUMBER_OF_THREADS));



	public StreamingBatchedFeatureExtractor() {
		super();
	}

	/**
	 * Implement to maintain the order of the extracted features to be in the same order as the input.
	 */
	@Override
	public Iterable<? extends ILabeledFeatureGram<FDATA>[]> nextBatch(List<? extends ILabeledDataWindow<WINDATA>> data,
			FeatureExtractionPipeline<WINDATA, FDATA> featurePipeline) {
		

		// TODO: We use Futures instead of a parallelStream to avoid what seems to be a bug in streams on IBM's Java 8 JRE. -dawood 10/30/2017
		// The bug/hand was only seen when training a MultiClassifier inside tomcat 8 on Ubuntu.
		// See https://github.ibm.com/IoT-Sound/iot-sound/issues/221
//		ExecutorService eservice = ExecutorUtil.getSharedService();
//		ExecutorService eservice = ExecutorUtil.newService(true);
		ExecutorService eservice = ExecutorUtil.newFixedSizeService(NUM_BATCHED_THREADS, true);

		List<Future<Object>> flist = new ArrayList<Future<Object>>();
		List<BatchItemTask<WINDATA, FDATA>> batchList = new ArrayList<BatchItemTask<WINDATA,FDATA>>();
		for (ILabeledDataWindow<WINDATA> ldw : data) {
			if (ldw != null) {
				BatchItemTask<WINDATA, FDATA> task = new BatchItemTask<WINDATA, FDATA>(ldw, featurePipeline);
				Future<Object> f = eservice.submit(task);
				flist.add(f);
				batchList.add(task);
			}
		}
		

		// Wait for completion of all tasks.
		for (Future<Object> f : flist) {
			try {
				f.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

		// Pull the results out in order of the input.
		List<ILabeledFeatureGram<FDATA>[]> resultList = new ArrayList<ILabeledFeatureGram<FDATA>[]>();
		for (BatchItemTask<WINDATA,FDATA> task : batchList) {
			if (task.result != null)
				resultList.add(task.result);
		}
		eservice.shutdown();	
		
		if (resultList.size() != batchList.size())
			resultList = null; 
		
		return resultList;
	}

	static class BatchItemTask<WINDATA, FDATA> implements Callable<Object> {

		private ILabeledDataWindow<WINDATA> labeledDataWindow;
		private FeatureExtractionPipeline<WINDATA, FDATA> featurePipeline;
		private ILabeledFeatureGram<FDATA>[] result;

		public BatchItemTask(ILabeledDataWindow<WINDATA> ldw, FeatureExtractionPipeline<WINDATA, FDATA> featurePipeline) {
			this.labeledDataWindow = ldw;
			this.featurePipeline = featurePipeline;
		}

		@Override
		public Object call() throws Exception {
			result = featurePipeline.extract(labeledDataWindow);
			return null;
		}

	}
}
