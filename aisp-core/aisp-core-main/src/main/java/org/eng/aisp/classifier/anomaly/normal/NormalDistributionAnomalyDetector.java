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

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.anomaly.IAnomalyDetector;
import org.eng.util.LinearDistributionTransform;
import org.eng.util.OnlineStats;

public class NormalDistributionAnomalyDetector implements IAnomalyDetector<Double> {

	private static final long serialVersionUID = -6759038743991400184L;
	protected final OnlineStats offlineNormalStats = new OnlineStats();
	protected final OnlineStats offlineAbnormalStats= new OnlineStats();
	private final double normalStddevMultiplier;
	private int samplesToAdaptToEnvironment; 
	private boolean isDirty = true;
	
	private transient double threshold;
	private transient RealDistribution distributionOfOnlineNormals;
	private transient RealDistribution distributionOfOnlineAbnormals;
	protected transient OnlineStats onlineNormalStats;
	protected transient int normalOnlineUpdates = 0;
	
	public NormalDistributionAnomalyDetector(int samplesToAdaptToEnvironment, double normalStddevMultiplier) {
		if (samplesToAdaptToEnvironment >= 0 && samplesToAdaptToEnvironment <= 1) 
			throw new IllegalArgumentException("samplesToAdaptToEnvironment must be larger than 1 to be able to compute a variance, or negative to disable learning new environments.");
		if (normalStddevMultiplier <= 0) 
			throw new IllegalArgumentException("normalStddevMultiplier must be larger than 0"); 
		this.normalStddevMultiplier = normalStddevMultiplier;
		this.samplesToAdaptToEnvironment = samplesToAdaptToEnvironment;
	}

	public NormalDistributionAnomalyDetector(double normalStddevMultiplier) {
		this(-1, normalStddevMultiplier);
	}

	@Override
	public AnomalyResult isAnomaly(long atTime, Double data) throws AISPException {
		// If learning a new envirnoment, we can't return abnormal until we received enough updates.
		if (this.normalOnlineUpdates < this.samplesToAdaptToEnvironment)
			return  new AnomalyResult(false, 0, 1);
		
		double datum = data.doubleValue();
		if (this.isDirty) {
			this.threshold = this.getThreshold();
			this.distributionOfOnlineNormals = null; 
			this.distributionOfOnlineAbnormals = null;
			this.isDirty = false;
		}
		datum = this.getPredeploymenValue(datum);
		
		boolean isAnomaly = this.isAnomaly(datum,this.threshold);
		double abnormalConf = this.getAbnormalConfidence(datum);
		double normalConf = this.getNormalConfidence(datum);


		return new AnomalyResult(isAnomaly, abnormalConf, normalConf);
	}

	@Override
	public void update(boolean isOfflineTraining, boolean isNormal, long atTime, Double data) throws AISPException {
		if (isOfflineTraining) {
			if (isNormal) {
				offlineNormalStats.addSample(data);
			} else {
				offlineAbnormalStats.addSample(data);
			}
		} else {
			this.normalOnlineUpdates++;
			if (isNormal && onlineNormalStats != null)
				onlineNormalStats.addSample(data);
		}
		this.isDirty = true;
	}
	
	/**
	 * Start recording the stats of the newly deployed normal values so we can adapt from our learned distributions to deployed distributions.
	 */
	@Override
	public void beginNewDeployment() {
		if (this.samplesToAdaptToEnvironment > 0)
			this.onlineNormalStats = new OnlineStats();
		normalOnlineUpdates = 0;
	}
	
	/**
	 * If we're adapting to the new environment and have enough samples, then transform from the learned distribution to the 
	 * deployed distribution.  Otherwise, return the untransformed magnitude.
	 * @param statsIndex
	 * @param mag
	 * @return
	 */
	private double getPredeploymenValue(double mag) {
		if (this.samplesToAdaptToEnvironment >= 0 
			&& this.normalOnlineUpdates >= this.samplesToAdaptToEnvironment
			&& this.onlineNormalStats != null 
			&& !Double.isNaN(onlineNormalStats.getStdDev()))  {
			 LinearDistributionTransform ldt = new LinearDistributionTransform(this.onlineNormalStats, offlineNormalStats); 
			 mag = ldt.transform(mag);
		}
		return mag;
	}


	private double getAbnormalConfidence(double mag) {
		double conf;
		if (this.offlineAbnormalStats.getSampleCount() == 0) {
			conf = 1 - getNormalConfidence(mag);
		} else {
			if (this.distributionOfOnlineAbnormals == null)
				this.distributionOfOnlineAbnormals = createNormalDistribution(offlineAbnormalStats); 
		    conf = getConfidence(this.distributionOfOnlineAbnormals, mag);
		}
		return conf; 
	}

	/**
	 * Get the confidence that the given value is with the given distribution.
	 * @param dist
	 * @param mag
	 * @return number from 0..1
	 */
	private static double getConfidence(RealDistribution dist, double mag) {
//		OnlineStats abnormStats = abnormalStats == null ?  null : abnormalStats[featureIndex];
//		double normMean = normStats.getMean();
//		double abnormMean = abnormStats == null ? Double.NaN : abnormStats.getMean(); 
		double conf, scale;
//		double stddevMultiplier = 4;
//		if (abnormStats == null)  {
			// We don't know anything about the abnormals, so anything plus or minus around the global median is abnormal.
			double distMean = dist.getNumericalMean();
//			double lowerBound = dist.getSupportLowerBound();
//			double upperBound = dist.getSupportUpperBound();
			if (mag < distMean)
//				conf = 0.5 - (dist.cumulativeProbability(distMean) - dist.cumulativeProbability(mag));
				conf = 0.5 - (dist.cumulativeProbability(distMean) - dist.cumulativeProbability(mag));
			else
				conf = 0.5 - (dist.cumulativeProbability(mag) - dist.cumulativeProbability(distMean));
			conf *= 2;
			
//			if (mag > normMean)
//				scale = (normMean + normStats.getStdDev() * stddevMultiplier);
//			else 	// TODO: the value produced here could be negative.  We should really use a gamma distribution in this case.
//				scale = (normMean - normStats.getStdDev() * stddevMultiplier);
//			conf = (Math.abs(mag - normMean)) / scale; 
			
//		} else if (abnormMean  > normMean)  {
//			// Abnormals have higher magnitude and are ABOVE the threshold.
//			scale = (normMean + normStats.getStdDev() * stddevMultiplier);
//			conf = (Math.abs(mag - normMean)) / scale; 
//		} else {
//			// Abnormals have lower magnitude and are BELOW the threshold.
//			 scale = (abnormMean + abnormStats.getStdDev() * stddevMultiplier);
//			conf = (Math.abs(mag - normMean)) / scale; 
//		}
		if (conf > 1)
			conf = 1;
		else if (conf < 0)
			conf = 0;	
		
//		AISPLogger.logger.info("mean=" + distMean + ", variance=" + dist.getNumericalVariance() + ",mag = " + mag + ", conf=" + conf);
		return conf;
	}

	/**
	 * Get a confidence value between 0 and 1.
	 * @param mag the signal magitude in the learned (not-deployed) distribution.
	 * @return
	 */
	private double getNormalConfidence(double mag) {
		if (this.distributionOfOnlineNormals ==  null)
			this.distributionOfOnlineNormals = createNormalDistribution(offlineNormalStats);
		return  getConfidence(distributionOfOnlineNormals, mag);
	}


	/**
	 * Get the threshold appropriate for calls to {@link #isAnomaly(double, double)}.
	 */
	private double getThreshold() {
			double threshold; 
			if (offlineAbnormalStats.getSampleCount() == 0) {
				threshold = normalStddevMultiplier * offlineNormalStats.getStdDev();
			} else {
				threshold = StatUtils.getEqualProbabiltyMidPoint(offlineNormalStats,offlineAbnormalStats);
//				threshold = normStats.getMaximum();	// Minimize false positive rate
//				threshold = abnormalStats[i].getMinimum();	// Maximize abnormal recall 
//				threshold = StatUtils.getEqualProbabiltyMidPoint(this.distributionOfNormals[i],this.distributionOfAbnormals[i]);
			}
		return threshold;
	}

	private static NormalDistribution createNormalDistribution(OnlineStats stats) {
		double mean = stats.getMean(), stddev = stats.getStdDev();
		return new NormalDistribution(mean,stddev);
	}

	/**
	 * Determine if the given value is an anomaly relative to the original training data distributions.
	 * @param featureIndex index of feature statistics/distribution against which the value will be judged. 
	 * @param mag a value in the same distribution space as the original training data.
	 * @param threshold for normals only, this is distane from he normal mean.  If we have abnormals, then it is
	 * an absolute value between the normal and abnormal means.
	 * @return true if the value represents an anomaly relative to the training statistics for the indexed feature.
	 */
	private boolean isAnomaly(double mag, double threshold) {
		boolean isAnomaly; 
		double normMean = offlineNormalStats.getMean();
		if (offlineAbnormalStats.getSampleCount() == 0)  {
			// We don't know anything about the abnormals, so anything plus or minus around the global median is abnormal.
//			double median = (normStats.getMinimum() + normStats.getMaximum()) / 2.0;
//			isAnomaly = (mag >= median + threshold) || (mag <= median - threshold);
//			isAnomaly = mag < normStats.getMinimum() || mag > normStats.getMaximum();
			isAnomaly = (mag >= normMean + threshold) || (mag <= normMean - threshold);
			
		} else if (offlineAbnormalStats.getMean() > normMean)  {
			// Abnormals have higher magnitude and are ABOVE the threshold.
			isAnomaly = mag >= threshold;
		} else {
			// Abnormals have lower magnitude and are BELOW the threshold.
			isAnomaly = mag <= threshold;
		}
		return isAnomaly;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isDirty ? 1231 : 1237);
		long temp;
		temp = Double.doubleToLongBits(normalStddevMultiplier);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((offlineAbnormalStats == null) ? 0 : offlineAbnormalStats.hashCode());
		result = prime * result + ((offlineNormalStats == null) ? 0 : offlineNormalStats.hashCode());
		result = prime * result + samplesToAdaptToEnvironment;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof NormalDistributionAnomalyDetector))
			return false;
		NormalDistributionAnomalyDetector other = (NormalDistributionAnomalyDetector) obj;
		if (isDirty != other.isDirty)
			return false;
		if (Double.doubleToLongBits(normalStddevMultiplier) != Double.doubleToLongBits(other.normalStddevMultiplier))
			return false;
		if (offlineAbnormalStats == null) {
			if (other.offlineAbnormalStats != null)
				return false;
		} else if (!offlineAbnormalStats.equals(other.offlineAbnormalStats))
			return false;
		if (offlineNormalStats == null) {
			if (other.offlineNormalStats != null)
				return false;
		} else if (!offlineNormalStats.equals(other.offlineNormalStats))
			return false;
		if (samplesToAdaptToEnvironment != other.samplesToAdaptToEnvironment)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "NormalDistributionAnomalyDetector [offlineNormalStats=" + offlineNormalStats + ", offlineAbnormalStats="
				+ offlineAbnormalStats + ", normalStddevMultiplier=" + normalStddevMultiplier
				+ ", samplesToLearnEnvironment=" + samplesToAdaptToEnvironment + ", isDirty=" + isDirty + "]";
	}


}
