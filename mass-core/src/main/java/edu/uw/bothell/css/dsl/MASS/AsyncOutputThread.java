package edu.uw.bothell.css.dsl.MASS;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncOutputThread extends Thread {
  private static final int NAGLE_TIMEOUT = 50; // milisec
  private static final int MIN_ITEM_TO_SEND = 5; // Change to 1 or less to send
                                                 // immediately
  private volatile int[] timeouts; // 0 not timeout, 1 timer started, 2 timeout

  private Vector<AgentMigrationRequest>[] migrationRequestMap;
  private AtomicInteger lastRequestRank = new AtomicInteger(0);
  private AtomicInteger incompleteMigrationCount = new AtomicInteger(0);
  private boolean running = true;
  private int port;

  // need to be set at the beginning of each execution
  private int agentHandle;
  private int placeHandle;

  // use in requestSlaveNodeAsyncCompleteness() call
  private AsyncCommunicationLock asyncCommLock = new AsyncCommunicationLock();

  public AsyncOutputThread(int port) {
    this.port = port;
    migrationRequestMap = (Vector<AgentMigrationRequest>[]) new Vector[MASS_base
        .getSystemSize()];
    timeouts = new int[MASS_base.getSystemSize()];
    for (int i = 0; i < timeouts.length; i++) {
      migrationRequestMap[i] = new Vector<AgentMigrationRequest>();
      timeouts[i] = 0;
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

    MASS.log("AsyncOutputThread start at port " + port);
    while (running) {
      synchronized (lastRequestRank) {
        while (migrationRequestMap[lastRequestRank.get()].size() < MIN_ITEM_TO_SEND
            && timeouts[lastRequestRank.get()] != 2 && running) {
          try {
            lastRequestRank.wait();
            MASS.log("AOT waken up state[" + lastRequestRank.get() + "] = "
                + migrationRequestMap[lastRequestRank.get()].size() + " && "
                + timeouts[lastRequestRank.get()] + " && " + running);
          } catch (InterruptedException e) {
          }
        }

        if (running) {
          timeouts[lastRequestRank.get()] = 0;
          MASS.log("timeouts[" + lastRequestRank.get() + "] reset = "
              + timeouts[lastRequestRank.get()]);
          if (migrationRequestMap[lastRequestRank.get()].size() > 0) {
            MASS.log("AOT async migrate to " + lastRequestRank
                + ", req size = "
                + migrationRequestMap[lastRequestRank.get()].size());
            synchronized (migrationRequestMap[lastRequestRank.get()]) {
              Vector<AgentMigrationRequest> reqlist = new Vector<AgentMigrationRequest>(
                  migrationRequestMap[lastRequestRank.get()]);
              Message messageToDest = new Message(
                  Message.ACTION_TYPE.AGENTS_ASYNC_MIGRATION_REMOTE_REQUEST,
                  agentHandle, placeHandle, reqlist);
              SendMessageByChild thread_ref = new SendMessageByChild(
                  lastRequestRank.get(), messageToDest);
              migrationRequestMap[lastRequestRank.get()].clear();
              thread_ref.start();
            }
          }
        }
      }
    }
    MASS.log("AsyncOutputThread ends");
  }

  public void finish() {
    running = false;
    synchronized (lastRequestRank) {
      lastRequestRank.notifyAll();
    }
    MASS.log("AsyncOutputThread finishes");
  }

  public void requestMigration(int destRank, AgentMigrationRequest request) {
    synchronized (migrationRequestMap[destRank]) {
      migrationRequestMap[destRank].add(request);
    }
    synchronized (lastRequestRank) {
      if (MIN_ITEM_TO_SEND > 1 && timeouts[lastRequestRank.get()] == 0) {
        // NAGLE algorithm in effect
        TimeoutHandler timeoutHandler = new TimeoutHandler(destRank);
        timeouts[lastRequestRank.get()] = 1;
        timeoutHandler.start();
      }
      lastRequestRank.getAndSet(destRank);
      lastRequestRank.notifyAll();
    }
    incompleteMigrationCount.incrementAndGet();
    if (MASS.isConsoleLoggingEnabled()) {
      MASS.log("requestMigration to [" + destRank
          + "] incompleteMigrationCount = " + incompleteMigrationCount.get());
    }
  }

  public void requestAsyncResults() {
    asyncCommLock.reset();
    asyncCommLock.setResult(Collections
        .synchronizedList(new LinkedList<Agent>()));
    asyncCommLock.setSecondResult((Object) new int[MASS_base.getRemoteNodes()
        .size()]);
    Iterator<MNode> remoteNodeIter = MASS_base.getRemoteNodes().iterator();
    while (remoteNodeIter.hasNext()) {
      new AsyncResultRequest(remoteNodeIter.next().getHostName()).start();
    }
    synchronized (asyncCommLock) {
      while (!asyncCommLock.isReady()) {
        try {
          asyncCommLock.wait();
        } catch (InterruptedException e) {
        }
      }
    }

    MASS_base.getCurrentAgents().getCompleteQueue()
        .addAll((List<Agent>) asyncCommLock.getResult());
    MASS.setLocalAgents((int[]) asyncCommLock.getSecondResult());
  }

  private class TimeoutHandler extends Thread {
    public int destRank;

    public TimeoutHandler(int rank) {
      destRank = rank;
    }

    public void run() {
      MASS.log("TimoutHandler start");
      try {
        Thread.sleep(NAGLE_TIMEOUT);
      } catch (InterruptedException e) {
      }
      synchronized (lastRequestRank) {
        lastRequestRank.set(destRank);
        timeouts[destRank] = 2;
        lastRequestRank.notifyAll();
        MASS.log("TimoutHandler notified " + lastRequestRank.get());
      }
    }
  }

  private class SendMessageByChild extends Thread {
    int rank;
    Message message;

    public SendMessageByChild(int rank, Message message) {
      this.rank = rank;
      this.message = message;
    }

    public void run() {
      String hostName = MASS_base.getHosts().get(rank);
      if (MASS.isConsoleLoggingEnabled()) {
        MASS_base.log("SendMessageByChild to rank " + rank + "= " + hostName
            + " starts for message type " + message.getActionString());
      }

      try {
        Socket sendSocket = new Socket(hostName, port);
        OutputStream os = sendSocket.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(message);
        oos.flush();
        if (message.getAction() == Message.ACTION_TYPE.AGENTS_ASYNC_MIGRATION_REMOTE_REQUEST) {
          MASS.log("Wait from migration complete ack");
          ObjectInputStream ois = new ObjectInputStream(
              sendSocket.getInputStream());
          int decrease = (int) ois.readObject();
          int incompleteCount = incompleteMigrationCount.addAndGet(decrease
              * -1);
          if (incompleteCount == 0) {
            synchronized (MASS_base.getCurrentAgents().getAsyncQueue()) {
              MASS_base.getCurrentAgents().getAsyncQueue().notifyAll();
            }
          }
          MASS.log("Migration complete ACK received " + incompleteCount);
          ois.close();
        }
        oos.close();
        os.close();
        sendSocket.close();
      } catch (IOException | ClassNotFoundException e) {
        MASS.logException(null, e);
      }

      if (MASS.isConsoleLoggingEnabled())
        MASS_base.log("Req to " + rank + " finished");
    }
  }

  private class AsyncResultRequest extends Thread {
    private String hostname;

    public AsyncResultRequest(String hname) {
      hostname = hname;
    }

    public void run() {
      try {
        Socket sendSocket = new Socket(hostname, port);
        Message message = new Message(Message.ACTION_TYPE.AGENT_ASYNC_RESULT);
        OutputStream os = sendSocket.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(message);
        ObjectInputStream ois = new ObjectInputStream(
            sendSocket.getInputStream());
        Message result = (Message) ois.readObject();
        synchronized (asyncCommLock) {
          ((List<Agent>) asyncCommLock.getResult()).addAll((List<Agent>) result
              .getArgument());
          ((int[]) asyncCommLock.getSecondResult())[result.getSourcePid() - 1] = result
              .getAgentPopulation();
          asyncCommLock.incrementCounter();
          if (asyncCommLock.getCounter() == MASS_base.getRemoteNodes().size()) {
            asyncCommLock.set();
            asyncCommLock.notifyAll();
          }
        }
        oos.close();
        ois.close();
        os.close();
        sendSocket.close();
      } catch (IOException | ClassNotFoundException e) {
        MASS.logException(null, e);
      }
    }
  }

  private class SlaveNodeCompletenessRequest extends Thread {
    private String hostname;

    public SlaveNodeCompletenessRequest(String hname) {
      hostname = hname;
    }

    public void run() {
      try {
        Socket sendSocket = new Socket(hostname, port);
        Message message = new Message(
            Message.ACTION_TYPE.NODE_MASTER_ASYNC_COMPLETE_REQUEST);
        OutputStream os = sendSocket.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(message);
        ObjectInputStream ois = new ObjectInputStream(
            sendSocket.getInputStream());
        boolean result = (boolean) ois.readObject();
        synchronized (asyncCommLock) {
          asyncCommLock.setResult(result);
          asyncCommLock.incrementCounter();
          if (!result
              || asyncCommLock.getCounter() == MASS_base.getRemoteNodes()
                  .size()) {
            asyncCommLock.set();
            asyncCommLock.notifyAll();
          }
        }
        oos.close();
        ois.close();
        os.close();
        sendSocket.close();
      } catch (IOException | ClassNotFoundException e) {
        MASS.logException(null, e);
      }
    }
  }

  /**
   * ONLY to be call by Master node
   * 
   * @return
   */
  public boolean requestSlaveNodeAsyncCompleteness() {
    asyncCommLock.reset();
    Iterator<MNode> remoteNodeIter = MASS_base.getRemoteNodes().iterator();
    while (remoteNodeIter.hasNext()) {
      new SlaveNodeCompletenessRequest(remoteNodeIter.next().getHostName())
          .start();
    }
    synchronized (asyncCommLock) {
      while (!asyncCommLock.isReady()) {
        try {
          asyncCommLock.wait();
        } catch (InterruptedException e) {
        }
      }
    }
    return (boolean) asyncCommLock.getResult();
  }

  public AsyncCommunicationLock getSlaveNodeCompleteLock() {
    return asyncCommLock;
  }

  public void notifyMasterOfCompleteness() {
    MASS.log("notifyMasterOfCompleteness");
    Message messageToDest = new Message(
        Message.ACTION_TYPE.NODE_SLAVE_ASYNC_COMPLETE_NOTIFY);
    SendMessageByChild thread_ref = new SendMessageByChild(0, messageToDest);
    thread_ref.start();
  }

  public boolean isMigrationRequestComplete() {
    if (MASS.isConsoleLoggingEnabled()) {
      MASS.log("isMigrationRequestComplete incompleteMigrationCount.get() = "
          + incompleteMigrationCount.get());
    }
    return incompleteMigrationCount.get() == 0;
  }
}
