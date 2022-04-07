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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Properties;

import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.IdentityFeatureExtractor;
import org.eng.aisp.feature.io.CSVFeatureReader;
import org.eng.aisp.feature.io.CSVFeatureWriter;
import org.eng.aisp.feature.io.ILabeledFeature;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.junit.Assert;
import org.junit.Test;

public class CSVFeatureReadWriteTest {

	@Test
	public void CSVFeatureReadWriteTest() throws IOException {
		IFeatureExtractor<double[],double[]> extractor = new IdentityFeatureExtractor();
		IFeatureProcessor<double[]> processor = null; 
		int samplingRate = 1000;
		int windowCount = 5, startMsec = 0, durationMsec = 1000, spacingMsec = 0, htz = 1000, channels = 1;
		int windowSizeMsec = 50, windowShiftMsec = 0;
		Properties labels = new Properties();
		labels.put("label1", "value1");
		labels.put("label2", "value2");

		// Create some tests sounds.
//		Iterable<SoundRecording> sounds = SoundTestSuite.createTrainingRecordings(windowCount, startMsec, durationMsec, spacingMsec, htz, labels);
		Iterable<SoundRecording> sounds = SoundTestUtils.createTrainingRecordings(windowCount, channels, samplingRate, 8, startMsec, durationMsec, spacingMsec, htz, labels);
		int dataLength = sounds.iterator().next().getDataWindow().getData().length;
		int subWindows = durationMsec / windowSizeMsec; 	// and assumes windowShiftMsec == 0 
//		System.out.println("dataLength=" + dataLength);
		
		// Write them out to a String.
		CSVFeatureWriter fwriter = new CSVFeatureWriter(extractor, processor, windowSizeMsec, windowShiftMsec);
		StringWriter stringWriter = new StringWriter();
		fwriter.write(stringWriter, sounds);
		stringWriter.close();

//		System.out.println(stringWriter.toString());

		// Read them back from the String.
		StringReader stringReader = new StringReader(stringWriter.toString());
		Iterator<ILabeledFeature<double[]>[]> features = CSVFeatureReader.read(stringReader, windowSizeMsec);
		
		int expectedFeaturesPerWindow = durationMsec / windowSizeMsec;
		// -1 to not could the last point which is at the end of a window
		// +1 to 
		int expectedFeatureLength =  (dataLength - 1) / expectedFeaturesPerWindow + 1;
		int featureArrays = 0;
		while (features.hasNext()) {
			ILabeledFeature<double[]>[] fa = features.next();
			Assert.assertTrue(fa != null);
			Assert.assertTrue(fa.length == expectedFeaturesPerWindow);
			int subFeatureCount = 0;
			for (int i=0 ; i<fa.length ; i++) {
				ILabeledFeature<double[]> lf = fa[i];
				Assert.assertTrue(lf != null);
				Assert.assertTrue(lf.getFeature().getData().length == expectedFeatureLength);	// We used an identity feature extractor so lengths should be the same.
				Assert.assertTrue(lf.getLabels().equals(labels));
				int featureLength = lf.getFeature().getData().length;
				subFeatureCount++;
				Assert.assertTrue(featureLength == expectedFeatureLength);
			}
			Assert.assertTrue(subFeatureCount == subWindows); 
			featureArrays++; 
		}
		Assert.assertTrue(featureArrays == windowCount);
		
	}

}
