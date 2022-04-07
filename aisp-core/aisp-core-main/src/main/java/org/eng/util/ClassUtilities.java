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
package org.eng.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author dawood
 *
 */
public class ClassUtilities {

	/**
	 * Clone the object using serialization.
	 * @param obj
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public Serializable clone(Serializable obj) throws IOException, ClassNotFoundException {
		return deserialize(serialize(obj));
	}
	private static List<List<Class>> getArgCombinations( List<Class> args) {
		List<List<Class>> argCombinations = new ArrayList<List<Class>>();
		getArgCombinations(args,0, argCombinations);
		return argCombinations;
	}
	private static void getArgCombinations( List<Class> args, int argIndex, List<List<Class>> argCombinations) {
		if (args.size() == argIndex)
			return;
		
		boolean firstCall = argCombinations.size() == 0;
		Class nextArg = args.get(argIndex);
		List<List<Class>> orig = new ArrayList<List<Class>>();
		orig.addAll(argCombinations);
		argCombinations.clear();
		Class[] superClasses = getAllSuperClassesAndInterfaces(nextArg);
		if (firstCall)	 {// First time here.
			for (Class c : superClasses) {
				List<Class> l = new ArrayList<Class>();
				l.add(c);
				argCombinations.add(l);
			}
		} else {
			for (List<Class> argList : orig) {
				for (Class c: superClasses) {
					List<Class> tmp = new ArrayList<Class>(argList);
					tmp.add(c);
					argCombinations.add(tmp);						
				}
			}
		}
	
		getArgCombinations(args, argIndex+1, argCombinations);
	
	}
	
	/**
	 * A convenience method which extracts the classes from
	 * the given args and calls {@link #getMethod(Class, String, Class...)}.
	 * @param clazz
	 * @param methodName
	 * @param args
	 * @return
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	@SuppressWarnings("unchecked")
	public static Method getMethod(Class clazz, String methodName, Object...args) throws 
											ClassNotFoundException, NoSuchMethodException, SecurityException{
		Class argTypes[] = null;
		if (args != null ) {
			argTypes = new Class[args.length];
			for (int i=0 ; i<args.length ; i++)
				argTypes[i] = args[i].getClass();
		}
		Method m = getMethod(clazz, methodName, argTypes);
		return m;
	}
	
	/**
	 * Find the named method in the given class that accepts the given arguments.
	 * This goes beyond the normal {@link Class#getMethod(String, Class...)} call in
	 * that it builds  argument list permutations based on the super classes and 
	 * interfaces of each of the arguments given here.  This is so that we can
	 * find a method that accepts the arguments given here when the arguments here
	 * are subclasses/interfaces of the those declared in the method signature.
	 * For example, if B is a subclass of A, but a method that takes an A is defined,
	 * the normal getMethod() will not return the method when passing in B.  
	 * @param clazz
	 * @param name
	 * @param args
	 * @return
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 */
	public static Method getMethod(Class clazz, String name, Class...args) throws SecurityException, NoSuchMethodException {
		if (args == null || args.length == 0) {
			return clazz.getMethod(name, (Class[])null);
		}
		
		int argCount = args.length;
		List<Class> argList = new ArrayList<Class>();
		for (Class c: args) argList.add(c);
		
		List<List<Class>> argCombinations = getArgCombinations(argList);
	
		Class[] newArgs = new Class[argCount];
		SecurityException se = null;
		for (List<Class> argCombo : argCombinations) {
			if (argCombo.size() != args.length)
				throw new RuntimeException("something is broken");
			argCombo.toArray(newArgs);
			try {
				Method m = clazz.getMethod(name,newArgs);
				return m;
			} catch (SecurityException e) {
				se = e;
			} catch (NoSuchMethodException e) {
				;
			}
		}
		if (se != null)
			throw se;
		else
			throw new NoSuchMethodException("Could not find method " + name + " in class " + clazz.getName());
		
	}
	public static Class[] getAllSuperClassesAndInterfaces(Class clazz) {
		List<Class> classList = new ArrayList<Class>();
		Stack<Class> classes = new Stack<Class>();
		classes.push(clazz);
		while (classes.size() != 0) {
			Class examining = classes.pop();
			classList.add(examining);
			Class c =examining.getSuperclass();
			if (c != null && !classList.contains(c))
				classes.push(c);
			Class[] interfaces = examining.getInterfaces();
			for (Class c2 : interfaces)
				if (!classList.contains(c2) )
					classes.push(c2);
		}
		Class[] r = new Class[classList.size()];
		return classList.toArray(r);
	}
	
	/**
	 * A convenience method over {@link #newInstance(Class)}.
	 * @param className
	 * @return
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static Object  newInstance(String className) throws ClassNotFoundException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Class<?> clazz = Class.forName(className);
		return newInstance(clazz);
	}
	
	/**
	 * Try and create an instance of the given class.
	 * We try the following ways to create the instance:
	 * <ul>
	 * <li> zero-arg constructor,
	 * <li> static <code>getInstance()</code> method.
	 * <li> static <code>instance()</code> method
	 * </ul>
	 * @param clazz
	 * @return an instance of the given class, never null.
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public static <T> T newInstance(Class<T> clazz) throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		T instance = null;
		
		// Try the zero-args constructor.
		try {
			instance = clazz.newInstance();
		} catch (IllegalAccessException e) {
			;
		} catch (InstantiationException e) {
			;
		}
		
		if (instance == null) {	// got exception above
			// Try the static instance creation methods.
			Method m = null;
//			NoSuchMethodException toThrow = null;
			SecurityException se = null;
			try {
				m = ClassUtilities.getMethod(clazz, "getInstance", (Class[]) null);
			} catch (SecurityException e1) {
				se = e1;
			} catch (NoSuchMethodException e1) {
				;
			}


			if (m == null) {
				try {
					m = ClassUtilities.getMethod(clazz, "instance",  (Class[])null);
				} catch (SecurityException e1) {
					se = e1;
				} catch (NoSuchMethodException e1) {
					;
				}
			}
			if (m == null) {
				if (se != null)
					throw se;
				throw new NoSuchMethodException("Could not find a zero-arg constructor or instance() or getInstance() methods");
			}
			instance = (T)m.invoke(null,  (Object[])null);
			if (instance == null) {
				throw new InvocationTargetException(new NullPointerException(), "Method " + m.getName() + " did not create the instance");
			}

		}

		return instance;
	}
	/**
	 * Serialize one or more objects into a byte array.
	 * @param o  objects to package.
	 * @return array of bytes that is the serialization of the input object(s), never null.
	 */
	public static byte[] serialize(Serializable o) throws IOException {
		ByteArrayOutputStream s = new ByteArrayOutputStream();
		serialize(s, o);
		s.close();
		byte bytes[] = s.toByteArray();
		return bytes;
	}

	/**
	 * Write the object to the file.
	 * @param fileName
	 * @param o 
	 * @throws IOException
	 */
	public static void serialize(String fileName, Serializable o) throws IOException {
		FileOutputStream fos = new FileOutputStream(fileName);
		serialize(fos,o);
		fos.close();
	}

	/**
	 * @param s
	 * @param o
	 * @throws IOException
	 */
	public static void serialize(OutputStream s, Serializable o) throws IOException {
		ObjectOutputStream os = new ObjectOutputStream(s);
		os.writeObject(o);
		os.flush();
	}
	
	/**
	 * Deserialize the bytes in the array into an object.  
	 * @param packet
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 */
	public static Serializable deserialize(byte[] b) throws IOException, ClassNotFoundException {
		return deserialize(b,0, b.length);
	}
	
	/**
	 * Deserialize file. 
	 * @param fileName
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 */
	public static Serializable deserialize(String fileName) throws IOException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream(fileName);
		Serializable s = deserialize(fis);
		fis.close();
		return s;
	}
	
	/**
	 * Deserialize the bytes in the array starting at the given offset and continuing for the given length, into an object.
	 * @param b
	 * @param offset
	 * @param length
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Serializable deserialize(byte[] b, int offset, int length) throws IOException, ClassNotFoundException {
		ByteArrayInputStream s = new ByteArrayInputStream(b, offset, length);
		return deserialize(s);
	}
	
	public static Serializable deserialize(InputStream stream) throws IOException, ClassNotFoundException {
		ObjectInputStream os = new ObjectInputStream(stream);
		Serializable o;
		o = (Serializable) os.readObject();
		return o;
	}
	
	/**
	 * Determine if the given right-hand-side class can be assigned to the given left-hand side class.
	 * @param lhsClassName
	 * @param rhsClassName
	 * @return
	 * @throws ClassNotFoundException
	 */
	public static boolean classMatch(String lhsClassName, String rhsClassName) throws ClassNotFoundException {
		if (rhsClassName.equals(lhsClassName))
			return true;

		Class lhsClass = Class.forName(lhsClassName);
		
		Class rhsClass = Class.forName(rhsClassName);
		return classMatch(lhsClass, rhsClass);

	}
	
	/**
	 * Determine if the given right-hand-side class can be assigned to the given left-hand side class.
	 * @param lhsClass
	 * @param rhsClass
	 * @return
	 * @throws ClassNotFoundException
	 */
	public static boolean classMatch(Class lhsClass, Class rhsClass) throws ClassNotFoundException {
		return lhsClass.isAssignableFrom(rhsClass);

	}
	
	/**
	 * Use reflection to get the value of the field or the zero-args method call.
	 * @param obj
	 * @param member
	 * @return null if the member is not found.
	 * @throws IllegalAccessException 
	 * @throws NoSuchFieldException 
	 * @throws SecurityException 
	 */
	public static Object getMemberValue(Object obj, String member) throws IllegalAccessException, SecurityException, NoSuchFieldException {
		Class clazz = obj.getClass();
		Field field = clazz.getField(member);
		try {
			return field.get(obj);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return null;
		}

	}
	
	/**
	 * Find the method with the given name and arguments from the given named class.
	 * @param className
	 * @param methodName
	 * @param argTypes
	 * @return
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	@SuppressWarnings("unchecked")
	public static Method getMethod(String className, String methodName, Class...argTypes) throws 
											ClassNotFoundException, NoSuchMethodException, SecurityException{
		Class clazz = Class.forName(className);
		Method m = clazz.getMethod(methodName, argTypes);
		return m;
	}

	/**
	 * Use de/serialization to make a copy/clone of the given object. 
	 * @param <TYPE>
	 * @param t object to copy.
	 * @return never null.
	 * @throws IOException
	 */
	public static <TYPE extends Serializable> TYPE copy(TYPE t) throws IOException {
		byte[] serialization = ClassUtilities.serialize(t);
		try {
			t = (TYPE) ClassUtilities.deserialize(serialization);
		} catch (ClassNotFoundException e) {
			// Should not be able to get here.
			e.printStackTrace();
			return null;
		}
		return t;
	}
	


}
