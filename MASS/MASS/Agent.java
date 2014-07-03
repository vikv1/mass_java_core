package MASS;



import java.io.Serializable;

/**
 * Instantiates and manipulates each agent object.
 *
 * @author  Tim Chuang, John Spiger, and Munehiro Fukuda (CSS, UW Bothell)
 * @since   6/18/10
 * @version 0.1
 */



public class Agent implements Serializable {
  
  /**
   * The handle of the MASS.Agents object this agent is a part of
   */
  final int agentsHandle;
  final int placesHandle;
  /**
   * The current place where this agent resides.
   */
  protected Place place = null;
  
  /**
   * An array to maintain the coordinates of where this agent
   * resides. Intuitively, index[0], index[1], and index[2]
   * correspond to coordinates of x, y, and z, or those of i, j, and
   * k.
   */
  int[] index = null;
  
  /**
   * This agent's identifier. It is calculated as: the sequence
   * number * the size of this agent's belonging matrix + the index
   * of the current place when all places are flattened to a single
   * dimensional array.
   */
  public final int agentId;
  
  /**
   * The identifier of this agent's parent.
   */
  public final int parentId;
  
  /**
   * A set arguments to be passed to a set of remote-cell functions
   * that will be invoked by exchangeAll( ) in the nearest future.
   */
  protected Object[] outMessages = null;
  
  /**
   * Values returned from exchangeAll( ), where inMessage[i]
   * receives a return value from a function call made to the i-th
   * remote cell through exchangeAll( ).
   */
  protected Object[] inMessages = null;
  
  /**
   * The number of new children created by this agent upon a next
   * call to MASS.Agents.manageAll( ).
   */
  int newChildren = 0;
  boolean[] spawnLock = new boolean[0];
  /**
   * An array of arguments, each passed to a different new child.
   */ 
  Object[] arguments = null;
  
  /**
   * This keeps true while this agent is active. Once it is set
   * false, this agent is killed upon a next call to
   * MASS.Agents.manageAll( ).
   */
  boolean alive = true;
  
  /**
   * The identifier of an event on which this agent sleeps on. The
   * eventId should be between 1 and 10. All the other numbers mean
   * that the agent does not sleep.
   */
  int eventId = 0;
  boolean[] schedulingSleep; //a lock object, value unimportant
  
  /**
   keyLock is used as an object to synchronize on when changing key,
   intended to be used only in setKey() so as to avoid deadlocks
   * The value used to sort agents.
   */
  boolean[] keyLock = new boolean[1];
  int key = 0;
  
  /**
   * Is the default constructor. No primitive data types can be
   * passed to the methods, since they are not derivable from the "Object"
   * class.
   *
   * @param args arguments passed to each agent.
   */
  protected Agent() {
	//the MASS.Agents class should have agentInitArgs ready
	int [] agentInitArgs = Agents.getAgentInitArgs();
	if (agentInitArgs != null && agentInitArgs.length == 4){
	  this.agentsHandle = agentInitArgs[0];
	  this.placesHandle = agentInitArgs[1];
	  this.agentId = agentInitArgs[2];
	  this.parentId = agentInitArgs[3];
	} else {
	  this.agentsHandle = -1; //i.e., error
	  this.placesHandle = -1; 
	  this.agentId = -1; 
	  this.parentId = -1;
	}
	schedulingSleep = new boolean[1];
  }
  
  /**
   * Returns the number of agents to initially instantiate on a
   * place indexed with coordinates[]. The maxAgents parameter
   * indicates the number of agents to create over the entire
   * application. The argument size[] defines the size of the
   * "MASS.Place" matrix to which a given "MASS.Agent" class belongs. The
   * system-provided (thus default) map( ) method distributes agents
   * over places uniformly as in: maxAgents / size.length. The map( )
   * method may be overloaded by an application-specific method. A
   * user-provided map( ) method may ignore maxAgents when creating
   * agents.
   *
   * @param maxAgents the total number of agents initially distirbuted
   *                  over a given MASS.Places matrix.
   * @param size      the size of a given MASS.Places matrix.
   * @param coordinates the coordinates of each matrix element.
   * @return the number of agents to be created in a given coordinates.
   */
  public int map( int maxAgents, int[] size, int[] coordinates ) 
  {
	int colonists = -1; //default, i.e., an error
	//validate args
	boolean validArgs = true;
	if ( maxAgents < 1 
		|| size == null
		|| coordinates == null
		|| size.length != coordinates.length 
		|| size.length == 0 
		|| coordinates.length == 0)
        {
	  validArgs = false;
	}
	if (validArgs)
        {
	  for (int i = 0; i < size.length && validArgs; i++)
          {
            if ( coordinates[i] < 0 
                    || size[i] < 1 
                    || coordinates[i] >= size[i])
            { 
              validArgs = false;
            }
	  }
	}
	//done checking args, do the calculation
	if (validArgs)
        {
	  int placesPos = Places.indexArr2Num(coordinates, size);
	  int placeTotal = 1;
	  for (int x = 0; x < size.length; x++)
          {
		placeTotal *= size[x]; //calculate total num of place in places
	  }
	  colonists = maxAgents/placeTotal;
	  int remainders = maxAgents % placeTotal;
	  if (placesPos < remainders) colonists++; //dist remainders
	}
	return colonists;
  }

  public int determinePlaceIndexToAttachAgent( int maxAgents, int[] size, int[] coordinates ) 
  {
	int colonists = -1; //default, i.e., an error
	//validate args
	boolean validArgs = true;
	if ( maxAgents < 1 
		|| size == null
		|| coordinates == null
		|| size.length != coordinates.length 
		|| size.length == 0 
		|| coordinates.length == 0)
        {
	  validArgs = false;
	}
	if (validArgs)
        {
	  for (int i = 0; i < size.length && validArgs; i++)
          {
            if ( coordinates[i] < 0 
                    || size[i] < 1 
                    || coordinates[i] >= size[i])
            { 
              validArgs = false;
            }
	  }
	}
	//done checking args, do the calculation
	if (validArgs)
        {
	  //int placesPos = Places.indexArr2Num(coordinates, size);
          int placesPos = Places.getGlobalLinearIndexFromGlobalArrayIndex(coordinates, size);
	  int placeTotal = 1;
	  for (int x = 0; x < size.length; x++)
          {
		placeTotal *= size[x]; //calculate total num of place in places
	  }
	  colonists = maxAgents/placeTotal;
	  int remainders = maxAgents % placeTotal;
	  if (placesPos < remainders) colonists++; //dist remainders
	}
	return colonists;
  }

  /**
   * Initiates an agent migration upon a next call to
   * MASS.Agents.manageAll( ). More specifically, migrate( ) updates the
   * calling agent's index[].
   *
   * @param index the index of a next cell to go.
   * @return true if a migration was scheduled in success, false if
   * index is the same as the current place or the index is not valid
   */
  public boolean migrate( int... index ) 
  {
	boolean retVal = false;
	int[] size = place.size;
	if (index.length == size.length)
        { //make sure destination exists
	  boolean alreadyMigrating = false;
	  if (!alreadyMigrating)
          {
		boolean validDestination = true;
		boolean identicalIndex = true;
		for (int i = 0; i < index.length && validDestination; i++)
                {
		  if (index[i] < 0 || index[i] >= size[i])
                  {
			validDestination = false;
		  }
		  if (index[i] != this.index[i])
                  {
			identicalIndex = false;
		  }
		}
		if (validDestination && !identicalIndex)
                {
		  this.index = index.clone();//assign the new index
		  retVal = true;
		}
	  }
	}
	return retVal; // default, i.e., an error
  }
  
  /**
   * Spawns a "numAgents" of new agents, as passing arguments[i] to
   * the i-th new agent upon a next call to MASS.Agents.manageAll( ).
   * More specifically, create( ) changes the calling agent's newChildren.
   * If numAgents != arguments.length, method does nothing.
   *
   * @param numAgents the number of agents to create.
   * @param arguments arguments passed to each child agent.
   */
  public void spawn( int numAgents, Object[] arguments ) 
  {
	synchronized (spawnLock) 
        {
	  if (numAgents == arguments.length)
          {
		newChildren = numAgents;
		this.arguments = arguments.clone();
	  }
	}
  }
  
  /**
   * Terminates the calling agent upon a next call to MASS.Agents.manageAll( ).
   * More specifically, kill( ) sets the "alive" variable false.
   */
  public void kill( ) {
	this.alive = false;
  }
  
  /**
   * Puts the calling agent to sleep on a given eventId whose value
   * should be 1 through to 10. If eventId is not in the range of 1
   * through to 10, the agent will not be suspended. The sleep( )
   * function returns true if the agent is suspended successfully.
   *
   * @param eventId the identifier of an event on which the calling agent
   *                wants to sleep.
   * @return true if a sleep was scheduled in success. Otherwise false.
   */
  public boolean sleep( int eventId ) {
	boolean retVal = false;
	// if the eventId is not already in use.
	synchronized (schedulingSleep) {
	  if ((this.eventId < 1 || this.eventId > 10)
		  && ( 1 <= eventId && eventId <= 10 )) {
		this.eventId = eventId;
		retVal = true;
	  }
	}
	return retVal; // default, i.e., an error
  }
  
  /**
   * a listener method called when an agent is woken up from a
   * scheduled wakeupAll at its place
   *
   * @param eventId on which the agent is waking up
   */
  protected void wakeup( int eventId ) {
	//nothing to do... subclass overrides
  }
  
  /**
   * Wakes up all agents that are sleeping on a given eventId within
   * the same place as where this calling agent resides.
   *
   * @param eventId the identifier of an event from which the calling agent
   *                wants to wake up all sleeping agents.
   */
  public void wakeupAll( int eventId ) {
	if (1 <= eventId && eventId <= 10){
	  synchronized (place.eventsToFire) {
		place.eventsToFire[eventId] = true;
	  }
	}
  }
  
  /**
   * Substitutes the calling agent's "key" variable with a given
   * value. It is used for sorting agents within the same place.
   *
   * @param value the value used to sort agents.
   */
  public void setKey( int value ) {
	synchronized (keyLock) {
	  this.key = value;
	}
  }
  
  /**
   * Is called from MASS.Agents.callAll( ) and exchangeAll( ), and
   * invokes mass_0, mass_1, mass_2, mass_3, or mass_4 whose postfix
   * number corresponds to functionId. An application may override
   * callMethod( ) so as to direct MASS.Agents to invoke an
   * application-specific method.
   *
   * @param functionId the identifier of a method to call.
   * @param argument an argument passed to a method to call.
   * @return a return value from functionId.
   */
  public Object callMethod( int functionId, Object argument ) {
        MASS.log("Base agent call method called!");
	return null; // default, i.e., an error
  }
  
  /*
   *  accessor method
   * @return the MASS.Place where the MASS.Agent currently resides.
   */
  public Place getPlace(){
	return place;
  }
  
}
