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
        MASS_base.logException(
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
    synchronized (MASS_base.getCurrentAgents().getAsyncQueue()) {
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
      if (MASS_base.getCurrentAgents() == null) {
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
          Places_base dstPlaces = MASS_base.getPlacesMap().get(
              new Integer(m.getDestHandle()));
          boolean chosen = false;
          synchronized (MASS_base.getCurrentAgents().getAsyncQueue()) {
            if (MASS_base.getMyPid() != 0 // Master doesn't need source ever
               // && MASS_base.getCurrentAgents().getAgents().estimateSize() == 0
                && MASS_base.getSourceAgentPid() == -1) {
              if(MASS.isConsoleLoggingEnabled()) {
                MASS.log("Choose " + m.getSourcePid() + " as source");
              }
              chosen = true;
              MASS_base.setSourceAgentPid(m.getSourcePid());
            }
          }
          OutputStream os = socket.getOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(os);
          oos.writeObject(new AgentMigrationResponse(receivedRequests.size(),
              chosen));
          oos.flush();
          // retrieve agents from receiveRequest
          synchronized (MASS_base.getCurrentAgents().getAsyncQueue()) {
            for (AgentMigrationRequest request : receivedRequests) {
              int globalLinearIndex = request.destGlobalLinearIndex;
              Agent agent = request.agent;
              agent.setStopProcessAsyncFuncList(false);
              agent.setPutBackToAsyncQueue(false);
              // local destination
              int destinationLocalLinearIndex = globalLinearIndex
                  - dstPlaces.getLowerBoundary();
              if (MASS.isConsoleLoggingEnabled())
                MASS_base.log(" dstLocalIndex = " + destinationLocalLinearIndex
                    + ", func size = " + agent.getAsyncFuncList().size());

              Place dstPlace = dstPlaces.getPlaces()[destinationLocalLinearIndex];

              // push this agent into the place and the entire agent bag.
              agent.setPlace(dstPlace);
              dstPlace.getAgents().add(agent); // auto sync
              agent.setCurrentIndex(MASS_base.getCurrentAgents().getAgents()
                  .size_unreduced());
              agent.setParentAgents(MASS_base.getCurrentAgents());
              MASS_base.getCurrentAgents().getAgents().add(agent);
              MASS_base.getCurrentAgents().getAsyncQueue().add(agent);
              if (MASS.isConsoleLoggingEnabled())
                MASS_base.log("migrate agent added to async queue, new size = "
                    + MASS_base.getCurrentAgents().getAsyncQueue().size());
            }
            MASS_base.getInAsyncAgents()[m.getSourcePid()] += receivedRequests
                .size();
            if(MASS.isConsoleLoggingEnabled()) {
              MASS_base.log("InAsync[" + m.getSourcePid() + "] is now " + MASS_base.getInAsyncAgents()[m.getSourcePid()]);
            }
            MASS_base.getCurrentAgents().getAsyncQueue().notifyAll();
          }

          oos.close();
          os.close();
          break;
        case AGENT_ASYNC_RESULT:
          MASS_base.getCurrentAgents().setResultRequestFromMaster(true);
          synchronized (MASS_base.getCurrentAgents().getAsyncQueue()) {
            MASS_base.getCurrentAgents().setLocalPopulation(
                MASS_base.getCurrentAgents().getAgents().size());
            MASS_base.getCurrentAgents().getAsyncQueue().notifyAll();
          }
          os = socket.getOutputStream();
          oos = new ObjectOutputStream(os);
          if (MASS.isConsoleLoggingEnabled()) {
            MASS.log("Return to master completeQueue of size "
                + MASS_base.getCurrentAgents().getCompleteQueue().size());
            for (Agent a : MASS_base.getCurrentAgents().getCompleteQueue()) {
              MASS.log("agent result size = " + a.getAsyncResults().size());
            }
          }
          Message result = new Message(Message.ACTION_TYPE.AGENT_ASYNC_RESULT,
              MASS_base.getCurrentAgents().getCompleteQueue(), MASS_base
                  .getCurrentAgents().getLocalPopulation());
          result.setSourcePid(MASS_base.getMyPid());
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
          synchronized (MASS_base.getCurrentAgents().getAsyncQueue()) {
            if (MASS_base.getOutAsyncAgents()[m.getSourcePid()] == m.getAgentPopulation()) {
              MASS_base.getChildAgentPids().remove(m.getSourcePid());
            }
          }
          break;
        default:
          break;
        }
        socket.close();
        synchronized (MASS_base.getCurrentAgents().getAsyncQueue()) {
          if (runningChildThreadCount.decrementAndGet() == 0
              && m.getAction() != Message.ACTION_TYPE.AGENTS_ASYNC_MIGRATION_REMOTE_REQUEST) {
            // Order is important here, always have to decrement
            // Already notify for remote migrate message
            MASS_base.getCurrentAgents().getAsyncQueue().notifyAll();
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
