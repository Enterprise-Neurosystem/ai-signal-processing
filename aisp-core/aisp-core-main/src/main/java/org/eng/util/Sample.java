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
package org.eng.util;

/**
 * Methods to resample a data array.
 * 
 * @author dawood
 *
 */
public class Sample {

	/**
	 * Down sample using rolling windows to produce a new array of the given size.
	 * @param data
	 * @param maxSize the requested size of the output array depending on padToMaxSize.  This is only a request as not all data lengths can accommodate any size.
	 * For example, 11 input size can not have an output size of 5 using rolling windows (window size of 3 gives size 4 and window size
	 * of 2 gives size 6).  
	 * @param padToMaxSize if true, then the output array is always of size maxSize and if needed the last values are copied from the last value to
	 * to given the requested size. 
	 * @return an array of the size equal to maxSize or less, unless padToMaxSize is set to true in which case the size will always be maxSize.
	 */
	public static double[] downSample(double[] data, int maxSize, boolean padToMaxSize )  { 
		int windowSize = (int)((double)(data.length + maxSize - 1) / maxSize) ;
//		System.out.println("data.length=" + data.length + ", size=" + reqSize + ", windowSize= " + windowSize);
		double[] sampled = null;
		if (padToMaxSize) 	// Provide the array to sample into since we know the length we want.
			sampled = new double[maxSize];
		
		sampled = downSample(data, windowSize, windowSize, sampled);
		
		return sampled;
	}

	/**
	 * Basic down sampling method.
	 * Elements of the output array are an average of the values from a range/window of elements in the input array.
	 * @param data data to down sample
	 * @param windowSize number of elements in the input to average together to produce a single element in the output array.
	 * @param windowShift the number of elements to shift by in the input when producing the next element of the output array.
	 * Setting this to one, effectively implements a box filter with full width equal to windowSize.
	 * Setting this to windowSize, gives a rolling window.
	 * Setting this to a value larger than windowSize will cause elements of the input array to not be included in the output.
	 * @return an array of size (data.length / windowShift + .5). 
	 */
	public static double[] downSample(double[] data, int windowSize, int windowShift) {
		return downSample(data,windowSize,windowShift, null);
	}
	
	/**
	 * Basic down sampling method.
	 * Elements of the output array are an average of the values from a range/window of elements in the input array.
	 * @param data data to down sample
	 * @param windowSize number of elements in the input to average together to produce a single element in the output array.
	 * @param windowShift the number of elements to shift by in the input when producing the next element of the output array.
	 * Setting this to one, effectively implements a box filter with full width equal to windowSize.
	 * Setting this to windowSize, gives a rolling window.
	 * Setting this to a value larger than windowSize will cause elements of the input array to not be included in the output.
	 * @param newData the destination for the padded results.  
	 * @return a new array of size (data.length / windowShift + .5) if newData is null, otherwise newData and if needed the
	 * last values of newData are set to the most recent value.
	 */
	private static double[] downSample(double[] data, int windowSize, int windowShift, double[] newData)  {
		if (windowSize < 1)
			throw new IllegalArgumentException("window size must be greater than 0");
		if (windowSize > data.length)
			throw new IllegalArgumentException("window size is larger than input data");
		if (windowShift < 1)
			throw new IllegalArgumentException("window shift must be greater than 0");
		if (windowShift > data.length)
			throw new IllegalArgumentException("window shift is larger than input data");
		int extraLen = data.length % windowShift; 
		int newSize = (int)((double)(data.length + extraLen) / windowShift);
		if (newData == null) {
	//		System.out.println("data.length=" + data.length + ", windowShift=" + windowShift + ", newSize= " + newSize);
			newData = new double[newSize];
		} else if (newData.length < newSize) {
			throw new IllegalArgumentException("Given newData array must be at least " + newSize + " long");
		}

		int srcIndex = 0;
		for (int i=0 ; i<newData.length ; i++) {
			double value = 0;
			int count=0;
			for (int j=srcIndex ; j<srcIndex+windowSize && j<data.length; j++) { 
				value += data[j];
				count++;
			}
			if (count == 0)	// Pad the end of the array with the last valid value
				newData[i] = newData[i-1]; 
			else
				newData[i] = value / count;
			srcIndex += windowShift;
		}
		return newData;
	}

	/**
	 * Convenience method on {@link #downSample(double[], int, double[], boolean)}.
	 * @param data
	 * @param size
	 * @return
	 */
	public static double[] downSample(double[] data, int size) {
		double[] newData = new double[size];
		downSample(data,newData, true);
		return newData;
	}

	/**
	 * Down sample to get exactly the number of requested output elements computed from variable sized windows, as necessary. 
	 * The  window sizes will be (int)(data.length /newData.length) +/- 1.
	 * @param data
	 * @param newData location to place new samples and defines the window sizes based on the length.
	 * @param checkValidity if true then only include valid values in computed averages for each cell.
	 * If no valid values are available for a given down sampled cell, then Double.NaN is placed in the cell.
	 * @return true if all cells could be filled with a value other than Double.NaN. 
	 */
	public static boolean downSample(double[] data, double[] newData, boolean checkValidity)  {
		if (newData == null)
			throw new IllegalArgumentException("newData must not be null");
		int size = newData.length;	
		if (size < 1)
			throw new IllegalArgumentException("window size must be greater than 0");
		if (size > data.length)
			throw new IllegalArgumentException("window size is larger than input data");

		final double winSize = (double)data.length/size;
		int start = 0;
		int end;
		boolean allValid = true;
//		System.out.println("winSize=" + winSize); 
		for (int i=0 ; i<newData.length ; i++) {
			double value = 0;
			int count=0;
			end   = (int)Math.floor(winSize*(i+1));
//			System.out.println("i=" + i + ", start/end=" + start + "/" + (end) +", winsize=" + (end-start));
			for (int j=start; j<end ; j++) { // j<data.length; j++) { 
				double v = data[j];
				if (!checkValidity || !Double.isNaN(v)) {
					value += v;
					count++;
				}
			}
			start = end; 
			if (count > 0) {
				newData[i] = value / count;
			} else {
				newData[i] = Double.NaN;
				allValid = false;
			}
		}
		return allValid;
	}
	
	public static void main(String args[]) {
		double[] data = new double[] { 1, 2, 3, 4, 5, 6,7, 8,9, 10,11 };
		double[] newData;
		newData = downSample(data, 1, 1);
		showData(newData);
		newData = downSample(data, 2, 1);
		showData(newData);
		newData = downSample(data, 2, 2);
		showData(newData);
		newData = downSample(data, 3, 2);
		showData(newData);
		newData = downSample(data, 5, 5);
		showData(newData);

		System.out.println("No padding");
		newData = downSample(data, 1, false);
		showData(newData);
		newData = downSample(data, 2, false);
		showData(newData);
		newData = downSample(data, 3, false);
		showData(newData);
		newData = downSample(data, 4, false);
		showData(newData);
		newData = downSample(data, 5, false);
		showData(newData);

		System.out.println("With padding");
		newData = downSample(data, 1, true);
		showData(newData);
		newData = downSample(data, 2, true);
		showData(newData);
		newData = downSample(data, 3, true);
		showData(newData);
		newData = downSample(data, 4, true);
		showData(newData);
		newData = downSample(data, 5, true);
		showData(newData);

		System.out.println("With varying windows");
		data[0] = Double.NaN;
		newData = downSample(data, 1);
		showData(newData);
		newData = downSample(data, 2);
		showData(newData);
		newData = downSample(data, 3);
		showData(newData);
		newData = downSample(data, 4);
		showData(newData);
		newData = downSample(data, 5);
		showData(newData);
		newData = downSample(data, data.length);
		showData(newData);
	}

	private static void showData(double[] data) {
		for (int i=0 ; i<data.length ; i++) {
			if (i != 0)
				System.out.print(", ");
			System.out.print(data[i]);
		}
		System.out.println("");
	}
		
}
