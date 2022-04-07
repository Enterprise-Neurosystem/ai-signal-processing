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
package org.eng.aisp.feature.processor;

import java.io.Serializable;

import org.eng.util.ClassUtilities;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractSerializableTest {

	public AbstractSerializableTest() {
		super();
	}
	
	/**
	 * Get a new instance of the class and always return an equivalent object, but a new instance.
	 * @return never null.
	 */
	protected abstract Serializable newSerializable();
	
	protected Object newInstance() {
		return newSerializable();
	}

	@Test
	public void testSerialize() {
		Serializable fp = newSerializable();
		try {
			ClassUtilities.serialize(fp);
		} catch (Exception e) {
			Assert.fail("Could not serialize instance of " + fp.getClass().getName() + ": "  + e.getMessage());
		}
		
	}

}
