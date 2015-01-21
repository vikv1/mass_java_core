package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Vector;

@SuppressWarnings("serial")
public class Message implements Serializable {

	public enum ACTION_TYPE { 
	    
    	EMPTY,                                    // 0             
	    FINISH,                                   // 1             
	    ACK,                                      // 2             

	    PLACES_INITIALIZE,                        // 3             
	    PLACES_CALL_ALL_VOID_OBJECT,              // 4             
	    PLACES_CALL_ALL_RETURN_OBJECT,            // 5             
	    PLACES_CALL_SOME_VOID_OBJECT,
	    PLACES_EXCHANGE_ALL,                      // 7             
	    PLACES_EXCHANGE_ALL_REMOTE_REQUEST,       // 8             
	    PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT, // 9             
	    PLACES_EXCHANGE_BOUNDARY,                 // 10            
	    PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST,  // 11            

	    AGENTS_INITIALIZE,                        // 12            
	    AGENTS_CALL_ALL_VOID_OBJECT,              // 13            
	    AGENTS_CALL_ALL_RETURN_OBJECT,            // 14            
	    AGENTS_MANAGE_ALL,                        // 15            
	    AGENTS_MIGRATION_REMOTE_REQUEST,          // 16  
	    
	    /** Async section **/
	    AGENTS_CALL_ALL_ASYNC_RETURN_OBJECT,      // 17
	    FINISH_ASYNC_SERVER                       // 18
    }
    
	private static final int VOID_HANDLE = -1;
    private ACTION_TYPE action;
    private int[] size = null;
    private int handle = VOID_HANDLE;
    private int dest_handle = VOID_HANDLE;
    private int functionId = 0;
    private String classname = null;      // classname.class must be located in CWD.
    private Object argument = null;
    private Vector<String> hosts = null; // all hosts participated in computation
    private Vector<int[]> destinations = null; // all destinations of exchangeAll
    private int agent_population = -1;
    private int boundary_width = 0;
    private Vector<RemoteExchangeRequest> exchangeReqList = null;
    private Vector<AgentMigrationRequest> migrationReqList = null;
    
    // Async vars
    private LinkedList<Integer> functionIds = null;

    // EMPTY
    public Message( ) { }

    // FINISH
    // ACK
    public Message( ACTION_TYPE action ) {

    	this.action = action;
    
    }

    // ACK used for AGENTS_INITIALIZE and AGENTS_CALL_ALL_VOID_OBJECT
    public Message( ACTION_TYPE action, int localPopulation ) {

    	this.action = action;
    	this.agent_population = localPopulation;
    
    }

    // AGENTS_MANAGE_ALL and PLACES_EXCHANGE_BOUNDARY
    public Message( ACTION_TYPE action, int handle, int dummy ) {

    	this.action = action;
    	this.handle = handle;
    	this.dest_handle = handle;
    
    }	

    // AGENTS_INITIALIZE
    public Message( ACTION_TYPE action, int initPopulation, int handle, int placeHandle, String className, Object argument ) {

    	this.action = action;
    	this.handle = handle;
    	this.dest_handle = placeHandle;
    	this.classname = className;
    	this.argument = argument;
    	this.agent_population = initPopulation;

    }

    // PLACES_EXCHANGE_ALL
    public Message( ACTION_TYPE action, int handle, int dest_handle, int functionId, Vector<int[]> destinations ) {

    	this.action = action;
    	this.handle = handle;
    	this.dest_handle = dest_handle;
    	this.functionId = functionId;
    	this.destinations = destinations;
    
    }

    // PLACES_EXCHANGE_ALL_REMOTE_REQUEST
    public Message( ACTION_TYPE action, int handle, int dest_handle, int functionId, Vector<RemoteExchangeRequest> exchangeReqList, int dummy ) {

    	this.action = action;
    	this.handle = handle;
    	this.dest_handle = dest_handle;
    	this.functionId = functionId;
    	this.exchangeReqList = exchangeReqList;

    }

    // PLACES_CALL_ALL_VOID_OBJECT,
    // PLACES_CALL_ALL_RETURN_OBJECT,
    // AGENTS_CALL_ALL_VOID_OBJECT,
    // AGENTS_CALL_ALL_RETURN_OBJECT
    public Message( ACTION_TYPE action, int handle, int functionId, Object argument ) {

    	this.action = action;
    	this.handle = handle;
    	this.functionId = functionId;
    	this.argument = argument;

    }

    // AGENTS_MIGRATION_REMOTE_REQUEST
    public Message( ACTION_TYPE action, int agentHandle, int placeHandle, Vector<AgentMigrationRequest> migrationReqList ) {

    	this.action = action;
    	this.handle = agentHandle;
    	this.dest_handle = placeHandle;
    	this.migrationReqList = migrationReqList;
    
    }

    // PLACES_INITIALIZE
    public Message( ACTION_TYPE action, int[] size, int handle,  String classname, Object argument, int boundary_width, Vector<String> hosts ) {

    	this.action = action;
    	this.size = size;
    	this.handle = handle;
    	this.classname = classname;
    	this.argument = argument;
    	this.hosts = hosts;
    	this.boundary_width= boundary_width;
    
    }

    // PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT,
    // PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST,
    // ACK used for PLACES_CALL_ALL_RETURN_OBJECT
    public Message( ACTION_TYPE action, Object retVals ) {

    	this.action = action;
    	this.argument = retVals;

    }

    // ACK used for AGENTS_CALL_ALL_RETURN_OBJECT
    public Message( ACTION_TYPE action, Object argument, int localPopulation ) {

    	this.action = action;
    	this.argument = argument;
    	this.agent_population = localPopulation;

    }

    public ACTION_TYPE getAction( ) { 
    	return action;
    }
    
    public int getAgentPopulation( ) { 
    	return agent_population;
    }
    
    public Object getArgument( ) { 
    	return argument;
    }
    
    public int getBoundaryWidth( ) { 
    	return boundary_width;
    }
    
    public String getClassname( ) { 
    	return classname;
    }
    
    public int getDestHandle( ) { 
    	return dest_handle;
    }
    
    public Vector<int[]> getDestinations( ) { 
    	return destinations;
    }
    
    public Vector<RemoteExchangeRequest> getExchangeReqList( ) {
    	return exchangeReqList;
    }
    
    public int getFunctionId( ) { 
    	return functionId;
    }
    
    public int getHandle( ) { 
    	return handle; 
    }
    
    public Vector<String> getHosts( ) { 
    	return hosts;
    }
    
    public Vector<AgentMigrationRequest> getMigrationReqList( ) {
    	return migrationReqList;
    }
    
    public int[] getSize( ) { 
    	return size; 
    }
    
    public boolean isArgumentValid( ) { 
    	return ( argument != null );
    }
    
    // Async methods
    // AGENTS_CALL_ALL_ASYNC_RETURN_OBJECT
    public Message( ACTION_TYPE action, int handle, LinkedList<Integer> functionIds, Object argument ) {

      this.action = action;
      this.handle = handle;
      this.functionIds = functionIds;
      this.argument = argument;

    }

}