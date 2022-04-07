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

import java.io.Serializable;

import org.eng.aisp.AISPException;

/**
 * Defines an anomaly detector that can be updated with new values and determine if a given sample is anomalous based on passed updates.
 * This is generally to be used in conjunction with classifier during calls to its train() and/or classify() methods. 
 * 
 * @param <DATA>
 */
public interface IAnomalyDetector<DATA> extends Serializable {

	/**
	 * Captures data returned by {@link IAnomalyDetector#isAnomaly(long, Object)}.
	 */
	public static class AnomalyResult {
		protected final boolean isAnomaly;
		protected final double anomalyConfidence;
		protected final double normalConfidence;
		/**
		 * @param isAnomaly
		 * @param anomalyConfidence
		 */
		public AnomalyResult(boolean isAnomaly, double anomalyConfidence, double normalConfidence) {
			this.isAnomaly = isAnomaly;
			this.anomalyConfidence = anomalyConfidence;
			this.normalConfidence = normalConfidence;
		}
		/**
		 * @return the isAnomaly
		 */
		public boolean isAnomaly() {
			return isAnomaly;
		}
		/**
		 * @return the anomalyConfidence
		 */
		public double getAnomalyConfidence() {
			return anomalyConfidence;
		}

		public double getNormalConfidence() {
			return normalConfidence;
		}
		

	}

	/**
	 * Determines if the given data at the given time is anomalous.
	 * @param atTime the time with which the data is associated.  Some implementations may choose to ignore this, but it can generally
	 * be assumed that the given time is later than any times provided to update(). 
	 * @param data the data to be checked for anomalous status
	 * @return an indication of whether the piece of data is anomalous and confidence values.
	 * @throws AISPException
	 */
	public AnomalyResult isAnomaly(long atTime, DATA data) throws AISPException;

	/**
	 * Make new data readings available to the detector.  This is generally called during initial training of the detector
	 * and may be call during operation when {@link #isAnomaly(long, Object)} is being called.
	 * @param isOfflineTraining true if called prior to making any calls to {@link #isAnomaly(long, Object)}.
	 * @param isNormal true if the given sample is known to be normal, otherwise the given sample is abnormal/anomalous.
	 * @param atTime the time associated with the given data. This is generally increasing and any value should be larger than
	 * the value provided to any previous calls to this instance.  Note that when isOfflineTraining is false, {@link #isAnomaly(long, Object)}
	 * may have been called prior to call this method with the same value of this parameter. 
	 * @param data the data to incorporate into the model as either normal or abnormal/anomalous.
	 * @throws AISPException
	 */
	public void update(boolean isOfflineTraining, boolean isNormal, long atTime, DATA data) throws AISPException;
	
	/**
	 * Signal the anomaly detector that it is being deployed in a new environment so that it may try and adapt to this new environment.
	 * This should restore the instance to the state prior to have been updated with online data through the {@link #update(boolean, boolean, long, Object)} call.
	 */
	public void beginNewDeployment();

}
