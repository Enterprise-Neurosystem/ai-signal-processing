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
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.eng.aisp.monitor.IAsyncDataProvider.IDataHandler;
import org.eng.util.AbstractDefaultIterator;

/**
 * An IDataHandler that simply places the clips in a queue.
 * This is intended to be used by a thread separate from the thread running SoundCapture.
 * @author dawood
 *
 */
public class QueuedDataHandler<DATA extends Object> extends AbstractDefaultIterator<DATA> implements IDataHandler<DATA>, Iterable<DATA>, Iterator<DATA> {
	
	ConcurrentLinkedDeque<DATA> queue = new ConcurrentLinkedDeque<DATA>();
	protected final int maxQueueLength;
	Object lock = new Object();
	
	public QueuedDataHandler(int maxQueueLength) {
		this.maxQueueLength = maxQueueLength;
	}

	@Override
	public void newDataProvided(DATA clip) throws Exception {
		synchronized(lock) {
			while (maxQueueLength > 0 && queue.size() >= maxQueueLength)
				queue.removeFirst();
			queue.add(clip);
			lock.notifyAll();
		}
	}
	private DATA nextData = null;

	@Override
	public boolean hasNext() {
		if (nextData == null)
			nextData = queue.pollFirst();
		return nextData != null; 
	}
	@Override
	public DATA next() {
		if (!hasNext())
			throw new NoSuchElementException();
		DATA r = nextData;
		nextData = null;
		return r;
	}
	
	/**
	 * Wait for the given amount of time for a clip.
	 * @param timeoutMsec if 0 then wait indefinitely.
	 * @return null if timeoutMsec is greater than 0 and no clip was found, always non-null when timeoutMsec is 0.
	 * @throws InterruptedException 
	 */
	public DATA next(int timeoutMsec) throws InterruptedException {
		if (hasNext())
			return next();
		
		while (true) {
			DATA clip;
			synchronized(lock) {
				try {
//					AISPLogger.logger.info("waiting: qsize=" + queue.size());
					lock.wait(timeoutMsec);
				} catch (InterruptedException e) {
					throw e;
				}
				clip = queue.pollFirst();
			}
			if (clip != null || timeoutMsec != 0) {
				return clip; 
			}
		}
	}

	@Override
	public Iterator<DATA> iterator() {
		return this;
	}

	public void clear() {
		this.queue.clear();
	}

}
