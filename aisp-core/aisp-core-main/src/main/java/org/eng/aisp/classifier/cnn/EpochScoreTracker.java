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
package org.eng.aisp.classifier.cnn;

import java.util.LinkedList;

import org.eng.aisp.AISPLogger;
import org.eng.util.OnlineLeastSquaresFitter;

/**
 * Tracks the scores for each epoch to provide the slope of the curve of score vs epoch number.
 * The slope is computed from the most recent N remembered/tracked (epoch,score) pairs, where N is defined by the constructor.
 * The only remembered/tracked pairs are those for which the score is larger than all previously kept scores.
 * The idea is that we track the upper envelope of the score vs epoch curve and then compute the slope from that upper envelope.
 * This means that the slope should always be positive. 
 * @author DavidWood
 *
 */
public class EpochScoreTracker {

	private static class Pair<X,Y> {
		public X x;
		public Y y;

		Pair(X x, Y y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public String toString() {
			return "Pair [x=" + x + ", y=" + y + "]";
		}
	}

	private final LinkedList<Pair<Integer,Double>> queue = new LinkedList<Pair<Integer,Double>>();
//	private final int minHistoryCount;
//	private final int maxHistoryCount;
//	private final int maxEpochHistory;
	private final int historyCount;
	
	public EpochScoreTracker(int historyCount) {
		this.historyCount = historyCount;
	}
	
	public void update(int epochNum, double score) {
		Pair<Integer, Double> point;
		if (queue.size() > 0) {
			point = queue.getLast(); 
			if (epochNum <= point.x)
				throw new IllegalArgumentException("epoch value must be always increasing");
		}
	
		// Some scores can drop to something like 1/2 the most recent value.  
		// Identify them and drop them.
		if (!useScore(score,3))
			return;

		if (CNNClassifier.VERBOSE)
			AISPLogger.logger.info("Adding score " + score + " at epoch " + epochNum);
		
		// Insert the item to the queue at the end.
		point = new Pair<Integer,Double>(epochNum,score);
		queue.addLast(point);

		// Trim items on the front to hold only the most recent maxHistoryCount items.
		while (queue.size() > historyCount) 
			queue.removeFirst();

		if (CNNClassifier.VERBOSE)
			getScoreSlope();	// triggers logger.
	}
	
	/**
	 * Return true if the given score is greater or equal to all previous scores. 
	 * @param score
	 * @param nStddevs
	 * @return
	 */
	private boolean useScore(double score, double nStddevs) {
		for (Pair<Integer,Double> p : queue) 
			if (score < p.y )
				return false;
		return true;
	}

	public double getScoreSlope() {
		if (queue.size() < historyCount)
			return Double.NaN;
		OnlineLeastSquaresFitter fitter = new OnlineLeastSquaresFitter();
		for (Pair<Integer,Double> p : queue) 
			fitter.updateModel(p.x, p.y);
		if (CNNClassifier.VERBOSE)
			AISPLogger.logger.info("Slope=" + fitter.getSlope() + ", scores=" + queue);
		return fitter.getSlope();
	}

	public static void main(String[] args) {
		EpochScoreTracker est = new EpochScoreTracker(3);
		int count = 0;
		est.update(count++, .4);
		est.update(count++, .5);
		est.update(count++, .4);
		est.update(count++, .6);
		est.update(count++, .7);
		est.update(count++, .6);
	}
	
}
