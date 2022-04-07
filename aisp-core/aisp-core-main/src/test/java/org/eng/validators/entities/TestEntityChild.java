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
package org.eng.validators.entities;


import java.util.List;

import org.eng.validators.annotation.Alphanumeric;

public class TestEntityChild extends TestEntity {

    @Alphanumeric
    private String someString;

    @Alphanumeric
    private List<String> listOfStrings;

    @Alphanumeric
    private List<InnerObjects> innerObjects;

    @Alphanumeric
    private InnerObjects innerObject;

    public String getSomeString() {
        return someString;
    }

    public void setSomeString(String someString) {
        this.someString = someString;
    }

    public List<String> getListOfStrings() {
        return listOfStrings;
    }

    public void setListOfStrings(List<String> listOfStrings) {
        this.listOfStrings = listOfStrings;
    }

    public List<InnerObjects> getInnerObjects() {
        return innerObjects;
    }

    public InnerObjects getInnerObject() {
        return innerObject;
    }

    public void setInnerObject(InnerObjects innerObject) {
        this.innerObject = innerObject;
    }

    public void setInnerObjects(List<InnerObjects> innerObjects) {
        this.innerObjects = innerObjects;
    }
}
