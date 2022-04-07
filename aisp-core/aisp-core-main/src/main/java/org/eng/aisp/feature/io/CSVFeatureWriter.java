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
/**
 * 
 */
package org.eng.aisp.feature.io;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import org.eng.aisp.feature.DoubleFeature;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;

/**
 * Extend the super class to write each labeled feature as a set of comma-separated values.
 * See {@link #write(Writer, int, int, ILabeledFeature)} for details on the format.
 * @author dawood
 *
 */
public class CSVFeatureWriter extends AbstractFeatureWriter {

	/**
	 * Create the instance to use the given extractor, processor and window settings when writing out the features.
	 * @param extractor
	 * @param processor
	 * @param windowSizeMsec
	 * @param windowShiftMsec
	 */
	public CSVFeatureWriter(IFeatureExtractor<double[], double[]> extractor, IFeatureProcessor<double[]> processor,
			int windowSizeMsec, int windowShiftMsec) {
		super(extractor, processor, windowSizeMsec, windowShiftMsec);
	}

	public static class CSVFeature {
		int windowIndex;
		int subWindowIndex;
		ILabeledFeature<double[]> feature;
	}

	/**
	 * Holds a the contents of a parsed feature.
	 * @author dawood
	 *
	 */
	public static class FeatureLine {
		private final int windowIndex;
		private final int subWindowIndex;
		private final ILabeledFeature<double[]> labeledFeature;

		public FeatureLine(int windowIndex, int subWindowIndex, ILabeledFeature<double[]> labeledFeature) {
			super();
			this.windowIndex = windowIndex;
			this.subWindowIndex = subWindowIndex;
			this.labeledFeature = labeledFeature;
		}

		/**
		 * @return the windowIndex
		 */
		public int getWindowIndex() {
			return windowIndex;
		}
		/**
		 * @return the subWindowIndex
		 */
		public int getSubWindowIndex() {
			return subWindowIndex;
		}
		/**
		 * @return the labeledFeature
		 */
		public ILabeledFeature<double[]> getLabeledFeature() {
			return labeledFeature;
		}
	}

	private final static int WINDOW_INDEX_COLUMN = 0;
	private final static int SUBWINDOW_INDEX_COLUMN = 1;
	private final static int DIMENSIONALITY_COLUMN = 2;
	private final static int LABELS_COLUMN = 3;
	private final static int FEATURE_START_COLUMN = 4;

	/**
	 * Parse a line of the CSV feature file.
	 * @param line line to be parsed and written in the same format as defined write(). 
	 * @param startTimeMsec start time to set in the resulting feature
	 * @param endTimeMsec end time to set in the resulting feature.
	 * @return an object that provides the labeled feature and indexing information.
	 * @throws IOException on a formatting error.
	 */
	static FeatureLine parseFeatureLine(String line, double startTimeMsec, double endTimeMsec) throws IOException {
		String columns[] = line.split(COMMA);
		if (columns == null || columns.length <= FEATURE_START_COLUMN) 
			throw new IOException("Not enough columns of data in line:" + line);
		if (startTimeMsec > endTimeMsec)
			throw new IllegalArgumentException("start time must be before end time");
		int windowIndex;
		try {
			windowIndex = Integer.parseInt(columns[WINDOW_INDEX_COLUMN]);
		} catch (NumberFormatException e) {
			throw new IOException("Could not parse window index from " + columns[WINDOW_INDEX_COLUMN]);
		}
		int subWindowIndex;
		try {
			subWindowIndex = Integer.parseInt(columns[SUBWINDOW_INDEX_COLUMN]);
		} catch (NumberFormatException e) {
			throw new IOException("Could not parse subwindow index from " + columns[SUBWINDOW_INDEX_COLUMN]);
		}
		int dimension = 0;	// Ignored
//		try {
//			dimension = Integer.parseInt(columns[DIMENSIONALITY_COLUMN]);
//		} catch (NumberFormatException e) {
//			throw new IOException("Could not parse dimensionality from " + columns[DIMENSIONALITY_COLUMN]);
//		}
		String labels = columns[LABELS_COLUMN];
		Properties labelProps = parseLabels(labels);
		double[] featureData = parseDoubles(columns,4);

		IFeature<double[]> f = new DoubleFeature(startTimeMsec, endTimeMsec, featureData);
		ILabeledFeature<double[]> lf = new LabeledFeature<double[]>(f, labelProps);
		return new FeatureLine(windowIndex, subWindowIndex, lf);
	}
	
	
	/**
	 * Starting at the given index in the given array of column values, parse out an array of double values.
	 * @param columns
	 * @param firstIndex
	 * @return
	 * @throws IOException 
	 */
	private static double[] parseDoubles(String[] columns, int firstIndex) throws IOException {
		double[] values = new double[columns.length - firstIndex];
		for (int i=0 ; firstIndex < columns.length ; firstIndex++, i++) {
			try {
				values[i] = Double.parseDouble(columns[firstIndex]);
			} catch (NumberFormatException e) {
				throw new IOException("Could not parse double from column " + firstIndex); 
			}
		}
		return values;
	}

	/**
	 * Parse label name/value pairs out of the given string.
	 * @param labels
	 * @return
	 */
	private static Properties parseLabels(String labels) throws IOException {
		String nameValues[] = labels.split(LABEL_SEPARATOR);
		Properties props = new Properties();
		if (nameValues == null || nameValues.length == 0)
			return props;

		for (String nameValue : nameValues) {
			if (nameValue.contains(NAME_VALUE_SEPARATOR)) {
				String pair[] = nameValue.split(NAME_VALUE_SEPARATOR);
				if (pair != null)
					props.put(pair[0], pair[1]);
			}
		}
		return props;
	}
	
	private final static String COMMA = ",";
	private final static String LABEL_SEPARATOR = ";";
	private final static String NAME_VALUE_SEPARATOR = "=";

	/**
	 * Write the feature in a CSV format.
	 * Columns are defined as follows:
	 * <ol>
	 * <li> 0-based window index,
	 * <li> 0-based subwindow index,
	 * <li> text specification of the layout of the feature data in the form P[xQ[xR...].  When more than 1 dimension is present,
	 * data is flattend in row-major order (C-style). So for a 2x3 tensor [[0, 1, 2], [3, 4, 5]] becomes [0, 1, 2, 3, 4, 5].
	 * <li> List of semi-colon separated labels in name=value format,
	 * <li> and following columns contain the list of features values for a given subwindow of data.
	 * </ol>
	 * No headers are written.
	 * For example,
	 * <pre>
	 * 0,0,2x128,gender=male;type=snore_nose;event=cough;smoker=false,-0.6391045345918763,-0.6232059399708378,...
	 * </pre>
	 * The first two integers are the window index and the subwindow index.
	 */
	@Override
	public void write(Writer writer, int windowIndex, int subwindowIndex, IFeature<double[]> feature, Properties labels) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(windowIndex);
		sb.append(COMMA);
		sb.append(subwindowIndex);
		sb.append(COMMA);
		sb.append(feature.getData().length);	// Always a 1-dimension vector of data.
		sb.append(COMMA);

		
		// Output the labels
		int index = 0;
		for (Object key : labels.keySet()) {
			if (index != 0)
				sb.append(LABEL_SEPARATOR);
			String name = key.toString();
			String value = labels.getProperty(name);
			sb.append(name);
			sb.append(NAME_VALUE_SEPARATOR);
			sb.append(value);
			index++;
		}

		// Output the double[] of features.
		if (index != 0) 	// No labels?
			sb.append(COMMA);
		double[] data = feature.getData();
		index = 0;
		for (double d : data) {
			if (index != 0)
				sb.append(COMMA);
			sb.append(d);
			index++;
		}
		sb.append("\n");
	
		writer.write(sb.toString());

	}

}
