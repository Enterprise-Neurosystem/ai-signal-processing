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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DaemonThreadFactory implements ThreadFactory {

	private final int priority;

	public DaemonThreadFactory(boolean prioritize) {
		if (prioritize)
			this.priority =  Thread.currentThread().getPriority() + 1;
		else
			this.priority =  -1; 	
	}

	private static class DaemonThread extends Thread {
		private static AtomicInteger counter = new AtomicInteger();

		public DaemonThread(Runnable r, String name) {
			super(r,name);
//			int count = counter.incrementAndGet();
//			ENGLogger.logger.info("Creating active thread " + name + " : " + count);
		}
		
		@Override
		protected void finalize() throws Throwable {
//			int count = counter.decrementAndGet();
//			ENGLogger.logger.info("Deleting active thread " + this.getName() + " : " + count);
			super.finalize();
		}
	}

	private AtomicInteger counter = new AtomicInteger();

	public Thread newThread(Runnable r) {
//		StackTraceElement trace[] = Thread.currentThread().getStackTrace();
		int index = counter.incrementAndGet();
		String name = "Thread " + index + " created by thread " + Thread.currentThread().getName(); //  + " in " + trace[2].getClassName() + "." + trace[2].getMethodName();
//	    Thread t = new DaemonThread(r,name);
//	    t.setDaemon(true);
	    Thread t = newThread(name,r);
	    if (priority >= 0) 
	    	t.setPriority(priority);
	    return t;
	}

	/**
	 * Create an unstarted daemon thread with the given name.
	 * @param name
	 * @param r
	 * @return
	 */
	public static Thread newThread(String name, Runnable r) {
	    Thread t = new DaemonThread(r,name);
	    t.setDaemon(true);
	    return t;
	}
	  
}
