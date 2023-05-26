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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.eng.aisp.AISPException;
import org.eng.util.FileUtils;

public class JScriptEngine {

	private final ScriptEngine engine;
	private final List<String> allowedClasses;

	public JScriptEngine(List<String> allowedClasses) {
		this.allowedClasses = allowedClasses;
//		if (allowedClasses != null) {
//			engine = createSecuredEngine(allowedClasses);
//			if (engine == null && requireSecurity) 
//				throw new IllegalArgumentException("Could not enable security");
//		} else {
			System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");	// Turn off graal interpreter warnings below
//			[To redirect Truffle log output to a file use one of the following options:
//			* '--log.file=<path>' if the option is passed using a guest language launcher.
//			* '-Dpolyglot.log.file=<path>' if the option is passed using the host Java launcher.
//			* Configure logging using the polyglot embedding API.]
//			[engine] WARNING: The polyglot context is using an implementation that does not support runtime compilation.
//			The guest application code will therefore be executed in interpreted mode only.
//			Execution only in interpreted mode will strongly impact the guest application performance.
//			For more information on using GraalVM see https://www.graalvm.org/java/quickstart/.
//			To disable this warning the '--engine.WarnInterpreterOnly=false' option or use the '-Dpolyglot.engine.WarnInterpreterOnly=false' system property.
			ScriptEngineManager sem= new ScriptEngineManager();
//			List<ScriptEngineFactory> sef = sem.getEngineFactories();
			engine = sem.getEngineByName("JavaScript");	// The graal engine (not graal.js apparently).
			Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
			bindings.put("polyglot.js.allowHostAccess", true);
			bindings.put("polyglot.js.allowHostClassLookup", (Predicate<String>) s -> true);
//		}
	}
	
//	private final static String SecureEngineFactoryClass = "jdk.nashorn.api.scripting.NashornScriptEngineFactory";
//  See https://docs.oracle.com/javase/8/docs/technotes/guides/scripting/nashorn/api.html
//	private static ScriptEngine createSecuredEngine(List<String> allowedClasses) {
//		try { 
////			NashornScriptEngineFactory factory = new NashornScriptEngineFactory(); 
//			Class klazz = Class.forName(SecureEngineFactoryClass);
//			Object obj = klazz.newInstance();
//			if (obj instanceof ScriptEngineFactory){
//				ScriptEngineFactory factory = (ScriptEngineFactory)obj;
//				factory.
//				ScriptEngine engine = factory.getScriptEngine(
//		    	      new MyClassFilterTest.MyCF());
//		} catch (Exception e) {
//			AISPLogger.logger.severe("Could not load secure engine class " + SecureEngineFactoryClass 
//					+ ". Restricting access to following classes is not available: " + allowedClasses);
//			e.printStackTrace();
//		}
//		return null;
//		
//	}

	public Map<String,Object> runScript(File jsFile, Map<String, Object> bindings, boolean clearBindings) throws ScriptException, IOException {
		String source = FileUtils.readTextFileIntoString(jsFile.getAbsolutePath());
		return runScript(source, bindings, clearBindings);
	}

	public Map<String,Object> runScript(String source, Map<String, Object> bindings, boolean clearBindings) throws ScriptException {
//		System.out.println("executing js source:" + source);
		Bindings engineBindings; 
		if (clearBindings) 
			engineBindings = engine.createBindings(); 
		else
			engineBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

		if (bindings != null) { 
			for (String key : bindings.keySet()) 
				engineBindings.put(key, bindings.get(key));
		}
		engine.setBindings(engineBindings, ScriptContext.ENGINE_SCOPE);
		engine.eval(source);
		return engine.getBindings(ScriptContext.ENGINE_SCOPE);
	}
	
	/**
	 * Find an instance of the given class in the given map.
	 * @param bindings
	 * @param klass class of object returned. 
	 * @return null if not found.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T getInstance(Map<String, Object> bindings, Class<T> klass) {
		T value = null;
		for (String key : bindings.keySet()) {
			Object o = bindings.get(key);
			value = getTypeMatchedInstance(o, klass);
			if (value != null)
//			if (o != null && klass.isAssignableFrom(o.getClass())) {
//				value = (T)o;
				break;
//			}
		}
		return value;
	}

	/**
	 * Convert a Nashorn ScriptObjectMirror to a more native (and serializable) Java object,
	 * including objects nested in Map or List objects.
	 * @param scriptObj
	 * @return a Java object
	 */
	private static Object toJava(Object scriptObj) {
		if (scriptObj == null)
			return null;
		
//	    if (scriptObj instanceof jdk.nashorn.api.scripting.ScriptObjectMirror) {
//	    	jdk.nashorn.api.scripting.ScriptObjectMirror scriptObjectMirror = (jdk.nashorn.api.scripting.ScriptObjectMirror) scriptObj;
//	        if (scriptObjectMirror.isArray()) {
//	            List<Object> list = new ArrayList<Object>(); 
//	            for (Map.Entry<String, Object> entry : scriptObjectMirror.entrySet()) {
//	                list.add(toJava(entry.getValue()));
//	            }
//	            return list;
//	        } else {
//	            Map<String, Object> map = new HashMap<String,Object>(); 
//	            for (Map.Entry<String, Object> entry : scriptObjectMirror.entrySet()) {
//	                map.put(entry.getKey(), toJava(entry.getValue()));
//	            }
//	            return map;
//	        }
//	    } else {
	        return scriptObj;
//	    }
	}
	
	/**
	 * Try and extract an instance of the given class from the given object retrieved from JavaScript bindings.
	 * @param <T>
	 * @param jsObj object retrieved from JavaScript bindings
	 * @param klass class to convert the object to and return an instance of.
	 * @return null if object could not be converted to an instance of the given class or is given as null.
	 */
	@SuppressWarnings({ "restriction", "unchecked" })
	public static <T> T getTypeMatchedInstance(Object jsObj, Class<T> klass) {
		if (jsObj == null)
			return null;
		
		jsObj = toJava(jsObj);
		
//		if (Map.class.isAssignableFrom(klass)) {
//			// Lists/arrays come back as ScriptMirrorObject in Nashorn.  Try and parse them out.
//			if (!(jsObj instanceof jdk.nashorn.api.scripting.ScriptObjectMirror))
//				return null;
//			jdk.nashorn.api.scripting.ScriptObjectMirror mirror = (jdk.nashorn.api.scripting.ScriptObjectMirror)jsObj;
//			@SuppressWarnings("rawtypes")
//			Map<Object , Object>  vlist = mirror.to(Map.class); 
//
////			Map<Object , Object>  vlist = new HashMap<Object,Object>(); 
////			for (Object key : mirror.keySet()) {
////				Object value = mirror.get(key);
//////				value = convertBestMatch(value);
////				vlist.put(key, value);
////			}
//			return (T)vlist;
//		} else if (List.class.isAssignableFrom(klass)) {
//			// Lists/arrays come back as ScriptMirrorObject in Nashorn.  Try and parse them out.
//			if (!(jsObj instanceof jdk.nashorn.api.scripting.ScriptObjectMirror))
//				return null;
//			jdk.nashorn.api.scripting.ScriptObjectMirror mirror = (jdk.nashorn.api.scripting.ScriptObjectMirror)jsObj;
//			if (!mirror.isArray())
//				return null;
//			@SuppressWarnings("rawtypes")
//			List vlist = mirror.to(List.class); 
//			return (T)vlist;
//		} else 
			if (klass.isAssignableFrom(jsObj.getClass()))  {
			return (T)jsObj;
		} else  {
			return null;
		}
	}
		
	/**
	 * Get the instance of the given class and optionally with the given name from the main script's bindings.
	 * @param klass class of instance expected.
	 * @param varName optional name of variable.
	 * @return null if not found. 
	 * @throws AISPException found, but not of the requested type. 
	 */
	@SuppressWarnings("unchecked")
	public <T> T getScriptVariable(Class<T> klass, String varName) throws AISPException {
		Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		T value;
		if (varName != null) {
			Object obj = bindings.get(varName);
			value = getTypeMatchedInstance(obj, klass);
			if (value == null) 
				throw new AISPException("Variable with name '" + varName + "' is not of the expected type (" + klass.getName() 
				+ ") or can not be converted.");
		} else {
			// Search all bindings
			value = getInstance(bindings, klass);
		}
		return (T)value;
	}

	/**
	 * Get the instance of the given class and optionally with the given name from the main script's bindings.
	 * @param klass class of instance expected.
	 * @param varName optional name of variable.
	 * @param dflt the default value to return if no variable was found matching the request. 
	 * @return the value from the script or the default value.
	 */
	public <T> T getScriptVariable(Class<T> klass, String varName, T dflt) throws AISPException {
		T value = getScriptVariable(klass, varName);

		// Check to see if we should use the default value given.
		if (value == null) {
			if (dflt == null) {	// Never return null
				return dflt;
//				if (varName != null) 
//					throw new DescriptorException("Variable with name '" + varName + "' not found.");
//				else
//					throw new DescriptorException(
//						"Variable with class " + klass.getName() + " was not found in script.");
			} else {
				value = dflt;
			}
		}
		return (T)value;
				
	}

}
