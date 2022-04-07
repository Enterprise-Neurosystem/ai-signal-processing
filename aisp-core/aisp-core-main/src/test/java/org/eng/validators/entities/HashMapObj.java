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

import java.util.Map;

import org.eng.validators.annotation.Alphanumeric;

public class HashMapObj {

    @Alphanumeric
    private Map<String,String> tags;

    @Alphanumeric
    private Map<String, Object> tags1;


    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getTags1() {
        return tags1;
    }

    public void setTags1(Map<String, Object> tags1) {
        this.tags1 = tags1;
    }
}
