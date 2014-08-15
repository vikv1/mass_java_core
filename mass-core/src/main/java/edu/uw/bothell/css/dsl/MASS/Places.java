package edu.uw.bothell.css.dsl.MASS;

import java.util.*;
import java.net.*;

public class Places extends Places_base {
    //Used to toggle comments from Places.java
    private static final boolean printOutput = false;
    //private static final boolean printOutput = true;

    public Places( int handle, String className, Object argument, 
		   int... size ) {
	super( handle, className, 0, argument, size );
	init_master( argument, 0 );
    }

    public Places( int handle, String className, int boundary_width, 
		   Object argument, int... size ) {
	super( handle, className, boundary_width, argument, size );
	init_master( argument, boundary_width );
    }

    public void init_master( Object argument, int boundary_width ) {

	// create a list of all host names;  
	// the master IP name
	Vector<String> hosts = new Vector<String>( );
	try {
	    String localhost 
		= InetAddress.getLocalHost( ).getCanonicalHostName( );
	    hosts.add( localhost );
	} catch ( Exception e ) {
	    MASS_base.log( "init_master: InetAddress.getLocalHost( ) " + e );
	    System.exit( -1 );
	}
	
	// all the slave IP names
	for ( int i = 0; i < MASS.mNodes.size( ); i++ ) {
	    hosts.add( MASS.mNodes.get(i).getHostName( ) );
	}
	
	// create a new list for message
	Message m = new Message( Message.ACTION_TYPE.PLACES_INITIALIZE, size,
				 handle, className,
				 argument, boundary_width, hosts );
	
	// send a PLACES_INITIALIZE message to each slave
	for ( int i = 0; i < MASS.mNodes.size( ); i++ ) {
	    MASS.mNodes.get(i).sendMessage( m );
	    
	    if ( printOutput == true )
		MASS_base.log( "PLACES_INITIALIZE sent to " + i );
	}
	
	// establish all inter-node connections within setHosts( )
	MASS_base.setHosts( hosts );

	// register this places in the places hash map
	MASS_base.placesMap.put( new Integer( handle ), this );
	
	// Synchronized with all slave processes
	MASS.barrier_all_slaves( );
	

    }

    public void callAll( int functionId ) {
	ca_setup( functionId, null, 
		  Message.ACTION_TYPE.PLACES_CALL_ALL_VOID_OBJECT );
    }
    
    public void callAll( int functionId, Object argument ) {
	
	if ( printOutput == true )
	    MASS_base.log( "callAll void object" );
	
	ca_setup( functionId, argument, 
		  Message.ACTION_TYPE.PLACES_CALL_ALL_VOID_OBJECT );
    }
    
    public Object callAll( int functionId, Object argument[] ) {
	
	if ( printOutput == true )
	    MASS_base.log( "callAll return object" );
	
	return ca_setup( functionId, ( Object )argument,
			 Message.ACTION_TYPE.PLACES_CALL_ALL_RETURN_OBJECT );
    }
    
    public Object ca_setup( int functionId, Object argument,
			    Message.ACTION_TYPE type ) {
	// calculate the total argument size for return-objects
	int total = 1; // the total number of place elements
	for ( int i = 0; i < size.length; i++ )
	    total *= size[i];
	int stripe = total / MASS_base.systemSize;
	
	// send a PLACES_CALLALL message to each slave
	Message m = null;
	for ( int i = 0; i <  MASS.mNodes.size( ); i++ ) {
	    // create a message
	    if ( type == Message.ACTION_TYPE.PLACES_CALL_ALL_VOID_OBJECT )
		m = new Message( type, this.handle, functionId, argument );
	    else { // PLACES_CALL_ALL_RETURN_OBJECT
		int arg_size = ( i == MASS.mNodes.size( ) - 1 ) ?
		    total - stripe * ( i + 1 ) : stripe;
		Object[] partialArguments = new Object[arg_size];
		System.arraycopy( (Object[])argument, stripe * ( i + 1 ),
				  partialArguments, 0, arg_size );
		
		m = new Message( type, this.handle, functionId, 
				 partialArguments );
		
		if ( printOutput == true ) 
		    MASS_base.log( "Places.callAll: arg_size = " + 
				   partialArguments.length +
				   " stripe = " + stripe + 
				   " i + 1 = " + (i + 1) );
	    }
	    
	    // send it
	    MASS.mNodes.get(i).sendMessage( m );
	    
	    if ( printOutput == true )
		MASS_base.log( "PLACES_CALL_ALL " + m.getAction( ) +
			       " sent to " + i );
	}
	
	// retrieve the corresponding places
	MASS_base.currentPlaces = this;
	MASS_base.currentFunctionId = functionId;
	MASS_base.currentArgument = argument;
	MASS_base.currentMsgType = type;
	MASS_base.currentReturns  // prepare an entire return space
	    = ( type == Message.ACTION_TYPE.PLACES_CALL_ALL_RETURN_OBJECT ) ?
	    new Object[total] : null;
	
	// resume threads
	Mthread.resumeThreads( Mthread.STATUS_TYPE.STATUS_CALLALL );
	
	// callall implementation
	if ( type == Message.ACTION_TYPE.PLACES_CALL_ALL_VOID_OBJECT )
	    super.callAll( functionId, argument, 0 ); // 0 = the main tid
	else
	    super.callAll( functionId, (Object[])argument, 
			   ((Object[])argument).length, 0 );
	
	// confirm all threads are done with callAll.
	Mthread.barrierThreads( 0 );
	
	// Synchronized with all slave processes
	MASS.barrier_all_slaves( MASS_base.currentReturns, stripe );
	
	return MASS_base.currentReturns;
    }
    
    public void exchangeAll( int dest_handle, int functionId, 
			     Vector<int[]> destinations ) {
	
	// send a PLACES_EXCHANGE_ALL message to each slave
	Message m = new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_ALL, 
				  this.handle, dest_handle, functionId, 
				 destinations );
	
	if ( printOutput == true )
	    MASS_base.log( "dest_handle = " + dest_handle );
	
	for ( int i =0; i < MASS.mNodes.size( ); i++ )
	    MASS.mNodes.get(i).sendMessage( m );
	
	// retrieve the corresponding places
	MASS_base.currentPlaces = this;
	MASS_base.destinationPlaces = 
	    MASS_base.placesMap.get( new Integer( dest_handle ) );
	MASS_base.currentFunctionId = functionId;
	MASS_base.currentDestinations = destinations;
	
	// reset requestCounter by the main thread
	MASS_base.requestCounter = 0;
	
	// for debug
	MASS_base.showHosts( );
	
	// resume threads
	Mthread.resumeThreads( Mthread.STATUS_TYPE.STATUS_EXCHANGEALL );
	
	// exchangeall implementation
	super.exchangeAll( MASS_base.destinationPlaces,
			   functionId, 
			   MASS_base.currentDestinations, 0 );
	
	// confirm all threads are done with exchangeAll.
	Mthread.barrierThreads( 0 );
	
	// Synchronized with all slave processes
	MASS.barrier_all_slaves( );
    }
    
    public void exchangeBoundary( ) {
	
	// send a PLACES_EXCHANGE_BOUNDARY message to each slave
	Message m = new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_BOUNDARY, 
				 this.handle,  0 ); // 0 is dummy
	
	for ( int i = 0; i < MASS.mNodes.size( ); i++ )
	    MASS.mNodes.get(i).sendMessage( m );

	// retrieve the corresponding places
	MASS_base.currentPlaces = this;
	
	// for debug
	MASS_base.showHosts( );
	
	// exchange boundary implementation
	super.exchangeBoundary( );
	
	// Synchronized with all slave processes
	MASS.barrier_all_slaves( );
    }
} 