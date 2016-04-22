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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;

public class AsyncOutputThread extends Thread {
	
	private static final int NAGLE_TIMEOUT = 30; // milisec
	
	// TODO a logic to set this wisely
	private static final int MIN_ITEM_TO_SEND = 1000; // Change to 1 or less to
														// send
	// immediately
	private volatile int[] timeouts; // 0 not timeout, 1 timer started, 2
										// timeout

	private volatile Vector<AgentMigrationRequest>[] migrationRequestMap;
	private volatile LinkedList<Integer> lastRequestRank = new LinkedList<Integer>();
	private volatile AtomicInteger runningChildRequestCount;
	// private volatile boolean sendCompleteNotifyToMaster = true;
	private boolean running = true;
	private int port;

	// need to be set at the beginning of each call all execution
	private int agentHandle;
	private int placeHandle;

	// use in requestSlaveNodeAsyncCompleteness() call
	private volatile AsyncCommunicationLock /*
											 * slaveCompleteLock = new
											 * AsyncCommunicationLock(),
											 */
	slaveResultLock = new AsyncCommunicationLock();

	// logging
	private Log4J2Logger logger = Log4J2Logger.getInstance();

	@SuppressWarnings("unchecked")
	public AsyncOutputThread(int port) {

		this.port = port;
		migrationRequestMap = (Vector<AgentMigrationRequest>[]) new Vector[MASSBase.getSystemSize()];
		timeouts = new int[MASSBase.getSystemSize()];

		for (int i = 0; i < timeouts.length; i++) {
			migrationRequestMap[i] = new Vector<AgentMigrationRequest>();
			timeouts[i] = 0;
		}

		runningChildRequestCount = new AtomicInteger(0);

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

		logger.debug("AsyncOutputThread start at port {}", port);
		while (running) {
			synchronized (lastRequestRank) {
				while (lastRequestRank.isEmpty() && running) {
					try {
						lastRequestRank.wait();
					} catch (InterruptedException e) {
						// TODO - really, really bad to swallow thread interruptions
						logger.error("Async thread interrupted", e);
					}
				}

				if (running) {
					while (!lastRequestRank.isEmpty()) {
						int dequeueRank = lastRequestRank.poll();
						if (migrationRequestMap[dequeueRank].size() >= MIN_ITEM_TO_SEND || timeouts[dequeueRank] == 2) {
							timeouts[dequeueRank] = 0;
							Vector<AgentMigrationRequest> reqlist = null;

							synchronized (migrationRequestMap[dequeueRank]) {
								if (migrationRequestMap[dequeueRank].size() > 0) {
										logger.debug("AOT async migrate to " + dequeueRank + ", req size = "
												+ migrationRequestMap[dequeueRank].size());
									reqlist = new Vector<AgentMigrationRequest>(migrationRequestMap[dequeueRank]);
									migrationRequestMap[dequeueRank].clear();
								}
							}

							if (reqlist != null) {
								Message messageToDest = new Message(
										Message.ACTION_TYPE.AGENTS_ASYNC_MIGRATION_REMOTE_REQUEST, agentHandle,
										placeHandle, reqlist);
								messageToDest.setSourcePid(MASSBase.getMyPid());
								SendMessageByChild thread_ref = new SendMessageByChild(dequeueRank, messageToDest);
								thread_ref.start();
							}

						}
					}
				}
			}
		}
		logger.debug("AsyncOutputThread ends");
	}

	public void finish() {
		running = false;
		synchronized (lastRequestRank) {
			lastRequestRank.notifyAll();
		}
		logger.debug("AsyncOutputThread finishes");
	}

	public void requestMigration(int destRank, AgentMigrationRequest request) {
		synchronized (lastRequestRank) {
			synchronized (migrationRequestMap[destRank]) {
				migrationRequestMap[destRank].add(request);
			}
			int lastCount = runningChildRequestCount.incrementAndGet();
				logger.debug("requestMigration to [" + destRank + "]. asyncfunc index = "
						+ request.agent.getAsyncFuncListIndex() + " runningChildRequestCount = " + lastCount);

			if (MIN_ITEM_TO_SEND > 1 && timeouts[destRank] == 0) {
				// NAGLE algorithm in effect
				TimeoutHandler timeoutHandler = new TimeoutHandler(destRank);
				timeoutHandler.start();
			}
			lastRequestRank.add(destRank);
			lastRequestRank.notifyAll();
		}
	}

	@SuppressWarnings("unchecked")
	public void requestAsyncResults() {
		slaveResultLock.reset();
		slaveResultLock.setResult(Collections.synchronizedList(new LinkedList<Agent>()));
		slaveResultLock.setSecondResult((Object) new int[MASSBase.getRemoteNodes().size()]);
		Iterator<MNode> remoteNodeIter = MASSBase.getRemoteNodes().iterator();
		while (remoteNodeIter.hasNext()) {
			new AsyncResultRequest(remoteNodeIter.next().getHostName()).start();
		}

		List<Agent> finalAgents = null;
		int[] finalLocalAgents = null;
		synchronized (slaveResultLock) {
			while (!slaveResultLock.isReady()) {
				try {
					slaveResultLock.wait();
				} catch (InterruptedException e) {
					// TODO - really, really bad idea to swallow thread interruption exceptions
					logger.error("Interrupted thread exception in requestAsyncResults", e);
				}
			}
			finalAgents = (List<Agent>) slaveResultLock.getResult();
			finalLocalAgents = (int[]) slaveResultLock.getSecondResult();
		}

		if (MASS.isConsoleLoggingEnabled())
			logger.debug("All slaves return " + (finalAgents != null ? finalAgents.size() : 0));

		synchronized (MASSBase.getCurrentAgentsBase().getAsyncCompletedAgentList()) {
			MASSBase.getCurrentAgentsBase().getAsyncCompletedAgentList().addAll(finalAgents);
			MASS.setLocalAgents(finalLocalAgents);
		}
	}

	private class TimeoutHandler extends Thread {
		public int destRank;

		public TimeoutHandler(int rank) {
			destRank = rank;
			timeouts[destRank] = 1;
		}

		public void run() {
			try {
				Thread.sleep(NAGLE_TIMEOUT);
			} catch (InterruptedException e) {
				// TODO - really bad idea to swallow thread interruption exceptions
				logger.error("Thread interruption exception in timout handler", e);
			}
			synchronized (lastRequestRank) {
				lastRequestRank.add(destRank);
				timeouts[destRank] = 2;
				lastRequestRank.notifyAll();
				logger.debug("TimoutHandler notified {}", destRank);
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
			
			String hostName = MASSBase.getHosts().get(rank);
			logger.debug("SendMessageByChild to rank " + rank + "= " + hostName + " starts for message type "
						+ message.getActionString());

			try {
				if (message.getAction() == Message.ACTION_TYPE.AGENTS_ASYNC_MIGRATION_REMOTE_REQUEST) {
					synchronized (MASSBase.getCurrentAgentsBase().getAsyncAgentIdList()) {
						MASSBase.getOutAsyncAgents()[rank] += message.getMigrationReqList().size();
					}
				}
				Socket sendSocket = new Socket(hostName, port);
				OutputStream os = sendSocket.getOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(os);
				oos.writeObject(message);
				oos.flush();
				if (message.getAction() == Message.ACTION_TYPE.AGENTS_ASYNC_MIGRATION_REMOTE_REQUEST) {
					logger.debug("{} agent(s) migrated. Wait from migration complete ack", message.getMigrationReqList().size());
					ObjectInputStream ois = new ObjectInputStream(sendSocket.getInputStream());
					AgentMigrationResponse decrease = (AgentMigrationResponse) ois.readObject();
					synchronized (MASSBase.getCurrentAgentsBase().getAsyncAgentIdList()) {
						int incompleteCount = runningChildRequestCount.addAndGet(decrease.getNumOfAgentReceived() * -1);
						if (decrease.isChosenAsParentPid()) {
							MASSBase.getChildAgentPids().add(rank);
						}
						if (incompleteCount == 0) {
							MASSBase.getCurrentAgentsBase().getAsyncAgentIdList().notifyAll();
						}
						logger.debug("Migration complete ACK received {}", incompleteCount);
					}
					ois.close();
				}
				oos.close();
				os.close();
				sendSocket.close();
				/*
				 * if (message.getAction() !=
				 * Message.ACTION_TYPE.AGENTS_ASYNC_MIGRATION_REMOTE_REQUEST) {
				 * if(runningChildRequestCount.decrementAndGet() == 0) {
				 * synchronized (MASS_base.getCurrentAgents().getAsyncQueue()) {
				 * MASS_base.getCurrentAgents().getAsyncQueue().notifyAll(); } }
				 * }
				 */
			} catch (IOException | ClassNotFoundException e) {
				logger.error("Unexpected exception in AsyncOutputThread", e);
			}

			logger.debug("Req to {} finished", rank);
		
		}
	}

	private class AsyncResultRequest extends Thread {
		private String hostname;

		public AsyncResultRequest(String hname) {
			runningChildRequestCount.incrementAndGet();
			hostname = hname;
		}

		@SuppressWarnings("unchecked")
		public void run() {
			try {
				Socket sendSocket = new Socket(hostname, port);
				Message message = new Message(Message.ACTION_TYPE.AGENT_ASYNC_RESULT);
				OutputStream os = sendSocket.getOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(os);
				oos.writeObject(message);
				ObjectInputStream ois = new ObjectInputStream(sendSocket.getInputStream());
				Message result = (Message) ois.readObject();
				synchronized (slaveResultLock) {
						logger.debug("Agent Async result from " + result.getSourcePid() + " has "
								+ ((List<Agent>) result.getArgument()).size() + " agents, local population is "
								+ result.getAgentPopulation());
					((List<Agent>) slaveResultLock.getResult()).addAll((List<Agent>) result.getArgument());
					((int[]) slaveResultLock.getSecondResult())[result.getSourcePid() - 1] = result
							.getAgentPopulation();
					slaveResultLock.incrementCounter();
					if (slaveResultLock.getCounter() == MASSBase.getRemoteNodes().size()) {
						slaveResultLock.set();
						slaveResultLock.notifyAll();
					}
				}
				oos.close();
				ois.close();
				os.close();
				sendSocket.close();
				synchronized (MASSBase.getCurrentAgentsBase().getAsyncAgentIdList()) {
					if (runningChildRequestCount.decrementAndGet() == 0) {
						MASSBase.getCurrentAgentsBase().getAsyncAgentIdList().notifyAll();
					}
				}
			} catch (IOException | ClassNotFoundException e) {
				logger.error("Unexpected exception in AsyncOutputThread", e);
			}
		}
	}

	/*
	 * private class SlaveNodeCompletenessRequest extends Thread { private MNode
	 * node;
	 * 
	 * public SlaveNodeCompletenessRequest(MNode n) { //
	 * runningChildRequestCount.incrementAndGet(); node = n; }
	 * 
	 * public void run() { try { Socket sendSocket = new
	 * Socket(node.getHostName(), port); Message message = new Message(
	 * Message.ACTION_TYPE.NODE_MASTER_ASYNC_COMPLETE_REQUEST); OutputStream os
	 * = sendSocket.getOutputStream(); ObjectOutputStream oos = new
	 * ObjectOutputStream(os); oos.writeObject(message); ObjectInputStream ois =
	 * new ObjectInputStream( sendSocket.getInputStream()); boolean result =
	 * (boolean) ois.readObject(); if (MASS.isConsoleLoggingEnabled()) {
	 * MASS_base.log("NODE_MASTER_ASYNC_COMPLETE_REQUEST from " +
	 * node.getHostName() + " return " + result); } synchronized
	 * (slaveCompleteLock) { if (!result) { slaveCompleteLock.setResult(result);
	 * MASS_base.getOutputMigrateSet().add(node.getPid()); }
	 * slaveCompleteLock.incrementCounter(); if (!result ||
	 * slaveCompleteLock.getCounter() == MASS_base.getRemoteNodes() .size()) {
	 * slaveCompleteLock.set(); slaveCompleteLock.notifyAll(); } } oos.close();
	 * ois.close(); os.close(); sendSocket.close(); synchronized
	 * (MASS_base.getCurrentAgents().getAsyncQueue()) { if
	 * (runningChildRequestCount.decrementAndGet() == 0) {
	 * MASS_base.getCurrentAgents().getAsyncQueue().notifyAll(); } if (/*
	 * MASS.isConsoleLoggingEnabled() true) { MASS.log(
	 * "SlaveNodeCompletenessRequest finish " + runningChildRequestCount.get());
	 * } }
	 * 
	 * } catch (IOException | ClassNotFoundException e) {
	 * MASS.logException(null, e); } } }
	 */

	/**
	 * ONLY to be call by Master node
	 * 
	 * @return public boolean requestSlaveNodeAsyncCompleteness() {
	 *         slaveCompleteLock.reset(); slaveCompleteLock.setResult(true);
	 *         Iterator<MNode> remoteNodeIter =
	 *         MASS_base.getRemoteNodes().iterator(); while
	 *         (remoteNodeIter.hasNext()) { new
	 *         SlaveNodeCompletenessRequest(remoteNodeIter.next()).start(); }
	 *         synchronized (slaveCompleteLock) { while
	 *         (!slaveCompleteLock.isReady()) { try { slaveCompleteLock.wait();
	 *         } catch (InterruptedException e) { } } } return (boolean)
	 *         slaveCompleteLock.getResult(); }
	 * 
	 *         public void notifyMasterOfCompleteness() { if
	 *         (sendCompleteNotifyToMaster) { // sendCompleteNotifyToMaster =
	 *         true; // return; if (MASS.isConsoleLoggingEnabled()) {
	 *         MASS.log("notifyMasterOfCompleteness"); } Message messageToDest =
	 *         new Message(
	 *         Message.ACTION_TYPE.NODE_SLAVE_ASYNC_COMPLETE_NOTIFY);
	 *         messageToDest.setSourcePid(MASS_base.getMyPid());
	 *         SendMessageByChild thread_ref = new SendMessageByChild(0,
	 *         messageToDest); thread_ref.start(); } }
	 */

	public void notifySourceOfCompleteness(int numOfInAgents) {
		if (MASSBase.getSourceAgentPid() != -1) {
			logger.debug("Send NODE_SLAVE_COMPLETE_NOTIFY_SENDER to {}", MASSBase.getSourceAgentPid());
			Message messageToDest = new Message(Message.ACTION_TYPE.NODE_COMPLETE_NOTIFY_SOURCE, numOfInAgents);
			messageToDest.setSourcePid(MASSBase.getMyPid());
			SendMessageByChild thread_ref = new SendMessageByChild(MASSBase.getSourceAgentPid(), messageToDest);
			thread_ref.start();
		}
		MASSBase.setSourceAgentPid(-1);
	}

	public boolean isIdle() {
		// read need to lock, use async queue to avoid nested lock in MProcess
		// and
		// Agents loop
		synchronized (MASSBase.getCurrentAgentsBase().getAsyncAgentIdList()) {
			logger.debug("AsyncOutputThread isIdle = {}", runningChildRequestCount.get());
			return runningChildRequestCount.get() == 0;
		}
	}

	/*
	 * public void setSendCompleteNotifyToMaster(boolean value) {
	 * sendCompleteNotifyToMaster = value; }
	 */
}
