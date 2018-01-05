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

import org.junit.Test;

/**
 * Perform a series of unit tests against the AgentSpawnRequest class to verify proper
 * and consistent behavior of the class / methods
 */
public class AgentSpawnRequestTest extends AbstractTest {

	private static final int MAX_AGENTS = 10;
	
	@Test
	public void getSetSerializedAgent() throws Exception {
		
		byte[] serializedAgent = new byte[ 0 ];
		
		AgentSpawnRequest request = new AgentSpawnRequest();
		request.setSerializedAgent( serializedAgent );

		assertEquals( serializedAgent, request.getSerializedAgent() );
		
	}
	
	@Test
	public void constructorInvalidMaxActiveSize() throws Exception {
		
		// should not result in an Exception - will default to reasonable settings
		@SuppressWarnings("unused")
		AgentSpawnRequestManager manager = new AgentSpawnRequestManager( 0 );
		
	}
	
	@Test
	public void defaultConstructor() throws Exception {
		
		// should not result in an Exception - will default to reasonable settings
		@SuppressWarnings("unused")
		AgentSpawnRequestManager manager = new AgentSpawnRequestManager( );
		
	}

	@Test
	public void shouldAgentRunInTheSystemEmpty() throws Exception {
		
		// make the agent spawn request manager think there is room for this one to run
		AgentSpawnRequestManager manager = new AgentSpawnRequestManager( MAX_AGENTS );
		assertTrue( manager.shouldAgentRunInTheSystem( new SimpleTestAgent( new String() ), 0 ) );
		
	}
	
	@Test
	public void shouldAgentRunInTheSystemFull() throws Exception {
		
		// make the agent spawn request manager think there is no room for this one to run
		AgentSpawnRequestManager manager = new AgentSpawnRequestManager( MAX_AGENTS );
		assertFalse( manager.shouldAgentRunInTheSystem( new SimpleTestAgent( new String() ), MAX_AGENTS ) );
		
	}
	
	@Test
	public void getNextAgentSpawnRequestNoSerializedAgents() throws Exception {
		
		AgentSpawnRequestManager manager = new AgentSpawnRequestManager( MAX_AGENTS );

		// should not be any agents in the queue for spawning
		assertNull( manager.getNextAgentSpawnRequest() );
		
	}

	@Test
	public void getSetNextAvailableAgentId() throws Exception {
		
		AgentSpawnRequestManager manager = new AgentSpawnRequestManager( MAX_AGENTS );

		// at first, should not be any agents in the queue for spawning
		assertEquals( -1, manager.getNextAvailableAgentId().intValue() );
		
		// add an agent ID, check for return
		manager.addAvailableAgentId( new Integer( 42 ) );
		assertEquals( 42, manager.getNextAvailableAgentId().intValue() );
		
	}

	@Test
	public void getNextAgentSpawnRequest() throws Exception {
		
		SimpleTestAgent agent = new SimpleTestAgent( new String() );
		agent.setAgentId( randomInt() );
		
		// make the agent spawn request manager think there is no room for this one to run
		AgentSpawnRequestManager manager = new AgentSpawnRequestManager( MAX_AGENTS );
		manager.shouldAgentRunInTheSystem( agent, MAX_AGENTS );
		
		// the agent should be returned when requested to get the next available
		Agent nextAgent = manager.getNextAgentSpawnRequest();
		
		// is it the same one?
		assertEquals( agent.getAgentId(), nextAgent.getAgentId() );
		
	}
	
}