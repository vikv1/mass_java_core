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

/**
 * Event Dispatcher is responsible for executing events synchronously (immediate) or
 * asynchronously (can be queued) based on annotated methods in the target object/class
 */
public interface EventDispatcher {

	/**
	 * Immediately (synchronous) execute a method
	 * @param eventAnnotation The annotation associated with the event method to execute
	 * @param object The target object the method will be invoked against
	 * @param arguments Arguments to supply to the method being invoked
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public void invokeImmediate( Class<? extends Annotation> eventAnnotation, Object object, Object ... arguments ) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException;

	/**
	 * Asynchronously execute a method
	 * @param eventAnnotation The annotation associated with the event method to execute
	 * @param object The target object the method will be invoked against
	 * @param arguments Arguments to supply to the method being invoked
	 * @throws IllegalArgumentException
	 */
	public void invokeAsync( Class<? extends Annotation> eventAnnotation, Object object, Object ... arguments ) throws IllegalArgumentException;

	/**
	 * Invoke all queued asynchronous methods
	 * @param eventAnnotation
	 */
	public void invokeQueuedAsync( Class<? extends Annotation> eventAnnotation );

	/**
	 * Queue a method to be asynchronously executed in the future. Calling "invokeQueuedAsync" will trigger
	 * parallel execution of all methods in the queue.
	 * @param eventAnnotation The annotation associated with the event method to execute
	 * @param object The target object the method will be invoked against
	 * @param arguments Arguments to supply to the method being invoked
	 */
	public void queueAsync( Class<? extends Annotation> eventAnnotation, Object object, Object ... arguments );
	
	/**
	 * Signal the dispatcher to complete any outstanding tasks and perform an orderly shutdown
	 */
	public void shutdown();
	
	/**
	 * A NOOP method used as a placeholder for event queues
	 */
	public static void noOp() {};
	
}
