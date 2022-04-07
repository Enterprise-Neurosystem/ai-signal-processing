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
package org.eng.validators;

import java.util.HashMap;
import java.util.Map;

import org.eng.storage.FieldValues;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FieldValidatorTest {

    private FieldValidator validator;

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Before
    public void setUp() {
        validator = new FieldValidator();
    }

    @Test
    public void testFieldValuesAreValidated() {
        FieldValues values = new FieldValues();
        Map<String, String> tags = new HashMap<>();
        tags.put("data-type", "Audio");
        values.put("tags", tags);
        validator.validate(values);
    }

    @Test
    public void testThatFieldIsValidated() {
        String validateString = "{state<script>alert(1)</script>=normal}";
        expected.expect(RuntimeException.class);
        validator.validate(validateString);
    }

    @Test
    public void testAdditionalChars() {
        String stringToValidate = "0_0_1615980973.wav";
        validator.validate(stringToValidate);
    }
}
