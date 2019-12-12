/*

 	MASS Java Software License
	© 2012-2019 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2019 University of Washington. MASS was developed by Computing and Software Systems at University of 
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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.collections4.map.MultiKeyMap;

import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.annotations.AnnotationProcessor;


/**
 * 
 * SimpleEventDispatcher is a simple implementation of an EventDispatcher
 *
 */
public class SimpleEventDispatcher implements EventDispatcher {

	public static final int DEFAULT_MAX_ASYNC_THREADS = Runtime.getRuntime().availableProcessors();
	private static final int SHUTDOWN_TIMEOUT_MS = 2000; 
	
	ExecutorService executorService = Executors.newFixedThreadPool( DEFAULT_MAX_ASYNC_THREADS );
	
	// collection of queues, one for each event annotation 
	private Map<Class<? extends Annotation>, Queue<QueueMethod>> eventQueues = new HashMap<>();
	
	// Cache of Class, Event, and Methods
	private MultiKeyMap<Class<?>, Method> methodCache = new MultiKeyMap<>();

	/**
     * Initializes singleton.
     *
     * {@link SingletonHolder} is loaded on the first execution of {@link Singleton#getInstance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     */
    private static class SingletonHolder {
    	private static final SimpleEventDispatcher INSTANCE = new SimpleEventDispatcher();
    }
    
    /**
     * Return this instance of this dispatcher, which is effectively a Singleton
     * @return The single instance of this EventDispatcher implementation
     */
    public static SimpleEventDispatcher getInstance() {
    	return SingletonHolder.INSTANCE;
    }

	private Method getEventMethod( Class< ? extends Annotation > eventAnnotation, Class< ? > clazz ) {

		Objects.requireNonNull( eventAnnotation, "Must provide an event annotation!" );
		Objects.requireNonNull( clazz, "Must provide a source class!" );
		
		// check cache for the method
		Method m = methodCache.get( clazz, eventAnnotation );
		if ( m != null ) return m;
		
		// get the annotated method (if present)
		m = AnnotationProcessor.getAnnotatedMethod(eventAnnotation, null, clazz);
		
		// method not found, use NOOP method instead
		if ( m == null ) {

			try {
				m = EventDispatcher.class.getMethod( "noOp" );
			} catch ( NoSuchMethodException e ) {
				return null;
			} catch ( SecurityException e ) {
				return null;
			}

		}
		
		// cache the method for quicker retrieval later
		methodCache.put( clazz, eventAnnotation, m );

		return m;

	}

	@Override
	public void invokeImmediate( Class<? extends Annotation> eventAnnotation, Object object, Object... arguments ) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

		Objects.requireNonNull( eventAnnotation, "Must provide an event annotation!" );
		Objects.requireNonNull( object, "Must provide a target Object!" );

		// get the method associated with the event
		final Method method = getEventMethod( eventAnnotation, object.getClass() );
		
		// object does not use the specified annotation?
		if ( method == null ) return;

		// invoke the method immediately
		method.invoke( object, arguments );
		
	}

	@Override
	public void invokeAsync( Class< ? extends Annotation > eventAnnotation, Object object, Object... arguments ) throws IllegalArgumentException {

		Objects.requireNonNull( eventAnnotation, "Must provide an event annotation!" );
		Objects.requireNonNull( object, "Must provide a target Object!" );

		// get the Method specified by the annotation
		Method method = getEventMethod( eventAnnotation, object.getClass() );
		
		// object does not use the specified annotation?
		if ( method == null ) return;

		// ExecutorService will execute this method when possible
		CompletableFuture.runAsync( () -> { 
			
			try {
			
				method.invoke( object, arguments );
		
			} 
		
			catch ( IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
				MASS.getLogger().error( "Exception generated when executing async event!", e );
			} 
		
		}, executorService );
		
	}

	@Override
	public void queueAsync( Class< ? extends Annotation > eventAnnotation, Object object, Object ... arguments ) {

		Objects.requireNonNull( eventAnnotation, "Must provide an event annotation!" );
		Objects.requireNonNull( object, "Must provide a target Object!" );

		// get the Method specified by the annotation
		Method method = getEventMethod( eventAnnotation, object.getClass() );

		// object does not use the specified annotation?
		if ( method == null ) return;
		
		// get the queue associated with this event, or create a new one
		Queue< QueueMethod > eventQueue = eventQueues.get( eventAnnotation );
		
		// need to create a new queue for this event?
		if ( eventQueue == null) {
			eventQueue = new ConcurrentLinkedQueue< QueueMethod >();
			eventQueues.put( eventAnnotation, eventQueue );
		}
		
		// build a QueueMethod and add to the queue
		eventQueue.add( new QueueMethod( method, object, arguments ) );
		
	}
	
	private class QueueMethod {
		
		private Method methodToInvoke;
		private Object targetObject;
		private Object[] arguments;
		
		public QueueMethod( Method methodToInvoke, Object targetObject, Object ... arguments ) {
			
			this.methodToInvoke = methodToInvoke;
			this.targetObject = targetObject;
			this.arguments = arguments;
			
		}

		public Method getMethod() {
			return methodToInvoke;
		}
		
		public Object getObject() {
			return targetObject;
		}
		
		public Object[] getArguments() {
			return arguments;
		}
		
	}

	@Override
	public void invokeQueuedAsync( Class< ? extends Annotation > eventAnnotation ) {
		
		// get the queue associated with this event
		Queue< QueueMethod > eventQueue = eventQueues.get( eventAnnotation );

		// is there a queue for this event?
		if ( eventQueue == null ) return;
		
		// for each Method in the queue, attempt to invoke asynchronously
		List< Future< ? > > futures = new ArrayList<>( );
		Stream.generate( eventQueue::poll ).takeWhile( Objects::nonNull ).forEach( m -> { futures.add( executorService.submit( () -> { 
			
				try {
				
					m.getMethod().invoke( m.getObject(), m.getArguments() );
			
				} 
			
				catch ( IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
				
					// TODO Auto-generated catch block
					e.printStackTrace();
			
				} 
		
			}
                              
				) );
		
			}
		
		);
		
		// check for completion of each task
		for( Future<  ? > f: futures) { try {
			f.get();
		} catch ( InterruptedException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( ExecutionException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} }
		
	}

	@Override
	public void shutdown() {
		
		// stop the executor normally
		executorService.shutdown();
		
		try {
		    
			// await a normal shutdown
			if ( !executorService.awaitTermination( SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS ) ) {
		        
				// timeout - shutdown NOW!
				executorService.shutdownNow();
		    
			} 
		
		} catch ( InterruptedException e ) {
		   
			// there was a problem encountered during shutdown, terminate NOW
			executorService.shutdownNow();
		
		}
		
	}

}