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

import com.jcraft.jsch.Channel;

public class MASS extends MASS_base {
    
	private static final boolean printOutput = false;
    // private static final boolean printOutput = true;
    
	private static final int JschPort = 22;
    private static Utilities util;

    protected static Vector<MNode> mNodes;

    @SuppressWarnings("unused")
	public static void init( String[] args, int nProc, int nThr ) {
    	
    	Vector<String> hosts = new Vector<String>( ); // a set of host names
    	util = new Utilities( );                // used for channel creation

    	// variable assginment
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

    	// Read a given machine file
    	BufferedReader fileReader = null;

    	try {

    		fileReader = new BufferedReader( new InputStreamReader
    				( new BufferedInputStream( new FileInputStream( 
    						new File( machineFilePath ) ) ) ) );

    		while( fileReader.ready( ) )
    			hosts.add( fileReader.readLine( ) );  

    		fileReader.close();

    	} 

    	catch( Exception e ) {

    		System.err.println( "machine file: " + machineFilePath +
    				" could not open." );

    		System.exit( -1 );

    	}

    	// For debugging
    	if ( printOutput == true ) {
    		for ( int i = 0; i < hosts.size( ); i++ )
    			System.err.println( "rank " + (i + 1) + ": " + 
    					hosts.get( i ) );
    	}

    	// Handle nProc
    	if ( nProc < 0 || nProc > hosts.size( ) )
    		nProc = hosts.size( ) + 1; // count the master node

    	systemSize = nProc;

    	// Initialize MASS_base.constants and identify the CWD.
    	initMASS_base( "localhost", 0, nProc, port );

    	// For debugging
    	// System.err.println( "CUR_DIR = " + CUR_DIR );

    	// Launch remote processes
    	mNodes = new Vector<MNode>( );
    	int pid = 1; // a slave process id

    	for ( int i = 0; i < hosts.size( ); i++, pid++ ) {

    		// retrieve each canonical remote machine name
    		String currHostName = hosts.get(i);

    		try {

    			InetAddress addr = InetAddress.getByName( currHostName );
    			currHostName = addr.getCanonicalHostName( );

    		} 

    		catch ( Exception e ) {

    			log( "wrong host name: " + currHostName );
    			System.exit( -1 );

    		}

    		// For debugging
    		if ( printOutput == true )
    			System.err.println( "curHostName = " + currHostName );

    		// Start a remote process
    		// java attributes and its jar files
    		String command = "java -Xms1g -Xmx2g -cp " + CUR_DIR + "/MASS.jar:";

    		if ( customJarList != null ) {

    			for( String customJar : customJarList )
    				command += CUR_DIR + "/" + customJar + ":";

    		}

    		// MProcess and its arguments
    		command += CUR_DIR + " edu.uw.bothell.css.dsl.MASS.MProcess ";
    		command += currHostName; command += " ";// 1st arg: hostName
    		command += pid; command += " ";         // 2nd arg: pid
    		command += systemSize; command += " ";  // 3rd arg: #processes
    		command += nThr; command += " ";        // 4th arg: #threads
    		command += MASS_PORT; command += " ";   // 5th arg: MASS_PORT
    		command += CUR_DIR;                     // 6th arg: cur working dir

    		// debug
    		System.err.println( "MProcess on " + currHostName +
    				" run with command: " + command );

    		try {

    			Channel ssh2connection
    			= util.LaunchRemoteProcess( currHostName,
    					JschPort,
    					command,
    					username, password );

    			if ( ssh2connection == null )
    				throw new Exception( "JSCH channel not created" );

    			// A new remote process launched. 
    			// The corresponding Mnode created
    			mNodes.add( new MNode( currHostName, pid, ssh2connection ) );

    		}

    		catch ( Exception e ) {

    			// connection failure
    			System.err.println( "MASS: error in connection to " + 
    					currHostName + " " + e );
    			System.exit( -1 );

    		}

    	}

    	initializeThreads( nThr );
    	INITIALIZED = true;

    	// Synchronize with all slave processes
    	for ( int i = 0; i < hosts.size( ); i++ ) {

    		if ( printOutput == true )
    			System.err.println( "init: wait for ack from " + 
    					mNodes.get(i).getHostName( ) );

    		Message m = mNodes.get(i).receiveMessage( );

    		if ( m.getAction( ) != Message.ACTION_TYPE.ACK ) {

    			System.err.println( "init didn't receive ack from rank " +
    					( i + 1 ) + " at " +
    					mNodes.get(i).getHostName( ) );
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
    	for ( int i = 0; i < mNodes.size( ); i++ ) {
    		// Send a finish messages
    		Message m = new Message( Message.ACTION_TYPE.FINISH );
    		mNodes.get(i).sendMessage( m );
    	}

    	// Synchronize with all slaves
    	barrier_all_slaves( );

    	for ( int i = 0; i < mNodes.size( ); i++ )
    		mNodes.get(i).closeMainConnection( );

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
    	for ( int i = 0; i < mNodes.size( ); i++ ) {
    		if( printOutput == true )
    			System.err.println( "barrier waits for ack from " +
    					mNodes.get(i).getHostName( ) );
    		
    		Message m = mNodes.get(i).receiveMessage( );

    		if( printOutput == true )
    			System.err.println( "barrier received a message from " +
    					mNodes.get(i).getHostName( ) +
    					"...message = " + m );

    		// check this is an Ack
    		if ( m.getAction( ) != Message.ACTION_TYPE.ACK ) {
    			System.err.println( "barrier didn't receive ack from rank " + 
    					( i + 1 ) + " at " + 
    					mNodes.get(i).getHostName( ) +
    					" message action type = " + m.getAction());
    			System.exit( -1 );
    		}

    		// retrieve arguments back from each Mprocess
    		if ( return_values != null ) {
    			if ( stripe > 0 && localAgents == null ) {
    				// places.callAll( ) with return values
    				System.arraycopy( m.getArgument( ), 0, 
    						return_values, stripe * ( i + 1 ),
    						stripe );
    			}
    			if ( stripe == 0 && localAgents != null ) {
    				// agents.callAll( ) with return values
    				/*
		    System.err.println( "m = " + m +
					", m.getArgument( )" + m.getArgument()+
					", return_values = " + return_values +
					", nAgentsSoFar = " + nAgentsSoFar +
					", localAgents = " + localAgents +
					", localAgents[i + 1] = " +
					localAgents[i + 1] );
    				 */
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

}
