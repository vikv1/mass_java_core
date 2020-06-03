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

import edu.uw.bothell.css.dsl.MASS.event.EventDispatcher;

/**
 * An implementation of the Global Logical Clock provides a mechanism for synchronizing or timing system-wide simulation events
 *
 */
public interface GlobalLogicalClock {

	public static final int INHIBIT = -1;
	public static final int RESUME = 0;
	
	/**
	 * Get the next value of the clock that will trigger event execution. This method allows for the "fast forward"
	 * feature - if there are no events that will be triggered for the next several clock cycles, might as well
	 * advance the clock to this next trigger value and continue from there.
	 * @return The next clock cycle value that will trigger execution of a method
	 */
	public long getNextEventTrigger();
	
	/**
	 * Get the current clock value
	 * @return The current clock value
	 */
	public long getValue();
	
	/**
	 * Increment the clock by one count
	 */
	public void increment();
	
	/**
	 * Initialize the clock and prepare it for operation
	 * @param eventDispatcher The EventDispatcher that the clock will use to invoke annotated methods
	 */
	public void init( EventDispatcher eventDispatcher );
	
	/**
	 * Reset the global clock to zero count
	 */
	public void reset();
	
	/**
	 * Set the global clock to a specific count. Once the new clock value has been set the clock will
	 * automatically invoke any methods that would be triggered on the new value
	 * @param value The new clock value
	 */
	public void setValue( long value );
	
}