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

package edu.uw.bothell.css.dsl.MASS.clock;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.uw.bothell.css.dsl.MASS.AbstractTest;
import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.MNode;
import edu.uw.bothell.css.dsl.MASS.PlacesBase;
import edu.uw.bothell.css.dsl.MASS.SimpleTestAgent;
import edu.uw.bothell.css.dsl.MASS.SimpleTestPlace;
import edu.uw.bothell.css.dsl.MASS.event.SimpleEventDispatcher;

public class SimpleGlobalClockTest extends AbstractTest {

	// class under test
	private static SimpleGlobalClock clock = SimpleGlobalClock.getInstance();
	
	// classes/variables needed for test environment
	private static PlacesBase placesBase;
	private static final int PLACES_HANDLE = randomInt();
	private static SimpleTestAgent agent = new SimpleTestAgent( null );
	
	@BeforeAll
	public static void beforeAll() {

		// force MASSBase to contain a single node
		MNode masterNode = new MNode();
		masterNode.setHostName( randomString() );
		masterNode.setMaster( true );
		MASSBase.addNode( masterNode );
		MASSBase.initMASSBase( masterNode );

		// init a Places space to put test Agents into
		placesBase = new PlacesBase( PLACES_HANDLE, SimpleTestPlace.class.getName(), 1, null, new int[]{ 1, 1, 1 } );
		MASSBase.getPlacesMap().put( PLACES_HANDLE, placesBase );
		MASSBase.setCurrentPlacesBase( placesBase );
		placesBase.getPlaces()[ 0 ].getAgents().add( agent );

		// prepare the clock for use with this instance of MASS
		clock.init( SimpleEventDispatcher.getInstance() );

	}
	
	/**
	 * Reset clock back to known state before using
	 */
	@BeforeEach
	public void resetClock() {
		clock.reset();
	}

	@Test
	public void initialState() throws Exception {
		
		// clock should be at zero for starters
		assertEquals( 0, clock.getValue() );
		
		// next event trigger should be set to the first "onValuesOf" value in the test Agent
		assertEquals( 5, clock.getNextEventTrigger() );
		
		
	}
	
	@Test
	public void increment() throws Exception {

		// clock should be at zero for starters
		assertEquals( 0, clock.getValue() );
		
		// increment the clock
		clock.increment();
		
		// value should have incremented
		assertEquals( 1, clock.getValue() );
		
	}

	@Test
	public void rolloverNoException() throws Exception {

		// set the clock to the maximum value for a Java long
		clock.setValue( Long.MAX_VALUE );
		
		// the clock should be sitting at that value now
		assertEquals( Long.MAX_VALUE, clock.getValue() );
		
		// increment the clock to trigger rollover
		clock.increment();
		
		// value should have rolled back to zero
		assertEquals( 0, clock.getValue() );
		
	}
	
	@Test
	public void clockValueSetBeforeFirstTriggerRemainUnchanged() throws Exception {
		
		// next event trigger should be set to the first "onValuesOf" value in the test Agent
		assertEquals( 5, clock.getNextEventTrigger() );
		
		// increment the clock to value 4
		clock.setValue( 4 );
		
		// next event trigger should not change
		assertEquals( 5, clock.getNextEventTrigger() );
		
	}

	@Test
	public void clockValueSetAfterFirstTriggerSetToNext() throws Exception {
		
		// next event trigger should be set to the first "onValuesOf" value in the test Agent
		assertEquals( 5, clock.getNextEventTrigger() );
		
		// increment the clock to value 6
		clock.setValue( 6 );
		
		// next event trigger should be set to the next "onValuesOf"
		assertEquals( 17, clock.getNextEventTrigger() );
		
	}

}
