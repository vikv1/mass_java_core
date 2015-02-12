package edu.uw.bothell.css.dsl.MASS;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Vector;

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

  public AsyncInputThread(int port) {
    portNumber = port;
  }

  public void run() {
    MASS.log("AsyncInputThread start at port " + portNumber);
    listening = true;
    try {
      serverSocket = new ServerSocket(portNumber);
      while (listening) {
        MASS.log("Waiting for request...");
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

  private class AsyncInputChildThread extends Thread {
    private Socket socket = null;

    public AsyncInputChildThread(Socket socket) {
      MASS.log("construct AsyncInputChildThread");
      this.socket = socket;
    }

    public void run() {
      try {
        MASS.log("AsyncInputChildThread processing");
        InputStream is = socket.getInputStream();
        ObjectInputStream ois = new ObjectInputStream(is);
        Message m = (Message) ois.readObject();
        switch (m.getAction()) {
        case AGENTS_ASYNC_MIGRATION_REMOTE_REQUEST:
          MASS.log("Receive Agent migration remote request");
          // process a message
          Vector<AgentMigrationRequest> receivedRequests = m
              .getMigrationReqList();
          Places_base dstPlaces = MASS_base.getPlacesMap().get(
              new Integer(m.getDestHandle()));

          OutputStream os = socket.getOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(os);
          oos.writeObject(new Integer(receivedRequests.size()));
          oos.flush();
          // retrieve agents from receiveRequest
          for (AgentMigrationRequest request : receivedRequests) {
            int globalLinearIndex = request.destGlobalLinearIndex;
            Agent agent = request.agent;
            // local destination
            int destinationLocalLinearIndex = globalLinearIndex
                - dstPlaces.getLowerBoundary();
            if (MASS.isConsoleLoggingEnabled() == true) {
              MASS_base.log(" dstLocal = " + destinationLocalLinearIndex);
            }

            Place dstPlace = dstPlaces.getPlaces()[destinationLocalLinearIndex];

            // push this agent into the place and the entire agent bag.
            agent.setPlace(dstPlace);
            dstPlace.getAgents().add(agent); // auto sync
            synchronized (MASS_base.getCurrentAgents().getAsyncQueue()) {
              agent.setCurrentIndex(MASS_base.getCurrentAgents().getAgents()
                  .size_unreduced());
              agent.setParentAgents(MASS_base.getCurrentAgents());
              MASS_base.getCurrentAgents().getAgents().add(agent);
              MASS_base.getCurrentAgents().getAsyncQueue().add(agent);
              MASS_base.getCurrentAgents().getAsyncQueue().notifyAll();
            }
          }
          oos.close();
          os.close();
          break;
        case AGENT_ASYNC_RESULT:
          MASS.log("Receive AGENT_ASYNC_RESULT");
          MASS_base.getCurrentAgents().setResultRequestFromMaster(true);
          synchronized (MASS_base.getCurrentAgents().getAsyncQueue()) {
            MASS_base.getCurrentAgents().setLocalPopulation(
                MASS_base.getCurrentAgents().getAgents().size());
            MASS_base.getCurrentAgents().getAsyncQueue().notifyAll();
          }
          os = socket.getOutputStream();
          oos = new ObjectOutputStream(os);
          Message result = new Message(Message.ACTION_TYPE.AGENT_ASYNC_RESULT,
              MASS_base.getCurrentAgents().getCompleteQueue(),
              MASS_base.getCurrentAgents().getLocalPopulation());
          result.setSourcePid(MASS_base.getMyPid());
          oos.writeObject(result);
          oos.flush();
          oos.close();
          os.close();
          break;
        case NODE_MASTER_ASYNC_COMPLETE_REQUEST:
          MASS.log("Receive MASTER_NODE_ASYNC_COMPLETE_REQUEST");
          os = socket.getOutputStream();
          oos = new ObjectOutputStream(os);
          synchronized (MASS_base.getCurrentAgents().getAsyncQueue()) {
            oos.writeObject(MASS_base.getCurrentAgents().getAsyncQueue().size() == 0);
          }
          oos.close();
          os.close();
          break;
        case NODE_SLAVE_ASYNC_COMPLETE_NOTIFY:
          MASS.log("Receive SLAVE_NODE_ASYNC_COMPLETE_NOTIFY");
          synchronized (MASS_base.getCurrentAgents().getAsyncQueue()) {
            if (MASS_base.getCurrentAgents().getAsyncQueue().size() == 0) {
              MASS.log("Master async queue is empty, wake him up");
              MASS_base.getCurrentAgents().getAsyncQueue().notifyAll();
            }
          }
          break;
        default:
          break;
        }
        socket.close();
      } catch (IOException | ClassNotFoundException e) {
        e.printStackTrace();
      }
      MASS.log("AsyncInputChildThread ends");
    }
  }
}
