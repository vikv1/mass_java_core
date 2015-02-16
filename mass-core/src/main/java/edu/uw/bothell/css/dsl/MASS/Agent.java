package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;

@SuppressWarnings("serial")
public class Agent implements Serializable {

	//@SuppressWarnings("unused")
	//private final int agentsHandle;

	//@SuppressWarnings("unused")
	//private final int placesHandle;

	private final int agentId;

	//@SuppressWarnings("unused")
	//private final int parentId;

	private Place place = null;
	private int[] index = null;
	private boolean alive = true;
	private int newChildren = 0;
	private Object[] arguments = null;
	
	// Async
	private LinkedList<Integer> asyncFuncList;
	private LinkedList<Object> asyncResults;
	private Object asyncArgument;
	private Agents_base parentAgents;
	// true to signal a thread to stop processing this Agent's asyncFuncList
	// this happens in kill & migrate case
	private boolean stopProcessAsyncFuncList = false;
	
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
	 * change when remote migrate, used for killing
	 */
	private int myCurrentIndex;
	
	/**
	 * Original Pid before execution
	 */
	private int myAsyncOriginalPid;

	public Agent ( ) {
		//agentsHandle = Agents.getAgentInitAgentsHandle();
		//placesHandle = Agents.getAgentInitPlacesHandle();
		agentId = Agents.getAgentInitAgentId();
		//parentId = Agents.getAgentInitParentId();
		asyncFuncList = new LinkedList<Integer>();
	}

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

	public void kill( ) {
		alive = false;
	}
	
	public void killAsync() {
	  kill();
	  stopProcessAsyncFuncList = true;
	  synchronized(Mthread.class){
	    Mthread.setAgentBagSize(Mthread.getAgentBagSize() - 1);
	  }
	  
    // remove the agent from this place
	  getPlace().getAgents().remove( this );

    // remove from AgentList, too!
	  // unlike sync myAsyncIndex start from 0
    parentAgents.getAgents().remove( myCurrentIndex );
    // So Agents_base put the result into completeQueue
    asyncFuncList.clear();
    parentAgents = null;
	}

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

	//Set index for an Agent to migrate to
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
    stopProcessAsyncFuncList = true;
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
	
	public void setAsyncFuncList(Collection<Integer> funcIds) {
	 asyncFuncList.clear();
	 asyncFuncList.addAll(funcIds);
	}
	
	public LinkedList<Integer> getAsyncFuncList() {
	  return asyncFuncList;
	}
	
	protected void appendAsyncResult(Object newResult) {
	  asyncResults.add(newResult);
	}
	
	public LinkedList<Object> getAsyncResults() {
	  return asyncResults;
	}
	
	public void resetAsyncResults() {
	  asyncResults = new LinkedList<Object>();
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
	
	public boolean shouldStopProcessAsyncFuncList() {
	  return stopProcessAsyncFuncList;
	}

	public void setStopProcessAsyncFuncList(boolean value) {
    stopProcessAsyncFuncList = value;
  }
	
	//Set number for spawning additional Agents
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
	 * @param arguments
	 * @param functionIds
	 */
	protected void spawnAsync(int numAgents, Object[] arguments, LinkedList<Integer>[] functionIds) {
	  if(numAgents > 0) {
	    parentAgents.spawnAsync(this, numAgents, arguments, functionIds);
	  }
	}
	
	/**
	 * Only FOR ASYNC
	 */
  protected Agent cloneForAsyncResult() {
      Agent result = new Agent();
      result.alive = this.alive;
      result.asyncResults = this.asyncResults;
      result.myAsyncOriginalPid = this.myAsyncOriginalPid;
      result.myOriginalAsyncIndex = this.myOriginalAsyncIndex;
      if(MASS.isConsoleLoggingEnabled()) {
        MASS_base.log("cloneForAsyncResult asyncResults size = " + result.asyncResults.size());
      }
      return result;
  }

}