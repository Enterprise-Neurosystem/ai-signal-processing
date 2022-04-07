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
package org.eng.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetaData {
	
	String name = "";	
	String description = "";
	boolean visible = true;
	List<MacroDefinition> macroDefinitions = new ArrayList<MacroDefinition>();

	/**
	 * Used only by YAML and other deserializers.
	 */
	public MetaData() {
		
	}
	
	/**
	 * @param name
	 * @param description
	 * @param macroDefinitions
	 */
	public MetaData(String name, String description, List<MacroDefinition> macroDefinitions) {
		this.name = name;
		this.description = description;
		this.macroDefinitions = macroDefinitions;
	}

	public static class MacroDefinition {
		String name;		// Name of macro
		String displayName;	// Name suitable for showing in a UI.
		String type= "string";
		String description;
		Object defaultValue;
		List<Object> choices;
		double min, max;

		/**
		 * Use only for YAML and other deserializers
		 */
		public MacroDefinition() {
			
		}

		public MacroDefinition(String name, String description, Object defaultValue) {
			this(name, null, description, defaultValue, null);
		}
		
		public MacroDefinition(String name, String displayName, String description, Object defaultValue, List<Object> choices) {
			this.name = name;
			this.displayName = displayName;
			this.description = description;
			this.defaultValue = defaultValue;
			this.choices = choices; 
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return the description
		 */
		public String getDescription() {
			return description;
		}

		/**
		 * @param description the description to set
		 */
		public void setDescription(String description) {
			this.description = description;
		}

		/**
		 * @return the defaultValue
		 */
		public Object getDefaultValue() {
			return defaultValue;
		}

		/**
		 * @param defaultValue the defaultValue to set
		 */
		public void setDefaultValue(Object defaultValue) {
			this.defaultValue = defaultValue;
		}

		public String prettyPrint() {
			String r = "name: " + name + ", description: " + description + ", default: " + defaultValue.toString();
			if (choices != null)
				r += ", choices: " + choices;
			return r;
		}

		@Override
		public String toString() {
			final int maxLen = 8;
			return "MacroDefinition [name=" + name + ", description=" + description + ", defaultValue=" + defaultValue
					+ ", restrictedValues="
					+ (choices != null ? choices.subList(0, Math.min(choices.size(), maxLen))
							: null)
					+ "]";
		}

		/**
		 * @return the choices
		 */
		public List<Object> getChoices() {
			return choices;
		}

		/**
		 * @param choices the choices to set
		 */
		public void setChoices(List<Object> choices) {
			this.choices = choices;
		}

		/**
		 * @return the type
		 */
		public String getType() {
			return type;
		}

		/**
		 * @param type the type to set
		 */
		public void setType(String type) {
			this.type = type;
		}

		/**
		 * @return the min
		 */
		public double getMin() {
			return min;
		}

		/**
		 * @param min the min to set
		 */
		public void setMin(double min) {
			this.min = min;
		}

		/**
		 * @return the max
		 */
		public double getMax() {
			return max;
		}

		/**
		 * @param max the max to set
		 */
		public void setMax(double max) {
			this.max = max;
		}

		/**
		 * @return the displayName
		 */
		public String getDisplayName() {
			return displayName;
		}

		/**
		 * @param displayName the displayName to set
		 */
		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}


		
	}
	
	private transient Map<String, MacroDefinition> mappedMacros = null; 
	
	private void initMacroMap() {
		if (mappedMacros == null) {
			mappedMacros = new HashMap<String,MacroDefinition>();
			for (MacroDefinition md : macroDefinitions) {
				mappedMacros.put(md.getName(), md);
			}
		}
	}

	public Object getDefaultValue(String macroName) {
		initMacroMap();
		MacroDefinition md = mappedMacros.get(macroName);
		if (md == null)
			return null;
		return md.getDefaultValue();
	}

	public boolean hasMacroDefinition(String macroName) {
		initMacroMap();
		MacroDefinition md = mappedMacros.get(macroName);
		return md != null;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the macroDefinitions
	 */
	public List<MacroDefinition> getMacroDefinitions() {
		return macroDefinitions;
	}

	/**
	 * @param macroDefinitions the macroDefinitions to set
	 */
	public void setMacroDefinitions(List<MacroDefinition> macroDefinitions) {
		this.macroDefinitions = macroDefinitions;
	}

	public String prettyPrint() {
		String r =    "Name: " + name + "\n" 
					+ "Description: " + description;
		if (macroDefinitions != null && macroDefinitions.size() > 0) {
			r += "\nMacros:";
			for (MacroDefinition md : macroDefinitions) {
				r += "\n\t";
				r += md.prettyPrint();
			}
	    }
		return r;
	}

	@Override
	public String toString() {
		final int maxLen = 8;
		return "MetaData [name=" + name + ", description=" + description + ", macroDefinitions="
				+ (macroDefinitions != null ? macroDefinitions.subList(0, Math.min(macroDefinitions.size(), maxLen))
						: null)
				+ "]";
	}

	/**
	 * @return the visible
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * @param visible the visible to set
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
	}



}
