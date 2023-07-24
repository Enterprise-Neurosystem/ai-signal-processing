package org.eng.aisp.classifier.soundanomaly;

import org.eng.aisp.*;
import org.eng.aisp.classifier.AbstractClassifier;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.TrainingSetInfo;
import org.eng.aisp.classifier.gmm.GMMClassifier;
import org.eng.aisp.dataset.LabeledSoundFiles;
import org.eng.util.IShuffleIterable;

import java.io.IOException;
import java.util.*;

/**
 * @author salenakha
 */
public class SoundAnomalyClassifier extends AbstractClassifier<double[]> implements IClassifier<double[]> {
    List<IClassifier> classifiers = new ArrayList<>();
    private IClassifier fullModel;

    /**
     *
     * @param trainingLabel
     * @param data labeled data to train the model.  Data must contain at enough data with the given label for the model to be trained.
     * @throws AISPException
     *
     * this method is responsible for training the classifier
     */

    public void train(String trainingLabel, Iterable<? extends ILabeledDataWindow<double[]>> data) throws AISPException {
        Set<String> labelValues = this.getLabelValuesFromData(trainingLabel, data);

        for (String labelValue : labelValues) {
            //goes through labelValues and trains each, then adds to list of classifiers
            IClassifier c = this.trainSubModel(trainingLabel, labelValue, data);
            classifiers.add(c);
        }

        this.fullModel = new GMMClassifier();
        this.fullModel.train(trainingLabel,data);

        this.trainedLabel = trainingLabel;
    }

    /**
     *
     * @param trainingLabel
     * @param labelValue
     * @param data
     * @return
     * @throws AISPException
     *
     * this method is responsible for training the sub-models of the classifier for a specific label value. for known
     * values, it will perform binary classification.
     */
    private IClassifier trainSubModel(String trainingLabel, String labelValue, Iterable<? extends ILabeledDataWindow<double[]>> data) throws AISPException {
        //this is training the sub model of each label value
        Iterable<? extends ILabeledDataWindow<double[]>> relabeledData = this.relabeledData(trainingLabel, labelValue, data);
        TrainingSetInfo tsi = TrainingSetInfo.getInfo(relabeledData);
        System.out.println(tsi.prettyFormat());

        //gmm classifier example
        IClassifier c = new GMMClassifier();
        c.train(trainingLabel, relabeledData);
        return c;
    }

    /**
     *
     * @param labelName
     * @param labelValue
     * @param data
     * @return
     *
     * this method is responsible for creating a new data set for training a specific sub-model of the classifier
     */
    public final static String unknown = "unknown";
    public final static String known = "known";
    private Iterable<? extends ILabeledDataWindow<double[]>> relabeledData(String labelName, String labelValue, Iterable<? extends ILabeledDataWindow<double[]>> data) {
        //this is used to train the sub-models of "one or not one," etc.

        List<ILabeledDataWindow<double[]>> relabeledData = new ArrayList<>();

        for (ILabeledDataWindow<double[]> window : data) {
            IDataWindow<double[]> features = window.getDataWindow();
            String originalLabel = window.getLabels().getProperty(labelName);

            if (originalLabel == null) {
                continue; //if null, skip because not labeled
            }

            String relabeledLabel; // if matches, assign the new label as 1
            if (originalLabel.equals(labelValue)) {
                relabeledLabel = known;
            } else {
                relabeledLabel = unknown;
            }

            Properties newLabels = new Properties();
            newLabels.put(labelName, relabeledLabel);

            //change this
            ILabeledDataWindow<double[]> relabeledWindow = new LabeledDataWindow<>(features, newLabels);
            relabeledData.add(relabeledWindow);

        }
        return relabeledData;
    }

    /**
     *
     * @param trainingLabel
     * @param data
     * @return
     *
     * this method is responsible for extracting the unique values associated with each label. obtains a set of unique
     * values that is used to train the sub-models
     */
    private Set<String> getLabelValuesFromData(String trainingLabel, Iterable<? extends ILabeledDataWindow<double[]>> data) {
        Set<String> labelValues = new HashSet<>();

        for (ILabeledDataWindow<double[]> window : data) {
            Properties labels = window.getLabels();
            String labelValue = labels.getProperty(trainingLabel);

            if(labelValue == null) {
                continue;
            }
            labelValues.add(labelValue);
        }
        return labelValues;
    }

    @Override
    public Map<String, Classification> classify(IDataWindow<double[]> sample) throws AISPException {
        Map<String, Classification> results = new HashMap<>(); //empty map to store

        boolean isKnown = false;

        for (IClassifier subModel : classifiers) { //loop through classifiers of sub-models (known sounds)
            String labelName = subModel.getTrainedLabel(); //label of value
            Map<String, Classification> map = (Map) subModel.classify(sample); //classification of value
            Classification c = map.get(labelName); //contains classification for sound
            String labelValue = c.getLabelValue(); //will set to known or unknown
            double confidence = c.getConfidence();

            if (labelValue.equals(known) && confidence > 0.5){
                //sound is known
                isKnown = true;
                break;
            }
        }

        if(isKnown){
            return fullModel.classify(sample);
            //if any of the known sounds were confidently classified as "known" by the sub-models
            //then proceed to classify with fullModel
        }

        //if unknown:
        String labelName = this.getTrainedLabel(); //labelName will be training label (ex: "cause")
        String labelValue = unknown;
        double confidence = 1; //high confidence that the sound is unknown
        Classification c = new Classification(labelName, labelValue, confidence);

        return results; //result for unknown
    }

    protected String trainedLabel;
    @Override
    public String getTrainedLabel() {
        return this.trainedLabel;
    }

    public static void main(String[] args) throws AISPException, IOException {
        IShuffleIterable<SoundRecording> sound1 = LabeledSoundFiles.loadMetaDataSounds("aisp-core/aisp-core-main/test-data/home", true);
        TrainingSetInfo tsi = TrainingSetInfo.getInfo(sound1);
        System.out.println(tsi.prettyFormat());
        System.out.flush();

        IShuffleIterable<SoundRecording> sound2 = LabeledSoundFiles.loadMetaDataSounds("aisp-core/aisp-core-main/test-data/chiller", true);


        IClassifier<double[]> classifier = new SoundAnomalyClassifier();

        classifier.train("cause", sound1);

        System.out.println("\n sound 1");

        for (SoundRecording sound : sound1) {
            Map<String, Classification> map = classifier.classify(sound.getDataWindow());
            System.out.println(map);
        }

        System.out.println("\nsound 2");

        for (SoundRecording sound : sound2) {
            Map<String, Classification> map = classifier.classify(sound.getDataWindow());
            System.out.println(map);
        }

    }

}
