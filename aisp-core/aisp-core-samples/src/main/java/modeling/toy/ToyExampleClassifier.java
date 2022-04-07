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
package modeling.toy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPRuntime;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.pipeline.FeatureExtractionPipeline;
import org.eng.aisp.feature.pipeline.LabeledFeatureIterable;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.util.OnlineStats;
import org.eng.util.TaggedEntity;

/**
 * Implements a basic classifier only extending the TaggedEntity class.
 * 
 * @author dawood, wangshiq
 *
 */
public class ToyExampleClassifier extends TaggedEntity implements IClassifier<double[]> {

  private static final long serialVersionUID = 2966528639796200129L;
  private String primaryTrainingLabel = null;
  private IFeatureGramDescriptor<double[], double[]> fgExtractor;

  /**
   * Hold our model of the training data.
   */
  private Map<String, OnlineStats> labelStats = new HashMap<String, OnlineStats>();

  public ToyExampleClassifier(IFeatureExtractor<double[], 
      double[]> extractor, IFeatureProcessor<double[]> processor, 
      int windowSizeMsec, int windowShiftMsec, double someParameter) {
	 fgExtractor = new FeatureGramDescriptor<double[],double[]>(windowSizeMsec, 
			 windowShiftMsec, extractor, processor);
  }

  /**
   * This simply keeps statistics (mean, stdev, etc) features grouped by label
   * value and then stores the model of statistics used during classification.
   */
  public void train(String trainingLabel, Iterable<? extends 
		  ILabeledDataWindow<double[]>> data) throws AISPException {
  this.primaryTrainingLabel = trainingLabel;

  // Create feature extractor that applies the FeatureExtractionPipeline
  // on the iterable of data windows in a streaming fashion.
  // The data window is partitioned into subwindows, then the feature
  // extractor is called on each, and if present, the processor works on 
  // the whole array of features for a given data window.
    LabeledFeatureIterable<double[], double[]> features = 
    		new LabeledFeatureIterable<double[],double[]>(data, 
    				Arrays.asList(fgExtractor));

    // This loop pulls the features out of the data. Here we use an OnlineStats 
    // object to calculate * the mean for each set of features for a given 
    // label value.
    for (ILabeledFeatureGram<double[]>[] lfArray : features) {
      // We only have the feature extractor and process to produce a single feature gram
      ILabeledFeatureGram<double[]> featureGram = lfArray[0];	
      // Get the labelValue from the first feature. All features in the
      // array will have the same labels.
      String labelValue = featureGram.getLabels().getProperty(trainingLabel);

      // Get the statistics for this label value and create one if needed
      OnlineStats stats = labelStats.get(labelValue);
      if (stats == null) {
        stats = new OnlineStats();
        labelStats.put(labelValue, stats);
      }

      // Add the features to the statistics for this label value.
      for (IFeature<double[]> f : featureGram.getFeatureGram().getFeatures())
        stats.addSamples(f.getData());
    }

  }

  @Override
  public Map<String, Classification> classify(IDataWindow<double[]> sample) 
      throws AISPException {

    // Extract the features from this window.
	FeatureExtractionPipeline<double[],double[]> fePipeline = AISPRuntime.getRuntime().getFeatureExtractionPipeline(fgExtractor);
	IFeatureGram<double[]> fgArray[] = fePipeline.extract(sample) ;
	IFeatureGram<double[]> featureGram = fgArray[0]; 
    IFeature<double[]>[] features = featureGram.getFeatures();

    // Compute the statistics on these features so we can compare them with
    // those computed during training.
    OnlineStats featureStats = new OnlineStats();
    for (IFeature<double[]> f : features)
      featureStats.addSamples(f.getData());

    // Now look for the label value whose stats are closest to the stats
    // for the feature being classified.
    double minDist = Double.MAX_VALUE;
    String minLabelValue = null;
    for (String labelValue : labelStats.keySet()) {
      OnlineStats stats = labelStats.get(labelValue);
      double distance = Math.abs(stats.getMean() - featureStats.getMean());
      if (distance < minDist) {
        minDist = distance;
        minLabelValue = labelValue;
      }
    }

    // We have best label, so let's create a single Classification and put
    // it in the returned list You may provide other classifications as
    // candidates if you like, but we don't here.
    Map<String, Classification> cmap = new HashMap<String, Classification>();
    Classification c = new Classification(this.primaryTrainingLabel, 
        minLabelValue, 1.0);
    cmap.put(this.primaryTrainingLabel, c);
    return cmap;
  }

  @Override
  public String getTrainedLabel() {
	return this.primaryTrainingLabel;
  }

}