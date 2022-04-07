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

import java.util.concurrent.Callable;

import org.eng.ENGLogger;

/**
 * Provides the ability to repeatedly call a Callable in another thread.
 * @author dawood
 *
 */
public class RepeatingCallable {
	
	protected final String threadName;
	protected final Callable<Integer> callable;
	protected boolean isStarted = false;
	protected boolean isRunning = false;
	protected Thread runner = null;

	/**
	 * 
	 * @param threadName name to give to the thread. Can be null in which case the simple name of the callable class will be used.
	 * @param callable a callable that is called repeatedly in another thread.  The return value of its call() method
	 * is the number of milliseconds to sleep the thread before calling call() again.
	 */
	public RepeatingCallable(String threadName, Callable<Integer> callable) {
		if (callable == null)
			throw new IllegalArgumentException("Callable may not be null");
		if (threadName == null)
			threadName = callable.getClass().getSimpleName();
		this.threadName = threadName;
		this.callable = callable;
	}

	private class Runner implements Runnable {

		@Override
		public void run() {
			isRunning = true;
			while (isStarted) {
				try {
					int sleepMsec = callable.call();
					if (sleepMsec > 0) 
						Thread.sleep(sleepMsec);
				} catch (InterruptedException e) {
					isStarted = false;
					Thread.currentThread().interrupt();
				} catch (Exception e) {
					ENGLogger.logger.warning(RepeatingCallable.this.threadName + " got exception " + e.getMessage());
					e.printStackTrace();
				}
			}
			isRunning = false;
		}
		
	}
	
	/**
	 * Start the thread that will repeatedly call the Callable in this instance.
	 * @throws Exception 
	 */
	public void start() throws Exception {
		if (isStarted)
			return;
		
		runner = new Thread(new Runner(), threadName);
		runner.setDaemon(true);
		isStarted = true;
		runner.start();
		while (!isRunning)
			Thread.yield();

	}
	
	/**
	 * Stop the thread that is running the Callable from this instance.
	 * The thread is interrupted, but if the {@link Callable#call()} method is not interruptable
	 * this will not return until the {@link Callable#call()} method has returned.
	 * 
	 */
	public void stop() { // throws Exception {
		if (!isStarted)
			return;
		isStarted = false;
		if (runner != Thread.currentThread()) {
			runner.interrupt();
			while (isRunning)
				Thread.yield();
		}
		runner = null;
	}
	
	public boolean isStarted() {
		return  isStarted;
	}

	
}
