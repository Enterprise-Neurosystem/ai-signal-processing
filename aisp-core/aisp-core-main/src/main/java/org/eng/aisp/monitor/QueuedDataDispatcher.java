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

import org.eng.aisp.monitor.IAsyncDataProvider.IDataHandler;
import org.eng.util.AbstractRunnable;
import org.eng.util.DaemonThreadFactory;

/**
 * This adds a queue over the ISyncDataProvider and dispatches the queued data through a handler as it becomes available.
 * One uses this instead of IAsyncDataProvider directly, to allow handling of the data without blocking the IAsyncDataProvider.
 * This allows the IAsyncDataProvider implementation to not have to implement threading itself to deliver the data w/o blocking.
 * <p>
 * Note: only a single IDataHandler is allowed at a time during while this thread is {@link #start(IDataHandler)}
 * @author DavidWood
 *
 * @param <SDATA>
 */
public class QueuedDataDispatcher<SDATA extends Object> {

	protected final IAsyncDataProvider<SDATA> dataProvider;
	private DataDispatcher<SDATA> dataDispatcher;
	private Thread dispatcherThread;
	private IDataHandler<SDATA> handler;
	private int maxQueueLength;

//	public QueuedDataDispatcher(IAsyncDataProvider<SDATA> dataProvider, IDataHandler<SDATA> handler) {
//
//	}

	/**
	 * 
	 * @param dataProvider provider of data, which must be started by the caller after this instance is started.
	 * @param handler
	 * @param maxQueueLength max data to queue.  Use 0 to have unlimited.
	 */
	public QueuedDataDispatcher(IAsyncDataProvider<SDATA> dataProvider, IDataHandler<SDATA> handler, int maxQueueLength) {
		this.dataProvider = dataProvider;
		this.handler = handler;
		this.maxQueueLength = maxQueueLength;
	}

	/**
	 * Provided to allow subclasses to also implement IDataHandler.
	 * @param dataProvider
	 * @param maxQueueLength
	 */
	protected QueuedDataDispatcher(IAsyncDataProvider<SDATA> dataProvider, int maxQueueLength) {
		this(dataProvider, null, maxQueueLength);
		if (!(this instanceof IDataHandler))
			throw new IllegalArgumentException("This constructor is only callable when the instance also implements " + IDataHandler.class.getName());
		this.handler = (IDataHandler)this;
	}

	/**
	 * Extends the super class to deliver queued data through a user-defined handler.
	 * @author DavidWood
	 *
	 */
	public static class DataDispatcher<SDATA> extends AbstractRunnable {

		private final IDataHandler<SDATA> handler;
		private final IAsyncDataProvider<SDATA> sensor;
		private QueuedDataCapture<SDATA> queuedData;
		private int maxQueueLength;
		
		public DataDispatcher(IAsyncDataProvider<SDATA> sensor, IDataHandler<SDATA> handler, int maxQueueLength) {
			this.sensor = sensor;
			this.handler = handler;
			this.maxQueueLength = maxQueueLength;
		}

		@Override
		protected void doRun() throws InterruptedException {
			if (queuedData == null) {
				this.queuedData = new QueuedDataCapture<SDATA>(sensor, maxQueueLength);
				try {
					this.queuedData.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
//			try {
				do {
					SDATA data = this.queuedData.next(1000);
					if (data != null) {
						try {
							this.handler.newDataProvided(data);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} while (queuedData.hasNext());
////			} catch (InterruptedException e) {
////				;
////			}	
		}

		@Override
		public void stop() {
			super.stop();
			if (queuedData != null)
				queuedData.stop();
		} 
		
		
	}

	/**
	 * Start the dataProvider to deliver data through an async queue to the given handler. 
	 * @param handler
	 * @throws Exception
	 */
	public synchronized void start() throws Exception { 
		if (dataDispatcher != null) 
			throw new RuntimeException("Already started");

		this.dataDispatcher = new DataDispatcher<SDATA>(dataProvider,handler, maxQueueLength);
		dispatcherThread = DaemonThreadFactory.newThread("Dispatcher thread", this.dataDispatcher);
		dispatcherThread.start();
		while (!this.dataDispatcher.isRunning())
			Thread.yield();
		this.dataProvider.start();	// We start the provider as a convenience, but the caller needs to stop it.
	}

	/**
	 * Stop data delivery on the handler provided to {@link #start(IDataHandler)}.
	 */
	public synchronized void stop() {
		this.dataProvider.stop();	// TODO: sbhould remove
		if (dispatcherThread == null)
			return;
		dispatcherThread.interrupt();
		dataDispatcher.stop();
		while (this.dataDispatcher.isRunning())
			Thread.yield();
		dataDispatcher = null;
		dispatcherThread = null;

	}

	public boolean isStarted() {
		return  dispatcherThread != null; 
	}
	
}
