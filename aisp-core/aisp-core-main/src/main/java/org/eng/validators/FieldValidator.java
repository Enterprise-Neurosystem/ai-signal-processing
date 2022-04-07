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

import java.util.List;
import java.util.Map;

import org.eng.storage.FieldValues;

/**
 * Simple alphanumeric validator to validate simple fields, not POJOs.
 * For POJOs use @{@link AnnotationValidator}
 *
 * Uses the same regex pattern as @{@link org.eng.validators.annotation.Alphanumeric}
 *
 */
public class FieldValidator extends Validator{
    public void validate(FieldValues fieldValues) {
        this.validate((Map) fieldValues);
    }

    public void validate(Map map) {
        if (map != null && !map.isEmpty()) {
            for (Object key : map.keySet()) {
                if (key instanceof String && isANonValidString((String) key)) {
                    throw new RuntimeException(NON_ALPHANUMERIC_FIELD + key);
                }
            }
            for (Object value : map.values()) {
                if (value instanceof Map) {
                    validate((Map)value);
                } else if (value instanceof String && isANonValidString((String) value)) {
                    throw new RuntimeException(NON_ALPHANUMERIC_FIELD + value);
                }
            }
        }
    }

    public void validate(List<String> list) {
        if (list != null && list.isEmpty()) {
            for (String item : list) {
                if (isANonValidString(item)) {
                    throw new RuntimeException(NON_ALPHANUMERIC_FIELD + item);
                }
            }
        }
    }

    public void validate(String fieldValue) {
        if (fieldValue != null && isANonValidString(fieldValue)) {
            throw new RuntimeException(NON_ALPHANUMERIC_FIELD + fieldValue);
        }
    }

}
