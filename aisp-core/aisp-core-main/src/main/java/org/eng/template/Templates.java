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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eng.template.MetaData.MacroDefinition;
import org.eng.template.yaml.YAMLTemplateIO;

public class Templates { 

	public final static String BEGIN_NEW_TEMPLATE = "// BEGIN NEW TEMPLATE";

	public static List<Template> read(String fileName, ITemplateIO templateIO) throws TemplateException, IOException {
		FileReader fr = new FileReader(fileName);
		return read(fr, templateIO);
	}
	
	/**
	 * 
	 * @param input closed upon return;
	 * @param factory
	 * @return
	 * @throws TemplateException
	 * @throws IOException 
	 */
	public static List<Template> read(Reader input, ITemplateIO templateReader) throws TemplateException, IOException {
		BufferedReader reader = new BufferedReader(input);
		StringBuilder templateText = null; 
		String line; 
		boolean haveTemplate = false;
		ArrayList<Template> ptList = new ArrayList<Template>();
		Template pt;
		int lineNum = 0;
		
		try {
			while ((line = reader.readLine()) != null) {
				lineNum++;
				boolean newMarker = line.startsWith(BEGIN_NEW_TEMPLATE);
				if (newMarker) { 	
					if (haveTemplate) {
						pt = templateReader.read(new StringReader(templateText.toString())); 
						ptList.add(pt);
						haveTemplate = false;
						templateText = null;
					}
				} else { // Start accumulating the template text
					if (templateText == null)
						templateText = new StringBuilder();
					// Accumulate the non-comments (i.e. policy strings).
					templateText.append(line);
					templateText.append('\n');
					haveTemplate = haveTemplate || line.trim().length() > 0;
				}
			}
			// Store the last template read.
			if (templateText != null) {
				pt = templateReader.read(new StringReader(templateText.toString())); 
				ptList.add(pt);
			}
		} catch (IOException e) {
			throw new TemplateException("Trouble reading from the given reader", e);
		} finally {
			reader.close();
		}
	
		return ptList;
	
	}
	
	public static void write (String fileName, List<Template> templates, ITemplateIO templateIO ) throws TemplateException, IOException {
		FileWriter fw = new FileWriter(fileName);
		write(fw, templates, templateIO);
		fw.close();
	}

	/**
	 * 
	 * @param writer not closed upon return.
	 * @param templates
	 * @param mdWriter
	 * @throws TemplateException
	 * @throws IOException
	 */
	public static void write (Writer writer, Iterable<Template> templates, ITemplateIO templateIO) throws TemplateException, IOException {
		Iterator<Template> iter = templates.iterator();
		while (iter.hasNext()) {
			templateIO.write(writer, iter.next());;
			if (iter.hasNext())
				writer.write(BEGIN_NEW_TEMPLATE + "\n");
		}
	}

	public static void main(String[] args) throws TemplateException, IOException {
		List<MacroDefinition> mdList = new ArrayList<MacroDefinition>();
		mdList.add(new MacroDefinition("macro1", "description 1", "defaultString"));
		mdList.add(new MacroDefinition("macro2", "description 2", new Integer(1)));
		MetaData md = new MetaData("template name", "template description", mdList);
		Template template = new Template("many lines\n of text\n for the macro1=${macro1} macro2=${macro2}   to read", md);
		List<Template> tList = new ArrayList<Template>();
		tList.add(template);
		String file = "template-test.txt";
		ITemplateIO templateIO = new YAMLTemplateIO();
		Templates.write(file, tList, templateIO);
		Templates.write(new PrintWriter(System.out), tList, templateIO); 

		
		tList = new Templates().read(file, templateIO); 
		System.out.println("Read back");
		Templates.write(new PrintWriter(System.out), tList, templateIO); 
		
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
