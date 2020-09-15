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

package edu.uw.bothell.css.dsl.MASS.factory;

import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.uw.bothell.css.dsl.MASS.SimpleTestPlace;

/**
 * Perform a series of unit tests against the SimpleObjectFactory class to verify proper
 * and consistent behavior of the class / methods
 */
public class SimpleObjectFactoryTest {

	// class under test
	private ObjectFactory objectFactory = SimpleObjectFactory.getInstance();

	@Test
	public void getInstance() throws Exception {
	
		SimpleTestPlace place = objectFactory.getInstance( SimpleTestPlace.class.getName(), new String() );
		
		assertNotNull( place );
		
	}
	
	@Test
	public void getInstanceNoObjectConstructor() throws Exception {
		
		Assertions.assertThrows(Exception.class, () -> {
			@SuppressWarnings("unused")
			SimpleTestPlace place = objectFactory.getInstance( String.class.getName(), new String() );
		});

	}
	
	@Test
	public void getInstanceClassNotFound() {
		
		Assertions.assertThrows(Exception.class, () -> {
			@SuppressWarnings("unused")
			SimpleTestPlace place = objectFactory.getInstance( "com.prr.lineswest.crestline.roundhouse", new String() );
		});

	}
	
	@Test
	public void addLibrary() throws Exception {

		// should NOT throw an Exception
		objectFactory.addLibrary( "rt.jar" );

	}
	
	@Test
	public void addInvalidUri() {
		
		Assertions.assertThrows(Exception.class, () -> {
			objectFactory.addUri( "www.pontiac.com" );
		});
		
	}
	
}
