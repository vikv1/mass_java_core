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
	 * Messages for events that are not immediate errors but could eventually lead to system instability or exceptions
	 */
	WARN,

	/**
	 * Messages used for debugging and troubleshooting
	 */
	DEBUG,

	/**
	 * Messages for closing tracking the execution of MASS. Extremely high log volume (lowest log level)
	 */
	TRACE,

	/**
	 * Disable all message logging
	 */
	OFF,
	

}
