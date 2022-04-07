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
package org.eng.aisp.classifier.gmm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLogger;
import org.eng.aisp.AISPProperties;
import org.eng.aisp.classifier.AbstractClassifier;
import org.eng.aisp.classifier.AbstractFixableFeatureExtractingClassifier;
import org.eng.aisp.classifier.IFixableClassifier;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.classifier.gaussianmixture.FixedGMMClassifier;
import org.eng.aisp.classifier.gaussianmixture.FixedSingleGaussianMixture;
import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.MFCCFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.transform.ITrainingWindowTransform;
import org.eng.util.ExecutorUtil;




/**
 * Gaussian mixture model classifier. 
 * @author wangshiq
 *
 */
public class GMMClassifier extends AbstractFixableFeatureExtractingClassifier<double[], double[]> implements IFixableClassifier<double[]> {

	
	private static final long serialVersionUID = -4514292010883401046L;


//    private final static int MAX_ACTIVE_LEARNING_TASKS = Runtime.getRuntime().availableProcessors();
//    private final static Semaphore ActiveLearners = new Semaphore(MAX_ACTIVE_LEARNING_TASKS);
//	private final static ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),new HigherPriorityDaemonThreadFactory());
	private final static ExecutorService executor = ExecutorUtil.getPrioritizingSharedService(); 

	/**
	 * Recommended default sub window size of subwindows on which features are extracted. 
	 * Value is {@value #DEFAULT_WINDOW_SIZE_MSEC}.
	 */
	public final static int DEFAULT_WINDOW_SIZE_MSEC = 40;
	/**
 	 * Recommended default window shift size when extracting subwindows for feature extraction. 0 should indicate rolling windows. 
	 * Value is {@value #DEFAULT_WINDOW_SHIFT_MSEC}.
	 */
	public final static int DEFAULT_WINDOW_SHIFT_MSEC = 20;
	
	
	public final static String DEFAULT_UNKNOWN_THRESH_COEFF_PROPERTY_NAME = "classifiers.gmm.unknown_threshold_coefficient";
	
    /** Used for determining the threshold for detecting known vs. unknown.
     *  The threshold is set as UNKNOWN_THRESH_COEFF multiplied by the minimum density (the sum density from the single GMMs for each model), 
     *  where the minimum is among the densities of all training sound clips.
     *  A sound is classified as unknown when its density is below the threshold.
     *  The value UNKNOWN_THRESH_COEFF needs to be >= 0.0.
     *  A larger value of UNKNOWN_THRESH_COEFF gives a higher sensitivity of unknown sound detection, i.e., more sounds will be detected as unknown.
     *  When UNKNOWN_THRESH_COEFF = 0.0, unknown detection is completely turned off.
     *  When UNKNOWN_THRESH_COEFF = 1.0, we have the highest sensitivity such that all the training samples (i.e. known sounds from training data) are classified as known.
     *  When UNKNOWN_THRESH_COEFF > 1.0, some training samples (i.e. known sounds from training data) are classified as unknown.
	 *  Default is 0.01.
     */
	public final static double DEFAULT_UNKNOWN_THRESH_COEFF = AISPProperties.instance().getProperty(DEFAULT_UNKNOWN_THRESH_COEFF_PROPERTY_NAME, 0.0);  
	
	/**
	 * Defines the default feature extractor to use.
	 * Default is a MFCCFeatureExtractor constructed with the constructor, {@link MFCCFeatureExtractor#MFCCFeatureExtractor(20)}.
	 */
	public final static IFeatureExtractor<double[],double[]> DEFAULT_FEATURE_EXTRACTOR = new MFCCFeatureExtractor(20);

	public static final ITrainingWindowTransform<double[]> DEFAULT_TRANSFORMS = null;
	
	/**
	 * Defines the default feature processor to use.
	 * Default is not use a feature processor. 
	 */
	public final static IFeatureProcessor<double[]> DEFAULT_FEATURE_PROCESSOR = null; 

	/**
	 * If true, a diagonal covariance matrix is used; else use full covariance matrix.
	 * Value is {@value #DEFAULT_USE_DIAGONAL_COVARIANCE}.
	 */
	public final static boolean DEFAULT_USE_DIAGONAL_COVARIANCE = true;

	/**
	 * Defines whether by default the disk cache should be used when training.
	 * Value is {@value #DEFAULT_USE_DISK_CACHE}.
	 */
	public static final boolean DEFAULT_USE_DISK_CACHE = false;
	
	/**
	 * Defines the number of Gaussians in the mixture model.
	 * Value is {@value #DEFAULT_NUM_GAUSSIANS}.
	 */
	public final static int DEFAULT_NUM_GAUSSIANS = 8;




	
	private final boolean diagonalCovariance;
	private final double unknownThreshCoeff;
    
	private final int numGaussiansToMix;
    private List<String> listOfLabelValues;
    private List<FixedSingleGaussianMixture> listOfModels;

    /**
     * Convenience constructor over {@link #GMMClassifier(String, IFeatureExtractor, IFeatureProcessor, int, int, boolean, int, boolean, double)}.
     * Using {@link #DEFAULT_FEATURE_EXTRACTOR}, {@link #DEFAULT_FEATURE_PROCESSOR}, 
     * {@link AbstractClassifier#DEFAULT_WINDOW_SIZE_MSEC}, 
     * {@link AbstractClassifier#DEFAULT_WINDOW_SHIFT_MSEC}, 
     * {@link #DEFAULT_USED_DISK_CACHE},
     * {@link #DEFAULT_NUM_GAUSSIANS},
     * {@link #DEFAULT_USE_DIAGONAL_COVARIANCE}, and
     * {@link #DEFAULT_UNKNOWN_THRESH_COEFF}.
     */
	public GMMClassifier() {
		this(DEFAULT_FEATURE_EXTRACTOR, DEFAULT_FEATURE_PROCESSOR);
	}

	public GMMClassifier(IFeatureExtractor<double[], double[]>  extractor, IFeatureProcessor<double[]> featureProcessor) {
		this(null, extractor, featureProcessor, DEFAULT_WINDOW_SIZE_MSEC,
					DEFAULT_WINDOW_SHIFT_MSEC, DEFAULT_USE_DISK_CACHE, 
					DEFAULT_NUM_GAUSSIANS, 
					DEFAULT_USE_DIAGONAL_COVARIANCE, DEFAULT_UNKNOWN_THRESH_COEFF);
	}

//	public GMMClassifier(String primaryTrainingLabel, double knownConfidenceCutoff) {
//		this(primaryTrainingLabel, AbstractClassifier.DEFAULT_WINDOW_SIZE_MSEC, AbstractClassifier.DEFAULT_WINDOW_SHIFT_MSEC, DEFAULT_NUM_GAUSSIANS, DEFAULT_USE_DIAGONAL_COVARIANCE, knownConfidenceCutoff);
//	}

//	public GMMClassifier(String primaryTrainingLabel, int windowSizeMsec, int windowShiftMsec) {
//		this(primaryTrainingLabel, windowSizeMsec, windowShiftMsec, DEFAULT_NUM_GAUSSIANS, DEFAULT_USE_DIAGONAL_COVARIANCE, DEFAULT_UNKNOWN_THRESH_COEFF);
//	}

	
//	public GMMClassifier(String primaryTrainingLabel, int windowSizeMsec, int windowShiftMsec, int numGaussiansToMix, boolean diagonalCovariance, double knownConfidenceCutoff) {
//		this(primaryTrainingLabel, DEFAULT_FEATURE_EXTRACTOR, DEFAULT_FEATURE_PROCESSOR, windowSizeMsec,windowShiftMsec, false, numGaussiansToMix, diagonalCovariance, knownConfidenceCutoff);
//	}

//	/**
//	 * Convenience constructor over {@link #GMMClassifier(String, IFeatureExtractor, IFeatureProcessor, int, int, boolean, int, boolean, double)}
//	 * Uses the default window size/shift from the super class, {@link #DEFAULT_NUM_GAUSSIANS}, {@link #DEFAULT_USE_DIAGONAL_COVARIANCE}, 
//	 * {@link #DEFAULT_UNKNOWN_THRESH_COEFF_PROPERTY_NAME} and uses a memory cache.
//	 */
//	public GMMClassifier(String primaryTrainingLabel, IFeatureExtractor<double[], double[]> fe, IFeatureProcessor<double[]> proc) {
//		this(primaryTrainingLabel, fe, proc, DEFAULT_WINDOW_SIZE_MSEC,DEFAULT_WINDOW_SHIFT_MSEC, false, DEFAULT_NUM_GAUSSIANS, DEFAULT_USE_DIAGONAL_COVARIANCE, DEFAULT_UNKNOWN_THRESH_COEFF);
//	}

	/**
	 * Convenience constructor over {@link #GMMClassifier(String, IFeatureExtractor, IFeatureProcessor, int, int, boolean, int, boolean, double)}
	 * Uses the {@link #DEFAULT_UNKNOWN_THRESH_COEFF_PROPERTY_NAME}
	 */
	public GMMClassifier(IFeatureExtractor<double[], double[]>  extractor, IFeatureProcessor<double[]> featureProcessor, 
			int windowSizeMsec, int windowShiftMsec, boolean useDiskCache, int numGaussiansToMix, boolean diagonalCovariance) {
		this(null, extractor, featureProcessor, windowSizeMsec, windowShiftMsec, useDiskCache, numGaussiansToMix, diagonalCovariance, DEFAULT_UNKNOWN_THRESH_COEFF);
	}
	
	public GMMClassifier(IFeatureExtractor<double[], double[]>  extractor, IFeatureProcessor<double[]> featureProcessor, 
			int windowSizeMsec, int windowShiftMsec) {
		this(null, extractor, featureProcessor, windowSizeMsec, windowShiftMsec, DEFAULT_USE_DISK_CACHE, DEFAULT_NUM_GAUSSIANS, DEFAULT_USE_DIAGONAL_COVARIANCE, DEFAULT_UNKNOWN_THRESH_COEFF);
	
	}
		


	/**
	 * Constructor for GMMClassifier.
	 * @param transforms
	 * @param extractor feature extractor
	 * @param featureProcessor feature processor
	 * @param windowSizeMsec size of subwindow in milliseconds
	 * @param windowShiftMsec shift of subwindow in milliseconds
	 * @param useDiskCache set to true for large data sets, otherwise set to false to use a memory cache on small data sets.
	 * @param numGaussiansToMix Defines the number of Gaussians in the mixture model.
	 * @param diagonalCovariance If true, a diagonal covariance matrix is used; else use full covariance matrix.
	 * @param knownConfidenceCutoff Used for determining the threshold for detecting known vs. unknown. See {@link #DEFAULT_UNKNOWN_THRESH_COEFF DEFAULT_KNOWN_CONFIDENCE_CUTOFF} for details.
	 */
	public GMMClassifier(ITrainingWindowTransform<double[]> transforms, 
			IFeatureExtractor<double[], double[]>  extractor,
			IFeatureProcessor<double[]> featureProcessor, 
			int windowSizeMsec, int windowShiftMsec, boolean useDiskCache, int numGaussiansToMix, boolean diagonalCovariance, double unknownThreshCoeff) {
		this(transforms, new FeatureGramDescriptor<double[],double[]>(windowSizeMsec, windowShiftMsec, extractor, featureProcessor), 
				useDiskCache, numGaussiansToMix, diagonalCovariance,  unknownThreshCoeff);

	}

	/**
	 * A convenience on {@link #GMMClassifier(ITrainingWindowTransform, IFeatureExtractor, IFeatureProcessor, int, int, boolean, int, boolean, double)}
	 * with null transform, no disk caching, default number of guassians, default use of diagonal covariance and default unknown threshold.
	 * @param fge
	 */
	public GMMClassifier(IFeatureGramDescriptor<double[], double[]>  fge) {
		this(null, fge, false, DEFAULT_NUM_GAUSSIANS, DEFAULT_USE_DIAGONAL_COVARIANCE, DEFAULT_UNKNOWN_THRESH_COEFF);
	}

	public GMMClassifier(ITrainingWindowTransform<double[]> transforms, 
			IFeatureGramDescriptor<double[], double[]>  fge, boolean useDiskCache, int numGaussiansToMix, boolean diagonalCovariance, double unknownThreshCoeff) {
		super(true, transforms,fge, false, useDiskCache);

		this.numGaussiansToMix = numGaussiansToMix;
		listOfLabelValues = new ArrayList<>();
		listOfModels = new ArrayList<>();
		this.diagonalCovariance = diagonalCovariance;
		if (unknownThreshCoeff < 0.0 || Double.isNaN(unknownThreshCoeff)) 
			throw new IllegalArgumentException("unknownThreshCoeff must be >= 0.0");
		this.unknownThreshCoeff = unknownThreshCoeff;	
	}
	


//This is the old training method without parallelization
//	@Override
//	protected IFixedClassifier<double[]> trainFixedClassifierOnFeatures(
//			Iterable<? extends ILabeledFeature<double[]>[]> featureWithLabels) throws AISPException {
//		
//		listOfLabelValues.clear();
//		listOfModels.clear();
//		
//		Map<String, List<IFeature<double[]>>> labelToFeaturesMap = new HashMap<String, List<IFeature<double[]>>>();
//		
//		for( ILabeledFeature<double[]>[] lfea : featureWithLabels) {
//			String labelValue = lfea[0].getLabels().getProperty(primaryTrainingLabel);
//			if (labelValue == null) {
//				continue;
//			}
//			
//			List<IFeature<double[]>> listOfFeatures = labelToFeaturesMap.get(labelValue);
//			if (listOfFeatures == null) {
//				listOfFeatures = new ArrayList<IFeature<double[]>>();
//			}
//			
//			for (ILabeledFeature<double[]> lfe : lfea) {
//				listOfFeatures.add(lfe.getFeature());
//			}
//			
//			labelToFeaturesMap.put(labelValue, listOfFeatures);
//		}
//		
//		if (labelToFeaturesMap.size() == 0)
//			throw new AISPException("Label '" + primaryTrainingLabel + "' was not found in training data");
//		
//
//		Set<String> labelValueSet = labelToFeaturesMap.keySet();
//		for (String labelValue : labelValueSet) {
//			List<IFeature<double[]>> featureList = labelToFeaturesMap.get(labelValue);
//			
//			List<double[]> dataVec = new ArrayList<>();
//
//			int dim = -1;
//			
//			for (IFeature<double[]> f : featureList) {
//				double[] fv = f.getData();
//				dim = fv.length;  //Assumes all features have same lengths
//				
//				dataVec.add(fv);
//			}
//			
//			
//            listOfLabelValues.add(labelValue);
//            listOfModels.add(GMMTrainingUtil.train(dim, numGaussiansToMix, diagonalCovariance, dataVec));
//		}
//		
//		
//		
//		// Find the threshold for unknown detection
//		List<Double> probOverallList = new ArrayList<>();
//
//		for( ILabeledFeature<double[]>[] lfea : featureWithLabels) {
//			for (int j=0; j<lfea.length; j++) {
//				double probOverall = 0.0;
//				
//				for (int i=0; i<listOfLabelValues.size(); i++) {
//					probOverall += listOfModels.get(i).density(lfea[j].getFeature().getData());
//				}
//				probOverallList.add(probOverall);
//			}
//		}
//		
//		Double[] probOverallArray = probOverallList.toArray(new Double[0]);
//		ArrayIndexComparator comparator = new ArrayIndexComparator(probOverallArray);
//		Integer[] indexes = comparator.createIndexArray();   //this contains indexes of sorted distances
//		Arrays.sort(indexes, comparator);
//		
//		int threshIndex = (int)(indexes.length * knownConfidenceCutoff);
//		if (threshIndex < 0) threshIndex = 0;
//		else if (threshIndex >= indexes.length) threshIndex = indexes.length - 1;
//		double thresh = probOverallArray[indexes[threshIndex]];	
//		
//		return new FixedGMMClassifier(primaryTrainingLabel, featureExtractor, featureProcessor, windowSizeMsec, 
//				windowShiftMsec, numGaussiansToMix, listOfLabelValues, listOfModels, thresh);
//
//		
//		
//	}


	
	
	
	
	
	/**
	 * The result produced by a LearnGMMTask.
	 */
	private static class LearnedGMM {
		FixedSingleGaussianMixture model;
		String labelValue;

		public LearnedGMM(String labelValue, FixedSingleGaussianMixture model) {
			this.labelValue = labelValue;
			this.model = model;
		}

		public String getLabelValue() {
			return labelValue;
		}

		public FixedSingleGaussianMixture getLearnedMixture() {
			return model;
		}
		
	}
	
	/**
	 * Task submitted to executor to learn the model for one label value.
	 * @author dawood
	 * @author wangshiq
	 *
	 */
	private class LearnGMMTask implements Callable<LearnedGMM> {

		private String labelValue;
		private Iterable<IFeature<double[]>> features;

		public LearnGMMTask(String labelValue, Iterable<IFeature<double[]>> featureList) {
			this.labelValue = labelValue;
			this.features = featureList;;
		}

		@Override
		public LearnedGMM call() throws Exception {
//			try {
//				ActiveLearners.acquire();

				Iterator<IFeature<double[]>> feIt = features.iterator();
				if (feIt.hasNext() == false) {
					throw new IllegalStateException("No element in features.");
				}
				int dim = feIt.next().getData().length;   //Assumes all features have same lengths
//				AISPLogger.logger.info("Training guassian");
				FixedSingleGaussianMixture model = GMMTrainingUtil.train(dim, numGaussiansToMix, diagonalCovariance, features);
//				AISPLogger.logger.info("Done Training guassian");
				
				return new LearnedGMM(labelValue, model);
				
//			} finally {
//				ActiveLearners.release();
//			}
		}
		
	}
	
	
	@Override
	protected IFixedClassifier<double[]> trainFixedClassifierOnFeatures(
			Iterable<? extends ILabeledFeatureGram<double[]>[]> labeledFeatureGrams) throws AISPException {
//		int count = 0;
//		int subCount = 0;
//		for (ILabeledFeature<double[]>[] ldw : featureWithLabels) {
//			count++;
//			subCount += ldw.length;
//		}
//		
//		AISPLogger.logger.info("count="+count + ", subCount=" + subCount);

//		long startMsec = System.currentTimeMillis();
//		AISPLogger.logger.info("Begin training");
		listOfLabelValues.clear();
		listOfModels.clear();

		//Use a nested list to group features by sound clip, needed for determining unknown detection threshold. 
		//The first list contains the list of features for each sound clip, the second list contains all the features for the corresponding sound clip.
		Map<String, List<List<IFeature<double[]>>>> labelToFeaturesMap = new HashMap<String, List<List<IFeature<double[]>>>>();
		
	
		// Group features by label value.
		ILabeledFeatureGram<double[]> firstLFG = null;
		boolean foundLabels = false;
		for( ILabeledFeatureGram<double[]>[] lfga : labeledFeatureGrams) {
			if (lfga.length > 1 )
				throw new IllegalArgumentException("More than one feature gram not supported");
			if (lfga.length == 0)
				continue;
			String labelValue = lfga[0].getLabels().getProperty(primaryTrainingLabel);
			if (labelValue == null) 
				continue;
			foundLabels = true;
			ILabeledFeatureGram<double[]> lfg = lfga[0];
			IFeature<double[]>[] features = lfg.getFeatureGram().getFeatures();
			if (features.length == 0)
				continue;

			List<List<IFeature<double[]>>> listOfFeatures = labelToFeaturesMap.get(labelValue);
			if (listOfFeatures == null) {
				listOfFeatures = new ArrayList<List<IFeature<double[]>>>();
				labelToFeaturesMap.put(labelValue, listOfFeatures);
			}
			
			List <IFeature<double[]>>subListOfFeatures = new ArrayList<IFeature<double[]>>();
			listOfFeatures.add(subListOfFeatures);
			for (IFeature<double[]> fe : features) 
				subListOfFeatures.add(fe);
			
		}
//		AISPLogger.logger.info("Done grouping features");
		if (!foundLabels)
			throw new AISPException("Label '" + primaryTrainingLabel + "' was not found in training data");
		if (labelToFeaturesMap.size() == 0)
			throw new AISPException("No feature subwindows found (is subwindow too small?)");
		
		List<Future<LearnedGMM>> futureList = new ArrayList<Future<LearnedGMM>>();
//		long afterFeaturesMsec = System.currentTimeMillis();
		
		Set<String> labelValueSet = labelToFeaturesMap.keySet();
		
		for (String labelValue : labelValueSet) {
			//Convert nested list to a single list
			List<IFeature<double[]>> featureList = new ArrayList<IFeature<double[]>>();
			List<List<IFeature<double[]>>> nestedFeatureList = labelToFeaturesMap.get(labelValue);
			for (List<IFeature<double[]>> subFeatureList : nestedFeatureList) {
				featureList.addAll(subFeatureList);
			}

			// Submit a task to learn the model for this label.
			LearnGMMTask task = new LearnGMMTask(labelValue, featureList);
			Future<LearnedGMM> f = executor.submit(task);
			futureList.add(f);
		}
	
//		AISPLogger.logger.info("Waiting on gmm learning tasks");
		// Wait for and collect all results.
		for (Future<LearnedGMM> f :  futureList) {
			LearnedGMM gmm;
			try {
				gmm = f.get();
				listOfLabelValues.add(gmm.getLabelValue());
				listOfModels.add(gmm.getLearnedMixture());
			} catch (InterruptedException | ExecutionException e) {
				AISPLogger.logger.warning("Could not get the model for one of the label values: " + e.getMessage());
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		}
//		AISPLogger.logger.info("Done with all learning tasks");
		
		// Find the threshold for unknown detection
		List<Double> logDensityOverallList = new ArrayList<>();
		
		for (String labelValue : labelValueSet) {
			List<List<IFeature<double[]>>> nestedFeatureList = labelToFeaturesMap.get(labelValue);
		
			for( List<IFeature<double[]>> feList : nestedFeatureList) {
//				double sumLogProbabilities = feList.parallelStream().map(fe -> {
				double sumLogProbabilities = feList.stream().map(fe -> {
					double probOverall = 0.0;
					
					for (int i=0; i<listOfLabelValues.size(); i++) {
						probOverall += listOfModels.get(i).density(fe.getData());
					}
					return Math.log(probOverall);
				}).collect(Collectors.summingDouble(Double::doubleValue)); 
				
				
				logDensityOverallList.add(sumLogProbabilities / feList.size());
			}
		}
		
//		AISPLogger.logger.info("Done getting densities");

		//The below commented part is not needed because the threshold is currently found based on the minimum only //		Double[] probOverallArray = logDensityOverallList.toArray(new Double[0]);
//		ArrayIndexComparator comparator = new ArrayIndexComparator(probOverallArray);
//		Integer[] indexes = comparator.createIndexArray();   //this contains indexes of sorted distances
//		Arrays.sort(indexes, comparator);
//		
//		double thresh = probOverallArray[indexes[0]] + unknownThreshAmplifyFactor;
		
		
		double minLogDensityOverall = logDensityOverallList.stream().min(Double::compare).get();
		double thresh = minLogDensityOverall + Math.log(unknownThreshCoeff) - 0.00001;  //Minus 0.00001 to avoid the threshold from being larger than minLogDensityOverall when unknownThreshAmplifyFactor=1.0 due to numerical inaccuracy
		
//		long afterLearningMsec = System.currentTimeMillis();
		
//		AISPLogger.logger.info("Feature extraction: " + (afterFeaturesMsec - startMsec) + ", training: " + (afterLearningMsec - afterFeaturesMsec));
//		AISPLogger.logger.info("Done training");
		return new FixedGMMClassifier(primaryTrainingLabel, featureGramDescriptors,
				listOfLabelValues, listOfModels, thresh);
	}

	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return "GMMClassifier [primaryTrainingLabel=" + primaryTrainingLabel 
				+ ", trainingWindowTransform=" + this.trainingWindowTransform 
				+ ", featureGramDescriptors=" + (featureGramDescriptors != null
						? featureGramDescriptors.subList(0, Math.min(featureGramDescriptors.size(), maxLen)) : null)
				+ ", numGaussiansToMix=" + numGaussiansToMix + ", diagonalCovariance=" + diagonalCovariance
				+ ", unknownThreshCoeff=" + unknownThreshCoeff + ", listOfLabelValues="
				+ (listOfLabelValues != null ? listOfLabelValues.subList(0, Math.min(listOfLabelValues.size(), maxLen))
						: null)
				+ ", listOfModels="
				+ (listOfModels != null ? listOfModels.subList(0, Math.min(listOfModels.size(), maxLen)) : null) + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (diagonalCovariance ? 1231 : 1237);
		long temp;
		temp = Double.doubleToLongBits(unknownThreshCoeff);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((listOfLabelValues == null) ? 0 : listOfLabelValues.hashCode());
		result = prime * result + ((listOfModels == null) ? 0 : listOfModels.hashCode());
		result = prime * result + numGaussiansToMix;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof GMMClassifier))
			return false;
		GMMClassifier other = (GMMClassifier) obj;
		if (diagonalCovariance != other.diagonalCovariance)
			return false;
		if (Double.doubleToLongBits(unknownThreshCoeff) != Double.doubleToLongBits(other.unknownThreshCoeff))
			return false;
		if (listOfLabelValues == null) {
			if (other.listOfLabelValues != null)
				return false;
		} else if (!listOfLabelValues.equals(other.listOfLabelValues))
			return false;
		if (listOfModels == null) {
			if (other.listOfModels != null)
				return false;
		} else if (!listOfModels.equals(other.listOfModels))
			return false;
		if (numGaussiansToMix != other.numGaussiansToMix)
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
