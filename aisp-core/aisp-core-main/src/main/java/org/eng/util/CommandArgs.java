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

import java.util.ArrayList;

/**
 * @author dawood
 *
 */
public class CommandArgs {

//	int nextReferenceArgIndex = 0
	ArrayList<String> args;

	public CommandArgs(String[] args) {
		this.args = new ArrayList<String>();
		if (args != null && args.length > 0) {
			for (int i=0 ; i<args.length ; i++)
				this.args.add(args[i]);
		}
	}

	/**
	 * Get the current set of args.
	 * This is useful when args are removed as they are processed.
	 * @return never null.
	 */
	public String[] getArgs() {
		int size = args == null ? 0 : args.size();
		String[] t = new String[size];
		if (size > 0)
			args.toArray(t);
		return t;
	}
	
	/**
	 * Get the 0-based indexed argument.
	 * @param index
	 * @return
	 */
	public String peekArg(int index) {
		if (index >= args.size() || index < 0)
			return null;
		return args.get(index);
	}
	
	public String removeArg(int index) {
		if (index >= args.size() || index < 0)
			return null;
		String value = args.get(index);
		args.remove(index);
		return value;
	}
	
	/**
	 * Get the current list of available (unconsumed) arguments.
	 * @return
	 */
	public int size() {
		return this.args.size();
	}

	public boolean hasArgument(String arg) {
		if (args == null)
			return false;
		
		if (arg.indexOf('-') != 0 && hasArgument("-" + arg))
			return true;
		
		for (int i=0 ; i<args.size() ; i++) {
			String v = args.get(i);
			if (v == null)
				return false;
			if (v.equals(arg)) {
//				adjustNextArgIndex(i+1);
				return true;
			}	
		}
		return false;	
	}
	
//	private void adjustNextArgIndex(int justReferenced) {
//		if (justReferenced == args.size())
//			nextReferenceArgIndex = -1;
//		else
//			nextReferenceArgIndex = justReferenced;
//	}
	
	public boolean getFlag(String flagName) {
		if (args == null)
			return false;
		
		if (flagName.indexOf('-') != 0)
			flagName = "-" + flagName;
		
		for (int i=0 ; i<args.size() ; i++) {
			String v = peekArg(i); // args.get(i);
			if (v == null)
				return false;
			if (v.equals(flagName)) {
				removeArg(i); // args.remove(i);
//				adjustNextArgIndex(i);
				return true;
			}	
		}
		return false;
	}

	/**
	 * Get the value of the first option where option is expected as '-&lt;optionName&gt; &ltoption value&gt;'.
	 * The option, if present, is removed.
	 * @param optionName name of option with or w/o the leading dash.
	 * @return null if not present.
	 */
	public String getOption(String optionName) {
		return getOption(optionName, true);
	}
	
	public String getOption(String optionName, String dflt) {
		String val = getOption(optionName, true);
		if (val == null)
			val = dflt;
		return val;
	}

	public int getOption(String optionName, int dflt) {
		String val = getOption(optionName, true);
		if (val == null)
			return dflt;
		return Integer.parseInt(val);
	}
	
	public double getOption(String optionName, double dflt) {
		String val = getOption(optionName, true);
		if (val == null)
			return dflt;
		return Double.parseDouble(val);
	}
	
	private String getOption(String optionName, boolean remove) {
		if (args == null)
			return null;
		if (optionName.indexOf('-') != 0)
			optionName = "-" + optionName;
		
		for (int i=0 ; i<args.size() ; i++) {
			String v = peekArg(i); // args.get(i);
			if (v == null)
				return null;
			if (v.equals(optionName)) {
				int valIndex;
				if (remove) {
					args.remove(i);
					valIndex = i;
				} else {
					valIndex = i+1;
				}
				if (args.size() > valIndex)
					v = args.get(valIndex);
				else
					v = null;
				
				if (v == null) {
//					adjustNextArgIndex(remove ? i : i + 1);
					return null;
				}
				if (remove)
					removeArg(i); // args.remove(i);
//				adjustNextArgIndex(remove ? i : i + 2);
				return v;
			}	
		}
		return null;
	}

	public String getOption(String optionName, String[] values, boolean remove) {
		String optVal = getOption(optionName, false);
		if (optVal == null)
			return null;
		for (int i=0 ; i<values.length ; i++) {
			if (values[i].equals(optVal)) {
				if (remove)
					getOption(optionName,true);
				return optVal;
			}
		}
		return null;
	}

//	public String nextArgument(boolean remove) {
//		if (nextReferenceArgIndex < 0 || nextReferenceArgIndex >= args.size() )
//			return null;
//		
//		String val = args.get(nextReferenceArgIndex);
//		if (val == null)
//			return null;
//		if (remove) {
//			args.remove(nextReferenceArgIndex);
//		} else {
//			adjustNextArgIndex(nextReferenceArgIndex + 1);
//		}
//		return val;
//	}

}
