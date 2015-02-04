package edu.uw.bothell.css.dsl.MASS;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Handle async Migration req and other type of message
 * from other nodes
 * @author hohung
 *
 */
public class AsyncInputThread extends Thread {
  private int portNumber;
  private ServerSocket serverSocket;
  private boolean listening;

  public AsyncInputThread(int port) {
    portNumber = port;
  }

  public void run() {
    listening = true;
    try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
      while (listening) {
        new AsyncInputChildThread(serverSocket.accept()).start();
      }
    } catch (IOException e) {
      // Only Unexpected exception need to be logged 
      if (listening || !(e instanceof SocketException)) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        MASS_base.log(e + ". Stacktrace: " + sw.toString());
      }
    }
  }

  public void finish() {
    listening = false;
    if (serverSocket != null) {
      try {
        serverSocket.close();
      } catch (IOException e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        MASS_base.log(e + ". Stacktrace: " + sw.toString());
      }
    }
  }
  
  private class AsyncInputChildThread extends Thread {
    private Socket socket = null;
 
    public AsyncInputChildThread(Socket socket) {
        super("AsyncCommunicationServerThread");
        this.socket = socket;
    }
     
    public void run() { 
        try {
          InputStream is = socket.getInputStream();
          ObjectInputStream ois = new ObjectInputStream(is);
          Message m = (Message)ois.readObject();
          switch( m.getAction( ) ) {
            case AGENTS_ASYNC_MIGRATION_REMOTE_REQUEST:
              // process a message
              Vector<AgentMigrationRequest> receivedRequests 
              = m.getMigrationReqList();
              Places_base dstPlaces = MASS_base.getPlacesMap().
                  get( new Integer( m.getDestHandle()));

              // retrieve agents from receiveRequest
              for(AgentMigrationRequest request : receivedRequests) {
                int globalLinearIndex = request.destGlobalLinearIndex;
                Agent agent = request.agent;
                // local destination
                int destinationLocalLinearIndex 
                = globalLinearIndex - dstPlaces.getLowerBoundary();
                if ( MASS.isConsoleLoggingEnabled() == true ) {
                  MASS_base.log( " dstLocal = " + 
                      destinationLocalLinearIndex );
                }

                Place dstPlace = 
                    dstPlaces.getPlaces()[destinationLocalLinearIndex];

                // push this agent into the place and the entire agent bag.
                agent.setPlace(dstPlace);
                dstPlace.getAgents().add( agent ); // auto sync
                MASS_base.getCurrentAgents().getAgents().add(agent);
                MASS_base.getCurrentAgents().getAsyncQueue().add(agent);
              }             
              break;
            case AGENT_ASYNC_RESULT:
              MASS_base.getCurrentAgents().getCompleteQueue().addAll((ArrayList<Agent>)m.getArgument());
              MASS.incrementAsyncResultCount(m.getSourcePid(), m.getAgentPopulation());
              break;
            default:
              break;
          }
          socket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
}
