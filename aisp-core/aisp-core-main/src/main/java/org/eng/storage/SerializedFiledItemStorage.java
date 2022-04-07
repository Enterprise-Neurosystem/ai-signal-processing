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
import java.io.Serializable;

import org.eng.util.ClassUtilities;
import org.eng.util.FileUtils;

/**
 * Provides a limited ability to store Java objects as java serialization.
>
 * TODO: This instance is thread-safe but not process safe as no file locking is done in the file system to assure exclusive access.
 *
 * @param <ITEM>
 */
public class SerializedFiledItemStorage<ITEM extends Serializable> extends AbstractFiledItemStorage<ITEM> implements IItemStorage<ITEM> {

	/**
	 * Create the instance to use the given directory to store the items and name the file containing the ids.
	 * @param storageDirectory directory where files associated with this storage instance will be written.
	 * The directory must already exist otherwise an exception will be thrown. 
	 * @param name name associated with the directory and that is used to construct a file in the directory that will contain
	 * the ids of items in the instance.  Since this is used to create a file, it must contain only characters that are
	 * valid in the file system.
	 */
	public SerializedFiledItemStorage(String storageDirectory, String name) {
		super(storageDirectory,name);
	}


	/** Unsynchronized directory and JSON file access follows */
	
	final static String FILE_EXTENSION = ".ser";

	/**
	 * Get the name of the json file used to store the item for a given id.
	 * @param id
	 * @return never null.
	 */
	private String getFileName(String id) {
		return this.itemsDirectoryPath + "/" + id + FILE_EXTENSION;
	}

	/**
	 * Delete the file containing the json for the item stored under the given id.
	 * @param id
	 */
	protected void deleteItem(String id) throws IOException {
		String fileName = getFileName(id);
		File file = new File(fileName);
		if (file.exists())
			file.delete();
	}

	/**
	 * Write the item to a json file with a name made from the given id.
	 * @param id
	 * @param item
	 * @throws IOException
	 */
	protected void writeItem(String id, ITEM item) throws IOException {
		String fileName = getFileName(id);
		byte[] bytes = ClassUtilities.serialize(item);
		FileUtils.writeByteArrayToFile(fileName, bytes);
	}

	/**
	 * Deserialize the JSON from the file associated with the given id.
	 * @param id
	 * @return
	 * @throws IOException
	 */
	protected ITEM readItem(String id) throws IOException {
		String fileName = getFileName(id);
		try {
			byte[] bytes = FileUtils.readFileIntoByteArray(fileName);
			ITEM item  = (ITEM)ClassUtilities.deserialize(bytes); 
			return item;
		} catch (Exception e) {
			throw new IOException("Error reading file contents from " + fileName, e);
		}
	}

}
