package edu.uw.bothell.css.dsl.MASS;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Vector;

public class Places_base {

	//Used to toggle comments from Places_base.java
    private static final boolean printOutput = false;
    //private static final boolean printOutput = true;

    private int total;
    private int stripe;

    private final int handle;
    private final String className;
    
    private int lower_boundary;
    private int upper_boundary;
    private int places_size;
    private int[] size;
    private int shadow_size;
    private int boundary_width;
    
    private static URLClassLoader placeLoader;
    private Class<?> placeClass;
    private Constructor<?> placeConstructor;
    private Place[] places;
    private Place[] left_shadow;
    private Place[] right_shadow;

    private static int[] placeInitIndex;
    private static int[] placeInitSize;

    @SuppressWarnings("unused")
	public Places_base( int handle, String className, int boundary_width,
			Object argument, int[] size ) {
	this.handle = handle;
	this.className = className;
	this.boundary_width = boundary_width;
	this.size = size;

	this.total = 0;
	this.stripe = 0;

	if ( printOutput == true )
	    MASS_base.log( "Places_base handle = " + handle
			   + ", class = " + className
			   + ", argument = " + argument
			   + ", boundary_width = " + boundary_width 
			   + ", size.length = " + size.length );

	init_all( argument );
    }

    @SuppressWarnings("unused")
	public void init_all( Object argument ) {
	// For debugging
	if ( printOutput == true ) {
	    MASS_base.log( "init_all handle = " + handle + 
			   ", class = " + className + 
			   ", argument = " + argument );
	}

	//String convert = null;
	//for ( int i = 0; i < size.length; i++ )
	//  convert += ( "size[" + i + "] = " + size[i] + "  " );
	//MASS_base.log(  convert );
	
	// Print the current working directory
	// MASS_base.log( "CUR_DIR = " + MASS_base.CUR_DIR );
	
	// load the place construtor
	File curDir   = new File( MASS.getWorkingDirectory() );
	try {
	    placeLoader =
		URLClassLoader.
		newInstance( new URL[] { curDir.toURI().toURL( ) } );
	    placeClass =                                        //get class
		Class.forName( className, true, placeLoader ); 
	    placeConstructor =                            //get constructor
		placeClass.getConstructor( Object.class ); 
	    
	    // calculate lower_boundary and upper_boundary
	    total = 1;
	    for ( int i = 0; i < size.length; i++ )
		total *= size[i];
	    stripe = total / MASS_base.getSystemSize();
	    
	    lower_boundary = stripe * MASS_base.getMyPid();
	    upper_boundary = (MASS_base.getMyPid() < MASS_base.getSystemSize() - 1) ?
		lower_boundary + stripe - 1 : total - 1;
	    places_size = upper_boundary - lower_boundary + 1;
	    
	    // instantiate Places objects
//	    this.places_size = places_size;
	    
	    //  maintaining an entire set
	    places = new Place[places_size];
	    
	    // initialize all Places objects
	    for ( int i = 0; i < places_size; i++ ) {
		// instanitate a new place
		placeInitSize = size.clone( );
		placeInitIndex = getGlobalArrayIndex( lower_boundary + i );
		places[i] = 
		    ( Place )placeConstructor.newInstance( argument );
	    }
	} catch ( Exception e ) {
	    MASS_base.log( "Places_base.init_all: " + className + 
			   " not loaded and/or instantiated " + e );
	}
	
	// allocate the left/right shadows
	
	if ( boundary_width <= 0 ) {
	    // no shadow space.
	    shadow_size = 0;
	    left_shadow = null;
	    right_shadow = null;
	    return;
	}
	
	shadow_size = ( size.length == 1 ) 
	    ? boundary_width : total / size[0] * boundary_width;
	if ( printOutput == true )
	    MASS_base.log( "Places_base.shadow_size = " + shadow_size );
	
	left_shadow = ( MASS_base.getMyPid() == 0 ) ?
	    null : new Place[ shadow_size ];
	right_shadow = 
	    ( MASS_base.getMyPid() == MASS_base.getSystemSize() - 1 ) ?
	    null : new Place[ shadow_size ];
	
	// initialize the left/right shadows
	try {
	    for ( int i = 0; i < shadow_size; i++ ) {
		
		// left shadow initialization
		if ( left_shadow != null ) {
		    // instanitate a new place
		    placeInitSize = size.clone( );
		    placeInitIndex = 
			getGlobalArrayIndex( lower_boundary - shadow_size 
					     + i );
		    left_shadow[i] = 
			( Place )placeConstructor.newInstance( argument );
		    left_shadow[i].setOutMessage(null);
		}
		
		// right shadow initialization
		if ( right_shadow != null ) {
		    // instanitate a new place
		    placeInitSize = size.clone( );
		    placeInitIndex = 
			getGlobalArrayIndex( upper_boundary + i );
		    right_shadow[i] = 
			( Place )placeConstructor.newInstance( argument );
		    right_shadow[i].setOutMessage(null);
		}
	    }
	} catch ( Exception e ) { } 
    }

    protected int[] getGlobalArrayIndex( int singleIndex ) {
    	
		int[] index = new int[size.length];
	
		for ( int i = size.length - 1; i >= 0; i-- ) {
		    // calculate from lower dimensions
		    index[i] = singleIndex % size[i];
		    singleIndex /= size[i];
		}
	
		return index;
    }

    @SuppressWarnings("unused")
	public void callAll( int functionId, Object argument, int tid ) {
	int[] range = new int[2];
	getLocalRange( range, tid );

	// debugging
	if ( printOutput == true )
	    MASS_base.log( "thread[" + tid + "] callAll functionId = " + 
			   functionId + ", range[0] = " + range[0] + 
			   " range[1] = " + range[1] );
	
	if ( range[0] >= 0 && range[1] >= 0 ) {
	    for ( int i = range[0]; i <= range[1]; i++ ) {
		if ( printOutput == true )
		    MASS_base.log( "thread[" + tid + "]: places[i] = " + 
				   places[i] );

		places[i].callMethod( functionId, argument );
	    }
	}
    }

    @SuppressWarnings("unused")
	public Object callAll( int functionId, Object[] arguments, int length,
			   int tid ) {
    	
		int[] range = new int[2];
		getLocalRange( range, tid );
	
		// debugging
		if ( printOutput == true )
		    MASS_base.log( "thread[" + tid + 
				   "] callAll_return object functionId = " + 
				   functionId + ", range[0] = " + range[0] + 
				   " range[1] = " + range[1] +
				   ", arguments.length = " + length );
	
		if ( range[0] >= 0 && range[1] >= 0 ) {
			
		    for ( int i = range[0]; i <= range[1]; i++ ) {
				if ( printOutput == true )
				    MASS_base.log( "thread[" + tid + "]: places[i] = " + 
						   places[i] );
		
				MASS_base.getCurrentReturns()[i] = 
				    places[i].callMethod( functionId, arguments[i] );
		    }
		}
		return null;
    }

    @SuppressWarnings("unused")
	public void exchangeAll( Places_base dstPlaces, int functionId, int tid ) {

		int[] range = new int[2];
		getLocalRange( range, tid );
		
		// debugging
		if ( printOutput == true )
		    MASS_base.log( "thread[" + tid + "] exchangeAll functionId = " + 
				   functionId + ", range[0] = " + range[0] + 
				   " range[1] = " + range[1] );
		
		// TODO: Need to find a way to replace destinations with same meaning code block
		
	//	if (printOutput == true) {
	//	    MASS_base.log( "tid[" + tid + "]: checks destinations:" );
	//	    for ( int i = 0; i < destinations.size( ); i++ ) {
	//			int[] offset = destinations.get(i);
	//			MASS_base.log( "[" + offset[0]+ "][" + offset[1] + "]  " );    
	//	    }
	//	}
	
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
				    getGlobalNeighborArrayIndex( srcPlace.getIndex(), offset, 
								 dstPlaces.size,
								 neighborCoord );
				    if ( printOutput == true )
					MASS_base.log( "tid[" + tid + "]: calls from"
						       + "[" + srcPlace.getIndex()[0]
						       + "][" + srcPlace.getIndex()[1] + "]"
						       + " (neighborCord[" + neighborCoord[0]
						       + "][" + neighborCoord[1] + "]"
						       + " dstPlaces.size[" 
						       + dstPlaces.size[0] 
						       + "][" + dstPlaces.size[1] + "]" );
		
				    if ( neighborCoord[0] != -1 ) { 
						// destination valid
						int globalLinearIndex = 
						    getGlobalLinearIndexFromGlobalArrayIndex( 
									     neighborCoord,
									     dstPlaces.size );
			
						if ( printOutput == true ) 
						    MASS_base.log( " linear = " + globalLinearIndex
								   + " lower = " 
								   + dstPlaces.lower_boundary
								   + " upper = " 
								   + dstPlaces.upper_boundary + ")" );
						
						if ( globalLinearIndex >= dstPlaces.lower_boundary &&
						     globalLinearIndex <= dstPlaces.upper_boundary ) {
						    // local destination
						    int destinationLocalLinearIndex 
							= globalLinearIndex - dstPlaces.lower_boundary;
						    Place dstPlace = 
							dstPlaces.places[destinationLocalLinearIndex];
						    
						    if ( printOutput == true )
							MASS_base.log( " to [" + dstPlace.getIndex()[0] +
								       "][" + dstPlace.getIndex()[1] + "]");
			
						    // call the destination function
						    Object inMessage =
							dstPlace.callMethod( functionId, 
									     srcPlace.getOutMessage() );
						    
						    // store this inMessage: 
						    srcPlace.getInMessages()[j] = inMessage;
						    
						    // for debug
						    if ( printOutput == true )
							MASS_base.log( " inMessage = " +
								       srcPlace.getInMessages()[j] );
						} else {
						    // remote destination
						    
						    // find the destination node
						    int destRank = getRankFromGlobalLinearIndex( 
			                                           globalLinearIndex );
						    
						    // create a request
						    int orgGlobalLinearIndex =
							getGlobalLinearIndexFromGlobalArrayIndex( 
									        srcPlace.getIndex(), size );
						    RemoteExchangeRequest request = new
							RemoteExchangeRequest( globalLinearIndex,
									       orgGlobalLinearIndex,
									       j, // inMsgIndex
									       srcPlace.getOutMessage() );
						    
						    // enqueue the request to this node.map
						    Vector<RemoteExchangeRequest> remoteRequests = 
							MASS_base.getRemoteRequests().get( destRank );
						    synchronized( remoteRequests ) {
							remoteRequests.add( request );
							if ( printOutput == true )
							    MASS_base.log( "remoteRequest[" + 
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
					if ( printOutput == true )
					    MASS_base.log( " to destination invalid" );
				    }
				}
		    }
		}
		
		// all threads must barrier synchronize here.
		Mthread.barrierThreads( tid );
		if ( tid == 0 ) {
		    
		    if ( printOutput == true )
			MASS_base.log( "tid[" + tid + 
				       "] now enters processRemoteExchangeRequest" );
			
		    // the main thread spawns as many communication threads as 
		    // the number of remote computing nodes and let each invoke 
		    // processRemoteExchangeReq.
		    
		    // args to threads: 
		    // rank, srcHandle, dstHandle, functionId, lower_boundary
		    int[][] comThrArgs = new int[MASS_base.getSystemSize()][5];
		    ProcessRemoteExchangeRequest[] thread_ref
			= new ProcessRemoteExchangeRequest[MASS_base.getSystemSize()]; 
		    for ( int rank = 0; rank < MASS_base.getSystemSize(); rank++ ) {
			
			if ( rank == MASS_base.getMyPid() ) // don't communicate with myself
			    continue;
			
			// set arguments 
			comThrArgs[rank][0] = rank;
			comThrArgs[rank][1] = handle;
			comThrArgs[rank][2] = dstPlaces.handle;
			comThrArgs[rank][3] = functionId;
			comThrArgs[rank][4] = lower_boundary;
			
			// start a communication thread
			thread_ref[rank] = 
			    new ProcessRemoteExchangeRequest( comThrArgs[rank] );
			thread_ref[rank].start( );
		    }
		    
		    // wait for all the communication threads to be terminated
		    for ( int rank = 0; rank < MASS_base.getSystemSize(); rank++ ) {
				if ( rank == MASS_base.getMyPid() ) // don't communicate with myself
				    continue;      
				try {
				    thread_ref[rank].join( );
				} catch ( Exception e ) { }
			}
		}
		else {
		    if ( printOutput == true )
			MASS_base.log( "tid[" + tid + 
				       "] skips processRemoteExchangeRequest" );
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

	@SuppressWarnings("unused")
	public void run( ) {
	
	    Vector<RemoteExchangeRequest> orgRequest = null;
	    
	    if ( printOutput == true )
		MASS_base.log( "rank[" + destRank + 
			       "]: starts processRemoteExchangeRequest" );
	    
	    // pick up the next rank to process
	    orgRequest = MASS_base.getRemoteRequests().get(destRank);
	    
	    // for debugging
	    synchronized( orgRequest ) {
		if ( printOutput == true ) {
		    MASS_base.log( "tid[" + destRank + 
				   "] sends an exhange request to rank: " +
				   destRank + " size() = " + 
				   orgRequest.size( ) );
		    for ( int i = 0; i < orgRequest.size( ); i++ )
			MASS_base.log( "send from " +
				       orgRequest.get(i).
				       getOrgGlobalLinearIndex() + " to " +
				       orgRequest.get(i).
				       getDestGlobalLinearIndex() + " at " +
				       orgRequest.get(i).getInMessageIndex() );
		}
	    }
	    
	    // now compose and send a message by a child
	    Message messageToDest = new
		Message( Message.ACTION_TYPE.
			 PLACES_EXCHANGE_ALL_REMOTE_REQUEST,
			 srcHandle, destHandle_at_src, functionId, 
			 orgRequest, 0 ); // 0 = dummy
	    
	    SendMessageByChild thread_ref = new 
		SendMessageByChild( destRank, messageToDest );
	    thread_ref.start( );
	    
	    // receive a message by myself
	    Message messageFromSrc = 
		MASS_base.getExchange().receiveMessage( destRank );
	    
	    // at this point, the message must be exchanged.
	    try {
		thread_ref.join( );
	    } catch ( Exception e ) { }
	    
	    // process a message
	    Vector<RemoteExchangeRequest> receivedRequest 
		= messageFromSrc.getExchangeReqList( );
	    
	    int destHandle_at_dst = messageFromSrc.getDestHandle( );
	    Places_base dstPlaces = 
		MASS_base.getPlacesMap().get( new Integer( destHandle_at_dst ) );
	    
	    if ( printOutput == true ) {
		MASS_base.log( "request from rank[" + destRank + "] = " +
			       receivedRequest );
		MASS_base.log( " size( ) = " + receivedRequest.size( ) );
	    }
	    
	    // get prepared for a space to sotre return values
	    Object[] retVals = new Object[receivedRequest.size( )];
	    
	    // for each place, call the corresponding callMethod( ).
	    for ( int i = 0; i < receivedRequest.size( ); i++ ) {
		
		if ( printOutput == true )
		    MASS_base.log( "received from " +
				   receivedRequest.get(i).
				   getOrgGlobalLinearIndex() + " to " +
				   receivedRequest.get(i).
				   getDestGlobalLinearIndex() + " at " +
				   receivedRequest.get(i).getInMessageIndex() + 
				   " dstPlaces.lower = " + 
				   dstPlaces.lower_boundary +
				   " dstPlaces.upper = " + 
				   dstPlaces.upper_boundary );

		int globalLinearIndex = 
		    receivedRequest.get(i).getDestGlobalLinearIndex();
		Object outMessage = receivedRequest.get(i).getOutMessage();
		
		if ( globalLinearIndex >= dstPlaces.lower_boundary &&
		     globalLinearIndex <= dstPlaces.upper_boundary ) {
		    // local destination
		    int destinationLocalLinearIndex 
			= globalLinearIndex - dstPlaces.lower_boundary;
		    
		    if ( printOutput == true )
			MASS_base.log( " dstLocal = " + 
				       destinationLocalLinearIndex );
				       
		    Place dstPlace 
			= dstPlaces.places[destinationLocalLinearIndex];
				       
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
	    Message messageFromDest 
		= MASS_base.getExchange().receiveMessage( destRank );
	    
	    // at this point, the message must be exchanged.
	    try {
		thread_ref.join( );
	    } catch ( Exception e ) { }
	    
	    // store return values to the orignal places
	    Object[] argument = (Object[])messageFromDest.getArgument( );
	    
	    for ( int i = 0; i < orgRequest.size( ); i++ ) {
		// local source
		int orgLocalLinearIndex
		    = orgRequest.get(i).getOrgGlobalLinearIndex() - 
		    my_lower_boundary;
		
		// locate a local place
		Places_base srcPlaces 
		    = MASS_base.getPlacesMap().get( new Integer( srcHandle ) );
		Place srcPlace = srcPlaces.places[orgLocalLinearIndex];
		
		// store a return value to it
		Object inMessage = argument[i];
		
		// insert an item at inMessageIndex or just append it.
		srcPlace.getInMessages()[orgRequest.get(i).getInMessageIndex()]
		    = inMessage;
		
		if ( printOutput == true )
		    MASS_base.log( "srcPlace[" + srcPlace.getIndex()[0]+ "][" 
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
	    MASS_base.getExchange().sendMessage( rank, message );
	}
    }
    
    @SuppressWarnings("unused")
	public void exchangeBoundary( ) {
	if ( shadow_size == 0 ) { // no boundary, no exchange
	    MASS_base.log( "places (handle = " + handle +
			   ") has NO boundary, " + 
			   "and thus invokes NO exchange boundary" );
	    return;
	}
	
	ExchangeBoundary_helper thread_ref = null;
	
	if ( printOutput == true ) {
	    MASS_base.log( "exchangeBoundary starts" );
	}
	
	int[][] param = new int[2][4];
	if ( MASS_base.getMyPid() < MASS_base.getSystemSize() - 1 ) {
	    // create a child in charge of handling the right shadow.
	    param[0][0] = 'R';
	    param[0][1] = handle;
	    param[0][2] = places_size;
	    param[0][3] = shadow_size;
	    if ( printOutput == true ) 
		MASS_base.log( "exchangeBoundary: " +
			       "pthreacd_create( helper, R ) places_size=" +
			       places_size );

	    thread_ref = new ExchangeBoundary_helper( param[0] );
	    thread_ref.start( );
	}
	
	if ( MASS_base.getMyPid() > 0 ) {
	    // the main takes charge of handling the left shadow.
	    param[1][0] = 'L';
	    param[1][1] = handle;    
	    param[1][2] = places_size;
	    param[1][3] = shadow_size;
	    if ( printOutput == true ) 
		MASS_base.log( "exchangeBoundary: " +
			       "main thread( helper, L ) places_size=" + 
			       places_size );

	    ( new ExchangeBoundary_helper( param[1] ) ).run( );
	}
	
	if ( thread_ref != null ) {
	    // we are done with exchangeBoundary
	    try {
		thread_ref.join( ); 
	    } catch ( Exception e ) {
		MASS_base.log( "exchangeBoundary: " +
			       "the main failed in joining with the child = " +
			       e );
	    }
	}
    }

    private class ExchangeBoundary_helper extends Thread {
	int direction;
	int handle;
	int places_size;
	int shadow_size;

	@SuppressWarnings("unused")
	public ExchangeBoundary_helper( int[] param ) {
	    // identifiy the boundary space;
	    direction = param[0];
	    handle = param[1];
	    places_size = param[2];
	    shadow_size = param[3];
	
	    if ( printOutput == true )
		MASS_base.log( "Places_base.ExchangeBoundary_helper direction"+
			       " = " + direction
			       + ", handle = " + handle
			       + ", places_size = " + places_size 
			       + ", shadow_size = " + shadow_size
			       //+ ", outMessage_size = " + outMessage_size
			       );
	}
	
	@SuppressWarnings("unused")
	public void run( ) {
	    int startIndex = 
		( direction == 'L' ) ? 0 : places_size -shadow_size;
	    Object[] buffer = new Object[ shadow_size ];

	    // copy all the outMessages into the buffer
	    for ( int i = 0; i < shadow_size; i++ )
		buffer[i] = places[startIndex + i].getOutMessage();
	    
	    if ( printOutput == true ) {
		MASS_base.log( "Places_base.exchangeBoundary_helper direction"+
			       " = " + direction );
		
		for ( int i = 0; i < shadow_size; i++ )
		    MASS_base.log( "buffer[" + i + "] = " + buffer[i] );
		
	    }
	    
	    // create a PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST message
	    Message messageToDest = 
		new Message( Message.ACTION_TYPE.
			     PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST,
			     buffer );
	    
	    // compose a PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST message
	    int destRank = ( direction == 'L' ) ? 
		MASS_base.getMyPid() - 1 : MASS_base.getMyPid() + 1;
	    
	    if ( printOutput == true )
		MASS_base.log( "Places_base.exchangeBoundary_helper direction"+
			       " = " + direction + ", rankNmessage.rank = " + 
			       destRank );
	    
	    // send it to my neighbor with a child
	    SendMessageByChild thread_ref = 
		new SendMessageByChild( destRank, messageToDest );
	    thread_ref.start( );
	    
	    // receive a PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST message from 
	    // my neighbor
	    Message messageFromDest 
		= MASS_base.getExchange().receiveMessage( destRank );
	    
	    if ( printOutput == true )
		MASS_base.log( "Places_base.exchangeBoundary_helper direction"+
			       " = " + direction
			       + ", messageFromDest = " + messageFromDest );
	    
	    // wait for the child termination
	    if ( thread_ref != null ) {
		try {
		    thread_ref.join( );
		} catch ( Exception e ) { }
		MASS_base.log( "Places_base.exchangeBoundary_helper " +
			       "direction = " + direction +
			       ", sendMessageByChild terminated" );
	    }
	    
	    buffer = null;
	    messageToDest = null;
	    
	    // extract the message received and copy it to the corresponding 
	    // shadow.
	    Place[] shadow = ( direction == 'L' ) ? 
		left_shadow : right_shadow;
	    buffer = (Object[])( messageFromDest.getArgument( ) );
	    
	    // copy the buffer contents into the corresponding shadow
	    for ( int i = 0; i < shadow_size; i++ ) {
		shadow[i].setOutMessage(buffer[i]);
		if ( printOutput == true ) 
		    MASS_base.log( "Places_base.exchangeBoundary_helper " +
				   "direction = " + direction +
				   ", shadow[" + i + "].outMessage = " +
				   shadow[i].getOutMessage() +
				   ", buffer = " + buffer[i] );
	    }  
	}
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
    
    protected int getGlobalLinearIndexFromGlobalArrayIndex( int index[], 
							    int size[] ) {
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
    
    protected int getRankFromGlobalLinearIndex( int globalLinearIndex ) {
	if ( total == 0 ) {
	    // first time computation
	    total = 1;
	    for ( int i = 0; i < size.length; i++ )
		total *= size[i];
	    stripe = total / MASS_base.getSystemSize();
	}
	
	int rank, scope;
	for ( rank = 0, scope = stripe ; rank < MASS_base.getSystemSize(); 
	      rank++, scope += stripe ) {
	    if ( globalLinearIndex < scope )
		break;
	}
	
	return ( rank == MASS_base.getSystemSize() ) ? rank - 1 : rank;
    }


    private void getLocalRange( int[] range, int tid ) {
	int nThreads = MASS_base.getThreads().length;
	int portion = places_size / nThreads; // per-thread allocated  range
	int remainder = places_size % nThreads;

	if ( portion == 0 ) {
	    // there are more threads than elements in the MASS.Places
	    if ( remainder > tid ) {
		range[0] = tid;
		range[1] = tid;
	    }
	    else {
		range[0] = -1;
		range[1] = -1;
	    }
	}
	else {
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
    
    public int getHandle( ) {
	return handle;
    }

    public int getPlacesSize( ) {
	return places_size;
    }

	public Place[] getPlaces() {
		return places;
	}

	public int[] getSize() {
		return size;
	}

	public int getLowerBoundary() {
		return lower_boundary;
	}

	public int getUpperBoundary() {
		return upper_boundary;
	}

	public int getShadowSize() {
		return shadow_size;
	}

	public Place[] getLeftShadow() {
		return left_shadow;
	}

	public Place[] getRightShadow() {
		return right_shadow;
	}

	public static int[] getPlaceInitIndex() {
		return placeInitIndex;
	}

	public static int[] getPlaceInitSize() {
		return placeInitSize;
	}

	public String getClassName() {
		return className;
	}

}