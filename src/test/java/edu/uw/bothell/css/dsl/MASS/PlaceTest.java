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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Vector;

import org.junit.After;
import org.junit.Test;

/**
 * Perform a series of unit tests against the Place class to verify proper
 * and consistent behavior of the class / methods
 */
public class PlaceTest extends AbstractTest {

	// class under test
	Place place = new Place();
	
	@After
	public void onTearDown() {
		
		// reset Agents collection
		place.getAgents().clear();
		place.setIndex( new int[ 0 ] );
		place.setInMessages( null );
		place.setNeighbors( null );
		place.setOutMessage( null );
		place.setSize( new int[ 0 ] );
		place.setVisited( false );
		
	}
	
	@Test
	public void callMethod() throws Exception {
	
		// always returns null!
		assertNull( place.callMethod( 0, new String() ) );
		
	}
	
	@Test
	public void getNumAgents() throws Exception {
		
		place.getAgents().add( new Agent() );
		
		assertEquals( 1, place.getNumAgents() );
		
	}
	
	@Test
	public void getSetDebugData() throws Exception {
		
		// intended to be overridden for debugging
		assertNull( place.getDebugData() );
		
		// no exception should be thrown
		place.setDebugData( 42 );
		place.setDebugData( new Object() );
		
	}
	
	@Test
	public void getSetIndex() throws Exception {

		int[] index = new int[]{ 42, 88, 89 };

		place.setIndex( index );
		
		assertEquals( index.length, place.getIndex().length );
		assertEquals( index[ 0 ], place.getIndex()[ 0 ] );
		assertEquals( index[ 1 ], place.getIndex()[ 1 ] );
		assertEquals( index[ 2 ], place.getIndex()[ 2 ] );
		
	}

	@Test
	public void getSetInMessages() throws Exception {

		Object[] messages = new Object[]{ new Message() };

		place.setInMessages( messages );
		
		assertEquals( messages.length, place.getInMessages().length );
		
	}

	@Test
	public void getSetNeighbors() throws Exception {

		Vector<int[]> neighbors = new Vector<>();
		neighbors.add( new int[]{ 42, 88, 89 } );

		place.setNeighbors( neighbors );
		
		assertEquals( 1, place.getNeighbours().size() );
		
	}

	@Test
	public void getSetOutMessages() throws Exception {

		Object message = new Object();

		place.setOutMessage( message );
		
		assertEquals( message, place.getOutMessage() );
		
	}

	@Test
	public void getSetSize() throws Exception {

		int[] size = new int[]{ 42, 88, 89 };

		place.setSize( size );
		
		assertEquals( size.length, place.getSize().length );
		assertEquals( size[ 0 ], place.getSize()[ 0 ] );
		assertEquals( size[ 1 ], place.getSize()[ 1 ] );
		assertEquals( size[ 2 ], place.getSize()[ 2 ] );
		
	}

	@Test
	public void getSetVisited() throws Exception {

		place.setVisited( true );
		
		assertTrue( place.getVisited() );
		
	}
	
}
