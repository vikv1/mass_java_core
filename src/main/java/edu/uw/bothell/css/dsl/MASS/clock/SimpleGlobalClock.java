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

import java.lang.reflect.Method;

import edu.uw.bothell.css.dsl.MASS.Agent;
import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.Place;
import edu.uw.bothell.css.dsl.MASS.annotations.AnnotationProcessor;
import edu.uw.bothell.css.dsl.MASS.annotations.Clocked;
import edu.uw.bothell.css.dsl.MASS.event.EventDispatcher;

public class SimpleGlobalClock implements GlobalLogicalClock {

	private long clockValue;	
	
	private EventDispatcher eventDispatcher = null;
    
    // the next most recent clock cycle that will trigger an event
	// this allows for "fast forwarding" the clock value - it is
	// possible to skip incrementing the clock when there are no
	// events that will be triggered by those values. When this
	// happens, simply advance the clock to the next event trigger
	// and continue from there
	private long nextClockTrigger;

	/**
     * Initializes singleton.
     *
     * {@link SingletonHolder} is loaded on the first execution of {@link Singleton#getInstance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     */
    private static class SingletonHolder {
    	private static final SimpleGlobalClock INSTANCE = new SimpleGlobalClock();
    }
	/**
     * Return this instance of this clock, which is effectively a Singleton
     * @return The single instance of this GlobalLogicalClock implementation
     */
    public static SimpleGlobalClock getInstance() {
    	return SingletonHolder.INSTANCE;
    }
	
	private void execClockedAgentMethods() {
		
		// these conditions should happen only in a test environment, but doesn't hurt to be defensive
		if ( eventDispatcher == null ) return;
		if ( MASSBase.getCurrentPlacesBase() == null || MASSBase.getCurrentPlacesBase().getPlaces() == null ) return;
		
		// reset next trigger value for recalculation
		nextClockTrigger = 0;
		
		// iterate through all Agents to see if there is a method to execute
		for ( Place place : MASSBase.getCurrentPlacesBase().getPlaces() ) {
			for ( Agent agent : place.getAgents() ) {
				
				// nothing to do if this Agent wants to sleep
				if ( agent.getInhibitUntil() > clockValue ) continue;
				
				// get "Clocked" annotated method for this Agent (if there is one)
				Method m = AnnotationProcessor.getAnnotatedMethod( Clocked.class, null, agent.getClass() );
				
				if ( m != null ) {
					
					boolean execMethod = false;
					
					// time to call this method?
					Clocked annotation = m.getAnnotation( Clocked.class );
					long[] execValues = annotation.onValuesOf();
					int[] execMultiples = annotation.onMultiplesOf();
					
					// no values - exec every time
					if ( execValues.length == 0 && execMultiples.length == 0 ) {
						
						// queue this method for execution
						execMethod = true;
						
						// this Agent will trigger again on the next cycle
						nextClockTrigger = clockValue + 1;
						
					}
					
					// check for "onMultipleOf" match
					if ( execMethod == false && execMultiples.length > 0 ) {
						
						// we should execute on multiples of the clock value
						for ( int multiple : execMultiples ) {
							
							// is the current clock value a desired multiple?
							if ( clockValue > 0 && ( clockValue % multiple == 0 ) ) execMethod = true;
							
						}
						
					}

					// check for "onValuesOf" match
					if ( execMethod == false && execValues.length > 0 ) {
						
						for ( long value : execValues ) {

							// execute method if a specified value was reached
							if ( value == clockValue ) execMethod = true;
							
						}
						
					}

					// queue execution of the method if it is time to do so
					if ( execMethod ) eventDispatcher.queueAsync( Clocked.class, agent );
					
					// determine when the next trigger will occur
					long nextCandidateTriggerValue = getNextClockTrigger( clockValue, execValues, execMultiples );
					if ( nextCandidateTriggerValue < nextClockTrigger || nextClockTrigger == 0 ) nextClockTrigger = nextCandidateTriggerValue;
					
				}
				
			}
			
		}
		
		// invoke all queued clock-driven methods
		eventDispatcher.invokeQueuedAsync( Clocked.class );
		
	}

	@Override
	public long getNextEventTrigger() {
		return nextClockTrigger;
	}

	@Override
	public long getValue() {
		return clockValue;
	}

	@Override
	public void increment() {

		// use existing setter to consolidate rollover logic
		setValue( getValue() + 1 );
		
	}	

	@Override
	public void init(EventDispatcher eventDispatcher) {
		this.eventDispatcher = eventDispatcher;
	}

	@Override
	public void reset() {
		
		clockValue = 0;
		nextClockTrigger = 0;
		
		// trigger method executions
		execClockedAgentMethods();
		
	}

	@Override
	public void setValue( long value ) {
		
		// handle negative values due to rollover
		if ( value < 0 ) value = 0;
		
		clockValue = value;
		
		// trigger method executions
		execClockedAgentMethods();
		
	}
	
	/**
	 * Predict the next clock value that will trigger execution of a clocked event
	 * @param currentClockValue The current value of the clock
	 * @param onValuesOf An array of discrete clock trigger values
	 * @param onMultiplesOf An Array of multiples of which trigger clocked events
	 * @return The next value of the clock that will trigger execution of a clocked method, or zero if there are no upcoming trigger values
	 */
	protected long getNextClockTrigger( long currentClockValue, long[] onValuesOf, int[] onMultiplesOf ) {

		// no values provided means next clock value is the trigger
		if ( onValuesOf.length == 0 && onMultiplesOf.length == 0 ) {
			return currentClockValue + 1;
		}

		// set next trigger to the largest value possible
		long nextTriggerValue = Long.MAX_VALUE;
		
		// check "onValuesOf"
		for ( long value : onValuesOf ) {
		
			// will this value occur before the next trigger?
			if ( value > clockValue && value < nextTriggerValue ) nextTriggerValue = value;

		}
		
		// check "onMultiplesOf"
		for ( int value : onMultiplesOf ) {
			
			// if the current clock value is before the multiple, then the multiple is the next trigger
			if ( currentClockValue < value && value < nextTriggerValue ) {
				nextTriggerValue = value;
				continue;
			}
			
			// if the current clock value is after the mutiple, will the next trigger be the mutiple?
			if ( currentClockValue > value ) {
				
				// when will be the next time this multiple triggers an event?
				long nextMultiplier = ( currentClockValue / value ) + 1;
				
				// will this be an earlier trigger?
				if ( ( nextMultiplier * value ) < nextTriggerValue ) nextTriggerValue = nextMultiplier * value;
				
			}
			
		}
		
		// is there a next trigger value? (zero means no upcoming triggers)
		if ( nextTriggerValue == Long.MAX_VALUE ) return 0;

		// return the calculated value
		return nextTriggerValue;
		
	}
	
}