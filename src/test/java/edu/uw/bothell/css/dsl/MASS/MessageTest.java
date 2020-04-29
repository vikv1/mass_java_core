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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Vector;

import edu.uw.bothell.css.dsl.test.IntegrationTest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Perform a series of unit tests against the Message class to verify proper
 * and consistent behavior of the class / methods
 */
@Category(IntegrationTest.class)
@Ignore
public class MessageTest extends AbstractTest {

	@Test
	public void testNoArgsConstructor() throws Exception {
		
		Message message = new Message();
		
		// check default field population - no exceptions should be thrown
		assertNull( message.getAction() );
		assertEquals( Message.VOID_HANDLE, message.getAgentPopulation() );
		assertNull( message.getArgument() );
		assertEquals( 0, message.getBoundaryWidth() );
		assertNull( message.getClassname() );
		assertEquals( Message.VOID_HANDLE, message.getDestHandle() );
		assertNull( message.getDestinations() );
		assertNull( message.getExchangeReqList() );
		assertEquals( 0, message.getFunctionId() );
		assertEquals( Message.VOID_HANDLE, message.getHandle() );
		assertNull( message.getHosts() );
		assertNull( message.getMigrationReqList() );
		assertNull( message.getSize() );
		assertFalse( message.isArgumentValid() );
		assertEquals( "UNDEFINED", message.getActionString() );
		
	}
	
	@Test
	public void testActionTypeConstructor() throws Exception {
		
		Message message = new Message( Message.ACTION_TYPE.FINISH );
		
		// check field population - no exceptions should be thrown
		assertEquals( Message.ACTION_TYPE.FINISH, message.getAction() );
		assertEquals( Message.VOID_HANDLE, message.getAgentPopulation() );
		assertNull( message.getArgument() );
		assertEquals( 0, message.getBoundaryWidth() );
		assertNull( message.getClassname() );
		assertEquals( Message.VOID_HANDLE, message.getDestHandle() );
		assertNull( message.getDestinations() );
		assertNull( message.getExchangeReqList() );
		assertEquals( 0, message.getFunctionId() );
		assertEquals( Message.VOID_HANDLE, message.getHandle() );
		assertNull( message.getHosts() );
		assertNull( message.getMigrationReqList() );
		assertNull( message.getSize() );
		assertFalse( message.isArgumentValid() );
		assertEquals( "FINISH", message.getActionString() );
		
	}
	
	@Test
	public void testAgentsInitAgentsCallAllConstructor() throws Exception {
		
		Message message = new Message( Message.ACTION_TYPE.AGENTS_INITIALIZE, 42 );
		
		// check field population - no exceptions should be thrown
		assertEquals( Message.ACTION_TYPE.AGENTS_INITIALIZE, message.getAction() );
		assertEquals( 42, message.getAgentPopulation() );
		assertNull( message.getArgument() );
		assertEquals( 0, message.getBoundaryWidth() );
		assertNull( message.getClassname() );
		assertEquals( Message.VOID_HANDLE, message.getDestHandle() );
		assertNull( message.getDestinations() );
		assertNull( message.getExchangeReqList() );
		assertEquals( 0, message.getFunctionId() );
		assertEquals( Message.VOID_HANDLE, message.getHandle() );
		assertNull( message.getHosts() );
		assertNull( message.getMigrationReqList() );
		assertNull( message.getSize() );
		assertFalse( message.isArgumentValid() );
		assertEquals( "UNDEFINED", message.getActionString() );
		
	}
	
	@Test
	public void testAgentsManageAllPlacesExchangeBoundaryConstructor() throws Exception {
		
		Message message = new Message( Message.ACTION_TYPE.AGENTS_MANAGE_ALL, 42, 89 );
		
		// check field population - no exceptions should be thrown
		assertEquals( Message.ACTION_TYPE.AGENTS_MANAGE_ALL, message.getAction() );
		assertEquals( Message.VOID_HANDLE, message.getAgentPopulation() );
		assertNull( message.getArgument() );
		assertEquals( 0, message.getBoundaryWidth() );
		assertNull( message.getClassname() );
		assertEquals( 42, message.getDestHandle() );
		assertNull( message.getDestinations() );
		assertNull( message.getExchangeReqList() );
		assertEquals( 0, message.getFunctionId() );
		assertEquals( 42, message.getHandle() );
		assertNull( message.getHosts() );
		assertNull( message.getMigrationReqList() );
		assertNull( message.getSize() );
		assertFalse( message.isArgumentValid() );
		assertEquals( "UNDEFINED", message.getActionString() );
		
	}

	@Test
	public void testHandlesAndFunctionIDConstructor() throws Exception {
		
		Message message = new Message( Message.ACTION_TYPE.PLACES_INITIALIZE, 42, 89, 71 );
		
		// check field population - no exceptions should be thrown
		assertEquals( Message.ACTION_TYPE.PLACES_INITIALIZE, message.getAction() );
		assertEquals( Message.VOID_HANDLE, message.getAgentPopulation() );
		assertNull( message.getArgument() );
		assertEquals( 0, message.getBoundaryWidth() );
		assertNull( message.getClassname() );
		assertEquals( 89, message.getDestHandle() );
		assertNull( message.getDestinations() );
		assertNull( message.getExchangeReqList() );
		assertEquals( 71, message.getFunctionId() );
		assertEquals( 42, message.getHandle() );
		assertNull( message.getHosts() );
		assertNull( message.getMigrationReqList() );
		assertNull( message.getSize() );
		assertFalse( message.isArgumentValid() );
		assertEquals( "UNDEFINED", message.getActionString() );
		
	}

	@Test
	public void testAgentInitializeConstructor() throws Exception {
		
		Object argument = new String();
		
		Message message = new Message( Message.ACTION_TYPE.AGENTS_INITIALIZE, 42, 89, 71, String.class.getName(), argument );
		
		// check field population - no exceptions should be thrown
		assertEquals( Message.ACTION_TYPE.AGENTS_INITIALIZE, message.getAction() );
		assertEquals( 42, message.getAgentPopulation() );
		assertEquals( argument, message.getArgument() );
		assertEquals( 0, message.getBoundaryWidth() );
		assertEquals( String.class.getName(), message.getClassname() );
		assertEquals( 71, message.getDestHandle() );
		assertNull( message.getDestinations() );
		assertNull( message.getExchangeReqList() );
		assertEquals( 0, message.getFunctionId() );
		assertEquals( 89, message.getHandle() );
		assertNull( message.getHosts() );
		assertNull( message.getMigrationReqList() );
		assertNull( message.getSize() );
		assertTrue( message.isArgumentValid() );
		assertEquals( "UNDEFINED", message.getActionString() );
		
	}

	@Test
	public void testPlacesExchangeAllConstructor() throws Exception {
		
		Vector<int[]> destinations = new Vector<>();
		
		Message message = new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_ALL, 42, 89, 71, destinations );
		
		// check field population - no exceptions should be thrown
		assertEquals( Message.ACTION_TYPE.PLACES_EXCHANGE_ALL, message.getAction() );
		assertEquals( Message.VOID_HANDLE, message.getAgentPopulation() );
		assertNull( message.getArgument() );
		assertEquals( 0, message.getBoundaryWidth() );
		assertNull( message.getClassname() );
		assertEquals( 89, message.getDestHandle() );
		assertEquals( destinations, message.getDestinations() );
		assertNull( message.getExchangeReqList() );
		assertEquals( 71, message.getFunctionId() );
		assertEquals( 42, message.getHandle() );
		assertNull( message.getHosts() );
		assertNull( message.getMigrationReqList() );
		assertNull( message.getSize() );
		assertFalse( message.isArgumentValid() );
		assertEquals( "UNDEFINED", message.getActionString() );
		
	}

	@Test
	public void testPlacesExchangeAllRemoteRequestConstructor() throws Exception {
		
		Vector<RemoteExchangeRequest> requests = new Vector<>();
		
		Message message = new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_ALL_REMOTE_REQUEST, 42, 89, 71, requests, 70 );
		
		// check field population - no exceptions should be thrown
		assertEquals( Message.ACTION_TYPE.PLACES_EXCHANGE_ALL_REMOTE_REQUEST, message.getAction() );
		assertEquals( Message.VOID_HANDLE, message.getAgentPopulation() );
		assertNull( message.getArgument() );
		assertEquals( 0, message.getBoundaryWidth() );
		assertNull( message.getClassname() );
		assertEquals( 89, message.getDestHandle() );
		assertNull( message.getDestinations() );
		assertEquals( requests, message.getExchangeReqList() );
		assertEquals( 71, message.getFunctionId() );
		assertEquals( 42, message.getHandle() );
		assertNull( message.getHosts() );
		assertNull( message.getMigrationReqList() );
		assertNull( message.getSize() );
		assertFalse( message.isArgumentValid() );
		assertEquals( "UNDEFINED", message.getActionString() );
		
	}

	@Test
	public void testCallAllConstructor() throws Exception {
		
		Object argument = new String();
		
		Message message = new Message( Message.ACTION_TYPE.PLACES_CALL_ALL_VOID_OBJECT, 42, 89, argument );
		
		// check field population - no exceptions should be thrown
		assertEquals( Message.ACTION_TYPE.PLACES_CALL_ALL_VOID_OBJECT, message.getAction() );
		assertEquals( Message.VOID_HANDLE, message.getAgentPopulation() );
		assertEquals( argument, message.getArgument() );
		assertEquals( 0, message.getBoundaryWidth() );
		assertNull( message.getClassname() );
		assertEquals( Message.VOID_HANDLE, message.getDestHandle() );
		assertNull( message.getDestinations() );
		assertNull( message.getExchangeReqList() );
		assertEquals( 89, message.getFunctionId() );
		assertEquals( 42, message.getHandle() );
		assertNull( message.getHosts() );
		assertNull( message.getMigrationReqList() );
		assertNull( message.getSize() );
		assertTrue( message.isArgumentValid() );
		assertEquals( "UNDEFINED", message.getActionString() );
		
	}

	@Test
	public void testAgentMigrationRemoteRequestConstructor() throws Exception {
		
		Vector<AgentMigrationRequest> requests = new Vector<>();
		
		Message message = new Message( Message.ACTION_TYPE.AGENTS_MIGRATION_REMOTE_REQUEST, 42, 89, requests );
		
		// check field population - no exceptions should be thrown
		assertEquals( Message.ACTION_TYPE.AGENTS_MIGRATION_REMOTE_REQUEST, message.getAction() );
		assertEquals( Message.VOID_HANDLE, message.getAgentPopulation() );
		assertNull( message.getArgument() );
		assertEquals( 0, message.getBoundaryWidth() );
		assertNull( message.getClassname() );
		assertEquals( 89, message.getDestHandle() );
		assertNull( message.getDestinations() );
		assertNull( message.getExchangeReqList() );
		assertEquals( 0, message.getFunctionId() );
		assertEquals( 42, message.getHandle() );
		assertNull( message.getHosts() );
		assertEquals( requests, message.getMigrationReqList() );
		assertNull( message.getSize() );
		assertFalse( message.isArgumentValid() );
		assertEquals( "UNDEFINED", message.getActionString() );
		
	}

	@Test
	public void testPlacesInitializeConstructor() throws Exception {
		
		int size[] = new int[ 0 ];
		Object argument = new String();
		Vector<String> hosts = new Vector<>();
		
		Message message = new Message( Message.ACTION_TYPE.PLACES_INITIALIZE, size, 42, String.class.getName(), argument, 89, hosts );
		
		// check field population - no exceptions should be thrown
		assertEquals( Message.ACTION_TYPE.PLACES_INITIALIZE, message.getAction() );
		assertEquals( Message.VOID_HANDLE, message.getAgentPopulation() );
		assertEquals( argument, message.getArgument() );
		assertEquals( 89, message.getBoundaryWidth() );
		assertEquals( String.class.getName(), message.getClassname() );
		assertEquals( Message.VOID_HANDLE, message.getDestHandle() );
		assertNull( message.getDestinations() );
		assertNull( message.getExchangeReqList() );
		assertEquals( 0, message.getFunctionId() );
		assertEquals( 42, message.getHandle() );
		assertEquals( hosts, message.getHosts() );
		assertNull( message.getMigrationReqList() );
		assertEquals( size, message.getSize() );
		assertTrue( message.isArgumentValid() );
		assertEquals( "UNDEFINED", message.getActionString() );
		
	}

	@Test
	public void testPlacesExchangeConstructor() throws Exception {
		
		Object returnValue = new String();
		
		Message message = new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT, returnValue );
		
		// check field population - no exceptions should be thrown
		assertEquals( Message.ACTION_TYPE.PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT, message.getAction() );
		assertEquals( Message.VOID_HANDLE, message.getAgentPopulation() );
		assertEquals( returnValue, message.getArgument() );
		assertEquals( 0, message.getBoundaryWidth() );
		assertNull( message.getClassname() );
		assertEquals( Message.VOID_HANDLE, message.getDestHandle() );
		assertNull( message.getDestinations() );
		assertNull( message.getExchangeReqList() );
		assertEquals( 0, message.getFunctionId() );
		assertEquals( Message.VOID_HANDLE, message.getHandle() );
		assertNull( message.getHosts() );
		assertNull( message.getMigrationReqList() );
		assertNull( message.getSize() );
		assertTrue( message.isArgumentValid() );
		assertEquals( "UNDEFINED", message.getActionString() );
		
	}

	@Test
	public void testAgentsCallAllConstructor() throws Exception {
		
		Object argument = new String();
		
		Message message = new Message( Message.ACTION_TYPE.AGENTS_CALL_ALL_RETURN_OBJECT, argument, 89 );
		
		// check field population - no exceptions should be thrown
		assertEquals( Message.ACTION_TYPE.AGENTS_CALL_ALL_RETURN_OBJECT, message.getAction() );
		assertEquals( 89, message.getAgentPopulation() );
		assertEquals( argument, message.getArgument() );
		assertEquals( 0, message.getBoundaryWidth() );
		assertNull( message.getClassname() );
		assertEquals( Message.VOID_HANDLE, message.getDestHandle() );
		assertNull( message.getDestinations() );
		assertNull( message.getExchangeReqList() );
		assertEquals( 0, message.getFunctionId() );
		assertEquals( Message.VOID_HANDLE, message.getHandle() );
		assertNull( message.getHosts() );
		assertNull( message.getMigrationReqList() );
		assertNull( message.getSize() );
		assertTrue( message.isArgumentValid() );
		assertEquals( "UNDEFINED", message.getActionString() );
		
	}

}