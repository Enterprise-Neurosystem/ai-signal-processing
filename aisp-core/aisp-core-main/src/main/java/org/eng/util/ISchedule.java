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

public interface ISchedule {
	/**
	 * Determine if the schedule indicates that the item  under question is scheduled.
	 * @return
	 */
	public boolean isScheduledNow();

	/**
	 * Determine the next time the schedule will change or should be checked.  Optimal implementations
	 * will return the time until the next schedule change.
	 * @return milliseconds to next time schedule should be examined.
	 */
	public int getMsecUntilNextScheduleChange(LocalTime time);
}