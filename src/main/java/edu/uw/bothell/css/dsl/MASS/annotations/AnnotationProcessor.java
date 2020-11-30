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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * AnnotationProcessor contains helper methods for working with annotations within MASS
 *
 */
public class AnnotationProcessor {

	/**
	 * Given an annotation, an optional argument, and the target class, obtain the first method in the target class that is annotated as such 
	 * @param annotation The annotation to lookup
	 * @param argumentClazz A single argument (optional) for matching the correct method
	 * @param clazz The target class to perform the lookup on
	 * @return The annotated Method that matches supplied arguments, or NULL if no such annotated method exists in the target class
	 */
	public static Method getAnnotatedMethod( Class< ? extends Annotation > annotation, Class< ? > argumentClazz, Class< ? > clazz ) {

		Objects.requireNonNull( annotation, "Must provide an annotation!" );
		Objects.requireNonNull( clazz, "Must provide a source class!" );
		
		// get all public methods exposed by this class
		final List< Method > allMethods = new ArrayList<>( Arrays.asList( clazz.getDeclaredMethods() ) );

		// find requested annotated method
		for ( final Method method : allMethods ) {
            
			if ( method.isAnnotationPresent( annotation ) ) {

				// are arguments required?
				if ( argumentClazz == null ) return method;
				
				// see if this method accepts a single argument, of type argumentClazz
				Class< ? >[] methodParams = method.getParameterTypes();
				if (methodParams.length != 1) continue;
				if (methodParams[0] == argumentClazz) return method;
				
            }
			
		}
		
		// method not found
		return null;

	}

}
