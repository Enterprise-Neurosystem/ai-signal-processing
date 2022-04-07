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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.eng.aisp.IDataWindow;
import org.eng.aisp.feature.FeatureGram;
import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.util.ExecutorUtil;

/**
 * Helper class to break data windows into sub-windows and extract features on the sub-windows and optionally apply a feature processor..
 * If windowSizeMsec <= 0, no subwindowing is performed and features are extracted on the entire IDataWindow
 * <p> 
 * This is a helper class to implement the full feature extraction pipeline.  Users should likely
 * use an instance of FeatureExtractionPipeline instead of this class.
 *
 * @author dawood
 * @param <WINDATA> the generic type
 * @param <FDATA> the generic type
 */
class FeatureGramExtractor<WINDATA, FDATA> implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -7371006264457526311L;
	
	private final IFeatureGramDescriptor<WINDATA,FDATA> fgDescriptor;

/** The Constant Cores. */
//	private final static ExecutorService executor = Executors.newCachedThreadPool(new HigherPriorityDaemonThreadFactory());
	private final static int Cores = Runtime.getRuntime().availableProcessors();
//	private final static int Cores = 1; 
/** The Constant executor. */
//	private final static ExecutorService executor = Executors.newFixedThreadPool(2*Cores, new HigherPriorityDaemonThreadFactory());
//	private final static ExecutorService executor = ExecutorUtil.getPrioritizingSharedService(); 
	private final static ExecutorService executor = ExecutorUtil.newFixedSizeService(Cores * 2, true);

/** The Constant PARALLEL_FEATURE_EXTRACTION_THRESHOLD. */
//	private final static Semaphore ThreadLimiter = new Semaphore(2*Cores);
	private static final int PARALLEL_FEATURE_EXTRACTION_THRESHOLD = 2 * Cores;	// 2 is a WAG
	
	/**
	 * Instantiates a new sub feature extractor 7.
	 * @param windowSizeMsec the size of the subwindows into which each labeled data window is divided into.  Set to 0 to not subdivide the labeled data windows.
	 * @param windowShiftMsec the amount of shift in time between subsequent subwindows.  Typical values are {@link #windowSizeMsec} or {@link #windowSizeMsec}/2.
	 * The former gives pure rolling windows, the latter a sliding window in which each subwindow overlaps with half the previous.  Use 0 to set to rolling windows.
	 * @param extractor the extractor
	 * @param processor optional feature gram processor.
	 */
	public FeatureGramExtractor(int windowSizeMsec, int windowShiftMsec, IFeatureExtractor<WINDATA,FDATA> extractor, IFeatureProcessor<FDATA> processor) {
		this(new FeatureGramDescriptor<WINDATA,FDATA>(windowSizeMsec, windowShiftMsec, extractor, processor));

	}

	public FeatureGramExtractor(IFeatureGramDescriptor<WINDATA, FDATA> fgd) {
		this.fgDescriptor = fgd;
	}

	public IFeatureGramDescriptor<WINDATA,FDATA> getFeatureGramDescriptor() {
		return this.fgDescriptor;
	}

	/**
	 * Serial extract sub features.
	 *
	 * @param window the window
	 * @return the list
	 */
	protected List<IFeature<FDATA>> serialExtractSubFeatures(IDataWindow<WINDATA> window)  {
		double windowSizeMsec = fgDescriptor.getWindowSizeMsec();
		double windowShiftMsec = fgDescriptor.getWindowShiftMsec();
		IFeatureExtractor<WINDATA,FDATA> extractor = fgDescriptor.getFeatureExtractor();
		double windowEndMsec = window.getEndTimeMsec();
		double startMsec = window.getStartTimeMsec();
		double endMsec = startMsec + windowSizeMsec; 
		List<IFeature<FDATA>> subFeatures = new ArrayList<IFeature<FDATA>>();
		while (endMsec <= windowEndMsec) {
			IDataWindow<WINDATA> subwin;
			subwin = window.subWindow(startMsec, endMsec);
			if (subwin != null) {
				IFeature<FDATA> r = extractor.apply(subwin);
				subFeatures.add(r);
			}
			startMsec += windowShiftMsec;
			endMsec += windowShiftMsec;
		}
		return subFeatures;
	}

	/**
	 * Extract feature gram.
	 *
	 * @param window the window
	 * @return the i feature gram
	 */
	public  IFeatureGram<FDATA> extractFeatureGram(IDataWindow<WINDATA> window) {
		List<IFeature<FDATA>> flist = this.extractSubFeatures(window);
		@SuppressWarnings("unchecked")
		IFeature<FDATA>[] farray = new IFeature[flist.size()]; 
		flist.toArray(farray);
		IFeatureGram<FDATA> fg = new FeatureGram<FDATA>(farray);
		IFeatureProcessor<FDATA> processor = this.fgDescriptor.getFeatureProcessor();
		if (processor != null)
			fg = processor.apply(fg);
		return fg;
	}
	


	/**
	 * The Class SubWindowTask.
	 *
	 * @param <WINDATA> the generic type
	 * @param <FDATA> the generic type
	 */
	private static class SubWindowTask<WINDATA,FDATA>  implements Callable<Object> {

		/** The mapped sub features. */
		private final Map<Double, IFeature<FDATA>> mappedSubFeatures;
		
		/** The window. */
		private final IDataWindow<WINDATA> window;
		
		/** The extractor. */
		private final IFeatureExtractor<WINDATA, FDATA> extractor;
		
		/** The start time iterator. */
		private final Iterator<Double> startTimeIterator;
		
		/** The window size msec. */
		private final double windowSizeMsec;

		/**
		 * Instantiates a new sub window task.
		 *
		 * @param window the window
		 * @param extractor the extractor
		 * @param windowSizeMsec the window size msec
		 * @param startTimeIterator the start time iterator
		 * @param mappedSubFeatures the mapped sub features
		 */
		public SubWindowTask(IDataWindow<WINDATA> window, IFeatureExtractor<WINDATA, FDATA> extractor,
				double windowSizeMsec, Iterator<Double> startTimeIterator,
				Map<Double, IFeature<FDATA>> mappedSubFeatures) {
			this.window = window;
			this.windowSizeMsec = windowSizeMsec;
			this.extractor = extractor;
			this.startTimeIterator = startTimeIterator;
			this.mappedSubFeatures = mappedSubFeatures;
		}

		/* (non-Javadoc)
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public Object call() throws Exception {
			while (true) {
				Double startTime;
				synchronized (startTimeIterator) {
					if (!startTimeIterator.hasNext())
						return null;;
					startTime = startTimeIterator.next();
				}
				IFeature<FDATA> r = null; 
				IDataWindow<WINDATA> subwin = window.subWindow(startTime, startTime + windowSizeMsec);
				if (subwin != null)  {
					r = extractor.apply(subwin);
					if (r != null) {
						synchronized(mappedSubFeatures) {
							mappedSubFeatures.put(startTime,  r);
						}
					}
				}
			}
		}
		
	}

	/**
	 * Parallel extract sub features.
	 *
	 * @param window the window
	 * @return the list
	 */
	protected List<IFeature<FDATA>> parallelExtractSubFeatures(IDataWindow<WINDATA> window) {
		double windowSizeMsec = fgDescriptor.getWindowSizeMsec();
		double windowShiftMsec = fgDescriptor.getWindowShiftMsec();
		if (windowShiftMsec <= 0)
			windowShiftMsec = windowSizeMsec;
		IFeatureExtractor<WINDATA,FDATA> extractor = fgDescriptor.getFeatureExtractor();
		double windowEndMsec = window.getEndTimeMsec();
		double startMsec = window.getStartTimeMsec();
		double endMsec = startMsec + windowSizeMsec; 
		List<Double> startTimes = new ArrayList<Double>(); 
		
		// Create the list of start times of the subwindows for which subfeatures will be computed.
		while (endMsec <= windowEndMsec) {
			startTimes.add(startMsec);
			startMsec += windowShiftMsec;
			endMsec += windowShiftMsec;
		}

		// In parallel, compute the subwindows for each start time and extract the features from those windows.
		// We put them in a map keyed by start time so that we can later sort them by start time into the returned list
		// of features.
		int nThreads = Runtime.getRuntime().availableProcessors();
		Map<Double, IFeature<FDATA>> mappedSubFeatures = new Hashtable<Double,IFeature<FDATA>>();
		Iterator<Double> startTimeIterator = startTimes.iterator();
		List<Future<Object>> futures = new ArrayList<Future<Object>>();
		for (int i=0 ; i<nThreads ; i++) {
			SubWindowTask<WINDATA,FDATA> task = new SubWindowTask<WINDATA,FDATA>(window, extractor, windowSizeMsec, startTimeIterator, mappedSubFeatures);
			Future<Object> f  = executor.submit(task);
			futures.add(f);
		}

		// Retrieve the futures 
		for (Future<Object> f : futures) {
			try {
				f.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Get the computed subfeatures in time order.
		List<IFeature<FDATA>> subFeatures = new ArrayList<IFeature<FDATA>>();
		for (Double startTime : startTimes) {
			IFeature<FDATA> f = mappedSubFeatures.get(startTime);
			if (f != null)
				subFeatures.add(f);
		}

		return subFeatures;

	}


		
    /**
	 * Extract 1 or more features on 1 or more subwindows from the given data window.
	 * Compute subwindows of the given window according to {@link #windowSizeMsec} and {@link #windowShiftMsec}
	 * and apply the feature extractor to each them.  Place the time-ordered  list of sub features into the
	 * given subFeatures list. 
	 *
	 * @param window the window
 	 * @return the list
	 */
	private  List<IFeature<FDATA>> extractSubFeatures(IDataWindow<WINDATA> window) {
		List<IFeature<FDATA>> subFeatures = getSubFeatures(window);
		return subFeatures;
	}

	/**
	 * This is the work horse that all feature extraction goes through.
	 * It decides whether or not to go parallel in doing the extraction.
	 *
	 * @param window the window
	 * @return the sub features
	 */
	private List<IFeature<FDATA>> getSubFeatures(IDataWindow<WINDATA> window) {
		List<IFeature<FDATA>> lfList;
		double windowSizeMsec = fgDescriptor.getWindowSizeMsec();
	    if  (windowSizeMsec <= 0) { 
			IFeatureExtractor<WINDATA,FDATA> extractor = fgDescriptor.getFeatureExtractor();
	    	lfList = new ArrayList<IFeature<FDATA>>();
			IFeature<FDATA> r = extractor.apply(window);
			lfList.add(r);
	    } else  {
	    	double windowShiftMsec = fgDescriptor.getWindowShiftMsec();
			double startMsec = window.getStartTimeMsec();
			double endMsec = window.getEndTimeMsec(); 
			double subWindowsCount = (endMsec - startMsec) / windowShiftMsec;
			if (Cores != 1 && subWindowsCount > PARALLEL_FEATURE_EXTRACTION_THRESHOLD ) {	
				lfList = parallelExtractSubFeatures(window );
			} else {
				lfList = serialExtractSubFeatures(window);
			}
	    }
		return lfList;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fgDescriptor == null) ? 0 : fgDescriptor.hashCode());
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof FeatureGramExtractor))
			return false;
		FeatureGramExtractor other = (FeatureGramExtractor) obj;
		if (fgDescriptor == null) {
			if (other.fgDescriptor != null)
				return false;
		} else if (!fgDescriptor.equals(other.fgDescriptor))
			return false;
		return true;
	}

}
