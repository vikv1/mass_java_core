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
import edu.uw.bothell.css.dsl.MASS.matrix.MatrixUtilities;

public class AgentsBase {


	/*
		The original developer did not want to mess up with the original agents.
		 Therefore, when an agent spawns another agent asynchronously, the ids start from
		 1 million and is incremented by one. (1 million is the upper limit for the number of agents).

		Currently we are using STARTING_CHILD_ASYNC_INDEX, however in the future
		 we are supposed to use currentAgentId.
	*/
	public static final int MAX_AGENTS_PER_NODE = AgentSerializer.getInstance().getMaxNumberOfAgents();


    private final int handle;
    private final String className;
    private final int placesHandle;
    private int initPopulation;
    private int localPopulation;
    private int currentAgentId;
    private AgentList agents;
    private static int agentInitAgentsHandle;
    private static int agentInitPlacesHandle;
    private static int agentInitAgentId;
    private static int agentInitParentId;
    
    private ObjectFactory objectFactory = SimpleObjectFactory.getInstance();

	/***** Agent population control *****/

	// the manager that takes care of excessive agent problem
	private AgentSpawnRequestManager agentSpawnRequestManager;

    public AgentsBase( int handle, String className, Object argument, int placesHandle, int initPopulation ) {
    	
    	this.handle = handle;
    	this.className = className;
    	this.placesHandle = placesHandle;
    	this.initPopulation = initPopulation;
    	this.agents = new AgentList( );
		this.agentSpawnRequestManager = new AgentSpawnRequestManager(MAX_AGENTS_PER_NODE);

    	// For debugging
		MASS.getLogger().debug( "handle = " + handle
    				+ ",placesHandle = " + placesHandle
    				+ ", class = " + className
    				+ ", argument = " + argument
    				+ ", initPopulation = " + initPopulation );

    	// initialize currentAgentId and localPopulation
    	currentAgentId = MASSBase.getMyPid() * MAX_AGENTS_PER_NODE;
    	localPopulation = 0;

    	// instantiate just one agent to call its map( ) function
    	agentInitAgentsHandle = handle;
    	agentInitPlacesHandle = placesHandle;
    	agentInitAgentId = -1; // proto
    	agentInitParentId = -1; // no parent
    	Agent protoAgent = null;
    	try {
    		protoAgent = objectFactory.getInstance(className, argument);
    	} catch ( Exception e ) {
    		// TODO - now what? There is an exception - what to do?
    		MASS.getLogger().error( "Agents_base.constructor: {} not instantiated ", className, e );
    	}

    	// retrieve the corresponding places
    	PlacesBase curPlaces = 
    			MASSBase.getPlacesMap().get( new Integer( placesHandle ) );

    	for ( int i = 0; i < curPlaces.getPlacesSize( ); i++ ) {

    		// scan each place to see how many agents it can create
    		Place curPlace = curPlaces.getPlaces()[i];

    		// create as many new agents as nColonists
    		for ( int nColonists =
    				protoAgent.map( initPopulation, curPlace.getSize(), 
    						curPlace.getIndex() );
    				nColonists > 0; nColonists--, localPopulation++ ) {

    			// agent instanstantiation and initialization
    			Agent newAgent = null;
    			try {
    				
    				agentInitAgentsHandle = handle;
    				agentInitPlacesHandle = placesHandle;
    				agentInitAgentId = currentAgentId++;
    				agentInitParentId = -1; // no parent
    				newAgent = objectFactory.getInstance(className, argument);
    			
    			} catch ( Exception e ) {
    				// TODO - now what? What to do when there is an exception?
    				MASS.getLogger().error( "Agents_base.constructor: {} not instaitated ", className, e );    			
    			}

    			newAgent.setPlace(curPlace);
    			newAgent.setIndex(curPlace.getIndex());

    			// store this agent in the bag of agents
    			agents.add( newAgent );

    			// register newAgent into curPlace
    			curPlace.getAgents().add( newAgent );    		
    		}
    	}
    }

    public void callAll( int functionId, Object argument, int tid ) {

    	int numOfOriginalVectors = MThread.getAgentBagSize();

    	while ( true ) {
    		
    		//Create the index for this iteration
    		int myIndex;

    		//Lock the index assignment so no two threads will receive 
    		// the same value
    		synchronized( this ) {

    			//Thread checking
    			MASS.getLogger().debug( "Starting index value is: {}", MThread.getAgentBagSize() );
    			
    			myIndex = MThread.getAgentBagSize();
    			MThread.setAgentBagSize(myIndex - 1);

    			//Error Checking
    			MASS.getLogger().debug( "Thread[" + tid + "]: agent(" + myIndex +	") assigned" );
    		
    		}

    		// Continue to run until the assigning index becomes negative
    		// (in which case, we've run out of agents)
    		if ( myIndex > 0 ) {

    			Agent tmpAgent = agents.get( myIndex - 1 );
    			
    			MASS.getLogger().debug( "Thread [" + tid + "]: agent(" + tmpAgent + ")[" + myIndex + "] was removed " );
    			MASS.getLogger().debug( "fId = " + functionId + " argument " + argument ); 
    			
    			//Use the Agents' callMethod to have it begin running
    			tmpAgent.callMethod( functionId, argument ); 

    			MASS.getLogger().debug( "Thread [" + tid + "]: (" + myIndex +	") has called its method; " +
    						"Current Agent Bag Size is: " + MThread.getAgentBagSize() );
    		
    		}
    		
    		//Otherwise, we are out of agents and should stop
    		//trying to assign any more
    		else {
    			break;
    		}
    	
    	}
    	
    	//Wait for the thread count to become zero
    	MThread.barrierThreads( tid );

    	//Assign the new bag of finished agents to the old pointer for reuse
    	if ( tid == 0 ) {
    		
    		MThread.setAgentBagSize(numOfOriginalVectors);

    		MASS.getLogger().debug( "Agents_base:callAll: agents.size = {}",
    					MASSBase.getAgentsMap().get( new Integer(handle) ).
    					agents.size_unreduced( ) );
    		MASS.getLogger().debug( "Agents_base:callAll: agentsBagSize = {}", MThread.getAgentBagSize() );
    	
    	}
    
    }

    public void callAll( int functionId, Object[] argument, int tid ) {

    	int numOfOriginalVectors = MThread.getAgentBagSize();

    	while ( true ) {
    		
    		// create the index for this iteration
    		int myIndex;

    		// Lock the index assginment so no tow threads will receive 
    		// the same index
    		synchronized( this ) {

    			// Thread checking
    			MASS.getLogger().debug( "Starting index value is: {}", MThread.getAgentBagSize() );
    			
    			myIndex = MThread.getAgentBagSize(); // myIndex == agentId + 1
    			MThread.setAgentBagSize(myIndex - 1);

    			//Error Checking
    			MASS.getLogger().debug( "Thread[" + tid + "]: agent(" + myIndex + ") assigned" );
    		
    		}

    		// While there are still indexes left, continue to grab and 
    		// execute threads.
    		if ( myIndex > 0 ) {
    			
    			// compute where to store this agent's return value
    			// note that myIndex = agentId + 1
    			Agent tmpAgent = agents.get( myIndex - 1 );
    			
    			//Use the Agents' callMethod to have it begin running
    			( (Object[])MASSBase.getCurrentReturns() )[myIndex - 1] =
    					tmpAgent.callMethod( functionId, 
    							argument[ myIndex - 1 ] );

    			MASS.getLogger().debug( "Thread [" + tid + "]: (" + myIndex +
    						") has called its method; " );
    		
    		}
    		
    		//Otherwise, we are out of agents and should stop
    		//trying to assign any more
    		else{
    			break;
    		}
    	
    	}
    	
    	//Confirm all threads have finished
    	MThread.barrierThreads( tid );

    	//Assign the new bag of finished agents to the old pointer for reuse
    	if ( tid == 0 ) {
    		
    		MThread.setAgentBagSize(numOfOriginalVectors);

    		MASS.getLogger().debug( "Agents_base:callAll: agents.size = {}", 
   					MASSBase.getAgentsMap().get( new Integer(handle) ).
   					agents.size_unreduced( ) );
   			
    		MASS.getLogger().debug( "Agents_base:callAll: agentsBagSize = {}", MThread.getAgentBagSize() );
   			
    	}
    
    }

    /**
     * Get the AgentList representation of all Agents located on this node
     * @return AgentList for this node
     */
	public AgentList getAgents() {
		return agents;
	}
	
    /**
     * Get the name of the class used for a Agent
     * @return The Agent implementation class name
     */
	protected String getClassName() {
		return className;
	}

	private void getGlobalAgentArrayIndex( int[] src_index, int[] dst_size, int[] dest_index ) {

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

	/**
	 * Get the Handle/ID number for this AgentsBase
	 * @return This AgentsBase ID
	 */
	public int getHandle() {
		return handle;
	}

	/**
	 * Get the number of Agents that were initially located on this node
	 * @return Number of local Agents present immediately after initialization
	 */
	protected int getInitPopulation() {
		return initPopulation;
	}

	/**
	 * Get the number of Agents located on this node
	 * @return Number of local Agents
	 */
	protected int getLocalPopulation() {
		return localPopulation;
	}
	
	/**
	 * Get the Places handle ID located on this node
	 * @return The local Places ID
	 */
	protected int getPlacesHandle() {
		return placesHandle;
	}

	public void manageAll( int tid ) {

    	//Create the dllclass to access our agents from, out agentsDllClass 
    	// for agent instantiation, and our bag for Agent objects after they 
    	// have finished processing
    	PlacesBase evaluatedPlaces	= MASSBase.getPlacesMap().get( new Integer( placesHandle ) );

    	// Spawn, Kill, Migrate. Check in that order throughout the bag of 
    	// agents  sequentially.
    	while ( true ) {
    		
    		int myIndex; // each thread's agent index

    		Agent evaluationAgent = null;
    		
    		synchronized( this ) {
    			
    			if ( ( myIndex = MThread.getAgentBagSize() ) == 0 )
    				break;

    			// Grab the last agent and remove it for processing. 
    			myIndex = MThread.getAgentBagSize();
    			MThread.setAgentBagSize(myIndex - 1);
    			evaluationAgent = agents.get( myIndex - 1 );

    			MASS.getLogger().debug( "Agents_base.manageALL: Thread " + tid + 
    						" picked up " 
    						+ evaluationAgent.getAgentId() );

    		}
    		
    		int argumentcounter = 0;

    		// If the spawn's newChildren field is set to anything higher than 
    		// zero, we need to create newChildren's worth of Agents in the 
    		// current location.

			/******* SPAWN() CHECK *******/
    		int childrenCounter = evaluationAgent.getNewChildren();

    		MASS.getLogger().debug( "agent " + evaluationAgent.getAgentId() +
    					"'s childrenCounter = " + childrenCounter );

    		while ( childrenCounter > 0 ) {

    			MASS.getLogger().debug( "Agent_base.manageALL: Thread " + tid +
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

						addAgent =
								(Agent) (// validate the correspondance of arguments and
										// argumentcounter
										(evaluationAgent.getArguments().length >
												argumentcounter) ?
												// yes: this child agent should recieve an argument.
												objectFactory.getInstance(className, evaluationAgent.getArguments()[argumentcounter++])
												:
												objectFactory.getInstance(className, dummyArgument)
								);

					}

					// Index SHOULD be set using getplace not agent.getindex()
					addAgent.setIndex(evaluationAgent.getPlace().getIndex());
    				addAgent.setPlace(evaluationAgent.getPlace());

					/** Agent population control work begins, execution order is important! **/

					// check if the agent is going to run in the system
					if (agentSpawnRequestManager.shouldAgentRunInTheSystem(addAgent, this.agents.size()))
					{
						// check if there is available agent id
						Integer availableAgentId = agentSpawnRequestManager.getNextAvailableAgentId();
						if (availableAgentId > -1)
						{
							addAgent.setAgentId(availableAgentId);
						}
						// assign a never used id
						else
						{
							addAgent.setAgentId(this.currentAgentId++);
						}

						// Push the created agent into our bag for returns and
						// update the counter needed to keep track of our agents.
						addAgent.getPlace().getAgents().add( addAgent ); // auto sync
						this.agents.add( addAgent );           // auto syn
					}

    			} catch ( Exception e ) {
    				// TODO - now what? What to do when an exception is thrown?
    				MASS.getLogger().error( "Agents_base.manageAll: {} not instantiated", this.className, e );
    			}

    			// Decrement the newChildren counter once an Agent has been 
    			// spawned
    			evaluationAgent.setNewChildren(evaluationAgent.getNewChildren() - 1);
    			childrenCounter--;

    			MASS.getLogger().debug( "Agent_base.manageALL: Thread " + tid +
    						" spawned a child of agent " + 
    						evaluationAgent.getAgentId() +
    						" and put the child " + addAgent.getAgentId() +
    						" child into retBag." );

				/**
				 * every time we spawn a new agent, we should check if there is available index first!!!
				 * */
    		
    		}
			/*****************************/

			/******* KILL() CHECK *******/
    		MASS.getLogger().debug( "Agent_base.manageALL: Thread " + tid +
    					" check " + evaluationAgent.getAgentId() + 
    					"'s alive = " + evaluationAgent.isAlive() );

    		if ( evaluationAgent.isAlive() == false ) {

    			// Get the place in which evaluationAgent is 'stored' in
    			Place evaluationPlace = evaluationAgent.getPlace();

    			// remove the agent from this place
    			evaluationPlace.getAgents().remove( evaluationAgent );

    			// remove from AgentList, too!
    			agents.remove( myIndex - 1 );

				/** Agent population control work begins, execution order is important! **/

				// every time we kill an agent, we should add its id to the available ids queue
				agentSpawnRequestManager.addAvailabeAgentId(evaluationAgent.getAgentId());

				// then we check if there is any agent spawn request
				Agent agentSpawnRequest = agentSpawnRequestManager.getNextAgentSpawnRequest();
				if (agentSpawnRequest != null)
				{
					// TODO VERIFY IF INDEX AND PLACE INFORMATION ARE CORRECT!!

					// check if there is available agent id
					Integer availableAgentId = agentSpawnRequestManager.getNextAvailableAgentId();
					if (availableAgentId > -1)
					{
						agentSpawnRequest.setAgentId(availableAgentId);
					}
					// assign a never used id
					else
					{
						agentSpawnRequest.setAgentId(this.currentAgentId++);
					}

					// retrieve the corresponding places
					PlacesBase curPlaces =
							MASSBase.getPlacesMap().get( new Integer( placesHandle ) );
					int globalLinearIndex = MatrixUtilities.getLinearIndex( curPlaces.getSize(), agentSpawnRequest.getIndex() );
					// local destination
					int destinationLocalLinearIndex = globalLinearIndex - curPlaces.getLowerBoundary();

					Place curPlace = curPlaces.getPlaces()[destinationLocalLinearIndex];

					// push this agent into the place and the entire agent bag.
					agentSpawnRequest.setPlace(curPlace);

					// Push the created agent into our bag for returns and
					// update the counter needed to keep track of our agents.
					agentSpawnRequest.getPlace().getAgents().add( agentSpawnRequest ); // auto sync
					this.agents.add( agentSpawnRequest );           // auto syn
				}

    			// don't go down to migrate
    			continue;
    		
    		}
			/****************************/

			/******* MIGRATE() CHECK *******/
    		//Iterate over all dimensions of the agent to check its location
    		//against that of its place. If they are the same, return back.
    		int agentIndex = evaluationAgent.getIndex().length;
    		int[] destCoord = new int[agentIndex];

    		// compute its coordinate
    		getGlobalAgentArrayIndex( evaluationAgent.getIndex(), 
    				evaluatedPlaces.getSize(), destCoord );

    		MASS.getLogger().debug( "pthread_self[" + Thread.currentThread( ) +
    					"tid[" + tid + "]: calls from" +
    					"[" + evaluationAgent.getIndex()[0] +
    					"].." +
    					" (destCoord[" + destCoord[0] +
    					"]..)" );

    		if( destCoord[0] != -1 ) { 
    			
    			// destination valid
    			int globalLinearIndex = MatrixUtilities.getLinearIndex( evaluatedPlaces.getSize(), destCoord );

    			MASS.getLogger().debug( " linear = " + globalLinearIndex +
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
    					MASS.getLogger().error( "evaluationAgent {}" + 
    								evaluationAgent.getAgentId() 
    								+ " couldn't been found in " +
    								"the old place!" );
    					
    					System.exit( -1 );
    				
    				}

    				MASS.getLogger().debug( "evaluationAgent " + 
    							evaluationAgent.getAgentId() 
    							+ " was removed from the oldPlace["
    							+ oldPlace.getIndex()[0] + "].." );

    				// insert the migration Agent to a local destination place
    				int destinationLocalLinearIndex 
    				= globalLinearIndex - evaluatedPlaces.getLowerBoundary();

    				MASS.getLogger().debug( "destinationLocalLinerIndex = {}", destinationLocalLinearIndex );

    				evaluationAgent.setPlace(MASSBase.getPlacesMap().
    						get( new Integer( placesHandle ) ).
    						getPlaces()[destinationLocalLinearIndex]);

    				evaluationAgent.getPlace().getAgents().add( evaluationAgent );

    				MASS.getLogger().debug( "evaluationAgent " + 
    							evaluationAgent.getAgentId() +
    							" was inserted into the destPlace[" +
    							evaluationAgent.getPlace().getIndex()[0] + "].." );
    			
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

    				MASS.getLogger().debug( "AgentMigrationRequest request = {}", request );

    				// enqueue the request to this node.map
    				Vector<AgentMigrationRequest> migrationReqList 
    				= MASSBase.getMigrationRequests().get( destRank );

    				synchronized( migrationReqList ) {
    					migrationReqList.add( request );

    					MASS.getLogger().debug( "remoteRequest[" + destRank +	"].add:" + " dst = " + globalLinearIndex );

    				}
    			
    			} 
    		
    		}
    		
    		else {
    			
    			MASS.getLogger().error( " to destination invalid" );
    		
    		}
			/*******************************/
    	} // end of while( true )

    	// When while loop finishes, all threads must barrier and tid = 0
    	// must adjust AgentList.
    	MThread.barrierThreads( tid );

    	if ( tid == 0 ) agents.reduce( );

    	// all threads must barrier synchronize here.
    	MThread.barrierThreads( tid );
    	if ( tid == 0 ) {

    		MASS.getLogger().debug( "tid[{}] now enters processAgentMigrationRequest", tid );

    		// the main thread spawns as many communication threads as the 
    		// number of remote computing nodes and let each invoke 
    		// processAgentMigrationReq. 
    		// args to threads: rank, agentHandle, placeHandle, lower_boundary
    		int[][] comThrArgs = new int[MASSBase.getSystemSize()][4];

    		// communication thread id
    		ProcessAgentMigrationRequest[] thread_ref
    		= new ProcessAgentMigrationRequest[MASSBase.getSystemSize()]; 
    		for ( int rank = 0; rank < MASSBase.getSystemSize(); rank++ ) {

    			if ( rank == MASSBase.getMyPid() ) // don't communicate with myself
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

    			MASS.getLogger().debug( "Agents_base.manageAll will start " +
    						"processAgentMigrationRequest thread[" +
    						rank + "] = " + thread_ref[rank] );
    		
    		}

    		// wait for all the communication threads to be terminated
    		for ( int rank = MASSBase.getSystemSize() - 1; rank >= 0; rank-- ) {

    			MASS.getLogger().debug( "Agents_base.manageAll will join " +
    						"processAgentMigrationRequest A thread["
    						+ rank + "] = " + thread_ref[rank] + 
    						" myPid = " + MASSBase.getMyPid() );

    			if ( rank == MASSBase.getMyPid() ) // don't communicate with myself
    				continue;      

    			MASS.getLogger().debug( "Agents_base.manageAll will join " +
    						"processAgentMigrationRequest B thread["
    						+ rank + "] = " + thread_ref[rank] );

    			try {
    				thread_ref[rank].join( );
    			}
    			catch ( Exception e ) {
    				MASS.getLogger().error("Unable to join rank!", e);
    			}

    			MASS.getLogger().debug( "Agents_base.manageAll joined " +
    						"processAgentMigrationRequest C thread[" +
    						rank + "] = " + thread_ref[rank] );
    		
    		}

    		localPopulation = agents.size_unreduced( );

    		MASS.getLogger().debug( "Agents_base.manageAll completed: localPopulation = {}", localPopulation );
    	
    	}
    	
    	else {
    		
    		MASS.getLogger().debug( "pthread_self[" + Thread.currentThread( ) +
    					"] tid[" + tid + 
    					"] skips processAgentMigrationRequest" );
    	
    	}
    
    }

	/**
	 * Get the number of Agents located on this node
	 * @return Number of local Agents
	 */
	public int nLocalAgents( ) {
    	return localPopulation; 
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

    		MASS.getLogger().debug( "pthread_self[" + Thread.currentThread( ) +
    					"] rank[" + destRank + 
    					"]: starts processAgentMigrationRequest" );

    		// pick up the next rank to process
    		orgRequest = MASSBase.getMigrationRequests().get( destRank );

    		// now compose and send a message by a child
    		Message messageToDest = 
    				new Message( Message.ACTION_TYPE.
    						AGENTS_MIGRATION_REMOTE_REQUEST,
    						agentHandle, placeHandle, orgRequest );

    		MASS.getLogger().debug( "tid[" + destRank + 
    					"] made messageToDest to rank: " + destRank ); 

    		SendMessageByChild thread_ref =
    				new SendMessageByChild( destRank, messageToDest );
    		thread_ref.start( );

    		// receive a message by myself
    		Message messageFromSrc = 
    				MASSBase.getExchange().receiveMessage( destRank );

    		// at this point, the message must be exchanged.
    		try {
    			thread_ref.join( );
    			orgRequest.clear( );
    		} 
    		catch ( Exception e ) {
    			// TODO - what to do if an exception is thrown?
    			MASS.getLogger().error("Exception thrown while exchanging async message", e);
    		}


    		MASS.getLogger().debug( "pthread id = " + thread_ref +
    					"pthread_join completed for rank[" +
    					destRank );

    		// process a message
    		Vector<AgentMigrationRequest> receivedRequest 
    		= messageFromSrc.getMigrationReqList( );

    		int agentsHandle = messageFromSrc.getHandle( );
    		int placesHandle = messageFromSrc.getDestHandle( );
    		PlacesBase dstPlaces = MASSBase.getPlacesMap().
    				get( new Integer( placesHandle ) );

    		MASS.getLogger().debug( "request from rank[" + destRank + "] = " + 
    					receivedRequest + " size( ) = " + 
    					receivedRequest.size( ) );

    		// retrieve agents from receiveRequest
    		while( receivedRequest.size( ) > 0 ) {
    			// TODO investigate
    			AgentMigrationRequest request = 
    					receivedRequest.remove( receivedRequest.size( ) - 1 );

    			int globalLinearIndex = request.destGlobalLinearIndex;
    			Agent agent = request.agent;

    			// local destination
    			int destinationLocalLinearIndex 
    			= globalLinearIndex - dstPlaces.getLowerBoundary();

    			MASS.getLogger().debug( " dstLocal = {}", destinationLocalLinearIndex );

    			Place dstPlace = dstPlaces.getPlaces()[destinationLocalLinearIndex];

    			// push this agent into the place and the entire agent bag.
    			agent.setPlace(dstPlace);
    			agent.setIndex(dstPlace.getIndex());
    			dstPlace.getAgents().add( agent ); // auto sync
    			agents.add( agent );          // auto sync
    		
    		}

    		MASS.getLogger().debug( "pthread_self[" + Thread.currentThread( ) +
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

    	public void run( ) {
    		
    		MASS.getLogger().debug( "pthread_self[" + Thread.currentThread( ) +
    					"] sendMessageByChild to " + rank + " starts" );

    		MASSBase.getExchange().sendMessage( rank, message );

    		MASS.getLogger().debug( "pthread_self[" + Thread.currentThread( ) +
    					"] sendMessageByChild to " + rank + 
    					" finished" );
    	
    	}
    
    }
}