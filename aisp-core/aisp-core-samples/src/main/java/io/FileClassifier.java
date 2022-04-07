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
package io;

import java.io.IOException;
import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.gmm.GMMClassifier;
import org.eng.aisp.util.FixedClassifiers;

public class FileClassifier {

	public static void main(String[] args) throws IOException, ClassNotFoundException, AISPException {
		// Create the classifier to train on 'mylabel'.
		IClassifier<double[]> classifier = new GMMClassifier();

		// ... train model on your sounds.
	    // Load the sounds and require a meta-data file providing labels & sensor ids.
	    // The sounds acquire their status labels from the metadata.csv file.
	    //  The location of the Sounds project (in the iot-sounds repo)
	    String srcDir = "sample-sounds/chiller";
	    Iterable<SoundRecording> sounds = SoundRecording.readMetaDataSounds(srcDir);	
	    classifier.train("status", sounds);
	    FixedClassifiers.write("mymodel.ser", classifier);
		System.out.println("Trained model and created in mymodel.ser");

		// Load the trained classifier and classify a sound.
		classifier =(IClassifier<double[]>) FixedClassifiers.read("mymodel.ser");
		
		SoundClip clip = SoundClip.readClip(srcDir + "/chiller-1.wav");
		Map<String,Classification> c = classifier.classify(clip);
		System.out.println("Classification: " + c);
	}

}
