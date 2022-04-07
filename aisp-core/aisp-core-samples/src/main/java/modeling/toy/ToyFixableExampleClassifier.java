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

import java.util.HashMap;
import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.AbstractFixableFeatureExtractingClassifier;
import org.eng.aisp.classifier.IFixableClassifier;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.transform.ITrainingWindowTransform;
import org.eng.util.OnlineStats;

/**
 * Implements a fixable classifier (one that can create a classifier does not support training - for the edge). 
 * 
 * @author dawood, wangshiq
 *
 */
public class ToyFixableExampleClassifier 
  extends AbstractFixableFeatureExtractingClassifier<double[], double[]> 
  implements IFixableClassifier<double[]> {

  private static final long serialVersionUID = 2966528639796200129L;
  
  public ToyFixableExampleClassifier( ITrainingWindowTransform<double[]> transforms,
        IFeatureExtractor<double[], double[]>  extractor, 
        IFeatureProcessor<double[]> processor,
        int windowSizeMsec, int windowShiftMsec, double someParameter) {
    super(false, transforms, extractor, (IFeatureProcessor<double[]>) null, windowSizeMsec, 
    		windowShiftMsec, false, false);  
  }

  /**
   * This simply keeps statistics (mean, stdev, etc) features grouped by label value 
   * and then creates the fixed classifier to use these during classification.
   */
  @Override
  protected IFixedClassifier<double[]> trainFixedClassifierOnFeatures(Iterable<? 
      extends ILabeledFeatureGram<double[]>[]> features) throws AISPException {
    Map<String, OnlineStats> labelStats = new HashMap<String,OnlineStats>();
    
    /**
     * This loop pulls the features out of the data.  The super class creates the 
     * iterable used here and that computes the features in a parallel and streaming 
     * manner. Here we use an OnlineStats object to calculate the mean for each set 
     * of features for a given label value.
     */
    for (ILabeledFeatureGram<double[]>[] lfArray : features) {
      // Get the labelValue from the first feature. All features in the array 
      // will have the same labels.
      String labelValue = lfArray[0].getLabels().getProperty(primaryTrainingLabel);

      // Get the statistics for this label value and create one if needed
      OnlineStats stats = labelStats.get(labelValue);
      if (stats == null) {
        stats = new OnlineStats();
        labelStats.put(labelValue, stats);
      }
      // Add the features to the statistics for this label value.
      for (IFeature<double[]> f : lfArray[0].getFeatureGram().getFeatures()) 
        stats.addSamples(f.getData());
    }

    // Create and return an instance of a IFixedClassifier that will use the 
    // statistics we just computed.
    return new ToyFixedExampleClassifier(this.primaryTrainingLabel,this.featureGramDescriptors, labelStats);
  }
}
