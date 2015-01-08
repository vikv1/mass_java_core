package edu.uw.bothell.css.dsl.MASS;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Hashtable;
import java.util.Vector;

import edu.uw.bothell.css.dsl.MASS.factory.ObjectFactory;
import edu.uw.bothell.css.dsl.MASS.factory.SimpleObjectFactory;

public class MASS_base {

    private static final boolean printOutput = false;
    // private static final boolean printOutput = true;

    private static Mthread[] threads;          // including main and children

    private static final String MASS_LOGS = "MASS_logs";

	private static int MASS_PORT;           // port # of the MASS library

    private static boolean initialized;  	// check if Mthreads are initialized

    private static String workingDirectory; // the current working directory

	private static String hostName;         // my local host name

	private static int myPid;               // my pid or rank

	private static int systemSize;          // # of processes (nodes) in the cluster

	private static FileOutputStream logger; // logger

	private static Vector<String> hosts = new Vector<String>( );    // all host names

	private static Hashtable<Integer, Places_base> placesMap = new Hashtable<Integer, Places_base>( );

	private static Hashtable<Integer, Agents_base> agentsMap = new Hashtable<Integer, Agents_base>( );

	private static Vector<Vector<RemoteExchangeRequest>> remoteRequests = new Vector<Vector<RemoteExchangeRequest>>( );

	private static Vector<Vector<AgentMigrationRequest>> migrationRequests = new Vector<Vector<AgentMigrationRequest>>( );

	@SuppressWarnings("unused")
	private static int requestCounter;

	private static Places_base currentPlaces = null;

	private static Agents_base currentAgents = null;

	private static ExchangeHelper exchange = new ExchangeHelper( );

	private static Places_base destinationPlaces;

	private static int currentFunctionId;

	private static Object currentArgument;

	private static Object[] currentReturns;

	//    private static Vector<int[]> currentDestinations;
	private static Message.ACTION_TYPE currentMsgType;

	private static Object log_lock;

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


	/**
     * Add a new node to the cluster
     * @param node The node to add to the cluster
     */
    public static void addNode(MNode node) {

    	// add the node to the collection of all nodes
    	allNodes.add(node);
    	
    	// if a remote, add to the collection of all remotes, or set the master if not
    	// this is done so remotes and master node configurations can be obtained quickly without a lookup
    	if (node.isMaster()) {

    		node.setPid(0);		// master node ALWAYS has a PID of zero
    		masterNode = node;
    		
    		
    	} else {
    		
    		// increment last PID and set for this remote node
    		lastPid++;
        	node.setPid(lastPid);

        	remoteNodes.add(node);
    	
    	}
    	
    }

	public static Agents getAgents( int handle ) {
    	return ( Agents )agentsMap.get( new Integer( handle ) );
    }
	public static Hashtable<Integer, Agents_base> getAgentsMap() {
		return agentsMap;
	}

	/**
     * Get all MNode objects, master and remotes
     * @return MNodes representing all nodes
	 */
	public static Vector<MNode> getAllNodes() {
		return allNodes;
	}
	public static int getCores( ) {
		// TODO: to be implemented
		return 2;
    }
	public static Agents_base getCurrentAgents( ) {
    	return currentAgents; 
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
    public static Places_base getCurrentPlaces( ) { 
    	return currentPlaces; 
    }
	public static Object[] getCurrentReturns() {
		return currentReturns;
	}

	public static Places_base getDestinationPlaces( ) { 
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
	
	public static int getMyPid( ) { return myPid; }
	public static Places getPlaces( int handle ) {
    	return ( Places )placesMap.get( new Integer( handle ) );
    }
	/**
	 * Get the collection of Places located on this node
	 * @return Places located on this node
	 */
	public static Hashtable<Integer, Places_base> getPlacesMap() {
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
		
		// must be using a legacy method of init, use the old method
		return systemSize;

	}
	public static Mthread[] getThreads() {
		return threads;
	}
/**
	 * Get the directory ("MASS Home") that this node is working from
	 * @return The working directory
	 */
	public static String getWorkingDirectory() {
		return workingDirectory;
	}

    @SuppressWarnings("unused")
	public static boolean initializeThreads( int nThr ) {
		
		if ( initialized ) {
			
			if( printOutput == true )
				MASS_base.log("Error: the MASS.init is already initializecd" );
			
			return false;
		
		}

		int cores = ( nThr <= 0 ) ? getCores( ) : nThr;

		// all pthread_t structures
		threads = new Mthread[ cores ];
		threads[0] = null; // reserved for the main thread

		// initialize Mthread's static variables
		Mthread.init( );

		// now launch child threads
		synchronized( Mthread.lock ) {
			Mthread.threadCreated = 0;
		}
		
		for ( int i = 1; i < cores; i++ ) {
			
			threads[i] = new Mthread( i );
			threads[i].start( );
			
			while ( true ) {
				
				synchronized( Mthread.lock ) {
					if ( Mthread.threadCreated == i )
						break;
				
				}
			
			}
		
		}

		if( printOutput == true )
			log( "Initialized threads - # " + cores );

		initialized = true;
		return true;
	
	}

    /**
     * Initialize MASS_base, using an MNode object representing this node as the source for configuration
     * @param nodeConfig The MNode object representing this node
     */
	// TODO - will need to specify system size potentially? Maybe pass in all MNodes?
    public static void initMASS_base(MNode nodeConfig) {
		
		MASS_base.hostName = nodeConfig.getHostName();
		MASS_base.myPid = nodeConfig.getPid();
		MASS_base.MASS_PORT = nodeConfig.getPort();
		MASS_base.setWorkingDirectory(nodeConfig.getMassHome());
		
		// Set the current working directory to default value if not set previously
		if (MASS_base.workingDirectory == null) MASS_base.workingDirectory = System.getProperty( "user.dir" );
		
		// add MASS home to the list of URLs to be used by the object factory
		try {
			objectFactory.addUri(new File(MASS_base.getWorkingDirectory()).toURI().toString());
		} catch (Exception e) {
			// TODO need to handle exceptions here better
		}
		
	}
    
    /**
	 * Initialize MASS_base, "legacy" mode
	 * @param name The hostname or IP address of this node
	 * @param myPid The PID assigned to this node
	 * @param nProc The total number of nodes in the cluster
	 * @param port The port number to use for communications with this node
	 */
	@Deprecated
	public static void initMASS_base( String name, int myPid, int nProc, int port) {
    	
    	// create a MNode representation of this node, only for init purposes (legacy mode)
    	MNode thisNode = new MNode();
    	thisNode.setHostName(name);
    	thisNode.setPid(myPid);
    	thisNode.setPort(port);    	
    	
		setSystemSize(nProc);
		
		// init from the MNode object
		initMASS_base(thisNode);

//		MASS_base.currentPlaces = null;
//		MASS_base.currentAgents = null;
//		MASS_base.requestCounter = 0;
//		MASS_base.hosts = new Vector<String>( );
//		MASS_base.exchange = new ExchangeHelper( );
	
//		placesMap = new Hashtable<Integer, Places_base>( );
//		agentsMap = new Hashtable<Integer, Agents_base>( );
//		remoteRequests = new Vector<Vector<RemoteExchangeRequest>>( );
//		migrationRequests = new Vector<Vector<AgentMigrationRequest>>( );
	
    }
    
    /**
     * Get the initialized status of this node
     * @return True, if this node has been initialized successfully
     */
	public static boolean isInitialized() {
		return initialized;
	}

    public static void log( String msg ) {

		try {
			
			if ( log_lock == null ) {
				
				log_lock = new Object( );
				
				if ( myPid > 0 )
					logger = new FileOutputStream( workingDirectory + "/" + 
							MASS_LOGS + "/PID" + 
							myPid + "_" + hostName + 
							"result.txt" );
			
			}

			synchronized( log_lock ) {
				
				if ( myPid == 0 ) {
					// The master directly prints out msg to standard error.
					System.err.println( msg );
				}
				
				else {
					
					// All the slaves print out msge to CUR_DIR/MASS_logs/.
					logger.write( msg.concat( "\n" ).getBytes( ) );
					logger.flush( );
				
				}
			
			}
		
		}
		
		catch( Exception e ) {	}
	
	}

    /**
	 * Reset the request counter
	 */
	public static void resetRequestCounter() {
		requestCounter = 0;
	}
	
	public static void setAgentsMap(Hashtable<Integer, Agents_base> agentsMap) {
		MASS_base.agentsMap = agentsMap;
	}

	public static void setCurrentAgents(Agents_base currentAgents) {
		MASS_base.currentAgents = currentAgents;
	}
	
	public static void setCurrentArgument(Object currentArgument) {
		MASS_base.currentArgument = currentArgument;
	}

	public static void setCurrentFunctionId(int currentFunctionId) {
		MASS_base.currentFunctionId = currentFunctionId;
	}

    public static void setCurrentMsgType(Message.ACTION_TYPE currentMsgType) {
		MASS_base.currentMsgType = currentMsgType;
	}

    /**
	 * Set the current Places object to be worked on
	 * @param currentPlaces The current Places object
	 */
	public static void setCurrentPlaces(Places_base currentPlaces) {
		MASS_base.currentPlaces = currentPlaces;
	};

    public static void setCurrentReturns(Object[] currentReturns) {
		MASS_base.currentReturns = currentReturns;
	}
    
    public static void setDestinationPlaces(Places_base destinationPlaces) {
		MASS_base.destinationPlaces = destinationPlaces;
	}
    
    @SuppressWarnings("unused")
    public static void setHosts( Vector<String> host_args ) {

    	if ( !hosts.isEmpty( ) ) {
    		// already initialized
    		return;

    	}

    	// register all hosts including myself
    	for ( int i = 0; i < host_args.size( ); i++ ) {

    		if ( printOutput == true )
    			log( host_args.get(i) );

    		hosts.add( host_args.get(i) );

    	}

    	// instantiate remoteRequests: Vector< Vector<RemoteExchangeReques> >
    	// as well as migrationRequests for the purpose of agent migration.
    	remoteRequests = new Vector<Vector<RemoteExchangeRequest>>( );
    	migrationRequests = new Vector<Vector<AgentMigrationRequest>>( );

    	for ( int i = 0; i < systemSize; i++ ) {
    		remoteRequests.add( new Vector<RemoteExchangeRequest>() );
    		migrationRequests.add( new Vector<AgentMigrationRequest>() );
    	}

    	// establish inter-MASS connection
    	exchange.establishConnection( systemSize, myPid, hosts, MASS_PORT );

    }
    
    /**
	 * Set the initialized status of this node
	 * @param initialized The initialization complete status for this node
	 */
	public static void setInitialized(boolean initialized) {
		MASS_base.initialized = initialized;
	}
    
    public static void setMigrationRequests(
			Vector<Vector<AgentMigrationRequest>> migrationRequests) {
		MASS_base.migrationRequests = migrationRequests;
	}
    
    public static void setRemoteRequests(
			Vector<Vector<RemoteExchangeRequest>> remoteRequests) {
		MASS_base.remoteRequests = remoteRequests;
	}
    
    //    public static Vector<int[]> getCurrentDestinations( ) {
    //	return currentDestinations; }

    /**
	 * Set the number of nodes in the cluster
	 * @param systemSize The number of nodes
	 */
	public static void setSystemSize(int systemSize) {
		MASS_base.systemSize = systemSize;
	}

    /**
	 * Set (override) the working directory ("MASS Home") for this node
	 * @param workingDirectory The new working directory for this node
	 */
	public static void setWorkingDirectory(String workingDirectory) {
		MASS_base.workingDirectory = workingDirectory;
	}

    @SuppressWarnings("unused")
    public static void showHosts( ) {
    	
    	if( printOutput == true ) {
    		
    		String convert = "hosts.....\n";
    		
    		for ( int i = 0; i < hosts.size( ); i++ ) {
    			convert += "rank[" + i + "] = " + hosts.get(i) + "\n";
    		}
    		
    		MASS_base.log( convert );
    	
    	}
    
    }

}