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
package org.eng.aisp.feature.extractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eng.aisp.AISPLogger;
import org.eng.aisp.DoubleWindow;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.feature.DoubleFeature;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.processor.AbstractCachingWindowProcessor;

/**
 * Allows a common feature extractor to be applied to multi-dimensional data.
 * The dimension of the data expected is configurable but is expected to be perfectly interleaved.
 * So for 3-d XYZ data the data is expected as 
 * <pre>
 * xyzxyz...xyz
 * </pre>
 * Feature extract proceeds as follows:
 * <ol>
 * <li> De-interleave the data
 * <li> apply the feature extractor separately to each dimension of data
 * <li> concatenate the resulting features into a single array.
 * </ol>
 * @author DavidWood
 *
 */
public class MultiDimensionFeatureExtractor extends AbstractCachingWindowProcessor<IDataWindow<double[]>, IFeature<double[]>> implements IFeatureExtractor<double[],double[]>{

	private static final long serialVersionUID = -5218454430329327053L;
	private final int nDimensions;
	private final IFeatureExtractor<double[], double[]> extractor;
	private final boolean usedDimensions[]; 

	/**
	 * A convenience on {@link #MultiDimensionFeatureExtractor(IFeatureExtractor, boolean[])} that extracts features from all the given dimensions. 
	 * @param extractor feature extract to be applied to all data dimensions.
	 * @param nDims expected dimension of interleaved data provided to {@link #applyImpl(IDataWindow)}.
	 */
	public MultiDimensionFeatureExtractor(IFeatureExtractor<double[],double[]> extractor, int nDims) {
		this(extractor, createTrueArray(nDims));
	}

	private static boolean[] createTrueArray(int nDims) {
		if (nDims < 1)
			throw new IllegalArgumentException("Number of dimensions must be 1 or larger.");
		boolean[] b = new boolean[nDims];
		Arrays.fill(b, true);
		return b;
	}

	/**
	 * 
	 * @param extractor
	 * @param usedDimensions defines 1) the dimension of the input data by its length and 2) selects which dimensions of the data the feature is extracted from.
	 * Must contain at least one true element and be of at least length 1.
	 */
	public MultiDimensionFeatureExtractor(IFeatureExtractor<double[],double[]> extractor, boolean[] usedDimensions) {
		if (extractor == null)
			throw new IllegalArgumentException("extractor must not be null");
		if (usedDimensions == null)
			throw new IllegalArgumentException("usedDimensions must not be null");
		if (usedDimensions.length == 0)
			throw new IllegalArgumentException("usedDimensions must be of length 1 or more");
		this.nDimensions =  usedDimensions.length;
		this.extractor = extractor;
		this.usedDimensions = usedDimensions;
	}

	@Override
	protected IFeature<double[]> applyImpl(IDataWindow<double[]> window) {
		double startMsec = window.getStartTimeMsec();
		double endMsec = window.getEndTimeMsec();
		double[] interleavedData = window.getData();
		int extras = interleavedData.length % nDimensions;
		if (extras != 0)  {
			AISPLogger.logger.warning("Window's data length (" + interleavedData.length + ") is not on integer multiple of the number of expected dimensions (" + nDimensions + "). Truncating.");
			interleavedData = Arrays.copyOfRange(interleavedData, 0, interleavedData.length - extras);
		}
		int size=0;
		List<double[]> features = new ArrayList<double[]>();
		for (int i=0 ; i<nDimensions; i++) {
			if (usedDimensions[i]) {
				double[] data = getDataSlice(interleavedData, i);
//				double[] data = VectorUtils.getInterleavedData(interleavedData, nDimensions, i);
				IDataWindow<double[]> dw = new DoubleWindow(startMsec, endMsec, data);
				IFeature<double[]> feature = extractor.apply(dw);
				if (feature == null)
					throw new RuntimeException("Feature extractor " + extractor.getClass().getName() + " return null.");
				double[] fdata = feature.getData();
				size += fdata.length;
				features.add(fdata);
			}
		}
		
		double[] newFeatureData = new double[size];
		int offset = 0;
		for (double[] fdata : features) {
			System.arraycopy(fdata, 0, newFeatureData, offset, fdata.length);
			offset += fdata.length;
		}
		IFeature<double[]> newFeature = new DoubleFeature(startMsec, endMsec, newFeatureData);
		return newFeature; 
		
	}

	private double[] getDataSlice(double[] interleavedData, int index) {
		int len = interleavedData.length / nDimensions;
		double[] slice = new double[len];
		for (int i=index,j=0 ; i<interleavedData.length && j<len ; i+=nDimensions,j++) {
			slice[j] = interleavedData[i];
		}
		return slice;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((extractor == null) ? 0 : extractor.hashCode());
		result = prime * result + nDimensions;
		result = prime * result + Arrays.hashCode(usedDimensions);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof MultiDimensionFeatureExtractor))
			return false;
		MultiDimensionFeatureExtractor other = (MultiDimensionFeatureExtractor) obj;
		if (extractor == null) {
			if (other.extractor != null)
				return false;
		} else if (!extractor.equals(other.extractor))
			return false;
		if (nDimensions != other.nDimensions)
			return false;
		if (!Arrays.equals(usedDimensions, other.usedDimensions))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final int maxLen = 5;
		return "MultiDimensionFeatureExtractor [nDimensions=" + nDimensions + ", extractor=" + extractor
				+ ", usedDimensions="
				+ (usedDimensions != null
						? Arrays.toString(Arrays.copyOf(usedDimensions, Math.min(usedDimensions.length, maxLen)))
						: null)
				+ "]";
	}

//	public static void main(String[] args) throws AISPException {
//		IFeatureExtractor<double[],double[]> mfcc = new MFCCFeatureExtractor();
//		IFeatureExtractor<double[],double[]>  accFE = new MultiDimensionalFeatureExtractor(3, mfcc);
//		IFeatureGramDescriptor<double[],double[]>  fge = new FeatureGramDescriptor<double[],double[]> (1000, 500, accFE, null);
//		IClassifier<double[]> classifier = new GMMClassifier(fge);
//		
//		int durationMsec = 3000;
//		int count = 10;
//		
//		String trainingLabel = "direction";
//		Properties xLabels = new Properties();
//		xLabels.put(trainingLabel, "x");
//		List<SoundRecording> srList = SoundTestUtils.createTrainingRecordings(count, durationMsec, 4000, xLabels, true);
//		List<SoundRecording> trainingSounds = interleave(srList,0,3);
//
//		Properties yLabels = new Properties();
//		yLabels.put(trainingLabel, "y");
//		srList = SoundTestUtils.createTrainingRecordings(count, durationMsec, 4000, yLabels, true);
//		trainingSounds.addAll(interleave(srList,1,3));
//
//		Properties zLabels = new Properties();
//		zLabels.put(trainingLabel, "z");
//		srList = SoundTestUtils.createTrainingRecordings(count, durationMsec, 4000, zLabels, true);
//		trainingSounds.addAll(interleave(srList,2,3));
//
//		TrainingSetInfo tsi = TrainingSetInfo.getInfo(trainingSounds);
//		System.out.println(tsi.prettyFormat());
//		
//		classifier.train(trainingLabel, trainingSounds);
//		
//		SoundTestUtils.verifyClassifications(classifier, trainingSounds, trainingLabel);
//
//	}

//	private static List<SoundRecording> interleave(List<SoundRecording> srList, int dim, int nDims) {
//		List<SoundRecording> interleavedSR = new ArrayList<SoundRecording>();
//		for (SoundRecording sr : srList) {
//			SoundClip clip = sr.getDataWindow();
//			double[] data = clip.getData();
//			double newData[] = new double[data.length * nDims];
//			Arrays.fill(newData,0);
//			for (int i=0, j=dim ; j<newData.length ; i++, j += nDims)
//				newData[j] = data[i];
//			SoundClip newClip = new SoundClip(clip.getStartTimeMsec(), clip.getEndTimeMsec(), newData);
//			SoundRecording newSR = new SoundRecording(sr, newClip);
//			interleavedSR.add(newSR);
//		}
//		return interleavedSR;
//	}

}
