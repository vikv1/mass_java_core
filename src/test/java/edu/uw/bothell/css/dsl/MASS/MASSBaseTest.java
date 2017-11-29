/*

 	MASS Java Software License
	© 2012-2017 University of Washington

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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import edu.uw.bothell.css.dsl.MASS.logging.LogLevel;

/**
 * Perform a series of unit tests against the MASSBase class to verify proper
 * and consistent behavior of the class / methods
 */
public class MASSBaseTest extends AbstractTest {

	@Mock
	private AgentsBase agentsBase;
	
	@Mock
	private ExchangeHelper exchangeHelper;
	
	@Mock
	private Places places;

	@Test
	public void addNode() throws Exception {
		
		// add a single master node
		MNode masterNode = new MNode();
		masterNode.setHostName( randomString() );
		masterNode.setMaster( true );
		MASSBase.addNode( masterNode );
		
		// verify node representations
		assertEquals( 1, MASSBase.getAllNodes().size() );
		assertEquals( 1, MASSBase.getHosts().size() );
		assertEquals( 0, MASSBase.getRemoteNodes().size() );
		assertEquals( 1, MASSBase.getSystemSize() );
		assertEquals( masterNode, MASSBase.getMasterNode() );
		
		// master node should have PID of zero
		assertEquals( 0, masterNode.getPid() );
		
		// add a remote node
		MNode remoteNode = new MNode();
		remoteNode.setHostName( randomString() );
		MASSBase.addNode( remoteNode );

		replayAll();

		// verify node representations
		assertEquals( 2, MASSBase.getAllNodes().size() );
		assertEquals( 2, MASSBase.getHosts().size() );
		assertEquals( 1, MASSBase.getRemoteNodes().size() );
		assertEquals( 2, MASSBase.getSystemSize() );
		assertEquals( masterNode, MASSBase.getMasterNode() );

		// remote node should have PID of one (auto incremented)
		assertEquals( 1, remoteNode.getPid() );

	}

	@Test
	public void getAgents() throws Exception {

		replayAll();

		// agents collection is managed by Agents (bad practice!)
		// only thing to do here is make sure it doesn't throw an exception
		assertNull( MASSBase.getAgents( 0 ) );
		
	}
	
	@Test
	public void getAgentsMap() throws Exception {

		replayAll();

		// agents collection is managed by Agents (bad practice!)
		// only thing to do here is make sure it isn't NULL on startup
		assertNotNull( MASSBase.getAgentsMap() );
		assertEquals( 0, MASSBase.getAgentsMap().size() );
		
	}

	@Test
	public void getAllNodesPreInit() throws Exception {
		
		replayAll();

		// should not have any nodes when first instantiated
		assertEquals( 0, MASSBase.getAllNodes().size() );
		assertEquals( 0, MASSBase.getHosts().size() );
		assertEquals( 0, MASSBase.getRemoteNodes().size() );
		assertEquals( 0, MASSBase.getSystemSize() );
		assertNull( MASSBase.getMasterNode() );
		
	}
	
	@Test
	public void getCores() throws Exception {

		replayAll();

		// should be non-zero, since it's calculated at runtime
		assertTrue( MASSBase.getCores() > 0 );
		
	}
	
	@Test
	public void getExchange() throws Exception {

		replayAll();

		// make sure one was init'd during MASSBase instantiation
		assertNotNull( MASSBase.getExchange() );
		
	}
	
	@Test
	public void getLogger() throws Exception {
		replayAll();
		assertNotNull( MASSBase.getLogger() );
	}
	
	@Test
	public void getPlaces() throws Exception {

		replayAll();

		// places map should not contain any entries yet
		Hashtable<Integer, PlacesBase> map =  MASSBase.getPlacesMap();
		assertNotNull( map );
		assertEquals( 0, map.size() );

		// force an entry into the map for testing
		map.put( 1, places );
		
		// should exist in the map, and should be able to retrieve by handle ID
		assertEquals( 1, map.size() );
		Places pl = MASSBase.getPlaces( 1 );
		assertEquals( places, pl );
		
	}

	@Test
	public void getSetAgentMigrationRequests() throws Exception {

		replayAll();

		// should not be a NULL collection to begin with
		assertNotNull( MASSBase.getMigrationRequests() );
		
		// should be able to set a new collection
		Vector< Vector < AgentMigrationRequest > > newReqestCollection = new Vector< Vector < AgentMigrationRequest > >();
		MASSBase.setMigrationRequests( newReqestCollection );
		
		assertEquals( newReqestCollection, MASSBase.getMigrationRequests() );
		
	}

	@Test
	public void getSetCommunicationPort() throws Exception {
		
		replayAll();

		// add a single master node
		MNode masterNode = new MNode();
		masterNode.setHostName( randomString() );
		masterNode.setMaster( true );
		masterNode.setPort( 80 );
		MASSBase.initMASSBase(masterNode);
		
		// should start with the port number set in the master MNode
		assertEquals( 80, MASSBase.getCommunicationPort() );

		// set a new port number
		int newPort = 42; 
		MASSBase.setCommunicationPort( newPort );
		
		// should have the new port number set
		assertEquals( newPort, MASSBase.getCommunicationPort() );
		
	}

	@Test
	public void setInvalidCommunicationPort() throws Exception {
		
		replayAll();

		// TODO - this should result in an IllegalArgumentException
		MASSBase.setCommunicationPort( 0 );
		
	}

	@Test
	public void getSetCurrentAgentsBase() throws Exception {

		replayAll();
		
		// should not be an existing association
		assertNull( MASSBase.getCurrentAgentsBase() );
		
		// set the current AgentsBase
		MASSBase.setCurrentAgentsBase( agentsBase );
		
		// should be there
		assertEquals( agentsBase, MASSBase.getCurrentAgentsBase() );
		
	}

	@Test
	public void getSetCurrentArgument() throws Exception {

		replayAll();

		// should not have a current argument
		assertNull( MASSBase.getCurrentArgument() );
		
		// set one, and check
		String testObj = randomString();
		MASSBase.setCurrentArgument( testObj );
		assertEquals( testObj, MASSBase.getCurrentArgument() );
		
	}

	@Test
	public void getSetCurrentFunctionId() throws Exception {

		replayAll();

		// should not have a current function ID
		assertEquals( 0,  MASSBase.getCurrentFunctionId() );
		
		// set one, and check
		int functionId = randomInt();
		MASSBase.setCurrentFunctionId( functionId );
		assertEquals( functionId, MASSBase.getCurrentFunctionId() );
		
	}

	@Test
	public void getSetCurrentMessageType() throws Exception {

		replayAll();

		// should not have a current message type
		assertNull( MASSBase.getCurrentMsgType() );
		
		// set one, and check
		MASSBase.setCurrentMsgType( Message.ACTION_TYPE.ACK );
		assertEquals( Message.ACTION_TYPE.ACK, MASSBase.getCurrentMsgType() );
		
	}

	@Test
	public void getSetCurrentPlacesBase() throws Exception {

		// MASSBase should be made ready for use before tests are run
		if ( MASSBase.getSystemSize() == 0 ) {
			MNode masterNode = new MNode();
			masterNode.setHostName( randomString() );
			masterNode.setMaster( true );
			MASSBase.addNode( masterNode );
			MASSBase.initMASSBase( masterNode );
		}

		replayAll();

		// force PlacesMap to something to prevent NPE
		PlacesBase pb = new PlacesBase( randomInt() , null, 0, null, new int[0] );
		MASSBase.getPlacesMap().put( pb.getHandle(), pb );
		
		// set the current PlacesBase
		MASSBase.setCurrentPlacesBase( pb );
		
		// should be there
		assertEquals( pb, MASSBase.getCurrentPlacesBase() );
		
	}

	@Test
	public void getSetCurrentReturns() throws Exception {

		replayAll();

		// should not have a current returns array
		assertNull( MASSBase.getCurrentReturns() );
		
		// set one, and check
		String[] testObj = new String[0];
		MASSBase.setCurrentReturns( testObj );
		assertNotNull( MASSBase.getCurrentReturns() );
		
	}

	@Test
	public void getSetDestinationPlaces() throws Exception {

		replayAll();

		// should not have a current destination
		assertNull( MASSBase.getDestinationPlaces() );
		
		// set one, and check
		PlacesBase pb = new PlacesBase( randomInt() , null, 0, null, new int[0] );
		MASSBase.setDestinationPlaces( pb );
		assertEquals( pb,  MASSBase.getDestinationPlaces() );
		
	}

	@Test
	public void getSetRemoteAgentMigrationRequests() throws Exception {

		replayAll();

		// should not be a NULL collection to begin with
		assertNotNull( MASSBase.getRemoteRequests() );
		
		// should be able to set a new collection
		Vector< Vector < RemoteExchangeRequest > > newReqestCollection = new Vector< Vector < RemoteExchangeRequest > >();
		MASSBase.setRemoteRequests( newReqestCollection );
		
		assertEquals( newReqestCollection, MASSBase.getRemoteRequests() );
		
	}
	
	@Test
	public void getSetSystemSize() throws Exception {
		
		replayAll();

		// forcing a value like what happens when remote MProcess is started
		int newSystemSize = randomInt(); 
		MASSBase.setSystemSize( newSystemSize );
		
		assertEquals( newSystemSize, MASSBase.getSystemSize() );
		
	}
	
	@Test
	public void getSetWorkingDirectory() throws Exception {

		replayAll();

		// remember the original setting to reset back after test completes
		String currentDirectory = MASSBase.getWorkingDirectory();
		assertNotNull( currentDirectory );
		
		// set to a new directory and test
		String newWorkingDirectory = "/";
		MASSBase.setWorkingDirectory( newWorkingDirectory );
		assertEquals( newWorkingDirectory, MASSBase.getWorkingDirectory() );
		
		// reset back and check
		MASSBase.setWorkingDirectory( currentDirectory );
		assertEquals( currentDirectory, MASSBase.getWorkingDirectory() );
		
	}
	
	@Test
	public void hasValidLogFilenameAutoDetectHostname() throws Exception {

		// init without specifying a hostname in node config
		MASSBase.initMASSBase( new MNode() );

		replayAll();

		// should generate a valid logging filename, with a valid host
		String loggingFilename = MASSBase.getLogFileName();
		assertNotNull( loggingFilename );
		assertFalse( loggingFilename.toLowerCase().contains( "null" ) );
		
	}

	@Test
	public void initMASSBaseLegacyMode() throws Exception {

		int port = randomInt();
		
		// "init" MASSBase to set it's own node
		MASSBase.initMASSBase( randomString(), 0, 0, port );

		replayAll();

		// verify, simple check of init
		assertEquals( port, MASSBase.getCommunicationPort() );
		
	}

	@Test
	public void initMASSBaseNullMNode() throws Exception {

		replayAll();

		// TODO - doesn't throw an exception at this time, but probably should!
		MASSBase.initMASSBase( null );
		
	}

	@Test
	public void resetRequestCounter() throws Exception {
		
		// nothing should happen, no exceptions thrown - (NOOP)
		replayAll();
		MASSBase.resetRequestCounter();
		
	}


	@Test
	public void setHosts() throws Exception {

		Vector<String> testHosts = new Vector<>();
		testHosts.add( "host1" );
		testHosts.add( "host2" );
		
		// get reference to current exchange helper for replacement once test is done
		ExchangeHelper currentExchangeHelper = MASSBase.getExchange();
		
		// using mock object for ExchangeHelper
		MASSBase.setExchange( exchangeHelper );
		
		exchangeHelper.establishConnection( testHosts.size(), 0, testHosts, MASSBase.getCommunicationPort() );
		
		replayAll();

		MASSBase.setHosts( testHosts );
		
		// exchange requests should be set to the number of hosts
		assertEquals( testHosts.size(), MASSBase.getMigrationRequests().size() );
		assertEquals( testHosts.size(), MASSBase.getRemoteRequests().size() );
		
		// return MASS Base back to original state
		MASSBase.setExchange( currentExchangeHelper );
		
	}

	@Test
	public void showHosts() throws Exception {
		
		replayAll();
		
		// the only thing to check for is no exceptions thrown
		// when logging is in debug mode
		MASSBase.getLogger().setLogLevel(LogLevel.DEBUG);
		MASSBase.showHosts();

	}

	@Before
	public void setUp() {

		// reset test subject fields
		MASSBase.getAllNodes().clear();
		MASSBase.setCurrentArgument( null );
		MASSBase.setCurrentFunctionId( 0 );
		MASSBase.setCurrentMsgType( null );
		MASSBase.setCurrentAgentsBase( null );
		MASSBase.setCurrentAgentsBase( null );
		MASSBase.setCurrentReturns( null );
		MASSBase.setDestinationPlaces( null );
		MASSBase.getPlacesMap().clear();
		MASSBase.getRemoteNodes().clear();
		MASSBase.getHosts().clear();
		
		// reset state
		MASSBase.getLogger().setLogLevel(LogLevel.OFF);


	}

	@Test
	public void initializeThreads() throws Exception {
		
		replayAll();

		// if not initialized already, test init here
		if ( !MASSBase.isInitialized() ) {
		
			// try init'ing
			assertTrue( MASSBase.initializeThreads( 3 ) );
			
			// attempt to init again should fail
			assertFalse( MASSBase.initializeThreads( 3 ) );
			
			// MASS should think it is init'd
			assertTrue( MASSBase.isInitialized() );
		
		}
		
	}

	@AfterClass
	public static void afterAll() {
		
		// clean up MASSBase
		resetMASSBase();
		
	}

}