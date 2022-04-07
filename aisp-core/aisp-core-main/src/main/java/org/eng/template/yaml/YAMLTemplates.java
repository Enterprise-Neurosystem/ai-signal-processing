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
package org.eng.template.yaml;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eng.template.MetaData;
import org.eng.template.MetaData.MacroDefinition;
import org.eng.template.Template;
import org.eng.template.TemplateException;
import org.eng.template.Templates;

public class YAMLTemplates { 

	public final static String BEGIN_NEW_TEMPLATE = "// BEGIN NEW TEMPLATE";
	
	private final static YAMLTemplateIO TemplateIO = new YAMLTemplateIO();

	public static List<Template> read(String fileName) throws TemplateException, IOException {
		return Templates.read(fileName, TemplateIO);
	}
	
	/**
	 * 
	 * @param input closed upon return;
	 * @param factory
	 * @return
	 * @throws TemplateException
	 * @throws IOException 
	 */
	public static List<Template> read(Reader input) throws TemplateException, IOException {
		return Templates.read(input, TemplateIO);
	}
	
	public static void write (String fileName, List<Template> templates) throws TemplateException, IOException {
		Templates.write(fileName, templates, TemplateIO);
	}

	/**
	 * 
	 * @param writer not closed upon return.
	 * @param templates
	 * @param mdWriter
	 * @throws TemplateException
	 * @throws IOException
	 */
	public static void write (Writer writer, Iterable<Template> templates) throws TemplateException, IOException {
		Templates.write(writer, templates, TemplateIO);
	}

	public static void main(String[] args) throws TemplateException, IOException {
		List<MacroDefinition> mdList = new ArrayList<MacroDefinition>();
		List<Object> options = new ArrayList<Object>();
		options.add(new Integer(0));
		options.add(new Double(1.5));
		
		mdList.add(new MacroDefinition("macro1", "display name1", "description 1", "defaultString", options));
		mdList.add(new MacroDefinition("macro2",  "description 2", new Integer(1)));
		MetaData md = new MetaData("template name", "template description", mdList);
		Template template = new Template("many lines\n of text\n for the macro1=${macro1} macro2=${macro2}   to read", md);
		List<Template> tList = new ArrayList<Template>();
		tList.add(template);
		String file = "template-test.txt";
		YAMLTemplates.write(file, tList);
		YAMLTemplates.write(new PrintWriter(System.out), tList); 

		
		tList = new YAMLTemplates().read(file); 
		System.out.println("Read back");
		YAMLTemplates.write(new PrintWriter(System.out), tList); 
		
		Map<String,Object> bindings = new HashMap<String,Object>();
		System.out.println(tList.get(0).generate(bindings));

		bindings.put("macro1", "value1");
		bindings.put("macro2", "value2");
		System.out.println(tList.get(0).generate(bindings));
		
//		Yaml yaml = new Yaml();
//		yaml.dump(template, new PrintWriter(System.out));
//		
//		Gson gson = new Gson();
//		System.out.println(gson.toJson(template));
	}
	
}
