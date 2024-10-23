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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import edu.uw.bothell.css.dsl.MASS.MassData.AgentData;
import edu.uw.bothell.css.dsl.MASS.MassData.InitialData;
import edu.uw.bothell.css.dsl.MASS.MassData.MASSRequest;
import edu.uw.bothell.css.dsl.MASS.MassData.PlaceData;
import edu.uw.bothell.css.dsl.MASS.MassData.UpdatePackage;
import edu.uw.bothell.css.dsl.MASS.event.EventDispatcher;
import edu.uw.bothell.css.dsl.MASS.event.SimpleEventDispatcher;
import edu.uw.bothell.css.dsl.MASS.logging.LogLevel;
import edu.uw.bothell.css.dsl.MASS.messaging.MASSMessaging;

/**
 *	MASS is responsible for the construction and deconstruction of the cluster. 
 */
public class MASS extends MASSBase {

	// A reference to the event dispatcher, primarily for status and shutdown
	private static EventDispatcher eventDispatcher = SimpleEventDispatcher.getInstance();
	
    // Locks should have a timeout, if for no other reason than to trigger an exception and log message 
    public static final int LOCK_TIMEOUT = 0;

	private static Utilities util = new Utilities( );  // used for channel creation

	// the number of threads to spawn on each node (default to 1)
    private static int numThreads = 1;

	// default user credentials (can be overridden via XML)
    private static String defaultUsername;
//	private static String defaultPassword;

	// name of file containing cluster node definitions
    private static String nodeFilePath = "nodes.xml";
    
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
    		
    		MASS.getLogger().debug( "barrier waits for ack from " +
    					getRemoteNodes().get(i).getHostName( ) );

    		Message m = getRemoteNodes().get(i).receiveMessage( );

    		MASS.getLogger().debug( "barrier received a message from " +
    					getRemoteNodes().get(i).getHostName( ) +
    					"...message = " + m.getArgument( ) );

    		// check this is an Ack
    		if ( m.getAction( ) != Message.ACTION_TYPE.ACK ) {
    			
    			MASS.getLogger().error( "barrier didn't receive ack from rank " +
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
    		MASS.getLogger().debug( "localAgents[" + (i + 1) +
    					"] = m.getAgentPopulation: "
    					+ m.getAgentPopulation( ) );

    		if ( localAgents != null ) {
    			localAgents[i + 1] = m.getAgentPopulation( );
    			nAgentsSoFar += localAgents[i + 1];
    		}

    		MASS.getLogger().debug( "message deleted" );

    	}

    }
    
 	/**
 	 *	Finish computation, terminate remote processes, and perform cleanup and
 	 * 	disconnection operations.
 	 * 
 	 *  This method should be called when all computational work has been completed.
 	 */
 	public static void finish( ) {

		System.out.println("MASS Shutting Down...");

 		MASSBase.finish();
		MASSBase.finishSharedGraphPlaces();

    	MThread.resumeThreads( MThread.STATUS_TYPE.STATUS_TERMINATE );
    	MThread.barrierThreads( 0 );

    	MASS.getLogger().debug( "MASS::finish: all MASS threads terminated" );
    	
		// Close connection and finish each mprocess
    	for ( MNode node : getRemoteNodes() ) {
			
    		// Send finish messages
			System.out.println( "Sending shutdown request to " + node.getHostName() );
			MASS.getLogger().debug( "Sending shutdown request to {}", node.getHostName() );
    		Message m = new Message( Message.ACTION_TYPE.FINISH );
    		node.sendMessage( m );
    		
    	}

    	// Synchronize with all slaves
    	barrierAllSlaves( );

    	for ( MNode node : getRemoteNodes() )
    		util.disconnectRemoteNode(node);

    	// shutdown the event dispatcher
    	eventDispatcher.shutdown();
    	
    	// shutdown messaging system
    	MASS.getMessagingProvider().shutdown();
    	
    	MASS.getLogger().debug( "MASS::finish: done" );
		System.out.println("MASS Shutdown Finished");
		
    	// force termination of this node
		// TODO - this is an ugly hack, but it guarantees termination regardless of the state of things
		System.exit(0);
    }
    
    /**
	 * Get the default username for connecting to remote nodes
	 * @return The default login username
	 */
	protected static String getDefaultUsername() {
		return defaultUsername;
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

	/**
	 * Initialize the MASS library (using settings made previously via setters).
	 * Calling this method effectively begins computation.
	 */
	public static void init() {

		// start MASS, providing filename of configuration
		init( getNodeFilePath() );
		
	}
	
	/**
	 * Initialize the MASS library providing a Nodelist for configuration
	 * Calling this method effectively begins computation.
	 * @param nodes The Nodelist configuration to use
	 */
	public static void init( Nodelist nodes ) {
	
		// must provide nodes for configuration!
		if ( nodes == null ) {
			System.err.println( "No nodes provided for configuration!" );
    		System.exit( -1 );
		}

		// iterate through the nodes, adding each
		for ( MNode node : nodes.getNodes() ) {
			addNode( node );
		}
		
		// start MASS
		init();
		
	}
	
	/**
	 * Initialize the MASS library providing a specific filename for Nodelist XML or machines.txt format configuration document.
	 * Calling this method effectively begins computation.
	 * @param nodeFilename The full path and filename of the Nodelist XML or machines.txt configuration document to use
	 */
	public static void init( String nodeFilename ) {
		
		// set config file path (in case it wasn't set already, for compatibility)
		setNodeFilePath( nodeFilename );

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
    			} catch (JAXBException e) {

        			System.err.println( "Error initializing JAXB parser..." );
		    		e.printStackTrace();

		    		MASS.getLogger().error( "Error initializing JAXB parser...", e );
		    		System.exit( -1 );

    			}
    		} else {   			
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
            	} catch( Exception e ) {

            		System.err.println( "machine file: " + getNodeFilePath() + " could not open." );
		    		MASS.getLogger().error( "Machine file: {} could not be opened!", getNodeFilePath(), e );
            		System.exit( -1 );

            	}
    		
    		}  		

    	} else {
			System.err.println(" No Node File Path Given" );
			System.exit( -1 );
		}

    	for ( MNode node : getRemoteNodes() )
    			MASSBase.getLogger().debug( "rank " + node.getPid() + ": " + 
    					node.getHostName() );

    	// if not already defined, create master node representation
    	if (getMasterNode() == null) {
    		
    		MNode masterNode = new MNode();
    		masterNode.setMaster(true);
    		addNode(masterNode);
    	
    	}
    	
    	// validate configuration before attempting to start remote nodes
    	if ( validateNodeConfiguration() == false ) {
    		MASSBase.getLogger().error( "Node configuration validation problems found, unable to initialize!" );
    		System.out.println( "Node configuration validation problems found, unable to initialize!" );
    		System.out.println( "Refer to log files for details of validation exception(s)" );
			System.exit( -1 );
    	}
    	
    	// Initialize MASS_base.constants and identify the CWD.
    	if (getMasterNode() != null) {
    		
    		// init using Master node config
    		initMASSBase(getMasterNode());
    	} else {
    		// init using "old" method
        	initMASSBase( "localhost", 0, getAllNodes().size(), getCommunicationPort() );
    	}

    	// select an IP address and port number for cluster communications, if there are remote nodes configured
    	String clusterCommunicationsAddress = null;
//    	if ( getRemoteNodes().size() > 0 ) {
    		clusterCommunicationsAddress = MASSMessaging.getRandomMulticastAddress() + ":" + MASSMessaging.getRandomPort();
//    	}
    	
    	// Launch remote processes
    	for (MNode node : getRemoteNodes()) {
    	
    		// set login credentials if not defined in the node config already
    		if (node.getUserName() == null) node.setUserName(getDefaultUsername());
    		
    		// For debugging
    		MASSBase.getLogger().debug( "curHostName = " + node.getHostName() );

    		// Start a remote process
    		// java attributes and its jar files
    		StringBuilder commandBuilder = new StringBuilder();
    		
    		// add location of JVM if specified
    		if (node.getJavaHome() != null) commandBuilder.append(node.getJavaHome() + "/");
    		
    		// gotta specify the JVM
    		commandBuilder.append( "java" );

    		// add arguments to prevent module warnings with Hazelcast
    		commandBuilder.append( " --add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED" );

    		// configurable heap memory size per node
    		if ( node.getMaxHeapSize() != null ) commandBuilder.append( " -Xmx" + node.getMaxHeapSize() );

    		// add MASS home directory itself as part of the classpath
    		if ( node.getMassHome() != null ) commandBuilder.append( " -cp " + node.getMassHome() + "/*.jar" );
    		
    		// TODO - this is a nice trick, but doesn't work if running in an IDE during debugging
//   			if (node.getMassHome() != null) {
//   				String jarName = new java.io.File(MASS.class.getProtectionDomain()
//						.getCodeSource()
//						.getLocation()
//						.getPath()).getName();
//
//	    		commandBuilder.append("-cp \"" + node.getMassHome() + "/" + jarName + "\" ");
//   			}

    		// MProcess and its arguments
    		commandBuilder.append(" " + MProcess.class.getCanonicalName() + " ");	// the program
    		commandBuilder.append( MProcess.CMD_ARG_HOSTNAME + "=\"" + node.getHostName() + "\" " );
    		commandBuilder.append( MProcess.CMD_ARG_MYPID + "=" + node.getPid() + " " );
    		commandBuilder.append( MProcess.CMD_ARG_NPROC + "=" + getAllNodes().size() + " " );
    		commandBuilder.append( MProcess.CMD_ARG_NTHREADS + "=" + getNumThreads() + " " );
    		commandBuilder.append( MProcess.CMD_ARG_SERVER_PORT + "=" + getCommunicationPort() + " " );
    		commandBuilder.append( MProcess.CMD_ARG_WORKING_DIRECTORY + "=\"" + node.getMassHome() + "\" " );
			commandBuilder.append( MProcess.CMD_ARG_MAX_AGENTS + "=" + AgentSerializer.getInstance().getMaxNumberOfAgents() );

			// cluster communications address, if defined
			if ( clusterCommunicationsAddress != null ) {
				commandBuilder.append( " " + MProcess.CMD_ARG_CLUSTER_COMMS_ADDRESS + "=\"" + clusterCommunicationsAddress + "\" " );
			}

			// username is necessary for shared graph
			commandBuilder.append(" " + MProcess.CMD_ARG_USERNAME + "=" + node.getUserName() + " ");
			
    		// debug
    		System.err.println( "MProcess on " + node.getHostName() +
    				" run with command: " + commandBuilder );

    		try {
    			
    			util.LaunchRemoteProcess( commandBuilder.toString(), node );

    			node.initialize();
    			
    		} catch ( Exception e ) {
    			// connection failure
    			System.err.println( "MASS: error in connection to " + 
    					node.getHostName() + " " + e );
    			System.exit( -1 );
    		}
    	}

    	initializeThreads( getNumThreads() );
    	setInitialized(true);	// this node is now running

    	// initialize the messaging system (if no remote nodes, comms address is NULL - which is okay)
    	MASS.getMessagingProvider().init( clusterCommunicationsAddress );
    	
    	// initialize the global clock
    	MASS.getGlobalClock().init( eventDispatcher );
    	
    	// Synchronize with all slave processes
    	for (MNode node : getRemoteNodes()) {
    	
    		MASSBase.getLogger().debug( "init: wait for ack from " + node.getHostName( ) );

    		Message m = node.receiveMessage( );

    		if ( m.getAction( ) != Message.ACTION_TYPE.ACK ) {

    			System.err.println( "init didn't receive ack from rank " +
    					( node.getPid() ) + " at " +
    					node.getHostName( ) );
    			MASSBase.getLogger().error( "init didn't receive ack from rank " + ( node.getPid() ) + " at " + node.getHostName( ) );
    			System.exit( -1 );
    		}
    	}
    	
    	System.err.println( "MASS.init: done" );
    }

	/**
	 * Initialize the MASS library using arguments. Calling this method lets MASS use JAVA serialization.
	 * @param maxNumberOfAgents max number of agents that can actively run within a node.
	 */
	public static void init(int maxNumberOfAgents )
	{
		// store class definitions
		AgentSerializer agentSerializer = AgentSerializer.getInstance();
		agentSerializer.setMaxNumberOfAgents(maxNumberOfAgents);

		// after classes have been stored, perform initialization
		init();
	}

    /**
	 * Initialize the MASS library using arguments. Calling this method lets MASS use Kryo serialization.
	 * @param classes An array of class definitions that need to be registered by Kryo serializer.
	 * @param maxNumberOfAgents max number of agents that can actively run within a node.
	 */
	public static void init( Class[] classes, int maxNumberOfAgents )
	{
		// store class definitions
		AgentSerializer agentSerializer = AgentSerializer.getInstance();
		agentSerializer.setRegisteredClasses(classes);
		agentSerializer.setMaxNumberOfAgents(maxNumberOfAgents);

		// after classes have been stored, perform initialization
		init();
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
//    	setDefaultPassword(args[1]);
    	setNodeFilePath(args[2]);
    	setCommunicationPort(Integer.parseInt( args[3] ));
    	setNumThreads(nThr);

    	// after parameters have been set, perform initialization
    	init();
    	
	}

//    /**
//	 * Set the default password for connecting to remote nodes
//	 * @param defaultPassword The default password
//	 */
//	@Deprecated
//	protected static void setDefaultPassword(String defaultPassword) {
//		MASS.defaultPassword = defaultPassword;
//	}

    /**
	 * Set the default username for connecting to remote nodes
	 * @param defaultUsername The default login username
	 */
	protected static void setDefaultUsername(String defaultUsername) {
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
	 * Set the number of threads to spawn on each node.
	 * <p>
	 * The default number of threads per node is one. If set to a number
	 * greater than one, then that number of threads will be launched on
	 * each node. 
	 * @param numThreads The number of threads to spawn
	 */
	public static void setNumThreads(int numThreads) {

		if (numThreads >= 1)
		{
			MASS.numThreads = numThreads;
		}
		
	}

	/**
	 * Change logger level
	 * @param level The logging level
	 */
	public static void setLoggingLevel(LogLevel level) {
		MASS.getLogger().setLogLevel(level);
	}
	
	/**
	 * START MASS DEBUGGER METHODS
	 */
	
	//MASS debugger variables
	public static final int DEBUGGER_HANDLE = 99;
	private static ObjectInputStream inputStream;
	private static ObjectOutputStream outputStream;
	private static ServerSocket socket;
	private static Socket client;
	private static int placesHandle = 0;
	static int agentsHandle = 0;
	
	public static void debugInit( int pHandle, int aHandle, int port ) throws IOException {
		
		//TODO - get rid of all params
		agentsHandle = aHandle;
		placesHandle = pHandle;

		//connect to GUI
		socket = new ServerSocket( port );
		client = socket.accept();
		outputStream = new ObjectOutputStream( client.getOutputStream() );
		inputStream = new ObjectInputStream( client.getInputStream() );

		//completely unnecessary, don't remove though!
		@SuppressWarnings("unused")
		MASSRequest request;

		try {
			request = ( MASSRequest )inputStream.readObject();
		} catch ( ClassNotFoundException e ) {
			MASS.getLogger().error( "Class not found exception caught in debugInit!", e );
		}
		
		//end completely unnecessary stuff

		String placesName = null;
		String agentsName = null;
		Class<? extends Number> placeDataType = null;
		Class<? extends Number> agentDataType = null;
		boolean overloadsPlaceData = false;
		boolean overloadsAgentData = false;
		int x = 0;
		int y = 0;
		int numberOfAgents = 0;
		
		if( getPlaces( placesHandle ) != null ) {
			x = MASS.getPlaces( placesHandle ).getSize()[0];
			y = MASS.getPlaces( placesHandle ).getSize()[1];
			placesName = MASS.getPlaces( placesHandle ).getPlaces()[0].getClass().getSimpleName();
			overloadsPlaceData = ( MASS.getPlaces( placesHandle ).getPlaces()[0].getDebugData() != null );
			if( overloadsPlaceData ) {
				placeDataType = MASS.getPlaces( placesHandle ).getPlaces()[0].getDebugData().getClass();
			}
		}
		
		if( getAgents( aHandle ) != null ) {
			numberOfAgents =  MASS.getAgents( aHandle ).getInitPopulation();
			agentsName = MASS.getAgents( aHandle ).getAgents().get(0).getClass().getSimpleName();
			overloadsAgentData = ( MASS.getAgents( aHandle ).getAgents().get(0).getDebugData() != null );
			if( overloadsAgentData ) {
				agentDataType = MASS.getAgents( aHandle ).getAgents().get(0).getDebugData().getClass();
			}
		}

		InitialData iniData = new InitialData();
		iniData.setAgentsName( agentsName );
		iniData.setPlacesName( placesName );
		iniData.setPlacesX( x );
		iniData.setPlacesY( y );
		iniData.setNumberOfAgents( numberOfAgents );
		iniData.setNumberOfPlaces(x * y);
		iniData.setPlaceDataType( placeDataType );
		iniData.setAgentDataType( agentDataType );
		iniData.placeOverloadsGetDebugData( overloadsPlaceData );
		iniData.agentOverloadsGetDebugData( overloadsAgentData );

		outputStream.writeObject( iniData );
		outputStream.flush();
	}

	public static void debugUpdate() throws IOException {

		MASSRequest request = null;

		try {
			request = ( MASSRequest ) inputStream.readObject();
		} catch ( ClassNotFoundException e ) {
			MASS.getLogger().error( "Class not found exception caught in debugUpdate!", e );
		}

		if ( request != null ) {
		switch( request.getRequest() ) {
				case INITIAL_DATA:
					//TODO - remove debugInit, handle from here
					break;
				case UPDATE_PACKAGE:
					sendUpdate();
					break;
				case INJECT_PLACE:
					injectPlace( request );
					break;
				case INJECT_AGENT:
					injectAgent( request );
					break;
				case TERMINATE:
					closeDebugConnection();
					break;
			}
		}
	}

	private static void injectPlace(MASSRequest request) {
		PlaceData updates = (PlaceData) request.getPacket();
		Place place = MASS.getCurrentPlacesBase().getPlaces()[updates.getIndex()];

		place.setDebugData(updates.getThisPlaceData());

		try {
			outputStream.writeObject(new UpdatePackage());
			outputStream.flush();
		} catch (IOException e) {
			MASS.getLogger().error( "IO exception caught in injectPlace!", e );
		}
	}

	private static void injectAgent( MASSRequest request ) {
		
		AgentData updates = ( AgentData )request.getPacket();

		for ( Place place : MASS.getCurrentPlacesBase().getPlaces() ) {
			for( Agent agent : place.getAgents() ) {
				if( updates.getId() == agent.getAgentId() ) {
					agent.setDebugData( updates.getDebugData() );
				}
			}
		}

		try {
			outputStream.writeObject( new UpdatePackage() );
			outputStream.flush();
		} catch ( IOException e ) {
			MASS.getLogger().error( "IO exception caught in injectAgent!", e );
		}

	}

	private static void closeDebugConnection() {

		try {
			outputStream.writeObject( new UpdatePackage() );
			outputStream.flush();
		} catch ( IOException e ) {
			MASS.getLogger().error( "IO exception caught in closeDebugConnection, while sending UpdatePackage!", e );
		}

		try {
			//todo - send null MASSPackage back first to prevent blocking
			outputStream.close();
			inputStream.close();
			client.close();
			socket.close();
		} catch ( IOException e ) {
			MASS.getLogger().error( "IO exception caught in closeDebugConnection, while closing streams!", e );
		}

	}

	private static void sendUpdate() {
		Place[] places = MASS.getCurrentPlacesBase().getPlaces();
		PlaceData[] updatedPlaces = new PlaceData[places.length];

		AgentData[] agentDataArr;

		for ( int i = 0; i < places.length; i++ ) {
			Number placeData = places[i].getDebugData();
			
			Set<Agent> agents = places[i].getAgents();
			int j = 0;
			agentDataArr = new AgentData[agents.size()];

			for ( Agent agent : agents ) {
				agentDataArr[j] = new AgentData();
				agentDataArr[j].setDebugData( agent.getDebugData() );
				agentDataArr[j].setChildren( agent.getNewChildren() );
				agentDataArr[j].setId( agent.getAgentId() );
				agentDataArr[j].setIsAlive( agent.isAlive() );
				agentDataArr[j].setIndex(i);
				j++;
			}

			updatedPlaces[i] = new PlaceData();
			updatedPlaces[i].setAgentDataOnThisPlace( agentDataArr );
			updatedPlaces[i].setThisPlaceData( placeData );
			updatedPlaces[i].setHasAgents( agents.size() != 0 );
		}

		UpdatePackage newPackage = new UpdatePackage();
		newPackage.setPlaceData( updatedPlaces );
		
		//write package
		try {
			outputStream.writeObject( newPackage );
			outputStream.flush();
		} catch ( IOException e ) {
			MASS.getLogger().error( "IO exception caught in sendUpdate!", e );
		}
		
	}

	public static void resetClock() {
		getGlobalClock().reset();
	}

	public static long getClockValue() {
		return getGlobalClock().getValue();
	}
	
	private static boolean validateNodeConfiguration() {
		
		// assume that everything is fine at first
		boolean validationSuccess = true;
		
		// validate all node configurations
		Set<String> nodeValidationExceptions = getMasterNode().validate();
		for ( MNode node : getRemoteNodes() ) {
			nodeValidationExceptions.addAll( node.validate() );
		}

		// any validation failures?
		if ( nodeValidationExceptions.size() > 0 ) {

			// failure!
			validationSuccess = false;
			
			for ( String validationException : nodeValidationExceptions ) {
			
				String message = "Configuration exception: " + validationException;
				System.out.println( message );
				MASS.getLogger().error( message );
			
			}
			
		}
		
		return validationSuccess;
		
	}

	/**
	 * Receive values (num = 2) from remote nodes' MASS
	 * @param maxLevel
	 * @param numOfLeaf
	 */
	static void barrierAllSlaves(int[] maxLevel, int[] numOfLeaf) { 

		MASS.getLogger().debug( "barrierAllSlaves(int[] maxLevel, int[] numOfLeaf) remote size = " + getRemoteNodes().size());
		// Synchronize with all slave processes
		for ( int i = 0; i < getRemoteNodes().size( ); i++ ) {

			MASS.getLogger().debug( "barrier waits for ack from " + getRemoteNodes().get(i).getHostName( ) );

			Message m = getRemoteNodes().get(i).receiveMessage( );

			MASS.getLogger().debug( "barrier received a message from " + getRemoteNodes().get(i).getHostName( ) + "...message = " + m );

			// check this is an Ack
			if ( m.getAction( ) != Message.ACTION_TYPE.ACK ) {

				MASS.getLogger().error( "barrier didn't receive ack from rank " +
						( i + 1 ) + " at " + getRemoteNodes().get(i).getHostName( ) + " message action type = " + m.getAction());

				System.exit( -1 );
			} 

			if ( maxLevel != null && numOfLeaf != null) {
				maxLevel[i + 1] = m.getMaxLevel();
				numOfLeaf[i + 1] = m.getNumOfLeaf();
			} 

			// retrieve agent population from each Mprocess
			MASS.getLogger().debug( "maxLevel[" + (i + 1) + "] = m.getMaxLevel: "
					+ m.getMaxLevel() + ", numOfLeaf[" + (i + 1) + "] = m.getNumOfLeaf: " + m.getNumOfLeaf());

			MASS.getLogger().debug( "message deleted" );

		}

	}

	/**
	 * barrierAllSlaves for treePlaces
	 * @param returnValues
	 * @param numOfLeaf
	 * @param localAgents
	 */
	static void barrierAllSlaves_treePlaces( Object[] returnValues, int[] numOfLeaf, int localAgents[] ) {

    	// counts the agent population from each Mprocess
    	int nAgentsSoFar = ( localAgents != null ) ? localAgents[0] : 0;
		int destPos = numOfLeaf[0];
    	// Synchronize with all slave processes
    	for ( int i = 0; i < getRemoteNodes().size( ); i++ ) {
    		
    		MASS.getLogger().debug( "barrier waits for ack from " + getRemoteNodes().get(i).getHostName( ) );

    		Message m = getRemoteNodes().get(i).receiveMessage( );

    		MASS.getLogger().debug( "barrier received a message from " + getRemoteNodes().get(i).getHostName( ) +
    			"...message = " + m );

    		// check this is an Ack
    		if ( m.getAction( ) != Message.ACTION_TYPE.ACK ) {
    			
    			MASS.getLogger().error( "barrier didn't receive ack from rank " + ( i + 1 ) + " at " +
    				getRemoteNodes().get(i).getHostName( ) + " message action type = " + m.getAction());
    			System.exit( -1 );
    		}

    		// retrieve arguments back from each Mprocess
    		// places.callAll( ) with return values
    		if ( returnValues != null ) {
				MASS.getLogger().debug("i = " + i + ", numOfLeaf[" + (i + 1) + "]=" + numOfLeaf[i + 1]);
    			if ( numOfLeaf[i + 1] > 0 && localAgents == null ) {

    				// check if the message is from the last mNode as
    				// the last mNode might have a remainder (stripe + rem)
    				// for simplicity, we just use the length of the returned
    				// array
					int copyLength;
    				copyLength = numOfLeaf[i + 1];

					MASS.getLogger().debug("copylength = " + copyLength + ", destPos = " + destPos + 
						", m.getArgument().size = " + ((Object[]) m.getArgument()).length + ", returnValue.length = " + returnValues.length);
    				// copy the partial array into the return_values array
					System.arraycopy( m.getArgument(), 0, returnValues, destPos, copyLength );
					destPos += copyLength;
				}
				
    			if ( numOfLeaf[i + 1] == 0 && localAgents != null ) {
    				// agents.callAll( ) with return values
    				System.arraycopy( m.getArgument( ), 0, returnValues, nAgentsSoFar, localAgents[i + 1] );
				}
				
    		}

    		// retrieve agent population from each Mprocess
    		MASS.getLogger().debug( "localAgents[" + (i + 1) + "] = m.getAgentPopulation: " + m.getAgentPopulation( ) );

    		if ( localAgents != null ) {
    			localAgents[i + 1] = m.getAgentPopulation( );
    			nAgentsSoFar += localAgents[i + 1];
    		}

    		MASS.getLogger().debug( "message deleted" ); 
    	}
    	
	}

	static void barrierAllSlaves_treePlaces( Object[] returnValues, int[] numOfLeaf ) {
    	barrierAllSlaves_treePlaces( returnValues, numOfLeaf, null ); 
	}

}