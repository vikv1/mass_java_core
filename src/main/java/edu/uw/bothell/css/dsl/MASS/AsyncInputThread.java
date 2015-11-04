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

  public AsyncInputThread(int port) {
    portNumber = port;
    runningChildThreadCount = new AtomicInteger(0);
  }

  public void run() {
    MASS.log("AsyncInputThread start at port " + portNumber);
    listening = true;
    try {
      serverSocket = new ServerSocket(portNumber);
      while (listening) {
        new AsyncInputChildThread(serverSocket.accept()).start();
      }
    } catch (IOException e) {
      // Only Unexpected exception need to be logged
      if (listening || !(e instanceof SocketException)) {
        MASSBase.logException(
            "Unexpected exception in AsyncInputThread.run()", e);
      }
    }
    MASS.log("AsyncInputThread.end");
  }

  public void finish() {
    listening = false;
    if (serverSocket != null) {
      MASS.log("AsyncInputThread tries to close server socket");
      try {
        serverSocket.close();
      } catch (IOException e) {
        MASS.logException(null, e);
      }
    }
    MASS.log("AsyncInputThread finishes");
  }

  /**
   * 
   * @param completeReqFromMaster
   *          : if true, including 1 req as complete
   * @return
   */
  public boolean isIdle(boolean completeReqFromMaster) {
    synchronized (MASSBase.getCurrentAgents().getAsyncQueue()) {
      if (MASS.isConsoleLoggingEnabled()) {
        MASS.log(completeReqFromMaster
            + " Asyncinput running child thread count = "
            + runningChildThreadCount.get());
      }
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
      if (MASS.isConsoleLoggingEnabled()) {
        MASS.log("construct AsyncInputChildThread");
      }
      runningChildThreadCount.incrementAndGet();
      this.socket = socket;
    }

    public void run() {
      if (MASSBase.getCurrentAgents() == null) {
        MASS.log("MASS is not ready " + socket.getRemoteSocketAddress());
        return;
      }
      try {
        if (MASS.isConsoleLoggingEnabled()) {
          MASS.log("AsyncInputChildThread processing "
              + socket.getRemoteSocketAddress());
        }
        InputStream is = socket.getInputStream();
        ObjectInputStream ois = new ObjectInputStream(is);
        Message m = (Message) ois.readObject();
        if (MASS.isConsoleLoggingEnabled()) {
          MASS.log("Receive m " + m.getActionString());
        }
        switch (m.getAction()) {
        case AGENTS_ASYNC_MIGRATION_REMOTE_REQUEST:
          // process a message
          Vector<AgentMigrationRequest> receivedRequests = m
              .getMigrationReqList();
          Places_base dstPlaces = MASSBase.getPlacesMap().get(
              new Integer(m.getDestHandle()));
          boolean chosen = false;
          synchronized (MASSBase.getCurrentAgents().getAsyncQueue()) {
            if (MASSBase.getMyPid() != 0 // Master doesn't need source ever
               // && MASS_base.getCurrentAgents().getAgents().estimateSize() == 0
                && MASSBase.getSourceAgentPid() == -1) {
              if(MASS.isConsoleLoggingEnabled()) {
                MASS.log("Choose " + m.getSourcePid() + " as source");
              }
              chosen = true;
              MASSBase.setSourceAgentPid(m.getSourcePid());
            }
          }
          OutputStream os = socket.getOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(os);
          oos.writeObject(new AgentMigrationResponse(receivedRequests.size(),
              chosen));
          oos.flush();
          // retrieve agents from receiveRequest
          synchronized (MASSBase.getCurrentAgents().getAsyncQueue()) {
            for (AgentMigrationRequest request : receivedRequests) {
              int globalLinearIndex = request.destGlobalLinearIndex;
              Agent agent = request.agent;
              agent.setHasAlreadyRemoteMigrated(false);
              agent.setPutBackToAsyncQueue(false);
              // local destination
              int destinationLocalLinearIndex = globalLinearIndex
                  - dstPlaces.getLowerBoundary();
              if (MASS.isConsoleLoggingEnabled())
                MASSBase.log(" dstLocalIndex = " + destinationLocalLinearIndex
                    + ", async func index = " + agent.getAsyncFuncListIndex());

              Place dstPlace = dstPlaces.getPlaces()[destinationLocalLinearIndex];

              // push this agent into the place and the entire agent bag.
              agent.setPlace(dstPlace);
              dstPlace.getAgents().add(agent); // auto sync
              agent.setCurrentIndex(MASSBase.getCurrentAgents().getAgents()
                  .size_unreduced());
              agent.setParentAgents(MASSBase.getCurrentAgents());
              MASSBase.getCurrentAgents().getAgents().add(agent);
              MASSBase.getCurrentAgents().asyncQueueAdd(agent.getCurrentIndex());
              if (MASS.isConsoleLoggingEnabled())
                MASSBase.log("migrate agent added to async queue, new size = "
                    + MASSBase.getCurrentAgents().asyncQueueSize());
            }
            MASSBase.getInAsyncAgents()[m.getSourcePid()] += receivedRequests
                .size();
            if(MASS.isConsoleLoggingEnabled()) {
              MASSBase.log("InAsync[" + m.getSourcePid() + "] is now " + MASSBase.getInAsyncAgents()[m.getSourcePid()]);
            }
            MASSBase.getCurrentAgents().getAsyncQueue().notifyAll();
          }

          oos.close();
          os.close();
          break;
        case AGENT_ASYNC_RESULT:
          MASSBase.getCurrentAgents().setResultRequestFromMaster(true);
          synchronized (MASSBase.getCurrentAgents().getAsyncQueue()) {
            MASSBase.getCurrentAgents().setLocalPopulation(
                MASSBase.getCurrentAgents().getAgents().size());
            MASSBase.getCurrentAgents().getAsyncQueue().notifyAll();
          }
          os = socket.getOutputStream();
          oos = new ObjectOutputStream(os);
          if (MASS.isConsoleLoggingEnabled()) {
            MASS.log("Return to master completeQueue of size "
                + MASSBase.getCurrentAgents().getCompleteQueue().size());
            for (Agent a : MASSBase.getCurrentAgents().getCompleteQueue()) {
              MASS.log("agent result size = " + a.asyncResultsSize());
            }
          }
          Message result = new Message(Message.ACTION_TYPE.AGENT_ASYNC_RESULT,
              MASSBase.getCurrentAgents().getCompleteQueue(), MASSBase
                  .getCurrentAgents().getLocalPopulation());
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
          synchronized (MASSBase.getCurrentAgents().getAsyncQueue()) {
            if (MASSBase.getOutAsyncAgents()[m.getSourcePid()] == m.getAgentPopulation()) {
              MASSBase.getChildAgentPids().remove(m.getSourcePid());
            }
          }
          break;
        default:
          break;
        }
        socket.close();
        synchronized (MASSBase.getCurrentAgents().getAsyncQueue()) {
          if (runningChildThreadCount.decrementAndGet() == 0
              && m.getAction() != Message.ACTION_TYPE.AGENTS_ASYNC_MIGRATION_REMOTE_REQUEST) {
            // Order is important here, always have to decrement
            // Already notify for remote migrate message
            MASSBase.getCurrentAgents().getAsyncQueue().notifyAll();
          }
          // MASS.log("runningChildThreadCount = " +
          // runningChildThreadCount.get());
        }
      } catch (IOException | ClassNotFoundException e) {
        MASS.logException(null, e);
      }
      if (MASS.isConsoleLoggingEnabled()) {
        MASS.log("AsyncInputChildThread ends");
      }
    }
  }
}
