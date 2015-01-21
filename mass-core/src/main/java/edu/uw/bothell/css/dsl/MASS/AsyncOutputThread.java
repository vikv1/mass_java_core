package edu.uw.bothell.css.dsl.MASS;

import java.util.LinkedList;

public class AsyncOutputThread extends Thread {
  private static final int NAGLE_TIMEOUT = 50; // milisec
  private static final int MIN_ITEM_TO_SEND = 5; // Change to 1 or less to send
                                                 // immediately
  private boolean[] timeouts;

  private LinkedList<AgentMigrationRequest>[] migrationRequestMap;
  private Integer lastRequestRank = 0;
  private boolean running = true;

  // need to be set at the beginning of each execution
  private int agentHandle;
  private int placeHandle;

  public AsyncOutputThread() {
    migrationRequestMap = (LinkedList<AgentMigrationRequest>[]) new LinkedList[MASS_base
        .getSystemSize()];
    timeouts = new boolean[MASS_base.getSystemSize()];
    for (int i = 0; i < timeouts.length; i++) {
      migrationRequestMap[i] = new LinkedList<AgentMigrationRequest>();
      timeouts[i] = false;
    }
  }
  
  public AsyncOutputThread(int agentHandle, int placeHandle) {
    this();
    setAgentHandle(agentHandle);
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
          timeouts[lastRequestRank] = false;
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
}
