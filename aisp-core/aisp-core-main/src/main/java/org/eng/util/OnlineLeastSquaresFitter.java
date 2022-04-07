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
/**
 * 
 */
package org.eng.util;

import java.io.Serializable;


/**
 * Provides some shared function for various linear regression models.
 * 
 * See http://phoenix.phys.clemson.edu/tutorials/excel/regression.html. 
 * @author dawood
 *
 */
public class OnlineLeastSquaresFitter  implements Serializable { // , Cloneable  {

	private static final long serialVersionUID = -8142057594582182659L;
	private double sumX;
	private double sumY;
	private double sumXY;
	private double sumX2;
	/** The total number of values, valid or not, provided to updateModel() */
	private int 	sampleCount = 0;
	/** The number of valid samples provided to updateMode() */
	private int validSamples = 0;
	/** 
	 * The total number of valid samples used in the model.  This is the same as validSamples if 
	 * not using history.  Otherwise it will max out at the length of the history.
	 */
	private int		usedSampleCount = 0;
	private CircularBuffer xHistory = null;
	private CircularBuffer yHistory = null;
	/** Before referencing this directly clear the isDirty bit by calling {@link #computeModelParams()} */
	private double yIntercept;
	/** Before referencing this directly clear the isDirty bit by calling {@link #computeModelParams()} */
	private double slope;
	private boolean isDirty = true;

	/** We try and reset the x values to a 0-based system to avoid loss of precision when x is milliseconds since the epoch */
	private double firstX;	

	public OnlineLeastSquaresFitter() {
		this(0);
	}
	
	/**
	 * Create the fitter with optional limited history length.
	 * @param historyLength if larger than 0, then limit the amount of data used to fit the line
	 * to the given number.
	 */
	public OnlineLeastSquaresFitter(int historyLength) {
		if (historyLength > 0) {
			xHistory = new CircularBuffer(historyLength,0);
			yHistory = new CircularBuffer(historyLength,0);
		}
		resetModel();
	}
	
	public void resetModel() {
		sampleCount = 0;
		validSamples = 0;
		usedSampleCount = 0;
		sumX = 0;
		sumY = 0 ;
		sumXY = 0;
		sumX2 = 0;
		isDirty = true;
		this.firstX = 0;
		if (xHistory != null) {
			xHistory.clear();
			yHistory.clear();
		}
	}
	
	public void updateModel(long[] x, double[] y) {
		
		for (int i=0 ; i < y.length ; i++) 
			updateModel(x[i],y[i]);
	}
	
	public void updateModel(double[] x, double[] y) {
		
		for (int i=0 ; i < y.length ; i++) 
			updateModel(x[i],y[i]);
	}

	
	public void updateModel(double x, double y) {
		if (this.usedSampleCount == 0)
			firstX = x;

		x = x - firstX;
		
		if (!Double.isNaN(y) && !Double.isInfinite(y)) {
			sumX += x;
			sumY += y;
			sumXY += x * y;
			sumX2 += x * x;

			// If we're keeping history, then remove the old values and add in the new values.
			if (xHistory != null) {
				int index = validSamples - xHistory.getLength();
				if (index >= 0) {	// If we have filled the buffer, then we subtract older values.
					long oldX = (long)xHistory.getValue(index);
					double oldY = yHistory.getValue(index);
					sumX -= oldX;
					sumY -= oldY;
					sumXY -= oldX * oldY;
					sumX2 -= oldX * oldX;
					usedSampleCount = xHistory.getLength();
				} else {
					usedSampleCount++;
				}
				// Save these new values.
				xHistory.setValue(validSamples, x);
				yHistory.setValue(validSamples, y);
			} else {
				usedSampleCount++;
			}
			validSamples++;
		}

		sampleCount += 1;
		
		isDirty = true;
	}

	/**
	 * compute the slope and intercept from the intermediate values.
	 */
	private void computeModelParams() {
		if (isDirty && usedSampleCount > 1) {
			double numerator =(usedSampleCount * sumXY - sumX * sumY); 
			double denominator = (usedSampleCount * sumX2 - sumX * sumX);
			if (numerator == 0) {
				slope = 0;
			} else if (denominator == 0) {
//				BigDecimal sumX2BD = new BigDecimal(sumX2);
//				BigDecimal sumXBD = new BigDecimal(sumX);
//				BigDecimal t1 = sumX2BD.multiply(new BigDecimal(usedSampleCount));
//				BigDecimal t2 = sumXBD.multiply(sumXBD);
//				t1 = t1.subtract(t2);
//				denominator = t1.doubleValue();
//				if (denominator == 0)
					throw new IllegalArgumentException("Can not compute slope. Possible loss of precision in x values.");
			} else {
				slope = numerator / denominator; 
			}
			yIntercept = (sumY - slope * sumX) / usedSampleCount - firstX*slope;
			isDirty = false;
		}
	}


	public double valueAt(long at) {
		if (!this.isInitialized())
			return Double.NaN;
		if (isDirty)
			computeModelParams();
		double y = yIntercept + slope * at;
		return y;
	}


	public int getSampleCount() {
		return sampleCount > 0 ? sampleCount : -1;
	}


	public boolean isInitialized() {
		return usedSampleCount > 1;
	}

	
	/**
	 * If we don't have enough samples or the model is not otherwise initialized, 
	 * then we don't have a slope.  
	 */
	public double getSlope() { 
		if (!isInitialized())
			return Double.NaN;
		if (isDirty)
			computeModelParams();
		return slope; 
	}
	
	/**
	 * If we don't have enough samples or the model is not otherwise initialized, 
	 * then we don't have an intercept.  
	 */
	public double getIntercept() { 
		if (!isInitialized())
			return Double.NaN;
		if (isDirty)
			computeModelParams();
		return yIntercept; 
	}

	@Override
	public String toString() {
		if (isDirty)
			computeModelParams();
		return "LeastSquaresFitter [yIntercept=" + yIntercept + ", slope=" + slope + "]";
	}

//	@Override
//	public OnlineLeastSquaresFitter clone() {
//		OnlineLeastSquaresFitter other;
//		try {
//			other = (OnlineLeastSquaresFitter) super.clone();
//		} catch (CloneNotSupportedException e) {
//			// never get here?
//			e.printStackTrace();
//			return null;
//		}
//		if (this.xHistory != null)
//			other.xHistory = this.xHistory.clone();
//		if (this.yHistory != null)
//			other.yHistory = this.yHistory.clone();
//		return other;
//	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isDirty ? 1231 : 1237);
		result = prime * result + sampleCount;
		long temp;
		temp = Double.doubleToLongBits(slope);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(sumX);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(sumX2);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(sumXY);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(sumY);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + usedSampleCount;
		result = prime * result + validSamples;
		result = prime * result + ((xHistory == null) ? 0 : xHistory.hashCode());
		result = prime * result + ((yHistory == null) ? 0 : yHistory.hashCode());
		temp = Double.doubleToLongBits(yIntercept);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OnlineLeastSquaresFitter other = (OnlineLeastSquaresFitter) obj;
		if (isDirty != other.isDirty)
			return false;
		if (sampleCount != other.sampleCount)
			return false;
		if (Double.doubleToLongBits(slope) != Double.doubleToLongBits(other.slope))
			return false;
		if (Double.doubleToLongBits(sumX) != Double.doubleToLongBits(other.sumX))
			return false;
		if (Double.doubleToLongBits(sumX2) != Double.doubleToLongBits(other.sumX2))
			return false;
		if (Double.doubleToLongBits(sumXY) != Double.doubleToLongBits(other.sumXY))
			return false;
		if (Double.doubleToLongBits(sumY) != Double.doubleToLongBits(other.sumY))
			return false;
		if (usedSampleCount != other.usedSampleCount)
			return false;
		if (validSamples != other.validSamples)
			return false;
		if (xHistory == null) {
			if (other.xHistory != null)
				return false;
		} else if (!xHistory.equals(other.xHistory))
			return false;
		if (yHistory == null) {
			if (other.yHistory != null)
				return false;
		} else if (!yHistory.equals(other.yHistory))
			return false;
		if (Double.doubleToLongBits(yIntercept) != Double.doubleToLongBits(other.yIntercept))
			return false;
		return true;
	}	

}
