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

import java.util.Arrays;

import org.eng.aisp.AISPException;

/**
 * Checks for a minimum percentage of anomalies within an array of doubles to determine if an anomaly is present.
 * Each element of the data array is assigned its own IAnomalyDetector<Double> according to the constructor. 
 * Alls calls to {@link #update(boolean, boolean, long, double[])} and {@link #isAnomaly(long, double[])} must
 * provide a data array the same length and of a length defined by the array of IAnomalyDetector instances
 * provided to the constructor.
 * 
 * @author DavidWood
 *
 */
public class MultidimensionalAnomalyDetector implements IAnomalyDetector<double[]>{

	
	private static final long serialVersionUID = 4758456690305720233L;
	protected final IAnomalyDetector<Double>[] detectors;
	protected final int minVotes;

	/**
	 * Defines the instance to act on array of doubles and only declare an anomaly when the given number
	 * of anomalies is found within the double array provided to {@link #isAnomaly(long, double[])}. 
	 * @param detectors defines the length of the array of doubles provided to {@link #isAnomaly(long, double[])} and 
	 * {@link #update(boolean, boolean, long, double[])} in 1:1 correspondence with the double arrays provided to those calls.
	 * @param minVotes the minimum number of anomalies declared within an array before an anomaly is declared on a given array.
	 * Must be larger than 0 and less then or equal to the number of detectors given here.
	 */
	public MultidimensionalAnomalyDetector(IAnomalyDetector<Double>[] detectors, int minVotes) {
		if (detectors == null || detectors.length == 0)
			throw new IllegalArgumentException("Detectors is null or empty");
		if (minVotes <= 0)
			throw new IllegalArgumentException("Minimum votes must be larger than 0");
		if (minVotes > detectors.length)
			throw new IllegalArgumentException("Minimum votes must be less than or equal to the number of detectors.");
		this.detectors = detectors;
		this.minVotes = minVotes;
	}

	/**
	 * A convenience on {@link #MultidimensionalAnomalyDetector(IAnomalyDetector[], int)} to create the detectors with the
	 * given builder.
	 * @param detectorBuilder instance to create the IAnomalyDetector<Double> instances for each data array element.
	 * @param dataLen the expect length of the arrays passed to {@link #isAnomaly(long, double[])} and 
	 * {@link #update(boolean, boolean, long, double[])}.
	 * @param minVotes
	 */
	public MultidimensionalAnomalyDetector(IAnomalyDetectorBuilder<Double> detectorBuilder, int dataLen, int minVotes) {
		this(createDetectors(detectorBuilder, dataLen), minVotes);
	}

	private static IAnomalyDetector<Double>[] createDetectors(IAnomalyDetectorBuilder<Double> detectorBuilder, int featureLen) {
		IAnomalyDetector<Double>[] detectorArray = new IAnomalyDetector[featureLen];
		for (int i=0 ; i<featureLen ; i++)
			detectorArray[i] = detectorBuilder.build();
		return detectorArray;
	}

	public AnomalyResult isAnomaly(long atTime, double[] data) throws AISPException {
		if (data.length != detectors.length)
			throw new IllegalArgumentException("Data array is not the same size as the anomaly detectors array");
		double votes = 0;
		double abnormalConf = 0;
		double normalConf = 0;
		// Go through each element to see if it is an anomaly.
		for (int i=0 ; i<data.length ; i++) {
			AnomalyResult ar = detectors[i].isAnomaly(atTime, data[i]);
			if (ar.isAnomaly)
				votes++;
			abnormalConf += ar.getAnomalyConfidence();
			normalConf += ar.getNormalConfidence();

		}
		// Accumulate the results.
		abnormalConf = abnormalConf / data.length; 
		if (abnormalConf > 1)
			abnormalConf= 1;
		normalConf = normalConf / data.length; 
		if (normalConf > 1)
			normalConf= 1;
		boolean isAnomaly = votes >= minVotes; 
		return new AnomalyResult(isAnomaly, abnormalConf, normalConf );
		
		
	}

	@Override
	public void update(boolean isOfflineTraining, boolean isNormal, long atTime, double[] data) throws AISPException {
		// Update each detector with its corresponding data element.
		for (int i=0 ; i<data.length ; i++) {
			detectors[i].update(isOfflineTraining, isNormal, atTime, data[i]);
		}
	}

	@Override
	public void beginNewDeployment() {
		// Let each detector know.
		for (int i=0 ; i<detectors.length ; i++) {
			detectors[i].beginNewDeployment();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(detectors);
		result = prime * result + minVotes;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof MultidimensionalAnomalyDetector))
			return false;
		MultidimensionalAnomalyDetector other = (MultidimensionalAnomalyDetector) obj;
		if (!Arrays.equals(detectors, other.detectors))
			return false;
		if (minVotes != other.minVotes)
			return false;
		return true;
	}
}
