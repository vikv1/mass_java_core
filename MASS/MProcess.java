package MASS;

import java.io.*;
import java.util.*;

public class MProcess {
    //  private static final boolean printOutput = false;
    private static final boolean printOutput = true;

    public MProcess( String hostName, int myPid, int nProc, int nThr, int port,
		     String curDir ) {
	this.hostName = hostName;
	this.myPid = myPid;
	this.nProc = nProc;
	this.nThr = nThr;
	MASS_base.initMASS_base( hostName, myPid, nProc, port );
	MASS_base.CUR_DIR = curDir; // mprocess manually changes it.

	// Create a logger
	try {
	    File massLogDir = new File( curDir + "/" + MASS_base.MASS_LOGS );
	    if( !massLogDir.exists( ) )
		massLogDir.mkdir( );
	} catch( Exception e ) {
	    System.exit( -1 );
	}

	MASS_base.initializeThreads( nThr );

	// set up a connection with the master process
	try {
	    MAIN_IOS = new ObjectInputStream( System.in );
	    MAIN_OOS = new ObjectOutputStream( System.out );
	}
	catch ( Exception e ) {
	    MASS_base.log( "MProcess.Mprocess: detected " + e );
	    System.exit( -1 );
	}
    }

    public void start( ) {
	MASS_base.log( "MProcess started" );

	// Synchronize with the master node first.
	sendAck( );

	boolean alive = true;
	while( alive ) {
	    // receive a new message from the master
	    Message m = receiveMessage( );

	    if ( printOutput == true )
		MASS_base.log( "A new message received: action = " +
			       m.getAction( ) );

	    // get prepared for the following arguments for PLACES_INITIALIZE
	    int[] size;                 // size[]
	    Vector<String> hosts = new Vector<String>( );
	    Object argument = null;
	    Places_base places = null;  // new Places
	    Agents_base agents = null;  // new Agents

	    // retrieve an argument
	    argument = m.getArgument( );

	    switch( m.getAction( ) ) {
	    case ACK:
		sendAck( );
		break;
		
	    case EMPTY:
		if( printOutput == true )
		    MASS_base.log( "EMPTY received!!!!" );
		sendAck( );
		break;
		
	    case FINISH:
		Mthread.resumeThreads( Mthread.STATUS_TYPE.STATUS_TERMINATE );
		// confirm all threads are done with finish
		Mthread.barrierThreads( 0 );
		MASS_base.exchange.terminateConnection( this.myPid );
		sendAck( );
		alive = false;
		if( printOutput == true )
		    MASS_base.log( "FINISH received and ACK sent" );
		break;

	    case PLACES_INITIALIZE:
		if ( printOutput == true )
		    MASS_base.log( "PLACES_INITIALIZE received" );
		// create a new Places
		size = m.getSize( );

		places = new Places_base( m.getHandle( ), m.getClassname( ),
					  m.getBoundaryWidth( ),
					  argument, size );

		for ( int i = 0; i < m.getHosts( ).size( ); i++ )
		    hosts.add( m.getHosts( ).get(i) );
		// establish all inter-node connections within setHosts( )
		MASS_base.setHosts( hosts );

		MASS_base.placesMap.put( new Integer( m.getHandle( ) ), 
						      places );
		sendAck( );
		if ( printOutput == true )
		    MASS_base.log( "PLACES_INITIALIZE completed and ACK sent");
		break;
	    }
	}
    }

    private void sendAck( ) {
	Message msg = new Message( Message.ACTION_TYPE.ACK );
	sendMessage( msg );
    }

    private void sendAck( int localPopulation ) {
	Message msg = new Message( Message.ACTION_TYPE.ACK, localPopulation );
	if( printOutput == true ) {
	    MASS_base.log( "msg.getAgentPopulation = " + 
			   msg.getAgentPopulation( ) );
	}
	sendMessage( msg );
    }

    private void sendReturnValues( Object argument ) {
	Message msg = new Message( Message.ACTION_TYPE.ACK, argument );
	sendMessage( msg );
    }

    private void sendReturnValues( Object argument, int localPopulation ) {
	Message msg = new Message( Message.ACTION_TYPE.ACK, argument,
				   localPopulation );
	sendMessage( msg );
    }

    private void sendMessage( Message msg ) {
	try {
	    MAIN_OOS.writeObject( msg );
	    MAIN_OOS.flush( );
	} catch ( Exception e ) {
	    MASS_base.log( "MProcess.sendMessage: " + e );
	    System.exit( -1 );
	}
    }

    Message receiveMessage( ) {
	try {
	    return ( Message )MAIN_IOS.readObject( );
	} catch ( Exception e ) {
	    MASS_base.log( "MProcess.receiveMessage: detected " + e );
	    System.exit( -1 );
	}
	return null;
    }

    public static void main( String[] args ) throws Exception {
        String hostName = args[0];
        int myPid = Integer.parseInt(args[1]);
        int nProc = Integer.parseInt(args[2]);
        int nThreads = Integer.parseInt(args[3]);
        int serverPort = Integer.parseInt(args[4]);
        String curDir = args[5];

        MProcess mprocess = new MProcess( hostName, myPid, nProc, nThreads, 
					  serverPort, curDir );
	mprocess.start( );
    }

    private String hostName;         // my local host name
    private int myPid;               // my pid or rank
    private int nProc;               // # processes
    private int nThr;                // # threads
    private Vector<String> hosts;    // all hosts participated in computation 

    private ObjectInputStream MAIN_IOS;  // input from the master process
    private ObjectOutputStream MAIN_OOS; // output to the master process
}