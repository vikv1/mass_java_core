/*

 	MASS Java Software License
	© 2012-2021 University of Washington

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

package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;
import java.util.Arrays;
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
		AGENTS_EXCHANGE_ALL,					  // 17

		SPACE_PLACES_INITIALIZE,
		AGENTS_INITIALIZE_SPACE,
		AGENTS_MANAGE_ALL_SPACE,

		BINARY_TREE_PLACES_INITIALIZE,
		BINARY_TREE_PLACES_ADD_PLACE,
		AGENTS_INITIALIZE_BINARY,
		AGENTS_MANAGE_ALL_BINARY,

		QUADTREE_PLACES_INITIALIZE,
		SEND_MAX_LEVEL,
		SUBDIVIDE_QUADTREE_PLACE,
		QUADTREE_PLACES_CALL_ALL_VOID_OBJECT,
		QUADTREE_PLACES_CALL_ALL_RETURN_OBJECT,
		AGENTS_INITIALIZE_QUADTREE,
		AGENTS_MANAGE_ALL_QUADTREE, 

		PLACES_INITIALIZE_GRAPH("PLACES_INITIALIZE_GRAPH"),

		// Retrieval
		// Graph Maintenance (Deprecated) Use of this message type is deprecated and will be
		// removed in a future release.
		MAINTENANCE_GET_PLACES("Maintenance.getPlaces"),
		MAINTENANCE_GET_PLACES_RESPONSE("Maintenance.getPlacesResponse"),

		// Exchange all
		GRAPH_PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT("GraphPlaces.remoteExchangeAll"),

		// Graph Maintenance (Deprecated) Use of this message type is deprecated and will be
		// removed in a future release.
		MAINTENANCE_ADD_PLACE("Maintenance.addPlace"),
		MAINTENANCE_ADD_EDGE("Maintenance.addEdge"),

		// Graph Maintenance (Deprecated) Use of this message type is deprecated and will be
		// removed in a future release.
		MAINTENANCE_REMOVE_PLACE("Maintenance.removePlace"),
		MAINTENANCE_REMOVE_EDGE("Maintenance.removeEdge"),

		// Graph maintenance messages.
		MAINTENANCE_ADD_VERTEX("Maintenance.addVertex"),
		MAINTENANCE_REMOVE_VERTEX("Maintenance.removeVertex"),
		MAINTENANCE_REMOVE_NEIGHBOR("Maintenance.removeNeighbor"),
		MAINTENANCE_GET_VERTEX("Maintenance.getVertex"),
		MAINTENANCE_GET_VERTEX_RESPONSE("Maintenance.getVertexResponse"),
		MAINTENANCE_REMOVE_EDGE_V2("Maintenance.removeEdgeV2"),
		MAINTENANCE_ADD_EDGE_V2("Maintenance.addEdgeV2"),
		MAINTENANCE_LOAD_DSL_FILE("Maintenance.loadDSLFile"),
		MAINTENANCE_LOAD_SAR_FILE("Maintenance.loadSARFile"),

		// HIPPIE Related Messages
		MAINTENANCE_BULK_GRAPH_HIPPIE_OPS("Maintenance.bulkGraphHippieOps"),
		MAINTENANCE_HIPPIE_ADD_EDGE_ATTRIB("Maintenance.hippieAddEdgeAttrib"),

		// MATSim Related Messages
		MAINTENANCE_BULK_GRAPH_MATSIM_OPS("Maintenance.bulkGraphMATSimOps"),
		MAINTENANCE_MATSIM_ADD_EDGE("Maintenance.matsimAddEdge"),

		// Re-Initialize
		MAINTENANCE_REINITIALIZE("Maintenance.reinitialize"),

		// Global Logical Clock commands
		CLOCK_SET_VALUE

		;

		private final String value;

		private ACTION_TYPE() {
			value = "UNDEFINED";
		}

		private ACTION_TYPE(String v) {
			value = v;
		}

		public String getValue() {
			return value;
		}
	}
	
	// until a valid handle ID is supplied, this value is used
	public static final int VOID_HANDLE = -1;
	public static final int VOID_DIMENSIONS = -1;
	public static final int VOID_GRANULARITY = -1;

	public static final Boundary VOID_BOUNDARY = null;

	private ACTION_TYPE action;
	private int[] size = null;
	private int dimensions = VOID_DIMENSIONS;
	private int granularity = VOID_GRANULARITY;
	private Boundary boundary = VOID_BOUNDARY;
	private int numOfNodes = -1;
	private double[] min = null;
	private double[] max = null;
	private int handle = VOID_HANDLE;
	private int destinationHandle = VOID_HANDLE;
	private int functionId = 0;
	private int maxLevel = 0;  // max level of quad tree
	private int numOfLeaf = 0;
    private double[] pivot;
    private BinaryTreePlace place;
	private String classname = null;      // classname.class must be located in CWD.
	private String filename = null;
	private Object argument = null;
	private Object input_argument = null;
	private Vector<String> hosts = null; // all hosts participated in computation
	private Vector<int[]> destinations = null; // all destinations of exchangeAll
	private int agentPopulation = -1;
	private int boundaryWidth = 0;
	private Vector<RemoteExchangeRequest> exchangeReqList = null;
	private Vector<AgentMigrationRequest> migrationReqList = null;

	public Message( ) { }

	/**
	 * Create a new Message with a specified ACTION_TYPE
	 * @param action The ACTION_TYPE of this Message
	 */
	public Message( ACTION_TYPE action ) {

		this.action = action;

	}

	/**
	 * ACK used for AGENTS_INITIALIZE and AGENTS_CALL_ALL_VOID_OBJECT
	 * @param action The ACTION_TYPE of this Message
	 * @param localPopulation The Agent population
	 */
	public Message( ACTION_TYPE action, int localPopulation ) {

		this.action = action;
		this.agentPopulation = localPopulation;

	}

	/**
	 * AGENTS_MANAGE_ALL and PLACES_EXCHANGE_BOUNDARY type Message
	 * @param action The ACTION_TYPE of this Message
	 * @param handle The source handle ID (also used as destination handle ID)
	 * @param dummy Not used (presumably to have a unique signature for this constructor?)
	 */
	public Message( ACTION_TYPE action, int handle, int dummy ) {

		this.action = action;
		this.handle = handle;
		this.destinationHandle = handle;

	}

	/**
	 * Construct a Message primarily used for BINARY_TREE_PLACES_INITIALIZE
	 * @param action The ACTION_TYPE of this Message
	 * @param handle The source handle ID
	 * @param dimensions The dimensions of Place
	 * @param classname The name of the class representing the Place
	 * @param filename The filename of inputs data points
	 * @param argument An argument to be passed to the Place during initialization
	 * @param boundaryWidth The boundary width of the Place
	 * @param hosts A collection of hostnames that are members of the cluster
	 */
	public Message(ACTION_TYPE action, int handle, double[] pivot, int numOfNodes, String classname, String filename, Object argument, Vector<String> hosts ) {
	
		this.action = action;
		MASSBase.getLogger().debug("action = " + action);

		this.handle = handle;
		MASSBase.getLogger().debug("handle = " + handle);

		this.pivot = pivot;
		MASSBase.getLogger().debug("pivot = " + Arrays.toString(pivot));
		this.numOfNodes = numOfNodes;
		MASSBase.getLogger().debug("numOfNodes = " + numOfNodes);

		this.classname = classname;
		MASSBase.getLogger().debug("classname = " + classname);

		this.filename = filename;
		MASSBase.getLogger().debug("filename = " + filename);

		this.argument = argument;
		MASSBase.getLogger().debug("argument = " + argument);

		this.hosts = hosts;
		MASSBase.getLogger().debug("host = " + hosts.toString());

	}
    
	/**
	 * Construct a Message primarily used for QUADTREE_PLACES_INITIALIZE
	 * @param action The ACTION_TYPE of this Message
	 * @param handle The source handle ID
	 * @param dimensions The dimensions of Place
	 * @param classname The name of the class representing the Place
	 * @param filename The filename of inputs data points
	 * @param argument An argument to be passed to the Place during initialization
	 * @param boundaryWidth The boundary width of the Place
	 * @param hosts A collection of hostnames that are members of the cluster
	 */
	public Message(ACTION_TYPE action, int handle,  int dimensions, Boundary boundary, int numOfNodes, String classname, String filename, Object argument, Vector<String> hosts ) {

		this.action = action;
		this.handle = handle;
		this.dimensions = dimensions;
		this.boundary = boundary;
		this.numOfNodes = numOfNodes;
		this.classname = classname;
		this.filename = filename;
		this.argument = argument;
		this.hosts = hosts;

	}	

	/**
	 * Construct a Message with a given ACTION_TYPE, handle ID, destination handle ID, and function ID
	 * @param action The ACTION_TYPE of this Message
	 * @param handle The source handle ID
	 * @param destHandle The destination handle ID
	 * @param funcID The ID of the function to call/was called
	 */
	public Message( ACTION_TYPE action, int handle, int destHandle, int funcID ) {
	
		this.action = action;
		this.handle = handle;
		this.destinationHandle = destHandle;
		this.functionId = funcID;
	
	}

	/**
	 * construct a Messgage primarily used for SUBDIVIDE_QUADTREE_PLACE
	 * @param action
	 * @return
	 */
	public Message(ACTION_TYPE action, int maxLevel, int numOfLeaf, int dummy1, int dummy2) {

		this.action = action;
		this.maxLevel = maxLevel;
		this.numOfLeaf = numOfLeaf;

	}

	/**
	 * Construct a Message primarily used for AGENTS_INITIALIZE action
	 * @param action The ACTION_TYPE of this Message
	 * @param initPopulation The initial Agent population
	 * @param handle The source handle ID
	 * @param placeHandle The handle ID for the referenced Place
	 * @param className The name of the class representing the Agent
	 * @param argument An argument to be passed to the Agent during initialization
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
	 * Construct a Message primarily used for PLACES_EXCHANGE_ALL action
	 * @param action The ACTION_TYPE of this Message
	 * @param handle The source handle ID
	 * @param dest_handle The destination handle ID
	 * @param functionId The ID of the function to call/was called
	 * @param destinations A collection of destinations to perform the function ID
	 */
	public Message( ACTION_TYPE action, int handle, int dest_handle, int functionId, Vector<int[]> destinations ) {

		this.action = action;
		this.handle = handle;
		this.destinationHandle = dest_handle;
		this.functionId = functionId;
		this.destinations = destinations;

	}

	/**
	 * Construct a Message primarily used for PLACES_EXCHANGE_ALL_REMOTE_REQUEST action
	 * @param action The ACTION_TYPE of this Message
	 * @param handle The source handle ID
	 * @param destinationHandle The destination handle ID
	 * @param functionId The ID of the function to call/was called
	 * @param exchangeReqList A collection of RemoteExchangeRequests
	 * @param dummy Not used (presumably to have a unique signature for this constructor?)
	 */
	public Message( ACTION_TYPE action, int handle, int destinationHandle, int functionId, Vector<RemoteExchangeRequest> exchangeReqList, int dummy ) {

		this.action = action;
		this.handle = handle;
		this.destinationHandle = destinationHandle;
		this.functionId = functionId;
		this.exchangeReqList = exchangeReqList;

	}

	/**
	 * Construct a Message primarily used for PLACES_CALL_ALL_VOID_OBJECT, PLACES_CALL_ALL_RETURN_OBJECT,
	 * AGENTS_CALL_ALL_VOID_OBJECT, AGENTS_CALL_ALL_RETURN_OBJECT and GET_LIVE_AGENTS actions
	 * @param action The ACTION_TYPE of this Message
	 * @param handle The source handle ID
	 * @param functionId The ID of the function to call/was called
	 * @param argument An argument to be passed to the Agent during initialization
	 */
	public Message( ACTION_TYPE action, int handle, int functionId, Object argument ) {

		this.action = action;
		this.handle = handle;
		this.functionId = functionId;
		this.argument = argument;

	}

	/**
	 * Construct a Message used for AGENTS_INITIALIZE_SPACE action ------------ yuna
	 * @param action The ACTION_TYPE of this Message
	 * @param handle The source handle ID
	 * @param placeHandle The handle ID for the referenced Place
	 * @param className The name of the class representing the Agent
	 * @param argument An argument to be passed to the Agent during initialization
	 */
	public Message( ACTION_TYPE action, int handle, int placeHandle, String className, Object input_argument, Object argument ) {

		this.action = action;
		this.handle = handle;
		this.destinationHandle = placeHandle;
		this.classname = className;
		this.argument = argument;
		this.input_argument = input_argument;

	}

	/**
	 * Construct a Message primarily used for AGENTS_MIGRATION_REMOTE_REQUEST action
	 * @param action The ACTION_TYPE of this Message
	 * @param agentHandle The handle ID of the Agent to send to the request to
	 * @param placeHandle The source Place handle ID
	 * @param migrationReqList A collection of requests to forward to the Agent
	 */
	public Message( ACTION_TYPE action, int agentHandle, int placeHandle, Vector<AgentMigrationRequest> migrationReqList ) {

		this.action = action;
		this.handle = agentHandle;
		this.destinationHandle = placeHandle;
		this.migrationReqList = migrationReqList;

	}

	public Message(ACTION_TYPE action, int handle, Object args) {
		this.action = action;
		this.handle = handle;
		this.argument = args;
	}

	/**
	 * Construct a Message primarily used for SPACE_PLACES_INITIALIZE
	 * @param action The ACTION_TYPE of this Message
	 * @param size The simulation space sizes/dimensions
	 * @param handle The source handle ID
	 * @param classname The name of the class representing the Place
	 * @param argument An argument to be passed to the Place during initialization
	 * @param boundaryWidth The boundary width of the Place
	 * @param hosts A collection of hostnames that are members of the cluster
	 */
	public Message(ACTION_TYPE action, int[] size, int handle,  int dimensions, int granularity, 
			String classname, double[] min, double[] max, Object argument, Vector<String> hosts ) {

		this.action = action;
		this.size = size.clone();
		this.handle = handle;
		this.dimensions = dimensions;
		this.granularity = granularity;
		this.classname = classname;
		this.min = min.clone();
		this.max = max.clone();
		this.argument = argument;
		this.hosts = hosts;

	}

	/**
	 * Construct a Message primarily used for PLACES_INITIALIZE
	 * @param action The ACTION_TYPE of this Message
	 * @param size The simulation space sizes/dimensions
	 * @param handle The source handle ID
	 * @param classname The name of the class representing the Place
	 * @param argument An argument to be passed to the Place during initialization
	 * @param boundaryWidth The boundary width of the Place
	 * @param hosts A collection of hostnames that are members of the cluster
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
	 * Construct a Message primarily used for PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT and 
	 * PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST, and as an ACK for PLACES_CALL_ALL_RETURN_OBJECT
	 * @param action The ACTION_TYPE of this Message
	 * @param retVals Return values from the affected Places or Agents
	 */
	public Message( ACTION_TYPE action, Object retVals ) {

		this.action = action;
		this.argument = retVals;

	}

	/**
	 * ACK used for AGENTS_CALL_ALL_RETURN_OBJECT, Message.ACTION_TYPE.AGENTS_CALL_ALL_RETURN_OBJECT_SINGLE_ARGUMENT
	 *  and AGENT_ASYNC_RESULT
	 * @param action The ACTION_TYPE of this Message
	 * @param argument An argument to be passed to the Agent
	 * @param localPopulation The number of local Agents
	 */
	public Message( ACTION_TYPE action, Object argument, int localPopulation ) {

		this.action = action;
		this.argument = argument;
		this.agentPopulation = localPopulation;

	}

	/**
	 * Get the action represented by this Message
	 * @return action The ACTION_TYPE of this message
	 */
	public ACTION_TYPE getAction( ) { 
		return action;
	}

	/**
	 * Get the verbose name of the action to be performed upon receipt of this Message
	 * @return The verbose action name
	 */
	public String getActionString() {

		if ( action != null) return action.getValue();

		return "UNDEFINED";

	}

	/**
	 * Get the Agent Populations
	 * @return Agent population
	 */
	public int getAgentPopulation( ) { 
		return agentPopulation;
	}

	/**
	 * Get the argument supplied to the recipient(s) of the Message
	 * @return argument The argument to supply to the recipient
	 */
	public Object getArgument( ) { 
		return argument;
	}

	/**
	 * Get the Boundary for QuadTreePlaces
	 * @return Boundary
	 */
	public Boundary getBoundary( ) { 
		return boundary;
	}

	/**
	 * Get the Boundary Width
	 * @return Boundary width
	 */
	public int getBoundaryWidth( ) { 
		return boundaryWidth;
	}

	/**
	 * Get the class name used to instantiate Places and Agents
	 * @return classname The name of the class to be used for instantiation
	 */
	public String getClassname( ) { 
		return classname;
	}

	/**
	 * Get the destination handle
	 * @return Destination handle ID
	 */
	public int getDestHandle( ) { 
		return destinationHandle;
	}

	/**
	 * Get the destinations for the Message, as a collection of handle IDs
	 * @return Destinations for the Message
	 */
	public Vector<int[]> getDestinations( ) { 
		return destinations;
	}

	public int getDimensions( ) { 
		return dimensions;
	}

	/**
	 * Get the collection of RemoteExchangeRequests to be performed by the recipient
	 * @return The RemoteExchangeRequests to be performed
	 */
	public Vector<RemoteExchangeRequest> getExchangeReqList( ) {
		return exchangeReqList;
	}

	public String getFilename( ) { 
		return filename;
	}

	/**
	 * Get the ID number of the function to execute by the recipient of this Message
	 * @return The ID of the function to execute
	 */
	public int getFunctionId( ) { 
		return functionId;
	}

	public int getGranularity( ) { 
		return granularity;
	}

	/**
	 * Get the handle ID of the Agent or Place referred to by this Message
	 * @return The handle ID
	 */
	public int getHandle( ) { 
		return handle; 
	}

	/**
	 * Get the hostnames of the nodes participating in the MASS cluster
	 * @return Node participants as network hostnames
	 */
	public Vector<String> getHosts( ) { 
		return hosts;
	}

	public Object getInputArgument() {
		return input_argument;
	}

	public double[] getMax() {
		return max;
	}

	/**
	 * Get the max level of quad tree
	 * @return maximum level of quad tree
	 */
	public int getMaxLevel() { 
		return maxLevel;
	}

	/**
	 * Get the Agent Migration Request List supplied to the Message recipient
	 * @return The collection of AgentMigrationRequests
	 */
	public Vector<AgentMigrationRequest> getMigrationReqList( ) {
		return migrationReqList;
	}

	public double[] getMin() {
		return min;
	}

	/**
	 * Get the number of leaf node
	 * @return num of leaf node
	 */
	public int getNumOfLeaf() { 
		return numOfLeaf;
	}

	public int getNumOfNodes( ) { 
		return numOfNodes;
	}

	/**
	 * Get the size of the simulation space, defined as size/dimension
	 * @return Simulation space sizes
	 */
	public int[] getSize( ) { 
		return size; 
	}

	/**
	 * Check if argument is valid
	 * @return TRUE if the argument supplied is valid
	 */
	public boolean isArgumentValid( ) { 
		return ( argument != null );
	}

}