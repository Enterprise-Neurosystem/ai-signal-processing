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
package org.eng.aisp.util;

/**
 * An of true/false positives/negatives to provide error metrics on a binary classifier. 
 * Metric definitions taken from <a href="https://en.wikipedia.org/wiki/Precision_and_recall">https://en.wikipedia.org/wiki/Precision_and_recall.</a>
 * @author eduardo.morales
 *
 */
public class BinaryConfusionMatrix {
	protected final double truePositive;
	protected final double falsePositive;
	protected final double falseNegative;
	protected final double trueNegative;
	protected final double total;
	
	/**
	 * Constructor for the Error Matrix, all values are not weighted.
	 * @param truePositive
	 * @param trueNegative
	 * @param falsePositive
	 * @param falseNegative
	 */
	public BinaryConfusionMatrix(int truePositive, int trueNegative, int falsePositive, int falseNegative){
		this.truePositive = truePositive;
		this.trueNegative = trueNegative;
		this.falsePositive = falsePositive;
		this.falseNegative = falseNegative;
		this.total = this.truePositive + this.trueNegative + this.falsePositive + this.falseNegative;
	}
	

	/**
	 * Getter for the truePositive
	 * @return truePositive
	 */
	public int getTruePositive(){
		return (int)this.truePositive;
	}
	
	public int getFalseNegative(){
		return (int)this.falseNegative;
	}
	
	/**
	 * Get the precision.
	 * <br>
	 * tp / (tp + fp)
	 * @return number in [0..1]
	 */
	public double getPrecision(){
		double divisor = this.truePositive + falsePositive;
		if (divisor == 0)
			return 0;
		else
			return this.truePositive / divisor;
	}
	
	/**
	 * Getter for the recall.
	 * <br>
	 * tp / (tp + fn)
	 * @return number in [0..1]
	 */
	public double getRecall(){
		double divisor = this.truePositive + falseNegative;
		if (divisor == 0)
			return 0;
		else
			return  this.truePositive / divisor; 
	}


	public double getTotal() {
		return this.total;
	}
	
	public double getAccuracy() {
		return (truePositive + trueNegative) / total;
	}
	
	/**
	 * Get the number of samples that were truly positive.
	 */
	public int getPositives() {
		return (int)(truePositive + falseNegative);
	}

	/**
	 * Get the number of samples that were truly negative.
	 */
	public int getNegatives() {
		return (int)(trueNegative + falsePositive);
	}
	
	/**
	 * Get the average of the true positive and true negative rates.
	 * @return
	 */
	public double getBalancedAccuracy() {
		return (this.getTrueNegativeRate() + this.getTruePositiveRate()) / 2.0;
	}
	
	/**
	 * Get the ratio of predicted negatives to actual positives. 
	 * <br>
	 * fn  / (fn + tp). 
	 * @return a number in [0..1].
	 */
	public double getFalseNegativeRate() {
		return falseNegative / getPositives(); 
	}
	
	/**
	 * Get the ratio of predicted positive to actual negatives. 
	 * <br>
	 * fp  / (fp + tn). 
	 * @return a number in [0..1].
	 */
	public double getFalsePositiveRate() {
		return falsePositive / getNegatives(); 
	}

	/**
	 * An alias for {@link #getRecall()}.
	 * @return
	 */
	public double getTruePositiveRate() {
		return this.getRecall();
	}

	/**
	 * Get the percentage of true negatives relative to the actual number of negatives.
	 * <br>
	 * tn / (tn + fp). 
	 * @return nubmer in [0..1]
	 */
	public double getTrueNegativeRate() {
		return trueNegative / getNegatives(); 
	}
	
	/**
	 * Get the ratio of predicted negatives to those that were predicteds as negative.
	 * <br>
	 * tn / (tn + fn). 
	 * @return a number in [0..].
	 */
	public double getNegativePredictiveValue() {
		return trueNegative / (trueNegative + falseNegative);
	}
	
	
	/**
	 * Turns the Error Matrix into a String
	 * @return errorMatrixAsString
	 */
	public String toString(){
    String errorMatrixAsString =
    	"TruePositive-->  " + this.truePositive +
    	"\nTrueNegative-->  " + this.trueNegative + 
    	"\nFalsePositive--> " + this.falsePositive +
    	"\nFalseNegative--> " + this.falseNegative;
    	
    	return errorMatrixAsString;
	}


	public double getF1Score() {
		return getFScore(1);
	}

	public double getFScore(double beta) {
		double p = getPrecision();
		double r = getRecall();
		if (p == 0 && r== 0)
			return 0;
		double b2 = beta * beta;
		return (1 + b2) * p * r / ( b2 * p + r); 
	}
}