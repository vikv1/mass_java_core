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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;

/**
 * Handle async Migration req and other type of message from other nodes
 * 
 * @author hohung
 *
 */
public class AsyncInputThread extends Thread {
  
	private int portNumber;
	private ServerSocket serverSocket = null;
	private boolean listening;
	private volatile AtomicInteger runningChildThreadCount;

	// logging
	private Log4J2Logger logger = Log4J2Logger.getInstance();

  public AsyncInputThread(int port) {
    portNumber = port;
    runningChildThreadCount = new AtomicInteger(0);
  }

  public void run() {

	  logger.debug("AsyncInputThread start at port {}", portNumber);
    
	  listening = true;
    try {
      serverSocket = new ServerSocket(portNumber);
      while (listening) {
        new AsyncInputChildThread(serverSocket.accept()).start();
      }
    } catch (IOException e) {
      // Only Unexpected exception need to be logged
      if (listening || !(e instanceof SocketException)) {
        logger.error("Unexpected exception in AsyncInputThread.run()", e);
      }
    }
    logger.debug("AsyncInputThread.end");
  }

  public void finish() {
    listening = false;
    if (serverSocket != null) {
      logger.debug("AsyncInputThread tries to close server socket");
      try {
        serverSocket.close();
      } catch (IOException e) {
        logger.error("Exception thrown while attempting to close server socket", e);
      }
    }
    logger.debug("AsyncInputThread finishes");
  }

  /**
   * 
   * @param completeReqFromMaster
   *          : if true, including 1 req as complete
   * @return
   */
  public boolean isIdle(boolean completeReqFromMaster) {
    synchronized (MASSBase.getCurrentAgentsBase().getAsyncAgentIdList()) {
        logger.debug(completeReqFromMaster
            + " Asyncinput running child thread count = "
            + runningChildThreadCount.get());
      if (completeReqFromMaster) {
        return runningChildThreadCount.get() == 1;
      } else {
        return runningChildThreadCount.get() == 0;
      }
    }
  }

  private class AsyncInputChildThread extends Thread {
    private Socket socket = null;

    public AsyncInputChildThread(Socket socket) {
      logger.debug("construct AsyncInputChildThread");
      runningChildThreadCount.incrementAndGet();
      this.socket = socket;
    }

    public void run() {
      if (MASSBase.getCurrentAgentsBase() == null) {
        logger.error("MASS is not ready " + socket.getRemoteSocketAddress());
        return;
      }
      try {

    	  logger.debug("AsyncInputChildThread processing " + socket.getRemoteSocketAddress());
        
    	  InputStream is = socket.getInputStream();
    	  ObjectInputStream ois = new ObjectInputStream(is);
    	  Message m = (Message) ois.readObject();
          
    	  logger.debug("Receive m {}", m.getActionString());
        
    	  switch (m.getAction()) {
        case AGENTS_ASYNC_MIGRATION_REMOTE_REQUEST:
          // process a message
          Vector<AgentMigrationRequest> receivedRequests = m
              .getMigrationReqList();
          PlacesBase dstPlaces = MASSBase.getPlacesMap().get(
              new Integer(m.getDestHandle()));
          boolean chosen = false;
          synchronized (MASSBase.getCurrentAgentsBase().getAsyncAgentIdList()) {
            if (MASSBase.getMyPid() != 0 // Master doesn't need source ever
               // && MASS_base.getCurrentAgents().getAgents().estimateSize() == 0
                && MASSBase.getSourceAgentPid() == -1) {
                logger.debug("Choose {} as source", m.getSourcePid());
              }
              chosen = true;
              MASSBase.setSourceAgentPid(m.getSourcePid());
            }
          OutputStream os = socket.getOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(os);
          oos.writeObject(new AgentMigrationResponse(receivedRequests.size(), chosen));
          oos.flush();
          // retrieve agents from receiveRequest
          synchronized (MASSBase.getCurrentAgentsBase().getAsyncAgentIdList()) {
            for (AgentMigrationRequest request : receivedRequests) {
              int globalLinearIndex = request.destGlobalLinearIndex;
              Agent agent = request.agent;
              agent.setHasAlreadyGone(false);
              agent.setNeedsToGoBackToAsyncQueue(false);
              // local destination
              int destinationLocalLinearIndex = globalLinearIndex
                  - dstPlaces.getLowerBoundary();
                logger.debug(" dstLocalIndex = " + destinationLocalLinearIndex
                    + ", async func index = " + agent.getAsyncFuncListIndex());

              Place dstPlace = dstPlaces.getPlaces()[destinationLocalLinearIndex];

              // push this agent into the place and the entire agent bag.
              agent.setPlace(dstPlace);
              dstPlace.getAgents().add(agent); // auto sync
              agent.setCurrentIndex(MASSBase.getCurrentAgentsBase().getAgents()
                  .size_unreduced());
              agent.setMyAgentsBase(MASSBase.getCurrentAgentsBase());
              MASSBase.getCurrentAgentsBase().getAgents().add(agent);
              MASSBase.getCurrentAgentsBase().asyncAgentIdListAdd(agent.getCurrentIndex());
              logger.debug("migrate agent added to async queue, new size = {}", MASSBase.getCurrentAgentsBase().asyncAgentIdListSize());
            }
            MASSBase.getInAsyncAgents()[m.getSourcePid()] += receivedRequests.size();
              logger.debug("InAsync[" + m.getSourcePid() + "] is now " + MASSBase.getInAsyncAgents()[m.getSourcePid()]);
            MASSBase.getCurrentAgentsBase().getAsyncAgentIdList().notifyAll();
          }

          oos.close();
          os.close();
          break;
        case AGENT_ASYNC_RESULT:
          MASSBase.getCurrentAgentsBase().setResultRequestFromMaster(true);
          synchronized (MASSBase.getCurrentAgentsBase().getAsyncAgentIdList()) {
            MASSBase.getCurrentAgentsBase().setLocalPopulation(
                MASSBase.getCurrentAgentsBase().getAgents().size());
            MASSBase.getCurrentAgentsBase().getAsyncAgentIdList().notifyAll();
          }
          os = socket.getOutputStream();
          oos = new ObjectOutputStream(os);
            
          if (logger.isDebugEnabled()) {

        	  logger.debug("Return to master completeQueue of size {}", MASSBase.getCurrentAgentsBase().getAsyncCompletedAgentList().size());
	            
        	  for (Agent a : MASSBase.getCurrentAgentsBase().getAsyncCompletedAgentList()) {
	              logger.debug("agent result size = {}", a.asyncResultsSize());
	            }
        	  
	          }
    	  
          Message result = new Message(Message.ACTION_TYPE.AGENT_ASYNC_RESULT,
              MASSBase.getCurrentAgentsBase().getAsyncCompletedAgentList(), MASSBase
                  .getCurrentAgentsBase().getLocalPopulation());
          result.setSourcePid(MASSBase.getMyPid());
          oos.writeObject(result);
          oos.flush();
          oos.close();
          os.close();
          break;
        /*
         * case NODE_MASTER_ASYNC_COMPLETE_REQUEST: os =
         * socket.getOutputStream(); oos = new ObjectOutputStream(os);
         * synchronized (MASS_base.getCurrentAgents().getAsyncQueue()) { boolean
         * finish = MASS_base.getCurrentAgents().getIsAsyncLoopIdle() &&
         * MASS_base.getAsyncInputThread().isIdle(true) &&
         * MASS_base.getAsyncOutputThread().isIdle() &&
         * MASS_base.getCurrentAgents().getAsyncQueue().isEmpty() &&
         * MASS_base.getCurrentAgents().hasNoInprocessAgents(); if
         * (MASS.isConsoleLoggingEnabled()) { MASS_base.log("after finish"); }
         * oos.writeObject(finish); } oos.close(); os.close(); break; case
         * NODE_SLAVE_ASYNC_COMPLETE_NOTIFY:
         * MASS.incrementEstimateSlaveNodeComplete(); synchronized
         * (MASS_base.getCurrentAgents().getAsyncQueue()) { if
         * (MASS_base.getCurrentAgents().getAsyncQueue().size() == 0) { if
         * (MASS.isConsoleLoggingEnabled()) {
         * MASS.log("Master async queue is empty, wake him up"); }
         * MASS_base.getCurrentAgents().getAsyncQueue().notifyAll(); } } break;
         */
        case NODE_COMPLETE_NOTIFY_SOURCE:
          synchronized (MASSBase.getCurrentAgentsBase().getAsyncAgentIdList()) {
            if (MASSBase.getOutAsyncAgents()[m.getSourcePid()] == m.getAgentPopulation()) {
              MASSBase.getChildAgentPids().remove(m.getSourcePid());
            }
          }
          break;
        default:
          break;
        }
        socket.close();
        synchronized (MASSBase.getCurrentAgentsBase().getAsyncAgentIdList()) {
          if (runningChildThreadCount.decrementAndGet() == 0
              && m.getAction() != Message.ACTION_TYPE.AGENTS_ASYNC_MIGRATION_REMOTE_REQUEST) {
            // Order is important here, always have to decrement
            // Already notify for remote migrate message
            MASSBase.getCurrentAgentsBase().getAsyncAgentIdList().notifyAll();
          }
          // MASS.log("runningChildThreadCount = " +
          // runningChildThreadCount.get());
        }
      } catch (IOException | ClassNotFoundException e) {
    	  // TODO - needs to be better handling of the exception rather than just logging it
        logger.error("Unknown exception in AsyncInputThread", e);
      }

      logger.debug("AsyncInputChildThread ends");
      
    }
  }
}
