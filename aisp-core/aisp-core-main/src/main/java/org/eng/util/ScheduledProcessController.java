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

import java.time.LocalTime;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.eng.ENGLogger;

/**
 * A process controller that uses a DailySchedule to control when the process is started.  
 * @author DavidWood
 *
 */
public class ScheduledProcessController extends AbstractProcessController implements IProcessController {

	RepeatingCallable checker;
	private DailySchedule schedule; 
	
	private static AtomicInteger counter = new AtomicInteger();

	public ScheduledProcessController(Collection<TimeRange> schedule) {
		this(new DailySchedule(schedule));
	}

	public ScheduledProcessController(DailySchedule schedule) {
		super(ENGLogger.logger);
		this.schedule = schedule;
	}

	private class Scheduler implements Callable<Integer> {
		boolean started = false;

		@Override
		public Integer call() throws Exception {
			if (schedule.isScheduledNow()) {
				if (!started) {
//					ENGLogger.logger.info("Starting process");
					ScheduledProcessController.this.notifyListeners(null, true);	// startProcessing()
					started = true;
				}
			} else if (started) {
//					ENGLogger.logger.info("Stopping process");
				ScheduledProcessController.this.notifyListeners(null, false);	// stopProcessing()
				started = false;
			}
			LocalTime now = LocalTime.now();
			int mseconds = schedule.getMsecUntilNextScheduleChange(now); 
			if (mseconds == 0)
				mseconds = 500;
//			ENGLogger.logger.info("now=" + now + ", schedule=" + schedule + ". Sleeping " + mseconds + " msec until next schedule change");
			return mseconds;
		}
		
	}

	@Override
	public synchronized boolean start() throws Exception {
		if (checker != null)
			return true;
		
		int index = counter.getAndIncrement();
		checker = new RepeatingCallable(this.getClass().getSimpleName() + "-" + index, new Scheduler()); 
		checker.start();
		return true;
	}

	@Override
	public synchronized void stop() {
		if (checker == null)
			return;
		checker.stop();
		checker = null;
	}

}
