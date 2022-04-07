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
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
/**
 * Extends the Java logger with varargs methods and other utilities.
 * The general model is to have an instance of this per package (or sub-package
 * hierarchy) and use that logger in the package logging.  The name
 * of such loggers should generally be children of the root logger, 
 * by giving the new logger a name that extends the name of the root
 * logger.  
 * <p>
 * If the control allowed by hierarchical loggers is not required,
 * then simply using the root logger is allowed.
 * <p>
 * The logger is initialized automatically within the library, via
 * {@link LibraryInitializer#Initialize()}.  Logging can be enabled
 * by setting the associated component property {@value #LOGGING_ENABLED_PROPERTY}
 * to true.  
 * 
 * @author dawood
 *
 */
public abstract class ComponentLogger  {//   extends Logger {
	
//	static { LibraryInitializer.Initialize(); }


	public static final String LOGGING_ENABLED_PROPERTY = "logging.enabled";
	private static final String unknown = "unknown";

	public void severe(String s) { this.severe(1,s); }
	public void severe(Object...o) { this.severe(1,o); }

	/**
	 * Log a message using Level.SEVERE level, if enabled.
	 * @param o
	 */
	public void severe(int extraStackDepth, Object... o) {
		initialize();
		if (!logger.isLoggable(Level.SEVERE))
			return;
		logObjects(extraStackDepth, Level.SEVERE, o );
	}

//	@Override
	public void severe(int extraStackDepth, String s) {
		initialize();
		if (!logger.isLoggable(Level.SEVERE))
			return;
		logObjects(extraStackDepth, Level.SEVERE, new Object[]{ s } );
	}

	public void warning(String s) { this.warning(1,s); }
	public void warning(Object...o) { this.warning(1,o); }
	/**
	 * Log a message using Level.WARNING level, if enabled.
	 * @param o
	 */
	public void warning(int extraStackDepth, Object... o) {
		initialize();
		if (!logger.isLoggable(Level.WARNING))
			return;
		logObjects(extraStackDepth, Level.WARNING, o );
	}

//	@Override
	public void warning(int extraStackDepth, String s) {
		initialize();
		if (!logger.isLoggable(Level.WARNING))
			return;
		logObjects(extraStackDepth,  Level.WARNING, new Object[]{ s } );
	}

	public void info(String s) { this.info(1,s); }
	public void info(Object...o) { this.info(1,o); }
	/**
	 * Log a message using Level.INFO level, if enabled.
	 * @param o
	 */
	private void info(int extraStackDepth, Object... o) {
		initialize();
		if (!logger.isLoggable(Level.INFO))
			return;
		logObjects(extraStackDepth, Level.INFO, o );
	}


	private void info(int extraStackDepth, String s) {
		initialize();
		if (!logger.isLoggable(Level.INFO))
			return;
		logObjects(extraStackDepth, Level.INFO, new Object[]{ s });
	}

	public void fine(String s) { this.fine(1,s); }
	public void fine(Object...o) { this.fine(1,o); }
	/**
	 * Log a message using Level.FINE level, if enabled.
	 * @param o
	 */
	private void fine(int extraStackDepth, Object... o) {
		initialize();
		if (!logger.isLoggable(Level.FINE))
			return;
		logObjects(extraStackDepth, Level.FINE, o );
	}


//	@Override
	private void fine(int extraStackDepth, String s) {
		initialize();
		if (!logger.isLoggable(Level.FINE))
			return;
		logObjects(extraStackDepth, Level.FINE, new Object[] { s } );
	}

	public void finer(String s) { this.finer(1,s); }
	public void finer(Object...o) { this.finer(1,o); }

	/**
	 * Log a message using Level.FINER level, if enabled.
	 * @param o
	 */
	private void finer(int extraStackDepth, Object... o) {
		initialize();
		if (!logger.isLoggable(Level.FINER))
			return;
		logObjects(extraStackDepth, Level.FINER, o );
	}

	private void finer(int extraStackDepth, String s) {
		initialize();
		if (!logger.isLoggable(Level.FINER))
			return;
		logObjects(extraStackDepth,  Level.FINER, new Object[]{ s } );
	}

	public void finest(String s) { this.finest(1,s); }
	public void finest(Object...o) { this.finest(1,o); }
	/**
	 * Log a message using Level.FINEST level, if enabled.
	 * @param o
	 */
	private void finest(int extraStackDepth, Object... o) {
		initialize();
		if (!logger.isLoggable(Level.FINEST))
			return;
		logObjects(extraStackDepth, Level.FINEST, o );
	}

	private void finest(int extraStackDepth, String s) {
		initialize();
		if (!logger.isLoggable(Level.FINEST))
			return;
		logObjects(extraStackDepth,  Level.FINEST, new Object[]{ s } );
	}

	/**
	 * Implement and call this, not in the constructor, so we can pass in the results of the
	 * getLoggerConfigProps() which may not return the proper value during the constructor.
	 */
	private boolean isInitialized = false;
//	private static String jvmVendor = System.getProperty("java.vendor");
//	private static String jvmVersion = System.getProperty("java.version");
//	private static int stackDepth = getStackDepth();
//	
//	private static int getStackDepth() {
//		int depth;
//		if (jvmVendor.contains("IBM")) {
//			depth = 4;	// 1.8 jvm
//		} else {
//			depth = 3;	// 1.8
//		}
//		return depth;
//	}

	/**
	 * 
	 * @param extraStackDepth this is the number of extra stack frames above the caller of this method.
	 * With the switch to the {@link #findCaller(StackTraceElement[], int)} method, this is only used to 
	 * determine when to re-learn the stack index to find the caller of this class or its sub-classes.
	 * Currently the callers of this are {@link #info(Object...)}), {@link #warning(Object...)} and {@link #severe(Object...)}
	 * which always pass in 1, but if a new method is added that changes the number of methods on the stack
	 * between this method and the caller of ComponentLogger methods, it should pass in the appropriate value.
	 * Again, when the value passed here is different than the last value passed.  This is all to optimize the
	 * selecting/finding of the calling stack element.
	 * @param l log level.
	 * @param objects objects to log using their toString() methods.
	 */
	private void logObjects(int extraStackDepth, Level l, Object[] objects) {
		StackTraceElement ste[] = Thread.currentThread().getStackTrace();
		StackTraceElement caller = null;
		// 4 below assumes that this method is called with only one of this
		// classe's methods on the stack (e.g. info(), severe(), etc), and more
		// specifically, that the method to list in the log message is 2 below
		// this method on the stack.
//		int effectiveStackDepth = stackDepth + extraStackDepth;
//		if (ste != null && ste.length > effectiveStackDepth )	
//			caller = ste[effectiveStackDepth];
		caller = findCaller(ste, extraStackDepth);
		String clazz, method, file;
		int lineno;
		if (caller == null) {
			clazz = unknown;
			method = unknown;
			file = unknown;
			lineno=0;
		} else {
			clazz = caller.getClassName();
			method = caller.getMethodName();
			file = caller.getFileName();
			lineno = caller.getLineNumber();
		}
		StringBuilder sb = new StringBuilder(10 * objects.length);
		for (int i=0 ; i<objects.length ; i++) {
			if (objects[i] != null)
				sb.append(objects[i].toString());
		}
		LogRecord lr = new LogRecord(l, sb.toString());
		lr.setLoggerName(logger.getName());
		lr.setMillis(System.currentTimeMillis());

		// The following is done so that the log message become links inside eclipse.
		lr.setSourceClassName(clazz + "." + method + "(" + file + ":" + lineno +")" );
//		lr.setSourceClassName(clazz);
		lr.setSourceMethodName("");
		logger.log(lr);
		
	}


	int callerStackIndex = -1;
	int lastExtraStackDepth = -1;
	
	/**
	 * @param ste
	 * @param extraStackDepth this is used as  a flag of when to trigger 
	 * re-learning the stack index to find the caller of this class or its sub-classes.
	 * If the passed value differs from the last passed value, then the stack index is re-learned.
	 * This is all to optimize the selecting/finding of the calling stack element.
	 * @return
	 */
	private synchronized StackTraceElement findCaller(StackTraceElement[] ste, int extraStackDepth) {
		if (callerStackIndex < 0 || extraStackDepth != lastExtraStackDepth) {
			callerStackIndex = 0;
			Class<?> thisClass = this.getClass();
			// First find an instance of this class or sub class in the stack
			while (callerStackIndex < ste.length) {
				String callingClassName = ste[callerStackIndex].getClassName();
				try {
					Class<?> callingClass = Class.forName(callingClassName);
					if (callingClass.isAssignableFrom(thisClass))	// We found a our class or sub class 
						break;
					callerStackIndex++;
				} catch (ClassNotFoundException e) {
					// Should NEVER get here.
					e.printStackTrace();
				}
			}
			// Now search down the stack until we find a class that is not an instance of this class or sub-class.
			// This class will be the caller of this class, which we assume is a logging method that needs to include 
			// the calling location.
			while (callerStackIndex < ste.length) {
				String callingClassName = ste[callerStackIndex].getClassName();
				try {
					Class<?> callingClass = Class.forName(callingClassName);
					if (!callingClass.isAssignableFrom(thisClass))	// We found a our class or sub class 
						break;
					callerStackIndex++;
				} catch (ClassNotFoundException e) {
					// Should NEVER get here.
					e.printStackTrace();
				}
			}
		}
		if (callerStackIndex >= ste.length)
			return null;
		return ste[callerStackIndex];
	}


	/**
	 * Holds properties for all files loaded which may contain disparate logging configuration.
	 */
	private static Properties globalLoggerConfig = new Properties();
	/**
	 * Called early on during initialization
	 * Uses the given component properties to configure the loggers.
	 * <p>
	 * NOTE: this is not completely wonderful.  It tries to keep an aggregate set of logging properties in
	 * {@link #globalLoggerConfig}, but it does not merge property values.  For example, it uses the most
	 * recent value of {@link #LOGGING_ENABLED_PROPERTY} and does not merge the <code>handlers</code> or
	 * <code>config</code> properties used by java.util.logging.LogManager.
	 */
	protected static void AppendLoggerProperties(ComponentProperties componentProps) {
	
			String s = componentProps.getProperty(LOGGING_ENABLED_PROPERTY, null);

	
	//		// If not, look in the WPML properties file.
	//		if (s == null) 
	//			s = PMLProperties.instance().getProperty(WPML_LOGGING_ENABLED_PROPERTY);
	
			boolean enabled = s == null ? false : Boolean.parseBoolean(s);
			if (enabled) {
				URL propsURL = componentProps.getPropertyURL();

				if (propsURL != null ) {
//					System.out.println("Appending properties file to logging configuration: " + propsURL);
					try {
						LogManager lm = LogManager.getLogManager();
						Properties newProperties = new Properties();
						InputStream stream = propsURL.openStream();
						newProperties.load(stream);	// This appends to the existing set.
						stream.close();
//						System.err.println("1 org.eng.dsm.level=" + loggerConfig.getProperty("org.eng.dsm.level"));
						globalLoggerConfig.putAll(newProperties);
//						System.err.println("2 org.eng.dsm.level=" + loggerConfig.getProperty("org.eng.dsm.level"));
//						System.getProperties();
//						System.getProperties().store(System.out, "");
						Properties r = getRelevantSystemProperties();
						globalLoggerConfig.putAll(r);	// Let the system properties override the static config file
//						System.err.println("3 org.eng.dsm.level=" + loggerConfig.getProperty("org.eng.dsm.level"));
						lm.readConfiguration(propsToStream(globalLoggerConfig));
					} catch (Exception e) {
						System.err.println("Error reading properties at " + propsURL);
						e.printStackTrace();
					}
				} else {
					System.out.println("Logging NOT enabled. No WPML properties found.");
				}
			}
			
			
		}

	/**
	 * We only get some of the system properties, because if we get them all, then it for some unknown reason
	 * resets the loggers back to INFO logging.
	 * @return
	 */
	private static Properties getRelevantSystemProperties() {
		Properties ret = new Properties();
		Properties sys = System.getProperties();
		for (Object key : sys.keySet()) {
			String str = (String)key;
			if (str.endsWith(".level") && !str.equals("sun.os.patch.level"))
				ret.put(str, sys.getProperty(str));
		}
		return ret;
	}

	private static InputStream propsToStream(Properties props) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(bos);
		props.list(ps);
		ps.flush();
		byte[] bytes = bos.toByteArray();
		return new ByteArrayInputStream(bytes);
	}

	private synchronized void initialize() {
		if (isInitialized)
			return;
		propertiesChanged();
		isInitialized = true;
	}

	/**
	 * Allow callers to reload the properties into the logger if they have changed.
	 * The caller should generally be aware of the return value of {@link #getLoggerConfigProps()}
	 * which has presumably changed and thus requires this call to be made.
	 */
	public void propertiesChanged() {
		AppendLoggerProperties(getLoggerConfigProps()); 
	}

	protected final Logger logger;
	
	/**
	 * @param name
	 * @param resourceBundleName
	 * @deprecated
	 */
	public ComponentLogger(String name, String resourceBundleName) {
		logger = Logger.getLogger(name);
		logger.setUseParentHandlers(true);
	}
	
	protected ComponentLogger(String name, boolean isPMLRoot) {
//		super(name, null);
		logger = Logger.getLogger(name);

//		LogManager.getLogManager().addLogger(this);
		logger.setUseParentHandlers(true);
//		InitializeLogger(getLoggerConfigProps()); 

		
//		Handler h[] = this.getParent().getHandlers();
//		if (isPMLRoot) {
//			ConsoleHandler h = new ConsoleHandler();
//			Handler h2[] = this.getHandlers();
//			if (h2 == null) {
//				this.addHandler(h);
//				Level l = this.getLevel();
//				if (l != null)
//					h.setLevel(l);
////			}
//
//		} else {
//			if (true) {
//				this.setUseParentHandlers(true);
//			} else {
//				this.setUseParentHandlers(false);
//				ConsoleHandler h = new ConsoleHandler();
//				this.addHandler(h);
//			}
//		}
	}

	/**
	 * Get the component properties that contains configuration for the logger.
	 * @return
	 */
	protected abstract ComponentProperties getLoggerConfigProps();

	public boolean isLoggable(Level level) {
		initialize();
		return logger.isLoggable(level);
	}

}
