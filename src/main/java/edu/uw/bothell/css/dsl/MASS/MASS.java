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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.jcraft.jsch.Channel;

import edu.uw.bothell.css.dsl.MASS.factory.ObjectFactory;
import edu.uw.bothell.css.dsl.MASS.factory.SimpleObjectFactory;

/**
 *	MASS is responsible for the construction and deconstruction of the cluster. 
 */
public class MASS extends MASSBase {

	private static boolean printOutput = false;

	private static final int JschPort = 22;

	private static Utilities util = new Utilities( );  // used for channel creation

	// the list of libraries ("Jars") to load
    private static Set<String> libraries = new HashSet<String>();

	// the number of threads to spawn on each node (default to 1)
    private static int numThreads = 1;

	// default user credentials (can be overridden via XML)
    private static String defaultUsername;
	private static String defaultPassword;

	// name of file containing cluster node definitions
    private static String nodeFilePath = "nodes.xml";

	// object factories are singletons, so we'll use this opportunity to initialize it
    private static ObjectFactory objectFactory = SimpleObjectFactory.getInstance();
    
    // Async
    // number of node that return async result
    private static int LocalAgents[];

	//MASS debugger variables
	private static Places debuggerInstance;
	public static final int DEBUGGER_HANDLE = 99;


	/**
     * Add a library ("Jar") to be loaded by the classloader on each node
     * @param libraryName The name of the library to load
     */
    public static void addLibrary(String libraryName) {
    	
    	// add the library to the object factory
    	try {
    		objectFactory.addLibrary(libraryName);
    	}
    	catch (Exception e) {
        MASS.logException(null, e);
    	}

    	// remember the specified library so it can be set on remote nodes as well
    	libraries.add(libraryName);
    
    }
    
	static void barrierAllSlaves( ) { 
    	barrierAllSlaves( null, 0,  null ); 
    }

	static void barrierAllSlaves( int localAgents[] ) { 
    	barrierAllSlaves( null, 0, localAgents );
    }

    static void barrierAllSlaves( Object[] returnValues, int stripe ) {
    	barrierAllSlaves( returnValues, stripe, null ); 
    }
    
 	static void barrierAllSlaves( Object[] returnValues, int stripe, int localAgents[] ) {

    	// counts the agent population from each Mprocess
    	int nAgentsSoFar = ( localAgents != null ) ? localAgents[0] : 0;

    	// Synchronize with all slave processes
    	for ( int i = 0; i < getRemoteNodes().size( ); i++ ) {
    		if( printOutput == true )
    			System.err.println( "barrier waits for ack from " +
    					getRemoteNodes().get(i).getHostName( ) );

    		Message m = getRemoteNodes().get(i).receiveMessage( );

    		if( printOutput == true )
    			System.err.println( "barrier received a message from " +
    					getRemoteNodes().get(i).getHostName( ) +
    					"...message = " + m );

    		// check this is an Ack
    		if ( m.getAction( ) != Message.ACTION_TYPE.ACK ) {
    			System.err.println( "barrier didn't receive ack from rank " +
    					( i + 1 ) + " at " +
    					getRemoteNodes().get(i).getHostName( ) +
    					" message action type = " + m.getAction());
    			System.exit( -1 );
    		}

    		// retrieve arguments back from each Mprocess
    		// places.callAll( ) with return values
    		if ( returnValues != null ) {
    			if ( stripe > 0 && localAgents == null ) {

    				// check if the message is from the last mNode as
    				// the last mNode might have a remainder (stripe + rem)
    				// for simplicity, we just use the length of the returned
    				// array
    				int copyLength;
    				if ( i == getRemoteNodes().size( ) - 1 ) {
    					copyLength = ( (Object[]) m.getArgument( ) ).length;
    				} else {
    					copyLength = stripe;
    				}

    				// copy the partial array into the return_values array
    				System.arraycopy( m.getArgument( ), 0,
    								  returnValues, stripe * ( i + 1 ),
    								  copyLength );
    				}
    				if ( stripe == 0 && localAgents != null ) {
    					// agents.callAll( ) with return values
    					System.arraycopy( m.getArgument( ), 0,
    									  returnValues, nAgentsSoFar,
    									  localAgents[i + 1] );
    				}
    			}

    		// retrieve agent population from each Mprocess
    		if( printOutput == true ) {
    			System.err.println( "localAgents[" + (i + 1) +
    					"] = m.getAgentPopulation: "
    					+ m.getAgentPopulation( ) );
    		}

    		if ( localAgents != null ) {
    			localAgents[i + 1] = m.getAgentPopulation( );
    			nAgentsSoFar += localAgents[i + 1];
    		}

    		if ( printOutput == true )
    			System.err.println( "message deleted" );

    	}

    }
    
 	/**
 	 *	Finish computation, terminate remote processes, and perform cleanup and
 	 * 	disconnection operations.
 	 * 
 	 *  This method should be called when all computational work has been completed.
 	 */
 	public static void finish( ) {

    	MThread.resumeThreads( MThread.STATUS_TYPE.STATUS_TERMINATE );
    	MThread.barrierThreads( 0 );

    	if ( MASS.isConsoleLoggingEnabled() )
    		System.err.println( "MASS::finish: all MASS threads terminated" );

    	// Close connection and finish each mprocess
    	for ( MNode node : getRemoteNodes() ) {
    		// Send a finish messages
    		Message m = new Message( Message.ACTION_TYPE.FINISH );
    		node.sendMessage( m );
    	}

    	// Synchronize with all slaves
    	barrierAllSlaves( );

    	for ( MNode node : getRemoteNodes() )
    		node.closeMainConnection( );
      
    	MASSBase.getAsyncOutputThread().finish();
    	MASSBase.getAsyncInputThread().finish();

    	System.err.println( "MASS::finish: done" );

    }
    
    /**
	 * Get the default password for connecting to remote nodes
	 * @return The default login password
	 */
	public static String getDefaultPassword() {
		return defaultPassword;
	}
    
    /**
	 * Get the default username for connecting to remote nodes
	 * @return The default login username
	 */
	public static String getDefaultUsername() {
		return defaultUsername;
	}
    
    /**
	 * Get a collection of all library names to be used by the classloaders on each node
	 * @return The collection of library names
	 */
	public static Set<String> getLibraries() {
		return libraries;
	}

    /**
	 * Get the filename for the cluster node definition file
	 * @return The cluster node definition filename
	 */
	public static String getNodeFilePath() {
		return nodeFilePath;
	}
    
    /**
	 * Get the number of threads that will be spawned on each node
	 * @return The number of threads spawned
	 */
	public static int getNumThreads() {
		return numThreads;
	}

	// TODO - replace with a logger library hopefully
	public static boolean isConsoleLoggingEnabled() {
		return printOutput;
	}
	
	/**
	 * Initialize the MASS library (using settings made previously via setters).
	 * Calling this method effectively begins computation.
	 */
	public static void init() {

    	// attempt to load node definitions from specified file
    	if (getNodeFilePath() != null && getNodeFilePath().length() > 0) {

    		// attempt to open the specified file
    		File machineFile = new File(getNodeFilePath());
    		
    		// does the file actually exist?
    		if (!machineFile.canRead()) {

    			System.err.println( "machine file: " + getNodeFilePath() +
        				" does not exist or is not readable." );

        		System.exit( -1 );

    		}
    		
        	// is the machine file an XML document? 
    		if (getNodeFilePath().toLowerCase().contains("xml")) {
    			
    			// yes - filename specified is an XML document - get MNodes directly from the doc
    			try {

        			JAXBContext jaxbContext = JAXBContext.newInstance(Nodelist.class);
            		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            		Nodelist nodeList = (Nodelist) jaxbUnmarshaller.unmarshal(machineFile);
            		
            		// iterate through the nodes, adding each
            		for (MNode node : nodeList.getNodes()) {
            			addNode(node);
            		}
            		
    			} 

        		catch (JAXBException e) {

        			System.err.println( "Error initializing JAXB parser..." +
		    				e.getStackTrace());

		    		System.exit( -1 );
				
				}
    			
    		}
    		
    		else {
    			
    			// no - this machine file is the classic one-line-per-node format
    			
            	BufferedReader fileReader = null;

            	try {

            		fileReader = new BufferedReader( new InputStreamReader
            				( new BufferedInputStream( new FileInputStream( 
            						machineFile ) ) ) );

            		while( fileReader.ready( ) ) {
            			
            			// create a new MNode for each line in the file (these will all be remote nodes)
            			MNode node = new MNode();
            			node.setHostName( fileReader.readLine( ) );
            			addNode( node );
            			
            		}

            		fileReader.close();

            	} 

            	catch( Exception e ) {

            		System.err.println( "machine file: " + getNodeFilePath() +
            				" could not open." );

            		System.exit( -1 );

            	}
    			
    		}
    		
    	}
    	
    	// For debugging
    	if ( printOutput == true ) {
    		for ( MNode node : getRemoteNodes() )
    			System.err.println( "rank " + node.getPid() + ": " + 
    					node.getHostName() );
    	}

    	// if not already defined, create master node representation
    	if (getMasterNode() == null) {
    		
    		MNode masterNode = new MNode();
    		masterNode.setMaster(true);
    		addNode(masterNode);
    		
    	}
    	
    	// Initialize MASS_base.constants and identify the CWD.
    	if (getMasterNode() != null) {
    		
    		// init using Master node config
    		initMASSBase(getMasterNode());
    		
    	} else {
    	
    		// init using "old" method
        	initMASS_base( "localhost", 0, getAllNodes().size(), getCommunicationPort() );

    	}

    	// Launch remote processes
    	for (MNode node : getRemoteNodes()) {
    	
    		// set login credentials if not defined in the node config already
    		if (node.getUserName() == null) node.setUserName(getDefaultUsername());
    		if (node.getPassWord() == null) node.setPassWord(getDefaultPassword());
    		
    		// retrieve each canonical remote machine name
    		try {

    			InetAddress addr = InetAddress.getByName( node.getHostName() );
    			node.setHostName( addr.getCanonicalHostName( ) );
    			
    		} 

    		catch ( Exception e ) {

    			log( "wrong host name: " + node.getHostName() );
    			System.exit( -1 );

    		}

    		// For debugging
    		if ( printOutput == true )
    			System.err.println( "curHostName = " + node.getHostName() );

    		// Start a remote process
    		// java attributes and its jar files
    		StringBuilder commandBuilder = new StringBuilder();
    		
    		// add location of JVM if specified
    		if (node.getJavaHome() != null) commandBuilder.append(node.getJavaHome() + "/");
    		
    		// gotta specify the JVM
    		commandBuilder.append("java ");
    		
    		// TODO - add configurable heap memory sizes per node
    		//commandBuilder.append("-Xms2g ");
    		commandBuilder.append("-Xmx9g ");
    		
    		// set location of MASS.jar
    		commandBuilder.append("-cp ");
    		if (node.getMassHome() != null) commandBuilder.append(node.getMassHome() + "/");
    		commandBuilder.append("MASS.jar");
    		
    		// add any custom JARs specified
   			for( String customJar : getLibraries() ) {
    				
   				commandBuilder.append(":");
   				if (node.getMassHome() != null) commandBuilder.append(node.getMassHome() + "/");
   				commandBuilder.append(customJar);

   			}
    		
    		// add MASS home directory itself as part of the classpath
   			if (node.getMassHome() != null) {
   				commandBuilder.append(":");
	    		commandBuilder.append(node.getMassHome());
	    		commandBuilder.append(" ");
   			}

    		// MProcess and its arguments
    		commandBuilder.append("edu.uw.bothell.css.dsl.MASS.MProcess ");	// the program
    		commandBuilder.append(node.getHostName() + " ");	// 1st arg: hostName
    		commandBuilder.append(node.getPid() + " ");			// 2nd arg: pid
    		commandBuilder.append(getAllNodes().size() + " ");	// 3rd arg: #processes
    		commandBuilder.append(getNumThreads() + " ");   	// 4th arg: #threads
    		commandBuilder.append(getCommunicationPort() + " ");// 5th arg: MASS_PORT
    		commandBuilder.append(node.getMassHome());			// 6th arg: cur working dir

    		// debug
    		System.err.println( "MProcess on " + node.getHostName() +
    				" run with command: " + commandBuilder );

    		try {

    			Channel ssh2connection
    			= util.LaunchRemoteProcess( node.getHostName(),
    					JschPort,
    					commandBuilder.toString(),
    					node.getUserName(),
    					node.getPassWord() );

    			if ( ssh2connection == null )
    				throw new Exception( "JSCH channel not created" );

    			// A new remote process launched. 
    			// The corresponding Mnode created
    			node.setChannel(ssh2connection);
    			node.initialize();
    			
    		}

    		catch ( Exception e ) {

    			// connection failure
    			System.err.println( "MASS: error in connection to " + 
    					node.getHostName() + " " + e );
    			System.exit( -1 );

    		}

    	}

    	initializeThreads( getNumThreads() );
    	setInitialized(true);	// this node is now running

    	// Synchronize with all slave processes
    	for (MNode node : getRemoteNodes()) {
    	
    		if ( printOutput == true )
    			System.err.println( "init: wait for ack from " + 
    					node.getHostName( ) );

    		Message m = node.receiveMessage( );

    		if ( m.getAction( ) != Message.ACTION_TYPE.ACK ) {

    			System.err.println( "init didn't receive ack from rank " +
    					( node.getPid() ) + " at " +
    					node.getHostName( ) );
    			System.exit( -1 );

    		}

    	}

    	System.err.println( "MASS.init: done" );

    }
    
    /**
     * Initialize the MASS library using arguments. Calling this method effectively begins computation.
     * @param args An array of command-line style arguments
     * @param nProc Unused - maintained only for compatibility with previous versions. Now calculated from number of defined nodes.
     * @param nThr The number of threads to spawn on each node
     */
	public static void init( String[] args, int nProc, int nThr ) {
    	
    	// variable assignment
    	setDefaultUsername(args[0]);
    	setDefaultPassword(args[1]);
    	setNodeFilePath(args[2]);
    	setCommunicationPort(Integer.parseInt( args[3] ));
    	setNumThreads(nThr);
		//MASS.nProc = nProc;

    	try {

    		if ( args.length > 4 ) {

    			String jarList = args[4];
    			String next;
    			// args list needs to be a semicolon delimited string
    			StringTokenizer tokenizer = new StringTokenizer(jarList, ";");

    			while( tokenizer.hasMoreTokens( ) ) {

    				next = tokenizer.nextToken( );
    				addLibrary( next );

    			}

    		}

    	}

    	catch ( Exception e ) {
    		System.err.println( "Error during MASS.init() optional argument" +
    				"parsing " + e.getStackTrace());

    		System.exit( -1 );

    	}
    
    	// after parameters have been set, perform initialization
    	init();
    	
	}

    /**
	 * Set the default password for connecting to remote nodes
	 * @param defaultPassword The default password
	 */
	public static void setDefaultPassword(String defaultPassword) {
		MASS.defaultPassword = defaultPassword;
	}

    /**
	 * Set the default username for connecting to remote nodes
	 * @param defaultUsername The default login username
	 */
	public static void setDefaultUsername(String defaultUsername) {
		MASS.defaultUsername = defaultUsername;
	}
    
    /**
	 * Set the filename for the cluster node definition file
	 * @param nodeFilePath The cluster node definition filename
	 */
	public static void setNodeFilePath(String nodeFilePath) {
		MASS.nodeFilePath = nodeFilePath;
	}

	/**
	 * Set the number of threads to spawn on each node
	 * @param numThreads The number of threads to spawn
	 */
	public static void setNumThreads(int numThreads) {
		
		// can't set number of threads < 1
		if (numThreads < 1) return;
		
		MASS.numThreads = numThreads;
		
	}
	
	public static int[] getLocalAgents() {
	  return LocalAgents;
	}
	
	public static void setLocalAgents(int[] values) {
	  LocalAgents = values;
	}
	
	/**
	 * ONLY to call by Master node
	 * @return
	public static boolean getSlaveNodeAsyncCompleteness() {
	  if(MASS.isConsoleLoggingEnabled()) {
	    MASS.log("getEsimateSlaveNodeComplete() = " + getEsimateSlaveNodeComplete());
	  }
	  
	  if(getEsimateSlaveNodeComplete() >= getRemoteNodes().size()) {
	    MASS_base.setCachedSlaveNodeAsyncCompleteness(getAsyncOutputThread().requestSlaveNodeAsyncCompleteness());
	  }
	  return MASS_base.getCachedSlaveNodeAsyncCompleteness();
	}
   */

  public static void getRemoteAsyncResults() {
    if(!getRemoteNodes().isEmpty()) {
      LocalAgents = new int[getRemoteNodes().size()];
      getAsyncOutputThread().requestAsyncResults();
    }
  }
	
	/**
	 * Overloaded MASS init method to be used in conjunction with MASS debugger application.
	 * Port number must match port number entered in the debugging GUI.
	 *
	 * @param args username, password, machinefile, MASS port number
	 * @param nProc number of processes
	 * @param nThr number of threads
	 * @param placeHandle place handle
	 * @param agentHandle agent handle
	 * @param portNumber Debugging port number
	 */
	public static void init(String args[], int nProc,int nThr, int placeHandle, int agentHandle, int portNumber)
	{
		MASS.init(args, nProc, nThr);
		MASS.debugInit(placeHandle, agentHandle, portNumber);
	}

	/**
	 * Alternative to the debugging init function. debugInit should be called after a call to the
	 * non debugging init - MASS.init(String[], int, int). Thus method instantiates the MASS
	 * debugger. Prt number must match port number entered in Debugging GUI.
	 *
	 * @param placeHandle Place handle
	 * @param agentHandle Agent handle, 0 if none
	 * @param portNumber The port used to communicate with the debugger GUI
	 */
	public static void debugInit(int placeHandle, int agentHandle, int portNumber)
	{
		int[] handles = new int[]{placeHandle, agentHandle};
		Places debugger = new Places(DEBUGGER_HANDLE, "edu.uw.bothell.css.dsl.MASS.Debugger", handles, 1);
		debugger.callAll(Debugger.INIT);
		MASS.debuggerInstance = debugger;
		DebuggerBase.setPort(portNumber);
	}

	/**
	 * Syncs MASS application with GUI
	 *
	 * @throws InterruptedException
	 */
	public static void debugSync() throws InterruptedException
	{
		synchronized(DebuggerBase.sending_lock){
			if(DebuggerBase.sending_lock[0]){
				try{
					DebuggerBase.sending_lock.wait();
				}catch(Exception e){}
			}
		}
		synchronized(DebuggerBase.stop_lock){
			if(DebuggerBase.stop_lock[0]){
				try{
					DebuggerBase.stop_lock.wait();
				}catch(Exception e){}
			}
		}
	}

	/**
	 * Updates the debugging GUI with current state of place and agents. This method should
	 * be called at the end of the users simulation loop.
	 */
	public static void debugUpdate() throws InterruptedException
	{
		MASS.debuggerInstance.callAll(Debugger.FETCH_DEBUG_DATA, new Integer[2]);
		Debugger.sendDataToGUI(1);

		//Debugger.sendDataToGUI(2);
		//MASS.debuggerInstance.callAll(Debugger.fetchAgentDebugData_, new Integer[2]);
	}

}