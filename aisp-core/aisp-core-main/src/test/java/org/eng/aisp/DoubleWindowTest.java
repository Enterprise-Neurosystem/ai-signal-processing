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
package org.eng.aisp;

import java.util.ArrayList;
import java.util.List;


public class DoubleWindowTest extends AbstractDoubleDataWindowTest {

	@Override
	protected List<IDataWindow> getSynchronizedDataWindow(double startTime, int samplingRate, int sampleSize) {
//		Assert.assertTrue("Must be zero so we can assume msec as the time unit", startTime == 0);
		List<IDataWindow> windowList = new ArrayList<IDataWindow>();
		double endTime = startTime + 1000.0 * (sampleSize) * (1.0 / samplingRate);	// SoundClip requires msec.
		double data[] = new double[sampleSize];
		windowList.add(getDataWindow(startTime, endTime, data));
		
		return windowList;
	}

	@Override
	protected IDataWindow<double[]> getDataWindow(double startMsec, double endMsec, double[] data) {
		return new DoubleWindow(startMsec, endMsec, data);
	}

}
