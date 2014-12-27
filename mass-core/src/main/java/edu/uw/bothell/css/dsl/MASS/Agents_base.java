package edu.uw.bothell.css.dsl.MASS;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Vector;

@SuppressWarnings("serial")
public class Agents_base implements Serializable {

	//Used to toggle comments from Places_base.java
    private static final boolean printOutput = false;
    //private static final boolean printOutput = true;

    public static final int MAX_AGENTS_PER_NODE = 100000000; // 100 million 

    private final int handle;
    private final String className;
    private final int placesHandle;

    private int initPopulation;
    private int localPopulation;
    private int currentAgentId;
    private AgentList agents;

    private static URLClassLoader agentLoader;
    private Class<?> agentClass;
    private Constructor<?> agentConstructor;

    private static int agentInitAgentsHandle;
    private static int agentInitPlacesHandle;
    private static int agentInitAgentId;
    private static int agentInitParentId;

    @SuppressWarnings("unused")
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
	    File curDir = new File( MASS.getWorkingDirectory() );
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
	currentAgentId = MASS_base.getMyPid() * MAX_AGENTS_PER_NODE;
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
	    MASS_base.getPlacesMap().get( new Integer( placesHandle ) );
	
	if ( printOutput == true )
	    MASS_base.log( "Agets_base constructor: placesDillClass = "
			   + " curPlaces = " + (Object)curPlaces );
	
	for ( int i = 0; i < curPlaces.getPlacesSize( ); i++ ) {
	    
	    // scan each place to see how many agents it can create
	    Place curPlace = curPlaces.getPlaces()[i];
	    
	    if ( printOutput == true )
		MASS_base.log( "Agent_base constructor place[" + i + "]" );
	    // create as many new agents as nColonists
	    for ( int nColonists =
		      protoAgent.map( initPopulation, curPlace.getSize(), 
				      curPlace.getIndex(), curPlace );
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

		newAgent.setPlace(curPlace);
		newAgent.setIndex(curPlace.getIndex());
		
		// store this agent in the bag of agents
		agents.add( newAgent );
		
		// register newAgent into curPlace
		curPlace.getAgents().add( newAgent );
	    }
	}
    }

    @SuppressWarnings("unused")
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
			       MASS_base.getAgentsMap().get( new Integer(handle) ).
			       agents.size_unreduced( ) + "\n" +
			       "Agents_base:callAll: agentsBagSize = " +
			       Mthread.agentBagSize );
	}
    }
    
    @SuppressWarnings("unused")
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
				   MASS_base.getCurrentReturns() );
		
		//Use the Agents' callMethod to have it begin running
		( (Object[])MASS_base.getCurrentReturns() )[myIndex - 1] =
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
			       MASS_base.getAgentsMap().get( new Integer(handle) ).
			       agents.size_unreduced( ) + "\n" +
			       "Agents_base:callAll: agentsBagSize = " + 
			       Mthread.agentBagSize );
	}
    }
    
    @SuppressWarnings("unused")
	public void manageAll( int tid ) {
	
	//Create the dllclass to access our agents from, out agentsDllClass 
	// for agent instantiation, and our bag for Agent objects after they 
	// have finished processing
	Places_base evaluatedPlaces 
	    = MASS_base.getPlacesMap().get( new Integer( placesHandle ) );
	
	// Spawn, Kill, Migrate. Check in that order throughout the bag of 
	// agents  sequentially.
	while ( true ) {
	    int myIndex; // each thread's agent index

	    Agent evaluationAgent = null;
	    synchronized( this ) {
		if ( ( myIndex = Mthread.agentBagSize ) == 0 )
		    break;

		// Grab the last agent and remove it for processing. 
		myIndex = Mthread.agentBagSize--;
		evaluationAgent = agents.get( myIndex - 1 );
		
		if ( printOutput == true ) 
		    MASS_base.log( "Agents_base.manageALL: Thread " + tid + 
				   " picked up " 
				   + evaluationAgent.getAgentId() );
		
	    }
	    int argumentcounter = 0;
	    
	    // If the spawn's newChildren field is set to anything higher than 
	    // zero, we need to create newChildren's worth of Agents in the 
	    // current location.

	    // Spawn() Check
	    int childrenCounter = evaluationAgent.getNewChildren();
	    
	    if ( printOutput == true ) 
		MASS_base.log( "agent " + evaluationAgent.getAgentId() +
			       "'s childrenCounter = " + childrenCounter );
	    
	    while ( childrenCounter > 0 ) {
		if ( printOutput == true )
		    MASS_base.log( "Agent_base.manageALL: Thread " + tid +
				   " will spawn a child of agent " + 
				   evaluationAgent.getAgentId() +
				   "...arguments.size( ) = " +
				   evaluationAgent.getArguments().length +
				   ", argumentcounter = " + argumentcounter );
		
		Agent addAgent = null;
		Object dummyArgument = new Object( );
		try {
		    agentInitAgentsHandle = this.handle;
		    agentInitPlacesHandle = this.placesHandle;
		    agentInitParentId = evaluationAgent.getAgentId();
		    synchronized( this ) {
		        agentInitAgentId = this.currentAgentId++;
		        addAgent =
			    // validate the correspondance of arguments and
			    // argumentcounter
			    ( evaluationAgent.getArguments().length >
			    argumentcounter ) ?
				// yes: this child agent should recieve an argument.
				( Agent )agentConstructor.
				newInstance( evaluationAgent.
				getArguments()[argumentcounter++] )
				:
				// no:  this child agent should not receive an arg.
				( Agent )agentConstructor.
				newInstance( dummyArgument );
		    }
		    addAgent.setIndex(evaluationAgent.getIndex());
		    addAgent.setPlace(evaluationAgent.getPlace());
		} catch ( Exception e ) {
		    MASS_base.log( "Agents_base.manageAll: " + this.className
				   + " not instantiated " + e );
		}
		
		// Push the created agent into our bag for returns and 
		// update the counter needed to keep track of our agents.
		
		addAgent.getPlace().getAgents().add( addAgent ); // auto sync
		this.agents.add( addAgent );           // auto syn
		
		// Decrement the newChildren counter once an Agent has been 
		// spawned
		evaluationAgent.setNewChildren(evaluationAgent.getNewChildren() - 1);
		childrenCounter--;
		
		if ( printOutput == true )
		    MASS_base.log( "Agent_base.manageALL: Thread " + tid +
				   " spawned a child of agent " + 
				   evaluationAgent.getAgentId() +
				   " and put the child " + addAgent.getAgentId() +
				   " child into retBag." );
	    }
	    
	    // Kill() Check
	    if ( printOutput == true )
		MASS_base.log( "Agent_base.manageALL: Thread " + tid +
			       " check " + evaluationAgent.getAgentId() + 
			       "'s alive = " + evaluationAgent.isAlive() );

	    if ( evaluationAgent.isAlive() == false ) {
		
		// Get the place in which evaluationAgent is 'stored' in
		Place evaluationPlace = evaluationAgent.getPlace();
		
                // remove the agent from this place
		evaluationPlace.getAgents().remove( evaluationAgent );

		// remove from AgentList, too!
		agents.remove( myIndex - 1 );

		// don't go down to migrate
		continue;
	    }
	    
	    //Migrate() check
	    
	    //Iterate over all dimensions of the agent to check its location
	    //against that of its place. If they are the same, return back.
	    int agentIndex = evaluationAgent.getIndex().length;
	    int[] destCoord = new int[agentIndex];
	    
	    // compute its coordinate
	    getGlobalAgentArrayIndex( evaluationAgent.getIndex(), 
				      evaluatedPlaces.getSize(), destCoord );
	    
	    if ( printOutput == true )
		MASS_base.log( "pthread_self[" + Thread.currentThread( ) +
			       "tid[" + tid + "]: calls from" +
			       "[" + evaluationAgent.getIndex()[0] +
			       "][" + evaluationAgent.getIndex()[1] + "]" +
			       " (destCoord[" + destCoord[0] +
			       "][" + destCoord[1] + "]" );
	    
	    if( destCoord[0] != -1 ) { 
		// destination valid
		int globalLinearIndex = 
		    evaluatedPlaces.
		    getGlobalLinearIndexFromGlobalArrayIndex( destCoord,
							      evaluatedPlaces.
							      getSize() );

		if ( printOutput == true )
		    MASS_base.log( " linear = " + globalLinearIndex +
				   " lower = " + evaluatedPlaces.getLowerBoundary()
				   + " upper = " + 
				   evaluatedPlaces.getUpperBoundary() + ")" );
		

		if ( globalLinearIndex >= evaluatedPlaces.getLowerBoundary() &&
		     globalLinearIndex <= evaluatedPlaces.getUpperBoundary() ) {
		    // local destination
		    
		    // Should remove the pointer object in the place that 
		    // points to the migrting Agent
		    Place oldPlace = evaluationAgent.getPlace();
		    if ( oldPlace.getAgents().remove( evaluationAgent ) == false ) {
			// should not happen
			if ( printOutput == true ) 
			    MASS_base.log( "evaluationAgent " + 
					   evaluationAgent.getAgentId() 
					   + " couldn't been found in " +
					   "the old place!" );
			System.exit( -1 );
		    }

		    if ( printOutput == true )
			MASS_base.log( "evaluationAgent " + 
				       evaluationAgent.getAgentId() 
				       + " was removed from the oldPlace["
				       + oldPlace.getIndex()[0] + "]["
				       + oldPlace.getIndex()[1] + "]" );
		    
		    // insert the migration Agent to a local destination place
		    int destinationLocalLinearIndex 
			= globalLinearIndex - evaluatedPlaces.getLowerBoundary();
		    
		    if ( printOutput == true )
			MASS_base.log( "destinationLocalLinerIndex = " 
				       + destinationLocalLinearIndex );

		    evaluationAgent.setPlace(MASS_base.getPlacesMap().
			get( new Integer( placesHandle ) ).
			getPlaces()[destinationLocalLinearIndex]);
		    
		    if ( printOutput == true )
			MASS_base.log( "evaluationAgent.place = " 
				       + evaluationAgent.getPlace() );
		    
		    evaluationAgent.getPlace().getAgents().add( evaluationAgent );
		    
		    if ( printOutput == true ) 
			MASS_base.log( "evaluationAgent " + 
				       evaluationAgent.getAgentId() +
				       " was inserted into the destPlace[" +
				       evaluationAgent.getPlace().getIndex()[0] + "][" +
				       evaluationAgent.getPlace().getIndex()[1] + "]" );
		} 
		else {
		    // remote destination

		    // remove evaluationAgent from AgentList
		    agents.remove( myIndex - 1 );
		    
		    // find the destination node
		    int destRank 
			= evaluatedPlaces.
			getRankFromGlobalLinearIndex( globalLinearIndex );

		    // relinquish the old place
		    evaluationAgent.setPlace(null);
		    
		    // create a request
		    AgentMigrationRequest request 
			= new AgentMigrationRequest( globalLinearIndex, 
						     evaluationAgent );
		    
		    if ( printOutput == true )
			MASS_base.log( "AgentMigrationRequest request = " + 
				       request );
		    
		    // enqueue the request to this node.map
		    Vector<AgentMigrationRequest> migrationReqList 
			= MASS_base.getMigrationRequests().get( destRank );

		    synchronized( migrationReqList ) {
			migrationReqList.add( request );
		    		    
			    if ( printOutput == true )
				MASS_base.log( "remoteRequest[" + destRank + 
					       "].add:" + " dst = " + 
					       globalLinearIndex );
		    }
		} 
	    }else {
		if ( printOutput == true )
		    MASS_base.log( " to destination invalid" );
	    }
	} // end of while( true )
	
	// When while loop finishes, all threads must barrier and tid = 0
	// must adjust AgentList.
	Mthread.barrierThreads( tid );
	
	if ( tid == 0 ) 
	    agents.reduce( );
	
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
	    int[][] comThrArgs = new int[MASS_base.getSystemSize()][4];

	    // communication thread id
	    ProcessAgentMigrationRequest[] thread_ref
		= new ProcessAgentMigrationRequest[MASS_base.getSystemSize()]; 
	    for ( int rank = 0; rank < MASS_base.getSystemSize(); rank++ ) {
		
		if ( rank == MASS_base.getMyPid() ) // don't communicate with myself
		    continue;
		
		// set arguments 
		comThrArgs[rank][0] = rank;
		comThrArgs[rank][1] = handle; // agents' handle
		comThrArgs[rank][2] = evaluatedPlaces.getHandle();
		comThrArgs[rank][3] = evaluatedPlaces.getLowerBoundary();
		
		// start a communication thread
		thread_ref[rank] 
		    = new ProcessAgentMigrationRequest( comThrArgs[rank] );
		thread_ref[rank].start( );
		
		if ( printOutput == true )
		    MASS_base.log( "Agents_base.manageAll will start " +
				   "processAgentMigrationRequest thread[" +
				   rank + "] = " + thread_ref[rank] );
	    }
	    
	    // wait for all the communication threads to be terminated
	    for ( int rank = MASS_base.getSystemSize() - 1; rank >= 0; rank-- ) {
		
		if ( printOutput == true )
		    MASS_base.log( "Agents_base.manageAll will join " +
				   "processAgentMigrationRequest A thread["
				   + rank + "] = " + thread_ref[rank] + 
				   " myPid = " + MASS_base.getMyPid() );

		if ( rank == MASS_base.getMyPid() ) // don't communicate with myself
		    continue;      
		
		if ( printOutput == true )
		    MASS_base.log( "Agents_base.manageAll will join " +
				   "processAgentMigrationRequest B thread["
				   + rank + "] = " + thread_ref[rank] );
		
		try {
		    thread_ref[rank].join( );
		}
		catch ( Exception e ) { }
		
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
		MASS_base.log( "pthread_self[" + Thread.currentThread( ) +
			       "] tid[" + tid + 
			       "] skips processAgentMigrationRequest" );
	}
    }
    
    private class ProcessAgentMigrationRequest extends Thread {
	private int destRank;
	private int agentHandle;
	private int placeHandle;

	public ProcessAgentMigrationRequest( int[] params ) {
	    destRank = params[0];
	    agentHandle = params[1];
	    placeHandle = params[2];
	}
	
	@SuppressWarnings("unused")
	public void run( ) {
	
	    Vector<AgentMigrationRequest> orgRequest = null;
	
	    if ( printOutput == true )
		MASS_base.log( "pthread_self[" + Thread.currentThread( ) +
			       "] rank[" + destRank + 
			       "]: starts processAgentMigrationRequest" );
	
	    // pick up the next rank to process
	    orgRequest = MASS_base.getMigrationRequests().get( destRank );
	
	    // for debugging
	    if ( printOutput == true ) {
		synchronized( orgRequest ) {
		    MASS_base.log( "tid[" + destRank + 
				   "] sends an exhange request to rank: " + 
				   destRank + " size() = " + 
				   orgRequest.size( ) );
	    
		    for ( int i = 0; i < orgRequest.size( ); i++ ) {
			MASS_base.log( "send " +
				       orgRequest.get(i).agent + " to " +
				       orgRequest.get(i).
				       destGlobalLinearIndex );
		    }
		}
	    }
	
	    // now compose and send a message by a child
	    Message messageToDest = 
		new Message( Message.ACTION_TYPE.
			     AGENTS_MIGRATION_REMOTE_REQUEST,
			     agentHandle, placeHandle, orgRequest );
	
	    if ( printOutput == true ) 
		MASS_base.log( "tid[" + destRank + 
			       "] made messageToDest to rank: " + destRank ); 

	    SendMessageByChild thread_ref =
		new SendMessageByChild( destRank, messageToDest );
	    thread_ref.start( );
	
	    // receive a message by myself
	    Message messageFromSrc = 
		MASS_base.getExchange().receiveMessage( destRank );
	
	    // at this point, the message must be exchanged.
	    try {
		thread_ref.join( );
		orgRequest.clear( );
	    } catch ( Exception e ) { }

	
	    if ( printOutput == true )
		MASS_base.log( "pthread id = " + thread_ref +
			       "pthread_join completed for rank[" +
			       destRank );
	
	    // process a message
	    Vector<AgentMigrationRequest> receivedRequest 
		= messageFromSrc.getMigrationReqList( );
	    
	    int agentsHandle = messageFromSrc.getHandle( );
	    int placesHandle = messageFromSrc.getDestHandle( );
	    Places_base dstPlaces = MASS_base.getPlacesMap().
		get( new Integer( placesHandle ) );
	
	    if ( printOutput == true )
		MASS_base.log( "request from rank[" + destRank + "] = " + 
			       receivedRequest + " size( ) = " + 
			       receivedRequest.size( ) );
	
	    // retrieve agents from receiveRequest
	    while( receivedRequest.size( ) > 0 ) {
		AgentMigrationRequest request = 
		    receivedRequest.remove( receivedRequest.size( ) - 1 );

		int globalLinearIndex = request.destGlobalLinearIndex;
		Agent agent = request.agent;
		
		// local destination
		int destinationLocalLinearIndex 
		    = globalLinearIndex - dstPlaces.getLowerBoundary();
		
		if ( printOutput == true )
		    MASS_base.log( " dstLocal = " + 
				   destinationLocalLinearIndex );
		
		Place dstPlace = 
		    dstPlaces.getPlaces()[destinationLocalLinearIndex];
		
		// push this agent into the place and the entire agent bag.
		agent.setPlace(dstPlace);
		dstPlace.getAgents().add( agent ); // auto sync
		agents.add( agent );          // auto sync
	    }
	    
	    if ( printOutput == true )
		MASS_base.log( "pthread_self[" + Thread.currentThread( ) +
			       "] retreive agents from rank[" + destRank + 
			       "] complated" );
	}
    }
    
    private class SendMessageByChild extends Thread {
	int rank;
	Message message;
	
	public SendMessageByChild( int rank, Message message ) {
	    this.rank = rank;
	    this.message = message;
	}
	
	@SuppressWarnings("unused")
	public void run( ) {
	    if ( printOutput == true )
		MASS_base.log( "pthread_self[" + Thread.currentThread( ) +
			       "] sendMessageByChild to " + rank + " starts" );
	    
	    MASS_base.getExchange().sendMessage( rank, message );
	    
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

	public static int getAgentInitAgentsHandle() {
		return agentInitAgentsHandle;
	}

	public static int getAgentInitPlacesHandle() {
		return agentInitPlacesHandle;
	}

	public static int getAgentInitParentId() {
		return agentInitParentId;
	}

	public static int getAgentInitAgentId() {
		return agentInitAgentId;
	}

	public String getClassName() {
		return className;
	}

	public int getInitPopulation() {
		return initPopulation;
	}

	public int getHandle() {
		return handle;
	}

	public int getPlacesHandle() {
		return placesHandle;
	}

	public int getLocalPopulation() {
		return localPopulation;
	}

	public AgentList getAgents() {
		return agents;
	}

}
