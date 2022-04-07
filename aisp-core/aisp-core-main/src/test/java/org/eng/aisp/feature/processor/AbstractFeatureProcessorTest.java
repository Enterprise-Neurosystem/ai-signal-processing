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

import org.eng.aisp.AISPException;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractFeatureProcessorTest extends AbstractSerializableTest {

	public AbstractFeatureProcessorTest() {
		super();
	}

	protected abstract IFeatureProcessor<?> newFeatureProcessor() ;
	
	protected Serializable newSerializable() {
		return newFeatureProcessor();
	}
	@Test
	public void testHashCode() throws AISPException {
		IFeatureProcessor<?> c1 = this.newFeatureProcessor();
		IFeatureProcessor<?> c2 = this.newFeatureProcessor();
		Assert.assertTrue(c1 != c2);
		Assert.assertTrue(c1.hashCode() == c2.hashCode());
	}
	
	@Test
	public void testEquals() throws AISPException {
		IFeatureProcessor<?> c1 = this.newFeatureProcessor();
		IFeatureProcessor<?> c2 = this.newFeatureProcessor();
		Assert.assertTrue(c1 != c2);
		Assert.assertTrue(c1.equals(c2));
	}
}
