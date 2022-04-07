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
package org.eng.aisp.util;

import java.util.Arrays;

import org.eng.aisp.AISPException;
import org.eng.util.OnlineStats;

/**
 * Utilities operating on matrices of double values in which the first index (i.e. double[i]) iterates over columns in the matrix.
 * @author dawood
 *
 */
public class MatrixUtil {

	/**
	 * Compute a new matrix that is the difference of the columns.
	 * Column i in the new matrix is the difference between column i+1 and column i in the given matrix.
	 * THe returned matrix has one less column than the input. 
	 * @param matrix a 2D matrix in which the columns are indexed by the 1st index and the rows by the 2nd index.
	 * @return never null. A matrix that has one less column than the input.
	 */
	public static double[][] columnDifference(double[][] matrix) {
		double[][] newFeatureData = new double[matrix.length-1][];
		for (int i=0 ; i<newFeatureData.length ; i++) {
			double[] t0Data = matrix[i];
			double[] t1Data = matrix[i+1];
			double[] diff = new double[t0Data.length];
			for (int j=0 ; j<diff.length ; j++) {
				diff[j] = t1Data[j] - t0Data[j];
			}
			newFeatureData[i] = diff; 
		}
		return newFeatureData;
	}
	
	/**
	 * Compute a new matrix that is the delta of the columns. The delta is defined as the standard way for speech processing
	 * as in <a href="http://practicalcryptography.com/miscellaneous/machine-learning/guide-mel-frequency-cepstral-coefficients-mfccs/#deltas-and-delta-deltas">
	 * http://practicalcryptography.com/miscellaneous/machine-learning/guide-mel-frequency-cepstral-coefficients-mfccs/#deltas-and-delta-deltas</a>.
	 * The returned matrix has the same number of columns as the input. 
	 * @param matrix a 2D matrix in which the columns are indexed by the 1st index and the rows by the 2nd index.
	 * @pamam windowHalfWidth The half size of the window across which the delta is taken, must be >= 1
	 * @return never null. A new matrix with the same dimensions as the input. 
	 * @throws AISPException 
	 */
	public static double[][] columnDelta(final double[][] matrix, int windowHalfWidth) {
		if (windowHalfWidth < 1) throw new IllegalArgumentException("Window size for delta computation must be >= 1.");
		
		int nColumns = matrix.length;
		if (nColumns < 1) throw new IllegalArgumentException("Matrix must have at least 1 column");
		int nRows = matrix[0].length;
		if (nRows < 1) throw new IllegalArgumentException("Matrix must have at least 1 row");

		double[][] newFeatureData = new double[nColumns][];
		for (int i=0 ; i<nColumns; i++) {	// Columns
			double sumDenominator = 0.0;
			double[] sumNumerator = new double[nRows];
			Arrays.fill(sumNumerator, 0.0);
			
			for (int j=1; j<=windowHalfWidth; j++) {
				int indexPos = i+j; // Math.min(i+j, nColumns-1); 
				int indexNeg = i-j; // Math.max(i-j, 0);
				if (indexPos < nColumns && indexNeg >= 0)	{ // a full window
					sumNumerator = VectorUtils.sum(sumNumerator, 
						VectorUtils.multiplyByScalar(VectorUtils.difference(matrix[indexPos], matrix[indexNeg]), j),
						true);	// modify sumNumerator 
					sumDenominator += Math.pow(j, 2);
				}
			}
//			sumDenominator = 1.0; //for testing
			if (sumDenominator == 0)
				newFeatureData[i] = sumNumerator;	// Already contains all 0s
			else 
				newFeatureData[i] = VectorUtils.divideByScalar(sumNumerator, sumDenominator * 2); 
		}
		return newFeatureData;
	}
	
	/**
	 * Get a single column of values that is average across corresponding rows of the matrix. 
	 * @param matrix a 2D matrix in which the columns are indexed by the 1st index and the rows by the 2nd index.
	 * @return never null. A vector of row averages. 
	 */
	public static double[] averageRows(double[][] matrix) {
		final int nColumns = matrix.length;
		final int nRows = matrix[0].length;
		double[] newData = new double[nRows];
		for (int rowIndex=0 ; rowIndex<nRows; rowIndex++) {	// for each row
			double sum = 0;
			for (int colIndex=0 ; colIndex<nColumns ; colIndex++) {	// for each column
				sum += matrix[colIndex][rowIndex];
			}
			newData[rowIndex] = sum / nColumns; 
		}
		return newData;
	}
	/**
	 * Get a single row of values that is average across corresponding columns of the matrix. 
	 * @param matrix a 2D matrix in which the columns are indexed by the 1st index and the rows by the 2nd index.
	 * @return never null. A vector of column averages. 
	 */
	public static double[] averageColumns(double[][] matrix) {
		final int nColumns = matrix.length;
		final int nRows = matrix[0].length;
		double[] newData = new double[nColumns];
		for (int colIndex=0 ; colIndex<nColumns ; colIndex++) {	// for each column
			double sum = 0;
			double[] column = matrix[colIndex];
			for (int rowIndex=0 ; rowIndex<nRows; rowIndex++) {	// for each row
				sum += column[rowIndex];
			}
			newData[colIndex] = sum / nRows; 
		}
		return newData;
	}

	/**
	 * Get a vector of values, one for each row,  equal to the maximum value in the corresponding row.
	 * @param matrix
	 * @return
	 */
	public static double[] rowMax(double[][] matrix) {
		int columnLen = matrix[0].length;
		double[] newData = new double[columnLen];
		for (int rowIndex=0 ; rowIndex<newData.length ; rowIndex++) {	
			double max = -Double.MAX_VALUE;
			for (int colIndex=0 ; colIndex<matrix.length ; colIndex++) {
				double value = matrix[colIndex][rowIndex];
				if (value > max)
					max = value;
			}
			newData[rowIndex] = max; 
		}
		return newData;
	}

	/**
	 * Find the min and max values in the matrix.
	 * @param matrix
	 * @return an array of length two containing the minimum and maximum, respectively.
	 */
	public static double[] getMinMax(double[][] matrix) {
		double max = -Double.MAX_VALUE;
		double min = Double.MAX_VALUE;
		for (int firstIndex=0 ; firstIndex<matrix.length ; firstIndex++) {	// Over each column of values 
			double data[] = matrix[firstIndex];
			if (data == null)
				continue;
			for (int secondIndex=0 ; secondIndex<matrix[0].length ; secondIndex++) {
				double value = data[secondIndex];
				if (value > max)
					max = value;
				if (value < min)
					min = value;
			}
		}
		return new double[] { min, max} ;
	}
	/**
	 * Multiply the given matrix by a scalar value.
	 * @param featureData
	 * @param multiplier
	 * @param inplace if true, then modify the input, otherwise create a new array to hold the new values.
	 * @return
	 */
	public static double[][] scalarMultiply(double[][] matrix, double multiplier, boolean inPlace) {
		return linearTransform(matrix, 0, multiplier, inPlace);
	}
	
	/**
	 * Apply a linear transformation to the value in the matrix.
	 * The transformation is defined as 
	 * <pre>
	 *   y = slope * x + offset;
	 * </pre> 
	 * @param matrix
	 * @param offset
	 * @param slope
	 * @param inPlace
	 * @return a transformed matrix, newly allocated if inPlace is true.
	 */
	public static double[][] linearTransform(double[][] matrix, double offset, double slope, boolean inPlace) {
		int columnLen = matrix[0].length;
		double[][] newData = inPlace ? matrix : new double[matrix.length][];
		for (int i=0 ; i<matrix.length ; i++) {	
			if (!inPlace)
				newData[i] = new double[columnLen];
			for (int j=0 ; j<columnLen; j++) {
				double value = matrix[i][j];
				newData[i][j] = offset + slope * value;
			}
		}
		return newData;
	}
	
	
	/**
	 * Make a deep clone of the given matrix. 
	 * @param featureData
	 * @return
	 */
	public static double[][] clone(double[][] matrix) {
		if (matrix == null)
			return null;
		if (matrix.length == 0)
			return new double[0][];
		
		int columnLen = matrix[0].length;
		double[][] newData = new double[matrix.length][];
		for (int i=0 ; i<matrix.length ; i++) {	
			newData[i] = new double[columnLen];
			for (int j=0 ; j<columnLen; j++) 
				newData[i][j] = matrix[i][j];
		}
		return newData;
	}

	/**
	 * Return a the values in the given matrix adjust to have a zero average value. 
	 * @param matrix
	 * @param inPlace true if the input matrix is modified, otherwise create a new matrix.
	 * @return never null.
	 */
	public static double[][] zeroMean(double[][] matrix, boolean inPlace) {
		double total = 0; 
		int count = 0;
		for (int i=0 ; i<matrix.length ; i++) {	
			double[] column = matrix[i];
			for (int j=0 ; j<column.length; j++) {
				total += column[j];
			}
			count += column.length;
		}
		if (count == 0)
			return matrix;

		double mean = total / count;
		
		double[][] newData = inPlace ? matrix : new double[matrix.length][];
		for (int i=0 ; i<matrix.length ; i++) {	
			double[] srcColumn = matrix[i];
			double[] destColumn;
			if (!inPlace) {
				newData[i] = new double[srcColumn.length];
				destColumn = newData[i];
			} else {
				destColumn = srcColumn;
			}
			for (int j=0 ; j<destColumn.length ; j++)
				destColumn[j] = srcColumn[j] - mean;
		}
		
		return newData;
	}

//	public static double[][] transpose(double[][] matrix) {
//		if (matrix == null || matrix.length == 0)
//			throw new IllegalArgumentException("null or empty matrix");
//
//		int inputColumns = matrix.length;
//		int inputRows = matrix[0].length;
//		
//		double[][] newMatrix = new double[inputRows][];
//		for (int i=0 ; i<matrix.length ; i++) {	
//			newMatrix[i] = new double[inputColumns];
//			for (int j=0 ; j<matrix[i].length ; j++) {	
//				newMatrix[i][j] = matrix[j][i];
//			}
//		}
//		return newMatrix;
//
//	}
	
	public static double[][] transpose(double[][] matrix) {
		if (matrix == null || matrix.length == 0)
			throw new IllegalArgumentException("null or empty matrix");
		int nColumns = matrix.length;
		int nRows = matrix[0].length;
		double[][] newMatrix ;
//		if (inPlace) {
//			if (nRows != nColumns)
//				throw new IllegalArgumentException("Can not transpose in place when matrix is not square.");
//			for (int i=0 ; i<nRows ; i++) {
//				for (int j=i+1 ; j<nColumns ; j++)  {
//					double tmp = matrix[i][j];
//					matrix[i][j] = matrix[j][i];
//					matrix[j][i] = tmp;
//				}
//			}
//			newMatrix = matrix;
//		} else {
			newMatrix = new double[nRows][];
			for (int i=0 ; i<nRows ; i++) {
				newMatrix[i] = new double[nColumns];
				for (int j=0 ; j<nColumns ; j++) 
					newMatrix[i][j] = matrix[j][i];
			}
//		}
		return newMatrix;
	}
	
	/**
	 * Returns an array containing only the diagonal elements of a matrix
	 * @param fullMatrix Full 2D matrix
	 * @return 1D array containing the diagonal elements of fullMatrix
	 */
	public static double[] selectDiagonalComponents(double[][] fullMatrix) {
		double[] diagMatrix = new double[fullMatrix.length];
		for (int i=0; i<fullMatrix.length; i++) {
			diagMatrix[i] = fullMatrix[i][i];
		}
		return diagMatrix;
	}

	public enum NormalizationMode {
		Row,
		Column,
		Matrix
	}
	/**
	 * Scale/shift values in the matrix.
	 * Columns are indexed by first index, rows by second index.
	 * @param m data to base return values on
	 * @param normalizeStddev if true, then normalize the inputs relative to the standard deviation.  If false,
	 * then normalize to the range of values.
	 * @param zeroMean if true, then also remove the mean from the input.
	 * @param mode controls whether normalization is done across the whole matrix, across individual rows or individual columns. 
	 * @param inPlace if true, then modify the input matrix, otherwise create a new one.
	 * @return a matrix of the same size as the input and allocated according to inPlace.
	 */
	public static double[][] normalize(double[][] matrix, boolean normalizeStddev, boolean zeroMean, NormalizationMode mode, boolean inPlace) {
		double max = -Double.MAX_VALUE, min = Double.MAX_VALUE;
		
		// Compute the summary stats, if not doing per column normalization.
//		OnlineStats matrixStats = mode == NormalizationMode.Matrix ?  new OnlineStats() : null;
		OnlineStats matrixStats = null; 
		if (mode == NormalizationMode.Matrix) 
			matrixStats = getStats(matrix);
		

		// Set up the matrix-wide scaling values, which are only used if perColumn = false
		double matrixRange = matrixStats == null  ? 1 : (normalizeStddev ? matrixStats.getStdDev() : (matrixStats.getMaximum() - matrixStats.getMinimum())); 
		if (matrixRange == 0 || Double.isNaN(matrixRange))
			matrixRange = 1;
		double matrixScale = 1.0 / matrixRange;
		double matrixMean = matrixStats == null || !zeroMean ? 0 : matrixStats.getMean();
		double[][] newMatrix;

		if (mode != NormalizationMode.Row) {
			newMatrix = inPlace ? matrix : new double[matrix.length][];
			boolean perColumn = mode == NormalizationMode.Column;
			for (int i=0 ; i<matrix.length ; i++) {	
				double[] srcColumn = matrix[i];
				double[] destColumn = srcColumn;
				if (!inPlace && !perColumn) {
					newMatrix[i] = new double[srcColumn.length];
					destColumn = newMatrix[i];
				}
				if (perColumn) {
					if (inPlace)
						VectorUtils.normalize(srcColumn, normalizeStddev, zeroMean, true);
					else
						newMatrix[i] = VectorUtils.normalize(srcColumn, normalizeStddev, zeroMean, false);
				} else {
					for (int j=0 ; j<srcColumn.length; j++) {
						double value = srcColumn[j]; 
						destColumn[j] = matrixScale * (value - matrixMean);
					}
				}
			}
		} else { // normalize by row
			int nRows = matrix[0].length;
			int nColumns = matrix.length;
			if (!inPlace) {
				newMatrix = new double[nColumns][];
				for (int i=0 ; i<nColumns ; i++)
					newMatrix[i] = new double[nRows];
			} else {
				newMatrix = matrix;
			}
			double[] rowData = new double[nColumns];				
			for (int i=0 ; i<nRows ; i++) {		// Over each row.
				// Get row values into new array
				for (int j=0 ; j<nColumns ; j++)  {
					rowData[j] = matrix[j][i];
//					if (Double.isNaN(rowData[j])) 
//						throw new IllegalArgumentException("NaN");
				}
				// Normalize the row
				rowData = VectorUtils.normalize(rowData, normalizeStddev, zeroMean, true);
				// Put values back into the row of the output matrix.
				for (int j=0 ; j<nColumns ; j++) {
//					if (Double.isNaN(rowData[j])) {
//						throw new IllegalArgumentException("NaN");
//					}
					newMatrix[j][i] = rowData[j];
				}
			}
		}
//		for (int i=0 ; i<newMatrix.length ; i++) {
//			for (int j=0 ; j<newMatrix[i].length ; j++) 
//				if (Double.isNaN(newMatrix[i][j]))
//					throw new IllegalArgumentException("NaN");
//		}
		return newMatrix;
	}

	/**
	 * Get the statistics across all cells in the matrix.
	 * @param matrix
	 */
	public static OnlineStats getStats(double[][] matrix) {
		OnlineStats matrixStats = new OnlineStats();
		for (int i=0 ; i<matrix.length ; i++) {	
			double[] column = matrix[i];
			matrixStats.addValidSamples(column);
		}
		return matrixStats;
	}


	
	public static void showMatrix(double[][] matrix) {
		int columns = matrix.length;
		int rows = matrix[0].length;
		for (int i=0 ; i<rows; i++) {
		   for (int j=0 ; j<columns; j++) {
			   System.out.print(matrix[j][i] + " ");
		   }
		   System.out.println("");
		}
		
	}
	public static void main(String[] args) {
		int size = 4;
		double[][] matrix = new double[size][];
		for (int i=0 ; i<size ; i++) {
			matrix[i] = new double[size];
			Arrays.fill(matrix[i], i);
		}
		
		System.out.println("");
		showMatrix(matrix);
		double[][] delta1 = MatrixUtil.columnDelta(matrix, 2); 
		System.out.println("");
		showMatrix(delta1);
		double[][] delta2 = MatrixUtil.columnDelta(delta1, 2); 
		System.out.println("");
		showMatrix(delta2);
	}

	/**
	 * Create a new matrix from a range of columns from the given matrix.
	 * @param matrix
	 * @param startColumn the first column, 0-indexed, to include in the output 
	 * @param lastColumnExclusive 0-based index of last column to include, exclusive.
	 * @param refColumns if false, then make copies of the source column, otherwise just reference the columns from the given matrix.
	 * @return never null. 
	 */
	public static double[][] selectColumns(double[][] matrix, int startColumn, int lastColumnExclusive, boolean refColumns) {
		if (startColumn < 0 || lastColumnExclusive < 0)
			throw new IllegalArgumentException("Column indices must not be negative.");
		if (startColumn >= matrix.length) 
			throw new IllegalArgumentException("start index is too large");
		if (lastColumnExclusive > matrix.length)
			throw new IllegalArgumentException("last index is too large");
			
		int columns = lastColumnExclusive - startColumn;
		double[][] newMatrix = new double[columns][];
		for (int i=0; i<columns; i++) {
			double[] column = matrix[startColumn + i];
			if (refColumns)
				newMatrix[i] = column; 
			else
				newMatrix[i] = Arrays.copyOf(column,column.length);
		}
		return newMatrix;
	}

	/**
	 * Append the columns of the two matrices to create a new matrix.  The columnar data in the
	 * output is shared with the data in the input - that is columns are not copied in the new matrix. 
	 * @param base
	 * @param toAppend
	 * @return a matrix that has the base matrix as the left-most (starting at 0 index) columns
	 * and the toAppend matrix as the right-most columsn (ends with the last index of the returned matrix).
	 */
	public static double[][] appendColumns(double[][] base, double[][] toAppend) {
		double newMatrix[][] = new double[base.length + toAppend.length][];
		System.arraycopy(base, 0, newMatrix, 0, base.length);
		System.arraycopy(toAppend, 0, newMatrix, base.length, toAppend.length);
		return newMatrix;
	}

	/**
	 * Compute the full auto-correlation with each column.
	 * @param matrix
	 * @param lags the number of lags to compute in the correlations, including 0.  
	 * Must be greater or equal to 0 and less then the size of a column.
	 * @return a matrix in which each column is the auto-correlation of the source column.
	 * The matrix has the same number of columns as the input and the size of
	 * each column is equal to the number of lags.
	 */
	public static double[][] autoCorrelateColumns(double[][] matrix, int lags) {
		int columnLength = matrix[0].length;
		if (lags < 0 || lags >= columnLength) 
			throw new IllegalArgumentException("lags must be greater or equal to 0 and less than the column length (" + columnLength + ")");
		double[][] newMatrix = new double[matrix.length][];
		for (int i=0 ; i<matrix.length ; i++) {
			double[] column = matrix[i];
//			double[] correlation = MathUtil.autoCovariance(column, lags, false);
			double[] correlation = VectorUtils.autoCorrelate(column);
			newMatrix[i] = correlation;
		}
		return newMatrix;
	}

	/**
	 * Linearly map the range (min, max) of the given matrix to range of the range-defining matrix. 
	 * @param matrix the matrix to rerange.
	 * @param range defines range of values to which the given matrix is mapped.
	 * @param inPlace if true, then the input array is modified in place, otherwise a 
	 * new array is created to contain the transformed values.
	 * @return
	 */
	public static double[][] matchRange(double[][] matrix, double[][] range, boolean inPlace) {
		OnlineStats rangeStats = getStats(range); 
		double destMax = rangeStats.getMaximum();
		double destMin = rangeStats.getMinimum();
		return matchRange(matrix, destMin, destMax, inPlace);
	}

	/**
	 * Linearly map the range (min, max) of the given matrix to range of the range-defining matrix. 
	 * @param matrix
	 * @param newMin the desired minimum of the returned matrix.
	 * @param newMax the desired maximum of the returned matrix.
	 * @param inPlace if true, then the input array is modified in place, otherwise a 
	 * new array is created to contain the transformed values.
	 * @return a matrix of the same dimensions as the input, linearly mapped to the given range.
	 */
	public static double[][] matchRange(double[][] matrix, double newMin, double newMax, boolean inPlace) {
		OnlineStats matrixStats = getStats(matrix); 
		double srcMax = matrixStats.getMaximum();
		double srcMin = matrixStats.getMinimum();
		
		/**
		 * See https://stackoverflow.com/questions/5731863/mapping-a-numeric-range-onto-another.
		 * Key point as follows:
	     *     double slope = 1.0 * (output_end - output_start) / (input_end - input_start)
    	 *     output = output_start + slope * (input - input_start)
    	 * or
    	 *    output = (output_start - slope * input_start) + slope * input
    	 * or
    	 *    offset = output_start - slope * input_start
    	 *    output = offset + slope * input (input = matrix[i])
		 */
		double slope = (newMax - newMin) / (srcMax - srcMin);
		double offset = newMin - slope * srcMin; 
		double[][] newMatrix = inPlace ? matrix : new double[matrix.length][];
		for (int i=0 ; i<newMatrix.length ; i++) {
			newMatrix[i] = VectorUtils.linearTransform(matrix[i], slope, offset, inPlace);
		}
//		AISPLogger.logger.info("min=" + newMin + " max=" + newMax + " OLD stats=" + matrixStats + " NEW stats= " + getStats(newMatrix));
		return newMatrix;
	}



//	/**
//	 * Join the given array of matrices into a single matrix by appending columns left to right. 
//	 * @param matrices
//	 * @param refColumns if false, then make copies of the source column, otherwise just reference the columns from the given matrix.
//	 * @return a new matrix that has the same number of rows as the input, and a number of columns equal to the sum of all
//	 * columns in the input matrices. never null.
//	 */
//	public static double[][] combineColumns(double[][][] matrices, boolean refColumns) {
//		// Find total columns in the output and create the output matrix.
//		int totalColumns = 0;
//		for (int i=0 ; i<matrices.length ; i++)
//			totalColumns += matrices[i].length;
//		double[][] newMatrix = new double[totalColumns][];
//		
//		// Copy the columns into the new matrix.
//		int colIndex = 0;
//		for (int i=0 ; i<matrices.length ; i++) {
//			for (int j=0 ; j<matrices[i].length ; j++) {
//				double[] column = matrices[i][j];
//				if (refColumns)
//					newMatrix[colIndex] = column;
//				else
//					newMatrix[colIndex] = Arrays.copyOf(column,column.length);
//				colIndex++;
//			}
//		}
//
//		return newMatrix;
//	}
	
//	/**
//	 * Create a new matrix that has the columns of the second appended to the columns of the first.
//	 * Columns are in the first index.
//	 * @param first
//	 * @param second
//	 * @return a matrix containing both matrices of data.
//	 */
//	public static double[][] stack(double[][] first, double[][] second) {
//		if (first.length != second.length)
//			throw new IllegalArgumentException("arrays must be of the same length in the first dimension");
//		double[][] stacked = new double[first.length][];
//		
//		for (int i=0 ; i<first.length ; i++) {
//			int len = first[i].length + second[i].length;
//			stacked[i] = new double[len];
//			System.arraycopy(first[i],  0, stacked[i], 0,            first[i].length);
//			System.arraycopy(second[i], 0, stacked[i], first.length, second[i].length);
//		}
//		
//		return stacked;
//	}

	
}
