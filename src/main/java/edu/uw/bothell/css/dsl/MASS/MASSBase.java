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

import java.io.File;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import edu.uw.bothell.css.dsl.MASS.factory.ObjectFactory;
import edu.uw.bothell.css.dsl.MASS.factory.SimpleObjectFactory;
import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;

/**
 * MASS_base maintains references to all Places, Agents, and mNode instances within the cluster.
 * Methods are provided to allow access to remote objects.
 */
public class MASSBase {

    private static MThread[] threads;          // including main and children
    private static boolean initialized;  	// check if Mthreads are initialized
	private static Vector<String> hosts = new Vector<String>( );    // all host names
	private static Hashtable<Integer, PlacesBase> placesMap = new Hashtable<Integer, PlacesBase>( );
	private static Hashtable<Integer, AgentsBase> agentsMap = new Hashtable<Integer, AgentsBase>( );
	private static Vector<Vector<RemoteExchangeRequest>> remoteRequests = new Vector<Vector<RemoteExchangeRequest>>( );
	private static Vector<Vector<AgentMigrationRequest>> migrationRequests = new Vector<Vector<AgentMigrationRequest>>( );
	private static PlacesBase currentPlacesBase = null;
	private static AgentsBase currentAgentsBase = null;
	private static ExchangeHelper exchange = new ExchangeHelper( );
	private static PlacesBase destinationPlaces;
	private static int currentFunctionId;
	private static Object currentArgument;
	private static Object[] currentReturns;
	private static Message.ACTION_TYPE currentMsgType;
	private static MNode thisNode;			// this node configuration

	// TODO - this is dumb. Calculate from number of hosts identified.
	private static int systemSize;          // # of processes (nodes) in the cluster (temporary!)
	
	// the collection of all nodes
    private static Vector<MNode> allNodes = new Vector<MNode>( );

	// for performance, collection of all remote nodes
    private static Vector<MNode> remoteNodes = new Vector<MNode>();

	// for performance, the master node
    private static MNode masterNode = null;

	// remember the last PID used
    private static int lastPid = 0;
    
    // object factories are singletons, continue configuration within this class
    private static ObjectFactory objectFactory = SimpleObjectFactory.getInstance();
    
    // logging
    private static Log4J2Logger logger = Log4J2Logger.getInstance();
    
    // helper classes
    private static Utilities utilities = new Utilities();

    /**
     *  Agents async migrate out and into this node
     */
    private static volatile int[] outAgents, inAgents;
    
    /**
     * END Async vars section
     */

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
    		masterNode = node;

    		logger.debug("This node is the MASTER node");
    		
    	} else {

    		logger.debug("This node is a REMOTE node");

    		// increment last PID and set for this remote node
    		lastPid++;
        	node.setPid(lastPid);

        	remoteNodes.add(node);
    	
    	}
    	
    }

	public static Agents getAgents( int handle ) {
    	return ( Agents )agentsMap.get( new Integer( handle ) );
    }
	
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
	 * Get the number of cores (Hyperthreading included!) in this system
	 * @return The number of CPU cores
	 */
	public static int getCores( ) {
		return Runtime.getRuntime().availableProcessors();
    }
	
	public static AgentsBase getCurrentAgentsBase( ) {
    	return currentAgentsBase;
    }
	
	public static Object getCurrentArgument( ) { 
    	return currentArgument;
    }
	
	public static int getCurrentFunctionId( ) { 
    	return currentFunctionId; 
    }
	
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
	
    public static Object[] getCurrentReturns() {
		return currentReturns;
	}

	public static PlacesBase getDestinationPlaces( ) { 
    	return destinationPlaces; 
    }
	
	/**
	 * Get the ExchangeHelper used by this instance of MASS_base
	 * @return The ExchangeHelper used by this instance
	 */
	public static ExchangeHelper getExchange() {
		return exchange;
	}
	
	public static Vector<String> getHosts() {
		return hosts;
	}
	
	/**
	 * Get the MNode representation of the master node only
	 * @return The MNode representation of the master node
	 */
	public static MNode getMasterNode() {
		return masterNode;
	}
	
	public static Vector<Vector<AgentMigrationRequest>> getMigrationRequests() {
		return migrationRequests;
	}
	
	/**
	 * Get the PID (or node number) of this node
	 * @return The PID of this node
	 */
	public static int getMyPid() {
		
		// TODO - Need to throw an Exception if MASS hasn't been init'd yet!
		return thisNode.getPid();
		
	};
	
	
	public static Places getPlaces( int handle ) {
    	return ( Places )placesMap.get( new Integer( handle ) );
    }
	
	/**
	 * Get the collection of Places located on this node
	 * @return Places located on this node
	 */
	public static Hashtable<Integer, PlacesBase> getPlacesMap() {
		return placesMap;
	}
	
	/**
     * Get all MNode objects representing remote nodes only
     * @return MNodes representing all remote nodes
     */
    public static Vector<MNode> getRemoteNodes() {
    	return remoteNodes;
    }
	
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
	
	public static MThread[] getThreads() {
		return threads;
	}

	/**
	 * Get the directory ("MASS Home") that this node is working from
	 * @return The working directory
	 */
	public static String getWorkingDirectory() {
		return thisNode.getMassHome();
	}

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
    
		logger.debug("MASSBase initialization complete");
	
    }
    
    /**
	 * Initialize MASS_base, "legacy" mode
	 * @param name The hostname or IP address of this node
	 * @param myPid The PID assigned to this node
	 * @param nProc The total number of nodes in the cluster
	 * @param port The port number to use for communications with this node
	 */
	//@Deprecated
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

    /**
	 * Reset the request counter
	 */
	public static void resetRequestCounter() {
		//requestCounter = 0;
	}
	
	public static void setAgentsMap(Hashtable<Integer, AgentsBase> agentsMap) {
		MASSBase.agentsMap = agentsMap;
	}

	public static void setCurrentAgentsBase(AgentsBase currentAgents) {
		MASSBase.currentAgentsBase = currentAgents;
	}
	
	public static void setCurrentArgument(Object currentArgument) {
		MASSBase.currentArgument = currentArgument;
	}

	public static void setCurrentFunctionId(int currentFunctionId) {
		MASSBase.currentFunctionId = currentFunctionId;
	}

    public static void setCurrentMsgType(Message.ACTION_TYPE currentMsgType) {
		MASSBase.currentMsgType = currentMsgType;
	}

    /**
	 * Set the current Places object to be worked on
	 * @param currentPlaces The current Places object
	 */
	public static void setCurrentPlacesBase(PlacesBase currentPlaces) {
		MASSBase.currentPlacesBase = currentPlaces;
	};

    public static void setCurrentReturns(Object[] currentReturns) {
		MASSBase.currentReturns = currentReturns;
	}
    
    public static void setDestinationPlaces(PlacesBase destinationPlaces) {
		MASSBase.destinationPlaces = destinationPlaces;
	}
    
    /**
     * Sets the hosts that MASS is using.
     * @param host_args
     */
    public static void setHosts( Vector<String> host_args ) {

    	if ( !hosts.isEmpty( ) ) {
    		// already initialized
    		return;

    	}

    	// register all hosts including myself
    	for ( int i = 0; i < host_args.size( ); i++ ) {
   			logger.debug("MASS_base.setHosts: Adding host {}", host_args.get(i) );
    		hosts.add( host_args.get(i) );
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
    	exchange.establishConnection( getSystemSize(), thisNode.getPid(), hosts, thisNode.getPort() );

    }
    
    /**
	 * Set the initialized status of this node
	 * @param initialized The initialization complete status for this node
	 */
	public static void setInitialized(boolean initialized) {
		MASSBase.initialized = initialized;
	}
    
    public static void setMigrationRequests(
			Vector<Vector<AgentMigrationRequest>> migrationRequests) {
		MASSBase.migrationRequests = migrationRequests;
	}
    
    public static void setRemoteRequests(
			Vector<Vector<RemoteExchangeRequest>> remoteRequests) {
		MASSBase.remoteRequests = remoteRequests;
	}
    
    /**
	 * Set (override) the working directory ("MASS Home") for this node
	 * @param workingDirectory The new working directory for this node
	 */
	public static void setWorkingDirectory(String workingDirectory) {
		
		// has MASS been initialized yet?
		if (thisNode == null) return;
		
		//		System.err.println("setWorkingDir = " + workingDirectory);
		thisNode.setMassHome( workingDirectory );
		
	}

	/**
	* Logs the hosts MASS is using.
	*/
	public static void showHosts( ) {
    	
    	if( logger.isDebugEnabled() ) {
    		
    		String convert = "Hosts: ";
    		
    		for ( int i = 0; i < hosts.size( ); i++ ) {
    			convert += "rank[" + i + "] = " + hosts.get(i) + " ";
    		}
    		
    		logger.debug( convert );
    	
    	}
    
    }
    
    public static int[] getOutAsyncAgents() {
      return outAgents;
    }
    
    public static int[] getInAsyncAgents() {
      return inAgents;
    }

    /**
     * END Async methods
     */

    /**
	 * Get the port number used for inter-node communications
	 * @return The port number
	 */
	public static int getCommunicationPort() {
		return thisNode.getPort();
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

}