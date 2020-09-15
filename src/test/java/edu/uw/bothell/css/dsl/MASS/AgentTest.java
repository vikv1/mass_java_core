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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;


/**
 * Perform a series of unit tests against the Agent class to verify proper
 * and consistent behavior of the class / methods
 */
public class AgentTest extends AbstractTest {

	// class under test
	private Agent agent = new Agent();
	
	@Test
	public void callMethod() {
		
		// this always returns NULL
		assertNull( agent.callMethod( randomInt(), new String( "Pontiac!" ) ) );
		
	}

	@Test
	public void getSetAgentId() {

		int newID = randomInt();
		int originalID = agent.getAgentId();
		
		agent.setAgentId( newID );
		assertEquals( newID, agent.getAgentId() );
		
		agent.setAgentId( originalID );
		
	}

	@Test
	public void getSetNewChildren() {

		int newNumber = randomInt();
		int originalNumber = agent.getNewChildren();
		
		agent.setNewChildren( newNumber );
		assertEquals( newNumber, agent.getNewChildren() );
		
		agent.setNewChildren( originalNumber );
		
	}

	@Test
	public void getSetDebugData() {
		
		Number number = Integer.valueOf( 1979 );
	
		// should not result in an Exception
		agent.setDebugData( number );

		// this is normally overridden by a derived class
		assertNull( agent.getDebugData() );
		
	}
	
//	@Test
//	public void getSetIndex() throws Exception {
//
//		int[] index = new int[]{ 1992 };
//
//		agent.setIndex( index );
//		
//		assertEquals( index.length, agent.getIndex().length );
//		assertEquals( index[ 0 ], agent.getIndex()[ 0 ] );
//		
//	}

	@Test
	public void getSetPlace() {
		
		Place place = new SimpleTestPlace( new String( "WS6" ) );
		
		// starts off NULL
		assertNull( agent.getPlace() );
		
		agent.setPlace( place );
		assertEquals( place, agent.getPlace() );
		
		agent.setPlace( null );
		
	}
	
	@Test
	public void getAliveAndKill() {
		
		// default is "alive"
		assertTrue( agent.isAlive() );
		
		// "kill" it
		agent.kill();
		assertFalse( agent.isAlive() );
		
		// agent remains "dead" at this point...
		
	}
	
	@Test
	public void spawn() {

		int numNewAgents = 1995;
		Object[] arguments = new Object[]{ new String( "TransAm" ) };
		
		agent.spawn( numNewAgents, arguments );
		
		assertEquals( numNewAgents, agent.getNewChildren() );
		assertEquals( arguments[ 0 ], agent.getArguments()[ 0 ] );
		
	}
	
	@Test
	public void spawnInvalidNumberAgents() {
		
		int originalNumChildren = agent.getNewChildren();
		
		// should not throw an exception, nor change existing values
		agent.spawn( -1, new Object[ 0 ] );
		
		assertEquals( originalNumChildren, agent.getNewChildren() );
		
	}
	
	@Test
	public void isMigrating() {

		// need a PlacesBase Place associated with the Agent for this test
		@SuppressWarnings("unused")
		PlacesBase placesBase = new PlacesBase(0, null, 0, 0, new int[]{2, 2, 2});
		
		Place place = new SimpleTestPlace( new String( "WS6" ) );
		agent.setPlace( place );
		
		// agent should not be migrating at first
		assertFalse( agent.isMigrating() );
		
		// migrate!
		agent.migrate( new int[]{ 1944, 1950, 1951 } );
		
		// agent should indicate that it will be migrating
		assertTrue( agent.isMigrating() );

		agent.setPlace( null );

	}
	
}