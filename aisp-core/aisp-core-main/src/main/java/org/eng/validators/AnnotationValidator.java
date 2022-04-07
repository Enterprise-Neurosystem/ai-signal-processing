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

import static org.apache.commons.lang3.reflect.FieldUtils.getAllFieldsList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.eng.validators.annotation.Alphanumeric;

/**
 * Validates POJOs annotated with @{@link Alphanumeric}. E.g.
 *
 * public class JSONGetConfusionMatrixRequest {
 *
 *    @Alphanumeric
 *    public final String labelName;
 *    @Alphanumeric
 *    public final String modelID;
 *    @Alphanumeric
 *    public final List<String> testSoundIDs;
 * }
 *
 * Calling {@link AnnotationValidator#validate(Object)} will validate all the annotated fields. <br>
 *
 * In order to validate POJO instance variables, the instance object needs to be annotated as well as fields
 * inside the object itself e.g.
 *
 * public class JSONGetConfusionMatrixRequest {
 *
 *    @Alphanumeric
 *    public final String labelName;
 *    @Alphanumeric
 *    public final String modelID;
 *    @Alphanumeric
 *    public AnotherRequestObject req;
 *
 * }
 * public class AnotherRequestObject {
 *
 *    @Alphanumeric
 *    private String fieldToValidate;
 * }
 *
 */
public class AnnotationValidator extends Validator implements EntityValidator{

    @Override
    public void validate(Object entity) {
        getAllFieldsList(entity.getClass())
                .stream()
                .peek(field -> field.setAccessible(true))
                .forEach(field -> {
                    Annotation[] annotations = field.getAnnotations();
                    if (annotations.length == 0) return;

                    Object fieldValue = getValue(field, entity);
                    for (Annotation annotation : annotations) {
                        if (fieldValue instanceof Collection) {
                           for (Object fieldVal : (Collection)fieldValue) {
                               doCheck(annotation, field.getName(), fieldVal);
                           }
                        } else {
                            doCheck(annotation, field.getName(), fieldValue);
                        }
                    }
                });
    }

    private void doCheck(Annotation annotation, String name, Object fieldValue) {
        if (annotation instanceof Alphanumeric && fieldValue != null) {
            if (fieldValue instanceof Map) {
                validateMapField(name, (Map) fieldValue);
            } else if (fieldValue instanceof Properties) {
                validatePropertiesField(name, (Properties) fieldValue);
            } else if (fieldValue.getClass() != null && !(fieldValue instanceof String)
                                        && fieldValue.getClass().getDeclaredFields().length > 0) {
                this.validate(fieldValue);

            } else if (fieldValue instanceof String && isANonValidString((String) fieldValue)) {
                throw new RuntimeException(NON_ALPHANUMERIC_FIELD + name);
            }
        }
    }

    private void validateMapField(String fieldName, Map fieldValue) {
        for (Object key : fieldValue.keySet()) {
            if (key instanceof String && isANonValidString((String) key)) {
                throw new RuntimeException(NON_ALPHANUMERIC_FIELD + fieldName);
            }
        }
        for (Object value : fieldValue.values()) {
            if (value instanceof Map) {
                validateMapField(fieldName, (Map) value);
            } else if (value instanceof String && isANonValidString((String) value)) {
                throw new RuntimeException(NON_ALPHANUMERIC_FIELD + fieldName);
            }
        }
    }

    private void validatePropertiesField(String fieldName, Properties properties) {
        for (Object key : properties.keySet()) {
            if (key instanceof String && isANonValidString((String) key)) {
                throw new RuntimeException(NON_ALPHANUMERIC_FIELD + fieldName);
            }
        }
        for (Object value : properties.values()) {
            if (value instanceof String && isANonValidString((String) value)) {
                throw new RuntimeException(NON_ALPHANUMERIC_FIELD + fieldName);
            }
        }
    }

    private Object getValue(Field field, Object entity) {
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
