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

import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Test;

public class MASSBaseTest extends AbstractTest {

	@TestSubject
	private MASSBase massBase = new MASSBase();
	
	@After
	@SuppressWarnings("static-access")
	public void tearDown() {

		// perform normal cleanup activities
		super.tearDown();
		
		// reset test subject fields
		massBase.setCurrentArgument( null );
		massBase.setCurrentFunctionId( 0 );
		massBase.setCurrentMsgType( null );
		massBase.setCurrentAgentsBase( null );
		massBase.setCurrentAgentsBase( null );
		massBase.setCurrentReturns( null );
		massBase.setDestinationPlaces( null );

	}

	@Test
	@SuppressWarnings("static-access")
	public void hasValidLogFilenameAutoDetectHostname() throws Exception {

		// testing in isolation...
		MASSBase mb = new MASSBase();

		// init without specifying a hostname in node config
		mb.initMASSBase( new MNode() );
		
		// should generate a valid logging filename, with a valid host
		String loggingFilename = mb.getLogFileName();
		assertNotNull( loggingFilename );
		assertFalse( loggingFilename.toLowerCase().contains( "null" ) );
		
	}
	
	@Test
	@SuppressWarnings("static-access")
	public void getAllNodesPreInit() throws Exception {
		
		// testing in isolation...
		MASSBase mb = new MASSBase();
		
		// should not have any nodes when first instantiated
		assertEquals( 0, mb.getAllNodes().size() );
		assertEquals( 0, mb.getHosts().size() );
		assertEquals( 0, mb.getRemoteNodes().size() );
		assertEquals( 0, mb.getSystemSize() );
		assertNull( mb.getMasterNode() );
		
	}

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
	public void getLogger() throws Exception {
		assertNotNull( massBase.getLogger() );
		
	}
	
	@Test
	@SuppressWarnings("static-access")
	public void getAgents() throws Exception {
		
		// agents collection is managed by Agents (bad practice!)
		// only thing to do here is make sure it doesn't throw an exception
		assertNull( massBase.getAgents( 0 ) );
		
	}
	
	@Test
	@SuppressWarnings("static-access")
	public void getAgentsMap() throws Exception {
		
		// agents collection is managed by Agents (bad practice!)
		// only thing to do here is make sure it isn't NULL on startup
		assertNotNull( massBase.getAgentsMap() );
		assertEquals( 0, massBase.getAgentsMap().size() );
		
	}
	
	@Test
	@SuppressWarnings("static-access")
	public void getCores() throws Exception {
		
		// should be non-zero, since it's calculated at runtime
		assertTrue( massBase.getCores() > 0 );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void getSetCurrentAgentsBase() throws Exception {

		// testing in isolation...
		MASSBase mb = new MASSBase();
		
		// add a single master node
		MNode masterNode = new MNode();
		masterNode.setHostName( randomString() );
		masterNode.setMaster( true );
		mb.addNode( masterNode );
		
		// "init" MASSBase to set it's own node
		mb.initMASSBase( masterNode );

		// force PlacesMap to something to prevent NPE
		PlacesBase pb = new PlacesBase( 1 , null, 0, null, new int[0] );
		mb.getPlacesMap().put( 1, pb );
		
		// nonsensical AgentsBase just for testing
		// classloader throws an exception during instantiation, but who cares - we're not testing AgentsBase here 
		AgentsBase ab = new AgentsBase( 1, Message.class.getName(), null, 1, 1 );
		
		// should not be an existing association
		assertNull( mb.getCurrentAgentsBase() );
		
		// set the current AgentsBase
		mb.setCurrentAgentsBase( ab );
		
		// should be there
		assertEquals( ab, mb.getCurrentAgentsBase() );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void getSetCurrentArgument() throws Exception {
		
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

		// should not be an existing association
		assertNull( mb.getCurrentPlacesBase() );

		// force PlacesMap to something to prevent NPE
		PlacesBase pb = new PlacesBase( 1 , null, 0, null, new int[0] );
		mb.getPlacesMap().put( 1, pb );
		
		// set the current PlacesBase
		mb.setCurrentPlacesBase( pb );
		
		// should be there
		assertEquals( pb, mb.getCurrentPlacesBase() );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void getSetCurrentReturns() throws Exception {
		
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
		
		// should not have a current destination
		assertNull( massBase.getDestinationPlaces() );
		
		// set one, and check
		PlacesBase pb = new PlacesBase( 1 , null, 0, null, new int[0] );
		massBase.setDestinationPlaces( pb );
		assertEquals( pb,  massBase.getDestinationPlaces() );
		
	}

}
