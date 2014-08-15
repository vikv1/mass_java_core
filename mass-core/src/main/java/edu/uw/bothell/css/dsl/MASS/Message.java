package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Vector;

public class Message implements Serializable {
    public enum ACTION_TYPE{ 
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
	    AGENTS_MIGRATION_REMOTE_REQUEST           // 16   
    }

    // PLACES_INITIALIZE
    Message( ACTION_TYPE action,
	     int[] size, int handle,  String classname, Object argument,
	     int boundary_width, Vector<String> hosts ) {
	this.action = action;
	this.size = size;
	this.handle = handle;
	this.dest_handle = VOID_HANDLE;
	this.functionId = 0;
	this.classname = classname;
	this.argument = argument;
	this.hosts = hosts;
	this.destinations = null;
	this.agent_population = -1;
	this.boundary_width= boundary_width;
	this.exchangeReqList = null;
	this.migrationReqList = null;
    }

    // PLACES_CALL_ALL_VOID_OBJECT,
    // PLACES_CALL_ALL_RETURN_OBJECT,
    // AGENTS_CALL_ALL_VOID_OBJECT,
    // AGENTS_CALL_ALL_RETURN_OBJECT
    Message( ACTION_TYPE action,
	     int handle, int functionId, Object argument ) {
	this.action = action;
	this.size = null;
	this.handle = handle;
	this.dest_handle = VOID_HANDLE;
	this.functionId = functionId;
	this.classname = null;
	this.argument = argument;
	this.hosts = null;
	this.destinations = null;
	this.agent_population = -1;
	this.boundary_width= 0;
	this.exchangeReqList = null;
	this.migrationReqList = null;
    }

    // PLACES_EXCHANGE_ALL
    Message( ACTION_TYPE action,
	     int handle, int dest_handle, int functionId,
	     Vector<int[]> destinations ) {
	this.action = action;
	this.size = null;
	this.handle = handle;
	this.dest_handle = dest_handle;
	this.functionId = functionId;
	this.classname = null;
	this.argument = argument;
	this.hosts = null;
	this.destinations = destinations;
	this.agent_population = -1;
	this.boundary_width= 0;
	this.exchangeReqList = null;
	this.migrationReqList = null;
    }

    // PLACES_EXCHANGE_ALL_REMOTE_REQUEST
    Message( ACTION_TYPE action,
	     int handle, int dest_handle, int functionId,
	     Vector<RemoteExchangeRequest> exchangeReqList, int dummy ) {
	this.action = action;
	this.size = null;
	this.handle = handle;
	this.dest_handle = dest_handle;
	this.functionId = functionId;
	this.classname = null;
	this.argument = null;
	this.hosts = null;
	this.destinations = null;
	this.agent_population = -1;
	this.boundary_width= 0;
	this.exchangeReqList = exchangeReqList;
	this.migrationReqList = null;
    }	

    // PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT,
    // PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST,
    // ACK used for PLACES_CALL_ALL_RETURN_OBJECT
    Message( ACTION_TYPE action, Object retVals ) {
	this.action = action;
	this.size = null;
	this.handle = VOID_HANDLE;
	this.dest_handle = VOID_HANDLE;
	this.functionId = 0;
	this.classname = null;
	this.argument = retVals;
	this.hosts = null;
	this.destinations = null;
	this.agent_population = -1;
	this.boundary_width= 0;
	this.exchangeReqList = null;
	this.migrationReqList = null;
    }

    // AGENTS_INITIALIZE
    Message( ACTION_TYPE action, int initPopulation, int handle,
	     int placeHandle, String className, Object argument ) {
	this.action = action;
	this.size = null;
	this.handle = handle;
	this.dest_handle = placeHandle;
	this.functionId = 0;
	this.classname = className;
	this.argument = argument;
	this.hosts = null;
	this.destinations = null;
	this.agent_population = initPopulation;
	this.boundary_width= 0;
	this.exchangeReqList = null;
	this.migrationReqList = null;
    }

    // AGENTS_MANAGE_ALL and PLACES_EXCHANGE_BOUNDARY
    Message( ACTION_TYPE action, int handle, int dummy ) {
	this.action = action;
	this.size = null;
	this.handle = handle;
	this.dest_handle = handle;
	this.functionId = 0;
	this.classname = null;
	this.argument = null;
	this.hosts = null;
	this.destinations = null;
	this.agent_population = -1;
	this.boundary_width= 0;
	this.exchangeReqList = null;
	this.migrationReqList = null;
    }

    // AGENTS_MIGRATION_REMOTE_REQUEST
    Message( ACTION_TYPE action, int agentHandle, int placeHandle,
             Vector<AgentMigrationRequest> migrationReqList ) {
	this.action = action;
	this.size = null;
	this.handle = agentHandle;
	this.dest_handle = placeHandle;
	this.functionId = 0;
	this.classname = null;
	this.argument = null;
	this.hosts = null;
	this.destinations = null;
	this.agent_population = -1;
	this.boundary_width= 0;
	this.exchangeReqList = null;
	this.migrationReqList = migrationReqList;
    }

    // FINISH
    // ACK
    Message( ACTION_TYPE action ) {
	this.action = action;
	this.size = null;
	this.handle = VOID_HANDLE;
	this.dest_handle = VOID_HANDLE;
	this.functionId = 0;
	this.classname = null;
	this.argument = null;
	this.hosts = null;
	this.destinations = null;
	this.agent_population = -1;
	this.boundary_width= 0;
	this.exchangeReqList = null;
	this.migrationReqList = null;
    }

    // ACK used for AGENTS_CALL_ALL_RETURN_OBJECT
    Message( ACTION_TYPE action, Object argument, int localPopulation ) {
	this.action = action;
	this.size = null;
	this.handle = VOID_HANDLE;
	this.dest_handle = VOID_HANDLE;
	this.functionId = 0;
	this.classname = null;
	this.argument = argument;
	this.hosts = null;
	this.destinations = null;
	this.agent_population = localPopulation;
	this.boundary_width= 0;
	this.exchangeReqList = null;
	this.migrationReqList = null;
    }

    // ACK used for AGENTS_INITIALIZE and AGENTS_CALL_ALL_VOID_OBJECT
    Message( ACTION_TYPE action, int localPopulation ) {
	this.action = action;
	this.size = null;
	this.handle = VOID_HANDLE;
	this.dest_handle = VOID_HANDLE;
	this.functionId = 0;
	this.classname = null;
	this.argument = null;
	this.hosts = null;
	this.destinations = null;
	this.agent_population = localPopulation;
	this.boundary_width= 0;
	this.exchangeReqList = null;
	this.migrationReqList = null;
    }

    // EMPTY
    Message( ) {
	this.action = action;
	this.size = null;
	this.handle = VOID_HANDLE;
	this.dest_handle = VOID_HANDLE;
	this.functionId = 0;
	this.classname = null;
	this.argument = null;
	this.hosts = null;
	this.destinations = null;
	this.agent_population = -1;
	this.boundary_width= 0;
	this.exchangeReqList = null;
	this.migrationReqList = null;
    }

    public ACTION_TYPE getAction( ) { return action; }
    public int[] getSize( ) { return size; }
    public int getHandle( ) { return handle; }
    public int getDestHandle( ) { return dest_handle; }
    public int getFunctionId( ) { return functionId; }
    public String getClassname( ) { return classname; }
    public boolean isArgumentValid( ) { return ( argument != null ); }
    public Object getArgument( ) { return argument; }
    public int getBoundaryWidth( ) { return boundary_width; }
    public int getAgentPopulation( ) { return agent_population; }
    public Vector<String> getHosts( ) { return hosts; }
    public Vector<int[]> getDestinations( ) { return destinations; }
    public Vector<RemoteExchangeRequest> getExchangeReqList( )
    { return exchangeReqList; }
    public Vector<AgentMigrationRequest> getMigrationReqList( )
    { return migrationReqList; }
    
    private static final int VOID_HANDLE = -1;
    private ACTION_TYPE action;
    private int[] size;
    private int handle;
    private int dest_handle;
    private int functionId;
    private String classname;      // classname.class must be located in CWD.
    private Object argument;
    private Vector<String> hosts; // all hosts participated in computation
    private Vector<int[]> destinations; // all destinations of exchangeAll
    private int agent_population;
    private int boundary_width;
    private Vector<RemoteExchangeRequest> exchangeReqList = null;
    private Vector<AgentMigrationRequest> migrationReqList = null;
}