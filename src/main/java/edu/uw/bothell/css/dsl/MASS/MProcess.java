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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;

/**
 *	MProcess exists to facilitate message-passing between remote and master nodes. 
 */
public class MProcess {

  private int myPid; // my pid or rank
  private ObjectInputStream MAIN_IOS; // input from the master process
  private ObjectOutputStream MAIN_OOS; // output to the master process

	// logging
	private Log4J2Logger logger = Log4J2Logger.getInstance();

  /**
   * Main MASS function that launches MProcess
   * @param args
   */
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

  /**
   * MProcesses are the MASS threads executing on various machines.  They are 
   * responsible for maintaining some number of the total Places being used by
   * the entire MASS program, as well as the associated Agents.  Each MProcess
   * is referred to by its rank.
   * @param hostName
   * @param myPid
   * @param nProc
   * @param nThr
   * @param port
   * @param curDir
   */
  public MProcess(String hostName, int myPid, int nProc, int nThr, int port,
      String curDir) {
    // this.hostName = hostName;
    this.myPid = myPid;
    // this.nProc = nProc;
    MASS.setNumThreads(nThr);
    MASSBase.setWorkingDirectory(curDir); // mprocess manually changes it.
    MASSBase.initMASSBase(hostName, myPid, nProc, port);

      logger.debug("Launching MProcess... (" + "hostname = " + hostName
          + ", myPid = " + myPid + ", nProc = " + nProc + ", nThr = " + nThr
          + ", port = " + port + ", curDir = " + curDir + ")");

    MASSBase.initializeThreads(MASS.getNumThreads());
    // set up a connection with the master process
    try {
      MAIN_IOS = new ObjectInputStream(System.in);
      MAIN_OOS = new ObjectOutputStream(System.out);
    } catch (Exception e) {
      logger.error("MProcess.Mprocess: detected ", e);
      System.exit(-1);
    }

  }

  Message receiveMessage() {

    try {
      return (Message) MAIN_IOS.readObject();
    } catch (Exception e) {
      logger.error("MProcess.receiveMessage: detected ", e);
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

      logger.error("MProcess.sendMessage: " + e);
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

    logger.debug("MProcess started");

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
      PlacesBase places = null; // new Places
      AgentsBase agents = null; // new Agents

      // retrieve an argument
      argument = m.getArgument();

      switch (m.getAction()) {

      case ACK:
        sendAck();
        break;

      case EMPTY:
        if (MASS.isConsoleLoggingEnabled())
          logger.debug("EMPTY received!!!!");
        sendAck();
        break;

      case FINISH:
        MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_TERMINATE);
        // confirm all threads are done with finish
        MThread.barrierThreads(0);
        MASSBase.getExchange().terminateConnection(this.myPid);
        sendAck();
        alive = false;
        // if( printOutput )
        // MASS_base.log( "FINISH received and ACK sent" );
        break;

      case PLACES_INITIALIZE:

        logger.debug("PLACES_INITIALIZE received");

        // create a new Places
        size = m.getSize();

        places = new PlacesBase(m.getHandle(), m.getClassname(),
            m.getBoundaryWidth(), argument, size);

        for (int i = 0; i < m.getHosts().size(); i++)
          hosts.add(m.getHosts().get(i));
        // establish all inter-node connections within setHosts( )
        MASSBase.setHosts(hosts);

        MASSBase.getPlacesMap().put(new Integer(m.getHandle()), places);
        sendAck();
        logger.debug("PLACES_INITIALIZE completed and ACK sent");
        break;

      case PLACES_CALL_ALL_VOID_OBJECT:

        logger.debug("PLACES_CALL_ALL_VOID_OBJECT received");

        // retrieve the corresponding places
        MASSBase.setCurrentPlacesBase(MASSBase.getPlacesMap().get(
            new Integer(m.getHandle())));
        MASSBase.setCurrentFunctionId(m.getFunctionId());
        MASSBase.setCurrentArgument(argument);
        MASSBase.setCurrentMsgType(m.getAction());

        // resume threads to work on call all.
        MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_CALLALL);

        // 3rd arg: 0 = the main thread id
        MASSBase.getCurrentPlacesBase().callAll(m.getFunctionId(), argument, 0);

        // confirm all threads are done with places.callAll
        MThread.barrierThreads(0);

        sendAck();
        break;

      case PLACES_CALL_ALL_RETURN_OBJECT:

        logger.debug("PLACES_CALL_ALL_RETURN_OBJECT received");

        // retrieve the corresponding places
        MASSBase.setCurrentPlacesBase(MASSBase.getPlacesMap().get(
            new Integer(m.getHandle())));
        MASSBase.setCurrentFunctionId(m.getFunctionId());
        MASSBase.setCurrentArgument(argument);
        MASSBase.setCurrentMsgType(m.getAction());
        MASSBase.setCurrentReturns(new Object[MASSBase.getCurrentPlacesBase()
            .getPlacesSize()]);

        // resume threads to work on call all.
        MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_CALLALL);

        // 3rd arg: 0 = the main thread id
        MASSBase.getCurrentPlacesBase().callAll(MASSBase.getCurrentFunctionId(),
            (Object[]) (MASSBase.getCurrentArgument()),
            ((Object[]) (MASSBase.getCurrentArgument())).length, 0);

        // confirm all threads are done with places.callAll w/ return
        MThread.barrierThreads(0);

        sendReturnValues(MASSBase.getCurrentReturns());
        break;

      case PLACES_EXCHANGE_ALL:

        logger.debug("PLACES_EXCHANGE_ALL recweived handle = "
              + m.getHandle() + " dest_handle = " + m.getDestHandle());

        // retrieve the corresponding places
        MASSBase.setCurrentPlacesBase(MASSBase.getPlacesMap().get(
            new Integer(m.getHandle())));
        MASSBase.setDestinationPlaces(MASSBase.getPlacesMap().get(
            new Integer(m.getDestHandle())));
        MASSBase.setCurrentFunctionId(m.getFunctionId());
        // MASS_base.currentDestinations = m.getDestinations( );

        // reset requestCounter by the main thread
        MASSBase.resetRequestCounter();

        // for debug
        MASSBase.showHosts();

        // resume threads to work on call all.
        MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_EXCHANGEALL);

        // exchangeall implementation
        MASSBase.getCurrentPlacesBase().exchangeAll(
            MASSBase.getDestinationPlaces(), MASSBase.getCurrentFunctionId(),
            0);

        // confirm all threads are done with places.exchangeall.
        MThread.barrierThreads(0);

        logger.debug("barrier done");

        sendAck();

        logger.debug("PLACES_EXCHANGE_ALL sent ACK");

        break;

      case PLACES_EXCHANGE_BOUNDARY:

        logger.debug("PLACES_EXCHANGE_BOUNDARY received handle = {}", m.getHandle());

        // retrieve the corresponding places
        MASSBase.setCurrentPlacesBase(MASSBase.getPlacesMap().get(new Integer(m.getHandle())));

        // for debug
        MASSBase.showHosts();

        // exchange boundary implementation
        MASSBase.getCurrentPlacesBase().exchangeBoundary();

        sendAck();

        logger.debug("PLACES_EXCHANGE_BOUNDARY completed and ACK sent");

        break;

      case PLACES_EXCHANGE_ALL_REMOTE_REQUEST:
      case PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT:
      case PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST:
        break;

      case AGENTS_INITIALIZE:

        logger.debug("AGENTS_INITIALIZE received");

        agents = new AgentsBase(m.getHandle(), m.getClassname(), argument,
            m.getDestHandle(), m.getAgentPopulation());

        MASSBase.getAgentsMap().put(new Integer(m.getHandle()), agents);

        sendAck(agents.getLocalPopulation());

        logger.debug("AGENTS_INITIALIZE completed and ACK sent");

        break;

      case AGENTS_CALL_ALL_VOID_OBJECT:

        logger.debug("AGENTS_CALL_ALL_VOID_OBJECT received");

        MASSBase.setCurrentAgentsBase(MASSBase.getAgentsMap().get(
            new Integer(m.getHandle())));
        MASSBase.setCurrentFunctionId(m.getFunctionId());
        MASSBase.setCurrentArgument(argument);
        MASSBase.setCurrentMsgType(m.getAction());

        MThread.setAgentBagSize(MASSBase.getCurrentAgentsBase().getAgents()
            .size_unreduced());

        // resume threads to work on call all
        MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_AGENTSCALLALL);

        MASSBase.getCurrentAgentsBase().callAll(m.getFunctionId(), argument, 0);

        // confirm all threads are done with agents.callAll
        MThread.barrierThreads(0);

        logger.debug("barrier done");

        sendAck(MASSBase.getCurrentAgentsBase().getLocalPopulation());
        break;

      case AGENTS_CALL_ALL_RETURN_OBJECT:

        logger.debug("AGENTS_CALL_ALL_RETURN_OBJECT received");

        MASSBase.setCurrentAgentsBase(MASSBase.getAgentsMap().get(
            new Integer(m.getHandle())));
        MASSBase.setCurrentFunctionId(m.getFunctionId());
        MASSBase.setCurrentArgument(argument);
        MASSBase.setCurrentMsgType(m.getAction());
        MASSBase.setCurrentReturns(new Object[MASSBase.getCurrentAgentsBase()
            .getLocalPopulation()]);

        MThread.setAgentBagSize(MASSBase.getCurrentAgentsBase().getAgents()
            .size_unreduced());

        // resume threads to work on call all with return objects
        MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_AGENTSCALLALL);

        MASSBase.getCurrentAgentsBase().callAll(MASSBase.getCurrentFunctionId(),
            (Object[]) (MASSBase.getCurrentArgument()), 0);

        // confirm all threads are done with agnets.callAll with
        // return objects
        MThread.barrierThreads(0);
        logger.debug("barrier done");

        sendReturnValues(MASSBase.getCurrentReturns(), MASSBase
            .getCurrentAgentsBase().getLocalPopulation());

        break;

      case AGENTS_MANAGE_ALL:

        logger.debug("AGENTS_MANAGE_ALL received");

        MASSBase.setCurrentAgentsBase(MASSBase.getAgentsMap().get(
            new Integer(m.getHandle())));
        MThread.setAgentBagSize(MASSBase.getCurrentAgentsBase().getAgents()
            .size_unreduced());

        MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_MANAGEALL);

        MASSBase.getCurrentAgentsBase().manageAll(0); // 0 = the main tid

        // confirm all threads are done with agents.manageAll.
        MThread.barrierThreads(0);

        logger.debug("sendAck will send localPopulation = {}", MASSBase.getCurrentAgentsBase().getLocalPopulation());

        sendAck(MASSBase.getCurrentAgentsBase().getLocalPopulation());

        break;

      case AGENTS_CALL_ALL_ASYNC_RETURN_OBJECT:

    	  logger.debug("AGENTS_CALL_ALL_ASYNC_RETURN_OBJECT received");

        MASSBase.prepareAsyncExecution(MASSBase.getAgentsMap().get(
            new Integer(m.getHandle())), m.getFunctionIds());
        Object[] arguments = (Object[]) argument;
        int[] autoMigrationStartIndices = m.getAutoMigrationStartingIndex();

        for (int i = 0; i < MASSBase.getCurrentAgentsBase().asyncAgentIdListSize(); i++) {
          if(arguments != null) {
            MASSBase.getCurrentAgentsBase().getAgents()
              .get(MASSBase.getCurrentAgentsBase().asyncAgentIdListGet(i)).setAsyncArgument(arguments[i]);
          }
          if(autoMigrationStartIndices != null) {
            MASSBase.getCurrentAgentsBase().getAgents()
            .get(MASSBase.getCurrentAgentsBase().asyncAgentIdListGet(i)).setAutoMigrationStartingIndex(i);
          }
        }

        if (!MASSBase.getCurrentAgentsBase().asyncAgentIdListIsEmpty()) {
          MASSBase.setSourceAgentPid(0); // Need to notify Master
          MASSBase.getInAsyncAgents()[0] += MASSBase.getCurrentAgentsBase().asyncAgentIdListSize();
        } else {
          MASSBase.setSourceAgentPid(-1);
        }
        // MASS_base.setCurrentReturns(new
        // Object[MASS_base.getCurrentAgents().getLocalPopulation()]); //
        // prepare an entire return space
        // resume threads
          logger.debug("MASS_base.currentgAgents = {}", MASSBase.getCurrentAgentsBase());
          logger.debug("MASS_base.getCurrentgAgents = {}", MASSBase.getCurrentAgentsBase());

        do {
          
        	// Mark myself as busy processing my async queue
          MASSBase.getCurrentAgentsBase().setIsAsyncLoopIdle(false);
          logger.debug("begin callAllAsync loop");

          // resume threads to work on call all
          MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_AGENTSCALLALL_ASYNC);
          try {
            //MASSBase.getCurrentAgentsBase().callAllAsync(0);
          } catch(Exception e) {
            logger.error("Unknown exception sending async callAll", e);
          }

          // confirm all threads are done with agents.callAllAsync
          // tell master that I'm done
          synchronized (MASSBase.getCurrentAgentsBase().getAsyncAgentIdList()) {
              /*
              logger.debug("output idle = "
                  + MASSBase.getAsyncOutputThread().isIdle()
                  + ", input idle = "
                  + MASSBase.getAsyncInputThread().isIdle(false)
                  + ", output migrate set is empty = "
                  + MASSBase.getChildAgentPids().isEmpty());
                */
            /*
            while ( (MASSBase.getCurrentAgentsBase().asyncAgentIdListIsEmpty() && MASSBase
                .getCurrentAgentsBase().hasNoInprocessAgents())
                && (!MASSBase.getAsyncOutputThread().isIdle()
                || !MASSBase.getAsyncInputThread().isIdle(false)
                || !MASSBase.getChildAgentPids().isEmpty())) {
              try {
                MASSBase.getCurrentAgentsBase().getAsyncAgentIdList().wait();
              } catch (InterruptedException e) {
              }
            }
            */

            // I'm done with my async queue and agents migration
            MASSBase.getCurrentAgentsBase().setIsAsyncLoopIdle(true);

            // Notify master that I am done
            /*
             * When an agent is started to run it is taken out from the async queue
             * However, an agent might still be running even if it is removed from the queue,
                therefore, we need to make sure if there is any agent is running.
             * When an agent stops running,  inProcessAgentCount variable is decreased by one
                in AgentsBase.java
            */
            if (MASSBase.getCurrentAgentsBase().asyncAgentIdListIsEmpty()
                && MASSBase.getCurrentAgentsBase().hasNoInprocessAgents()
                && MASSBase.getChildAgentPids().isEmpty()
                && MASSBase.getSourceAgentPid() > -1)
            {
              /*
                MASSBase.getAsyncOutputThread()
                    .notifySourceOfCompleteness(
                        MASSBase.getInAsyncAgents()[MASSBase
                            .getSourceAgentPid()]);*/
            }

            while ((!MASSBase.getCurrentAgentsBase().getResultRequestFromMaster()
                && MASSBase.getCurrentAgentsBase().asyncAgentIdListIsEmpty() && MASSBase
                .getCurrentAgentsBase().hasNoInprocessAgents())) {
                logger.debug("After notifying Master: "
                    + !MASSBase.getCurrentAgentsBase()
                        .getResultRequestFromMaster() + " && "
                    + MASSBase.getCurrentAgentsBase().asyncAgentIdListIsEmpty());
              }
              try {
                MASSBase.getCurrentAgentsBase().getAsyncAgentIdList().wait();
              } catch (InterruptedException e) {
              }
            }
            logger.debug("end of callAllAsync loop: "
                + !MASSBase.getCurrentAgentsBase().getResultRequestFromMaster());

          // Mthread.barrierThreads(0);
        } while (!MASSBase.getCurrentAgentsBase().getResultRequestFromMaster());


        logger.debug("barrier done callAll_ASync");
        
        break;
      }

    }

  }

}