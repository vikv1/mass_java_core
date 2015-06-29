package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Agent implements Serializable {

	/** 
	 * Is this agent’s identifier. It is calculated as: 
	 * the sequence number * the size of this agent’s belonging 
	 * matrix + the index of the current place when all
	 * places are flattened to a single dimensional array.
	 */
	private int agentId;

	/**
	 * The current place where this Agent resides
	 */
	private Place place = null;
	
	/**
	 * Is an array that maintains the coordinates of where this agent resides.
	 * Intuitively, index[0], index[1], and index[2] correspond to coordinates of
	 * x, y, and z, or those of i, j, and k.
	 */
	private int[] index = null;
	
	/** 
	 * Is true while this agent is active. Once it is set false, this agent is 
	 * killed upon a next call to Agents.manageAll( ).
	 */
	private boolean alive = true;
	
	/**
	 * Is the number of new children created by this agent upon a next call to
	 * Agents.manageAll( ).
	 */
	private int newChildren = 0;
	
	/** 
	 * Is an array of arguments, each passed to a different new child.
	 */
	private Object[] arguments = null;

	// Async
	private volatile int asyncFuncListIndex = 0; // next func in the async func list to execute
	private Object[] asyncResults;
	private volatile int asyncResultsIndex = 0; // next index to be inserted
	private Object asyncArgument;
	private volatile Agents_base parentAgents;

	// true to signal a thread to stop processing this Agent's asyncFuncList
	// this happens in kill & migrate case
	private volatile boolean hasAlreadyRemoteMigrated = false;
	private volatile boolean putBackToAsyncQueue = false;

	/**
	 *  backward compatibility with agentbag,
	 *  together with myAsyncPid keep track of
	 *  the original position of the agent,
	 *  set at the beginning of callAllAsync and not changed
	 *  throughout execution
	 */
	private int myOriginalAsyncIndex;

	/**
	 * The current index of this agent in agent list
	 * change when remote migrate, used for killing, async queue access
	 */
	private volatile int myCurrentIndex;

	/**
	 * Original Pid before execution
	 */
	private int myAsyncOriginalPid;

	private int autoMigrationStartingIndex;

	public Agent ( ) {
		agentId = Agents.getAgentInitAgentId();
	}

	/**
	 * Is called from Agents.callAll. It invokes the function specified with
	 * functionId as passing arguments to this function. A user-derived Agent
	 * class must implement this method.
	 * @param functionId
	 * @param argument
	 * @return 
	 */  	
	public Object callMethod( int functionId, Object argument ) {
		 return null;
	 }

	public int getAgentId() {
		return agentId;
	}

	public Object[] getArguments() {
		return arguments;
	}

	/**
	 * Get debug data from the agent 
	 * @return Debug data
	 */
	public Object getDebugData(){
		Integer id = new Integer(agentId);
		return (Object)id;
	}

	public int[] getIndex() {
		return index;
	}

	public int getNewChildren() {
		return newChildren;
	}

	public Place getPlace() {
		return place;
	}

	public boolean isAlive() {
		return alive;
	}

	/**
	 * Terminates the calling agent upon a next call to Agents.manageAll( ).
	 * More specifically, kill( ) sets the "alive" variable false.
	 */
	public void kill( ) {
		alive = false;
	}

	public void killAsync() {

		kill();
		hasAlreadyRemoteMigrated = true;

		synchronized(Mthread.class){
			Mthread.setAgentBagSize(Mthread.getAgentBagSize() - 1);
		}

		// remove the agent from this place
		getPlace().getAgents().remove( this );

		// remove from AgentList, too!
		// unlike sync myAsyncIndex start from 0
		/** TO DO IN callAllAsyncLoop only
	  	parentAgents.getAgents().remove( myCurrentIndex );*/
		// So Agents_base put the result into completeQueue
		asyncFuncListIndex = -1;
		parentAgents = null;

	}

	 /**
	  * Returns the number of agents to initially instantiate on a place indexed
	  * with coordinates[]. The maxAgents parameter indicates the number of
	  * agents to create over the entire application. The argument size[] defines
	  * the size of the "Place" matrix to which a given "Agent" class belongs. The
	  * system-provided (thus default) map( ) method distributes agents over
	  * places uniformly as in:
	  *        maxAgents / size.length
	  * The map( ) method may be overloaded by an application-specific method.
	  * A user-provided map( ) method may ignore maxAgents when creating
	  * agents.
	  * @param initPopulation
	  * @param size
	  * @param index
	  * @param curPlace
	  * @return 
	  */	
	public int map( int initPopulation, int[] size, int[] index, Place curPlace) {

		// compute the total # places
		int placeTotal = 1;
		for ( int x = 0; x < size.length; x++ )
			placeTotal *= size[x];

		// compute the global linear index
		int linearIndex = 0;
		for ( int i = 0; i < index.length; i++ ) {
			if ( index[i] >= 0 && size[i] > 0 && index[i] < size[i] ) {
				linearIndex = linearIndex * size[i];
				linearIndex += index[i];
			}
		}

		// compute #agents per place a.k.a. colonists
		int colonists = initPopulation / placeTotal;
		int remainders = initPopulation % placeTotal;
		if ( linearIndex < remainders ) colonists++; // add a remainder

		return colonists;

	}

	/**
	 * Initiates an agent migration upon a next call to Agents.manageAll( ). More
	 * specifically, migrate( ) updates the calling agent’s index[].
	 * @param index
	 * @return 
	 */
	protected boolean migrate( int... index ) { 

		int[] placesSize = place.getSize();
		for ( int i = 0; i < placesSize.length; i++ ) {
			if ( index[i] >= 0 && index[i] < placesSize[i] ) {
				continue;
			} else {
				return false;
			}
		}

		this.index = index.clone( ); // assign the new index
		return true;

	}

	protected boolean migrateAsync(int... index) {
		boolean result = migrate(index);
		//stopProcessAsyncFuncList = true;
		parentAgents.migrateAsync(this);
		return result;
	}

	// TODO - modify debug data of the agent, overridden as necessary by the developer for now
	public void setDebugData(Object argument){

	}

	public void setIndex(int[] index) {
		this.index = index;
	}

	public void setNewChildren(int newChildren) {
		this.newChildren = newChildren;
	}

	public void setPlace(Place place) {
		this.place = place;
	}

	public int getAsyncFuncListIndex() {
		return asyncFuncListIndex;
	}

	public int pollAsyncFuncListIndex() {
		++asyncFuncListIndex;
		return asyncFuncListIndex - 1;
	}

	public void setAsyncFuncListIndex(int index) {
		asyncFuncListIndex = index;
	}

	public Object[] getAsyncResults() {
		return asyncResults;
	}

	protected void appendAsyncResult(Object newResult) {
		asyncResults[asyncResultsIndex] = newResult;
		++asyncResultsIndex;
	}

	public void resetAsyncResults() {
		asyncResults = new Object[parentAgents.getAsyncFuncList().length];
		asyncResultsIndex = 0;
	}

	public int asyncResultsSize() {
		return asyncResultsIndex;
	}

	public void setAsyncArgument(Object newArg) {
		asyncArgument = newArg;
	}

	public Object getAsyncArgument(){
		return asyncArgument;
	}

	public void setMyOriginalAsyncIndex(int newIndex) {
		myOriginalAsyncIndex = newIndex;
	}

	public int getMyOriginalAsyncIndex() {
		return myOriginalAsyncIndex;
	}

	public void setCurrentIndex(int newIndex) {
		myCurrentIndex = newIndex;
	}

	public int getCurrentIndex() {
		return myCurrentIndex;
	}

	public void setMyAsyncOriginalPid(int pid) {
		myAsyncOriginalPid = pid;
	}

	public int getMyAsyncOriginalPid() {
		return myAsyncOriginalPid;
	}

	public void setParentAgents(Agents_base parent) {
		parentAgents = parent;
	}

	public Agents_base getParentAgents() {
		return parentAgents;
	}

	public boolean hasAlreadyRemoteMigrate() {
		return hasAlreadyRemoteMigrated;
	}

	public void setHasAlreadyRemoteMigrated(boolean value) {
		hasAlreadyRemoteMigrated = value;
	}

	public boolean shouldPutBackToAsyncQueue() {
		return putBackToAsyncQueue;
	}

	public void setPutBackToAsyncQueue(boolean value) {
		putBackToAsyncQueue = value;
	}

	/**
	 * Spawns a “numAgents’ of new agents, as passing arguments[i] (with arg_size) 
	 * to the i-th new agent upon a next call to Agents.manageAll( ).
	 * More specifically, spawn( ) changes the calling agent’s newChildren.
	 * @param numAgents
	 * @param arguments
	 */
	protected void spawn( int numAgents, Object[] arguments ) { 

		//Only want to make changes if the number to be created is above zero
		if ( numAgents > 0 ) {
			newChildren = numAgents;
			this.arguments = arguments.clone( );			
		}

	}

	/**
	 * Spawn new children async and supply them with the arguments and functionIds
	 * @param numAgents
	 * @param initializedArguments
	 * @param arguments
	 */
	protected void spawnAsync(int numAgents, Object[] initializedArguments, Object[] arguments) {
		if(numAgents > 0) {
			parentAgents.spawnAsync(this, numAgents, initializedArguments, arguments);
		}
	}

	/**
	 * Only FOR ASYNC
	 */
	protected Agent cloneForAsyncResult() {
		Agent result = new Agent();
		result.alive = this.alive;
		result.asyncResults = new Object[asyncResultsIndex];
		for(int i = 0; i < asyncResultsIndex; i++) {
			result.asyncResults[i] = this.asyncResults[i];
		}
		result.myAsyncOriginalPid = this.myAsyncOriginalPid;
		result.myOriginalAsyncIndex = this.myOriginalAsyncIndex;
		if(MASS.isConsoleLoggingEnabled())
			MASS_base.log("cloneForAsyncResult asyncResults size = " + result.asyncResultsSize() + " original idx " + result.myOriginalAsyncIndex);
		return result;
	}

	void autoMigrateStart() {
		int[] size = place.getSize();
		int[] index = size.clone();
		for(int i = size.length - 1; i >= 0; i--) {
			// autoMigrationStartingIndex value is altered after this
			index[i] = this.autoMigrationStartingIndex % size[i];
			this.autoMigrationStartingIndex = autoMigrationStartingIndex / size[i];
		}
		migrateAsync(index);
	}

	void autoMigrateNext() {
		int[] index = this.getPlace().getIndex().clone();
		++index[index.length - 1];
		migrateAsync(index);
	}

	public void setAutoMigrationStartingIndex(int i) {
		this.autoMigrationStartingIndex = i;
	}

}