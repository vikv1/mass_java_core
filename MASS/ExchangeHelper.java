package MASS;

import java.util.Vector;
import java.net.*;
import java.io.*;

public class ExchangeHelper {
    //Used to toggle output for ExchangeHelper
    // private static final boolean printOutput = false;
    private static final boolean printOutput = true;  

    public void establishConnection( int size, int rank,
				     Vector<String> hosts, int port ) {
	try {
	    // prepare a server socket
	    ServerSocket server = new ServerSocket( port );
	    
	    // create sockets[]
	    sockets = new Socket[size];
	    
	    // accept connections from higher ranks
	    for ( int i = rank + 1; i < size; i++ ) {
		if ( printOutput == true )
		    MASS_base.log( "rank[" + rank + "] will accept " + i + 
				   "-th connection" );
		
		// accept a new connection
		Socket socket = server.accept( );
		socket.setReuseAddress( true );
		
		// retrieve the client socket ipaddress and port
		InetAddress addr = socket.getInetAddress( );
		String ipaddr = addr.getCanonicalHostName( );
		
		if ( printOutput == true ) {
		    MASS_base.log( "connection from " + ipaddr );
		}
		
		// idenfity the rank of this connection from ipaddr
		for ( int j = rank + 1; j < size; j++ ) {
		    if ( printOutput == true )
			MASS_base.log( "compare with " + hosts.get(j) );
		    
		    if ( hosts.get(j).equals( ipaddr ) ) {
			// matched and assigned this socket to rank j.
			sockets[j] = socket; 
			if ( printOutput == true ) {
			    MASS_base.log( "rank" + rank + 
					   "] accepted from rank[" +
					   j + "]:" + hosts.get(j) );
			}
			break;
		    }
		}
	    }
	}
	catch ( Exception e ) {
	    MASS_base.log( "exchange.establishConnection: server " + e );
	    System.exit( -1 );
	}

	// sends connection requests to lower ranks
	for ( int i = 0; i < rank; i++ ) {
	    for ( int j = 0; j < 5; j++ ) {
		try {
		    sockets[i] = new Socket( hosts.get(i), port );
		    sockets[i].setReuseAddress( true );
		    break;
		} catch ( Exception e1 ) {
		    MASS_base.log( "rank" + rank + "] " + j + 
				   "-th try to connect to" +
				   "rank [" + i + "]: " + hosts.get(i) );
		    try {
			Thread.currentThread( ).sleep( 1000 );
		    } catch ( Exception e2 ) { }
		}
	    }
	    if ( sockets[i] == null ) {
		MASS_base.log( "exchange.establishConnection: client failed" );
		System.exit( -1 );
	    }
	    if ( printOutput == true ) {
		MASS_base.log( "rank[" + rank + 
			       "] has connected to rank[" + i + "]: " + 
			       hosts.get(i) );
	    }
	}
    }

    public void sendMessage( int rank, Message exchangeReq ) {

	if ( printOutput == true )
	    MASS_base.log( "exchange.sendMessage will be sent to rank: " +
			   rank );

	try {
	    OOS[rank].writeObject( exchangeReq );
	    OOS[rank].flush( );
	} catch ( Exception e ) {
	    MASS_base.log ( "exchange.sendMessage to rank: " + rank + 
			    ". Error: " + e );
	}

	if ( printOutput == true )
	    MASS_base.log( "exchange.sendMessage has been sent to rank: " +
			   rank );
    }
    
    public Message receiveMessage( int rank ) {

	if ( printOutput == true )
	    MASS_base.log( "exchange.receiveMessage will receive from rank: " 
			   + rank );

	Message m = null;
	try {
	    m = ( Message )OIS[rank].readObject( );
	} catch ( Exception e ) {
	    MASS_base.log ( "exchange.receiveMessage from rank: " + rank + 
			    ". Error: " + e );
	}

	if ( m != null ) {
	    if ( printOutput == true )
		MASS_base.log( "exchange.receiveMessage received from rank: " 
			       + rank );
	    return m;
	} else {
	    if( printOutput == true )
		MASS_base.log( "exchange.receiveMessage error from rank[" + 
			       rank + "]" );
	    System.exit( -1 );
	}

	return null;
    }

    public void terminateConnection( int rank ) {
	// disconnect to lower ranks
	for ( int i = 0; i < rank; i++ ) {
	    try {
		sockets[i].close( );
	    } catch ( Exception e ) { }
	    if ( printOutput == true ) {
		MASS_base.log( "rank[" + rank + 
			       "] has disconnected to rank[" + i + "]: " );
	    }
	}
    }

    private static Socket socket;
    private static Socket[] sockets;
    private static ObjectInputStream[] OIS;
    private static ObjectOutputStream[] OOS;
}