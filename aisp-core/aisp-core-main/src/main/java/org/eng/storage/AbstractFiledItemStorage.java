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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eng.util.FileUtils;

/**
 * Provides  the base interface implementation only requires the subclass to provide a few methods.
 * <p>
 * TODO: This instance is thread-safe but not process safe as no file locking is done in the file system to assure exclusive access.
 *
 * @param <ITEM>
 */
public abstract class AbstractFiledItemStorage<ITEM> extends AbstractDeleteableItemStorage<ITEM> implements IItemStorage<ITEM> {

	/** Directory containing all the files for this instance  */
	protected final File storageDirectoryFile;
	/** Path to the directory file containing the list of item ids currently in the instance */
	protected final String directoryPath;
	/** Path to the directory file, as File object */ 
	protected final File directoryFile;
	
	/** Cache of ids on disk */
	private List<String> cachedIDs = null;

	protected final String itemsDirectoryPath;
	protected final File itemsDirectoryFile;
	


	/**
	 * Delete the file containing the json for the item stored under the given id.
	 * @param id
	 */
	protected abstract void deleteItem(String id) throws IOException; 

	/**
	 * Write the item to a json file with a name made from the given id.
	 * @param id
	 * @param item
	 * @throws IOException
	 */
	protected abstract void writeItem(String id, ITEM item) throws IOException;

	/**
	 * Deserialize the JSON from the file associated with the given id.
	 * @param id
	 * @return
	 * @throws IOException
	 */
	protected abstract ITEM readItem(String id) throws IOException; 
	/**
	 * Create the instance to use the given directory to store the items and name the file containing the ids.
	 * @param storageDirectory directory where files associated with this storage instance will be written.
	 * The directory must already exist otherwise an exception will be thrown. 
	 * @param name name associated with the directory and that is used to construct a file in the directory that will contain
	 * the ids of items in the instance.  Since this is used to create a file, it must contain only characters that are
	 * valid in the file system.
	 */
	public AbstractFiledItemStorage(String storageDirectory, String name) {
		this.storageDirectoryFile = new File(storageDirectory);
		if (!storageDirectoryFile.exists())
			throw new IllegalArgumentException("Storage directory " + storageDirectory + " does not exist.");
//		this.storageDirectory = storageDirectoryFile.getAbsolutePath(); 
		this.directoryPath = storageDirectory + "/" + name + ".txt"; 
		this.directoryFile= new File(directoryPath); 
		this.itemsDirectoryPath = storageDirectory + "/" + name + "-items";
		this.itemsDirectoryFile = new File(this.itemsDirectoryPath); 
		itemsDirectoryFile.mkdirs();

		try {
			connect();
		} catch (StorageException e) {
			throw new IllegalArgumentException("Could not create directory " + storageDirectoryFile.getAbsolutePath());
		}
	}

	@Override
	public void connect() throws StorageException { 
		if (!storageDirectoryFile.exists() && !storageDirectoryFile.mkdirs())
			throw new StorageException("Could not create directory " + storageDirectoryFile.getAbsolutePath());
		return; 
	}

	@Override
	public void disconnect() { 
		try {
			if (getCachedIDs().size() == 0)  {
				FileUtils.deleteDirContents(itemsDirectoryFile);
				itemsDirectoryFile.delete();
			}
		} catch (StorageException e) {
			;// ignored
		}
	}

	@Override
	public boolean isConnected() { return storageDirectoryFile.exists(); }

	@Override
	public synchronized List<String> getIDs() throws StorageException {
		List<String> forUser = new ArrayList<String>();
		forUser.addAll(getCachedIDs());
		return forUser;
	}
	
	private transient long dirLastLoadedTime = 0;
	
	/**
	 * Get the ids in the order returned by {@link #getIDs()} (most recent to least recently inserted).
	 * @return
	 * @throws StorageException
	 */
	private synchronized List<String> getCachedIDs() throws StorageException {
		long t = directoryFile.lastModified();
		if (t > dirLastLoadedTime)  {
			dirLastLoadedTime = t;
			cachedIDs = null;
		}
		if (cachedIDs == null) {
			try {
				this.cachedIDs = loadIDs();
			} catch (IOException e) {
				throw new StorageException("Could not load ids.", e);
			}
		}
		return cachedIDs;
	}


	@Override
	public synchronized ITEM findItem(String id) throws StorageException {
		try {
			List<String> ids = getCachedIDs(); 
			if (!ids.contains(id))
				return null;
			return readItem(id);
		} catch (IOException e) {
			throw new StorageException("Could not load item with id " + id, e);
		}
	}

	/**
	 * Check to make sure the given id is one that was previously returned by {@link #add(Object)}. 
	 * @param id
	 * @throws StorageException if id is not valid.
	 */
	private static void validateID(String id) throws StorageException {
		try {
			UUID.fromString(id);
		} catch (IllegalArgumentException e) {
			throw new StorageException("Invalid id " + id);
		}
	}

	@Override
	public synchronized String add(ITEM item) throws StorageException {
		String id = UUID.randomUUID().toString();
		try {
			writeItem(id, item);
			appendID(id);
		} catch (IOException e) {
			throw new StorageException("Could not write item " + item, e);
		}
		return id;
	}



	@Override
	public synchronized boolean update(String id, ITEM item) throws StorageException {
		validateID(id);
		List<String> ids = getCachedIDs(); 
		if (!ids.contains(id))
			throw new StorageException("id not found");
		try {
			writeItem(id,item);
		} catch (IOException e) {
			throw new StorageException("Could not update item with id " + id, e);
		}
		return true;
	}

	@Override
	public synchronized boolean delete(Iterable<String> ids) throws StorageException {
		for (String id : ids) 
			validateID(id);
		
		try {
			if (!deleteIDs(ids))
				return false;
			cachedIDs = null; 
		} catch (IOException e) {
			throw new StorageException("Could not delete one or more items", e);
		}
		for (String id: ids) {
			try {
				deleteItem(id);
			} catch (IOException e) {
				throw new StorageException("Could not delete json file for item with id " + id, e);
			}
		}
		return true;
	}

	
	/**
	 * Append the given id to the directory file and update cachedIDs to include it.
	 * @param id 
	 * @throws StorageException
	 * @throws IOException
	 */
	private void appendID(String id) throws StorageException, IOException {
		List<String> ids = getCachedIDs();  
		ids.add(0,id);	// updates cachedIDs , most recent are first.
		writeIDs(ids);
	}

	/**
	 * Delete all the ids and return true.
	 * @param ids
	 * @return false if any one of the ids is bad or could not be deleted.
	 * @throws IOException
	 * @throws StorageException
	 */
	private boolean deleteIDs(Iterable<String> ids) throws IOException, StorageException {
		List<String> currentIDs = getCachedIDs(); 
		int startSize = currentIDs.size();
		if (ids == currentIDs) {
			currentIDs = new ArrayList<String>();
		} else {
			for (String id : ids)  {
				if (!currentIDs.contains(id))
					return false;
			}
			for (String id : ids) 
				currentIDs.remove(id);	// updates cachedIDs
		}
		if (currentIDs.size() != startSize) {
			if (currentIDs.size() == 0)
				FileUtils.deleteFile(this.directoryFile);
			else
				writeIDs(currentIDs);
		}
		return true;
	}
	
	/**
	 * Overwrite the current file of ids, with the given list of ids.
	 * @param ids
	 * @throws IOException
	 */
	private void writeIDs(List<String> ids) throws IOException {
		StringBuilder sb = null; 
		for (String id : ids) {
			if (sb == null) {
				sb = new StringBuilder();
			} else { 
				sb.append('\n');
			}
			sb.append(id);
		}
		if (sb != null)
			FileUtils.writeStringToTextFile(sb.toString(), this.directoryFile);
		
	}

	/**
	 * Read all the ids out of the ids directory file of ids.
	 * @return 
	 * @throws IOException
	 */
	private List<String> loadIDs() throws IOException {
		if (!this.directoryFile.exists())
			return new ArrayList<String>();
		String contents = FileUtils.readTextFileIntoString(this.directoryFile);
		String ids[] = contents.split("\n");
		List<String> idList = new ArrayList<String>();
		for (String id : ids) {
			id = id.trim();
			idList.add(id);
		}
		return idList; 
	}


}
