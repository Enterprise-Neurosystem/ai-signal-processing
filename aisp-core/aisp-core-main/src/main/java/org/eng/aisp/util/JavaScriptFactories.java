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
package org.eng.aisp.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLogger;
import org.eng.aisp.util.ClassFinder.Visitor;
import org.eng.template.Template;
import org.eng.template.TemplateException;
import org.eng.template.yaml.YAMLTemplates;
import org.eng.util.FileUtils;


public class JavaScriptFactories {

	public static class InstanceSpecification {
		protected final String name;
		protected final InstanceSpecificationType type;
		protected final String location;
		protected final Template specification;
		/**
		 * @param name
		 * @param type
		 * @param location
		 * @param specification
		 */
		public InstanceSpecification(String name, InstanceSpecificationType type, String location, String specification) {
			this(name, type, location, new Template(specification));
		}
		public InstanceSpecification(String name, InstanceSpecificationType type, String location, Template specification) {
			this.name = name;
			this.type = type;
			this.location = location;
			this.specification = specification;
		}
	
		public String getName() { return name; }
		public InstanceSpecificationType getType() { return type; }
		public String getLocation() { return location; }
		public Template getSpecification() { return specification; }
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "InstanceSpecification [name=" + name + ", type=" + type + ", location=" + location + ", specification="
					+ specification + "]";
		}
		
	}

	public static enum InstanceSpecificationType {
		JavaScript
	}

	protected static final String JAVASCRIPT_EXTENSION = ".js";
	protected static final String YAML_JAVASCRIPT_EXTENSION = ".jsyt";
	private static final String[]  ALL_JS_EXTENSIONS = { JAVASCRIPT_EXTENSION, YAML_JAVASCRIPT_EXTENSION};
	
	/**
	 * @param specName javascript file name 
	 * @param resourceName the name of the resource with either .js or .yamljs extension. 
     * @return null if resource not found.
	 * @throws AISPException parsing the yaml file.
	 * @throws IOException  on reading the resource 
	 */
	protected static InstanceSpecification readResourceAsJSInstanceSpec(String specName, String resourceName) throws AISPException, IOException {
		InstanceSpecification ms = null;
		if (resourceName == null)
			throw new IllegalArgumentException("resourceName is null");
		InputStream is = InstanceSpecification.class.getResourceAsStream(resourceName);
		if (is != null) {
//			try {
				String text = readStream(is); 
				is.close();
				ms = createInstanceSpecification(specName, resourceName, text);
//			} catch (IOException e) {
//					throw new AISPException("Could not read stream from " + resourceName);
//			}
		}
		return ms;
	}

	/**
	 * @param specName
	 * @param resourceName
	 * @param text
	 * @return
	 * @throws IOException
	 * @throws AISPException
	 */
	protected static InstanceSpecification createInstanceSpecification(String specName, String resourceName,
			String text) throws IOException, AISPException {
		InstanceSpecification ms;
		Template template;
		if (resourceName.endsWith(JAVASCRIPT_EXTENSION)) {
			template = new Template(text);
		} else if (resourceName.endsWith(YAML_JAVASCRIPT_EXTENSION)) {
			List<Template> tList;
			try {
				tList = YAMLTemplates.read(new StringReader(text));
			} catch (TemplateException e) {
				throw new AISPException("Could not parse YAML template: " + e.getMessage(), e);
			}
			if (tList.size() != 1)
				throw new AISPException("Resource at " + resourceName + " contains " + tList.size() + " templates.");
			template = tList.get(0);
		} else {
			throw new IllegalArgumentException("Unrecognized extension: " + resourceName);
		}
		ms = new InstanceSpecification(specName,InstanceSpecificationType.JavaScript, resourceName, template);
		return ms;
	}

//	protected static String readResource(String resourceName) throws IOException {
//		if (resourceName == null)
//			throw new IllegalArgumentException("resourceName is null");;
//		InputStream is = InstanceSpecification.class.getResourceAsStream(resourceName);
//		if (is == null)
//			return null;
//		String text = readStream(is);
//		is.close();
//		return text;
//	}

	/**
	 * @param is
	 * @return
	 * @throws IOException
	 */
	protected static String readStream(InputStream is) throws IOException {
		String text = null; 
		if (is != null) {
			byte[] bytes;
			bytes = FileUtils.readByteArray(is);
			is.close();
			text = new String(bytes);
		}
		return text;
	}
	  
	/**
	 * Get this list of all  JavaScript specifications within the given package. 
	 * This is the base names of all javascript files (ending with .js or .jsyt) found in the given package.
	 * @param packageName the name of the package in which to search for specifications.
	 * @return never null.  Basenames of javascript resources files in the same package as this class anywhere on the classpath.
	 */
	public static synchronized Map<String,InstanceSpecification> getJSInstanceSpecifications(String packageName) {
		Map<String,InstanceSpecification> specMap = new HashMap<String,InstanceSpecification>();
//			final String packageName = ClassifierFactories.class.getPackage().getName().replaceAll("\\.", "/");
		ClassFinder.visitResourcesOnClassPath(new Visitor<String>() {
			public boolean visit(String resourceName) {
				if (!resourceName.endsWith(JAVASCRIPT_EXTENSION) && !resourceName.endsWith(YAML_JAVASCRIPT_EXTENSION))
					return true;	// Ignore anything doesn't look like a javascript file 
				if (resourceName.startsWith(packageName)) {	// Only javascript files in the same package as this class.
					int index = resourceName.lastIndexOf(JAVASCRIPT_EXTENSION);
					if (resourceName.endsWith(JAVASCRIPT_EXTENSION))
						index = resourceName.lastIndexOf(JAVASCRIPT_EXTENSION);
					else 	// YAML JS
						index = resourceName.lastIndexOf(YAML_JAVASCRIPT_EXTENSION);
					String specName = resourceName.substring(0, index);	// remove .js extension
					index = specName.lastIndexOf("/");
					specName= specName.substring(index+1);		// remove package name
					InstanceSpecification ms;
					try {
						ms = readResourceAsJSInstanceSpec(specName, "/" + resourceName);
					} catch (Exception e) {
						AISPLogger.logger.severe("IGNORING " + resourceName + ": " + e.getMessage());
						ms = null;
					}
					if (ms != null)
						specMap.put(specName, ms);
				}
				return true;
			}
		}, ALL_JS_EXTENSIONS);	// end of .js files .
		return specMap;
	} 

}
