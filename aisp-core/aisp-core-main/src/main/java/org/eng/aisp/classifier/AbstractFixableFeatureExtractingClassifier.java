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
package org.eng.aisp.classifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.eng.aisp.AISPException;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.AISPRuntime;
import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.pipeline.LabeledFeatureIterable;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.transform.ITrainingWindowTransform;
import org.eng.aisp.transform.TransformingIterable;
import org.eng.aisp.transform.TransformingSizedShuffleIterable;
import org.eng.util.CachingIterable;
import org.eng.util.ISizedShuffleIterable;

/**
 * Provides a base set of function for implementers of IFixableClassifer that need to perform feature extraction.
 * Extends the super class to extract the features for the subclass implementations of {@link #trainFixedClassifierOnFeatures(Iterable)}.
 * 
 * @author dawood
 */
public abstract class AbstractFixableFeatureExtractingClassifier<WINDATA, FDATA> extends AbstractFixableClassifier<WINDATA> implements IFixableClassifier<WINDATA> {

	private static final long serialVersionUID = -3841277302940832291L;

	protected String primaryTrainingLabel;
	
	protected final ITrainingWindowTransform<WINDATA> trainingWindowTransform;
	
	protected List<IFeatureGramDescriptor<WINDATA,FDATA>> featureGramDescriptors;
	


	protected final boolean useMemoryCache;
	protected final boolean useDiskCache;

	protected final static boolean DefaultSoftReferenceFeatures = LabeledFeatureIterable.FeatureStreamingEnabledPropertyValue;
	
	/**
	 * If false, then features are computed all at once and in addition to be placed in the case are hard-referenced during training. 
	 * If true, then features are not hard referenced in LabeledFeatureIterable during training and they are only maintained in the soft cache so may be evicted.
	 */
	protected final boolean softReferenceFeatures;

	/**
	 * Convenience over {@link #AbstractFixableFeatureExtractingClassifier(boolean, String, IFeatureExtractor, IFeatureProcessor, int, int, boolean, boolean)}
	 * that does no local feature caching.
	 * @deprecated in favor of {@link #AbstractFixableFeatureExtractingClassifier(boolean, String, IFeatureExtractor, IFeatureProcessor, int, int, boolean, boolean)}
	 */
	protected AbstractFixableFeatureExtractingClassifier(boolean preShuffleData, ITrainingWindowTransform<WINDATA> transforms,
			IFeatureExtractor<WINDATA, FDATA> featureExtractor,
			IFeatureProcessor<FDATA> featureProcessor, int windowSizeMsec, int windowShiftMsec) {
		this(preShuffleData, transforms, featureExtractor, featureProcessor, windowSizeMsec, windowShiftMsec, false, false);
	}
	
	/**
	 * Wrap the process in a list.
	 * @param p
	 * @return never null.
	 */
	protected static <FDATA> List<IFeatureProcessor<FDATA>> toProcessorList(IFeatureProcessor<FDATA> p) {
		List<IFeatureProcessor<FDATA>> plist = new ArrayList<IFeatureProcessor<FDATA>>();
		if (p != null)
			plist.add(p);
		return plist;
	}

	/**
	 * Wrap the given feature gram extractor in a list, which is usually used by the fundamental constructor.
	 * @param fge
	 * @return never null.
	 */
	protected static <WINDATA,FDATA> List<IFeatureGramDescriptor<WINDATA,FDATA>> toFGEList(IFeatureGramDescriptor <WINDATA, FDATA> fge) {
		List<IFeatureGramDescriptor<WINDATA, FDATA>> list = new ArrayList<IFeatureGramDescriptor<WINDATA,FDATA>>();
		if (fge != null)
			list.add(fge);
		return list;
	}

	protected AbstractFixableFeatureExtractingClassifier(boolean preShuffleData, ITrainingWindowTransform<WINDATA> transforms,
			IFeatureExtractor<WINDATA, FDATA> featureExtractor,
			IFeatureProcessor<FDATA> featureProcessor, int windowSizeMsec, int windowShiftMsec,
			boolean useMemoryCache, boolean useDiskCache) {
		this(preShuffleData, transforms, featureExtractor, toProcessorList(featureProcessor), windowSizeMsec, windowShiftMsec, useMemoryCache, useDiskCache);
	}

	/**
	 * A convenience on {@link #AbstractFixableFeatureExtractingClassifier(boolean, ITrainingWindowTransform, List, boolean, boolean, boolean)} that sets
	 * the last 3 params to false as such assumes features will be hard referenced during training.
	 * @param preShuffleData
	 * @param transform
	 * @param featureGramDescriptors
	 */
	protected AbstractFixableFeatureExtractingClassifier(boolean preShuffleData, ITrainingWindowTransform<WINDATA> transform, List<IFeatureGramDescriptor<WINDATA,FDATA>> featureGramExtractors) {
		this(preShuffleData,  transform, featureGramExtractors, false, false, false);
	}

	/**
	 * The fundamental constructor.
	 * @param preShuffleData if true, the shuffle that data (in a repeatable way) during training
	 * @param transform the optional transform to be applied to the training data.  The transform is only applied to training data so should be some
	 * form of augmentation and not transformation of the original data.  This is NOT applied during classification.
	 * @param featureGramDescriptors a list of 1 or more feature gram extractors.  The underlying model must be prepared to handle the number
	 * of feature grams generated (i.e. the size of this list).  
	 * @param useMemoryCache - should memory caching of features be enabled during training.
	 * @param useDiskCache - should disk caching of features be enabled during training.
	 * @param softReferenceFeatures - if true, then features will be computed on demand and placed in the cache(s) only.  If false, then 
	 * features are computed and maintained both inside and outside of the cache (using a hard reference) during training.  
	 */
	protected AbstractFixableFeatureExtractingClassifier(boolean preShuffleData, ITrainingWindowTransform<WINDATA> transform, List<IFeatureGramDescriptor<WINDATA,FDATA>> featureGramExtractors,  boolean useMemoryCache,
							boolean useDiskCache, boolean softReferenceFeatures) {
		super(preShuffleData);
		if (featureGramExtractors == null  || featureGramExtractors.size() == 0)
			throw new IllegalArgumentException("featureGramDescriptors  must not be null or empty.");
		this.trainingWindowTransform = transform;
		this.featureGramDescriptors = featureGramExtractors;
		this.useMemoryCache = useMemoryCache;
		this.useDiskCache = useDiskCache;
		this.softReferenceFeatures = softReferenceFeatures;
	}
	
	protected AbstractFixableFeatureExtractingClassifier(boolean preShuffleData, ITrainingWindowTransform<WINDATA> transform, IFeatureGramDescriptor<WINDATA,FDATA> featureGramExtractor,  boolean useMemoryCache,
			boolean useDiskCache) {
		this(preShuffleData, transform, toFGEList(featureGramExtractor), useMemoryCache, useDiskCache, DefaultSoftReferenceFeatures);
	}

	protected AbstractFixableFeatureExtractingClassifier(boolean preShuffleData, IFeatureGramDescriptor<WINDATA,FDATA> featureGramExtractor,  boolean useDiskCache) {
		this(preShuffleData, null, toFGEList(featureGramExtractor), true, useDiskCache, DefaultSoftReferenceFeatures);
	}



	/**
	 * #param preShuffleData causes training data to be shuffled before feature are extractor or transforms are applied.
	 * @param transform optional transform applied to data before features are extracted.
	 * @param featureExtractor extract applied to windows or subwindows of the labeled data.
	 * @param windowSizeMsec the size of the subwindows into which each labeled data window is divided into.  Set to 0 to not subdivide the labeled data windows.
	 * @param windowShiftMsec the amount of shift in time between subsequent subwindows.  Typical values are {@link #windowSizeMsec} or {@link #windowSizeMsec}/2.  
	 * @param useMemoryCache if true, then cache the extracted features using a memory cache.  This is probably used for smaller data sets and when 
	 * the subclass makes multiple passes through the features during training.
	 * @param useDiskCache if true, then cache the extracted features using a disk cache.  This is used for larger data sets and when the classifier makes multiple
	 * passes through the features during training.
	 * The former gives pure rolling windows, the latter a sliding window in which each subwindow overlaps with half the previous.  Use 0 to set to rolling windows
	 * @param featureProcessor option processor that acts on 1 or more features extracted from a single window.
	 */
	protected AbstractFixableFeatureExtractingClassifier(boolean preShuffleData, ITrainingWindowTransform<WINDATA> transforms,
			IFeatureExtractor<WINDATA, FDATA> featureExtractor,
			List<IFeatureProcessor<FDATA>> featureProcessors, int windowSizeMsec, int windowShiftMsec,
			boolean useMemoryCache, boolean useDiskCache) {
		this(preShuffleData, transforms, makeFeatureGramExtractors(featureExtractor, featureProcessors, windowSizeMsec, windowShiftMsec), useMemoryCache, useDiskCache, DefaultSoftReferenceFeatures);
//		if (trainingLabel == null)
//			throw new IllegalArgumentException("training label must not be null");
//		if (featureExtractor == null)
//			throw new IllegalArgumentException("feature extractor must not be null");
//		this.primaryTrainingLabel = trainingLabel;
//		this.featureExtractor = featureExtractor;
//		this.windowSizeMsec = windowSizeMsec;
//		this.windowShiftMsec = windowShiftMsec;
//		this.featureProcessor = null; 
//		if (featureProcessors != null) 
//			this.featureProcessors.addAll(featureProcessors);
//		this.useMemoryCache = useMemoryCache;
//		this.useDiskCache = useDiskCache;
	}
	


	/**
	 * Create the list of feature gram extractors from the given data.
	 * @param featureExtractor
	 * @param featureProcessors
	 * @param windowSizeMsec
	 * @param windowShiftMsec
	 * @return
	 */
	private static <WINDATA, FDATA> List<IFeatureGramDescriptor<WINDATA, FDATA>> makeFeatureGramExtractors(
			IFeatureExtractor<WINDATA, FDATA> featureExtractor, List<IFeatureProcessor<FDATA>> featureProcessors,
			int windowSizeMsec, int windowShiftMsec) {
		List<IFeatureGramDescriptor<WINDATA,FDATA>> fgeList = new ArrayList<IFeatureGramDescriptor<WINDATA,FDATA>>();
		IFeatureGramDescriptor<WINDATA,FDATA> fge;
		if (featureProcessors == null || featureProcessors.size() == 0) {
			fge = new FeatureGramDescriptor<WINDATA,FDATA>(windowSizeMsec, windowShiftMsec, featureExtractor, null);
			fgeList.add(fge);
		} else {
			for (IFeatureProcessor<FDATA> fp : featureProcessors) {
				fge = new FeatureGramDescriptor<WINDATA,FDATA>(windowSizeMsec, windowShiftMsec, featureExtractor, fp);
				fgeList.add(fge);
			}

		}
		return fgeList;
	}
	
	
//	@Override
//	public final void train(Iterable<? extends ILabeledDataWindow<WINDATA>> data) throws AISPException {
//		if (this.primaryTrainingLabel == null)
//			throw new RuntimeException("training label was not set via the constructor");
//		super.train(this.primaryTrainingLabel, data);
//	}

	/**
	 * Implements feature extraction on the data.  The features are then passed to the sub class via {@link #trainFixedClassifierOnFeatures(Iterable)}
	 * to actually build the classifier.  Each ILabeledDataWindow in the given iterable is processed to produce an array of 1 or more features as
	 * determined by the {@link #windowSizeMsec} and {@link #windowShiftMsec}.  If {@link #windowSizeMsec} is 0, then the size of the array will be one,
	 * otherise for each labeled data window it will be of size at least (window duration / {@link #windowSizeMsec} plus more depending on the value
	 * of {@link #windowShiftMsec}.
	 */
	@Override
	protected final IFixedClassifier<WINDATA> trainFixedClassifierOnData(String trainingLabel, Iterable<? extends ILabeledDataWindow<WINDATA>> data) throws AISPException {
	
		this.primaryTrainingLabel = trainingLabel;
		
		if (trainingWindowTransform != null) 
			data = getTransformingIterable(data);
	
		// Exclude data that does not have the training label and that has the training label but is values is the 'unknown' value.
		Predicate<ILabeledDataWindow<WINDATA>> dataPredicate = new TrainingDataPredicate<WINDATA>(primaryTrainingLabel, true);
		// TODO: it might be good if this was an IShuffleIterable, although at this time (9/28/2018), no subclass seems to require this.
		Iterable<ILabeledFeatureGram<FDATA>[]> fi = new LabeledFeatureIterable<WINDATA,FDATA>(data, dataPredicate, featureGramDescriptors, this.softReferenceFeatures); 
		fi = AISPRuntime.getRuntime().getCachingFeatureGramIterable(fi, useDiskCache, useMemoryCache);
		
		// Verify that the first (and subsequent) feature grams have at least one valid feature
		checkFeatureGrams(fi);
		
//		AISPLogger.logger.info("w/s: " + windowSizeMsec + "/" + windowShiftMsec + ", windowCount="+windowCount + ", featureCount=" + featureCount + ", subFeatureCount=" + subFeatureCount);
//		AISPLogger.logger.info("Begin training on features, classifier= " + this.getClass().getSimpleName());
		IFixedClassifier<WINDATA> r = trainFixedClassifierOnFeatures(fi);
		if (fi instanceof CachingIterable)	 {
			// Not strictly necessary, but more efficient to proactively clean things up rather than let the GC do it. 
			((CachingIterable)fi).clearCachedItems();
		}
//		AISPLogger.logger.info("Done training on features, classifier= " + this.getClass().getSimpleName());
		return r;
	}

	/**
	 * Look at the first feature gram in each array of feature grams to make sure they have at least one feature. 
	 * @param fi
	 * @throws AISPException if invalid feature gram is found.
	 */
	protected void checkFeatureGrams(Iterable<ILabeledFeatureGram<FDATA>[]> fi) throws AISPException {
		// Iterator.
		Iterator<ILabeledFeatureGram<FDATA>[]> fgArrayIter = fi.iterator();
		// Make sure there is at least one feature gram arrayu 
		if (!fgArrayIter.hasNext()) 
			throw new AISPException("Feature gram extractor(s) failed to produce any feature gram arrays. Did you specify the correct training label?");
		// Make sure the first array of feature grams has at least 1 element.
		ILabeledFeatureGram<FDATA>[] fgArray0 = fgArrayIter.next();
		if (fgArray0 == null || fgArray0.length == 0)
			throw new AISPException("Feature extractor(s) failed to produce any feature grams");
		// Check the first feature gram in each array to make sure it has some features.
		for (int index = 0 ; index<fgArray0.length ; index++) {
			ILabeledFeatureGram<FDATA> fg = fgArray0[index];
			IFeature<?> fArray[] = fg.getFeatureGram().getFeatures(); 
			if (fArray.length == 0)
				throw new AISPException("Featuregram[" + index + "] in the first array of feature grams contains zero features."); 
//			IFeature<?> feature = fArray[0];
	//			double[] data = feature.getData();
	//			if (data == null || data.length == 0)
	//				throw new AISPException("Featuregram[" + index + "] contains zero features."); 
				
		}
	}
	

	/**
	 * Generate a transforming iterable that best matches the data type of the given data.
	 * ISizedShuffleIterable and ISizedIterable are handled.
	 * We try and maintain the ISizedIterable and/or ISizedShuffleIterable as passed to 
	 * the feature extraction pipeline and the sub-class training algorithms.
	 * @param data
	 * @return
	 */
	private Iterable<? extends ILabeledDataWindow<WINDATA>> getTransformingIterable(Iterable<? extends ILabeledDataWindow<WINDATA>> data) {
		if (data instanceof ISizedShuffleIterable) 
			data = new TransformingSizedShuffleIterable<WINDATA>(this.primaryTrainingLabel, 
						(ISizedShuffleIterable<ILabeledDataWindow<WINDATA>>)data, 
						this.trainingWindowTransform);
//		else if (data instanceof ISizedIterable) 
//			data = new TransformingSizedIterable<WINDATA>(this.primaryTrainingLabel, 
//						(ISizedIterable<ILabeledDataWindow<WINDATA>>)data, 
//						this.trainingWindowTransform);
		else 
			data = new TransformingIterable<WINDATA>(this.primaryTrainingLabel, 
						(Iterable<ILabeledDataWindow<WINDATA>>)data, 
						this.trainingWindowTransform);

		return data;
	}

	/**
	 * Build the classifier from the given set of features.
	 * If this is called, then the iterable has at least one feature array in it.
	 * {@link #primaryTrainingLabel} has been set to the value passed to {@link #train(String, Iterable)}
	 * when this is called and should be used by the implementation to define the labels on which the model
	 * is trained.
	 * @param features the iterable over arrays of labeled features.  Each array of features corresponds to an individual training sample as provided
	 * to {@link #trainFixedClassifierOnData(String, Iterable)} as part of the iterable provided there.
	 * @return never null.
	 * @throws AISPException
	 */
	protected abstract IFixedClassifier<WINDATA> trainFixedClassifierOnFeatures(Iterable<? extends ILabeledFeatureGram<FDATA>[]> features) throws AISPException;


	@Override
	public String toString() {
		final int maxLen = 8;
		return "AbstractFixableFeatureExtractingClassifier [primaryTrainingLabel=" + primaryTrainingLabel
				+ ", trainingWindowTransform=" + trainingWindowTransform + ", featureGramDescriptors="
				+ (featureGramDescriptors != null ? toString(featureGramDescriptors, maxLen) : null) + ", useMemoryCache="
				+ useMemoryCache + ", useDiskCache=" + useDiskCache + ", softReferenceFeatures=" + softReferenceFeatures
				+ ", classifier=" + classifier + ", preShuffleData=" + preShuffleData + ", tags="
				+ (tags != null ? toString(tags.entrySet(), maxLen) : null) + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((featureGramDescriptors == null) ? 0 : featureGramDescriptors.hashCode());
		result = prime * result + ((primaryTrainingLabel == null) ? 0 : primaryTrainingLabel.hashCode());
		result = prime * result + (softReferenceFeatures ? 1231 : 1237);
		result = prime * result + ((trainingWindowTransform == null) ? 0 : trainingWindowTransform.hashCode());
		result = prime * result + (useDiskCache ? 1231 : 1237);
		result = prime * result + (useMemoryCache ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof AbstractFixableFeatureExtractingClassifier))
			return false;
		AbstractFixableFeatureExtractingClassifier other = (AbstractFixableFeatureExtractingClassifier) obj;
		if (featureGramDescriptors == null) {
			if (other.featureGramDescriptors != null)
				return false;
		} else if (!featureGramDescriptors.equals(other.featureGramDescriptors))
			return false;
		if (primaryTrainingLabel == null) {
			if (other.primaryTrainingLabel != null)
				return false;
		} else if (!primaryTrainingLabel.equals(other.primaryTrainingLabel))
			return false;
		if (softReferenceFeatures != other.softReferenceFeatures)
			return false;
		if (trainingWindowTransform == null) {
			if (other.trainingWindowTransform != null)
				return false;
		} else if (!trainingWindowTransform.equals(other.trainingWindowTransform))
			return false;
		if (useDiskCache != other.useDiskCache)
			return false;
		if (useMemoryCache != other.useMemoryCache)
			return false;
		return true;
	}

	private String toString(Collection<?> collection, int maxLen) {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		int i = 0;
		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
			if (i > 0)
				builder.append(", ");
			builder.append(iterator.next());
		}
		builder.append("]");
		return builder.toString();
	}
	

}
