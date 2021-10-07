/*

 	MASS Java Software License
	© 2012-2020 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2020 University of Washington. MASS was developed by Computing and Software Systems at University of 
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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.stream.Collectors;

import edu.uw.bothell.css.dsl.MASS.annotations.OnArrival;
import edu.uw.bothell.css.dsl.MASS.annotations.OnCreation;
import edu.uw.bothell.css.dsl.MASS.annotations.OnDeparture;
import edu.uw.bothell.css.dsl.MASS.annotations.OnMessage;
import edu.uw.bothell.css.dsl.MASS.clock.GlobalLogicalClock;
import edu.uw.bothell.css.dsl.MASS.event.EventDispatcher;
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
	private int localPopulation = 0;
	private int currentAgentId;
	private AgentList agents;

	@SuppressWarnings("unused")
	private static int agentInitAgentsHandle;

	@SuppressWarnings("unused")
	private static int agentInitPlacesHandle;

	private static int agentInitAgentId;

	@SuppressWarnings("unused")
	private static int agentInitParentId;

	private ObjectFactory objectFactory = SimpleObjectFactory.getInstance();
	private EventDispatcher eventDispatcher = MASS.getEventDispatcher();
	private GlobalLogicalClock clock = MASS.getGlobalClock();

	/***** Agent population control *****/

	// the manager that takes care of excessive agent problem
	private AgentSpawnRequestManager agentSpawnRequestManager;

	/**
	 * constructor for Space class instantiate Agents with input_args(data points for SpaceAgent constructor) and
	 * init_args(for application specific Agent constructor)
	 * --------------modified by Yuna
	 */
	public AgentsBase( int handle, String className, Object input_argument, Object init_argument, int placesHandle) {

		this.handle = handle;
		this.className = className;
		this.placesHandle = placesHandle;

		this.agents = new AgentList( );
		this.agentSpawnRequestManager = new AgentSpawnRequestManager(MAX_AGENTS_PER_NODE);

		// For debugging
		MASS.getLogger().debug( "handle = " + handle + ",placesHandle = " + placesHandle
				+ ", class = " + className);

		// retrieve the corresponding places
		SpacePlacesBase curPlaces = (SpacePlacesBase) MASSBase.getPlacesMap().get( placesHandle );

		// retrieve input points and locate agent to a input point
		ArrayList<Point> inputData = (ArrayList<Point>) input_argument;
		this.initPopulation = inputData.size();
		MASS.getLogger().debug("inputData size = " + initPopulation);

		// initialize currentAgentId and localPopulation
		currentAgentId = MASSBase.getMyPid() * MAX_AGENTS_PER_NODE;

		// create for agent resides in the corresponding place
		SpacePlace curPlace = null;
		for (int i = 0; i < initPopulation; i++) {

			// agent instantiation and initialization
			SpaceAgent newAgent = null; 
			try {

				double[] location = inputData.get(i).getCoordinates();
				int[] index = SpaceUtilities.findDestIndex(location, curPlaces);  // find the index of place agent resides

				// find the place agent resides
				int linearIndex = MatrixUtilities.getLinearIndex(curPlaces.getSize(), index);

				MASS.getLogger().debug("find dest index: " + "index[" + index[0] + ", " + index[1] + ", linearIndex = " + linearIndex + 
						"], coordinates = [" + location[0] + "," + location[1] + "].");

				int lowerBoundary = curPlaces.getLowerBoundary();
				int upperBoundary = curPlaces.getUpperBoundary();
				double[] interval = curPlaces.getInterval();
				double[] subInterval = curPlaces.getSubInterval();
				double[] min = curPlaces.getMin();
				double[] max = curPlaces.getMax();
				int granularity = curPlaces.getGranularity();

				// if the point is not in the range of current Places, do not instantiate agent, go to next point
				if (linearIndex < lowerBoundary || linearIndex > upperBoundary) {
					continue;
				}

				agentInitAgentsHandle = handle;
				agentInitPlacesHandle = placesHandle;
				agentInitAgentId = currentAgentId++;
				agentInitParentId = -1; // no parent

				curPlace = (SpacePlace)(curPlaces.getPlaces()[linearIndex - lowerBoundary]);
				MASS.getLogger().debug("curPlace index = [" + curPlace.getIndex()[0] + ", " + curPlace.getIndex()[1] + "]"); 

				// find sub-index in the place where agent reside
				int dimensions = index.length;
				int[] subIndex = SpaceUtilities.findSubIndex(location, index, interval, subInterval, min);
				MASS.getLogger().debug("find dest subIndex: " + subIndex[0] + ", " + subIndex[1] + "]");


				SpaceAgentArgs spaceAgentArgs = new SpaceAgentArgs(location, location, index, subIndex, 0, i);
				MASS.getLogger().debug("spaceAgentArgs created.");


				Object[] finalArgs = new Object[2];
				MASS.getLogger().debug("Object[] finalArgs = new Object[2];");
				finalArgs[0] = (Object) spaceAgentArgs;
				MASS.getLogger().debug("finalArgs[0]");
				finalArgs[1] = init_argument;
				MASS.getLogger().debug("finalArgs[1]");

				newAgent = objectFactory.getInstance(className, (Object) finalArgs);  // initialize agent's location
				localPopulation++;

				newAgent.setAgentId( agentInitAgentId );
				newAgent.setPlace(curPlace);

				MASS.getLogger().debug("AgentsBase: newAgent id = " + newAgent.getAgentId() + " created. place index = [" + 
						newAgent.getPlace().getIndex()[0] + "," + newAgent.getPlace().getIndex()[1] + "], subindex = [" + subIndex[0] + 
						"," + subIndex[1] + "], currentCoordinates = [" + newAgent.getCurrentCoordinates()[0] + "," + newAgent.getCurrentCoordinates()[1] + 
						"], nextCoordinates = [" + newAgent.getNextCoordinates()[0] + "," + newAgent.getNextCoordinates()[1] + "], generation = " + 
						newAgent.getGeneration() + ".");   

				curPlace.addAgent(newAgent, subIndex);            

			} catch ( Exception e ) {
				// TODO - now what? What to do when there is an exception?
				MASS.getLogger().error( "Agents_base.constructor: {} not instaitated ", className, e );    			
			}
			// store this agent in the bag of agents
			agents.add( newAgent );

			// register newAgent into curPlace
			curPlace.getAgents().add( newAgent );   

			// register the new Agent with messaging provider
			MASS.getMessagingProvider().registerAgent( newAgent );

			// Agent has been created
			eventDispatcher.queueAsync( OnCreation.class, newAgent );

			// Place has an arriving Agent
			eventDispatcher.queueAsync( OnArrival.class, curPlace );

			// Agent has arrived at a Place
			eventDispatcher.queueAsync( OnArrival.class, newAgent );
		}

		// invoke queued Agents OnCreation methods
		eventDispatcher.invokeQueuedAsync( OnCreation.class );

		// invoke queued Agents OnArrival methods
		eventDispatcher.invokeQueuedAsync( OnArrival.class );

	}

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
		PlacesBase curPlaces = MASSBase.getPlacesMap().get( placesHandle );

		if (GraphPlaces.class.isAssignableFrom(curPlaces.getClass())) {
			initForGraph((GraphPlaces) curPlaces, protoAgent, argument);
		} else {
			for (int i = 0; i < curPlaces.getPlacesSize(); i++ ) {

				// scan each place to see how many agents it can create
				Place curPlace = curPlaces.getPlaces()[i];
	
				// actual size:
				int[] placesSize = MASSBase.getPlacesMap().get(getPlacesHandle()).getSize();
	
				// create as many new agents as nColonists
				// change to protoAgent.map(int initPopulation, int[] size, int[] index, int offset)
				// added offset
				for ( int nColonists =
						protoAgent.map( initPopulation, placesSize,
								curPlace.getIndex(), curPlaces.getSize()[0] );
						nColonists > 0; nColonists--, localPopulation++ ) {		
	
					// agent instantiation and initialization
					Agent newAgent = null;
					try {
	
						agentInitAgentsHandle = handle;
						agentInitPlacesHandle = placesHandle;
						agentInitAgentId = currentAgentId++;
						agentInitParentId = -1; // no parent
						newAgent = objectFactory.getInstance(className, argument);
						newAgent.setAgentId( agentInitAgentId );
	
					} catch ( Exception e ) {
						// TODO - now what? What to do when there is an exception?
						MASS.getLogger().error( "Agents_base.constructor: {} not instaitated ", className, e );    			
					}
	
					newAgent.setPlace(curPlace);
	
					// store this agent in the bag of agents
					agents.add( newAgent );
	
					// register newAgent into curPlace
					curPlace.getAgents().add( newAgent );
	
					// register the new Agent with messaging provider
					MASS.getMessagingProvider().registerAgent( newAgent );
	
					// Agent has been created
					eventDispatcher.queueAsync( OnCreation.class, newAgent );
	
					// Place has an arriving Agent
					eventDispatcher.queueAsync( OnArrival.class, curPlace );
	
					// Agent has arrived at a Place
					eventDispatcher.queueAsync( OnArrival.class, newAgent );
				}
			}
		}

		// invoke queued Agents OnCreation methods
		eventDispatcher.invokeQueuedAsync( OnCreation.class );

		// invoke queued Agents OnArrival methods
		eventDispatcher.invokeQueuedAsync( OnArrival.class );

	}

	private void initForGraph(GraphPlaces graphPlaces, Agent protoAgent, Object argument) {
		// scan each place to see how many agents it can create
		Vector<VertexPlace> places = graphPlaces.getGraphPlaces();

		int graphSize = places.size();
		int[] placesSize = { graphSize };

		for (int i = 0; i < graphSize; i++) {
			// not sure what offset is supposed to be. The previous code was passing it the
			// graph size so I continue to do that here.
			int nColonists = protoAgent.map(initPopulation, placesSize, new int[]{i}, graphSize);

			// Create nColonists agents
			for (int j = 0; j < nColonists; j++) {
				Agent newAgent = null;
				try {
					agentInitAgentsHandle = handle;
					agentInitPlacesHandle = placesHandle;
					agentInitAgentId = currentAgentId++;
					agentInitParentId = -1; // no parent
					newAgent = objectFactory.getInstance(className, argument);
					newAgent.setAgentId(agentInitAgentId);

				} catch (Exception e) {
					MASS.getLogger().error("Agents_base.constructor::initForGraph: {} not instaitated ", className, e);
				}

				newAgent.setPlace(places.get(i));

				// store this agent in the bag of agents
				agents.add(newAgent);

				// increment local population
				localPopulation++;

				// register newAgent into curPlace
				places.get(i).getAgents().add(newAgent);

    			// register the new Agent with messaging provider
    			MASS.getMessagingProvider().registerAgent( newAgent );

				// Agent has been created
				eventDispatcher.queueAsync(OnCreation.class, newAgent);

				// Place has an arriving Agent
				eventDispatcher.queueAsync(OnArrival.class, places.get(i));

				// Agent has arrived at a Place
				eventDispatcher.queueAsync(OnArrival.class, newAgent);
			}
		}
	}

	public void callAll( int functionId, Object argument, int tid ) {

		MASS.getLogger().debug("*************** AgentBase callAll ****************");

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
				MASS.getLogger().debug( "in the other arg: " + argument);
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
					MASSBase.getAgentsMap().get( handle ).
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

				int argIndex = myIndex;

				//Use the Agents' callMethod to have it begin running
				if (argument != null) {
					( (Object[])MASSBase.getCurrentReturns() )[myIndex - 1] =
							tmpAgent.callMethod( functionId, argument[ myIndex - 1 ] );
				} else {
					// ----------- modified by Yuna
					( (Object[])MASSBase.getCurrentReturns() )[myIndex - 1] =
							tmpAgent.callMethod( functionId, null );
				}

				MASS.getLogger().debug( "Thread [" + tid + "]: (" + argIndex +
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
					MASSBase.getAgentsMap().get( handle ).
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
				Arrays.fill( dest_index, -1 );
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

		// Get the PlacesBase to access our agents fromvfor agent instantiation,
		// and our bag for Agent objects after they have finished processing
		PlacesBase evaluatedPlaces	= MASSBase.getPlacesMap().get( placesHandle );

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

					addAgent.setPlace(evaluationAgent.getPlace());

    				// Agent has been created

    				// register the new Agent with messaging provider
        			MASS.getMessagingProvider().registerAgent( addAgent );

        			// Call OnCreation event on the new Agent
    				eventDispatcher.queueAsync( OnCreation.class, addAgent );

					/** Agent population control work begins, execution order is important! **/

					// check if the agent is going to run in the system
					if (agentSpawnRequestManager.shouldAgentRunInTheSystem(addAgent, this.agents.size()))
					{
						// check if there is available agent id
						Integer availableAgentId = agentSpawnRequestManager.getNextAvailableAgentId();
						if (availableAgentId != null && availableAgentId > -1)
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

						// queue Place OnArrival method 
						eventDispatcher.queueAsync( OnArrival.class, addAgent.getPlace() );

						// queue Agent OnArrival method
						eventDispatcher.queueAsync( OnArrival.class, addAgent );

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
				agentSpawnRequestManager.addAvailableAgentId(evaluationAgent.getAgentId());

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
					PlacesBase curPlaces = MASSBase.getPlacesMap().get( placesHandle );
					int globalLinearIndex = MatrixUtilities.getLinearIndex( curPlaces.getSize(), agentSpawnRequest.getIndex() );

					// local destination
					int destinationLocalLinearIndex = globalLinearIndex - curPlaces.getLowerBoundary();
					// changes from Jonathan
					Place curPlace = null;
					if (curPlaces.getPlaces() == null) // added line jonathan Empty graph does not initialize places[]
						curPlace = ((GraphPlaces)curPlaces).getVertexPlace(destinationLocalLinearIndex);	// vertexPlace used instead of getPlaces[n] 
					else
						curPlace = curPlaces.getPlaces()[destinationLocalLinearIndex];
					// changes done

					// push this agent into the place and the entire agent bag.
					agentSpawnRequest.setPlace(curPlace);

					// Push the created agent into our bag for returns and
					// update the counter needed to keep track of our agents.
					agentSpawnRequest.getPlace().getAgents().add( agentSpawnRequest ); // auto sync
					this.agents.add( agentSpawnRequest );           // auto syn

	    			// register the new Agent with messaging provider
	    			MASS.getMessagingProvider().registerAgent( agentSpawnRequest );

					// init the Agent immediately
					try {
						eventDispatcher.invokeImmediate(OnCreation.class, agentSpawnRequest );
					} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {

						e.printStackTrace();
						MASS.getLogger().error( "Exception caught during initialization of serialized Agent", e );

					}

					// queue remaining events for the newly activated Agent
					eventDispatcher.queueAsync( OnArrival.class, curPlace );
					eventDispatcher.queueAsync( OnArrival.class, agentSpawnRequest );

				}

				// don't go down to migrate
				continue;

			}
			/****************************/

			/******* MIGRATE() CHECK *******/

			// first, check to see if the Agent is actually wanting to migrate
			// if not, no point doing anything else
			if ( !evaluationAgent.isMigrating() ) continue;

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

			if( !GraphPlaces.class.isAssignableFrom(evaluatedPlaces.getClass()) && destCoord[0] != -1 ) { 

				// destination valid
				int globalLinearIndex = MatrixUtilities.getLinearIndex( evaluatedPlaces.getSize(), destCoord );

				MASS.getLogger().debug( " linear = " + globalLinearIndex +
						" lower = " + evaluatedPlaces.getLowerBoundary()
						+ " upper = " + 
						evaluatedPlaces.getUpperBoundary() + ")" );

				Place oldPlace = evaluationAgent.getPlace();

				// Should remove the pointer object in the place that 
				// points to the migrating Agent
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

				if ( globalLinearIndex >= evaluatedPlaces.getLowerBoundary() &&
						globalLinearIndex <= evaluatedPlaces.getUpperBoundary() ) {

					// local destination

					// insert the migration Agent to a local destination place
					int destinationLocalLinearIndex 
					= globalLinearIndex - evaluatedPlaces.getLowerBoundary();

					MASS.getLogger().debug( "destinationLocalLinerIndex = {}", destinationLocalLinearIndex );

					// queue up OnDeparture events
					eventDispatcher.queueAsync( OnDeparture.class, evaluationAgent );
					eventDispatcher.queueAsync( OnDeparture.class, oldPlace );

					evaluationAgent.setPlace(MASSBase.getPlacesMap().
							get( placesHandle ).
							getPlaces()[destinationLocalLinearIndex]);

					evaluationAgent.getPlace().getAgents().add( evaluationAgent );

					MASS.getLogger().debug( "evaluationAgent " + 
							evaluationAgent.getAgentId() +
							" was inserted into the destPlace[" +
							evaluationAgent.getPlace().getIndex()[0] + "].." );

					// if the agent actually moved, queue up OnArrival events
					eventDispatcher.queueAsync( OnArrival.class, evaluationAgent.getPlace() );
					eventDispatcher.queueAsync( OnArrival.class, evaluationAgent );

				} 

				else {

					// remote destination
					MASS.getLogger().debug("going to a remoteNode");
					// remove evaluationAgent from AgentList
					agents.remove( myIndex - 1 );

					// find the destination node
					int destRank = evaluatedPlaces.getRankFromGlobalLinearIndex( globalLinearIndex );

					// OnDeparture events must be run immediately before agent is serialized and moved
					try {

						eventDispatcher.invokeImmediate( OnDeparture.class, oldPlace );
						eventDispatcher.invokeImmediate( OnDeparture.class, evaluationAgent );

					} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
						MASS.getLogger().error("Exception thrown when invoking OnDeparture events!", e);
					}

					// relinquish the old place
					evaluationAgent.setPlace(null);

    				// Agent should no longer receieve messages from this node
    				MASS.getMessagingProvider().unregisterAgent( evaluationAgent );

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

			} else if (GraphPlaces.class.isAssignableFrom(evaluatedPlaces.getClass())) {
				GraphPlaces graphPlaces = (GraphPlaces) evaluatedPlaces;

				int globalLinearIndex = evaluationAgent.getIndex()[0];
				int nodeId = graphPlaces.getOwnerID(globalLinearIndex);

				if (nodeId == MASSBase.getMyPid()) {
					// local migration
					int localIndex = globalLinearIndex / MASS.getSystemSize();
					Place oldPlace = evaluationAgent.getPlace();

					if (oldPlace.getAgents().remove(evaluationAgent) == false) {
						// should not happen
						String errorMessage = "evaluationAgent {}" +
								evaluationAgent.getAgentId()
						+ " couldn't been found in " +
						"the old place!";

						MASS.getLogger().error(errorMessage);

						// throw it back to our new fatal exception handler
						throw new RuntimeException(errorMessage);
					}

					evaluationAgent.setPlace(graphPlaces.places.get(localIndex));

					evaluationAgent.getPlace().getAgents().add(evaluationAgent);
				} else {
					// remote migration
					agents.remove(myIndex - 1);

					evaluationAgent.setPlace(null);

					AgentMigrationRequest request = new AgentMigrationRequest(globalLinearIndex, evaluationAgent);

					Vector<AgentMigrationRequest> migrationRequestVector = MASSBase.getMigrationRequests().get(nodeId);

					synchronized (migrationRequestVector) {
						migrationRequestVector.add(request);
					}
				}
			} else {
				String destinationString = Arrays.stream(evaluationAgent.getIndex()).mapToObj(Integer::toString).collect(Collectors.joining(", "));

				MASS.getLogger().error(" to destination (" + destinationString + ") invalid");

			}
		} // end of while( true )

		// When while loop finishes, all threads must barrier and tid = 0
		// must adjust AgentList.
		MThread.barrierThreads( tid );

		if ( tid == 0 ) agents.reduce( );

		// all threads must barrier synchronize here.
		MThread.barrierThreads( tid );
		if ( tid == 0 ) {

			// remember the earliest clock cycle value on any node which will trigger an event
			long nextClockTrigger = Long.MAX_VALUE;

			MASS.getLogger().debug( "tid[{}] now enters processAgentMigrationRequest", tid );

			// the main thread spawns as many communication threads as the 
			// number of remote computing nodes and let each invoke 
			// processAgentMigrationReq. 

			// communication thread id
			ProcessAgentMigrationRequest[] thread_ref
			= new ProcessAgentMigrationRequest[MASSBase.getSystemSize()]; 
			for ( int rank = 0; rank < MASSBase.getSystemSize(); rank++ ) {

				if ( rank == MASSBase.getMyPid() ) // don't communicate with myself
					continue;

				// start a communication thread
				MASS.getLogger().debug("handle:" + evaluatedPlaces.getHandle());
				thread_ref[rank] = new ProcessAgentMigrationRequest( rank, handle, evaluatedPlaces.getHandle() );
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

				// get the next clock value at which this remote node will trigger an event
				if ( thread_ref[ rank ].getNextEventTrigger() > 0 && thread_ref[ rank ].getNextEventTrigger() < nextClockTrigger ) {
					nextClockTrigger = thread_ref[ rank ].getNextEventTrigger();
				}

			}

			localPopulation = agents.size_unreduced( );

			// fire queued events
			eventDispatcher.invokeQueuedAsync( OnCreation.class );
			eventDispatcher.invokeQueuedAsync( OnDeparture.class );
			eventDispatcher.invokeQueuedAsync( OnArrival.class );

			// what should the next value be for the Global Logical Clock?
			if ( nextClockTrigger > clock.getNextEventTrigger() ) nextClockTrigger = clock.getNextEventTrigger();
			if ( nextClockTrigger > ( clock.getValue() + 1 ) ) {

				// broadcast new value to all nodes
				Message m = new Message( Message.ACTION_TYPE.CLOCK_SET_VALUE, nextClockTrigger );
				MASS.getExchange().broadcastMessage( m );		

				// we can fast-forward to the next trigger (local)
				clock.setValue( nextClockTrigger );

			}

			else {

				// can't fast-forward - just increment Global Logical Clock value
				clock.increment();

			}

			MASS.getLogger().debug( "Agents_base.manageAll completed: localPopulation = {}", localPopulation );

		}

		else {

			MASS.getLogger().debug( "pthread_self[" + Thread.currentThread( ) +
					"] tid[" + tid + 
					"] skips processAgentMigrationRequest" );

		}

	}

	// manageAll method for Space class ------------------ modified by Yuna
	public void manageAll_space( int tid ) {

		MASS.getLogger().debug("******************* MANAGE ALL SPACE ***********************");

		//Get the PlacesBase to access our agents fromvfor agent instantiation,
		//and our bag for Agent objects after they have finished processing
		SpacePlacesBase evaluatedPlaces	= (SpacePlacesBase) MASSBase.getPlacesMap().get( placesHandle );
		MASS.getLogger().debug("MANAGE_ALL evaluatedPlace: " + evaluatedPlaces.getHandle());

		// Spawn, Kill, Migrate. Check in that order throughout the bag of agents  sequentially.
		while ( true ) {

			int myIndex; // each thread's agent index
			SpaceAgent evaluationAgent = null;
			SpacePlace evaluationPlace = null;
			synchronized( this ) {   			
				if ( ( myIndex = MThread.getAgentBagSize() ) == 0 ) break;
				// Grab the last agent and remove it for processing. 
				myIndex = MThread.getAgentBagSize();
				MThread.setAgentBagSize(myIndex - 1);
				evaluationAgent = (SpaceAgent) agents.get( myIndex - 1 );
				MASS.getLogger().debug( "Agents_base.manageALL: Thread " + tid + " picked up " + evaluationAgent.getAgentId() );
			}

			int argumentcounter = 0;

			//If the spawn's newChildren field is set to anything higher than 
			//zero, we need to create newChildren's worth of Agents in the current location.

			//******* SPAWN() CHECK *******
			MASS.getLogger().debug("******* SPAWN() CHECK *******");
			int childrenCounter = evaluationAgent.getNewChildren();
			MASS.getLogger().debug( "agent " + evaluationAgent.getAgentId() + "'s childrenCounter = " + childrenCounter );
			while ( childrenCounter > 0 ) {
				MASS.getLogger().debug( "Agent_base.manageALL: Thread " + tid + " will spawn a child of agent " + 
						evaluationAgent.getAgentId() + "...arguments.size( ) = " + evaluationAgent.getArguments().length +
						", argumentcounter = " + argumentcounter );

				SpaceAgent addAgent = null;
				Object dummyArgument = new Object();
				try {
					agentInitAgentsHandle = this.handle;
					agentInitPlacesHandle = this.placesHandle;
					agentInitParentId = evaluationAgent.getAgentId();
					synchronized( this ) {

						// validate the correspondance of arguments and argumentcounter
						addAgent = (SpaceAgent) ((evaluationAgent.getArguments().length > argumentcounter) ?
								// yes: this child agent should recieve an argument.
								objectFactory.getInstance(className, evaluationAgent.getArguments()[argumentcounter++])
								: objectFactory.getInstance(className, dummyArgument));
					}

					addAgent.setPlace(evaluationAgent.getPlace());

					// Agent has been created
					eventDispatcher.queueAsync( OnCreation.class, addAgent );

					// Agent population control work begins, execution order is important!
					// check if the agent is going to run in the system
					if (agentSpawnRequestManager.shouldAgentRunInTheSystem(addAgent, this.agents.size())) {
						// check if there is available agent id
						Integer availableAgentId = agentSpawnRequestManager.getNextAvailableAgentId();

						if (availableAgentId > -1) {
							addAgent.setAgentId(availableAgentId); 
						}  else {   // assign a never used id
							addAgent.setAgentId(this.currentAgentId++);
						}

						// Push the created agent into our bag for returns and
						// update the counter needed to keep track of our agents.
						SpacePlace curPlace = (SpacePlace) addAgent.getPlace();
						curPlace.addAgent(addAgent, addAgent.getSubIndex());  
						this.agents.add( addAgent );           // auto syn
					}                                                  

					// queue Place OnArrival method 
					eventDispatcher.queueAsync( OnArrival.class, addAgent.getPlace() );

					// queue Agent OnArrival method
					eventDispatcher.queueAsync( OnArrival.class, addAgent );

				} catch ( Exception e ) {
					// TODO - now what? What to do when an exception is thrown?
					MASS.getLogger().error( "Agents_base.manageAll: {} not instantiated", this.className, e );
				}

				//Decrement the newChildren counter once an Agent has been spawned
				evaluationAgent.setNewChildren(evaluationAgent.getNewChildren() - 1);
				childrenCounter--;
				MASS.getLogger().debug( "Agent_base.manageALL: Thread " + tid + " spawned a child of agent " + 
						evaluationAgent.getAgentId() + " and put the child " + addAgent.getAgentId() + " child into retBag." );
				MASS.getLogger().debug("newAgent id = " + addAgent.getAgentId() + ", isMigrating = " + addAgent.isMigrate_toString() + ".");

				//every time we spawn a new agent, we should check if there is available index first!!!

			}

			//******* KILL() CHECK *******
			MASS.getLogger().debug("******* KILL() CHECK *******");  
			MASS.getLogger().debug( "Agent_base.manageALL: Thread " + tid + " check " + evaluationAgent.getAgentId() + "'s alive = " + evaluationAgent.isAlive() );

			if ( evaluationAgent.isAlive() == false ) {

				// Get the place in which evaluationAgent is 'stored' in
				evaluationPlace = (SpacePlace) evaluationAgent.getPlace();

				//remove the agent from this place  				
				evaluationPlace.removeAgent(evaluationAgent, evaluationAgent.getSubIndex());

				// remove from AgentList, too!
				agents.remove( myIndex - 1 );
				int n = myIndex - 1;

				//** Agent population control work begins, execution order is important! **
				// every time we kill an agent, we should add its id to the available ids queue
				agentSpawnRequestManager.addAvailableAgentId(evaluationAgent.getAgentId());

				// then we check if there is any agent spawn request
				Agent agentSpawnRequest = agentSpawnRequestManager.getNextAgentSpawnRequest();
				if (agentSpawnRequest != null) {
					// TODO VERIFY IF INDEX AND PLACE INFORMATION ARE CORRECT!!
					// check if there is available agent id
					Integer availableAgentId = agentSpawnRequestManager.getNextAvailableAgentId();

					if (availableAgentId > -1) {
						agentSpawnRequest.setAgentId(availableAgentId);
					} else {  // assign a never used id
						agentSpawnRequest.setAgentId(this.currentAgentId++);
					}

					// retrieve the corresponding places
					SpacePlacesBase curPlaces = (SpacePlacesBase) MASSBase.getPlacesMap().get( placesHandle );
					int globalLinearIndex = MatrixUtilities.getLinearIndex( curPlaces.getSize(), agentSpawnRequest.getIndex() );

					// local destination
					int destinationLocalLinearIndex = globalLinearIndex - curPlaces.getLowerBoundary();
					SpacePlace curPlace = (SpacePlace)(curPlaces.getPlaces()[destinationLocalLinearIndex]);

					// push this agent into the place and the entire agent bag.
					agentSpawnRequest.setPlace(curPlace);

					// Push the created agent into our bag for returns and
					// update the counter needed to keep track of our agents.
					agentSpawnRequest.getPlace().getAgents().add( agentSpawnRequest ); // auto sync
					this.agents.add( agentSpawnRequest );           // auto syn

					// init the Agent immediately
					try {
						eventDispatcher.invokeImmediate(OnCreation.class, agentSpawnRequest );
					} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {		
						e.printStackTrace();
						MASS.getLogger().error( "Exception caught during initialization of serialized Agent", e );	
					}
					// queue remaining events for the newly activated Agent
					eventDispatcher.queueAsync( OnArrival.class, curPlace );
					eventDispatcher.queueAsync( OnArrival.class, agentSpawnRequest );	
				}
				// don't go down to migrate
				continue;
			}

			//****************************
			//******* MIGRATE() CHECK *******

			// first, check to see if the Agent is actually wanting to migrate
			// if not, no point doing anything else
			//System.out.println("BEFORE MIGRATE---------");
			//System.out.println(evaluationAgent.toString());
			MASS.getLogger().debug("******* MIGRATE() CHECK , agent id = " + evaluationAgent.getAgentId() + " *******");

			//System.out.println(evaluationAgent.toString());
			if ( !evaluationAgent.isMigrating() ) continue;

			evaluationPlace = (SpacePlace)evaluationAgent.getPlace();
			//Iterate over all dimensions of the agent to check its location
			//against that of its place. If they are the same, return back.

			double[] currentCoordinates = evaluationAgent.getCurrentCoordinates();
			double[] destCoordinates = evaluationAgent.getNextCoordinates();
			int[] destIndex = SpaceUtilities.findDestIndex(destCoordinates, evaluatedPlaces);
			int[] currentIndex = SpaceUtilities.findDestIndex(currentCoordinates, evaluatedPlaces);

			// calculate the old sub-index and new sub-index in place        		
			int granularity = evaluationPlace.getGranularity();
			double[] interval = evaluationPlace.getInterval();
			double[] subInterval = evaluationPlace.getSubInterval();
			double[] min = evaluatedPlaces.getMin();  //min value of SpacePlaces

			int[] oldSubIndex = SpaceUtilities.findSubIndex(currentCoordinates, currentIndex, interval, subInterval, min);
			int[] newSubIndex = SpaceUtilities.findSubIndex(destCoordinates, destIndex, interval, subInterval, min);


			MASS.getLogger().debug( "pthread_self[" + Thread.currentThread( ) + "tid[" + tid + "]: calls from" +
					"[" + evaluationAgent.getIndex()[0] + "," + evaluationAgent.getIndex()[1] + "]," + 
					" (destCoordinates = [" + destCoordinates[0] + "," + destCoordinates[1] + "])" );

			/*
			MASS.getLogger().debug("CURRENT: coordinates[" + currentCoordinates[0] + "," + currentCoordinates[1] + "], index[" + 
				currentIndex[0] + "," + currentIndex[1] + "], subIndex[" + oldSubIndex[0] + "," + oldSubIndex[1] + "]");

			MASS.getLogger().debug("DESTINATION: coordinates[" + destCoordinates[0] + "," + destCoordinates[1] + "], index[" + 
				destIndex[0] + "," + destIndex[1] + "], subIndex[" + newSubIndex[0] + "," + newSubIndex[1] + "]");
			 */			

			if (destIndex[0] != -1) {

				// destination valid
				int destIndexLinear = MatrixUtilities.getLinearIndex(evaluatedPlaces.getSize(), destIndex);
				MASS.getLogger().debug( " linear = " + destIndexLinear + " lower = " + evaluatedPlaces.getLowerBoundary()
				+ " upper = " + evaluatedPlaces.getUpperBoundary() + ")" );


				SpacePlace oldPlace = (SpacePlace) (evaluationAgent.getPlace());

				//should remove the pointer object in the place that points to the migrating agent
				//local destination
				if (destIndexLinear >= evaluatedPlaces.getLowerBoundary() && destIndexLinear <= evaluatedPlaces.getUpperBoundary()) {

					if (SpaceUtilities.sameIndex(destIndex, currentIndex)) {
						//MASS.getLogger().debug("same place: ========= agents map ===========");
						//MASS.getLogger().debug(oldPlace.agentMap_toString());
						// if destination index is the same as current index, agent only move within the place, do not need to update index
						// only update sub-index

						if (!evaluationPlace.updateAgent(evaluationAgent, oldSubIndex, newSubIndex)){

							MASS.getLogger().error( "evaluationAgent {}" + evaluationAgent.getAgentId() + " couldn't been found in " + "the old place!" );
							System.exit( -1 );
						}

						//update subIndex
						evaluationAgent.setSubIndex(newSubIndex);

						//update coordinates
						evaluationAgent.setCurrentCoordinates(destCoordinates);


					} else {
						//MASS.getLogger().debug("different places: ========= agents map ===========");
						//MASS.getLogger().debug(oldPlace.agentMap_toString());

						//else remove agent from old place and add it to new place 
						if (!evaluationPlace.removeAgent(evaluationAgent, oldSubIndex)) {  // remove old sub-index
							// if could not be removed successfully (should not happen)
							MASS.getLogger().error( "evaluationAgent {}" + evaluationAgent.getAgentId() + " couldn't been found in " + "the old place!" );
							System.exit( -1 );
						}

						MASS.getLogger().debug( "evaluationAgent " + evaluationAgent.getAgentId() + " was removed from the oldPlace[" + currentIndex[0] + "].." );

						// add agent to agentsMap in destination place
						SpacePlace newPlace = (SpacePlace) (evaluatedPlaces.getPlaces()[destIndexLinear - evaluatedPlaces.getLowerBoundary()]);   // get new place
						MASS.getLogger().debug( "destinationLocalLinerIndex = {}", destIndexLinear);
						evaluationAgent.setPlace(newPlace);  // set new place
						evaluationAgent.setIndex(destIndex);  // set new index
						evaluationAgent.setSubIndex(newSubIndex);
						evaluationAgent.setCurrentCoordinates(destCoordinates);
						((SpacePlace)(evaluationAgent.getPlace())).addAgent(evaluationAgent, newSubIndex);   // add new sub-index to new place

						MASS.getLogger().debug( "evaluationAgent " +	evaluationAgent.getAgentId() +
								" was inserted into the destPlace[" + evaluationAgent.getPlace().getIndex()[0] + "].."
								+ " with sub-index = " + evaluationAgent.getSubIndex()[0] + ", " + 
								evaluationAgent.getSubIndex()[1] + "]."); 

					} 

				} else {

					// remote destination
					// remove evaluationAgent from AgentList
					agents.remove( myIndex - 1 );
					((SpacePlace)(evaluationAgent.getPlace())).removeAgent(evaluationAgent, oldSubIndex); 

					// find the destination node
					int destRank = evaluatedPlaces.getRankFromGlobalLinearIndex(destIndexLinear);

					// relinquish the old place
					evaluationAgent.setPlace(null);

					// create a request
					AgentMigrationRequest request = new AgentMigrationRequest(destIndexLinear, evaluationAgent );

					MASS.getLogger().debug( "AgentMigrationRequest request = {}", request );

					// enqueue the request to this node.map
					Vector<AgentMigrationRequest> migrationReqList	= MASSBase.getMigrationRequests().get( destRank );

					synchronized( migrationReqList ) {
						migrationReqList.add( request );
						MASS.getLogger().debug( "remoteRequest[" + destRank +	"].add:" + " dst = " + destIndexLinear);
					}
				}
			} else {
				MASS.getLogger().error( " to destination invalid" );	
			}

		} // end of while( true )

		// When while loop finishes, all threads must barrier and tid = 0
		// must adjust AgentList.
		MThread.barrierThreads( tid );
		if ( tid == 0 ) agents.reduce( );
		// all threads must barrier synchronize here.
		MThread.barrierThreads( tid ); 
		if ( tid == 0 ) {
			MASS.getLogger().debug( "tid[{}] now enters processSpaceAgentMigrationRequest", tid );

			// the main thread spawns as many communication threads as the 
			// number of remote computing nodes and let each invoke 
			// processSpaceAgentMigrationReq. 

			// communication thread id
			ProcessSpaceAgentMigrationRequest[] thread_ref = new ProcessSpaceAgentMigrationRequest[MASSBase.getSystemSize()]; 
			for ( int rank = 0; rank < MASSBase.getSystemSize(); rank++ ) {
				if ( rank == MASSBase.getMyPid()) {
					continue;    // don't communicate with myself
				}
				// start a communication thread
				thread_ref[rank] = new ProcessSpaceAgentMigrationRequest( rank, handle, evaluatedPlaces.getHandle() );
				thread_ref[rank].start( );
				MASS.getLogger().debug( "Agents_base.manageAll will start " + "processSpaceAgentMigrationRequest thread[" + rank + "] = " + thread_ref[rank]);
			}

			// wait for all the communication threads to be terminated
			for ( int rank = MASSBase.getSystemSize() - 1; rank >= 0; rank-- ) {
				MASS.getLogger().debug( "Agents_base.manageAll will join " + "processSpaceAgentMigrationRequest A thread["
						+ rank + "] = " + thread_ref[rank] + " myPid = " + MASSBase.getMyPid() );

				if ( rank == MASSBase.getMyPid() )  continue;      // don't communicate with myself

				MASS.getLogger().debug( "Agents_base.manageAll will join " + "processSpaceAgentMigrationRequest B thread[" + rank + "] = " + thread_ref[rank] );
				try {
					thread_ref[rank].join( );
				}  catch ( Exception e ) {
					MASS.getLogger().error("Unable to join rank!", e);
				}
				MASS.getLogger().debug( "Agents_base.manageAll joined " + "processSpaceAgentMigrationRequest C thread[" + rank + "] = " + thread_ref[rank] );
			}
			localPopulation = agents.size_unreduced( );

			MASS.getLogger().debug( "Agents_base.manageAll completed: localPopulation = {}", localPopulation );
		}  else {
			MASS.getLogger().debug( "pthread_self[" + Thread.currentThread( ) + "] tid[" + tid + "] skips processSpaceAgentMigrationRequest" );
		}   

	}

	/**
	 * Get the number of Agents located on this node
	 * @return Number of local Agents
	 */
	public int nLocalAgents( ) {
		return localPopulation; 
	}

	/**
	 * Trigger exchange of all outgoing and incoming messages to Agents
	 */
	public void exchangeAll() {

		// transmit outgoing messages
		MASS.getMessagingProvider().flushAgentMessages();

		// execute methods queued by incoming messages
		MASS.getEventDispatcher().invokeQueuedAsync( OnMessage.class );

	}

	private class ProcessAgentMigrationRequest extends Thread {

		private int destRank;
		private int agentHandle;
		private int placeHandle;
		private long nextClockTrigger;

		public ProcessAgentMigrationRequest( int destRank, int agentHandle, int placeHandle ) {
			this.destRank = destRank;
			this.agentHandle = agentHandle;
			this.placeHandle = placeHandle;
		}

		@SuppressWarnings("unused")
		public void run( ) {

			MASS.getLogger().debug( "pthread_self[" + Thread.currentThread( ) +
					"] rank[" + destRank + 
					"]: starts processAgentMigrationRequest" );

			// pick up the next rank to process
			Vector<AgentMigrationRequest> orgRequest = MASSBase.getMigrationRequests().get( destRank );

			// now compose and send a message by a child
			Message messageToDest = 
					new Message( Message.ACTION_TYPE.
							AGENTS_MIGRATION_REMOTE_REQUEST,
							agentHandle, placeHandle, orgRequest );

			MASS.getLogger().debug( "tid[" + destRank + 
					"] made messageToDest to rank: " + destRank ); 


			new Thread( () -> {

				// send the message
				MASSBase.getExchange().sendMessage( destRank, messageToDest ); 

				// at this point, the message must be exchanged
				orgRequest.clear( );

			}).start();

			// receive a message by myself
			Message messageFromSrc = MASSBase.getExchange().receiveMessage( destRank );

			MASS.getLogger().debug( "Message exchange completed for rank [" + destRank + "]" );

			// what is the next clock value where an event will occur on that node?
			if ( messageFromSrc.getArgument() != null ) {

				// only if the argument returned is a long will we assume its the next trigger value
				if ( messageFromSrc.getArgument() instanceof Long ) {
					this.nextClockTrigger = (long) messageFromSrc.getArgument();
				}

			}

			// process a message
			Vector<AgentMigrationRequest> receivedRequest 
			= messageFromSrc.getMigrationReqList( );

			int agentsHandle = messageFromSrc.getHandle( );
			int placesHandle = messageFromSrc.getDestHandle( );
			PlacesBase dstPlaces = MASSBase.getPlacesMap().
					get( placesHandle );

			MASS.getLogger().debug( "request from rank[" + destRank + "] = " + 
					receivedRequest + " size( ) = " + 
					receivedRequest.size( ) );

			// retrieve agents from receiveRequest
			while( receivedRequest.size( ) > 0 ) {
				// TODO investigate
				AgentMigrationRequest request = 
						receivedRequest.remove( receivedRequest.size( ) - 1 );

				int globalLinearIndex = request.destGlobalLinearIndex;
				if (GraphPlaces.class.isAssignableFrom(dstPlaces.getClass())) {
					boolean found = true;
					Agent evaluationAgent = request.agent;
					GraphPlaces graphPlaces = (GraphPlaces) dstPlaces;
					int localIndex = globalLinearIndex / MASS.getSystemSize();

					// local migration
					Place oldPlace = evaluationAgent.getPlace();
					if(oldPlace != null)
						found = oldPlace.getAgents().remove(evaluationAgent);

					// changes include found init and usage Jonathan 
					if (found == false) {
						// should not happen
						String errorMessage = "evaluationAgent {}" +
								evaluationAgent.getAgentId()
						+ " couldn't been found in " +
						"the old place!";

						MASS.getLogger().error(errorMessage);

						// throw it back to our new fatal exception handler
						throw new RuntimeException(errorMessage);
					}

					evaluationAgent.setPlace(graphPlaces.places.get(localIndex));
					evaluationAgent.getPlace().getAgents().add(evaluationAgent);
					agents.add(evaluationAgent);

					// invoke OnArrival events immediately
					try {
						eventDispatcher.invokeImmediate(OnArrival.class, graphPlaces.places.get(localIndex));
						eventDispatcher.invokeImmediate(OnArrival.class, evaluationAgent);
					} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
						MASS.getLogger().error("Exception thrown when invoking OnArrival events!", e);
					}
				} else {
					Agent agent = request.agent;

					// local destination

					int destinationLocalLinearIndex 
					= globalLinearIndex - dstPlaces.getLowerBoundary();

					MASS.getLogger().debug( " dstLocal = " + destinationLocalLinearIndex + " g: " + globalLinearIndex + " d: " + dstPlaces.getLowerBoundary());

					// modified by jonathan
					Place dstPlace = null;
					if(dstPlaces.getPlaces() != null)
						dstPlace = dstPlaces.getPlaces()[destinationLocalLinearIndex];
					// end of changes

					MASS.getLogger().debug( "dsrLocal is null:  " +  (dstPlace == null));
					// push this agent into the place and the entire agent bag.
					agent.setPlace(dstPlace);
					dstPlace.getAgents().add( agent ); // auto sync
					agents.add( agent );          // auto sync

					// invoke OnArrival events immediately
					try {

						eventDispatcher.invokeImmediate(OnArrival.class, dstPlace);
						eventDispatcher.invokeImmediate(OnArrival.class, agent);

					} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
						MASS.getLogger().error("Exception thrown when invoking OnArrival events!", e);
					}
				}

			}

			MASS.getLogger().debug( "pthread_self[" + Thread.currentThread( ) +
					"] retreive agents from rank[" + destRank + 
					"] completed" );

		}

		/**
		 * Get the next clock value at which an event will be triggered on this remote node
		 * @return The next value of the clock at which an event will occur
		 */
		public long getNextEventTrigger() {
			return this.nextClockTrigger;
		}

	}

	private class ProcessSpaceAgentMigrationRequest extends Thread {

		private int destRank;
		private int agentHandle;
		private int placeHandle;
		private long nextClockTrigger;

		public ProcessSpaceAgentMigrationRequest( int destRank, int agentHandle, int placeHandle ) {
			this.destRank = destRank;
			this.agentHandle = agentHandle;
			this.placeHandle = placeHandle;
		}

		@SuppressWarnings("unused")
		public void run( ) {

			MASS.getLogger().debug( "pthread_self[" + Thread.currentThread( ) +
					"] rank[" + destRank + "]: starts processAgentMigrationRequest" );

			// pick up the next rank to process
			Vector<AgentMigrationRequest> orgRequest = MASSBase.getMigrationRequests().get( destRank );

			// now compose and send a message by a child
			Message messageToDest = new Message( Message.ACTION_TYPE.AGENTS_MIGRATION_REMOTE_REQUEST, agentHandle, placeHandle, orgRequest );

			MASS.getLogger().debug( "tid[" + destRank + "] made messageToDest to rank: " + destRank ); 


			new Thread( () -> {

				// send the message
				MASSBase.getExchange().sendMessage( destRank, messageToDest ); 

				// at this point, the message must be exchanged
				orgRequest.clear( );

			}).start();

			// receive a message by myself
			Message messageFromSrc = MASSBase.getExchange().receiveMessage( destRank );

			MASS.getLogger().debug( "Message exchange completed for rank [" + destRank + "]" );

			// what is the next clock value where an event will occur on that node?
			if ( messageFromSrc.getArgument() != null ) {

				// only if the argument returned is a long will we assume its the next trigger value
				if ( messageFromSrc.getArgument() instanceof Long ) {
					this.nextClockTrigger = (long) messageFromSrc.getArgument();
				}

			}

			// process a message
			Vector<AgentMigrationRequest> receivedRequest = messageFromSrc.getMigrationReqList( );

			int agentsHandle = messageFromSrc.getHandle( );
			int placesHandle = messageFromSrc.getDestHandle( );
			SpacePlacesBase dstPlaces = (SpacePlacesBase) (MASSBase.getPlacesMap().get(placesHandle));

			MASS.getLogger().debug( "request from rank[" + destRank + "] = " + 
					receivedRequest + " size( ) = " + receivedRequest.size( ) );

			// retrieve agents from receiveRequest
			int totalRequest = receivedRequest.size( );
			while( receivedRequest.size( ) > 0 ) {
				MASS.getLogger().debug("Migration request " + (totalRequest - receivedRequest.size() + 1) + ": ");
				// TODO investigate
				AgentMigrationRequest request = receivedRequest.remove( receivedRequest.size( ) - 1 );

				int globalLinearIndex = request.destGlobalLinearIndex;
				SpaceAgent agent = (SpaceAgent) request.agent;

				// local destination
				int destinationLocalLinearIndex = globalLinearIndex - dstPlaces.getLowerBoundary();

				MASS.getLogger().debug( " dstLocal = {}", destinationLocalLinearIndex );

				SpacePlace dstPlace = (SpacePlace) (dstPlaces.getPlaces()[destinationLocalLinearIndex]);

				// push this agent into the place and the entire agent bag.
				agent.setPlace(dstPlace);
				agent.setIndex(dstPlace.getIndex());

				//set current coordinates
				double[] destCoordinates = agent.getNextCoordinates();
				agent.setCurrentCoordinates(destCoordinates);

				int[] destIndex = SpaceUtilities.findDestIndex(destCoordinates, dstPlaces);
				int[] destSubIndex = SpaceUtilities.findSubIndex(destCoordinates, destIndex, dstPlaces.getInterval(), 
						dstPlaces.getSubInterval(), dstPlaces.getMin());
				agent.setSubIndex(destSubIndex);
				agents.add( agent );          // auto sync

				dstPlace.addAgent(agent, destSubIndex);   // add new sub-index to new place  

				// invoke OnArrival events immediately 
				try {

					eventDispatcher.invokeImmediate( OnArrival.class,  dstPlace );
					eventDispatcher.invokeImmediate( OnArrival.class,  agent );

				} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
					MASS.getLogger().error("Exception thrown when invoking OnArrival events!", e);
				}

				MASS.getLogger().debug( "Agent id = " + agent.getAgentId() + " migrate to remote Place coordinates = [" + agent.getCurrentCoordinates()[0] + "," + 
						agent.getCurrentCoordinates()[1] + "]");
				//MASS.getLogger().debug(" index = [" + agent.getIndex()[0] + "," + agent.getIndex()[1] + "]");
				//MASS.getLogger().debug(" subIndex = [" + agent.getSubIndex()[0] + "," + agent.getSubIndex()[1] + "]");	
			}


			MASS.getLogger().debug( "pthread_self[" + Thread.currentThread( ) +
					"] retreive agents from rank[" + destRank + "] completed" );

		}

		/**
		 * Get the next clock value at which an event will be triggered on this remote node
		 * @return The next value of the clock at which an event will occur
		 */
		public long getNextEventTrigger() {
			return this.nextClockTrigger;
		}

	}

}