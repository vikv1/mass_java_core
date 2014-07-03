package MASS;

 
/**
* Instantiates and manipulates a collection of MASS.Agents object over a cluster
* of multi-core computing nodes.
*
* @author  Tim Chuang, John Spiger, and Munehiro Fukuda (CSS, UW Bothell)
* @since   6/18/10
* @version 0.1
*/



import java.io.File;
import java.util.*;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;

public class Agents {
  
  int handle;
  private static Class<?> klas;
  private static Constructor<?> ctor;
  Places places;
  private static Vector<Agent> bag;
  private static int[] agentInitArgs;
  private static Object agentArgument;
  private int total;
  
  /**
   * A class for the MASS.MASS class to use when doing SortAll
   *
   */
   /* package */ 
  class AgentComparator implements Comparator<Agent> 
  {	
	AgentComparator(){}
	
	public int compare (Agent a1, Agent a2){
	  int retVal = 0;
	  synchronized (a1.keyLock) {
		synchronized (a2.keyLock) {
		  if (a1.key < a2.key){
			retVal = -1;
		  } else if (a1.key > a2.key){
			retVal = 1;
		  }
		}
	  }
	  return retVal;
	}
	
	public boolean equals(Object o){
	  return false;
	}
  }
  
  /**
   * Is the constructor that instantiates "className" objects as a
   * collection of multi-agents.
   *
   * @throws Exception if there is a problem with a parameter
   * @param handle a user-given non-negative number to uniquely
   *               identify this collection of distributed
   *               multi-agents over the system.
   * @param className the name of the class from which each agent is 
   *                  instantiated.
   * @param argument an argument passed to each agent.
   * @param places a distributed array where new agents are instantiated.
   * @param initPopulation the total number of agents to be created.
   */
  public Agents( int handle
		, String className
		, Object argument
		, Places places
		, int initPopulation ) throws Exception {
	if (handle < 0)
        {
	  throw new Exception("The handle must be an integer of zero or more.");
	} 
        else if (className == null || className.trim().length() == 0)
        {//TODO better validation
	  throw new Exception("The class name must be a valid class name");
	}
        else if (places == null)
        {
	  throw new Exception("The MASS.Places object cannot be null.");
	} 
        else if (initPopulation < 0)
        {
	  throw new Exception("The starting population must be zero or more.");
	}

	setTotalAgents( initPopulation ); // added by Fukuda on 11-22-13

        // master rank sends all initialization parameters to other ranks        
        if(MASS.myPid == 0 && MASS.systemSize > 1)
        {
            MASS.log("------------------Beginning Agent Initialization sequence for place handle " + this.handle  + "-----------------" );             
            Message m = new Message();
            m.createAgentnitializationMessage(handle, className, argument, places.getHandle(), initPopulation);
            for(MNode node : MASS.mNodes) 
            {
                node.sendMessage(m);
            } 
            System.err.println("Agent Information sent! Awaiting Acknowledgement... ");

            for(MNode node: MASS.mNodes)
            {
              node.receiveMessage();
            }
            System.err.println("Received all Acknowledgement... ");           
        }
        
        this.handle = handle;
	this.places = places;
        this.agentArgument = argument;
        
        File curDir = new File(MASS.CUR_DIR);
        //URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { curDir.toURI().toURL() });         
	klas = Class.forName(className, true, Places.loader);
	ctor = klas.getConstructor(Object.class);
        //TODO possibly check to see if places is in the MASS.MASS environment
        if (MASS.addAgents(this))
        { // ---------- agents added to MASS.MASS here -----------
          //make all the agents first
          this.bag = new Vector<Agent>();
          Vector<Agent> tempBag = new Vector<Agent>();
          for (int i = 0; i < initPopulation; i++)
          {               
                bag.add(createAgent(argument, -1)); //-1 for parentId to be same as agentId, root agent
          }
          tempBag.addAll(bag);
          bag.clear();
          //get ready to distribute agent objs among place objs
          Places.Iterator placesIter = places.iterator();
          java.util.Iterator<Agent> agentsIter = tempBag.iterator();
          int[] placesSize = places.size();
          Place currentPlace = null; 
          Agent currentAgent = null; 
          int placeIndex = -1; //incremented at start so will be 0
          int colonistsNum = -1;
          boolean needNewAgent = true;
          //do the distributing, iterate through each MASS.Place
          //every step thru iteration get a new place, but not always a new agent
          while (placesIter.hasNext() 
                         && (agentsIter.hasNext() || !needNewAgent) ) 
          {
                placeIndex++;//first value in loop is 0
                currentPlace = placesIter.next();
                //will stick with the same agent if previous place had 0 colonists
                //need to always have an agent to work with first to do agent.map()
                if (needNewAgent)
                {
                  currentAgent = agentsIter.next();
                  needNewAgent = false;
                }

                colonistsNum = currentAgent.map(initPopulation, placesSize, currentPlace.index);
                //fill place with each agent colonist
                while (colonistsNum > 0
                           && (!needNewAgent || agentsIter.hasNext()) )
                {
                  if (needNewAgent)
                  {
                        currentAgent = agentsIter.next();//happens whenever > 1 colonists
                        needNewAgent = false;
                  }
                  currentAgent.index = currentPlace.index.clone();
                  currentAgent.place = currentPlace;
                  currentPlace.agents.add(currentAgent);//agent added to place
                  bag.add(currentAgent); //the 1 place in MASS.Agents where an agent is added to the bag
                  needNewAgent = true;
                  colonistsNum--;
                }
          }
        } 
        else 
        {
          throw new Exception("That handle is already in use.");
        }

  }
  
  /*
   * Converts an array to a string.  for debugging
   */
/*  private static String arr2str(int[] arr){
	String str = "";
	if (arr != null){
	  for (int i = 0; i < arr.length; i++){
		str += arr[i];
		if (i < arr.length -1) str += " ";
	  }
	} else {
	  str = "null";
	}
	return str;
  }
  */
  
  /*
   * Creates a new agent for this MASS.Agents. agentId && parentId are set, as are
   * the agentsHandle and placesHandle. New agent has NO INDEX and NO PLACE.
   * @param argument the argument to pass to the constructor for the MASS.Agent subclass
   * @param parentId the ID number for the parent, -1 if agent has no parent
   * @return the new MASS.Agent, a part of the MASS.Agents, but IS NOT added to the MASS.Places
   */
  Agent createAgent(Object argument, int parentId){
	Agent agent = null;
	try 
        {
	  agentInitArgs = new int[1];//something to synchronize on
	  synchronized (agentInitArgs) 
          {
		int agentId = bag.size();
		agentInitArgs = new int[4];
		agentInitArgs[0] = this.handle;
		agentInitArgs[1] = this.places.getHandle();
		agentInitArgs[2] = agentId;
		if (parentId == -1)
                {
		  agentInitArgs[3] = agentId; //root agent, no parent
		} 
                else 
                {
		  agentInitArgs[3] = parentId;
		}
		agent = (Agent)ctor.newInstance(argument);
		
		agentInitArgs = null;
	  }
	} 
        catch (Exception e)
        {
	  agent = null;
	  e.printStackTrace();
	}
	return agent;
  }
  
  //package scope...
  static int[] getAgentInitArgs(){
	return agentInitArgs.clone();
  }
  
  /*
   * Creates a new agent for this MASS.Agents. agentId && parentId are set, as are
   * the agentsHandle and placesHandle. New agent has NO INDEX and NO PLACE.
   * @param argument the argument to pass to the constructor for the MASS.Agent subclass
   * @param parentId the ID number for the parent, -1 if agent has no parent
   * @return the new MASS.Agent, a part of the MASS.Agents, but IS NOT added to the MASS.Places
   */
  static Agent createAgentForMigrate(RemoteAgentRequest req){
	Agent agent = null;
	try 
        {
	  agentInitArgs = new int[1];//something to synchronize on
	  synchronized (agentInitArgs) 
          {
		agentInitArgs = new int[4];
		agentInitArgs[0] = req.getAgentsHandle();
		agentInitArgs[1] = req.getPlacesHandle();
		agentInitArgs[2] = bag.size();
		agentInitArgs[3] = req.getParentId(); //root agent, no parent                            
		agent = (Agent)ctor.newInstance(agentArgument);
                agent.inMessages = req.getInMessages();
                agent.outMessages = req.getOutMessages();
                agent.arguments = req.getArguments();
                bag.add(agent); //the 1 place in MASS.Agents where an agent is added to the bag
		agentInitArgs = null;
	  }
	} 
        catch (Exception e)
        {
            MASS.log("Cannot create agent for migrate...");
            agent = null;
            e.printStackTrace();
	}
	return agent;
  }  
  
  /**
   * Returns the handle associated with this agent set.
   *
   * @return the handle associated with this MASS.Agents object, -1 if handle not set
   */
  public int getHandle( ) {
	if ( handle > -1 ){
	  return handle;
	} else {
	  return -1; // default, i.e., an error //should never happen
	}
  }
  
  /**
   * Return the total number of agents per node (rather than the entire system). 
   *
   * @return the total number of agents per node (rather than the entire system).
   */
    public int nAgents( ) { // comment added by Fukuda on 11-22-13
	int retVal = -1;
	if (bag != null){
	  retVal = bag.size();
	}
	return retVal;
    }

    /**
     * Return the total number of agents over the system.
     *
     * @return the total number of agents over the system.
     */
    public int totalAgents( ) {
	return total;
    }

    void setTotalAgents( int newPopulation ) {
	total = newPopulation;
    }
  
  void removeAgent(Agent agent)
  {
      if(bag != null)
          bag.remove(agent);
  }
  
  int getAgentIndexInBag(Agent agent)
  {
      return bag.indexOf(agent);
  }
  
  /**
   * Calls the method specified with functionId of all agents. Done
   * in parallel among multi-processes/threads
   *
   * @param functionId the idnetifier of a method to call.  
   */
  public void callAll( int functionId ) {
	callAll( functionId, null );
  }
  
  /**
   * Calls the method specified with functionId of all agents as
   * passing an Object argument to the method. Done in parallel
   * among multi-processes/threads.
   *
   * @param functionId the identifier of a method to call.
   * @param argument an argumenet passsed to each method.
   */
  public void callAll( int functionId, Object argument ) {
	MASS.agentsCallAll(handle, places, functionId, argument);
  }
  
  /**
   * Calls the method specified with functionId of all agents as
   * passing arguments[i] to agent[i]'s method, and receives a
   * return value from it into Object[i]. Done in parallel among
   * multi-processes/threads. The order of agents depends on the
   * index of a place where they resides, starts from the
   * place[0][0]...[0], and gets increased with the right-most index
   * first and the left-most index last.
   *
   * @param functionId the identifier of a method to call.
   * @param arguments an argumenet passsed to each method.
   * @return Object[i] used to sotre a return value from agent[i].
   */
  public Object[] callAll( int functionId, Object[] arguments ) {
	return MASS.agentsCallAll(handle, places, functionId, arguments);
  }
  /**
   * Updates each agent's status, based on each of its latest
   * migrate( ), spawn( ), kill( ), sleep( ), wakeup( ), and
   * wakeUpAll( ) calls. These methods are defined in the MASS.Agent
   * base class and may be invoked from other functions through
   * callAll and exchangeAll. Done in parallel among
   * multi-processes/threads.
   */
  public void manageAll( ) {
	MASS.agentsManageAll(handle, places);
  }
  
  /**
   * Sorts agents within each place in the descending order of their
   * "key" values.
   *
   * @param descending true to sort agents in a descending
   *                   order. Otherwise in a ascending order.
   */
  public void sortAll( boolean descending ) {
	MASS.agentsSortAll(handle, descending);
  }
  
  /**
   * Allows each agent to call the method specified with functionId
   * of all the other agents residing within the same place as where
   * the calling agent exists as well as belonging to the agent
   * group with "handle". The caller agent's outMessage, (i.e., an
   * Object) is a set of arguments passed to the calle's method. The
   * caller's inMessages[], (i.e., an array of Objects) stores
   * values returned from all callee agents. More specifically,
   * inMessages[i] maintains a set of return values from the ith
   * callee.
   * 
   * @param handle the handle of the callee MASS.Agents object.
   * @param functionId the identifier of a method to call.
   */
  public void exchangeAll( int handle, int functionId ) {
	MASS.agentsExchangeAll( this.handle, handle, places, functionId);
  }
  

}
