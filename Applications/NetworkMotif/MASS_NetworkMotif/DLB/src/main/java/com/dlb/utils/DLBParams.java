package main.java.com.dlb.utils;

import java.io.FileOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class DLBParams {
	
	/**
	 * Maximum simulation size.
	 */
	public static int MAX_SIM_SIZE             = 0;
		
	public static boolean LB_FLAG              = false; //<-- done
	public static boolean doLoadBalanceFlag    = false; //<-- pending
	
	/**
	 * The concurrent map that holds a Slice object for a 
	 * thread id.
	 */
	public static ConcurrentMap<Long, Slice> boundaryMap = new ConcurrentHashMap<Long, Slice>();
	
	public static Thread[] threadList          = null; 
	
	public static int TIME_COUNTER = 0;
	
	/**
	 * Debug flag, will print all debug statements
	 * when turned to true
	 */
	public static boolean DEBUG = false;
	
	public static boolean DEBUG_SP = true;
	
	/**
	 * Flag to compute slope based algorithm
	 */
	public static boolean SLOPE_BASED = false;
	/**
	 * Flag to compute window based algorithm
	 */
	public static boolean WINDOW_BASED = false;
	/**
	 * Flag to compute history based algorithm.
	 */
	public static boolean HISTORY_BASED = false;
	
	
	public static final String DLB_PROPERTY_FILE_NAME = "DLB.properties";
	
}
