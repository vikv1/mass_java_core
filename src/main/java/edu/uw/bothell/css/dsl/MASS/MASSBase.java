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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.Iterator;

import edu.uw.bothell.css.dsl.MASS.clock.GlobalLogicalClock;
import edu.uw.bothell.css.dsl.MASS.clock.SimpleGlobalClock;
import edu.uw.bothell.css.dsl.MASS.event.EventDispatcher;
import edu.uw.bothell.css.dsl.MASS.event.SimpleEventDispatcher;
import edu.uw.bothell.css.dsl.MASS.factory.ObjectFactory;
import edu.uw.bothell.css.dsl.MASS.factory.SimpleObjectFactory;
import edu.uw.bothell.css.dsl.MASS.infra.DistributedMap;
import edu.uw.bothell.css.dsl.MASS.infra.MASSSimpleDistributedMap;
import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;
import edu.uw.bothell.css.dsl.MASS.messaging.MASSMessaging;

/**
 * MASS_base maintains references to all Places, Agents, and mNode instances within the cluster.
 * Methods are provided to allow access to remote objects.
 */
public class MASSBase {

    private static MThread[] threads = new MThread[0];          // including main and children
    private static boolean initialized;  	// check if Mthreads are initialized
	private static Vector<String> hosts = new Vector<String>( );    // all host names
	private static Hashtable<Integer, PlacesBase> placesMap = new Hashtable<Integer, PlacesBase>( );
	private static Hashtable<Integer, AgentsBase> agentsMap = new Hashtable<Integer, AgentsBase>( );
	private static Vector<Vector<RemoteExchangeRequest>> remoteRequests = new Vector<Vector<RemoteExchangeRequest>>( );
	private static Vector<Vector<AgentMigrationRequest>> migrationRequests = new Vector<Vector<AgentMigrationRequest>>( );
	private static PlacesBase currentPlacesBase = null;
	private static AgentsBase currentAgentsBase = null;
	private static ExchangeHelper exchange;// = new ExchangeHelper( );
	private static PlacesBase destinationPlaces;
	private static int currentFunctionId;
	private static Object currentArgument;
	private static Object[] currentReturns;
	private static Message.ACTION_TYPE currentMsgType;
	private static MNode thisNode;			// this node configuration

	private static List< Double[] > recordPoints = new ArrayList<>();  // aid for displaying
	private static List< Double[] > recordLines = new ArrayList<>();

	// Object[] won't work as return values for treePlaces (recursion), use List<> as a temp returns and convert to array
	private static List<Object> currentReturnsTemp = new ArrayList<>(); 

	// TODO: We should have access checks. This should also not just be a public static member of MASS
	//       For example: Maybe only places should have access to the map
	//                           key,    global index
	public static MASSSimpleDistributedMap<Object, Integer> distributed_map;

	// TODO - this is dumb. Calculate from number of hosts identified.
	private static int systemSize = 1;          // # of processes (nodes) in the cluster (temporary!)
	
	// the collection of all nodes
    private static Vector<MNode> allNodes = new Vector<MNode>( );

	// for performance, collection of all remote nodes
    private static Vector<MNode> remoteNodes = new Vector<MNode>();

	// remember the last PID used
    private static int lastPid = 0;
    
    // object factories are singletons, continue configuration within this class
    private static ObjectFactory objectFactory = SimpleObjectFactory.getInstance();
    
    // logging
    private static Log4J2Logger logger = Log4J2Logger.getInstance();
    
    // helper classes
    private static Utilities utilities = new Utilities();
    
    // event dispatcher for annotation-based event triggers
    private static EventDispatcher eventDispatcher = SimpleEventDispatcher.getInstance();
    
    // messaging
    private static MASSMessaging messenger = MASSMessaging.getInstance();
    
    // global logical clock
    private static GlobalLogicalClock clock = SimpleGlobalClock.getInstance();


	/**
     * Add a new node to the cluster
     * @param node The node to add to the cluster
     */
    public static void addNode(MNode node) {

    	logger.debug("Adding a node ({}) to the cluster...", node.getHostName());
    	
    	// add the node to the collection of all nodes
    	allNodes.add(node);
    	
    	// if a remote, add to the collection of all remotes, or set the master if not
    	// this is done so remotes and master node configurations can be obtained quickly without a lookup
    	if (node.isMaster()) {

    		node.setPid(0);		// master node ALWAYS has a PID of zero

    		logger.debug("This node is the MASTER node");
    		
    	} else {

    		logger.debug("This node is a REMOTE node");

    		// increment last PID and set for this remote node
    		lastPid++;
        	node.setPid(lastPid);

        	remoteNodes.add(node);
    	
    	}
    	
    }

	protected static void finish() {
		/*
		try {
			distributed_map.close();
		} catch (IOException e) {
			logger.error("Error closing dmap instance:");

			Arrays.stream(e.getStackTrace()).forEach(element -> logger.error(element.toString()));
		}*/
		distributed_map.close();
	}

	// this will be called by MASS on master node, by MProcess on slave nodes
	// to finish the shared graph places
	public static void finishSharedGraphPlaces() {
		logger.debug("finish shared place");
		Iterator<Integer> iterator = placesMap.keySet().iterator();
		while (iterator.hasNext()) {
			Integer key = iterator.next();
			PlacesBase value = placesMap.get(key);
			if (value instanceof GraphPlaces) {
				logger.debug("GraphPlaces found, finish it");
				GraphPlaces gp = (GraphPlaces)value;
				gp.finish();
			}
		}
	}


	/**
     * Get Agents class for a specific Agents Handle ID
     * @param handle The Agents Handle ID to retrieve
     * @return The Agents class having the specified Handle ID
     */
	public static Agents getAgents( int handle ) {
    	return ( Agents )agentsMap.get( handle );
    }

	/**
	 * Get the collection of Agents currently residing on this node (as AgentsBase)
	 * @return The Agents (as AgentsBase) located on this node
	 */
	public static Hashtable<Integer, AgentsBase> getAgentsMap() {
		return agentsMap;
	}
	
	/**
     * Get all MNode objects, master and remotes
     * @return MNodes representing all nodes
	 */
	public static Vector<MNode> getAllNodes() {
		return allNodes;
	}
	
	/**
	 * Get the port number used for inter-node communications
	 * @return The port number
	 */
	public static int getCommunicationPort() {
		return thisNode.getPort();
	}
	
	/**
	 * Get the number of cores (Hyperthreading included!) in this system
	 * @return The number of CPU cores
	 */
	public static int getCores( ) {
		return Runtime.getRuntime().availableProcessors();
    }
	
	/**
	 * Get the current AgentsBase this node is working with
	 * @return The current AgentsBase this node is using
	 */
	public static AgentsBase getCurrentAgentsBase( ) {
    	return currentAgentsBase;
    }
	
	/**
	 * Get the current argument (supplied to MProcess)
	 * @return The current argument used by MProcess
	 */
	public static Object getCurrentArgument( ) { 
    	return currentArgument;
    }
	
	/**
	 * Get the current function ID that Agents are executing
	 * @return The current Agents function ID
	 */
	public static int getCurrentFunctionId( ) { 
    	return currentFunctionId; 
    }
	
    /**
	 * Get the current Message type enumeration, used by MThread
	 * @return The current Message type enumeration
	 */
	public static Message.ACTION_TYPE getCurrentMsgType( ) { 
    	return currentMsgType;
    }

    /**
     * Get the current Places object being worked on
     * @return The current Places object
     */
    public static PlacesBase getCurrentPlacesBase( ) {
    	return currentPlacesBase;
    }
	
	/**
     * Get the current returns from Places or Agents resulting from the last callAll
     * @return Current returns array
     */
    public static Object[] getCurrentReturns() {
		return currentReturns;
	}

	/**
     * Get the current temperate returns from TreePlaces the last callAll
     * @return Current returns List
     */
    public static List< Object > getCurrentReturnsTemp() {
		return currentReturnsTemp;
	}

	/**
     * Get the PlacesBase representing the destination for an Agent
     * @return Destination PlacesBase
     */
	public static PlacesBase getDestinationPlaces( ) { 
    	return destinationPlaces; 
    }
	
	/**
	 * Get the event dispatcher currently in use
	 * @return The event dispatcher currently being used
	 */
	public static EventDispatcher getEventDispatcher() {
		return eventDispatcher;
	}
	
	/**
	 * Get the ExchangeHelper used by this instance of MASS_base
	 * @return The ExchangeHelper used by this instance
	 */
	public synchronized static ExchangeHelper getExchange() {
		
		if ( exchange == null ) exchange = new ExchangeHelper();
		return exchange;
	
	}
	
	/**
	 * Get the instance of the Global Logical Clock
	 * @return The Global Logical Clock currently in use
	 */
	protected static GlobalLogicalClock getGlobalClock() {
		return clock;
	}

	public static Integer getGlobalIndexForKey(Object key) {
		return distributed_map.getOrDefault(key, -1);
	}
	
	/**
	 * Get all hosts, as a collection of host names
	 * @return All host names used as MASS nodes
	 */
	public static Vector<String> getHosts() {
		return new Vector<>( allNodes.stream().map( MNode::getHostName ).collect( Collectors.toList() ) );
	}
	
	/**
	 * Get the filename of the log file, based in part on the node number and hostname
	 * @return The name of the file that should be used for logging
	 */
	public static String getLogFileName() {
		
		if ( thisNode == null ) return null;	// not initialized yet!
		
		// make sure hostname is cleansed to provide a safe filename fragment
		String safeHostname = thisNode.getHostName();
		if (safeHostname != null) {
			
			// dots mess up paths
			safeHostname = safeHostname.replace(".", "_");
			
		}
		
		String logFilename = getWorkingDirectory() + "/logs/" + "PID" + getMyPid() + "_" + safeHostname + "_result.txt";
		return logFilename;
		
	}

	/**
	 * Get the Logger instance, primarily for MASS applications to record messages to the same
	 * logger the library is using
	 * @return The logger
	 */
	public static Log4J2Logger getLogger() {
		return logger;
	}
	
	/**
	 * Get the MNode representation of the master node only
	 * @return The MNode representation of the master node
	 */
	public static MNode getMasterNode() {
		return allNodes.stream().filter( node -> node.isMaster() ).findFirst().orElse( null );
	}
	
    /**
	 * Get the messaging provider currently in use
	 * @return The messaging provider currently being used
	 */
	public static MASSMessaging getMessagingProvider() {
		return messenger;
	}
	
	/**
	 * Get any outstanding Agent migration requests
	 * @return Current Agent migration requests
	 */
	public static Vector<Vector<AgentMigrationRequest>> getMigrationRequests() {
		return migrationRequests;
	}

	/**
	 * Get hostname for this node
	 * @return hostname of thisNode
	 */
	public static String getMyHostname() {
		return thisNode.getHostName();
	}

	/**
	 * Get the PID (or node number) of this node
	 * @return The PID of this node
	 */
	public static int getMyPid() {
		
		// TODO - Need to throw an Exception if MASS hasn't been init'd yet!
		return thisNode.getPid();
		
	}

    /**
	 * Get Places object for a specific handle ID
	 * @param handle The ID of the Places object to retrieve
	 * @return The Places object with the matching handle ID
	 */
	public static Places getPlaces( int handle ) {
    	return ( Places )placesMap.get( handle );
    }
    
    /**
	 * Get the collection of Places located on this node
	 * @return Places located on this node
	 */
	public static Hashtable<Integer, PlacesBase> getPlacesMap() {
		return placesMap;
	}
    
    protected static List<Double[]> getRecordLines() {
		return recordLines;
	}

    protected static List<Double[]> getRecordPoints() {
		return recordPoints;
	}
	
	/**
     * Get all MNode objects representing remote nodes only
     * @return MNodes representing all remote nodes
     */
    public static Vector<MNode> getRemoteNodes() {
    	return remoteNodes;
    }
	
	/**
	 * Get any outstanding Remote Agent migration requests
	 * @return Current Remote Agent migration requests
	 */
    public static Vector<Vector<RemoteExchangeRequest>> getRemoteRequests() {
		return remoteRequests;
	}

	/**
	 * Get the total number of nodes in the cluster
	 * @return The number of nodes
	 */
	public static int getSystemSize() {
		
		// if nodes have been defined, use the number of nodes as the system size
		if (allNodes.size() > 0) return allNodes.size();
		
		// have the hosts been identified this way?
		if (hosts.size() > 0) return hosts.size();
		
		// TODO - should not need to set system size using constructor
		// must be using a legacy method of init, use the old method
		return systemSize;

	}

	/**
	 * Get the username of user who runs the current MASS program, used for multi-user feature
	 * @return The username
	 */
	public static String getUserName() {
		return thisNode.getUserName();
	}

	/**
	 * Get the collection of threads managed by MThread
	 * @return The threads currently being managed by MThread
	 */
	public static MThread[] getThreads() {
		return threads;
	}

    /**
	 * Get the directory ("MASS Home") that this node is working from
	 * @return The working directory
	 */
	public static String getWorkingDirectory() {
		return thisNode.getMassHome();
	};

	protected static void initDistributedData() {
		MASSBase.distributed_map = new MASSSimpleDistributedMap<Object, Integer>();
	}

    /**
	 * Initialize MThread and start child execution threads
	 * @param nThr The number of threads to start (will default to the number of CPU cores at a minimum)
	 * @return The number of threads to start
	 */
	public static boolean initializeThreads( int nThr ) {
		
		if ( initialized ) {
			
			logger.error("Error: MASS.init is already initialized" );
			return false;
		
		}

		int cores = ( nThr <= 0 ) ? getCores( ) : nThr;

		// all pthread_t structures
		threads = new MThread[ cores ];
		threads[0] = null; // reserved for the main thread

		// initialize Mthread's static variables
		MThread.init( );

		// now launch child threads
		synchronized( MThread.getLock() ) {
			MThread.setThreadCreated(0);
		}
		
		for ( int i = 1; i < cores; i++ ) {
			
			threads[i] = new MThread( i );
			threads[i].start( );
			
			while ( true ) {
				
				synchronized( MThread.getLock() ) {
					if ( MThread.getThreadCreated() == i )
						break;
				
				}
			
			}
		
		}

		logger.debug( "Initialized threads - # " + cores );

		initialized = true;
		return true;
	
	}
    
    /**
     * Initialize MASS_base, using an MNode object representing this node as the source for configuration
     * @param nodeConfig The MNode object representing this node
     */
    public static void initMASSBase(MNode nodeConfig) {
		
    	// TODO - everything assumes that a nodeConfig is supplied! Probably should throw IllegalArgumentException.
    	if (nodeConfig == null) return;
    	
    	MASSBase.thisNode = nodeConfig;
    	
		// Set hostname if not set previously
		if (thisNode.getHostName() == null) {
			thisNode.setHostName( utilities.getLocalHostname() );
		}
		
		// Set the current working directory to default value if not set previously
		if (thisNode.getMassHome() == null) {
		  thisNode.setMassHome( System.getProperty( "user.dir" ) );
		}

		// with options set, now configure logging
		logger.setLogFileName( getLogFileName() );
		
		// log options that have been set, now that there is a valid log filename
		logger.debug("Working directory set to {}", getWorkingDirectory());
		logger.debug("Hostname set to {}", thisNode.getHostName());

		// add MASS home to the list of URLs to be used by the object factory
		try {
			objectFactory.addUri(new File(MASSBase.getWorkingDirectory()).toURI().toString());
		} catch (Exception e) {
			logger.error("Exception caught while adding ObjectFactory URI",  e);
		}

		initDistributedData();

		logger.debug("MASSBase initialization complete");
	
    }
    
    /**
	 * Initialize MASS_base, "legacy" mode
	 * This method is also used by MProcess during the initialization process
	 * of the remote node
	 * @param name The hostname or IP address of this node
	 * @param myPid The PID assigned to this node
	 * @param nProc The total number of nodes in the cluster
	 * @param port The port number to use for communications with this node
	 */
	public static void initMASSBase( String name, int myPid, int nProc, int port) {
    	
    	// create a MNode representation of this node, only for init purposes (legacy mode)
    	MNode thisNode = new MNode();
    	thisNode.setHostName(name);
    	thisNode.setPid(myPid);
    	thisNode.setPort(port);    	
    	
    	// TODO - this is a hack. System size is the number of identified hosts, not some command-line argument.
    	systemSize = nProc;
    	
		// init from the MNode object
		initMASSBase(thisNode);

    }
    
	/**
     * Get the initialized status of this node
     * @return True, if this node has been initialized successfully
     */
	public static boolean isInitialized() {
		return initialized;
	}
    
	public static void reinitializeMap() {
		initDistributedData();
	}
    
    /**
	 * Reset the request counter
	 */
	public static void resetRequestCounter() {
		//requestCounter = 0;
	}

	/**
	 * Set the port number used for inter-node communications
	 * @param communicationPort The port number
	 */
	public static void setCommunicationPort(int communicationPort) {
		
		// can't set port to zero
		// TODO - should throw IllegalArgumentException
		if (communicationPort == 0) return;
		
		// not init'd yet?
		// TODO - should throw some form of Exception
		if (thisNode == null) return;
		
		thisNode.setPort( communicationPort );
	
	}

    /**
	 * Set the current AgentsBase this node is working with
	 * @param currentAgents The current AgentsBase object
	 */
	public static void setCurrentAgentsBase(AgentsBase currentAgents) {
		MASSBase.currentAgentsBase = currentAgents;
	}

    /**
	 * Set the current argument (supplied to MProcess)
	 * @param currentArgument The current argument to be used by MProcess
	 */
	public static void setCurrentArgument(Object currentArgument) {
		MASSBase.currentArgument = currentArgument;
	}
	
	/**
	 * Set the current function ID that Agents will execute
	 * @param currentFunctionId The current Agents function ID to execute
	 */
	public static void setCurrentFunctionId(int currentFunctionId) {
		MASSBase.currentFunctionId = currentFunctionId;
	}
	
	/**
	 * Set the current Message type enumeration, used by MThread
	 * @param currentMsgType The Message type enumeration to be used by MThread
	 */
    public static void setCurrentMsgType(Message.ACTION_TYPE currentMsgType) {
		MASSBase.currentMsgType = currentMsgType;
	}

	/**
	 * Set the current Places object to be worked on
	 * @param currentPlaces The current Places object
	 */
	public static void setCurrentPlacesBase(PlacesBase currentPlaces) {
		MASSBase.currentPlacesBase = currentPlaces;
	}

	/**
     * set the current returns from Places or Agents resulting from the last callAll
     * @param currentReturns Returns array result from callAll
     */
    public static void setCurrentReturns(Object[] currentReturns) {
		MASSBase.currentReturns = currentReturns;
	}

	/**
     * When current returns were already initialized, set partial current returns from 
     * Places or Agents resulting from the last callAll from a certain position
     * @param returnValues Returns array result from callAll
     * @param pos
     */
    public static void setCurrentReturns(Object[] returnValues, int pos) {

    	// if currentReturns has not been initialized, initialize it as currentReturns
    	if (MASSBase.currentReturns == null) {
    		MASSBase.currentReturns = returnValues;
    		return;
    	}
    	
    	// if currentReturns already initialized, copy returnValues to currentReturns from a certain starting position
    	if (returnValues.length > MASSBase.currentReturns.length) { //if currentReturns' size < return values
    		logger.error("ArrayOutOfIndex: size of initialized currentReturns < return values ");
    		return;
    	}
    
    	System.arraycopy(returnValues, 0, currentReturns, pos, returnValues.length);
    
    }
    
    /**
     * Set the current returns from Places or Agents resulting from the last callAll
     * @param currentReturns Returns array result from callAll
     */
    public static void setCurrentReturnsTemp( List< Object > currentReturnsTemp ) {
		MASSBase.currentReturnsTemp = currentReturnsTemp;
	}


	/**
     * Set the PlacesBase representing the destination for an Agent
     * @param destinationPlaces The destination PlacesBase
     */
    public static void setDestinationPlaces(PlacesBase destinationPlaces) {
		MASSBase.destinationPlaces = destinationPlaces;
	}

	/**
	 * Set the ExchangeHelper used by this instance of MASS_base
	 * @param exchangeHelper The ExchangeHelper used by this instance
	 */
	protected static void setExchange( ExchangeHelper exchangeHelper ) {
		
		exchange = exchangeHelper;
	
	}

	/**
     * Sets the hosts that MASS is using.
     * @param host_args
     */
    public synchronized static void setHosts( Vector<String> host_args ) {

    	if ( !hosts.isEmpty( ) ) {
    		// already initialized
    		return;

    	}

    	// register all hosts including myself
    	for ( String host : host_args ) {
   			logger.debug("MASS_base.setHosts: Adding host {}", host );
    		hosts.add( host );
    	}
    	
		logger.debug( "MASS_base.setHosts: System size = {}", getSystemSize() );

    	// instantiate remoteRequests: Vector< Vector<RemoteExchangeReques> >
    	// as well as migrationRequests for the purpose of agent migration.
    	remoteRequests = new Vector<Vector<RemoteExchangeRequest>>( );
    	migrationRequests = new Vector<Vector<AgentMigrationRequest>>( );

    	for ( int i = 0; i < getSystemSize(); i++ ) {
    		remoteRequests.add( new Vector<RemoteExchangeRequest>() );
    		migrationRequests.add( new Vector<AgentMigrationRequest>() );
    	}

    	// establish inter-MASS connection
    	if ( exchange == null) exchange = new ExchangeHelper();
    	exchange.establishConnection( getSystemSize(), thisNode.getPid(), hosts, thisNode.getPort() );

    }
	
	/**
	 * Set the initialized status of this node
	 * @param initialized The initialization complete status for this node
	 */
	public static void setInitialized(boolean initialized) {
		MASSBase.initialized = initialized;
	}
	
	/**
	 * Set outstanding Agent migration requests
	 * @param migrationRequests Current Agent migration requests
	 */
    public static void setMigrationRequests(
			Vector<Vector<AgentMigrationRequest>> migrationRequests) {
		MASSBase.migrationRequests = migrationRequests;
	}

	/**
	 * Set outstanding Remote Agent migration requests
	 * @param remoteRequests Current Remote Agent migration requests
	 */
   public static void setRemoteRequests(
			Vector<Vector<RemoteExchangeRequest>> remoteRequests) {
		MASSBase.remoteRequests = remoteRequests;
	}

	/**
	 * For remote hosts, force the system size since it is not aware of all the nodes in the cluster
	 * @param numNodes The total number of nodes in the cluster
	 */
	protected static void setSystemSize( int numNodes ) {
		systemSize = numNodes;
	}

	/**
	 * Set (override) the working directory ("MASS Home") for this node
	 * @param workingDirectory The new working directory for this node
	 */
	public static void setWorkingDirectory(String workingDirectory) {
		
		// has MASS been initialized yet?
		if (thisNode == null) return;
		
		thisNode.setMassHome( workingDirectory );
		
	}

	/**
	 * Set the distributed_map for this node, since it's accessing a shared graph that has been stored on cluster
	 * @param newdisMap The new distributed_map for this node
	 */
	public static void setDistributedMap(MASSSimpleDistributedMap<Object, Integer> newdisMap) {
		distributed_map = newdisMap;
	}

	/**
	* Logs the hosts MASS is using.
	*/
	public static void showHosts( ) {
    	
    	if( logger.isDebugEnabled() ) {
    		
    		String convert = "Hosts: ";
    		
    		for (MNode node : allNodes ) {
    			convert += "rank[" + node.getPid() + "] = " + node.getHostName() + " ";
    		}
    		
    		logger.debug( convert );
    	
    	}
    
    }

}