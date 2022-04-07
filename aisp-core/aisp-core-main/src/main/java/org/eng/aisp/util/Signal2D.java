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

import org.eng.util.Vector;

/**
 * Simply defines the x as a Vector and y values as a double[].
 * @author dawood
 *
 */
public class Signal2D extends GenericSignal2D<Vector, double[]> {
	
	/**
	 * @param xValues
	 * @param yValues
	 */
	public Signal2D(Vector xValues, double[] yValues) {
		super(xValues, yValues);
	}
	
	
	public int size() {
		return this.xyValues.getX().length();
	}

	/**
	 * 
	 * @param startIndex index of first item, inclusive.
	 * @param endIndex index of last item, exclusive.
	 * @return never null.
	 */
	public Signal2D trim(int startIndex, int endIndex) {
		int size = this.size();
		if (startIndex < 0)
			throw new IllegalArgumentException("startIndex must be 0 or larger");
		if (startIndex >= size)
			throw new IllegalArgumentException("startIndex is larger than the size");
		if (endIndex <= startIndex)
			throw new IllegalArgumentException("endIndex must be larger than startIndex");
		if (endIndex >= size)
			throw new IllegalArgumentException("endIndex is larger than the size");

		Vector v = this.xyValues.xValue;
		double[] y = this.xyValues.yValue;
		int len = endIndex - startIndex;
		double[] newY = new double[len];
		System.arraycopy(y, startIndex, newY, 0, len);

		if (v.isRegular()) {
			double origin = v.getOrigin() + startIndex * v.getDelta();
			v = new Vector(origin, v.getDelta(), len);
		} else {
			double[] x = this.xyValues.xValue.getVector();
			double[] newX = new double[len];
			System.arraycopy(x, startIndex, newX, 0, len);
			v = new Vector(newX);
		}
		return new Signal2D(v, newY)	;
	}

	public Signal2D trimX(double minX, double maxX) {
		if (minX >= maxX)
			throw new IllegalArgumentException("min (" + minX + ") is greater or equal to max (" + maxX + ")");
		Vector v = this.xyValues.xValue;
		double[] y = this.xyValues.yValue;
		int maxIndex = y.length-1;
		if (v.isRegular()) {
			double delta = v.getDelta();
			int firstIndex = (int)((minX - v.getOrigin()) / delta + .5);	// >= to minX
			if (firstIndex < 0)
				firstIndex = 0;
			else if (firstIndex > maxIndex)
				return new Signal2D(new Vector(minX, delta, 0), new double[0]);
			int lastIndex = firstIndex + (int)((maxX - minX) / delta);		// <= maxX
			if (lastIndex > maxIndex)
				lastIndex = maxIndex;
			int len = lastIndex - firstIndex + 1;
			if (len == y.length)
				return this;
			double[] newY = new double[len];
			System.arraycopy(y, firstIndex, newY, 0, len);
			return new Signal2D(new Vector(minX, delta, len), newY);
			
		} else 
		{
			double[] x = this.xyValues.xValue.getVector();
			int firstIndex=0;
			while (firstIndex < maxIndex &&  x[firstIndex] < minX)
				firstIndex++;
			int lastIndex=maxIndex;
			while (lastIndex > firstIndex &&  x[lastIndex] > maxX)
				lastIndex--;
			int len = lastIndex - firstIndex + 1;
			if (len == y.length)
				return this;
			double[] newX = new double[len];
			System.arraycopy(x, firstIndex, newX, 0, len);
			double[] newY = new double[len];
			System.arraycopy(y, firstIndex, newY, 0, len);
			return new Signal2D(new Vector(newX), newY);
		}
	}


	/**
	 * Add the given number of pad values to the end of the vector.
	 * If this is an irregular vector (with explicit X values), then the added X values
	 * are computed based on the average delta value.
	 * @param newLength
	 * @param padValue
	 * @return
	 */
	public Signal2D pad(int newLength, double padValue) {
		int size = this.size();
		if (newLength < 0)
			throw new IllegalArgumentException("newLength must be 0 or larger");
		if (newLength <= size)
			throw new IllegalArgumentException("newLengt must be larger than current size");

		double[] y = this.xyValues.yValue;
		double[] newY = new double[newLength];
		System.arraycopy(y, 0, newY, 0, y.length); 

		Vector v = this.xyValues.xValue;
		if (v.isRegular()) {
			v = new Vector(v.getOrigin(), v.getDelta(), newLength);
		} else {
			double[] x = this.xyValues.xValue.getVector();
			double[] newX = new double[newLength];
			System.arraycopy(x, 0, newX, 0, x.length);
			double lastX = x[x.length-1];
			double delta = (lastX - x[0]) / (x.length-1);
			for (int i=x.length ; i<newLength ; i++) {
				newX[i] =  lastX + (i-x.length+1) * delta;
			}
			v = new Vector(newX);
		}
		return new Signal2D(v, newY);
	}
}
