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
package modeling;
import java.io.IOException;
import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.gmm.GMMClassifier;

public class TrainAndClassify {

  public static void main(String[] args) throws AISPException, IOException {
    //  The location of the Sounds project (in the iot-sounds repo)
    String metadata = "sample-sounds/chiller";
    
    // Load the sounds and require a meta-data file providing labels & sensor ids.
    // The sounds acquire their status labels from the metadata.csv file.
    Iterable<SoundRecording> srList = SoundRecording.readMetaDataSounds(metadata);

    // Create and train the classifier on the "status" label attached to 
    // the loaded sounds.  MultiClassifier is a good general purpose 
    // classifier that can do outlier detection and train on multiple labels.
    // It uses the EnsembleClassifier, which explores the combination of
    // of feature extractors, processors and classifier algorithms to determine
    // the highest accuracy combinations for the given training data.
    final String trainingLabel = "status";
    IClassifier<double[]> classifier = new GMMClassifier();
    classifier.train(trainingLabel, srList);
    
    // Now see if the classifier works on the training data.
    for (SoundRecording sr : srList) {
      String expectedValue = sr.getLabels().getProperty(trainingLabel);
      SoundClip clip = sr.getDataWindow();  // Raw data window w/o labels
      Map<String, Classification> cmap = classifier.classify(clip);
      Classification c = cmap.get(trainingLabel); 
      String classifiedValue = c == null ? Classification.UndefinedLabelValue 
                : c.getLabelValue();
      System.out.println("Expected label= " + expectedValue 
            + ", classified label=" + classifiedValue);
    }
  }

}
