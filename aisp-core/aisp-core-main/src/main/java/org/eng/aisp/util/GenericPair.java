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

/**
 * A generic pair of two objects, referenced as x and y.
 * 
 * @author dawood

 */
public class GenericPair<XDATA, YDATA> {

	protected final XDATA xValue;
	protected final YDATA yValue;

	public GenericPair(XDATA xValue, YDATA yValue) {
		super();
		this.xValue = xValue;
		this.yValue = yValue;
	}
	/**
	 * @return the xValues
	 */
	public XDATA getX() {
		return xValue;
	}

	/**
	 * @return the yValues
	 */
	public YDATA getY() {
		return yValue;
	}
	@Override
	public String toString() {
		return "GenericPair [xValue=" + xValue + ", yValue=" + yValue + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((xValue == null) ? 0 : xValue.hashCode());
		result = prime * result + ((yValue == null) ? 0 : yValue.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof GenericPair))
			return false;
		GenericPair other = (GenericPair) obj;
		if (xValue == null) {
			if (other.xValue != null)
				return false;
		} else if (!xValue.equals(other.xValue))
			return false;
		if (yValue == null) {
			if (other.yValue != null)
				return false;
		} else if (!yValue.equals(other.yValue))
			return false;
		return true;
	}

}
