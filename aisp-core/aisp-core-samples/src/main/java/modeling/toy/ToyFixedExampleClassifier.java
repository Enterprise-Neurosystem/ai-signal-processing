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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.AbstractFixedFeatureExtractingClassifier;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.util.OnlineStats;
/**
 * Encapsulates the classification based on a feature.
 * @author dawood
 *
 */
public class ToyFixedExampleClassifier 
  extends AbstractFixedFeatureExtractingClassifier<double[],double[]> 
  implements IFixedClassifier<double[]> {

  private static final long serialVersionUID = -3239508962940231962L;
  private final String trainingLabel;
  private final Map<String,OnlineStats> labelStats;
  
  protected ToyFixedExampleClassifier(String trainingLabel, 
        List<IFeatureGramDescriptor<double[], double[]>> fgeList,
        Map<String, OnlineStats> labelStats) {
    super(fgeList);
    this.trainingLabel = trainingLabel;
    this.labelStats = labelStats;
  }

  /**
   * Called by the super class after extracting the features from the data 
   * passed to {@link #classify(org.eng.aisp.IDataWindow)}.
   */
  @Override
  protected List<Classification> classify(IFeatureGram<double[]>[] features) 
		  throws AISPException {
    // Compute the statistics on these features so we can compare them with 
    // those computed during training.
    OnlineStats featureStats = new OnlineStats();
    for (IFeatureGram<double[]> fg : features) 
      for (IFeature<double[]> f  : fg.getFeatures())
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
    List<Classification> clist = new ArrayList<Classification>();
    Classification c = new Classification(trainingLabel,minLabelValue,1.0);
    clist.add(c);
    return clist; 
  }

	@Override
	public String getTrainedLabel() {
		return this.trainingLabel;
	}
  
}