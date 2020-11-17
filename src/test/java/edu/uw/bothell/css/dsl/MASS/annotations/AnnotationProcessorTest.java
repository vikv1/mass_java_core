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

package edu.uw.bothell.css.dsl.MASS.annotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.uw.bothell.css.dsl.MASS.AbstractTest;
import edu.uw.bothell.css.dsl.MASS.SimpleTestAgent;

public class AnnotationProcessorTest extends AbstractTest {

	// Agent for testing the AnnotationProcessor
	private SimpleTestAgent agent = new SimpleTestAgent( String.class );

	/**
	 * Reset test Agents back to known state before using
	 */
	@BeforeEach
	public void resetTestAgent() {
		
		agent.resetEventCounters();
		
	}

	@Test
	public void getAnnotatedNoArgumentMethod() throws Exception {

		// get the "OnArrival" annotated method, which has no arguments
		Method onArrivalMethod = AnnotationProcessor.getAnnotatedMethod( OnArrival.class, null, SimpleTestAgent.class );
		
		// a method should have been retrieved
		assertNotNull( onArrivalMethod );	
		
		// invoke the method and check to see if the correct one was provided
		onArrivalMethod.invoke( agent );
		assertEquals( 1, agent.getArrivalEventCount() );
		
	}
	
	@Test
	public void getAnnotatedSingleArgumentMethod() throws Exception {
		
		// get the "OnMessage" annotated method that accepts a String as an argument
		Method onMessageMethod = AnnotationProcessor.getAnnotatedMethod( OnMessage.class, String.class, SimpleTestAgent.class );
		
		// a method should have been retrieved
		assertNotNull( onMessageMethod );	
		
		// invoke the method and check to see if the correct one was provided
		onMessageMethod.invoke( agent, new String() );
		assertEquals( 1, agent.getReceivedMessageEventCount() );
		
	}

	@Test
	public void noSuchAnnotatedMethod() throws Exception {
		
		// attempt to get the "OnArrival" annotated method from a class that does not have this annotation
		Method onArrivalMethod = AnnotationProcessor.getAnnotatedMethod( OnArrival.class, null, String.class );
		
		// no method should have been retrieved
		assertNull( onArrivalMethod );	
		
	}
	
}
