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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import edu.uw.bothell.css.dsl.MASS.factory.ObjectFactory;
import edu.uw.bothell.css.dsl.MASS.factory.SimpleObjectFactory;
import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;

@SuppressWarnings("serial")
public class AgentsBase implements Serializable {


	/*
		The original developer did not want to mess up with the original agents.
		 Therefore, when an agent spawns another agent asynchronously, the ids start from
		 1 million and is incremented by one. (1 million is the upper limit for the number of agents).

		Currently we are using STARTING_CHILD_ASYNC_INDEX, however in the future
		 we are supposed to use currentAgentId.
	*/
    public static final int MAX_AGENTS_PER_NODE = 100000000; // 100 million 
    public static final int STARTING_CHILD_ASYNC_INDEX = 1000000; // 1 million


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

	// logging
	private transient Log4J2Logger logger = Log4J2Logger.getInstance();

    // Async section
    //private volatile LinkedList<Agent> asyncQueue;
    private volatile int[] asyncAgentIdList; // indices of agents in the bag - list of agent ids for callAllAsync
    private volatile int asyncAgentIdListHead = 0; // next location in the queue to poll index
    private volatile int asyncAgentIdListTail = 0; // next location in the queue to insert index
    private volatile List<Agent> asyncCompletedAgentList; // agents that have been completed from callAllAsync
    private volatile int[] asyncFuncList; // list of function ids to be executed asynchronously
    private volatile boolean isIdle = true; // whether this node is processing async queue or not
    private volatile AtomicInteger inProcessAgentCount = new AtomicInteger(0); // the number of agents in process
																				// - shared among MThreads
    
    /**
     * true when slave node receive request 
     * for result from master
     */
    private boolean resultRequestFromMaster = false;
    
    /**
     * to avoid unnecessary synchronization
     * use this index to assign to spawned agents' asyncIndex
     * during callAllAsync 
     */
    private int childAsyncIndex = STARTING_CHILD_ASYNC_INDEX;
    
    public AgentsBase( int handle, String className, Object argument, int placesHandle, int initPopulation ) {
    	
    	this.handle = handle;
    	this.className = className;
    	this.placesHandle = placesHandle;
    	this.initPopulation = initPopulation;
    	this.agents = new AgentList( );
    	
    	// Async handling
    	//this.asyncQueue = new LinkedList<Agent>();
    	//this.asyncQueue = new ConcurrentLinkedQueue<Agent>();

    	// For debugging
  		logger.debug( "handle = " + handle
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
    		logger.error( "Agents_base.constructor: {} not instantiated ", className, e );
    	}

    	// retrieve the corresponding places
    	PlacesBase curPlaces = 
    			MASSBase.getPlacesMap().get( new Integer( placesHandle ) );

    		// TODO - what is really being logged here?
//   		logger.debug( "Agents_base constructor: placesClass = " 
//    				+ " curPlaces = " + (Object)curPlaces );

    	for (int i = 0; i < curPlaces.getNumberOfPlacesOnCurrentNode( ); i++ ) {

    		// scan each place to see how many agents it can create
    		Place curPlace = curPlaces.getPlaces()[i];

   			// logger.debug( "Agent_base constructor place[{}]", i );
    		
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
    				logger.error( "Agents_base.constructor: {} not instaitated ", className, e );    			
    			}

    			// TODO - what is being logged here?
//   				logger.debug( " newAgent[" + localPopulation + "] = " + 
//    						(Object)newAgent );

    			newAgent.setPlace(curPlace);
    			newAgent.setIndex(curPlace.getIndex());

    			// store this agent in the bag of agents
    			agents.add( newAgent );

    			// register newAgent into curPlace
    			curPlace.getAgents().add( newAgent );    		
    		}
    	}
    }

    protected static int getAgentInitAgentId() {
		return agentInitAgentId;
	}
    
    protected static Object processAgentMigrationRequest( Object param ) {
    	return null;
    }
    
    protected static Object sendMessageByChild( Object param ) {
    	return null;
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
   				logger.debug( "Starting index value is: {}", MThread.getAgentBagSize() );
    			
    			myIndex = MThread.getAgentBagSize();
    			MThread.setAgentBagSize(myIndex - 1);

    			//Error Checking
   				logger.debug( "Thread[" + tid + "]: agent(" + myIndex +	") assigned" );
    		
    		}

    		// Continue to run until the assigning index becomes negative
    		// (in which case, we've run out of agents)
    		if ( myIndex > 0 ) {

    			Agent tmpAgent = agents.get( myIndex - 1 );
    			
   				logger.debug( "Thread [" + tid + "]: agent(" + tmpAgent + ")[" + myIndex + "] was removed " );
    			logger.debug( "fId = " + functionId + " argument " + argument ); 
    			
    			//Use the Agents' callMethod to have it begin running
    			tmpAgent.callMethod( functionId, argument ); 

   				logger.debug( "Thread [" + tid + "]: (" + myIndex +	") has called its method; " +
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

   			logger.debug( "Agents_base:callAll: agents.size = " +
    					MASSBase.getAgentsMap().get( new Integer(handle) ).
    					agents.size_unreduced( ) + "\n" +
    					"Agents_base:callAll: agentsBagSize = " +
    					MThread.getAgentBagSize() );
    	
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
   				logger.debug( "Starting index value is: {}", MThread.getAgentBagSize() );
    			
    			myIndex = MThread.getAgentBagSize(); // myIndex == agentId + 1
    			MThread.setAgentBagSize(myIndex - 1);

    			//Error Checking
   				logger.debug( "Thread[" + tid + "]: agent(" + myIndex + ") assigned" );
    		
    		}

    		// While there are still indexes left, continue to grab and 
    		// execute threads.
    		if ( myIndex > 0 ) {
    			
    			// compute where to store this agent's return value
    			// note that myIndex = agentId + 1
    			Agent tmpAgent = agents.get( myIndex - 1 );
    			
   				logger.debug( "Thread[" + tid + "]: agent("+ myIndex + 
    						"): MASS_base::currentReturns  = " + 
    						MASSBase.getCurrentReturns() );

    			//Use the Agents' callMethod to have it begin running
    			( (Object[])MASSBase.getCurrentReturns() )[myIndex - 1] =
    					tmpAgent.callMethod( functionId, 
    							argument[ myIndex - 1 ] );

   				logger.debug( "Thread [" + tid + "]: (" + myIndex +
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

   			logger.debug( "Agents_base:callAll: agents.size = " + 
   					MASSBase.getAgentsMap().get( new Integer(handle) ).
   					agents.size_unreduced( ) + "\n" +
   					"Agents_base:callAll: agentsBagSize = " + 
   					MThread.getAgentBagSize() );
    	}
    
    }

  public void callAllAsync(int tid ) throws Exception {	 
	  int executedAgentIndex = -1; 	// next agent id in the list to be executed
	  int inProcessCount = 0;		// thread local variable copied from inProcessAgentCount
	  do 
	  {
		  // access by multiple thread
		  // each thread pick up the next agent id to be executed
		  synchronized(asyncAgentIdList) {

			  logger.debug("callAllAsync synchronized asyncQueue size = {}", asyncAgentIdListSize());

			  executedAgentIndex = asyncAgentIdListGetNextId();
			  if(executedAgentIndex != -1) {
			  		inProcessCount = inProcessAgentCount.incrementAndGet();
			  }
		  }

		  // this thread still has an agent to be executed
		  if(executedAgentIndex != -1) {

  		  logger.debug("dequeue index " + executedAgentIndex + ", null = " + (MASSBase.getCurrentAgentsBase().getAgents()
              .get(executedAgentIndex) == null));

			  // invoke all functions in this agent's function list
			  int asyncFuncIndex;
			  while((asyncFuncIndex = MASSBase.getCurrentAgentsBase().getAgents()
				.get(executedAgentIndex).nextAsyncFuncListIndex()) < asyncFuncList.length)
			  {
					/*
					int asyncFuncIndex = MASSBase.getCurrentAgents().getAgents()
					  .get(executedAgentIndex).nextAsyncFuncListIndex();
					*/
					//
					switch(asyncFuncList[asyncFuncIndex]) {
						case -2:
							MASSBase.getCurrentAgentsBase().getAgents().get(executedAgentIndex).autoMigrateStart();
							break;
						case -1:
							MASSBase.getCurrentAgentsBase().getAgents().get(executedAgentIndex).autoMigrateNext();
							break;
						default:
							MASSBase.getCurrentAgentsBase().getAgents().get(executedAgentIndex).callMethod(
							asyncFuncList[asyncFuncIndex],
											MASSBase.getCurrentAgentsBase().getAgents()
											.get(executedAgentIndex).getAsyncArgument());
							break;
					}

					// only the first method has arg
					/*
						* In future development, we need to pass argumentList and setAsyncArgument from this list.
					* */
					MASSBase.getCurrentAgentsBase().getAgents().get(executedAgentIndex).setAsyncArgument(null);

					// If this agent has been terminated, we should make sure to kill it from our local process too
					if(MASSBase.getCurrentAgentsBase().getAgents()
					  .get(executedAgentIndex).isHasAlreadyGone())
					{
						MASSBase.getCurrentAgentsBase().getAgents()
						.get(executedAgentIndex).setHasAlreadyGone(false);
						agents.remove( executedAgentIndex);
						break;
					}

				  	//  If this agent has been migrated to local node, add the agent into the list that is going to
				    // 		be executed in callAllAsync
					if(MASSBase.getCurrentAgentsBase().getAgents()
					   .get(executedAgentIndex).isNeedsToGoBackToAsyncQueue())
					{
						synchronized(asyncAgentIdList) {
						  MASSBase.getCurrentAgentsBase().getAgents()
							.get(executedAgentIndex).setNeedsToGoBackToAsyncQueue(false);
							asyncAgentIdListAdd(executedAgentIndex);
							asyncAgentIdList.notifyAll();
						}
						break;
					}
			  }

			  // If there is no more agents in the current bag and there is no more function to
			  //  be executed, then we put agent into completed list.
			  if(MASSBase.getCurrentAgentsBase().getAgents()
					.get(executedAgentIndex) != null &&
					MASSBase.getCurrentAgentsBase().getAgents()
					.get(executedAgentIndex).getAsyncFuncListIndex() >= asyncFuncList.length)
			  {
					synchronized(asyncCompletedAgentList) {
						  asyncCompletedAgentList.add(MASSBase.getCurrentAgentsBase().getAgents()
							.get(executedAgentIndex).cloneForAsyncResult());

						logger.debug(MASSBase.getCurrentAgentsBase().getAgents()
							.get(executedAgentIndex).getMyOriginalAsyncIndex() +
							" asyncCompletedAgentList size is now " + asyncCompletedAgentList.size());
					}
			  }


			  synchronized(asyncAgentIdList)
			  {
				  if(executedAgentIndex != -1)
				  {
					  inProcessCount = inProcessAgentCount.decrementAndGet();
					  asyncAgentIdList.notifyAll();
				  }
			 }
		  }
	  }

	  while(executedAgentIndex != -1 && inProcessCount > 0);
    
	  /**
	   * How to collect results?
	   * We need to mark each agents index before the
	   * These agents' results will be kept in order
	   * Spawned agents result order is not guarantee
	   * How master thread know it's done?
	   * when all the slave thread (KEEP TRACK of thread count somewhere?) has pass the result
	   * (in Agents_base?) in 
	   * master thread consolidate and
	   * send the result to master node (in Agents)
	   * Master thread in master node wait for all results from slave
	   * nodes and return to caller
	   */
	  // TODO Keep track of slave thread pass result
	  // TODO Keep track of slave node pass result
	  // TODO sorting agent result
	  
	  // TODO 'population' update local & from other nodes
	  
    	// Confirm all threads have finished.
	  	// Backward compatibility, so that Mthread can return to status
	  	// Ready
	   MThread.barrierThreads( tid );
	}
	
	public AgentList getAgents() {
		return agents;
	}
	
	public int[] getAsyncAgentIdList() {
	//public LinkedList<Agent> getAsyncQueue() {
	  return asyncAgentIdList;
	}
	
	protected int[] getAsyncFuncList() {
	  return asyncFuncList;
	}
	
	protected void setAsyncFuncList(int[] value) {
	  asyncFuncList = value;
	}
	
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

	public int getHandle() {
		return handle;
	}

	protected int getInitPopulation() {
		return initPopulation;
	}

	protected int getLocalPopulation() {
		return localPopulation;
	}
	
	protected void setLocalPopulation(int population) {
	  localPopulation = population;
	}

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

   				logger.debug( "Agents_base.manageALL: Thread " + tid + 
    						" picked up " 
    						+ evaluationAgent.getAgentId() );

    		}
    		
    		int argumentcounter = 0;

    		// If the spawn's newChildren field is set to anything higher than 
    		// zero, we need to create newChildren's worth of Agents in the 
    		// current location.

    		// Spawn() Check
    		int childrenCounter = evaluationAgent.getNewChildren();

   			logger.debug( "agent " + evaluationAgent.getAgentId() +
    					"'s childrenCounter = " + childrenCounter );

    		while ( childrenCounter > 0 ) {

   				logger.debug( "Agent_base.manageALL: Thread " + tid +
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
    							(Agent) (// validate the correspondance of arguments and
    									// argumentcounter
    									( evaluationAgent.getArguments().length >
    									argumentcounter ) ?
    											// yes: this child agent should recieve an argument.
    											//    									( Agent )agentConstructor.
    											//    									newInstance( evaluationAgent.
    											//    											getArguments()[argumentcounter++] )
    											objectFactory.getInstance(className, evaluationAgent.getArguments()[argumentcounter++])
    											:
    												// no:  this child agent should not receive an arg.
    												//    												( Agent )agentConstructor.
    												//    												newInstance( dummyArgument ));
    												objectFactory.getInstance(className, dummyArgument)
    									);

    				}

    				addAgent.setIndex(evaluationAgent.getIndex());
    				addAgent.setPlace(evaluationAgent.getPlace());

    			} catch ( Exception e ) {
    				// TODO - now what? What to do when an exception is thrown?
    				logger.error( "Agents_base.manageAll: {} not instantiated", this.className, e );
    			}

    			// Push the created agent into our bag for returns and 
    			// update the counter needed to keep track of our agents.
    			addAgent.getPlace().getAgents().add( addAgent ); // auto sync
    			this.agents.add( addAgent );           // auto syn

    			// Decrement the newChildren counter once an Agent has been 
    			// spawned
    			evaluationAgent.setNewChildren(evaluationAgent.getNewChildren() - 1);
    			childrenCounter--;

   				logger.debug( "Agent_base.manageALL: Thread " + tid +
    						" spawned a child of agent " + 
    						evaluationAgent.getAgentId() +
    						" and put the child " + addAgent.getAgentId() +
    						" child into retBag." );
    		
    		}

    		// Kill() Check
   			logger.debug( "Agent_base.manageALL: Thread " + tid +
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

   			logger.debug( "pthread_self[" + Thread.currentThread( ) +
    					"tid[" + tid + "]: calls from" +
    					"[" + evaluationAgent.getIndex()[0] +
    					"].." +
    					" (destCoord[" + destCoord[0] +
    					"]..)" );

    		if( destCoord[0] != -1 ) { 
    			
    			// destination valid
    			int globalLinearIndex = 
    					evaluatedPlaces.
    					getGlobalLinearIndexFromGlobalArrayIndex( destCoord,
    							evaluatedPlaces.
    							getSize() );

   				logger.debug( " linear = " + globalLinearIndex +
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
   						logger.error( "evaluationAgent {}" + 
    								evaluationAgent.getAgentId() 
    								+ " couldn't been found in " +
    								"the old place!" );
    					
    					System.exit( -1 );
    				
    				}

   					logger.debug( "evaluationAgent " + 
    							evaluationAgent.getAgentId() 
    							+ " was removed from the oldPlace["
    							+ oldPlace.getIndex()[0] + "].." );

    				// insert the migration Agent to a local destination place
    				int destinationLocalLinearIndex 
    				= globalLinearIndex - evaluatedPlaces.getLowerBoundary();

   					logger.debug( "destinationLocalLinerIndex = {}", destinationLocalLinearIndex );

    				evaluationAgent.setPlace(MASSBase.getPlacesMap().
    						get( new Integer( placesHandle ) ).
    						getPlaces()[destinationLocalLinearIndex]);

    				// TODO - what is the purpose of logging an object?
//   					logger.debug( "evaluationAgent.place = {}", evaluationAgent.getPlace() );

    				evaluationAgent.getPlace().getAgents().add( evaluationAgent );

   					logger.debug( "evaluationAgent " + 
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

   					logger.debug( "AgentMigrationRequest request = {}", request );

    				// enqueue the request to this node.map
    				Vector<AgentMigrationRequest> migrationReqList 
    				= MASSBase.getMigrationRequests().get( destRank );

    				synchronized( migrationReqList ) {
    					migrationReqList.add( request );

   					logger.debug( "remoteRequest[" + destRank +	"].add:" + " dst = " + globalLinearIndex );

    				}
    			
    			} 
    		
    		}
    		
    		else {
    			
    			logger.error( " to destination invalid" );
    		
    		}
    	
    	} // end of while( true )

    	// When while loop finishes, all threads must barrier and tid = 0
    	// must adjust AgentList.
    	MThread.barrierThreads( tid );

    	if ( tid == 0 ) agents.reduce( );

    	// all threads must barrier synchronize here.
    	MThread.barrierThreads( tid );
    	if ( tid == 0 ) {

    		logger.debug( "tid[{}] now enters processAgentMigrationRequest", tid );

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

   				logger.debug( "Agents_base.manageAll will start " +
    						"processAgentMigrationRequest thread[" +
    						rank + "] = " + thread_ref[rank] );
    		
    		}

    		// wait for all the communication threads to be terminated
    		for ( int rank = MASSBase.getSystemSize() - 1; rank >= 0; rank-- ) {

   				logger.debug( "Agents_base.manageAll will join " +
    						"processAgentMigrationRequest A thread["
    						+ rank + "] = " + thread_ref[rank] + 
    						" myPid = " + MASSBase.getMyPid() );

    			if ( rank == MASSBase.getMyPid() ) // don't communicate with myself
    				continue;      

   				logger.debug( "Agents_base.manageAll will join " +
    						"processAgentMigrationRequest B thread["
    						+ rank + "] = " + thread_ref[rank] );

    			try {
    				thread_ref[rank].join( );
    			}
    			catch ( Exception e ) {
    				logger.error("Unable to join rank!", e);
    			}

   				logger.debug( "Agents_base.manageAll joined " +
    						"processAgentMigrationRequest C thread[" +
    						rank + "] = " + thread_ref[rank] );
    		
    		}

    		localPopulation = agents.size_unreduced( );

   			logger.debug( "Agents_base.manageAll completed: localPopulation = {}", localPopulation );
    	
    	}
    	
    	else {
    		
   			logger.debug( "pthread_self[" + Thread.currentThread( ) +
    					"] tid[" + tid + 
    					"] skips processAgentMigrationRequest" );
    	
    	}
    
    }

	public int nLocalAgents( ) {
    	return localPopulation; 
    }
	
  	public synchronized void spawnAsync(Agent targetAgent, int numAgents,
									  	Object[] initializedArguments,
									  	Object[] arguments)
  	{
		int argumentIndex = 0;

	  	for (int i = numAgents; i > 0; i--)
	  	{

        	logger.debug( "Agent_base.spawnAsync will spawn a child of agent " +
            	targetAgent.getAgentId() +
            	"...arguments index = " + argumentIndex);

      		Agent addAgent = null;
      		Object dummyArgument = new Object( );

			try {
				agentInitAgentsHandle = handle;
				agentInitPlacesHandle = placesHandle;
				agentInitParentId = targetAgent.getAgentId();
				agentInitAgentId = currentAgentId++;

				addAgent = (Agent) // validate the correspondance of arguments and
						   (
						  		// argumentcounter
						  		( initializedArguments != null ) ?
							  	// yes: this child agent should recieve an argument.
							  	//                      ( Agent )agentConstructor.
							  	//                      newInstance( evaluationAgent.
							  	//                          getArguments()[argumentcounter++] )
							  	objectFactory.getInstance(className, initializedArguments[argumentIndex])
							  	: objectFactory.getInstance(className, dummyArgument)
						   );
				  // TODO auto migration somewhere in here?

				addAgent.setIndex(targetAgent.getIndex());
				addAgent.setPlace(targetAgent.getPlace());
				addAgent.setAsyncFuncListIndex(0);
				addAgent.setMyAgentsBase(this);
				addAgent.resetAsyncResults();

				if(arguments != null) {
					addAgent.setAsyncArgument(arguments[argumentIndex]);
				}

				addAgent.setMyAsyncOriginalPid(MASSBase.getMyPid());
				addAgent.setMyOriginalAsyncIndex(childAsyncIndex);
				childAsyncIndex++;
				argumentIndex++;
      		}
      
			// TODO - now what? An exception was thrown - what to do next?
			catch ( Exception e ) {
				logger.error("spawn async", e);
			}

			// Push the created agent into our bag for returns and
			// update the counter needed to keep track of our agents.
			addAgent.getPlace().getAgents().add( addAgent ); // auto sync
			synchronized(asyncAgentIdList) {
				addAgent.setCurrentIndex(this.agents.size_unreduced());
				this.agents.add( addAgent );           // auto syn
					asyncAgentIdListAdd(addAgent.getCurrentIndex());
					asyncAgentIdList.notifyAll();
			}
      //numAgents--;
    	}

  	}

  public void migrateAsync(Agent targetAgent) {
	  //Iterate over all dimensions of the agent to check its location
	  // against that of its place. If they are the same, return back.
	  PlacesBase evaluatedPlaces = MASSBase.getPlacesMap().get( new Integer( placesHandle ) );
	  int[] destCoord = new int[targetAgent.getIndex().length];

	  // compute its coordinate
	  getGlobalAgentArrayIndex(targetAgent.getIndex(),
			  				   evaluatedPlaces.getSize(),
			  			       destCoord);

	  // debugging message
	  if (MASS.isConsoleLoggingEnabled())
	  {
		  StringBuilder targetCoordStr = new StringBuilder();
		  StringBuilder destCoordStr = new StringBuilder();
		  for(int i = 0; i < destCoord.length; i++) {
			  targetCoordStr.append("[" + targetAgent.getIndex()[i] + "]");
				destCoordStr.append("[" + destCoord[i] + "]");
		  }

		  logger.debug( "migrate async from " + targetCoordStr.toString() + " (destCoord" + destCoordStr.toString() );
	  }

	// check if the destination is valid
    if( destCoord[0] != -1 ) {      

      int globalLinearIndex = 
          evaluatedPlaces.
          getGlobalLinearIndexFromGlobalArrayIndex( destCoord,
              evaluatedPlaces.getSize() );

       logger.debug( " linear = " + globalLinearIndex +
            " lower = " + evaluatedPlaces.getLowerBoundary()
            + " upper = " + 
            evaluatedPlaces.getUpperBoundary() + ")" );

      if ( globalLinearIndex >= evaluatedPlaces.getLowerBoundary() &&
          globalLinearIndex <= evaluatedPlaces.getUpperBoundary() ) {
        
        // local destination

        // Should remove the pointer object in the place that 
        // points to the migrating Agent
        Place oldPlace = targetAgent.getPlace();
        if ( oldPlace.getAgents().remove( targetAgent ) == false ) {          

        	// should not happen
            logger.error( "evaluationAgent " + targetAgent.getAgentId() + " couldn't been found in the old place!" );
            System.exit( -1 );        
        }

        logger.debug( "evaluationAgent " + 
              targetAgent.getAgentId() 
              + " was removed from the oldPlace["
              + oldPlace.getIndex()[0] + "].." );

        // insert the migration Agent to a local destination place
        int destinationLocalLinearIndex 
        = globalLinearIndex - evaluatedPlaces.getLowerBoundary();

        logger.debug( "destinationLocalLinerIndex = {}",               + destinationLocalLinearIndex );

		  // Let this agent memorize where it has migrated to
		  targetAgent.setPlace(MASSBase.getPlacesMap()
            					.get( new Integer( placesHandle ) )
            					.getPlaces()[destinationLocalLinearIndex]);

        logger.debug( "evaluationAgent.place = {}", targetAgent.getPlace() );

		  // Now put the agent into its new place
		  targetAgent.getPlace().getAgents().add( targetAgent );

          logger.debug( "evaluationAgent " + 
              targetAgent.getAgentId() +
              " was inserted into the destPlace[" +
              targetAgent.getPlace().getIndex()[0] + "].." );

          synchronized(asyncAgentIdList)
		  {
			  // Since the agent stays in local, we need to put back into async queue
			  if(!asyncAgentIdListIsEmpty()) {
				  targetAgent.setNeedsToGoBackToAsyncQueue(true);
			  }
		  }
      }      
      else {       
        // remote destination
        targetAgent.setHasAlreadyGone(true);

        /* remove evaluationAgent from AgentList
         * DO NOT REMOVE, to remove in callAllAsync loop only
        synchronized(agents) {
          agents.remove( targetAgent.getCurrentIndex() );
        }*/

        // find the destination node
        int destRank = evaluatedPlaces.getRankFromGlobalLinearIndex( globalLinearIndex );

        logger.debug( "AgentMigrationRequest request from to dest rank " + 
              destRank + ", globalLinearIndex " + globalLinearIndex );

        // relinquish the old place
        targetAgent.setPlace(null);
        /* relinquish the parent too, for async purpose
         * DO NOT REMOVE to remove in callAllAsync loop only
        targetAgent.setCurrentIndex(-1);*/
        targetAgent.setMyAgentsBase(null);
        // create a request
        AgentMigrationRequest request = new AgentMigrationRequest( globalLinearIndex, targetAgent );
        
        MASS.getAsyncOutputThread().requestMigration(destRank, request);

        logger.debug("remoteRequest[" + destRank + 
                "].add:" + " globalLinearIndex = " + 
                globalLinearIndex );

      }
    }    
    else {      
        logger.error( " to destination invalid" );
    }
  }
	
  protected void resetChildAsyncIndex() {
    childAsyncIndex = STARTING_CHILD_ASYNC_INDEX;
  }

  public void resetAsyncCompletedAgentList() {
    int estFinalSize = (int)(1.2 * agents.size_unreduced());
	  asyncCompletedAgentList = Collections.synchronizedList(new ArrayList<Agent>(estFinalSize));
  }
  
  public List<Agent> getAsyncCompletedAgentList() {
    return asyncCompletedAgentList;
  }

  protected boolean getResultRequestFromMaster() {
    return resultRequestFromMaster;
  }
  
  protected void setResultRequestFromMaster(boolean value) {
    resultRequestFromMaster = value;
  }
  
  public boolean getIsAsyncLoopIdle() {
    logger.debug("getIsAsyncIdle return {}", isIdle);
    return isIdle;
  }
  
  protected void setIsAsyncLoopIdle(boolean value) {
    logger.debug("setIsAsyncIdle to {}", value);
    isIdle = value;
  }
  
  protected boolean hasNoInprocessAgents() {
    return inProcessAgentCount.get() == 0;
  }
  
  /**
   * AsyncQueue functions only to be called when asyncQueue is synchronized
   * @return
   */
  public int asyncAgentIdListSize() {
    if(asyncAgentIdListTail < asyncAgentIdListHead) {
      return asyncAgentIdListTail - asyncAgentIdListHead + asyncAgentIdList.length;
    }
    else {
      return asyncAgentIdListTail - asyncAgentIdListHead;
    }
  }
  
  public int asyncAgentIdListGetNextId() {
    if(asyncAgentIdListSize() == 0) {
      return -1;
    } else {
      int index = asyncAgentIdListHead;
		asyncAgentIdListHead = (asyncAgentIdListHead + 1) % asyncAgentIdList.length;
      return asyncAgentIdList[index];
    }
  }

  public void asyncAgentIdListClear() {
	  asyncAgentIdList = new int[1000000];
	  asyncAgentIdListHead = 0;
	  asyncAgentIdListTail = 0;
  }
  
  public void asyncAgentIdListAdd(int value) {
	  asyncAgentIdList[asyncAgentIdListTail] = value;
	  asyncAgentIdListTail = (asyncAgentIdListTail + 1)% asyncAgentIdList.length;
  }
  
  public boolean asyncAgentIdListIsEmpty() {
    return asyncAgentIdListTail == asyncAgentIdListHead;
  }
  
  public int asyncAgentIdListGet(int index) {
    return asyncAgentIdList[(index + asyncAgentIdListHead) % asyncAgentIdList.length];
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

   			logger.debug( "pthread_self[" + Thread.currentThread( ) +
    					"] rank[" + destRank + 
    					"]: starts processAgentMigrationRequest" );

    		// pick up the next rank to process
    		orgRequest = MASSBase.getMigrationRequests().get( destRank );

    		// for debugging
//    			synchronized( orgRequest ) {
//    				
//    				logger.debug( "tid[" + destRank + 
//    						"] sends an exhange request to rank: " + 
//    						destRank + " size() = " + 
//    						orgRequest.size( ) );
//
//    				for ( int i = 0; i < orgRequest.size( ); i++ ) {
//    					logger.debug( "send " +
//    							orgRequest.get(i).agent + " to " +
//    							orgRequest.get(i).
//    							destGlobalLinearIndex );
//    				}
//    			
//    			}
    		
//    		}

    		// now compose and send a message by a child
    		Message messageToDest = 
    				new Message( Message.ACTION_TYPE.
    						AGENTS_MIGRATION_REMOTE_REQUEST,
    						agentHandle, placeHandle, orgRequest );

   			logger.debug( "tid[" + destRank + 
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
    			logger.error("Exception thrown while exchanging async message", e);
    		}


   			logger.debug( "pthread id = " + thread_ref +
    					"pthread_join completed for rank[" +
    					destRank );

    		// process a message
    		Vector<AgentMigrationRequest> receivedRequest 
    		= messageFromSrc.getMigrationReqList( );

    		int agentsHandle = messageFromSrc.getHandle( );
    		int placesHandle = messageFromSrc.getDestHandle( );
    		PlacesBase dstPlaces = MASSBase.getPlacesMap().
    				get( new Integer( placesHandle ) );

   			logger.debug( "request from rank[" + destRank + "] = " + 
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

   				logger.debug( " dstLocal = {}", destinationLocalLinearIndex );

    			Place dstPlace = dstPlaces.getPlaces()[destinationLocalLinearIndex];

    			// push this agent into the place and the entire agent bag.
    			agent.setPlace(dstPlace);
    			agent.setIndex(dstPlace.getIndex());
    			dstPlace.getAgents().add( agent ); // auto sync
    			agents.add( agent );          // auto sync
    		
    		}

   			logger.debug( "pthread_self[" + Thread.currentThread( ) +
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
    		
   			logger.debug( "pthread_self[" + Thread.currentThread( ) +
    					"] sendMessageByChild to " + rank + " starts" );

    		MASSBase.getExchange().sendMessage( rank, message );

  			logger.debug( "pthread_self[" + Thread.currentThread( ) +
    					"] sendMessageByChild to " + rank + 
    					" finished" );
    	
    	}
    
    }
}