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
package org.eng; 

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLogger;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.segmented.SegmentedSoundRecording;
import org.eng.util.ClassUtilities;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;


/**
 * A general purpose test class that supports optional testing of clonability and serialization.
 */

public abstract class AbstractObjectTest {

	/**
	 * Get a new instance used to test equals() and hashCode().
	 * All calls should return a new instance that is equal to all others and has the same hash code.
	 * @return never null
	 */
	protected abstract Object getTestObject();

	/**
	 * Get the serializable object to test, null if not actually serializable.
	 * @return
	 * @throws Exception 
	 */
	protected abstract Serializable getSerializableTestObject() throws Exception; 

	/**
	 * May be overridden by sub-classes to provide additional validity tests on the deserialized object beyond simple ser.equals(deserialized) test.
	 * @param ser	 the original object return by {@link #getSerializableTestObject()}.
	 * @param deserialized the object deserialized from the file system.
	 * @throws Exception 
	 */
	protected void verifySerializableEquivalence(Serializable ser, Serializable deserialized) throws Exception { }
	
	/**
	 * Get the cloneable object to test, null if not actually clonable.
	 * @return
	 */
	protected abstract Cloneable getCloneable(); 

	@Test
	public void testHashCode() throws AISPException {
		Object c1 = this.getTestObject();
		Object c2 = this.getTestObject();
		Assert.assertTrue(c1 != c2);
		Assert.assertTrue(c1.hashCode() == c2.hashCode());
	}



	@Test
	public void testEquals() throws AISPException {
		Object c1 = this.getTestObject();
		Object c2 = this.getTestObject();
		Assert.assertTrue(c1 != c2);
		Assert.assertTrue(c1.equals(c2));
	}

	/**
	 * Verify that previously stored serializations work, and regenerate them if they have been removed.
	 * @throws Exception 
	 */
	@Test
	public void testPastSerialization() throws Exception {

		Serializable ser = getSerializableTestObject();
		Assume.assumeTrue("Skipping serialization test", ser!= null);
		String baseName = this.getClass().getSimpleName();
		// Check for past serialization
		List<Serializable> slist = ENGTestUtils.verifyPastSerializations(ENGTestUtils.SerializationsDir, baseName, ser, false);
		if (slist.size() == 0) {
			// No past serialization, regenerate them.
			AISPLogger.logger.info("Regenerating serialization of " + ser.getClass().getSimpleName());
			ENGTestUtils.generateSerialization(ENGTestUtils.SerializationsDir, baseName, ser);
		}
		// Now verify the past serialization.
		slist = ENGTestUtils.verifyPastSerializations(ENGTestUtils.SerializationsDir, baseName, ser, true);
		Assert.assertTrue("Expected only 1 object", slist.size() == 1);
		Serializable deserialized = (Serializable)slist.get(0); 
		Assert.assertTrue(ser.equals(deserialized));
		verifySerializableEquivalence(ser, deserialized);
	}
	


	@Test
	public void testSerializationEquality() throws Exception {
		Serializable obj = getSerializableTestObject();
		Assume.assumeTrue("Skipping serialization test", obj != null);
		try {
			byte[] bytes = ClassUtilities.serialize(obj); 
			Serializable deserialized = ClassUtilities.deserialize(bytes);
			assertTrue("Got some thing", deserialized != null); 
			Assert.assertTrue(obj.equals(deserialized));
			verifySerializableEquivalence(obj, deserialized);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Could not serialize");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			fail("Could not serialize");
		}
	}

	@Test
	public void testCloneAndEquals() throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Cloneable obj = getCloneable();
		Assume.assumeTrue("Skipping cloneable test", obj != null);
		Method clone = obj.getClass().getMethod("clone");
		Assert.assertTrue(clone != null);
		Object o = clone.invoke(obj);
		Assert.assertTrue(o != null);
		Assert.assertTrue(o.equals(obj));
	}

}