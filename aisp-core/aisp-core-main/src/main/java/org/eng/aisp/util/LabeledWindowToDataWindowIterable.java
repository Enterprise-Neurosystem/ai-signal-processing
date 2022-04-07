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

import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.IDataWindow;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.util.IMutator;
import org.eng.util.MutatingIterable;

/**
 * Allows the conversion of an iterable of ILabeledDataWindows to IDataWindows.
 * 
 * @author DavidWood
 */
public class LabeledWindowToDataWindowIterable<WINDATA, LDW extends ILabeledDataWindow<WINDATA>, DW extends IDataWindow<WINDATA>> extends MutatingIterable<LDW,DW> {

	/**
	 * Mutator that converts the ILabeledDataWindow to an IDataWindow.
	 * @author DavidWood
	 * @param <DATA>
	 */
	static class LDW2DWMutator<WINDATA, LDW extends ILabeledDataWindow<WINDATA>, DW extends IDataWindow<WINDATA>> implements IMutator<LDW, DW> {


		@Override
		public List<DW> mutate(LDW item) {
			DW dw = (DW) item.getDataWindow();
			List<DW> dwList = new ArrayList<DW>();
			dwList.add(dw);
			return dwList;
		}
		
	}

	public LabeledWindowToDataWindowIterable(Iterable<LDW> iterable) {
		super(iterable, new LabeledWindowToDataWindowIterable.LDW2DWMutator<WINDATA, LDW, DW>());
	}
}