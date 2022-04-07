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
import java.time.temporal.ChronoUnit;

/**
 * Represents a single start and stop time within a day.
 * Implementation uses integer fields instead of LocalTime instances to better support JSON and REST apis.
 * @author DavidWood
 *
 */
public class TimeRange implements Comparable<TimeRange> {

	protected final int startHour, startMinute, startSecond;
	protected final int endHour,   endMinute,   endSecond;
	
	transient LocalTime start = null;
	transient LocalTime end = null;

	public TimeRange() {
		this(0,0,0,0,0,0);
		this.end = null;
		this.start= null;
	}

	public TimeRange(LocalTime start, LocalTime end) {
		this(start.getHour(), start.getMinute(), start.getSecond(), end.getHour(), end.getMinute(), end.getSecond());
	}

	public TimeRange(int startHour, int startMinute, int startSecond, int endHour, int endMinute, int endSecond) {
		this.startHour = startHour;
		this.startMinute = startMinute;
		this.startSecond = startSecond;
		this.endHour = endHour;
		this.endMinute = endMinute;
		this.endSecond = endSecond;
		initLocalTimes();
		if (this.end.isBefore(start))
			throw new IllegalArgumentException("End time (" + end + ") is before the start time ( " + start + ")");
	}
	
	private void initLocalTimes() {
//		ENGLogger.logger.info("start=" + start + ", end=" + end);
		if (start == null)
			start = LocalTime.of(startHour, startMinute,startSecond);
		if (end == null)
			end = LocalTime.of(endHour, endMinute,endSecond);
	}

	@Override
	public int compareTo(TimeRange that) {
		that.initLocalTimes();
		initLocalTimes();
		return start.compareTo(that.start); 
	}

	private LocalTime zeroNanos(LocalTime time) {
		return LocalTime.of(time.getHour(),  time.getMinute(), time.getSecond());
	}
	/**
	 * Determine if this time range includes the given time.
	 * @param time
	 * @return
	 */
	public boolean contains(LocalTime time) {
		initLocalTimes();
		LocalTime zeroedNanos = time.truncatedTo(ChronoUnit.MILLIS) ;
		zeroedNanos = time; 
		if (start.equals(zeroedNanos))
			return true;
		if (end.equals(zeroedNanos))
			return true;
		return start.isBefore(time) && end.isAfter(time);
	}

	/**
	 * Determine if this range (i.e. start time) falls after the given time. 
	 * @param time
	 * @return
	 */
	public boolean after(LocalTime time) {
		initLocalTimes();
		return start.isAfter(time); 
	}
	public LocalTime getStartTime() {
		initLocalTimes();
		return start;
	}
	public LocalTime getEndTime() {
		initLocalTimes();
		return end;
	}

	@Override
	public String toString() {
		initLocalTimes();
		return "TimeRange [start=" + start + ", end=" + end + "]";
	}
	

}
