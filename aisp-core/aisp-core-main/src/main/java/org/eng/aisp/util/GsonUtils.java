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

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.AISPException;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GsonUtils {

	private static class InterfaceSerializer<T> implements JsonSerializer<T> {
	    public JsonElement serialize(T link, Type type, JsonSerializationContext context) {
	        // Odd Gson quirk
	        // not smart enough to use the actual type rather than the interface
	        return context.serialize(link, link.getClass());
	    }
	}
	
	/**
	 *  Used to fix a problem with KNNVectorSummarizer classifier which mistakenly duplicates a superclass field and 
	 *  makes it so GSON will not serialize.
	 */
	public static class SuperclassExclusionStrategy implements ExclusionStrategy
	{
	    public boolean shouldSkipClass(Class<?> arg0)
	    {
	        return false;
	    }

	    public boolean shouldSkipField(FieldAttributes fieldAttributes)
	    {
	        String fieldName = fieldAttributes.getName();
	        Class<?> theClass = fieldAttributes.getDeclaringClass();

	        return isFieldInSuperclass(theClass, fieldName);            
	    }

	    private boolean isFieldInSuperclass(Class<?> subclass, String fieldName)
	    {
//	    	System.out.println("field=" + subclass + "." + fieldName);
	        Class<?> superclass = subclass.getSuperclass();
	        Field field;
//	        if (subclass.getSimpleName().contains("KNNVectorSummarizer") && fieldName.equals("enableOutlierDetection"))
//	        	fieldName = "enableOutlierDetection";
	        while(superclass != null)
	        {   
	            field = getField(superclass, fieldName);

	            if(field != null)
	                return true;

	            superclass = superclass.getSuperclass();
	        }

	        return false;
	    }

	    private Field getField(Class<?> theClass, String fieldName)
	    {
	        try
	        {
	            return theClass.getDeclaredField(fieldName);
	        }
	        catch(Exception e)
	        {
	            return null;
	        }
	    }
	}
	
	public static GsonBuilder getGsonInterfaceSerializer(List<Class<?>> interfaces, boolean pretty) {
		GsonBuilder builder = new GsonBuilder();
		if (pretty)
			builder.setPrettyPrinting();

		JsonSerializer<?> serializer = new InterfaceSerializer<>();
		
		for (Class<?> c : interfaces) {
			builder.registerTypeAdapter(c, serializer) 
				.addSerializationExclusionStrategy(new SuperclassExclusionStrategy())
				.addDeserializationExclusionStrategy(new SuperclassExclusionStrategy())
				.serializeSpecialFloatingPointValues()
				;
		}
		return builder;
	}

	public static GsonBuilder getInterfaceSerializer(boolean pretty) {
		// Build a list of interfaces so we can create type adapters for Gson to properly serialize 
		String regex = AISPException.class.getPackage().getName() +  ".*/I.*";
		List<String> classNames = ClassFinder.findClassNames(regex);

		List<Class<?>> klasses =  new ArrayList<Class<?>>();
		for (String name : classNames) {
			try {
				name = name.replaceAll("/", ".");
				Class<?> c = Class.forName(name);
				klasses.add(c);
			} catch (ClassNotFoundException e) {
				;
			}
		}
		return getGsonInterfaceSerializer(klasses, pretty);
	}
	
//	public 
//	Gson gson = new GsonBuilder().setPrettyPrinting()
//			.registerTypeAdapter(IClassifier.class, new InterfaceSerializer<>())
//			.registerTypeAdapter(IFixableClassifier.class, new InterfaceSerializer<>())
//			.registerTypeAdapter(IFixedClassifier.class, new InterfaceSerializer<>())
//			.registerTypeAdapter(IFeatureGramDescriptor.class, new InterfaceSerializer<>())
//			.registerTypeAdapter(IFeatureExtractor.class, new InterfaceSerializer<>())
//			.registerTypeAdapter(IFeatureProcessor.class, new InterfaceSerializer<>())
//			.registerTypeAdapter(IDistanceFunction.class, new InterfaceSerializer<>())
//			.registerTypeAdapter(INearestNeighborFunction.class, new InterfaceSerializer<>())
//			.serializeSpecialFloatingPointValues()
//			.create();
}
