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

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * MProcess exists to facilitate message-passing between remote and master
 * nodes.
 */
public class MProcess {

	// representation of this remote node, containing all configuration info and object streams
	private MNode thisNode;

	private int myPid; // my pid or rank
	private ObjectInputStream MAIN_IOS = null; 	// input from the master process
	private ObjectOutputStream MAIN_OOS = null; // output to the master process

	/**
	 * MProcesses are the MASS threads executing on various machines. They are
	 * responsible for maintaining some number of the total Places being used by
	 * the entire MASS program, as well as the associated Agents. Each MProcess
	 * is referred to by its rank.
	 * 
	 * @param hostName The hostname or IP address of this node
	 * @param myPid The PID assigned to this node
	 * @param nProc The total number of nodes in the cluster
	 * @param nThr The number of threads to start on this remote node
	 * @param port The port number to use for communications with this node
	 * @param curDir The working directory this remote node should use
	 * @param in An InputStream override for message communication to this node
	 * @param out an OutputStream override for message communications from this node
	 */
	public MProcess(String hostName, int myPid, int nProc, int nThr, int port, String curDir, InputStream in, OutputStream out ) {
		
		// override console input/output, primarily for unit testing
		try {

			if ( in != null) MAIN_IOS = new ObjectInputStream( in );
			if ( out != null) MAIN_OOS = new ObjectOutputStream( out );

		} catch (Exception e) {

			MASSBase.getLogger().error("MProcess.Mprocess: detected ", e);
			System.exit(-1);

		}

		// perform normal init
		init( hostName, myPid, nProc, nThr, port, curDir );
		
	}

	/**
	 * MProcesses are the MASS threads executing on various machines. They are
	 * responsible for maintaining some number of the total Places being used by
	 * the entire MASS program, as well as the associated Agents. Each MProcess
	 * is referred to by its rank.
	 * @param hostName The hostname or IP address of this node
	 * @param myPid The PID assigned to this node
	 * @param nProc The total number of nodes in the cluster
	 * @param nThr The number of threads to start on this remote node
	 * @param port The port number to use for communications with this node
	 * @param curDir The working directory this remote node should use
	 */
	public MProcess(String hostName, int myPid, int nProc, int nThr, int port, String curDir) {
		init( hostName, myPid, nProc, nThr, port, curDir );
	}

	/**
	 * Main MASS function that launches MProcess
	 * 
	 * @param args
	 *            Required arguments for launching MProcess (hostname, my PID,
	 *            number of cluster nodes, number of threads to start, port
	 *            number for communications, and the working directory to use)
	 */
	public static void main(String[] args) throws Exception {

		String hostName = args[0];
		int myPid = Integer.parseInt(args[1]);
		int nProc = Integer.parseInt(args[2]);
		int nThreads = Integer.parseInt(args[3]);
		int serverPort = Integer.parseInt(args[4]);
		String curDir = args[5];

		MProcess mprocess = new MProcess(hostName, myPid, nProc, nThreads, serverPort, curDir);
		mprocess.start();

	}

	// Initialize this MProcess (this used to be handled by a single constructor)
	private void init(String hostName, int myPid, int nProc, int nThr, int port, String curDir) {

		this.myPid = myPid;

		// create a MNode representation of this node for init purposes
		thisNode = new MNode();
		thisNode.setHostName(hostName);
		thisNode.setPid(myPid);
		thisNode.setPort(port);
		thisNode.setMassHome(curDir);

		MASS.setNumThreads(nThr);
		MASSBase.setWorkingDirectory(curDir); // mprocess manually changes it.
		MASSBase.setSystemSize(nProc); // must force system size since we don't
										// have visibility to all nodes
		MASSBase.initMASSBase(thisNode);

		MASSBase.getLogger().debug("Launching MProcess... (" + "hostname = " + hostName + ", myPid = " + myPid
				+ ", nProc = " + nProc + ", nThr = " + nThr + ", port = " + port + ", curDir = " + curDir + ")");

		MASSBase.initializeThreads(MASS.getNumThreads());

		/*
		 * Set up a connection with the master process on the master node
		 * (communications at this point to/from master node are channeled
		 * through SSH connection), but only if the streams have not already been
		 * instantiated.
		 */
		try {

			if ( MAIN_IOS == null) MAIN_IOS = new ObjectInputStream( System.in );
			if ( MAIN_OOS == null) MAIN_OOS = new ObjectOutputStream( System.out );

		} catch (Exception e) {

			MASSBase.getLogger().error("MProcess.Mprocess: detected ", e);
			System.exit(-1);

		}

    	// initialize the messaging system
    	MASS.getMessagingProvider().init( null, null );
		
	}

	private Message receiveMessage() {

		try {
			return (Message) MAIN_IOS.readObject();
		} catch (Exception e) {
			MASSBase.getLogger().error("MProcess.receiveMessage: detected ", e);
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
		MASSBase.getLogger().debug("msg.getAgentPopulation = {}", msg.getAgentPopulation());

		sendMessage(msg);

	}

	private void sendMessage(Message msg) {

		try {

			MAIN_OOS.writeObject(msg);
			MAIN_OOS.flush();

		} catch (Exception e) {

			MASSBase.getLogger().error("MProcess.sendMessage: ", e);
			System.exit(-1);

		}

	}

	private void sendReturnValues(Object argument) {
		sendMessage( new Message( Message.ACTION_TYPE.ACK, argument ) );
	}

	private void sendReturnValues(Object argument, int localPopulation) {
		sendMessage( new Message( Message.ACTION_TYPE.ACK, argument, localPopulation ) );
	}

	/**
	 * Start this MProcess
	 */
	public void start() {

		MASSBase.getLogger().debug("MProcess started");

		// Synchronize with the master node first.
		sendAck();

		boolean alive = true;
		while (alive) {

			// receive a new message from the master
			Message m = receiveMessage();

			MASSBase.getLogger().debug("A new message received: action = {}", m.getAction());

			// get prepared for the following arguments for PLACES_INITIALIZE
			PlacesBase places = null; // new Places
			AgentsBase agents = null; // new Agents

			// retrieve an argument
			Object argument = m.getArgument();

			switch ( m.getAction() ) {

			// NOOPs
			case AGENTS_MIGRATION_REMOTE_REQUEST:
			case PLACES_EXCHANGE_ALL_REMOTE_REQUEST:
			case PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT:
			case PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST:
			case PLACES_CALL_SOME_VOID_OBJECT:
				break;
			
			case ACK:
				sendAck();
				break;

			case EMPTY:
				MASSBase.getLogger().debug("EMPTY received!!!!");
				sendAck();
				break;

			case FINISH:

				// shutdown messaging system
		    	MASS.getMessagingProvider().shutdown();

				MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_TERMINATE);
				
				// confirm all threads are done with finish
				MThread.barrierThreads(0);
				MASSBase.getExchange().terminateConnection(this.myPid);
				sendAck();
				alive = false;
				MASSBase.getLogger().debug("FINISH received and ACK sent");

				break;

			case PLACES_INITIALIZE:

				MASSBase.getLogger().debug("PLACES_INITIALIZE received");

				// create a new Places
				places = new PlacesBase( m.getHandle(), m.getClassname(), m.getBoundaryWidth(), argument, m.getSize() );

				// establish all inter-node connections within setHosts( )
				MASSBase.setHosts( m.getHosts() );
				MASSBase.getPlacesMap().put( m.getHandle(), places );

				sendAck();
				MASSBase.getLogger().debug("PLACES_INITIALIZE completed and ACK sent");
				
				break;

			case PLACES_CALL_ALL_VOID_OBJECT:

				MASSBase.getLogger().debug("PLACES_CALL_ALL_VOID_OBJECT received");

				// retrieve the corresponding places
				MASSBase.setCurrentPlacesBase(MASSBase.getPlacesMap().get(m.getHandle()));
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

				MASSBase.getLogger().debug("PLACES_CALL_ALL_RETURN_OBJECT received");

				// retrieve the corresponding places
				MASSBase.setCurrentPlacesBase(MASSBase.getPlacesMap().get(m.getHandle()));
				MASSBase.setCurrentFunctionId(m.getFunctionId());
				MASSBase.setCurrentArgument(argument);
				MASSBase.setCurrentMsgType(m.getAction());
				MASSBase.setCurrentReturns(new Object[MASSBase.getCurrentPlacesBase().getPlacesSize()]);

				// From Jas' and Michael's implementation
				// TODO - better to use this than "getPlacesSize" ?
				// MASSBase.setCurrentReturns(new Object[MASSBase.getCurrentPlacesBase().getNumberOfPlacesOnCurrentNode()]);
				
				// resume threads to work on call all.
				MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_CALLALL);

				// 3rd arg: 0 = the main thread id
				MASSBase.getCurrentPlacesBase().callAll(MASSBase.getCurrentFunctionId(),
						(Object[]) (MASSBase.getCurrentArgument()), ((Object[]) (MASSBase.getCurrentArgument())).length,
						0);

				// confirm all threads are done with places.callAll w/ return
				MThread.barrierThreads(0);

				sendReturnValues(MASSBase.getCurrentReturns());
				break;

			case PLACES_EXCHANGE_ALL:

				MASSBase.getLogger().debug("PLACES_EXCHANGE_ALL received handle = " + m.getHandle() + " dest_handle = "
						+ m.getDestHandle());

				// retrieve the corresponding places
				MASSBase.setCurrentPlacesBase(MASSBase.getPlacesMap().get(m.getHandle()));
				MASSBase.setDestinationPlaces(MASSBase.getPlacesMap().get(m.getDestHandle()));
				MASSBase.setCurrentFunctionId(m.getFunctionId());
				// MASS_base.currentDestinations = m.getDestinations( );

				// reset requestCounter by the main thread
				MASSBase.resetRequestCounter();

				// for debug
				MASSBase.showHosts();

				// resume threads to work on call all.
				MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_EXCHANGEALL);

				// exchangeall implementation
				MASSBase.getCurrentPlacesBase().exchangeAll(MASSBase.getDestinationPlaces(),
						MASSBase.getCurrentFunctionId(), 0);

				// confirm all threads are done with places.exchangeall.
				MThread.barrierThreads(0);
				MASSBase.getLogger().debug("barrier done");

				sendAck();
				MASSBase.getLogger().debug("PLACES_EXCHANGE_ALL sent ACK");

				break;

			case PLACES_EXCHANGE_BOUNDARY:

				MASSBase.getLogger().debug("PLACES_EXCHANGE_BOUNDARY received handle = {}", m.getHandle());

				// retrieve the corresponding places
				MASSBase.setCurrentPlacesBase(MASSBase.getPlacesMap().get(m.getHandle()));

				// for debug
				MASSBase.showHosts();

				// exchange boundary implementation
				MASSBase.getCurrentPlacesBase().exchangeBoundary();

				sendAck();

				MASSBase.getLogger().debug("PLACES_EXCHANGE_BOUNDARY completed and ACK sent");

				break;

			case AGENTS_INITIALIZE:

				MASSBase.getLogger().debug("AGENTS_INITIALIZE received");

				agents = new AgentsBase(m.getHandle(), m.getClassname(), argument, m.getDestHandle(),
						m.getAgentPopulation());

				MASSBase.getAgentsMap().put(m.getHandle(), agents);

				sendAck(agents.getLocalPopulation());

				MASSBase.getLogger().debug("AGENTS_INITIALIZE completed and ACK sent");

				break;

			case AGENTS_CALL_ALL_VOID_OBJECT:

				MASSBase.getLogger().debug("AGENTS_CALL_ALL_VOID_OBJECT received");

				MASSBase.setCurrentAgentsBase( MASSBase.getAgentsMap().get( m.getHandle() ) );
				MASSBase.setCurrentFunctionId( m.getFunctionId() );
				MASSBase.setCurrentArgument( argument );
				MASSBase.setCurrentMsgType( m.getAction() );

				MThread.setAgentBagSize(MASSBase.getCurrentAgentsBase().getAgents().size_unreduced());

				// resume threads to work on call all
				MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_AGENTSCALLALL);

				MASSBase.getCurrentAgentsBase().callAll(m.getFunctionId(), argument, 0);

				// confirm all threads are done with agents.callAll
				MThread.barrierThreads(0);

				MASSBase.getLogger().debug("barrier done");

				sendAck(MASSBase.getCurrentAgentsBase().getLocalPopulation());
				break;

			case AGENTS_CALL_ALL_RETURN_OBJECT:

				MASSBase.getLogger().debug("AGENTS_CALL_ALL_RETURN_OBJECT received");

				MASSBase.setCurrentAgentsBase( MASSBase.getAgentsMap().get( m.getHandle() ) );
				MASSBase.setCurrentFunctionId( m.getFunctionId() );
				MASSBase.setCurrentArgument( argument );
				MASSBase.setCurrentMsgType( m.getAction() );
				MASSBase.setCurrentReturns( new Object[ MASSBase.getCurrentAgentsBase().getLocalPopulation() ] );

				MThread.setAgentBagSize(MASSBase.getCurrentAgentsBase().getAgents().size_unreduced());

				// resume threads to work on call all with return objects
				MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_AGENTSCALLALL);

				MASSBase.getCurrentAgentsBase().callAll(MASSBase.getCurrentFunctionId(),
						(Object[]) (MASSBase.getCurrentArgument()), 0);

				// confirm all threads are done with agnets.callAll with
				// return objects
				MThread.barrierThreads(0);
				MASSBase.getLogger().debug("barrier done");

				sendReturnValues(MASSBase.getCurrentReturns(), MASSBase.getCurrentAgentsBase().getLocalPopulation());

				break;

			case AGENTS_MANAGE_ALL:

				MASSBase.getLogger().debug("AGENTS_MANAGE_ALL received");

				MASSBase.setCurrentAgentsBase(MASSBase.getAgentsMap().get(m.getHandle()));
				MThread.setAgentBagSize(MASSBase.getCurrentAgentsBase().getAgents().size_unreduced());

				MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_MANAGEALL);

				MASSBase.getCurrentAgentsBase().manageAll(0); // 0 = the main
																// tid

				// confirm all threads are done with agents.manageAll.
				MThread.barrierThreads(0);

				MASSBase.getLogger().debug("sendAck will send localPopulation = {}",
						MASSBase.getCurrentAgentsBase().getLocalPopulation());

				sendAck(MASSBase.getCurrentAgentsBase().getLocalPopulation());

				break;
				
			case AGENTS_EXCHANGE_ALL:
				
				MASSBase.getLogger().debug("AGENTS_EXCHANGE_ALL received");
				
				MASSBase.getCurrentAgentsBase().exchangeAll();
				MThread.barrierThreads(0);
				sendAck();
				
				break;

			}

		}

	}

}