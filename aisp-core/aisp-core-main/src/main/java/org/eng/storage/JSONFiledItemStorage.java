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

import org.eng.util.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Provides a limited ability to store Java objects as serialized JSON objects.
 * <p>
 * WARNING: While complex Java classes are supported, currently there is no support for Java objects that use 
 * polymorphism (i.e. interfaces and super classes) in types of their fields. 
 * <p>
 * TODO: This instance is thread-safe but not process safe as no file locking is done in the file system to assure exclusive access.
 *
 * @param <ITEM>
 */
public class JSONFiledItemStorage<ITEM> extends AbstractFiledItemStorage<ITEM> implements IItemStorage<ITEM> {



	transient Gson gson = new GsonBuilder().setPrettyPrinting().create();


	/**
	 * Create the instance to use the given directory to store the items and name the file containing the ids.
	 * @param storageDirectory directory where files associated with this storage instance will be written.
	 * The directory must already exist otherwise an exception will be thrown. 
	 * @param name name associated with the directory and that is used to construct a file in the directory that will contain
	 * the ids of items in the instance.  Since this is used to create a file, it must contain only characters that are
	 * valid in the file system.
	 */
	public JSONFiledItemStorage(String storageDirectory, String name) {
		super(storageDirectory,name);
	}


	/** Unsynchronized directory and JSON file access follows */
	
	final static String JSON_EXTENSION = ".json";

	/**
	 * Get the name of the json file used to store the item for a given id.
	 * @param id
	 * @return never null.
	 */
	private String getJSONFileName(String id) {
		return this.itemsDirectoryPath + "/" + id + JSON_EXTENSION;
	}

	/**
	 * Delete the file containing the json for the item stored under the given id.
	 * @param id
	 */
	protected void deleteItem(String id) throws IOException {
		String fileName = getJSONFileName(id);
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
		String fileName = getJSONFileName(id);
		String json = gson.toJson(item);
		String className = item.getClass().getName();
		String contents = className + "\n" + json;
		FileUtils.writeStringToTextFile(contents, fileName);
	}

	/**
	 * Deserialize the JSON from the file associated with the given id.
	 * @param id
	 * @return
	 * @throws IOException
	 */
	protected ITEM readItem(String id) throws IOException {
		String fileName = getJSONFileName(id);
		File file = new File(fileName);
		if (!file.exists()) 
			throw new IOException("File for id " + id + " does not exist.  Bad id.");
		String contents = FileUtils.readTextFileIntoString(fileName);
		int index = contents.indexOf('\n');
		if (index < 0) 
			throw new IOException("Item format error.  No new line found in json file " + file.getAbsolutePath());
		String className = contents.substring(0, index);
		String json = contents.substring(index+1);
		try {
			Class itemClass = Class.forName(className);
			ITEM item = (ITEM) gson.fromJson(json, itemClass);
			return item;
		} catch (Exception e) {
			throw new IOException("Error reading json file contents from " + file.getAbsolutePath(), e);
		}
	}

}
