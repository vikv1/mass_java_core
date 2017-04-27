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
 * @author msell
 *
 */
public class Log4J2Logger {

	private static final String DEFAULT_LOG_FILENAME = "mass_log.log";
	private static final LogLevel DEFAULT_LOG_LEVEL = LogLevel.DEBUG;
	
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
     * Initializes singleton.
     *
     * {@link SingletonHolder} is loaded on the first execution of {@link Singleton#getInstance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     */
    private static class SingletonHolder {
    	private static final Log4J2Logger INSTANCE = new Log4J2Logger();
    }

    /**
     * Return this instance of the logger, which is effectively a Singleton
     * @return The single instance of this logger implementation
     */
    public static Log4J2Logger getInstance() {
    	return SingletonHolder.INSTANCE;
    }

    public void error(String message) {
    	logger.error(message);
    }
    
	public void error(String message, Exception e) {
		logger.error(message, e);
	}

	public void error(String message, Object parameter, Exception e) {
		logger.error(message, parameter, e);
	}

	public void debug(String message) {
		logger.debug(message);
	}

	public void debug(String message, Object parameter) {
		logger.debug(message, parameter);
	}

	public void setLogFileName(String name) {

		// no point trying to set a filename that doesn't exist
		if (name == null || name.length() == 0) return;
		
		currentLogFileName = name;
		
		// refresh logger to use the new filename
		refreshConfiguration();
		
	}

	private void initLogger() {
		
		// force loading of system properties
		refreshConfiguration();
		
		// MASSBase is considered to be the parent for all core library classes, so use it as the "root" node
		if (logger == null) logger = LoggerFactory.getLogger(MASSBase.class);
		
		// register a shutdown hook to gracefully shut down logger when JVM is requested to exit
		Runtime.getRuntime().addShutdownHook(new Thread() {
			
			public void run() {
			
				logger.debug( "Stopping Logger" );
				LogManager.shutdown();	// normally this shouldn't be necessary, but just in case...
				
			}
			
		});
		
	}
    
	public void setLogLevel(LogLevel level) {

		// no point trying to set a level that doesn't exist
		if (level == null) return;
		
		currentLogLevel = level;
		
		// refresh logger to use the new log level
		refreshConfiguration();
		
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
	
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

//	@Override
//	public boolean isEnabled(int arg0) {
//		// TODO Auto-generated method stub
//		return true;
//	}
//
//	@Override
//	public void log(int arg0, String arg1) {
//		System.out.println(arg1);
//	}
	
}