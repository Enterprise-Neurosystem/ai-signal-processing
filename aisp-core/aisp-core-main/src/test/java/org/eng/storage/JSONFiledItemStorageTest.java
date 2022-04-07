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
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

public class JSONFiledItemStorageTest extends AbstractItemStorageTest<Map> {

	final static File STORAGE_DIR_FILE = new File("junit-json-storage");
	
    @BeforeClass
    public static void setup() {
    	STORAGE_DIR_FILE.mkdir();
    }
 
    @AfterClass
    public static void tearDown() {
    	STORAGE_DIR_FILE.delete();
    	Assert.assertTrue(!STORAGE_DIR_FILE.exists());
    }	

	@Override
	public IItemStorage<Map> getStorage() {
		return new JSONFiledItemStorage<Map>(STORAGE_DIR_FILE.getPath(), "map-test");
	}

	@Override
	public Map getItemToStore(int index) throws IOException, StorageException {
		Map map = new HashMap();
		map.put(String.valueOf(index), (double)index);	// Convert to double, because Gson deserializes it as a double and not integer.
		return map;
	}
	
}
