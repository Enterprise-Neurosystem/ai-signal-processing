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
package org.eng.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.eng.util.ComponentLogger;
import org.eng.util.ExecutorUtil;
import org.eng.util.ISizedIterable;
import org.eng.util.IterableIterable;

class StorageUtil {
	
	public final static boolean ParallelLoadEnabled = Boolean.valueOf(System.getProperty("storage.parallelLoad.enabled", "true"));

	private static class LoadItemTask<ITEM> implements Callable<Object> {

		private IReadOnlyStorage<ITEM> storage;
		private Iterator<String> ids;
		private Map<String, ITEM> id2Items;

		public LoadItemTask(IReadOnlyStorage<ITEM> storage, Iterator<String> idIter, Map<String, ITEM> id2Items) {
			this.storage = storage;
			this.ids = idIter;
			this.id2Items = id2Items;
		}

		@Override
		public Object call() throws Exception {
			while (ids.hasNext()) {
				String id = null;
				synchronized(ids) {
					if (ids.hasNext())
						id = ids.next();
				}
				if (id != null) {
					ITEM item = storage.findItem(id);
					synchronized(id2Items) {
						id2Items.put(id, item);
					}
				}
			}
			return null;
		}
		
	}
	
	
	/**
	 * Load the items for the given ids from the given repositories in batches.
	 * We try not to load all items.
	 * @param <ITEM>
	 * @param storage
	 * @param ids
	 * @param batchSize
	 * @return
	 * @throws StorageException
	 */
	public static <ITEM> Iterable<ITEM> batchLoad(IReadOnlyStorage<ITEM> storage, Iterable<String> ids, ComponentLogger logger) throws StorageException {
		int perProcessorItems = 25;
		int nThreads = Runtime.getRuntime().availableProcessors();
		return batchLoad(storage,ids, nThreads * perProcessorItems, logger);

	}
	private static boolean warned = false;
	
	public static <ITEM> Iterable<ITEM> batchLoad(IReadOnlyStorage<ITEM> storage, Iterable<String> ids, int batchSize, ComponentLogger logger) throws StorageException {
		if (!ParallelLoadEnabled) {
			if (!warned) {
				if (logger != null)
					logger.warning("Parallel loading is disabled, but batchLoad() was called.  Providing serial implementation.");
				warned = true;
			}
			return new StoredItemByIDIterable<ITEM>(storage,ids, 1);
		}
			
		int minParallelLoadsPerThread = 2; 
		Iterable<ITEM> items;
		int nThreads = Runtime.getRuntime().availableProcessors(); 
		int minParallelItems = nThreads * minParallelLoadsPerThread; 
		int itemCount = getCount(ids);
		// itemCount                  Load type
		// 0				   -+
		// |					| Single-threaded
		// minParallelItems	   -+
		// |					|
		// |					| Multi-threaded threaded
		// |				 	|
		// batchSize		   -+ 
		// |				 	|
		// |				 	| Multiple iterables of size=batchSize	
		if (itemCount == 0) {
			items = new ArrayList<ITEM>(); 
		} else if (itemCount < minParallelItems)  {	// Don't bother going parallel
			items = new StoredItemByIDIterable<ITEM>(storage,ids, 1);
		} else if (itemCount <= batchSize) {	// Create iterables, each of which will come back here 
			items = loadInParallel(storage, ids, nThreads);
		} else  {	// Create iterables, each of which will come back here 
			// NOTE: to get true speed up, this relies on the StoredItemByIDIterable.dereference(List<String> ids)  to 
			// call storage.findItems() and the implementation of findItems() to call this method.
			items = batchedIterable(storage, ids, itemCount, batchSize);
		}
		
		return items;

	}

	/**
	 * @param <ITEM>
	 * @param storage
	 * @param ids
	 * @param nThreads
	 * @return
	 * @throws StorageException
	 */
	protected static <ITEM> List<ITEM> loadInParallel(IReadOnlyStorage<ITEM> storage, Iterable<String> ids, int nThreads) throws StorageException {
		ExecutorService service = ExecutorUtil.getPrioritizingSharedService();
		Iterator<String> idIter = ids.iterator();
		Map<String, ITEM> id2Items = new HashMap<String, ITEM>();
		List<Future<?>> futureList = new ArrayList<Future<?>>();
		
		// Create tasks to load items referenced by the ids placing them into the map of ids to items.
		for (int i=0 ; i<nThreads ; i++) {
			Callable<Object> task = new LoadItemTask<ITEM>(storage, idIter, id2Items);
			Future<Object> f = service.submit(task);
			futureList.add(f);
		}
		// Wait for completion of the tasks  (all items to be loaded for each id)
		for (Future<?> f : futureList) {
			try {
				f.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new StorageException(e.getLocalizedMessage(), e.getCause());
			} catch (ExecutionException e) {
				throw new StorageException(e.getLocalizedMessage(), e.getCause());
			}
		}

		// Extract the items from the map putting them in the same order as the original id list.
		List<ITEM> itemList = new ArrayList<ITEM>();
		for (String id: ids) {
			ITEM item = id2Items.get(id);
			itemList.add(item);
		}
		return itemList;
	}

	private static <ITEM> Iterable<ITEM> batchedIterable(IReadOnlyStorage<ITEM> storage, Iterable<String> ids, int itemCount, int batchSize) {
		List<Iterable<ITEM>> batchedItems = new ArrayList<Iterable<ITEM>>();
		List<String> currentIDBatch = new ArrayList<String>();
		// Go through the ids and build into batches of iterables.
		for (String id : ids) {
			currentIDBatch.add(id);
			if (currentIDBatch.size() == batchSize) {
				Iterable<ITEM> batched = new StoredItemByIDIterable<ITEM>(storage, currentIDBatch, batchSize);
				batchedItems.add(batched);
				currentIDBatch = new ArrayList<String>();
			}
		}

		if (currentIDBatch.size() > 0) {
			Iterable<ITEM> batched = new StoredItemByIDIterable<ITEM>(storage, currentIDBatch, currentIDBatch.size());
			batchedItems.add(batched);
		}
			
		return new IterableIterable<ITEM>(batchedItems); 
	}

	private static int getCount(Iterable<String> ids) {
		if (ids instanceof Collection)
			return ((Collection)ids).size();
		else if (ids instanceof ISizedIterable)
			return ((ISizedIterable)ids).size();
		int count = 0;
		for (String s : ids)
			count++;
		return count;
	}

}
