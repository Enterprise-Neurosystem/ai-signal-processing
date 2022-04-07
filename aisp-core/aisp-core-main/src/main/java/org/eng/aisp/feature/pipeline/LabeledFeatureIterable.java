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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

import org.eng.aisp.AISPProperties;
import org.eng.aisp.AISPRuntime;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.dataset.ILabeledDataSet;
import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.util.ISizedIterable;

/**
 * An iterable over input training data to produce an iterator over resulting
 * labeled features. Each window in the contained iterable of ILabeledDataWindow
 * is operated on by a feature gram extractor to one or more feature/spectrograms for
 * each window.  A predicate can be used to limit the windows that are included
 * in producing the output feature gram arrays. 
 * <h3>Memory considerations</h3>
 * The instance is configurable to use streaming computation on batched groups of windows.
 * Both can be configured to impact memory requirements.
 * <h4> Streaming computation </h4>
 * Features can be computed in one of two ways:
 * <ol>
 * <li> Streamed - the output of the iterable/iterator is computed on demand as
 * elements of the iterator are accessed (using next()).  The references to these
 * outputs are not internally referenced so that they may be lost if a hard reference
 * is not maintained.  If feature computation is expensive, it could be  
 * expensive to have multiple iterations (if items are evicted from the feature cache by the GC, for example). 
 * <li> Non-streamed - all future outputs of the iterable/iterator are computed
 * on the first access and saved internally for subsequent iteration.  This means
 * that the items produces are hard-referenced and will not be lost or need to be
 * recomputed as long as the LabeledFeatureIterable is hard referenced. 
 * In this case, users should consider the total amount of memory required to 
 * hold all feature grams computed by the instance. As an example, 100 hours of
 * data at 44.1khz sampling rate, with a feature gram computed using 
 * 50 msec sub-windows with 50% overlap and 64 features/sub-window will generally require
 * about <code> (100 * 60 * 60 * 1000/50 * 2 * 64 * sizeof(double) / 1024^3 ) =  6.9 gb. </code>
 * </ol>
 * using streaming mode will have lower memory requirement relative to the non-streamed mode.
 * As such, if training data size is large relative to memory (and you are getting OOM exceptions), 
 * then you may need to use streaming mode. 
 * The {@link #LabeledFeatureIterable(Iterable, Predicate, List, boolean)} constructor allows configuration
 * of the streaming nature of the instance.  The other constructors generally use 
 * the {@value #FEATURE_STREAMING_ENABLED_PROPERTY_NAME} system property to define their behavior.
 * The default value for this property is {@value #DefaultFeatureStreamingEnabled}.
 * 
 * <h4> Batched computation</h4>
 * Each feature gram computed from a data window is computed as a part of a batch of data windows.
 * While the iterable of data windows may be streaming, each batch of data windows is pulled into
 * memory such that the memory must be able to accomodate the full batch of windows.  The batch
 * size is defined by a the {@link #BATCH_SIZE_PROPERTY_NAME} system property which defaults to
 * the number of available processors.   For given batch size of N with each data window with a
 * a sampling rate of S being L seconds long requires <code>2 * N * L * S * sizeof(double)</code>.
 * A factor of 2 is include due to sub-windowing which effectively copies the data window.  A factor
 * of 3 might used if using 50% sliding windows.
 * As an example then, For 5 second clips at 44.1khz sampling rate and a batch size of 8, with
 * 50% sliding windows this would be about this would be about 8.5 MB - a relatively small requirement.
 * 
 * @author dawood
 *
 * @param <WINDATA>
 * @param <FDATA>
 */
public class LabeledFeatureIterable<WINDATA, FDATA> implements Iterable<ILabeledFeatureGram<FDATA>[]>, ISizedIterable<ILabeledFeatureGram<FDATA>[]> {

	public final static String BATCH_SIZE_PROPERTY_NAME = "feature.iterable.batch_size";
	public final static int BATCH_SIZE_DEFAULT = 8 * Runtime.getRuntime().availableProcessors();
	
	/**
	 * The name of a property that when set to <code>true</code>, the default, causes all features computed for a training request to be hard referenced instead of soft referenced
	 * via a cache where they might be evicted during training, requiring (automatic) re-computation.   
	 * A value of <code>true</code> generally requires more heap memory during training than would be required if caching is used.
	 */
	public final static String FEATURE_STREAMING_ENABLED_PROPERTY_NAME = "feature.iterable.streaming.enabled";
	public final static boolean DefaultFeatureStreamingEnabled = false; 
	public final static boolean FeatureStreamingEnabledPropertyValue = AISPProperties.instance().getProperty(FEATURE_STREAMING_ENABLED_PROPERTY_NAME, DefaultFeatureStreamingEnabled);
	

	/**
	 * Controls the size of the batch of data windows on which feature grams are computed and processed. 
	 * The batch of data windows are all brought into memory during processing. 
	 * As such a larger batch size will require more memory.
	 */
	private final static int BatchSize = AISPProperties.instance().getProperty(BATCH_SIZE_PROPERTY_NAME, BATCH_SIZE_DEFAULT);

	private final Iterable<? extends ILabeledDataWindow<WINDATA>> data;
	private final FeatureExtractionPipeline<WINDATA,FDATA> featureExtractionPipeline;
	private Predicate<ILabeledDataWindow<WINDATA>> dataPredicate;
	private boolean useStreaming;
	private List<ILabeledFeatureGram<FDATA>[]> allFeatures = null;

	/**
	 * A convenience on {@link #LabeledFeatureIterable(Iterable, Predicate, List)} w/o a data predicate.
	 */
	public LabeledFeatureIterable(Iterable<? extends ILabeledDataWindow<WINDATA>> data,
			List<IFeatureGramDescriptor<WINDATA, FDATA>> featureGramExtractors) {
		this(data,null, featureGramExtractors);
	}

	/**
	 * A convenience on {@link #LabeledFeatureIterable(Iterable, Predicate, List)}. 
	 */
	public LabeledFeatureIterable(Iterable<? extends ILabeledDataWindow<WINDATA>> data,
			Predicate<ILabeledDataWindow<WINDATA>> dataPredicate,
			IFeatureGramDescriptor<WINDATA, FDATA> featureGramExtractor) {
		this(data, dataPredicate, Arrays.asList(new IFeatureGramDescriptor[] { featureGramExtractor}));
	}

	/**
	 * A convenience on {@link #LabeledFeatureIterable(Iterable, Predicate, List, boolean)} setting the
	 * useStreaming argument to that defined by {@value #FEATURE_STREAMING_ENABLED_PROPERTY_NAME} and its
	 * default {@value #DefaultFeatureStreamingEnabled}.
	 */
	public LabeledFeatureIterable(Iterable<? extends ILabeledDataWindow<WINDATA>> data,
			Predicate<ILabeledDataWindow<WINDATA>> dataPredicate,
			List<IFeatureGramDescriptor<WINDATA, FDATA>> featureGramExtractors) {
		this(data, dataPredicate, featureGramExtractors, FeatureStreamingEnabledPropertyValue);

	}

	/**
	 * 
	 * @param data	data from which feature grames are to be extracted
	 * @param dataPredicate optional predicate (null allowed) to determine whether or not the data from the
	 * given data iterable is included in the output of this instance.
	 * @param featureGramDescriptors the transform applied to the data windows to produce the feature grames.
	 * @param useStreaming controls the mode of coputation of the features - in bulk or streamed.  See class
	 * documentation for details.
	 */
	public LabeledFeatureIterable(Iterable<? extends ILabeledDataWindow<WINDATA>> data,
			Predicate<ILabeledDataWindow<WINDATA>> dataPredicate,
			List<IFeatureGramDescriptor<WINDATA, FDATA>> featureGramExtractors, boolean useStreaming) {
		this.data = data;
//		if (dataPredicate == null)
//			dataPredicate = new AllDataPredicate<WINDATA>();
		this.dataPredicate = dataPredicate;
		this.featureExtractionPipeline = AISPRuntime.getRuntime().getFeatureExtractionPipeline(featureGramExtractors);
		this.useStreaming = useStreaming;

	}

	
	/**
	 * Just wrap a list around the given feature gram extractor.
	 * @param fge
	 * @return
	 */
	private static <WINDATA,FDATA> List<IFeatureGramDescriptor<WINDATA,FDATA>> makeFGEList(IFeatureGramDescriptor<WINDATA,FDATA> fge) {
		List<IFeatureGramDescriptor<WINDATA,FDATA>> fgeList = new ArrayList<IFeatureGramDescriptor<WINDATA,FDATA>>();
		fgeList.add(fge); 
		return fgeList;
	}

	/**
	 * A convenience on {@link #LabeledFeatureIterable(Iterable, Predicate, List)} w/o a predicate.
	 * @deprecated in favor of {@link #LabeledFeatureIterable(Iterable, Predicate, List)}
	 */
	public LabeledFeatureIterable(Iterable<? extends ILabeledDataWindow<WINDATA>> data, 
			IFeatureExtractor<WINDATA, FDATA> featureExtractor, int windowSizeMsec, int windowShiftMsec,
			IFeatureProcessor<FDATA> featureProcessor) {
		this(data, null, makeFGEList(new FeatureGramDescriptor<WINDATA,FDATA>(windowSizeMsec, windowShiftMsec, featureExtractor, featureProcessor)));
	}
	


	@Override
	public Iterator<ILabeledFeatureGram<FDATA>[]> iterator() { 
	    Iterator<ILabeledFeatureGram<FDATA>[]> iterator; 
		if (!useStreaming) {
			initAllExtractedFeatures();
			iterator = allFeatures.iterator();
		} else {
			iterator = streamedIterator(); 
		}
		return iterator;
	}

	/**
	 *  go through our streamedIterator() and get all features into {@link #allFeatures} field.
	 */
	private synchronized void initAllExtractedFeatures() {
		Iterator<ILabeledFeatureGram<FDATA>[]> iterator;
		if (this.allFeatures == null) {
			iterator = streamedIterator(); 
			allFeatures = new ArrayList<ILabeledFeatureGram<FDATA>[]>();
			while (iterator.hasNext()) {
				ILabeledFeatureGram<FDATA>[] lfga = iterator.next();
				allFeatures.add(lfga);
			}
		}
	}

	private Iterator<ILabeledFeatureGram<FDATA>[]> streamedIterator() { 
		IBatchedFeatureExtractor<WINDATA, FDATA> batchedFeatureExtractor;
		batchedFeatureExtractor = new StreamingBatchedFeatureExtractor();
		return new LabeledFeatureIterator(this.data.iterator(), this.dataPredicate, batchedFeatureExtractor, this.featureExtractionPipeline, this.BatchSize);
	}

	@Override
	public int size() {
		// If data is SoundRecordingIterable and has size(), return the size; otherwise enumerate through all data and get size
		int ret;
		if (data instanceof ISizedIterable && this.dataPredicate == null) {
			ret = ((ISizedIterable<? extends ILabeledDataWindow<WINDATA>>) data).size();
		} else if (allFeatures != null) {
			return allFeatures.size();
		} else {
			int i = 0;
			for (ILabeledDataWindow<WINDATA> d : data) {
				if (dataPredicate == null || dataPredicate.test(d))
					i++;
			}
			ret = i;
		}
		return ret;
	}


	
	/**
	 * Return a list of all unique label values for the given label.
	 * @param labelName
	 * @return never null.
	 * @throws IOException 
	 */
	public List<String> getAllLabelValues(String labelName) throws IOException {
		List<String> values; 

		if (data instanceof ILabeledDataSet && this.dataPredicate == null) {
			// TODO: the predicate should be used here.
			values = ((ILabeledDataSet) data).getLabelValues(labelName);
		} else {
			values = new ArrayList<String>();
			Set<String> valueSet = new HashSet<String>();
			for (ILabeledDataWindow<WINDATA> d : data) {
				if (dataPredicate == null || dataPredicate.test(d)) {
					Properties labels = d.getLabels();
					String value = labels.getProperty(labelName);
					if (value != null)
						valueSet.add(value);
				}
			}
			values.addAll(valueSet);
		}
		return values;
	}
	
	public boolean isStreamed() {
		return this.useStreaming;
	}



}
