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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.FixedSegmentClassifier;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.SegmentClassification;
import org.eng.aisp.classifier.gmm.GMMClassifier;
import org.junit.Assert;
import org.junit.Test;

public class FixedSegmentClassifierTest {
	
	@Test
	public void testSegments() throws AISPException, IOException {
		String trainingLabel = "status";
		String normalLabelValue = "normal";
		String abnormalLabelValue = "abnormal";
		int clipLenMsec = 1000;
		int normalCount = 5, abnormalCount = 5;
		List<SoundRecording> trainingData = SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, normalLabelValue, 
				clipLenMsec, normalCount, abnormalLabelValue, abnormalCount);
		SoundClip normalClip = SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, normalLabelValue, 
				clipLenMsec, 1, abnormalLabelValue, 0).get(0).getDataWindow();
		SoundClip abnormalClip = SoundTestUtils.createNormalAbnormalTrainingRecordings(trainingLabel, normalLabelValue, 
				clipLenMsec, 0, abnormalLabelValue, 1).get(0).getDataWindow();
		
		IClassifier<double[]> classifier = new GMMClassifier();
		classifier.train(trainingLabel, trainingData);

		FixedSegmentClassifier<double[]> sc = new FixedSegmentClassifier<double[]>(classifier, clipLenMsec);
		testSequence(sc, trainingLabel, normalClip, abnormalClip, new int[] { 5 }, new double[] {0, 5000}, new String[] { normalLabelValue} );
		testSequence(sc, trainingLabel, normalClip, abnormalClip, new int[] { 2, 3 }, new double[] {0, 2000, 5000}, new String[] { normalLabelValue, abnormalLabelValue} );
		testSequence(sc, trainingLabel, normalClip, abnormalClip, new int[] { 1, 1, 1, 1 }, new double[] {0, 1000, 2000, 3000, 4000}, new String[] { normalLabelValue, abnormalLabelValue, normalLabelValue, abnormalLabelValue} );

		// Test unwegmented classification
		sc = new FixedSegmentClassifier<double[]>(classifier, 0);
		testSequence(sc, trainingLabel, normalClip, abnormalClip, new int[] { 5 }, new double[] {0, 5000}, new String[] { normalLabelValue} );

	}
	/**
	 * Create a concatenation of sound clips and do a segmented classification and make sure we get the expected results, per the inputs. 
	 * @param classifier the segmenting classifier used
	 * @param trainingLabel the label on which the classifier is trained and which we should expect classification results for.
	 * @param clip1 the first clip used in the concattentation and for which the expected labels would match the first label given. 
	 * @param clip2 the 2nd clip used in the concatenation
	 * @param alternatingClipCounts an array specifying numbers of clip1, then clip2, then clip1, etc.
	 * @param expectedSegmentBoundaryMsec an array of milliseconds defining the expected start and stop times of the resulting grouping of consecutive segments with the same classification values. 
	 * @param expectedLabelValues an array of label values for in the classification results for the given training label from the the resulting segments.
	 * @throws IOException
	 * @throws AISPException
	 */
	private void testSequence(FixedSegmentClassifier<double[]> classifier, String trainingLabel, SoundClip clip1, SoundClip clip2, 
			int[] alternatingClipCounts, double[] expectedSegmentBoundaryMsec, String[] expectedLabelValues) throws IOException, AISPException {
		Assert.assertTrue(expectedSegmentBoundaryMsec.length-1 == expectedLabelValues.length);	// Otherwise, test is broken.
		// Build a list of clips according to the alternatingClipCounts, in which the first element is the number of
		// normal clips, the 2nd the number of abnormal, the 3rd normal, etc.
		List<SoundClip> clipList = new ArrayList<SoundClip>();
		boolean useNormal = true;
		for (int count : alternatingClipCounts) {
			SoundClip clip;
			if (useNormal) {
				clip = clip1;
			} else {
				clip = clip2;
			}
			for (int i=0 ; i<count ; i++)
				clipList.add(clip);
			useNormal = !useNormal;
		}

		// Build a single clip from the list of clips
		SoundClip clip = concatenate(clipList);
		
		// Do a segmented classification
		List<SegmentClassification> segs = classifier.classify(clip);
		
		// GO through the segments and validate each one against the expected times and classification results
		Assert.assertTrue(segs != null);
		Assert.assertTrue(segs.size() == expectedSegmentBoundaryMsec.length-1);
		double lastEnd = -1;
		for (int i=0 ; i<segs.size() ; i++) {
			SegmentClassification sc = segs.get(i);
			// Test start and end times of segment.
			double start = sc.getStartMsec(); 
			double   end = sc.getEndMsec();
			double expectedStart = expectedSegmentBoundaryMsec[i];
			double expectedEnd = expectedSegmentBoundaryMsec[i+1];
			Assert.assertTrue(Math.abs(expectedStart - start) < .0001);
			Assert.assertTrue(Math.abs(expectedEnd   - end  ) < .0001);
			if (lastEnd > 0) 
				Assert.assertTrue(start == lastEnd);
			lastEnd = end;
			// Test label value of segment.
			Map<String,Classification> cmap = sc.getClassification();
			Assert.assertTrue(cmap != null);
			Classification c = cmap.get(trainingLabel);
			Assert.assertTrue(c != null);
			String labelValue = c.getLabelValue();
			Assert.assertTrue(labelValue != null);
			String expectedLabelValue = expectedLabelValues[i];
			Assert.assertTrue(expectedLabelValue != null);	 // Otherwise, test misconfigured.
			Assert.assertTrue(expectedLabelValue.equals(labelValue));
		}
		
	}

	/**
	 * Concatenate the sounds, assuming they all have the same sampling rate, channels and bits per sample.
	 * @param clipList
	 * @return
	 * @throws IOException
	 */
	private SoundClip concatenate(List<SoundClip> clipList) throws IOException {
 		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		boolean first=true;
		for (SoundClip clip : clipList) {
			byte[] pcm = clip.getPCMData();
			if (!first) {
				// So we don't add time, make the first sample overlap with the last sample by removing the 1st sample. 
				pcm = Arrays.copyOf(pcm, pcm.length);	
			} else {
				first = false;
			}
			bos.write(pcm);
		}
		SoundClip firstClip = clipList.get(0);
		double samplingRate = firstClip.getSamplingRate();
		int bitsPerSample = firstClip.getBitsPerSample();
		int channels = firstClip.getChannels();
// 		public SoundClip(double startTimeMsec, int channels, int bitsPerSample, double samplesPerSecond, byte pcmData[]) {

		return new SoundClip(0, channels, bitsPerSample, samplingRate, bos.toByteArray()); 
	}


}
