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
package org.eng.aisp.feature;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.eng.util.IMutator;
import org.eng.util.OnlineStats;

/**
 * Provides normalization of each feature element of the feature vectors of the feature grams produced by an iterable.
 * Normalization is done across time through the feature gram on each index of the feature factor. 
 * This normalization can be important when the features elements have different units and the underlying algorithm is sensitive
 * to the scale of the features.  The distance-based algorithms are sensitive to scale.  The can be enabled in the AbstractNearestNeighbor
 * classifiers as needed.  
 * <p>
 * The following is an example of the treatment of element 0 of the array feature grams as they are produced by the iterable of feature gram arrays.
 * <pre>
 * | a0 a1 a2| |a3 a4 a5| ... |an ... |
 * | b0 b1 b2| |b3 b4 b5| ... |bn ... |
 * | c0 c1 c2| |c3 c4 c5| ... |cn ... |
 * gives
 * | a0' a1' a2'| |a3' a4' a5'| ... |an' ... |
 * | b0' b1' b2'| |b3' b4' b5'| ... |bn' ... |
 * | c0' c1' c2'| |c3' c4' c5'| ... |cn' ... |
 * </pre>
 * where for example 
 * <pre>
 * a0' = (a0 - mean(a0..an)) / stddev(a0..an), and
 * b0' = (b0 - mean(b0..bn)) / stddev(b0..bn)
 * </pre>
 * This does mean that a single pass is made through the features to capture the mean and standard deviations.
 * 
 * @author DavidWood
 *
 */
public class FeatureGramNormalizer implements IMutator<ILabeledFeatureGram<double[]>[], ILabeledFeatureGram<double[]>[] >, Serializable {

	private static final long serialVersionUID = -7478839309750973504L;
	/**
	 * The statistics for each column of features in each array of feature grams
	 * stats[i][j] are the statistics for feature element j of the ith feature gram across all ith feature grams in the iterable.
	 */
	private OnlineStats[][] stats = null;

	/**
	 * Create the normalizer to normalize the given features.
	 * The computation of the means and standard deviations is deferred until the first call to {@link #normalize(IFeatureGram[])}
	 * and {@link #mutate(ILabeledFeatureGram[])}.
	 * @param labeledFeatureGrams features to normalize
	 */
	public FeatureGramNormalizer(Iterable<? extends ILabeledFeatureGram<double[]>[]> labeledFeatureGrams) {
		this.initializeStats(labeledFeatureGrams);
	}

	/**
	 * @param labeledFeatureGrams
	 */
	protected void initializeStats(Iterable<? extends ILabeledFeatureGram<double[]>[]> labeledFeatureGrams) {
		int featureGramCount = 0;
		// Loop over each array of feature grams (1 feature gram array per data window, generally).
		for (ILabeledFeatureGram<double[]> fga[] :  labeledFeatureGrams) {
			if (stats == null) {	// first time through, so initialize some things.
				featureGramCount = fga.length;
				stats = new OnlineStats[featureGramCount][];
			}
			// Loop over each feature gram for the current window of features
			for (int featureGramIndex = 0 ; featureGramIndex<featureGramCount ; featureGramIndex++) {
				ILabeledFeatureGram<double[]> lfg =  fga[featureGramIndex]; 
				IFeatureGram<double[]> fg =  lfg.getFeatureGram(); 
				IFeature<double[]>[] features = fg.getFeatures();
				int featureCount = features.length;
				int featureVectorLength = features[0].getData().length; 
				// Initialize the stats for all feature grams 
				OnlineStats[] fgStats = stats[featureGramIndex];
				if (fgStats == null) {
					fgStats = new OnlineStats[featureVectorLength];
					for (int k=0 ; k<featureVectorLength ; k++) {
						fgStats[k] = new OnlineStats(); 
					}
					stats[featureGramIndex] = fgStats; 
				}
				// Add the features from this feature gram to each of the feature stats.
				for (int featureIndex=0 ; featureIndex < featureCount ; featureIndex++) {
					IFeature<double[]> feature = features[featureIndex];
					double[] fdata = feature.getData();
					for (int i=0 ; i<fdata.length ; i++) {
						fgStats[i].addSample(fdata[i]);
					}
				}
			}
		}
//		AISPLogger.logger.info("\n" + getStats());
	}

	private String getStats() {
		StringBuilder sb = new StringBuilder("");
		for (int i=0 ; i<stats.length ; i++) {
			OnlineStats[] fgStats = stats[i];
			for (int k=0 ; k<fgStats.length ; k++) {
				sb.append("Feature Stats[" + i + "][" + k + "]= " + fgStats[k]); 
				sb.append("\n");
			}
		}
		return sb.toString();
		
	}

	@Override
	public List<ILabeledFeatureGram<double[]>[]> mutate(ILabeledFeatureGram<double[]>[] item) {
		ILabeledFeatureGram<double[]>[] mutated = new ILabeledFeatureGram[item.length]; 
		IFeatureGram<double[]>[] fga = new IFeatureGram[item.length]; 
		for (int i=0 ; i<item.length ; i++) 
			fga[i] = item[i].getFeatureGram();
		IFeatureGram<double[]>[] newfga = normalize(fga); 
		for (int featureGramIndex=0 ; featureGramIndex<mutated.length ; featureGramIndex++) {
			ILabeledFeatureGram<double[]> lfg = item[featureGramIndex]; 
			IFeatureGram<double[]> newfg = newfga[featureGramIndex]; 
			ILabeledFeatureGram<double[]> newlfg = new LabeledFeatureGram<double[]>(newfg, lfg.getLabels()); 
			mutated[featureGramIndex] = newlfg;
		}
		List<ILabeledFeatureGram<double[]>[]> mutatedList = new ArrayList<ILabeledFeatureGram<double[]>[]>();
		mutatedList.add(mutated);
		return mutatedList;
	}

	public IFeatureGram<double[]>[] normalize(IFeatureGram<double[]>[] featureGrams) {
		IFeatureGram<double[]>[] newfga = new IFeatureGram[featureGrams.length]; 
		if (stats.length != featureGrams.length) {
			throw new IllegalArgumentException("Number of feature grams passed (" + featureGrams.length 
					+ " is not equal to equal to the number provided during initialization (" + stats.length + ")");
		}
//		AISPLogger.logger.info("stats=" + stats[0][0]);

		for (int featureGramIndex=0 ; featureGramIndex<featureGrams.length ; featureGramIndex++) {
			OnlineStats featureGramStats[] = stats[featureGramIndex];
			IFeatureGram<double[]> featureGram = featureGrams[featureGramIndex];  
			IFeature<double[]> features[] = featureGram.getFeatures();
			IFeature<double[]> newfeatures[] = new IFeature[features.length]; 
			for (int featureVectorIndex=0 ; featureVectorIndex<features.length ; featureVectorIndex++) {
				IFeature<double[]> feature  = features[featureVectorIndex];
				double[] fdata = feature.getData();
				if (fdata.length != featureGramStats.length) 
					throw new IllegalArgumentException("Number of feature passed (" + fdata.length 
							+ " is not equal to equal to the number provided during initialization (" + featureGramStats.length + ")");
				double[] newfdata = new double[fdata.length]; 
				for (int i=0 ; i<fdata.length ; i++) {
					newfdata[i] = (fdata[i] - featureGramStats[i].getMean())  / featureGramStats[i].getStdDev();
//					newfdata[i] = (fdata[i] - featureGramStats[i].getMean())  / (featureGramStats[i].getMaximum() - featureGramStats[i].getMinimum());
				}
				newfeatures[featureVectorIndex] = new DoubleFeature(feature.getStartTimeMsec(), feature.getEndTimeMsec(), newfdata);
			}
			IFeatureGram<double[]> newfg = new FeatureGram<double[]>(newfeatures); 
			newfga[featureGramIndex] = newfg;
		}
		return newfga;
	}
	
}