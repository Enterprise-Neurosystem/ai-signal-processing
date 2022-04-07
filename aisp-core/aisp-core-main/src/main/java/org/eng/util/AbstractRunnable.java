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

/**
 * A Runnable that continually loops over a method and that can be stopped externally. 
 * Sub-classes must implement {@link #doRun()} which is called until {@link #stop()} is called
 * on the instance.
 * @author DavidWood
 *
 */
public abstract class AbstractRunnable implements Runnable {

	protected boolean isRunning = false;
	protected boolean isStarted = true;
	
	public AbstractRunnable() {
	}

	@Override
	public void run() {
		isRunning = true;
		while (isStarted)
			try {
				doRun();
			} catch (InterruptedException e) {
				isStarted = false;
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				e.printStackTrace();
			}
		isRunning = false;
	}

	/**
	 * Called in the while loop of {@link #run()} while isStarted is true.
	 */
	protected abstract void doRun() throws Exception; 
	
	/**
	 * Determine if the runnable is entered the {@link #run()} method.
	 * @return true if entered, false if exited or exiting.
	 */
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * Request the runnable to stop doing its work via {@link #doRun()}.
	 * {@link #isRunning} will return false when it has actually stopped.
	 */
	public void stop() {
		isStarted = false; 
		while (isRunning)
			Thread.yield();
	}

}