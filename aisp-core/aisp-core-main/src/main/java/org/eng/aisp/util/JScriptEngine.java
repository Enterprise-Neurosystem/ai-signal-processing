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
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLogger;
import org.eng.util.FileUtils;

import com.google.gson.Gson;

public class JScriptEngine {

	private final ScriptEngine engine;
//	private final List<String> allowedClasses;

	/**
	 * @param allowedClasses
	 * @deprecated in favor of {@link JScriptEngine#JScriptEngine()} since we don't support this and haven't for some time.
	 */
	public JScriptEngine(List<String> allowedClasses) {
		this();
	}

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
//			engine = sem.getEngineByName("JavaScript");	// The graal engine (not graal.js apparently).
			engine = sem.getEngineByName("graal.js");	// The graal engine (not graal.js apparently).
			Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
			bindings.put("polyglot.js.allowHostAccess", true);
			bindings.put("polyglot.js.allowHostClassLookup", (Predicate<String>) s -> true);
//		}
	}
	



	public Map<String,Object> runScript(File jsFile, Map<String, Object> bindings, boolean clearBindings) throws ScriptException, IOException {
		String source = FileUtils.readTextFileIntoString(jsFile.getAbsolutePath());
		return runScript(source, bindings, clearBindings);
	}

	/**
	 * A convenience on {@link #runScript(String, Map, boolean, boolean)} with conversion of bindings to Serializable.
	 * @param source
	 * @param bindings
	 * @param clearBindings
	 * @return
	 * @throws ScriptException
	 */
	public Map<String,Object> runScript(String source, Map<String, Object> bindings, boolean clearBindings) throws ScriptException {
		return this.runScript(source, bindings, clearBindings, true);
	}

	public Map<String,Object> runScript(String source, Map<String, Object> bindings, boolean clearBindings, boolean convertToSerializable) throws ScriptException {
//		System.out.println("executing js source:" + source);
		bindings = runScriptForBindings(source, bindings, clearBindings);
		if (convertToSerializable)
			bindings = (Map<String,Object>)makeSerializable(bindings);
		return bindings;

	}


	private Map<String, Object> runScriptForBindings(String source, Map<String, Object> bindings,
			boolean clearBindings) throws ScriptException {
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
		
		// For Graal the values returned here may be instances of PolyglotMap. 
		bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

		// Extract the real Java objects (i.e. avoid PolyglotMap). 
		Map<String,Object> map = new HashMap<>();
		for (String key : bindings.keySet()) {
			// This get() seems to convert/get the actual underlying Java object created in Javascript,
			// which is otherwise a PolyglotMap which we don't want to return as a value in the returned map.
			Object obj = bindings.get(key);
			map.put(key, obj);
		}
		return map;
	}
	
	/**
	 * Find an instance of the given class in the given map.
	 * @param bindings
	 * @param klass class of object returned. 
	 * @return null if not found.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T getInstance(Map<String, Object> bindings, Class<T> klass, boolean convertToSerializable) {
		T value = null;
		for (String key : bindings.keySet()) {
			Object o = bindings.get(key);
			value = getTypeMatchedInstance(o, klass, convertToSerializable);
			if (value != null)
				return value;
		}
		return null;
	}

	private static final Gson gson = new Gson();
	

	/**
	 * Convert bindings returned by the script engine to a more native (and serializable) Java object,
	 * including objects nested in Map or List objects.
	 * @param scriptObj
	 * @return a Java object
	 * @throws ScriptException 
	 */
	private static Object makeSerializable(Object scriptObj) throws ScriptException {
		if (scriptObj == null) 
			return null;
		
		String className = scriptObj.getClass().getName();
//		ENGLogger.logger.info("class=" + className);

		if (className.equals("com.oracle.truffle.polyglot.PolyglotMap")) {
			Object newObject = tryPolyglotAsList(scriptObj); 
			if (newObject == null)
				newObject = polyglotAsMap(scriptObj);
			if (newObject != null)	// else probably fail later.
				scriptObj = newObject;
			else
				AISPLogger.logger.warning("Could not convert object of class " + className);
//		} else if ( className.equals("com.oracle.truffle.polyglogObjectProxyHandler")) {
//			AISPLogger.logger.info("scriptObj=" + scriptObj.toString());
		} else if ( className.equals("com.oracle.truffle.js.scriptengine.GraalJSBindings")) {
			scriptObj = makeMapSerializable((Map)scriptObj);
		}  else if (scriptObj instanceof Map) {	// Make sure all values are also java objects. 
			scriptObj = makeMapSerializable((Map)scriptObj);
		}
		
		if (!(scriptObj instanceof Serializable))
			throw new ScriptException("Object of class " + scriptObj.getClass().getName() 
					+ " could not be converted to Serializable");
		
	    return scriptObj;
	}

	/**
	 *  Graal toString for lists seems to be (%d)[<items>].
	 *  This pattern matches the (%d)[ part
	 */
	private static Pattern listValuePattern = Pattern.compile("^\\(\\d+\\)\\[");

	private static List<?> tryPolyglotAsList(Object scriptObj) throws ScriptException {
		List<?> newObject = null;
		String value = scriptObj.toString();
		Matcher m = listValuePattern.matcher(value);
		if (m.find()) {
			value = m.replaceAll("[");
			// If it is a list of non-primitives, in particular Java objects, they are not serialized with our toJson().
			// toString() produces somethign like the following when this is the case.
			// (5)[JavaObject[org.eng.aisp.classifier.gmm.GMMClassifierBuilder], 
			// for an eventual answer, hopefully.
			// Note that if we use Bindings.get(<varname>) the actual object does come back, but not here when we don't have and engine instance.
			// See runScriptForBindings() for usage the extracts the Java object created in JavaScript.
			// Also note, that the same thing would happen in polyglotAsMap(), its just that so far our scripts have not put our Java objects in maps. 
			// For now we don't see a way to support this.
			// See https://stackoverflow.com/questions/76466839/how-do-i-convert-objects-created-in-graal-javascript-to-serializable-java-object
			String json; 
			if (value.contains("JavaObject")) {
				throw new ScriptException("Can not convert Java objects");
			} else {
				json = toJson(scriptObj);
			}
			newObject = gson.fromJson(json, List.class);
		}
		return newObject;
	}


	
	/**
	 * Convert the given script-created object to a json string.
	 * The implementation does NOT use Gson since its toJson() methods throws a ClassNotFoundException on ThreadMXBean class.
	 * Instead, use JavaScript's JSON.stringify() to convert object to json string. 
	 * @param scriptObj
	 * @return never null
	 * @throws ScriptException
	 */
	private static String toJson(Object scriptObj) throws ScriptException {
		JScriptEngine engine = new JScriptEngine();
		String script = "var json = JSON.stringify(obj)";
		Map<String,Object> bindings = new HashMap<>();
		bindings.put("obj", scriptObj);
		Map<String,Object> result = engine.runScriptForBindings(script, bindings, false);
		Object json = result.get("json");
		json = json.toString();
		return (String)json;
	}
	
	/**
	 * Try and convert the given PolyglotMap instance to a Map.
	 * @param polyglot
	 * @return null if could not be converted.
	 * @throws ScriptException 
	 */
	private static Map polyglotAsMap(Object polyglot) throws ScriptException {
		// We need to convert the map to a serializable map, including its contents.
		// The PolygloMap was found to contain a Proxy$<N> object which contains another truffle class. Yuck!
		// So try to work with a json formatting of the map.
		// Sad but true, gson.toJson() throws an exception on missing class when trying to convert to json string.
		// The toString() seems to print a json-formatting of the map, so use that.
		Map newObject = null;
		String json; 

		// This seems to help use avoid treating polyglot object as Map below, when values are Proxy$ instances.
		// Leaving values as $Proxy is bad since they don't seem to be serializable.
		try {
			json = toJson(polyglot);
			Map map = gson.fromJson(json, Map.class);
			newObject = makeMapSerializable(map);
			return newObject;
		} catch (Exception e) { 
			;
		}

		// Try this last 
		if (newObject == null && polyglot instanceof Map) 
			newObject = makeMapSerializable((Map)polyglot); 
		return newObject;
	}
	
	
	private static Map<Object, Object> makeMapSerializable(Map map) throws ScriptException {
		Map<Object,Object> newMap = new HashMap<>();
		for (Object key : map.keySet()) {
			Object value = map.get(key);
			Object newKey = makeSerializable(key);
			Object newValue = makeSerializable(value);
			newMap.put(newKey, newValue);
		}
		return newMap;
	}
	
	/**
	 * A convenience on {@link #getTypeMatchedInstance(Object, Class, boolean)} with serializability conversion.
	 * @param <T>
	 * @param jsObj
	 * @param klass
	 * @return
	 * @deprecated in favor of {@link #getTypeMatchedInstance(Object, Class, boolean)}
	 */
	@SuppressWarnings({ "restriction", "unchecked" })
	public static <T> T getTypeMatchedInstance(Object jsObj, Class<T> klass) {
		return getTypeMatchedInstance(jsObj, klass, true);
	}

	/**
	 * Try and extract an instance of the given class from the given object retrieved from JavaScript bindings.
	 * @param <T>
	 * @param jsObj object retrieved from JavaScript bindings
	 * @param klass class to convert the object to and return an instance of.
	 * @param convertToSerializable if true, then make sure the obj value is serializable or fail in the conversion and return null.
	 * @return null if object could not be converted to an instance of the given class, serializability is requested but could not be converted, or is given as null.
	 */
	@SuppressWarnings({ "restriction", "unchecked" })
	public static <T> T getTypeMatchedInstance(Object jsObj, Class<T> klass, boolean convertToSerializable) {
		if (jsObj == null)
			return null;
		
		if (convertToSerializable) {
			try {
				jsObj = makeSerializable(jsObj);
			} catch (ScriptException e) {
				return null;
			}
		}
		
		if (klass.isAssignableFrom(jsObj.getClass()))  {
			return (T)jsObj;
		} else  {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getScriptVariable(Class<T> klass, String varName) throws AISPException {
		return getScriptVariable(klass, varName, true);
	}
		
	/**
	 * Get the instance of the given class and optionally with the given name from the main script's bindings.
	 * @param klass class of instance expected.
	 * @param varName optional name of variable.
	 * @return null if not found. 
	 * @throws AISPException found, but not of the requested type. 
	 */
	@SuppressWarnings("unchecked")
	public <T> T getScriptVariable(Class<T> klass, String varName, boolean convertToSerializable) throws AISPException {
		Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		T value;
		if (varName != null) {
			Object obj = bindings.get(varName);
			value = getTypeMatchedInstance(obj, klass, convertToSerializable);
			if (value == null) 
				throw new AISPException("Variable with name '" + varName + "' is not of the expected type (" + klass.getName() 
				+ ") or can not be converted.");
		} else {
			// Search all bindings
			value = getInstance(bindings, klass, convertToSerializable);
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
	public <T> T getScriptVariable(Class<T> klass, String varName, T dflt, boolean convertToSerializable) throws AISPException {
		T value = getScriptVariable(klass, varName, convertToSerializable);

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
