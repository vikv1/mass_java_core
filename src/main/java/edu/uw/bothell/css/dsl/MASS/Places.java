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

import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;

/**
 * Places manages all Place elements within the simulation space.
 */
public class Places extends PlacesBase {

	// logging
	private Log4J2Logger logger = Log4J2Logger.getInstance();

	/**
	 * Places constructor that creates places with a given dimension.
	 * @param handle	A unique identifier that designates a group of places.
	 * 					Must be unique over all machines.
	 * @param className	The user-implemented class Places are constructed from
	 * @param boundaryWidth
	 * @param argument
	 * @param size
	 */	public Places( int handle, String className, int boundaryWidth, Object argument, int... size ) {
    	
		super( handle, className, boundaryWidth, argument, size );
		init_master( argument, boundaryWidth );
    
    }

	/**
	 * Instantiates a shared array with "size[]" from the "className" class as
	 * passing an argument to the "className" constructor. This array is
	 * associated with a user-given handle that must be unique over
	 * machines.
	 * Dimensions are numerated in the "..." format.
	 * @param handle	A unique identifier that designates a group of places.  
	 *                 	Must be unique over all machines.
	 * @param className	The user-implemented class Places are constructed from
	 * @param argument
	 * @param size
	 */
    public Places( int handle, String className, Object argument, int... size ) {
    	
		super( handle, className, 0, argument, size );
		init_master( argument, 0 );
    
    }

	private Object[] ca_setup( int functionId, Object argument, Message.ACTION_TYPE type ) {
    	
		// calculate the total argument size for return-objects
		int total = 1; // the total number of place elements
		for ( int i = 0; i < getSize().length; i++ )
		    total *= getSize()[i];
		int stripe = total / MASSBase.getSystemSize();
		

	
		// send a PLACES_CALLALL message to each slave
		Message m = null;
		
		for ( int i = 0; i <  MASS.getRemoteNodes().size( ); i++ ) {
			
		    // create a message
		    if ( type == Message.ACTION_TYPE.PLACES_CALL_ALL_VOID_OBJECT )
		    	m = new Message( type, this.getHandle(), functionId, argument );
		    
		    else { 
		    	
		    	// PLACES_CALL_ALL_RETURN_OBJECT
		    	
				int arg_size = ( i == MASS.getRemoteNodes().size( ) - 1 ) ? total - stripe * ( i + 1 ) : stripe;
				
				Object[] partialArguments = new Object[arg_size];
				
				// This is a band-aid If you just pass null into this function this function will throw a null pointer exception. 
				try {
					System.arraycopy( (Object[])argument, stripe * ( i + 1 ), partialArguments, 0, arg_size );
				} catch ( NullPointerException e ) {
					
				}
				
				m = new Message( type, this.getHandle(), functionId, partialArguments );
				
			    logger.debug( "Places.callAll: arg_size = " + 
						   (partialArguments == null ? 0 : partialArguments.length) +
						   " stripe = " + stripe + 
						   " i + 1 = " + (i + 1) );
		    }
		    
		    // send it
		    MASS.getRemoteNodes().get(i).sendMessage( m );
		    
		    logger.debug( "PLACES_CALL_ALL " + m.getAction( ) +
				       " sent to " + i );
		}
	
		// retrieve the corresponding places
		MASSBase.setCurrentPlaces(this);
		MASSBase.setCurrentFunctionId(functionId);
		MASSBase.setCurrentArgument(argument);
		MASSBase.setCurrentMsgType(type);
		
		if (type == Message.ACTION_TYPE.PLACES_CALL_ALL_RETURN_OBJECT) {
			MASSBase.setCurrentReturns(new Object[total]);  // prepare an entire return space
		} else {
			MASSBase.setCurrentReturns(null);
		}
		
		// resume threads
		MThread.resumeThreads( MThread.STATUS_TYPE.STATUS_CALLALL );
		
		// callAll implementation
		if ( type == Message.ACTION_TYPE.PLACES_CALL_ALL_VOID_OBJECT /*|| type == Message.ACTION_TYPE.PLACES_CALL_ALL_RETURN_OBJECT */)
		    super.callAll( functionId, argument, 0 ); // 0 = the main tid\
		else {
			int argumentLength = argument == null ? 0 : ((Object[])argument).length;
		    super.callAll( functionId, (Object[])argument, argumentLength, 0 );
		}
		
		// confirm all threads are done with callAll.
		MThread.barrierThreads( 0 );
		
		// Synchronized with all slave processes
		MASS.barrierAllSlaves( MASSBase.getCurrentReturns(), stripe );
		
		return MASSBase.getCurrentReturns();
    
    }

	/**
	 * Calls the method specified with functionId of all array elements. Done
	 * in parallel among multi-processes/threads.
	 * @param functionId The ID of the function to call
	 */
	public void callAll( int functionId ) {
		ca_setup( functionId, null, Message.ACTION_TYPE.PLACES_CALL_ALL_VOID_OBJECT );
    }
    
	/**
	 * Calls the method specified with functionId of all array elements as
	 * passing an argument to the method. Done in parallel among multi-
	 * processes/threads.
	 * @param functionId The ID of the function to call
	 * @param argument An argument to supply to the function being called in each Place
	 */
	public void callAll( int functionId, Object argument ) {
	
	    logger.debug( "callAll void object" );
		
		ca_setup( functionId, argument, 
			  Message.ACTION_TYPE.PLACES_CALL_ALL_VOID_OBJECT );
    
    }
    
	/**
	 * Calls the method specified with functionId of all array elements as
	 * passing arguments[i] to element[i]’s method, and receives a return
	 * value from it into (void *)[i] whose element’s size is return_size. Done 
	 * in parallel among multi-processes/threads. In case of a multi-
	 * dimensional array, "i" is considered as the index when the array is
	 * flattened to a single dimension.
	 * @param functionId The ID of the function to call
	 * @param argument An argument to supply to the function being called in each Place
	 * @return An Object (actually, an Object[]) with each element set to the return value
	 * 			supplied by each Place in the cluster
	 */
	public Object[] callAll( int functionId, Object argument[] ) {
	
	    logger.debug( "callAll return object" );
		
		return ca_setup( functionId, ( Object )argument,
				 Message.ACTION_TYPE.PLACES_CALL_ALL_RETURN_OBJECT );
    
    }
	
    
	/**
	 * Calls from each of all cells to the method specified with functionId of
	 * all destination cells, each indexed with a different Vector element.
	 * Each vector element, say destination[] is an array of integers where
	 * destination[i] includes a relative index (or a distance) on the coordinate
	 * i from the current caller to the callee cell. The caller cell’s outMessage
	 * is a continuous set of arguments passed to the callee’s method. The
	 * caller’s inMessages[] stores values returned from all callees. More
	 * specifically, inMessages[i] maintains a set of return values from the i th
	 * callee.
	 * @param destinationHandle
	 * @param functionId The ID of the function to call
	 */
	public void exchangeAll( int destinationHandle, int functionId ) {
	
		// send a PLACES_EXCHANGE_ALL message to each slave
		Message m = new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_ALL, this.getHandle(), destinationHandle, functionId );
		
	    logger.debug( "dest_handle = {}", destinationHandle );
		
		for ( int i =0; i < MASS.getRemoteNodes().size( ); i++ )
		    MASS.getRemoteNodes().get(i).sendMessage( m );
		
		// retrieve the corresponding places
		MASSBase.setCurrentPlaces(this);
		MASSBase.setDestinationPlaces(MASSBase.getPlacesMap().get( new Integer( destinationHandle ) ));
		MASSBase.setCurrentFunctionId(functionId);
		//MASS_base.currentDestinations = destinations;
		
		// reset requestCounter by the main thread
		MASSBase.resetRequestCounter();
		
		// for debug
		MASSBase.showHosts( );
		
		// resume threads
		MThread.resumeThreads( MThread.STATUS_TYPE.STATUS_EXCHANGEALL );
		
		// exchangeall implementation
		super.exchangeAll( MASSBase.getDestinationPlaces(),
				   functionId, 0 );
		
		// confirm all threads are done with exchangeAll.
		MThread.barrierThreads( 0 );
		
		// Synchronized with all slave processes
		MASS.barrierAllSlaves( );
    
    }

	/**
	 * ExchangeAll method for compatibility with older versions of MASS.
	 * Sets provided neighbors Vector to each place object.
	 *
	 * @see Places#exchangeAll(int, int)
	 * @param destinationHandle
	 * @param functionId The ID of the function to call
	 * @param neighbors
	 */
	@Deprecated
	public void exchangeAll(int destinationHandle, int functionId, Vector<int[]> neighbors){
		//Add our neighbors to each place
		this.setAllPlacesNeighbors(neighbors);
		//Now call exchangeAll to act on those neighbors
		this.exchangeAll(destinationHandle, functionId);
	}

	/**
	 * Sets each place object with a reference to the neighbors Vector.
	 *
	 * @param neighbors The vector to set
	 */
	public void setAllPlacesNeighbors(Vector<int[]> neighbors) {
		for(int i = 0; i < this.getPlacesSize(); i++)
		{
			this.getPlaces()[i].setNeighbors(neighbors);
		}
	}
    
    public void exchangeBoundary( ) {
	
		// send a PLACES_EXCHANGE_BOUNDARY message to each slave
		Message m = new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_BOUNDARY, 
					 this.getHandle(),  0 ); // 0 is dummy
		
		for ( MNode node : MASS.getRemoteNodes() )
		    node.sendMessage( m );
	
		// retrieve the corresponding places
		MASSBase.setCurrentPlaces(this);
		
		// for debug
		MASSBase.showHosts( );
		
		// exchange boundary implementation
		super.exchangeBoundary( );
		
		// Synchronized with all slave processes
		MASS.barrierAllSlaves( );
    
    }
    
    /**
     * Initializes the places with the given arguments and boundary width.
     * @param argument
     * @param boundaryWidth
     */
    private void init_master( Object argument, int boundaryWidth ) {

		// create a list of all host names;  
		// the master IP name
		Vector<String> hosts = new Vector<String>( );
		
		try {
		    hosts.add( MASS.getMasterNode().getHostName() );
		} catch ( Exception e ) {
		    logger.error( "init_master: InetAddress.getLocalHost( ) ", e );
		    System.exit( -1 );
		}
		
		// all the slave IP names
		for ( MNode node : MASS.getRemoteNodes() ) {
		    hosts.add( node.getHostName( ) );
		}
	
		// create a new list for message
		Message m = new Message( Message.ACTION_TYPE.PLACES_INITIALIZE, getSize(),
					 getHandle(), getClassName(),
					 argument, boundaryWidth, hosts );
		
		// send a PLACES_INITIALIZE message to each slave
		for ( MNode node : MASS.getRemoteNodes() ) {
		    
			node.sendMessage( m );
		    
			logger.debug( "PLACES_INITIALIZE sent to {}", node.getPid() );
		
		}
		
		// establish all inter-node connections within setHosts( )
		MASSBase.setHosts( hosts );
	
		// register this places in the places hash map
		MASSBase.getPlacesMap().put( new Integer( getHandle() ), this );
		
		// Synchronized with all slave processes
		MASS.barrierAllSlaves( );

    }

}