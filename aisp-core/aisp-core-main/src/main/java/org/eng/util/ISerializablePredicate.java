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
package org.eng.util;

import java.io.Serializable;
import java.util.function.Predicate;

/**
 * A simple interface to make Predicates that are Serializable.
 * Iinitial motivation is for models that use Predicates in their fields and thereby serialized form.
 * 
 */
public interface ISerializablePredicate<T> extends Predicate<T>, Serializable {


}
