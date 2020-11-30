/*

 	MASS Java Software License
	© 2012-2020 University of Washington

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


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import edu.uw.bothell.css.dsl.MASS.annotations.OnCreation;
import edu.uw.bothell.css.dsl.MASS.event.EventDispatcher;
import edu.uw.bothell.css.dsl.MASS.event.SimpleEventDispatcher;
import edu.uw.bothell.css.dsl.MASS.factory.ObjectFactory;
import edu.uw.bothell.css.dsl.MASS.factory.SimpleObjectFactory;
import edu.uw.bothell.css.dsl.MASS.graph.HIPPIETABFormatLineParts;
import edu.uw.bothell.css.dsl.MASS.matrix.MatrixUtilities;


public class PlacesBase {

	// the total number of Places, determined by multiplying the values in the "size" array
    private int[] nextIndex;
    private final int handle;
    private final String className;
    private int lowerBoundary;
    private int upperBoundary;
    private int placesSize;
    private int[] size;
    private int shadowSize;
    private int boundaryWidth;
    private Place[] places;
    private Place[] leftShadow;
    private Place[] rightShadow;
    private ObjectFactory objectFactory = SimpleObjectFactory.getInstance();
    private EventDispatcher eventDispatcher = SimpleEventDispatcher.getInstance();

	/**
	 * Instantiate a PlacesBase for this node
	 * @param handle The Handle ID identifying this PlacesBase
	 * @param className The class that represents a Place
	 * @param boundary_width The width of the boundary between nodes, used to calculate shadow space
	 * @param argument The argument to supply to the Place during initialization
	 * @param size Matrix dimensions, as an array of integers representing dimension sizes
	 */
	public PlacesBase( int handle, String className, int boundary_width, Object argument, int[] size ) {
		
		this.handle = handle;
		this.className = className;
		this.boundaryWidth = boundary_width;
		this.size = size;

		MASSBase.getLogger().debug( "Places_base handle = " + handle
					+ ", class = " + className
					+ ", argument = " + argument
					+ ", boundary_width = " + boundary_width 
					+ ", size.length = " + size.length );
	
		init_all( argument );
	
	}

//	private static String getSimplifiedClassname(String className) {
//		// set simple name used for logging and monitoring
//		String simpleClassName = className.contains(".")
//				? className.substring(className.lastIndexOf('.') + 1)
//				: className;
//		simpleClassName = simpleClassName.contains("$")
//				? simpleClassName.substring(simpleClassName.lastIndexOf('$') + 1)
//				: simpleClassName;
//		return simpleClassName;
//	}

	/**
	 * Instantiate a generic PlacesBase for this node
	 * @param handle The Handle ID identifying this PlacesBase
	 * @param className The class that represents a Place
	 * @param argument The argument to supply to the Place during initialization
	 */
	public PlacesBase( int handle, String className, Object argument ) {

		this.handle = handle;
		this.className = className;

		MASSBase.getLogger().debug( "Places_base handle = " + handle
				+ ", class = " + className
				+ ", argument = " + argument );

		init_all( argument );

	}

	public PlacesBase(int handle, String classname, String[] graphArgs, Object[] initArgs) {
		this.handle = handle;
		this.className = classname;

		init_all_graph(graphArgs, initArgs);
	}

	protected PlacesBase(int handle, String className) {
		this.handle = handle;
		this.className = className;
	}
	private class ExchangeBoundary_helper extends Thread {

    	int direction;
    	int places_size;
    	int shadow_size;

    	public ExchangeBoundary_helper( int direction, int placesSize, int shadowSize ) {
    	
    		// identify the boundary space;
    		this.direction = direction;
    		this.places_size = placesSize;
    		this.shadow_size = shadowSize;

    		MASSBase.getLogger().debug( "Places_base.ExchangeBoundary_helper direction = " + direction
    					+ ", places_size = " + places_size 
    					+ ", shadow_size = " + shadow_size
    					);
    	
    	}

    	public void run( ) {

    		int startIndex = ( direction == 'L' ) ? 0 : places_size -shadow_size;
    		Object[] outBuffer = new Object[ shadow_size ];
		Object[][] inBuffer  = new Object[ shadow_size ][];

    		// copy all the outMessages into the outBuffer
    		for ( int i = 0; i < shadow_size; i++ ) {
    			outBuffer[i] = places[startIndex + i].getOutMessage();
			inBuffer[i]  = ( direction == 'L' ) ? leftShadow[i].getInMessages() : rightShadow[i].getInMessages();;
		}

    		if ( MASSBase.getLogger().isDebugEnabled() ) {
    			
    			MASSBase.getLogger().debug( "Places_base.exchangeBoundary_helper direction = {}", direction );

    			for ( int i = 0; i < shadow_size; i++ ) {
    				MASSBase.getLogger().debug( "outBuffer[" + i + "] = " + outBuffer[i] );
    				MASSBase.getLogger().debug( "inBuffer[" + i + "] = " + inBuffer[i] );				
			}
    		}

    		// create a PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST message
    		Message outMessageToDest = new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST, outBuffer );
		Message inMessageToDest = new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST, inBuffer );

    		// compose a PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST message
    		int destRank = ( direction == 'L' ) ? MASSBase.getMyPid() - 1 : MASSBase.getMyPid() + 1;

    		MASSBase.getLogger().debug( "Places_base.exchangeBoundary_helper direction = " + direction + ", rankNmessage.rank = " + destRank );

    		// send it to my neighbor with a child
		Thread sendThr = null;
    		( sendThr = new Thread( () -> {
			MASSBase.getExchange().sendMessage( destRank, outMessageToDest );
			MASSBase.getExchange().sendMessage( destRank, inMessageToDest );
		    } ) ).start();

    		// receive a PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST message from my neighbor
    		Message outMessageFromDest = MASSBase.getExchange().receiveMessage( destRank );
		Message inMessageFromDest = MASSBase.getExchange().receiveMessage( destRank );

    		MASSBase.getLogger().debug( "Places_base.exchangeBoundary_helper direction = " + direction
					    + ", outMessageFromDest = " + outMessageFromDest
					    + ", inMessageFromDest = " + inMessageFromDest );

		// synch with sendTrh
		try {
		    sendThr.join( );
		} catch ( Exception e ) { }

    		// extract the outMessage received and copy it to the corresponding shadow
    		Place[] shadow = ( direction == 'L' ) ? leftShadow : rightShadow;
    		outBuffer = ( Object[] )( outMessageFromDest.getArgument( ) );

    		// copy the outBuffer contents into the corresponding shadow
    		for ( int i = 0; i < shadow_size; i++ ) {

    			shadow[i].setOutMessage(outBuffer[i]);

    			if ( MASSBase.getLogger().isDebugEnabled() ) 
    				MASSBase.getLogger().debug( "Places_base.exchangeBoundary_helper direction = " + direction +
    						", shadow[" + i + "].outMessage = " +
    						shadow[i].getOutMessage() +
    						", outBuffer = " + outBuffer[i] );

    		}

		// extract the inMessage received and copy it to the boundary places
    		inBuffer = ( Object[][] )( inMessageFromDest.getArgument( ) );		
    		// copy the inBuffer into all the inMessages 
    		for ( int i = 0; i < shadow_size; i++ ) {
		    MASSBase.getLogger().debug( "place[" + (startIndex + i ) + "].inMessages = " + inBuffer[i] );
		    places[startIndex + i].setInMessages( inBuffer[i] );
		}
    	}
    
    }

    private class ProcessRemoteExchangeRequest extends Thread {

    	private int destRank;
    	private int srcHandle;
    	private int destHandle_at_src;
    	private int functionId;
    	private int my_lower_boundary;

    	public ProcessRemoteExchangeRequest( int[] param ) {
    		
    		destRank = param[0];
    		srcHandle = param[1];
    		destHandle_at_src = param[2];
    		functionId = param[3];
    		my_lower_boundary = param[4];
    	
    	}

    	public void run( ) {

   			MASSBase.getLogger().debug( "rank[{}]: starts processRemoteExchangeRequest", destRank );

    		// pick up the next rank to process
   			Vector<RemoteExchangeRequest> orgRequest = MASSBase.getRemoteRequests().get(destRank);

    		// for debugging
    		if ( MASSBase.getLogger().isDebugEnabled() ) {
    			
    			synchronized( orgRequest ) {
    			
    				MASSBase.getLogger().debug( "tid[" + destRank + 
    						"] sends an exhange request to rank: " +
    						destRank + " size() = " + 
    						orgRequest.size( ) );
    				
    				for ( int i = 0; i < orgRequest.size( ); i++ )
    					MASSBase.getLogger().debug( "send from " +
    							orgRequest.get(i).
    							getOrgGlobalLinearIndex() + " to " +
    							orgRequest.get(i).
    							getDestGlobalLinearIndex() + " at " +
    							orgRequest.get(i).getInMessageIndex() );
    			
    			}
    		
    		}
    	
    		// now compose and send a message by a child
			Message messageToDest = new Message(Message.ACTION_TYPE.PLACES_EXCHANGE_ALL_REMOTE_REQUEST, srcHandle,
					destHandle_at_src, functionId, orgRequest, 0); // 0 = dummy
			
			// for latter sync
			Thread sender = new Thread( () -> MASSBase.getExchange().sendMessage( destRank, messageToDest ) );
			sender.start();
  		
    		// receive a message by myself
    		Message messageFromSrc = MASSBase.getExchange().receiveMessage( destRank );

    		// process a message
    		Vector<RemoteExchangeRequest> receivedRequest = messageFromSrc.getExchangeReqList( );

    		int destHandle_at_dst = messageFromSrc.getDestHandle( );
    		PlacesBase dstPlaces = MASSBase.getPlacesMap().get( destHandle_at_dst );

    		MASSBase.getLogger().debug( "request from rank[" + destRank + "] = ", receivedRequest );
    		MASSBase.getLogger().debug( " size( ) = " + receivedRequest.size( ) );

    		// get prepared for a space to sotre return values
    		Object[] retVals = new Object[receivedRequest.size( )];

    		// for each place, call the corresponding callMethod( ).
    		for ( int i = 0; i < receivedRequest.size( ); i++ ) {

    			if ( MASSBase.getLogger().isDebugEnabled() )
    				MASSBase.getLogger().debug( "received from " +
    						receivedRequest.get(i).
    						getOrgGlobalLinearIndex() + " to " +
    						receivedRequest.get(i).
    						getDestGlobalLinearIndex() + " at " +
    						receivedRequest.get(i).getInMessageIndex() + 
    						" dstPlaces.lower = " + 
    						dstPlaces.lowerBoundary +
    						" dstPlaces.upper = " + 
    						dstPlaces.upperBoundary );

    			int globalLinearIndex = receivedRequest.get(i).getDestGlobalLinearIndex();
    			Object outMessage = receivedRequest.get(i).getOutMessage();

    			if ( globalLinearIndex >= dstPlaces.lowerBoundary && globalLinearIndex <= dstPlaces.upperBoundary ) {
    				
    				// local destination
    				int destinationLocalLinearIndex = globalLinearIndex - dstPlaces.lowerBoundary;

    				MASSBase.getLogger().debug( " dstLocal = ", destinationLocalLinearIndex );

    				Place dstPlace = dstPlaces.places[destinationLocalLinearIndex];

    				// call the destination function
    				retVals[i] = dstPlace.callMethod( functionId, outMessage );
    			
    			}
    		
    		}
			
			// create return message
			Message messageToSrc = new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT, retVals );
			
			// sync sender
			try {
				sender.join();
			} catch (InterruptedException e) {
				MASSBase.getLogger().error("Unable to join sender thread", e);
			}
    		// send return values by a child thread
    		new Thread( () -> MASSBase.getExchange().sendMessage( destRank, messageToSrc ) ).start();
    		
    		// receive return values by myself in parallel
    		Message messageFromDest = MASSBase.getExchange().receiveMessage( destRank );

    		// store return values to the orignal places
    		Object[] argument = ( Object[] ) messageFromDest.getArgument( );

    		for ( int i = 0; i < orgRequest.size( ); i++ ) {
    			
    			// local source
    			int orgLocalLinearIndex = 
    					orgRequest.get(i).getOrgGlobalLinearIndex() - my_lower_boundary;

    			// locate a local place
    			PlacesBase srcPlaces = 
    					MASSBase.getPlacesMap().get( srcHandle );
    			Place srcPlace = srcPlaces.places[orgLocalLinearIndex];

    			// store a return value to it
    			Object inMessage = argument[i];

    			// insert an item at inMessageIndex or just append it.
    			srcPlace.getInMessages()[orgRequest.get(i).getInMessageIndex()] = 
    					inMessage;

    			if ( MASSBase.getLogger().isDebugEnabled() )
    				MASSBase.getLogger().debug( "srcPlace[" + srcPlace.getIndex()[0]+ "][" 
    						+ srcPlace.getIndex()[1] + "] inserted " 
    						+ "at " 
    						+ orgRequest.get(i).getInMessageIndex() );
    		
    		}
			
			// clear requests
			orgRequest.clear();

    	}
    
    }
    
    /**
     * Execute a function (specified by ID) on each Place, with a single argument
     * @param functionId The function (method) to execute
     * @param argument Optional argument to be supplied to the method
     * @param tid The ID of the thread that should execute the method
     */
    public void callAll( int functionId, Object argument, int tid ) {
    	
    	int[] range = new int[2];
    	getLocalRange( range, tid );

    	// debugging
    	if ( MASSBase.getLogger().isDebugEnabled() )
    		MASSBase.getLogger().debug( "thread[" + tid + "] callAll functionId = " + 
    				functionId + ", range[0] = " + range[0] + 
    				" range[1] = " + range[1] );

    	if ( range[0] >= 0 && range[1] >= 0 ) {
    		
    		for ( int i = range[0]; i <= range[1]; i++ ) {
    			places[i].callMethod( functionId, argument );
    		}
    	
    	}

    	if (GraphPlaces.class.isAssignableFrom(this.getClass())) {
			((GraphPlaces)this).reallyCallAll(functionId, argument, tid);
		}
    }

    /**
     * Execute a function (specified by ID) on each Place, with multiple arguments
     * @param functionId The function (method) to execute
     * @param arguments Optional arguments to be supplied to the method
     * @param length The number of arguments supplied
     * @param tid The ID of the thread that should execute the method
     */
    public Object callAll( int functionId, Object[] arguments, int length, int tid ) {

    	int[] range = new int[2];
    	getLocalRange( range, tid );

    	// debugging
    	if ( MASSBase.getLogger().isDebugEnabled() )
    		MASSBase.getLogger().debug( "thread[" + tid + 
    				"] callAll_return object functionId = " + 
    				functionId + ", range[0] = " + range[0] + 
    				" range[1] = " + range[1] +
    				", arguments.length = " + length );

    	if ( range[0] >= 0 && range[1] >= 0 ) {

    		for ( int i = range[0]; i <= range[1]; i++ ) {
    			
    			if ( MASSBase.getLogger().isDebugEnabled() )
    				MASSBase.getLogger().debug( "thread[" + tid + "]: places[" + i + "] = " + 
    						places[i] );

    			// this fix is kind of a band aid too.
    			if ( arguments == null || (!(i < arguments.length)) )
    				MASSBase.getCurrentReturns()[i] = places[i].callMethod( functionId, null );
    			else
    				MASSBase.getCurrentReturns()[i] = places[i].callMethod( functionId, arguments[i] );
    		}
    	
    	}

		if (GraphPlaces.class.isAssignableFrom(this.getClass())) {
			((GraphPlaces)this).reallyCallAllWithReturns(functionId, MASSBase.getCurrentReturns(), arguments);
		}
    	
    	return null;
    
    }
    
    /**
     * Perform an ExchangeAll operation on Places
     * @param dstPlaces The Places on which to perform the Exchange All
     * @param functionId The function/method ID to execute
     * @param tid The ID of the thread that should execute the method
     */
    public void exchangeAll( PlacesBase dstPlaces, int functionId, int tid ) {

    	int[] range = new int[2];
    	getLocalRange( range, tid );

    	// debugging
    	if ( MASSBase.getLogger().isDebugEnabled() )
    		MASSBase.getLogger().debug( "thread[" + tid + "] exchangeAll functionId = " + 
    				functionId + ", range[0] = " + range[0] + 
    				" range[1] = " + range[1] );

    	// TODO: Need to find a way to replace destinations with same meaning code block

    	// now scan all places within range[0] ~ range[1]
    	if ( range[0] >= 0 && range[1] >= 0 ) {

    		for ( int i = range[0]; i <= range[1]; i++ ) {

    			// for each place
    			Place srcPlace = places[i];
    			// Java version's inMessages are an array rather than a vector.
    			srcPlace.setInMessages(new Object[srcPlace.getNeighbours().size( )]);

    			// check its neighbors
    			for ( int j = 0; j < srcPlace.getNeighbours().size( ); j++ ) {

    				// for each neighbor
    				int[] offset = srcPlace.getNeighbours().get(j);
    				int[] neighborCoord = new int[dstPlaces.size.length];

    				// compute its coordinate
    				getGlobalNeighborArrayIndex( srcPlace.getIndex(), offset, dstPlaces.size, neighborCoord );

    				if ( MASSBase.getLogger().isDebugEnabled() )
    					MASSBase.getLogger().debug( "tid[" + tid + "]: calls from"
    							+ "[" + srcPlace.getIndex()[0]
    									+ "][" + srcPlace.getIndex()[1] + "]"
    									+ " (neighborCord[" + neighborCoord[0]
    											+ "][" + neighborCoord[1] + "]"
    											+ " dstPlaces.size[" 
    											+ dstPlaces.size[0] 
    													+ "][" + dstPlaces.size[1] + "]" );

    				if ( neighborCoord[0] != -1 ) { 

    					// destination valid
    					int globalLinearIndex = MatrixUtilities.getLinearIndex(dstPlaces.size, neighborCoord);

    					if ( MASSBase.getLogger().isDebugEnabled() ) 
    						MASSBase.getLogger().debug( " linear = " + globalLinearIndex
    								+ " lower = " 
    								+ dstPlaces.lowerBoundary
    								+ " upper = " 
    								+ dstPlaces.upperBoundary + ")" );

    					if ( globalLinearIndex >= dstPlaces.lowerBoundary && globalLinearIndex <= dstPlaces.upperBoundary ) {

    						// local destination
    						int destinationLocalLinearIndex = globalLinearIndex - dstPlaces.lowerBoundary;
    						Place dstPlace = dstPlaces.places[destinationLocalLinearIndex];

    						if ( MASSBase.getLogger().isDebugEnabled() )
    							MASSBase.getLogger().debug( " to [" + dstPlace.getIndex()[0] +
    									"][" + dstPlace.getIndex()[1] + "]");

    						// call the destination function
    						Object inMessage = dstPlace.callMethod( functionId, srcPlace.getOutMessage() );

    						// store this inMessage: 
    						srcPlace.getInMessages()[j] = inMessage;

    						// for debug
    						MASSBase.getLogger().debug( " inMessage = {}",srcPlace.getInMessages()[j] );

    					} else {

    						// remote destination

    						// find the destination node
    						int destRank = getRankFromGlobalLinearIndex( globalLinearIndex );

    						// create a request
    						int orgGlobalLinearIndex = MatrixUtilities.getLinearIndex( size, srcPlace.getIndex() );
    						RemoteExchangeRequest request = new
    								RemoteExchangeRequest( globalLinearIndex,
    										orgGlobalLinearIndex,
    										j, // inMsgIndex
    										srcPlace.getOutMessage() );

    						// enqueue the request to this node.map
    						Vector<RemoteExchangeRequest> remoteRequests = 
    								MASSBase.getRemoteRequests().get( destRank );

    						synchronized( remoteRequests ) {
    							
    							remoteRequests.add( request );
    							
    							if ( MASSBase.getLogger().isDebugEnabled() )
    								MASSBase.getLogger().debug( "remoteRequest[" + 
    										destRank + "].add:" +
    										" org = " + 
    										orgGlobalLinearIndex +
    										" dst = " + 
    										globalLinearIndex +
    										" size( ) = " +
    										remoteRequests.size( ) );
    						
    						}
    					
    					}

    				}
    				
    			}
 
    		}
    	
    	}

    	// all threads must barrier synchronize here.
    	MThread.barrierThreads( tid );
    	
    	if ( tid == 0 ) {
    		
    		MASSBase.getLogger().debug( "tid[{}] now enters processRemoteExchangeRequest", tid );

    		// the main thread spawns as many communication threads as 
    		// the number of remote computing nodes and let each invoke 
    		// processRemoteExchangeReq.

    		// args to threads: 
    		// rank, srcHandle, dstHandle, functionId, lower_boundary
    		int[][] comThrArgs = new int[MASSBase.getSystemSize()][5];
    		ProcessRemoteExchangeRequest[] thread_ref = new ProcessRemoteExchangeRequest[MASSBase.getSystemSize()]; 
    		for ( int rank = 0; rank < MASSBase.getSystemSize(); rank++ ) {
    			
    			if ( rank == MASSBase.getMyPid() ) // don't communicate with myself
    				continue;

    			// set arguments 
    			comThrArgs[rank][0] = rank;
    			comThrArgs[rank][1] = handle;
    			comThrArgs[rank][2] = dstPlaces.handle;
    			comThrArgs[rank][3] = functionId;
    			comThrArgs[rank][4] = lowerBoundary;

    			// start a communication thread
    			thread_ref[rank] = new ProcessRemoteExchangeRequest( comThrArgs[rank] );
    			thread_ref[rank].start( );
    		
    		}

    		// wait for all the communication threads to be terminated
    		for ( int rank = 0; rank < MASSBase.getSystemSize(); rank++ ) {
    			
    			if ( rank == MASSBase.getMyPid() ) // don't communicate with myself
    				continue;      
    			try {
    				thread_ref[rank].join( );
    			}
    			
    			// TODO - should something be done here on exception?
    			catch ( Exception e ) {
    				MASSBase.getLogger().error("Exception thrown in PlacesBase while attempting to join rank {}", rank, e);
    			}
    		
    		}
    	
    	} else {
    		MASSBase.getLogger().debug( "tid[{}] skips processRemoteExchangeRequest", tid );
    	}
    
    }

    /**
     * Exchange boundaries between neighboring Places
     */
    public void exchangeBoundary( ) {
    	
    	if ( shadowSize == 0 ) { // no boundary, no exchange
    		MASSBase.getLogger().debug( "places (handle = {}) has NO boundary, and thus invokes NO exchange boundary", handle );
    		return;
    	}

    	ExchangeBoundary_helper thread_ref = null;

    	MASSBase.getLogger().debug( "exchangeBoundary starts" );

    	if ( MASSBase.getMyPid() < MASSBase.getSystemSize() - 1 ) {
    		
    		// create a child in charge of handling the right shadow.
    		MASSBase.getLogger().debug( "exchangeBoundary: pthreacd_create( helper, R ) places_size= {}", placesSize );

    		thread_ref = new ExchangeBoundary_helper( 'R', placesSize, shadowSize );
    		thread_ref.start( );
    	
    	}

    	if ( MASSBase.getMyPid() > 0 ) {
    		
    		// the main takes charge of handling the left shadow.
    		MASSBase.getLogger().debug( "exchangeBoundary: main thread( helper, L ) places_size = {}", placesSize );
    		( new ExchangeBoundary_helper( 'L', placesSize, shadowSize ) ).run( );
    	
    	}

    	if ( thread_ref != null ) {
    		
    		// we are done with exchangeBoundary
    		try {
    			thread_ref.join( ); 
    		} 
    		catch ( Exception e ) {
    			MASSBase.getLogger().debug( "exchangeBoundary: the main failed in joining with the child = {}", e );
    		}
    	
    	}
    
    }

    /**
     * Get the name of the class used for a Place
     * @return The Place implementation class name
     */
    protected String getClassName() {
		return className;
	}
    
    /**
     * Get the index array representing neighboring locations
     * @param src_index The source index
     * @param offset An offset into the source index
     * @param dst_size The destination index array (populated by this method)
     * @param dest_index The destination index
     */
    protected void getGlobalNeighborArrayIndex( int src_index[], int offset[], int dst_size[], int dest_index[] ) {
    	
    	for (int i = 0; i < dest_index.length; i++ ) {
    		
    		dest_index[i] = src_index[i] + offset[i]; // calculate dest index

    		if ( dest_index[i] < 0 || dest_index[i] >= dst_size[i] ) {
    			
    			// out of range
    			Arrays.fill( dest_index, -1 );
    			return;
    			
    		}
    	
    	}
    
    }
    
    /**
     * Get the handle (ID) for PlacesBase on this node
     * @return PlacesBase handle ID
     */
    public int getHandle( ) {
    	return handle;
    }

	/**
	 * Get the "leftmost" Places shared with the adjacent node (lower rank number)
	 * @return The leftmost shared Places
	 */
    protected Place[] getLeftShadow() {
		return leftShadow;
	}

    /** 
     * Returns the first and last of the range that should be allocated
     * to a given thread
     *
     * @param range An array of two integers: element 0 = the first and 
     *           element 1 = the last
     * @param tid An id of the thread that calls this function.
     */
    protected void getLocalRange( int[] range, int tid ) {

    	int nThreads = MASSBase.getThreads().length;
    	int portion = placesSize / nThreads; // per-thread allocated  range
    	int remainder = placesSize % nThreads;

		MASSBase.getLogger().debug(String.format("getLocalRange: { nThreads: %d , placesSize: %d }", nThreads, placesSize));

    	if ( portion == 0 ) {

    		// there are more threads than elements in the MASS.Places
    		if ( remainder > tid ) {
    			range[0] = tid;
    			range[1] = tid;
    		} else {
    			range[0] = -1;
    			range[1] = -1;
    		}

    	} else {

    		// there are more MASS.Places than threads
    		int first = tid * portion;
    		int last = ( tid + 1 ) * portion - 1;
    		if ( tid < remainder ) {
    			// add in remainders
    			first += tid;
    			last = last + tid + 1; // 1 is one of remainders.
    		}
    		else {
    			// remainders have been assigned to previous threads
    			first += remainder;
    			last += remainder;
    		}

    		range[0] = first;
    		range[1] = last;

    	}

    }

	/**
	 * Get the lower limit array element number of Places located on this node
	 * @return The lowest number Place element located on this node
	 */
	protected int getLowerBoundary() {
		return lowerBoundary;
	}

	/**
	 * Get the Places located on this node
	 * @return The actual Places on this node
	 */
	public Place[] getPlaces() {
		return places;
	}

	/**
	 * Get the number of Places located on this node
	 * @return The number of Places on this node
	 */
	public int getPlacesSize( ) {
    	return placesSize;
    }

	/**
	 * Given a Place index, referenced to a gloabl index of Places, return the "rank"
	 * or node number where the Place is actually located
	 * @param globalLinearIndex The array index for which to obtain the node or rank number
	 * @return The node/rank number associated with that Place
	 */
	protected int getRankFromGlobalLinearIndex( int globalLinearIndex ) {
		return MatrixUtilities.getRankFromGlobalLinearIndex( globalLinearIndex, size, MASSBase.getSystemSize() );
    }

	/**
	 * Get the "rightmost" Places shared with the adjacent node (higher rank number)
	 * @return The rightmost shared Places
	 */
	protected Place[] getRightShadow() {
		return rightShadow;
	}

	/**
	 * Get the size of the shadow space, "left" or "right" (same size)
	 * @return The size of the shadow space
	 */
	protected int getShadowSize() {
		return shadowSize;
	}

	/**
	 * Get the total size of the simulation space, as an array of matrix dimensions
	 * @return Simulation space size, as matrix dimensions
	 */
	public int[] getSize() {
		return size;
	}

	/**
	 * Get the upper limit array element number of Places located on this node
	 * @return The highest number Place element located on this node
	 */
	protected int getUpperBoundary() {
		return upperBoundary;
	}

    protected void init_all( Object argument ) {
    	MASSBase.getLogger().debug("PlacesBase - init_all");

    	// TODO - HACK! Agents and Places need to be able to "reach" this PlacesBase during instantiation
    	if ( MASS.getCurrentPlacesBase() == null ) MASS.setCurrentPlacesBase( this );
    	
    	// For debugging
    	MASSBase.getLogger().debug( "init_all handle = " + handle + 
    				", class = " + className + 
    				", argument = " + argument );

		// calculate "total", which is equal to the number of dimensions in "size" 
		int total = MatrixUtilities.getMatrixSize( size );

		// stripe size is total number of places divided by the number of nodes
		MASSBase.getLogger().debug( "Calculating stripe size, total number of Places is {}", total );
		MASSBase.getLogger().debug( "Calculating stripe size, system size (number of nodes) is {}", MASSBase.getSystemSize() );
		int stripeSize = total / MASSBase.getSystemSize();

    	// load the place constructor
    	try {


    		// lower_boundary is the first place managed by this node
    		lowerBoundary = stripeSize * MASSBase.getMyPid();
    		
    		// upperBoundary is the last place managed by this node
    		upperBoundary = (MASSBase.getMyPid() < MASSBase.getSystemSize() - 1) ?
    				lowerBoundary + stripeSize - 1 : total - 1;
    		
    		// placesSize is the total number of places managed by this node
    		placesSize = upperBoundary - lowerBoundary + 1;
    		
    		//  maintaining an entire set
    		places = new Place[placesSize];

			MASSBase.getLogger().debug(String.format("init_all: { lowerBoundary: %d, upperBoundary: %d, placesSize: %d }", lowerBoundary, upperBoundary, placesSize));

    		// initialize all Places objects
    		for ( int i = 0; i < placesSize; i++ ) {
    			
    			// TODO - hack! should be able to set index on a Place without having to resort to calling back for it (should be pushed, not pulled)
    			nextIndex = MatrixUtilities.getIndex( size, lowerBoundary + i);
    			
    			// instantiate and configure new place
				Place newPlace = objectFactory.getInstance(className, argument);
				newPlace.setIndex( nextIndex );		// this is better behavior, not optimal, though

				// Place has been created
    			eventDispatcher.queueAsync( OnCreation.class, newPlace );

				places[i] = newPlace;

    		}
    	} 
    	
    	// TODO - what to do when this exception is caught?
    	catch ( Exception e ) {
    		MASSBase.getLogger().error( "Places_base.init_all: {} not loaded and/or instantiated", className, e);
    	}

    	// Places have been instantiated
    	eventDispatcher.invokeQueuedAsync( OnCreation.class );

    	// allocate the left/right shadows

    	if ( boundaryWidth <= 0 ) {
    		// no shadow space.
    		shadowSize = 0;
    		leftShadow = null;
    		rightShadow = null;
    		return;
    	}

    	shadowSize = ( size.length == 1 ) ? boundaryWidth : total / size[0] * boundaryWidth;
    	
    	MASSBase.getLogger().debug( "Places_base.shadow_size = {}", shadowSize );

    	leftShadow = ( MASSBase.getMyPid() == 0 ) ? null : new Place[ shadowSize ];
    	rightShadow = ( MASSBase.getMyPid() == MASSBase.getSystemSize() - 1 ) ? null : new Place[ shadowSize ];

    	// initialize the left/right shadows
    	try {
    		
    		for ( int i = 0; i < shadowSize; i++ ) {

    			// left shadow initialization
    			// TODO - this check should be made before entering the loop to init leftShadow or rightShadow - no point looping to init shadows that dont exist
    			if ( leftShadow != null ) {

    				// instantiate a new place
    				// TODO - see "hack" comments above
    				nextIndex = MatrixUtilities.getIndex( size, lowerBoundary - shadowSize + i);
    				Place newPlace = objectFactory.getInstance(className, argument);
					newPlace.setIndex( nextIndex );
					leftShadow[i] = newPlace;

    			}

    			// right shadow initialization
    			if ( rightShadow != null ) {

    				// instantiate a new place
    				// TODO - see "hack" comments above
    				nextIndex = MatrixUtilities.getIndex( size, upperBoundary + i);
					Place newPlace = objectFactory.getInstance(className, argument);
					newPlace.setIndex( nextIndex );
					rightShadow[i] = newPlace;

    			}
    		
    		}
    	
    	}
    	
    	// TODO - what to do if this is caught?
    	catch ( Exception e ) {
    		MASSBase.getLogger().error("Unknown exception caught in PlacesBase while initializing left/right shadows", e);
    	}
    	
    }

	protected void init_all_graph(String[] graphArgs, Object[] initArgs) {
		String networkFilename = graphArgs[0];
		String extension = networkFilename.substring(networkFilename.lastIndexOf("."));

		// TODO: Implement the type paremeter to avoid dependency on file extension
		if (extension.equals(".csv")) {
			init_all_graph_csv(graphArgs, initArgs);
		} else if (extension.equals(".xml")) {
			init_all_graph_matsim(graphArgs, initArgs);
		} else if (extension.equals(".tsv")) {
			init_all_graph_hippie(graphArgs, initArgs);
		} else if (extension.equals(".sar")) {
			init_all_graph_sar(graphArgs, initArgs);
		} else {
			init_all_graph_csv(graphArgs, initArgs);
		}
	}

	protected void init_all_graph_blank(int size) {
		// using size 0 may be a quick hack to skip dimensional logic
		this.size = new int [] { size };

		// TODO - HACK! Agents and Places need to be able to "reach" this PlacesBase during instantiation
		if ( MASS.getCurrentPlacesBase() == null ) MASS.setCurrentPlacesBase( this );
	}

	/**
	 * Add a new place to this node at the end of the array
	 *
	 * @return the index of the new node -1 on error
	 */
//	protected int addPlace(int vertexId) {
//		final int currentSize = places.length;
//
//		try {
//			Place newPlace = objectFactory.getInstance(className, null);
//
//			newPlace.setIndex(new int[] { currentSize });
//
//			newPlaces[currentSize] = newPlace;
//
//			places = newPlaces;
//
//			int globalLinearIndex = MASSBase.getMyPid() * placesSize + currentSize;
//
//			MASSBase.distributed_map.put(vertexId, new VertexMetaValues(vertexId, globalLinearIndex));
//
//			// Increase this places size for calculating the linear index
//			this.size[0]++;
//
//			return currentSize;
//		} catch (Exception e) {
//			MASSBase.getLogger().error("Error instantiating for new place: " + e.getMessage(), e);
//		}
//
//		return -1;
//	}

	/**
	 * Add a new place to the specified host
	 *
	 * @param host to add to
	 */
//	protected int addPlace(String host, int vertexId) {
//		Message message = new Message(Message.ACTION_TYPE.MAINTENANCE_ADD_PLACE, vertexId);
//
//		Optional<MNode> hostOption = MASS.getAllNodes().stream().filter(node -> node.getHostName().equals(host)).findFirst();
//
//		if (hostOption.isPresent()) {
//			// TODO: Check if this is the host before looking
//			if (MASSBase.getMyHostname().equals(hostOption.get().getHostName())) {
//				addPlace(vertexId);
//			} else {
//				hostOption.get().sendMessage(message);
//			}
//
//		} else {
//			MASSBase.getLogger().error("Failed to send addPlace message to " + host + "; host not found");
//		}
//
//		return -1;
//	}

	private void init_all_graph_hippie(String[] graphArgs, Object[] initArgs) {
		String graphNeighborsFilename = graphArgs[0];

		MASSBase.getLogger().debug("PlacesBase - init_all_graph::hippie");

		// TODO - HACK! Agents and Places need to be able to "reach" this PlacesBase during instantiation
		if ( MASS.getCurrentPlacesBase() == null ) MASS.setCurrentPlacesBase( this );

		// For debugging
		MASSBase.getLogger().debug( "init_all_graph handle = " + handle +
				", class = " + className +
				", arguments = " + initArgs
				+ ", graphArgs = [" + Arrays.stream(graphArgs).collect(Collectors.joining(",")) + "] ");

		// load the place constructor
		try {
			// calculate vertex distribution
			int totalSize = populateMap(graphNeighborsFilename);

			this.size = new int[] { totalSize };

			int stripeSize = totalSize / MASSBase.getSystemSize();

			// lower_boundary is the first place managed by this node
			lowerBoundary = stripeSize * MASSBase.getMyPid();

			// upperBoundary is the last place managed by this node
			upperBoundary = (MASSBase.getMyPid() < MASSBase.getSystemSize() - 1) ?
					lowerBoundary + stripeSize - 1 : totalSize - 1;

			// placesSize is the total number of places managed by this node
			placesSize = upperBoundary - lowerBoundary + 1;

			MASSBase.getLogger().debug(String.format("init_all_graph: { totalSize: %d, lowerBoundary: %d, upperBoundary: %d, placesSize: %d }",
					totalSize, lowerBoundary, upperBoundary, placesSize));

			//  maintaining an entire set
			places = new Place[placesSize];

			Object [] finalGraphArgs = new Object[3];

			finalGraphArgs[0] = graphArgs[0];
			finalGraphArgs[1] = graphArgs[1];

			// initialize all Places objects
			for ( int i = 0; i < placesSize; i++ ) {
				int myIndex = MatrixUtilities.getIndex( size, lowerBoundary + i)[0];

				finalGraphArgs[2] = myIndex;

				// instantiate and configure new place
				Object [] ctorArguments;

				if (initArgs != null) {
					ctorArguments = Stream.concat(Arrays.stream(finalGraphArgs),
							Arrays.stream(initArgs)).toArray(Object[]::new);
				} else {
					ctorArguments = finalGraphArgs;
				}

				Place newPlace = objectFactory.getInstance(className, ctorArguments);

				newPlace.setIndex( new int[] { myIndex } );

				//newPlace.setIndex(getGlobalArrayIndex(lowerBoundary + i));
				//newPlace.setSize(size);
				places[i] = newPlace;
			}
		}

		// TODO - what to do when this exception is caught?
		catch ( Exception e ) {
			MASSBase.getLogger().error( "Places_base.init_all_graph: {} not loaded and/or instantiated", className, e);
		}
	}

	/**
	 * Import the vertices from a given file into the dmap
	 *
	 * @param graphNeighborsFilename
	 * @return the number of vertices imported
	 */
	private int populateMap(String graphNeighborsFilename) {
		int vertexCount = 0;

		Path filePath = Paths.get(MASSBase.getWorkingDirectory(), graphNeighborsFilename);

		try (BufferedReader br = new BufferedReader(new FileReader(filePath.toString()))) {
			String line = "";

			while ((line = br.readLine()) != null) {
				String [] parts = line.split("\t");

				String key = HIPPIETABFormatLineParts.getPart(parts, HIPPIETABFormatLineParts.PROTEIN_KEY);

				if (!MASSBase.distributed_map.containsKey(key)) {
					// HIPPIETABEdge edge = HIPPIETABEdge.fromParts(parts);

					MASSBase.distributed_map.put(key, vertexCount);

					vertexCount++;
				}
			}
		} catch (FileNotFoundException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);

			MASSBase.getLogger().error(sw.toString());
		} catch (IOException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);

			MASSBase.getLogger().error(sw.toString());
		}


 		return vertexCount;
	}

	// TODO: Size is input from message
	protected void init_all_graph_csv(String[] graphArgs, Object[] initArgs) {
		String graphNeighborsFilename = graphArgs[0];

		MASSBase.getLogger().debug("PlacesBase - init_all_graph");

		// TODO - HACK! Agents and Places need to be able to "reach" this PlacesBase during instantiation
		if ( MASS.getCurrentPlacesBase() == null ) MASS.setCurrentPlacesBase( this );

		// For debugging
		MASSBase.getLogger().debug( "init_all_graph handle = " + handle +
				", class = " + className +
				", arguments = " + initArgs
				+ ", graphArgs = [" + Arrays.stream(graphArgs).collect(Collectors.joining(",")) + "] ");

		// load the place constructor
		try {
			// calculate vertex distribution
			int totalSize = getVertexCount(graphNeighborsFilename);

			this.size = new int[] { totalSize };

			int stripeSize = totalSize / MASSBase.getSystemSize();

			// lower_boundary is the first place managed by this node
			lowerBoundary = stripeSize * MASSBase.getMyPid();

			// upperBoundary is the last place managed by this node
			upperBoundary = (MASSBase.getMyPid() < MASSBase.getSystemSize() - 1) ?
					lowerBoundary + stripeSize - 1 : totalSize - 1;

			// placesSize is the total number of places managed by this node
			placesSize = upperBoundary - lowerBoundary + 1;

			MASSBase.getLogger().debug(String.format("init_all_graph: { totalSize: %d, lowerBoundary: %d, upperBoundary: %d, placesSize: %d }",
					totalSize, lowerBoundary, upperBoundary, placesSize));

			//  maintaining an entire set
			places = new Place[placesSize];

			Object [] finalGraphArgs = new Object[3];

			finalGraphArgs[0] = graphArgs[0];
			finalGraphArgs[1] = graphArgs[1];

			// initialize all Places objects
			for ( int i = 0; i < placesSize; i++ ) {
				int myIndex = MatrixUtilities.getIndex( size, lowerBoundary + i)[0];

				finalGraphArgs[2] = myIndex;

				Object [] ctorArguments;

				if (initArgs != null) {
					ctorArguments = Stream.concat(Arrays.stream(finalGraphArgs),
							Arrays.stream(initArgs)).toArray(Object[]::new);
				} else {
					ctorArguments = finalGraphArgs;
				}

				// instantiate and configure new place
				Place newPlace = objectFactory.getInstance(className, ctorArguments);

				newPlace.setIndex( new int[] { myIndex } );

				//newPlace.setIndex(getGlobalArrayIndex(lowerBoundary + i));
				//newPlace.setSize(size);
				places[i] = newPlace;
			}
		}

		// TODO - what to do when this exception is caught?
		catch ( Exception e ) {
			MASSBase.getLogger().error( "Places_base.init_all_graph: {} not loaded and/or instantiated", className, e);
		}

	}

	private int getVertexCount(String graphNeighborsFilename) {
		int vertexCount = 0;

		Path filePath = Paths.get(MASSBase.getWorkingDirectory(), graphNeighborsFilename);

		try (BufferedReader br = new BufferedReader(new FileReader(filePath.toString()))) {
			while (br.readLine() != null) vertexCount++;
		} catch (FileNotFoundException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);

			MASSBase.getLogger().error(sw.toString());
		} catch (IOException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);

			MASSBase.getLogger().error(sw.toString());
		}

		return vertexCount;
	}

	protected void init_all_graph_sar(String[] graphArgs, Object[] initArgs) {
		String graphNeighborsFilename = graphArgs[0];

		MASSBase.getLogger().debug("PlacesBase - init_all_graph");

		// TODO - HACK! Agents and Places need to be able to "reach" this PlacesBase during instantiation
		if ( MASS.getCurrentPlacesBase() == null ) MASS.setCurrentPlacesBase( this );

		// For debugging
		MASSBase.getLogger().debug( "init_all_graph handle = " + handle +
				", class = " + className +
				", arguments = " + initArgs
				+ ", graphArgs = [" + Arrays.stream(graphArgs).collect(Collectors.joining(",")) + "] ");

		// load the place constructor
		try {
			String sarOnset = "";

			Path filePath = Paths.get(MASSBase.getWorkingDirectory(), graphNeighborsFilename);

			try (BufferedReader br = new BufferedReader(new FileReader(filePath.toString()))) {
				sarOnset = br.readLine();
			} catch (FileNotFoundException e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);

				MASSBase.getLogger().error(sw.toString());
			} catch (IOException e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);

				MASSBase.getLogger().error(sw.toString());
			}

			// maybe this should be a long
			int totalSize = Math.toIntExact(Arrays.stream(sarOnset.split(",")).count());

			this.size = new int[] { totalSize };

			int stripeSize = totalSize / MASSBase.getSystemSize();

			// lower_boundary is the first place managed by this node
			lowerBoundary = stripeSize * MASSBase.getMyPid();

			// upperBoundary is the last place managed by this node
			upperBoundary = (MASSBase.getMyPid() < MASSBase.getSystemSize() - 1) ?
					lowerBoundary + stripeSize - 1 : totalSize - 1;

			// placesSize is the total number of places managed by this node
			placesSize = upperBoundary - lowerBoundary + 1;

			MASSBase.getLogger().debug(String.format("init_all_graph: { totalSize: %d, lowerBoundary: %d, upperBoundary: %d, placesSize: %d }",
					totalSize, lowerBoundary, upperBoundary, placesSize));

			//  maintaining an entire set
			places = new Place[placesSize];

			Object [] finalGraphArgs = new Object[3];

			finalGraphArgs[0] = graphArgs[0];
			finalGraphArgs[1] = graphArgs[1];

			// initialize all Places objects
			for ( int i = 0; i < placesSize; i++ ) {
				int myIndex = MatrixUtilities.getIndex( size, lowerBoundary + i)[0];

				MASS.distributed_map.put(myIndex, myIndex);

				finalGraphArgs[2] = myIndex;

				if (initArgs == null) {
					initArgs = new Object[1];
				}

				// instantiate and configure new place
				Place newPlace = objectFactory.getInstance(className, Stream.concat(Arrays.stream(finalGraphArgs), Arrays.stream(initArgs)).toArray(Object[]::new));

				newPlace.setIndex( new int[] { myIndex } );

				//newPlace.setIndex(getGlobalArrayIndex(lowerBoundary + i));
				//newPlace.setSize(size);
				places[i] = newPlace;
			}
		}

		// TODO - what to do when this exception is caught?
		catch ( Exception e ) {
			MASSBase.getLogger().error( "Places_base.init_all_graph: {} not loaded and/or instantiated", className, e);
		}

	}

	// TODO: Size is input from message
	protected void init_all_graph_matsim(String[] graphArgs, Object[] initArgs) {
		String networkFilename = graphArgs[0];

		MASSBase.getLogger().debug("PlacesBase - init_all_graph_matsim");

		// TODO - HACK! Agents and Places need to be able to "reach" this PlacesBase during instantiation
		if ( MASS.getCurrentPlacesBase() == null ) MASS.setCurrentPlacesBase( this );

		// For debugging
		MASSBase.getLogger().debug( "init_all_graph handle = " + handle +
				", class = " + className +
				", arguments = " + initArgs
				+ ", graphArgs = [" + Arrays.stream(graphArgs).collect(Collectors.joining(",")) + "] ");

		// load the place constructor
		try {
			// calculate vertex distribution
			int totalSize = getMatsimNetworkNodeCount(networkFilename);

			this.size = new int[] { totalSize };

			int stripeSize = totalSize / MASSBase.getSystemSize();

			// lower_boundary is the first place managed by this node
			lowerBoundary = stripeSize * MASSBase.getMyPid();

			// upperBoundary is the last place managed by this node
			upperBoundary = (MASSBase.getMyPid() < MASSBase.getSystemSize() - 1) ?
					lowerBoundary + stripeSize - 1 : totalSize - 1;

			// placesSize is the total number of places managed by this node
			placesSize = upperBoundary - lowerBoundary + 1;

			MASSBase.getLogger().debug(String.format("init_all_graph: { totalSize: %d, lowerBoundary: %d, upperBoundary: %d, placesSize: %d }",
					totalSize, lowerBoundary, upperBoundary, placesSize));

			//  maintaining an entire set
			places = new Place[placesSize];

			Object [] finalGraphArgs = new Object[3];

			finalGraphArgs[0] = graphArgs[0];
			finalGraphArgs[1] = graphArgs[1];

			// initialize all Places objects
			for ( int i = 0; i < placesSize; i++ ) {
				int myIndex = MatrixUtilities.getIndex( size, lowerBoundary + i)[0];

				finalGraphArgs[2] = myIndex;

				// instantiate and configure new place
				Object [] ctorArguments;

				if (initArgs != null) {
					ctorArguments = Stream.concat(Arrays.stream(finalGraphArgs),
							Arrays.stream(initArgs)).toArray(Object[]::new);
				} else {
					ctorArguments = finalGraphArgs;
				}

				// instantiate and configure new place
				Place newPlace = objectFactory.getInstance(className, ctorArguments);

				newPlace.setIndex( new int[] { myIndex } );

				//newPlace.setIndex(getGlobalArrayIndex(lowerBoundary + i));
				//newPlace.setSize(size);
				places[i] = newPlace;
			}
		}

		// TODO - what to do when this exception is caught?
		catch ( Exception e ) {
			MASSBase.getLogger().error( "Places_base.init_all_graph: {} not loaded and/or instantiated", className, e);
		}

	}

	public static int getMatsimNetworkNodeCount(String networkFilename) {
		int result = 0;

		XPathFactory factory = XPathFactory.newInstance();

		XPath path = factory.newXPath();

		XPathExpression expression;

		try {
			expression = path.compile("//nodes/node/@id");

			NodeList nodeList = (NodeList) expression.evaluate(new InputSource(networkFilename),
					XPathConstants.NODESET);

			for (int n = 0; n < nodeList.getLength(); n++) {
				Node idNode = nodeList.item(n);
				
				String id = idNode.getNodeValue();
				
				if (id != null && !MASSBase.distributed_map.containsKey(id)) {
					MASSBase.distributed_map.put(id, result++);
				}
			}
			
			// result = nodeList.getLength();
		} catch (XPathExpressionException e) {
			MASSBase.getLogger().error("Exception parsing network xml: " + e.getMessage());
		}

		return result;
	}

	/**
     * During instantiation of Places, the "index" must be set. To preserve compatibility with classes derived from Place
     * that require an index value when the constructor is called, this method is provided. This method returns the "index"
     * that should be used for the next Place instantiated.
     * <p>
     * When the Place is instantiated, it's constructor calls this method to get the index value so that the derived class
     * may use it in it's constructor.
     * @return The index value the next Place instantiation will need
     */
    public int[] getNextIndex() {
    	return nextIndex;
    }

    protected void reinitialize() {
    	placesSize = 0;
    	places = null;
	}
}
