// ExchangeHelper.java
package MASS;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.util.Set;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * 
 * @author M.Fukuda 3/26/2014 (previously: Tim Chuang)
 */
public class ExchangeHelper {
    private HashMap<String, StreamHandler> connectionMap = null;
    private final int INTERVAL = 1000;   // 1 second
    private final int RETRY_LIMIT = 10;

    /**
     * Is called internally to find the cannonical host name
     * @param host the name of a given computing node to find
     * @return the canonical host name of this node
     */
    private String getCanonicalName( String host ) {
	try {
	    InetAddress addr = InetAddress.getByName( host );
	    return addr.getCanonicalHostName( );
	} catch( Exception e ) {
	    MASS.log( "ExchangeHelper.getCanonicalName can't find: " + host );
	    System.exit( -1 );
	}
	return null;
    }
    
    /**
     * Is called from the Places constructor at the master or from 
     * MProcess.start( ) at each slave when creating the very first Places
     */
    public void establishConnection( ) {
	if ( connectionMap != null )
	    // connections have already established.
	    return;
	
	connectionMap = new HashMap<String, StreamHandler>( );
	
	// will use: MASS.myPid, MASS.nodePidMap, MASS_PORT
	try {
	    ServerSocket server = new ServerSocket( MASS.MASS_PORT );
	    MASS.log( "ExchangeHelper: created ServerSocket" );
	    
	    // accept connection requests from higher ranks.
	    for ( int i = MASS.myPid + 1; i < MASS.nodePidMap.size( ); i++ ) {
		
		// accept a new TCP connection
		Socket client = null;
		MASS.log( "ExchangeHelper: waiting for " + i + "-th request" );
		client = server.accept( );
		client.setSoLinger( false, 0 ); 
		if ( client == null ) {
		    MASS.log( "ExchangeHelper.establishConnection: "  +
			      " failed to accept a new connection" );
		    continue;
		}
		
		// Set stream
		StreamHandler sh = new StreamHandler( client );
		synchronized( connectionMap ) {
		    // MASS.log( "connectionMap.put (passive)" + sh.getHostName( ) );
		    connectionMap.put( sh.getHostName( ), sh );
		}
	    }

	    // don't keep the ServerSocket open.
	    server.close( );
	    
	} catch ( Exception e ) {
	    MASS.log( "ExchangeHelper.establishConnection (passive): " 
		      + e.getMessage( ) );
	    System.exit( -1 );
	}
	
	// sends TCP connection requests to lower ranks
	Set nodeEntrySet = MASS.nodePidMap.entrySet( );
	Iterator iter = nodeEntrySet.iterator( );
	while ( iter.hasNext( ) ) {
	    // retrieve each remote host name and rank
	    Entry nodeEntry = ( Entry )iter.next( );
	    String remoteName = ( String )nodeEntry.getKey( );
	    int remoteRank = ( ( Integer )nodeEntry.getValue( ) ).intValue( );
	    
	    if ( remoteRank < MASS.myPid ) {
		// send a connection request to this remote
		Socket client = null;
		for ( int retry = 0; retry < RETRY_LIMIT; retry++ ) {
		    try {
			client = new Socket( remoteName, MASS.MASS_PORT );
			client.setSoLinger( false, 0 );
		    } catch ( Exception e ) {
			MASS.log( "ExchangeHelper.establishConnection (active)"
				  + "retry = " + retry + ": "
				  + e.getMessage( ) );
		    }
		    if ( client != null ) 
			break;
		    try {
			Thread.currentThread( ).sleep( INTERVAL );
		    } catch ( Exception e ) {
			MASS.log( "ExchangeHelper.establishConnection (sleep):"
				  + e.getMessage( ) );
		    }
		}
		if ( client == null ) {
		    MASS.log( "ExchangeHelper.establishConnection (active):"
			      + " finished in failure" );
		    System.exit( -1 );
		}
		
		// connection established and register it into conenctionMap
		// Set stream
		StreamHandler sh = new StreamHandler( client, remoteName );
		synchronized( connectionMap ) {
		    // MASS.log( "connectionMap.put( active ) " + sh.getHostName( ) );
		    connectionMap.put( sh.getHostName( ), sh );
		}
	    }
	}
    }

    /**
     * Closes all StreamHandlers' ObjectInput/OutputStreams.
     */
    public void finish( ) {
        synchronized( connectionMap ) {
            // close streams
            Set entrySet = connectionMap.entrySet( );
            Entry entry;
            for ( Iterator it = entrySet.iterator( ); it.hasNext( ); ) {
                entry = ( Entry )it.next( );
                connectionMap.get( entry.getKey( ).toString( ) ).
		    closeConnections( );
            }
        } 
    }

    /**
     * Sends a request of exchangeAll to a given remote host.
     * @param hostName the destination host name
     * @param exgReq   a message of exchangeAll request
     */
    public void sendRequest( String hostName, Message exgReq ) {
        StreamHandler sh;
	synchronized( connectionMap ) {
	    hostName = getCanonicalName( hostName );
	    sh = connectionMap.get( hostName );
	    if ( sh == null ) {
		MASS.log( "ExchangeHelper.sendRequest: " +
			  "connectionMap.get( " + hostName + " ) = null" );
		System.exit( -1 );
	    }
	}
        //MASS.log("ExchangeHelper.sendReuqest:sh.sendExchangeRequest start: " 
		 //+ hostName );
        sh.sendExchangeRequest(exgReq);

        //MASS.log("ExchangeHelper.sendRequest: sh.sendExchangeRequest done" );
   } 

    /**
     * Receives, processes, and sends back return values of an exchange
     * request from a given remote host.
     * @param the remote host name that sent an exchange request.
     */
    public void processRequest( String hostName ) 
    {       
        StreamHandler sh;
	synchronized( connectionMap ) {
	    hostName = getCanonicalName( hostName );
	    sh = connectionMap.get( hostName );
	    if ( sh == null ) {
		MASS.log( "ExchangeHelper.processRequest: " +
			  "connectionMap.get( " + hostName + " ) = null" );
		System.exit( -1 );
	    }
	}

	MASS.log(Thread.currentThread( ) +
		 "ExchangeHelper.processRequest:readExchangeRequest begin for:"
		 + hostName );
        ArrayList<RemoteExchangeRequest> exgReq = sh.readExchangeRequest( );

        // process the request
        // retrieve the local value and send it to the requesting node
        ArrayList<RemoteExchangeRequest> reqVals 
	    = MASS.doRemoteExchangeAll( exgReq );
        Message exchangeMsg = new Message( );
        exchangeMsg.createExchangeAllRequestMessage( reqVals ); 

	ParallelWriter writer = new ParallelWriter( sh, exchangeMsg );
	writer.start( );

        ArrayList<RemoteExchangeRequest> retVals = sh.readExchangeRequest();
	
	try {
	    writer.join( );
	} catch ( InterruptedException e ) { }

        MASS.updateInMessages(retVals);
        // MASS.log("ExchangeHelper.processRequest: done size="+exgReq.size());
    } 

    private class ParallelWriter extends Thread {
	private StreamHandler sh;
	Message msg;
	public ParallelWriter( StreamHandler sh, Message msg ) {
	    this.sh = sh;
	    this.msg = msg;
	}
	public void run( ) {
	    sh.sendExchangeRequest( msg );
	}
    }
 
    /**
     * Sends a request of agent migration to a given remote host.
     * @param hostNmae the destination host name to send these agents
     */
    public void processAgentMigrateRequest( String hostName ) {
        StreamHandler sh;
	synchronized( connectionMap ) {
	    hostName = getCanonicalName( hostName );
	    sh = connectionMap.get( hostName );
	    if ( sh == null ) {
		MASS.log( "ExchangeHelper.processAgentMigrationRequest: " +
			  "connectionMap.get( " + hostName + " ) = null" );
		System.exit( -1 );
	    }
	}

        //MASS.log("ExchangeHelper.processAgentMigrationRequest: " + hostName );
        ArrayList<RemoteAgentRequest> req = sh.readAgentMigrateRequest( );

        // retrieve the local value and send it to the requesting node
        MASS.doRemoteAgentMigrate( req );
	//MASS.log( "ExchangeHelper.processAgentMigrationRequest done: " +
		  //"size = " + ( ( req == null ) ? 0 : req.size( ) ) + " for " +
		  //hostName );
    }    

    /**
     * This class includes a pair of ObjectInputStream and ObjectOutputStream 
     * for each remote node. These streams are used to send and receive
     * an exchange-request message.
     *
     */
    private class StreamHandler {
	private String hostName;
	private ObjectInputStream input;
	private ObjectOutputStream output;
	
	public String getHostName( ) {
	    return hostName;
	}

	/**
	 * The constructor records a pair of ObjectInput/OutputStreams.
	 * upon a passive connection
	 * @param remoteSocket a socket established to remoteHost
	 */
	public StreamHandler( Socket remoteSocket ) {
	    init( remoteSocket, null );
	}
	
	/**
	 * The constructor records a pair of ObjectInput/OutputStreams.
	 * upon an active connection
	 * @param remoteSocket a socket established to remoteHost
	 * @param host a remote server host name
	 */
	public StreamHandler( Socket remoteSocket, String remoteName ) {
	    init( remoteSocket, remoteName );
	}

	/**
	 * Is the actual body of constructors.
	 * @param remoteSocket a socket established to remoteHost
	 * @param host a remote server host name
	 */
	private void init( Socket remoteSocket, String remoteName ) {
	    try {
		if ( remoteName == null ) {
		    // a passive connection: create ObjetInputStream first
		    input = new 
			ObjectInputStream( remoteSocket.getInputStream( ) );
		    output = new 
			ObjectOutputStream( remoteSocket.getOutputStream( ) );
		}
		else {
		    // an active connection: create ObjectOutputStream first
		    output = new 
			ObjectOutputStream( remoteSocket.getOutputStream( ) );
		    input = new 
			ObjectInputStream( remoteSocket.getInputStream( ) );
		}
	    } catch ( Exception e ) {
		MASS.log( "ExchangeHeloper.StreamHandler.constructor: " 
			  + e.getMessage( ) );
		System.exit( -1 );
	    }

	    if ( remoteName == null ) // a passive connection
		try {
		    // receive the client name
		    remoteName = ( String )input.readObject( );
		    this.hostName 
			= InetAddress.getByName( remoteName ).getCanonicalHostName( );
		} catch( Exception e ) {
		   // MASS.log( "ExchangeHelper.StreamHandler.ndHostName to " 
			 //     + remoteName + ": " + e.getMessage( ) );
		    System.exit( -1 );
		}
		       
	    else {                   // an active connection
		try {
		    // send my name and register the server name (remoteName)
		    String localName
			= InetAddress.getLocalHost( ).getHostName( );
		    MASS.log( "ExchangeHelper.StreamHandler send local host : "
			      + localName + " to " + remoteName );
		    output.writeObject( localName );
		    output.flush( );
		    // a remote name is recarded
		    this.hostName 
			= InetAddress.getByName( remoteName ).getCanonicalHostName( );
		} catch( Exception e ) {
		    MASS.log( "ExchangeHelper.StreamHandler send localhost to "
			      + remoteName + ": " + e.getMessage( ) );
		    System.exit( -1 );
		}
	    }
	}

	/**
	 * Sends an entire package of exchangeAll requests to a remote node.
	 * @param exchangePackage an entire package of exchangeAll requests
	 *                        to this remote node.
	 */
	public void sendExchangeRequest( Message exchangePackage ) {
	    try {
		//MASS.log( Thread.currentThread( ) +
			  //"ExchangeHelper.StreamHandler.sendExchangeReques to "
			  //+ hostName + ": " + exchangePackage );
		output.writeObject( exchangePackage );
		output.flush( );
	    } catch ( Exception e ) {
		//MASS.log( Thread.currentThread( ) +
			  //"ExchangeHelper.StreamHandler.sendExchangRequest to "
			  //+ hostName + ": "
			  //+ e.getMessage( ) );
		System.exit( -1 );
	    }
	}

	/**
	 * Receives an entire package of exchangeAll requests from a remote 
	 * node.
	 * @return an exchangeAll request message from a given remote node
	 */
	public ArrayList<RemoteExchangeRequest> readExchangeRequest( ) {
            Message ret = null;
            try	{
		//MASS.log( Thread.currentThread( ) +
			 //"ExchangeHelper.StreamHandler.readExchangeReques from"
			  //+ " " + hostName );
		ret = ( Message )input.readObject( );
	    }  catch( Exception e ) {
		//MASS.log( Thread.currentThread( ) +
			  //"ExchangeHelper.StreamHandler.readExchangeRequest"
			  //+ " from " + hostName + ": "
			  //+ e.getMessage( ) );
		System.exit( -1 );
	    }
	    return ( ret != null ) ? ret.getExchangeAllMessage( ) : null;
        }

	/**
	 * Receives an entire package of agent migration request from a remote
	 * node.
	 * @return an agent migration request message from a given remote node
	 */
        public ArrayList<RemoteAgentRequest> readAgentMigrateRequest( ) {
            Message ret = null;
            try {
		ret = ( Message )input.readObject( );
	    } catch( Exception e ) {
		//MASS.log( "ExchangeHelper.StreamHandler." +
			  //"readAgentMigrationRequest from " + hostName + ": "
			  //+ e.getMessage( ) );
		System.exit( -1 );
	    }
	    return ( ret != null ) ? 
		ret.getRemoteAgentMigrateRequest( ) : null;
        }

	/**
	 * Closes the pair of ObjectInput/OutputStreams that have been 
	 * maintained inside.
	 */
	public void closeConnections( ) {
            try {
		input.close( );
		output.close( );
	    } catch( Exception e ) { }
        }
    }
}