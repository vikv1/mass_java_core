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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
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
import edu.uw.bothell.css.dsl.MASS.factory.ObjectFactory;
import edu.uw.bothell.css.dsl.MASS.factory.SimpleObjectFactory;
import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;
import edu.uw.bothell.css.dsl.MASS.logging.LogLevel;

/**
 *	MASS is responsible for the construction and deconstruction of the cluster. 
 */
public class MASS extends MASSBase {

	private static boolean printOutput = false;
	//private static boolean printOutput = true;

	private static Utilities util = new Utilities( );  // used for channel creation

	// the number of threads to spawn on each node (default to 1)
    private static int numThreads = 1;

	// default user credentials (can be overridden via XML)
    private static String defaultUsername;
	private static String defaultPassword;

	// name of file containing cluster node definitions
    private static String nodeFilePath = "nodes.xml";

	// object factories are singletons, so we'll use this opportunity to initialize it
    // yes - unused at this point right now...
    @SuppressWarnings("unused")
	private static ObjectFactory objectFactory = SimpleObjectFactory.getInstance();
    
    // Async
    // number of agents at rank i that returns async results
    private static int[] LocalAgents;

	// Logging
	private static Log4J2Logger logger = Log4J2Logger.getInstance();

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
    		logger.debug( "barrier waits for ack from {}",
    					getRemoteNodes().get(i).getHostName( ) );

    		Message m = getRemoteNodes().get(i).receiveMessage( );

    		logger.debug( "barrier received a message from " +
    					getRemoteNodes().get(i).getHostName( ) +
    					"...message = {}", m );

    		// check this is an Ack
    		if ( m.getAction( ) != Message.ACTION_TYPE.ACK ) {
    			logger.debug( "barrier didn't receive ack from rank " +
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
    		logger.debug( "localAgents[" + (i + 1) +
    					"] = m.getAgentPopulation: "
    					+ m.getAgentPopulation( ) );

    		if ( localAgents != null ) {
    			localAgents[i + 1] = m.getAgentPopulation( );
    			nAgentsSoFar += localAgents[i + 1];
    		}

    		logger.debug( "message deleted" );

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

    	logger.debug( "MASS::finish: all MASS threads terminated" );

    	// Close connection and finish each mprocess
    	for ( MNode node : getRemoteNodes() ) {
    		// Send a finish messages
    		Message m = new Message( Message.ACTION_TYPE.FINISH );
    		node.sendMessage( m );
    	}

    	// Synchronize with all slaves
    	barrierAllSlaves( );

    	for ( MNode node : getRemoteNodes() )
    		util.disconnectRemoteNode( node );
      
    	MASSBase.getAsyncOutputThread().finish();
    	MASSBase.getAsyncInputThread().finish();

    	logger.debug( "MASS::finish: done" );

    }
    
    /**
	 * Get the default password for connecting to remote nodes
	 * @return The default login password
	 */
 	@Deprecated
	protected static String getDefaultPassword() {
		return defaultPassword;
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
    			} catch (JAXBException e) {

        			System.err.println( "Error initializing JAXB parser..." +
		    				e.getStackTrace());

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
            		System.err.println( "machine file: " + getNodeFilePath() +
            				" could not open." );

            		System.exit( -1 );
            	}
    		}  		
    	} else {
			System.err.println(" No Node File Path Given" );
			System.exit( -1 );
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
        	initMASSBase( "localhost", 0, getAllNodes().size(), getCommunicationPort() );
    	}

    	// Launch remote processes
    	for (MNode node : getRemoteNodes()) {
    	
    		// set login credentials if not defined in the node config already
    		if (node.getUserName() == null) node.setUserName(getDefaultUsername());
    		//if (node.getPassWord() == null) node.setPassWord(getDefaultPassword());
    		
    		// retrieve each canonical remote machine name
    		try {

    			InetAddress addr = InetAddress.getByName( node.getHostName() );
    			node.setHostName( addr.getCanonicalHostName( ) );
    			
    		} catch ( Exception e ) {

    			logger.error( "Wrong host name: {}", node.getHostName(), e );
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
    		commandBuilder.append("-Xmx20g ");
    		
       		// add MASS home directory itself as part of the classpath
			if (node.getMassHome() != null) {
				commandBuilder.append("-cp " + node.getMassHome() + "/*.jar ");
			}
			// add MASS home directory itself as part of the classpath
			// if (node.getMassHome() != null) {
			// 	commandBuilder.append("-cp \"");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/etc/hadoop:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/commons-configuration2-2.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jettison-1.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/kerb-identity-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jackson-core-asl-1.9.13.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/curator-client-2.12.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/metrics-core-3.0.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jackson-databind-2.7.8.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jackson-xc-1.9.13.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/httpclient-4.5.2.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jetty-webapp-9.3.11.v20160721.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/commons-logging-1.1.3.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jetty-xml-9.3.11.v20160721.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jline-0.9.94.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/gson-2.2.4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/kerb-admin-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/xz-1.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jetty-security-9.3.11.v20160721.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/htrace-core4-4.1.0-incubating.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/kerb-core-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jetty-io-9.3.11.v20160721.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/kerby-pkix-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/commons-io-2.4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/kerb-crypto-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/slf4j-api-1.7.25.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/commons-codec-1.4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/guava-11.0.2.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/kerby-config-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/nimbus-jose-jwt-3.9.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/snappy-java-1.0.4.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jsch-0.1.54.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jcip-annotations-1.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/kerby-xdr-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/hadoop-annotations-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/commons-compress-1.4.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/re2j-1.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/protobuf-java-2.5.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/kerb-client-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jaxb-impl-2.2.3-1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jaxb-api-2.2.11.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/commons-math3-3.1.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/commons-lang-2.6.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/json-smart-1.1.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jackson-jaxrs-1.9.13.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jackson-annotations-2.7.8.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/javax.servlet-api-3.1.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/curator-recipes-2.12.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/paranamer-2.3.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/slf4j-log4j12-1.7.25.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jersey-servlet-1.19.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/hamcrest-core-1.3.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/commons-beanutils-1.9.3.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jetty-util-9.3.11.v20160721.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/avro-1.7.4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/hadoop-auth-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/kerby-util-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/commons-cli-1.2.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/httpcore-4.4.4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jersey-json-1.19.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jersey-core-1.19.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jetty-http-9.3.11.v20160721.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jetty-servlet-9.3.11.v20160721.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jersey-server-1.19.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/zookeeper-3.4.9.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jsp-api-2.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jsr305-3.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/stax2-api-3.1.4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/commons-net-3.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jackson-mapper-asl-1.9.13.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/kerb-util-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jackson-core-2.7.8.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jetty-server-9.3.11.v20160721.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/kerb-common-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jul-to-slf4j-1.7.25.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/kerb-server-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/jsr311-api-1.1.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/commons-lang3-3.3.2.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/woodstox-core-5.0.3.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/curator-framework-2.12.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/kerby-asn1-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/junit-4.11.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/commons-collections-3.2.2.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/netty-3.10.5.Final.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/kerb-simplekdc-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/mockito-all-1.8.5.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/lib/log4j-1.2.17.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/hadoop-common-3.0.0-alpha4-tests.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/hadoop-kms-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/hadoop-common-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/common/hadoop-nfs-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/commons-configuration2-2.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jettison-1.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/okio-1.4.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/kerb-identity-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/xercesImpl-2.9.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jackson-core-asl-1.9.13.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/curator-client-2.12.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jackson-databind-2.7.8.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jackson-xc-1.9.13.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/httpclient-4.5.2.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/xml-apis-1.3.04.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jetty-webapp-9.3.11.v20160721.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/commons-logging-1.1.3.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jetty-xml-9.3.11.v20160721.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jline-0.9.94.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/leveldbjni-all-1.8.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/gson-2.2.4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/kerb-admin-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/xz-1.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jetty-security-9.3.11.v20160721.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/htrace-core4-4.1.0-incubating.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/kerb-core-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jetty-io-9.3.11.v20160721.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/kerby-pkix-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/commons-io-2.4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/netty-all-4.0.23.Final.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/kerb-crypto-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/okhttp-2.4.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/commons-codec-1.4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/guava-11.0.2.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/kerby-config-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/nimbus-jose-jwt-3.9.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/snappy-java-1.0.4.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jsch-0.1.54.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jcip-annotations-1.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/kerby-xdr-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/hadoop-annotations-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/commons-compress-1.4.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/re2j-1.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/protobuf-java-2.5.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/kerb-client-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jaxb-impl-2.2.3-1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jaxb-api-2.2.11.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/commons-math3-3.1.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/commons-lang-2.6.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/json-smart-1.1.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jackson-jaxrs-1.9.13.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jackson-annotations-2.7.8.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/javax.servlet-api-3.1.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/curator-recipes-2.12.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/commons-daemon-1.0.13.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/paranamer-2.3.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jersey-servlet-1.19.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/commons-beanutils-1.9.3.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jetty-util-9.3.11.v20160721.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/avro-1.7.4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/hadoop-auth-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/kerby-util-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jetty-util-ajax-9.3.11.v20160721.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/commons-cli-1.2.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/httpcore-4.4.4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jersey-json-1.19.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jersey-core-1.19.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jetty-http-9.3.11.v20160721.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jetty-servlet-9.3.11.v20160721.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jersey-server-1.19.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/zookeeper-3.4.9.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jsr305-3.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/stax2-api-3.1.4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/commons-net-3.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jackson-mapper-asl-1.9.13.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/kerb-util-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jackson-core-2.7.8.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jetty-server-9.3.11.v20160721.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/kerb-common-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/kerb-server-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/jsr311-api-1.1.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/commons-lang3-3.3.2.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/woodstox-core-5.0.3.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/curator-framework-2.12.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/kerby-asn1-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/json-simple-1.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/commons-collections-3.2.2.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/netty-3.10.5.Final.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/kerb-simplekdc-1.0.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/lib/log4j-1.2.17.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/hadoop-hdfs-client-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/hadoop-hdfs-3.0.0-alpha4-tests.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/hadoop-hdfs-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/hadoop-hdfs-nfs-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/hadoop-hdfs-httpfs-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/hadoop-hdfs-native-client-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/hadoop-hdfs-native-client-3.0.0-alpha4-tests.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/hdfs/hadoop-hdfs-client-3.0.0-alpha4-tests.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/mapreduce/hadoop-mapreduce-client-jobclient-3.0.0-alpha4-tests.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/mapreduce/hadoop-mapreduce-client-common-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/mapreduce/hadoop-mapreduce-client-shuffle-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/mapreduce/hadoop-mapreduce-client-app-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/mapreduce/hadoop-mapreduce-client-hs-plugins-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/mapreduce/hadoop-mapreduce-examples-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/mapreduce/hadoop-mapreduce-client-hs-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/mapreduce/hadoop-mapreduce-client-jobclient-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/mapreduce/hadoop-mapreduce-client-core-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/mapreduce/hadoop-mapreduce-client-nativetask-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/hbase-hadoop-compat-1.2.6.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/jackson-module-jaxb-annotations-2.7.8.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/joni-2.1.2.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/servlet-api-2.5-6.1.14.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/fst-2.50.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/metrics-core-3.0.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/commons-csv-1.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/jasper-runtime-5.5.23.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/commons-el-1.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/jsp-2.1-6.1.14.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/jackson-jaxrs-json-provider-2.7.8.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/aopalliance-1.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/findbugs-annotations-1.3.9-1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/disruptor-3.3.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/hbase-hadoop2-compat-1.2.6.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/javassist-3.18.1-GA.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/commons-math-2.2.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/commons-httpclient-3.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/metrics-core-2.2.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/javax.inject-1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/hbase-annotations-1.2.6.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/guice-4.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/jsp-api-2.1-6.1.14.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/hbase-server-1.2.6.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/jcodings-1.0.8.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/hbase-client-1.2.6.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/guice-servlet-4.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/curator-test-2.12.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/jackson-jaxrs-base-2.7.8.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/hbase-procedure-1.2.6.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/jamon-runtime-2.4.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/hbase-protocol-1.2.6.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/java-util-1.9.0.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/zookeeper-3.4.9-tests.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/hbase-common-1.2.6.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/hbase-prefix-tree-1.2.6.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/jersey-client-1.19.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/htrace-core-3.1.0-incubating.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/json-io-2.5.1.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/jasper-compiler-5.5.23.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/lib/jersey-guice-1.19.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/hadoop-yarn-common-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/hadoop-yarn-server-web-proxy-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/hadoop-yarn-applications-distributedshell-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/hadoop-yarn-server-nodemanager-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/hadoop-yarn-server-tests-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/hadoop-yarn-client-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/hadoop-yarn-server-sharedcachemanager-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/hadoop-yarn-server-common-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/hadoop-yarn-server-timelineservice-hbase-tests-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/hadoop-yarn-server-timeline-pluginstorage-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/hadoop-yarn-registry-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/hadoop-yarn-server-applicationhistoryservice-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/hadoop-yarn-server-timelineservice-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/hadoop-yarn-api-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/hadoop-yarn-server-resourcemanager-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/hadoop-yarn-applications-unmanaged-am-launcher-3.0.0-alpha4.jar:");
			// 	commandBuilder.append("/CSSDIV/research/dslab/hadoop-3.0.0-alpha4/share/hadoop/yarn/hadoop-yarn-server-timelineservice-hbase-3.0.0-alpha4.jar:");
			// 	commandBuilder.append(node.getMassHome() + "/MASS_Parallel_Input_Tests.jar\" ");
			// }

    		// MProcess and its arguments
    		commandBuilder.append(MProcess.class.getCanonicalName() + " ");	// the program
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
//    			Channel ssh2connection = util.LaunchRemoteProcess( node.getHostName(),
//    					JschPort,
//    					commandBuilder.toString(),
//    					node.getUserName(),
//    					node.getPassWord() );
    			
//    			Channel ssh2connection = util.LaunchRemoteProcess( commandBuilder.toString(), node );
    			util.LaunchRemoteProcess( commandBuilder.toString(), node );

//    			if ( ssh2connection == null )
//    				throw new Exception( "JSCH channel not created" );

    			// A new remote process launched. 
    			// The corresponding Mnode created
//    			node.setChannel(ssh2connection);
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
     * IniNBA LIVE 2003 Soundtracktialize the MASS library using arguments. Calling this method effectively begins computation.
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

    	// after parameters have been set, perform initialization
    	init();
    	
	}

    /**
	 * Set the default password for connecting to remote nodes
	 * @param defaultPassword The default password
	 */
	@Deprecated
	protected static void setDefaultPassword(String defaultPassword) {
		MASS.defaultPassword = defaultPassword;
	}

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
	 * Set the number of threads to spawn on each node
	 * @param numThreads The number of threads to spawn
	 */
	public static void setNumThreads(int numThreads) {

		if (numThreads >= 1)
		{
			MASS.numThreads = numThreads;
		}
		
	}
	
	protected static int[] getLocalAgents() {
	  return LocalAgents;
	}
	
	protected static void setLocalAgents(int[] values) {
	  LocalAgents = values;
	}

  	public static void getRemoteAsyncResults() {
  		
  		if (!getRemoteNodes().isEmpty()) {
  			LocalAgents = new int[getRemoteNodes().size()];
  			getAsyncOutputThread().requestAsyncResults();
  		}

  	}
  
  /**
	 * Change logger level
	 * @param level The logging level
	 */
	public static void setLoggingLevel(LogLevel level) {
		logger.setLogLevel(level);
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
			//request = ( MASSRequest ) (( ObjectInputStream )inputStream ).readObject();
			request = ( MASSRequest )inputStream.readObject();
		} catch ( ClassNotFoundException e ) {
			e.printStackTrace();
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

		//( (ObjectOutputStream )outputStream ).writeObject( iniData );
		outputStream.writeObject( iniData );
		outputStream.flush();
	}

	public static void debugUpdate() throws IOException {

		MASSRequest request = null;

		try {
			request = ( MASSRequest ) inputStream.readObject();
		} catch ( ClassNotFoundException e ) {
			e.printStackTrace();
		}

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

	private static void injectPlace(MASSRequest request) {
		PlaceData updates = (PlaceData) request.getPacket();
		Place place = MASS.getCurrentPlacesBase().getPlaces()[updates.getIndex()];

		place.setDebugData(updates.getThisPlaceData());

		try {
			outputStream.writeObject(new UpdatePackage());
			outputStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void injectAgent( MASSRequest request ) {
		
		AgentData updates = ( AgentData )request.getPacket();

		//fantastic complexity...
		for( int i = 0; i < MASS.getCurrentPlacesBase().getPlaces().length; i++ ) {
			for( int j = 0; j < MASS.getCurrentPlacesBase().getPlaces()[i].getAgents().size(); j++ ) {
				Set<Agent> agents = MASS.getCurrentPlacesBase().getPlaces()[i].getAgents();
				for(Agent agent : agents) {
					if( updates.getId() == agent.getAgentId() ) {
						agent.setDebugData( updates.getDebugData() );
					}
				}
			}
		}

		try {
			// ( ObjectOutputStream )outputStream ).writeObject( new UpdatePackage() );
			outputStream.writeObject( new UpdatePackage() );
			outputStream.flush();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}

	private static void closeDebugConnection() {
		try {
			//todo - send null MASSPackage back first to prevent blocking
			outputStream.close();
			inputStream.close();
			client.close();
			socket.close();
		} catch ( IOException e ) {
			e.printStackTrace();
		}

		try {
			//( ( ObjectOutputStream )outputStream ).writeObject( new UpdatePackage() );
			outputStream.writeObject( new UpdatePackage() );
			outputStream.flush();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}

	private static void sendUpdate() {
		Place[] places = MASS.getCurrentPlacesBase().getPlaces();
		//Place[] places = MASSBase.getPlaces(placesHandle).getPlaces();
		//System.out.println(places.length);
		PlaceData[] updatedPlaces = new PlaceData[places.length];

		AgentData[] agentDataArr;

		for ( int i = 0; i < places.length; i++ ) {
			Number placeData = places[i].getDebugData();
			
			//if (placeData == null) System.out.println("placeData == null");
			
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
			//updatedPlaces[i] = new PlaceData( placeData, i, agents.size() != 0, agentDataArr );
		}

		UpdatePackage newPackage = new UpdatePackage();
		newPackage.setPlaceData( updatedPlaces );
		
		
		

		//write package
		try {
			//( ( ObjectOutputStream )outputStream ).writeObject( newPackage );
			outputStream.writeObject( newPackage );
			outputStream.flush();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}

	/**
	 * END MASS DEBUGGER METHODS
	 */
}