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
package org.eng.aisp.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eng.aisp.AISPLogger;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.dataset.MetaData;
import org.eng.storage.AbstractReadOnlyStorage;
import org.eng.storage.FieldValues;
import org.eng.storage.INamedItemStorage;
import org.eng.storage.StorageException;
import org.eng.util.FileUtils;
import org.eng.util.IMutator;
import org.eng.util.MutatingShuffleIterable;

/**
 * Provides file-based storage using unsegmented files in a metadata.csv file.
 * This class is intended to be thread-safe but NOT process-safe.
 * @author DavidWood
 *
 */
public class FileSoundStorage extends AbstractReadOnlyStorage<SoundRecording> implements INamedItemStorage<SoundRecording> {

	protected final File soundDir;
	protected final File metaDataFile;
//	protected final File metaDataDir;
	protected final Object fileLock;

	private static final String MetaDataFileName = MetaData.DEFAULT_METADATA_FILE_NAME; // "metadata.csv";
	protected static final String HIDDEN_NAME_LABEL = "__nAmE__";
//	protected static final String HIDDEN_SESSION_INDEX_LABEL = "__sessionINDEX__";
	private static final String HIDDEN_START_TIME_LABEL = "__STARTtime__";

	private final static Map<File, Object> FileLocks = new HashMap<File, Object>();
	private static synchronized Object getFileLock(File f) {
		Object lock = FileLocks.get(f);
		if (lock == null) {
			lock = new Object();
			FileLocks.put(f, lock);
		}
		return lock;
	}

	public FileSoundStorage(String soundDir) {
		this(new File(soundDir));
	}

	public FileSoundStorage(File soundDir) {
		
		this.soundDir = soundDir;
//		this.metaDataDir = new File(soundDir.getAbsolutePath());
		this.metaDataFile = new File(soundDir.getAbsolutePath() +  "/" + MetaDataFileName);
		fileLock= getFileLock(metaDataFile);

		synchronized(fileLock) {
			if (!soundDir.exists())		
				soundDir.mkdirs();
			else if (!soundDir.isDirectory())
				throw new IllegalArgumentException("File " + soundDir + " is not a directory");
			resetMetaDataCache();
		}
	}

	/** Only to be modified while holding the fileLock */
	private MetaData cachedMetaData = null; 
	
	protected void resetMetaDataCache() {
		synchronized(fileLock) { 
			cachedMetaData = null;
		}
	}

	protected void saveMetaData(MetaData md) throws StorageException {
		synchronized(fileLock) { 
			if (!soundDir.exists())
				soundDir.mkdirs();
			try {
				cachedMetaData = md;
				md.write(metaDataFile.getAbsolutePath());
			} catch (IOException e) {
				throw new StorageException("Could not save file to " + metaDataFile, e);
			}
		}
	}

	/** Only to be modified while holding the fileLock */
	private transient long lastCacheLoad = 0;

	boolean isCacheStale() {
		synchronized(fileLock) {
			if (!metaDataFile.exists())
				return true;
			return this.metaDataFile.lastModified() > lastCacheLoad;
		}
		
	}
	/**
	 * Load the metadata for the store items.
	 * @return never null.
	 * @throws StorageException
	 */
	protected MetaData loadMetaData() throws StorageException {
		synchronized(fileLock) { 
			if (cachedMetaData == null || isCacheStale()) {
				try {
					if (!soundDir.exists())
						soundDir.mkdirs();
					if (!metaDataFile.exists()) {
						cachedMetaData = new MetaData(metaDataFile.getAbsolutePath());
					} else {
						cachedMetaData = MetaData.read(metaDataFile.getAbsolutePath());
						lastCacheLoad = metaDataFile.lastModified(); 
					}
				} catch (IOException e) {
					throw new StorageException("Could not load file at " + metaDataFile, e);
				}
			}
		}
		return cachedMetaData;
	}

	/**
	 * Add labels to new SoundRecording to store important data, name, time, etc.
	 * @param name
	 * @param sound
	 * @return
	 */
	protected SoundRecording annotateSoundRecording(String name, SoundRecording sound) {
		Properties p = new Properties();
		p.putAll(sound.getLabels());
		if (name != null)
			p.put(HIDDEN_NAME_LABEL,name);
		p.put(HIDDEN_START_TIME_LABEL,String.valueOf(sound.getDataWindow().getStartTimeMsec()));
		SoundRecording sr = new SoundRecording(sound.getDataWindow(), p, sound.getTagsAsProperties(), true, null);	// Type tags is already in tags.
		return sr;
	}

	/**
	 * Create a new SoundRecording from the hidden labels on the given SoundRecording.
	 * @param sr contains hidden labels in its labels.  Not modified on return.
	 * @return a new SOundRecording.
	 */
	protected SoundRecording deannotateSoundRecording(final SoundRecording sr) {
		Properties inputLabels = sr.getLabels();
		Properties labels = new Properties();
		labels.putAll(inputLabels);
		String startTimeMsecLabel = labels.getProperty(HIDDEN_START_TIME_LABEL);
		SoundClip clip; 
		if (startTimeMsecLabel != null) {
			double startTimeMsec = Double.parseDouble(startTimeMsecLabel);
			clip = new SoundClip(startTimeMsec, sr.getDataWindow());
		} else {
			clip = sr.getDataWindow();
		}
//		String sessionID = labels.getProperty(HIDDEN_SESSION_ID_LABEL);
//		labels.remove(HIDDEN_SESSION_ID_LABEL);
		labels.remove(HIDDEN_NAME_LABEL);
		labels.remove(HIDDEN_START_TIME_LABEL);
		Properties tags = new Properties(); 
		tags.putAll(sr.getTags());
		tags.remove(MetaData.FILENAME_TAG);	// Don't send this back to the user.

//		if (sessionID != null)	// Should never be the case, but just in case. 
//			tags.put(RECORDING_SESSION_ID_KEY, sessionID);
		SoundRecording nsr = new SoundRecording(clip, labels, tags, true, null);	// Tag includes a recording session identifier and data type
		return nsr;
	}


	
	/**
	 *
	 * @param name
	 * @param sound
	 * @return id that can be used to retrieve and delete sounds.
	 * @throws IOException 
	 */
	public String addNamed(String name, SoundRecording sound) throws StorageException {
		// Store the name and other important data in the labels while stored locally. 
		SoundRecording sr = annotateSoundRecording(name, sound);
		synchronized(fileLock) {
			AISPLogger.logger.fine("Appending sound to local storage");
			String filekey;
			try {
				filekey = MetaData.appendMetaDataSound(soundDir.getAbsolutePath(), sr);
				this.resetMetaDataCache();
			} catch (IOException e) {
				throw new StorageException("Could not append sound", e);
			}
			return filekey;
		}	// sync
	}
	
	@Override
	public SoundRecording findItem(String id) throws StorageException {
		if (!isValidID(id))
			throw new StorageException("Bad id given: '" + id + "'");
		synchronized(fileLock) {
			try {
				MetaData md = loadMetaData();
				Properties labels = md.getLabels(id);
				if (labels == null)
					return null;	// not found, bad id.
				SoundRecording sr = md.readSound(id);
				sr = this.deannotateSoundRecording(sr);
				return sr;
			} catch (IOException e) {
				throw new StorageException("", e);
			}
		}
	}


	@Override
	public String add(SoundRecording item) throws StorageException {
		return addNamed(null, item);
	}

	@Override
	public Iterable<SoundRecording> findItems(FieldValues itemFieldValues) throws StorageException {
		return findNamedItems(null, itemFieldValues);
	}

	@Override
	public boolean update(String id, SoundRecording item) throws StorageException {
		if (!isValidID(id))
			throw new StorageException("Bad id given: '" + id + "'");
		synchronized(fileLock) {
			MetaData md = loadMetaData();
			Properties labels = md.getLabels(id);
			if (labels == null)
				return false;
			String fileName = md.getReferenceableFile(id);
			String name = getItemName(id);
			SoundRecording sr = this.annotateSoundRecording(name, item);
			try {
				sr.getDataWindow().writeWav(fileName);
				md.removeRecord(id);
				md.add(id, sr); 
				saveMetaData(md);
			} catch (IOException e) {
				throw new StorageException("Could not write sound file", e);
			}
		}
		return true;
	}

	private class DeAnnotator implements IMutator<SoundRecording, SoundRecording> {
		@Override
		public List<SoundRecording> mutate(SoundRecording item) {
			List<SoundRecording> srList = new ArrayList<SoundRecording>();
			srList.add(FileSoundStorage.this.deannotateSoundRecording(item)); 
			return srList; 
		}
	}

	/**
	 * This returns an iterable that reads the sounds as they are iterated and defined at the time of this call.  
	 * If while iterating, a sound is removed from the file system, an exception can be thrown during iteration.
	 */
	@Override
	public Iterable<SoundRecording> items() throws StorageException {
		synchronized(fileLock) {
			MetaData md = loadMetaData();
			try {
				return new MutatingShuffleIterable<SoundRecording,SoundRecording>(md.readSounds(), new DeAnnotator(), true);
			} catch (IOException e) {
				throw new StorageException(e.getMessage(), e);
			}
		}
	}

	@Override
	public void connect() throws StorageException {
		
	}

	@Override
	public void disconnect() {
		
	}

	@Override
	public boolean isConnected() {
		return true;
	}

	/**
	 * The ids created by {@link #add(SoundRecording)} are the names of wav files stored in the file system.
	 * @param id
	 * @return
	 */
	protected boolean isValidID(String id) {
		return id.endsWith(".wav");
	}

	@Override
	public boolean delete(Iterable<String> ids) throws StorageException {
		synchronized(fileLock) {
			for (String id : ids) {
				if (!isValidID(id))
					throw new StorageException("bad id given to delete: '" + id + "'");
				MetaData md = loadMetaData();
				if (md.getLabels(id) == null)
					return false; // not found, bad id.
				String fileName = md.getReferenceableFile(id);
				md.removeRecord(id);
				saveMetaData(md);
				FileUtils.deleteFile(fileName);
			}
		}
		return true;
	}
	
	/**
	 * Delete the sound referenced by the given id.
	 * @param id
	 * @return true if found and deleted.
	 * @throws StorageException
	 */
	public boolean delete(String id) throws StorageException {
		List<String> ids = new ArrayList<String>();
		ids.add(id);
		return delete(ids);
	}

	/**
	 * Also returns the ids in the reverse order they were added to storage.
	 */
	@Override
	public Iterable<String> getIDs() throws StorageException {
		List<String> sortedIDs = new ArrayList<String>();
		synchronized(fileLock) {
			MetaData md = loadMetaData();
			for (String f : md.getReferences()) {
				sortedIDs.add(f);
			}
		}
		Collections.reverse(sortedIDs);
		return sortedIDs;
	}

	@Override
	public long count() throws StorageException {
		synchronized(fileLock) {
			MetaData md = loadMetaData();
			return md.size();
		}
	}

	@Override
	public void clear() throws StorageException {
		synchronized(fileLock) {
			MetaData md =  loadMetaData();
			for (String f : md.getFiles()) {
				String ref = md.getReferenceableFile(f);
				FileUtils.deleteFile(ref);
			}
			FileUtils.deleteFile(metaDataFile.getAbsolutePath());
			this.resetMetaDataCache();
		}
	}

	@Override
	public Iterable<String> findIDs(FieldValues itemFieldValues) throws StorageException {
		return findNamedIDs(null, itemFieldValues);
	}


	@Override
	public Iterable<SoundRecording> findNamedItems(String name) throws StorageException {
		return findNamedItems(name, null);
	}

	@Override
	public String getItemName(String id) throws StorageException {
		MetaData md;
		synchronized(fileLock) {
			md = loadMetaData();
			Properties labels = md.getLabels(id);
			if (labels == null)
				return null;
			return labels.getProperty(HIDDEN_NAME_LABEL);
		}
	}

	@Override
	public Iterable<String> findNamedIDs(String name) throws StorageException {
		List<String> ids = new ArrayList<String>();
		synchronized(fileLock) {
			MetaData md = loadMetaData();
			for (String id : md.getReferences()) {
				if (name != null) {
					Properties labels = md.getLabels(id);
					String itemName = labels.getProperty(HIDDEN_NAME_LABEL);
					if (!name.equals(itemName))
						continue;
				}
				ids.add(id);
			}
		}
		Collections.reverse(ids);	// Must return ids in most recent to least recent inserted order.
		return ids;
	}

	private boolean fieldMatch(SoundRecording sr, FieldValues itemFieldValues) {
		if (itemFieldValues == null || itemFieldValues.isEmpty())
			return true;
		boolean matched = true;
		for (String name : itemFieldValues.keySet()) {
			Object value = itemFieldValues.get(name); 
			if (value == null) {
				AISPLogger.logger.warning("Null value given for matching on field " + name + ". Ignoring.");
			} else if (name.equals(SoundRecording.LABELS_FIELD_NAME)) {
				if (value instanceof Properties) {
					Properties labels = (Properties)value;
					matched = labels.equals(sr.getLabels());
				} else {
					throw new IllegalArgumentException("Value for " + name + " field must a Properties object");
				}
			} else if (name.equals(SoundRecording.TAGS_FIELD_NAME)) {
				if (value instanceof Properties) {
					Properties labels = (Properties)value;
					matched = labels.equals(sr.getTagsAsProperties());
				} else {
					throw new IllegalArgumentException("Value for " + name + " field must a Properties object");
				}
			} else {
				AISPLogger.logger.warning("Matching unsupported field name: " + name); 
				matched = false;
			}
			if (!matched)
				break;
		}
		return matched; 
	}

	/**
	 * Supports search on sensor id, labels, and tags field values.
	 */
	@Override
	public Iterable<String> findNamedIDs(String name, FieldValues itemFieldValues) throws StorageException {
		List<String> ids = new ArrayList<String>();
		synchronized(fileLock) {
			for (String id : findNamedIDs(name)) {
				SoundRecording sr = findItem(id);
				if (fieldMatch(sr, itemFieldValues))
					ids.add(id);
			}
		}
		return ids;
	}


	/**
	 * Supports search on sensor id, labels, and tags field values.
	 */
	@Override
	public Iterable<SoundRecording> findNamedItems(String name, FieldValues itemFieldValues) throws StorageException {
		List<SoundRecording> sounds = new ArrayList<SoundRecording>();
		synchronized(fileLock) {
			for (String id : findNamedIDs(name)) {
				SoundRecording sr = findItem(id);
				if (fieldMatch(sr, itemFieldValues))
					sounds.add(sr);
			}
		}
		return sounds;
	}
	
//	protected boolean exists(String id) throws StorageException {
//		MetaData md = loadMetaData();
//		return md.getLabels(id) != null;
//	}

	/**
	 * Get a file that references the underlying sound file for the given id.
	 * <p>
	 * WARNING: This currently returns a reference to the file as it exists in the underlying
	 * storage implementation.  As such, do not a) delete the returned file and/or b) access this instance
	 * while the file opened by the caller.
	 * @param id
	 * @return null if id is not associated with an existing item. 
	 * @throws StorageException 
	 */
	public File getSoundFile(String id) throws StorageException {
		if (id == null)
			throw new IllegalArgumentException("null id given");
		synchronized(this.fileLock) {
			MetaData md = loadMetaData();
			if (md.getLabels(id) == null)
				return null;
			id = md.getReferenceableFile(id);
			return new File(id);
		}
	}
	
	
	/**
	 * Implement to read all items under the ids and return as a  list.
	 */
	@Override
	public List<SoundRecording> findItems(Iterable<String> ids) throws StorageException {
		List<SoundRecording> srList = new ArrayList<SoundRecording>();
		synchronized(fileLock) {
			for (String id : ids) {
				SoundRecording sr = findItem(id);
				if (sr == null)
					throw new StorageException("Item with id" + id + " not found");
				srList.add(sr);
			}
		}
		return srList;
	}


	@Override
	public Iterable<String> findPagedIDs(int pageIndex, int pageSize, FieldValues itemFieldValues) throws StorageException {
		return getPagedIDs(pageIndex, pageSize, findIDs(itemFieldValues));
	}
}
