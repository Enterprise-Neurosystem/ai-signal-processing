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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLogger;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.transform.ITrainingWindowTransform;
import org.eng.aisp.util.ClassFinder.Visitor;

/**
 * A base class to help with the creation of a Java instance from a JavaScript file that creates the instance.
 * @author DavidWood
 *
 * @param <RESULT>
 */
public class JScriptBuilder {

	/**
	 * List of packages (regex) to search for classes to be imported into the script. 
	 */
	protected static final String[] INCLUDED_PACKAGE_PREFIXES = {
			IFeature.class.getPackage().getName(), 
			IClassifier.class.getPackage().getName(), 
			ITrainingWindowTransform.class.getPackage().getName(), 
		};
	/**
	 * List of packages (regex) to NOT search for classes to be imported into the script. 
	 */
	protected static final String[] EXCLUDED_PACKAGE_PREFIXES = {
			// ".*Spark.*"
		};

	/**
	 * List of strings, 1 of which is required, in the class name before we add to the list of imports in the script. 
	 */
	private final static String[] ClassNameMatches = new String[] { "Classifier", "Feature", "Processor", "Transform" };
	/**
	 * List of strings, any of which is required, in the class names that are NOT added to the script. 
	 */
	private final static String[] ClassNameNonMatches = new String[] { "$", "Abstract", "Test", "package-info", "Spark" };
	
	/** String containing all Java import statements automatically included in all scripts before executing */
	protected static List<String> AVAILABLE_CLASSES_FOR_IMPORT = null;
	protected final String script;
	private final JScriptEngine jsEngine = new JScriptEngine();
	protected final Map<String,Object> bindings;
	private Class<?>[] resultClasses;
	private final String[] preferredResultVarNames;

	/**
	 * 
	 * @param script java script file content
	 * @param resultClass the class of the results returned by {@link #build()}.
	 * @param preferredResultVarName the name of the variable of the given class to return from {@link #build()} when
	 * multiple instances of the given class are found.
	 */
	public JScriptBuilder(String script, Class<?> resultClass, String preferredResultVarName)  {
		this(script, new Class<?>[] { resultClass }, new String[] { preferredResultVarName});
	}

	public JScriptBuilder(String script, Class<?>[] resultClasses, String[] preferredResultVarNames)  {
		if (script == null)
			throw new IllegalArgumentException("script must be non-null");
		if (resultClasses == null)
			throw new IllegalArgumentException("result class must be non-null");
		if (preferredResultVarNames == null)
			throw new IllegalArgumentException("var names must be non-null");
		if (resultClasses.length != preferredResultVarNames.length)
			throw new IllegalArgumentException("Number of result classes must equal the number of names provided");
		
		this.resultClasses = resultClasses;
		this.preferredResultVarNames = preferredResultVarNames;
		if (AVAILABLE_CLASSES_FOR_IMPORT == null)	// Defer this until the class is actually instantiated (not just loaded) to improve performance.
			AVAILABLE_CLASSES_FOR_IMPORT = findClassesToImport(INCLUDED_PACKAGE_PREFIXES, EXCLUDED_PACKAGE_PREFIXES, ClassNameMatches, ClassNameNonMatches ); 

		if (script.contains("exit"))	// A simple test for System.exit().
			throw new IllegalArgumentException("Script has suspicious content.  Not evaluating.");
			
//		if (trainingLabel != null) {
//			bindings = new HashMap<String,Object>();
//			bindings.put(TRAINING_LABEL_VAR_NAME, trainingLabel);
//		} else {
			bindings = null;
//		}
		String importStatements = getNeededImports(script, AVAILABLE_CLASSES_FOR_IMPORT);
		this.script = importStatements + script;
	}
	
	/**
	 * Look in the given script for classes found in the given availableClasses and create
	 * and import statement for those found.
	 * @param script
	 * @param availableClasses
	 * @return never null.
	 */
	private static String getNeededImports(String script, List<String> availableClasses) {
		String ret = ""; 
		for (String name : availableClasses ) {
			String simpleName = getSimpleClassName(name); 
			if (script.contains(simpleName)) {
				String statement = "var " + simpleName + " = Java.type(\"" + name + "\");\n";
				ret = ret + statement; 
			}
		}
		return ret;
	}
	private final static String DotClass = ".class";

	/**
	 * Get the simple name of a class as in class.getSimpleName();
	 * @param className name using "."  or "/" as package separators and optionally ending in .class.
	 * @return the simple class name as would be returned by {@link Class#getSimpleName()}. 
	 */
	private static String getSimpleClassName(String className) {
		int index;
		if (className.endsWith(DotClass)) {
			index = className.lastIndexOf(DotClass);
			if (index >= 0)
				className = className.substring(0, index);
		}
		index = className.lastIndexOf("/");
		if (index < 0)
			index = className.lastIndexOf(".");
		String simpleName = className.substring(index+1);
		if (index > 0)
			simpleName = className.substring(index+1);
		else 
			simpleName = className;
	
		return simpleName;
	}

//	/**
//	 * Search the class path and select classes matching the given class names.
//	 * Ignore classes that don't have Classifier, Extractor or Processor in the name.
//	 * Ignore classes that are inner, Abstractor or Test classes.
//	 * Finally, make sure we match one of the names given.
//	 * Convert names of classes into 'var ... = Java.type("...")' statements.
//	 * @param importClassesExcluded 
//	 * @param names
//	 * @return
//	 */
//	private static String createImportStatements(final String[] includedClassNames, String[] excludedClassNames) {
//	
//		final List<String> importedClasses = findClassesToImport(includedClassNames, excludedClassNames);
//		
//		// Now build the javascript for each of the classes we want to make available.
//		String ret = ""; 
//		for (String name : importedClasses ) {
//			String simpleName = getSimpleClassName(name); 
//			String statement = "var " + simpleName + " = Java.type(\"" + name + "\");\n";
//			ret = ret + statement; 
//		}
//		return ret;
//	}


	/**
	 * @param includedPackagePrefixes
	 * @param excludedPackagePrefixes
	 * @return
	 */
	protected static List<String> findClassesToImport(final String[] includedPackagePrefixes, String[] excludedPackagePrefixes,
			String[] includeClassNameTokens, String[] excludedClassNameTokens) {
		final List<String> importedClasses = new ArrayList<String>();
	
		ClassFinder.findClasses(new Visitor<String>() {
			public boolean visit(String className) {
				String simpleName = getSimpleClassName(className); 
				boolean matched = false;
				for (int i=0 ; i<includeClassNameTokens.length  && !matched ; i++) 
					matched = matched || simpleName.contains(includeClassNameTokens[i]);
				if (!matched)
					return true;	// Ignore anything doesn't look like a classifier, extractor or processor. 
				matched = false;
				for (int i=0 ; i<excludedClassNameTokens.length  && !matched ; i++) 
					matched = matched || simpleName.contains(excludedClassNameTokens[i]);
				if (matched)
					return true;	// Ignore inner classes and abstract classes.
				className = className.replaceAll("/",".");
				boolean include = false;
				for (int i=0 ; i<includedPackagePrefixes.length && !include; i++) {	// TODO: this could probably be sped up.
					if (className.startsWith(includedPackagePrefixes[i])) 
						include = true;
				}
				if (include) {
					for (int i=0 ; i<excludedPackagePrefixes.length && !include; i++) {	// TODO: this could probably be sped up.
						if (className.startsWith(excludedPackagePrefixes[i])) 
							include = false;
					}
					if (include)  {
//						AISPLogger.logger.info("INcluding " + className);
						if (className.endsWith(DotClass)) {
							int index = className.lastIndexOf(DotClass);
							className = className.substring(0,index); 
						}
						importedClasses.add(className);
					}
				}
				return true;
			};
	
		});	// end of findClasses() call.
		return importedClasses;
	}

	/**
	 * Run the script and extract the named variables.
	 * @return a map of objects of the class and optional names created in the script keyed by the names provided to the constructor.
	 * On successful return, the map contains objects of the type/class specified in the constructor.
	 */
	public Map<String,Object> buildAll() throws AISPException {
		Map<String,Object> results = new HashMap<String,Object>();
		
		try {
//			AISPLogger.logger.info("Script is...\n" + script);
			this.jsEngine.runScript(script, bindings, false, false);
			for (int i=0; i<resultClasses.length ; i++) {
				String varName = preferredResultVarNames[i];
				Class<?> resultClass = resultClasses[i];
				Object result = this.jsEngine.getScriptVariable(resultClass, varName, false);
				if (result == null) 
					result = this.jsEngine.getScriptVariable(resultClass, null, false);
				if (result == null) 
					throw new AISPException("Script did not create an instance of " + resultClass.getName());
				results.put(varName,  result);
			}
		} catch (Exception e) {
			throw new AISPException("Error evaluating script: "  + e.getMessage(), e);
		}
		return results;
	}

}
