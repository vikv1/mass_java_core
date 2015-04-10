package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("serial")
public class Agents extends Agents_base implements Serializable {

  private int[] localAgents; // localAgents[i] = # agents in rank[i]
  private int total;

  public Agents(int handle, String className, Object argument, Places places,
      int initPopulation) {

    super(handle, className, argument, places.getHandle(), initPopulation);
    localAgents = new int[MASS_base.getSystemSize()];
    init_master(argument);

  }

  Object ca_setup(int functionId, Object argument, Message.ACTION_TYPE type) {

    // calculate the total number of agents
    total = 0;
    for (int i = 0; i < MASS_base.getSystemSize(); i++)
      total += localAgents[i];

    // send a AGENTS_CALL_ALL message to each slave
    Message m = null;
    for (int i = 0; i < MASS.getRemoteNodes().size(); i++) {

      // create a message
      if (type == Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT)

        m = new Message(type, this.getHandle(), functionId, argument);

      else {

        // calculate argument position
        int arg_pos = 0;
        for (int dest = 0; dest <= i; dest++) {
          arg_pos += localAgents[dest];

          if (MASS.isConsoleLoggingEnabled())
            System.err
                .println("Agents.callAll: calc arg_pos = " + arg_pos
                    + " localAgents[" + (dest + 1) + "] = "
                    + localAgents[dest + 1]);

        }

        Object[] partitioned_argument = new Object[localAgents[i + 1]];

        System.arraycopy((Object[]) argument, arg_pos, partitioned_argument, 0,
            localAgents[i + 1]);

        m = new Message(type, this.getHandle(), functionId,
            partitioned_argument);

        if (MASS.isConsoleLoggingEnabled())
          System.err.println("Agents.callAll: to rank[" + (i + 1)
              + "] arg_pos = " + arg_pos);

      }

      // send it
      MASS.getRemoteNodes().get(i).sendMessage(m);

      if (MASS.isConsoleLoggingEnabled()) {

        System.err
            .println("AGENTS_CALL_ALL " + m.getAction() + " sent to " + i);

        System.err.println("Bag Size is: "
            + MASS_base.getAgentsMap().get(new Integer(getHandle()))
                .getAgents().size_unreduced());

      }

    }

    Mthread.setAgentBagSize(MASS_base.getAgentsMap()
        .get(new Integer(getHandle())).getAgents().size_unreduced());

    // Check for correct behavior post-Agents_base implementation
    // retrieve the corresponding agents

    // shared between agents
    MASS_base.setCurrentAgents(this);
    MASS_base.setCurrentFunctionId(functionId);
    MASS_base.setCurrentArgument(argument);
    MASS_base.setCurrentMsgType(type);

    if (type == Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT) {
      MASS_base.setCurrentReturns(null);
    } else {
      MASS_base.setCurrentReturns(new Object[total]); // prepare an entire
                                                      // return space
    }

    // resume threads
    if (MASS.isConsoleLoggingEnabled()) {

      MASS_base.log("MASS_base.currentgAgents = "
          + MASS_base.getCurrentAgents());

      MASS_base.log("MASS_base.getCurrentgAgents = "
          + MASS_base.getCurrentAgents());

    }

    Mthread.resumeThreads(Mthread.STATUS_TYPE.STATUS_AGENTSCALLALL);

    // callall implementatioin
    if (type == Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT)
      super.callAll(functionId, argument, 0); // 0 = main tid
    else
      super.callAll(functionId, (Object[]) argument,
          ((Object[]) argument).length, 0);

    // confirm all threads are done with agents.callAll
    Mthread.barrierThreads(0);
    localAgents[0] = getLocalPopulation();

    // Synchronized with all slave processes by main thread.
    MASS.barrier_all_slaves(MASS_base.getCurrentReturns(), 0, localAgents);

    total = 0;
    for (int i = 0; i < MASS_base.getSystemSize(); i++) {

      total += localAgents[i];

      // for debugging
      if (MASS.isConsoleLoggingEnabled())
        System.err.println("rank[" + i + "]'s local agent population = "
            + localAgents[i]);

    }

    return MASS_base.getCurrentReturns();

  }

  @SuppressWarnings("unused")
  List<Agent> ca_setupAsync(int[] functionIds, Object[] arguments, boolean autoMigration) throws Exception {
    // FOR auto migration
    Places places = MASS_base.getPlaces(this.getPlacesHandle());
    int lastDimensionLength = places.getSize()
        [places.getSize().length - 1];
    
    if(autoMigration) {
      // if user supplies funcs a b c then the func list
      // become -2 a b c -1 a b c -1 a b c .. -1 a b c
      int[] tempFuncIds = functionIds;
      functionIds = new int[lastDimensionLength * (1 + tempFuncIds.length)];
      functionIds[0] = -2;
      for(int i = 0; i < tempFuncIds.length; i++) {
        functionIds[i + 1] = tempFuncIds[i];
      }
      for(int i = 1; i < lastDimensionLength; i++) {
        functionIds[i*(tempFuncIds.length + 1)] = -1;
        for(int j = 0; j < tempFuncIds.length; j++) {
          functionIds[i*(tempFuncIds.length + 1) + j + 1] = tempFuncIds[j];
        }
      }
    }
    
    // Preparing this node for callAllAsync
    MASS_base.prepareAsyncExecution(this, functionIds);

    // calculate the total number of agents
    total = 0;
    for (int i = 0; i < MASS_base.getSystemSize(); i++) {
      total += localAgents[i];
      if(i!=0 && localAgents[i] != 0) {
        // Node started with zero agent won't send completeness notification
        MASS.getChildAgentPids().add(i);
        MASS.getOutAsyncAgents()[i] += localAgents[i]; // a way for master to keep track
        if(MASS.isConsoleLoggingEnabled()) {
          MASS.log("Node " + i + " has master as originator");
        }
      }
    }
    
    if(autoMigration) {
      int expectedAgentSize = 1;
      // if dimension is a1 * a2 * ... an then there need to be
      // a1*a2*..*a(n-1) agents
      for(int i = 0; i < places.getSize().length - 1; i++) {
        expectedAgentSize *= places.getSize()[i];
      }
      if(total != expectedAgentSize) {
        MASS.log("Need " + expectedAgentSize + " for automigration. There are " + total + " agents total");
        return null;
      }
    }

    // send a AGENTS_CALL_ALL_ASYNC message to each slave
    Message m = null;
    for (int i = 0; i < MASS.getRemoteNodes().size(); i++) {
      // calculate argument position
      int arg_pos = 0;
      for (int dest = 0; dest <= i; dest++) {
        arg_pos += localAgents[dest];
        if(MASS.isConsoleLoggingEnabled()) 
          System.err.println("Agents.callAll: calc arg_pos = " + arg_pos
              + " localAgents[" + (dest + 1) + "] = " + localAgents[dest + 1]);        
      }

      Object[] partitioned_argument = new Object[localAgents[i + 1]];
      if(arguments != null) {
        System.arraycopy((Object[]) arguments, arg_pos, partitioned_argument, 0,
            localAgents[i + 1]);
      }
      m = new Message(Message.ACTION_TYPE.AGENTS_CALL_ALL_ASYNC_RETURN_OBJECT,
          this.getHandle(), functionIds, partitioned_argument);
      if(autoMigration) {
        int[] startingPlaceGlobalIndex = new int[localAgents[i + 1]];
        for(int j = 0; j < startingPlaceGlobalIndex.length; j++) {
          startingPlaceGlobalIndex[j] = (arg_pos + j) * lastDimensionLength;
        }
        m.setAutoMigrationStartingIndex(startingPlaceGlobalIndex);
      }
      if(MASS.isConsoleLoggingEnabled())
        System.err.println("Agents.callAll: to rank[" + (i + 1)
            + "] arg_pos = " + arg_pos);

      // send callAllAsync to other nodes
      MASS.getRemoteNodes().get(i).sendMessage(m);
      if (MASS.isConsoleLoggingEnabled()) {
        System.err.println("AGENTS_CALL_ALL_ASYNC " + m.getAction()
            + " sent to " + i);
        System.err.println("Bag Size is: "
            + MASS_base.getAgentsMap().get(new Integer(getHandle()))
                .getAgents().size_unreduced());
      }
    }

    for (int i = 0; i < asyncQueueSize(); i++) {
      getAgents().get(asyncQueueGet(i)).setAsyncArgument(arguments[i]);
      getAgents().get(asyncQueueGet(i)).setAutoMigrationStartingIndex(i * lastDimensionLength);
    }
    // shared between agents
    // TODO What is share here?

    // We need this so AsyncInputThread can quickly pass the migration request
    /*
     * MASS_base.setCurrentFunctionId(functionId);
     * MASS_base.setCurrentArgument(argument);
     * MASS_base.setCurrentMsgType(type);
     */

    // resume threads
    if (MASS.isConsoleLoggingEnabled()) {
      MASS_base.log("MASS_base.currentgAgents = "
          + MASS_base.getCurrentAgents());
    }

    boolean asyncQueueComplete = false;
    do {
      if (MASS.isConsoleLoggingEnabled())
        MASS.log("Begin callAllAsync loop");
      
      // Mark myself as busy executing my async queue
      setIsAsyncLoopIdle(false);
      // callAllAsync to all slave threads
      Mthread.resumeThreads(Mthread.STATUS_TYPE.STATUS_AGENTSCALLALL_ASYNC);

      // callAllAsync in my own thread
      super.callAllAsync(0);

      // Done with processing my async queue
      setIsAsyncLoopIdle(true);
      synchronized (getAsyncQueue()) {
          asyncQueueComplete = asyncQueueIsEmpty() && hasNoInprocessAgents();
          if(MASS.isConsoleLoggingEnabled()) {
            MASS.log("getAsyncQueue().isEmpty() && hasNoInprocessAgents() = " 
                + asyncQueueIsEmpty() + " && " + hasNoInprocessAgents() +
                "; MASS.getChildAgentPids().isEmpty() = " +
                MASS.getChildAgentPids().isEmpty());
          }
          while((!MASS.getChildAgentPids().isEmpty() || 
              !MASS.getAsyncOutputThread().isIdle()
              || !MASS.getAsyncInputThread().isIdle(false))
              && asyncQueueComplete) {
              if (MASS.isConsoleLoggingEnabled()) {
                MASS.log(MASS.getChildAgentPids().isEmpty() + " && "
                    + MASS.getAsyncOutputThread().isIdle() + " && "
                    + MASS.getAsyncInputThread().isIdle(false) + 
                    " getAsyncQueue().size() = " + asyncQueueSize());
              }
              try {
                getAsyncQueue().wait();
              } catch (InterruptedException e) {
                MASS.logException(null, e);
              }              
              asyncQueueComplete = asyncQueueIsEmpty() && hasNoInprocessAgents();
            }
          }
      
      // confirm all threads are done with agents.callAllAsync
      // backward compatibility barrier twice,
      // once in callAllAsync in each thread, but then slave thread
      // enter another barrier at the end of Mthread.run() while() loop
      // so master thread has to barrier here again to get every one back onto
      // the top
      //Mthread.barrierThreads(0);
    } while (!asyncQueueComplete);
    
    collectAsyncResult();
    return getCompleteQueue();
  }

  public void callAll(int functionId) {
    ca_setup(functionId, null, Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT);
  }

  public void callAll(int functionId, Object argument) {
    ca_setup(functionId, argument,
        Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT);
  }

  public Object callAll(int functionId, Object[] argument) {
    return ca_setup(functionId, argument,
        Message.ACTION_TYPE.AGENTS_CALL_ALL_RETURN_OBJECT);
  }

  public List<Agent> callAllAsync(int[] functionIds,
      Object[] arguments) throws Exception {
    return ca_setupAsync(functionIds, arguments, false);
  }
  
  public List<Agent> callAllAsync(int[] functionIds,
      Object[] arguments, boolean autoMigration) throws Exception {
    return ca_setupAsync(functionIds, arguments, autoMigration);
  }

  public void init_master(Object argument) {

    // check if MASS_base.hosts is empty (i.e., Places not yet created)
    if (MASS_base.getHosts().isEmpty()) {
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
      if (MASS.isConsoleLoggingEnabled() == true)
        MASS_base.log("AGENT_INITIALIZE sent to " + node.getPid());

    }

    // Synchronized with all slave processes
    MASS.barrier_all_slaves(localAgents);
    localAgents[0] = getLocalPopulation();

    total = 0;
    for (int i = 0; i < MASS_base.getSystemSize(); i++) {

      total += localAgents[i];
      // for debugging

      if (MASS.isConsoleLoggingEnabled())
        System.err.println("rank[" + i + "]'s local agent population = "
            + localAgents[i]);

    }

    // register this agents in the places hash map
    MASS_base.getAgentsMap().put(new Integer(getHandle()), this);

  }

  public void ma_setup() {

    // send an AGENTS_MANAGE_ALL message to each slave
    Message m = null;
    for (MNode node : MASS.getRemoteNodes()) {

      // create a message
      m = new Message(Message.ACTION_TYPE.AGENTS_MANAGE_ALL, this.getHandle(),
          0);

      // send it
      node.sendMessage(m);

      // MThread Update
      Mthread.setAgentBagSize(MASS_base.getAgentsMap()
          .get(new Integer(getHandle())).getAgents().size_unreduced());

    }

    // retrieve the corresponding agents
    MASS_base.setCurrentAgents(this);
    MASS_base.setCurrentMsgType(Message.ACTION_TYPE.AGENTS_MANAGE_ALL);

    // resume threads
    Mthread.resumeThreads(Mthread.STATUS_TYPE.STATUS_MANAGEALL);

    // callall implementatioin
    super.manageAll(0); // 0 = the main thread id

    // confirm all threads are done with agents.callAll
    Mthread.barrierThreads(0);

    // Synchronized with all slave processes
    MASS.barrier_all_slaves(localAgents);
    localAgents[0] = getLocalPopulation();

    total = 0;
    for (int i = 0; i < MASS_base.getSystemSize(); i++) {

      total += localAgents[i];

      // for debugging
      if (MASS.isConsoleLoggingEnabled() == true)
        System.err.println("rank[" + i + "]'s local agent population = "
            + localAgents[i]);

    }

  }

  public void manageAll() {
    ma_setup();
  }

  public int nAgents() {

    int nAgents = 0;
    for (int i = 0; i < MASS_base.getSystemSize(); i++)
      nAgents += localAgents[i];

    return nAgents;

  }

  private void collectAsyncResult() {
    // TODO Auto Migration somewhere?

    // in case of killing agent, backward compatibility
    getAgents().reduce();
    setLocalPopulation(getAgents().size_unreduced());
    localAgents[0] = getLocalPopulation();

    MASS.getRemoteAsyncResults();
    Collections.sort(getCompleteQueue(), new AgentAsyncComparator());
    MASS_base.setCurrentReturns(getCompleteQueue().toArray());
    for (int i = 1; i < MASS_base.getSystemSize(); i++) {
      localAgents[i] = MASS.getLocalAgents()[i - 1];
    }
    total = 0;
    for (int i = 0; i < MASS_base.getSystemSize(); i++) {
      total += localAgents[i];
      // for debugging
      if (MASS.isConsoleLoggingEnabled()) {
        System.err.println("rank[" + i + "]'s local agent population = "
            + localAgents[i]);
      }
    }
  }
}