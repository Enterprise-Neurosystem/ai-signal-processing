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
package org.eng.aisp.transform;

import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.DoubleWindow;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.ILabeledDataWindow;

public class LogScalingTrainingWindowTransform extends AbstractTrainingWindowTransform<double[]>
		implements ITrainingWindowTransform<double[]> {

	private static final long serialVersionUID = 1781181884984600416L;

	protected final double applicableMinimum;

	public LogScalingTrainingWindowTransform(double applicableMinimum) {
		super(false);
		this.applicableMinimum = applicableMinimum;
	}


	@Override
	public ITrainingWindowTransform<double[]> newInstance() {
		return new LogScalingTrainingWindowTransform(applicableMinimum);
	}

	@Override
	public int multiplier() {
		return 2;
	}

	@Override
	protected List<IDataWindow<double[]>> getMutatedWindows(String trainingLabel, ILabeledDataWindow<double[]> ldw) {
		double logMinThreshold = Math.log10(applicableMinimum);
		IDataWindow<double[]> clip = ldw.getDataWindow();
			double[] data = clip.getData();
			double[] newData = new double[data.length]; 
			double max = 0;
			for (int i=0 ; i<data.length ; i++) {
				double d = data[i];
				double absd = Math.abs(d);
				if (absd > max)
					max = absd;
				if (absd >= applicableMinimum) {
//					System.out.print("LOG: d=" + d);
					boolean wasNegative = d < 0;
					// -1 < d <= 1.  d != 0
					d = -Math.log10(absd);	// d now > 0  
					if (d > logMinThreshold)
						d = logMinThreshold;
					d = logMinThreshold - d;	// Invert so originally largest values are set to 8.
					d /= logMinThreshold;		// scale back into the range 0..1
					if (wasNegative)
						d = -d;
//					System.out.println("newd=" + d);
				}
				newData[i] = absd;
			}
			
			// Scale back to something like the original range.
			if (max != 1.0) {
				for (int i=0 ; i<newData.length ; i++) 
					newData[i] *= max; 
			}

			clip = new DoubleWindow(clip.getStartTimeMsec(), clip.getEndTimeMsec(), newData);
			List<IDataWindow<double[]>> clist = new ArrayList<IDataWindow<double[]>>();
			clist.add(clip);
			return clist;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		long temp;
		temp = Double.doubleToLongBits(applicableMinimum);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof LogScalingTrainingWindowTransform))
			return false;
		LogScalingTrainingWindowTransform other = (LogScalingTrainingWindowTransform) obj;
		if (Double.doubleToLongBits(applicableMinimum) != Double.doubleToLongBits(other.applicableMinimum))
			return false;
		return true;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "LogScalingTrainingWindowTransform [applicableMinimum=" + applicableMinimum + "]";
	}

}
