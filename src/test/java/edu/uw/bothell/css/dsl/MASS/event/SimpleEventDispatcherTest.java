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

package edu.uw.bothell.css.dsl.MASS.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.uw.bothell.css.dsl.MASS.AbstractTest;
import edu.uw.bothell.css.dsl.MASS.SimpleTestAgent;
import edu.uw.bothell.css.dsl.MASS.annotations.OnArrival;
import edu.uw.bothell.css.dsl.MASS.annotations.OnDeparture;

/**
 * Tests the SimpleEventDispatcher
 */
public class SimpleEventDispatcherTest extends AbstractTest {

	// class under test
	private EventDispatcher eventDispatcher = new SimpleEventDispatcher();
	
	// Agents for testing the event dispatcher
	private SimpleTestAgent agent1 = new SimpleTestAgent( String.class );
	private SimpleTestAgent agent2 = new SimpleTestAgent( String.class );
	private SimpleTestAgent agent3 = new SimpleTestAgent( String.class );

	/**
	 * Reset test Agents back to known state before using
	 */
	@BeforeEach
	public void resetTestAgent() {
		
		agent1.resetEventCounters();
		agent2.resetEventCounters();
		agent3.resetEventCounters();
		
	}

	@Test
	public void invokeImmediate() throws Exception {
		
		// OnArrival execution count should start at zero
		assertEquals( 0, agent1.getArrivalEventCount() );
		
		// invoke the OnArrival method
		eventDispatcher.invokeImmediate( OnArrival.class, agent1 );
		
		// method should have executed
		assertEquals( 1, agent1.getArrivalEventCount() );

	}

	@Test
	public void invokeAsync() throws Exception {
		
		// OnArrival execution count should start at zero
		assertEquals( 0, agent1.getArrivalEventCount() );
		
		// invoke the OnArrival method asynchronously
		eventDispatcher.invokeAsync( OnArrival.class, agent2 );
		
		// wait a few seconds, just to make sure time is given for execution by a different thread
		Thread.sleep( 2000 );
		
		// method should have executed
		assertEquals( 1, agent2.getArrivalEventCount() );

	}

	@Test
	public void invokeQueuedAsyncSingleQueue() throws Exception {
		
		// queue up several events in different orders
		eventDispatcher.queueAsync( OnArrival.class, agent1 );
		eventDispatcher.queueAsync( OnArrival.class, agent2 );
		eventDispatcher.queueAsync( OnArrival.class, agent3 );
		eventDispatcher.queueAsync( OnArrival.class, agent2 );
		eventDispatcher.queueAsync( OnArrival.class, agent1 );
		eventDispatcher.queueAsync( OnArrival.class, agent3 );
		
		// trigger execution of all queued events for a specified event annotation
		eventDispatcher.invokeQueuedAsync( OnArrival.class );
		
		// wait a few seconds, just to make sure time is given for execution by different threads
		Thread.sleep( 2000 );
		
		// all methods should have executed
		assertEquals( 2, agent1.getArrivalEventCount() );
		assertEquals( 2, agent2.getArrivalEventCount() );
		assertEquals( 2, agent3.getArrivalEventCount() );
		
	}

	@Test
	public void invokeMultipleQueuedAsyncSingleQueue() throws Exception {
		
		// queue up a single event
		eventDispatcher.queueAsync( OnArrival.class, agent1 );
		
		// trigger execution of all queued events for a specified event annotation
		// do this multiple times to make sure queue is actually being flushed
		eventDispatcher.invokeQueuedAsync( OnArrival.class );
		eventDispatcher.invokeQueuedAsync( OnArrival.class );
		eventDispatcher.invokeQueuedAsync( OnArrival.class );
		
		// wait a few seconds, just to make sure time is given for execution by different threads
		Thread.sleep( 2000 );
		
		// only one method should have executed event though dispatcher execute called several times
		assertEquals( 1, agent1.getArrivalEventCount() );
		
	}

	@Test
	public void invokeAllQueuedAsyncMultipleQueues() throws Exception {
		
		// queue up several events in different orders
		eventDispatcher.queueAsync( OnArrival.class, agent1 );
		eventDispatcher.queueAsync( OnArrival.class, agent2 );
		eventDispatcher.queueAsync( OnArrival.class, agent3 );
		eventDispatcher.queueAsync( OnDeparture.class, agent3 );
		eventDispatcher.queueAsync( OnDeparture.class, agent2 );
		eventDispatcher.queueAsync( OnDeparture.class, agent1 );
		eventDispatcher.queueAsync( OnArrival.class, agent2 );
		eventDispatcher.queueAsync( OnArrival.class, agent1 );
		eventDispatcher.queueAsync( OnArrival.class, agent3 );
		eventDispatcher.queueAsync( OnDeparture.class, agent3 );
		eventDispatcher.queueAsync( OnDeparture.class, agent1 );
		eventDispatcher.queueAsync( OnDeparture.class, agent2 );
		eventDispatcher.queueAsync( OnArrival.class, agent2 );
		eventDispatcher.queueAsync( OnArrival.class, agent1 );
		eventDispatcher.queueAsync( OnArrival.class, agent3 );
		
		// trigger execution of all queued events
		eventDispatcher.invokeQueuedAsync( OnArrival.class );
		eventDispatcher.invokeQueuedAsync( OnDeparture.class );
		
		// wait a few seconds, just to make sure time is given for execution by different threads
		Thread.sleep( 2000 );
		
		// all methods should have executed
		assertEquals( 3, agent1.getArrivalEventCount() );
		assertEquals( 3, agent2.getArrivalEventCount() );
		assertEquals( 3, agent3.getArrivalEventCount() );
		assertEquals( 2, agent1.getDepartureEventCount() );
		assertEquals( 2, agent2.getDepartureEventCount() );
		assertEquals( 2, agent3.getDepartureEventCount() );
		
	}

}
