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

import java.util.regex.Pattern;

import org.eng.ENGLogger;

public class Validator {

    protected static final String NON_ALPHANUMERIC_FIELD = "Non alphanumeric value in field: ";

    private static final Pattern INVALID_CHARACTERS_PATTERN = Pattern.compile(
    		// Anything but (^) the characters here are invalid
    		// original [^Aa-zA-Z0-9_ -\\.@/=]
//    		"[^a-zA-Z0-9_\t\r\n -\\.\\!@#\\$%\\^&\\*\\(\\)/\\?=\\:;]");	// ~, `, |, [, ], < and > are missing
    		// % and + are needed for arguments in urlencoded urls containing spaces. as in '..../someurl/arg 1'
    		// Some are need since we pass json.  For example, [] and "
    		"[^a-zA-Z0-9_=+:;@,%/ \t\\-\\.\\!\\$\\(\\)\\?\\\\\\[\\]\\\"]");		// #, ~, `, |, < and > are missing, among others

    protected boolean isANonValidString(String fieldValue) {
        boolean hasSpecialChar = INVALID_CHARACTERS_PATTERN.matcher(fieldValue).find();
        if (hasSpecialChar) 
        	ENGLogger.logger.info("Invalid value:'"+ fieldValue + "'");
        return hasSpecialChar; 
    }
    
//    public static void main(String[] args ) {
//    	String s = "C:\\Users\\DavidWood\\git\\acoustic-analyzer\\IoT-SoundServer\\test-data\\chiller\\chiller-1.wav";
//    	s = "userProfileID";
//    	Validator v = new Validator();
//    	System.out.println("Is invalid " + v.isANonValidString(s));
//    }
}