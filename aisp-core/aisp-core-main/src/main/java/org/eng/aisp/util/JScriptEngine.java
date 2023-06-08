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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.eng.ENGLogger;
import org.eng.aisp.AISPException;
import org.eng.util.FileUtils;

import com.google.gson.Gson;

public class JScriptEngine {

	private final ScriptEngine engine;
//	private final List<String> allowedClasses;

	public JScriptEngine() {
//		this.allowedClasses = allowedClasses;
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
		bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		return (Map<String,Object>)toJava(bindings);

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

	private static final Gson gson = new Gson();
	
	/**
	 * Convert bindings returned by the script engine to a more native (and serializable) Java object,
	 * including objects nested in Map or List objects.
	 * @param scriptObj
	 * @return a Java object
	 */
	private static Object toJava(Object scriptObj) {
		if (scriptObj == null)
			return null;
		
		String className = scriptObj.getClass().getName();
//		ENGLogger.logger.info("class=" + className);

		if (className.equals("com.oracle.truffle.polyglot.PolyglotMap")) {
			// We need to convert the map to a serializable map, including its contents.
			// The PolygloMap was found to contain a Proxy$<N> object which contains another truffle class. Yuck!
			// So instead try to work with a json formatting of the map.
			// Sad but true, gson.toJson() throws an exception on missing class when trying to convert to json string.
			// The toString() seems to print a json-formatting of the map, so use that.
			String json = scriptObj.toString(); // gson.toJson(scriptObj);
			Object newObject = null;
			try {
				Map map = gson.fromJson(json, Map.class);
				newObject = convertMap(map);
			} catch (Exception e) { 
				;	// Not a map
			}
			if (newObject == null) {	// Try a List
				// Lists come through as "(size)[...]" and don't have any keys?
				int index = json.indexOf(")");
				if (index >= 0) {
					json = json.substring(index+1);
					try {
						newObject = gson.fromJson(json, List.class);
					} catch (Exception e) {
						;	// Not a list;
					}
				}
			}
			if (newObject != null)	// else probably fail later.
				scriptObj = newObject;
		} else if ( className.equals("com.oracle.truffle.js.scriptengine.GraalJSBindings")) {
			Map map = (Map)scriptObj; 
			Map<Object, Object> newMap = convertMap(map);
			scriptObj = newMap;
		} 
	    return scriptObj;
	}

	protected static Map<Object, Object> convertMap(Map map) {
		Map<Object,Object> newMap = new HashMap<>();
		for (Object key : map.keySet()) {
			Object value = map.get(key);
			Object newKey = toJava(key);
			Object newValue = toJava(value);
			newMap.put(newKey, newValue);
		}
		return newMap;
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
