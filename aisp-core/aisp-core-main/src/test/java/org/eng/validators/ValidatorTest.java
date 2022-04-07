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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eng.validators.entities.HashMapObj;
import org.eng.validators.entities.InnerObjects;
import org.eng.validators.entities.PropertiesObj;
import org.eng.validators.entities.PropertiesObject;
import org.eng.validators.entities.TestEntity;
import org.eng.validators.entities.TestEntityChild;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ValidatorTest {

    private EntityValidator validator;

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Before
    public void setUp() {
        validator = new AnnotationValidator();
    }

    @Test
    public void testNameFieldIsValidated() {
        expected.expect(RuntimeException.class);
//        expected.expectMessage("Non alphanumeric field: name");
        TestEntity testEntity = getObjectToTest();
        validator.validate(testEntity);
    }

    @Test
    public void testNameFieldIsValidatedAndNoErrorsFound() {
        TestEntity testEntity = getObjectToTest();
        testEntity.setName("validname");
        validator.validate(testEntity);
    }

    @Test
    public void testWithChildsIsWorking() {
        TestEntityChild testEntity = getTestEntityChild();
        expected.expect(RuntimeException.class);
//        expected.expectMessage("Non alphanumeric field: name");
        validator.validate(testEntity);
    }

    @Test
    public void testThatElementsInListAreValidatedAsWell() {
        TestEntityChild testEntity = getTestEntityChild();
        testEntity.setName("aaaa");
        testEntity.setAnything("aaaaad");
        testEntity.setListOfStrings(Arrays.asList("asss", "33434", "<script>"));
        expected.expect(RuntimeException.class);
//        expected.expectMessage("Non alphanumeric field: listOfStrings");
        validator.validate(testEntity);
    }

    @Test
    public void testThatElementsInListAreValidatedAsWell2() {
        TestEntityChild testEntity = getTestEntityChild();
        testEntity.setName("aaaa");
        testEntity.setAnything("aaaaad");
        testEntity.setListOfStrings(Arrays.asList("asss", "33434"));
        validator.validate(testEntity);
    }

    @Test
    public void testThatMinusIsAValidChar() {
        TestEntityChild testEntity = getTestEntityChild();
        testEntity.setAnything("aaaaad");
        testEntity.setName("aa-aa");
        validator.validate(testEntity);
    }

    @Test
    public void testThatNullFieldsDoesntThrowError() {
        TestEntityChild testEntity = getTestEntityChild();
        testEntity.setName("aaaa");
        testEntity.setAnything("aaaaad");
        validator.validate(testEntity);
    }

    @Test
    public void testThatListOfInnerObjectsAreValidatedToo() {
        TestEntityChild testEntity = getTestEntityChild();
        InnerObjects inner1 = new InnerObjects();
        inner1.setNonValidatable("<efdf>");
        inner1.setNameInnerObject("goodvalue");
        inner1.setSomeListField(Arrays.asList("goodval1", "bad<val2"));
        InnerObjects inner2 = new InnerObjects();
        inner2.setNameInnerObject("<badvalue.");
        testEntity.setInnerObjects(Arrays.asList(inner1, inner2));
        expected.expect(RuntimeException.class);
//        expected.expectMessage("Non alphanumeric field: someListField");
        validator.validate(testEntity);
    }

    @Test
    public void testThatListOfInnerObjectsAreValidatedToo2() {
        TestEntityChild testEntity = getTestEntityChild();
        InnerObjects inner1 = new InnerObjects();
        inner1.setNonValidatable("<efdf>");
        inner1.setNameInnerObject("goodvalue");
        inner1.setSomeListField(Arrays.asList("goodval1", "goodval2"));
        InnerObjects inner2 = new InnerObjects();
        inner2.setNameInnerObject("<badvalue.");
        testEntity.setInnerObjects(Arrays.asList(inner1, inner2));
        expected.expect(RuntimeException.class);
//        expected.expectMessage("Non alphanumeric field: nameInnerObject");
        validator.validate(testEntity);
    }

    @Test
    public void testThatInnerObjectsIsValidatedToo() {
        TestEntityChild testEntity = getTestEntityChild();
        InnerObjects inner1 = new InnerObjects();
        inner1.setNonValidatable("<efdf>");
        inner1.setNameInnerObject("goodvalue>.");
        testEntity.setInnerObject(inner1);
        expected.expect(RuntimeException.class);
//        expected.expectMessage("Non alphanumeric field: nameInnerObject");
        validator.validate(testEntity);
    }

    @Test
    public void testThatPropertiesAreValidated() {
        PropertiesObject propertiesObject = new PropertiesObject();
        Properties properties = new Properties();
        properties.setProperty("validKey", "<nonvalidobject>");
        propertiesObject.setProperties(properties);
        expected.expect(RuntimeException.class);
//        expected.expectMessage("Non alphanumeric field: properties");
        validator.validate(propertiesObject);
    }

    @Test
    public void testThatNonValidPropertiesKeyIsValidated() {
        PropertiesObject propertiesObject = new PropertiesObject();
        Properties properties = new Properties();
        properties.setProperty("validKey", "<nonvalidobject>");
        propertiesObject.setProperties(properties);
        expected.expect(RuntimeException.class);
//        expected.expectMessage("Non alphanumeric field: properties");
        validator.validate(propertiesObject);
    }

    @Test
    public void testThatInnerInChildPropertiesObjectIsValidated() {
        PropertiesObj obj = new PropertiesObj();
        Properties properties = new Properties();
        properties.setProperty("validKey", "<nonvalidobject>");
        obj.setProperties(properties);
        expected.expect(RuntimeException.class);
//        expected.expectMessage("Non alphanumeric field: properties");
        validator.validate(obj);
    }

    @Test
    public void testThatHashMapFieldIsValidated() {
        HashMapObj obj = new HashMapObj();
        Map<String, String> map = new HashMap<>();
        map.put("<wrongkey>", "goodvalue");
        obj.setTags(map);
        expected.expect(RuntimeException.class);
//        expected.expectMessage("Non alphanumeric field: tags");
        validator.validate(obj);
    }

    @Test
    public void testEqualIsValid() {
        TestEntity testEntity = new TestEntity();
        testEntity.setName("normal-device-index=0");
        validator.validate(testEntity);
    }

    @Test
    public void testThatInnerInChildHashMapFieldIsValidated() {
        HashMapObj obj = new HashMapObj();
        Map<String, Object> map = new HashMap<>();
        map.put("<wrongkey>", new Properties());
        obj.setTags1(map);
        expected.expect(RuntimeException.class);
//        expected.expectMessage("Non alphanumeric field: tags1");
        validator.validate(obj);
    }

    @Test
    public void testThatSpacesAreTreatedAsValidCharacter() {
        PropertiesObj obj = new PropertiesObj();
        Properties properties = new Properties();
        properties.setProperty("valid Key ", "validfield");
        obj.setProperties(properties);
        validator.validate(obj);
    }

    private TestEntityChild getTestEntityChild() {
        TestEntityChild testEntity = new TestEntityChild();
        testEntity.setName("<fdf4");
        testEntity.setAnything("<<<ff");
        testEntity.setPrimitive(3);
        testEntity.setWrapperObject(55L);
        testEntity.setSomeString("validstring");
        return testEntity;
    }

    private TestEntity getObjectToTest() {
        TestEntity testEntity = new TestEntity();
        testEntity.setName("<fdf4");
        testEntity.setAnything("<<<ff");
        testEntity.setPrimitive(3);
        testEntity.setWrapperObject(55L);
        return testEntity;
    }
}
