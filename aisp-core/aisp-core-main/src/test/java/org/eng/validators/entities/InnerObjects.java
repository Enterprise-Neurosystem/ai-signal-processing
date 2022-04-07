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

public class InnerObjects {

    @Alphanumeric
    private List<String> someListField;

    @Alphanumeric
    private String nameInnerObject;

    private String nonValidatable;

    public List<String> getSomeListField() {
        return someListField;
    }

    public void setSomeListField(List<String> someListField) {
        this.someListField = someListField;
    }

    public String getNameInnerObject() {
        return nameInnerObject;
    }

    public void setNameInnerObject(String nameInnerObject) {
        this.nameInnerObject = nameInnerObject;
    }

    public String getNonValidatable() {
        return nonValidatable;
    }

    public void setNonValidatable(String nonValidatable) {
        this.nonValidatable = nonValidatable;
    }
}
