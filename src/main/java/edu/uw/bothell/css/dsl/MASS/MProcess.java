/*

 	MASS Java Software License
	© 2012-2020 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2020 University of Washington. MASS was developed by Computing and Software Systems at University of 
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
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.ArrayList;
import edu.uw.bothell.css.dsl.MASS.graph.hippie.Hippie.*;
import edu.uw.bothell.css.dsl.MASS.graph.hippie.HippieVertex.VertexInitData;
import edu.uw.bothell.css.dsl.MASS.graph.hippie.*;
import edu.uw.bothell.css.dsl.MASS.graph.GraphMaintenance;
import edu.uw.bothell.css.dsl.MASS.GraphPlaces.InitArgs;
import java.lang.reflect.*;

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
		int maxNumberOfAgents = Integer.parseInt(args[6]);

		MASSBase.getLogger().debug("MProcess - main");

		AgentSerializer agentSerializer = AgentSerializer.getInstance();

		agentSerializer.setMaxNumberOfAgents(maxNumberOfAgents);

		// TODO: It is officially time to design a better way to configure the remote process

		try {
			MProcess mprocess = new MProcess(hostName, myPid, nProc, nThreads, serverPort, curDir);
			mprocess.start();
		} catch (Exception e) {
			try (PrintWriter pw = new PrintWriter("mass_fatal.log")) {
				e.printStackTrace(pw);
			}
		}

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
    	
    	// initialize the global clock
    	MASS.getGlobalClock().init( MASS.getEventDispatcher() );

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
	@SuppressWarnings("unused")
	public void start() {

		MASSBase.getLogger().debug("MProcess started");

		// Synchronize with the master node first.
		MASSBase.getLogger().debug("Sending MProcess startup ACK to master node...");
		sendAck();

		boolean alive = true;
		while (alive) {

			// receive a new message from the master
			Message m = receiveMessage();

			MASSBase.getLogger().debug("A new message received: action = {}", m.getAction());

			// get prepared for the following arguments for PLACES_INITIALIZE
			PlacesBase places = null; // new Places
			AgentsBase agents = null; // new Agents

			GraphPlaces graphPlaces;

			// retrieve an argument
			Object argument = m.getArgument();

			int returnsSize;

			switch ( m.getAction() ) {

			// NOOPs
			case PLACES_EXCHANGE_ALL_REMOTE_REQUEST:
			case PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT:
			case PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST:
			case PLACES_CALL_SOME_VOID_OBJECT:
				break;
			
			case ACK:
				sendAck();
				break;

			case AGENTS_MIGRATION_REMOTE_REQUEST:
				
				// if the Global Logical Clock is active, return the next value at which an event will be triggered
				if ( MASS.getGlobalClock() != null && MASS.getGlobalClock().getNextEventTrigger() > 0 ) {
					Message msg = new Message(Message.ACTION_TYPE.ACK, Long.valueOf( MASS.getGlobalClock().getNextEventTrigger() ) );
					sendMessage( msg);
				}
				
				break;
			
			case CLOCK_SET_VALUE:
				
				// force the Global Logical Clock to a specific value
				if ( MASS.getGlobalClock() != null && m.getArgument() != null && m.getArgument() instanceof Long ) {
					MASS.getGlobalClock().setValue( ( long ) m.getArgument() );
				}
				
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

				// force termination of this node
				// TODO - this is an ugly hack, but it guarantees termination regardless of the state of things
				System.exit( 0 );
				
				// yes, there is a "break" after system exit. just keeping things tidy.
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

			case PLACES_INITIALIZE_GRAPH:

				MASSBase.getLogger().debug("PLACES_INITIALIZE_GRAPH received");
				InitArgs initArgs = (InitArgs)argument;

				Object graphPlace = null;
				try {
					Class<?> cls = Class.forName(initArgs.className);
					Constructor<?> contructor = cls.getConstructor(int.class, String.class);
					graphPlace = contructor.newInstance(initArgs.handle, initArgs.vertexClassName);
				} catch (Exception e) {
					MASSBase.getLogger().error("PLACES_INITIALIZE_GRAPH exception thrown: " + e);
				}

				places = (PlacesBase)graphPlace;

				// establish all inter-node connections within setHosts( )
				MASSBase.setHosts( m.getHosts() );

				MASSBase.getPlacesMap().put( m.getHandle(), places );

				sendAck();
				MASSBase.getLogger().debug("PLACES_INITIALIZE_GRAPH completed");

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

				returnsSize = MASSBase.getCurrentPlacesBase().getPlacesSize();

				if ( GraphPlaces.class.isAssignableFrom( MASSBase.getCurrentPlacesBase().getClass() ) ) {
					
					if ( places != null ) {
						
						graphPlaces = (GraphPlaces) places;
						returnsSize = returnsSize + graphPlaces.getExtendedPlacesSize();

					}

				}

				MASSBase.setCurrentReturns(new Object[returnsSize]);

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

				// Perform graph exchangeAll separately for now
				if (GraphPlaces.class.isAssignableFrom(MASSBase.getCurrentPlacesBase().getClass())) {
					graphPlaces = (GraphPlaces) MASSBase.getCurrentPlacesBase();

					graphPlaces.exchangeAll(MASSBase.getCurrentFunctionId());
				}

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
				MASSBase.getLogger().debug("MASSBase.getCurrentReturns()" + MASSBase.getCurrentReturns().length);
				for(Object aObject :  MASSBase.getCurrentReturns()){
					MASSBase.getLogger().debug("aObject: " + aObject);
				}
				MASSBase.getLogger().debug(" MASSBase.getCurrentAgentsBase().getLocalPopulation()" +  MASSBase.getCurrentAgentsBase().getLocalPopulation() );
				
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
				
			// This case statement is executed by only the main thread. No MThread.barrierThreads(0) is necessary.
			case AGENTS_EXCHANGE_ALL:
				
				MASSBase.getLogger().debug("AGENTS_EXCHANGE_ALL received");
				
				MASSBase.getCurrentAgentsBase().exchangeAll();

				sendAck();
				
				break;

			case MAINTENANCE_ADD_PLACE:
				MASSBase.getLogger().warning("MAINTENANCE_ADD_PLACE has been deprecated. " +
					"Please migrate to using MAINTENANCE_ADD_VERTEX.");
				MASSBase.getLogger().debug("MAINTENANCE_ADD_PLACE received");

				places = MASS.getPlaces(m.getHandle());

				String errorMessage = "MAINTENANCE_ADD_PLACE [handle=" + m.getHandle() + "; places=" + places + "; argument=" + m.getArgument() + "]";

				MASSBase.getLogger().error(errorMessage);
				
				int result = ((GraphPlaces)places).addPlaceLocally(((Object[])argument)[0], ((Object[])argument)[1]);
				MASSBase.getLogger().debug("MAINNTENANCE_ADD_PLACE completed result: " + result);
				sendAck(result);
				
				break;

			case MAINTENANCE_ADD_VERTEX:
				MASSBase.getLogger().debug("MAINTENANCE_ADD_VERTEX received");

				places = MASS.getPlaces(m.getHandle());

				MASSBase.getLogger().debug("MAINTENANCE_ADD_VERTEX [handle=" + m.getHandle() + "; places=" + places + "; argument=" + m.getArgument() + "]");

				Object[] args = (Object[])argument;
				int vertexID = (Integer)args[0];
				Object vertexParams = args[1];
				
				boolean success = ((GraphPlaces)places).addVertexOnNode(MASS.getMyPid(), vertexID, vertexParams);
				MASSBase.getLogger().debug("MAINTENANCE_ADD_VERTEX completed result: " + success);
				sendAck(success ? 1 : 0);
			
				break;

			case MAINTENANCE_ADD_EDGE:
				MASSBase.getLogger().debug("MAINTENANCE_ADD_EDGE received");
				MASSBase.getLogger().warning("MAINTENANCE_ADD_EDGE is deprecated " +
					"and will be removed in a future release. Please migrate to using " +
					"MAINTENANCE_ADD_EDGE_V2.");

				places = MASS.getPlaces(m.getHandle());

				graphPlaces = ((GraphPlaces) places);

				graphPlaces.addEdgeLocally((Integer) ((Object[])argument)[0], (Integer)((Object[])argument)[1], (Double)((Object[])argument)[2]);

				sendAck();

				MASSBase.getLogger().debug("MAINTENANCE_ADD_EDGE completed");
				break;
			
			case MAINTENANCE_ADD_EDGE_V2:
				MASSBase.getLogger().debug("MAINTENANCE_ADD_EDGE_V2 received");

				places = MASS.getPlaces(m.getHandle());

				graphPlaces = ((GraphPlaces) places);
				Object[] addEdgeArgs = (Object[])argument;

				graphPlaces.addEdgeOnNode(
					MASS.getMyPid(), 
					(Integer)addEdgeArgs[0], 
					(Integer)addEdgeArgs[1], 
					(Double)addEdgeArgs[2]
				);

				sendAck();

				MASSBase.getLogger().debug("MAINTENANCE_ADD_EDGE_V2 completed");
				break;

			case MAINTENANCE_REMOVE_PLACE:
				MASSBase.getLogger().warning("MAINTENANCE_REMOVE_PLACE has been deprecated. " +
					"Please migrate to using MAINTENANCE_REMOVE_VERTEX.");
				MASSBase.getLogger().debug("MAINTENANCE_REMOVE_PLACE received");

				places = MASS.getPlaces(m.getHandle());

				((GraphPlaces) places).removeVertexLocally((Integer) m.getArgument());

				sendAck();

				MASSBase.getLogger().debug("MAINNTENANCE_REMOVE_PLACE completed");
				break;
			
			case MAINTENANCE_REMOVE_VERTEX:
				MASSBase.getLogger().debug("MAINTENANCE_REMOVE_VERTEX received");

				places = MASS.getPlaces(m.getHandle());
				
				((GraphPlaces) places).removeVertexOnNode(MASS.getMyPid(), (Integer) m.getArgument());

				sendAck();

				MASSBase.getLogger().debug("MAINTENANCE_REMOVE_VERTEX completed");
				break;

			case MAINTENANCE_REMOVE_NEIGHBOR:
				MASSBase.getLogger().debug("MAINTENANCE_REMOVE_NEIGHBOR received");

				places = MASS.getPlaces(m.getHandle());
				((GraphPlaces) places).removeNeighborFromLocalVertices((Integer) m.getArgument());
				sendAck();
				
				MASSBase.getLogger().debug("MAINTENANCE_REMOVE_NEIGHBOR completed");
				break;

			case MAINTENANCE_REMOVE_EDGE:
				MASSBase.getLogger().debug("MAINTENANCE_REMOVE_EDGE received");
				MASSBase.getLogger().warning("MAINTENANCE_REMOVE_EDGE is deprecated " +
					"and will be removed in a future release. Please migrate to using " +
					"MAINTENANCE_REMOVE_EDGE_V2.");

				places = MASS.getPlaces(m.getHandle());

				graphPlaces = ((GraphPlaces) places);

				graphPlaces.removeEdgeLocally((Integer) ((Object[])argument)[0], (Integer)((Object[])argument)[1]);

				sendAck();

				MASSBase.getLogger().debug("MAINNTENANCE_REMOVE_EDGE completed");
				break;
			
			case MAINTENANCE_REMOVE_EDGE_V2:
				MASSBase.getLogger().debug("MAINTENANCE_REMOVE_EDGE_V2 received");

				places = MASS.getPlaces(m.getHandle());

				graphPlaces = ((GraphPlaces) places);
				Object[] removeEdgeArgs = (Object[])argument;

				graphPlaces.removeEdgeOnNode(
					MASS.getMyPid(), 
					(Integer)removeEdgeArgs[0], 
					(Integer)removeEdgeArgs[1]
				);

				sendAck();

				MASSBase.getLogger().debug("MAINTENANCE_REMOVE_EDGE_V2 completed");
				break;

			case MAINTENANCE_GET_VERTEX:
				MASSBase.getLogger().debug("MAINTENANCE_GET_VERTEX received");
				int handle = m.getHandle();
				places = MASS.getPlaces(handle);
				graphPlaces = ((GraphPlaces) places);

				// Retrieve and clone the vertex place. Cloning is necessary
				// to prevent the object stream from sending a reference from
				// an old object. The alternative is to reset the stream which 
				// may have other unknown performance implications. Depending
				// how how the message stream is utilized elsewhere in MASS.
				Object vertex = null;
				try {
					vertex = graphPlaces.getVertexFromNode(
						MASS.getMyPid(),
						((Integer) m.getArgument()).intValue()
					).clone();
				} catch (CloneNotSupportedException cnse) {
					MASSBase.getLogger().error("VertexPlace not cloneable: " + cnse);
				} catch (Exception e) {
					MASSBase.getLogger().error("An unexpected error occurred: " + e);
				}

				// Send vertex to requester.
				Message msg = new Message(
					Message.ACTION_TYPE.MAINTENANCE_GET_VERTEX_RESPONSE,
					handle,
					vertex
				);
				sendMessage(msg);

				MASSBase.getLogger().debug("MAINTENANCE_GET_VERTEX_RESPONSE sent");
				break;
			
			case MAINTENANCE_BULK_GRAPH_HIPPIE_OPS:
				MASSBase.getLogger().debug("MAINTENANCE_BULK_GRAPH_HIPPIE_OPS received");

				places = MASS.getPlaces(m.getHandle());
				Hippie hippiePlaces = (Hippie)places;

				ArrayList<HippieOp> ops = (ArrayList<HippieOp>)argument;

				int myPid = MASS.getMyPid();

				for (HippieOp op : ops) {
					if (op.funcID == HippieOp.FUNC_ADD_VERTEX) {
						VertexInitData vid = new VertexInitData(op.sourceKeys[0], op.sourceKeys[1]);
						hippiePlaces.addVertexOnNode(myPid, op.sourceID, vid);
					} else if (op.funcID == HippieOp.FUNC_ADD_EDGE) {
						hippiePlaces.addEdgeOnNode(myPid, op.sourceID, op.destID, op.edgeWeight, op.extendedAttribs);
					}
				}

				sendAck();

				MASSBase.getLogger().debug("MAINTENANCE_BULK_GRAPH_HIPPIE_OPS completed");
				break;

			case MAINTENANCE_GET_PLACES:
				MASSBase.getLogger().debug("MAINTENANCE_GET_PLACES received");

				places = MASS.getPlaces(m.getHandle());

				sendMessage(new Message(Message.ACTION_TYPE.MAINTENANCE_GET_PLACES_RESPONSE, GraphMaintenance.getPlaces((GraphPlaces)places)));

				MASSBase.getLogger().debug("MAINTENANCE_GET_PLACES received");
				break;
			

			case GRAPH_PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT:
				MASSBase.getLogger().debug("GRAPH_PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT");

				graphPlaces = (GraphPlaces) MASS.getPlaces(m.getHandle());

				Object o = graphPlaces.exchangeNeighbor(m.getFunctionId(), (Integer) m.getArgument());

				sendMessage(new Message(Message.ACTION_TYPE.GRAPH_PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT, o));

				break;

			case MAINTENANCE_REINITIALIZE:
				MASSBase.getLogger().debug("GRAPH_PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT");

				graphPlaces = (GraphPlaces) MASS.getPlaces(m.getHandle());

				graphPlaces.reinitialize();

				sendAck();

				break;
			
			default:
				MASSBase.getLogger().debug( "Unrecognized Message Type!" );
				break;
			
			}

		}
	}

//	private void sendMessage(Serializable object) {
//		try {
//			MAIN_OOS.writeObject(object);
//			MAIN_OOS.flush();
//		} catch (IOException e) {
//			MASSBase.getLogger().error("Exception sending object to remote host", e);
//		}
//	}

}
