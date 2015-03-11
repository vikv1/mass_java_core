package edu.uw.bothell.css.dsl.MASS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import edu.uw.bothell.css.dsl.MASS.factory.ObjectFactory;
import edu.uw.bothell.css.dsl.MASS.factory.SimpleObjectFactory;

public class MASS_base {

    private static Mthread[] threads;          // including main and children
    private static final String MASS_LOGS = "MASS_logs";
	private static int MASS_PORT = 3400;    // port # of the MASS library
    private static boolean initialized;  	// check if Mthreads are initialized
    private static String workingDirectory; // the current working directory
	private static String hostName;         // my local host name
	private static int myPid;               // my pid or rank
	private static FileOutputStream logger; // logger
	private static Vector<String> hosts = new Vector<String>( );    // all host names
	private static Hashtable<Integer, Places_base> placesMap = new Hashtable<Integer, Places_base>( );
	private static Hashtable<Integer, Agents_base> agentsMap = new Hashtable<Integer, Agents_base>( );
	private static Vector<Vector<RemoteExchangeRequest>> remoteRequests = new Vector<Vector<RemoteExchangeRequest>>( );
	private static Vector<Vector<AgentMigrationRequest>> migrationRequests = new Vector<Vector<AgentMigrationRequest>>( );
	//@SuppressWarnings("unused")
	//private static int requestCounter;
	private static Places_base currentPlaces = null;
	private static Agents_base currentAgents = null;
	private static ExchangeHelper exchange = new ExchangeHelper( );
	private static Places_base destinationPlaces;
	private static int currentFunctionId;
	private static Object currentArgument;
	private static Object[] currentReturns;
	private static Message.ACTION_TYPE currentMsgType;
	private static Object log_lock;

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
    
    /**
     * BEGIN ASync vars section
     */
    private static AsyncInputThread inputThread = null;
    private static AsyncOutputThread outputThread = null;
    
    /**
     *  Estimated number of completed slave node in order to
     *  reduce number of complete check in case master finish 
     * too early, issue check IFF this >= # of slaves
     */
    //private static AtomicInteger estimateSlaveNodeComplete = new AtomicInteger(0);
    /**
     *  Agents async migrate out and into this node
     */
    private static volatile int[] outAgents, inAgents;
    private static volatile int sourceAgentPid = -1;
    private static volatile Set<Integer> childAgentPids = new HashSet<Integer>();
    
    /**
     * END Async vars section
     */

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
	
	/**
	 * Get the number of cores (Hyperthreading included!) in this system
	 * @return The number of CPU cores
	 */
	public static int getCores( ) {
		return Runtime.getRuntime().availableProcessors();
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
		
		// have the hosts been identified this way?
		if (hosts.size() > 0) return hosts.size();
		
		// TODO - should not need to set system size using constructor
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

	public static boolean initializeThreads( int nThr ) {
		
		if ( initialized ) {
			
			if( MASS.isConsoleLoggingEnabled() == true )
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
		synchronized( Mthread.getLock() ) {
			Mthread.setThreadCreated(0);
		}
		
		for ( int i = 1; i < cores; i++ ) {
			
			threads[i] = new Mthread( i );
			threads[i].start( );
			
			while ( true ) {
				
				synchronized( Mthread.getLock() ) {
					if ( Mthread.getThreadCreated() == i )
						break;
				
				}
			
			}
		
		}

		if( MASS.isConsoleLoggingEnabled() == true )
			log( "Initialized threads - # " + cores );

		initialized = true;
		return true;
	
	}

    /**
     * Initialize MASS_base, using an MNode object representing this node as the source for configuration
     * @param nodeConfig The MNode object representing this node
     */
    public static void initMASS_base(MNode nodeConfig) {
		
		MASS_base.hostName = nodeConfig.getHostName();
		MASS_base.myPid = nodeConfig.getPid();
		setCommunicationPort(nodeConfig.getPort());
		if(nodeConfig.getMassHome() != null) {
		  setWorkingDirectory(nodeConfig.getMassHome());
		}
		
		// Set the current working directory to default value if not set previously
		if (MASS_base.workingDirectory == null) {
		  MASS_base.workingDirectory = System.getProperty( "user.dir" );
		}
    ensureLoggingFileExists();
		
		// add MASS home to the list of URLs to be used by the object factory
		try {
			objectFactory.addUri(new File(MASS_base.getWorkingDirectory()).toURI().toString());
		} catch (Exception e) {
      MASS.logException(null, e);
		}
		// Async section
    initAsyncCommunicationThreads();
    // ensure logging folders and file exist
    MASS.log("initMASS_base done");
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
    	
    	// TODO - this is a hack. System size is the number of identified hosts, not some
    	// command-line argument.
    	systemSize = nProc;
    	
		// init from the MNode object
		initMASS_base(thisNode);

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
				//if ( myPid > 0 )
					logger = new FileOutputStream( workingDirectory + "/" + 
							MASS_LOGS + "/PID" + 
							myPid + "_" + hostName + 
							"result.txt" );			
			}
      String lstring = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss.SSS").format(new Date()) 
      + " , " + Thread.currentThread().getName() +" , " + msg;

			synchronized( log_lock ) {
				
				if ( myPid == 0 ) {
					// The master directly prints out msg to standard error.
					System.err.println(lstring);
				}
				if(logger == null) {
				  logger = new FileOutputStream( workingDirectory + "/" + 
              MASS_LOGS + "/PID" + 
              myPid + "_" + hostName + 
              "result.txt" ); 
				}
					// All the slaves print out msge to CUR_DIR/MASS_logs/.
					logger.write( lstring.concat("\n").getBytes( ) );
					logger.flush( );
					logger.getFD().sync();
			}		
		}		
		catch( Exception e ) { 
		  logException(null, e);
    }	
	}
    
    public static void logException(String message, Throwable e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      log(message + "-" + sw.toString());
    }
  private static void ensureLoggingFileExists() {
    System.err.println("ensureLoggingFileExists: " + workingDirectory + "/" + 
              MASS_LOGS + "/PID" + 
              myPid + "_" + hostName + 
              "result.txt");
      File logFile = new File(workingDirectory + "/" + 
              MASS_LOGS + "/PID" + 
              myPid + "_" + hostName + 
              "result.txt");
      if(!logFile.isFile()) {
        System.err.println("ensureLoggingFileExists: !isFile");
        if (logFile.getParentFile().exists() || logFile.getParentFile().mkdirs()){
          System.err.println("ensureLoggingFileExists: about to create");
          try
          {
              logFile.createNewFile();
          }
          catch(IOException e)
          {
            logException(null, e);
          }
        }
      }
  }

    /**
	 * Reset the request counter
	 */
	public static void resetRequestCounter() {
		//requestCounter = 0;
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
    
    public static void setHosts( Vector<String> host_args ) {

    	if ( !hosts.isEmpty( ) ) {
    		// already initialized
    		return;

    	}

    	// register all hosts including myself
    	for ( int i = 0; i < host_args.size( ); i++ ) {
    		if ( MASS.isConsoleLoggingEnabled() ) {
    			log( "MASS_base.setHosts: Adding host " + host_args.get(i) );
    		}
    		hosts.add( host_args.get(i) );

    	}
    	
		if ( MASS.isConsoleLoggingEnabled() )
			log( "MASS_base.setHosts: System size = " + getSystemSize() );

    	// instantiate remoteRequests: Vector< Vector<RemoteExchangeReques> >
    	// as well as migrationRequests for the purpose of agent migration.
    	remoteRequests = new Vector<Vector<RemoteExchangeRequest>>( );
    	migrationRequests = new Vector<Vector<AgentMigrationRequest>>( );

    	for ( int i = 0; i < getSystemSize(); i++ ) {
    		remoteRequests.add( new Vector<RemoteExchangeRequest>() );
    		migrationRequests.add( new Vector<AgentMigrationRequest>() );
    	}

    	// establish inter-MASS connection
    	exchange.establishConnection( getSystemSize(), myPid, hosts, MASS_PORT );

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
    
    /**
	 * Set (override) the working directory ("MASS Home") for this node
	 * @param workingDirectory The new working directory for this node
	 */
	public static void setWorkingDirectory(String workingDirectory) {
	  System.err.println("setWorkingDir = " + workingDirectory);
		MASS_base.workingDirectory = workingDirectory;
	}

    public static void showHosts( ) {
    	
    	if( MASS.isConsoleLoggingEnabled() == true ) {
    		
    		String convert = "hosts.....\n";
    		
    		for ( int i = 0; i < hosts.size( ); i++ ) {
    			convert += "rank[" + i + "] = " + hosts.get(i) + "\n";
    		}
    		
    		MASS_base.log( convert );
    	
    	}
    
    }
    
    /**
     * BEGIN Async methods
     */
    
    public static AsyncInputThread getAsyncInputThread()
    {
      return inputThread;
    }
    
    public static AsyncOutputThread getAsyncOutputThread()
    {
      return outputThread;
    }
    
    public static void initAsyncCommunicationThreads() {
      MASS.log("init Async Communication Threads");
      inputThread = new AsyncInputThread(MASS_PORT + 1);
      outputThread = new AsyncOutputThread(MASS_PORT + 1);
      inputThread.start();
      outputThread.start();
    }

    public static void prepareAsyncExecution(Agents_base agents, int[] fIds) {
      setCurrentAgents(agents);
      Mthread.setAgentBagSize(currentAgents.getAgents().size());
      
      currentAgents.setAsyncFuncList(fIds);
      currentAgents.resetChildAsyncIndex();
      currentAgents.resetCompleteQueue();
      
      currentAgents.asyncQueueClear();
      for(int i = 0; i < currentAgents.getAgents().size_unreduced(); i++) {
        currentAgents.asyncQueueAdd(i);
        currentAgents.getAgents().get(i).setAsyncFuncListIndex(0);
        currentAgents.getAgents().get(i).resetAsyncResults();
        currentAgents.getAgents().get(i).setMyAsyncOriginalPid(getMyPid());
        currentAgents.getAgents().get(i).setMyOriginalAsyncIndex(i);
        currentAgents.getAgents().get(i).setCurrentIndex(i);
        currentAgents.getAgents().get(i).setParentAgents(currentAgents);
      }
      outputThread.setAgentHandle(agents.getHandle());
      outputThread.setPlaceHandle(agents.getPlacesHandle());
      outAgents = new int[getSystemSize()];
      inAgents = new int[getSystemSize()];
      for(int i = 0; i < outAgents.length; i++) {
        outAgents[i] = 0;
        inAgents[i] = 0;
      }
      currentAgents.setResultRequestFromMaster(false);
      sourceAgentPid = -1;
      childAgentPids.clear();
    }
    
    /*public static void resetEstimateSlaveNodeComplete() {
      estimateSlaveNodeComplete.set(0);
    }
    
    public static int getEsimateSlaveNodeComplete() {
      return estimateSlaveNodeComplete.get();
    }
    
    public static int incrementEstimateSlaveNodeComplete() {
      if(MASS.isConsoleLoggingEnabled()) {
        MASS.log("getEsimateSlaveNodeComplete() increment");
      }
      return estimateSlaveNodeComplete.incrementAndGet();
    }
    
    public static int decrementEstimateSlaveNodeComplete() {
      if(MASS.isConsoleLoggingEnabled()) {
        MASS.log("getEsimateSlaveNodeComplete() decrement");
      }
      return estimateSlaveNodeComplete.decrementAndGet();
    }
    

    public static boolean getCachedSlaveNodeAsyncCompleteness() {
      if(MASS.isConsoleLoggingEnabled()) {
        MASS.log("getCached complete = " + cachedAllAsyncNodeComplete);
      }
      return cachedAllAsyncNodeComplete;
    }
    
    public static void setCachedSlaveNodeAsyncCompleteness(boolean value) {
      cachedAllAsyncNodeComplete = value;
    } */
    
    public static Set<Integer> getChildAgentPids() {
      return childAgentPids;
    }
    
    public static int getSourceAgentPid() {
      return sourceAgentPid;
    }
    
    public static void setSourceAgentPid(int value) {
      sourceAgentPid = value;
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
		return MASS_PORT;
	}

    /**
	 * Set the port number used for inter-node communications
	 * @param communicationPort The port number
	 */
	public static void setCommunicationPort(int communicationPort) {
		
		// can't set port to zero
		if (communicationPort == 0) return;
		
		MASS_PORT = communicationPort;
	
	}

 /* public static void notifyMasterOfCompleteness() {
    outputThread.notifyMasterOfCompleteness();
  }*/
}