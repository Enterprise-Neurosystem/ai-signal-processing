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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eng.template.MetaData.MacroDefinition;

public class Template {

	protected MetaData metadata;
	
	private String templateText;

	protected static final String ID_REGEX =  "[_a-zA-Z0-9]+";

	protected final String startMacro;
	protected final String endMacro;
	protected final String stringQuote;
//
//	private final Pattern startMacroPattern;
//	private final Pattern endMacroPattern;
	private final Pattern macroRefPattern; 

	
	/**
	 * For YAML and other deserializers.
	 */
	public Template() {
		this("", new MetaData());
	}

	public Template(String templateText) {
		this(templateText, new MetaData());
	}

	public Template(String templateText, MetaData metadata) {
		this(templateText, metadata, null, "${", "}");
	}

	/**
	 * 
	 * @param templateText
	 * @param metadata may be null.
	 * @param stringQuote
	 * @param beginMacro
	 * @param endMacro
	 */
	public Template(String templateText, MetaData metadata, String stringQuote, String beginMacro, String endMacro) {
		this.metadata = metadata;
		this.templateText = templateText;
		this.stringQuote = stringQuote;
		this.startMacro = beginMacro;
		this.endMacro = endMacro;
		String QUOTED_START_MACRO = Pattern.quote(beginMacro);
		String QUOTED_END_MACRO = Pattern.quote(endMacro); 
//		startMacroPattern = Pattern.compile(QUOTED_START_MACRO);
//		endMacroPattern = Pattern.compile(QUOTED_END_MACRO);
		macroRefPattern = Pattern.compile(QUOTED_START_MACRO + ID_REGEX + QUOTED_END_MACRO);
	}


	/**
	 * Fill the template with the values from the binding and/or the defaults.
	 * @param bindings may be null in which case only the defaults are used.  If provided, 
	 * overrides any default values.
	 * @return never null.
	 * @throws TemplateException if there are an incomplete set of substitutions and macro references would 
	 * be present in the returned value.
	 */
	public String generate(Map<String, ? extends Object> bindings) throws TemplateException {
		if (templateText == null)
			return "";
		
		String concrete = templateText;
		if (metadata != null && this.metadata.getMacroDefinitions() != null) {
			for (MacroDefinition md : metadata.getMacroDefinitions()) {
				String name = md.getName();
				Object value = bindings == null ? null : bindings.get(name);
				if (value == null)
					value = md.getDefaultValue();
				if (value != null) 
					concrete = substituteMacroValue(concrete, name, value);
			}
		}

		// Check for unbound/macros that did not get substituted
		Matcher m = macroRefPattern.matcher(concrete);
		List<String> unbound = new ArrayList<String>();
		while (m.find()) {
			String ref = concrete.substring(m.start(), m.end());
			unbound.add(ref);
		}
		if (unbound.size() > 0) 
			throw new TemplateException("Unbound macros: " + unbound);
		
		return concrete;
	}

//	/**
//	 * Strip off the referencing characters.  For exmaple, '${abc}' -> 'abc'.
//	 * @param ref
//	 * @return never null.
//	 */
//	private String getMacroNameFromReference(String ref) {
//		Matcher m = startMacroPattern.matcher(ref);
//		ref = m.replaceAll("");
//		m = endMacroPattern.matcher(ref);
//		ref = m.replaceAll("");
//		return ref;
//	}

	private String substituteMacroValue(String target, String macroName, Object value) {
		String ref = getMacroReference(macroName);
		ref = Pattern.quote(ref);
		String substitution = getSubstitutionString(value);
		target = target.replaceAll(ref, substitution);
		return target;
	}

	private String getSubstitutionString(Object value) {
		String substitution;
		if ( stringQuote != null && value instanceof String)
			substitution =  stringQuote + value.toString() + stringQuote;
		else
			substitution = value.toString();
		return substitution;
	}

	/**
	 * Get an unquoted reference to the give named macro.
	 * @param macroName 
	 * @return never null
	 */
	private String getMacroReference(String macroName) {
		String macro = startMacro + macroName + endMacro; 
		return macro;
	}


	public String getTemplateText() {
		return templateText; 
	}

	/**
	 * @return the metadata
	 */
	public MetaData getMetadata() {
		return metadata;
	}

	/**
	 * @param metadata the metadata to set
	 */
	public void setMetadata(MetaData metadata) {
		this.metadata = metadata;
	}

	/**
	 * @param templateText the templateText to set
	 */
	public void setTemplateText(String templateText) {
		this.templateText = templateText;
	}

	public String prettyPrint() {
		String r; 
		if (metadata != null) {
			r = metadata.prettyPrint();
			r += "\n";
			r += templateText;
		} else {
			r = templateText;
		}
		return r;
	}

	@Override
	public String toString() {
		return "Template [metadata=" + metadata + ", templateText=" + templateText + ", startMacro=" + startMacro
				+ ", endMacro=" + endMacro + ", stringQuote=" + stringQuote + ", macroRefPattern=" + macroRefPattern
				+ "]";
	}
}
