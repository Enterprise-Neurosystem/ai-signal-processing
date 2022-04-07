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

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.deeplearning4j.earlystopping.scorecalc.ScoreCalculator;
import org.deeplearning4j.earlystopping.termination.EpochTerminationCondition;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.eng.aisp.AISPLogger;
import org.eng.util.CSVTable;

class OptimizedScorerEpochTerminator implements ScoreCalculator<Model>, EpochTerminationCondition {

	private static final long serialVersionUID = 4601661441265818374L;

	private final int maxEpochs;
	private final double targetScore;
	private final ScoreCalculator<Model> scorer;

	private int nextScoringEpoch;
	private double lastScore;
	private double epochsSinceLastScore = 0;
	private int startScoringEpoch;
	
	private final EpochScoreTracker epochScoreTracker;

	private double minScoreChangePerEpoch;

	/**
	 * 
	 * @param maxEpochs
	 * @param scorer
	 * @param targetScore score threshold, which if reached, will cause terminate() to return true.
	 * @param startScoringEpoch sets epoch to start scoring using 0-based indexing. if -1 then score all epochs.
	 * @param epochScoreHistoryCount number of past epoch scores to use to determine if the score has leveled off. See minScoreChange parameter. 
	 * @param minScoreChange the threshold of per epoch change in score over the last epochScoreHistoryCount epochs under which terminate() will return true.
	 */
	public OptimizedScorerEpochTerminator(int maxEpochs, ScoreCalculator<Model> scorer, double targetScore, int startScoringEpoch, int epochScoreHistoryCount, double minScoreChangePerEpoch) {
		if (startScoringEpoch > maxEpochs)
			throw new IllegalArgumentException("maxEpochs must be at least as large as startScoringEpoch");
		this.maxEpochs = maxEpochs;
		this.scorer = scorer;
		if (startScoringEpoch < 0) {
			startScoringEpoch = maxEpochs/3;
		}
		this.startScoringEpoch = startScoringEpoch; 
		this.nextScoringEpoch = startScoringEpoch; 
		this.targetScore = targetScore;
		this.lastScore = scorer.minimizeScore() ? Double.MAX_VALUE : -Double.MAX_VALUE;
		epochScoreTracker = new EpochScoreTracker(epochScoreHistoryCount);	
		this.minScoreChangePerEpoch = minScoreChangePerEpoch;
	}

//	/**
//	 * Convenience over {@link #OptimizedScorerEpochTerminator(int, ScoreCalculator, double, int, int, double)} with the epoch to start scoring set to
//	 * max(15, nEpochs/4).
//	 */
//	public OptimizedScorerEpochTerminator(int nEpochs, ScoreCalculator<Model> scorer, double minEarlyStoppingScore, int epochScoreHistorySize, double minScoreChangePerEpoch) {
//		this(nEpochs, scorer, minEarlyStoppingScore, 
//				// Start at 25 epochs or higher and  if nEpochs is large start at 1/3 of them (75->25), 
//				// but always score at least the last epochScoreHistorySize epochs or all of them. 
//				// This makes sure there are always at least some from which to pick the best scoring model.
//				Math.min(Math.max(25,nEpochs/3), nEpochs - epochScoreHistorySize),
//				epochScoreHistorySize, minScoreChangePerEpoch);
//	}

	@Override	// EpochTerminationCondition
	public void initialize() { }

	@Override	// EpochTerminationCondition
//	public boolean terminate(int epochNum, double score) {	// beta2
	public boolean terminate(int epochNum, double score, boolean minimize) {	// beta4
		epochNum++;	// Convert from 0-based to 1-based.
		if (epochNum >= maxEpochs) {
			if (CNNClassifier.VERBOSE)
				AISPLogger.logger.info("Terminating due to  maximum epochs");
			return true;
		} else if (this.minimizeScore() && score <= targetScore) {
			if (CNNClassifier.VERBOSE)
				AISPLogger.logger.info("Terminating because target score of " + targetScore + " was reached");
			return true;
		} else if (!this.minimizeScore() && score >= targetScore) {
			if (CNNClassifier.VERBOSE)
				AISPLogger.logger.info("Terminating because target score of " + targetScore + " was reached");
			return true;
		} else if (epochNum > startScoringEpoch && score != lastScore && isScorePlateau(epochNum, score)) {
//		} else if (epochNum > startScoringEpoch && isScorePlateau(epochNum, score)) {
			if (CNNClassifier.VERBOSE)
				AISPLogger.logger.info("Terminated due to plateauing of slope.");
			return true;
		}
		return false;

//		return epochNum+1 >= maxEpochs || (this.minimizeScore() ? score <= targetScore : score >= targetScore) || 
//				(epochNum+1 >= startScoringEpoch && isScorePlateau(epochNum, score));
	}	 

	
	private boolean isScorePlateau(int epochNum, double score) {
		if (minScoreChangePerEpoch == 0)
			return false;	// Cause all epochs to be used.
		epochScoreTracker.update(epochNum, score);
		double slope = epochScoreTracker.getScoreSlope();
		return !Double.isNaN(slope) && Math.abs(slope) <= minScoreChangePerEpoch;	
	}

	@Override	// ScoreCalculator
	public double calculateScore(Model model) {
		if (!(model instanceof MultiLayerNetwork))
			throw new RuntimeException("unexpected");
		MultiLayerNetwork network = (MultiLayerNetwork)model;
		double score;
		int currentEpoch = network.getEpochCount();		// 0-based index. 0 after first fit. 
		epochsSinceLastScore++;			// Assumes this is evaluated for every epoch.
		if ((currentEpoch == nextScoringEpoch || nextScoringEpoch < 0)) {
			// The following is independent of scorer.minimizeScore();
			score = scorer.calculateScore(model);
			if (CNNClassifier.VERBOSE)
				AISPLogger.logger.info("Scored epoch " + currentEpoch + " equals " + score);
			int epochsToSkip; 
//			if (currentEpoch >= epochToStartScoringAll) {
//				// As we get to the end and we haven't yet met the target, assume we won't meet the target and have plateaued.
//				// Start scoring each epoch near the end so as to find and use the best of the last few.
//				epochsToSkip = 0;
//			} else 
//			if (Math.abs(lastScore) == Double.MAX_VALUE) {	// First time
				epochsToSkip = 0;
//			} else {
//				double delta = (score - lastScore) / epochsSinceLastScore;		// per epoch score change..
//				// number of epochs into the future where we might expect to achieve target score.
//				epochsToSkip = (int)Math.floor((targetScore - score) / delta);	
//				if (epochsToSkip < 0)
//					epochsToSkip = 0;
//				// Never skip more than 1/2 the remaining epochs.
//				int maxEpochsToSkip = Math.max(0, (maxEpochs - currentEpoch) / 2 - 1);	
//				if (epochsToSkip > maxEpochsToSkip)
//					epochsToSkip = maxEpochsToSkip;
//			}
			if (nextScoringEpoch >= 0)
				nextScoringEpoch = Math.min(maxEpochs, nextScoringEpoch + 1 + epochsToSkip);
			lastScore = score;
			epochsSinceLastScore = 0;
			if (CNNClassifier.VERBOSE)
				AISPLogger.logger.info("Next score will be calculated at epoch " + nextScoringEpoch);
		} else {
			if (CNNClassifier.VERBOSE)
				AISPLogger.logger.info("Skipping scoring on epoch " + currentEpoch + ", next score will be computed at epoch " + nextScoringEpoch);
			// keep the EarlyStopping framework from saving the model.
			score = lastScore;
		}
		return score;
	}

	@Override	// ScoreCalculator
	public boolean minimizeScore() {
		return scorer.minimizeScore();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "OptimizedScorerEpochTerminator [maxEpochs=" + maxEpochs + ", targetScore=" + targetScore
				+ ", scorer=" + scorer + ", nextScoringEpoch=" + nextScoringEpoch + ", lastScore=" + lastScore
				+ ", epochsSinceLastScore=" + epochsSinceLastScore + "]";
	}
	
	private static class CSVScorer implements ScoreCalculator<Model> {

		CSVTable scores;
		Iterator<CaseInsensitiveMap> rowIterator;
		
		public CSVScorer(CSVTable scores) {
			this.scores = scores;
			this.rowIterator = scores.iterator();
		}

		@Override
		public double calculateScore(Model network) {
			if (!rowIterator.hasNext())
				return Double.NaN;
			CaseInsensitiveMap row = rowIterator.next();
			int epoch = Integer.parseInt(row.get("1").toString());
			double score1 = Double.parseDouble(row.get("2").toString());
			double score2 = Double.parseDouble(row.get("3").toString());
			return score2;
		}

		@Override
		public boolean minimizeScore() {
			return false;
		}
		
	}
	
	public static void main(String[] args) throws IOException {
		CSVTable scores = CSVTable.readFile("cnn-epoch-scores.csv", 0);	// epoch, train score, test score
		System.out.println(scores.write());
		CSVScorer csvScorer = new CSVScorer(scores);
		OptimizedScorerEpochTerminator scorer = new OptimizedScorerEpochTerminator(75, csvScorer, .9995, 1, 10, .001);
		for (CaseInsensitiveMap row: scores) {
			int epoch = Integer.parseInt(row.get("1").toString());
			double score1 = Double.parseDouble(row.get("2").toString());
			double score2 = Double.parseDouble(row.get("3").toString());
//			boolean terminated = scorer.terminate(epoch, score2);			// beta2
			boolean terminated = scorer.terminate(epoch, score2, false);	// beta4
			if (terminated) {
				System.out.println("Terminated at epoch " + epoch);
				break;
			}
		}
	}

}
