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
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Perform a series of unit tests against the AgentSerializer class to verify proper
 * and consistent behavior of the class / methods
 */
public class AgentSerializerTest extends AbstractTest {

	// class under test
	private AgentSerializer serializer = AgentSerializer.getInstance();
	
	@Test
	public void defaultNumberOfAgents() throws Exception {
		
		// by default, should be set to some positive value
		assertTrue( serializer.getMaxNumberOfAgents() > 0 );
		
	}
	
	@Test
	@SuppressWarnings("rawtypes")
	public void getSetRegisteredClasses() throws Exception {
		
		Class[] originalRegistration = serializer.getRegisteredClasses();
		Class[] newRegistration = new Class[]{ String.class };
		
		serializer.setRegisteredClasses( newRegistration );
		
		assertEquals( String.class, serializer.getRegisteredClasses()[ 0 ] );
		
		serializer.setRegisteredClasses( originalRegistration );
		
	}
	
	@Test
	public void setInvalidNumberOfAgents() throws Exception {
		
		int originalNumAgents = serializer.getMaxNumberOfAgents();

		// zero or fewer max agents should not change original value
		
		serializer.setMaxNumberOfAgents( 0 );
		assertEquals( originalNumAgents, serializer.getMaxNumberOfAgents() );
		
		serializer.setMaxNumberOfAgents( -1 );
		assertEquals( originalNumAgents, serializer.getMaxNumberOfAgents() );
		
	}
	
	@Test
	public void getSetMaxNumberOfAgents() throws Exception {
		
		int originalNumAgents = serializer.getMaxNumberOfAgents();

		serializer.setMaxNumberOfAgents( originalNumAgents + 1 );
		assertEquals( originalNumAgents + 1, serializer.getMaxNumberOfAgents() );
		
		serializer.setMaxNumberOfAgents( originalNumAgents );
		assertEquals( originalNumAgents, serializer.getMaxNumberOfAgents() );
		
	}
	
	@Test
	public void serializeDeserializeAgent() throws Exception {

		// a basic Agent with a few populated fields for testing 
		Agent agent = new SimpleTestAgent( new String( randomString() ) );
		agent.setAgentId( randomInt() );
		
		// serialize the Agent
		byte[] serializedAgent = serializer.serializeAgent( agent );
		
		// deserialize the Agent
		Agent deserializedAgent = serializer.deserializeAgent( serializedAgent );
		
		// fields should match original settings
		assertEquals( agent.getAgentId(), deserializedAgent.getAgentId() );
		
	}
	
}
