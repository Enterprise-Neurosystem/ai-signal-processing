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



import org.eng.aisp.AISPTestSuite;
import org.eng.aisp.SoundTestUtils;
import org.eng.storage.ENGStorageTestSuite;
import org.eng.util.ENGUtilTestSuite;
import org.eng.validators.ENGValidatorTestSuite;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Tests for both the src/main and src/client source.
 * @author dawood
 * Each package under this one should declare its own test suite and list it below.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({

	ENGValidatorTestSuite.class,
	ENGUtilTestSuite.class,
	ENGStorageTestSuite.class,
	AISPTestSuite.class
        })
public class MainTestSuite {
	@BeforeClass
	public static void beforeClass() {
		System.setProperty(SoundTestUtils.EXTENDED_RUNTIME_TESTING_PROPERTY_NAME, "true");
	}
	
}
