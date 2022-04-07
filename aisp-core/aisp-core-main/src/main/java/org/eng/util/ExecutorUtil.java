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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eng.ENGLogger;

public class ExecutorUtil implements RejectedExecutionHandler {


	private static ExecutorService cachedUnprioritizedThreadService = null; 
	private static ExecutorService cachedPrioritizedThreadService = null; 

	/**
	 * Get a service that uses daemon threads at the same priority as the creating thread. 
	 * <br>
	 * WARNING: In general, because this is a shared service, one should not
	 * call shutdown() on the returned service unless you know you're shutting down the JVM or other corner case.
	 * @return
	 */
	public static synchronized ExecutorService getSharedService() {
		return getSharedService(false);
	}

	/**
	 * Get a service that uses daemon threads with a higher priority than the creating thread. 
	 * <br>
	 * WARNING: In general, because this is a shared service, one should not
	 * call shutdown() on the returned service unless you know you're shutting down the JVM or other corner case.
	 * @return
	 */
	public static synchronized ExecutorService getPrioritizingSharedService() {
		return getSharedService(true);
	}

	/**
	 * Get the single global instance of our cached thread executor service that uses our DaemonThreadFactor();
	 * There is no limit to the number of tasks submitted or threads created to service those tasks.
	 * <br>
	 * WARNING: In general, because this is a shared service, one should not
	 * call shutdown() on the returned service unless you know you're shutting down the JVM or other corner case.
	 * @return never null.
	 */
	private static synchronized ExecutorService getSharedService(boolean prioritize) {
		ThreadPoolExecutor pool = null; 
		ExecutorService retVal;
		if (prioritize) {
			if (cachedUnprioritizedThreadService == null) {
				cachedUnprioritizedThreadService = Executors.newCachedThreadPool(new DaemonThreadFactory(prioritize));
				pool = (ThreadPoolExecutor) cachedUnprioritizedThreadService;
			}
			retVal = cachedUnprioritizedThreadService;
		} else {
			if (cachedPrioritizedThreadService == null) {
				cachedPrioritizedThreadService = Executors.newCachedThreadPool(new DaemonThreadFactory(prioritize));
				pool = (ThreadPoolExecutor) cachedPrioritizedThreadService;
			}
			retVal = cachedPrioritizedThreadService;
		}
		if (pool != null) {	// First time creating
			pool.setKeepAliveTime(5, TimeUnit.SECONDS);
			pool.setRejectedExecutionHandler(new ExecutorUtil());
		}
			
//			int cpus = Runtime.getRuntime().availableProcessors();
//	        ThreadPoolExecutor pool = new ThreadPoolExecutor (
//	                /* core size */ (5 * cpus),
//	                /* max size */  (15 * cpus),
//	                /* idle timeout */ 60, TimeUnit.SECONDS,
//	                new SynchronousQueue<Runnable>(),
//	                new DaemonThreadFactory()
//	              );
//	        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
//	        cachedUnprioritizedThreadService = pool;
		 return retVal; 
	}

	/**
	 * A convenience on {@link #newFixedSizeService(int,boolean)} with the max threads set to the number of available processors.
	 * @return
	 */
	public static ExecutorService newService(boolean prioritize) {
		return newFixedSizeService(Runtime.getRuntime().availableProcessors(), prioritize);
	}

	/**
	 * A convenience on {@link #newFixedSizeService(int,boolean)} with prioritize set to false. 
	 * @param maxThreads
	 * @return
	 */
	public static ExecutorService newFixedSizeService(int maxThreads) {
		return newFixedSizeService(maxThreads, false);
	}

	/**
	 * Get a new service that uses a fixed size thread pool and our DaemonThreadFactory.
	 * @param maxThreads
	 * @param prioritize if true, then the threads created will have a higher priority than the thread creating the new thread.
	 * @return
	 */
	public static ExecutorService newFixedSizeService(int maxThreads, boolean prioritize) {
		 return Executors.newFixedThreadPool(maxThreads,new DaemonThreadFactory(prioritize));
	}

	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		ENGLogger.logger.warning("Task rejected: " + r.getClass());
		
	}
}
