package edu.uw.bothell.css.dsl.MASS;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;

public class Agents_base implements Serializable {
    //Used to toggle comments from Places_base.java
    //private static final boolean printOutput = false;
    private static final boolean printOutput = true;

    public static final int MAX_AGENTS_PER_NODE = 100000000; // 100 million 

    public Agents_base( int handle, String className, Object argument,
			int placesHandle, int initPopulation ) {
	this.handle = handle;
	this.className = className;
	this.placesHandle = placesHandle;
	this.initPopulation = initPopulation;
	this.agents = new AgentList( );

	// For debugging
	if ( printOutput == true ) 
	    MASS_base.log( "handle = " + handle
			   + ",placesHandle = " + placesHandle
			   + ", class = " + className
			   + ", argument = " + (String)argument
			   + ", initPopulation = " + initPopulation );
	
	// load the construtor and destructor
	try {
	    File curDir = new File( MASS.CUR_DIR );
	    agentLoader =
		URLClassLoader.
		newInstance( new URL[] { curDir.toURI().toURL( ) } );
	    agentClass =                                           // get class
		Class.forName( className, true, agentLoader );
	    agentConstructor =                               // get constructor
		agentClass.getConstructor ( Object.class );
			     
	} catch ( Exception e ) {
	    MASS_base.log( "Agents_base.constructor: " + className +
			   " not loaded " + e );
	}

	// initialize currentAgentId and localPopulation
	currentAgentId = MASS_base.myPid * MAX_AGENTS_PER_NODE;
	localPopulation = 0;
	
	// instantiate just one agent to call its map( ) function
	agentInitAgentsHandle = handle;
	agentInitPlacesHandle = placesHandle;
	agentInitAgentId = -1; // proto
	agentInitParentId = -1; // no parent
	Agent protoAgent = null;
	try {
	    protoAgent = ( Agent )agentConstructor.newInstance( argument );
	} catch ( Exception e ) {
	    MASS_base.log( "Agents_base.constructor: " + className +
			   " not instantiated " + e );
	}
	
	// retrieve the corresponding places
	Places_base curPlaces = 
	    MASS_base.placesMap.get( new Integer( placesHandle ) );
	
	if ( printOutput == true )
	    MASS_base.log( "Agets_base constructor: placesDillClass = "
			   + " curPlaces = " + (Object)curPlaces );
	
	for ( int i = 0; i < curPlaces.getPlacesSize( ); i++ ) {
	    
	    // scan each place to see how many agents it can create
	    Place curPlace = curPlaces.places[i];
	    
	    if ( printOutput == true )
		MASS_base.log( "Agent_base constructor place[" + i + "]" );
	    // create as many new agents as nColonists
	    for ( int nColonists =
		      protoAgent.map( initPopulation, curPlace.size, 
				      curPlace.index );
		  nColonists > 0; nColonists--, localPopulation++ ) {
		
		// agent instanstantiation and initialization
		Agent newAgent = null;
		try {
		    agentInitAgentsHandle = handle;
		    agentInitPlacesHandle = placesHandle;
		    agentInitAgentId = currentAgentId++;
		    agentInitParentId = -1; // no parent
		    newAgent = (Agent)agentConstructor.newInstance( argument );
		} catch ( Exception e ) {
		    MASS_base.log( "Agents_base.constructor: " + className +
				   " not instaitated " + e );
		}
		
		if ( printOutput == true )
		    MASS_base.log( " newAgent[" + localPopulation + "] = " + 
				   (Object)newAgent );

		newAgent.place = curPlace;
		newAgent.index = curPlace.index;
		
		// store this agent in the bag of agents
		agents.add( newAgent );
		
		// TODO: register newAgent into curPlace
		curPlace.agents.add( newAgent );
	    }
	}
    }

    public void callAll( int functionId, Object argument, int tid ) {

	int numOfOriginalVectors = Mthread.agentBagSize;

	while ( true ) {
	    //Create the index for this iteration
	    int myIndex;
	    
	    //Lock the index assignment so no two threads will receive 
	    // the same value
	    synchronized( this ) {
		
		//Thread checking
		if ( printOutput == true ) 
		    MASS_base.log( "Starting index value is: " + 
				   Mthread.agentBagSize );
		myIndex = Mthread.agentBagSize--;
		
		//Error Checking
		if ( printOutput == true )
		    MASS_base.log( "Thread[" + tid + "]: agent(" + myIndex +
				   ") assigned" );
	    }
	    
	    // Continue to run until the assigning index becomes negative
	    // (in which case, we've run out of agents)
	    if ( myIndex > 0 ) {
		
		Agent tmpAgent = agents.get( myIndex - 1 );
		if ( printOutput == true ) {
		    MASS_base.log( "Thread [" + tid + "]: agent(" +
				   tmpAgent + ")[" + myIndex +
				   "] was removed " );
		    MASS_base.log( "fId = " + functionId + " argument " +
				   argument ); 
		}
		
		//Use the Agents' callMethod to have it begin running
		tmpAgent.callMethod( functionId, argument ); 
		
		if ( printOutput == true )
		    MASS_base.log( "Thread [" + tid + "]: (" + myIndex +
				   ") has called its method; " +
				   "Current Agent Bag Size is: " + 
				   Mthread.agentBagSize );
	    }
	    //Otherwise, we are out of agents and should stop
	    //trying to assign any more
	    else {
		break;
	    }
	}
	//Wait for the thread count to become zero
	Mthread.barrierThreads( tid );
	
	//Assign the new bag of finished agents to the old pointer for reuse
	if ( tid == 0 ) {
	    Mthread.agentBagSize = numOfOriginalVectors;
	    
	    if ( printOutput == true )
		MASS_base.log( "Agents_base:callAll: agents.size = " +
			       MASS_base.agentsMap.get( new Integer(handle) ).
			       agents.size_unreduced( ) + "\n" +
			       "Agents_base:callAll: agentsBagSize = " +
			       Mthread.agentBagSize );
	}
    }
    
    public void callAll( int functionId, Object[] argument, int length,
			 int tid ) {

	int numOfOriginalVectors = Mthread.agentBagSize;
	
	while ( true ) {
	    // create the index for this iteration
	    int myIndex;
	    
	    // Lock the index assginment so no tow threads will receive 
	    // the same index
	    synchronized( this ) {
	    
		// Thread checking
		if ( printOutput == true )
		    MASS_base.log( "Starting index value is: " +
				   Mthread.agentBagSize );
		myIndex = Mthread.agentBagSize--; // myIndex == agentId + 1
		
		//Error Checking
		if ( printOutput == true )
		    MASS_base.log( "Thread[" + tid + "]: agent(" + myIndex + 
				   ") assigned" );
	    }
	    
	    // While there are still indexes left, continue to grab and 
	    // execute threads.
	    if ( myIndex > 0 ) {
		// compute where to store this agent's return value
		// note that myIndex = agentId + 1
		
		Agent tmpAgent = agents.get( myIndex - 1 );
		if ( printOutput == true )
		    MASS_base.log( "Thread[" + tid + "]: agent("+ myIndex + 
				   "): MASS_base::currentReturns  = " + 
				   MASS_base.currentReturns );
		
		//Use the Agents' callMethod to have it begin running
		( (Object[])MASS_base.currentReturns )[myIndex - 1] =
		    tmpAgent.callMethod( functionId, 
					 argument[ myIndex - 1 ] );
		
		if ( printOutput == true )
		    MASS_base.log( "Thread [" + tid + "]: (" + myIndex +
				   ") has called its method; " );
	    }
	    //Otherwise, we are out of agents and should stop
	    //trying to assign any more
	    else{
		break;
	    }
	}
	//Confirm all threads have finished
	Mthread.barrierThreads( tid );
	
	//Assign the new bag of finished agents to the old pointer for reuse
	if ( tid == 0 ) {
	    Mthread.agentBagSize = numOfOriginalVectors;
	    
	    if ( printOutput == true ) 
		MASS_base.log( "Agents_base:callAll: agents.size = " + 
			       MASS_base.agentsMap.get( new Integer(handle) ).
			       agents.size_unreduced( ) + "\n" +
			       "Agents_base:callAll: agentsBagSize = " + 
			       Mthread.agentBagSize );
	}
    }
    
    public void manageAll( int tid ) {

	int numOfOriginalVectors = Mthread.agentBagSize;

	Places_base evaluatedPlaces = 
	    MASS_base.placesMap.get( new Integer( placesHandle ) );
	
	// Spawn, Kill, Migrate. Check in that order throughout the bag of 
	// agents  sequentially.
	while ( true ) {
	    int myIndex;
	    
	    synchronized( this ) {

		if ( Mthread.agentBagSize == 0 )
		    break;
		myIndex = Mthread.agentBagSize--;
	    }
	    // Grab the last agent and remove it for processing. 
	    // Be sure to lock on removal
	    Agent evaluationAgent = agents.get( myIndex - 1 );
	    
	    if ( printOutput == true ) 
		MASS_base.log( "Agents_base.manageALL: Thread " + tid + 
			       " picked up " 
			       + evaluationAgent.agentId );
	    
	    int argumentcounter = 0;
	    
	    // If the spawn's newChildren field is set to anything higher than 
	    // zero, we need to create newChildren's worth of Agents in the 
	    // current location.

	    // Spawn() Check
	    int childrenCounter = evaluationAgent.newChildren;
	    
	    if ( printOutput == true ) 
		MASS_base.log( "agent " + evaluationAgent.agentId +
			       "'s childrenCounter = " + childrenCounter );
	    
	    while ( childrenCounter > 0 ) {
		if ( printOutput == true )
		    MASS_base.log( "Agent_base.manageALL: Thread " + tid +
				   " will spawn a child of agent " + 
				   evaluationAgent.agentId +
				   "...arguments.length = " +
				   evaluationAgent.arguments.length +
				   ", argumentcounter = " + argumentcounter );
		
		Agent addAgent = null;
		Object dummyArgument = new Object( );
		try {
		    agentInitAgentsHandle = handle;
                    agentInitPlacesHandle = placesHandle;
                    agentInitAgentId = currentAgentId++;
                    agentInitParentId = evaluationAgent.agentId;

		    addAgent = 
		    // validate the correspondance of arguments and 
		    // argumentcounter
		    ( evaluationAgent.arguments.length > 
		      argumentcounter ) ?
		    // yes: this child agent should recieve an argument.
		    ( Agent )
			( agentConstructor.
			  newInstance( evaluationAgent.
				       arguments[argumentcounter++] ) )
		    :
		    // no:  this child agent should not receive an argument.
		    ( Agent )
			( agentConstructor.newInstance( dummyArgument ) );

		    // initialize this child agent's attributes: 
		    addAgent.index = evaluationAgent.index;
		    addAgent.place = evaluationAgent.place;
		} catch ( Exception e ) {
		    MASS_base.log( "Agents_base.manageAll: " + className +
				   " not instantiated " + e );
		}
		
		// Push the created agent into our bag for returns and update 
		// the counter
		// needed to keep track of our agents.
		
		// Lock will be done within agents.add( )
		agents.add( addAgent );
		
		// Decrement the newChildren counter once an Agent has been 
		// spawned
		evaluationAgent.newChildren--;
		childrenCounter--;
		
		if ( printOutput == true )
		    MASS_base.log( "Agent_base.manageALL: Thread " + tid +
				   " spawned a child of agent " + 
				   evaluationAgent.agentId +
				   " and put the child " + addAgent.agentId +
				   " child into retBag." );
	    }
	    
	    // Kill() Check
	    if ( printOutput == true )
		MASS_base.log( "Agent_base.manageALL: Thread " + tid +
			       " check " + evaluationAgent.agentId + 
			       "'s alive = " + evaluationAgent.alive );

	    if ( evaluationAgent.alive == false ) {
		
		//Get the place in which evaluationAgent is 'stored' in
		Place evaluationPlace = evaluationAgent.place;
		
		// Move through the list of Agents to locate which to delete
		// Do so non-interruptively.
		synchronized( evaluationPlace.agents ) {
		    int evalPlaceAgents = evaluationPlace.agents.size( );
		    for ( int i = 0; i < evalPlaceAgents; i++ ) {
			Agent comparisonAgent = evaluationPlace.agents.get(i);

			// Check the Id against the ID of the agent to be 
			// removed. 
			// If it matches, remove it Lock
			if ( ( evaluationAgent.agentId == 
			       comparisonAgent.agentId ) 
			     && 
			     ( evaluationAgent.agentsHandle == 
			       comparisonAgent.agentsHandle ) ) {

			    evaluationPlace.agents.remove( i );

			    if ( printOutput == true ) 
				MASS_base.log( "Agent_base.manageALL: Thread "
					       + tid + " deleted " + 
					       evaluationAgent.agentId  + 
					       " from place[" + 
					       evaluationPlace.index[0] +
					       "][" + 
					       evaluationPlace.index[1] + 
					       "]" );
			    break;			    
			}
		    }
		}

		// Delete the agent
		agents.remove( myIndex - 1 ); // automatic synchronization
		continue;
	    }
	    
	    //Migrate() check
	    
	    //Iterate over all dimensions of the agent to check its location
	    //against that of its place. If they are the same, return back.
	    int agentIndex = evaluationAgent.index.length;
	    int[] destCoord = new int[agentIndex];
	    
	    // compute its coordinate
	    getGlobalAgentArrayIndex( evaluationAgent.index, 
				      evaluatedPlaces.size, destCoord );
	    
	    if ( printOutput == true )
		MASS_base.log( "pthread_self[" + Thread.currentThread( ) +
			       "tid[" + tid + "]: calls from" +
			       "[" + evaluationAgent.index[0] +
			       "][" + evaluationAgent.index[1] + "]" +
			       " (destCoord[" + destCoord[0] +
			       "][" + destCoord[1] + "]"+
			       " evaluatedPlaces.size[" + 
			       evaluatedPlaces.size[0] +
			       "][" + evaluatedPlaces.size[1] + "]" );
	    
	    if( destCoord[0] != -1 ) { 
		// destination valid
		if ( printOutput == true )
		    MASS_base.log( "getGlobalLinearIndexFromGlobalArrayIndex:"
				   + " evaluatedPlace = " + evaluatedPlaces
				   + " destCoard = " + destCoord );

		int globalLinearIndex = 
		    evaluatedPlaces.
		    getGlobalLinearIndexFromGlobalArrayIndex( destCoord,
							      evaluatedPlaces.
							      size );

		if ( printOutput == true )
		    MASS_base.log( " linear = " + globalLinearIndex +
				   " lower = " + 
				   evaluatedPlaces.lower_boundary +
				   " upper = " + 
				   evaluatedPlaces.upper_boundary + ")" );

		// Remove this agent locally.
		// Vector.remove( ) is synchronized.
		Place oldPlace = evaluationAgent.place;
		if ( oldPlace.agents.remove( evaluationAgent ) == false ) {
		    if ( printOutput == true ) 
			MASS_base.log( "evaluationAgent " + 
				       evaluationAgent.agentId +
				       " couldn't been found in the "+
				       "old place!" );
		    System.exit( -1 );
		}
		if ( printOutput == true )
		    MASS_base.log( "evaluationAgent " + 
				   evaluationAgent.agentId +
				   " was removed from the oldPlace[" +
				   oldPlace.index[0] + "][" +
				   oldPlace.index[1] + "]" );
		
		if ( globalLinearIndex >= evaluatedPlaces.lower_boundary &&
		     globalLinearIndex <= evaluatedPlaces.upper_boundary ) {
		    // local destination
		    
		    // insert the migration Agent to a local destination place
		    int destinationLocalLinearIndex 
			= globalLinearIndex - evaluatedPlaces.lower_boundary;
		    
		    if ( printOutput == true )
			MASS_base.log( "destinationLocalLinerIndex = " 
				       + destinationLocalLinearIndex );
		    
		    evaluationAgent.place =
			MASS_base.placesMap.get( new Integer( placesHandle ) ).
			places[destinationLocalLinearIndex];
		    
		    if ( printOutput == true )
			MASS_base.log( "evaluationAgent.place = " 
				       + evaluationAgent.place );

		    evaluationAgent.place.agents.add( evaluationAgent );
		    
		    if ( printOutput == true ) 
			MASS_base.log( "evaluationAgent " + 
				       evaluationAgent.agentId +
				       " was inserted into the destPlace[" +
				       evaluationAgent.place.index[0] + "][" +
				       evaluationAgent.place.index[1] + "]" );
		} 
		else {
		    // remote destination
		    
		    // find the destination node
		    int destRank 
			= evaluatedPlaces.
			getRankFromGlobalLinearIndex( globalLinearIndex );
		    
		    // create a request
		    evaluationAgent.place = null; // departing from cur place
		    AgentMigrationRequest request 
			= new AgentMigrationRequest( globalLinearIndex, 
						     evaluationAgent );
		    
		    if ( printOutput == true )
			MASS_base.log( "AgentMigrationRequest request = " + 
				       request );
		    
		    // enqueue the request to this node.map
		    Vector<AgentMigrationRequest> migrationRequests =
			MASS_base.migrationRequests.get( destRank );
		    synchronized( migrationRequests ) {
			migrationRequests.add( request );
			if ( printOutput == true )
			    MASS_base.log( "remoteRequest[" + destRank + 
					   "].push_back:" +
					   " dst = " + globalLinearIndex );
		    }
		}
	    } else {
		if ( printOutput == true )
		    MASS_base.log( " to destination invalid" );
	    }
	} // end of while( true )
	
	//When while loop finishes, all Agents reside in retBag. 
	// Need to hook back up.
	Mthread.barrierThreads( tid );
	
	//Reduce AgentList
	if ( tid == 0 ) {
	    agents.reduce( );
	    
	    if ( printOutput == true ) {
		MASS_base.log( "Agents_base:manageAll: agents.size = " 
			       + agents.size( ) + "\n" );
		MASS_base.log( "Agents_base:manageAll: agentsBagSize = " 
			       + Mthread.agentBagSize );
	    }
	}
	
	// all threads must barrier synchronize here.
	Mthread.barrierThreads( tid );
	if ( tid == 0 ) {
	    
	    if ( printOutput == true )
		MASS_base.log( "tid[" + tid + 
			       "] now enters processAgentMigrationRequest" );
	    
	    // the main thread spawns as many communication threads as the 
	    // number of remote computing nodes and let each invoke 
	    // processAgentMigrationReq.
	    
	    // args to threads: rank, agentHandle, placeHandle, lower_boundary
	    int[][] comThrArgs = new int[MASS_base.systemSize][4];
	    ProcessAgentMigrationRequest[] thread_ref
		= new ProcessAgentMigrationRequest[MASS_base.systemSize];
	    for ( int rank = 0; rank < MASS_base.systemSize; rank++ ) {
		
		if ( rank == MASS_base.myPid ) // don't communicate with myself
		    continue;
		
		// set arguments 
		comThrArgs[rank][0] = rank;
		comThrArgs[rank][1] = handle; // agents' handle
		comThrArgs[rank][2] = evaluatedPlaces.handle;
		comThrArgs[rank][3] = evaluatedPlaces.lower_boundary;
		
		// start a communication thread
		thread_ref[rank] =
		    new ProcessAgentMigrationRequest( comThrArgs[rank] );
		thread_ref[rank].start( );
		
		if ( printOutput == true )
		    MASS_base.log( "Agents_base.manageAll will start " +
				   "processAgentMigrationRequest thread[" +
				   rank + "] = " + thread_ref[rank] );
	    }
	    
	    // wait for all the communication threads to be terminated
	    for ( int rank = MASS_base.systemSize - 1; rank >= 0; rank-- ) {
		
		if ( printOutput == true )
		    MASS_base.log( "Agents_base.manageAll will join " +
				   "processAgentMigrationRequest A thread[" +
				   rank + "] = " + thread_ref[rank] + 
				   " myPid = " + MASS_base.myPid );

		if ( rank == MASS_base.myPid ) // don't communicate with myself
		    continue;      
		
		if ( printOutput == true )
		    MASS_base.log( "Agents_base.manageAll will join " +
				   "processAgentMigrationRequest B thread[" +
				   rank + "] = " + thread_ref[rank] );
		try {
		    thread_ref[rank].join( );
		} catch ( Exception e ) { }
		
		if ( printOutput == true )
		    MASS_base.log( "Agents_base.manageAll joined " +
				   "processAgentMigrationRequest C thread[" +
				   rank + "] = " + thread_ref[rank] );
	    }
	    localPopulation = agents.size_unreduced( );
	    
	    if ( printOutput == true )
		MASS_base.log( "Agents_base.manageAll completed: " + 
			       "localPopulation = " + localPopulation );
	}
	else {
	    if ( printOutput == true )
		MASS_base.log( " tid[" + tid + 
			       "] skips processAgentMigrationRequest" );
	}
    }
    
    private class ProcessAgentMigrationRequest extends Thread {
	private int destRank;
	private int agentHandle;
	private int placeHandle;
	// private in my_lower_boundary;

	public ProcessAgentMigrationRequest( int[] param ) {
	    destRank = param[0];
	    agentHandle = param[1];
	    placeHandle = param[2];
	    // my_lower_boundary = ( (int )param )[3];
	}

	public void run( ) {
	
	    Vector<AgentMigrationRequest> orgRequest = null;
	    
	    if ( printOutput == true )
		MASS_base.log( "pthread_self[" + Thread.currentThread( ) +
			       "] rank[" + destRank + 
			       "]: starts processAgentMigrationRequest" );
	    
	    // pick up the next rank to process
	    orgRequest = MASS_base.migrationRequests.get(destRank);
	    
	    // for debugging
	    synchronized( orgRequest ) {
		if ( printOutput == true ) {
		    MASS_base.log( "tid[" + destRank + 
				   "] sends an exhange request to rank: " + 
				   destRank +
				   " size() = " + orgRequest.size( ) );
		    
		    for ( int i = 0; i < orgRequest.size( ); i++ )
			MASS_base.log( "send " +
				       orgRequest.get(i).agent + " to " +
				       orgRequest.get(i).
				       destGlobalLinearIndex );
		}
	    }
	    
	    // now compose and send a message by a child
	    Message messageToDest = new
		Message( Message.ACTION_TYPE.
			 AGENTS_MIGRATION_REMOTE_REQUEST,
			 agentHandle, placeHandle, orgRequest );
	    
	    if ( printOutput == true ) 
		MASS_base.log( "tid[" + destRank + 
			       "] made messageToDest to rank: " + destRank ); 

            SendMessageByChild thread_ref = new
                SendMessageByChild( destRank, messageToDest );
            thread_ref.start( );

            // receive a message by myself
            Message messageFromSrc =
                MASS_base.exchange.receiveMessage( destRank );

            // at this point, the message must be exchanged.
            try {
                thread_ref.join( );

		if ( printOutput == true )
		    MASS_base.log( "pthread id = " + thread_ref +
				   "pthread_join completed for rank[" +
				   destRank + "]" );
            } catch ( Exception e ) { }
	    
	    // process a message
	    Vector<AgentMigrationRequest> receivedRequest 
		= messageFromSrc.getMigrationReqList( );
	    
	    int agentsHandle = messageFromSrc.getHandle( );
	    int placesHandle = messageFromSrc.getDestHandle( );
	    Places_base dstPlaces 
		= MASS_base.placesMap.get( new Integer( placesHandle ) );
	    
	    if ( printOutput == true ) {
		MASS_base.log( "request from rank[" + destRank + "] = " + 
			       receivedRequest );
		MASS_base.log( " size( ) = " + receivedRequest.size( ) );
	    }
	    
	    // retrieve agents from receiveRequest
	    while( receivedRequest.size( ) > 0 ) {
		AgentMigrationRequest request 
		    = receivedRequest.remove( receivedRequest.size( ) - 1 );
		int globalLinearIndex = request.destGlobalLinearIndex;
		Agent agent = request.agent;
		
		// local destination
		int destinationLocalLinearIndex 
		    = globalLinearIndex - dstPlaces.lower_boundary;
		
		if ( printOutput == true )
		    MASS_base.log( " dstLocal = " + 
				   destinationLocalLinearIndex );
		
		Place dstPlace = dstPlaces.places[destinationLocalLinearIndex];
		
		// push this agent into the place and the entire agent bag.
		agent.place = dstPlace;
		dstPlace.agents.add( agent ); // auto sync.
		agents.add( agent );          // auto sync.
	    }
	    
	    if ( printOutput == true )
		MASS_base.log( "pthread_self[" + Thread.currentThread( ) + 
			       "] retreive agents from rank[" + destRank + 
			       "]complated" );
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
	    if ( printOutput == true )
		MASS_base.log( "pthread_self[" + Thread.currentThread( ) +
			       "] sendMessageByChild to " + rank + " starts" );

            MASS_base.exchange.sendMessage( rank, message );

	    if ( printOutput == true )
		MASS_base.log( "pthread_self[" + Thread.currentThread( ) +
			       "] sendMessageByChild to " + rank + 
			       " finished" );
        }
    }

    public int nLocalAgents( ) { 
	return localPopulation; 
    }

    public void getGlobalAgentArrayIndex( int[] src_index,
					  int[] dst_size, int[] dest_index )
    {
	for (int i = 0; i < dest_index.length; i++ ) {
	    dest_index[i] = src_index[i]; // calculate dest index
	    
	    if ( dest_index[i] < 0 || dest_index[i] >= dst_size[i] ) {
		// out of range
		for ( int j = 0; j < dest_index.length; j++ ) {
		    // all index must be set -1
		    dest_index[j] = -1;
		}
		return;
	    }
	}
    }

    protected static Object processAgentMigrationRequest( Object param ) {
	return null;
    }

    protected static Object sendMessageByChild( Object param ) {
	return null;
    }

    protected final int handle;
    protected final String className;
    protected final int placesHandle;

    protected int initPopulation;
    protected int localPopulation;
    protected int currentAgentId;
    protected AgentList agents;

    private static URLClassLoader agentLoader;
    private Class<?> agentClass;
    private Constructor<?> agentConstructor;

    public static int agentInitAgentsHandle;
    public static int agentInitPlacesHandle;
    public static int agentInitAgentId;
    public static int agentInitParentId;
}