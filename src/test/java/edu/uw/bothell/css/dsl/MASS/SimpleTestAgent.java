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

import edu.uw.bothell.css.dsl.MASS.annotations.Clocked;
import edu.uw.bothell.css.dsl.MASS.annotations.OnArrival;
import edu.uw.bothell.css.dsl.MASS.annotations.OnCreation;
import edu.uw.bothell.css.dsl.MASS.annotations.OnDeparture;
import edu.uw.bothell.css.dsl.MASS.annotations.OnMessage;

@SuppressWarnings("serial")
public class SimpleTestAgent extends Agent {

	// counters for checking event method invocation
	private int arrivalEventCount = 0;
	private int departureEventCount = 0;
	private int creationEventCount = 0;
	private int messageReceiveCount = 0;
	private int onValuesOfClockedCount = 0;
	
	public SimpleTestAgent( Object obj ) {
		
	}
	
	/**
	 * Get the number of times the OnArrival-annotated method was called
	 * @return The number of OnArrival events since last counter reset
	 */
	public int getArrivalEventCount() {
		return arrivalEventCount;
	}

	/**
	 * Get the number of times the OnCreation-annotated method was called
	 * @return The number of OnCreation events since last counter reset
	 */
	public int getCreationEventCount() {
		return creationEventCount;
	}

	/**
	 * Get the number of times the OnDeparture-annotated method was called
	 * @return The number of OnDeparture events since last counter reset
	 */
	public int getDepartureEventCount() {
		return departureEventCount;
	}

	/**
	 * Get the number of times an OnMessage-annotated method was called
	 * @return The number of OnMessage events since last counter reset
	 */
	public int getReceivedMessageEventCount() {
		return messageReceiveCount;
	}

	@OnArrival
	public void onArrivalEvent() {
		arrivalEventCount ++;
	}

	@OnCreation
	public void onCreationEvent() {
		creationEventCount ++;
	}

	@OnDeparture
	public void onDepartureEvent() {
		departureEventCount ++;
	}

	@OnMessage
	public void onMessageStringArgument( String messageBody ) {
		messageReceiveCount ++;
	}
	
	@Clocked( onValuesOf = { 5, 17 } )
	public void onValuesOfClockedMethod() {
		onValuesOfClockedCount ++;
	}
	
	/**
	 * Reset all event counters to zero
	 */
	public void resetEventCounters() {

		arrivalEventCount = 0;
		departureEventCount = 0;
		creationEventCount = 0;
		messageReceiveCount = 0;
		onValuesOfClockedCount = 0;
		
	}
	
}