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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.Vector;

import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import edu.uw.bothell.css.dsl.MASS.logging.LogLevel;

public class MASSBaseTest extends AbstractTest {

	@TestSubject
	private MASSBase massBase = new MASSBase();
	
	@Mock
	private AgentsBase agentsBase;
	
	@Mock
	private ExchangeHelper exchangeHelper;
	
	@Mock
	private Places places;

	@Test
	@SuppressWarnings("static-access")
	public void addNode() throws Exception {
		
		// testing in isolation...
		MASSBase mb = new MASSBase();
		
		// add a single master node
		MNode masterNode = new MNode();
		masterNode.setHostName( randomString() );
		masterNode.setMaster( true );
		mb.addNode( masterNode );
		
		// verify node representations
		assertEquals( 1, mb.getAllNodes().size() );
		assertEquals( 1, mb.getHosts().size() );
		assertEquals( 0, mb.getRemoteNodes().size() );
		assertEquals( 1, mb.getSystemSize() );
		assertEquals( masterNode, mb.getMasterNode() );
		
		// master node should have PID of zero
		assertEquals( 0, masterNode.getPid() );
		
		// add a remote node
		MNode remoteNode = new MNode();
		remoteNode.setHostName( randomString() );
		mb.addNode( remoteNode );

		replayAll();

		// verify node representations
		assertEquals( 2, mb.getAllNodes().size() );
		assertEquals( 2, mb.getHosts().size() );
		assertEquals( 1, mb.getRemoteNodes().size() );
		assertEquals( 2, mb.getSystemSize() );
		assertEquals( masterNode, mb.getMasterNode() );

		// remote node should have PID of one (auto incremented)
		assertEquals( 1, remoteNode.getPid() );

	}

	@Test
	@SuppressWarnings("static-access")
	public void getAgents() throws Exception {

		replayAll();

		// agents collection is managed by Agents (bad practice!)
		// only thing to do here is make sure it doesn't throw an exception
		assertNull( massBase.getAgents( 0 ) );
		
	}
	
	@Test
	@SuppressWarnings("static-access")
	public void getAgentsMap() throws Exception {

		replayAll();

		// agents collection is managed by Agents (bad practice!)
		// only thing to do here is make sure it isn't NULL on startup
		assertNotNull( massBase.getAgentsMap() );
		assertEquals( 0, massBase.getAgentsMap().size() );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void getAllNodesPreInit() throws Exception {
		
		// testing in isolation...
		MASSBase mb = new MASSBase();

		replayAll();

		// should not have any nodes when first instantiated
		assertEquals( 0, mb.getAllNodes().size() );
		assertEquals( 0, mb.getHosts().size() );
		assertEquals( 0, mb.getRemoteNodes().size() );
		assertEquals( 0, mb.getSystemSize() );
		assertNull( mb.getMasterNode() );
		
	}
	
	@Test
	@SuppressWarnings("static-access")
	public void getCores() throws Exception {

		replayAll();

		// should be non-zero, since it's calculated at runtime
		assertTrue( massBase.getCores() > 0 );
		
	}
	
	@Test
	@SuppressWarnings("static-access")
	public void getExchange() throws Exception {

		replayAll();

		// make sure one was init'd during MASSBase instantiation
		assertNotNull( massBase.getExchange() );
		
	}
	
	@Test
	@SuppressWarnings("static-access")
	public void getLogger() throws Exception {
		replayAll();
		assertNotNull( massBase.getLogger() );
	}
	
	@Test
	@SuppressWarnings("static-access")
	public void getPlaces() throws Exception {

		replayAll();

		// places map should not contain any entries yet
		Hashtable<Integer, PlacesBase> map =  massBase.getPlacesMap();
		assertNotNull( map );
		assertEquals( 0, map.size() );

		// force an entry into the map for testing
		map.put( 1, places );
		
		// should exist in the map, and should be able to retrieve by handle ID
		assertEquals( 1, map.size() );
		Places pl = massBase.getPlaces( 1 );
		assertEquals( places, pl );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void getSetAgentMigrationRequests() throws Exception {

		replayAll();

		// should not be a NULL collection to begin with
		assertNotNull( massBase.getMigrationRequests() );
		
		// should be able to set a new collection
		Vector< Vector < AgentMigrationRequest > > newReqestCollection = new Vector< Vector < AgentMigrationRequest > >();
		massBase.setMigrationRequests( newReqestCollection );
		
		assertEquals( newReqestCollection, massBase.getMigrationRequests() );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void getSetCommunicationPort() throws Exception {
		
		replayAll();

		// add a single master node
		MNode masterNode = new MNode();
		masterNode.setHostName( randomString() );
		masterNode.setMaster( true );
		masterNode.setPort( 80 );
		massBase.initMASSBase(masterNode);
		
		// should start with the port number set in the master MNode
		assertEquals( 80, massBase.getCommunicationPort() );

		// set a new port number
		int newPort = 42; 
		massBase.setCommunicationPort( newPort );
		
		// should have the new port number set
		assertEquals( newPort, massBase.getCommunicationPort() );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void getSetCurrentAgentsBase() throws Exception {

		replayAll();
		
		// should not be an existing association
		assertNull( massBase.getCurrentAgentsBase() );
		
		// set the current AgentsBase
		massBase.setCurrentAgentsBase( agentsBase );
		
		// should be there
		assertEquals( agentsBase, massBase.getCurrentAgentsBase() );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void getSetCurrentArgument() throws Exception {

		replayAll();

		// should not have a current argument
		assertNull( massBase.getCurrentArgument() );
		
		// set one, and check
		String testObj = randomString();
		massBase.setCurrentArgument( testObj );
		assertEquals( testObj, massBase.getCurrentArgument() );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void getSetCurrentFunctionId() throws Exception {

		replayAll();

		// should not have a current function ID
		assertEquals( 0,  massBase.getCurrentFunctionId() );
		
		// set one, and check
		int functionId = randomInt();
		massBase.setCurrentFunctionId( functionId );
		assertEquals( functionId, massBase.getCurrentFunctionId() );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void getSetCurrentMessageType() throws Exception {

		replayAll();

		// should not have a current message type
		assertNull( massBase.getCurrentMsgType() );
		
		// set one, and check
		massBase.setCurrentMsgType( Message.ACTION_TYPE.ACK );
		assertEquals( Message.ACTION_TYPE.ACK, massBase.getCurrentMsgType() );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void getSetCurrentPlacesBase() throws Exception {

		// testing in isolation...
		MASSBase mb = new MASSBase();
		
		// add a single master node
		MNode masterNode = new MNode();
		masterNode.setHostName( randomString() );
		masterNode.setMaster( true );
		mb.addNode( masterNode );
		
		// "init" MASSBase to set it's own node
		mb.initMASSBase( masterNode );

		replayAll();

		// should not be an existing association
		assertNull( mb.getCurrentPlacesBase() );

		// force PlacesMap to something to prevent NPE
		PlacesBase pb = new PlacesBase( randomInt() , null, 0, null, new int[0] );
		mb.getPlacesMap().put( pb.getHandle(), pb );
		
		// set the current PlacesBase
		mb.setCurrentPlacesBase( pb );
		
		// should be there
		assertEquals( pb, mb.getCurrentPlacesBase() );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void getSetCurrentReturns() throws Exception {

		replayAll();

		// should not have a current returns array
		assertNull( massBase.getCurrentReturns() );
		
		// set one, and check
		String[] testObj = new String[0];
		massBase.setCurrentReturns( testObj );
		assertNotNull( massBase.getCurrentReturns() );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void getSetDestinationPlaces() throws Exception {

		replayAll();

		// should not have a current destination
		assertNull( massBase.getDestinationPlaces() );
		
		// set one, and check
		PlacesBase pb = new PlacesBase( randomInt() , null, 0, null, new int[0] );
		massBase.setDestinationPlaces( pb );
		assertEquals( pb,  massBase.getDestinationPlaces() );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void getSetRemoteAgentMigrationRequests() throws Exception {

		replayAll();

		// should not be a NULL collection to begin with
		assertNotNull( massBase.getRemoteRequests() );
		
		// should be able to set a new collection
		Vector< Vector < RemoteExchangeRequest > > newReqestCollection = new Vector< Vector < RemoteExchangeRequest > >();
		massBase.setRemoteRequests( newReqestCollection );
		
		assertEquals( newReqestCollection, massBase.getRemoteRequests() );
		
	}
	
	@Test
	@SuppressWarnings("static-access")
	public void getSetSystemSize() throws Exception {
		
		replayAll();

		// forcing a value like what happens when remote MProcess is started
		int newSystemSize = randomInt(); 
		massBase.setSystemSize( newSystemSize );
		
		assertEquals( newSystemSize, massBase.getSystemSize() );
		
	}
	
	@Test
	@SuppressWarnings("static-access")
	public void getSetWorkingDirectory() throws Exception {

		replayAll();

		// remember the original setting to reset back after test completes
		String currentDirectory = massBase.getWorkingDirectory();
		assertNotNull( currentDirectory );
		
		// set to a new directory and test
		String newWorkingDirectory = "/";
		massBase.setWorkingDirectory( newWorkingDirectory );
		assertEquals( newWorkingDirectory, massBase.getWorkingDirectory() );
		
		// reset back and check
		massBase.setWorkingDirectory( currentDirectory );
		assertEquals( currentDirectory, massBase.getWorkingDirectory() );
		
	}
	
	@Test
	@SuppressWarnings("static-access")
	public void hasValidLogFilenameAutoDetectHostname() throws Exception {

		// testing in isolation...
		MASSBase mb = new MASSBase();

		// init without specifying a hostname in node config
		mb.initMASSBase( new MNode() );

		replayAll();

		// should generate a valid logging filename, with a valid host
		String loggingFilename = mb.getLogFileName();
		assertNotNull( loggingFilename );
		assertFalse( loggingFilename.toLowerCase().contains( "null" ) );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void initMASSBaseLegacyMode() throws Exception {

		// testing in isolation...
		MASSBase mb = new MASSBase();
		int port = randomInt();
		
		// "init" MASSBase to set it's own node
		mb.initMASSBase( randomString(), 0, 0, port );

		replayAll();

		// verify, simple check of init
		assertEquals( port, mb.getCommunicationPort() );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void resetRequestCounter() throws Exception {
		
		// nothing should happen, no exceptions thrown - (NOOP)
		replayAll();
		massBase.resetRequestCounter();
		
	}


	@Test
	@SuppressWarnings("static-access")
	public void setHosts() throws Exception {

		Vector<String> testHosts = new Vector<>();
		testHosts.add( "host1" );
		testHosts.add( "host2" );
		
		// get reference to current exchange helper for replacement once test is done
		ExchangeHelper currentExchangeHelper = massBase.getExchange();
		
		// using mock object for ExchangeHelper
		massBase.setExchange( exchangeHelper );
		
		exchangeHelper.establishConnection( testHosts.size(), 0, testHosts, massBase.getCommunicationPort() );
		
		replayAll();

		massBase.setHosts( testHosts );
		
		// exchange requests should be set to the number of hosts
		assertEquals( testHosts.size(), massBase.getMigrationRequests().size() );
		assertEquals( testHosts.size(), massBase.getRemoteRequests().size() );
		
		// return MASS Base back to original state
		massBase.setExchange( currentExchangeHelper );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void showHosts() throws Exception {
		
		replayAll();
		
		// the only thing to check for is no exceptions thrown
		// when logging is in debug mode
		massBase.getLogger().setLogLevel(LogLevel.DEBUG);
		massBase.showHosts();

	}

	@After
	@SuppressWarnings("static-access")
	public void tearDown() {

		// perform normal cleanup activities
		super.tearDown();
		
		// reset test subject fields
		massBase.getAllNodes().clear();
		massBase.setCurrentArgument( null );
		massBase.setCurrentFunctionId( 0 );
		massBase.setCurrentMsgType( null );
		massBase.setCurrentAgentsBase( null );
		massBase.setCurrentAgentsBase( null );
		massBase.setCurrentReturns( null );
		massBase.setDestinationPlaces( null );
		massBase.getPlacesMap().clear();
		massBase.getRemoteNodes().clear();
		massBase.getHosts().clear();
		
		// reset state
		massBase.getLogger().setLogLevel(LogLevel.OFF);


	}

	// TODO - can be tested?
	@Ignore
	@Test
	@SuppressWarnings("static-access")
	public void initializeThreads() throws Exception {
		
		replayAll();
		
		// try init'ing zero threads
		assertTrue( massBase.initializeThreads( 0 ) );
		
		
		
	}

}