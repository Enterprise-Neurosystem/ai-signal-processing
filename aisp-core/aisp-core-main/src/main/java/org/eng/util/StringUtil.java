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

import java.util.regex.Pattern;

public class StringUtil {

	static char[] MappingSourceCharacters = 			{  '\n',  '\t',  '\b',  '\r',  '\f' };
	static String[] MappingDestinationCharacters =   { "\\n", "\\t", "\\b", "\\r", "\\f" };
	
	private static String getEscapeString(char src) {
		for (int i=0 ; i<MappingSourceCharacters.length ; i++) {
			if (MappingSourceCharacters[i] == src)
				return MappingDestinationCharacters[i];
		}
		return null;
	}

	/**
	 * Escape all the given characters with the given escape char.
	 * @param target
	 * @param charsToEscape list of single characters to escape.
	 * @param escapeChar
	 * @return
	 */
	public static String escape(String target, String charsToEscape,  char escapeChar) {
		int size = charsToEscape.length();
		for (int i=0 ; i<size ; i++) {
			char c = charsToEscape.charAt(i);
			target = escape(target,c, escapeChar);
		}
		return target;
	}

	/**
	 * Escape all the given characters with the given escape char.
	 * @param target
	 * @param charsToDeescape list of single characters to escape.
	 * @param escapeChar
	 * @return
	 */
	public static String deescape(String target, String charsToDeescape,  char escapeChar) {
		int size = charsToDeescape.length();
		for (int i=0 ; i<size ; i++) {
			char c = charsToDeescape.charAt(i);
			target = deescape(target,c, escapeChar);
		}
		return target;
	}

	/**
	 * Escape the given character using the given escape character and or mapping of.
	 * @param target
	 * @param charToEscape
	 * @param escapeChar
	 * @return
	 */
	public static String escape(String target, char charToEscape,  char escapeChar) {
		StringBuilder sb = new StringBuilder();
		for (int i=0 ; i<target.length(); i++) {
			char c = target.charAt(i);
			if (c == charToEscape) {
				String replaceStr = getEscapeString(charToEscape);
				if (replaceStr != null) {
					sb.append(replaceStr);
				} else {
				    sb.append(escapeChar);
					sb.append(c);
				}
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
		
	}

	/**
	 * Replace all occurrences of the given escape character with "", except
	 * when the character being deescaped has a replacement mapping in which case
	 * we replace the mapping with the character.
	 * @param target string to do replacement on
	 * @param charThatWasEscaped the character that was previously escaped in the given target string.
	 * @param escapeChar the character to remove from the string.
	 * @return never null.
	 */ 
	public static String deescape(String target,  char charThatWasEscaped, char escapeChar) {
		String replacementTarget = getEscapeString(charThatWasEscaped);
		String original = Character.toString(charThatWasEscaped);
		if (replacementTarget == null)  
			replacementTarget = Character.toString(escapeChar) + original;
	    
		original = escape(original,'\\','\\');
		replacementTarget = escape(replacementTarget,'\\','\\');
		String s = target.replaceAll(replacementTarget, original);
		return s;
	}

	/**
	 * Replace the occurrence of the replacmentTarget with the replacment value in the given target string.
	 * @param target
	 * @param replacementTarget string that may have special characters which are quoted before replacement.
	 * @param replacement
	 * @return
	 */
	public static String quotedReplacex(String target,  String replacementTarget, String replacement) {
		String search = Pattern.quote(replacementTarget);
		String copy;
		do {
			copy = target;
			target = target.replaceFirst(search, replacement);
		} while (!target.equals(copy));
		return target;
	}

}
