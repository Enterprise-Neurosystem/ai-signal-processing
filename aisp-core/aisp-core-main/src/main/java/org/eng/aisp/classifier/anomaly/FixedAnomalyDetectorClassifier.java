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
import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.AbstractFixedFeatureExtractingClassifier;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.classifier.anomaly.IAnomalyDetector.AnomalyResult;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.IFeatureGramDescriptor;

/**
 * Provides a classifier that uses 1 or more feature-gram-based anomaly detectors and a configurable minimum to declare anomalies. 
 * The classifier can be configured to update the feature gram anomaly detectors with the data being classified.
 * <p>
 * The classifier supports training of the anomaly detectors during deployment, without any previous training.
 * To enable, instantiate the classifier with an IAnomalyDetectorBuilder and a non-zero time to learn the initial
 * normal environment. 
 * <p>
 * Per loose contract, the classifier will output two classification label values.  The first is
 * the trained label with the values of 'normal' or 'abnormal'.  The second is a label name of 'abnormal' always
 * having a value of 'true' and a confidence score that can be considered an <i>anomaly score</i> (a value between
 * 0 and 1 with 1 being very confident that it is an anomaly).  The anomaly score may be useful when a continuous
 * anomaly indication is preferred over the the binary classification value in the trained label.
 */
public class FixedAnomalyDetectorClassifier extends AbstractFixedFeatureExtractingClassifier<double[], double[]> implements IFixedClassifier<double[]> {
	
	private static final long serialVersionUID = -8834181879856994629L;
	public static String ABNORMAL_LABEL_VALUE = "abnormal";
	public static String NORMAL_LABEL_VALUE = "normal";
	
	public enum UpdateMode {
		NONE,
		NORMAL_ONLY,
		ALL
	}
	
	private IAnomalyDetector<IFeatureGram<double[]>>[] featureGramAnomalyDetectors;

	private final int minFeatureVotes;
	private final String labelName;
	private final UpdateMode updateMode;
	private final long nextAtTime;
	private final long learnNormalEnvUntilTime;
	
	private transient boolean deploymentStarted = false;
	private transient long currentNextAtTime = 0;
	private IFeatureGramAnomalyDetectorBuilder featureGramAnomalyDetectorBuilder;
	
	/**
	 * Creates the classifier from pre-trained anomaly detectors, generally called from an AnomalyDetectorClassifier train() method.
	 * @param labelName
	 * @param fgeList
	 * @param featureAnomalyDetectors
	 * @param nextAtTime
	 * @param votePercent
	 * @param updateMode
	 */
	public FixedAnomalyDetectorClassifier(String labelName, List<IFeatureGramDescriptor<double[],double[]>> fgeList, IAnomalyDetector<IFeatureGram<double[]>>[] featureAnomalyDetectors, 
						long nextAtTime, double votePercent, UpdateMode updateMode) {
			this(labelName, fgeList, featureAnomalyDetectors, null, nextAtTime, votePercent, updateMode, 0); 
	}



	/**
	 * Creates the classifier to learn the environment w/o initially trained IAnomalyDetector instances.
	 * @param labelName the label name to emit from calls to {@link #classify(org.eng.aisp.IDataWindow)}.
	 * @param fgeList the list of feature gram extractors.
	 * @param featureAnomalyDetectorBuilder a builder to create the IAnomalyDetector instances, one for each feature grame.
	 * @param votePercent
	 * @param updateMode
	 * @param samplesToLearnNormalEnv
	 */
	public FixedAnomalyDetectorClassifier(String labelName, List<IFeatureGramDescriptor<double[],double[]>> fgeList, IAnomalyDetectorBuilder<Double> featureAnomalyDetectorBuilder, 
						double votePercent, UpdateMode updateMode, int samplesToLearnNormalEnv) {
			this(labelName, fgeList, 
					null, 	// No detectors defined yet.
					new FeatureGramAnomalyDetectorBuilder(featureAnomalyDetectorBuilder, votePercent, votePercent),
					0, votePercent, updateMode, samplesToLearnNormalEnv
				); 
	}

	private FixedAnomalyDetectorClassifier(String labelName, List<IFeatureGramDescriptor<double[],double[]>> fgeList, IAnomalyDetector<IFeatureGram<double[]>>[] featureGramAnomalyDetectors, 
			IFeatureGramAnomalyDetectorBuilder featureGramAnomalyDetectorBuilder,
			long nextAtTime, double votePercent, UpdateMode updateMode, int samplesToLearnNormalEnv) {
		super(fgeList);
		if (votePercent <= 0)
			throw new IllegalArgumentException("Vote percentage must be larger than 0");
		if (votePercent > 1) 
			throw new IllegalArgumentException("Vote percentage must be less or equal to 1");
		if (featureGramAnomalyDetectors != null && featureGramAnomalyDetectorBuilder != null) 
			throw new IllegalArgumentException("Only 1 of detector or detector builds can be non-null");
		if (featureGramAnomalyDetectorBuilder != null && samplesToLearnNormalEnv <= 0) 
			throw new IllegalArgumentException("When a detector builder is provided, samplesToLearnNormalEnv must be larger than 0");

		this.labelName = labelName;
		this.featureGramAnomalyDetectors = featureGramAnomalyDetectors;		// Pre-trained if not null.
		this.featureGramAnomalyDetectorBuilder = featureGramAnomalyDetectorBuilder;	// Online learning if not null.
		this.nextAtTime = nextAtTime;
		this.minFeatureVotes = (int)Math.max(1, Math.round(fgeList.size() * votePercent));
		this.updateMode = updateMode;
		this.learnNormalEnvUntilTime = samplesToLearnNormalEnv > 0 ? nextAtTime + samplesToLearnNormalEnv : 0;
	}

	@Override
	public String getTrainedLabel() {
		return this.labelName;
	}

	/**
	 * Classify the given set of feature grams as anomalous or not.
	 * Each feature gram is given to a corresponding anomaly detector and if a threshold of feature grams are declared
	 * as anomalous then an anomaly is declared for the set.
	 * If the updateMode is ALL or NORMAL_ONLY and the featuregram was declared an anomaly, it is updated into the 
	 * feature gram's anomaly detector model using the value anomaly status returned by the detectors isAnomaly() method.
	 * The returned list of classifications includes values for both the trained label and one for the {@value #ABNORMAL_LABEL_VALUE}.
	 * The latter's value is always 'true' so that its confidence can be considered an <i>anomaly score</i>.
	 */
	@Override
	protected List<Classification> classify(IFeatureGram<double[]>[] features) throws AISPException {
		if (features.length == 0)
			throw new AISPException("Got unexpected zero length features array");

		// If this is the first call to this method since creation or deserialization, 
		// then notify the detectors that this is a new deployment.
		if (!this.deploymentStarted) {
			deploymentStarted = true;
			reset();
		}

		int votes = 0;
		double abnormalConfSum = 0;
		double normalConfSum = 0;
		int featureCount = features.length;
		boolean doUpdate;
		boolean learningEnvironment = learnNormalEnvUntilTime > 0 && currentNextAtTime <= learnNormalEnvUntilTime;
		for (int i=0 ; i<featureCount; i++) {	// Over each feature gram
			IFeatureGram<double[]> fg = features[i]; 
			if (featureGramAnomalyDetectors[i] == null) {
				int featureLen = fg.getFeatures()[0].getData().length;
				featureGramAnomalyDetectors[i] = featureGramAnomalyDetectorBuilder.build(featureLen);
			}
			boolean isAnomaly;
			if (learningEnvironment) {
				// We're learning the environment as normal, so declare everything as normal and always update the detector.
				// This learning is considered "offline", even though we are deployed.
				normalConfSum += 1;
				isAnomaly = false;
				doUpdate = true;
			} else {
				AnomalyResult ar = this.featureGramAnomalyDetectors[i].isAnomaly(currentNextAtTime, fg); 
				abnormalConfSum += ar.getAnomalyConfidence();
				normalConfSum += ar.getNormalConfidence();
				isAnomaly = ar.isAnomaly(); 
				// Update the detector with this sample if requested.
				doUpdate = this.updateMode.equals(UpdateMode.ALL) || (!isAnomaly && this.updateMode.equals(UpdateMode.NORMAL_ONLY));
			}
			if (isAnomaly) 
				votes++;
			if (doUpdate)
				this.featureGramAnomalyDetectors[i].update(learningEnvironment, !isAnomaly, currentNextAtTime, fg);	
		}

		this.currentNextAtTime++;

		List<Classification> clist = createClassificationResult(votes, featureCount, abnormalConfSum, normalConfSum);
		return clist;
	}

	/**
	 * @param votes
	 * @param featureCount
	 * @param abnormalConfSum
	 * @param normalConfSum
	 * @return
	 */
	private List<Classification> createClassificationResult(int votes, int featureCount, double abnormalConfSum,
			double normalConfSum) {
		double normalConfidence = normalConfSum / featureCount;
		double abnormalConfidence = abnormalConfSum / featureCount;

		// Voting seems to be better than comparing normal and abnormal confidence. - dawood 4/2021
		boolean isAnomaly = votes >= this.minFeatureVotes; 
		String labelValue;
		
		double conf;
		if (isAnomaly) {		
			labelValue = ABNORMAL_LABEL_VALUE;
			conf = abnormalConfidence;
		} else {
			labelValue = NORMAL_LABEL_VALUE; 
			conf = normalConfidence;
		}
//		AISPLogger.logger.info(this.trainingLabel + "=" + labelValue + ", conf=" + conf + ", abconf=" + abnormalConfidence);
		List<Classification> clist = new ArrayList<Classification>();
		Classification c = new Classification(this.labelName, labelValue, conf);
		clist.add(c);
		c = new Classification(ABNORMAL_LABEL_VALUE, "true", abnormalConfidence);	
		clist.add(c);
		return clist;
	}

	/**
	 * Return the instance to is pre-deployment state.
	 * For now, this may only be exposed for testing purposes.
	 */
	protected void reset() {
		currentNextAtTime = this.nextAtTime;
		if (this.featureGramAnomalyDetectorBuilder != null) {	// Create brand new detectors since we don't have any pre-trained ones.
			this.featureGramAnomalyDetectors = new IAnomalyDetector[this.featureGramDescriptors.size()]; 
		} else {
			for (int i=0 ; i<featureGramAnomalyDetectors.length ; i++) {	// Over each feature gram
				this.featureGramAnomalyDetectors[i].beginNewDeployment();
			}
		}
	}

}
