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
package org.eng.cache;

import java.util.Arrays;

class CacheUtil {


		
	private static int[] primes = {3, 5, 7,11, 13, 17, 19, 23, 29, 31, 37, 41 };


	public static long hash(Object[] keys) {
			if (keys.length > primes.length) 
				throw new RuntimeException("keys array has more items (" + keys.length + ") than we have primes defined (" + primes.length + ")");
			long hash = 1;
			for (int i=0 ; i< keys.length ; i++)  {
				long code;
				if (keys[i] != null)  {
					Class keyClass = keys[i].getClass();
					if (keyClass.isArray()) {
						if (keyClass.getComponentType().isPrimitive())
							code = hashDoubleArray((double[])keys[i]);
	//						code = Arrays.hashCode((double[])keys[i]);
						else
							code = hash((Object[])keys[i]);
					} else {
						code = keys[i].hashCode();
					}
				} else {
					code = 1;
				}
				hash += code * primes[i];
			}
			
			return hash;
		}

	public static int hashDoubleArray(double[] ds) {
			double hash = 0;
			int multiplier = 1;
			for (int i=0 ; i<ds.length ; i++) {
				hash += multiplier * ds[i];
				multiplier++;
			}
	//		int exp = (int)Math.log10(Math.abs(hash));
	//		hash = (hash * Math.pow(10, 15-exp));	// 15 decimal places, which is max for long 

//			return Double.hashCode(hash); 	// Java 8
			return new Double(hash).hashCode();	// Java 7
		}

	public static boolean equalObjectArrays(Object[] objs1, Object[] objs2) {
		if (objs1 == objs2)
			return true;
		if (objs1.length != objs2.length)
			return false;
		for (int i=0 ; i< objs1.length ; i++)  {
			Object obj1 = objs1[i];
			Object obj2 = objs2[i];
			if (obj1 == null) {
				if (obj2 != null)
					return false;
			} else if (obj2 == null) {
				return false;
			} else {
				Class key1Class = obj1.getClass();
				if (key1Class.isArray()) {
					Class key2Class = obj1.getClass();
					if (!key1Class.equals(key2Class))
						return false;
					if (key1Class.getComponentType().isPrimitive()) {
						if (!Arrays.equals((double[])obj1, (double[])obj2))
							return false;
					} else if (!equalObjectArrays((Object[])obj1, (Object[])obj2)) {
						return false;
					} else {
						return false;
					}
				} else {
					if (!obj1.equals(obj2))
					return false;
				}
			}
		}
		return true;
	}

}
