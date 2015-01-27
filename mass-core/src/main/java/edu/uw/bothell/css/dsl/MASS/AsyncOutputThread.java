package edu.uw.bothell.css.dsl.MASS;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;

import edu.uw.bothell.css.dsl.MASS.Agents_base.SendMessageByChild;

public class AsyncOutputThread extends Thread {
  private static final int NAGLE_TIMEOUT = 50; // milisec
  private static final int MIN_ITEM_TO_SEND = 5; // Change to 1 or less to send
                                                 // immediately
  private boolean[] timeouts;

  private LinkedList<AgentMigrationRequest>[] migrationRequestMap;
  private Integer lastRequestRank = 0;
  private boolean running = true;
  private int port;

  // need to be set at the beginning of each execution
  private int agentHandle;
  private int placeHandle;

  public AsyncOutputThread(int port) {
    this.port = port;
    migrationRequestMap = (LinkedList<AgentMigrationRequest>[]) new LinkedList[MASS_base
        .getSystemSize()];
    timeouts = new boolean[MASS_base.getSystemSize()];
    for (int i = 0; i < timeouts.length; i++) {
      migrationRequestMap[i] = new LinkedList<AgentMigrationRequest>();
      timeouts[i] = false;
    }
  }
  
  public AsyncOutputThread(int port, int agentHandle, int placeHandle) {
    this(port);
    setAgentHandle(agentHandle);
    setPlaceHandle(placeHandle);
  }
  
  public void setAgentHandle(int newHandle) {
    agentHandle = newHandle;
  }
  
  public void setPlaceHandle(int newHandle) {
    placeHandle = newHandle;
  }

  public void run() {
    while (running) {
      synchronized (lastRequestRank) {
        while (migrationRequestMap[lastRequestRank].size() < MIN_ITEM_TO_SEND
            && !timeouts[lastRequestRank] && running) {
          try {
            lastRequestRank.wait();
          } catch (InterruptedException e) {
          }
        }

        if (running) {
          synchronized (lastRequestRank) {
            Message messageToDest = 
              new Message( Message.ACTION_TYPE.
                  AGENTS_ASYNC_MIGRATION_REMOTE_REQUEST,
                  agentHandle, placeHandle, migrationRequestMap[lastRequestRank] );
            SendMessageByChild thread_ref =
                new SendMessageByChild( lastRequestRank, messageToDest );
            thread_ref.start( );
            timeouts[lastRequestRank] = false;
          }
        }
      }
    }
  }

  public void finish() {
    running = false;
    synchronized (lastRequestRank) {
      lastRequestRank.notifyAll();
    }
  }

  public void requestMigration(int destRank,
      AgentMigrationRequest request) {
    migrationRequestMap[destRank].add(request);
    synchronized (lastRequestRank) {
      if (MIN_ITEM_TO_SEND > 1) // NAGLE algorithm in effect
      {
        TimeoutHandler timeoutHandler = new TimeoutHandler(destRank);
        timeoutHandler.start();
      }
      lastRequestRank = destRank;
      lastRequestRank.notifyAll();
    }
  }

  private class TimeoutHandler extends Thread {
    public int destRank;

    public TimeoutHandler(int rank) {
      destRank = rank;
    }

    public void run() {
      try {
        Thread.sleep(NAGLE_TIMEOUT);
      } catch (InterruptedException e) {
      }
      synchronized (lastRequestRank) {
        lastRequestRank = destRank;
        timeouts[destRank] = true;
        lastRequestRank.notifyAll();
      }
    }
  }
  
  private class SendMessageByChild extends Thread {
    int rank;
    Message message;
    
    public SendMessageByChild( int rank, Message message ) {
      this.rank = rank;
      this.message = message;
    }

    public void run( ) {      
      if ( MASS.isConsoleLoggingEnabled() == true )
        MASS_base.log( "pthread_self[" + Thread.currentThread( ) +
            "] sendMessageByChild to " + rank + " starts" );
      String hostName = MASS_base.getMasterNode().getHostName();
      if(rank > 0) {
        hostName = MASS_base.getRemoteNodes().get(rank - 1).getHostName();
      }
        try {
          Socket sendSocket = new Socket(hostName, port);
          OutputStream os = sendSocket.getOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(os);
          oos.writeObject(message);
          oos.close();
          os.close();
          sendSocket.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
        }
      
      if ( MASS.isConsoleLoggingEnabled() == true )
        MASS_base.log( "pthread_self[" + Thread.currentThread( ) +
            "] sendMessageByChild to " + rank + 
            " finished" );
    
    }
  
  }
}
