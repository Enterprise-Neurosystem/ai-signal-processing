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
package org.eng.aisp.classifier;

import java.io.IOException;
import java.io.Serializable;

import org.eng.AbstractObjectTest;
import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.util.ClassUtilities;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractFixedClassifierTest extends AbstractObjectTest {

	/**
	 * Return a new instance of a new untrained classifier.
	 * @return
	 * @throws AISPException
	 */
	protected abstract IFixedClassifier<?> getClassifier() throws AISPException;

	protected IFixedClassifier<?> getTestObject() {
		try {
			return getClassifier();
		} catch (AISPException e) {
			throw new RuntimeException("Could not create classifier: " + e.getMessage(), e);
		}
	}

	@Override
	protected Serializable getSerializableTestObject() throws Exception {
		// TODO: should create a trained classifier and test it.
		return getTestObject();
	}

	@Override
	protected Cloneable getCloneable() {
		return null;	// We don't implement cloneable
	}

	
}