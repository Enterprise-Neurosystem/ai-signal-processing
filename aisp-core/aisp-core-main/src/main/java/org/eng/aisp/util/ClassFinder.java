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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eng.aisp.AISPProperties;

public class ClassFinder {

	public interface Visitor<T> {
		/**
		 * @return {@code true} if the algorithm should visit more results,
		 *         {@code false} if it should terminate now.
		 */
		public boolean visit(T t);
	}
	
	/**
	 * Get all names of classes on the classpath that match the given regular expression. 
	 * @param classNameRegex package names are separated by '/'.
	 * @return never null. A list of names matching the regular expression so that returned names use '/' and not '.' as package separator.
	 */
	public static List<String> findClassNames(String classNameRegex) {
		List<String> classNames = new ArrayList<String>();
		Pattern pattern = Pattern.compile(classNameRegex);

		ClassFinder.findClasses( new Visitor<String>() {

			@Override
			public boolean visit(String className) {
				Matcher matcher = pattern.matcher(className);
				if (matcher.matches()) {
					className = className.replace(".class", "");	// Remove trailing .class.
					classNames.add(className);
				}
				return true;
			}
			
		});

		return classNames;
	}

	/**
	 * Search the JVM classpaths and an optional classpath defined in a ClassFinder.class.getName() + ".classpath" property
	 * with AISPProperties.instance().
	 * @param visitor
	 */
	public static void findClasses(Visitor<String> visitor) {
		visitResourcesOnClassPath(visitor, ".class");
	}

	
	private static String[] normalizeExtensions(String[] extensions) {
		if (extensions == null || extensions.length == 0)
			return null;
		String[] r = new String[extensions.length];
		for (int i=0 ; i<r.length ;i++) {
			String ext = extensions[i];
			if (!ext.startsWith("."))
				ext= "." + ext; 
			r[i] = ext.toLowerCase();
		}
		return r;
	}

	public static void visitResourcesOnClassPath(Visitor<String> visitor, String resourceExtension) {
		String[] extensions = resourceExtension == null ? null : new String[] { resourceExtension};
		visitResourcesOnClassPath(visitor, extensions);
	}

	/**
	 * Call the visitor on all resources on the class path that have the given extension. 
	 * @param visitor
	 * @param resourceExtensions
	 */
	public static void visitResourcesOnClassPath(Visitor<String> visitor, String[] resourceExtensions) {
		resourceExtensions = normalizeExtensions(resourceExtensions);

		List<String> paths = new ArrayList<String>();
		String classpath = System.getProperty("java.class.path");
		String[] t;
		if (classpath != null) {
			t = classpath.split(System.getProperty("path.separator"));
			for (String path : t) 
				paths.add(path);
		}
		
		String propName = ClassFinder.class.getName();
		classpath = AISPProperties.instance().getProperty(propName + ".classpath");
		if (classpath != null) {
			t = classpath.split(System.getProperty("path.separator"));
			for (String path : t) 
				paths.add(path);
		}
		
//		AISPLogger.logger.info("java.class.path=" + classpath);
		String javaHome = System.getProperty("java.home");
		File file = new File(javaHome + File.separator + "lib");
		if (file.exists()) {
			paths.add(file.getAbsolutePath());
//			visitResources(file, file, true, visitor, resourceExtension);
		}

		for (String path : paths) {
			file = new File(path);
			if (file.exists()) {
				visitResources(file, file, true, visitor, resourceExtensions);
			}
		}
	}

	private static boolean visitResources(File root, File file, boolean includeJars, Visitor<String> visitor, String[] normalizedExtensions) {

		if (file.isDirectory()) {
//			AISPLogger.logger.info("Search directory " + root);
			for (File child : file.listFiles()) {
				if (!visitResources(root, child, includeJars, visitor, normalizedExtensions)) {
					return false;
				}
			}
		} else {
//			AISPLogger.logger.info("Search file " + root);
			if (file.getName().toLowerCase().endsWith(".jar") && includeJars) {
//				AISPLogger.logger.info("Search jar " + root);
				JarFile jar = null;
				try {
					jar = new JarFile(file);
					if (jar != null) {
						Enumeration<JarEntry> entries = jar.entries();
						while (entries.hasMoreElements()) {
							JarEntry entry = entries.nextElement();
							String name = entry.getName();
							if ((normalizedExtensions == null || isExtensionMatch(name, normalizedExtensions)) && !visitor.visit(name))
								return false;
						}
					}
				} catch (Exception ex) {
					;
				} finally {
					if (jar != null) {
						try {
							jar.close();
						} catch (IOException e) {
							;
						}
					}
				}
			} else if (isExtensionMatch(file.getName(),normalizedExtensions)) {
				if (!visitor.visit(createClassName(root, file))) {
					return false;
				}
			}
		}

		return true;
	}

	private static boolean isExtensionMatch(String fileName, String[] extensions) {
		if (extensions == null || extensions.length == 0)
			return true;
		fileName = fileName.toLowerCase();
		for (String ext : extensions) {
			if (fileName.endsWith(ext))
				return true;
		}
		return false;
	}

	private static String createClassName(File root, File file) {
		StringBuffer sb = new StringBuffer();
		String fileName = file.getName();
//		sb.append(fileName.substring(0, fileName.lastIndexOf(".class")));
		sb.append(fileName);
		file = file.getParentFile();
		while (file != null && !file.equals(root)) {
			sb.insert(0, "/").insert(0, file.getName());
			file = file.getParentFile();
		}
		return sb.toString();
	}
}
