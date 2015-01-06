package edu.uw.bothell.css.dsl.MASS;

import java.util.Vector;

public class Places extends Places_base {
	
    //Used to toggle comments from Places.java
    private static final boolean printOutput = false;
    //private static final boolean printOutput = true;

    public Places( int handle, String className, int boundary_width, Object argument, int... size ) {
    	
		super( handle, className, boundary_width, argument, size );
		init_master( argument, boundary_width );
    
    }

    public Places( int handle, String className, Object argument, int... size ) {
    	
		super( handle, className, 0, argument, size );
		init_master( argument, 0 );
    
    }

    @SuppressWarnings("unused")
	public Object ca_setup( int functionId, Object argument,
			    Message.ACTION_TYPE type ) {
    	
		// calculate the total argument size for return-objects
		int total = 1; // the total number of place elements
		for ( int i = 0; i < getSize().length; i++ )
		    total *= getSize()[i];
		int stripe = total / MASS_base.getSystemSize();
	
		// send a PLACES_CALLALL message to each slave
		Message m = null;
		
		for ( int i = 0; i <  MASS.getRemoteNodes().size( ); i++ ) {
			
		    // create a message
		    if ( type == Message.ACTION_TYPE.PLACES_CALL_ALL_VOID_OBJECT )
		    	m = new Message( type, this.getHandle(), functionId, argument );
		    
		    else { 
		    	
		    	// PLACES_CALL_ALL_RETURN_OBJECT
		    	
				int arg_size = ( i == MASS.getRemoteNodes().size( ) - 1 ) ?
				    total - stripe * ( i + 1 ) : stripe;
				
				Object[] partialArguments = new Object[arg_size];
				System.arraycopy( (Object[])argument, stripe * ( i + 1 ),
						  partialArguments, 0, arg_size );
				
				m = new Message( type, this.getHandle(), functionId, 
						 partialArguments );
				
				if ( printOutput == true ) 
				    MASS_base.log( "Places.callAll: arg_size = " + 
						   partialArguments.length +
						   " stripe = " + stripe + 
						   " i + 1 = " + (i + 1) );
		    }
		    
		    // send it
		    MASS.getRemoteNodes().get(i).sendMessage( m );
		    
		    if ( printOutput == true )
			MASS_base.log( "PLACES_CALL_ALL " + m.getAction( ) +
				       " sent to " + i );
		}
	
		// retrieve the corresponding places
		MASS_base.setCurrentPlaces(this);
		MASS_base.setCurrentFunctionId(functionId);
		MASS_base.setCurrentArgument(argument);
		MASS_base.setCurrentMsgType(type);
		
		if (type == Message.ACTION_TYPE.PLACES_CALL_ALL_RETURN_OBJECT) {
			MASS_base.setCurrentReturns(new Object[total]);  // prepare an entire return space
		} else {
			MASS_base.setCurrentReturns(null);
		}
		
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
		MASS.barrier_all_slaves( MASS_base.getCurrentReturns(), stripe );
		
		return MASS_base.getCurrentReturns();
    
    }

    public void callAll( int functionId ) {
		ca_setup( functionId, null, Message.ACTION_TYPE.PLACES_CALL_ALL_VOID_OBJECT );
    }
    
    @SuppressWarnings("unused")
	public void callAll( int functionId, Object argument ) {
	
		if ( printOutput == true )
		    MASS_base.log( "callAll void object" );
		
		ca_setup( functionId, argument, 
			  Message.ACTION_TYPE.PLACES_CALL_ALL_VOID_OBJECT );
    
    }
    
    @SuppressWarnings("unused")
	public Object callAll( int functionId, Object argument[] ) {
	
		if ( printOutput == true )
		    MASS_base.log( "callAll return object" );
		
		return ca_setup( functionId, ( Object )argument,
				 Message.ACTION_TYPE.PLACES_CALL_ALL_RETURN_OBJECT );
    
    }
    
    @SuppressWarnings("unused")
	public void exchangeAll( int dest_handle, int functionId ) {
	
		// send a PLACES_EXCHANGE_ALL message to each slave
		Message m = new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_ALL, 
					  this.getHandle(), dest_handle, functionId );
		
		if ( printOutput == true )
		    MASS_base.log( "dest_handle = " + dest_handle );
		
		for ( int i =0; i < MASS.getRemoteNodes().size( ); i++ )
		    MASS.getRemoteNodes().get(i).sendMessage( m );
		
		// retrieve the corresponding places
		MASS_base.setCurrentPlaces(this);
		MASS_base.setDestinationPlaces(MASS_base.getPlacesMap().get( new Integer( dest_handle ) ));
		MASS_base.setCurrentFunctionId(functionId);
		//MASS_base.currentDestinations = destinations;
		
		// reset requestCounter by the main thread
		MASS_base.resetRequestCounter();
		
		// for debug
		MASS_base.showHosts( );
		
		// resume threads
		Mthread.resumeThreads( Mthread.STATUS_TYPE.STATUS_EXCHANGEALL );
		
		// exchangeall implementation
		super.exchangeAll( MASS_base.getDestinationPlaces(),
				   functionId, 0 );
		
		// confirm all threads are done with exchangeAll.
		Mthread.barrierThreads( 0 );
		
		// Synchronized with all slave processes
		MASS.barrier_all_slaves( );
    
    }
    
    public void exchangeBoundary( ) {
	
		// send a PLACES_EXCHANGE_BOUNDARY message to each slave
		Message m = new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_BOUNDARY, 
					 this.getHandle(),  0 ); // 0 is dummy
		
		for ( MNode node : MASS.getRemoteNodes() )
		    node.sendMessage( m );
	
		// retrieve the corresponding places
		MASS_base.setCurrentPlaces(this);
		
		// for debug
		MASS_base.showHosts( );
		
		// exchange boundary implementation
		super.exchangeBoundary( );
		
		// Synchronized with all slave processes
		MASS.barrier_all_slaves( );
    
    }
    
    @SuppressWarnings("unused")
	public void init_master( Object argument, int boundary_width ) {

		// create a list of all host names;  
		// the master IP name
		Vector<String> hosts = new Vector<String>( );
		
		try {
		    hosts.add( MASS.getMasterNode().getHostName() );
		} catch ( Exception e ) {
		    MASS_base.log( "init_master: InetAddress.getLocalHost( ) " + e );
		    System.exit( -1 );
		}
		
		// all the slave IP names
		for ( MNode node : MASS.getRemoteNodes() ) {
		    hosts.add( node.getHostName( ) );
		}
	
		// create a new list for message
		Message m = new Message( Message.ACTION_TYPE.PLACES_INITIALIZE, getSize(),
					 getHandle(), getClassName(),
					 argument, boundary_width, hosts );
		
		// send a PLACES_INITIALIZE message to each slave
		for ( MNode node : MASS.getRemoteNodes() ) {
		    
			node.sendMessage( m );
		    
		    if ( printOutput == true )
			MASS_base.log( "PLACES_INITIALIZE sent to " + node.getPid() );
		
		}
		
		// establish all inter-node connections within setHosts( )
		MASS_base.setHosts( hosts );
	
		// register this places in the places hash map
		MASS_base.getPlacesMap().put( new Integer( getHandle() ), this );
		
		// Synchronized with all slave processes
		MASS.barrier_all_slaves( );

    }

}