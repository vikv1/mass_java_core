package edu.uw.bothell.css.dsl.MASS;

import java.util.Vector;
import java.net.*;
import java.io.*;

public class ExchangeHelper {

	//Used to toggle output for ExchangeHelper
    private static final boolean printOutput = false;
    // private static final boolean printOutput = true;  

    @SuppressWarnings("unused")
	private static Socket socket;
    private static Socket[] sockets;
    private static InputStream[] inputs;
    private static OutputStream[] outputs;

    @SuppressWarnings({ "unused", "static-access" })
	public void establishConnection( int size, int rank,
				     Vector<String> hosts, int port ) {
	inputs = new InputStream[size];
	outputs = new OutputStream[size];
	try {
	    // prepare a server socket
	    @SuppressWarnings("resource")
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
			inputs[j] =  sockets[j].getInputStream( );
			outputs[j] = sockets[j].getOutputStream( );
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
		    outputs[i] = sockets[i].getOutputStream( );
		    inputs[i] = sockets[i].getInputStream( );
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

    @SuppressWarnings("unused")
	public void sendMessage( int rank, Message exchangeReq ) {

	if ( printOutput == true )
	    MASS_base.log( "exchange.sendMessage will be sent to rank: " +
			   rank + ", exchangeReq.exchangeReqList = " +
			   exchangeReq.getExchangeReqList()  + 
			   ", exchangeReq.migrationReqList = " +
			   exchangeReq.getMigrationReqList() );

	try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream( );
            ObjectOutputStream oos 
		= new ObjectOutputStream( baos );
            oos.writeObject( exchangeReq );
            oos.close( );
            baos.close( );
            byte[] bArray = baos.toByteArray( );
            byte[] length = new byte[4];
            length[0] = (byte) (bArray.length >> 24);
            length[1] = (byte) (bArray.length >> 16);
            length[2] = (byte) (bArray.length >> 8);
            length[3] = (byte) bArray.length;
            outputs[rank].write( length );
            outputs[rank].write( bArray );
	} catch ( Exception e ) {
	    MASS_base.log ( "exchange.sendMessage to rank: " + rank + 
			    ". Error: " + e + ", outputs[rank] = " + 
			    outputs[rank] + 
			    ", exchangeReq" + exchangeReq );
	}

	if ( printOutput == true )
	    MASS_base.log( "exchange.sendMessage has been sent to rank: " +
			   rank );
    }
    
    @SuppressWarnings("unused")
	public Message receiveMessage( int rank ) {

	if ( printOutput == true )
	    MASS_base.log( "exchange.receiveMessage will receive from rank: " 
			   + rank );

	Message m = null;
	try {
            byte[] length = new byte[4];
            inputs[rank].read(length);
            int intLength = 0;
            for (int i = 0; i < 4; i++) {
                int shift = (3 - i) * 8;
                intLength += (length[i] & 0xff) << shift;
            }
            byte[] bArray = new byte[intLength];
            for (int nRead = 0; nRead < intLength;
                 nRead += inputs[rank].read(bArray, nRead, intLength - nRead));

            ByteArrayInputStream bais  = new ByteArrayInputStream(bArray);
            ObjectInputStream ois = new ObjectInputStream( bais );
            m = (Message) ois.readObject();
            bais.close();
            ois.close();
	} catch ( Exception e ) {
	    MASS_base.log ( "exchange.receiveMessage from rank: " + rank + 
			    ". Error: " + e + ", inputs[rank] = " + 
			    inputs[rank] );
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

    @SuppressWarnings("unused")
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

}