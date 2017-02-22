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

import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;

/**
 * An Agent is an execution instance that resides in a Place, perform
 * operations on objects contained by the Place, and possibly migrate
 * to another Place. 
 */
@SuppressWarnings("serial")
public class Agents extends AgentsBase implements Serializable {

  private int[] localAgents; // localAgents[i] = # agents in rank[i]
  private int total;

	// logging
	private transient Log4J2Logger logger = Log4J2Logger.getInstance();

  /**
   * Instantiates a set of agents from the "className" class, passes the
   * "argument" object to their constructor, associates them with a given
   * "Places" matrix, and distributes them over these places, based the
   * map( ) method that is defined within the Agent class. If a user does not
   * overload it by him/herself, map( ) uniformly distributes an
   * "initPopulation" number of agents. If a user-provided map( ) method is
   * used, it must return the number of agents spawned at each place
   * regardless of the initPopulation parameter. Each set of agents is
   * associated with a user-given handle that must be unique over machines.
   * @param handle
   * @param className The name of the user-defined class to instantiate
   * @param argument The argument to pass to each Agent as it is being instantiated
   * @param places The Places instance that will contain the Agents
   * @param initPopulation The number of Agents to create
   */
  public Agents(int handle, String className, Object argument, Places places,
      int initPopulation) {

    super(handle, className, argument, places.getHandle(), initPopulation);
    localAgents = new int[MASSBase.getSystemSize()];
    initMaster(argument);

  }

  Object callAllSetup(int functionId, Object argument, Message.ACTION_TYPE type) {

    // calculate the total number of agents
    total = 0;
    for (int i = 0; i < MASSBase.getSystemSize(); i++)
      total += localAgents[i];

    // send a AGENTS_CALL_ALL message to each slave
    // i is the indicator of MNode at ith position of the MNode vector
    Message m = null;
    for (int i = 0; i < MASS.getRemoteNodes().size(); i++) {

      // create a message
      if (type == Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT)

        m = new Message(type, this.getHandle(), functionId, argument);

      else {

        // calculate argument position
        int argumentPosition = 0;
        for (int dest = 0; dest <= i; dest++) {
          argumentPosition += localAgents[dest];

          if (MASS.isConsoleLoggingEnabled())
            System.err
                .println("Agents.callAll: calc arg_pos = " + argumentPosition
                    + " localAgents[" + (dest + 1) + "] = "
                    + localAgents[dest + 1]);

        }

        Object[] partitionedArgument = new Object[localAgents[i + 1]];

        System.arraycopy((Object[]) argument, argumentPosition, partitionedArgument, 0,
            localAgents[i + 1]);

        m = new Message(type, this.getHandle(), functionId,
            partitionedArgument);

        if (MASS.isConsoleLoggingEnabled())
          System.err.println("Agents.callAll: to rank[" + (i + 1)
              + "] arg_pos = " + argumentPosition);

      }

      // send it
      MASS.getRemoteNodes().get(i).sendMessage(m);

      if (MASS.isConsoleLoggingEnabled()) {

        System.err
            .println("AGENTS_CALL_ALL " + m.getAction() + " sent to " + i);

        System.err.println("Bag Size is: "
            + MASSBase.getAgentsMap().get(new Integer(getHandle()))
                .getAgents().size_unreduced());

      }

    }

    MThread.setAgentBagSize(MASSBase.getAgentsMap()
        .get(new Integer(getHandle())).getAgents().size_unreduced());

    // Check for correct behavior post-Agents_base implementation
    // retrieve the corresponding agents

    // shared between agents
    MASSBase.setCurrentAgentsBase(this);
    MASSBase.setCurrentFunctionId(functionId);
    MASSBase.setCurrentArgument(argument);
    MASSBase.setCurrentMsgType(type);

    if (type == Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT) {
      MASSBase.setCurrentReturns(null);
    } else {
      MASSBase.setCurrentReturns(new Object[total]); // prepare an entire
                                                      // return space
    }

    // resume threads
    logger.debug("MASS_base.currentAgents = {}", MASSBase.getCurrentAgentsBase());

    MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_AGENTSCALLALL);

    // callall implementation
    // Agents.java is visible to user but AgentsBase(super) should not be visible
    if (type == Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT)
      super.callAll(functionId, argument, 0); // 0 = main tid
    else
      super.callAll(functionId, (Object[]) argument, 0);

    // confirm all threads are done with agents.callAll
    MThread.barrierThreads(0);
    localAgents[0] = getLocalPopulation();

    // Synchronized with all slave processes by main thread.
    MASS.barrierAllSlaves(MASSBase.getCurrentReturns(), 0, localAgents);

    total = 0;
    for (int i = 0; i < MASSBase.getSystemSize(); i++) {

      total += localAgents[i];

      // for debugging
      if (MASS.isConsoleLoggingEnabled())
        System.err.println("rank[" + i + "]'s local agent population = "
            + localAgents[i]);

    }

    return MASSBase.getCurrentReturns();

  }

  /**
   * Calls the method specified with functionId of all agents. Done in
   * parallel among multi-processes/threads
   * @param functionId
   */
  public void callAll(int functionId) {
    callAllSetup(functionId, null, Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT);
  }

  /**
   * Calls the method specified with functionId of all agents as passing a
   * (void) argument to the method. Done in parallel among 
   * multi-processes/threads.
   * @param functionId
   * @param argument
   */  public void callAll(int functionId, Object argument) {
    callAllSetup(functionId, argument,
        Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT);
  }

   /**
    * Calls the method specified with functionId of all agents as passing
    * arguments[i] to agent[i]’s method, and receives a return value from it
    * into (void *)[i] whose element’s size is return_value. Done in parallel
    * among multi-processes/threads. The order of agents depends on the
    * index of a place where they resides, starts from the place[0][0]…[0],
    * and gets increased with the right-most index first and the left-most
    * index last.
    * @param functionId
    * @param argument
    * @return 
    */
  public Object callAll(int functionId, Object[] argument) {
    return callAllSetup(functionId, argument,
        Message.ACTION_TYPE.AGENTS_CALL_ALL_RETURN_OBJECT);
  }

  private void initMaster(Object argument) {

    // check if MASS_base.hosts is empty (i.e., Places not yet created)
    if (MASSBase.getHosts().isEmpty()) {
      System.err.println("Agents(" + getClassName()
          + ") can't be created without Places!!");
      System.exit(-1);
    }

    // create a new list for message
    Message m = new Message(Message.ACTION_TYPE.AGENTS_INITIALIZE,
        getInitPopulation(), getHandle(), getPlacesHandle(), getClassName(),
        argument);

    // send a AGENT_INITIALIZE message to each slave
    for (MNode node : MASS.getRemoteNodes()) {

      node.sendMessage(m);
      logger.debug("AGENT_INITIALIZE sent to {}", node.getPid());
    
    }

    // Synchronized with all slave processes
    MASS.barrierAllSlaves(localAgents);
    localAgents[0] = getLocalPopulation();

    total = 0;
    for (int i = 0; i < MASSBase.getSystemSize(); i++) {

      total += localAgents[i];
      logger.debug("rank[" + i + "]'s local agent population = " + localAgents[i]);

    }

    // register this agents in the places hash map
    MASSBase.getAgentsMap().put(new Integer(getHandle()), this);

  }

  private void manageAllSetup() {

    // send an AGENTS_MANAGE_ALL message to each slave
    Message m = null;
    for (MNode node : MASS.getRemoteNodes()) {

      // create a message
      m = new Message(Message.ACTION_TYPE.AGENTS_MANAGE_ALL, this.getHandle(),
          0);

      // send it
      node.sendMessage(m);

      // MThread Update
      MThread.setAgentBagSize(MASSBase.getAgentsMap()
          .get(new Integer(getHandle())).getAgents().size_unreduced());

    }

    // retrieve the corresponding agents
    MASSBase.setCurrentAgentsBase(this);
    MASSBase.setCurrentMsgType(Message.ACTION_TYPE.AGENTS_MANAGE_ALL);

    // resume threads
    MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_MANAGEALL);

    // callall implementatioin
    super.manageAll(0); // 0 = the main thread id

    // confirm all threads are done with agents.callAll
    MThread.barrierThreads(0);

    // Synchronized with all slave processes
    MASS.barrierAllSlaves(localAgents);
    localAgents[0] = getLocalPopulation();

    total = 0;
    for (int i = 0; i < MASSBase.getSystemSize(); i++) {

      total += localAgents[i];

      // for debugging
      if (MASS.isConsoleLoggingEnabled() == true)
        System.err.println("rank[" + i + "]'s local agent population = "
            + localAgents[i]);

    }

  }

  /**
   * Updates each agent’s status, based on each of its latest migrate( ),
   * spawn( ), and kill( ) calls. These methods are defined in the Agent base
   * class and may be invoked from other functions through callAll and
   * exchangeAll. Done in parallel among multi-processes/threads 
   */
  public void manageAll() {
    manageAllSetup();
  }

  /**
   * Calls callAll and manageAll functions consecutively without responding
   *  back to user application in each iteration.
   *
   * @param functionId the function id that is executed
   * @param argument the argument to pass to each Agent
   * @param numberOfIterations number of consecutive calls of callAll() and manageAll() functions
   */
  public void doAll(int functionId, Object argument, int numberOfIterations)
  {
      for (int i=0; i<numberOfIterations; i++)
      {
          callAllSetup(functionId, argument, Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT);
          manageAllSetup();
      }
  }

  /**
   * Returns the current number of agents.
   * @return nAgents
   */
  public int nAgents() {

    int nAgents = 0;
    for (int i = 0; i < MASSBase.getSystemSize(); i++)
      nAgents += localAgents[i];

    return nAgents;

  }
}