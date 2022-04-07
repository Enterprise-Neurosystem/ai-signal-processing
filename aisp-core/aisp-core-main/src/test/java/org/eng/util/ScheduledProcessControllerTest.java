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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.eng.ENGException;
import org.eng.util.IProcessController.IProcessControllerListener;
import org.junit.Assert;
import org.junit.Test;


;
public class ScheduledProcessControllerTest {
	
	private static class ScheduleListener implements IProcessControllerListener {

		List<LocalTime> startTimes = new ArrayList<LocalTime>();
		List<LocalTime> stopTimes = new ArrayList<LocalTime>();
		
		@Override
		public void startProcessing() throws ENGException, IOException {
			LocalTime lt = LocalTime.now();
//			AISPLogger.logger.info("Starting: " + lt);
			startTimes.add(lt);
		}

		@Override
		public void stopProcessing() throws ENGException, IOException {
			LocalTime lt = LocalTime.now();
//			AISPLogger.logger.info("Stopping: " + lt);
			stopTimes.add(lt);
		}
		
	}

	@Test
	public void testStartStop() throws Exception {
		// Create schedule over the next minute
		List<TimeRange> schedule = new ArrayList<TimeRange>();
		int intervalLen = 2;	// Seconds
		int intervals = 3;
		int scheduleLen =  intervals * intervalLen;
		int sleepMsec = 1000*(scheduleLen + intervalLen);
		LocalTime now = LocalTime.now();
		LocalTime firstTime = now.plusSeconds(intervalLen);
		LocalTime secondTime = now.plusSeconds(2*intervalLen);
		LocalTime lastTime = now.plusSeconds(scheduleLen);
		TimeRange firstRange = new TimeRange(now, firstTime);
		TimeRange secondRange = new TimeRange(secondTime, lastTime);	// Leaves a gap from firstTime to secondTime
		schedule.add(firstRange);	// 0..1/3
		schedule.add(secondRange);	// 2/3..1
		
		ScheduledProcessController controller = new ScheduledProcessController(schedule);
		ScheduleListener listener = new ScheduleListener();
		controller.addListener(listener);
		controller.start();
		Thread.sleep(sleepMsec);
		controller.stop();
	
		// Two appointments/schedules, so we should see 2 starts and 2 stops.
		Assert.assertTrue(listener.startTimes.size() == 2);
		Assert.assertTrue(listener.stopTimes.size() == 2);
		
	}
}
