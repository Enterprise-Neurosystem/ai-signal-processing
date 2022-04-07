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
package org.eng.aisp.classifier;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.eng.aisp.AISPException;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.ConfusionMatrix;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.classifier.ConfusionMatrix.Normalization;
import org.eng.util.TaggedEntity;
import org.junit.Assert;
import org.junit.Test;


public class ConfusionMatrixTester {


    private static class TestClassifier extends TaggedEntity implements IFixedClassifier<double[]>{
		private static final long serialVersionUID = 1L;
		private List<String> classifyValues;
		private AtomicInteger index = new AtomicInteger();
		private String trainingLabel;
		Map<IDataWindow, String> predictions = new HashMap<IDataWindow,String>();
		
//		TestClassifier(List<String> labelValues, String trainingLabel){
//			this(null,labelValues, trainingLabel);
//		}
		TestClassifier(List<ILabeledDataWindow<double[]>> sounds, List<String> labelValues, String trainingLabel){
			if (sounds != null) {
				int index = 0;
				for (ILabeledDataWindow ldw : sounds) {
					IDataWindow clip = ldw.getDataWindow();
					predictions.put(clip, labelValues.get(index++));
				}
		    }
			this.classifyValues = labelValues;
			this.trainingLabel = trainingLabel;
    	}

		@Override
		public Map<String, Classification> classify(IDataWindow<double[]> sample) throws AISPException {
//			int index = this.index.getAndIncrement();
//			AISPLogger.logger.info("Expected calls = " + (classifyValues.size()+1) + ", actual calls = " + (index+1));
//			String labelValue = classifyValues.get(index);
			String labelValue = predictions.get(sample);
//			AISPLogger.logger.info("prediction=" + labelValue);
			Map <String, Classification> cMap = new HashMap<>();
			Classification c = new Classification(trainingLabel, labelValue, 1);
			cMap.put(trainingLabel, c);
			return cMap;
		}

//		@Override
//		public String showModel() {
//			return null;
//		}

		/**
		 * @return the trainingLabel
		 */
		public String getTrainedLabel() {
			return trainingLabel;
		}
    }

    private List<String> realLabels1      = new ArrayList<>(Arrays.asList("Dog", "Cat", "Horse", "Monkey"));
    private List<String> predictedLabels1 = new ArrayList<>(Arrays.asList("Unicorn", "Fish", "Moose", "Mouse"));

    private double[][] expected1 = new double[][] {
    		{1.0, 0.0, 0.0, 0.0},
    		{0.0, 0.0, 0.0, 1.0},
    		{0.0, 1.0, 0.0, 0.0},
    		{0.0, 0.0, 1.0, 0.0}};
    private double[][] expectedpcnt1 = new double[][] {
    		{100.0, 0.0, 0.0, 0.0},
    		{0.0, 0.0, 0.0, 100.0},
    		{0.0, 100.0, 0.0, 0.0},
    		{0.0, 0.0, 100.0, 0.0}};

    private List<String> realLabels2 = new ArrayList<>(Arrays.asList("Dog", "Dog", "Dog", "Dog"));
    private List<String> predictedLabels2 = new ArrayList<>(Arrays.asList("Dog", "Cat", "Horse", "Monkey"));
    private double[][] expected2 = new double[][] {
            {1.0, 1.0, 1.0, 1.0}
    };
    private double[][] expectedpcnt2 = new double[][] {
            {25, 25, 25, 25}
    };

    private List<String> realLabels3 = new ArrayList<>(Arrays.asList("Elephant", "Cat", "Horse", "Monkey"));
    private List<String> predictedLabels3 = new ArrayList<>(Arrays.asList("Elephant", "Cat", "Horse", "Monkey"));
    private double[][] expected3 = new double[][]{
            {1.0, 0.0, 0.0, 0.0},
            {0.0, 1.0, 0.0, 0.0},
            {0.0, 0.0, 1.0, 0.0},
            {0.0, 0.0, 0.0, 1.0}
    };
    private double[][] expectedpcnt3 = new double[][]{
            {100.0, 0.0, 0.0, 0.0},
            {0.0, 100.0, 0.0, 0.0},
            {0.0, 0.0, 100.0, 0.0},
            {0.0, 0.0, 0.0, 100.0}
    };


    private List<String> realLabels4 = new ArrayList<>(Arrays.asList("Dog", "Dog", "Dog", "Dog"));
    private List<String> predictedLabels4 = new ArrayList<>(Arrays.asList("Dog", "Dog", "Dog", "Dog"));
    private double[][] expected4 = new double[][]{
            {4}
    };
    private double[][] expectedpcnt4 = new double[][]{
            {100.0}
    };

    private List<String> realLabels5 = new ArrayList<>(Arrays.asList("Dog", "Dog", "Dog", "Dog"));
    private List<String> predictedLabels5 = new ArrayList<>(Arrays.asList("Dog", "Cat", "Dog", "Dog"));
    private double[][] expected5 = new double[][]{
            {1, 3}
    };
    private double[][] expectedpcnt5 = new double[][]{
            {25.0, 75.0}
    };


    private List<String> realLabels6 = new ArrayList<String>(Arrays.asList("A", "B", "C", "D"));
    private List<String> predictedLabels6 = new ArrayList<String>(Arrays.asList("A", "B", "C", "D"));
    private double[][] expected6 = new double[][]{
            {1, 0, 0, 0},
            {0, 1, 0, 0},
            {0, 0, 1, 0},
            {0, 0, 0, 1}
    };
    private double[][] expectedpcnt6 = new double[][]{
            {100, 0, 0, 0},
            {0, 100, 0, 0},
            {0, 0, 100, 0},
            {0, 0, 0, 100}
    };

    private List<String> realLabels7 = new ArrayList<String>(Arrays.asList("Dog", "Cat", "Cat", "Dog"));
    private List<String> predictedLabels7 = new ArrayList<String>(Arrays.asList("Dog", Classification.UndefinedLabelValue, "Cat", "Dog"));
    private double[][] expected7 = new double[][]{
    		{1.0, 1.0, 0.0},
    		{0.0, 0.0, 2.0}
    };
    private double[][] expectedpcnt7 = new double[][]{
			{50.0, 50.0, 0.0},
			{0.0, 0.0, 100.0}
    };


    @Test
    public void testForCorrectnessZeroLabelMatch(){
    	ConfusionMatrix cm1 = new ConfusionMatrix("",realLabels1, predictedLabels1);
//    	System.out.println(cm1.formatCounts());
//    	System.out.println(cm1.formatPercents());

        assertArrayEquals(expected1, cm1.getCountsArray());
        assertArrayEquals(expectedpcnt1, cm1.getPercentsArray(Normalization.Recall));
        

    }

    @Test
    public void testForCorrectnessOneLabelMatch(){
    	ConfusionMatrix cm2 = new ConfusionMatrix("",realLabels2, predictedLabels2);
        assertArrayEquals(expected2, cm2.getCountsArray());
        assertArrayEquals(expectedpcnt2, cm2.getPercentsArray(Normalization.Recall));

    }

    @Test
    public void testForCorrectnessAllLabelMatch() {
    	ConfusionMatrix cm3 = new ConfusionMatrix("",realLabels3, predictedLabels3);
        assertArrayEquals(expected3, cm3.getCountsArray());
        assertArrayEquals(expectedpcnt3, cm3.getPercentsArray(Normalization.Recall));
    }

    @Test
    public void testForCorrectnessAllSameLabel() {
    	ConfusionMatrix cm4 = new ConfusionMatrix("",realLabels4, realLabels4);
        assertArrayEquals(expected4, cm4.getCountsArray());
        assertArrayEquals(expectedpcnt4, cm4.getPercentsArray(Normalization.Recall));

    }

    @Test
    public void testForCorrectnessAllLabelMatchExceptOne() {
    	ConfusionMatrix cm5 = new ConfusionMatrix("",realLabels5, predictedLabels5);
        assertArrayEquals(expected5, cm5.getCountsArray());
        assertArrayEquals(expectedpcnt5, cm5.getPercentsArray(Normalization.Recall));

    }

    @Test
    public void testForOneCharLabels() {
    	ConfusionMatrix cm6 = new ConfusionMatrix("",realLabels6, predictedLabels6);
        assertArrayEquals(expected6, cm6.getCountsArray());
        assertArrayEquals(expectedpcnt6, cm6.getPercentsArray(Normalization.Recall));
    }

    @Test
    public void testForUndefinedLabelValue() {
    	ConfusionMatrix cm7 = new ConfusionMatrix("",realLabels7, predictedLabels7);
        assertArrayEquals(expected7, cm7.getCountsArray());
        assertArrayEquals(expectedpcnt7, cm7.getPercentsArray(Normalization.Recall));
    }


    @Test
    public void testForAddition1() {
//        private List<String> realLabels1 = new ArrayList<>(Arrays.asList("Dog", "Cat", "Horse", "Monkey"));
//        private List<String> predictedLabels1 = new ArrayList<>(Arrays.asList("Unicorn", "Fish", "Moose", "Mouse"));
    	ConfusionMatrix cm1 = new ConfusionMatrix("",realLabels1, predictedLabels1);
//        private List<String> realLabels2 = new ArrayList<>(Arrays.asList("Dog", "Dog", "Dog", "Dog"));
//        private List<String> predictedLabels2 = new ArrayList<>(Arrays.asList("Dog", "Cat", "Horse", "Monkey"));
    	ConfusionMatrix cm2 = new ConfusionMatrix("",realLabels2, predictedLabels2);
        ConfusionMatrix actual = cm1.add(cm2);
    	Map<String, Map<String, Integer>> expected = new HashMap<String, Map<String, Integer>>();
    	expected.put("Dog", new HashMap<String, Integer>());
    	expected.get("Dog").put("Unicorn", 1);
    	expected.get("Dog").put("Cat", 1);
    	expected.get("Dog").put("Horse", 1);
    	expected.get("Dog").put("Monkey", 1);
    	expected.get("Dog").put("Dog", 1);
    	expected.get("Dog").put("Mouse", 0);
    	expected.get("Dog").put("Fish", 0);
    	expected.get("Dog").put("Moose", 0);

    	
//    	expected.put("Mouse", new HashMap<String, Integer>());
//    	expected.get("Mouse").put("Unicorn", 0);
//    	expected.get("Mouse").put("Cat", 0);
//    	expected.get("Mouse").put("Horse", 0);
//    	expected.get("Mouse").put("Monkey", 0);
//    	expected.get("Mouse").put("Dog", 0);
//    	expected.get("Mouse").put("Mouse", 0);
//    	expected.get("Mouse").put("Fish", 0);
//    	expected.get("Mouse").put("Moose", 0);

    	
//    	expected.put("Moose", new HashMap<String, Integer>());
//    	expected.get("Moose").put("Unicorn", 0);
//    	expected.get("Moose").put("Cat", 0);
//    	expected.get("Moose").put("Horse", 0);
//    	expected.get("Moose").put("Monkey", 0);
//    	expected.get("Moose").put("Dog", 0);
//    	expected.get("Moose").put("Mouse", 0);
//    	expected.get("Moose").put("Fish", 0);
//    	expected.get("Moose").put("Moose", 0);


    	
//    	expected.put("Unicorn", new HashMap<String, Integer>());
//    	expected.get("Unicorn").put("Unicorn", 0);
//    	expected.get("Unicorn").put("Cat", 0);
//    	expected.get("Unicorn").put("Horse", 0);
//    	expected.get("Unicorn").put("Monkey", 0);
//    	expected.get("Unicorn").put("Dog", 0);
//    	expected.get("Unicorn").put("Mouse", 0);
//    	expected.get("Unicorn").put("Fish", 0);
//    	expected.get("Unicorn").put("Moose", 0);

    	
    	expected.put("Cat", new HashMap<String, Integer>());
    	expected.get("Cat").put("Unicorn", 0);
    	expected.get("Cat").put("Cat", 0);
    	expected.get("Cat").put("Horse", 0);
    	expected.get("Cat").put("Monkey", 0);
    	expected.get("Cat").put("Dog", 0);
    	expected.get("Cat").put("Mouse", 0);
    	expected.get("Cat").put("Fish", 1);
    	expected.get("Cat").put("Moose", 0);

    	
    	expected.put("Horse", new HashMap<String, Integer>());
    	expected.get("Horse").put("Unicorn", 0);
    	expected.get("Horse").put("Cat", 0);
    	expected.get("Horse").put("Horse", 0);
    	expected.get("Horse").put("Monkey", 0);
    	expected.get("Horse").put("Dog", 0);
    	expected.get("Horse").put("Mouse", 0);
    	expected.get("Horse").put("Fish", 0);
    	expected.get("Horse").put("Moose", 1);

    	
    	
    	
    	expected.put("Monkey", new HashMap<String, Integer>());
    	expected.get("Monkey").put("Unicorn", 0);
    	expected.get("Monkey").put("Cat", 0);
    	expected.get("Monkey").put("Horse", 0);
    	expected.get("Monkey").put("Monkey", 0);
    	expected.get("Monkey").put("Dog", 0);
    	expected.get("Monkey").put("Mouse", 1);
    	expected.get("Monkey").put("Fish", 0);
    	expected.get("Monkey").put("Moose", 0);
    	
//    	expected.put("Fish", new HashMap<String, Integer>());
//    	expected.get("Fish").put("Unicorn", 0);
//    	expected.get("Fish").put("Cat", 0);
//    	expected.get("Fish").put("Horse", 0);
//    	expected.get("Fish").put("Monkey", 0);
//    	expected.get("Fish").put("Dog", 0);
//    	expected.get("Fish").put("Mouse", 0);
//    	expected.get("Fish").put("Fish", 0);
//    	expected.get("Fish").put("Moose", 0);

    	
    	assertTrue(actual.getCounts().equals(expected));
    }
    @Test
    public void checkForAddition2() {
    	ConfusionMatrix cm2 = new ConfusionMatrix("", realLabels2, predictedLabels2);
    	ConfusionMatrix cm3 = new ConfusionMatrix("", realLabels3, predictedLabels3);
        ConfusionMatrix actual = cm2.add(cm3);
    	
        Map<String, Map<String, Integer>> expected = new HashMap<String, Map<String, Integer>>();
    	expected.put("Elephant", new HashMap<String, Integer>());
    	expected.put("Cat", new HashMap<String, Integer>());
    	expected.put("Horse", new HashMap<String, Integer>());
    	expected.put("Monkey", new HashMap<String, Integer>());

    	expected.get("Elephant").put("Elephant", 1);
    	expected.get("Elephant").put("Cat", 0);
    	expected.get("Elephant").put("Horse", 0);
    	expected.get("Elephant").put("Monkey", 0);
    	expected.get("Elephant").put("Dog", 0);

    	expected.get("Cat").put("Elephant", 0);
    	expected.get("Cat").put("Cat", 1);
    	expected.get("Cat").put("Horse", 0);
    	expected.get("Cat").put("Monkey", 0);
    	expected.get("Cat").put("Dog", 0);
    	
    	
    	expected.get("Horse").put("Elephant", 0);
    	expected.get("Horse").put("Cat", 0);
    	expected.get("Horse").put("Horse", 1);
    	expected.get("Horse").put("Monkey", 0);
    	expected.get("Horse").put("Dog", 0);
    	
    	expected.get("Monkey").put("Elephant", 0);
    	expected.get("Monkey").put("Cat", 0);
    	expected.get("Monkey").put("Horse", 0);
    	expected.get("Monkey").put("Monkey", 1);
    	expected.get("Monkey").put("Dog", 0);
    	
    	expected.put("Dog", new HashMap<String, Integer>());
    	expected.get("Dog").put("Dog", 1);
    	expected.get("Dog").put("Cat", 1);
    	expected.get("Dog").put("Horse", 1);
    	expected.get("Dog").put("Monkey", 1);
    	expected.get("Dog").put("Elephant", 0);

    	assertTrue(actual.getCounts().equals(expected));
    }
    
    @Test
    public void checkForAddition3() {
//        private List<String> realLabels4 = new ArrayList<>(Arrays.asList("Dog", "Dog", "Dog", "Dog"));
//        private List<String> predictedLabels4 = new ArrayList<>(Arrays.asList("Dog", "Dog", "Dog", "Dog"));
    	ConfusionMatrix cm3 = new ConfusionMatrix("", realLabels4, predictedLabels4);
//        private List<String> realLabels5 = new ArrayList<>(Arrays.asList("Dog", "Dog", "Dog", "Dog"));
//        private List<String> predictedLabels5 = new ArrayList<>(Arrays.asList("Dog", "Cat", "Dog", "Dog"))
    	ConfusionMatrix cm4 = new ConfusionMatrix("", realLabels5, predictedLabels5);
        ConfusionMatrix actual = cm3.add(cm4);
    	Map<String, Map<String, Integer>> expected = new HashMap<String, Map<String, Integer>>();
    	expected.put("Dog", new HashMap<String, Integer>());
//    	expected.put("Cat", new HashMap<String, Integer>());

    	expected.get("Dog").put("Dog", 7);
    	expected.get("Dog").put("Cat", 1);
//    	expected.get("Cat").put("Cat", 0);
//    	expected.get("Cat").put("Dog", 0);

    	assertTrue(actual.getCounts().equals(expected));
    }


    @Test
    public void testForEmptyListException() {
        try {
            new ConfusionMatrix("", Arrays.asList(new String[] {}), Arrays.asList(new String[] {}));
            Assert.fail("Did not get expected NPE");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testClassification1() throws AISPException {
    	String trainingLabel = "animal";
    	List<ILabeledDataWindow<double[]>> sounds = new ArrayList<>();
    	Properties label;
    	
    	for (int i = 0; i < realLabels1.size(); i++){
    		label = new Properties();
    		label.put(trainingLabel, realLabels1.get(i));
    		ILabeledDataWindow<double[]> sound = SoundTestUtils.createTrainingRecording(i, 100, 1000, label, false);
    		sounds.add(sound);
    	}
    	IFixedClassifier<double[]> testclassifier = new TestClassifier(sounds, predictedLabels1, trainingLabel);

    	ConfusionMatrix actual = ConfusionMatrix.compute(trainingLabel, testclassifier, sounds);
    	
    	Map<String, Map<String, Integer>> cmMap1 = new HashMap<String, Map<String, Integer>>();
    	cmMap1.put("Dog", new HashMap<String, Integer>());
    	cmMap1.get("Dog").put("Unicorn", 1);
//    	cmMap1.get("Dog").put("Cat", 0);
//    	cmMap1.get("Dog").put("Horse", 0);
//    	cmMap1.get("Dog").put("Monkey", 0);
    	cmMap1.get("Dog").put("Fish", 0);
//    	cmMap1.get("Dog").put("Dog", 0);
    	cmMap1.get("Dog").put("Moose", 0);
    	cmMap1.get("Dog").put("Mouse", 0);

    	
//    	cmMap1.put("Mouse", new HashMap<String, Integer>());
//    	cmMap1.get("Mouse").put("Unicorn", 0);
//    	cmMap1.get("Mouse").put("Cat", 0);
//    	cmMap1.get("Mouse").put("Horse", 0);
//    	cmMap1.get("Mouse").put("Monkey", 0);
//    	cmMap1.get("Mouse").put("Fish", 0);
//    	cmMap1.get("Mouse").put("Dog", 0);
//    	cmMap1.get("Mouse").put("Moose", 0);
//    	cmMap1.get("Mouse").put("Mouse", 0);
//
//    	
//    	cmMap1.put("Moose", new HashMap<String, Integer>());
//    	cmMap1.get("Moose").put("Unicorn", 0);
//    	cmMap1.get("Moose").put("Cat", 0);
//    	cmMap1.get("Moose").put("Horse", 0);
//    	cmMap1.get("Moose").put("Monkey", 0);
//    	cmMap1.get("Moose").put("Fish", 0);
//    	cmMap1.get("Moose").put("Dog", 0);
//    	cmMap1.get("Moose").put("Moose", 0);
//    	cmMap1.get("Moose").put("Mouse", 0);

    	
    	cmMap1.put("Cat", new HashMap<String, Integer>());
    	cmMap1.get("Cat").put("Unicorn", 0);
//    	cmMap1.get("Cat").put("Cat", 0);
//    	cmMap1.get("Cat").put("Horse", 0);
//    	cmMap1.get("Cat").put("Monkey", 0);
    	cmMap1.get("Cat").put("Fish", 1);
//    	cmMap1.get("Cat").put("Dog", 0);
    	cmMap1.get("Cat").put("Moose", 0);
    	cmMap1.get("Cat").put("Mouse", 0);

    	
    	cmMap1.put("Horse", new HashMap<String, Integer>());
    	cmMap1.get("Horse").put("Unicorn", 0);
//    	cmMap1.get("Horse").put("Cat", 0);
//    	cmMap1.get("Horse").put("Horse", 0);
//    	cmMap1.get("Horse").put("Monkey", 0);
    	cmMap1.get("Horse").put("Fish", 0);
//    	cmMap1.get("Horse").put("Dog", 0);
    	cmMap1.get("Horse").put("Moose", 1);
    	cmMap1.get("Horse").put("Mouse", 0);

    	
    	cmMap1.put("Monkey", new HashMap<String, Integer>());
    	cmMap1.get("Monkey").put("Unicorn", 0);
//    	cmMap1.get("Monkey").put("Cat", 0);
//    	cmMap1.get("Monkey").put("Horse", 0);
//    	cmMap1.get("Monkey").put("Monkey", 0);
    	cmMap1.get("Monkey").put("Fish", 0);
//    	cmMap1.get("Monkey").put("Dog", 0);
    	cmMap1.get("Monkey").put("Moose", 0);
    	cmMap1.get("Monkey").put("Mouse", 1);

    	
//    	cmMap1.put("Unicorn", new HashMap<String, Integer>());
//    	cmMap1.get("Unicorn").put("Unicorn", 0);
//    	cmMap1.get("Unicorn").put("Cat", 0);
//    	cmMap1.get("Unicorn").put("Horse", 0);
//    	cmMap1.get("Unicorn").put("Monkey", 0);
//    	cmMap1.get("Unicorn").put("Fish", 0);
//    	cmMap1.get("Unicorn").put("Dog", 0);
//    	cmMap1.get("Unicorn").put("Moose", 0);
//    	cmMap1.get("Unicorn").put("Mouse", 0);

    	
//    	cmMap1.put("Fish", new HashMap<String, Integer>());
//    	cmMap1.get("Fish").put("Unicorn", 0);
//    	cmMap1.get("Fish").put("Cat", 0);
//    	cmMap1.get("Fish").put("Horse", 0);
//    	cmMap1.get("Fish").put("Monkey", 0);
//    	cmMap1.get("Fish").put("Fish", 0);
//    	cmMap1.get("Fish").put("Dog", 0);
//    	cmMap1.get("Fish").put("Moose", 0);
//    	cmMap1.get("Fish").put("Mouse", 0);


    	if (!actual.getCounts().equals(cmMap1))
    		assertTrue(actual.getCounts().equals(cmMap1));
    }


    @Test
    public void testClassification2() throws AISPException {
    	String trainingLabel = "animal";
    	List<ILabeledDataWindow<double[]>> sounds = new ArrayList<>();
    	Properties label;
    	for (int i = 0; i < realLabels2.size(); i++){
    		label = new Properties();
    		label.put(trainingLabel, realLabels2.get(i));
    		ILabeledDataWindow<double[]> sound = SoundTestUtils.createTrainingRecording(i, 100, 1000, label, false);
    		sounds.add(sound);
    	}
    	IFixedClassifier<double[]> testclassifier = new TestClassifier(sounds, predictedLabels2, trainingLabel);

    	Map<String, Map<String, Integer>> cmMap2 = new HashMap<String, Map<String, Integer>>();
    	cmMap2.put("Dog", new HashMap<String, Integer>());
    	cmMap2.get("Dog").put("Dog", 1);
    	cmMap2.get("Dog").put("Cat", 1);
    	cmMap2.get("Dog").put("Horse", 1);
    	cmMap2.get("Dog").put("Monkey", 1);
//
//    	cmMap2.put("Cat", new HashMap<String, Integer>());
//    	cmMap2.get("Cat").put("Dog", 0);
//    	cmMap2.get("Cat").put("Cat", 0);
//    	cmMap2.get("Cat").put("Horse", 0);
//    	cmMap2.get("Cat").put("Monkey", 0);
//    	
//    	cmMap2.put("Horse", new HashMap<String, Integer>());
//    	cmMap2.get("Horse").put("Dog", 0);
//    	cmMap2.get("Horse").put("Cat", 0);
//    	cmMap2.get("Horse").put("Horse", 0);
//    	cmMap2.get("Horse").put("Monkey", 0);
//    	
//    	cmMap2.put("Monkey", new HashMap<String, Integer>());
//    	cmMap2.get("Monkey").put("Dog", 0);
//    	cmMap2.get("Monkey").put("Cat", 0);
//    	cmMap2.get("Monkey").put("Horse", 0);
//    	cmMap2.get("Monkey").put("Monkey", 0);

    	
    	ConfusionMatrix actual = ConfusionMatrix.compute(trainingLabel, testclassifier, sounds);
//    	System.out.println(actual.formatCounts());
    	assertTrue(actual.getCounts().equals(cmMap2));
    }


    @Test
    public void testClassification3() throws AISPException {
    	String trainingLabel = "animal";
    	List<ILabeledDataWindow<double[]>> sounds = new ArrayList<>();
    	Properties label;
    	for (int i = 0; i < realLabels3.size(); i++){
    		label = new Properties();
    		label.put(trainingLabel, realLabels3.get(i));
    		ILabeledDataWindow<double[]> sound = SoundTestUtils.createTrainingRecording(i, 100, 1000, label, false);
    		sounds.add(sound);
    	}
    	IFixedClassifier<double[]> testclassifier = new TestClassifier(sounds, predictedLabels3, trainingLabel);

    	ConfusionMatrix actual = ConfusionMatrix.compute(trainingLabel, testclassifier, sounds);
    	Map<String, Map<String, Integer>> cmMap3 = new HashMap<String, Map<String, Integer>>();
    	cmMap3.put("Cat", new HashMap<String, Integer>());
    	cmMap3.put("Elephant", new HashMap<String, Integer>());
    	cmMap3.put("Horse", new HashMap<String, Integer>());
    	cmMap3.put("Monkey", new HashMap<String, Integer>());


    	cmMap3.get("Cat").put("Elephant", 0);
    	cmMap3.get("Cat").put("Cat", 1);
    	cmMap3.get("Cat").put("Horse", 0);
    	cmMap3.get("Cat").put("Monkey", 0);
    	
  
    	cmMap3.get("Elephant").put("Elephant", 1);
    	cmMap3.get("Elephant").put("Cat", 0);
    	cmMap3.get("Elephant").put("Horse", 0);
    	cmMap3.get("Elephant").put("Monkey", 0);


    	
    	cmMap3.get("Horse").put("Elephant", 0);
    	cmMap3.get("Horse").put("Cat", 0);
    	cmMap3.get("Horse").put("Horse", 1);
    	cmMap3.get("Horse").put("Monkey", 0);

    	cmMap3.get("Monkey").put("Elephant", 0);
    	cmMap3.get("Monkey").put("Cat", 0);
    	cmMap3.get("Monkey").put("Horse", 0);
    	cmMap3.get("Monkey").put("Monkey", 1);

    	if (!actual.getCounts().equals(cmMap3))
    		assertTrue(actual.getCounts().equals(cmMap3));
    }


    @Test
    public void testClassification4() throws AISPException {
    	String trainingLabel = "animal";
    	List<ILabeledDataWindow<double[]>> sounds = new ArrayList<>();
    	Properties label;
    	for (int i = 0; i < realLabels4.size(); i++){
    		label = new Properties();
    		label.put(trainingLabel, realLabels4.get(i));
    		ILabeledDataWindow<double[]> sound = SoundTestUtils.createTrainingRecording(i, 100, 1000, label, false);
    		sounds.add(sound);
    	}
    	IFixedClassifier<double[]> testclassifier = new TestClassifier(sounds, predictedLabels4, trainingLabel);

    	ConfusionMatrix actual = ConfusionMatrix.compute(trainingLabel, testclassifier, sounds);
    	Map<String, Map<String, Integer>> cmMap4 = new HashMap<String, Map<String, Integer>>();
    	cmMap4.put("Dog", new HashMap<String, Integer>());
    	cmMap4.get("Dog").put("Dog", 4);
    	assertTrue(actual.getCounts().equals(cmMap4));    }

    @Test
    public void testClassification5() throws AISPException {
    	String trainingLabel = "animal";
    	List<ILabeledDataWindow<double[]>> sounds = new ArrayList<>();
    	Properties label;
    	for (int i = 0; i < realLabels5.size(); i++){
    		label = new Properties();
    		label.put(trainingLabel, realLabels5.get(i));
    		ILabeledDataWindow<double[]> sound = SoundTestUtils.createTrainingRecording(i, 100, 1000, label, false);
    		sounds.add(sound);
    	}
    	IFixedClassifier<double[]> testclassifier = new TestClassifier(sounds, predictedLabels5, trainingLabel);

    	ConfusionMatrix actual = ConfusionMatrix.compute(trainingLabel, testclassifier, sounds);
    	Map<String, Map<String, Integer>> cmMap5 = new HashMap<String, Map<String, Integer>>();
    	cmMap5.put("Dog", new HashMap<String, Integer>());
    	cmMap5.get("Dog").put("Dog", 3);
    	cmMap5.get("Dog").put("Cat", 1);
    	
//    	cmMap5.put("Cat", new HashMap<String, Integer>());
//    	cmMap5.get("Cat").put("Dog", 0);
//    	cmMap5.get("Cat").put("Cat", 0);

//    	System.out.println(actual.formatCounts());
    	assertTrue(actual.getCounts().equals(cmMap5));    }

    @Test
    public void testClassification6() throws AISPException {
    	String trainingLabel = "animal";
    	List<ILabeledDataWindow<double[]>> sounds = new ArrayList<>();
    	Properties label;
    	for (int i = 0; i < realLabels6.size(); i++){
    		label = new Properties();
    		label.put(trainingLabel, realLabels6.get(i));
    		ILabeledDataWindow<double[]> sound = SoundTestUtils.createTrainingRecording(i, 100, 1000, label, false);
    		sounds.add(sound);
    	}
    	IFixedClassifier<double[]> testclassifier = new TestClassifier(sounds, predictedLabels6, trainingLabel);

    	ConfusionMatrix actual = ConfusionMatrix.compute(trainingLabel, testclassifier, sounds);
    	Map<String, Map<String, Integer>> cmMap6 = new HashMap<String, Map<String, Integer>>();
    	cmMap6.put("A", new HashMap<String, Integer>());
    	cmMap6.put("B", new HashMap<String, Integer>());
    	cmMap6.put("C", new HashMap<String, Integer>());
    	cmMap6.put("D", new HashMap<String, Integer>());

    	cmMap6.get("A").put("A", 1);
    	cmMap6.get("A").put("B", 0);
    	cmMap6.get("A").put("C", 0);
    	cmMap6.get("A").put("D", 0);
    	
    	cmMap6.get("B").put("A", 0);
    	cmMap6.get("B").put("B", 1);
    	cmMap6.get("B").put("C", 0);
    	cmMap6.get("B").put("D", 0);
    	
    	cmMap6.get("C").put("A", 0);
    	cmMap6.get("C").put("B", 0);
    	cmMap6.get("C").put("C", 1);
    	cmMap6.get("C").put("D", 0);

    	cmMap6.get("D").put("A", 0);
    	cmMap6.get("D").put("B", 0);
    	cmMap6.get("D").put("C", 0);
    	cmMap6.get("D").put("D", 1);

    	assertTrue(actual.getCounts().equals(cmMap6));
    }

    @Test
    public void testClassification7() throws AISPException {
    	String trainingLabel = "animal";
    	List<ILabeledDataWindow<double[]>> sounds = new ArrayList<>();
    	Properties label;
    	for (int i = 0; i < realLabels7.size(); i++){
    		label = new Properties();
    		label.put(trainingLabel, realLabels7.get(i));
    		ILabeledDataWindow<double[]> sound = SoundTestUtils.createTrainingRecording(i, 100, 1000, label, false);
    		sounds.add(sound);
    	}
    	IFixedClassifier<double[]> testclassifier = new TestClassifier(sounds, predictedLabels7, trainingLabel);

    	ConfusionMatrix actual = ConfusionMatrix.compute(trainingLabel, testclassifier, sounds);
    	assertArrayEquals(expected7, actual.getCountsArray());
    	assertArrayEquals(expectedpcnt7, actual.getPercentsArray(Normalization.Recall));
    }

    @Test
    public void testClassificationAddition1() throws AISPException {
    	String trainingLabel = "animal";
    	List<ILabeledDataWindow<double[]>> sounds1 = new ArrayList<>();
    	List<ILabeledDataWindow<double[]>> sounds2 = new ArrayList<>();
    	Properties label1;
    	Properties label2;
    	for (int i = 0; i < realLabels1.size(); i++){
    		label1 = new Properties();
    		label1.put(trainingLabel, realLabels1.get(i));
    		ILabeledDataWindow<double[]> sound = SoundTestUtils.createTrainingRecording(i, 100, 1000, label1, false);
    		sounds1.add(sound);
    	}
    	IFixedClassifier<double[]> testclassifier1 = new TestClassifier(sounds1, predictedLabels1, trainingLabel);

    	for (int i = 0; i < realLabels2.size(); i++){
    		label2 = new Properties();
    		label2.put(trainingLabel, realLabels2.get(i));
    		ILabeledDataWindow<double[]> sound = SoundTestUtils.createTrainingRecording(i, 100, 1000, label2, false);
    		sounds2.add(sound);
    	}
    	IFixedClassifier<double[]> testclassifier2 = new TestClassifier(sounds2, predictedLabels2, trainingLabel);

    	ConfusionMatrix cm1 = ConfusionMatrix.compute(trainingLabel, testclassifier1, sounds1);
    	ConfusionMatrix cm2 = ConfusionMatrix.compute(trainingLabel, testclassifier2, sounds2);
        ConfusionMatrix actual = cm1.add(cm2);
        Map<String, Map<String, Integer>> expected = new HashMap<String, Map<String, Integer>>();
        expected.put("Dog", new HashMap<String, Integer>());
        expected.get("Dog").put("Dog", 1);
        expected.get("Dog").put("Cat", 1);
        expected.get("Dog").put("Horse", 1);
        expected.get("Dog").put("Monkey", 1);
        expected.get("Dog").put("Fish", 0);
        expected.get("Dog").put("Moose", 0);
        expected.get("Dog").put("Mouse", 0);
        expected.get("Dog").put("Unicorn", 1);

        
//        expected.put("Mouse", new HashMap<String, Integer>());
//        expected.get("Mouse").put("Dog", 0);
//        expected.get("Mouse").put("Cat", 0);
//        expected.get("Mouse").put("Horse", 0);
//        expected.get("Mouse").put("Monkey", 0);
//        expected.get("Mouse").put("Fish", 0);
//        expected.get("Mouse").put("Moose", 0);
//        expected.get("Mouse").put("Mouse", 0);
//        expected.get("Mouse").put("Unicorn", 0);

        	
        expected.put("Cat", new HashMap<String, Integer>());
        expected.get("Cat").put("Dog", 0);
        expected.get("Cat").put("Cat", 0);
        expected.get("Cat").put("Horse", 0);
        expected.get("Cat").put("Monkey", 0);
        expected.get("Cat").put("Fish", 1);
        expected.get("Cat").put("Moose", 0);
        expected.get("Cat").put("Mouse", 0);
        expected.get("Cat").put("Unicorn", 0);

        
        expected.put("Horse", new HashMap<String, Integer>());
        expected.get("Horse").put("Dog", 0);
        expected.get("Horse").put("Cat", 0);
        expected.get("Horse").put("Horse", 0);
        expected.get("Horse").put("Monkey", 0);
        expected.get("Horse").put("Fish", 0);
        expected.get("Horse").put("Moose", 1);
        expected.get("Horse").put("Mouse", 0);
        expected.get("Horse").put("Unicorn", 0);

        
        expected.put("Monkey", new HashMap<String, Integer>());
        expected.get("Monkey").put("Dog", 0);
        expected.get("Monkey").put("Cat", 0);
        expected.get("Monkey").put("Horse", 0);
        expected.get("Monkey").put("Monkey", 0);
        expected.get("Monkey").put("Fish", 0);
        expected.get("Monkey").put("Moose", 0);
        expected.get("Monkey").put("Mouse", 1);
        expected.get("Monkey").put("Unicorn", 0);
        
//        expected.put("Fish", new HashMap<String, Integer>());
//        expected.get("Fish").put("Dog", 0);
//        expected.get("Fish").put("Cat", 0);
//        expected.get("Fish").put("Horse", 0);
//        expected.get("Fish").put("Monkey", 0);
//        expected.get("Fish").put("Fish", 0);
//        expected.get("Fish").put("Moose", 0);
//        expected.get("Fish").put("Mouse", 0);
//        expected.get("Fish").put("Unicorn", 0);
//
//        
//        expected.put("Unicorn", new HashMap<String, Integer>());
//        expected.get("Unicorn").put("Dog", 0);
//        expected.get("Unicorn").put("Cat", 0);
//        expected.get("Unicorn").put("Horse", 0);
//        expected.get("Unicorn").put("Monkey", 0);
//        expected.get("Unicorn").put("Fish", 0);
//        expected.get("Unicorn").put("Moose", 0);
//        expected.get("Unicorn").put("Mouse", 0);
//        expected.get("Unicorn").put("Unicorn", 0);
//        
//        expected.put("Moose", new HashMap<String, Integer>());
//        expected.get("Moose").put("Dog", 0);
//        expected.get("Moose").put("Cat", 0);
//        expected.get("Moose").put("Horse", 0);
//        expected.get("Moose").put("Monkey", 0);
//        expected.get("Moose").put("Fish", 0);
//        expected.get("Moose").put("Moose", 0);
//        expected.get("Moose").put("Mouse", 0);
//        expected.get("Moose").put("Unicorn", 0);
//    	System.out.println(actual.formatCounts());
        assertTrue(actual.getCounts().equals(expected));
    }

    @Test	
    public void testClassificationAddition2() throws AISPException {
    	String trainingLabel = "animal";
    	List<ILabeledDataWindow<double[]>> sounds2 = new ArrayList<>();
    	List<ILabeledDataWindow<double[]>> sounds3 = new ArrayList<>();
    	Properties label2;
    	Properties label3;
    	for (int i = 0; i < realLabels2.size(); i++){
    		label2 = new Properties();
    		label2.put(trainingLabel, realLabels2.get(i));
    		ILabeledDataWindow<double[]> sound = SoundTestUtils.createTrainingRecording(i, 100, 1000, label2, false);
    		sounds2.add(sound);
    	}
    	IFixedClassifier<double[]> testclassifier2 = new TestClassifier(sounds2, predictedLabels2, trainingLabel);

    	for (int i = 0; i < realLabels3.size(); i++){
    		label3 = new Properties();
    		label3.put(trainingLabel, realLabels3.get(i));
    		ILabeledDataWindow<double[]> sound = SoundTestUtils.createTrainingRecording(i, 100, 1000, label3, false);
    		sounds3.add(sound);
    	}
    	IFixedClassifier<double[]> testclassifier3 = new TestClassifier(sounds3, predictedLabels3, trainingLabel);

    	
    	ConfusionMatrix cm2 = ConfusionMatrix.compute(trainingLabel, testclassifier2, sounds2);
    	ConfusionMatrix cm3 = ConfusionMatrix.compute(trainingLabel, testclassifier3, sounds3);
        ConfusionMatrix actual = cm2.add(cm3);
//        System.out.println("cm2:\n" + cm2.formatCounts());
//        System.out.println("cm3:\n" + cm3.formatCounts());
//        System.out.println("actual:\n" + actual.formatCounts());
    	Map<String, Map<String, Integer>> expected = new HashMap<String, Map<String, Integer>>();
    	expected.put("Elephant", new HashMap<String, Integer>());
    	expected.put("Cat", new HashMap<String, Integer>());
    	expected.put("Horse", new HashMap<String, Integer>());
    	expected.put("Monkey", new HashMap<String, Integer>());

    	expected.get("Elephant").put("Elephant", 1);
    	expected.get("Elephant").put("Cat", 0);
    	expected.get("Elephant").put("Horse", 0);
    	expected.get("Elephant").put("Monkey", 0);
    	expected.get("Elephant").put("Dog", 0);


    	
    	expected.get("Cat").put("Elephant",0);
    	expected.get("Cat").put("Cat", 1);
    	expected.get("Cat").put("Horse", 0);
    	expected.get("Cat").put("Monkey", 0);
    	expected.get("Cat").put("Dog", 0);

    	
    	expected.get("Horse").put("Elephant",0);
    	expected.get("Horse").put("Cat", 0);
    	expected.get("Horse").put("Horse", 1);
    	expected.get("Horse").put("Monkey", 0);
    	expected.get("Horse").put("Dog", 0);

    	
    	expected.get("Monkey").put("Elephant",0);
    	expected.get("Monkey").put("Cat", 0);
    	expected.get("Monkey").put("Horse", 0);
    	expected.get("Monkey").put("Monkey", 1);
    	expected.get("Monkey").put("Dog", 0);

    	
    	expected.put("Dog", new HashMap<String, Integer>());
    	expected.get("Dog").put("Dog", 1);
    	expected.get("Dog").put("Cat", 1);
    	expected.get("Dog").put("Horse", 1);
    	expected.get("Dog").put("Monkey", 1);
    	expected.get("Dog").put("Elephant", 0);

    	
    	if (!actual.getCounts().equals(expected))
    			assertTrue(actual.getCounts().equals(expected));
    }
    
    @Test	
    public void testForCorrectnessZeroLabelMatchMap() { 		
    	Map<String, Map<String, Integer>> cmMap1 = new HashMap<String, Map<String, Integer>>();
//    	cmMap1.put("Mouse", new HashMap<String, Integer>());
//    	cmMap1.get("Mouse").put("Mouse", 0);
//    	cmMap1.get("Mouse").put("Unicorn", 0);
//    	cmMap1.get("Mouse").put("Cat", 0);
//    	cmMap1.get("Mouse").put("Horse", 0);
//    	cmMap1.get("Mouse").put("Fish", 0);
//    	cmMap1.get("Mouse").put("Dog", 0);
//    	cmMap1.get("Mouse").put("Moose", 0);
//    	cmMap1.get("Mouse").put("Monkey", 0);


//    	cmMap1.put("Unicorn", new HashMap<String, Integer>());
//    	cmMap1.get("Unicorn").put("Mouse", 0);
//    	cmMap1.get("Unicorn").put("Unicorn", 0);
//    	cmMap1.get("Unicorn").put("Cat", 0);
//    	cmMap1.get("Unicorn").put("Horse", 0);
//    	cmMap1.get("Unicorn").put("Fish", 0);
//    	cmMap1.get("Unicorn").put("Dog", 0);
//    	cmMap1.get("Unicorn").put("Moose", 0);
//    	cmMap1.get("Unicorn").put("Monkey", 0);


    	cmMap1.put("Cat", new HashMap<String, Integer>());
    	cmMap1.get("Cat").put("Mouse", 0);
    	cmMap1.get("Cat").put("Unicorn", 0);
//    	cmMap1.get("Cat").put("Cat", 0);
//    	cmMap1.get("Cat").put("Horse", 0);
    	cmMap1.get("Cat").put("Fish", 1);
//    	cmMap1.get("Cat").put("Dog", 0);
    	cmMap1.get("Cat").put("Moose", 0);
//    	cmMap1.get("Cat").put("Monkey", 0);

    	
    	cmMap1.put("Dog", new HashMap<String, Integer>());
    	cmMap1.get("Dog").put("Unicorn", 1);
    	cmMap1.get("Dog").put("Mouse", 0);
//    	cmMap1.get("Dog").put("Cat", 0);
//    	cmMap1.get("Dog").put("Horse", 0);
    	cmMap1.get("Dog").put("Fish", 0);
//    	cmMap1.get("Dog").put("Dog", 0);
    	cmMap1.get("Dog").put("Moose", 0);
//    	cmMap1.get("Dog").put("Monkey", 0);

    	
//    	cmMap1.put("Fish", new HashMap<String, Integer>());
//    	cmMap1.get("Fish").put("Mouse", 0);
//    	cmMap1.get("Fish").put("Unicorn", 0);
//    	cmMap1.get("Fish").put("Cat", 0);
//    	cmMap1.get("Fish").put("Horse", 0);
//    	cmMap1.get("Fish").put("Fish", 0);
//    	cmMap1.get("Fish").put("Dog", 0);
//    	cmMap1.get("Fish").put("Moose", 0);
//    	cmMap1.get("Fish").put("Monkey", 0);

    	
    	cmMap1.put("Horse", new HashMap<String, Integer>());
    	cmMap1.get("Horse").put("Moose", 1);
    	cmMap1.get("Horse").put("Unicorn", 0);
//    	cmMap1.get("Horse").put("Cat", 0);
//    	cmMap1.get("Horse").put("Horse", 0);
    	cmMap1.get("Horse").put("Fish", 0);
//    	cmMap1.get("Horse").put("Dog", 0);
    	cmMap1.get("Horse").put("Mouse", 0);
//    	cmMap1.get("Horse").put("Monkey", 0);

    	
    	cmMap1.put("Monkey", new HashMap<String, Integer>());
    	cmMap1.get("Monkey").put("Mouse", 1);
    	cmMap1.get("Monkey").put("Unicorn", 0);
//    	cmMap1.get("Monkey").put("Cat", 0);
//    	cmMap1.get("Monkey").put("Horse", 0);
    	cmMap1.get("Monkey").put("Fish", 0);
//    	cmMap1.get("Monkey").put("Dog", 0);
    	cmMap1.get("Monkey").put("Moose", 0);
//    	cmMap1.get("Monkey").put("Monkey", 0);

    	
//    	cmMap1.put("Moose", new HashMap<String, Integer>());
//    	cmMap1.get("Moose").put("Mouse", 0);
//    	cmMap1.get("Moose").put("Unicorn", 0);
//    	cmMap1.get("Moose").put("Cat", 0);
//    	cmMap1.get("Moose").put("Horse", 0);
//    	cmMap1.get("Moose").put("Fish", 0);
//    	cmMap1.get("Moose").put("Dog", 0);
//    	cmMap1.get("Moose").put("Moose", 0);
//    	cmMap1.get("Moose").put("Monkey", 0);

    	
    	ConfusionMatrix cm1 = new ConfusionMatrix("", realLabels1, predictedLabels1);
    	
    	assertTrue(cm1.getCounts().equals(cmMap1));
    }
    

    @Test
    public void testForCorrectnessOneLabelMatchMap(){
    	Map<String, Map<String, Integer>> cmMap2 = new HashMap<String, Map<String, Integer>>();
    	cmMap2.put("Dog", new HashMap<String, Integer>());
    	cmMap2.get("Dog").put("Dog", 1);
    	cmMap2.get("Dog").put("Cat", 1);
    	cmMap2.get("Dog").put("Horse", 1);
    	cmMap2.get("Dog").put("Monkey", 1);

    	ConfusionMatrix cm2 = new ConfusionMatrix("",realLabels2, predictedLabels2);
    	Assert.assertTrue(cm2.getCounts().get("Dog").get("Dog").equals(1));
    	Assert.assertTrue(cm2.getCounts().get("Dog").get("Cat").equals(1));
    	Assert.assertTrue(cm2.getCounts().get("Dog").get("Horse").equals(1));
    	Assert.assertTrue(cm2.getCounts().get("Dog").get("Monkey").equals(1));
    }
    
    @Test	
    public void testForCorrectnessAllLabelMatchMap() {
    	Map<String, Map<String, Integer>> cmMap3 = new HashMap<String, Map<String, Integer>>();
    	cmMap3.put("Elephant", new HashMap<String, Integer>());
    	cmMap3.put("Cat", new HashMap<String, Integer>());
    	cmMap3.put("Horse", new HashMap<String, Integer>());
    	cmMap3.put("Monkey", new HashMap<String, Integer>());



    	cmMap3.get("Elephant").put("Elephant", 1);
    	cmMap3.get("Elephant").put("Cat", 0);
    	cmMap3.get("Elephant").put("Horse", 0);
    	cmMap3.get("Elephant").put("Monkey", 0);
    	cmMap3.get("Cat").put("Elephant", 0);
    	cmMap3.get("Cat").put("Cat", 1);
    	cmMap3.get("Cat").put("Horse", 0);
    	cmMap3.get("Cat").put("Monkey", 0);
    	cmMap3.get("Horse").put("Horse", 1);
    	cmMap3.get("Horse").put("Elephant", 0);
    	cmMap3.get("Horse").put("Cat", 0);
    	cmMap3.get("Horse").put("Monkey", 0);
    	cmMap3.get("Monkey").put("Elephant", 0);
    	cmMap3.get("Monkey").put("Cat", 0);
    	cmMap3.get("Monkey").put("Horse", 0);
    	cmMap3.get("Monkey").put("Monkey", 1);
    	
    	ConfusionMatrix cm3 = new ConfusionMatrix("", realLabels3, predictedLabels3);
    	assertTrue(cm3.getCounts().equals(cmMap3));

    }
    
    @Test
    public void testForCorrectnessAllSameLabelMap() {
    	Map<String, Map<String, Integer>> cmMap4 = new HashMap<String, Map<String, Integer>>();
    	cmMap4.put("Dog", new HashMap<String, Integer>());
    	cmMap4.get("Dog").put("Dog", 4);

    	ConfusionMatrix cm4 = new ConfusionMatrix("", realLabels4, realLabels4);
    	assertTrue(cm4.getCounts().equals(cmMap4));
    }
    
    
    @Test
    public void testForCorrectnessAllLabelMatchExceptOneMap() {
    	Map<String, Map<String, Integer>> cmMap5 = new HashMap<String, Map<String, Integer>>();
    	cmMap5.put("Dog", new HashMap<String, Integer>());
    	cmMap5.get("Dog").put("Dog", 3);
    	cmMap5.get("Dog").put("Cat", 1);
    	
//    	cmMap5.put("Cat", new HashMap<String, Integer>());
//    	cmMap5.get("Cat").put("Cat", 0);
//    	cmMap5.get("Cat").put("Dog", 0);

    	ConfusionMatrix cm5 = new ConfusionMatrix("", realLabels5, predictedLabels5);
    	assertTrue(cm5.getCounts().equals(cmMap5));

    }
    
    @Test
    public void testForOneCharLabelsMap() {
    	Map<String, Map<String, Integer>> cmMap6 = new HashMap<String, Map<String, Integer>>();
    	cmMap6.put("A", new HashMap<String, Integer>());
    	cmMap6.put("B", new HashMap<String, Integer>());
    	cmMap6.put("C", new HashMap<String, Integer>());
    	cmMap6.put("D", new HashMap<String, Integer>());

    	cmMap6.get("A").put("A", 1);
    	cmMap6.get("A").put("B", 0);
    	cmMap6.get("A").put("C", 0);
    	cmMap6.get("A").put("D", 0);

    	
    	cmMap6.get("B").put("B", 1);
    	cmMap6.get("B").put("A", 0);
    	cmMap6.get("B").put("C", 0);
    	cmMap6.get("B").put("D", 0);
    	
    	cmMap6.get("C").put("C", 1);
    	cmMap6.get("C").put("A", 0);
    	cmMap6.get("C").put("B", 0);
    	cmMap6.get("C").put("D", 0);
    	
    	cmMap6.get("D").put("D", 1);
    	cmMap6.get("D").put("A", 0);
    	cmMap6.get("D").put("B", 0);
    	cmMap6.get("D").put("C", 0);


    	ConfusionMatrix cm6 = new ConfusionMatrix("", realLabels6, predictedLabels6);

    	assertTrue(cm6.getCounts().equals(cmMap6));

    }
    
    @Test
    public void testForUndefinedLabelValueMap() {
    	ConfusionMatrix cm7 = new ConfusionMatrix("", realLabels7, predictedLabels7);
    	Map<String, Map<String, Integer>> cmMap7 = new HashMap<String, Map<String, Integer>>();

    	cmMap7.put("Dog", new HashMap<String, Integer>());
    	cmMap7.put("Cat", new HashMap<String, Integer>());

    	cmMap7.get("Dog").put("Dog", 2);
    	cmMap7.get("Dog").put("Cat", 0);
    	cmMap7.get("Dog").put("", 0);

    	cmMap7.get("Cat").put("Cat", 1);
    	cmMap7.get("Cat").put("Dog", 0);
    	cmMap7.get("Cat").put("", 1);
    	
//    	System.out.println(cm7.formatCounts());
    	assertTrue(cm7.getCounts().equals(cmMap7));

    }
    
    
    private static void addLabels(List<String> list, int count, String value) {
    	for (int i=0 ; i<count; i++)
    		list.add(value);
    }
    

    @Test
    public void testStats(){
    	String labelName = "anything";
    	ConfusionMatrix cm;
    	List<String> predictedLabels = new ArrayList<String>();
    	final List<String> realLabels = new ArrayList<String>();

    	addLabels(realLabels, 10, "a");
    	addLabels(realLabels, 10, "b");
    	addLabels(realLabels, 10, "c");
    	addLabels(realLabels, 10, "d");
    	addLabels(realLabels, 60, "x");	// X is used to just spray in amongst the a,b,c and d labels
    	final int totalLabels = 100;
    	
    	// 100% accuracy
    	predictedLabels.addAll(realLabels);
    	cm = new ConfusionMatrix(labelName, realLabels, predictedLabels);    	
    	//   actual \/   predicted->|  a  |  b  |  c  |  d  |  x 
    	//     a        			| 10  |  0  |  0  |  0  |  0
    	//     b        			|  0  | 10  |  0  |  0  |  0
    	//     c        			|  0  |  0  | 10  |  0  |  0
    	//     d        			|  0  |  0  |  0  | 10  |  0
    	//     x        			|  0  |  0  |  0  |  0  |  60 
    	Assert.assertTrue(cm.getAccuracy().getMean() == 1.0);
    	Assert.assertTrue(cm.getRecall().getMean() == 1.0);
    	Assert.assertTrue(cm.getPrecision().getMean() == 1.0);

    	// 0% accuracy everything recognized as x
    	predictedLabels.clear();
    	addLabels(predictedLabels, totalLabels, "x");
    	cm = new ConfusionMatrix(labelName, realLabels, predictedLabels);    	
    	//   actual \/   predicted->|  a  |  b  |  c  |  d  |  x 
    	//     a        			|  0  |  0  |  0  |  0  |  10
    	//     b        			|  0  |  0  |  0  |  0  |  10
    	//     c        			|  0  |  0  |  0  |  0  |  10
    	//     d        			|  0  |  0  |  0  |  0  |  10
    	//     x        			|  0  |  0  |  0  |  0  |  60 
    	validateStats(cm, "a", 0.0, 0.0);	// a=90/100, p=0/10, r=0/0
    	validateStats(cm, "b", 0.0, 0.0);	// a=90/100, p=0/10, r=0/0
    	validateStats(cm, "c", 0.0, 0.0);	// a=90/100, p=0/10, r=0/0
    	validateStats(cm, "d", 0.0, 0.0);	// a=90/100, p=0/10, r=0/0
    	
    	// 95% accuracy, 100% precision, 50% recall
    	predictedLabels.clear();
    	addLabels(predictedLabels, 5, "a");
    	addLabels(predictedLabels, 5, "x");
    	addLabels(predictedLabels, 5, "b");
    	addLabels(predictedLabels, 5, "x");
    	addLabels(predictedLabels, 5, "c");
    	addLabels(predictedLabels, 5, "x");
    	addLabels(predictedLabels, 5, "d");
    	addLabels(predictedLabels, 5, "x");
    	addLabels(predictedLabels, 60, "x");
    	cm = new ConfusionMatrix(labelName, realLabels, predictedLabels);    	
    	//   actual \/   predicted->|  a  |  b  |  c  |  d  |  x 
    	//     a        			|  5  |  0  |  0  |  0  |  5
    	//     b        			|  0  |  5  |  0  |  0  |  5 
    	//     c        			|  0  |  0  |  5  |  0  |  5 
    	//     d        			|  0  |  0  |  0  |  5  |  5 
    	//     x        			|  0  |  0  |  0  |  0  |  60 
    	validateStats(cm, "a", 1.0, .5);	// a=95//100, p=5/5, r=5/10
    	validateStats(cm, "b", 1.0, .5);	// a=95//100, p=5/5, r=5/10;
    	validateStats(cm, "c", 1.0, .5);	// a=95//100, p=5/5, r=5/10;
    	validateStats(cm, "d", 1.0, .5);	// a=95//100, p=5/5, r=5/10;
    
    	//  non-100% precision
    	predictedLabels.clear();
    	addLabels(predictedLabels, totalLabels, "a");
    	cm = new ConfusionMatrix(labelName, realLabels, predictedLabels);    	
    	//   actual \/   predicted->|  a  |  b  |  c  |  d  |  x 
    	//     a        			| 10  |  0  |  0  |  0  |  0
    	//     b        			| 10  |  0  |  0  |  0  |  0 
    	//     c        			| 10  |  0  |  0  |  0  |  0 
    	//     d        			| 10  |  0  |  0  |  0  |  0 
    	//     x        			| 60  |  0  |  0  |  0  |  0 
    	validateStats(cm, "a", 0.1, 1.0);	// a=10/100, p=10/100, r=10/10;
    	validateStats(cm, "b", 0.0, 0.0);	// a=90/100, p=0/0, r=0/10;
    	validateStats(cm, "c", 0.0, 0.0);	// a=90/100, p=0/0, r=0/10;
    	validateStats(cm, "d", 0.0, 0.0);	// a=90/100, p=0/0, r=0/10;

    	//  accuracy != recall 
    	predictedLabels.clear();
    	addLabels(predictedLabels, 10, "a"); // as as a
    	addLabels(predictedLabels, 5, "b");	// b as b
    	addLabels(predictedLabels, 5, "a");	// b as a
    	addLabels(predictedLabels, 5, "c");	// c as c
    	addLabels(predictedLabels, 5, "a");	// c as a
    	addLabels(predictedLabels, 5, "d"); // d as d
    	addLabels(predictedLabels, 5, "a");	// d as a
    	addLabels(predictedLabels, 60, "x");
    	cm = new ConfusionMatrix(labelName, realLabels, predictedLabels);    	
    	//   actual \/   predicted->|  a  |  b  |  c  |  d  |  x 
    	//     a        			| 10  |  0  |  0  |  0  |  0
    	//     b        			|  5  |  5  |  0  |  0  |  0 
    	//     c        			|  5  |  0  |  5  |  0  |  0 
    	//     d        			|  5  |  0  |  0  |  5  |  0 
    	//     x        			|  0  |  0  |  0  |  0  |  60 
    	validateStats(cm, "a", 10./25, 1.0);	// a=85/100, p=10/25, r=10/10;
    	validateStats(cm, "b", 1.0, 0.5);	// a=95/100, p=5/5, r=5/10;
    	validateStats(cm, "c", 1.0, 0.5);	// a=95/100, p=5/5, r=5/10;
    	validateStats(cm, "d", 1.0, 0.5);	// a=95/100, p=5/5, r=5/10;



    }

    @Test
    public void testMicroMacro(){
    	String labelName = "anything";
    	ConfusionMatrix cm;
    	List<String> predictedLabels = new ArrayList<String>();
    	final List<String> realLabels = new ArrayList<String>();

    	addLabels(realLabels, 10, "a");
    	addLabels(realLabels, 10, "b");
    	
    	// 100% accuracy
    	predictedLabels.addAll(realLabels);
    	cm = new ConfusionMatrix(labelName, realLabels, predictedLabels);    	
    	
    	// Data is balanced, so macro and micro values should be the same
    	Assert.assertTrue(cm.getRecall(true).equals(cm.getRecall(false)));
    	Assert.assertTrue(cm.getPrecision(true).equals(cm.getPrecision(false)));
    	Assert.assertTrue(cm.getF1Score(true).equals(cm.getF1Score(false)));
    	
    	predictedLabels.clear();
    	realLabels.clear();
    	addLabels(realLabels, 10, "a");
    	addLabels(realLabels, 90, "b");
    	addLabels(predictedLabels, 10, "a");
    	addLabels(predictedLabels, 90, "a");
    	//   actual \/   predicted->|  a  |  b  
    	//     a        			|  10 |  0  
    	//     b        			|  90 |  0  
    	cm = new ConfusionMatrix(labelName, realLabels, predictedLabels);    	
    	validateStats(cm, "a", 0.1, 1.0);		// p=10/100, r=10/10;
    	validateStats(cm, "b", 0.0, 0.0);		// p=0/0,    r=0/90;
    	Assert.assertTrue(cm.getPrecision(true).getMean() == 0.1); // 10/100 
    	Assert.assertTrue(cm.getPrecision(false).getMean() == 0.05);// (.1  + 0) / 2;	
    	Assert.assertTrue(cm.getRecall(true).getMean() == 0.1); 	// 10/100 
    	Assert.assertTrue(cm.getRecall(false).getMean() == 0.5); 	// (1  + 0) / 2;	
    }
    
    	
	private void validateStats(ConfusionMatrix cm, String label, double precision, double recall) {
//		if (accuracy >= 0) {
//			double v = cm.getAccuracy(label);
//			Assert.assertTrue("computed accurancy for label " + label + " " + v + " not equal to expected " + accuracy, v == accuracy);
//		}
		if (precision >= 0) {
			double v = cm.getPrecision(label);
			Assert.assertTrue("computed precision for label " + label + " " + v + " not equal to expected " + precision, v == precision);
		}
		if (recall >= 0) {
			double v = cm.getRecall(label);
			Assert.assertTrue("computed recall for label " + label + " " +  v + " not equal to expected " + recall, v == recall);
		}
	} 
	
	@Test
	public void testMissingTestData() {
		// Model trained on a,b,c
		// Test data has only a and b
		// Predic
		String[] actual    = new String[] { "a", "a", "b", "b", "b", "b" };
		String[] predicted = new String[] { "a", "a", "b", "a", "c", "c" };
		List<String> actualL = Arrays.asList(actual);
		List<String> predictedL = Arrays.asList(predicted);
		
		ConfusionMatrix cm = new ConfusionMatrix("mylabel", actualL, predictedL);
		System.out.println(cm.formatCounts());
		System.out.println(cm.formatStats());

		Assert.assertTrue(cm.getPrecision("a") == 2.0/3.0);
		Assert.assertTrue(cm.getRecall("a") == 1.00);
		Assert.assertTrue(cm.getPrecision("b") == 1.00);
		Assert.assertTrue(cm.getRecall("b") == .25);
		try { 
			Double.isNaN(cm.getPrecision("c")); 
			Assert.fail("Did not get exeption on unknown label");
		} catch (Exception e) { }

		try { 
			Double.isNaN(cm.getRecall("c")); 
			Assert.fail("Did not get exeption on unknown label");
		} catch (Exception e) { }

		Assert.assertTrue(cm.getPrecision(true).getMean() == 0.50); 
		Assert.assertTrue(cm.getRecall(true).getMean() == 0.50 );
		Assert.assertTrue(cm.getPrecision(false).getMean() == (2.0/3 + 1) /2);
		Assert.assertTrue(cm.getRecall(false).getMean() == 0.625);

		
	}
}
