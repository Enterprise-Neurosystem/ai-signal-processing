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
package org.eng.aisp.classifier.anomaly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.classifier.AbstractFixableFeatureExtractingClassifier;
import org.eng.aisp.classifier.ConfusionMatrix;
import org.eng.aisp.classifier.IFixableClassifier;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.classifier.KFoldModelEvaluator;
import org.eng.aisp.classifier.anomaly.FixedAnomalyDetectorClassifier.UpdateMode;
import org.eng.aisp.classifier.anomaly.normal.NormalDistributionAnomalyDetectorBuilder;
import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.IdentityFeatureGramDescriptor;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.FFTFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.feature.processor.vector.DeltaFeatureProcessor;
import org.eng.aisp.feature.processor.vector.L1MagnitudeFeatureProcessor;
import org.eng.aisp.feature.processor.vector.LpMagnitudeFeatureProcessor;
import org.eng.aisp.tools.GetModifiedSoundOptions;
import org.eng.aisp.transform.ITrainingWindowTransform;
import org.eng.util.CommandArgs;
import org.eng.util.IShuffleIterable;

/**
 * Uses a feature/spectrogram anomaly detector to provide binary (normal/abnormal) classification of samples. 
 * The IAnomalyDetector<IFeatureGram<double[]>> is currently implemented as multiple IAnomalyDetector<Double> instances,
 * one for each feature element of the feature gram through time.  
 * <p>
 * Per loose contract, the trained classifier will output two classification label values.  The first is
 * the trained label with the values of 'normal' or 'abnormal'.  The second is a label name of 'abnormal' always
 * having a value of 'true' and a confidence score that can be considered an <i>anomaly score</i> (a value between
 * 0 and 1 with 1 being very confident that it is an anomaly).  The anomaly score may be useful when a continuous
 * anomaly indication is preferred over the the binary classification value in the trained label.
 * @author DavidWood
 *
 */
public class AnomalyDetectorClassifier extends AbstractFixableFeatureExtractingClassifier<double[], double[]> implements IFixableClassifier<double[]> {

	
	private static final long serialVersionUID = 7344440141632581047L;

	public static final UpdateMode DEFAULT_UPDATE_MODE = UpdateMode.NONE;
	public static final double DEFAULT_VOTE_PERCENT = 0.50;
	/**
	 * Creates to  operate on the magnitude of the raw signal where magnitude is calculated using Lp distance with p=0.5.
	 */
	public final static IFeatureGramDescriptor<double[], double[]> DEFAULT_FEATURE_GRAM_EXTRACTOR = new IdentityFeatureGramDescriptor(new LpMagnitudeFeatureProcessor()); 

	private final double votePercent;
	private UpdateMode updateMode;

	protected IFeatureGramAnomalyDetectorBuilder detectorBuilder;
	
	public AnomalyDetectorClassifier(List<IFeatureGramDescriptor<double[], double[]>> featureGramExtractors, IAnomalyDetectorBuilder<Double> detectorBuilder, double votePercentage, UpdateMode updateMode) {
		this(null, featureGramExtractors, detectorBuilder, votePercentage, updateMode);
	}

	/**
	 * 
	 * @param transform
	 * @param featureGramDescriptors
	 * @param detectorBuilder creates IAnomalyDetector instances that are used on each element (or time sequence of elements)  of a feature vector.
	 * @param votePercentage the percentage of votes used across feature elements and feature grams required to declare an anomaly.
	 * @param updateMode  used during classification to determine if/which samples are added to the IAnomalyDetector instances.
	 */
	public AnomalyDetectorClassifier(ITrainingWindowTransform<double[]> transform, List<IFeatureGramDescriptor<double[], double[]>> featureGramExtractors,
			IAnomalyDetectorBuilder<Double> detectorBuilder, double votePercentage, UpdateMode updateMode) {
		super(false, transform, featureGramExtractors, true, false, false);
		this.detectorBuilder = new FeatureGramAnomalyDetectorBuilder(detectorBuilder, votePercentage, votePercentage);
		this.updateMode = updateMode;
		this.votePercent = votePercentage;
	}


	
	@Override
	protected IFixedClassifier<double[]> trainFixedClassifierOnFeatures( Iterable<? extends ILabeledFeatureGram<double[]>[]> features) throws AISPException {
		// Create the feature-gram based anomaly detectors, 1 for each element of the feature gram array.
		IAnomalyDetector<IFeatureGram<double[]>>[] featureAnomalyDetectors = createFeatureAnomalyDetectors(features); 	// One for each feature gram
		if (featureAnomalyDetectors == null)
			throw new AISPException("No data available for training.");
		
		// Ask each feature gram anomaly detector if the feature gram is an anomaly and keep track.
		long atTime = 1;
		int sampleCount = 0;
		for (ILabeledFeatureGram<double[]>[] featureArray : features) {	// Over each data sample
			for (int i=0 ; i<featureArray.length ; i++) {				// Over each feature gram computed for a given sample.
				Properties  labels = featureArray[i].getLabels();
				String labelValue = labels.getProperty(this.primaryTrainingLabel);

				if (labelValue == null)	// Skip data w/o the training label.
					continue;

				// Data is abnormal only if it has the abnormal label value.
				boolean isNormal = !labelValue.equals(FixedAnomalyDetectorClassifier.ABNORMAL_LABEL_VALUE);

				// Train spectrogram into the detector for this feature gram
				IAnomalyDetector<IFeatureGram<double[]>> detector= featureAnomalyDetectors[i];
				IFeatureGram<double[]> featureGram = featureArray[i].getFeatureGram();
				detector.update(true, isNormal, atTime, featureGram);
				if (i == 0)
					sampleCount++;
			}
			atTime++;	// TODO: assumes that samples are considered to be evenly spaced in time.
		}
		
		
		if (sampleCount == 0)
			throw new AISPException("No data available for training.");
		
//		AISPLogger.logger.info("Done training");

		return new FixedAnomalyDetectorClassifier(this.primaryTrainingLabel, this.featureGramDescriptors, featureAnomalyDetectors, 
				atTime, votePercent, updateMode);
	}

	/**
	 * Get a FeatureAnomalyDetector for each feature gram extractor.
	 * @param features
	 * @return null if no feature grams found.
	 */
	private IAnomalyDetector<IFeatureGram<double[]>>[]  createFeatureAnomalyDetectors( Iterable<? extends ILabeledFeatureGram<double[]>[]> features) {
		IAnomalyDetector<IFeatureGram<double[]>>[]  multiDetectors = null; 
		Iterator<? extends ILabeledFeatureGram<double[]>[]> iter = features.iterator();
		if (!iter.hasNext())
			return null;
		ILabeledFeatureGram<double[]>[] featureArray = iter.next(); 
		if (featureArray == null)
			return null;
		
		multiDetectors = new IAnomalyDetector[featureArray.length]; 
		for (int i=0 ; i<featureArray.length ; i++) {			// Over each feature gram
			int featureLen = featureArray[i].getFeatureGram().getFeatures()[0].getData().length;
			IAnomalyDetector<IFeatureGram<double[]>>  mdad = this.detectorBuilder.build(featureLen); 
			multiDetectors[i] = mdad;
		}

		return multiDetectors;
	}

	/**
	 * Forget all learning of the current environment and begin relearning on the next call to {@link #classify(org.eng.aisp.IDataWindow)}. 
	 */
	public void reset() {
		if (this.classifier != null) {
			((FixedAnomalyDetectorClassifier)this.classifier).reset();
		}
		
	}

	public static void main(String[] args) throws AISPException {
		CommandArgs cmdargs = new CommandArgs(args);
		GetModifiedSoundOptions soundOptions = new GetModifiedSoundOptions(false);
		if (!soundOptions.parseOptions(cmdargs)) {
			return;
		}
		IShuffleIterable<SoundRecording> srIter = soundOptions.getSounds();
		List<SoundRecording> sounds = new ArrayList<SoundRecording>();
		for (SoundRecording sr : srIter) {
			double[] data = sr.getDataWindow().getData();
			for (int i=0 ; i<data.length ; i++)
				data[i] += 1;	// make positive for BATS boxcox.
			sounds.add(sr);
		}

		String labelName = "state";
//		OnlineAnomalyDetector bestClassifier = search(labelName, sounds);
		
//		distFunc = new L1DistanceMergeKNNFunc(false);
////		distFunc = new EuclidianDistanceMergeKNNFunc(false);
		List<IFeatureGramDescriptor<double[],double[]>> fgeList = new ArrayList<IFeatureGramDescriptor<double[],double[]>>();
		IFeatureExtractor<double[],double[]> fe;
//		fe = new LogMelFeatureExtractor(0, 8, 500, 10000, 8);	
		fe = new FFTFeatureExtractor(0, 500, 10000, false, true, 32);	
		IFeatureProcessor<double[]> fp;
		fp = new DeltaFeatureProcessor(2,new double[] { 1,1,1});
		fp = new L1MagnitudeFeatureProcessor(false);
		fp = new L1MagnitudeFeatureProcessor(true);
		fp = new L1MagnitudeFeatureProcessor();
		fgeList.add(new FeatureGramDescriptor<double[],double[]>(92,92, fe, fp));
		IAnomalyDetectorBuilder builder = new NormalDistributionAnomalyDetectorBuilder(120, 3); 
		AnomalyDetectorClassifier classifier = new AnomalyDetectorClassifier(fgeList, builder, .50, UpdateMode.NONE);
		KFoldModelEvaluator kfold = new KFoldModelEvaluator(2,KFoldModelEvaluator.DEFAULT_SEED, false, true, false);
		ConfusionMatrix matrix = kfold.getConfusionMatrix(classifier, sounds, labelName, 2, 0);
		System.out.println("Classifer=" + classifier);
		System.out.println(matrix.formatCounts());
		System.out.println(matrix.formatPercents());
		System.out.println(matrix.formatStats());
		
//		bestClassifier.reset();
//		matrix = ConfusionMatrix.compute(labelName, bestClassifier, sounds );
//		System.out.println("Best Classifer=" + bestClassifier);
//		System.out.println(matrix.formatCounts());
//		System.out.println(matrix.formatPercents());
//		System.out.println(matrix.formatStats());
//		
//		System.out.println("Classifiers are equal: " + bestClassifier.equals(classifier));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((detectorBuilder == null) ? 0 : detectorBuilder.hashCode());
		result = prime * result + ((updateMode == null) ? 0 : updateMode.hashCode());
		long temp;
		temp = Double.doubleToLongBits(votePercent);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof AnomalyDetectorClassifier))
			return false;
		AnomalyDetectorClassifier other = (AnomalyDetectorClassifier) obj;
		if (detectorBuilder == null) {
			if (other.detectorBuilder != null)
				return false;
		} else if (!detectorBuilder.equals(other.detectorBuilder))
			return false;
		if (updateMode != other.updateMode)
			return false;
		if (Double.doubleToLongBits(votePercent) != Double.doubleToLongBits(other.votePercent))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final int maxLen = 8;
		return "AnomalyDetectorClassifier [votePercent=" + votePercent + ", updateMode=" + updateMode
				+ ", detectorBuilder=" + detectorBuilder + ", primaryTrainingLabel=" + primaryTrainingLabel
				+ ", featureGramDescriptors="
				+ (featureGramDescriptors != null ? toString(featureGramDescriptors, maxLen) : null) + ", classifier="
				+ classifier + ", preShuffleData=" + preShuffleData + ", tags="
				+ (tags != null ? toString(tags.entrySet(), maxLen) : null) + "]";
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
