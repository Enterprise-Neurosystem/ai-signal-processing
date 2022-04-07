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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * Maintains a list of TimeRanges representing a schedule of some sort.
 * @author DavidWood
 *
 */
public class DailySchedule implements ISchedule {

	List<TimeRange> schedule = new ArrayList<TimeRange>();

	private final static int MSEC_PER_MINUTE = 60 * 1000;
	private final static int MSEC_PER_HOUR = 60 * MSEC_PER_MINUTE; 
	private final static int MSEC_PER_DAY = 24 * MSEC_PER_HOUR; 
	
	public DailySchedule(Collection<TimeRange> schedule) {
		if (schedule.size() == 0)
			throw new IllegalArgumentException("Schedule is empty");
		this.schedule.addAll(schedule);
		Collections.sort(this.schedule);
	}

	@Override
	public boolean isScheduledNow() {
		LocalTime now = LocalTime.now(); 
		for (TimeRange range : schedule) {
			if (range.contains(now))
				return true;
			else if (range.after(now))
				break;
		}
		return false;
	}

	@Override
	public int getMsecUntilNextScheduleChange(LocalTime time) {
		LocalTime nextChange;
		for (TimeRange range : schedule) {
			if (range.contains(time))  {
//				ENGLogger.logger.info("Using end time: time="+time + ", range=" + range);
				nextChange = range.getEndTime();
			} else if (range.after(time)) {
//				ENGLogger.logger.info("Using start time: time="+time + ", range=" + range);
				nextChange = range.getStartTime();
			} else { 
				nextChange = null;
			}
			if (nextChange != null) 
				return computeMsecUntil(time, nextChange);
		}
		// Given time falls after all scheduled times so compute until time to first scheduled item.
		TimeRange range = this.schedule.get(0);
		nextChange = range.getStartTime();
//		ENGLogger.logger.info("Using 1st start time: time="+ time + ", range=" + range);
		return computeMsecUntil(time, nextChange);
	}


	/**
	 * Compute the number of milliseconds from the given time to the given to time.
	 * If the to time is less than the from time, then assume we wrap around midnight
	 * to get to the to time.
	 * @param from 
	 * @param to 
	 * @return
	 */
	private int computeMsecUntil(LocalTime from, LocalTime to) {
//		from = from.truncatedTo(ChronoUnit.MILLIS);
//		to = to.truncatedTo(ChronoUnit.MILLIS);
		int delta;
//		ENGLogger.logger.info("0 from=" + from + ",  to=" +  to); 
		if (to.equals(from)) {
			delta = 0;
//			ENGLogger.logger.info("1 Delta from " + from + " to " +  to + " is " + delta + " seconds");
		} else if (to.isAfter(from)) {
			delta =   MSEC_PER_HOUR * (to.getHour() - from.getHour()) 
					+ MSEC_PER_MINUTE * (to.getMinute() - from.getMinute()) 
					+ 1000 * (to.getSecond() - from.getSecond()) 
					+ (to.getNano() - from.getNano()) / 1000000; 
//			ENGLogger.logger.info("2 Delta from " + from + " to " +  to + " is " + delta + " seconds");
		} else {	// wrap around midnight
			delta = MSEC_PER_DAY - computeMsecUntil(to,from); 
//			ENGLogger.logger.info("3 Delta from " + from + " to " +  to + " is " + delta + " seconds");
		}
		return delta;
	}

	@Override
	public String toString() {
		return "DailySchedule [schedule=" + schedule + "]";
	}

}
