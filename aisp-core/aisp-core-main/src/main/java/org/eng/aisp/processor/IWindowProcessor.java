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
package org.eng.aisp.processor;

import org.eng.aisp.IDataWindow;

/**
 * A generic interface that provides an {@link #apply(Object)} method that operations on a IDataWindow to produce a result.
 * @author dawood
 *
 * @param <WINDOW> the specific type of IDataWindow on which {@link #apply(Object)} will be called.
 * @param <RESULT> the output of the {@link #apply(Object)} method.
 */
//public interface IWindowProcessor<WINDOW extends IDataWindow<?>, RESULT extends Object> extends Function<WINDOW, RESULT> { 
public interface IWindowProcessor<WINDOW extends IDataWindow<?>, RESULT extends Object> {  

    RESULT apply(WINDOW t);
}
