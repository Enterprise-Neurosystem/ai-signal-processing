package org.eng.aisp.feature.pipeline;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.eng.aisp.AISPLogger;
import org.eng.aisp.AISPProperties;
import org.eng.aisp.AISPRuntime;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.cache.Cache;
import org.eng.cache.IMultiKeyCache;

/**
 * Provides feature gram extraction (sub-windowing, feature extraction, feature processing), but caches the intermediate feature gram.
 * To enable this the class enable the ExtAISPRuntime class via the {@value AISPRuntime#AISP_RUNTIME_CLASS_PROPERTY_NAME} property.
 * @author 285782897
 *
 * @param <WINDATA>
 * @param <FDATA>
 */
public class CachingFeatureExtractionPipeline<WINDATA,FDATA> extends FeatureExtractionPipeline<WINDATA, FDATA> {

	public final static int DEFAULT_NUM_WINDOW_LOCKS = Runtime.getRuntime().availableProcessors() * 128;	// Somewhat arbitrary
	private final static boolean FEATURE_CACHING_ENABLED_DEFAULT = true;
	private final static String FEATURE_CACHING_ENABLED_PROPERTY_NAME = "feature.gram.pipeline.caching.enabled";
	private final static boolean FeatureGramCachingEnabled = AISPProperties.instance().getProperty(FEATURE_CACHING_ENABLED_PROPERTY_NAME, FEATURE_CACHING_ENABLED_DEFAULT);
	/** Used with {@link #ShowCacheHitModulus} to count the number of cache hits */
	private static AtomicInteger hits = new AtomicInteger();
	/** Used with {@link #ShowCacheMissModulus} to count the number of cache misses */
	private static AtomicInteger misses = new AtomicInteger();
	public final static String NUM_WINDOW_LOCKS_PROPERTY_NAME = "feature.gram.pipeline.numlocks";
	private final static int NUM_WINDOW_LOCKS = AISPProperties.instance().getProperty(NUM_WINDOW_LOCKS_PROPERTY_NAME, DEFAULT_NUM_WINDOW_LOCKS);
	/** Set to a non-zero value to show every Nth cache hit */
	private static int ShowCacheHitModulus = 0;
	/** Set to a non-zero value to show every Nth cache miss */
	private static int ShowCacheMissModulus = 0;
	private static IMultiKeyCache unprocessedFeatureCache = FeatureGramCachingEnabled ? Cache.newManagedMemoryCache() : null;
	private static IMultiKeyCache windowFeatureCache = FeatureGramCachingEnabled ? Cache.newManagedMemoryCache() : null;
	/** 
	 * Array of objects used to avoid simultaneous operations on the same window.
	 * We use this instead of synchronizing on the window itself to avoid deadlock with outside systems that might try and do the same.
	 * We then use the window ID to select the lock.  This can mean that different windows use the same lock, but that doesn't seem
	 * too big of a performance hit.  If it is, then increase NUM_WINDOW_LOCKS.
	 */
	private static Object[] WindowLocks = new Object[NUM_WINDOW_LOCKS];
	private transient int cachedHashCode;
	private transient Map<Integer, ReentrantLock> hashedLocks = new HashMap<Integer,ReentrantLock>();
	
	private transient FeatureGramExtractor<WINDATA,FDATA> subFeatureExtractor;

	static {
		for (int i=0 ; i<NUM_WINDOW_LOCKS ; i++)
			WindowLocks[i] = new Object();
	}

	public CachingFeatureExtractionPipeline(IFeatureGramDescriptor<WINDATA, FDATA> featureGramExtractor) {
		this(Arrays.asList(featureGramExtractor));
	}

	public CachingFeatureExtractionPipeline(List<IFeatureGramDescriptor<WINDATA, FDATA>> featureGramExtractors) {
		super(featureGramExtractors);
	}
	
//	/**
//	 * Override to implement caching of intermediate (feature) results.
//	 */
//	@Override
//	public IFeatureGram<FDATA>[] extract(IDataWindow<WINDATA> dataWindow) {
//		IFeatureGram<FDATA>[] fgArray = new IFeatureGram[extractorList.size()];
//		int index = 0;
//		for (FeatureGramExtractor<WINDATA, FDATA> sfe : extractorList) {
//			IFeatureGram<FDATA> fg = extract(dataWindow, sfe.getFeatureGramDescriptor()); 
//			fgArray[index] = fg;
//			index++;
//		}
//		return fgArray;
//	}
		
	
	/**
	 * Override to call our caching extractor.
	 */
	@Override
	protected IFeatureGram<FDATA> extract(IDataWindow<WINDATA> dataWindow, FeatureGramExtractor<WINDATA, FDATA> fge) {
		IFeatureGram<FDATA> fg = extract(dataWindow, fge.getFeatureGramDescriptor());
		return fg;
	}

	protected IFeatureGram<FDATA> extract(IDataWindow<WINDATA> dataWindow, IFeatureGramDescriptor<WINDATA,FDATA> fgd) {
	
			long windowID = dataWindow.getInstanceID();
			int windowSizeMsec = (int)fgd.getWindowSizeMsec();
			int windowShiftMsec = (int)fgd.getWindowShiftMsec();
			IFeatureExtractor<WINDATA,FDATA> extractor = fgd.getFeatureExtractor();
			IFeatureProcessor<FDATA> processor = fgd.getFeatureProcessor();
			IFeatureGram<FDATA> featureGram = windowFeatureCache == null ? null : (IFeatureGram<FDATA>)windowFeatureCache.get(windowID, fgd); 
	
			if (featureGram != null) {
				if (ShowCacheHitModulus != 0) {
					int v = hits.incrementAndGet();
					if (v % ShowCacheHitModulus == 0)
						AISPLogger.logger.info("Cache hit " + v + ", id=" + windowID + ", this=" + this);
	//				showFeatures("1", dataWindow, featureArray);
				}
				return featureGram;
			} else if (ShowCacheMissModulus != 0) {
				int v = misses.incrementAndGet();
				if (v % ShowCacheMissModulus == 0)
					AISPLogger.logger.info("Cache MISS " + v + ", id=" + windowID + ", this=" + this);
			}
			// Try and avoid computing the same thing more than once.
			int lockIndex = Math.abs((int)(windowID % NUM_WINDOW_LOCKS));	// abs() is just in case.
			synchronized(WindowLocks[lockIndex]) {
				featureGram = windowFeatureCache == null ? null : (IFeatureGram<FDATA>)windowFeatureCache.get(windowID, fgd); 
				if (featureGram != null) {
					if (ShowCacheHitModulus != 0) {
						int v = hits.incrementAndGet();
						if (v % ShowCacheHitModulus == 0)
							AISPLogger.logger.info("Cache hit (with lock) " + v + ", id=" + windowID + ", this=" + fgd);
					}
	//	    		showFeatures("2", dataWindow, featureArray);
					return featureGram;
				} else if (ShowCacheMissModulus != 0) {
					int v = misses.incrementAndGet();
					if (v % ShowCacheMissModulus == 0)
						AISPLogger.logger.info("Cache MISS (with lock) " + v + ". id=" + windowID + " processor:", fgd);
				}
	
				featureGram =  unprocessedFeatureCache == null ? null : (IFeatureGram<FDATA>)unprocessedFeatureCache.get(windowID, extractor, windowSizeMsec, windowShiftMsec ); 
				if (featureGram != null) {
//					AISPLogger.logger.info("FOUND cached featureGram under windowId=" + windowID + ", extactor=" + extractor.hashCode());
					if (ShowCacheHitModulus != 0) {
						int v = hits.incrementAndGet();
						if (v % ShowCacheHitModulus == 0)
							AISPLogger.logger.info("Cache hit (featureGram)" + v + ", id=" + windowID + ", this=" + fgd);
					}
				} else {
//					AISPLogger.logger.info("Did not find cached featureGram under windowId=" + windowID + ", extactor=" + extractor.hashCode());
					if (ShowCacheMissModulus != 0) {
						int v = misses.incrementAndGet();
						if (v % ShowCacheMissModulus == 0)
							AISPLogger.logger.info("Cache miss " + v + ", id=" + windowID + ", this=" + fgd);
					}
					FeatureGramExtractor subFeatureExtractor = new FeatureGramExtractor<WINDATA,FDATA>(windowSizeMsec, windowShiftMsec, extractor,null);
					featureGram = subFeatureExtractor.extractFeatureGram(dataWindow);
					if (unprocessedFeatureCache != null)
						unprocessedFeatureCache.put(featureGram, windowID, extractor, windowSizeMsec, windowShiftMsec);
//					AISPLogger.logger.info("caching unprocessed featureGram under windowId=" + windowID + ", extactor=" + extractor.hashCode());
				}
				if (processor != null)  
					featureGram = processor.apply(featureGram);
			
				if (windowFeatureCache != null)
					windowFeatureCache.put(featureGram, windowID, fgd);
	//			showFeatures("3", dataWindow, featureArray);
			}	// synchronized
			return featureGram;
		}

	//	public ILabeledFeatureGram<FDATA> extract(ILabeledDataWindow<WINDATA> labeledDataWindow) {
	////		AISPLogger.logger.info("Getting features from labeled window with id " + labeledDataWindow.getDataWindow().getInstanceID());
	//		IFeatureGram<FDATA> features = extract(labeledDataWindow.getDataWindow());
	//	    Properties labels = labeledDataWindow.getLabels();
	//		return new LabeledFeatureGram<FDATA>(features,labels);
	//	}
	
	private ReentrantLock getLock(IDataWindow window) {
		if (cachedHashCode == 0)
			cachedHashCode = this.hashCode();
		Object[] keys = new Object[] { window.getInstanceID(), cachedHashCode};
		int hash = Arrays.hashCode(keys);// % 31;
		ReentrantLock l = hashedLocks.get(hash);
		if (l != null)
			return l;
		synchronized (hashedLocks) {
			l = hashedLocks.get(hash);
			if (l == null) {
				l = new ReentrantLock(true);
				hashedLocks.put(hash, l);
			}
		}
		return l;
	}

	public static void clearCache() {
		if (windowFeatureCache != null)
			windowFeatureCache.clear();
		if (unprocessedFeatureCache != null)
			unprocessedFeatureCache.clear();
	}

}
