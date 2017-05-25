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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;

public class ExchangeHelper {

    private static Socket[] sockets;
    private static InputStream[] inputs;
    private static OutputStream[] outputs;

	// logging
	private Log4J2Logger logger = Log4J2Logger.getInstance();

    @SuppressWarnings("static-access")
    public void establishConnection( int size, int rank, Vector<String> hosts, int port ) {
    	
    	inputs = new InputStream[size];
    	outputs = new OutputStream[size];
    	
    	try {
    		
    		// prepare a server socket
    		@SuppressWarnings("resource")
    		ServerSocket server = new ServerSocket( port );
			logger.debug( "bound to port " + port );

    		// create sockets[]
    		sockets = new Socket[size];

    		// accept connections from higher ranks
    		for ( int i = rank + 1; i < size; i++ ) {
    			
   				logger.debug( "rank[" + rank + "] will accept " + i + "-th connection" );

    			// accept a new connection
    			Socket socket = server.accept( );
    			socket.setReuseAddress( true );

    			// retrieve the client socket ipaddress and port
    			InetAddress addr = socket.getInetAddress( );
    			String ipaddr = addr.getCanonicalHostName( );

   				logger.debug( "connection from {}", ipaddr );

    			// identify the rank of this connection from ipaddr
    			for ( int j = rank + 1; j < size; j++ ) {
    				
   					logger.debug( "compare with {}", hosts.get(j) );

    				if ( hosts.get(j).equals( ipaddr ) ) {
    					
    					// matched and assigned this socket to rank j.
    					sockets[j] = socket; 
    					inputs[j] =  sockets[j].getInputStream( );
    					outputs[j] = sockets[j].getOutputStream( );
    					
   						logger.debug( "rank" + rank + "] accepted from rank[" + j + "]:" + hosts.get(j) );
    					
    					break;
    				
    				}
    			
    			}
    		
    		}
    	
    	}
    	
    	catch ( Exception e ) {
    		logger.error( "exchange.establishConnection: server " + e );
    		System.exit( -1 );
    	}

    	// sends connection requests to lower ranks
    	for ( int i = 0; i < rank; i++ ) {
    		for ( int j = 0; j < 5; j++ ) {
    			
    			try {
    				
    				logger.debug( "exchange.establishConnection: attempting to connect to " +
    						hosts.get(i) + ":" + port + "...");
    						
    				sockets[i] = new Socket( hosts.get(i), port );
    				sockets[i].setReuseAddress( true );
    				outputs[i] = sockets[i].getOutputStream( );
    				inputs[i] = sockets[i].getInputStream( );
    				break;
    			
    			} 
    			
    			catch ( Exception e1 ) {
    				
    				logger.debug( "rank" + rank + "] " + j + 
    						"-th try to connect to" +
    						"rank [" + i + "]: " + hosts.get(i) );
    				try {
    					Thread.currentThread( ).sleep( 1000 );
    				} 
    				
    				catch ( Exception e2 ) { 
    					logger.error("Exception while attempting to connect in ExchangeHelper", e2);}
    			
    			}
    		
    		}
    		
    		if ( sockets[i] == null ) {
    			logger.debug( "exchange.establishConnection: client failed" );
    			System.exit( -1 );
    		}
    		
   			logger.debug( "rank[" + rank + "] has connected to rank[" + i + "]: " +	hosts.get(i) );
    		
    	}
    
    }

    public Message receiveMessage( int rank ) {

   		logger.debug( "exchange.receiveMessage will receive from rank: {}", rank );

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
    	
    	} 
    	
    	catch ( Exception e ) {
    		
    		logger.debug( "exchange.receiveMessage from rank: " + rank + 
    				". Error: " + e + ", inputs[rank] = " + 
    				inputs[rank] );
    	
    	}

    	if ( m != null ) {
    		
   			logger.debug( "exchange.receiveMessage received from rank: {}", rank );
    		
    		return m;
    	
    	} else {
    		
   			logger.debug( "exchange.receiveMessage error from rank[{}]", rank );
    		
    		System.exit( -1 );
    	
    	}

    	return null;
    
    }

    public void sendMessage( int rank, Message exchangeReq ) {

   		logger.debug( "exchange.sendMessage will be sent to rank: " +
    				rank + ", exchangeReq.exchangeReqList = " +
    				exchangeReq.getExchangeReqList()  + 
    				", exchangeReq.migrationReqList = " +
    				exchangeReq.getMigrationReqList() );

    	try {
    		
    		ByteArrayOutputStream baos = new ByteArrayOutputStream( );
    		ObjectOutputStream oos = new ObjectOutputStream( baos );
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
    	
    	} 
    	
    	catch ( Exception e ) {
    		
    		logger.debug ( "exchange.sendMessage to rank: " + rank + 
    				". Error: " + e + ", outputs[rank] = " + 
    				outputs[rank] + 
    				", exchangeReq" + exchangeReq );
    	
    	}

   		logger.debug( "exchange.sendMessage has been sent to rank: {}",	rank );
    
    }

    public void terminateConnection( int rank ) {

    	// disconnect to lower ranks
    	for ( int i = 0; i < rank; i++ ) {
    		
    		try {
    			sockets[i].close( );
    		} 
    		
    		catch ( Exception e ) {
    			logger.error("Exception thrown while terminating connection in ExchangeHelper", e); 
    		}
    		
   			logger.debug( "rank[" + rank + "] has disconnected to rank[" + i + "]: " );
    		
    	
    	}
    
    }

}