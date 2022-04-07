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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * Provides a search space for a set of properties identified by a component name.
 * The component name is used to define the names of environment and system variables
 * where the properties file is searched for.  The {@link #getPropertyURL()} is
 * the method that is responsible for the search order, which is defined as follows for 
 * an instance with name 'foo':
 * <ol>
 * <li> a file name specified by the environment variable with name <b>FOO_PROPERTIES_FILE</b>
 * <li> a file name specified by the system property <b>foo.properties.file</b> 	 
 * <li> a <b>foo.properties</b> file in the current directory.
 * <li> the path composed with the value of the <b>FOO_HOME</b> environment variable as follows:
 *    <ul>
 *    <li> $FOO_HOME/foo.properties
 *    <li> $FOO_HOME/lib/foo.properties
 *    </ul>
 * <li> foo.properties as a resource of the instance's ClassLoader.
 * </ol>
 * When requesting property values, properties are first searched for in the Java System
 * properties, and if not found there, then the properties file specified above is used.
 * This allows the associated component properties file to define the defaults, but have
 * them overridden on the command line via -Dproperty=value syntax.
 * @author dawood
 *
 */
public class ComponentProperties {
	
	
	/** The extension of the properties file */
	protected final static String PROPERTIES_FILE_EXTENSION = ".properties";
	
	/** Environment variable use to specify the location of the the installation directory, and thus
	 * a place to look for the properties file.
	 */
	protected final static String HOME_ENV_SUFFIX = "_HOME";
	
	/** Environment variable that can be used to specify the location of the properties file */
	protected final static String PROPERTIES_FILE_ENV_SUFFIX = "_PROPERTIES_FILE";
	

	private String userDefinedPropFileLocation = null;
	
	// Make this private since we set its value inside a sync.
	private Properties componentProperties = null;
	
	/** Lowercase name of component */
	protected final String componentName;
	
	/**
	 * Create a component with the given name.
	 * Name should not contain spaces or special characters other than '-', which 
	 * will be replaced with '_' in the name of the HOME environment variable name.
	 * @param componentName case-insensitive name of the component.
	 */
	public ComponentProperties(String componentName) {
		if (componentName == null)
			throw new NullPointerException("component name can not be null");
		this.componentName = componentName.toLowerCase();
	}
	
	/**
	 * Get name of the property file without any path information.
	 * @return never null.
	 */
	public String getPropertyFileName() {
		return componentName + PROPERTIES_FILE_EXTENSION;
	}
	
	/**
	 * Name of component.
	 * @return never null, always lower case.
	 */
	public String getComponentName() {
		return componentName;
	}
	
	/**
	 * Get the property whose name is built from the class name and the given suffix.
	 * The property name is created by concatenating the full dotted name of the 
	 * class, another dot, and the given name.  The String value for that system
	 * property as returned by {@link System#getProperty(String)} is returned.
	 * 
	 * @param clazz
	 * @param name
	 * @return the value of the property 
	 */
	public String getProperty(Class clazz, String name) {
		String propName = clazz.getName() + "." + name;
		String value =  System.getProperty(propName);
		if (value == null)
			value = getProperty(propName);
		return value;
	}
	
	/**
	 * Get the integer value of system or property  and if not found use the given default.
	 * The  property name is derived using the same mechanism as 
	 * {@link #getSystemProperty(Class, String)}.  We first check the system properties 
	 * and then search properties.
	 * 
	 * @param clazz
	 * @param name
	 * @param dflt
	 * @return
	 */
	public int getProperty(Class clazz, String name, int dflt) {
		String value = getProperty(clazz,name);
		if (value == null)
			return dflt;
		else
			return Integer.parseInt(value.trim());
	}
	

	
	/**
	 * Get the name of the system property that may be used to specify the 
	 * name of the properties file associated with this component instance.
	 * @return
	 */
	public String getFileSystemPropertyName() {
		return envToProperty(getFileEnvVarName());
	}
	
	/**
	 * Get the name of the environment variable that may be used to specify the 
	 * name of the properties file associated with this component instance.
	 * @return
	 */
	public String getFileEnvVarName() {
		return componentName.toUpperCase() + PROPERTIES_FILE_ENV_SUFFIX;
	}
	
	/**
	 * Get the name of the environment variable that may be used to specify the 
	 * home directory in which we look for the properties file for this instance.
	 * Special characters ('-') are replaced with '_'.
	 * @return
	 */
	public String getHomeEnvVarName() {
		return componentName.toUpperCase().replace("-","_") + HOME_ENV_SUFFIX;
	}
	
	/**
	 * Get the name of the environment variable that may be used to specify the 
	 * home directory in which we look for the properties file for this instance.
	 * @return
	 */
	public String getHomeSystemPropertyName() {
		return envToProperty(getHomeEnvVarName());
	}
	
	/**
	 * Get the name of the directory considered to be the home for this component.
	 * We search first the environment variable then the system property.
	 * @return null if not specified.
	 */
	public String getHomePath() {
		String home = System.getenv(getHomeEnvVarName());
		if (home == null)
			home = System.getProperties().getProperty(getHomeSystemPropertyName());
		return home;
	}

	/**
	 * Get the directory where the properties file is being read from and is known to exist.
	 * @return null if we can't find a directory containing the properties file.
	 */
	public File getPropertyFileDir() {
		File props = getPropertyFileInternal();
		if (props == null)
			return null;
		return props.getAbsoluteFile().getParentFile();
	}
	/**
	 * Look in the file system according to system properties and environment
	 * variables for the properties file.  If not found, return null.
	 * @return null or File.
	 */
	private File getPropertyFileInternal() {
		File f;
		String propertiesFileName;
		
		//  programmatically set location takes precedence
		propertiesFileName =  userDefinedPropFileLocation;
		if (propertiesFileName != null) {
			f = new File(propertiesFileName);
			if (f.exists()) {
				return f;
			} else {
				Util.logger.warning("User-defined properties file: ", propertiesFileName,
						" does not exist.");
			}
		}
		
		// Then look for an environment variable the specifics the location
		String appFileEnv = getFileEnvVarName();
		propertiesFileName = System.getenv(appFileEnv);
		if (propertiesFileName != null) {
			f =  new File(propertiesFileName);
			if (f.exists())
				return f;
		}
		
		// Then look for a system property.
		String sysProperty = envToProperty(appFileEnv);
		propertiesFileName = System.getProperties().getProperty(sysProperty);
		if (propertiesFileName != null) {
			f = new File(propertiesFileName);
			if (f.exists())
				return f;
		}

		// Then look in the current directory.
		String baseFileName = componentName + PROPERTIES_FILE_EXTENSION;
		f = new File(baseFileName);
		if (f.exists())
			return f;
		
		// Then look in the "home" directory for the  properties file (if it exists).
		String homeDir = getHomePath();
		if (homeDir != null) {
			propertiesFileName = homeDir + "/" + baseFileName;
			f = new File(propertiesFileName);
			if (f.exists())
				return f;
			propertiesFileName = homeDir + "/lib/" + baseFileName;
			f = new File(propertiesFileName);
			if (f.exists())
				return f;
		}
		return null;
	}
	
	/**
	 * Get the File object that references this component's properties file according
	 * the search strategy defined by this class (see above).
	 * @return never null
	 * @deprecated in favor of getPropertyURL()
	 */
	public File getPropertyFile() {
		File  f = getPropertyFileInternal();
		if (f == null)
			// Do this just so we don't return null.
			f = new File(getPropertyFileName());
		return f;
	}
	
	/**
	 * Get the full path to the properties file that will be used
	 * to resolved property look ups.
	 * @return
	 * @deprecated in favor of getPropertyURL()
	 */
	public synchronized String getPropertyFilePath() {
		File f = getPropertyFile();
		return f.getAbsolutePath();
	}
	
	/**
	 * Get a URL to the properties file.  First
	 * we search the file system according the class documentation, and if
	 * not found, then we use {@link ClassLoader#getResource(String)}.
	 * @return a URL or null if not found.
	 */
	public URL getPropertyURL() {
		File f = getPropertyFileInternal();
		URL url;
		if (f == null) {
			url = this.getClass().getClassLoader().getResource(getPropertyFileName());
		} else {
			try {
				url = f.toURL();
			} catch (MalformedURLException e) {
				url = null;
			}
		}
		return url;
	}

	/**
	 * Convert an environment variable name to a property name by 
	 * make lowercase and replacing _ with .  For example FOO_BAR_BAZ becomes foo.bar.baz.
	 * @param appFileEnv
	 * @return
	 */
	private static String envToProperty(String appFileEnv) {
		appFileEnv = appFileEnv.toLowerCase();
		appFileEnv = appFileEnv.replaceAll("_", ".");
		return appFileEnv;
	}
	


	/**
	 * Get the value of the given property (ala java.util.Properties) from the 
	 * properties file returned by {@link #getPropertyURL()}.
	 * @param propertyName
	 * @return null if the named property is not found.
	 */
	public String getProperty(String propertyName)  {
		return getProperty(propertyName, null);
	}
	
	/**
	 * Get the value of the given property (ala java.util.Properties) from the 
	 * properties file returned by {@link #getPropertyURL()} and if it doesn't
	 * exist, return the given default value.  If the property value can not
	 * be parsed to an int, then it a message is logged and the default is used.
	 * @param propertyName
	 * @param dflt value to return if the property does not exist.
	 * @return dflt if the named property is not found, otherwise the property value as in integer.
	 */
	public int getProperty(String propertyName, int dflt)  {
		String s = getProperty(propertyName, null);
		if (s != null) {
			try {
				dflt = Integer.parseInt(s);
			} catch (Throwable t) {
				Util.logger.severe("Value of property ", propertyName, ", ", s,
				", could not be parsed as an integer. Using given default value of ", dflt, " instead.");
			}
		}
		return dflt;
	}
	
	/**
	 * Get the value of the given property (ala java.util.Properties) from the 
	 * properties file returned by {@link #getPropertyURL()} and if it doesn't
	 * exist, return the given default value.  If the property value can not
	 * be parsed to a boolean, then it a message is logged and the default is used.
	 * @param propertyName
	 * @param dflt value to return if the property does not exist.
	 * @return dflt if the named property is not found, otherwise the property value as in boolean.
	 */
	public boolean getProperty(String propertyName, boolean dflt)  {
		String s = getProperty(propertyName, null);
		if (s != null) {
			try {
				dflt = Boolean.valueOf(s);
			} catch (Throwable t) {
				Util.logger.severe("Value of property ", propertyName, ", ", s,
				", could not be parsed as a boolean. Using given default value of ", dflt, " instead.");
			}
		}
		return dflt;
	}
	
	/**
	 * Parse the property value into a double and if not present or badly formatted, use the given
	 * default value.
	 * @param propertyName
	 * @param dfltValue
	 * @return
	 */
	public double getProperty(String propertyName, double dfltValue) {
		String v = this.getProperty(propertyName);
		if (v != null) {
			try {
				double d = Double.parseDouble(v);
				dfltValue = d;
			} catch (NumberFormatException e) {
				Util.logger.severe("Value '" + v + " of property " 
						+ propertyName + " could not be parsed to a double. Using default value " 
						+ dfltValue);
			}
		}
		return dfltValue;
		
	}
	/**
	 * Change the property and persist the change to the underlying properties file.
	 * @param propertyName
	 * @param value
	 */
	public synchronized void changeProperty(String propertyName, String value)  {
		changeProperty(propertyName, value, true);
	}

	/**
	 * Replace the value of the given property (ala java.util.Properties) from the 
	 * properties File.  This only works when the properties are coming from a writable
	 * file (and not a URL as in {@link #getPropertyURL()}.
	 * @param propertyName
	 * @param value
	 * @param persist if true, then persist the change to the underlying property file.
	 */
	public synchronized void changeProperty(String propertyName, String value, boolean persist)  {
		loadProperties();
		this.componentProperties.setProperty(propertyName, value);
		if (persist) 
			persistProperties();
	}

	/**
	 * @param persist
	 * @throws RuntimeException
	 */
	private void persistProperties() throws RuntimeException {
		File propFile = getPropertyFileInternal();
		if (propFile != null) {
			try {
				FileOutputStream fos = new FileOutputStream(propFile);
				componentProperties.store(fos, null);
				fos.close();
			} catch (Exception e) {
				throw new RuntimeException("Can not replace property in " + propFile.getAbsolutePath());
			}

		}
	}
	
	/**
	 * Look in the system properties for the named property and if doesn't exist
	 * then get it out of the component properties file identified by {@link #getPropertyURL()}.  
	 * If it doesn't exist in
	 * either, then returnt the given default value.
	 * @param propertyName
	 * @param dflt value to return if the property does not exist.
	 * @return dflt if the named property is not found, otherwise the property value.
	 */
	public synchronized String getProperty(String propertyName, String dflt)  {
		loadProperties();
//		String s = componentProperties.getProperty(propertyName);
//		if (s == null)
//			s = System.getProperty(propertyName, dflt);
		String s = System.getProperty(propertyName, null);
		if (s == null) {
			s = componentProperties.getProperty(propertyName);
			if (s == null)
				s = dflt;
		}
		
		return s;
	}
	
	/**
	 * Reload the properties from the properties file.
	 */
	public synchronized void reload() {
		componentProperties = null;
		loadProperties();
	}
	
	private void loadProperties() {
		if (componentProperties == null) {
			componentProperties = new Properties();
			URL propFile = getPropertyURL();
			if (propFile != null) {
					try {
						System.err.println("Loading " + componentName + " properties from " + propFile);
						InputStream stream = propFile.openStream();
						componentProperties.load(stream);
						stream.close();
					} catch (Exception e) {
						throw new RuntimeException("Can not read " + componentName + " properties file at " + propFile);
					}
			}
		}
	}
	
	/**
	 * Install the given properties as the properties to use for this component.  
	 * The properties are not persisted to the file system by this call.
	 * @param props
	 * @param logger optional logger that is using these properties and is to be notified of the change.
	 */
	public synchronized void setProperties(Properties props, ComponentLogger logger) {
		Util.logger.fine("Setting properties ");
		this.componentProperties = (Properties)props.clone();
		userDefinedPropFileLocation = null;
		if (logger != null)
			logger.propertiesChanged();
	}
	
	/**
	 * Set the location and name of the properties file overriding other environment
	 * and properties mechanisms of setting the location.  To revert to
	 * those mechanisms, set the location to 'null'.
	 * Whenever this is set, the previously loaded properties (if any) are discarded,
	 * so that the next call to {@link #getProperty(String)} will cause a
	 * new set or properties to be loaded.
	 * @param propFile filename or null.
	 * @param logger optional logger that is using these properties and is to be notified of the change.
	 */
	public synchronized void setPropertyFilePath(String propFile, ComponentLogger logger) {
		if (logger != null)
			logger.fine("Setting properties file location to " + propFile);
		
		if (propFile != null) {
			File f = new File(propFile);
			if (!f.exists()) {
				Util.logger.warning("User-defined properties file: ", propFile,
						" does not exist.");
				return;
			}
		}

		componentProperties = null;
		userDefinedPropFileLocation = propFile;
		if (logger != null)
			logger.propertiesChanged();	// This is bogus and should be done externally. // Force the logger to reread the log properties from this new location.
			
		// Do this again in case the earlier properties didn't log the above 
		// (it may have been a different logger properties).
		if (logger != null)
			logger.fine("Set properties file location to " + propFile);
	}

	/**
	 * @return the componentProperties
	 */
	public synchronized Properties getProperties() {
		loadProperties();
		return componentProperties;
	}
}
