package edu.uw.bothell.css.dsl.MASS.logging;

/**
 * Enumerations for MASS message logging
 */
public enum LogLevel {

	/**
	 * Record messages for events generating exceptions or result in system instability
	 */
	ERROR,

	/**
	 * Messages used for debugging and troubleshooting (lowest log level)
	 */
	DEBUG,
	
	/**
	 * Disable all message logging (highest log level)
	 */
	OFF,
	
	/**
	 * Messages for events that are not immediate errors but could eventually lead to system instability or exceptions
	 */
	WARN
	
}
