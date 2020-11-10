/*

 	MASS Java Software License
	© 2012-2020 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2020 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS.logging;

import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uw.bothell.css.dsl.MASS.MASSBase;

/**
 * This Logger implements Apache Log4J2, with the SLF4J facade.
 * 
 * This implementation of the Singleton pattern is based on the sample code provided here:
 * https://en.wikipedia.org/wiki/Singleton_pattern
 * 
 */
public class Log4J2Logger {

	/**
     * Initializes singleton.
     *
     * {@link SingletonHolder} is loaded on the first execution of {@link Singleton#getInstance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     */
    private static class SingletonHolder {
    	private static final Log4J2Logger INSTANCE = new Log4J2Logger();
    }
	private static final String DEFAULT_LOG_FILENAME = "mass_log.log";
	
	private static final LogLevel DEFAULT_LOG_LEVEL = LogLevel.ERROR;

    /**
     * Return this instance of the logger, which is effectively a Singleton
     * @return The single instance of this logger implementation
     */
    public static Log4J2Logger getInstance() {
    	return SingletonHolder.INSTANCE;
    }
    
    // this is the SLF4J facade for the Apache Log4J2 logger
    private Logger logger;
    
    // current logging level
    private LogLevel currentLogLevel = DEFAULT_LOG_LEVEL;

    // current log filename
    private String currentLogFileName = DEFAULT_LOG_FILENAME;

    // Private constructor. Prevents instantiation from other classes.
    private Log4J2Logger() { 
    	
    	// initialize logger with default properties 
    	initLogger();
    	
    }

    /**
     * Record a DEBUG message
     * @param message The DEBUG message to record
     */
	public void debug(String message) {
		logger.debug(message);
	}
    
    /**
	 * Record a DEBUG message with single parameter
	 * @param message The DEBUG message to log
	 * @param parameter A parameter to include in the DEBUG message
	 */
	public void debug(String message, Object parameter) {
		logger.debug(message, parameter);
	}

	/**
	 * Record a TRACE message
	 * @param message the TRACE message to record
	 */
	public void trace(String message) { logger.trace(message); }

	/**
     * Record an ERROR message, without having access to an Exception
     * @param message The ERROR message to record
     */
    public void error(String message) {
    	logger.error(message);
    }

    /**
     * Record an ERROR message, providing the Exception that was caught
     * @param message The ERROR message to log
     * @param e The caught Exception
     */
	public void error(String message, Exception e) {
		logger.error(message, e);
	}

	/**
	 * Record an ERROR message with single parameter and an Exception
	 * @param message The ERROR message to log
	 * @param parameter A parameter to include in the ERROR message
	 * @param e The caught Exception
	 */
	public void error(String message, Object parameter, Exception e) {
		logger.error(message, parameter, e);
	}

	private void initLogger() {
		
		// force loading of system properties
		refreshConfiguration();
		
		// MASSBase is considered to be the parent for all core library classes, so use it as the "root" node
		if (logger == null) logger = LoggerFactory.getLogger(MASSBase.class);
		
	}

	/**
	 * Convenience method to determine of the logger is recording DEBUG level messages
	 * @return TRUE if the logger is recording DEBUG messages, FALSE if not
	 */
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	private void refreshConfiguration() {

		// system property that overrides logger level
		System.setProperty("MASSLogLevel", currentLogLevel.toString());

		// system property that overrides logger filename
		System.setProperty("MASSLogFilename", currentLogFileName);

		// instruct the logger to refresh its' configuration
		org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
		ctx.reconfigure();
		
	}
	
	/**
	 * Set the name of the log file to record messages to
	 * @param name The name of the log file to use
	 */
	public void setLogFileName(String name) {

		// no point trying to set a filename that doesn't exist
		if (name == null || name.length() == 0) return;
		
		currentLogFileName = name;
		
		// refresh logger to use the new filename
		refreshConfiguration();
		
	}
	
	/**
	 * Set the minimum level at which log messages are recorded. Messages of a lower priority are ignored.
	 * @param level The minimum level to record message
	 */
	public void setLogLevel(LogLevel level) {

		// no point trying to set a level that doesn't exist
		if (level == null) return;
		
		currentLogLevel = level;
		
		// refresh logger to use the new log level
		refreshConfiguration();
		
	}

    /**
     * Record a WARNING message
     * @param message The WARNING message to record
     */
	public void warning(String message) {
		logger.warn(message);
	}
	
}