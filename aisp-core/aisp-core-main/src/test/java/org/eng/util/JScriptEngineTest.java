package org.eng.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.eng.aisp.util.JScriptEngine;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;

public class JScriptEngineTest {
	private static Gson gson = new Gson();
	
	@Test
	public void testMapReturnType() throws ScriptException {
		String value = "{ \"a\" : 1.0, \"b\" : \"str\", \"c\" : { \"d\" : 2.0 } }";
		Class<Map> clazz = Map.class;
		variableTest(value, clazz);
	}

	@Test
	public void testListReturnType() throws ScriptException {
		String value = "[ 1, 2, 3 ]";
		Class<List> clazz = List.class;
		variableTest(value, clazz);
	}

	@Test
	public void testIntegerReturnType() throws ScriptException {
		String value = "3";
		Class<Integer> clazz = Integer.class;
		variableTest(value, clazz);
	}

	@Test
	public void testDoubleReturnType() throws ScriptException {
		String value = "3.1";
		Class<Double> clazz = Double.class;
		variableTest(value, clazz);
	}

	@Test
	public void testStringReturnType() throws ScriptException {
		String value = "\"string\"";
		Class<String> clazz = String.class;
		variableTest(value, clazz);
	}
	
	/**
	 * Assign the value in JavaScript, read it back and expect to get an object that is json-formattable to something
	 * that matches the json formatting 
	 * @param <TYPE>
	 * @param value javascript-formatted variable value.
	 * @param clazz which the given value should be json-parsable into.
	 * @throws ScriptException
	 */
	protected static <TYPE> void variableTest(String value, Class<TYPE> clazz) throws ScriptException {
		// Create an assignment statement using the given string value.
		String varName = "m";
		String script = "var " + varName + " = " + value; 
		JScriptEngine engine = new JScriptEngine();
		Map<String,Object> inputBindings = new HashMap<>();
		Map<String,Object> outputBindings = engine.runScript(script, inputBindings, false);
		
		// Retreive the value of the assigned object back.
		Assert.assertTrue(outputBindings != null);
		Object assignedValue = outputBindings.get(varName);
		Assert.assertTrue(assignedValue != null);

		TYPE expectedValue = gson.fromJson(value, clazz);
		Assert.assertTrue(expectedValue.equals(assignedValue));
//		String emJson = gson.toJson(expectedValue);
//		String jsObjJson = gson.toJson(assignedValue);
//		Assert.assertTrue(emJson.equals(jsObjJson));

		// This is not strictly necessary, but since we use JavaScript to create IClassifier instances, which
		// must be serializable and which are sometimes configured (python) with the javascript values placed
		// into them, we do this check also.
		Assert.assertTrue(assignedValue instanceof Serializable);
	}
}
