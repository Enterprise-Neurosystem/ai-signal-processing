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

import org.eng.aisp.ILabeledDataWindow;

public class IdentityWindowTransform implements ITrainingWindowTransform<double[]>  {

	private static final long serialVersionUID = 4893466875415242470L;
	final int nCopies;
	
	public IdentityWindowTransform(int nCopies) {
		this.nCopies = nCopies;
	}

	@Override
	public IdentityWindowTransform newInstance() {
		return new IdentityWindowTransform(nCopies) ;
	}

	@Override
	public Iterable<ILabeledDataWindow<double[]>> apply(String trainingLabel, ILabeledDataWindow<double[]> t) {
		List<ILabeledDataWindow<double[]>> list = new ArrayList<ILabeledDataWindow<double[]>>();
		for (int i=0 ; i<nCopies ; i++)
			list.add(t);
		return list;
	}

	@Override
	public int multiplier() {
		return nCopies;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + nCopies;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof IdentityWindowTransform))
			return false;
		IdentityWindowTransform other = (IdentityWindowTransform) obj;
		if (nCopies != other.nCopies)
			return false;
		return true;
	}

}
