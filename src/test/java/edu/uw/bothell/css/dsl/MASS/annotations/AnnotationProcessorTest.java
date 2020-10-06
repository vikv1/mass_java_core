package edu.uw.bothell.css.dsl.MASS.annotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
