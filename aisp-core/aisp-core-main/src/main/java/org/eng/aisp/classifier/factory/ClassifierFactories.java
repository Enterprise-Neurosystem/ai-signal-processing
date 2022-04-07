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
package org.eng.aisp.classifier.factory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.IClassifier;
import org.eng.aisp.classifier.IClassifierFactory;
import org.eng.aisp.util.JavaScriptFactories;
import org.eng.template.Template;
import org.eng.template.TemplateException;
import org.eng.util.FileUtils;

/**
 * Provides access to various classifier factories and builders.
 * @author dawood
 *
 */
public class ClassifierFactories extends JavaScriptFactories {
	
	
	private final static String PackageName = ClassifierFactories.class.getPackage().getName().replaceAll("\\.", "/");
	
//	public static IClassifier<double[]> newClassifier(String classifierSpec, String trainingLabel) throws AISPException {
//		
//	}

	/**
	 * A convenience on {@link #newBuiltInClassifier(String, Map)} without any overriding bindings.
	 * @param classifierSpec
	 * @return
	 * @throws AISPException
	 */
	public static IClassifier<double[]> newDefaultClassifier(String classifierSpec) throws AISPException {
		return newClassifier(classifierSpec, null);
	}

	/**
	 * Parse the specification to provide a classifier. 
	 * @param classifierSpec a factory specification in the form of &lt;type&gt;:&lt;type-specific builder content&gt;.
	 * The following types are supported
	 * <ul>
	 * <li> 'js'  builder content is a javascript string to be used by a JScriptClassifierFactory.
	 * <li> 'jsfile' builder content is the name of a file containing java script or java script template to be used by JScriptClassifierFactory.
	 * <li> empty then we fall over to {@link #newBuiltInClassifier(String, String)}.  See it for details. 
	 * </ul>
	 * @param bindings provides values to fill into java script templates if used to match the given classifier spec.  may be null.
	 * @return
	 * @throws AISPException if unrecognized classifierSpec is given.
	 * @see {@link JScriptClassifierFactory}
	 */
	public static IClassifier<double[]> newClassifier(String classifierSpec, Map<String,?> bindings) throws AISPException {
		int index = classifierSpec.indexOf(':');
		if (index >= 0) {
			String specType = classifierSpec.substring(0, index);
			String spec = classifierSpec.substring(index+1);
			IClassifierFactory<double[]> factory;
			if (specType.equals("jsfile")) { 
				File jsFile = new File(spec);
				if (!jsFile.exists()) 
					throw new AISPException("Could not find file " + jsFile);
				String text;
				InstanceSpecification is; 
				try {
					text = FileUtils.readTextFileIntoString(jsFile);
					is = createInstanceSpecification(spec, jsFile.getAbsolutePath(), text);
				} catch (Exception e) {
					throw new AISPException("Could not read JavaScript file/template at " + jsFile, e);
				}
				return buildClassifier(is,bindings);
			} else if (specType.equals("js")) {
				factory = new JScriptClassifierFactory<double[]>(spec);
			} else if (specType.equals("jst")) {
				factory = new JScriptClassifierFactory<double[]>(spec);
			} else {
				throw new AISPException("Unrecognized spec: " + specType);
			}
			return factory.build();
		} else {
			return newBuiltInClassifier(classifierSpec, bindings);
		}
	}


	/**
	 * A convenience on {@link #newBuiltInClassifier(String, Map)} w/o the overriding bindings.
	 */
	public static IClassifier<double[]> newDefaultBultInClassifier(String modelTypeName) throws AISPException {
		return newBuiltInClassifier(modelTypeName,null);
	}

	/**
	 * Convert a string and the name of a training label into an IClassifier instance.
	 * Use this instread of {@link #newClassifier(String, Map)} when you don't want to support the other reference types such as "jsfile:".
	 * Support type names are javascript file names found in the classpath in this package.
	 * See {@link #getModelSpecifications()} for the complete list. 
	 * @param modelTypeName
	 * @param bindings applied to the template if found for the give modelTypeName.  May be null.
	 * @return never null 
	 * @throws AISPException if unrecognized type name is given.
	 */
	public static IClassifier<double[]> newBuiltInClassifier(String modelTypeName, Map<String,? extends Object> bindings) throws AISPException {
		IClassifier<double[]> classifier = null;

		// Assume the spec is the name of a JavaScript file on the classpath.
		String jsResourceName = "/" + PackageName  + "/" + modelTypeName +  JAVASCRIPT_EXTENSION;
		InstanceSpecification modelSpec;
		try {
			modelSpec = readResourceAsJSInstanceSpec(modelTypeName, jsResourceName);
		} catch (IOException e1) {
			throw new AISPException("Could not parse/read " + jsResourceName);
		}
		if (modelSpec == null) {
			String yamljsResourceName = "/" + PackageName  + "/" + modelTypeName + YAML_JAVASCRIPT_EXTENSION; 
			try {
				modelSpec = readResourceAsJSInstanceSpec(modelTypeName, yamljsResourceName);
			} catch (IOException e) {
				throw new AISPException("Could not parse/read " + yamljsResourceName);
			}
		}
		if (modelSpec != null && modelSpec.getType().equals(InstanceSpecificationType.JavaScript)) 
			classifier = buildClassifier(modelSpec, bindings);

		if (classifier == null)
			throw new AISPException("Unrecognized classifier spec:" + modelTypeName);
		return classifier;
	}

	/**
	 * Create the classifier using the given modelSpec and optional bindings.
	 * Use the spec's embedded Template object to generate the JavaScript used to create the classifier.
	 * @param modelSpec
	 * @param bindings
	 * @return never null.
	 * @throws AISPException
	 */
	protected static IClassifier<double[]> buildClassifier(InstanceSpecification modelSpec, Map<String, ? extends Object> bindings) throws AISPException {
		IClassifier<double[]> classifier;
		Template template = modelSpec.getSpecification();
		String js;
		try {
			js = template.generate(bindings);
		} catch (TemplateException e) {
			throw new AISPException("Could not generate JavaScript from template read from " + modelSpec.getLocation() + ": " + e.getMessage(), e);
		}
		IClassifierFactory<double[]> factory = new JScriptClassifierFactory<double[]>(js);
		classifier = factory.build();
		return classifier;
	}
		

	/** A list of all base names of javascript files contained in the same package as this class's package. */
	private static Map<String,InstanceSpecification> InstanceSpecifications = null;
	  
	/**
	 * Get this list of all model specifications known to this factory.  
	 * This is the base names of all javascript files (ending with .js or .jsyt) found in the same package as this class.
	 * @return never null.  Basenames of javascript resources files in the same package as this class anywhere on the classpath.
	 */
	public static synchronized Map<String,InstanceSpecification> getModelSpecifications() {
		if (InstanceSpecifications == null) {
			InstanceSpecifications = JavaScriptFactories.getJSInstanceSpecifications(PackageName);
		}
		return InstanceSpecifications;
	} 

	
}


