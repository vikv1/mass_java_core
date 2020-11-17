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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;



/**
 * Perform a series of unit tests against the AgentsList class to verify proper
 * and consistent behavior of the class / methods
 */
public class AgentListTest extends AbstractTest {

	// class under test
	private AgentList agentList = new AgentList();
	
	@Test
	@SuppressWarnings("deprecation")
	public void checkInternal() {

		AgentList al = new AgentList( );
		Agent a = new SimpleTestAgent( new String() );
		al.add( a );
		
		// only a debugging method, but shouldn't throw an Exception
		al.checkInternal();
		
	}
	
	@Test
	public void size_unreducedNoAgents() {

		AgentList al = new AgentList( 1000 );

		// should not have any Agents
		assertEquals( 0, al.size_unreduced() );
		
	}
	
	@Test
	public void sizeNoAgents() {

		AgentList al = new AgentList( 1000 );

		// should not have any Agents
		assertEquals( 0, al.size() );
		
	}
	
	@Test
	public void add() {

		AgentList al = new AgentList( );

		// should start with zero size
		assertEquals( 0, al.size() );

		// add an Agent
		Agent a = new SimpleTestAgent( new String() );
		al.add( a );
		
		// should increment size
		assertEquals( 1, al.size() );
		
	}

	@Test
	public void clear() {

		AgentList al = new AgentList( );
		Agent a = new SimpleTestAgent( new String() );
		al.add( a );
		
		// should start with size of one
		assertEquals( 1, al.size() );
		
		al.clear();
		
		// should now be "empty"
		assertEquals( 0, al.size() );
		
	}

	@Test
	public void remove() {
		
		AgentList al = new AgentList( );

		// add three Agents for testing
		Agent a = new SimpleTestAgent( new String() );
		al.add( a );
		
		Agent b = new SimpleTestAgent( new String() );
		al.add( b );
		
		Agent c = new SimpleTestAgent( new String() );
		al.add( c );

		// should have three Agents
		assertEquals( 3, al.size() );
		
		// remove one of the Agents
		al.remove( b );
		
		// should not be able to find the Agent
		assertEquals( -1, al.indexOf( b ) );
		
		// should now only have two Agents
		assertEquals( 2, al.size() );
		
		// others should still remain
		assertTrue( al.indexOf( a ) >= 0 );
		assertTrue( al.indexOf( c ) >= 0 );

	}
	
	@Test
	public void removeInvalidAgent() {

		AgentList al = new AgentList( );

		// should not result in an Exception
		al.remove( new SimpleTestAgent( new String() ) );
		
	}
	
	@Test
	public void get() {

		AgentList al = new AgentList( );
		Agent a = new SimpleTestAgent( new String() );
		al.add( a );

		int index = al.indexOf( a );

		// should get the same Agent when retrieving by index number
		assertEquals( a, al.get( index ) );
		
	}

	@Test
	public void getInvalidIndex() {
		
		assertNull( agentList.get( 100000 ));
		
	}
	
	@Test
	public void reduce() {
		
		// can only check for Exceptions - functionality checked elsewhere
		agentList.reduce();
		
	}
	
	@Disabled		// see issues #119 and #120
	@Test
	public void addSpecifyingIndexPosition() {
		
		AgentList al = new AgentList( );

		// add three Agents for testing, different index positions
		Agent a = new SimpleTestAgent( new String() );
		al.add( a, 0 );
		
		Agent b = new SimpleTestAgent( new String() );
		al.add( b, 1 );
		
		Agent c = new SimpleTestAgent( new String() );
		al.add( c, 2 );

		// should have three Agents
		assertEquals( 3, al.size() );

		// check to make sure indexes are still set correctly
		assertEquals( 0, al.indexOf( a ) );
		assertEquals( 1, al.indexOf( b ) );
		assertEquals( 2, al.indexOf( c ) );
		
	}
	
	@Test
	public void iteratorOperationCheck() {
		
		AgentList al = new AgentList( );

		Agent a = new SimpleTestAgent( new String() );
		al.add( a );
		
		Agent b = new SimpleTestAgent( new String() );
		al.add( b );
		
		Agent c = new SimpleTestAgent( new String() );
		al.add( c );
		
		// set iterator to initial zero position
		al.setIterator();
		
		// there should be a "next"
		assertTrue( al.hasNext() );
		
		// get the Agent pointed to by the iterator
		assertEquals( a, al.next() );
		
		// check the next Agent
		assertTrue( al.hasNext() );
		assertEquals( b, al.next() );
		
		// check last Agent
		assertTrue( al.hasNext() );
		assertEquals( c, al.next() );

		// should not be a "next"
		assertFalse( al.hasNext() );

	}
	
}