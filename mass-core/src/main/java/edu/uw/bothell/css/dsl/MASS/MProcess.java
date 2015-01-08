package edu.uw.bothell.css.dsl.MASS;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

public class MProcess {

	//  private static final boolean printOutput = false;
	//  private static final boolean printOutput = true;
    @SuppressWarnings("unused")
	private String hostName;         // my local host name
    private int myPid;               // my pid or rank
    @SuppressWarnings("unused")
	private int nProc;               // # processes
    @SuppressWarnings("unused")
	private int nThr;                // # threads

    @SuppressWarnings("unused")
	private Vector<String> hosts;    // all hosts participated in computation 
    private ObjectInputStream MAIN_IOS;  // input from the master process

    private ObjectOutputStream MAIN_OOS; // output to the master process

    public static void main( String[] args ) throws Exception {
    
    	String hostName = args[0];
        int myPid = Integer.parseInt(args[1]);
        int nProc = Integer.parseInt(args[2]);
        int nThreads = Integer.parseInt(args[3]);
        int serverPort = Integer.parseInt(args[4]);
        String curDir = args[5];

        MProcess mprocess = new MProcess( hostName, myPid, nProc, nThreads, 
					  serverPort, curDir );
        mprocess.start( );
    
    }

    public MProcess( String hostName, int myPid, int nProc, int nThr, int port, String curDir ) {

    	this.hostName = hostName;
    	this.myPid = myPid;
    	this.nProc = nProc;
    	this.nThr = nThr;
    	MASS_base.initMASS_base( hostName, myPid, nProc, port );
    	MASS_base.setWorkingDirectory(curDir); // mprocess manually changes it.

    	// Create a logger
    	//	try {
    	//	    File massLogDir = new File( curDir + "/" + MASS_base.MASS_LOGS );
    	//	    if( !massLogDir.exists( ) )
    	//		massLogDir.mkdir( );
    	//	} catch( Exception e ) {
    	//	    System.exit( -1 );
    	//	}

    	MASS_base.initializeThreads( nThr );

    	// set up a connection with the master process
    	try {
    		MAIN_IOS = new ObjectInputStream( System.in );
    		MAIN_OOS = new ObjectOutputStream( System.out );
    	}
    	catch ( Exception e ) {
    		MASS_base.log( "MProcess.Mprocess: detected " + e );
    		System.exit( -1 );
    	}

    }

    Message receiveMessage( ) {

    	try {
    		return ( Message )MAIN_IOS.readObject( );
    	} 
    	catch ( Exception e ) {
    		MASS_base.log( "MProcess.receiveMessage: detected " + e );
    		System.exit( -1 );
    	}
    	
    	return null;
    
    }

    private void sendAck( ) {
		Message msg = new Message( Message.ACTION_TYPE.ACK );
		sendMessage( msg );
    }

    private void sendAck( int localPopulation ) {
    
    	Message msg = new Message( Message.ACTION_TYPE.ACK, localPopulation );
    	//	if( printOutput ) {
    	//	    MASS_base.log( "msg.getAgentPopulation = " + 
    	//			   msg.getAgentPopulation( ) );
    	//	}
    	sendMessage( msg );

    }

    private void sendMessage( Message msg ) {

    	try {

    		MAIN_OOS.writeObject( msg );
    		MAIN_OOS.flush( );
    	
    	} 
    	catch ( Exception e ) {
    		
    		MASS_base.log( "MProcess.sendMessage: " + e );
    		System.exit( -1 );
    	
    	}
    
    }

    private void sendReturnValues( Object argument ) {
    	Message msg = new Message( Message.ACTION_TYPE.ACK, argument );
    	sendMessage( msg );
    }

    private void sendReturnValues( Object argument, int localPopulation ) {
    	Message msg = new Message( Message.ACTION_TYPE.ACK, argument,
    			localPopulation );
    	sendMessage( msg );
    }

    @SuppressWarnings("incomplete-switch")
    public void start( ) {
    	
    	MASS_base.log( "MProcess started" );

    	// Synchronize with the master node first.
    	sendAck( );

    	boolean alive = true;
    	while( alive ) {
    		
    		// receive a new message from the master
    		Message m = receiveMessage( );

    		//	    if ( printOutput )
    		//		MASS_base.log( "A new message received: action = " +
    		//			       m.getAction( ) );

    		// get prepared for the following arguments for PLACES_INITIALIZE
    		int[] size;                 // size[]
    		Vector<String> hosts = new Vector<String>( );
    		Object argument = null;
    		Places_base places = null;  // new Places
    		Agents_base agents = null;  // new Agents

    		// retrieve an argument
    		argument = m.getArgument( );

    		switch( m.getAction( ) ) {
    		
    		case ACK:
    			sendAck( );
    			break;

    		case EMPTY:
    			//		if( printOutput )
    			//		    MASS_base.log( "EMPTY received!!!!" );
    			sendAck( );
    			break;

    		case FINISH:
    			Mthread.resumeThreads( Mthread.STATUS_TYPE.STATUS_TERMINATE );
    			// confirm all threads are done with finish
    			Mthread.barrierThreads( 0 );
    			MASS_base.getExchange().terminateConnection( this.myPid );
    			sendAck( );
    			alive = false;
    			//		if( printOutput )
    			//		    MASS_base.log( "FINISH received and ACK sent" );
    			break;

    		case PLACES_INITIALIZE:
    			//		if ( printOutput )
    			//		    MASS_base.log( "PLACES_INITIALIZE received" );
    			// create a new Places
    			size = m.getSize( );

    			places = new Places_base( m.getHandle( ), m.getClassname( ),
    					m.getBoundaryWidth( ),
    					argument, size );

    			for ( int i = 0; i < m.getHosts( ).size( ); i++ )
    				hosts.add( m.getHosts( ).get(i) );
    			// establish all inter-node connections within setHosts( )
    			MASS_base.setHosts( hosts );

    			MASS_base.getPlacesMap().put( new Integer( m.getHandle( ) ), places );
    			sendAck( );
    			//		if ( printOutput )
    			//		    MASS_base.log( "PLACES_INITIALIZE completed and ACK sent");
    			break;

    		case PLACES_CALL_ALL_VOID_OBJECT:
    			//		if ( printOutput )
    			//		    MASS_base.log( "PLACES_CALL_ALL_VOID_OBJECT received" );

    			// retrieve the corresponding places
    			MASS_base.setCurrentPlaces(MASS_base.getPlacesMap().get( new Integer( m.getHandle( ) ) ));
    			MASS_base.setCurrentFunctionId(m.getFunctionId( ));
    			MASS_base.setCurrentArgument(argument);
    			MASS_base.setCurrentMsgType(m.getAction( ));

    			// resume threads to work on call all.
    			Mthread.resumeThreads( Mthread.STATUS_TYPE.STATUS_CALLALL );

    			// 3rd arg: 0 = the main thread id
    			MASS_base.getCurrentPlaces().callAll( m.getFunctionId( ), 
    					argument, 0 );

    			// confirm all threads are done with places.callAll
    			Mthread.barrierThreads( 0 );

    			sendAck( );
    			break;

    		case PLACES_CALL_ALL_RETURN_OBJECT:
    			//		if( printOutput )
    			//		    MASS_base.log( "PLACES_CALL_ALL_RETURN_OBJECT received" );

    			// retrieve the corresponding places
    			MASS_base.setCurrentPlaces(MASS_base.getPlacesMap().get( new Integer( m.getHandle( ) ) ));
    			MASS_base.setCurrentFunctionId(m.getFunctionId( ));
    			MASS_base.setCurrentArgument(argument);
    			MASS_base.setCurrentMsgType(m.getAction( ));
    			MASS_base.setCurrentReturns(new Object[MASS_base.getCurrentPlaces().getPlacesSize()]);

    			// resume threads to work on call all.
    			Mthread.resumeThreads( Mthread.STATUS_TYPE.STATUS_CALLALL );

    			// 3rd arg: 0 = the main thread id
    			MASS_base.getCurrentPlaces().callAll( MASS_base.getCurrentFunctionId(),
    					( Object[] )
    					( MASS_base.getCurrentArgument() ),
    					( (Object[])
    							(MASS_base.getCurrentArgument())
    							).length,
    							0 );

    			// confirm all threads are done with places.callAll w/ return
    			Mthread.barrierThreads( 0 );

    			//		if ( printOutput )
    			//		    MASS_base.log( "PLACES_CALL_ALL_RETURN_OBJECT " +
    			//				   "checking currentReturns" );

    			sendReturnValues( MASS_base.getCurrentReturns() );
    			break;

    		case PLACES_EXCHANGE_ALL:
    			//		if ( printOutput )
    			//		    MASS_base.log( "PLACES_EXCHANGE_ALL recweived handle = " +
    			//				   m.getHandle( ) + " dest_handle = " +
    			//				   m.getDestHandle( ) );

    			// retrieve the corresponding places
    			MASS_base.setCurrentPlaces(MASS_base.getPlacesMap().get( new Integer( m.getHandle( ) ) ) );
    			MASS_base.setDestinationPlaces(MASS_base.getPlacesMap().get(new Integer( m.getDestHandle() ) ) );
    			MASS_base.setCurrentFunctionId(m.getFunctionId( ));
    			//		MASS_base.currentDestinations = m.getDestinations( );

    			// reset requestCounter by the main thread
    			MASS_base.resetRequestCounter();

    			// for debug
    			MASS_base.showHosts( );

    			// resume threads to work on call all.
    			Mthread.resumeThreads(Mthread.STATUS_TYPE.STATUS_EXCHANGEALL);

    			// exchangeall implementation
    			MASS_base.
    			getCurrentPlaces().exchangeAll( MASS_base.getDestinationPlaces(),
    					MASS_base.getCurrentFunctionId(), 0 );

    			// confirm all threads are done with places.exchangeall.
    			Mthread.barrierThreads( 0 );

    			//		if ( printOutput )
    			//		    MASS_base.log( "barrier done" );

    			sendAck( );
    			//		if ( printOutput )
    			//		    MASS_base.log( "PLACES_EXCHANGE_ALL sent ACK" );
    			break;

    		case PLACES_EXCHANGE_BOUNDARY:
    			//		if ( printOutput ) 
    			//		    MASS_base.log( "PLACES_EXCHANGE_BOUNDARY received handle="
    			//				   + m.getHandle( ) );

    			// retrieve the corresponding places
    			MASS_base.setCurrentPlaces(MASS_base.getPlacesMap().get( new Integer( m.getHandle( ) ) ));

    			// for debug
    			MASS_base.showHosts( );

    			// exchange boundary implementation
    			MASS_base.getCurrentPlaces().exchangeBoundary( );

    			sendAck( );
    			//		if ( printOutput )
    			//		    MASS_base.log( "PLACES_EXCHANGE_BOUNDARY " +
    			//				   "completed and ACK sent" );
    			break;

    		case PLACES_EXCHANGE_ALL_REMOTE_REQUEST:
    		case PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT:
    		case PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST:
    			break;

    		case AGENTS_INITIALIZE:
    			//		if ( printOutput )
    			//		    MASS_base.log( "AGENTS_INITIALIZE received" );

    			agents = new Agents_base( m.getHandle( ), m.getClassname( ),
    					argument,
    					m.getDestHandle( ),
    					m.getAgentPopulation( ) );

    			MASS_base.getAgentsMap().put( new Integer( m.getHandle() ), 
    					agents );

    			sendAck( agents.getLocalPopulation() );
    			//		if ( printOutput )
    			//		    MASS_base.log("AGENTS_INITIALIZE completed and ACK sent" );
    			break;

    		case AGENTS_CALL_ALL_VOID_OBJECT:
    			//		if ( printOutput )
    			//		    MASS_base.log( "AGENTS_CALL_ALL_VOID_OBJECT received" );
    			MASS_base.setCurrentAgents(MASS_base.getAgentsMap().get( new Integer( m.getHandle() ) ));
    			MASS_base.setCurrentFunctionId(m.getFunctionId( ));
    			MASS_base.setCurrentArgument(argument);
    			MASS_base.setCurrentMsgType(m.getAction( ));

    			Mthread.agentBagSize = MASS_base.getCurrentAgents().getAgents().size_unreduced( );

    			// resume threads to work on call all
    			Mthread.resumeThreads( Mthread.STATUS_TYPE. STATUS_AGENTSCALLALL );

    			MASS_base.getCurrentAgents().callAll( m.getFunctionId(), argument, 0);

    			// confirm all threads are done with agents.callAll
    			Mthread.barrierThreads( 0 );
    			//		if ( printOutput )
    			//		    MASS_base.log( "barrier done" );

    			sendAck( MASS_base.getCurrentAgents().getLocalPopulation() );
    			break;

    		case AGENTS_CALL_ALL_RETURN_OBJECT:
    			//		if ( printOutput )
    			//		    MASS_base.log( "AGENTS_CALL_ALL_RETURN_OBJECT received" );
    			MASS_base.setCurrentAgents(MASS_base.getAgentsMap().get( new Integer( m.getHandle() ) ));
    			MASS_base.setCurrentFunctionId(m.getFunctionId( ));
    			MASS_base.setCurrentArgument(argument);
    			MASS_base.setCurrentMsgType(m.getAction( ));
    			MASS_base.setCurrentReturns(new Object[MASS_base.getCurrentAgents().getLocalPopulation()]);

    			Mthread.agentBagSize 
    			= MASS_base.getCurrentAgents().getAgents().size_unreduced( );

    			// resume threads to work on call all with return objects
    			Mthread.resumeThreads( Mthread.STATUS_TYPE.
    					STATUS_AGENTSCALLALL );

    			MASS_base.getCurrentAgents().callAll( MASS_base.getCurrentFunctionId(),
    					(Object[])(MASS_base.
    							getCurrentArgument()),
    							((Object[])(MASS_base.
    									getCurrentArgument())).
    									length,
    									0 );

    			// confirm all threads are done with agnets.callAll with 
    			// return objects  
    			Mthread.barrierThreads( 0 );
    			//		if ( printOutput )
    			//		    MASS_base.log( "barrier done" );

    			sendReturnValues( MASS_base.getCurrentReturns(),
    					MASS_base.getCurrentAgents().getLocalPopulation() );

    			break;

    		case AGENTS_MANAGE_ALL:
    			//		if ( printOutput )
    			//		    MASS_base.log( "AGENTS_MANAGE_ALL received" );
    			MASS_base.setCurrentAgents(MASS_base.getAgentsMap().get( new Integer( m.getHandle() ) ));
    			Mthread.agentBagSize 
    			= MASS_base.getCurrentAgents().getAgents().size_unreduced( );

    			Mthread.resumeThreads( Mthread.STATUS_TYPE.STATUS_MANAGEALL );

    			MASS_base.getCurrentAgents().manageAll( 0 ); // 0 = the main tid

    			// confirm all threads are done with agents.manageAll.
    			Mthread.barrierThreads( 0 );
    			//		if ( printOutput )
    			//		    MASS_base.log( "sendAck will send localPopulation = " + 
    			//				   MASS_base.currentAgents.localPopulation );

    			sendAck( MASS_base.getCurrentAgents().getLocalPopulation() );

    			break;

    		case AGENTS_MIGRATION_REMOTE_REQUEST:
    			break;

    		}
    	
    	}
    
    }

}