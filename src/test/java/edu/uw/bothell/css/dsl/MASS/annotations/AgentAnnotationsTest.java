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

package edu.uw.bothell.css.dsl.MASS.annotations;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import edu.uw.bothell.css.dsl.MASS.AbstractTest;
import edu.uw.bothell.css.dsl.MASS.SimpleTestAgent;
import edu.uw.bothell.css.dsl.MASS.event.EventDispatcher;
import edu.uw.bothell.css.dsl.MASS.event.SimpleEventDispatcher;

/**
 * Tests annotations that may be used by an Agent (or subclass)
 */
public class AgentAnnotationsTest extends AbstractTest {

	// class under test
	private SimpleTestAgent agent = new SimpleTestAgent( String.class );
	
	// event dispatcher for checking annotations
	private EventDispatcher eventDispatcher = new SimpleEventDispatcher();

	
	/**
	 * Reset test Agent back to known state before using
	 */
	@Before
	public void resetTestAgent() {
		agent.resetEventCounters();
	}

	@Test
	public void invokeOnArrival() throws Exception {

		// arrival event invocation count should start at zero
		assertEquals( 0, agent.getArrivalEventCount() );
		
		// invoke the OnArrival method
		eventDispatcher.invokeImmediate( OnArrival.class, agent );
		
		// method should have executed
		assertEquals( 1, agent.getArrivalEventCount() );
		
	}

	@Test
	public void invokeOnCreation() throws Exception {

		// creation event invocation count should start at zero
		assertEquals( 0, agent.getCreationEventCount() );
		
		// invoke the OnCreation method
		eventDispatcher.invokeImmediate( OnCreation.class, agent );
		
		// method should have executed
		assertEquals( 1, agent.getCreationEventCount() );
		
	}

	@Test
	public void invokeOnDeparture() throws Exception {

		// departure event invocation count should start at zero
		assertEquals( 0, agent.getDepartureEventCount() );
		
		// invoke the OnDeparture method
		eventDispatcher.invokeImmediate( OnDeparture.class, agent );
		
		// method should have executed
		assertEquals( 1, agent.getDepartureEventCount() );
		
	}

}