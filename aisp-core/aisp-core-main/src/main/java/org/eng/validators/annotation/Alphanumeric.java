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
package org.eng.validators.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The name is a little bit misleading, because it supports alphanumeric
 * and also _ -.@/ <br>
 * _ and . are required for captured sound file name validation. <br>
 * @ for username validation for which sound was captured <br>
 *
 * Annotating POJOs allows for @{@link org.eng.validators.AnnotationValidator}
 * to validate extended objects and instance variables.
 * To validate non POJO use @{@link org.eng.validators.FieldValidator}
 *
 */
@Retention(RUNTIME)
@Target({FIELD, PARAMETER})
public @interface Alphanumeric {
}
