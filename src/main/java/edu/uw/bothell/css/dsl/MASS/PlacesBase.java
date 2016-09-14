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

import java.util.Vector;

import edu.uw.bothell.css.dsl.MASS.factory.ObjectFactory;
import edu.uw.bothell.css.dsl.MASS.factory.SimpleObjectFactory;
import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;

public class PlacesBase {

	// the total number of Places, determined by multiplying the values in the "size" array
    private int total;
    
    private int stripeSize;
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

	// logging
	private Log4J2Logger logger = Log4J2Logger.getInstance();

	public PlacesBase( int handle, String className, int boundary_width, Object argument, int[] size ) {
		
		this.handle = handle;
		this.className = className;
		this.boundaryWidth = boundary_width;
		this.size = size;

		logger.debug( "Places_base handle = " + handle
					+ ", class = " + className
					+ ", argument = " + argument
					+ ", boundary_width = " + boundary_width 
					+ ", size.length = " + size.length );
	
		init_all( argument );
	
	}

	private class ExchangeBoundary_helper extends Thread {

    	int direction;
    	int handle;
    	int places_size;
    	int shadow_size;

    	public ExchangeBoundary_helper( int[] param ) {
    	
    		// identify the boundary space;
    		direction = param[0];
    		handle = param[1];
    		places_size = param[2];
    		shadow_size = param[3];

   			logger.debug( "Places_base.ExchangeBoundary_helper direction"+
    					" = " + direction
    					+ ", handle = " + handle
    					+ ", places_size = " + places_size 
    					+ ", shadow_size = " + shadow_size
    					//+ ", outMessage_size = " + outMessage_size
    					);
    	
    	}

    	public void run( ) {
    		
    		int startIndex = 
    				( direction == 'L' ) ? 0 : places_size -shadow_size;
    		Object[] buffer = new Object[ shadow_size ];

    		// copy all the outMessages into the buffer
    		for ( int i = 0; i < shadow_size; i++ )
    			buffer[i] = places[startIndex + i].getOutMessage();

    		if ( logger.isDebugEnabled() ) {
    			logger.debug( "Places_base.exchangeBoundary_helper direction = {}", direction );

    			for ( int i = 0; i < shadow_size; i++ )
    				logger.debug( "buffer[" + i + "] = " + buffer[i] );

    		}

    		// create a PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST message
    		Message messageToDest = 
    				new Message( Message.ACTION_TYPE.
    						PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST,
    						buffer );

    		// compose a PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST message
    		int destRank = ( direction == 'L' ) ? 
    				
    				MASSBase.getMyPid() - 1 : MASSBase.getMyPid() + 1;

   					logger.debug( "Places_base.exchangeBoundary_helper direction"+
    							" = " + direction + ", rankNmessage.rank = " + 
    							destRank );

    				// send it to my neighbor with a child
    				SendMessageByChild thread_ref = 
    						new SendMessageByChild( destRank, messageToDest );
    				thread_ref.start( );

    				// receive a PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST message from 
    				// my neighbor
    				Message messageFromDest 
    				= MASSBase.getExchange().receiveMessage( destRank );

   					logger.debug( "Places_base.exchangeBoundary_helper direction"+
    							" = " + direction
    							+ ", messageFromDest = " + messageFromDest );

    				// wait for the child termination
    				if ( thread_ref != null ) {
    					
    					try {
    						thread_ref.join( );
    					} 
    					catch ( Exception e ) {
    						logger.error("Unknown exception caught waiting for child termination", e);
    					}
    					
    					logger.debug( "Places_base.exchangeBoundary_helper direction = {}, sendMessageByChild terminated", direction );
    				
    				}

    				buffer = null;
    				messageToDest = null;

    				// extract the message received and copy it to the corresponding 
    				// shadow.
    				Place[] shadow = ( direction == 'L' ) ? 
    						leftShadow : rightShadow;
    				buffer = (Object[])( messageFromDest.getArgument( ) );

    				// copy the buffer contents into the corresponding shadow
    				for ( int i = 0; i < shadow_size; i++ ) {
    					
    					shadow[i].setOutMessage(buffer[i]);
    					
    					if ( logger.isDebugEnabled() ) 
    						logger.debug( "Places_base.exchangeBoundary_helper " +
    								"direction = " + direction +
    								", shadow[" + i + "].outMessage = " +
    								shadow[i].getOutMessage() +
    								", buffer = " + buffer[i] );
    				
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

    		Vector<RemoteExchangeRequest> orgRequest = null;

    		if ( MASS.isConsoleLoggingEnabled() )
    			logger.debug( "rank[{}]: starts processRemoteExchangeRequest", destRank );

    		// pick up the next rank to process
    		orgRequest = MASSBase.getRemoteRequests().get(destRank);

    		// for debugging
    		if ( logger.isDebugEnabled() ) {
    			synchronized( orgRequest ) {
    				logger.debug( "tid[" + destRank + 
    						"] sends an exhange request to rank: " +
    						destRank + " size() = " + 
    						orgRequest.size( ) );
    				
    				for ( int i = 0; i < orgRequest.size( ); i++ )
    					logger.debug( "send from " +
    							orgRequest.get(i).
    							getOrgGlobalLinearIndex() + " to " +
    							orgRequest.get(i).
    							getDestGlobalLinearIndex() + " at " +
    							orgRequest.get(i).getInMessageIndex() );
    			}
    		}
    	
    		// now compose and send a message by a child
    		Message messageToDest = 
    				new Message( Message.ACTION_TYPE.
    						PLACES_EXCHANGE_ALL_REMOTE_REQUEST,
    						srcHandle, destHandle_at_src, functionId, 
    						orgRequest, 0 ); // 0 = dummy

    		SendMessageByChild thread_ref = new SendMessageByChild( destRank, messageToDest );
    		thread_ref.start( );

    		// receive a message by myself
    		Message messageFromSrc = MASSBase.getExchange().receiveMessage( destRank );

    		// at this point, the message must be exchanged.
    		try {
    			thread_ref.join( );
    		} catch ( Exception e ) {
    			// TODO - should do something when this exception is caught - not just swallow it
    			logger.error("Exception during message exchanging in PlacesBase", e);
    		}

    		// process a message
    		Vector<RemoteExchangeRequest> receivedRequest = messageFromSrc.getExchangeReqList( );

    		int destHandle_at_dst = messageFromSrc.getDestHandle( );
    		PlacesBase dstPlaces = 
    				MASSBase.getPlacesMap().get( new Integer( destHandle_at_dst ) );

    		if ( logger.isDebugEnabled() ) {
    			logger.debug( "request from rank[" + destRank + "] = ", receivedRequest );
    			logger.debug( " size( ) = " + receivedRequest.size( ) );
    		}

    		// get prepared for a space to sotre return values
    		Object[] retVals = new Object[receivedRequest.size( )];

    		// for each place, call the corresponding callMethod( ).
    		for ( int i = 0; i < receivedRequest.size( ); i++ ) {

    			if ( logger.isDebugEnabled() )
    				logger.debug( "received from " +
    						receivedRequest.get(i).
    						getOrgGlobalLinearIndex() + " to " +
    						receivedRequest.get(i).
    						getDestGlobalLinearIndex() + " at " +
    						receivedRequest.get(i).getInMessageIndex() + 
    						" dstPlaces.lower = " + 
    						dstPlaces.lowerBoundary +
    						" dstPlaces.upper = " + 
    						dstPlaces.upperBoundary );

    			int globalLinearIndex = 
    					receivedRequest.get(i).getDestGlobalLinearIndex();
    			Object outMessage = receivedRequest.get(i).getOutMessage();

    			if ( globalLinearIndex >= dstPlaces.lowerBoundary &&
    					globalLinearIndex <= dstPlaces.upperBoundary ) {
    				// local destination
    				int destinationLocalLinearIndex = 
    						globalLinearIndex - dstPlaces.lowerBoundary;

    				logger.debug( " dstLocal = ", destinationLocalLinearIndex );

    				Place dstPlace = dstPlaces.places[destinationLocalLinearIndex];

    				// call the destination function
    				retVals[i] = dstPlace.callMethod( functionId, outMessage );
    			}
    		}

    		// send return values by a child thread
    		Message messageToSrc = 
    				new Message( Message.ACTION_TYPE.
    						PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT,
    						retVals );
    		thread_ref = new SendMessageByChild( destRank, messageToSrc );
    		thread_ref.start( );

    		// receive return values by myself in parallel
    		Message messageFromDest = 
    				MASSBase.getExchange().receiveMessage( destRank );

    		// at this point, the message must be exchanged.
    		try {
    			thread_ref.join( );
    		} catch ( Exception e ) {
    			// TODO - need to so something once this exception is thrown
    			logger.debug("Exception thrown while exchanging messages in PlacesBase", e);
    		}

    		// store return values to the orignal places
    		Object[] argument = (Object[])messageFromDest.getArgument( );

    		for ( int i = 0; i < orgRequest.size( ); i++ ) {
    			
    			// local source
    			int orgLocalLinearIndex = 
    					orgRequest.get(i).getOrgGlobalLinearIndex() - my_lower_boundary;

    			// locate a local place
    			PlacesBase srcPlaces = 
    					MASSBase.getPlacesMap().get( new Integer( srcHandle ) );
    			Place srcPlace = srcPlaces.places[orgLocalLinearIndex];

    			// store a return value to it
    			Object inMessage = argument[i];

    			// insert an item at inMessageIndex or just append it.
    			srcPlace.getInMessages()[orgRequest.get(i).getInMessageIndex()] = 
    					inMessage;

    			if ( logger.isDebugEnabled() )
    				logger.debug( "srcPlace[" + srcPlace.getIndex()[0]+ "][" 
    						+ srcPlace.getIndex()[1] + "] inserted " 
    						+ "at " 
    						+ orgRequest.get(i).getInMessageIndex() );
    		}
    	}
    }
    
    private class SendMessageByChild extends Thread {

    	int rank;
    	Message message;
    	
    	public SendMessageByChild( int rank, Message message ) {
    		this.rank = rank;
    		this.message = message;
    	}
    	
    	public void run( ) {
    		MASSBase.getExchange().sendMessage( rank, message );
    	}
    
    }

    public void callAll( int functionId, Object argument, int tid ) {
    	
    	int[] range = new int[2];
    	getLocalRange( range, tid );

    	// debugging
    	if ( logger.isDebugEnabled() )
    		logger.debug( "thread[" + tid + "] callAll functionId = " + 
    				functionId + ", range[0] = " + range[0] + 
    				" range[1] = " + range[1] );

    	if ( range[0] >= 0 && range[1] >= 0 ) {
    		
    		for ( int i = range[0]; i <= range[1]; i++ ) {
    			
    			// TODO - what is being logged here? A Places object?
//    			if ( logger.isDebugEnabled() )
//    				logger.debug( "thread[" + tid + "]: places[i] = " + 
//    						places[i] );

    			places[i].callMethod( functionId, argument );
    		
    		}
    	
    	}
    
    }

    public Object callAll( int functionId, Object[] arguments, int length, int tid ) {

    	int[] range = new int[2];
    	getLocalRange( range, tid );

    	// debugging
    	if ( logger.isDebugEnabled() )
    		logger.debug( "thread[" + tid + 
    				"] callAll_return object functionId = " + 
    				functionId + ", range[0] = " + range[0] + 
    				" range[1] = " + range[1] +
    				", arguments.length = " + length );

    	if ( range[0] >= 0 && range[1] >= 0 ) {

    		for ( int i = range[0]; i <= range[1]; i++ ) {
    			
    			if ( logger.isDebugEnabled() )
    				logger.debug( "thread[" + tid + "]: places[" + i + "] = " + 
    						places[i] );

    			// this fix is kind of a band aid too.
    			if ( arguments == null ) 
    				MASSBase.getCurrentReturns()[i] = places[i].callMethod( functionId, null );
    			else
    				MASSBase.getCurrentReturns()[i] = places[i].callMethod( functionId, arguments[i] );
    		}
    	}
    	
    	return null;
    
    }
    
    public void exchangeAll( PlacesBase dstPlaces, int functionId, int tid ) {
    	int[] range = new int[2];
    	getLocalRange( range, tid );

    	// debugging
    	if ( logger.isDebugEnabled() )
    		logger.debug( "thread[" + tid + "] exchangeAll functionId = " + 
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
    				
    				if ( logger.isDebugEnabled() )
    					logger.debug( "tid[" + tid + "]: calls from"
    							+ "[" + srcPlace.getIndex()[0]
    							+ "][" + srcPlace.getIndex()[1] + "]"
    							+ " (neighborCord[" + neighborCoord[0]
    							+ "][" + neighborCoord[1] + "]"
    							+ " dstPlaces.size[" 
    							+ dstPlaces.size[0] 
    							+ "][" + dstPlaces.size[1] + "]" );

    				if ( neighborCoord[0] != -1 ) { 
    					
    					// destination valid
    					int globalLinearIndex = getGlobalLinearIndexFromGlobalArrayIndex( 
    									neighborCoord,
    									dstPlaces.size );

    					if ( logger.isDebugEnabled() ) 
    						logger.debug( " linear = " + globalLinearIndex
    								+ " lower = " 
    								+ dstPlaces.lowerBoundary
    								+ " upper = " 
    								+ dstPlaces.upperBoundary + ")" );

    					if ( globalLinearIndex >= dstPlaces.lowerBoundary &&
    							globalLinearIndex <= dstPlaces.upperBoundary ) {
    						// local destination
    						int destinationLocalLinearIndex = globalLinearIndex - dstPlaces.lowerBoundary;
    						Place dstPlace = dstPlaces.places[destinationLocalLinearIndex];

    						if ( logger.isDebugEnabled() )
    							logger.debug( " to [" + dstPlace.getIndex()[0] +
    									"][" + dstPlace.getIndex()[1] + "]");

    						// call the destination function
    						Object inMessage = dstPlace.callMethod( functionId, srcPlace.getOutMessage() );

    						// store this inMessage: 
    						srcPlace.getInMessages()[j] = inMessage;

    						// for debug
    						logger.debug( " inMessage = {}",srcPlace.getInMessages()[j] );
    					
    					} else {
    						// remote destination

    						// find the destination node
    						int destRank = getRankFromGlobalLinearIndex( globalLinearIndex );

    						// create a request
    						int orgGlobalLinearIndex =
    								getGlobalLinearIndexFromGlobalArrayIndex( srcPlace.getIndex(), size );
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
    							if ( logger.isDebugEnabled() )
    								logger.debug( "remoteRequest[" + 
    										destRank + "].add:" +
    										" org = " + 
    										orgGlobalLinearIndex +
    										" dst = " + 
    										globalLinearIndex +
    										" size( ) = " +
    										remoteRequests.size( ) );
    						}
    					}
    				} else {
    					//This just fills the log with junk
    					// logger.error( " to destination invalid" );
    				}
    			}
    		}
    	}

    	// all threads must barrier synchronize here.
    	MThread.barrierThreads( tid );
    	
    	if ( tid == 0 ) {
    		logger.debug( "tid[{}] now enters processRemoteExchangeRequest", tid );

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
    				logger.error("Exception thrown in PlacesBase while attempting to join rank {}", rank, e);
    				e.printStackTrace();
    			}
    		}
    	} else {
    		logger.debug( "tid[{}] skips processRemoteExchangeRequest", tid );
    	}
    }

    public void exchangeBoundary( ) {
    	
    	if ( shadowSize == 0 ) { // no boundary, no exchange
    		logger.debug( "places (handle = {}) has NO boundary, and thus invokes NO exchange boundary", handle );
    		return;
    	}

    	ExchangeBoundary_helper thread_ref = null;

    	logger.debug( "exchangeBoundary starts" );

    	int[][] param = new int[2][4];
    	if ( MASSBase.getMyPid() < MASSBase.getSystemSize() - 1 ) {
    		
    		// create a child in charge of handling the right shadow.
    		param[0][0] = 'R';
    		param[0][1] = handle;
    		param[0][2] = placesSize;
    		param[0][3] = shadowSize;
    		logger.debug( "exchangeBoundary: pthreacd_create( helper, R ) places_size= {}", placesSize );

    		thread_ref = new ExchangeBoundary_helper( param[0] );
    		thread_ref.start( );
    	
    	}

    	if ( MASSBase.getMyPid() > 0 ) {
    		
    		// the main takes charge of handling the left shadow.
    		param[1][0] = 'L';
    		param[1][1] = handle;    
    		param[1][2] = placesSize;
    		param[1][3] = shadowSize;

    		logger.debug( "exchangeBoundary: main thread( helper, L ) places_size = {}", placesSize );

    		( new ExchangeBoundary_helper( param[1] ) ).run( );
    	
    	}

    	if ( thread_ref != null ) {
    		
    		// we are done with exchangeBoundary
    		try {
    			thread_ref.join( ); 
    		} 
    		catch ( Exception e ) {
    			
    			logger.debug( "exchangeBoundary: the main failed in joining with the child = {}", e );
    		
    		}
    	
    	}
    
    }

    protected String getClassName() {
		return className;
	}
    
    /** 
     * Converts a given plain single index into a multidimensional index.
     * @param singleIndex An index in a plain single dimension that will be
     *                        converted in a multidimensional index.
     * @return a multidimensional index			   
     */
    protected int[] getGlobalArrayIndex( int singleIndex ) {

    	int[] index = new int[size.length];

    	for ( int i = size.length - 1; i >= 0; i-- ) {
    		// calculate from lower dimensions
    		index[i] = singleIndex % size[i];
    		singleIndex /= size[i];
    	}

    	return index;
    }
    
    protected int getGlobalLinearIndexFromGlobalArrayIndex( int index[], int size[] ) {
    	
    	int retVal = 0;

    	for ( int i = 0; i < index.length; i++ ) {
    		
    		if ( size[i] <= 0 )
    			continue;
    		
    		if ( index[i] >= 0 && index[i] < size[i] ) {
    			retVal = retVal * size[i];
    			retVal += index[i];
    		}
    		else
    			return Integer.MIN_VALUE; // out of space
    	
    	}

    	return retVal;
    
    }

    @SuppressWarnings("unused")
    protected void getGlobalNeighborArrayIndex( int src_index[], 

    		int offset[],
    		int dst_size[], 
    		int dest_index[] ) {
    	
    	for (int i = 0; i < dest_index.length; i++ ) {
    		dest_index[i] = src_index[i] + offset[i]; // calculate dest index

    		if ( dest_index[i] < 0 || dest_index[i] >= dst_size[i] ) {
    			
    			// out of range
    			for ( int j = 0; j < dest_index.length; j++ ) {
    				// all index must be set -1
    				dest_index[j] = -1;
    				return;
    			}
    		
    		}
    	
    	}
    
    }
    
    public int getHandle( ) {
    	return handle;
    }

    protected Place[] getLeftShadow() {
		return leftShadow;
	}

    /** 
     * Returns the first and last of the range that should be allocated
     *        to a given thread
     *
     * @param tid An id of the thread that calls this function.
     * @return An array of two integers: element 0 = the first and 
     *           element 1 = the last
     */
    private void getLocalRange( int[] range, int tid ) {

    	int nThreads = MASSBase.getThreads().length;
    	int portion = placesSize / nThreads; // per-thread allocated  range
    	int remainder = placesSize % nThreads;

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

	protected int getLowerBoundary() {
		return lowerBoundary;
	}

	protected Place[] getPlaces() {
		return places;
	}

	protected int getPlacesSize( ) {
    	return placesSize;
    }

	protected int getRankFromGlobalLinearIndex( int globalLinearIndex ) {

    	if ( total == 0 ) {
    		
    		// first time computation
    		total = 1;
    		for ( int i = 0; i < size.length; i++ )
    			total *= size[i];
    		
    		stripeSize = total / MASSBase.getSystemSize();
    	
    	}

    	int rank, scope;
    	for ( rank = 0, scope = stripeSize ; rank < MASSBase.getSystemSize(); 
    			rank++, scope += stripeSize ) {
    		
    		if ( globalLinearIndex < scope )
    			break;
    	
    	}

    	return ( rank == MASSBase.getSystemSize() ) ? rank - 1 : rank;
    
    }

	protected Place[] getRightShadow() {
		return rightShadow;
	}

	protected int getShadowSize() {
		return shadowSize;
	}

	public int[] getSize() {
		return size;
	}

	protected int getUpperBoundary() {
		return upperBoundary;
	}

    private void init_all( Object argument ) {
    	
    	// For debugging
    	logger.debug( "init_all handle = " + handle + 
    				", class = " + className + 
    				", argument = " + argument );

    	// load the place constructor
    	try {

    		// calculate "total", which is equal to the number of dimensions in "size" 
    		total = 1;
    		for (int i : size) {
    			total *= i;
    		}

    		// stripe size is total number of places divided by the number of nodes
    		stripeSize = total / MASSBase.getSystemSize();

    		// lower_boundary is the first place managed by this node
    		lowerBoundary = stripeSize * MASSBase.getMyPid();
    		
    		// upperBoundary is the last place managed by this node
    		upperBoundary = (MASSBase.getMyPid() < MASSBase.getSystemSize() - 1) ?
    				lowerBoundary + stripeSize - 1 : total - 1;
    		
    		// placesSize is the total number of places managed by this node
    		placesSize = upperBoundary - lowerBoundary + 1;
    		
    		//  maintaining an entire set
    		places = new Place[placesSize];

    		// initialize all Places objects
    		for ( int i = 0; i < placesSize; i++ ) {
    			
    			// instantiate and configure new place
				Place newPlace = objectFactory.getInstance(className, argument);
				newPlace.setIndex(getGlobalArrayIndex(lowerBoundary + i));
				newPlace.setSize(size);
				places[i] = newPlace;

    		}
    	
    	} 
    	
    	// TODO - what to do when this exception is caught?
    	catch ( Exception e ) {
    	  logger.error( "Places_base.init_all: {} not loaded and/or instantiated", className, e);
    	}

    	// allocate the left/right shadows

    	if ( boundaryWidth <= 0 ) {
    		// no shadow space.
    		shadowSize = 0;
    		leftShadow = null;
    		rightShadow = null;
    		return;
    	}

    	shadowSize = ( size.length == 1 ) 
    			? boundaryWidth : total / size[0] * boundaryWidth;
    	
    	logger.debug( "Places_base.shadow_size = {}", shadowSize );

    	leftShadow = ( MASSBase.getMyPid() == 0 ) ?
    			null : new Place[ shadowSize ];
    	rightShadow = 
    			( MASSBase.getMyPid() == MASSBase.getSystemSize() - 1 ) ?
    					null : new Place[ shadowSize ];

    	// initialize the left/right shadows
    	try {
    		
    		for ( int i = 0; i < shadowSize; i++ ) {

    			// left shadow initialization
    			if ( leftShadow != null ) {

    				// instantiate a new place
					Place newPlace = objectFactory.getInstance(className, argument);
					newPlace.setSize(size);
					newPlace.setIndex(getGlobalArrayIndex(lowerBoundary - shadowSize + i));
					leftShadow[i] = newPlace;

    			}

    			// right shadow initialization
    			if ( rightShadow != null ) {

    				// instantiate a new place
					Place newPlace = objectFactory.getInstance(className, argument);
					newPlace.setSize(size);
					newPlace.setIndex(getGlobalArrayIndex(upperBoundary + i));
					rightShadow[i] = newPlace;

    			}
    		
    		}
    	
    	}
    	
    	// TODO - what to do if this is caught?
    	catch ( Exception e ) {
        	logger.error("Unknown exception caught in PlacesBase while initializing left/right shadows", e);
    	} 
    
    }

}