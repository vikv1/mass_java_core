/*

 	MASS Java Software License
	© 2012-2015 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2015 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;
import java.util.Vector;

@SuppressWarnings("serial")
public class Message implements Serializable {

	/**
	 * ACTION_TYPE
	 * A list of actions assigned to numbers.
	 */
	public enum ACTION_TYPE { 
	    
    	EMPTY,                                    // 0             
	    FINISH("FINISH"),                         // 1             
	    ACK("ACK"),                               // 2             

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
	    AGENTS_CALL_ALL_ASYNC_RETURN_OBJECT("AGENTS_CALL_ALL_ASYNC_RETURN_OBJECT"),      // 17
	    NODE_MASTER_ASYNC_COMPLETE_REQUEST("NODE_MASTER_ASYNC_COMPLETE_REQUEST"),        //18 check if all slaves are completed
	    AGENTS_ASYNC_MIGRATION_REMOTE_REQUEST("AGENTS_ASYNC_MIGRATION_REMOTE_REQUEST"),    // 19
	    AGENT_ASYNC_RESULT("AGENT_ASYNC_RESULT"),                       // 20
	   // NODE_SLAVE_ASYNC_COMPLETE_NOTIFY("NODE_SLAVE_ASYNC_COMPLETE_NOTIFY"),          
    	// 21 tell master that I'm done
	    NODE_COMPLETE_NOTIFY_SOURCE("NODE_COMPLETE_NOTIFY_SOURCE");
    	// 22
    	
    	private final String value;
    	
    	private ACTION_TYPE(String v) {
    	  value = v;
    	}
    	
    	private ACTION_TYPE() {
    	  value = "UNDEFINED";
    	}
    	
    	public String getValue() {
    	  return value;
    	}
	}
    
	private static final int VOID_HANDLE = -1;
    private ACTION_TYPE action;
    private int[] size = null;
    private int handle = VOID_HANDLE;
    private int destinationHandle = VOID_HANDLE;
    private int functionId = 0;
    private String classname = null;      // classname.class must be located in CWD.
    private Object argument = null;
    private Vector<String> hosts = null; // all hosts participated in computation
    private Vector<int[]> destinations = null; // all destinations of exchangeAll
    private int agentPopulation = -1;
    private int boundaryWidth = 0;
    private Vector<RemoteExchangeRequest> exchangeReqList = null;
    private Vector<AgentMigrationRequest> migrationReqList = null;
    // Pid of the source when sending back result in
    // callAllAsync
    private int sourcePid = -1;
    
    // Async vars
    private int[] functionIds = null;
    private int[] autoMigrateStartingIndex = null;

    // EMPTY
    public Message( ) { }

    /**
     * FINISH
     * ACK
     * @param action
     */
    public Message( ACTION_TYPE action ) {

    	this.action = action;
    
    }

    /**
     * ACK used for AGENTS_INITIALIZE and AGENTS_CALL_ALL_VOID_OBJECT
     * @param action
     * @param localPopulation
     */
    public Message( ACTION_TYPE action, int localPopulation ) {

    	this.action = action;
    	this.agentPopulation = localPopulation;
    
    }

    /**
     * AGENTS_MANAGE_ALL and PLACES_EXCHANGE_BOUNDARY
     * @param action
     * @param handle
     * @param dummy
     */
    public Message( ACTION_TYPE action, int handle, int dummy ) {

    	this.action = action;
    	this.handle = handle;
    	this.destinationHandle = handle;
    
    }	

    /**
     * AGENTS_INITIALIZE
     * @param action
     * @param initPopulation
     * @param handle
     * @param placeHandle
     * @param className
     * @param argument
     */
    public Message( ACTION_TYPE action, int initPopulation, int handle, int placeHandle, String className, Object argument ) {

    	this.action = action;
    	this.handle = handle;
    	this.destinationHandle = placeHandle;
    	this.classname = className;
    	this.argument = argument;
    	this.agentPopulation = initPopulation;

    }

    /**
     * PLACES_EXCHANGE_ALL
     * @param action
     * @param handle
     * @param dest_handle
     * @param functionId
     * @param destinations
     */
    public Message( ACTION_TYPE action, int handle, int dest_handle, int functionId, Vector<int[]> destinations ) {

    	this.action = action;
    	this.handle = handle;
    	this.destinationHandle = dest_handle;
    	this.functionId = functionId;
    	this.destinations = destinations;
    
    }

    /**
     * PLACES_EXCHANGE_ALL_REMOTE_REQUEST
     * @param action
     * @param handle
     * @param destinationHandle
     * @param functionId
     * @param exchangeReqList
     * @param dummy
     */
    public Message( ACTION_TYPE action, int handle, int destinationHandle, int functionId, Vector<RemoteExchangeRequest> exchangeReqList, int dummy ) {

    	this.action = action;
    	this.handle = handle;
    	this.destinationHandle = destinationHandle;
    	this.functionId = functionId;
    	this.exchangeReqList = exchangeReqList;

    }

    /**
     * PLACES_CALL_ALL_VOID_OBJECT,
     * PLACES_CALL_ALL_RETURN_OBJECT,
     * AGENTS_CALL_ALL_VOID_OBJECT,
     * AGENTS_CALL_ALL_RETURN_OBJECT
     * @param action
     * @param handle
     * @param functionId
     * @param argument
     */
    public Message( ACTION_TYPE action, int handle, int functionId, Object argument ) {

    	this.action = action;
    	this.handle = handle;
    	this.functionId = functionId;
    	this.argument = argument;

    }

    /**
     * AGENTS_MIGRATION_REMOTE_REQUEST
     * @param action
     * @param agentHandle
     * @param placeHandle
     * @param migrationReqList
     */
    public Message( ACTION_TYPE action, int agentHandle, int placeHandle, Vector<AgentMigrationRequest> migrationReqList ) {

    	this.action = action;
    	this.handle = agentHandle;
    	this.destinationHandle = placeHandle;
    	this.migrationReqList = migrationReqList;
    
    }

    /**
     * PLACES_INITIALIZE
     * @param action
     * @param size
     * @param handle
     * @param classname
     * @param argument
     * @param boundaryWidth
     * @param hosts
     */
    public Message( ACTION_TYPE action, int[] size, int handle,  String classname, Object argument, int boundaryWidth, Vector<String> hosts ) {

    	this.action = action;
    	this.size = size;
    	this.handle = handle;
    	this.classname = classname;
    	this.argument = argument;
    	this.hosts = hosts;
    	this.boundaryWidth= boundaryWidth;
    
    }

    /**
     * PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT and 
     * PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST
     * ACK used for PLACES_CALL_ALL_RETURN_OBJECT
     * @param action
     * @param retVals
     */
    public Message( ACTION_TYPE action, Object retVals ) {

    	this.action = action;
    	this.argument = retVals;

    }

    /**
     * ACK used for AGENTS_CALL_ALL_RETURN_OBJECT and AGENT_ASYNC_RESULT
     * @param action
     * @param argument
     * @param localPopulation
     */
    public Message( ACTION_TYPE action, Object argument, int localPopulation ) {

    	this.action = action;
    	this.argument = argument;
    	this.agentPopulation = localPopulation;

    }
    
    // AGENT_ASYNC_RESULT
    public void setSourcePid(int pid) {
      sourcePid = pid;
    }
    
    /**
     * Get the action
     * @return action
     */
   public ACTION_TYPE getAction( ) { 
    	return action;
    }
    
   /**
    * Get the Agent Populations
    * @return agent_population
    */
    public int getAgentPopulation( ) { 
    	return agentPopulation;
    }
    
    /**
     * Get the argument
     * @return argument
     */
    public Object getArgument( ) { 
    	return argument;
    }
    
    /**
     * Get the Boundary Width
     * @return boundary_width
     */
    public int getBoundaryWidth( ) { 
    	return boundaryWidth;
    }
    
    /**
     * Get the class name
     * @return classname
     */
    public String getClassname( ) { 
    	return classname;
    }
    
    /**
     * Get the destination handle
     * @return dest_handle
     */
    public int getDestHandle( ) { 
    	return destinationHandle;
    }
    
    /**
     * Get the destinations
     * @return destinations
     */
    public Vector<int[]> getDestinations( ) { 
    	return destinations;
    }
    
    public Vector<RemoteExchangeRequest> getExchangeReqList( ) {
    	return exchangeReqList;
    }
    
    /**
     * Get the functionId
     * @return functionId
     */
    public int getFunctionId( ) { 
    	return functionId;
    }
    
    /**
     * Get the handle
     * @return handle
     */
    public int getHandle( ) { 
    	return handle; 
    }
    
    /**
     * Get the hosts
     * @return *hosts
     */
    public Vector<String> getHosts( ) { 
    	return hosts;
    }
    
    /**
     * Get the Agent Migration Request List
     * @return migrationReqList
     */
    public Vector<AgentMigrationRequest> getMigrationReqList( ) {
    	return migrationReqList;
    }
    
    /**
     * Get the size
     * @return size
     */
    public int[] getSize( ) { 
    	return size; 
    }
    
    /**
     * Check if argument is valid
     * @return (argument != NULL)
     */
    public boolean isArgumentValid( ) { 
    	return ( argument != null );
    }
    
    // Async methods
    // AGENTS_CALL_ALL_ASYNC_RETURN_OBJECT
    public Message( ACTION_TYPE action, int handle, int[] functionIds, Object argument ) {

      this.action = action;
      this.handle = handle;
      this.functionIds = functionIds;
      this.argument = argument;

    }
    
    public int[] getFunctionIds() {
      return functionIds;
    }
    
    public int getSourcePid() {
      return sourcePid;
    }

    public String getActionString() {
      return action.getValue();
    }

    public void setAutoMigrationStartingIndex(int[] startingPlaceGlobalIndex) {
      this.autoMigrateStartingIndex  = startingPlaceGlobalIndex;
    }
    
    public int[] getAutoMigrationStartingIndex() {
      return this.autoMigrateStartingIndex;
    }
}