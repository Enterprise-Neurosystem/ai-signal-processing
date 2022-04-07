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

import java.io.IOException;

import org.eng.ENGException;
import org.eng.util.IProcessController.IProcessControllerListener;
import org.junit.Assert;
import org.junit.Test;

public class RESTProcessControllerTest {
	
	public class StartStopListener implements IProcessControllerListener {

		int startCount = 0;
		int stopCount = 0;
		
		@Override
		public void startProcessing() throws ENGException, IOException {
			startCount++;
		}

		@Override
		public void stopProcessing() throws ENGException, IOException {
			stopCount++;
		}
		
	}

	@Test
	public void testStartStop() throws Exception {
		int port = 31234;
		IProcessController controller = new RESTProcessController(port);
		StartStopListener ssl = new StartStopListener();
		controller.addListener(ssl);
		controller.start();
		
		String startURL = "http://localhost:" + port + "/start";
		String stopURL = "http://localhost:" + port + "/stop";

		HttpUtil.GET(startURL, null);
		HttpUtil.GET(stopURL, null);
		
		controller.stop();
		
		Assert.assertTrue(ssl.startCount == 1);
		Assert.assertTrue(ssl.stopCount == 1);
		
	}
}
