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

import java.io.Serializable;

class SampleCounter implements Serializable { 

	private static final long serialVersionUID = -924021939146204773L;
	protected int totalSamples = 0;
	protected double totalMsec = 0;

	SampleCounter() {
		this(0,0);
	}
	
	SampleCounter(int totalSamples, double totalMsec) {
		this.totalSamples = totalSamples;
		this.totalMsec = totalMsec;
	}
	/**
	 * @return the totalSamples
	 */
	public int getTotalSamples() {
		return totalSamples;
	}
	/**
	 * @return the totalMsec
	 */
	public double getTotalMsec() {
		return totalMsec;
	}
	
	protected void appendSample(double durationMsec) {
		totalSamples++;
		totalMsec += durationMsec;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(totalMsec);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + totalSamples;
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SampleCounter))
			return false;
		SampleCounter other = (SampleCounter) obj;
		if (Double.doubleToLongBits(totalMsec) != Double.doubleToLongBits(other.totalMsec))
			return false;
		if (totalSamples != other.totalSamples)
			return false;
		return true;
	}
	
	
}
