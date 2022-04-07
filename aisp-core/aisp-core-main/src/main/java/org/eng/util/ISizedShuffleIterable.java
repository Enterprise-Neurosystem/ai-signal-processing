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
 * An iterable that can report it size and can be shuffled.
 * 
 * @author dawood
 *
 * @param <T>
 */
public interface ISizedShuffleIterable<T> extends IShuffleIterable<T>, ISizedIterable<T> {

	@Override	// To refine return type.
	public ISizedShuffleIterable<T> shuffle();

	@Override	// To refine return type.
	public ISizedShuffleIterable<T> shuffle(long seed);

	@Override	// To refine return type.
	public ISizedShuffleIterable<T> newIterable(Iterable<String> references);

}
