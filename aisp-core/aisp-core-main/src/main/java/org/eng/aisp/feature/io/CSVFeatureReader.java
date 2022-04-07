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
package org.eng.aisp.feature.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.eng.aisp.DoubleWindow;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.aisp.LabeledDataWindow;
import org.eng.aisp.feature.io.CSVFeatureWriter.FeatureLine;

/**
 * Provides a way to read features from a CSV file into an Iterable over those features or a reconstructed data windows.
 * @author dawood
 *
 */
public class CSVFeatureReader {


	private static class CSVFeatureIterator implements Iterator<ILabeledFeature<double[]>[]> {
		BufferedReader reader = null;
//		ILabeledFeature<double[]>[] nextFeatureSet = null;
		List<ILabeledFeature<double[]>> nextFeatureSet = new ArrayList<ILabeledFeature<double[]>>();
		/** Feature in next feature set that has not yet been returned */
		FeatureLine featureReadLastTime = null;
		private final int windowSizeMsec;	
		private double nextStartMsec = 0;	

		public CSVFeatureIterator(String featureFile, int windowSizeMsec) {
			try {
				reader = new BufferedReader(new FileReader(featureFile));
			} catch (FileNotFoundException e) {
				throw new RuntimeException("File " + featureFile + " not found.");
			}
			this.windowSizeMsec = windowSizeMsec;
		}

		public CSVFeatureIterator(StringReader reader, int windowSizeMsec) {
			this.reader = new BufferedReader(reader);
			this.windowSizeMsec = windowSizeMsec;
		}
		
		@Override
		public boolean hasNext() {
			if (reader == null)
				return false;

			if (nextFeatureSet.isEmpty()) {
				try {
					int currentWindowIndex; 
					if (featureReadLastTime != null) {
						currentWindowIndex = featureReadLastTime.getWindowIndex();
						nextFeatureSet.add(featureReadLastTime.getLabeledFeature());
						featureReadLastTime = null;
					} else {
						currentWindowIndex = -1;	// Use first index in features file.
					}
					FeatureLine fl = null; 
					boolean done = false;
					while (!done) {
						String line = reader.readLine();
						if (line == null) {
							try { reader.close(); } catch (IOException e2) { }
							reader = null;
							featureReadLastTime = null;
							fl = null;
						} else {
							double endTimeMsec = nextStartMsec + windowSizeMsec;
							fl = CSVFeatureWriter.parseFeatureLine(line, nextStartMsec, endTimeMsec);
							nextStartMsec = endTimeMsec; 
							featureReadLastTime = fl;
						}
						if (fl != null && currentWindowIndex < 0)
							currentWindowIndex = fl.getWindowIndex();
					    done = fl == null || fl.getWindowIndex() != currentWindowIndex;
					    if (!done)
					    	nextFeatureSet.add(fl.getLabeledFeature());
					    else
							featureReadLastTime = fl;
					} 
				} catch (IOException e) {
					e.printStackTrace();
					try { reader.close(); } catch (IOException e2) { }
					reader = null;
					featureReadLastTime = null;
				}
			}
			
			return !nextFeatureSet.isEmpty();
		}

		@Override
		public ILabeledFeature<double[]>[] next() {
			if (nextFeatureSet.isEmpty() && !hasNext()) 
				throw new NoSuchElementException();
			ILabeledFeature<double[]>[] r = new ILabeledFeature[nextFeatureSet.size()];
			nextFeatureSet.toArray(r);;
			nextFeatureSet.clear();
			return r;
		}
		
		protected void finalize() throws Throwable {
			if (reader != null) {
				reader.close();
				reader = null;
			}
			super.finalize();
		}
		
	}

	private static class CSVFeatureIterable implements Iterable<ILabeledFeature<double[]>[]> {
		String featureFile;
		private int windowSizeMsec;
		List<ILabeledFeature<double[]>[]> featureArrayList = null;
		
		public CSVFeatureIterable(String featureFile, int windowSizeMsec) {
			if (windowSizeMsec <= 0)
				throw new IllegalArgumentException("window size must be larger than 0");
			this.featureFile = featureFile;
			this.windowSizeMsec = windowSizeMsec;
		}

		/**
		 * Multiple times iteration through the reader.
		 * This requires reading the contents of the reader into memory.
		 * @param reader
		 * @param windowSizeMsec
		 * @throws IOException 
		 */
		public CSVFeatureIterable(Reader reader, int windowSizeMsec) throws IOException {
			this.windowSizeMsec = windowSizeMsec;
			StringBuilder readerContent = loadReader(reader);
			CSVFeatureIterator fiter = new CSVFeatureIterator(new StringReader(readerContent.toString()),windowSizeMsec);
			this.featureArrayList = new ArrayList<ILabeledFeature<double[]>[]>();
			while (fiter.hasNext()) {
				ILabeledFeature<double[]> f[] = fiter.next(); 
				this.featureArrayList.add(f);
			}
		}

		@Override
		public Iterator<ILabeledFeature<double[]>[]> iterator() {
			if (featureFile != null)
				return new CSVFeatureIterator(featureFile,windowSizeMsec);
			else {
				return this.featureArrayList.iterator(); 
			}
		}

		private StringBuilder loadReader(Reader reader) throws IOException {
			StringBuilder sb = new StringBuilder();
			BufferedReader br = new BufferedReader(reader);
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append('\n');
			}
			return sb;
			
		}
		
	}
	

	
	private static class CSVFeatureToDataIterator implements Iterator<ILabeledDataWindow<double[]>> {

//		private int subWindowSizeMsec;
		private Iterator<ILabeledFeature<double[]>[]> featureIterator;
//		private double nextStartMsec = 0;
		
		public CSVFeatureToDataIterator(Iterator<ILabeledFeature<double[]>[]> iterator) {
			this.featureIterator = iterator;
//			this.subWindowSizeMsec = subWindowSizeMsec;
		}

		@Override
		public boolean hasNext() {
			return featureIterator.hasNext();
		}

		@Override
		public ILabeledDataWindow<double[]> next() {
			ILabeledFeature<double[]>[] features = featureIterator.next();
			if (features == null || features.length == 0)
				throw new NoSuchElementException();
			double[] data = featureMatrixToDataVector(features);
			double startMsec = features[0].getFeature().getStartTimeMsec();
			double endMsec =   features[features.length-1].getFeature().getEndTimeMsec();
			IDataWindow<double[]> dw = new DoubleWindow(startMsec, endMsec, data);
			ILabeledDataWindow<double[]> ldw = new LabeledDataWindow<double[]>(dw, features[0].getLabels());
			return ldw;
		}

		/**
		 * @param features
		 * @return
		 */
		private double[] featureMatrixToDataVector(ILabeledFeature<double[]>[] features) {
			final int featureLen = features[0].getFeature().getData().length;
			final int dataLength = featureLen * features.length;
			double[] data = new double[dataLength]; 
			int destPos = 0;
			for (int i=0 ; i<features.length; i++) {
				double fdata[] = features[i].getFeature().getData();
				if (fdata.length != featureLen) 
					throw new RuntimeException("Inconsistent feature length within a feature set");
				System.arraycopy(fdata, 0, data, destPos, featureLen);
				destPos += featureLen;
			}
			return data;
		}

	}

	private static class CSVFeatureToDataIterable implements Iterable<ILabeledDataWindow<double[]>> {

		private CSVFeatureIterable csvFeatureIterable;

		public CSVFeatureToDataIterable(CSVFeatureIterable csvFeatureIterable) {
			this.csvFeatureIterable = csvFeatureIterable;
		}

		@Override
		public Iterator<ILabeledDataWindow<double[]>> iterator() {
			return new CSVFeatureToDataIterator(csvFeatureIterable.iterator());
		}

	}

	/**
	 * Read the features from the given file and build them into labeled data windows.
	 * Subwindows of features within a window are grouped into a single labeled data window.
	 * The data contained within each labeled data window is the concatenation of all data from 
	 * each subwindow feature within a given window of features.
	 * The first data window starts at time 0 and the length of each subwindow will be set to 
	 * the given size.  
	 * The duration of any given labeled data window will be N * {@link #windowSizeMsec} where N is the 
	 * number of subwindows in any given feature window.  
	 * The subwindows are effectively set to be consecutive sliding
	 * windows so that the end time of subwindow N is effectively the start time of subwindow N+1.
	 * Similarly, the end time of labeled data window N will be the start time of labeled data window N+1.
	 * @param featureFile file of features with format defined by {@link CSVFeatureWriter}
	 * @param windowSizeMsec the duration of time assigned to each subwindow of features. This can be set
	 * to anything larger than 0.
	 * @return an iterable over labeled data window in which each labeled data window corresponds to the features
	 * from one window of features.
	 * @throws IOException
	 */
	public static Iterable<ILabeledDataWindow<double[]>> readAsLabeledData(String featureFile, int windowSizeMsec) throws IOException {
		File f= new File(featureFile);
		if (!f.exists())
			throw new IOException("File " + f + " does not exist.");
		return new CSVFeatureToDataIterable(new CSVFeatureIterable(featureFile,windowSizeMsec));
	}
	
	/**
	 * Same function as {@link #readAsLabeledData(String, int)}.
	 * @return
	 * @throws IOException
	 */
	public static Iterable<ILabeledDataWindow<double[]>> readAsLabeledData(Reader reader, int windowSizeMsec) throws IOException {
		return new CSVFeatureToDataIterable(new CSVFeatureIterable(reader,windowSizeMsec));
	}
	
	/**
	 * Read the arrays of subwindow features from the given file.
	 * Subwindows of features within a window are grouped into a single array of labeled features.
	 * The first feature in the first array of features returned starts at time 0.
	 * The length of each subwindow will be set to the given size.  
	 * The subwindows are effectively set to be consecutive sliding
	 * windows so that the end time of subwindow N is effectively the start time of subwindow N+1.
	 * @param featureFile file of features with format defined by {@link CSVFeatureWriter}
	 * @param windowSizeMsec the duration of time assigned to each subwindow of features. This can be set
	 * to anything larger than 0.
	 * @return an iterable over array sof labeled features in which each array corresponds to the features
	 * from one window of features.
	 * @throws IOException
	 */
	public static Iterable<ILabeledFeature<double[]>[]> read(String featureFile, int windowSizeMsec) throws IOException {
		File f= new File(featureFile);
		if (!f.exists())
			throw new IOException("File " + f + " does not exist.");
		return new CSVFeatureIterable(featureFile, windowSizeMsec);
	}

	
	/**
	 * Return an iterator that will read features from the given reader.
	 * @param reader
	 * @param windowSizeMsec
	 * @return
	 * @throws IOException
	 */
	public static Iterator<ILabeledFeature<double[]>[]> read(Reader reader, int windowSizeMsec) throws IOException {
		return new CSVFeatureIterable(reader, windowSizeMsec).iterator();
	}

}
