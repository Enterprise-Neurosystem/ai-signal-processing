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

/**
 * Extends the super class to indicate if the mutator is "unary".
 * Unary means that the for each INPUT object it creates 0 or 1 OUTPUT objects.
 * Used by the MutatingShuffleIterable.
 * @author DavidWood
 *
 * @param <INPUT>
 * @param <OUTPUT>
 */
public interface IShuffleMutator<INPUT,OUTPUT> extends IMutator<INPUT, OUTPUT> {
	/**
	 * Return true if every INPUT always produces 1 or 0 OUPUT, for each input, otherwise false.
	 * @return
	 */
	public abstract boolean isUnaryMutator();

}
