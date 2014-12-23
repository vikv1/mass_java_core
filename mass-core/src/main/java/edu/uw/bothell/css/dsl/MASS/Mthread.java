package edu.uw.bothell.css.dsl.MASS;


public class Mthread extends Thread {
    private static final boolean printOutput = false;
    //private static final boolean printOutput = true;

    public enum STATUS_TYPE { STATUS_READY,          // 0
  	                      STATUS_TERMINATE,      // 1
	                      STATUS_CALLALL,        // 2
	                      STATUS_EXCHANGEALL,    // 3
	                      STATUS_AGENTSCALLALL,  // 4
	                      STATUS_MANAGEALL       // 5
	    }

    public static Object lock;
    public static int barrier_count;
    public static STATUS_TYPE status;

    public static int threadCreated;
    public static int agentBagSize;

    private static int barrier_phases;
    private int tid;                  // this mthread's id


    public static void init( ) {
	lock = new Object( );
	status = STATUS_TYPE.STATUS_READY;
	barrier_count = 0;
	barrier_phases = 0;
    }

    public Mthread( int id ) {
	this.tid = id;
    }

    public void run( ) {
	synchronized( lock ) {
	    threadCreated = tid;  // to inform MASS_base of my invocation
	}
	
	// breath message
	if ( printOutput )
	    MASS_base.log( "Mthread[" + tid + "] invoked" );

	// the following variables are used to call callAll( )
	Places_base places = null;
	Places_base destinationPlaces = null;
	Agents_base agents = null;

	int functionId = 0;
	Object argument = null;
	Message.ACTION_TYPE msgType = Message.ACTION_TYPE.EMPTY;
	//Vector<int[]> destinations = null;

	boolean running = true;
	while ( running ) {
	    // wait for a new command
	    synchronized( lock ) {
		if ( status == STATUS_TYPE.STATUS_READY )
		    try {
			lock.wait( );
		    } catch ( Exception e ) { }

		// wake-up message
		if( printOutput )
		    MASS_base.log( "Mthread[" + tid + "] woken up" );
	    }

	    // perform each task
	    switch( status ) {
	    case STATUS_READY:
		if ( printOutput )
		    MASS_base.log( "Mthread reached STATUS_READY in switch" );
		System.exit( -1 );
		break;
	    case STATUS_TERMINATE:
		running = false;
		break;
	    case STATUS_CALLALL:
		places = MASS_base.getCurrentPlaces( );
		functionId = MASS_base.getCurrentFunctionId( );
		argument = MASS_base.getCurrentArgument( );
		msgType = MASS_base.getCurrentMsgType( );

		if ( printOutput )
		    MASS_base.log( "Mthread[" +tid + "] works on CALLALL:" +
				   " placese = " + places +
				   " functionId = " + functionId +
				   " argument = " + argument +
				   " msgType = " + msgType );

		if ( msgType == 
		     Message.ACTION_TYPE.PLACES_CALL_ALL_VOID_OBJECT ) {
		    places.callAll( functionId, argument, tid );
		}
		else {
		    places.callAll( functionId, (Object[])argument, 
				    ((Object[])argument).length, tid );
		}
		break;

	    case STATUS_EXCHANGEALL:
		if ( printOutput )
		    MASS_base.log( "Mthread[" + tid + 
				   "] works on EXCHANGEALL" );

		places = MASS_base.getCurrentPlaces( );
		functionId = MASS_base.getCurrentFunctionId( );
		destinationPlaces = MASS_base.getDestinationPlaces( );
		//destinations = MASS_base.getCurrentDestinations( );

//		places.exchangeAll( destinationPlaces, functionId, 
//			    destinations, tid );
		places.exchangeAll( destinationPlaces, functionId, tid );
		break;

	    case STATUS_AGENTSCALLALL:
		agents = MASS_base.getCurrentAgents( );
		functionId = MASS_base.getCurrentFunctionId( );
		argument = MASS_base.getCurrentArgument( );
		msgType = MASS_base.getCurrentMsgType( );
		
		if ( printOutput )
		    MASS_base.log( "Mthread[" + tid + 
				   "] works on AGENST_CALLALL:" +
				   " agents = " + agents +
				   " functionId = " + functionId +
				   " argument = " + argument +
				   " msgType = " + msgType );

		if( msgType==Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT) {
		    //System.err.println( "Mthread[" + tid + 
		    //			"] call all agent void object" );

		    agents.callAll( functionId, argument, tid );
		}else{
		    //System.err.println( "Mthread[" + tid + 
		    //			"] call all agents return object" );

		    agents.callAll( functionId, (Object[])argument, 
				    ( (Object[])argument ).length, tid) ;
		}
		break;

	    case STATUS_MANAGEALL:
		//Get agents to be called with Manageall
		agents = MASS_base.getCurrentAgents( );
		
		//Send logging message
		if ( printOutput )
		    MASS_base.log( "Mthread[" + tid + "] works on MANAGEALL:" +
				   " agents = " + agents );
		
		//Sent message for manageall
		agents.manageAll( tid );
		
		break;
	    }

	    // barrier
	    barrierThreads( tid );

	}
	
	// last message
	if ( printOutput )
	    MASS_base.log( "Mthread[" + tid + "] terminated" );
    }

    public static void resumeThreads( STATUS_TYPE new_status ) {
	synchronized( lock ) {
	    status = new_status;
	    lock.notifyAll( );
	}
    }

    public static void barrierThreads( int tid ) {
	synchronized( lock ) {
	    if ( ++barrier_count < MASS_base.threads.length ) {
		if( printOutput )
		    MASS_base.log( "tid[" + tid + 
				   "] waiting: barrier = " + barrier_phases );
		try {
		    lock.wait( );
		} catch( Exception e ) { }
	    } else {
		barrier_count = 0;
		status = STATUS_TYPE.STATUS_READY;
		if( printOutput ) 
		    MASS_base.log( "tid[" + tid + "] woke up all: barrier = " 
				   + barrier_phases );
		barrier_phases++;
		lock.notifyAll( );
	    }
	}
    }
}