package edu.uw.bothell.css.dsl.MASS;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.Vector;

public class MProcess {

  // @SuppressWarnings("unused")
  // private String hostName; // my local host name
  private int myPid; // my pid or rank
  // @SuppressWarnings("unused")
  // private int nProc; // # processes

  // @SuppressWarnings("unused")
  // private Vector<String> hosts; // all hosts participated in computation
  private ObjectInputStream MAIN_IOS; // input from the master process

  private ObjectOutputStream MAIN_OOS; // output to the master process

  public static void main(String[] args) throws Exception {
    String hostName = args[0];
    int myPid = Integer.parseInt(args[1]);
    int nProc = Integer.parseInt(args[2]);
    int nThreads = Integer.parseInt(args[3]);
    int serverPort = Integer.parseInt(args[4]);
    String curDir = args[5];

    MProcess mprocess = new MProcess(hostName, myPid, nProc, nThreads,
        serverPort, curDir);
    mprocess.start();

  }

  public MProcess(String hostName, int myPid, int nProc, int nThr, int port,
      String curDir) {
    // this.hostName = hostName;
    this.myPid = myPid;
    // this.nProc = nProc;
    MASS.setNumThreads(nThr);
    MASS_base.setWorkingDirectory(curDir); // mprocess manually changes it.
    MASS_base.initMASS_base(hostName, myPid, nProc, port);

    if (MASS.isConsoleLoggingEnabled()) {
      MASS_base.log("Launching MProcess... (" + "hostname = " + hostName
          + ", myPid = " + myPid + ", nProc = " + nProc + ", nThr = " + nThr
          + ", port = " + port + ", curDir = " + curDir + ")");
    }

    MASS_base.initializeThreads(MASS.getNumThreads());
    // set up a connection with the master process
    try {
      MAIN_IOS = new ObjectInputStream(System.in);
      MAIN_OOS = new ObjectOutputStream(System.out);
    } catch (Exception e) {
      MASS_base.logException("MProcess.Mprocess: detected ", e);
      System.exit(-1);
    }

  }

  Message receiveMessage() {

    try {
      return (Message) MAIN_IOS.readObject();
    } catch (Exception e) {
      MASS_base.logException("MProcess.receiveMessage: detected ", e);
      System.exit(-1);
    }

    return null;

  }

  private void sendAck() {
    Message msg = new Message(Message.ACTION_TYPE.ACK);
    sendMessage(msg);
  }

  private void sendAck(int localPopulation) {

    Message msg = new Message(Message.ACTION_TYPE.ACK, localPopulation);
    // if( printOutput ) {
    // MASS_base.log( "msg.getAgentPopulation = " +
    // msg.getAgentPopulation( ) );
    // }
    sendMessage(msg);

  }

  private void sendMessage(Message msg) {

    try {

      MAIN_OOS.writeObject(msg);
      MAIN_OOS.flush();

    } catch (Exception e) {

      MASS_base.log("MProcess.sendMessage: " + e);
      System.exit(-1);

    }

  }

  private void sendReturnValues(Object argument) {
    Message msg = new Message(Message.ACTION_TYPE.ACK, argument);
    sendMessage(msg);
  }

  private void sendReturnValues(Object argument, int localPopulation) {
    Message msg = new Message(Message.ACTION_TYPE.ACK, argument,
        localPopulation);
    sendMessage(msg);
  }

  @SuppressWarnings("incomplete-switch")
  public void start() {

    MASS_base.log("MProcess started");

    // Synchronize with the master node first.
    sendAck();

    boolean alive = true;
    while (alive) {

      // receive a new message from the master
      Message m = receiveMessage();

      // if ( printOutput )
      // MASS_base.log( "A new message received: action = " +
      // m.getAction( ) );

      // get prepared for the following arguments for PLACES_INITIALIZE
      int[] size; // size[]
      Vector<String> hosts = new Vector<String>();
      Object argument = null;
      Places_base places = null; // new Places
      Agents_base agents = null; // new Agents

      // retrieve an argument
      argument = m.getArgument();

      switch (m.getAction()) {

      case ACK:
        sendAck();
        break;

      case EMPTY:
        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("EMPTY received!!!!");
        sendAck();
        break;

      case FINISH:
        Mthread.resumeThreads(Mthread.STATUS_TYPE.STATUS_TERMINATE);
        // confirm all threads are done with finish
        Mthread.barrierThreads(0);
        MASS_base.getExchange().terminateConnection(this.myPid);
        MASS_base.getAsyncOutputThread().finish();
        MASS_base.getAsyncInputThread().finish();
        sendAck();
        alive = false;
        // if( printOutput )
        // MASS_base.log( "FINISH received and ACK sent" );
        break;

      case PLACES_INITIALIZE:

        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("PLACES_INITIALIZE received");

        // create a new Places
        size = m.getSize();

        places = new Places_base(m.getHandle(), m.getClassname(),
            m.getBoundaryWidth(), argument, size);

        for (int i = 0; i < m.getHosts().size(); i++)
          hosts.add(m.getHosts().get(i));
        // establish all inter-node connections within setHosts( )
        MASS_base.setHosts(hosts);

        MASS_base.getPlacesMap().put(new Integer(m.getHandle()), places);
        sendAck();
        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("PLACES_INITIALIZE completed and ACK sent");
        break;

      case PLACES_CALL_ALL_VOID_OBJECT:

        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("PLACES_CALL_ALL_VOID_OBJECT received");

        // retrieve the corresponding places
        MASS_base.setCurrentPlaces(MASS_base.getPlacesMap().get(
            new Integer(m.getHandle())));
        MASS_base.setCurrentFunctionId(m.getFunctionId());
        MASS_base.setCurrentArgument(argument);
        MASS_base.setCurrentMsgType(m.getAction());

        // resume threads to work on call all.
        Mthread.resumeThreads(Mthread.STATUS_TYPE.STATUS_CALLALL);

        // 3rd arg: 0 = the main thread id
        MASS_base.getCurrentPlaces().callAll(m.getFunctionId(), argument, 0);

        // confirm all threads are done with places.callAll
        Mthread.barrierThreads(0);

        sendAck();
        break;

      case PLACES_CALL_ALL_RETURN_OBJECT:

        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("PLACES_CALL_ALL_RETURN_OBJECT received");

        // retrieve the corresponding places
        MASS_base.setCurrentPlaces(MASS_base.getPlacesMap().get(
            new Integer(m.getHandle())));
        MASS_base.setCurrentFunctionId(m.getFunctionId());
        MASS_base.setCurrentArgument(argument);
        MASS_base.setCurrentMsgType(m.getAction());
        MASS_base.setCurrentReturns(new Object[MASS_base.getCurrentPlaces()
            .getPlacesSize()]);

        // resume threads to work on call all.
        Mthread.resumeThreads(Mthread.STATUS_TYPE.STATUS_CALLALL);

        // 3rd arg: 0 = the main thread id
        MASS_base.getCurrentPlaces().callAll(MASS_base.getCurrentFunctionId(),
            (Object[]) (MASS_base.getCurrentArgument()),
            ((Object[]) (MASS_base.getCurrentArgument())).length, 0);

        // confirm all threads are done with places.callAll w/ return
        Mthread.barrierThreads(0);

        // if ( printOutput )
        // MASS_base.log( "PLACES_CALL_ALL_RETURN_OBJECT " +
        // "checking currentReturns" );

        sendReturnValues(MASS_base.getCurrentReturns());
        break;

      case PLACES_EXCHANGE_ALL:

        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("PLACES_EXCHANGE_ALL recweived handle = "
              + m.getHandle() + " dest_handle = " + m.getDestHandle());

        // retrieve the corresponding places
        MASS_base.setCurrentPlaces(MASS_base.getPlacesMap().get(
            new Integer(m.getHandle())));
        MASS_base.setDestinationPlaces(MASS_base.getPlacesMap().get(
            new Integer(m.getDestHandle())));
        MASS_base.setCurrentFunctionId(m.getFunctionId());
        // MASS_base.currentDestinations = m.getDestinations( );

        // reset requestCounter by the main thread
        MASS_base.resetRequestCounter();

        // for debug
        MASS_base.showHosts();

        // resume threads to work on call all.
        Mthread.resumeThreads(Mthread.STATUS_TYPE.STATUS_EXCHANGEALL);

        // exchangeall implementation
        MASS_base.getCurrentPlaces().exchangeAll(
            MASS_base.getDestinationPlaces(), MASS_base.getCurrentFunctionId(),
            0);

        // confirm all threads are done with places.exchangeall.
        Mthread.barrierThreads(0);

        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("barrier done");

        sendAck();

        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("PLACES_EXCHANGE_ALL sent ACK");

        break;

      case PLACES_EXCHANGE_BOUNDARY:

        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("PLACES_EXCHANGE_BOUNDARY received handle="
              + m.getHandle());

        // retrieve the corresponding places
        MASS_base.setCurrentPlaces(MASS_base.getPlacesMap().get(
            new Integer(m.getHandle())));

        // for debug
        MASS_base.showHosts();

        // exchange boundary implementation
        MASS_base.getCurrentPlaces().exchangeBoundary();

        sendAck();

        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("PLACES_EXCHANGE_BOUNDARY " + "completed and ACK sent");

        break;

      case PLACES_EXCHANGE_ALL_REMOTE_REQUEST:
      case PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT:
      case PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST:
        break;

      case AGENTS_INITIALIZE:

        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("AGENTS_INITIALIZE received");

        agents = new Agents_base(m.getHandle(), m.getClassname(), argument,
            m.getDestHandle(), m.getAgentPopulation());

        MASS_base.getAgentsMap().put(new Integer(m.getHandle()), agents);

        sendAck(agents.getLocalPopulation());

        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("AGENTS_INITIALIZE completed and ACK sent");

        break;

      case AGENTS_CALL_ALL_VOID_OBJECT:

        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("AGENTS_CALL_ALL_VOID_OBJECT received");

        MASS_base.setCurrentAgents(MASS_base.getAgentsMap().get(
            new Integer(m.getHandle())));
        MASS_base.setCurrentFunctionId(m.getFunctionId());
        MASS_base.setCurrentArgument(argument);
        MASS_base.setCurrentMsgType(m.getAction());

        Mthread.setAgentBagSize(MASS_base.getCurrentAgents().getAgents()
            .size_unreduced());

        // resume threads to work on call all
        Mthread.resumeThreads(Mthread.STATUS_TYPE.STATUS_AGENTSCALLALL);

        MASS_base.getCurrentAgents().callAll(m.getFunctionId(), argument, 0);

        // confirm all threads are done with agents.callAll
        Mthread.barrierThreads(0);

        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("barrier done");

        sendAck(MASS_base.getCurrentAgents().getLocalPopulation());
        break;

      case AGENTS_CALL_ALL_RETURN_OBJECT:

        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("AGENTS_CALL_ALL_RETURN_OBJECT received");

        MASS_base.setCurrentAgents(MASS_base.getAgentsMap().get(
            new Integer(m.getHandle())));
        MASS_base.setCurrentFunctionId(m.getFunctionId());
        MASS_base.setCurrentArgument(argument);
        MASS_base.setCurrentMsgType(m.getAction());
        MASS_base.setCurrentReturns(new Object[MASS_base.getCurrentAgents()
            .getLocalPopulation()]);

        Mthread.setAgentBagSize(MASS_base.getCurrentAgents().getAgents()
            .size_unreduced());

        // resume threads to work on call all with return objects
        Mthread.resumeThreads(Mthread.STATUS_TYPE.STATUS_AGENTSCALLALL);

        MASS_base.getCurrentAgents().callAll(MASS_base.getCurrentFunctionId(),
            (Object[]) (MASS_base.getCurrentArgument()),
            ((Object[]) (MASS_base.getCurrentArgument())).length, 0);

        // confirm all threads are done with agnets.callAll with
        // return objects
        Mthread.barrierThreads(0);
        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("barrier done");

        sendReturnValues(MASS_base.getCurrentReturns(), MASS_base
            .getCurrentAgents().getLocalPopulation());

        break;

      case AGENTS_MANAGE_ALL:

        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("AGENTS_MANAGE_ALL received");

        MASS_base.setCurrentAgents(MASS_base.getAgentsMap().get(
            new Integer(m.getHandle())));
        Mthread.setAgentBagSize(MASS_base.getCurrentAgents().getAgents()
            .size_unreduced());

        Mthread.resumeThreads(Mthread.STATUS_TYPE.STATUS_MANAGEALL);

        MASS_base.getCurrentAgents().manageAll(0); // 0 = the main tid

        // confirm all threads are done with agents.manageAll.
        Mthread.barrierThreads(0);

        if (MASS.isConsoleLoggingEnabled())
          MASS_base.log("sendAck will send localPopulation = "
              + MASS_base.getCurrentAgents().getLocalPopulation());

        sendAck(MASS_base.getCurrentAgents().getLocalPopulation());

        break;

      case AGENTS_CALL_ALL_ASYNC_RETURN_OBJECT:
        if (MASS.isConsoleLoggingEnabled()) {
          MASS_base.log("AGENTS_CALL_ALL_ASYNC_RETURN_OBJECT received");
        }

        MASS_base.prepareAsyncExecution(MASS_base.getAgentsMap().get(
            new Integer(m.getHandle())));
        Object[] arguments = (Object[]) argument;
        int idx = 0;
        for (Iterator<Agent> iter = MASS_base.getCurrentAgents()
            .getAsyncQueue().iterator(); iter.hasNext();) {
          Agent agent = iter.next();
          agent.setAsyncFuncList(m.getFunctionIds());
          agent.resetAsyncResults();
          agent.setAsyncArgument(arguments[idx]);
          agent.setMyAsyncOriginalPid(MASS_base.getMyPid());
          agent.setMyOriginalAsyncIndex(idx);
          agent.setCurrentIndex(idx);
          agent.setParentAgents(MASS_base.getCurrentAgents());
          ++idx;
        }
        if(idx == 0) {
          // has no agent
          MASS_base.getAsyncOutputThread().setSendCompleteNotifyToMaster(false);
        } else {
          MASS_base.getAsyncOutputThread().setSendCompleteNotifyToMaster(true);
        }
        // MASS_base.setCurrentReturns(new
        // Object[MASS_base.getCurrentAgents().getLocalPopulation()]); //
        // prepare an entire return space
        // resume threads
        if (MASS.isConsoleLoggingEnabled()) {
          MASS_base.log("MASS_base.currentgAgents = "
              + MASS_base.getCurrentAgents());
          MASS_base.log("MASS_base.getCurrentgAgents = "
              + MASS_base.getCurrentAgents());
        }

        do {
          // Mark myself as busy processing my async queue
          MASS_base.getCurrentAgents().setIsAsyncLoopIdle(false);
          if (MASS.isConsoleLoggingEnabled()) {
            MASS_base.log("begin callAllAsync loop");
          }
          // resume threads to work on call all
          Mthread.resumeThreads(Mthread.STATUS_TYPE.STATUS_AGENTSCALLALL_ASYNC);
          MASS_base.getCurrentAgents().callAllAsync(0);

          // confirm all threads are done with agents.callAllAsync
          // tell master that I'm done
          synchronized (MASS_base.getCurrentAgents().getAsyncQueue()) {
                while (!MASS_base.getAsyncOutputThread().isIdle()
                    || !MASS_base.getAsyncInputThread().isIdle(false)
                    || !MASS_base.getOutputMigrateSet().isEmpty()
                    /*|| !MASS_base.getCurrentAgents().getAsyncQueue().isEmpty()
                    || !MASS_base.getCurrentAgents().hasNoInprocessAgents() */
                    ) {
                  if ( MASS.isConsoleLoggingEnabled()) {
                    MASS_base.log("output idle = "
                        + MASS_base.getAsyncOutputThread().isIdle()
                        + ", input idle = "
                        + MASS_base.getAsyncInputThread().isIdle(false)
                        + ", output migrate set is empty = "
                        + MASS_base.getOutputMigrateSet().isEmpty());
                  }
                  try {
                    MASS_base.getCurrentAgents().getAsyncQueue().wait();
                  } catch (InterruptedException e) {
                  }
                }

            // I'm done with my async queue and agents migration
            MASS_base.getCurrentAgents().setIsAsyncLoopIdle(true);

            // tell master about that
            if (MASS_base.getCurrentAgents().getAsyncQueue().isEmpty()
                && MASS_base.getCurrentAgents().hasNoInprocessAgents()
                && MASS_base.getOutputMigrateSet().isEmpty()) {
              MASS_base.getAsyncOutputThread().notifyMigrateSenderOfCompleteness();
              MASS_base.notifyMasterOfCompleteness();
            }

            while (!MASS_base.getCurrentAgents().getResultRequestFromMaster()
                && MASS_base.getCurrentAgents().getAsyncQueue().isEmpty()
                && MASS_base.getCurrentAgents().hasNoInprocessAgents()) {
              if ( MASS.isConsoleLoggingEnabled()) {
                MASS_base.log("After notifying Master: "
                    + !MASS_base.getCurrentAgents()
                        .getResultRequestFromMaster() + " && "
                    + MASS_base.getCurrentAgents().getAsyncQueue().isEmpty());
              }
              try {
                MASS_base.getCurrentAgents().getAsyncQueue().wait();
              } catch (InterruptedException e) {
              }
            }
          }
          if (MASS.isConsoleLoggingEnabled()) {
            MASS_base.log("end of callAllAsync loop: "
                + !MASS_base.getCurrentAgents().getResultRequestFromMaster());
          }
          
        //  Mthread.barrierThreads(0);
        } while (!MASS_base.getCurrentAgents().getResultRequestFromMaster());
        
        if (MASS.isConsoleLoggingEnabled()) {
          MASS_base.log("barrier done callAll_ASync");
        }
        break;
      }

    }

  }

}