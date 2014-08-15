package edu.uw.bothell.css.dsl.MASS;

import java.io.FileOutputStream;
import java.util.Vector;
import java.util.Hashtable;

public class MASS_base {
    private static final boolean printOutput = false;
    // private static final boolean printOutput = true;

    public static void initMASS_base( String name, int myPid, int nProc,
				      int port) {
	MASS_base.hostName = name;
	MASS_base.myPid = myPid;
	MASS_base.systemSize = nProc;
	MASS_base.MASS_PORT = port;
	MASS_base.currentPlaces = null;
	MASS_base.currentAgents = null;
	MASS_base.requestCounter = 0;
	MASS_base.hosts = new Vector<String>( );
	MASS_base.exchange = new ExchangeHelper( );

	placesMap = new Hashtable<Integer, Places_base>( );
	agentsMap = new Hashtable<Integer, Agents_base>( );
	remoteRequests = new Vector<Vector<RemoteExchangeRequest>>( );
	migrationRequests = new Vector<Vector<AgentMigrationRequest>>( );

	// Get the current working directory
	MASS_base.CUR_DIR = System.getProperty( "user.dir" );
    }

    public static boolean initializeThreads( int nThr ) {
	if ( INITIALIZED ) {
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

	INITIALIZED = true;
	return true;
    }

    public static void log( String msg ) {
	try {
	    if ( log_lock == null ) {
		log_lock = new Object( );
		if ( myPid > 0 )
		    logger = new FileOutputStream( CUR_DIR + "/" + 
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

    public static int getCores( ) {
	// TODO: to be implemented
	return 2;
    }

    public static int getMyPid( ) { return myPid; };

    public static Places_base getCurrentPlaces( ) { return currentPlaces; }
    public static Places_base getDestinationPlaces( ) { 
	return destinationPlaces; }
    public static Agents_base getCurrentAgents( ) { return currentAgents; }
    public static int getCurrentFunctionId( ) { return currentFunctionId; }
    public static Object getCurrentArgument( ) { return currentArgument; }
    public static Message.ACTION_TYPE getCurrentMsgType( ) { 
	return currentMsgType; }
    public static Vector<int[]> getCurrentDestinations( ) {
	return currentDestinations; }

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

    public static void showHosts( ) {
	if( printOutput == true ) {
	    String convert = "hosts.....\n";
	    for ( int i = 0; i < hosts.size( ); i++ ) {
		convert += "rank[" + i + "] = " + hosts.get(i) + "\n";
	    }
	    MASS_base.log( convert );
	}
    }

    public static Mthread[] threads;          // including main and children
    public static final String MASS_LOGS = "MASS_logs";

    protected static int MASS_PORT;           // port # of the MASS library
    protected static boolean INITIALIZED;  // check if Mthreads are initialized
    protected static String CUR_DIR;          // the current working directory
    protected static String hostName;         // my local host name
    protected static int myPid;               // my pid or rank
    protected static int systemSize;          // # processes
    protected static FileOutputStream logger; // logger
    protected static Vector<String> hosts;    // all host names

    protected static Hashtable<Integer, Places_base> placesMap;
    protected static Hashtable<Integer, Agents_base> agentsMap;
    protected static Vector<Vector<RemoteExchangeRequest>> remoteRequests;
    protected static Vector<Vector<AgentMigrationRequest>> migrationRequests;
    protected static int requestCounter;
    protected static Places_base currentPlaces;
    protected static Agents_base currentAgents;
    protected static ExchangeHelper exchange;
    protected static Places_base destinationPlaces;
    protected static int currentFunctionId;
    protected static Object currentArgument;
    protected static Object[] currentReturns;
    protected static Vector<int[]> currentDestinations;
    protected static Message.ACTION_TYPE currentMsgType;

    private static Object log_lock;
}