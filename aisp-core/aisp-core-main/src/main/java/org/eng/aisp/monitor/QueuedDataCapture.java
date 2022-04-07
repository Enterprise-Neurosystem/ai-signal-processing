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
package org.eng.aisp.monitor;

import java.util.Iterator;

import org.eng.util.AbstractDefaultIterator;

/**
 * Enabled asynchronous data capture from a sensor that generates data asynchronously.
 * IAsyncDataProvider instance is the sensor producing the data and 
 * Call {@link #start()} to start putting data into the queue.
 * Call {@link #stop()} to stop capture.
 * @author DavidWood
 *
 * @param <DATA>
 */
public class QueuedDataCapture<DATA extends Object> extends AbstractDefaultIterator<DATA> implements Iterable<DATA>, Iterator<DATA> {

	protected final IAsyncDataProvider<DATA> dataProvider;
	private final QueuedDataHandler<DATA> queue;

	public QueuedDataCapture(IAsyncDataProvider<DATA> dataProvider) {
		this(dataProvider,2);
	}

	/**
	 * Used to generate the data. 
	 * @param dataProvider
	 * @param maxQueueLength max data to queue.  Use 0 to have unlimited.
	 */
	public QueuedDataCapture(IAsyncDataProvider<DATA> dataProvider, int maxQueueLength) {
		this.dataProvider = dataProvider;
		this.queue = new QueuedDataHandler<DATA>(maxQueueLength);

	}

	/**
	 * Get the next piece of data from the dataProvider that has been placed in the queue.
	 * {@link #start()} must have been called for this to return non-null.
	 * @param timeoutMsec 0 or larger
	 * @return non-null when timeout is 0, otherwise always null if no data available in the given timeout.
	 * @throws InterruptedException 
	 */
	public DATA next(int timeoutMsec) throws InterruptedException {
		return queue.next(timeoutMsec);
	}

	@Override
	public Iterator<DATA> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return queue.hasNext();
	}

	@Override
	public DATA next() {
		return queue.next();
	}

	/**
	 * Start the dataProvider and begin capturing the data in the queue.
	 * Data is then available via the {@link #next()} and {@link #next(int)} methods.
	 * @throws Exception
	 */
	public void start() throws Exception {
		dataProvider.addListener(queue);
		dataProvider.start();
	}

	/**
	 * Stop the dataProvider and clear the queue.
	 */
	public void stop() {
		dataProvider.removeListener(queue);
		dataProvider.stop();
		queue.clear();
		
	}

}