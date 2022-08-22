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
package org.eng.aisp.classifier.anomaly.normal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.classifier.ConfusionMatrix;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.KFoldModelEvaluator;
import org.eng.aisp.classifier.anomaly.AnomalyDetectorClassifier;
import org.eng.aisp.classifier.anomaly.FixedAnomalyDetectorClassifier.UpdateMode;
import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.FFTFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.LogMelFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.feature.processor.vector.DeltaFeatureProcessor;
import org.eng.aisp.feature.processor.vector.L1MagnitudeFeatureProcessor;
import org.eng.aisp.feature.processor.vector.PipelinedFeatureProcessor;
import org.eng.aisp.tools.GetModifiedSoundOptions;
import org.eng.aisp.transform.ITrainingWindowTransform;
import org.eng.util.CommandArgs;
import org.eng.util.IShuffleIterable;

/**
 * Extends the super class to define the anomaly detector as NormalDistributionAnomalyDetector.
 * This classifier can train on normal and abnormal data or just normal data.  
 * A distribution is created for each feature element in the spectrogram (across time if more than on feature is present in a spectrogram).  
 * A single distribution can be used through the use of L1/LpMagnitudeFeatureProcessor, for example, but otherwise there are multiple distributions.
 * In the case of multiple distributions, a threshold percentage of distributions must declare an anomaly in order to generate an anomaly classification.
 * <p>
 * When training on both, each distribution learns the best single boundary between normal and abnormal values (be they larger or smaller than the other).
 * When training on only normal data, a multiplier on the normal standard deviation is used to set the threshold.
 * <p>
 * Through the NormalDistributionAnomalyDetector, this classifier is able to train on one distribution of
 * data and optionally adapt itself to a new environment where there may be gain change on the microphone or a
 * change in background noise.  This is disabled by default but can be enabled by setting the the number of
 * samples to learn the environment.
 * @author DavidWood
 *
 */
public class NormalDistributionAnomalyClassifier extends AnomalyDetectorClassifier {

	private static final long serialVersionUID = 6504056080094603785L;
	public static final double DEFAULT_NORMAL_STDDEV_MULTIPLIER = 3.0;
	public static final int DEFAULT_SAMPLES_TO_ADAPT_TO_ENV = -1;

	/**
	 * Uses all defaults defined on this and our super class, including not learning the environment.
	 */
	public NormalDistributionAnomalyClassifier() {
		this(AnomalyDetectorClassifier.DEFAULT_FEATURE_GRAM_EXTRACTOR, AnomalyDetectorClassifier.DEFAULT_VOTE_PERCENT, DEFAULT_NORMAL_STDDEV_MULTIPLIER, DEFAULT_SAMPLES_TO_ADAPT_TO_ENV);
	}

	/**
	 * Convenience on {@link #NormalDistributionAnomalyClassifier(ITrainingWindowTransform, List, double, double, boolean, int)} with no transform.
	 */
	public NormalDistributionAnomalyClassifier(IFeatureGramDescriptor<double[], double[]> featureGramExtractor,
			double votePercentage, double normalStddevMultiplier, int samplesToAdaptToEnvironment) {
		this(null, toFGEList(featureGramExtractor), votePercentage, normalStddevMultiplier, samplesToAdaptToEnvironment);
	}

	public NormalDistributionAnomalyClassifier(IFeatureGramDescriptor<double[], double[]> featureGramExtractor,
			double votePercentage, double normalStddevMultiplier) {
		this(null, toFGEList(featureGramExtractor), normalStddevMultiplier, votePercentage);
	}
	
	/**
	 * 
	 * @param transform
	 * @param featureGramExtractor
	 * @param votePercentage defines the percentage of features within  a featuregram and within a feature, that must declare an anomaly. 
	 * @param normalStddevMultiplier	if no abnormals are present in training data, then use this value times the normal standard deviation to define the threshold beyond
	 * with anomalies are declared.  See {@link NormalDistributionAnomalyDetectorBuilder}.
	 */
	public NormalDistributionAnomalyClassifier(ITrainingWindowTransform<double[]> transform,
			IFeatureGramDescriptor<double[], double[]> featureGramExtractor,
			double votePercentage, double normalStddevMultiplier) {
		super(transform, toFGEList(featureGramExtractor), new NormalDistributionAnomalyDetectorBuilder(normalStddevMultiplier), votePercentage, UpdateMode.NONE);
	}

	/**
	 * 
	 * @param transform
	 * @param featureGramDescriptors
	 * @param votePercentage defines the percentage of features within  a featuregram and within a feature, that must declare an anomaly. 
	 * @param normalStddevMultiplier	if no abnormals are present in training data, then use this value times the normal standard deviation to define the threshold beyond
	 * with anomalies are declared.  See {@link NormalDistributionAnomalyDetectorBuilder}.
	 * @param samplesToAdaptToEnvironment of 2 or larger defines the number of samples required before anomalies can be generated and over which the new environment 
	 * sample distribution is learned so as to be able to map back to the learned distribution.
	 * @param adaptDuringDeployment if true, then then {@link #classify(org.eng.aisp.IDataWindow)} will learn the environment and not generate an anomaly
	 * until sampleToLearnEnv classificaitons have been requested.
	 */
	public NormalDistributionAnomalyClassifier(ITrainingWindowTransform<double[]> transform, List<IFeatureGramDescriptor<double[], double[]>> featureGramExtractors,
			double votePercentage, double normalStddevMultiplier, int samplesToAdaptToEnvironment) {
		super(transform, featureGramExtractors, 
				new NormalDistributionAnomalyDetectorBuilder(samplesToAdaptToEnvironment, normalStddevMultiplier), 
				votePercentage, UpdateMode.NORMAL_ONLY);
	}	
	
	public NormalDistributionAnomalyClassifier(ITrainingWindowTransform<double[]> transform, List<IFeatureGramDescriptor<double[], double[]>> featureGramExtractors,
			double normalStddevMultiplier, double votePercentage) {
		super(transform, featureGramExtractors, 
				new NormalDistributionAnomalyDetectorBuilder(normalStddevMultiplier), 
				votePercentage, UpdateMode.NORMAL_ONLY);
	}	


	public static void main(String[] args) throws AISPException {
			CommandArgs cmdargs = new CommandArgs(args);
			GetModifiedSoundOptions soundOptions = new GetModifiedSoundOptions(false, true, false);
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
			IFeatureProcessor<double[]> fp, deltaFP;
			int config = 2;
			double votePercent;
			if (config == 1) {	// hardcrash 9/19
				fe = new LogMelFeatureExtractor(0, 128, 200, 11000, 0);	
				fp = new L1MagnitudeFeatureProcessor(true);
				votePercent = 0.50;	/// 9/22
				votePercent = 0.60;	/// 9/20
				votePercent = 0.65;	// 8/15
				votePercent = 0.6125;	// 9/20
				votePercent = 0.625;	// 9/15
			} else {
				 fe = new FFTFeatureExtractor(0, 20, 10000, false, true, 128);	
				deltaFP = new DeltaFeatureProcessor(2,new double[] { 0,1,0});
				fp = new L1MagnitudeFeatureProcessor(true);
				fp = new PipelinedFeatureProcessor(deltaFP, fp);
				fp = null;
				votePercent = .55;
			}
			IFeatureGramDescriptor<double[], double[]> fge = new FeatureGramDescriptor<double[],double[]>(92,92, fe, fp);
			fgeList.add(fge);
			IClassifier<double[]> classifier= new NormalDistributionAnomalyClassifier(null, fgeList, 3.0,  votePercent); 


//			classifier = new NormalDistributionAnomalyClassifier(null, fgeList, 3.0,  0.50, 120); 
			KFoldModelEvaluator kfold = new KFoldModelEvaluator(2,KFoldModelEvaluator.DEFAULT_SEED, true, true, false);
			ConfusionMatrix matrix = kfold.getConfusionMatrix(classifier, sounds, labelName, 2, 1633);
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
	public String toString() {
		final int maxLen = 8;
		return "NormalDistributionAnomalyClassifier [detectorBuilder=" + detectorBuilder + ", primaryTrainingLabel="
				+ primaryTrainingLabel + ", featureGramDescriptors="
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
