package edu.uw.bothell.css.dsl.MASS;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.jcraft.jsch.Channel;

public class MASS extends MASS_base {
    
	private static final boolean printOutput = false;
    // private static final boolean printOutput = true;
    
	private static final int JschPort = 22;
    private static Utilities util;

    // the collection of all nodes
    private static Vector<MNode> allNodes = new Vector<MNode>( );
    
    // for performance, collection of all remote nodes
    private static Vector<MNode> remoteNodes = new Vector<MNode>();
    
    // for performance, the master node
    private static MNode masterNode = null;
    
    // remember the last PID used
    private static int lastPid = 0;

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
    
    @SuppressWarnings("unused")
	public static void init( String[] args, int nProc, int nThr ) {
    	
    	util = new Utilities( );                // used for channel creation

    	// variable assignment
    	String username = args[0];
    	String password = args[1];
    	String machineFilePath = args[2];
    	int port = Integer.parseInt( args[3] );

    	// load any custom jars
    	ArrayList<String> customJarList = null; // storage for custom jars

    	try {

    		if ( args.length > 4 ) {

    			customJarList = new ArrayList<String>();
    			String jarList = args[4];
    			String next;
    			// args list needs to be a semicolon delimited string
    			StringTokenizer tokenizer = new StringTokenizer(jarList, ";");

    			while( tokenizer.hasMoreTokens( ) ) {

    				next = tokenizer.nextToken( );
    				customJarList.add( next );

    			}

    		}

    	}

    	catch ( Exception e ) {

    		System.err.println( "Error during MASS.init() optional argument" +
    				"parsing " + e.getStackTrace());

    		System.exit( -1 );

    	}

    	// attempt to load node definitions from specified file
    	if (machineFilePath != null && machineFilePath.length() > 0) {

    		// attempt to open the specified file
    		File machineFile = new File(machineFilePath);
    		
    		// does the file actually exist?
    		if (!machineFile.canRead()) {

    			System.err.println( "machine file: " + machineFilePath +
        				" does not exist or is not readable." );

        		System.exit( -1 );

    		}
    		
        	// is the machine file an XML document? 
    		if (machineFilePath.toLowerCase().contains("xml")) {
    			
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

            		System.err.println( "machine file: " + machineFilePath +
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

    	// Handle nProc
    	if ( nProc < 0 || nProc > getAllNodes().size( ) )
    		nProc = getAllNodes().size(); // count the master node and all remotes

    	systemSize = nProc;

    	// Initialize MASS_base.constants and identify the CWD.
    	initMASS_base( "localhost", 0, nProc, port );

    	// For debugging
    	// System.err.println( "CUR_DIR = " + CUR_DIR );

    	// Launch remote processes
    	for (MNode node : getRemoteNodes()) {
    	
    		// set login credentials if not defined in the node config already
    		if (node.getUserName() == null) node.setUserName(username);
    		if (node.getPassWord() == null) node.setPassWord(password);
    		
    		// set default MASS directory if not defined already per node
    		if (node.getMassHome() == null) node.setMassHome(CUR_DIR);
    		
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
    		if (node.getJavaHome() != null) commandBuilder.append(node.getJavaHome());
    		
    		// gotta specify the JVM
    		commandBuilder.append("java ");
    		
    		// TODO - add configurable heap memory sizes per node
    		commandBuilder.append("-Xms1g ");
    		commandBuilder.append("-Xmx2g ");
    		
    		// set location of MASS.jar
    		commandBuilder.append("-cp ");
    		commandBuilder.append(node.getMassHome());
    		commandBuilder.append("MASS.jar");
    		
    		//= "java -Xms1g -Xmx2g -cp " + CUR_DIR + "/MASS.jar:";

    		// add any custom JARs specified
    		if ( customJarList != null ) {

    			for( String customJar : customJarList ) {
    				
    				commandBuilder.append(":");
    				commandBuilder.append(node.getMassHome());
    				commandBuilder.append(customJar);

    			}
    		
    		}

    		// add MASS home directory itself as part of the classpath
    		commandBuilder.append(":");
    		commandBuilder.append(node.getMassHome());
    		commandBuilder.append(" ");

    		// MProcess and its arguments
    		commandBuilder.append("edu.uw.bothell.css.dsl.MASS.MProcess ");	// the program
    		commandBuilder.append(node.getHostName() + " ");	// 1st arg: hostName
    		commandBuilder.append(node.getPid() + " ");		// 2nd arg: pid
    		commandBuilder.append(systemSize + " ");  				// 3rd arg: #processes
    		commandBuilder.append(nThr + " ");        				// 4th arg: #threads
    		commandBuilder.append(MASS_PORT + " ");   				// 5th arg: MASS_PORT
    		commandBuilder.append(node.getMassHome());		// 6th arg: cur working dir

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

    	initializeThreads( nThr );
    	INITIALIZED = true;

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
    
    @SuppressWarnings("unused")
	public static void finish( ) {

    	Mthread.resumeThreads( Mthread.STATUS_TYPE.STATUS_TERMINATE );
    	Mthread.barrierThreads( 0 );

    	if ( printOutput == true )
    		System.err.println( "MASS::finish: all MASS threads terminated" );

    	// Close connection and finish each mprocess
    	for ( MNode node : getRemoteNodes() ) {
    		// Send a finish messages
    		Message m = new Message( Message.ACTION_TYPE.FINISH );
    		node.sendMessage( m );
    	}

    	// Synchronize with all slaves
    	barrier_all_slaves( );

    	for ( MNode node : getRemoteNodes() )
    		node.closeMainConnection( );

    	System.err.println( "MASS::finish: done" );

    }

    static void barrier_all_slaves( ) { 
    	barrier_all_slaves( null, 0,  null ); 
    }

    static void barrier_all_slaves( int localAgents[] ) { 
    	barrier_all_slaves( null, 0, localAgents );
    }

    static void barrier_all_slaves( Object[] return_values, int stripe ) {
    	barrier_all_slaves( return_values, stripe, null ); 
    }
    
    @SuppressWarnings("unused")
	static void barrier_all_slaves( Object[] return_values, int stripe,
    		int localAgents[] ) {

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
    		if ( return_values != null ) {
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
    								  return_values, stripe * ( i + 1 ),
    								  copyLength );
    				}
    				if ( stripe == 0 && localAgents != null ) {
    					// agents.callAll( ) with return values
    					System.arraycopy( m.getArgument( ), 0,
    									  return_values, nAgentsSoFar,
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
     * Get all MNode objects, master and remotes
     * @return MNodes representing all nodes
	 */
	public static Vector<MNode> getAllNodes() {
		return allNodes;
	}

    /**
	 * Get the MNode representation of the master node only
	 * @return The MNode representation of the master node
	 */
	public static MNode getMasterNode() {
		return masterNode;
	}

	/**
     * Get all MNode objects representing remote nodes only
     * @return MNodes representing all remote nodes
     */
    public static Vector<MNode> getRemoteNodes() {
    	return remoteNodes;
    }
    
}