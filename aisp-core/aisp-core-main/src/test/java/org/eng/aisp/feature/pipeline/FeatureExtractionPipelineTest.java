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
package org.eng.aisp.feature.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.eng.aisp.AISPException;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.feature.DoubleFeature;
import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.ILabeledFeatureGram;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.extractor.vector.IdentityFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.feature.processor.vector.IdentityFeatureProcessor;
import org.eng.cache.Cache;
import org.junit.Assert;
import org.junit.Test;



public class FeatureExtractionPipelineTest {
	
	/**
	 * Get the pipeline over which the tests should be run.
	 * This allows sub-classes that define implementation-specific tests to redefine this to use their implementation. 
	 * @param fgList
	 * @return
	 */
	protected FeatureExtractionPipeline<double[],double[]> getTestPipeline(List<IFeatureGramDescriptor<double[],double[]>> fgList) {
		return new FeatureExtractionPipeline<double[],double[]>(fgList);
	}

	/**
	 * Dummy feature extract that just counts the number of calls.
	 * @author dawood
	 *
	 */
	protected static class TestExtractor implements IFeatureExtractor<double[],double[]> {
	
		private static final long serialVersionUID = 9100811623781981605L;
		public AtomicInteger callCount = new AtomicInteger();
		final int id;
		public TestExtractor(int id) {
			this.id = id;
		}
	
		@Override
		public synchronized IFeature<double[]> apply(IDataWindow<double[]> t) {
			callCount.incrementAndGet();
			return new DoubleFeature(t.getStartTimeMsec(), t.getEndTimeMsec(), t.getData());
		}
	
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
			return result;
		}
	
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof TestExtractor))
				return false;
			TestExtractor other = (TestExtractor) obj;
			if (id != other.id)
				return false;
			return true;
		}
	
	}

	/**
	 * Dummy feature processor that just counts the number of calls.
	 * @author dawood
	 *
	 */
	protected static class TestProcessor implements IFeatureProcessor<double[]> {
	
		private static final long serialVersionUID = -8054156604775122466L;
		public AtomicInteger callCount = new AtomicInteger();
		final int id;
	
		public TestProcessor(int id) {
			this.id = id;
		}
	
		@Override
		public synchronized IFeatureGram<double[]> apply(IFeatureGram<double[]> features) {
			callCount.incrementAndGet();
			return features;
		}
	
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
			return result;
		}
	
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof TestProcessor))
				return false;
			TestProcessor other = (TestProcessor) obj;
			if (id != other.id)
				return false;
			return true;
		} 
	}

	@Test
	public void testMultipleFeatureGrams() {
		int count = 1, durationMsec = 1000;
		Properties labels = new Properties();
		labels.put("label", "value");
		List<SoundRecording> srList = SoundTestUtils.createTrainingRecordings(count, durationMsec, 1000, labels, true);
		SoundRecording soundRecording = srList.get(0);
		IFeatureExtractor<double[],double[]> fe = new IdentityFeatureExtractor();
		List<IFeatureProcessor<double[]>> processorList = new ArrayList<IFeatureProcessor<double[]>>();
		List<IFeatureGramDescriptor<double[],double[]>> fgList = new ArrayList<IFeatureGramDescriptor<double[],double[]>>();
		IFeatureGramDescriptor<double[],double[]> fgd = new FeatureGramDescriptor<double[],double[]>(100,100, fe, null);
		fgList.add(fgd);
		
		for (int i=0 ; i<3 ; i++) {
			FeatureExtractionPipeline<double[],double[]> fep = getTestPipeline(fgList);
			ILabeledFeatureGram<double[]>[] lfgArray = fep.extract(soundRecording);
			Assert.assertTrue(lfgArray != null);
			int expectedSize = fgList.size();
			Assert.assertTrue(lfgArray.length == expectedSize);
			// Make sure each returned feature gram has the same labels; 
			for (int j=0 ; j<expectedSize ; j++) {
				ILabeledFeatureGram<double[]> lfg = lfgArray[j];
				Properties lfgLabels = lfg.getLabels();
				Assert.assertTrue(lfgLabels != null);
				Assert.assertTrue(lfgLabels.size() != 0);
				Assert.assertTrue(labels.equals(lfgLabels));
			}
			// Make sure each returned feature gram is the same structure as the first 
			ILabeledFeatureGram<double[]> lfg0 = lfgArray[0];
			for (int j=1 ; j<expectedSize ; j++) {
				ILabeledFeatureGram<double[]> lfg = lfgArray[j];
				Assert.assertTrue(lfg0.equals(lfg));
			}
			
			// Add a processor for the next time through the loop
			fgd = new FeatureGramDescriptor<double[],double[]>(100,100, fe, new IdentityFeatureProcessor());
			fgList.add(fgd);
		}
	}

	protected FeatureExtractionPipeline<double[],double[]> getPipeline(int windowSizeMsec, int windowShiftMsec, IFeatureExtractor<double[],double[]> fe, List<IFeatureProcessor<double[]>> fpList) {
		List<IFeatureGramDescriptor<double[],double[]>> fgdList = new ArrayList<>();
		for (IFeatureProcessor<double[]> fp : fpList)  {
			FeatureGramDescriptor<double[],double[]> fgd = new FeatureGramDescriptor<>(windowSizeMsec, windowShiftMsec,fe, fp);
			fgdList.add(fgd);
		}
		FeatureExtractionPipeline<double[],double[]> pipeline = this.getTestPipeline(fgdList);
		return pipeline;
	}

	protected FeatureExtractionPipeline<double[],double[]> getPipeline(int windowSizeMsec, int windowShiftMsec, List<IFeatureExtractor<double[],double[]>> feList, IFeatureProcessor<double[]> fp) {
		List<IFeatureGramDescriptor<double[],double[]>> fgdList = new ArrayList<>();
		for (IFeatureExtractor<double[],double[]> fe : feList)  {
			FeatureGramDescriptor<double[],double[]> fgd = new FeatureGramDescriptor<>(windowSizeMsec, windowShiftMsec,fe, fp);
			fgdList.add(fgd);
		}
		FeatureExtractionPipeline<double[],double[]> pipeline = this.getTestPipeline(fgdList);
		return pipeline;
	}

	/**
	 * Test to make sure the feature extractors and processors are being called the expected number of times.
	 * Created in response to issue #298.
	 * @throws AISPException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testExtractorProcessorCalling() throws AISPException, IOException, InterruptedException  {
		String trainingLabel = "status";
		int count = 3;
		// These are set to avoid round off error, which for 1000 and 100 gives only 9 subwindows.
		int durationMsec = 10000, windowSizeMsec = 1000;
		int subWindowsPerClip = durationMsec / windowSizeMsec;
		int windowShiftMsec = 0;
		Properties labels = new Properties();
		labels.put(trainingLabel, "doesntmatter");
//		System.out.println("Before creating sounds");
		List<SoundRecording> srList = SoundTestUtils.createTrainingRecordings(count, 0, durationMsec, 0, 1000, labels, true);
//		System.out.println("After creating sounds");
	
		TestExtractor fe1 = new TestExtractor(1);
		TestExtractor fe2 = new TestExtractor(2);
		TestProcessor fp1 = new TestProcessor(3);
		TestProcessor fp2 = new TestProcessor(4);
		List<IFeatureProcessor<double[]>> fpList = new ArrayList<IFeatureProcessor<double[]>>();
		fpList.add(fp1);
		fpList.add(fp2);
		List<IFeatureExtractor<double[],double[]>> feList = new ArrayList<IFeatureExtractor<double[],double[]>>();
		feList.add(fe1);
		feList.add(fe2);
		int expectedProcessorCalls = count;	// one call for each full windows.
	

		// Create the pipeline with rolling windows and 2 processors.
		windowShiftMsec = 0;
		int expectedExtractorCalls = srList.size() * subWindowsPerClip * fpList.size();
		FeatureExtractionPipeline<double[],double[]> pipeline = getPipeline(windowSizeMsec, windowShiftMsec, fe1, fpList); 

		Cache.clearManagedCaches();	// Make sure the extractors and processors get called
		for (SoundRecording sr: srList) {
			ILabeledFeatureGram<double[]> fg[] = pipeline.extract(sr);
			Assert.assertTrue(fg.length == fpList.size());
		}
		Assert.assertTrue("count=" + fe1.callCount + "!= " + (expectedExtractorCalls), fe1.callCount.get() == expectedExtractorCalls);	// Feature extractor applied to all sub windows in training set
		Assert.assertTrue(fp1.callCount.get()== expectedProcessorCalls);				// Feature processor call once for each data window.
		Assert.assertTrue(fp2.callCount.get()== expectedProcessorCalls);				// Feature processor call once for each data window.

		// Test with rolling windows, 2 extractors and zero processors 
		Cache.clearManagedCaches();	// Make sure the extractors and processors get called.
		pipeline = getPipeline(windowSizeMsec, windowShiftMsec, feList, null); 
		expectedExtractorCalls = srList.size() * subWindowsPerClip;
		fe1.callCount.set(0); fe2.callCount.set(0); fp1.callCount.set(0); fp2.callCount.set(0);
		for (SoundRecording sr: srList) {
			ILabeledFeatureGram<double[]> fg[] = pipeline.extract(sr);
			Assert.assertTrue(fg.length == feList.size());
		}
		Assert.assertTrue("count=" + fe1.callCount + "!= " + (expectedExtractorCalls), fe1.callCount.get() == expectedExtractorCalls);	// Feature extractor applied to all sub windows in training set
		Assert.assertTrue("count=" + fe2.callCount + "!= " + (expectedExtractorCalls), fe2.callCount.get() == expectedExtractorCalls);	// Feature extractor applied to all sub windows in training set
		Assert.assertTrue(fp1.callCount.get() == 0);				// Feature processor not called 
		Assert.assertTrue(fp2.callCount.get() == 0);				// Feature processor not called 

		windowShiftMsec = windowSizeMsec / 2;
		subWindowsPerClip = durationMsec / windowSizeMsec * 2 - 1;
	
		Cache.clearManagedCaches();	// Make sure the extractors and processors get called.
//		System.out.println("Exiting");
	}

}
